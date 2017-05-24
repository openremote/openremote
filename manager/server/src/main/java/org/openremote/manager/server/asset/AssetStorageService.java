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
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Constants;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.postgresql.util.PGobject;

import javax.persistence.EntityManager;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetRoute.isPersistenceEventForEntityType;
import static org.openremote.manager.server.event.ClientEventService.CLIENT_EVENT_TOPIC;
import static org.openremote.manager.server.event.ClientEventService.getSessionKey;

public class AssetStorageService extends RouteBuilder implements ContainerService, Consumer<AssetState> {

    private static final Logger LOG = Logger.getLogger(AssetStorageService.class.getName());
    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected ManagerIdentityService managerIdentityService;
    protected ClientEventService clientEventService;

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        persistenceService = container.getService(PersistenceService.class);
        managerIdentityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);

        clientEventService.addSubscriptionAuthorizer((auth, subscription) -> {
            if (!subscription.isEventType(AssetTreeModifiedEvent.class))
                return false;

            // Superuser can get all
            if (auth.isSuperUser())
                return true;

            // Restricted users get nothing (they don't have asset trees, just a list of linked assets)
            if (managerIdentityService.isRestrictedUser(auth.getUserId()))
                return false;

            // User must have role
            auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID);

            // Ensure filter matches authenticated realm
            if (subscription.getFilter() instanceof AssetTreeModifiedEvent.TenantFilter) {
                AssetTreeModifiedEvent.TenantFilter filter =
                    (AssetTreeModifiedEvent.TenantFilter) subscription.getFilter();

                Tenant authenticatedTenant =
                    managerIdentityService.getTenantForRealm(auth.getAuthenticatedRealm());
                if (authenticatedTenant == null)
                    return false;
                if (filter.getRealmId().equals(authenticatedTenant.getId()))
                    return true;
            }

            return false;
        });

        container.getService(WebService.class).getApiSingletons().add(
            new AssetResourceImpl(
                managerIdentityService,
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
        // Some sanity checking, of course the timestamp should never be -1 if we store updated attribute state
        long timestamp = assetState.getAttribute().getValueTimestamp();
        String valueTimestamp = Long.toString(
            timestamp >= 0 ? timestamp : timerService.getCurrentTimeMillis()
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
                    managerIdentityService.isRestrictedUser(authContext.getUserId()) // Restricted users get filtered state
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
        return find(new AssetQuery().select(new AssetQuery.Select(loadComplete, false)).id(assetId));
    }

    /**
     * @param loadComplete    If the whole asset data (including path and attributes) should be loaded.
     * @param filterProtected If the asset attributes should be filtered and only protected details included.
     */
    public ServerAsset find(String assetId, boolean loadComplete, boolean filterProtected) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().select(new AssetQuery.Select(loadComplete, filterProtected)).id(assetId));
    }

    public ServerAsset find(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> find(em, query));
    }

    public List<ServerAsset> findAll(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> findAll(em, query));
    }

    public List<String> findAllIds(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> findAllIdentifiersOnly(em, query));
    }

    /**
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public ServerAsset merge(ServerAsset asset) {
        return persistenceService.doReturningTransaction(em -> {
            // Validate realm
            if (!managerIdentityService.isActiveTenant(asset.getRealmId())) {
                throw new IllegalStateException("Realm not found/active: " + asset.getRealmId());
            }
            // Validate parent
            if (asset.getParentId() != null) {
                // If this is a not a root asset...
                ServerAsset parent = find(em, asset.getParentId(), true, false);
                // .. the parent must exist
                if (parent == null)
                    throw new IllegalStateException("Parent not found: " + asset.getParentId());
                // ... the parent can not be a child of the asset
                if (parent.pathContains(asset.getId()))
                    throw new IllegalStateException("Invalid parent");
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

            LOG.fine("Storing: " + asset);
            return em.merge(asset);
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
            UserAsset userAsset = entityManager.find(UserAsset.class, new UserAsset(userId, assetId));
            return userAsset != null;
        });
    }

    public void storeUserAsset(String userId, String assetId) {
        persistenceService.doTransaction(entityManager -> {
            UserAsset userAsset = new UserAsset(userId, assetId);
            entityManager.merge(userAsset);
        });
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

    protected ServerAsset find(EntityManager em, String assetId, boolean loadComplete, boolean filterProtected) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(em, new AssetQuery().select(new AssetQuery.Select(loadComplete, filterProtected)).id(assetId));
    }

    public ServerAsset find(EntityManager em, AssetQuery query) {
        List<ServerAsset> result = findAll(em, query);
        if (result.size() == 0)
            return null;
        if (result.size() > 1) {
            throw new IllegalArgumentException("Query returned more than one asset");
        }
        return result.get(0);

    }

    protected List<ServerAsset> findAll(EntityManager em, AssetQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSelectString(query, false));
        sb.append(buildFromString(query));
        Pair<String, List<ParameterBinder>> whereClause = buildWhereClause(query);
        sb.append(whereClause.key);
        sb.append(buildOrderByString(query));
        return em.unwrap(Session.class).doReturningWork(new AbstractReturningWork<List<ServerAsset>>() {
            @Override
            public List<ServerAsset> execute(Connection connection) throws SQLException {
                PreparedStatement st = connection.prepareStatement(sb.toString());
                for (ParameterBinder binder : whereClause.value) {
                    binder.accept(st);
                }
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

    protected List<String> findAllIdentifiersOnly(EntityManager em, AssetQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSelectString(query, true));
        sb.append(buildFromString(query));
        Pair<String, List<ParameterBinder>> whereClause = buildWhereClause(query);
        sb.append(whereClause.key);
        sb.append(buildOrderByString(query));
        return em.unwrap(Session.class).doReturningWork(new AbstractReturningWork<List<String>>() {
            @Override
            public List<String> execute(Connection connection) throws SQLException {
                PreparedStatement st = connection.prepareStatement(sb.toString());
                for (ParameterBinder binder : whereClause.value) {
                    binder.accept(st);
                }
                try (ResultSet rs = st.executeQuery()) {
                    List<String> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(rs.getString("ID"));
                    }
                    return result;
                }
            }
        });
    }

    protected String buildSelectString(AssetQuery query, boolean identifiersOnly) {
        StringBuilder sb = new StringBuilder();

        sb.append("select ");

        if (identifiersOnly) {
            sb.append(" A.ID as ID ");
            return sb.toString();
        }

        sb.append("A.ID as ID, A.OBJ_VERSION as OBJ_VERSION, A.CREATED_ON as CREATED_ON, A.NAME as NAME, A.ASSET_TYPE as ASSET_TYPE, ");
        sb.append("A.PARENT_ID as PARENT_ID, ");
        sb.append(query.parentPredicate != null && query.parentPredicate.noParent
            ? " NULL PARENT_NAME, NULL as PARENT_TYPE, "
            : " P.NAME as PARENT_NAME, P.ASSET_TYPE as PARENT_TYPE, "
        );
        sb.append("A.REALM_ID as REALM_ID, R.NAME as TENANT_NAME, RA.VALUE as TENANT_DISPLAY_NAME, ");
        sb.append("A.LOCATION as LOCATION");

        if (query.select != null && query.select.loadComplete) {
            sb.append(", get_asset_tree_path(A.ID) as PATH ");
            sb.append(", A.ATTRIBUTES as ATTRIBUTES");
        } else {
            sb.append(", NULL as PATH, NULL as ATTRIBUTES");
        }

        return sb.toString();
    }

    protected String buildFromString(AssetQuery query) {
        StringBuilder sb = new StringBuilder();

        sb.append(" from ASSET A ");
        sb.append(" join REALM R on R.ID = A.REALM_ID ");
        sb.append(" join REALM_ATTRIBUTE RA on RA.REALM_ID = R.ID and RA.NAME = 'displayName'");

        if (query.parentPredicate != null && !query.parentPredicate.noParent) {
            sb.append(" cross join ASSET P ");
        } else {
            sb.append(" left outer join ASSET P on A.PARENT_ID = P.ID ");
        }

        if (query.id != null) {
            return sb.toString();
        }

        if (query.userId != null) {
            sb.append(" cross join USER_ASSET ua ");
        }

        return sb.toString();
    }

    protected String buildOrderByString(AssetQuery query) {
        StringBuilder sb = new StringBuilder();

        if (query.id != null) {
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
                    sb.append(" A.REALM_ID");
                    break;
            }
            sb.append(query.orderBy.descending ? " desc" : " asc ");
        }

        return sb.toString();
    }

    protected Pair<String, List<ParameterBinder>> buildWhereClause(AssetQuery query) {
        StringBuilder sb = new StringBuilder();
        List<ParameterBinder> binders = new ArrayList<>();

        sb.append(" where true ");

        if (query.id != null) {
            sb.append(" and A.ID = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.id));
        }

        if (query.name != null) {
            sb.append(query.name.caseSensitive ? " and A.NAME " : " and upper(A.NAME) ");
            sb.append(query.name.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.name.prepareValue()));
        }

        if (query.parentPredicate != null) {
            if (query.parentPredicate.id != null) {
                sb.append(" and p.ID = a.PARENT_ID ");
                sb.append(" and A.PARENT_ID = ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.parentPredicate.id));
            } else if (query.parentPredicate.type != null) {
                sb.append(" and p.ID = a.PARENT_ID ");
                sb.append(" and P.ASSET_TYPE = ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.parentPredicate.type));
            } else if (query.parentPredicate.noParent) {
                sb.append(" and A.PARENT_ID is null ");
            }
        }

        if (query.pathPredicate != null && query.pathPredicate.hasPath()) {
            sb.append(" and ? <@ get_asset_tree_path(A.ID)");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setArray(pos, st.getConnection().createArrayOf("text", query.pathPredicate.path)));
        }

        if (query.tenantPredicate != null && query.tenantPredicate.realmId != null) {
            sb.append(" and R.ID = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.tenantPredicate.realmId));
        } else if (query.tenantPredicate != null && query.tenantPredicate.realm != null) {
            sb.append(" and R.NAME = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.tenantPredicate.realm));
        }

        if (query.userId != null) {
            sb.append(" and ua.ASSET_ID = a.ID and ua.USER_ID = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.userId));
        }

        if (query.type != null) {
            sb.append(query.type.caseSensitive ? " and A.ASSET_TYPE" : " and upper(A.ASSET_TYPE) ");
            sb.append(query.type.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.type.prepareValue()));
        }

        if (query.attributeMetaPredicate != null) {
            StringBuilder attributeMetaBuilder = new StringBuilder();

            if (query.attributeMetaPredicate.itemNamePredicate != null) {
                attributeMetaBuilder.append(query.attributeMetaPredicate.itemNamePredicate.caseSensitive
                    ? " and AM.VALUE #>> '{name}' "
                    : " and upper(AM.VALUE #>> '{name}') "
                );
                attributeMetaBuilder.append(query.attributeMetaPredicate.itemNamePredicate.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.attributeMetaPredicate.itemNamePredicate.prepareValue()));
            }
            if (query.attributeMetaPredicate.itemValuePredicate != null) {
                if (query.attributeMetaPredicate.itemValuePredicate instanceof AssetQuery.StringPredicate) {
                    AssetQuery.StringPredicate stringPredicate = (AssetQuery.StringPredicate) query.attributeMetaPredicate.itemValuePredicate;
                    attributeMetaBuilder.append(stringPredicate.caseSensitive
                        ? " and AM.VALUE #>> '{value}' "
                        : " and upper(AM.VALUE #>> '{value}') "
                    );
                    attributeMetaBuilder.append(stringPredicate.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
                } else if (query.attributeMetaPredicate.itemValuePredicate instanceof AssetQuery.BooleanPredicate) {
                    AssetQuery.BooleanPredicate booleanPredicate = (AssetQuery.BooleanPredicate) query.attributeMetaPredicate.itemValuePredicate;
                    attributeMetaBuilder.append(" and AM.VALUE #> '{value}' = to_jsonb(").append(booleanPredicate.predicate).append(") ");
                } else if (query.attributeMetaPredicate.itemValuePredicate instanceof AssetQuery.StringArrayPredicate) {
                    AssetQuery.StringArrayPredicate stringArrayPredicate = (AssetQuery.StringArrayPredicate) query.attributeMetaPredicate.itemValuePredicate;
                    for (int i = 0; i < stringArrayPredicate.predicates.length; i++) {
                        AssetQuery.StringPredicate stringPredicate = stringArrayPredicate.predicates[i];
                        attributeMetaBuilder.append(stringPredicate.caseSensitive
                            ? " and AM.VALUE #> '{value}' ->> " + i
                            : " and upper(AM.VALUE #> '{value}' ->> " + i + ") "
                        );
                        attributeMetaBuilder.append(stringPredicate.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                        final int pos = binders.size() + 1;
                        binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
                    }
                }
            }

            if (attributeMetaBuilder.length() > 0) {
                sb.append(" and A.ID in (select A.ID from ");
                sb.append(" jsonb_each(A.ATTRIBUTES) as AX, ");
                sb.append(" jsonb_array_elements(AX.VALUE #> '{meta}') as AM ");
                sb.append(" where true ");
                sb.append(attributeMetaBuilder);
                sb.append(")");
            }
        }

        return new Pair<>(sb.toString(), binders);
    }

    protected ServerAsset mapResultTuple(AssetQuery query, ResultSet rs) throws SQLException {
        return new ServerAsset(
            query.select != null && query.select.filterProtected,
            rs.getString("ID"), rs.getLong("OBJ_VERSION"), rs.getTimestamp("CREATED_ON"), rs.getString("NAME"), rs.getString("ASSET_TYPE"),
            rs.getString("PARENT_ID"), rs.getString("PARENT_NAME"), rs.getString("PARENT_TYPE"),
            rs.getString("REALM_ID"), rs.getString("TENANT_NAME"), rs.getString("TENANT_DISPLAY_NAME"),
            rs.getObject("LOCATION"), rs.getArray("PATH"), rs.getString("ATTRIBUTES")
        );
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
                    new AssetTreeModifiedEvent(asset.getRealmId(), asset.getId())
                );
                break;
            case UPDATE:

                // Did the name change?
                String previousName = persistenceEvent.getPreviousState("name");
                String currentName = persistenceEvent.getCurrentState("name");
                if (!Objects.equals(previousName, currentName)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(asset.getRealmId(), asset.getId())
                    );
                    break;
                }

                // Did the parent change?
                String previousParentId = persistenceEvent.getPreviousState("parentId");
                String currentParentId = persistenceEvent.getCurrentState("parentId");
                if (!Objects.equals(previousParentId, currentParentId)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(asset.getRealmId(), asset.getId())
                    );
                    break;
                }

                // Did the realm change?
                String previousRealmId = persistenceEvent.getPreviousState("realmId");
                String currentRealmId = persistenceEvent.getCurrentState("realmId");
                if (!Objects.equals(previousRealmId, currentRealmId)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(asset.getRealmId(), asset.getId())
                    );
                    break;
                }

                break;
            case DELETE:
                clientEventService.publishEvent(
                    new AssetTreeModifiedEvent(asset.getRealmId(), asset.getId())
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