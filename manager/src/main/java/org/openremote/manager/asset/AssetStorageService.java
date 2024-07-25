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

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jpa.AvailableHints;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.console.ConsoleResourceImpl;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.event.EventSubscriptionAuthorizer;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.*;
import org.openremote.model.asset.impl.GroupAsset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.event.shared.EventRequestResponseWrapper;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.filter.*;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.groupingBy;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.manager.event.ClientEventService.CLIENT_INBOUND_QUEUE;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;
import static org.openremote.model.query.AssetQuery.*;
import static org.openremote.model.query.AssetQuery.Access.*;
import static org.openremote.model.query.filter.ValuePredicate.asPredicateOrTrue;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;
import static org.openremote.model.value.MetaItemType.ACCESS_PUBLIC_READ;
import static org.openremote.model.value.MetaItemType.ACCESS_RESTRICTED_READ;

public class AssetStorageService extends RouteBuilder implements ContainerService {

    protected static class PreparedAssetQuery {

        final protected String querySql;
        final protected List<ParameterBinder> binders;

        public PreparedAssetQuery(String querySql, List<ParameterBinder> binders) {
            this.querySql = querySql;
            this.binders = binders;
        }

        protected void apply(EntityManager em, org.hibernate.query.Query<Object[]> query) {
            for (ParameterBinder binder : binders) {
                binder.accept(em, query);
            }
        }
    }

    public interface ParameterBinder extends BiConsumer<EntityManager, org.hibernate.query.Query<Object[]>> {

        @Override
        default void accept(EntityManager em, org.hibernate.query.Query<Object[]> st) {
            try {
                acceptStatement(em, st);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        void acceptStatement(EntityManager em, org.hibernate.query.Query<Object[]> st) throws SQLException;
    }

    private static final Logger LOG = Logger.getLogger(AssetStorageService.class.getName());
    public static final int PRIORITY = MED_PRIORITY;
//    protected static final Field assetParentNameField;
//    protected static final Field assetParentTypeField;
//
//    static {
//        try {
//            assetParentNameField = Asset.class.getDeclaredField("parentName");
//            assetParentNameField.setAccessible(true);
//            assetParentTypeField = Asset.class.getDeclaredField("parentType");
//            assetParentTypeField.setAccessible(true);
//        } catch (NoSuchFieldException e) {
//            LOG.log(Level.SEVERE, "Failed to find Asset parentName and/or parentType fields in the Asset class", e);
//            throw new IllegalStateException();
//        }
//    }

    @SuppressWarnings("unchecked")
    public static <T extends SharedEvent & AssetInfo> EventSubscriptionAuthorizer assetInfoAuthorizer(ManagerIdentityService identityService, AssetStorageService assetStorageService) {

         return (requestRealm, auth, sub) -> {

             @SuppressWarnings("unchecked")
             EventSubscription<T> subscription = (EventSubscription<T>)sub;

             // Only Asset<?> filters allowed
             if (subscription.getFilter() != null && !(subscription.getFilter() instanceof AssetFilter)) {
                 return false;
             }

             AssetFilter<T> filter = (AssetFilter<T>) subscription.getFilter();
             if (filter == null) {
                 filter = new AssetFilter<>();
                 subscription.setFilter(filter);
             }

             filter.setInternal(subscription.isInternal());

             // Internal subscribers and superusers can get events for any asset in any realm
             if (subscription.isInternal() || (auth != null && auth.isSuperUser())) {
                 return true;
             }

             requestRealm = filter.getRealm() != null ? filter.getRealm() : requestRealm;
             boolean isAnonymous = auth == null;
             boolean isRestricted = identityService.getIdentityProvider().isRestrictedUser(auth);
             String userId = !isAnonymous ? auth.getUserId() : null;
             String realm = requestRealm != null ? requestRealm : !isAnonymous ? auth.getAuthenticatedRealmName() : null;

             if (realm == null) {
                 LOG.info("Anonymous AssetInfo subscriptions must specify a realm");
                 return false;
             }

             if (isAnonymous || (requestRealm != null && !requestRealm.equals(auth.getAuthenticatedRealmName()))) {
                 // Users can only request public assets in different realms so force public events in the filter
                 filter.setPublicEvents(true);
             }

             // Regular user must have role
             if (!filter.isPublicEvents() && (isAnonymous || !auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID))) {
                 return false;
             }

             filter.setRealm(realm);

             if (isRestricted) {
                 filter.setRestrictedEvents(true);

                 // Restricted user can only subscribe to assets they are linked to so go fetch these
                 // TODO: Update asset IDs when user asset links are modified
                 filter.setUserAssetIds(
                     assetStorageService.findUserAssetLinks(realm, userId, null)
                         .stream()
                         .map(userAssetLink -> userAssetLink.getId().getAssetId())
                         .toList()
                 );
             }

             if (filter.getAssetIds() != null) {
                 // Client can subscribe to several assets
                 for (String assetId : filter.getAssetIds()) {
                     Asset<?> asset = assetStorageService.find(assetId, false);
                     // If the asset doesn't exist, subscription must fail
                     if (asset == null)
                         return false;
                     if (isRestricted) {
                         // Restricted users can only get events for their linked assets
                         if (!filter.getUserAssetIds().contains(assetId))
                             return false;
                     } else {
                         // Regular users can only get events for assets in their realm
                         if (!asset.getRealm().equals(realm))
                             return false;
                     }
                 }
             }

             return true;
         };
    }

    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;

    /**
     * Will evaluate each {@link CalendarEventPredicate} and apply it depending on the {@link LogicGroup} type
     * that each appears in. It tests the recurrence rule as simple start/end is checked in the DB query.
     */
    protected static boolean calendarEventPredicateMatches(Supplier<Long> currentMillisSupplier, AssetQuery query, Asset<?> asset) {

        if (query.attributes == null) {
            return true;
        }

        return calendarEventPredicateMatches(currentMillisSupplier, query.attributes, asset);
    }

    protected static boolean calendarEventPredicateMatches(Supplier<Long> currentMillisSupplier, LogicGroup<AttributePredicate> group, Asset<?> asset) {
        boolean isOr = group.operator == LogicGroup.Operator.OR;
        boolean matches = true;

        if (group.items != null) {
            for (AttributePredicate attributePredicate : group.items) {
                if (attributePredicate.value instanceof CalendarEventPredicate) {
                    Predicate<Object> namePredicate = asPredicateOrTrue(currentMillisSupplier, attributePredicate.name);
                    Predicate<Object> valuePredicate = asPredicateOrTrue(currentMillisSupplier, attributePredicate.value);
                    List<Attribute<?>> matchedAttributes = asset.getAttributes().stream()
                        .filter(attr -> namePredicate.test(attr.getName())).toList();

                    matches = true;

                    if (!matchedAttributes.isEmpty()) {
                        for (Attribute<?> attribute : matchedAttributes) {
                            matches = valuePredicate.test(attribute.getValue().orElse(null));
                            if (isOr && matches) {
                                break;
                            }
                            if (!isOr && !matches) {
                                break;
                            }
                        }
                    }

                    if (isOr && matches) {
                        break;
                    }
                    if (!isOr && !matches) {
                        break;
                    }
                }
            }
        }

        if (isOr && matches) {
            return true;
        }
        if (!isOr && !matches) {
            return false;
        }

        if (group.groups != null) {
            for (LogicGroup<AttributePredicate> childGroup : group.groups) {
                matches = calendarEventPredicateMatches(currentMillisSupplier, childGroup, asset);

                if (isOr && matches) {
                    break;
                }
                if (!isOr && !matches) {
                    break;
                }
            }
        }

        return matches;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);
        gatewayService = container.getService(GatewayService.class);
        EventSubscriptionAuthorizer assetEventAuthorizer = AssetStorageService.assetInfoAuthorizer(identityService, this);

        clientEventService.addSubscriptionAuthorizer((realm, auth, subscription) -> {
             if (!subscription.isEventType(AssetEvent.class)) {
                 return false;
             }

            return assetEventAuthorizer.authorise(realm, auth, subscription);
        });

        clientEventService.addEventAuthorizer((realm, auth, event) -> {
            boolean authorize = event instanceof HasAssetQuery;

            if (event instanceof ReadAssetEvent readAssetEvent) {
                if (readAssetEvent.getAssetQuery() == null) {
                    LOG.info("Read asset event must specify an asset ID");
                    return false;
                }
            } else if (event instanceof ReadAttributeEvent readAttributeEvent) {
                if (readAttributeEvent.getAssetQuery() == null) {
                    LOG.info("Read attribute event must specify an asset ID");
                    return false;
                }
            }

            return authorize && authorizeAssetQuery(((HasAssetQuery)event).getAssetQuery(), auth, realm);
        });

