/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.server.asset;

import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.event.ClientEventService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.security.UserConfiguration;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.manager.shared.security.User;
import org.openremote.model.Constants;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.*;
import org.openremote.model.asset.AbstractAssetQuery.OrderBy;
import org.openremote.model.asset.AbstractAssetQuery.Select;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;
import org.postgresql.util.PGobject;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetRoute.isPersistenceEventForEntityType;
import static org.openremote.manager.server.event.ClientEventService.CLIENT_EVENT_TOPIC;
import static org.openremote.manager.server.event.ClientEventService.getSessionKey;
import static org.openremote.model.asset.AbstractAssetQuery.Access.PRIVATE_READ;
import static org.openremote.model.asset.AbstractAssetQuery.Access.RESTRICTED_READ;
import static org.openremote.model.asset.AbstractAssetQuery.Include.ALL;
import static org.openremote.model.asset.AbstractAssetQuery.Include.ALL_EXCEPT_PATH_AND_ATTRIBUTES;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AssetStorageService extends RouteBuilder implements ContainerService, Consumer<AssetState> {

    protected class PreparedAssetQuery {
        protected String querySql;
        protected List<ParameterBinder> binders;

        public PreparedAssetQuery(String querySql, List<ParameterBinder> binders) {
            this.querySql = querySql;
            this.binders = binders;
        }

        protected void apply(PreparedStatement preparedStatement) {
            for (ParameterBinder binder : binders) {
                binder.accept(preparedStatement);
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(AssetStorageService.class.getName());

    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;
    protected ClientEventService clientEventService;

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);

        clientEventService.addSubscriptionAuthorizer((auth, subscription) -> {
            if (!subscription.isEventType(AssetTreeModifiedEvent.class))
                return false;
            return identityService.getIdentityProvider().canSubscribeWith(
                auth,
                subscription.getFilter() instanceof TenantFilter ? ((TenantFilter) subscription.getFilter()) : null,
                ClientRole.READ_ASSETS
            );
        });

        container.getService(WebService.class).getApiSingletons().add(
            new AssetResourceImpl(
                container.getService(TimerService.class),
                identityService,
                this,
                container.getService(MessageBrokerService.class)
            )
        );

        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void accept(AssetState assetState) {
        String assetId = assetState.getId();
        String attributeName = assetState.getAttribute().getName()
            .orElseThrow(() -> new IllegalStateException("Cannot store asset state for attribute with no name"));
        Value value = assetState.getAttribute().getValue().orElse(null);
        // If there is no timestamp, use system time (0 or -1 are "no timestamp")
        Optional<Long> timestamp = assetState.getAttribute().getValueTimestamp();
        String valueTimestamp = Long.toString(
            timestamp.map(ts -> ts > 0 ? ts : timerService.getCurrentTimeMillis())
                .orElseGet(() -> timerService.getCurrentTimeMillis())
        );
        if (!storeAttributeValue(assetId, attributeName, value, valueTimestamp)) {
            throw new RuntimeException("Database update failed, no rows updated");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        // If any asset was modified in the database, publish events
        from(PERSISTENCE_TOPIC)
            .routeId("AssetPersistenceChanges")
            .filter(isPersistenceEventForEntityType(ServerAsset.class))
            .process(exchange -> publishModificationEvents(exchange.getIn().getBody(PersistenceEvent.class)));

        // React if a client wants to read attribute state
        from(CLIENT_EVENT_TOPIC)
            .routeId("FromClientReadRequests")
            .filter(body().isInstanceOf(ReadAssetAttributesEvent.class))
            .process(exchange -> {
                ReadAssetAttributesEvent event = exchange.getIn().getBody(ReadAssetAttributesEvent.class);
                LOG.fine("Handling from client: " + event);

                if (event.getAssetId() == null || event.getAssetId().isEmpty())
                    return;

                String sessionKey = getSessionKey(exchange);
                AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);

                // Superuser can get all
                if (authContext.isSuperUser()) {
                    ServerAsset asset = find(event.getAssetId(), true);
                    if (asset != null)
                        replyWithAttributeEvents(sessionKey, asset, event.getAttributeNames());
                    return;
                }

                // User must have role
                if (!authContext.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                    return;
                }

                ServerAsset asset = find(
                    event.getAssetId(),
                    true,
                    identityService.getIdentityProvider().isRestrictedUser(authContext.getUserId()) ? RESTRICTED_READ : PRIVATE_READ
                );
                if (asset != null) {
                    replyWithAttributeEvents(sessionKey, asset, event.getAttributeNames());
                }
            });

    }

    public ServerAsset find(String assetId) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().id(assetId));
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     */
    public ServerAsset find(String assetId, boolean loadComplete) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().select(new Select(loadComplete ? ALL : ALL_EXCEPT_PATH_AND_ATTRIBUTES)).id(assetId));
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @param access       The required access permissions of the asset data.
     */
    public ServerAsset find(String assetId, boolean loadComplete, AbstractAssetQuery.Access access) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().select(new Select(loadComplete ? ALL : ALL_EXCEPT_PATH_AND_ATTRIBUTES, access)).id(assetId));
    }

    public ServerAsset find(AbstractAssetQuery query) {
        return persistenceService.doReturningTransaction(em -> find(em, query));
    }

    public List<ServerAsset> findAll(AbstractAssetQuery query) {
        return persistenceService.doReturningTransaction(em -> findAll(em, query));
    }

    public List<String> findNames(String... ids) {
        if (ids == null || ids.length == 0)
            return new ArrayList<>();

        // TODO: Do this in a loop in reasonably sized batches
        return persistenceService.doReturningTransaction(em -> {
            List<Object[]> result = em.createQuery("select a.id, a.name from Asset a where a.id in :ids", Object[].class)
                .setParameter("ids", Arrays.asList(ids))
                .getResultList();
            List<String> names = new ArrayList<>();
            for (String id : ids) {
                for (Object[] tuple : result) {
                    if (tuple[0].equals(id)) {
                        names.add((String) tuple[1]);
                        break;
                    }
                }
            }
            return names;
        });
    }

    /**
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public ServerAsset merge(ServerAsset asset) {
        return merge(asset, false);
    }

    /**
     * @param overrideVersion If <code>true</code>, the merge will override the data in the database, independent of version.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public ServerAsset merge(ServerAsset asset, boolean overrideVersion) {
        return merge(asset, overrideVersion, null);
    }

    /**
     * @param userName the user which this asset needs to be assigned to.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public ServerAsset merge(ServerAsset asset, String userName) {
        return merge(asset, false, userName);
    }

    /**
     * @param overrideVersion If <code>true</code>, the merge will override the data in the database, independent of version.
     * @param userName        the user which this asset needs to be assigned to.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public ServerAsset merge(ServerAsset asset, boolean overrideVersion, String userName) {
        return persistenceService.doReturningTransaction(em -> {

            // Update all empty attribute timestamps with server-time (a caller which doesn't have a
            // reliable time source such as a browser should clear the timestamp when setting an attribute
            // value).
            asset.getAttributesStream().forEach(attribute -> {
                Optional<Long> timestamp = attribute.getValueTimestamp();
                if (!timestamp.isPresent() || timestamp.get() <= 0) {
                    attribute.setValueTimestamp(timerService.getCurrentTimeMillis());
                }
            });

            // Validate parent
            if (asset.getParentId() != null) {
                // If this is a not a root asset...
                ServerAsset parent = find(em, asset.getParentId(), true);
                // .. the parent must exist
                if (parent == null)
                    throw new IllegalStateException("Parent not found: " + asset.getParentId());
                // ... the parent can not be a child of the asset
                if (parent.pathContains(asset.getId()))
                    throw new IllegalStateException("Invalid parent");

                // .. the parent should be in the same realm
                if (asset.getRealmId() != null && !parent.getRealmId().equals(asset.getRealmId())) {
                    throw new IllegalStateException("Parent not in same realm as asset: " + asset.getRealmId());
                } else if (asset.getRealmId() == null) {
                    // ... and if we don't have a realm identifier, use the parent's
                    asset.setRealmId(parent.getRealmId());
                }
            }

            // Validate realm
            if (!identityService.getIdentityProvider().isActiveTenant(asset.getRealmId())) {
                throw new IllegalStateException("Realm not found/active: " + asset.getRealmId());
            }

            // Validate attributes
            int invalid = 0;
            for (AssetAttribute attribute : asset.getAttributesList()) {
                List<ValidationFailure> validationFailures = attribute.getValidationFailures();
                if (!validationFailures.isEmpty()) {
                    LOG.warning("Validation failure(s) " + validationFailures + ", can't store: " + attribute);
                    invalid++;
                }
            }
            if (invalid > 0) {
                throw new IllegalStateException("Storing asset failed, invalid attributes: " + invalid);
            }

            // If this is real merge and desired, copy the persistent version number over the detached
            // version, so the detached state always wins and this update will go through and ignore
            // concurrent updates
            if (asset.getId() != null && overrideVersion) {
                ServerAsset existing = em.find(ServerAsset.class, asset.getId());
                if (existing != null) {
                    asset.setVersion(existing.getVersion());
                }
            }

            // If username present
            User user = null;
            if (!TextUtil.isNullOrEmpty(userName)) {
                user = identityService.getIdentityProvider().getUser(asset.getRealmId(), userName);
                if (user == null) {
                    throw new IllegalStateException("User not found: " + userName);
                }
            }

            LOG.fine("Storing: " + asset);

            ServerAsset updatedAsset = em.merge(asset);

            if (user != null) {
                storeUserAsset(em, new UserAsset(user.getRealmId(), user.getId(), updatedAsset.getId()));
            }

            return updatedAsset;
        });
    }

    /**
     * @return <code>true</code> if the asset was deleted, false if the asset still has children and can't be deleted.
     */
    public boolean delete(String assetId) {
        return persistenceService.doReturningTransaction(em -> {
            Asset asset = em.find(ServerAsset.class, assetId);
            if (asset != null) {
                List<ServerAsset> children = findAll(em, new AssetQuery()
                    .parent(new AssetQuery.ParentPredicate(asset.getId()))
                );
                if (children.size() > 0)
                    return false;
                LOG.fine("Removing: " + asset);
                em.remove(asset);
            }
            return true;
        });
    }

    public boolean isUserAsset(String userId, String assetId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            try {
                return entityManager.createQuery(
                    "select count(ua) from UserAsset ua where ua.id.userId = :userId and ua.id.assetId = :assetId",
                    Long.class)
                    .setParameter("userId", userId)
                    .setParameter("assetId", assetId)
                    .getSingleResult() > 0;
            } catch (NoResultException ex) {
                return false;
            }
        });
    }

    public List<UserAsset> findUserAssets(String realmId, String userId, String assetId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            if (isNullOrEmpty(userId) && isNullOrEmpty(assetId)) {
                return entityManager.createQuery(
                    "select ua from UserAsset ua where ua.id.realmId = :realmId order by ua.createdOn desc", UserAsset.class
                ).setParameter("realmId", realmId).getResultList();
            } else if (isNullOrEmpty(assetId)) {
                return entityManager.createQuery(
                    "select ua from UserAsset ua where ua.id.realmId = :realmId and ua.id.userId = :userId order by ua.createdOn desc", UserAsset.class
                ).setParameter("realmId", realmId).setParameter("userId", userId).getResultList();

            } else if (isNullOrEmpty(userId)) {
                return entityManager.createQuery(
                    "select ua from UserAsset ua where ua.id.realmId = :realmId and ua.id.assetId = :assetId order by ua.createdOn desc", UserAsset.class
                ).setParameter("realmId", realmId).setParameter("assetId", assetId).getResultList();
            } else {
                return entityManager.createQuery(
                    "select ua from UserAsset ua where ua.id.realmId = :realmId and ua.id.userId = :userId and ua.id.assetId = :assetId order by ua.createdOn desc", UserAsset.class
                ).setParameter("realmId", realmId).setParameter("userId", userId).setParameter("assetId", assetId).getResultList();
            }
        });
    }

    /**
     * Also marks the user as restricted, manages {@link UserConfiguration#isRestricted()}.
     */
    public void storeUserAsset(UserAsset userAsset) {
        persistenceService.doTransaction(entityManager -> storeUserAsset(entityManager, userAsset));
    }

    /**
     * Also marks the user as unrestricted if no assets are linked to the user, manages {@link UserConfiguration#isRestricted()}.
     */
    public void deleteUserAsset(String realmId, String userId, String assetId) {
        persistenceService.doTransaction(entityManager -> {
            UserAsset userAsset = entityManager.find(UserAsset.class, new UserAsset.Id(realmId, userId, assetId));
            if (userAsset != null)
                entityManager.remove(userAsset);

            // If there are no more assets linked to this user
            long count = entityManager.createQuery(
                "select count(ua) from UserAsset ua where ua.id.userId = :userId", Long.class
            ).setParameter("userId", userId).getSingleResult();
            if (count == 0) {
                // The user must be configured as not restricted
                UserConfiguration userConfiguration = getUserConfiguration(entityManager, userAsset.getId().getUserId());
                userConfiguration.setRestricted(false);
                mergeUserConfiguration(entityManager, userConfiguration);
            }
        });
    }

    protected void storeUserAsset(EntityManager entityManager, UserAsset userAsset) {
        // The user must be configured as restricted
        UserConfiguration userConfiguration = getUserConfiguration(entityManager, userAsset.getId().getUserId());
        userConfiguration.setRestricted(true);
        mergeUserConfiguration(entityManager, userConfiguration);

        userAsset.setCreatedOn(new Date(timerService.getCurrentTimeMillis()));
        entityManager.merge(userAsset);
    }

    protected UserConfiguration getUserConfiguration(EntityManager em, String userId) {
        UserConfiguration userConfiguration = em.find(UserConfiguration.class, userId);
        if (userConfiguration == null) {
            userConfiguration = new UserConfiguration(userId);
            userConfiguration = mergeUserConfiguration(em, userConfiguration);
        }
        return userConfiguration;
    }

    protected UserConfiguration mergeUserConfiguration(EntityManager em, UserConfiguration userConfiguration) {
        if (userConfiguration.getUserId() == null || userConfiguration.getUserId().length() == 0) {
            throw new IllegalArgumentException("User ID must be set on: " + userConfiguration);
        }
        return em.merge(userConfiguration);
    }

    /* ####################################################################################### */

    protected interface ParameterBinder extends Consumer<PreparedStatement> {
        @Override
        default void accept(PreparedStatement st) {
            try {
                acceptStatement(st);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        void acceptStatement(PreparedStatement st) throws SQLException;
    }

    protected ServerAsset find(EntityManager em, String assetId, boolean loadComplete) {
        return find(em, assetId, loadComplete, PRIVATE_READ);
    }

    protected ServerAsset find(EntityManager em, String assetId, boolean loadComplete, AbstractAssetQuery.Access access) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(
            em,
            new AssetQuery().select(
                new Select(loadComplete ? ALL : ALL_EXCEPT_PATH_AND_ATTRIBUTES, access)
            ).id(assetId)
        );
    }

    public ServerAsset find(EntityManager em, AbstractAssetQuery query) {
        List<ServerAsset> result = findAll(em, query);
        if (result.size() == 0)
            return null;
        if (result.size() > 1) {
            throw new IllegalArgumentException("Query returned more than one asset");
        }
        return result.get(0);

    }

    protected List<ServerAsset> findAll(EntityManager em, AbstractAssetQuery query) {

        // Use a default projection if it's missing
        if (query.select == null)
            query.select = new Select();
        if (query.select.include == null)
            query.select.include = ALL_EXCEPT_PATH_AND_ATTRIBUTES;
        if (query.select.access == null)
            query.select.access = PRIVATE_READ;

        // Default to order by creation date if the query may return multiple results
        if (query.orderBy == null && query.id == null)
            query.orderBy = new OrderBy(OrderBy.Property.CREATED_ON);

        PreparedAssetQuery querySql = buildQuery(query);

        return em.unwrap(Session.class).doReturningWork(new AbstractReturningWork<List<ServerAsset>>() {
            @Override
            public List<ServerAsset> execute(Connection connection) throws SQLException {
                LOG.fine("Executing: " + querySql.querySql);
                PreparedStatement st = connection.prepareStatement(querySql.querySql);
                querySql.apply(st);

                try (ResultSet rs = st.executeQuery()) {
                    List<ServerAsset> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(mapResultTuple(query, rs));
                    }
                    return result;
                }
            }
        });
    }

    protected PreparedAssetQuery buildQuery(AbstractAssetQuery query) {
        LOG.fine("Building: " + query);
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.select.recursive;
        List<ParameterBinder> binders = new ArrayList<>();
        sb.append(buildSelectString(query, 1, binders));
        sb.append(buildFromString(query, 1));
        sb.append(buildWhereClause(query, 1, binders));

        if (recursive) {
            sb.insert(0, "WITH RECURSIVE top_level_assets AS ((");
            sb.append(") UNION (");
            sb.append(buildSelectString(query, 2, binders));
            sb.append(buildFromString(query, 2));
            sb.append(buildWhereClause(query, 2, binders));
            sb.append("))");
            sb.append(buildSelectString(query, 3, binders));
            sb.append(buildFromString(query, 3));
            sb.append(buildWhereClause(query, 3, binders));
        }

        sb.append(buildOrderByString(query));
        return new PreparedAssetQuery(sb.toString(), binders);
    }

    protected String buildSelectString(AbstractAssetQuery query, int level, List<ParameterBinder> binders) {
        // level = 1 is main query select
        // level = 2 is union select
        // level = 3 is CTE select
        StringBuilder sb = new StringBuilder();
        AssetQuery.Include include = query.select.include;
        boolean recursive = query.select.recursive;
        boolean includeMainProperties = include == ALL ||
            include == AssetQuery.Include.ALL_EXCEPT_PATH ||
            include == ALL_EXCEPT_PATH_AND_ATTRIBUTES;

        sb.append("select A.ID as ID, A.NAME as NAME, A.ACCESS_PUBLIC_READ as ACCESS_PUBLIC_READ");

        if (query.orderBy != null || includeMainProperties) {
            sb.append(", A.CREATED_ON AS CREATED_ON, A.ASSET_TYPE AS ASSET_TYPE, A.PARENT_ID AS PARENT_ID, A.REALM_ID AS REALM_ID");
        }

        if (include == AssetQuery.Include.ONLY_ID_AND_NAME) {
            return sb.toString();
        }
        switch (include) {
            case ALL_EXCEPT_PATH_AND_ATTRIBUTES:
            case ALL_EXCEPT_PATH:
            case ALL:
                sb.append(", A.OBJ_VERSION as OBJ_VERSION, A.LOCATION as LOCATION");
                sb.append(", P.NAME as PARENT_NAME, P.ASSET_TYPE as PARENT_TYPE");
                if (!recursive || level == 3) {
                    sb.append(", R.NAME as TENANT_NAME, RA.VALUE as TENANT_DISPLAY_NAME");
                }
                break;
        }

        if (!recursive || level == 3) {
            if (include == ALL) {
                sb.append(", get_asset_tree_path(A.ID) as PATH");
            } else {
                sb.append(", NULL as PATH");
            }
        }

        if (include != ALL_EXCEPT_PATH_AND_ATTRIBUTES) {

            if (recursive && level != 3) {
                sb.append(", A.ATTRIBUTES as ATTRIBUTES");
            } else {
                boolean namesOnly = include == AssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES;
                sb.append(buildAttributeSelect(query.select.attributeNames, query.select.access, namesOnly, binders));
            }
        } else {
            sb.append(", NULL as ATTRIBUTES");
        }

        return sb.toString();
    }

    protected String buildAttributeSelect(String[] attributeNames, AbstractAssetQuery.Access access, boolean namesOnly, List<ParameterBinder> binders) {
        if (attributeNames == null && access == PRIVATE_READ && !namesOnly) {
            return ", A.ATTRIBUTES as ATTRIBUTES";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(", (");

        if (namesOnly) {
            sb.append("select json_object_agg(AX.key, jsonb_set('{}'::jsonb, '{meta}', (");
            sb.append("select jsonb_agg(AM.VALUE)");
            sb.append(" from jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
            sb.append(" where AM.VALUE #>> '{name}' = '");
            sb.append(AssetMeta.LABEL.getUrn());
            sb.append("'))) from jsonb_each(A.attributes) as AX");
            if (access != PRIVATE_READ) {
                // Use implicit inner join on meta array set to only select non-private attributes
                sb.append(", jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
            }
        } else if (access != PRIVATE_READ) {
            // Use sub-select for processing the attributes the meta inside each attribute is replaced with filtered meta
            // (coalesce null to empty array because jsonb_set() with null will clear the whole object)
            sb.append("select json_object_agg(AX.key, jsonb_set(AX.value, '{meta}', coalesce(AMF.VALUE, jsonb_build_array()), false)) from jsonb_each(A.attributes) as AX");
            // Use implicit inner join on meta array set to only select attributes with a non-private access meta item
            sb.append(", jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
            // Use subquery to filter out meta items not marked as non-private access
            sb.append(" INNER JOIN LATERAL (");
            sb.append("select jsonb_agg(AM.value) AS VALUE from jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
            sb.append(" where AM.VALUE #>> '{name}' IN");
            sb.append(access == RESTRICTED_READ ? AssetModel.META_ITEM_RESTRICTED_READ_SQL_FRAGMENT : AssetModel.META_ITEM_PUBLIC_READ_SQL_FRAGMENT);
            sb.append(") as AMF ON true");
        } else {
            sb.append("select json_object_agg(AX.key, AX.value) from jsonb_each(A.attributes) as AX");
        }

        sb.append(" where true");

        if (attributeNames != null && attributeNames.length > 0) {
            sb.append(" AND AX.key IN (");
            for (int i = 0; i < attributeNames.length; i++) {
                sb.append(i == attributeNames.length - 1 ? "?" : "?,");
                final String attributeName = attributeNames[i];
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, attributeName));
            }
            sb.append(") ");
        }

        if (access != PRIVATE_READ) {
            // Filter non-private access attributes
            AssetQuery.AttributeMetaPredicate accessPredicate =
                new AssetQuery.AttributeMetaPredicate()
                    .itemName(access == RESTRICTED_READ ? AssetMeta.ACCESS_RESTRICTED_READ: AssetMeta.ACCESS_PUBLIC_READ)
                    .itemValue(new AssetQuery.BooleanPredicate(true));
            sb.append(buildAttributeMetaFilter(accessPredicate, binders));
        }

        sb.append(") AS ATTRIBUTES");
        return sb.toString();
    }

    protected String buildFromString(AbstractAssetQuery query, int level) {
        // level = 1 is main query
        // level = 2 is union
        // level = 3 is CTE
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.select.recursive;

        if (level == 1) {
            sb.append(" from ASSET A ");
        } else if (level == 2) {
            sb.append(" from top_level_assets P ");
            sb.append("join ASSET A on A.PARENT_ID = P.ID ");
        } else {
            sb.append(" from top_level_assets A ");
        }

        boolean includeRealmInfo = query.select.include != AssetQuery.Include.ONLY_ID_AND_NAME &&
            query.select.include != AssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES &&
            query.select.include != AssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES;

        if ((!recursive || level == 3) && (includeRealmInfo || query.tenantPredicate != null)) {
            sb.append("join PUBLIC.REALM R on R.ID = A.REALM_ID ");
            sb.append("join PUBLIC.REALM_ATTRIBUTE RA on RA.REALM_ID = R.ID and RA.NAME = 'displayName' ");
        }

        if ((!recursive || level == 3) && query.id == null && query.userId != null) {
            sb.append("cross join USER_ASSET ua ");
        }

        if (level == 1) {
            if (query.parentPredicate != null && !query.parentPredicate.noParent) {
                sb.append("cross join ASSET P ");
            } else {
                sb.append("left outer join ASSET P on A.PARENT_ID = P.ID ");
            }
        }

        return sb.toString();
    }

    protected String buildOrderByString(AbstractAssetQuery query) {
        StringBuilder sb = new StringBuilder();

        if (query.id != null && !query.select.recursive) {
            return sb.toString();
        }

        if (query.orderBy != null && query.orderBy.property != null) {
            sb.append(" order by ");

            switch (query.orderBy.property) {
                case CREATED_ON:
                    sb.append(" A.CREATED_ON ");
                    break;
                case ASSET_TYPE:
                    sb.append(" A.ASSET_TYPE ");
                    break;
                case NAME:
                    sb.append(" A.NAME ");
                    break;
                case PARENT_ID:
                    sb.append(" A.PARENT_ID ");
                    break;
                case REALM_ID:
                    sb.append(" A.REALM_ID ");
                    break;
            }
            sb.append(query.orderBy.descending ? "desc " : "asc ");
        }

        return sb.toString();
    }

    protected String buildWhereClause(AbstractAssetQuery query, int level, List<ParameterBinder> binders) {
        // level = 1 is main query
        // level = 2 is union
        // level = 3 is CTE
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.select.recursive;
        sb.append(" where true");

        if (level == 2) {
            return sb.toString();
        }

        if (level == 1 && query.id != null) {
            sb.append(" and A.ID = ?");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.id));
        }

        if (level == 1 && query.namePredicate != null) {
            sb.append(query.namePredicate.caseSensitive ? " and A.NAME " : " and upper(A.NAME)");
            sb.append(query.namePredicate.match == AssetQuery.Match.EXACT ? " = ?" : " like ?");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.namePredicate.prepareValue()));
        }

        if (query.parentPredicate != null) {
            // Can only restrict recursive query parent by asset type
            if (level == 1 && query.parentPredicate.id != null) {
                sb.append(" and p.ID = a.PARENT_ID");
                sb.append(" and A.PARENT_ID = ?");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.parentPredicate.id));
            } else if (query.parentPredicate.type != null) {
                sb.append(" and p.ID = a.PARENT_ID");
                sb.append(" and P.ASSET_TYPE = ?");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.parentPredicate.type));
            } else if (level == 1 && query.parentPredicate.noParent) {
                sb.append(" and A.PARENT_ID is null");
            }
        }

        if (level == 1 && query.pathPredicate != null && query.pathPredicate.hasPath()) {
            sb.append(" and ? <@ get_asset_tree_path(A.ID)");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setArray(pos, st.getConnection().createArrayOf("text", query.pathPredicate.path)));
        }

        if (!recursive || level == 3) {
            if (query.tenantPredicate != null && query.tenantPredicate.realmId != null) {
                sb.append(" and R.ID = ?");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.tenantPredicate.realmId));
            } else if (query.tenantPredicate != null && query.tenantPredicate.realm != null) {
                sb.append(" and R.NAME = ?");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.tenantPredicate.realm));
            }

            if (query.userId != null) {
                sb.append(" and ua.ASSET_ID = a.ID and ua.USER_ID = ?");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.userId));
            }

            if (level == 1 && query.select.access == AbstractAssetQuery.Access.PUBLIC_READ) {
                sb.append(" and A.ACCESS_PUBLIC_READ is true");
            }

            if (query.type != null) {
                sb.append(query.type.caseSensitive ? " and A.ASSET_TYPE" : " and upper(A.ASSET_TYPE)");
                sb.append(query.type.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.type.prepareValue()));
            }

            if (query.attributeMetaPredicate != null) {
                String attributeMetaFilter = buildAttributeMetaFilter(query.attributeMetaPredicate, binders);

                if (attributeMetaFilter.length() > 0) {
                    sb.append(" and A.ID in (select A.ID from");
                    sb.append(" jsonb_each(A.ATTRIBUTES) as AX,");
                    sb.append(" jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
                    sb.append(" where true");
                    sb.append(attributeMetaFilter);
                    sb.append(")");
                }
            }

            if (query.attributePredicateArray != null && query.attributePredicateArray.predicates != null) {
                for (AssetQuery.AttributePredicate attributePredicate : query.attributePredicateArray.predicates) {
                    StringBuilder attributeFilterBuilder = new StringBuilder();
                    attributeFilterBuilder.append(buildAttributeFilter(attributePredicate, binders));

                    if (attributeFilterBuilder.length() > 0) {
                        sb.append(" and A.ID in (select A.ID from");
                        sb.append(" jsonb_each(A.ATTRIBUTES) as AX");
                        sb.append(" where true");
                        sb.append(attributeFilterBuilder.toString());
                        sb.append(")");
                    }
                }
            }
        }
        return sb.toString();
    }

    protected String buildAttributeMetaFilter(AssetQuery.AttributeMetaPredicate attributeMetaPredicate, List<ParameterBinder> binders) {
        StringBuilder attributeMetaBuilder = new StringBuilder();

        if (attributeMetaPredicate.itemNamePredicate != null) {
            attributeMetaBuilder.append(attributeMetaPredicate.itemNamePredicate.caseSensitive
                ? " and AM.VALUE #>> '{name}'"
                : " and upper(AM.VALUE #>> '{name}')"
            );
            attributeMetaBuilder.append(attributeMetaPredicate.itemNamePredicate.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, attributeMetaPredicate.itemNamePredicate.prepareValue()));
        }
        if (attributeMetaPredicate.itemValuePredicate != null) {
            if (attributeMetaPredicate.itemValuePredicate instanceof AssetQuery.StringPredicate) {
                AssetQuery.StringPredicate stringPredicate = (AssetQuery.StringPredicate) attributeMetaPredicate.itemValuePredicate;
                attributeMetaBuilder.append(stringPredicate.caseSensitive
                    ? " and AM.VALUE #>> '{value}'"
                    : " and upper(AM.VALUE #>> '{value}')"
                );
                attributeMetaBuilder.append(stringPredicate.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
            } else if (attributeMetaPredicate.itemValuePredicate instanceof AssetQuery.BooleanPredicate) {
                AssetQuery.BooleanPredicate booleanPredicate = (AssetQuery.BooleanPredicate) attributeMetaPredicate.itemValuePredicate;
                attributeMetaBuilder.append(" and AM.VALUE #> '{value}' = to_jsonb(")
                    .append(booleanPredicate.predicate)
                    .append(")");
            } else if (attributeMetaPredicate.itemValuePredicate instanceof AssetQuery.StringArrayPredicate) {
                AssetQuery.StringArrayPredicate stringArrayPredicate = (AssetQuery.StringArrayPredicate) attributeMetaPredicate.itemValuePredicate;
                for (int i = 0; i < stringArrayPredicate.predicates.length; i++) {
                    AssetQuery.StringPredicate stringPredicate = stringArrayPredicate.predicates[i];
                    attributeMetaBuilder.append(stringPredicate.caseSensitive
                        ? " and AM.VALUE #> '{value}' ->> " + i
                        : " and upper(AM.VALUE #> '{value}' ->> " + i + ")"
                    );
                    attributeMetaBuilder.append(stringPredicate.match == AssetQuery.Match.EXACT ? " = ?" : " like ?");
                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
                }
            }
        }

        return attributeMetaBuilder.toString();
    }

    protected String buildAttributeFilter(AssetQuery.AttributePredicate attributePredicate, List<ParameterBinder> binders) {
        StringBuilder attributeBuilder = new StringBuilder();

        if (attributePredicate.itemNamePredicate != null) {
            attributeBuilder.append(attributePredicate.itemNamePredicate.caseSensitive
                ? " and AX.key"
                : " and upper(AX.key)"
            );
            attributeBuilder.append(attributePredicate.itemNamePredicate.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, attributePredicate.itemNamePredicate.prepareValue()));
        }
        if (attributePredicate.itemValuePredicate != null) {
            if (attributePredicate.itemValuePredicate instanceof AssetQuery.StringPredicate) {
                AssetQuery.StringPredicate stringPredicate = (AssetQuery.StringPredicate) attributePredicate.itemValuePredicate;
                attributeBuilder.append(stringPredicate.caseSensitive
                    ? " and AX.VALUE #>> '{value}'"
                    : " and upper(AX.VALUE #>> '{value}')"
                );
                attributeBuilder.append(stringPredicate.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
            } else if (attributePredicate.itemValuePredicate instanceof AssetQuery.BooleanPredicate) {
                AssetQuery.BooleanPredicate booleanPredicate = (AssetQuery.BooleanPredicate) attributePredicate.itemValuePredicate;
                attributeBuilder.append(" and AX.VALUE #> '{value}' = to_jsonb(")
                    .append(booleanPredicate.predicate)
                    .append(")");
            } else if (attributePredicate.itemValuePredicate instanceof AssetQuery.StringArrayPredicate) {
                AssetQuery.StringArrayPredicate stringArrayPredicate = (AssetQuery.StringArrayPredicate) attributePredicate.itemValuePredicate;
                for (int i = 0; i < stringArrayPredicate.predicates.length; i++) {
                    AssetQuery.StringPredicate stringPredicate = stringArrayPredicate.predicates[i];
                    attributeBuilder.append(stringPredicate.caseSensitive
                        ? " and AX.VALUE #> '{value}' ->> " + i
                        : " and upper(AX.VALUE #> '{value}' ->> " + i + ")"
                    );
                    attributeBuilder.append(stringPredicate.match == AssetQuery.Match.EXACT ? " = ?" : " like ?");
                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
                }
            } else if (attributePredicate.itemValuePredicate instanceof AssetQuery.DateTimePredicate) {
                AssetQuery.DateTimePredicate dateTimePredicate = (AssetQuery.DateTimePredicate) attributePredicate.itemValuePredicate;
                attributeBuilder.append(" and to_timestamp(AX.VALUE #>> '{value}', ?)");
                final int keyFormatPos = binders.size() + 1;
                binders.add(st -> st.setString(keyFormatPos, dateTimePredicate.dateFormat));

                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, dateTimePredicate.value));
                final int formatPos = binders.size() + 1;
                binders.add(st -> st.setString(formatPos, dateTimePredicate.dateFormat));

                switch (dateTimePredicate.operatorMatch) {
                    case EXACT:
                        attributeBuilder.append(" = to_timestamp(?, ?)");
                        break;
                    case GREATER_THEN:
                        attributeBuilder.append(" > to_timestamp(?, ?)");
                        break;
                    case GREATER_EQUALS:
                        attributeBuilder.append(" >= to_timestamp(?, ?)");
                        break;
                    case LESS_THEN:
                        attributeBuilder.append(" < to_timestamp(?, ?)");
                        break;
                    case LESS_EQUALS:
                        attributeBuilder.append(" <= to_timestamp(?, ?)");
                        break;
                    case BETWEEN:
                        attributeBuilder.append(" BETWEEN to_timestamp(?, ?) AND to_timestamp(?, ?)");
                        final int pos2 = binders.size() + 1;
                        binders.add(st -> st.setString(pos2, dateTimePredicate.rangeValue));
                        final int formatPos2 = binders.size() + 1;
                        binders.add(st -> st.setString(formatPos2, dateTimePredicate.dateFormat));
                        break;
                }
            } else if (attributePredicate.itemValuePredicate instanceof AssetQuery.NumberPredicate) {
                AssetQuery.NumberPredicate numberPredicate = (AssetQuery.NumberPredicate) attributePredicate.itemValuePredicate;
                attributeBuilder.append(" and (AX.VALUE #>> '{value}')::numeric");
                switch (numberPredicate.operatorMatch) {
                    case EXACT:
                    default:
                        attributeBuilder.append(" = ?");
                        break;
                    case GREATER_THEN:
                        attributeBuilder.append(" > ?");
                        break;
                    case GREATER_EQUALS:
                        attributeBuilder.append(" >= ?");
                        break;
                    case LESS_THEN:
                        attributeBuilder.append(" < ?");
                        break;
                    case LESS_EQUALS:
                        attributeBuilder.append(" <= ?");
                        break;
                    case BETWEEN:
                        attributeBuilder.append(" BETWEEN ? AND ?");
                        break;
                }

                final int pos = binders.size() + 1;
                switch (numberPredicate.numberType) {
                    case DOUBLE:
                    default:
                        binders.add(st -> st.setDouble(pos, numberPredicate.value));
                        if (numberPredicate.operatorMatch == AbstractAssetQuery.OperatorMatch.BETWEEN) {
                            final int pos2 = binders.size() + 1;
                            binders.add(st -> st.setDouble(pos, numberPredicate.rangeValue));
                        }
                        break;
                    case INTEGER:
                        binders.add(st -> st.setInt(pos, (int) numberPredicate.value));
                        if (numberPredicate.operatorMatch == AbstractAssetQuery.OperatorMatch.BETWEEN) {
                            final int pos2 = binders.size() + 1;
                            binders.add(st -> st.setInt(pos2, (int) numberPredicate.rangeValue));
                        }
                        break;
                }
            }
        }

        return attributeBuilder.toString();
    }

    protected ServerAsset mapResultTuple(AbstractAssetQuery query, ResultSet rs) throws SQLException {
        switch (query.select.include) {
            case ONLY_ID_AND_NAME:
            case ONLY_ID_AND_NAME_AND_ATTRIBUTES:
            case ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES:
                ServerAsset asset = new ServerAsset();
                asset.setId(rs.getString("ID"));
                asset.setName(rs.getString("NAME"));
                asset.setAccessPublicRead(rs.getBoolean("ACCESS_PUBLIC_READ"));
                if (query.select.include != AssetQuery.Include.ONLY_ID_AND_NAME) {
                    if (rs.getString("ATTRIBUTES") != null) {
                        asset.setAttributes(Values.instance().<ObjectValue>parse(rs.getString("ATTRIBUTES")).orElse(null));
                    }
                }
                return asset;
            case ALL_EXCEPT_PATH_AND_ATTRIBUTES:
            case ALL_EXCEPT_PATH:
            case ALL:
                return new ServerAsset(
                    rs.getString("ID"), rs.getLong("OBJ_VERSION"), rs.getTimestamp("CREATED_ON"), rs.getString("NAME"),
                    rs.getString("ASSET_TYPE"), rs.getBoolean("ACCESS_PUBLIC_READ"),
                    rs.getString("PARENT_ID"), rs.getString("PARENT_NAME"), rs.getString("PARENT_TYPE"),
                    rs.getString("REALM_ID"), rs.getString("TENANT_NAME"), rs.getString("TENANT_DISPLAY_NAME"),
                    rs.getObject("LOCATION"), rs.getArray("PATH"), rs.getString("ATTRIBUTES"));
            default:
                throw new UnsupportedOperationException("Select include option not supported: " + query.select.include);
        }
    }

    protected boolean storeAttributeValue(String assetId, String attributeName, Value value, String timestamp) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.unwrap(Session.class).doReturningWork(connection -> {

                String update =
                    "update ASSET" +
                        " set ATTRIBUTES = jsonb_set(jsonb_set(ATTRIBUTES, ?, ?, true), ?, ?, true)" +
                        " where ID = ? and ATTRIBUTES -> ? is not null";
                try (PreparedStatement statement = connection.prepareStatement(update)) {

                    // Bind the value (and check we don't have a SQL injection hole in attribute name!)
                    if (!AssetAttribute.ATTRIBUTE_NAME_VALIDATOR.test(attributeName)) {
                        LOG.fine(
                            "Invalid attribute name (must match '" + AssetAttribute.ATTRIBUTE_NAME_PATTERN + "'): " + attributeName
                        );
                        return false;
                    }

                    Array attributeValuePath = connection.createArrayOf(
                        "text",
                        new String[]{attributeName, "value"}
                    );
                    statement.setArray(1, attributeValuePath);

                    PGobject pgJsonValue = new PGobject();
                    pgJsonValue.setType("jsonb");
                    // Careful, do not set Java null (as returned by value.toJson()) here! It will erase your whole SQL column!
                    pgJsonValue.setValue(value == null ? "null" : value.toJson());
                    statement.setObject(2, pgJsonValue);

                    // Bind the value timestamp
                    Array attributeValueTimestampPath = connection.createArrayOf(
                        "text",
                        new String[]{attributeName, "valueTimestamp"}
                    );
                    statement.setArray(3, attributeValueTimestampPath);
                    PGobject pgJsonValueTimestamp = new PGobject();
                    pgJsonValueTimestamp.setType("jsonb");
                    pgJsonValueTimestamp.setValue(timestamp);
                    statement.setObject(4, pgJsonValueTimestamp);

                    // Bind asset ID and attribute name
                    statement.setString(5, assetId);
                    statement.setString(6, attributeName);

                    int updatedRows = statement.executeUpdate();
                    LOG.fine("Stored asset '" + assetId + "' attribute '" + attributeName + "' value, affected rows: " + updatedRows);
                    return updatedRows == 1;
                }
            })
        );
    }

    protected void publishModificationEvents(PersistenceEvent<ServerAsset> persistenceEvent) {
        ServerAsset asset = persistenceEvent.getEntity();
        switch (persistenceEvent.getCause()) {
            case INSERT:
                clientEventService.publishEvent(
                    new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealmId(), asset.getId())
                );
                if (asset.getParentId() != null) {
                    // Child asset created
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealmId(), asset.getParentId(), true)
                    );
                } else {
                    // Child asset created (root asset)
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealmId(), true)
                    );
                }
                break;
            case UPDATE:

                // Did the name change?
                String previousName = persistenceEvent.getPreviousState("name");
                String currentName = persistenceEvent.getCurrentState("name");
                if (!Objects.equals(previousName, currentName)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealmId(), asset.getId())
                    );
                    break;
                }

                // Did the parent change?
                String previousParentId = persistenceEvent.getPreviousState("parentId");
                String currentParentId = persistenceEvent.getCurrentState("parentId");
                if (!Objects.equals(previousParentId, currentParentId)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealmId(), asset.getId())
                    );
                    break;
                }

                // Did the realm change?
                String previousRealmId = persistenceEvent.getPreviousState("realmId");
                String currentRealmId = persistenceEvent.getCurrentState("realmId");
                if (!Objects.equals(previousRealmId, currentRealmId)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealmId(), asset.getId())
                    );
                    break;
                }

                break;
            case DELETE:
                clientEventService.publishEvent(
                    new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealmId(), asset.getId())
                );
                break;
        }
    }

    protected void replyWithAttributeEvents(String sessionKey, ServerAsset asset, String[] attributeNames) {
        List<String> names = attributeNames == null ? Collections.emptyList() : Arrays.asList(attributeNames);

        // Client may want to read a subset or all attributes of the asset
        List<AttributeEvent> events = asset.getAttributesStream()
            .filter(attribute -> names.isEmpty() || attribute.getName().filter(names::contains).isPresent())
            .map(AssetAttribute::getStateEvent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        clientEventService.sendToSession(sessionKey, events.toArray(new AttributeEvent[events.size()]));
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}