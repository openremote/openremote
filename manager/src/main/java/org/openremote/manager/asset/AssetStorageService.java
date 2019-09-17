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
package org.openremote.manager.asset;

import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RRule;
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
import org.openremote.manager.asset.console.ConsoleResourceImpl;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.rules.AssetQueryPredicate;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.calendar.RecurrenceRule;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.filter.*;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;
import org.openremote.model.util.AssetModelUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;
import org.postgresql.util.PGobject;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
import static org.openremote.manager.event.ClientEventService.CLIENT_EVENT_TOPIC;
import static org.openremote.manager.event.ClientEventService.getSessionKey;
import static org.openremote.model.asset.AssetAttribute.*;
import static org.openremote.model.attribute.MetaItemType.ACCESS_RESTRICTED_READ;
import static org.openremote.model.query.AssetQuery.*;
import static org.openremote.model.query.AssetQuery.Access.PRIVATE;
import static org.openremote.model.query.AssetQuery.Access.PROTECTED;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AssetStorageService extends RouteBuilder implements ContainerService {

    protected class PreparedAssetQuery {

        final protected String querySql;
        final protected List<ParameterBinder> binders;

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

    private static final Logger LOG = Logger.getLogger(AssetStorageService.class.getName());
    protected static String META_ITEM_RESTRICTED_READ_SQL_FRAGMENT;
    protected static String META_ITEM_PUBLIC_READ_SQL_FRAGMENT;
    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;
    protected ClientEventService clientEventService;

    protected static boolean calendarEventPredicateMatches(CalendarEventActivePredicate eventActivePredicate, Asset asset) {
        return CalendarEventConfiguration.getCalendarEvent(asset)
            .map(calendarEvent -> calendarEventActiveOn(calendarEvent,
                new Date(1000L * eventActivePredicate.timestampSeconds)))
            .orElse(true);
    }

    protected static boolean calendarEventActiveOn(CalendarEvent calendarEvent, Date when) {
        if (calendarEvent.getRecurrence() == null) {
            return (!when.before(calendarEvent.getStart()) && !when.after(calendarEvent.getEnd()));
        }

        RecurrenceRule recurrenceRule = calendarEvent.getRecurrence();
        Recur recurrence;

        if (recurrenceRule.getCount() != null) {
            recurrence = new Recur(recurrenceRule.getFrequency().name(), recurrenceRule.getCount());
        } else if (recurrenceRule.getUntil() != null) {
            recurrence = new Recur(recurrenceRule.getFrequency().name(),
                new net.fortuna.ical4j.model.Date(recurrenceRule.getUntil()));
        } else {
            recurrence = new Recur(recurrenceRule.getFrequency().name(), null);
        }

        if (recurrenceRule.getInterval() != null) {
            recurrence.setInterval(recurrenceRule.getInterval());
        }

        RRule rRule = new RRule(recurrence);
        VEvent vEvent = new VEvent(new DateTime(calendarEvent.getStart()),
            new DateTime(calendarEvent.getEnd()), "");
        vEvent.getProperties().add(rRule);
        Period period = new Period(new DateTime(when), new Dur(0, 0, 1, 0));
        PeriodRule periodRule = new PeriodRule(period);
        return periodRule.evaluate(vEvent);
    }

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);

        META_ITEM_RESTRICTED_READ_SQL_FRAGMENT =
            " ('" + Arrays.stream(AssetModelUtil.getMetaItemDescriptors()).filter(i -> i.getAccess().restrictedRead).map(MetaItemDescriptor::getUrn).collect(joining("','")) + "')";

        META_ITEM_PUBLIC_READ_SQL_FRAGMENT =
            " ('" + Arrays.stream(AssetModelUtil.getMetaItemDescriptors()).filter(i -> i.getAccess().publicRead).map(MetaItemDescriptor::getUrn).collect(joining("','")) + "')";

        clientEventService.addSubscriptionAuthorizer((auth, subscription) ->
            (subscription.isEventType(AssetTreeModifiedEvent.class))
                && identityService.getIdentityProvider().canSubscribeWith(
                auth,
                subscription.getFilter() instanceof TenantFilter ? ((TenantFilter) subscription.getFilter()) : null,
                ClientRole.READ_ASSETS)
        );

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new AssetModelResourceImpl(
                container.getService(TimerService.class),
                identityService
            )
        );

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new AssetResourceImpl(
                container.getService(TimerService.class),
                identityService,
                this,
                container.getService(MessageBrokerService.class)
            )
        );

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new ConsoleResourceImpl(container.getService(TimerService.class),
                identityService,
                this)
        );

        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        // If any asset was modified in the database, publish events
        from(PERSISTENCE_TOPIC)
            .routeId("AssetPersistenceChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> publishModificationEvents(exchange.getIn().getBody(PersistenceEvent.class)));

        // React if a client wants to read assets and attributes
        from(CLIENT_EVENT_TOPIC)
            .routeId("FromClientReadRequests")
            .filter(exchange ->
                (exchange.getIn().getBody() instanceof ReadAssetAttributesEvent) || (exchange.getIn().getBody() instanceof ReadAssetEvent))
            .process(exchange -> {
                ReadAssetEvent event = exchange.getIn().getBody(ReadAssetEvent.class);
                LOG.fine("Handling from client: " + event);
                boolean isAttributeRead = event instanceof ReadAssetAttributesEvent;

                if (event.getAssetId() == null || event.getAssetId().isEmpty())
                    return;

                String sessionKey = getSessionKey(exchange);
                AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);


                // Superuser can get all, User must have role
                if (!authContext.isSuperUser() && !authContext.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                    return;
                }

                Access access = authContext.isSuperUser() || !identityService.getIdentityProvider().isRestrictedUser(authContext.getUserId()) ? PRIVATE : PROTECTED;

                Asset asset = find(
                    new AssetQuery()
                        .select(Select.selectAll())
                        .ids(event.getAssetId())
                        .access(access));

                if (asset != null) {
                    if (isAttributeRead) {
                        replyWithAttributeEvents(sessionKey, event.getSubscriptionId(), asset, ((ReadAssetAttributesEvent) event).getAttributeNames());
                    } else {
                        replyWithAssetEvent(sessionKey, event.getSubscriptionId(), asset);
                    }
                }
            });
    }

    public Asset find(String assetId) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().ids(assetId));
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     */
    public Asset find(String assetId, boolean loadComplete) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().select(loadComplete ? Select.selectAll() : Select.selectExcludePathAndAttributes()).ids(assetId));
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     */
    public Asset find(EntityManager em, String assetId, boolean loadComplete) {
        return find(em, assetId, loadComplete, PRIVATE);
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @param access       The required access permissions of the asset data.
     */
    public Asset find(String assetId, boolean loadComplete, Access access) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery()
            .select(loadComplete
                ? Select.selectAll()
                : Select.selectExcludePathAndAttributes())
            .ids(assetId)
            .access(access));
    }

    public Asset find(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> find(em, query));
    }

    public Asset find(EntityManager em, AssetQuery query) {
        List<Asset> result = findAll(em, query);
        if (result.size() == 0)
            return null;
        if (result.size() > 1) {
            throw new IllegalArgumentException("Query returned more than one asset");
        }
        return result.get(0);

    }

    public List<Asset> findAll(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> findAll(em, query));
    }

    public List<String> findNames(String... ids) {
        if (ids == null || ids.length == 0)
            return new ArrayList<>();

        // TODO: Do this in a loop in reasonably sized batches
        return persistenceService.doReturningTransaction(em -> {
            List<Object[]> result = em.createQuery("select a.id, a.name from Asset a where a.id in :ids",
                Object[].class)
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
    public Asset merge(Asset asset) {
        return merge(asset, false);
    }

    /**
     * @param overrideVersion If <code>true</code>, the merge will override the data in the database, independent of
     *                        version.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public Asset merge(Asset asset, boolean overrideVersion) {
        return merge(asset, overrideVersion, null);
    }

    /**
     * @param userName the user which this asset needs to be assigned to.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public Asset merge(Asset asset, String userName) {
        return merge(asset, false, userName);
    }

    /**
     * @param overrideVersion If <code>true</code>, the merge will override the data in the database, independent of
     *                        version.
     * @param userName        the user which this asset needs to be assigned to.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public Asset merge(Asset asset, boolean overrideVersion, String userName) {
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
                Asset parent = find(em, asset.getParentId(), true);
                // .. the parent must exist
                if (parent == null)
                    throw new IllegalStateException("Parent not found: " + asset.getParentId());
                // ... the parent can not be a child of the asset
                if (parent.pathContains(asset.getId()))
                    throw new IllegalStateException("Invalid parent");

                // .. the parent should be in the same realm
                if (asset.getRealm() != null && !parent.getRealm().equals(asset.getRealm())) {
                    throw new IllegalStateException("Parent not in same realm as asset: " + asset.getRealm());
                } else if (asset.getRealm() == null) {
                    // ... and if we don't have a realm identifier, use the parent's
                    asset.setRealm(parent.getRealm());
                }
            }

            // Validate realm
            if (!identityService.getIdentityProvider().tenantExists(asset.getRealm())) {
                throw new IllegalStateException("Realm not found/active: " + asset.getRealm());
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
                Asset existing = em.find(Asset.class, asset.getId());
                if (existing != null) {
                    asset.setVersion(existing.getVersion());
                }
            }

            // If username present
            User user = null;
            if (!TextUtil.isNullOrEmpty(userName)) {
                user = identityService.getIdentityProvider().getUser(asset.getRealm(), userName);
                if (user == null) {
                    throw new IllegalStateException("User not found: " + userName);
                }
            }

            LOG.fine("Storing: " + asset);

            Asset updatedAsset = em.merge(asset);

            if (user != null) {
                storeUserAsset(em, new UserAsset(user.getRealm(), user.getId(), updatedAsset.getId()));
            }

            return updatedAsset;
        });
    }

    /**
     * @return <code>true</code> if the assets were deleted, false if any of the assets still have children and can't be deleted.
     */
    public boolean delete(List<String> assetIds) {
        try {
            persistenceService.doTransaction(em -> {
                LOG.fine("Removing: " + String.join(", ", assetIds));
                List<Asset> assets = em
                    .createQuery("select a from Asset a where not exists(select child.id from Asset child where child.parentId = a.id) and a.id in :ids", Asset.class)
                    .setParameter("ids", assetIds)
                    .getResultList();

                if (assetIds.size() != assets.size()) {
                    throw new IllegalArgumentException("Cannot delete one or more requested assets as they either have children or don't exist");
                }

                assets.forEach(em::remove);
            });
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean isUserAsset(String assetId) {
        return isUserAsset((String) null, assetId);
    }

    public boolean isUserAsset(String userId, String assetId) {
        if (TextUtil.isNullOrEmpty(userId) || TextUtil.isNullOrEmpty(assetId)) {
            return false;
        }
        return persistenceService.doReturningTransaction(entityManager -> {
            try {
                String queryStr = TextUtil.isNullOrEmpty(userId) ?
                    "select count(ua) from UserAsset ua where ua.id.assetId = :assetId" :
                    "select count(ua) from UserAsset ua where ua.id.userId = :userId and ua.id.assetId = :assetId";

                TypedQuery<Long> query = entityManager.createQuery(
                    queryStr,
                    Long.class).setParameter("assetId", assetId);

                if (!TextUtil.isNullOrEmpty(userId)) {
                    query.setParameter("userId", userId);
                }

                return query.getSingleResult() > 0;
            } catch (NoResultException ex) {
                return false;
            }
        });
    }

    public boolean isUserAsset(List<String> userIds, String assetId) {
        if (userIds == null || userIds.isEmpty() || TextUtil.isNullOrEmpty(assetId)) {
            return false;
        }
        return persistenceService.doReturningTransaction(entityManager -> {
            try {
                return entityManager.createQuery(
                    "select count(ua) from UserAsset ua where ua.id.userId in :userIds and ua.id.assetId = :assetId",
                    Long.class)
                    .setParameter("userIds", userIds)
                    .setParameter("assetId", assetId)
                    .getSingleResult() > 0;
            } catch (NoResultException ex) {
                return false;
            }
        });
    }

    public boolean isUserAssets(String userId, List<String> assetIds) {
        if (TextUtil.isNullOrEmpty(userId) || assetIds == null || assetIds.isEmpty()) {
            return false;
        }
        return persistenceService.doReturningTransaction(entityManager -> {
            try {
                return entityManager.createQuery(
                    "select count(ua) from UserAsset ua where ua.id.userId = :userId and ua.id.assetId in :assetIds",
                    Long.class)
                    .setParameter("userId", userId)
                    .setParameter("assetIds", assetIds)
                    .getSingleResult() == assetIds.size();
            } catch (NoResultException ex) {
                return false;
            }
        });
    }

    /**
     * Indicates if the specified asset belongs to the specified realm
     */
    public boolean isRealmAsset(String realm, String assetId) {
        return isRealmAssets(realm, Collections.singletonList(assetId));
    }

    /**
     * Indicates if the specified assets belong to the specified realm
     */
    public boolean isRealmAssets(String realm, List<String> assetIds) {
        if (TextUtil.isNullOrEmpty(realm) || assetIds == null || assetIds.isEmpty()) {
            return false;
        }
        return persistenceService.doReturningTransaction(entityManager -> {
            try {
                return entityManager.createQuery(
                    "select count(a) from Asset a where a.realm = :realm and a.id in :assetIds",
                    Long.class)
                    .setParameter("realm", realm)
                    .setParameter("assetIds", assetIds)
                    .getSingleResult() == assetIds.size();
            } catch (NoResultException ex) {
                return false;
            }
        });
    }

    public boolean isDescendantAsset(String parentAssetId, String assetId) {
        return isDescendantAssets(parentAssetId, Collections.singletonList(assetId));
    }

    public boolean isDescendantAssets(String parentAssetId, List<String> assetIds) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) throws SQLException {
                try (PreparedStatement st = connection.prepareStatement("select count(*) from Asset a where ? = ANY(get_asset_tree_path(a.ID)) AND a.id = ANY(?)")) {
                    st.setString(1, parentAssetId);
                    st.setArray(2, st.getConnection().createArrayOf("text", assetIds.toArray()));
                    ResultSet rs = st.executeQuery();
                    return rs.next() && rs.getInt(1) == assetIds.size();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, "Failed to execute isDescendantAssets query", ex);
                    return false;
                }
            }
        }));
    }

    public List<UserAsset> findUserAssets(String realm, String userId, String assetId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            StringBuilder sb = new StringBuilder();
            Map<String, Object> parameters = new HashMap<>(3);
            sb.append("select ua from UserAsset ua where 1=1");

            if (!isNullOrEmpty(realm)) {
                sb.append(" and ua.id.realm = :realm");
                parameters.put("realm", realm);
            }
            if (!isNullOrEmpty(userId)) {
                sb.append(" and ua.id.userId = :userId");
                parameters.put("userId", userId);
            }
            if (!isNullOrEmpty(assetId)) {
                sb.append(" and ua.id.assetId = :assetId");
                parameters.put("assetId", assetId);
            }

            sb.append(" order by ua.createdOn desc");

            TypedQuery<UserAsset> query = entityManager.createQuery(sb.toString(), UserAsset.class);
            parameters.forEach(query::setParameter);
            return query.getResultList();
        });
    }

    /* ####################################################################################### */

    /**
     * This used to automatically make the user restricted as well but this has been disabled as it no longer fitted
     * with use cases.
     */
    public void storeUserAsset(UserAsset userAsset) {
        persistenceService.doTransaction(entityManager -> storeUserAsset(entityManager, userAsset));
    }

    /**
     * This used to automatically unrestrict a user  if no assets are linked to the them anymore but this has been
     * disabled as it no longer fitted with use cases.
     */
    public void deleteUserAsset(String realm, String userId, String assetId) {
        persistenceService.doTransaction(entityManager -> {
            UserAsset userAsset = entityManager.find(UserAsset.class, new UserAsset.Id(realm, userId, assetId));
            if (userAsset != null)
                entityManager.remove(userAsset);
        });
    }

    protected void storeUserAsset(EntityManager entityManager, UserAsset userAsset) {
        userAsset.setCreatedOn(new Date(timerService.getCurrentTimeMillis()));
        entityManager.merge(userAsset);
    }

    protected Asset find(EntityManager em, String assetId, boolean loadComplete, Access access) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(
            em,
            new AssetQuery()
                .select(loadComplete
                    ? Select.selectAll()
                    : Select.selectExcludePathAndAttributes())
                .ids(assetId)
                .access(access)
        );
    }

    protected List<Asset> findAll(EntityManager em, AssetQuery query) {

        // Use a default projection if it's missing
        if (query.select == null)
            query.select = new Select();
        if (query.access == null)
            query.access = PRIVATE;

        // Default to order by creation date if the query may return multiple results
        if (query.orderBy == null && query.ids == null)
            query.orderBy = new OrderBy(OrderBy.Property.CREATED_ON);

        PreparedAssetQuery querySql = buildQuery(query);

        return em.unwrap(Session.class).doReturningWork(new AbstractReturningWork<List<Asset>>() {
            @Override
            public List<Asset> execute(Connection connection) throws SQLException {
                LOG.fine("Executing: " + querySql.querySql);
                try (PreparedStatement st = connection.prepareStatement(querySql.querySql)) {
                    querySql.apply(st);

                    try (ResultSet rs = st.executeQuery()) {
                        List<Asset> result = new ArrayList<>();
                        if (query.calendarEventActive != null) {
                            while (rs.next()) {
                                Asset asset = mapResultTuple(query, rs);
                                if (calendarEventPredicateMatches(query.calendarEventActive, asset)) {
                                    result.add(asset);
                                }
                            }
                        } else {
                            while (rs.next()) {
                                result.add(mapResultTuple(query, rs));
                            }
                        }
                        return result;
                    }
                }
            }
        });
    }

    protected PreparedAssetQuery buildQuery(AssetQuery query) {
        LOG.fine("Building: " + query);
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.recursive;
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
        sb.append(buildLimitString(query));
        return new PreparedAssetQuery(sb.toString(), binders);
    }

    protected String buildSelectString(AssetQuery query, int level, List<ParameterBinder> binders) {
        // level = 1 is main query select
        // level = 2 is union select
        // level = 3 is CTE select
        StringBuilder sb = new StringBuilder();
        AssetQuery.Select select = query.select;

        sb.append("select A.ID as ID, A.NAME as NAME, A.ACCESS_PUBLIC_READ as ACCESS_PUBLIC_READ");
        sb.append(", A.CREATED_ON AS CREATED_ON, A.ASSET_TYPE AS ASSET_TYPE, A.PARENT_ID AS PARENT_ID");
        sb.append(", A.REALM AS REALM, A.OBJ_VERSION as OBJ_VERSION");

        if (!select.excludeParentInfo) {
            sb.append(", P.NAME as PARENT_NAME, P.ASSET_TYPE as PARENT_TYPE");
        }

        if (!select.excludeRealm) {
            if (!query.recursive || level == 3) {
                sb.append(", R.NAME as TENANT_NAME");
            }
        }

        if (!query.recursive || level == 3) {
            if (!select.excludePath) {
                sb.append(", get_asset_tree_path(A.ID) as PATH");
            } else {
                sb.append(", NULL as PATH");
            }
        }

        if (!select.excludeAttributes) {
            if (query.recursive && level != 3) {
                sb.append(", A.ATTRIBUTES as ATTRIBUTES");
            } else {
                sb.append(buildAttributeSelect(query, binders));
            }
        } else {
            sb.append(", NULL as ATTRIBUTES");
        }

        return sb.toString();
    }

    protected String buildAttributeSelect(AssetQuery query, List<ParameterBinder> binders) {

        Select select = query.select;
        boolean hasMetaFilter = !select.excludeAttributeMeta && select.meta != null && select.meta.length > 0;
        boolean fullyPopulateAttributes = !(select.excludeAttributeMeta || select.excludeAttributeValue || select.excludeAttributeTimestamp || select.meta != null);

        if (select.attributes == null && query.access == PRIVATE && fullyPopulateAttributes) {
            return ", A.ATTRIBUTES as ATTRIBUTES";
        }

        StringBuilder attributeBuilder = new StringBuilder();

        if (select.excludeAttributeMeta) {
            attributeBuilder.append(" - 'meta'");
        }
        if (select.excludeAttributeTimestamp) {
            attributeBuilder.append(" - 'valueTimestamp'");
        }
        if (select.excludeAttributeValue) {
            attributeBuilder.append(" - 'value'");
        }
        if (select.excludeAttributeType) {
            attributeBuilder.append(" - 'type'");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(", (");

        if (select.excludeAttributeMeta) {
            sb.append("select json_object_agg(AX.key, AX.value");
            sb.append(attributeBuilder);
            sb.append(") from jsonb_each(A.attributes) as AX");
            if (query.access != PRIVATE) {
                // Use implicit inner join on meta array set to only select non-private attributes
                sb.append(", jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
            }
        } else if (query.access != PRIVATE || hasMetaFilter) {
            // Use sub-select for processing the attributes the meta inside each attribute is replaced with filtered meta
            // (coalesce null to empty array because jsonb_set() with null will clear the whole object)
            sb.append(
                "select json_object_agg(AX.key, jsonb_set(AX.value, '{meta}', coalesce(AMF.VALUE, jsonb_build_array()), false)) from jsonb_each(A.attributes) as AX");
            // Use implicit inner join on meta array set to only select attributes with a non-private access meta item
            sb.append(", jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
            // Use subquery to filter out meta items not marked as non-private access
            sb.append(" INNER JOIN LATERAL (");
            sb.append("select jsonb_agg(AM.value) AS VALUE from jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
            sb.append(" where AM.VALUE #>> '{name}' IN");

            if (query.access != PRIVATE) {
                sb.append(query.access == PROTECTED ? META_ITEM_RESTRICTED_READ_SQL_FRAGMENT : META_ITEM_PUBLIC_READ_SQL_FRAGMENT);
                if (hasMetaFilter) {
                    sb.append(" AND  AM.VALUE #>> '{name}' IN");
                }
            }

            if (hasMetaFilter) {
                sb.append(" ('");
                sb.append(String.join("','", select.meta));
                sb.append("')");
            }

            sb.append(") as AMF ON true");
        } else {
            sb.append("select json_object_agg(AX.key, AX.value) from jsonb_each(A.attributes) as AX");
        }

        sb.append(" where true");

        // Filter attributes
        if (select.attributes != null && select.attributes.length > 0) {
            sb.append(" AND AX.key IN (");
            for (int i = 0; i < select.attributes.length; i++) {
                sb.append(i == select.attributes.length - 1 ? "?" : "?,");
                final String attributeName = select.attributes[i];
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, attributeName));
            }
            sb.append(")");
        }

        if (query.access != PRIVATE) {
            // Filter non-private access attributes
            AttributeMetaPredicate accessPredicate =
                new AttributeMetaPredicate()
                    .itemName(query.access == PROTECTED ? ACCESS_RESTRICTED_READ : MetaItemType.ACCESS_PUBLIC_READ)
                    .itemValue(new BooleanPredicate(true));
            sb.append(buildAttributeMetaFilter(binders, accessPredicate));
        }

        sb.append(") AS ATTRIBUTES");
        return sb.toString();
    }

    protected String buildFromString(AssetQuery query, int level) {
        // level = 1 is main query
        // level = 2 is union
        // level = 3 is CTE
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.recursive;
        boolean includeRealmInfo = !query.select.excludeRealm;

        if (level == 1) {
            sb.append(" from ASSET A ");
        } else if (level == 2) {
            sb.append(" from top_level_assets P ");
            sb.append("join ASSET A on A.PARENT_ID = P.ID ");
        } else {
            sb.append(" from top_level_assets A ");
        }

        if ((!recursive || level == 3) && (includeRealmInfo || query.tenant != null)) {
            sb.append("join PUBLIC.REALM R on R.NAME = A.REALM ");
        }

        if ((!recursive || level == 3) && query.ids == null && query.userIds != null && query.userIds.length > 0) {
            sb.append("cross join USER_ASSET ua ");
        }

        if (level == 1) {
            if (hasParentConstraint(query.parents)) {
                sb.append("cross join ASSET P ");
            } else {
                sb.append("left outer join ASSET P on A.PARENT_ID = P.ID ");
            }
        }

        return sb.toString();
    }

    protected boolean hasParentConstraint(ParentPredicate[] parentPredicates) {
        if (parentPredicates == null || parentPredicates.length == 0) {
            return false;
        }

        return Arrays.stream(parentPredicates)
            .anyMatch(p -> !p.noParent && (p.id != null || p.type != null || p.name != null));
    }


    protected String buildOrderByString(AssetQuery query) {
        StringBuilder sb = new StringBuilder();

        if (query.ids != null && !query.recursive) {
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
                case REALM:
                    sb.append(" A.REALM ");
                    break;
            }
            sb.append(query.orderBy.descending ? "desc " : "asc ");
        }

        return sb.toString();
    }

    protected String buildLimitString(AssetQuery query) {
        if (query.limit > 0) {
            return " LIMIT " + query.limit;
        }
        return "";
    }

    protected String buildWhereClause(AssetQuery query, int level, List<ParameterBinder> binders) {
        // level = 1 is main query
        // level = 2 is union
        // level = 3 is CTE
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.recursive;
        sb.append(" where true");

        if (level == 2) {
            return sb.toString();
        }

        if (level == 1 && query.ids != null && query.ids.length > 0) {
            sb.append(" and A.ID IN (?");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.ids[0]));

            for (int i = 1; i < query.ids.length; i++) {
                sb.append(",?");
                final int pos2 = binders.size() + 1;
                final int index = i;
                binders.add(st -> st.setString(pos2, query.ids[index]));
            }
            sb.append(")");
        }

        if (level == 1 && query.names != null && query.names.length > 0) {

            sb.append(" and (");
            boolean isFirst = true;

            for (StringPredicate pred : query.names) {
                if (!isFirst) {
                    sb.append(" or ");
                }
                isFirst = false;
                sb.append(pred.caseSensitive ? "A.NAME " : "upper(A.NAME)");
                sb.append(buildMatchFilter(pred));
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, pred.prepareValue()));
            }
            sb.append(")");
        }

        if (query.parents != null && query.parents.length > 0) {

            sb.append(" and (");
            boolean isFirst = true;

            for (ParentPredicate pred : query.parents) {
                if (!isFirst) {
                    sb.append(" or (");
                } else {
                    sb.append("(");
                }
                isFirst = false;

                if (level == 1 && pred.id != null) {
                    sb.append("p.ID = a.PARENT_ID");
                    sb.append(" and A.PARENT_ID = ?");
                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, pred.id));
                } else if (level == 1 && pred.noParent) {
                    sb.append("A.PARENT_ID is null");
                } else if (pred.type != null || pred.name != null) {

                    sb.append("p.ID = a.PARENT_ID");

                    if (pred.type != null) {
                        sb.append(" and P.ASSET_TYPE = ?");
                        final int pos = binders.size() + 1;
                        binders.add(st -> st.setString(pos, pred.type));
                    }
                    if (pred.name != null) {
                        sb.append(" and P.NAME = ?");
                        final int pos = binders.size() + 1;
                        binders.add(st -> st.setString(pos, pred.name));
                    }
                }

                sb.append(")");
            }

            sb.append(")");
        }

        if (level == 1 && hasPathConstraint(query.paths)) {
            sb.append(" and (");
            boolean isFirst = true;

            for (PathPredicate pred : query.paths) {
                if (!isFirst) {
                    sb.append(" or ");
                }
                isFirst = false;

                sb.append("? <@ get_asset_tree_path(A.ID)");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setArray(pos, st.getConnection().createArrayOf("text", pred.path)));
            }

            sb.append(")");
        }

        if (!recursive || level == 3) {
            if (query.tenant != null && !TextUtil.isNullOrEmpty(query.tenant.realm)) {
                sb.append(" and R.NAME = ?");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.tenant.realm));
            }

            if (query.ids == null && query.userIds != null && query.userIds.length > 0) {
                sb.append(" and ua.ASSET_ID = a.ID and ua.USER_ID IN (?");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.userIds[0]));

                for (int i = 1; i < query.userIds.length; i++) {
                    sb.append(",?");
                    final int pos2 = binders.size() + 1;
                    final int index = i;
                    binders.add(st -> st.setString(pos2, query.userIds[index]));
                }
                sb.append(")");
            }

            if (level == 1 && query.access == Access.PUBLIC) {
                sb.append(" and A.ACCESS_PUBLIC_READ is true");
            }

            if (query.types != null && query.types.length > 0) {
                sb.append(" and (");
                boolean isFirst = true;

                for (StringPredicate pred : query.types) {
                    if (!isFirst) {
                        sb.append(" or (");
                    } else {
                        sb.append("(");
                    }
                    isFirst = false;

                    sb.append(pred.caseSensitive ? "A.ASSET_TYPE" : " and upper(A.ASSET_TYPE)");
                    sb.append(buildMatchFilter(pred));
                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, pred.prepareValue()));
                    sb.append(")");
                }

                sb.append(")");
            }

            if (query.attributeMeta != null) {
                for (AttributeMetaPredicate attributeMetaPredicate : query.attributeMeta) {
                    String attributeMetaFilter = buildAttributeMetaFilter(binders, attributeMetaPredicate);

                    if (attributeMetaFilter.length() > 0) {
                        sb.append(" and A.ID in (select A.ID from");
                        sb.append(" jsonb_each(A.ATTRIBUTES) as AX,");
                        sb.append(" jsonb_array_elements(AX.VALUE #> '{meta}') as AM");
                        sb.append(" where true");
                        sb.append(attributeMetaFilter);
                        sb.append(")");
                    }
                }
            }

            if (query.attributes != null) {
                AtomicInteger joinCounter = new AtomicInteger(1);
                sb.append(" and A.ID in (select A.ID from");
                sb.append(" jsonb_each(A.ATTRIBUTES) as AX1");
                int offset = sb.length();
                sb.append(" where true AND ");
                addAttributePredicateGroupQuery(sb, binders, joinCounter, query.attributes);
                sb.append(")");

                int counter = joinCounter.get();

                while (counter > 2) {
                    sb.insert(offset, ", jsonb_each(A.ATTRIBUTES) as AX" + (counter-1));
                    counter--;
                }
            }
        }
        return sb.toString();
    }

    protected void addAttributePredicateGroupQuery(StringBuilder sb, List<ParameterBinder> binders, AtomicInteger joinCounter, LogicGroup<AttributePredicate> attributePredicateGroup) {

        LogicGroup.Operator operator = attributePredicateGroup.operator;

        if (operator == null) {
            operator = LogicGroup.Operator.AND;
        }

        sb.append("(");

        if (!attributePredicateGroup.getItems().isEmpty()) {

            boolean isFirst = true;
            Collection<List<AttributePredicate>> grouped;

            if (operator == LogicGroup.Operator.AND) {
                // Group predicates by their attribute name predicate
                grouped = attributePredicateGroup.getItems().stream().collect(groupingBy(predicate -> predicate.name)).values();
            } else {
                grouped = new ArrayList<>();
                grouped.add(attributePredicateGroup.getItems());
            }

            for (List<AttributePredicate> group : grouped) {
                for (AttributePredicate attributePredicate : group) {
                    if (!isFirst) {
                        sb.append(operator == LogicGroup.Operator.OR ? " or " : " and ");
                    }
                    isFirst = false;

                    sb.append("(");
                    sb.append(buildAttributeFilter(attributePredicate, joinCounter.get(), binders));
                    sb.append(")");
                }
                joinCounter.incrementAndGet();
            }
        }

        if (attributePredicateGroup.groups != null && attributePredicateGroup.groups.size() > 0) {
            for (LogicGroup<AttributePredicate> group : attributePredicateGroup.groups) {
                sb.append(operator == LogicGroup.Operator.OR ? " or " : " and ");
                addAttributePredicateGroupQuery(sb, binders, joinCounter, group);
            }
        }
        sb.append(")");
    }

    protected boolean hasPathConstraint(PathPredicate[] pathPredicates) {
        if (pathPredicates == null || pathPredicates.length == 0) {
            return false;
        }

        return Arrays.stream(pathPredicates).anyMatch(p -> p.path != null);
    }

    protected String buildAttributeMetaFilter(List<ParameterBinder> binders, AttributeMetaPredicate...attributeMetaPredicates) {
        StringBuilder sb = new StringBuilder();

        if (attributeMetaPredicates == null || attributeMetaPredicates.length == 0) {
            return "";
        }

        sb.append(" AND (");

        boolean isFirst = true;
        for (AttributeMetaPredicate attributeMetaPredicate : attributeMetaPredicates) {

            if (!isFirst) {
                sb.append(" OR (true");
            } else {
                sb.append("(true");
            }
            isFirst = false;

            if (attributeMetaPredicate.itemNamePredicate != null) {
                sb.append(attributeMetaPredicate.itemNamePredicate.caseSensitive
                    ? " and AM.VALUE #>> '{name}'"
                    : " and upper(AM.VALUE #>> '{name}')"
                );
                sb.append(buildMatchFilter(attributeMetaPredicate.itemNamePredicate));
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, attributeMetaPredicate.itemNamePredicate.prepareValue()));
            }

            if (attributeMetaPredicate.itemValuePredicate != null) {
                if (attributeMetaPredicate.itemValuePredicate instanceof StringPredicate) {
                    StringPredicate stringPredicate = (StringPredicate) attributeMetaPredicate.itemValuePredicate;
                    sb.append(stringPredicate.caseSensitive
                        ? " and AM.VALUE #>> '{value}'"
                        : " and upper(AM.VALUE #>> '{value}')"
                    );
                    sb.append(buildMatchFilter(stringPredicate));

                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
                } else if (attributeMetaPredicate.itemValuePredicate instanceof BooleanPredicate) {
                    BooleanPredicate booleanPredicate = (BooleanPredicate) attributeMetaPredicate.itemValuePredicate;
                    sb.append(" and AM.VALUE #> '{value}' = to_jsonb(")
                        .append(booleanPredicate.value)
                        .append(")");
                } else if (attributeMetaPredicate.itemValuePredicate instanceof StringArrayPredicate) {
                    StringArrayPredicate stringArrayPredicate = (StringArrayPredicate) attributeMetaPredicate.itemValuePredicate;
                    for (int i = 0; i < stringArrayPredicate.predicates.length; i++) {
                        StringPredicate stringPredicate = stringArrayPredicate.predicates[i];
                        sb.append(stringPredicate.caseSensitive
                            ? " and AM.VALUE #> '{value}' ->> " + i
                            : " and upper(AM.VALUE #> '{value}' ->> " + i + ")"
                        );
                        sb.append(buildMatchFilter(stringPredicate));
                        final int pos = binders.size() + 1;
                        binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
                    }
                }
            }
            sb.append(")");
        }
        sb.append(")");
        return sb.toString();
    }

    protected String buildAttributeFilter(AttributePredicate attributePredicate, int joinCounter, List<ParameterBinder> binders) {
        StringBuilder attributeBuilder = new StringBuilder();

        if (attributePredicate.name != null) {
            attributeBuilder.append(attributePredicate.name.caseSensitive
                ? "AX" + joinCounter + ".key"
                : "upper(AX" + joinCounter + ".key)"
            );
            attributeBuilder.append(buildMatchFilter(attributePredicate.name));

            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, attributePredicate.name.prepareValue()));
        }
        if (attributePredicate.value != null) {

            if (attributePredicate.name != null) {
                attributeBuilder.append(" and ");
            }

            if (attributePredicate.value instanceof StringPredicate) {
                StringPredicate stringPredicate = (StringPredicate) attributePredicate.value;
                attributeBuilder.append(stringPredicate.caseSensitive
                    ? "AX" + joinCounter + ".VALUE #>> '{value}'"
                    : "upper(AX" + joinCounter + ".VALUE #>> '{value}')"
                );
                attributeBuilder.append(buildMatchFilter(stringPredicate));
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
            } else if (attributePredicate.value instanceof BooleanPredicate) {
                BooleanPredicate booleanPredicate = (BooleanPredicate) attributePredicate.value;
                attributeBuilder.append("AX")
                    .append(joinCounter)
                    .append(".VALUE #> '{value}' = to_jsonb(")
                    .append(booleanPredicate.value)
                    .append(")");
            } else if (attributePredicate.value instanceof StringArrayPredicate) {
                StringArrayPredicate stringArrayPredicate = (StringArrayPredicate) attributePredicate.value;
                for (int i = 0; i < stringArrayPredicate.predicates.length; i++) {
                    StringPredicate stringPredicate = stringArrayPredicate.predicates[i];
                    attributeBuilder.append(stringPredicate.caseSensitive
                        ? "AX" + joinCounter + ".VALUE #> '{value}' ->> " + i
                        : "upper(AX" + joinCounter + ".VALUE #> '{value}' ->> " + i + ")"
                    );
                    attributeBuilder.append(buildMatchFilter(stringPredicate));
                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, stringPredicate.prepareValue()));
                }
            } else if (attributePredicate.value instanceof DateTimePredicate) {
                DateTimePredicate dateTimePredicate = (DateTimePredicate) attributePredicate.value;
                attributeBuilder.append("(AX")
                    .append(joinCounter)
                    .append(".Value #>> '{value}')::timestamp");

                Pair<Long, Long> fromAndTo = AssetQueryPredicate.asFromAndTo(timerService.getCurrentTimeMillis(), dateTimePredicate);

                final int pos = binders.size() + 1;
                binders.add(st -> st.setObject(pos, new java.sql.Timestamp(fromAndTo.key).toLocalDateTime()));
                attributeBuilder.append(buildOperatorFilter(dateTimePredicate.operator, dateTimePredicate.negate));

                if (dateTimePredicate.operator == Operator.BETWEEN) {
                    final int pos2 = binders.size() + 1;
                    binders.add(st -> st.setObject(pos2, new java.sql.Timestamp(fromAndTo.value).toLocalDateTime()));
                }
            } else if (attributePredicate.value instanceof NumberPredicate) {
                NumberPredicate numberPredicate = (NumberPredicate) attributePredicate.value;
                attributeBuilder.append("(AX")
                    .append(joinCounter)
                    .append(".VALUE #>> '{value}')::numeric");
                attributeBuilder.append(buildOperatorFilter(numberPredicate.operator, numberPredicate.negate));

                final int pos = binders.size() + 1;
                switch (numberPredicate.numberType) {
                    case DOUBLE:
                    default:
                        binders.add(st -> st.setDouble(pos, numberPredicate.value));
                        if (numberPredicate.operator == Operator.BETWEEN) {
                            final int pos2 = binders.size() + 1;
                            binders.add(st -> st.setDouble(pos2, numberPredicate.rangeValue));
                        }
                        break;
                    case INTEGER:
                        binders.add(st -> st.setInt(pos, (int) numberPredicate.value));
                        if (numberPredicate.operator == Operator.BETWEEN) {
                            final int pos2 = binders.size() + 1;
                            binders.add(st -> st.setInt(pos2, (int) numberPredicate.rangeValue));
                        }
                        break;
                }
            } else if (attributePredicate.value instanceof ObjectValueKeyPredicate) {
                ObjectValueKeyPredicate keyPredicate = (ObjectValueKeyPredicate) attributePredicate.value;
                if (keyPredicate.negated) {
                    attributeBuilder.append("NOT(AX").append(joinCounter).append(".VALUE #> '{value}' ?? ? ) ");
                } else {
                    attributeBuilder.append("AX").append(joinCounter).append(".VALUE #> '{value}' ?? ? ");
                }
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, keyPredicate.key));
            } else if (attributePredicate.value instanceof ArrayPredicate) {
                ArrayPredicate arrayPredicate = (ArrayPredicate) attributePredicate.value;
                attributeBuilder.append("true");
                if (arrayPredicate.negated) {
                    attributeBuilder.append(" and NOT(");
                }
                if (arrayPredicate.value != null) {
                    if (arrayPredicate.index != null) {
                        attributeBuilder.append("AX")
                            .append(joinCounter)
                            .append(".VALUE -> ")
                            .append(arrayPredicate.index);
                    } else {
                        attributeBuilder.append("AX").append(joinCounter).append(".VALUE");
                    }
                    attributeBuilder.append(" @> ?");
                    final int pos = binders.size() + 1;
                    PGobject pgJsonValue = new PGobject();
                    pgJsonValue.setType("jsonb");
                    try {
                        pgJsonValue.setValue(arrayPredicate.value.toJson());
                    } catch (SQLException e) {
                        LOG.log(Level.SEVERE, "Failed to build SQL statement for array predicate", e);
                        return "";
                    }
                    binders.add(st -> st.setObject(pos, pgJsonValue));
                }
                if (arrayPredicate.lengthEquals != null) {
                    attributeBuilder.append("json_array_length(AX")
                        .append(joinCounter)
                        .append(".VALUE) = ")
                        .append(arrayPredicate.lengthEquals);
                }
                if (arrayPredicate.lengthGreaterThan != null) {
                    attributeBuilder.append("json_array_length(AX")
                        .append(joinCounter)
                        .append(".VALUE) > ")
                        .append(arrayPredicate.lengthGreaterThan);
                }
                if (arrayPredicate.lengthLessThan != null) {
                    attributeBuilder.append("json_array_length(AX")
                        .append(joinCounter)
                        .append(".VALUE) < ")
                        .append(arrayPredicate.lengthLessThan);
                }
                if (arrayPredicate.negated) {
                    attributeBuilder.append(")");
                }
            } else if (attributePredicate.value instanceof GeofencePredicate) {
                if (attributePredicate.value instanceof RadialGeofencePredicate) {
                    RadialGeofencePredicate location = (RadialGeofencePredicate) attributePredicate.value;
                    attributeBuilder.append("ST_Distance_Sphere(ST_MakePoint(")
                        .append("(AX")
                        .append(joinCounter)
                        .append(".VALUE #>> '{value,coordinates,0}')::numeric")
                        .append(", (AX")
                        .append(joinCounter)
                        .append(".VALUE #>> '{value,coordinates,1}')::numeric")
                        .append("), ST_MakePoint(")
                        .append(location.lng)
                        .append(",")
                        .append(location.lat)
                        .append(location.negated ? ")) > " : ")) <= ")
                        .append(location.radius);
                } else if (attributePredicate.value instanceof RectangularGeofencePredicate) {
                    RectangularGeofencePredicate location = (RectangularGeofencePredicate) attributePredicate.value;
                    if (location.negated) {
                        attributeBuilder.append("NOT");
                    }
                    attributeBuilder.append(" ST_Within(ST_MakePoint(")
                        .append("(AX")
                        .append(joinCounter)
                        .append(".VALUE #>> '{value,coordinates,0}')::numeric")
                        .append(", (AX").append(joinCounter).append(".VALUE #>> '{value,coordinates,1}')::numeric")
                        .append(")")
                        .append(", ST_MakeEnvelope(")
                        .append(location.lngMin)
                        .append(",")
                        .append(location.latMin)
                        .append(",")
                        .append(location.lngMax)
                        .append(",")
                        .append(location.latMax)
                        .append("))");
                }
            } else if (attributePredicate.value instanceof ValueNotEmptyPredicate) {
                attributeBuilder.append("AX").append(joinCounter).append(".VALUE ->> 'value' IS NOT NULL");
            } else if (attributePredicate.value instanceof ValueEmptyPredicate) {
                attributeBuilder.append("AX").append(joinCounter).append(".VALUE ->> 'value' IS NULL");
            } else {
                throw new UnsupportedOperationException("Attribute value predicate is not supported: " + attributePredicate.value);
            }
        }

        return attributeBuilder.toString();
    }

    protected String buildOperatorFilter(AssetQuery.Operator operator, boolean negate) {
        switch (operator) {
            case EQUALS:
                if (negate) {
                    return " <> ? ";
                }
                return " = ? ";
            case GREATER_THAN:
                if (negate) {
                    return " <= ? ";
                }
                return " > ? ";
            case GREATER_EQUALS:
                if (negate) {
                    return " < ? ";
                }
                return " >= ? ";
            case LESS_THAN:
                if (negate) {
                    return " >= ? ";
                }
                return " < ? ";
            case LESS_EQUALS:
                if (negate) {
                    return " > ? ";
                }
                return " <= ? ";
            case BETWEEN:
                if (negate) {
                    return " NOT BETWEEN ? AND ? ";
                }
                return " BETWEEN ? AND ? ";
        }

        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }

    protected String buildMatchFilter(StringPredicate predicate) {
        switch (predicate.match) {
            case BEGIN:
            case END:
            case CONTAINS:
                if (predicate.negate) {
                    return " not like ? ";
                }
                return " like ? ";
            default:
                if (predicate.negate) {
                    return " <> ? ";
                }
                return " = ? ";
        }
    }

    protected Asset mapResultTuple(AssetQuery query, ResultSet rs) throws SQLException {
        Asset asset = new Asset();
        asset.setId(rs.getString("ID"));
        asset.setType(rs.getString("ASSET_TYPE"));
        asset.setName(rs.getString("NAME"));
        asset.setVersion(rs.getLong("OBJ_VERSION"));
        asset.setCreatedOn(rs.getTimestamp("CREATED_ON"));
        asset.setAccessPublicRead(rs.getBoolean("ACCESS_PUBLIC_READ"));

        if (!query.select.excludeParentInfo) {
            asset.setParentId(rs.getString("PARENT_ID"));
            asset.setParentName(rs.getString("PARENT_NAME"));
            asset.setParentType(rs.getString("PARENT_TYPE"));
        }

        if (!query.select.excludeRealm) {
            asset.setRealm(rs.getString("REALM"));
        }

        if (!query.select.excludeAttributes) {
            if (rs.getString("ATTRIBUTES") != null) {
                asset.setAttributes(Values.instance().<ObjectValue>parse(rs.getString("ATTRIBUTES")).orElse(null));
            }
        }

        if (!query.select.excludePath) {
            Array path = rs.getArray("PATH");
            if (path != null) {
                asset.setPath((String[]) path.getArray());
            }
        }

        return asset;
    }

    public boolean storeAttributeValue(EntityManager em, String assetId, String attributeName, Value value, String timestamp) {
        return em.unwrap(Session.class).doReturningWork(connection -> {
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
                LOG.fine("Stored asset '" + assetId
                    + "' attribute '" + attributeName
                    + "' (affected rows: " + updatedRows + ") value: "
                    + (value != null ? value.toJson() : "null"));
                return updatedRows == 1;
            }
        });
    }

    // TODO: Remove AssetTreeModifiedEvent once GWT client replaced
    protected void publishModificationEvents(PersistenceEvent<Asset> persistenceEvent) {
        Asset asset = persistenceEvent.getEntity();
        switch (persistenceEvent.getCause()) {
            case CREATE:
                // Fully load the asset (excluding attributes)
                Asset loadedAsset = find(new AssetQuery().select(Select.selectAll().excludeAttributes(true)).ids(asset.getId()));

                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.CREATE, loadedAsset, null)
                );

                clientEventService.publishEvent(
                    new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealm(), asset.getId())
                );
                if (asset.getParentId() != null) {
                    // Child asset created
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(),
                            asset.getRealm(),
                            asset.getParentId(),
                            true)
                    );
                } else {
                    // Child asset created (root asset)
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealm(), true)
                    );
                }

                // Raise attribute event for each attribute
                asset.getAttributesStream().forEach(newAttribute ->
                    clientEventService.publishEvent(
                        new AttributeEvent(asset.getId(),
                            newAttribute.getNameOrThrow(),
                            newAttribute.getValue().orElse(null),
                            newAttribute.getValueTimestamp().orElse(timerService.getCurrentTimeMillis()))
                    ));
                break;
            case UPDATE:

                // Use simple equality check on each property
                String[] updatedProperties = Arrays.stream(persistenceEvent.getPropertyNames()).filter(propertyName -> {
                    if ("attributes".equals(propertyName)) {
                        return false;
                    }
                    Object oldValue = persistenceEvent.getPreviousState(propertyName);
                    Object newValue = persistenceEvent.getCurrentState(propertyName);
                    return !Objects.equals(oldValue, newValue);
                }).toArray(String[]::new);

                // Fully load the asset (excluding attributes)
                loadedAsset = find(new AssetQuery().select(Select.selectAll().excludeAttributes(true)).ids(asset.getId()));

                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.UPDATE, loadedAsset, updatedProperties)
                );

                // Did the name change?
                String previousName = persistenceEvent.getPreviousState("name");
                String currentName = persistenceEvent.getCurrentState("name");
                if (!Objects.equals(previousName, currentName)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(),
                            asset.getRealm(),
                            asset.getId())
                    );
                    break;
                }

                // Did the parent change?
                String previousParentId = persistenceEvent.getPreviousState("parentId");
                String currentParentId = persistenceEvent.getCurrentState("parentId");
                if (!Objects.equals(previousParentId, currentParentId)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(),
                            asset.getRealm(),
                            asset.getId())
                    );
                    break;
                }

                // Did the realm change?
                String previousRealm = persistenceEvent.getPreviousState("realm");
                String currentRealm = persistenceEvent.getCurrentState("realm");
                if (!Objects.equals(previousRealm, currentRealm)) {
                    clientEventService.publishEvent(
                        new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(),
                            asset.getRealm(),
                            asset.getId())
                    );
                    break;
                }

                // Did any attributes change if so raise attribute events on the event bus
                List<AssetAttribute> oldAttributes = attributesFromJson(persistenceEvent.getPreviousState("attributes"),
                    asset.getId()).collect(Collectors.toList());
                List<AssetAttribute> newAttributes = attributesFromJson(persistenceEvent.getCurrentState(
                    "attributes"), asset.getId()).collect(Collectors.toList());

                // Get removed attributes and raise an attribute event with deleted flag in attribute state
                getAddedAttributes(newAttributes, oldAttributes).forEach(obsoleteAttribute ->
                    clientEventService.publishEvent(
                        new AttributeEvent(asset.getId(), obsoleteAttribute.getNameOrThrow(), true)
                    ));


                // Get new or modified attributes
                getAddedOrModifiedAttributes(oldAttributes,
                    newAttributes)
                    .forEach(newOrModifiedAttribute ->
                        clientEventService.publishEvent(
                            new AttributeEvent(
                                asset.getId(),
                                newOrModifiedAttribute.getNameOrThrow(),
                                newOrModifiedAttribute.getValue().orElse(null),
                                newOrModifiedAttribute.getValueTimestamp().orElse(timerService.getCurrentTimeMillis()))
                        ));
                break;
            case DELETE:

                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.DELETE, asset, null)
                );

                clientEventService.publishEvent(
                    new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), asset.getRealm(), asset.getId())
                );

                // Raise attribute event with deleted flag for each attribute
                attributesFromJson(persistenceEvent.getPreviousState("attributes"), asset.getId())
                    .forEach(obsoleteAttribute ->
                        clientEventService.publishEvent(
                            new AttributeEvent(asset.getId(), obsoleteAttribute.getNameOrThrow(), true)
                        ));

                break;
        }
    }

    protected void replyWithAttributeEvents(String sessionKey, String subscriptionId, Asset asset, String[] attributeNames) {
        List<String> names = attributeNames == null ? Collections.emptyList() : Arrays.asList(attributeNames);

        // Client may want to read a subset or all attributes of the asset
        AttributeEvent[] events = asset.getAttributesStream()
            .filter(attribute -> names.isEmpty() || attribute.getName().filter(names::contains).isPresent())
            .map(AssetAttribute::getStateEvent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toArray(AttributeEvent[]::new);
        TriggeredEventSubscription triggeredEventSubscription = new TriggeredEventSubscription(events, subscriptionId);
        clientEventService.sendToSession(sessionKey, triggeredEventSubscription);
    }

    protected void replyWithAssetEvent(String sessionKey, String subscriptionId, Asset asset) {
        AssetEvent event = new AssetEvent(AssetEvent.Cause.READ, asset, null);
        TriggeredEventSubscription triggeredEventSubscription = new TriggeredEventSubscription(new AssetEvent[]{event}, subscriptionId);
        clientEventService.sendToSession(sessionKey, triggeredEventSubscription);
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}