        container.getService(ManagerWebService.class).addApiSingleton(
            new AssetResourceImpl(
                container.getService(TimerService.class),
                identityService,
                this,
                container.getService(MessageBrokerService.class),
                clientEventService
            )
        );

        container.getService(ManagerWebService.class).addApiSingleton(
            new ConsoleResourceImpl(container.getService(TimerService.class),
                identityService,
                this,
                clientEventService)
        );

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
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
            .routeId("Persistence-Asset")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> publishModificationEvents(exchange.getIn().getBody(PersistenceEvent.class)));

        // React if a client wants to read assets and attributes
        from(CLIENT_INBOUND_QUEUE)
            .routeId("ClientInbound-Query")
            .filter(body().isInstanceOf(HasAssetQuery.class))
            .process(exchange -> {
                HasAssetQuery hasAssetQuery = exchange.getIn().getBody(HasAssetQuery.class);
                AssetQuery assetQuery = hasAssetQuery.getAssetQuery();
                String sessionKey = ClientEventService.getSessionKey(exchange);
                String messageId = exchange.getIn().getHeader(ClientEventService.HEADER_REQUEST_RESPONSE_MESSAGE_ID, String.class);
                Object response = null;

                if (hasAssetQuery instanceof ReadAssetsEvent) {
                    List<Asset<?>> assets = findAll(assetQuery);
                    response = new AssetsEvent(assets);
                } else {
                    Asset<?> asset = find(assetQuery);
                    String assetId;
                    String attributeName = null;

                    if (asset != null) {
                        if (hasAssetQuery instanceof ReadAttributeEvent readAttributeEvent) {
                            assetId = readAttributeEvent.getAttributeRef().getId();
                            attributeName = readAttributeEvent.getAttributeRef().getName();
                        } else {
                            assetId = ((ReadAssetEvent) hasAssetQuery).getAssetId();
                        }

                        if (!TextUtil.isNullOrEmpty(attributeName)) {
                            Attribute<?> assetAttribute = asset.getAttributes().get(attributeName).orElse(null);
                            if (assetAttribute != null) {
                                // Check access constraints
                                if (assetQuery.access == null
                                    || assetQuery.access == PRIVATE
                                    || (assetQuery.access == PUBLIC && assetAttribute.getMetaValue(ACCESS_PUBLIC_READ).orElse(false))
                                    || (assetQuery.access == PROTECTED && assetAttribute.getMetaValue(ACCESS_RESTRICTED_READ).orElse(false))) {
                                    response = new AttributeEvent(assetId, attributeName, assetAttribute.getValue().orElse(null), assetAttribute.getTimestamp().orElse(0L));
                                }
                            }
                        } else {
                            response = new AssetEvent(AssetEvent.Cause.READ, asset, null);
                        }
                    }
                }

                if (response != null) {
                    if (!isNullOrEmpty(messageId)) {
                        response = new EventRequestResponseWrapper<>(messageId, (SharedEvent)response);
                    }
                    clientEventService.sendToSession(sessionKey, response);
                }

            })
            .end();
    }

    /**
     * Authorizes an {@link AssetQuery} by validating it against security constraints and/or applying default options to the query
     * based on security constraints.
     */
    public boolean authorizeAssetQuery(AssetQuery query, AuthContext authContext, String requestRealm) {

        boolean isAnonymous = authContext == null;
        boolean isSuperUser = authContext != null && authContext.isSuperUser();
        boolean isRestricted = identityService.getIdentityProvider().isRestrictedUser(authContext);

        // Take realm from query, requestRealm or lastly auth context (super users can query with no realm)
        String realm = query.realm != null ? query.realm.name : requestRealm != null ? requestRealm : (!isSuperUser && authContext != null ? authContext.getAuthenticatedRealmName() : null);

        if (!isSuperUser) {
            if (TextUtil.isNullOrEmpty(realm)) {
                String msg = "Realm must be specified to read assets";
                LOG.finest(msg);
                return false;
            }

            if (isAnonymous) {
                if (query.access != null && query.access != PUBLIC) {
                    String msg = "Only public access allowed for anonymous requests";
                    LOG.finest(msg);
                    return false;
                }
                query.access = PUBLIC;
            } else if (isRestricted) {
                if (query.access == PRIVATE) {
                    String msg = "Only public or restricted access allowed for restricted requests";
                    LOG.finest(msg);
                    return false;
                }
                if (query.access == null) {
                    query.access = PROTECTED;
                }
            }

            if (query.access != PUBLIC && !authContext.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                String msg = "User must have '" + ClientRole.READ_ASSETS.getValue() + "' role to read non public assets";
                LOG.finest(msg);
                return false;
            }

            if (query.access != PUBLIC && !realm.equals(authContext.getAuthenticatedRealmName())) {
                String msg = "Realm must match authenticated realm for non public access queries";
                LOG.finest(msg);
                return false;
            }

            query.realm = new RealmPredicate(realm);

            if (query.access == PROTECTED) {
                query.userIds(authContext.getUserId());
            }
        }

        if (!identityService.getIdentityProvider().isRealmActiveAndAccessible(authContext, realm)) {
            String msg = "Realm is not present or is inactive";
            LOG.finest(msg);
            return false;
        }

        return true;
    }

    public Asset<?> find(String assetId) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().ids(assetId));
    }

    @SuppressWarnings("unchecked")
    public <T extends Asset<?>> T find(String assetId, Class<T> assetType) {
        Asset<?> asset = find(assetId);
        if (asset != null && !assetType.isAssignableFrom(asset.getClass())) {
            asset = null;
        }
        return (T)asset;
    }

    /**
     * @param loadComplete If the whole asset data (including attributes) should be loaded.
     */
    public Asset<?> find(String assetId, boolean loadComplete) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().select(loadComplete ? null : new Select().excludeAttributes()).ids(assetId));
    }

    @SuppressWarnings("unchecked")
    public <T extends Asset<?>> T find(String assetId, boolean loadComplete, Class<T> assetType) {
        Asset<?> asset = find(assetId, loadComplete);
        if (asset != null && !assetType.isAssignableFrom(asset.getClass())) {
            asset = null;
        }
        return (T)asset;
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     */
    public Asset<?> find(EntityManager em, String assetId, boolean loadComplete) {
        return find(em, assetId, loadComplete, PRIVATE);
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @param access       The required access permissions of the asset data.
     */
    public Asset<?> find(String assetId, boolean loadComplete, Access access) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery()
            .select(loadComplete
                ? null
                : new Select().excludeAttributes())
            .ids(assetId)
            .access(access));
    }

    public Asset<?> find(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> find(em, query));
    }

    public Asset<?> find(EntityManager em, AssetQuery query) {
        query.limit = 1;
        List<Asset<?>> result = findAll(em, query);
        if (result.isEmpty())
            return null;
        return result.get(0);
    }

    public List<Asset<?>> findAll(AssetQuery query) {
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
    public <T extends Asset<?>> T merge(T asset) throws IllegalStateException, ConstraintViolationException {
        return merge(asset, false);
    }

    /**
     * @param overrideVersion If <code>true</code>, the merge will override the data in the database, independent of
     *                        version.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public <T extends Asset<?>> T merge(T asset, boolean overrideVersion) throws IllegalStateException, ConstraintViolationException {
        return merge(asset, overrideVersion, false, null);
    }

    /**
     * @param userName the user which this asset needs to be assigned to.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public <T extends Asset<?>> T merge(T asset, String userName) throws IllegalStateException, ConstraintViolationException {
        return merge(asset, false, false, userName);
    }

    /**
     * Merge the requested {@link Asset} checking that it meets all constraint requirements before doing so; the
     * timestamp of each {@link Attribute} will also be updated to the current system time if it has changed to assist
     * with {@link Attribute} equality (see {@link Attribute#equals}).
     * @param overrideVersion If <code>true</code>, the merge will override the data in the database, independent of
     *                        version.
     * @param skipGatewayCheck Don't check if asset is a gateway asset and merge asset into local persistence service.
     * @param userName        the user which this asset needs to be assigned to.
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    @SuppressWarnings("unchecked")
    public <T extends Asset<?>> T merge(T asset, boolean overrideVersion, boolean skipGatewayCheck, String userName) throws IllegalStateException, ConstraintViolationException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Merging asset: " + asset);
        }

        return persistenceService.doReturningTransaction(em -> {

            long startTime = System.currentTimeMillis();
            String gatewayId = gatewayService.getLocallyRegisteredGatewayId(asset.getId(), asset.getParentId());

            if (!skipGatewayCheck && gatewayId != null) {
                LOG.fine("Sending asset merge request to gateway: Gateway ID=" + gatewayId);
                return gatewayService.mergeGatewayAsset(gatewayId, asset);
            }

            // Validate realm
            if (asset.getRealm() == null) {
                String msg = "Asset realm must be set : asset=" + asset;
                LOG.warning(msg);
                throw new IllegalStateException(msg);
            }

            // Do standard JSR-380 validation on the asset (includes custom validation using descriptors and constraints)
            Set<ConstraintViolation<Asset<?>>> validationFailures = ValueUtil.validate(asset);

            if (!validationFailures.isEmpty()) {
                String msg = "Asset merge failed as asset has failed constraint validation: asset=" + asset;
                ConstraintViolationException ex = new ConstraintViolationException(validationFailures);
                LOG.log(Level.WARNING, msg + ", exception=" + ex.getMessage());
                throw ex;
            }

            T existingAsset = TextUtil.isNullOrEmpty(asset.getId()) ? null : (T)em.find(Asset.class, asset.getId());

            if (existingAsset != null) {

                // Verify type has not been changed
                if (!existingAsset.getType().equals(asset.getType())) {
                    String msg = "Asset type cannot be changed: asset=" + asset;
                    LOG.warning(msg);
                    throw new IllegalStateException(msg);
                }

                if (!existingAsset.getRealm().equals(asset.getRealm())) {
                    String msg = "Asset realm cannot be changed: asset=" + asset;
                    LOG.warning(msg);
                    throw new IllegalStateException(msg);
                }

                // Update timestamp on modified attributes this allows fast equality checking
                asset.getAttributes().stream().forEach(attr ->
                    existingAsset.getAttribute(attr.getName()).ifPresent(existingAttr -> {
                        // If attribute is modified make sure the timestamp is also updated to allow simple equality
                        if (!attr.deepEquals(existingAttr) && attr.getTimestamp().orElse(0L) <= existingAttr.getTimestamp().orElse(0L)) {
                            // In the unlikely situation that we are in the same millisecond as last update
                            // we will always ensure a delta of >= 1ms
                            attr.setTimestamp(Math.max(existingAttr.getTimestamp().orElse(0L)+1, timerService.getCurrentTimeMillis()));
                        }
                }));

                // If this is real merge and desired, copy the persistent version number over the detached
                // version, so the detached state always wins and this update will go through and ignore
                // concurrent updates
                if (overrideVersion) {
                    asset.setVersion(existingAsset.getVersion());
                }
            }

            if (!identityService.getIdentityProvider().realmExists(asset.getRealm())) {
                String msg = "Asset realm not found or is inactive: asset=" + asset;
                LOG.warning(msg);
                throw new IllegalStateException(msg);
            }

            if (asset.getParentId() != null && asset.getParentId().equals(asset.getId())) {
                String msg = "Asset parent cannot be the asset: asset=" + asset;
                LOG.warning(msg);
                throw new IllegalStateException(msg);
            }

            // Validate parent only if asset is new or parent has changed
            if ((existingAsset == null && asset.getParentId() != null)
                || (existingAsset != null && asset.getParentId() != null && !asset.getParentId().equals(existingAsset.getParentId()))) {

                Asset<?> parent = find(em, asset.getParentId(), true);

                // The parent must exist
                if (parent == null) {
                    String msg = "Asset parent not found: asset=" + asset;
                    LOG.warning(msg);
                    throw new IllegalStateException(msg);
                }

                // The parent can not be a child of the asset
                if (parent.pathContains(asset.getId())) {
                    String msg = "Asset parent cannot be a descendant of the asset: asset=" + asset;
                    LOG.warning(msg);
                    throw new IllegalStateException(msg);
                }

                // The parent should be in the same realm
                if (!parent.getRealm().equals(asset.getRealm())) {
                    String msg = "Asset parent must be in the same realm: asset=" + asset;
                    LOG.warning(msg);
                    throw new IllegalStateException(msg);
                }

                // if parent is of type group then this child asset must have the correct type
                if (parent instanceof GroupAsset) {
                    String childAssetType = parent.getAttributes().getValue(GroupAsset.CHILD_ASSET_TYPE)
                        .orElseThrow(() -> {
                            String msg = "Asset parent is of type GROUP but the childAssetType attribute is invalid: asset=" + asset;
                            LOG.warning(msg);
                            return new IllegalStateException(msg);
                        });

                    // Look through type hierarchy for a match - this allows sub types
                    Class<?> clazz = asset.getClass();
                    boolean typeMatch = childAssetType.equals(clazz.getSimpleName());

                    while (!typeMatch && clazz != Asset.class) {
                        clazz = clazz.getSuperclass();
                        typeMatch = childAssetType.equals(clazz.getSimpleName());
                    }

                    if (!typeMatch) {
                        String msg = "Asset type does not match parent GROUP asset's childAssetType attribute: asset=" + asset;
                        LOG.warning(msg);
                        throw new IllegalStateException(msg);
                    }
                }
            }

            // Validate group child asset type attribute
            if (asset instanceof GroupAsset) {
                String childAssetType = ((GroupAsset)asset).getChildAssetType()
                    .map(childAssetTypeString -> TextUtil.isNullOrEmpty(childAssetTypeString) ? null : childAssetTypeString)
                    .orElseThrow(() -> {
                        String msg = "Asset of type GROUP childAssetType attribute must be a valid string: asset=" + asset;
                        LOG.warning(msg);
                        return new IllegalStateException(msg);
                    });

                String existingChildAssetType = existingAsset != null ? ((GroupAsset)existingAsset)
                    .getChildAssetType()
                    .orElseThrow(() -> {
                        String msg = "Asset of type GROUP childAssetType attribute must be a valid string: asset=" + asset;
                        LOG.warning(msg);
                        return new IllegalStateException(msg);
                    }) : childAssetType;

                if (!childAssetType.equals(existingChildAssetType)) {
                    String msg = "Asset of type GROUP so childAssetType attribute cannot be changed: asset=" + asset;
                    LOG.warning(msg);
                    throw new IllegalStateException(msg);
                }
            }

            // Update all empty attribute timestamps with server-time (a caller which doesn't have a
            // reliable time source such as a browser should clear the timestamp when setting an attribute
            // value).
            asset.getAttributes().forEach(attribute -> {
                if (!attribute.hasExplicitTimestamp()) {
                    attribute.setTimestamp(timerService.getCurrentTimeMillis());
                }
            });

            // If username present
            User user = null;
            if (!TextUtil.isNullOrEmpty(userName)) {
                user = identityService.getIdentityProvider().getUserByUsername(asset.getRealm(), userName);
                if (user == null) {
                    String msg = "User not found: " + userName;
                    LOG.warning(msg);
                    throw new IllegalStateException(msg);
                }
            }

            T updatedAsset = em.merge(asset);

            if (LOG.isLoggable(FINE)) {
                LOG.fine("Asset merge took: " + (System.currentTimeMillis() - startTime) + "ms");
            }

            if (user != null) {
                createUserAssetLinks(em, Collections.singletonList(new UserAssetLink(user.getRealm(), user.getId(), updatedAsset.getId())));
            }

            if (existingAsset == null && updatedAsset instanceof ThingAsset && !ThingAsset.DESCRIPTOR.getName().equals(updatedAsset.getType())) {
                // When an asset is first saved then any custom type is not persisted as JPA will set it to ThingAsset so we need to override
                // We don't need to do this when updating an existing asset as JPA doesn't overwrite the type - if this changes in future it
                // should be detected by tests
                em.createNativeQuery("update ASSET set type = ? where id = ?;")
                    .setParameter(1, updatedAsset.getType())
                    .setParameter(2, updatedAsset.getId())
                    .executeUpdate();
            }

            return updatedAsset;
        });
    }

    /**
     * @return <code>true</code> if the assets were deleted, false if any of the assets still have children and can't be deleted.
     */
    public boolean delete(List<String> assetIds) {
        return delete(assetIds, false);
    }

    public boolean delete(List<String> assetIds, boolean skipGatewayCheck) {

        List<String> ids = new ArrayList<>(assetIds);
        Map<String, List<String>> gatewayIdAssetIdMap = new HashMap<>();

        if (!skipGatewayCheck) {
            List<String> gatewayIds = ids.stream().filter(id -> gatewayService.isLocallyRegisteredGateway(id)).toList();

            if (!gatewayIds.isEmpty()) {
                // Handle gateway asset deletion in a special way
                ids.removeAll(gatewayIds);
                for (String gatewayId : gatewayIds) {
                    try {
                        boolean deleted = gatewayService.deleteGateway(gatewayId);
                        if (!deleted) {
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            }

            ids.removeIf(id -> {
                String gatewayId = gatewayService.getLocallyRegisteredGatewayId(id, null);
                if (gatewayId != null) {

                    if (gatewayIds.contains(gatewayId)) {
                        // Gateway is being deleted so no need to try and delete this descendant asset
                        return true;
                    }

                    gatewayIdAssetIdMap.compute(gatewayId, (gId, aIds) -> {
                        if (aIds == null) {
                            aIds = new ArrayList<>();
                        }
                        aIds.add(id);
                        return aIds;
                    });
                    return true;
                }
                return false;
            });

            if (gatewayIdAssetIdMap.isEmpty() && ids.isEmpty()) {
                return true;
            }

            // This is not atomic across gateways
            if (!gatewayIdAssetIdMap.isEmpty()) {
                for (Map.Entry<String, List<String>> gatewayIdAssetIds : gatewayIdAssetIdMap.entrySet()) {
                    String gatewayId = gatewayIdAssetIds.getKey();
                    List<String> gatewayAssetIds = gatewayIdAssetIds.getValue();
                    try {
                        boolean deleted = gatewayService.deleteGatewayAssets(gatewayId, gatewayAssetIds);
                        if (!deleted) {
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }

                if (ids.isEmpty()) {
                    return true;
                }
            }
        }

        try {
            persistenceService.doTransaction(em -> {
                List<Asset<?>> assets = em
                    .createQuery("select a from Asset a where not exists(select child.id from Asset child where child.parentId = a.id and not child.id in :ids) and a.id in :ids", Asset.class)
                    .setParameter("ids", ids)
                    .getResultList().stream().map(asset -> (Asset<?>) asset).collect(Collectors.toList());

                if (assetIds.size() != assets.size()) {
                    throw new IllegalArgumentException("Cannot delete one or more requested assets as they either have children or don't exist");
                }

                assets.sort(Comparator.comparingInt((Asset<?> asset) -> asset.getPath() == null ? 0 : asset.getPath().length).reversed());
                assets.forEach(em::remove);
                em.flush();
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
                    "select count(ual) from UserAssetLink ual where ual.id.assetId = :assetId" :
                    "select count(ual) from UserAssetLink ual where ual.id.userId = :userId and ual.id.assetId = :assetId";

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
                    "select count(ual) from UserAssetLink ual where ual.id.userId in :userIds and ual.id.assetId = :assetId",
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
                    "select count(ual) from UserAssetLink ual where ual.id.userId = :userId and ual.id.assetId in :assetIds",
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
        return persistenceService.doReturningTransaction(entityManager -> entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<>() {
            @Override
            public Boolean execute(Connection connection) throws SQLException {
                try (PreparedStatement st = connection.prepareStatement("select count(*) from Asset a where a.path ~ lquery(?) AND a.id = ANY(?)")) {
                    st.setString(1, "*." + parentAssetId + ".*");
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

    public List<UserAssetLink> findUserAssetLinks(String realm, String userId, String assetId) {
        return findUserAssetLinks(
            realm,
            userId != null ? Collections.singletonList(userId) : null,
            assetId != null ? Collections.singletonList(assetId) : null);
    }

    public List<UserAssetLink> findUserAssetLinks(String realm, Collection<String> userIds, Collection<String> assetIds) {

        if (realm == null && (userIds == null || userIds.isEmpty()) && (assetIds == null || assetIds.isEmpty())) {
            return Collections.emptyList();
        }

        return persistenceService.doReturningTransaction(em ->
            buildFindUserAssetLinksQuery(em, realm, userIds, assetIds).getResultList());
    }

    protected TypedQuery<UserAssetLink> buildFindUserAssetLinksQuery(EntityManager em, String realm, Collection<String> userIds, Collection<String> assetIds) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>(3);
        sb.append("select ua from UserAssetLink ua where 1=1");

        if (!isNullOrEmpty(realm)) {
            sb.append(" and ua.id.realm in :realm");
            parameters.put("realm", realm);
        }
        if (userIds != null && !userIds.isEmpty()) {
            sb.append(" and ua.id.userId in :userId");
            parameters.put("userId", userIds);
        }
        if (assetIds != null && !assetIds.isEmpty()) {
            sb.append(" and ua.id.assetId in :assetId");
            parameters.put("assetId", assetIds);
        }

        sb.append(" order by ua.createdOn desc");

        TypedQuery<UserAssetLink> query = em.createQuery(sb.toString(), UserAssetLink.class);
        parameters.forEach(query::setParameter);
        return query;
    }

    /* ####################################################################################### */

    /**
     * Delete specific {@link UserAssetLink}s.
     */
    public void deleteUserAssetLinks(List<UserAssetLink> userAssetLinks) {

        if (userAssetLinks == null || userAssetLinks.isEmpty()) {
            return;
        }

        Set<String> assetIds = new HashSet<>(userAssetLinks.size());
        Set<String> userIds = new HashSet<>(userAssetLinks.size());

        userAssetLinks.forEach(userAssetLink -> {
            userIds.add(userAssetLink.getId().getUserId());
            assetIds.add(userAssetLink.getId().getAssetId());
        });

        List<UserAssetLink> existingLinks = new ArrayList<>();

        persistenceService.doTransaction(entityManager -> {
             existingLinks.addAll(buildFindUserAssetLinksQuery(entityManager, null, userIds.stream().toList(), assetIds.stream().toList())
                .getResultList().stream().filter(userAssetLinks::contains).toList());

            if (existingLinks.size() != userAssetLinks.size()) {
                throw new IllegalArgumentException("Cannot delete one or more requested user asset links as they don't exist");
            }

            StringBuilder sb = new StringBuilder("DELETE FROM user_asset_link WHERE (1=0");

            IntStream.range(0, userAssetLinks.size()).forEach(i -> sb.append(" OR (asset_id=?")
                .append((3*i)+1)
                .append(" AND user_id=?")
                .append((3*i)+2)
                .append(" AND realm=?")
                .append((3*i)+3)
                .append(")"));
            sb.append(")");

            Query query = entityManager.createNativeQuery(sb.toString());

            IntStream.range(0, userAssetLinks.size()).forEach(i -> {
                UserAssetLink userAssetLink = userAssetLinks.get(i);
                query.setParameter((3*i)+1, userAssetLink.getId().getAssetId());
                query.setParameter((3*i)+2, userAssetLink.getId().getUserId());
                query.setParameter((3*i)+3, userAssetLink.getId().getRealm());
            });

            int deleteCount = query.executeUpdate();

            if (deleteCount != userAssetLinks.size()) {
                throw new IllegalArgumentException("Cannot delete one or more requested user asset links as they don't exist");
            }
        });

        existingLinks.forEach(userAssetLink ->
            persistenceService.publishPersistenceEvent(
                PersistenceEvent.Cause.DELETE,
                null,
                userAssetLink,
                UserAssetLink.class,
                null,
                null));

        if (LOG.isLoggable(FINE)) {
            LOG.fine("Deleted user asset links: count=" + userAssetLinks.size() + ", links=" + userAssetLinks.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Delete all {@link UserAssetLink}s for the specified {@link User}
     */
    public void deleteUserAssetLinks(String userId) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("DELETE FROM UserAssetLink ual WHERE ual.id.userId = ?1");
            query.setParameter(1, userId);
            int deleteCount = query.executeUpdate();
            LOG.fine("Deleted all user asset links for user: user ID=" + userId + ", count=" + deleteCount);
        });
    }

    /**
     * Create specified {@link UserAssetLink}s.
     */
    public void storeUserAssetLinks(List<UserAssetLink> userAssetLinks) {

        if (userAssetLinks == null || userAssetLinks.isEmpty()) {
            return;
        }

        Set<String> assetIds = new HashSet<>(userAssetLinks.size());
        Set<String> userIds = new HashSet<>(userAssetLinks.size());

        userAssetLinks.forEach(userAssetLink -> {
            userIds.add(userAssetLink.getId().getUserId());
            assetIds.add(userAssetLink.getId().getAssetId());
        });

        persistenceService.doTransaction(em -> {
            List<UserAssetLink> existingLinks = buildFindUserAssetLinksQuery(em, null, userIds.stream().toList(), assetIds.stream().toList())
                .getResultList();

            List<UserAssetLink> newLinks = userAssetLinks.stream()
                .filter(userAssetLink -> !existingLinks.contains(userAssetLink))
                .toList();

            createUserAssetLinks(em, newLinks);
        });
    }

    protected void createUserAssetLinks(EntityManager em, List<UserAssetLink> userAssets) {

        em.unwrap(Session.class).doWork(connection -> {

            if (LOG.isLoggable(FINE)) {
                LOG.fine("Storing user asset links: count=" + userAssets.size() + ", links=" + userAssets.stream().map(Object::toString).collect(Collectors.joining(", ")));
            }
            PreparedStatement st;

            try {
                st = connection.prepareStatement("INSERT INTO USER_ASSET_LINK (asset_id, realm, user_id, created_on) VALUES (?, ?, ?, ?) ON CONFLICT (asset_id, realm, user_id) DO NOTHING");
                for (UserAssetLink userAssetLink : userAssets) {
                    st.setString(1, userAssetLink.getId().getAssetId());
                    st.setString(2, userAssetLink.getId().getRealm());
                    st.setObject(3, userAssetLink.getId().getUserId());
                    st.setTimestamp(4, new Timestamp(timerService.getCurrentTimeMillis()));
                    st.addBatch();
                }
                st.executeBatch();

                // Create a persistence event for each one
                userAssets.forEach(userAssetLink ->
                    persistenceService.publishPersistenceEvent(
                        PersistenceEvent.Cause.CREATE,
                        userAssetLink,
                        null,
                        UserAssetLink.class,
                        null,
                        null)
                );
            } catch (Exception e) {
                String msg = "Failed to create user asset links: count=" + userAssets.size();
                LOG.log(Level.WARNING, msg, e);
                throw new IllegalStateException(msg, e);
            }
        });
    }

    protected Asset<?> find(EntityManager em, String assetId, boolean loadComplete, Access access) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(
            em,
            new AssetQuery()
                .select(loadComplete
                    ? null
                    : new Select().excludeAttributes())
                .ids(assetId)
                .access(access)
        );
    }

    @SuppressWarnings("unchecked")
    protected List<Asset<?>> findAll(EntityManager em, AssetQuery query) {

        long startMillis = System.currentTimeMillis();

        if (query.access == null)
            query.access = PRIVATE;

        // Do some sanity checks on query values and return empty result set if empty query parameters
        if (query.ids != null && query.ids.length == 0) {
            return Collections.emptyList();
        }
        if (query.paths != null && query.paths.length == 0) {
            return Collections.emptyList();
        }
        if (query.types != null && query.types.length == 0) {
            return Collections.emptyList();
        }
        if (query.names != null && query.names.length == 0) {
            return Collections.emptyList();
        }
        if (query.userIds != null && query.userIds.length == 0) {
            return Collections.emptyList();
        }
        if (query.parents != null && query.parents.length == 0) {
            return Collections.emptyList();
        }

        // Default to order by creation date if the query may return multiple results
        if (query.orderBy == null && query.ids == null)
            query.orderBy = new OrderBy(OrderBy.Property.CREATED_ON);

        Pair<PreparedAssetQuery, Boolean> queryAndContainsCalendarPredicate = buildQuery(query, timerService::getCurrentTimeMillis);
        PreparedAssetQuery querySql = queryAndContainsCalendarPredicate.key;
        boolean containsCalendarPredicate = queryAndContainsCalendarPredicate.value;

        if (containsCalendarPredicate && (query.select != null && (query.select.attributes == null))) {
            LOG.warning("Asset query contains a calendar event predicate which requires the attribute values and types to be included in the select (as calendar event predicate is applied post DB query)");
            throw new IllegalArgumentException("Asset query contains a calendar event predicate which requires the attribute values and types to be included in the select (as calendar event predicate is applied post DB query)");
        }

        // RT: No longer used as parent info removed for simplicity and security reasons
//        // Use a SqlResultSetMapping to allow auto hydration with retrieval of transient data as well
//        // Using hibernate query object rather than JPA as postgres array parameter support doesn't work in JPQL without specifying the data type
//        org.hibernate.query.Query<Object[]> jpql = em.createNativeQuery(querySql.querySql, "AssetMapping").unwrap(org.hibernate.query.Query.class);
//        querySql.apply(em, jpql);
//        List<Object[]> results = jpql.getResultList();
//        Stream<Asset<?>> assetStream = results.stream().map(objArr -> {
//            Asset<?> asset = (Asset<?>)objArr[0];
//
//            if (objArr.length == 3) {
//                // We have transient parent info
//                String parentName = (String)objArr[1];
//                String parentType = (String)objArr[2];
//                try {
//                    assetParentNameField.set(asset, parentName);
//                    assetParentTypeField.set(asset, parentType);
//                } catch (IllegalAccessException e) {
//                    LOG.log(Level.WARNING, "Failed to set asset parent name and/or type fields", e);
//                }
//            }
//            return asset;
//        });

        org.hibernate.query.Query<Object[]> jpql = em.createNativeQuery(querySql.querySql, Asset.class).unwrap(org.hibernate.query.Query.class)
            .setHint(AvailableHints.HINT_READ_ONLY, true); // Make query readonly so no dirty checks are performed
        querySql.apply(em, jpql);
        List<Asset<?>> assets = (List<Asset<?>>)(Object)jpql.getResultList();

        if (containsCalendarPredicate) {
            return assets.stream().filter(asset -> calendarEventPredicateMatches(timerService::getCurrentTimeMillis, query, asset)).toList();
        }

        if (LOG.isLoggable(FINE)) {
            LOG.fine("Asset query took " + (System.currentTimeMillis() - startMillis) + "ms: return count=" + assets.size());
        }

        return assets;
    }

    /**
     * This does a low level JDBC update so hibernate event interceptor doesn't get called and we 'manually'
     * generate the {@link AttributeEvent}
     */
    @SuppressWarnings("unchecked")
    protected boolean updateAttributeValue(EntityManager em, AttributeEvent event) throws ConstraintViolationException {

        long timestamp = event.getTimestamp() > 0 ? event.getTimestamp() : timerService.getCurrentTimeMillis();

        try {
            PGobject valueTimestampJSON = new PGobject();
            valueTimestampJSON.setType("jsonb");
            valueTimestampJSON.setValue("{\"value\":" + ValueUtil.asJSON(event.getValue().orElse(null)).orElse(ValueUtil.NULL_LITERAL) + ",\"timestamp\":" + timestamp + "}");

            // TODO: Use jsonb type directly to optimise over wire data (couldn't get this to work even after seeing https://stackoverflow.com/questions/53847917/postgresql-throws-column-is-of-type-jsonb-but-expression-is-of-type-bytea-with)
            Query query = em.createNativeQuery("UPDATE asset SET attributes[?] = attributes[?] || ?\\:\\:jsonb where id = ?")
                .setParameter(1, event.getName())
                .setParameter(2, event.getName())
                .setParameter(3, "{\"value\":" + ValueUtil.asJSON(event.getValue().orElse(null)).orElse(ValueUtil.NULL_LITERAL) + ",\"timestamp\":" + timestamp + "}")
                .setParameter(4, event.getId());

            int affectedRows = query.executeUpdate();
            boolean success = affectedRows == 1;

            if (success) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Updated attribute value assetID=" + event.getId() + ", attributeName=" + event.getName() + ", timestamp=" + timestamp);
                }
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Failed to update attribute value assetID=" + event.getId() + ", attributeName=" + event.getName() + ", timestamp=" + timestamp);
                }
            }

            if (success) {
                clientEventService.publishEvent(event);
            }

            return success;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to store attribute value", e);
            return false;
        }
    }

    protected void publishModificationEvents(PersistenceEvent<Asset<?>> persistenceEvent) {
        Asset<?> asset = persistenceEvent.getEntity();
        switch (persistenceEvent.getCause()) {
            case CREATE -> {
                // Fully load the asset
                Asset<?> loadedAsset = find(new AssetQuery().ids(asset.getId()));
                if (loadedAsset == null) {
                    return;
                }
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Asset created: " + loadedAsset.toStringAll());
                } else {
                    LOG.fine("Asset created: " + loadedAsset);
                }
                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.CREATE, loadedAsset, null)
                );

                // Raise attribute event for each created attribute
                asset.getAttributes().forEach(newAttribute ->
                    clientEventService.publishEvent(
                        new AttributeEvent(
                            asset,
                            newAttribute,
                            getClass().getSimpleName(),
                            newAttribute.getValue().orElse(null),
                            newAttribute.getTimestamp().orElse(0L),
                            newAttribute.getValue().orElse(null),
                            newAttribute.getTimestamp().orElse(0L))
                    ));
            }
            case UPDATE -> {
                boolean nonAttributeChange = persistenceEvent.getPropertyNames().size() > 1 || !persistenceEvent.hasPropertyChanged("attributes");
                boolean attributesChanged = persistenceEvent.hasPropertyChanged("attributes");
                LOG.finest(() -> "Asset updated: " + persistenceEvent);

                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.UPDATE, asset, persistenceEvent.getPropertyNames().toArray(String[]::new))
                );

                AttributeMap oldAttributes = attributesChanged ? persistenceEvent.getPreviousState("attributes") : asset.getAttributes();
                AttributeMap newAttributes = attributesChanged ? persistenceEvent.getCurrentState("attributes") : asset.getAttributes();

                // Publish events for deleted attributes
                if (attributesChanged) {
                    // Get removed attributes and raise an attribute event with deleted flag in attribute state
                    oldAttributes.stream()
                        .filter(oldAttribute ->
                            newAttributes.stream().noneMatch(newAttribute ->
                                oldAttribute.getName().equals(newAttribute.getName())
                            ))
                        .forEach(obsoleteAttribute ->
                            clientEventService.publishEvent(
                                new AttributeEvent(asset, obsoleteAttribute, getClass().getSimpleName(), null, timerService.getCurrentTimeMillis(), null, 0L)
                                    .setDeleted(true)
                            ));
                }

                Stream<Attribute<?>> attributeStream;

                if (nonAttributeChange) {
                    // If something other than attributes has changed then treat as if attributes changed as path etc could have changed
                    attributeStream = newAttributes.values().stream();
                } else {
                    // Get new or modified attributes
                    attributeStream = getAddedOrModifiedAttributes(oldAttributes.values(), newAttributes.values());
                }

                attributeStream
                    .forEach(newOrModifiedAttribute -> {
                        Optional<Attribute<?>> oldAttribute = oldAttributes.get(newOrModifiedAttribute.getName());
                        clientEventService.publishEvent(new AttributeEvent(
                            asset,
                            newOrModifiedAttribute,
                            getClass().getSimpleName(),
                            newOrModifiedAttribute.getValue().orElse(null),
                            newOrModifiedAttribute.getTimestamp().orElse(0L),
                            oldAttribute.flatMap(Attribute::getValue).orElse(null),
                            oldAttribute.flatMap(Attribute::getTimestamp).orElse(0L)
                        ));
                    });
            }
            case DELETE -> {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Asset deleted: " + asset.toStringAll());
                } else {
                    LOG.fine("Asset deleted: " + asset);
                }
                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.DELETE, asset, null)
                );

                // Raise attribute event with deleted flag for each attribute
                AttributeMap deletedAttributes = asset.getAttributes();
                deletedAttributes.forEach(obsoleteAttribute ->
                    clientEventService.publishEvent(
                        new AttributeEvent(asset, obsoleteAttribute, getClass().getSimpleName(), null, timerService.getCurrentTimeMillis(), null, 0L)
                            .setDeleted(true)
                    ));
            }
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }


    /* SQL BUILDER METHODS */


    protected static Pair<PreparedAssetQuery, Boolean> buildQuery(AssetQuery query, Supplier<Long> timeProvider) {
        LOG.finest("Building: " + query);
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.recursive;
        List<ParameterBinder> binders = new ArrayList<>();
        sb.append(buildSelectString(query, 1, binders, timeProvider));
        sb.append(buildFromString(query, 1));
        boolean containsCalendarPredicate = appendWhereClause(sb, query, 1, binders, timeProvider);

        if (recursive) {
            sb.insert(0, "WITH RECURSIVE top_level_assets AS ((");
            sb.append(") UNION (");
            sb.append(buildSelectString(query, 2, binders, timeProvider));
            sb.append(buildFromString(query, 2));
            containsCalendarPredicate = !containsCalendarPredicate && appendWhereClause(sb, query, 2, binders, timeProvider);
            sb.append("))");
            sb.append(buildSelectString(query, 3, binders, timeProvider));
            sb.append(buildFromString(query, 3));
            containsCalendarPredicate = !containsCalendarPredicate && appendWhereClause(sb, query, 3, binders, timeProvider);
        }

        sb.append(buildOrderByString(query));
        sb.append(buildLimitString(query));
        return new Pair<>(new PreparedAssetQuery(sb.toString(), binders), containsCalendarPredicate);
    }

    protected static String buildSelectString(AssetQuery query, int level, List<ParameterBinder> binders, Supplier<Long> timeProvider) {
        // level = 1 is main query select
        // level = 2 is union select
        // level = 3 is CTE select
        StringBuilder sb = new StringBuilder();
        AssetQuery.Select select = query.select;

        sb.append("select A.ID as ID, A.NAME as NAME, A.ACCESS_PUBLIC_READ as ACCESS_PUBLIC_READ");
        sb.append(", A.CREATED_ON AS CREATED_ON, A.TYPE AS TYPE, A.PARENT_ID AS PARENT_ID");
        sb.append(", A.REALM AS REALM, A.VERSION as VERSION");

        if (!query.recursive || level == 3) {
            sb.append(", A.PATH as PATH");
        } else {
            sb.append(", NULL as PATH");
        }

        if (select == null || select.attributes == null || select.attributes.length > 0) {
            if (query.recursive && level != 3) {
                sb.append(", A.ATTRIBUTES as ATTRIBUTES");
            } else {
                sb.append(buildAttributeSelect(query, binders, timeProvider));
            }
        } else {
            sb.append(", NULL as ATTRIBUTES");
        }

        return sb.toString();
    }

    protected static String buildAttributeSelect(AssetQuery query, List<ParameterBinder> binders, Supplier<Long> timeProvider) {

        Select select = query.select;
        boolean hasAttributeFilter = select != null && select.attributes != null && select.attributes.length > 0;

        if (!hasAttributeFilter && query.access == PRIVATE) {
            return ", A.ATTRIBUTES as ATTRIBUTES";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(", (");
        sb.append("select json_object_agg(AX.key, AX.value) from jsonb_each(A.attributes) as AX");
        sb.append(" where true");

        // Filter attributes
        if (select != null && select.attributes != null && select.attributes.length > 0) {
            final int pos = binders.size() + 1;
            sb.append(" AND AX.key = ANY(")
                .append("?")
                .append(pos)
                .append(")");
            binders.add((em, st) -> st.setParameter(pos, select.attributes));
        }

        if (query.access != PRIVATE) {
            String metaName = query.access == PROTECTED ? ACCESS_RESTRICTED_READ.getName() : ACCESS_PUBLIC_READ.getName();
            sb.append(" AND AX.VALUE #>> '{meta,").append(metaName).append("}' = 'true'");
        }

        sb.append(") AS ATTRIBUTES");
        return sb.toString();
    }

    protected static String buildFromString(AssetQuery query, int level) {
        // level = 1 is main query
        // level = 2 is union
        // level = 3 is CTE
        StringBuilder sb = new StringBuilder();
        boolean recursive = query.recursive;

        if (level == 1) {
            sb.append(" from Asset A ");
        } else if (level == 2) {
            sb.append(" from top_level_assets P ");
            sb.append("join Asset A on A.PARENT_ID = P.ID ");
        } else {
            sb.append(" from top_level_assets A ");
        }

        if ((!recursive || level == 3) && query.userIds != null) {
            sb.append("right join USER_ASSET_LINK UA on A.ID = UA.ASSET_ID ");
        }

        return sb.toString();
    }

    protected static String buildOrderByString(AssetQuery query) {
        StringBuilder sb = new StringBuilder();

        if (query.ids != null && !query.recursive) {
            return sb.toString();
        }

        if (query.orderBy != null && query.orderBy.property != null) {
            sb.append(" order by ");

            switch (query.orderBy.property) {
                case CREATED_ON -> sb.append(" A.CREATED_ON ");
                case ASSET_TYPE -> sb.append(" A.TYPE ");
                case NAME -> sb.append(" A.NAME ");
                case PARENT_ID -> sb.append(" A.PARENT_ID ");
                case REALM -> sb.append(" A.REALM ");
            }
            sb.append(query.orderBy.descending ? "desc " : "asc ");
        }

        return sb.toString();
    }

    protected static String buildLimitString(AssetQuery query) {
        if (query.limit > 0) {
            return " LIMIT " + query.limit;
        }
        return "";
    }

    protected static boolean appendWhereClause(StringBuilder sb, AssetQuery query, int level, List<ParameterBinder> binders, Supplier<Long> timeProvider) {
        // level = 1 is main query
        // level = 2 is union
        // level = 3 is CTE
        boolean containsCalendarPredicate = false;
        boolean recursive = query.recursive;
        sb.append(" where true");

        if (level == 2) {
            return false;
        }

        if (level == 1 && query.ids != null) {
            final int pos = binders.size() + 1;
            sb.append(" and A.ID = ANY(?")
                .append(pos)
                .append(")");
            binders.add((em, st) -> st.setParameter(pos, query.ids));
        }

        if (level == 1 && query.names != null) {

            sb.append(" and (");
            boolean isFirst = true;

            for (StringPredicate pred : query.names) {
                if (!isFirst) {
                    sb.append(" or ");
                }
                isFirst = false;
                final int pos = binders.size() + 1;
                sb.append(pred.caseSensitive ? "A.NAME " : "upper(A.NAME)");
                sb.append(StringPredicate.toSQLParameter(pred, pos, false));
                binders.add((em, st) -> st.setParameter(pos, pred.prepareValue()));
            }
            sb.append(")");
        }

        if (query.parents != null) {

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
                    final int pos = binders.size() + 1;
                    sb.append("A.PARENT_ID = ?").append(pos);
                    binders.add((em, st) -> st.setParameter(pos, pred.id));
                } else if (level == 1) {
                    sb.append("A.PARENT_ID is null");
                } else {
                    sb.append("true");
                }

                sb.append(")");
            }

            sb.append(")");
        }

        if (level == 1 && query.paths != null) {
            sb.append(" and (");
            Arrays.stream(query.paths)
                .map(p -> String.join(".", p.path))
                .forEach(lqueryStr -> {
                    int pos = binders.size() + 1;
                    sb.append("A.PATH ~ lquery(?").append(pos).append(") or ");
                    binders.add((em, st) -> st.setParameter(pos, "*." + lqueryStr + ".*"));
                });

            sb.append("false)");
        }

        if (!recursive || level == 3) {
            if (query.realm != null && !TextUtil.isNullOrEmpty(query.realm.name)) {
                final int pos = binders.size() + 1;
                sb.append(" and A.REALM = ?").append(pos);
                binders.add((em, st) -> st.setParameter(pos, query.realm.name));
            }

            if (query.userIds != null) {
                final int pos = binders.size() + 1;
                sb.append(" and UA.USER_ID = ANY(?")
                    .append(pos)
                    .append(")");
                binders.add((em, st) -> st.setParameter(pos, query.userIds));
            }

            if (level == 1 && query.access == Access.PUBLIC) {
                sb.append(" and A.ACCESS_PUBLIC_READ is true");
            }

            if (query.types != null) {
                String[] resolvedTypes = getResolvedAssetTypes(query.types);
                final int pos = binders.size() + 1;
                sb.append(" and A.TYPE = ANY(?")
                    .append(pos)
                    .append(")");
                binders.add((em, st) -> st.setParameter(pos, resolvedTypes));
            }

            if (query.attributes != null) {
                sb.append(" and A.id in (select A.id from ");
                AtomicInteger offset = new AtomicInteger(sb.length());
                Consumer<String> selectInserter = (str) -> sb.insert(offset.getAndAdd(str.length()), str);
                sb.append(" where true AND ");
                containsCalendarPredicate = addAttributePredicateGroupQuery(sb, binders, 0, selectInserter, query.attributes, timeProvider);
                sb.append(")");
            }
        }
        return containsCalendarPredicate;
    }

    protected static boolean addAttributePredicateGroupQuery(StringBuilder sb, List<ParameterBinder> binders, int groupIndex, Consumer<String> selectInserter, LogicGroup<AttributePredicate> attributePredicateGroup, Supplier<Long> timeProvider) {

        boolean containsCalendarPredicate = false;
        LogicGroup.Operator operator = attributePredicateGroup.operator;

        if (operator == null) {
            operator = LogicGroup.Operator.AND;
        }

        sb.append("(");

        if (!attributePredicateGroup.getItems().isEmpty()) {

            Collection<List<AttributePredicate>> grouped;

            if (operator == LogicGroup.Operator.AND) {
                // Group predicates by their attribute name predicate
                grouped = attributePredicateGroup.getItems().stream().collect(groupingBy(predicate -> predicate.name != null ? predicate.name : "")).values();
            } else {
                grouped = new ArrayList<>();
                grouped.add(attributePredicateGroup.getItems());
            }

            boolean isFirst = true;

            for (List<AttributePredicate> group : grouped) {
                if (!isFirst) {
                    sb.append(operator == LogicGroup.Operator.OR ? " or " : " and ");
                }
                isFirst = false;

                selectInserter.accept((groupIndex > 0 ? ", " : "") + "jsonb_each(A.attributes) as AX" + groupIndex);
                containsCalendarPredicate = !containsCalendarPredicate && addNameValuePredicates(group, sb, binders, "AX" + groupIndex, selectInserter, operator == LogicGroup.Operator.OR, timeProvider);
                groupIndex++;
            }
        }

        if (attributePredicateGroup.groups != null && attributePredicateGroup.groups.size() > 0) {
            for (LogicGroup<AttributePredicate> group : attributePredicateGroup.groups) {
                sb.append(operator == LogicGroup.Operator.OR ? " or " : " and ");
                boolean containsCalPred = addAttributePredicateGroupQuery(sb, binders, groupIndex, selectInserter, group, timeProvider);
                if (!containsCalendarPredicate && containsCalPred) {
                    containsCalendarPredicate = true;
                }
            }
        }
        sb.append(")");

        return containsCalendarPredicate;
    }

    protected static boolean addNameValuePredicates(List<? extends NameValuePredicate> nameValuePredicates, StringBuilder sb, List<ParameterBinder> binders, String jsonObjName, Consumer<String> selectInserter, boolean useOr, Supplier<Long> timeProvider) {
        boolean containsCalendarPredicate = false;

        boolean isFirst = true;
        int metaIndex = 0;
        for (NameValuePredicate nameValuePredicate : nameValuePredicates) {
            if (!containsCalendarPredicate && nameValuePredicate.value instanceof CalendarEventPredicate) {
                containsCalendarPredicate = true;
            }
            if (!isFirst) {
                sb.append(useOr ? " or " : " and ");
            }
            isFirst = false;

            sb.append("(");

            sb.append(buildNameValuePredicateFilter(nameValuePredicate, jsonObjName, binders, timeProvider));

            if (nameValuePredicate instanceof AttributePredicate attributePredicate) {

                if (attributePredicate.meta != null && attributePredicate.meta.length > 0) {
                    String metaJsonObjName = jsonObjName + "_AM" + metaIndex++;
                    selectInserter.accept(" LEFT JOIN jsonb_each(" + jsonObjName + ".VALUE #> '{meta}') as " + metaJsonObjName + " ON true");
                    sb.append(" and (");
                    addNameValuePredicates(Arrays.asList(attributePredicate.meta.clone()), sb, binders, metaJsonObjName, selectInserter, true, timeProvider);
                    sb.append(")");
                }
            }
            sb.append(")");
        }

        return containsCalendarPredicate;
    }

    protected static String buildNameValuePredicateFilter(NameValuePredicate nameValuePredicate, String jsonObjName, List<ParameterBinder> binders, Supplier<Long> timeProvider) {
        if (nameValuePredicate.name == null && nameValuePredicate.value == null) {
            return "TRUE";
        }

        StringBuilder attributeBuilder = new StringBuilder();

        // Only append not on the outer filter if value predicate is not set otherwise it will match any attribute with different name and value
        if (nameValuePredicate.negated && (nameValuePredicate.value == null || nameValuePredicate.name == null)) {
            attributeBuilder.append("NOT (");
        }

        if (nameValuePredicate.name != null) {

            attributeBuilder.append(nameValuePredicate.name.caseSensitive
                ? jsonObjName + ".key"
                : "upper(" + jsonObjName + ".key)"
            );

            final int pos = binders.size() + 1;
            attributeBuilder.append(StringPredicate.toSQLParameter(nameValuePredicate.name, pos, false));
            binders.add((em, st) -> st.setParameter(pos, nameValuePredicate.name.prepareValue()));

        }

        if (nameValuePredicate.value != null) {

            if (nameValuePredicate.name != null) {
                attributeBuilder.append(" and ");

                if (nameValuePredicate.negated) {
                    attributeBuilder.append(" NOT(");
                }
            }

            // Inserts the SQL string and adds the parameters
            BiConsumer<StringBuilder, List<ParameterBinder>> valuePathInserter;
            boolean isAttributePredicate = nameValuePredicate instanceof AttributePredicate;
            boolean isTextCompare = (nameValuePredicate.value instanceof ValueEmptyPredicate) || (nameValuePredicate.value instanceof StringPredicate);
            final String operator = isTextCompare ? "#>>" : "#>";

            if (nameValuePredicate.path == null || nameValuePredicate.path.getPaths().length == 0) {
                valuePathInserter = (sb, b) -> {
                    if (isAttributePredicate) {
                        sb.append("(").append(jsonObjName).append(".VALUE ").append(operator).append(" '{value}')");
                    } else {
                        sb.append(jsonObjName).append(".VALUE");
                        if (isTextCompare) {
                            sb.append(" #>> '{}'");
                        }
                    }
                };
            } else {
                List<String> paths = new ArrayList<>();
                if (isAttributePredicate) {
                    paths.add("value");
                }
                paths.addAll(Arrays.stream(nameValuePredicate.path.getPaths()).map(Object::toString).toList());

                valuePathInserter = (sb, b) -> {
                    final int pos = binders.size() + 1;
                    sb.append("(").append(jsonObjName).append(".VALUE ").append(operator).append(" ?").append(pos).append(")");
                    binders.add((em, st) -> st.setParameter(pos, paths.toArray(new String[0])));
                };
            }

            if (nameValuePredicate.value instanceof StringPredicate stringPredicate) {
                if (!stringPredicate.caseSensitive) {
                    attributeBuilder.append("upper(");
                }
                valuePathInserter.accept(attributeBuilder, binders);
                if (!stringPredicate.caseSensitive) {
                    attributeBuilder.append(")");
                }
                final int pos = binders.size() + 1;
                attributeBuilder.append(StringPredicate.toSQLParameter(stringPredicate, pos, false));
                binders.add((em, st) -> st.setParameter(pos, stringPredicate.prepareValue()));
            } else if (nameValuePredicate.value instanceof BooleanPredicate booleanPredicate) {
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" = to_jsonb(")
                    .append(booleanPredicate.value)
                    .append(")");
            } else if (nameValuePredicate.value instanceof DateTimePredicate dateTimePredicate) {
                attributeBuilder.append("(");
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" #>> '{}')\\:\\:timestamp");

                Pair<Long, Long> fromAndTo = dateTimePredicate.asFromAndTo(timeProvider.get());

                final int pos = binders.size() + 1;
                binders.add((em, st) -> st.setParameter(pos, new java.sql.Timestamp(fromAndTo.key != null ? fromAndTo.key : 0L)));
                attributeBuilder.append(buildOperatorFilter(dateTimePredicate.operator, dateTimePredicate.negate, pos));

                if (dateTimePredicate.operator == Operator.BETWEEN) {
                    final int pos2 = binders.size() + 1;
                    binders.add((em, st) -> st.setParameter(pos2, new java.sql.Timestamp(fromAndTo.value != null ? fromAndTo.value : Long.MAX_VALUE)));
                }
            } else if (nameValuePredicate.value instanceof NumberPredicate numberPredicate) {
                attributeBuilder.append("(");
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" #>> '{}')\\:\\:numeric");
                final int pos = binders.size() + 1;
                attributeBuilder.append(buildOperatorFilter(numberPredicate.operator, numberPredicate.negate, pos));
                binders.add((em, st) -> st.setParameter(pos, numberPredicate.value));
                if (numberPredicate.operator == Operator.BETWEEN) {
                    final int pos2 = binders.size() + 1;
                    binders.add((em, st) -> st.setParameter(pos2, numberPredicate.rangeValue));
                }
            } else if (nameValuePredicate.value instanceof ArrayPredicate arrayPredicate) {
                if (arrayPredicate.negated) {
                    attributeBuilder.append("NOT(");
                }
                if (arrayPredicate.value != null) {
                    valuePathInserter.accept(attributeBuilder, binders);

                    if (arrayPredicate.index != null) {
                        attributeBuilder
                            .append(" -> ")
                            .append(arrayPredicate.index);
                    }
                    final int pos = binders.size() + 1;
                    attributeBuilder.append(" @> ?").append(pos).append(" \\:\\:jsonb");
                    binders.add((em, st) -> st.setParameter(pos, ValueUtil.asJSON(arrayPredicate.value).orElse(ValueUtil.NULL_LITERAL)));
                } else {
                    attributeBuilder.append("true");
                }

                if (arrayPredicate.lengthEquals != null) {
                    attributeBuilder.append(" and jsonb_array_length(");
                    valuePathInserter.accept(attributeBuilder, binders);
                    attributeBuilder
                        .append(") = ")
                        .append(arrayPredicate.lengthEquals);
                }
                if (arrayPredicate.lengthGreaterThan != null) {
                    attributeBuilder.append(" and jsonb_array_length(");
                    valuePathInserter.accept(attributeBuilder, binders);
                    attributeBuilder
                        .append(") > ")
                        .append(arrayPredicate.lengthGreaterThan);
                }
                if (arrayPredicate.lengthLessThan != null) {
                    attributeBuilder.append(" and jsonb_array_length(");
                    valuePathInserter.accept(attributeBuilder, binders);
                    attributeBuilder
                        .append(") < ")
                        .append(arrayPredicate.lengthLessThan);
                }
                if (arrayPredicate.negated) {
                    attributeBuilder.append(")");
                }
            } else if (nameValuePredicate.value instanceof GeofencePredicate) {
                if (nameValuePredicate.value instanceof RadialGeofencePredicate location) {
                    attributeBuilder.append("ST_DistanceSphere(ST_MakePoint((");
                    valuePathInserter.accept(attributeBuilder, binders);
                    attributeBuilder
                        .append(" #>> '{coordinates,0}')\\:\\:numeric")
                        .append(", (");
                    valuePathInserter.accept(attributeBuilder, binders);
                    attributeBuilder
                        .append(" #>> '{coordinates,1}')\\:\\:numeric")
                        .append("), ST_MakePoint(")
                        .append(location.lng)
                        .append(",")
                        .append(location.lat)
                        .append(location.negated ? ")) > " : ")) <= ")
                        .append(location.radius);
                } else if (nameValuePredicate.value instanceof RectangularGeofencePredicate location) {
                    if (location.negated) {
                        attributeBuilder.append("NOT");
                    }
                    attributeBuilder.append(" ST_Within(ST_MakePoint((");
                    valuePathInserter.accept(attributeBuilder, binders);
                    attributeBuilder
                        .append(" #>> '{coordinates,0}')\\:\\:numeric")
                        .append(", (");
                    valuePathInserter.accept(attributeBuilder, binders);
                    attributeBuilder
                        .append(" #>> '{coordinates,1}')\\:\\:numeric")
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
            } else if (nameValuePredicate.value instanceof ValueEmptyPredicate) {
                // Two situations - key is present and not null (cannot use IS NULL for this) or key is not present at all (have to use IS NULL for this)
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder.append(((ValueEmptyPredicate) nameValuePredicate.value).negate ? " IS NOT NULL" : " IS NULL");
            } else if (nameValuePredicate.value instanceof CalendarEventPredicate) {
                final int pos = binders.size() + 1;
                java.sql.Timestamp when = new java.sql.Timestamp(((CalendarEventPredicate)nameValuePredicate.value).timestamp.getTime());

                // The recurrence logic is applied post DB query just check start key is present and in the past and also
                // that the end key is numeric and in the future if no recurrence value
                attributeBuilder.append("(jsonb_typeof(");
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" #> '{start}') = 'number' AND jsonb_typeof(");
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" #> '{end}') = 'number' AND to_timestamp((");
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" #>> '{start}')\\:\\:float / 1000) <= ?").append(pos).append(" AND (to_timestamp((");
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" #>> '{end}')\\:\\:float / 1000) > ?").append(pos+1).append(" OR jsonb_typeof(");
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" #> '{recurrence}') = 'string'))");
                binders.add((em, st) -> st.setParameter(pos, when));
                binders.add((em, st) -> st.setParameter(pos+1, when));
            } else {
                throw new UnsupportedOperationException("Attribute value predicate is not supported: " + nameValuePredicate.value);
            }
        }

        if (nameValuePredicate.negated) {
            attributeBuilder.append(")");

            if (nameValuePredicate.value == null) {
                // We have to include `is null` in where clause also as technically name not equals X is satisfied by null - mostly useful for meta items
                attributeBuilder.append(" or ").append(jsonObjName).append(".key IS NULL");
            }
        }

        return attributeBuilder.toString();
    }

    protected static String buildOperatorFilter(AssetQuery.Operator operator, boolean negate, int pos) {
        switch (operator) {
            case EQUALS -> {
                if (negate) {
                    return " <> ?" + pos + " ";
                }
                return " = ?" + pos + " ";
            }
            case GREATER_THAN -> {
                if (negate) {
                    return " <= ?" + pos + " ";
                }
                return " > ?" + pos + " ";
            }
            case GREATER_EQUALS -> {
                if (negate) {
                    return " < ?" + pos + " ";
                }
                return " >= ?" + pos + " ";
            }
            case LESS_THAN -> {
                if (negate) {
                    return " >= ?" + pos + " ";
                }
                return " < ?" + pos + " ";
            }
            case LESS_EQUALS -> {
                if (negate) {
                    return " > ?" + pos + " ";
                }
                return " <= ?" + pos + " ";
            }
            case BETWEEN -> {
                if (negate) {
                    return " NOT BETWEEN ?" + pos + " AND ?" + (pos + 1) + " ";
                }
                return " BETWEEN ?" + pos + " AND ?" + (pos + 1) + " ";
            }
        }

        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
}
