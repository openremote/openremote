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

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
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
import org.openremote.model.asset.*;
import org.openremote.model.asset.impl.GroupAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.AssetInfo;
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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.lang.reflect.Field;
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

import static java.util.stream.Collectors.groupingBy;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
import static org.openremote.manager.event.ClientEventService.CLIENT_EVENT_TOPIC;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;
import static org.openremote.model.query.AssetQuery.*;
import static org.openremote.model.query.AssetQuery.Access.PRIVATE;
import static org.openremote.model.query.AssetQuery.Access.PROTECTED;
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
    protected static final Field assetParentNameField;
    protected static final Field assetParentTypeField;

    static {
        try {
            assetParentNameField = Asset.class.getDeclaredField("parentName");
            assetParentNameField.setAccessible(true);
            assetParentTypeField = Asset.class.getDeclaredField("parentType");
            assetParentTypeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOG.log(Level.SEVERE, "Failed to find Asset parentName and/or parentType fields in the Asset class", e);
            throw new IllegalStateException();
        }
    }

    public static <T extends SharedEvent & AssetInfo> EventSubscriptionAuthorizer assetInfoAuthorizer(ManagerIdentityService identityService, AssetStorageService assetStorageService) {

         return (auth, sub) -> {

             @SuppressWarnings("unchecked")
             EventSubscription<T> subscription = (EventSubscription<T>)sub;

             // Only Asset<?> filters allowed
             if (subscription.getFilter() != null && !(subscription.getFilter() instanceof AssetFilter)) {
                 return false;
             }

             AssetFilter<T> filter = (AssetFilter<T>) subscription.getFilter();

             // Superuser can get events for any asset
             if (auth.isSuperUser())
                 return true;

             // Regular user must have role
             if (!auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), auth.getClientId())) {
                 return false;
             }

             boolean isRestrictedUser = identityService.getIdentityProvider().isRestrictedUser(auth);

             if (filter == null) {
                 filter = new AssetFilter<>();
                 subscription.setFilter(filter);
             }

             // Regular and restricted users can only subscribe to assets in their realm
             filter.setRealm(auth.getAuthenticatedRealm());

             // Restricted user can only subscribe to assets they are linked to
             boolean skipAssetIdCheck = false;
             if (isRestrictedUser && (filter.getAssetIds() == null || filter.getAssetIds().length == 0)) {
                 filter.setAssetIds(
                     assetStorageService.findUserAssetLinks(auth.getAuthenticatedRealm(), auth.getUserId(), null)
                         .stream()
                         .map(userAssetLink -> userAssetLink.getId().getAssetId())
                         .toArray(String[]::new)
                 );
                 skipAssetIdCheck = true;
             }

             if (!skipAssetIdCheck && filter.getAssetIds() != null) {
                 // Client can subscribe to several assets
                 for (String assetId : filter.getAssetIds()) {
                     Asset<?> asset = assetStorageService.find(assetId, false);
                     // If the asset doesn't exist, subscription must fail
                     if (asset == null)
                         return false;
                     if (isRestrictedUser) {
                         // Restricted users can only get events for their linked assets
                         if (!assetStorageService.isUserAsset(auth.getUserId(), assetId))
                             return false;
                         // TODO Restricted clients should only receive events for PROTECTED attributes!
                     } else {
                         // Regular users can only get events for assets in their realm
                         if (!asset.getRealm().equals(auth.getAuthenticatedRealm()))
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
                        .filter(attr -> namePredicate.test(attr.getName())).collect(Collectors.toList());

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

        clientEventService.addSubscriptionAuthorizer((auth, subscription) -> {
             if (!subscription.isEventType(AssetEvent.class)) {
                 return false;
             }

            return assetEventAuthorizer.apply(auth, subscription);
        });

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
            .routeId("AssetPersistenceChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> publishModificationEvents(exchange.getIn().getBody(PersistenceEvent.class)));

        // React if a client wants to read assets and attributes
        from(CLIENT_EVENT_TOPIC)
            .routeId("FromClientReadRequests")
            .filter(
                or(body().isInstanceOf(ReadAssetsEvent.class), body().isInstanceOf(ReadAssetEvent.class), body().isInstanceOf(ReadAttributeEvent.class)))
            .choice()
                .when(or(body().isInstanceOf(ReadAssetEvent.class), body().isInstanceOf(ReadAttributeEvent.class)))
                    .process(exchange -> {
                        LOG.finer("Handling from client: " + exchange.getIn().getBody());

                        String assetId;
                        String attributeName = null;

                        if (exchange.getIn().getBody() instanceof ReadAssetEvent) {
                            assetId = exchange.getIn().getBody(ReadAssetEvent.class).getAssetId();
                        } else {
                            ReadAttributeEvent assetAttributeEvent = exchange.getIn().getBody(ReadAttributeEvent.class);
                            assetId = assetAttributeEvent.getAttributeRef().getId();
                            attributeName = assetAttributeEvent.getAttributeRef().getName();
                        }

                        if (TextUtil.isNullOrEmpty(assetId)) {
                            return;
                        }

                        boolean isAttributeRead = !TextUtil.isNullOrEmpty(attributeName);
                        String sessionKey = ClientEventService.getSessionKey(exchange);
                        AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);

                        // Superuser can get all, User must have role
                        if (!authContext.isSuperUser() && !authContext.hasResourceRole(ClientRole.READ_ASSETS.getValue(), authContext.getClientId())) {
                            return;
                        }

                        Access access = authContext.isSuperUser() || !identityService.getIdentityProvider().isRestrictedUser(authContext) ? PRIVATE : PROTECTED;
                        AssetQuery assetQuery = new AssetQuery()
                            .ids(assetId)
                            .select(new Select().excludePath(false).excludeParentInfo(true))
                            .access(access);

                        if (access == PROTECTED) {
                            assetQuery.userIds(authContext.getUserId());
                        }

                        Asset<?> asset = find(assetQuery);

                        if (asset != null) {
                            String messageId = exchange.getIn().getHeader(ClientEventService.HEADER_REQUEST_RESPONSE_MESSAGE_ID, String.class);
                            Object response = null;

                            if (isAttributeRead) {
                                Attribute<?> assetAttribute = asset.getAttributes().get(attributeName).orElse(null);
                                if (assetAttribute != null) {
                                    response = new AttributeEvent(assetId, attributeName, assetAttribute.getValue().orElse(null), assetAttribute.getTimestamp().orElse(0L));
                                }
                            } else {
                                response = new AssetEvent(AssetEvent.Cause.READ, asset, null);
                            }

                            if (response != null) {
                                if (!isNullOrEmpty(messageId)) {
                                    response = new EventRequestResponseWrapper<>(messageId, (SharedEvent)response);
                                }
                                clientEventService.sendToSession(sessionKey, response);
                            }
                        }
                    })
                .when(body().isInstanceOf(ReadAssetsEvent.class))
                    .process(exchange -> {
                        ReadAssetsEvent readAssets = exchange.getIn().getBody(ReadAssetsEvent.class);
                        AssetQuery query = readAssets.getAssetQuery();

                        String sessionKey = ClientEventService.getSessionKey(exchange);
                        AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);

                        // Superuser can get all, User must have role
                        if (!authContext.isSuperUser() && !authContext.hasResourceRole(ClientRole.READ_ASSETS.getValue(), authContext.getClientId())) {
                            return;
                        }

                        Access access = authContext.isSuperUser() || !identityService.getIdentityProvider().isRestrictedUser(authContext) ? PRIVATE : PROTECTED;

                        if (!authContext.isSuperUser()) {
                            // Force realm to match users
                            query.tenant(new TenantPredicate(authContext.getAuthenticatedRealm()));
                        }

                        // Set access requirements
                        query.access(access);

                        if (access == PROTECTED) {
                            query.userIds(authContext.getUserId());
                        }

                        List<Asset<?>> assets = findAll(query);

                        String messageId = exchange.getIn().getHeader(ClientEventService.HEADER_REQUEST_RESPONSE_MESSAGE_ID, String.class);

                        if (isNullOrEmpty(messageId)) {
                            clientEventService.sendToSession(sessionKey, new AssetsEvent(assets));
                        } else {
                            clientEventService.sendToSession(sessionKey, new EventRequestResponseWrapper<>(messageId, new AssetsEvent(assets)));
                        }
                    })
                    .stop()
            .endChoice()
            .end();
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
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     */
    public Asset<?> find(String assetId, boolean loadComplete) {
        if (assetId == null)
            throw new IllegalArgumentException("Can't query null asset identifier");
        return find(new AssetQuery().select(loadComplete ? null : Select.selectExcludeAll()).ids(assetId));
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
                : Select.selectExcludePathAndAttributes())
            .ids(assetId)
            .access(access));
    }

    public Asset<?> find(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> find(em, query));
    }

    public Asset<?> find(EntityManager em, AssetQuery query) {
        List<Asset<?>> result = findAll(em, query);
        if (result.size() == 0)
            return null;
        if (result.size() > 1) {
            throw new IllegalArgumentException("Query returned more than one asset");
        }
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
        return persistenceService.doReturningTransaction(em -> {

            T existingAsset = TextUtil.isNullOrEmpty(asset.getId()) ? null : (T)em.find(Asset.class, asset.getId());

            // Do standard JSR-380 validation on the asset (includes custom validation)
            Set<ConstraintViolation<Asset<?>>> validationFailures = ValueUtil.validate(asset, Asset.AssetSave.class);

            if (validationFailures.size() > 0) {
                String msg = "Asset merge failed as asset has failed constraint validation: asset=" + asset;
                ConstraintViolationException ex = new ConstraintViolationException(validationFailures);
                LOG.log(Level.WARNING, msg + ", exception=" + ex.getMessage(), ex);
                throw ex;
            }

            if (existingAsset != null) {

                // Verify type has not been changed
                if (!existingAsset.getType().equals(asset.getType())) {
                    String msg = "Asset type cannot be changed: asset=" + asset;
                    LOG.info(msg);
                    throw new IllegalStateException(msg);
                }

                if (!existingAsset.getRealm().equals(asset.getRealm())) {
                    String msg = "Asset realm cannot be changed: asset=" + asset;
                    LOG.info(msg);
                    throw new IllegalStateException(msg);
                }

                // Update timestamp on modified attributes
                asset.getAttributes().stream().forEach(attr -> {
                    existingAsset.getAttribute(attr.getName()).ifPresent(existingAttr -> {
                        // If attribute is modified make sure the timestamp is also updated to allow simple equality
                        if (!attr.deepEquals(existingAttr) && attr.getTimestamp().orElse(0L) <= existingAttr.getTimestamp().orElse(0L)) {
                            attr.setTimestamp(timerService.getCurrentTimeMillis());
                        }
                    });
                });

                // If this is real merge and desired, copy the persistent version number over the detached
                // version, so the detached state always wins and this update will go through and ignore
                // concurrent updates
                if (overrideVersion) {
                    asset.setVersion(existingAsset.getVersion());
                }
            }

            // Validate parent
            if (asset.getParentId() != null) {
                // If this is a not a root asset...
                Asset<?> parent = find(em, asset.getParentId(), true);

                // .. the parent must exist
                if (parent == null) {
                    String msg = "Asset parent not found: asset=" + asset;
                    LOG.info(msg);
                    throw new IllegalStateException(msg);
                }

                // ... the parent can not be a child of the asset
                if (parent.pathContains(asset.getId())) {
                    String msg = "Asset parent cannot be a descendant of the asset: asset=" + asset;
                    LOG.info(msg);
                    throw new IllegalStateException(msg);
                }

                // .. the parent should be in the same realm
                if (!parent.getRealm().equals(asset.getRealm())) {
                    String msg = "Asset parent must be in the same realm: asset=" + asset;
                    LOG.info(msg);
                    throw new IllegalStateException(msg);
                }

                // if parent is of type group then this child asset must have the correct type
                if (parent instanceof GroupAsset) {
                    String childAssetType = parent.getAttributes().getValue(GroupAsset.CHILD_ASSET_TYPE)
                        .orElseThrow(() -> {
                            String msg = "Asset parent is of type GROUP but the childAssetType attribute is invalid: asset=" + asset;
                            LOG.info(msg);
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
                        LOG.info(msg);
                        throw new IllegalStateException(msg);
                    }
                }
            }

            // Validate realm
            if (asset.getRealm() == null) {
                String msg = "Asset realm must be set : asset=" + asset;
                LOG.info(msg);
                throw new IllegalStateException(msg);
            }

            if (!identityService.getIdentityProvider().tenantExists(asset.getRealm())) {
                String msg = "Asset realm not found or is inactive: asset=" + asset;
                LOG.info(msg);
                throw new IllegalStateException(msg);
            }

            // Validate group child asset type attribute
            if (asset instanceof GroupAsset) {
                String childAssetType = ((GroupAsset)asset).getChildAssetType()
                    .map(childAssetTypeString -> TextUtil.isNullOrEmpty(childAssetTypeString) ? null : childAssetTypeString)
                    .orElseThrow(() -> {
                        String msg = "Asset of type GROUP childAssetType attribute must be a valid string: asset=" + asset;
                        LOG.info(msg);
                        return new IllegalStateException(msg);
                    });

                String existingChildAssetType = existingAsset != null ? ((GroupAsset)existingAsset)
                    .getChildAssetType()
                    .orElseThrow(() -> {
                        String msg = "Asset of type GROUP childAssetType attribute must be a valid string: asset=" + asset;
                        LOG.info(msg);
                        return new IllegalStateException(msg);
                    }) : childAssetType;

                if (!childAssetType.equals(existingChildAssetType)) {
                    String msg = "Asset of type GROUP so childAssetType attribute cannot be changed: asset=" + asset;
                    LOG.info(msg);
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
                    LOG.info(msg);
                    throw new IllegalStateException(msg);
                }
            }

            T updatedAsset;
            String gatewayId = gatewayService.getLocallyRegisteredGatewayId(asset.getId(), asset.getParentId());

            if (!skipGatewayCheck && gatewayId != null) {
                LOG.fine("Sending asset merge request to gateway: Gateway ID=" + gatewayId);
                updatedAsset = gatewayService.mergeGatewayAsset(gatewayId, asset);
            } else {
                updatedAsset = em.merge(asset);
                if (existingAsset == null) {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer("Asset created: " + updatedAsset.toStringAll());
                    } else {
                        LOG.fine("Asset created: " + updatedAsset);
                    }
                } else {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer("Asset updated: new = " + updatedAsset.toStringAll() + ", old = " + existingAsset.toStringAll());
                    } else {
                        LOG.fine("Asset updated: " + updatedAsset);
                    }
                }
            }

            if (user != null) {
                storeUserAssetLinks(em, Collections.singletonList(new UserAssetLink(user.getRealm(), user.getId(), updatedAsset.getId())));
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
            List<String> gatewayIds = ids.stream().filter(id -> gatewayService.isLocallyRegisteredGateway(id)).collect(Collectors.toList());

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

            //noinspection ConstantConditions
            if (gatewayIdAssetIdMap.isEmpty() && ids.isEmpty()) {
                return true;
            }

            // This is not atomic across gateways
            //noinspection ConstantConditions
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
                assets.forEach(asset -> {
                    em.remove(asset);
                    LOG.fine("Asset deleted: " + asset);
                });
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

    public List<UserAssetLink> findUserAssetLinks(String realm, String userId, String assetId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            StringBuilder sb = new StringBuilder();
            Map<String, Object> parameters = new HashMap<>(3);
            sb.append("select ua from UserAssetLink ua where 1=1");

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

            TypedQuery<UserAssetLink> query = entityManager.createQuery(sb.toString(), UserAssetLink.class);
            parameters.forEach(query::setParameter);
            return query.getResultList();
        });
    }

    /* ####################################################################################### */

    /**
     * Delete specific {@link UserAssetLink}s.
     */
    public void deleteUserAssetLinks(List<UserAssetLink> userAssetLinks) {
        persistenceService.doTransaction(entityManager -> {
            StringBuilder sb = new StringBuilder("DELETE FROM user_asset_link WHERE (1=0");

            IntStream.range(0, userAssetLinks.size()).forEach(i -> sb.append(" OR (asset_id=?")
                .append(3*i)
                .append(" AND user_id=?")
                .append((3*i)+1)
                .append(" AND realm=?")
                .append((3*i)+2)
                .append(")"));
            sb.append(")");

            Query query = entityManager.createNativeQuery(sb.toString());

            IntStream.range(0, userAssetLinks.size()).forEach(i -> {
                UserAssetLink userAssetLink = userAssetLinks.get(i);
                query.setParameter((3*i), userAssetLink.getId().getAssetId());
                query.setParameter((3*i)+1, userAssetLink.getId().getUserId());
                query.setParameter((3*i)+2, userAssetLink.getId().getRealm());
            });

            int deleteCount = query.executeUpdate();

            if (deleteCount != userAssetLinks.size()) {
                throw new IllegalArgumentException("Cannot delete one or more requested user asset link as they don't exist");
            }
        });
    }

    /**
     * Delete all {@link UserAssetLink}s for the specified realm (must be called before the realm is removed)
     */
    public void deleteUserAssetsByRealm(String realm) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("DELETE FROM UserAssetLink ual WHERE ual.id.realm = ?0");
            query.setParameter(0, realm);
            int deleteCount = query.executeUpdate();
            LOG.fine("Deleted all user asset links for realm: realm=" + realm + ", count=" + deleteCount);
        });
    }

    /**
     * Delete all {@link UserAssetLink}s for the specified {@link User} (must be called before the user is removed)
     */
    public void deleteUserAssetsByUserId(String userId) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("DELETE FROM UserAssetLink ual WHERE ual.id.userId = ?0");
            query.setParameter(0, userId);
            int deleteCount = query.executeUpdate();
            LOG.fine("Deleted all user asset links for user: user ID=" + userId + ", count=" + deleteCount);
        });
    }

    /**
     * Delete all {@link UserAssetLink}s for the specified {@link Asset} (must be called before the asset is removed)
     */
    public void deleteUserAssetsByAssetId(String assetId) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("DELETE FROM UserAssetLink ual WHERE ual.id.assetId = ?0");
            query.setParameter(0, assetId);
            int deleteCount = query.executeUpdate();
            LOG.fine("Deleted all user asset links for asset: asset ID=" + assetId + ", count=" + deleteCount);
        });
    }

    /**
     * Create specified {@link UserAssetLink}s.
     */
    public void storeUserAssetLinks(List<UserAssetLink> userAssetLinks) {

        if (userAssetLinks.isEmpty()) {
            return;
        }

        persistenceService.doTransaction(em -> storeUserAssetLinks(em, userAssetLinks));
    }
    protected void storeUserAssetLinks(EntityManager em, List<UserAssetLink> userAssets) {

        em.unwrap(Session.class).doWork(connection -> {

            LOG.finest("Storing user assets: count=" + userAssets.size());
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
            } catch (Exception e) {
                String msg = "Failed to create user assets: count=" + userAssets.size();
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
                    : Select.selectExcludePathAndAttributes())
                .ids(assetId)
                .access(access)
        );
    }

    @SuppressWarnings("unchecked")
    protected List<Asset<?>> findAll(EntityManager em, AssetQuery query) {

        if (query.access == null)
            query.access = PRIVATE;

        // Default to order by creation date if the query may return multiple results
        if (query.orderBy == null && query.ids == null)
            query.orderBy = new OrderBy(OrderBy.Property.CREATED_ON);

        Pair<PreparedAssetQuery, Boolean> queryAndContainsCalendarPredicate = buildQuery(query, timerService::getCurrentTimeMillis);
        PreparedAssetQuery querySql = queryAndContainsCalendarPredicate.key;
        boolean containsCalendarPredicate = queryAndContainsCalendarPredicate.value;

        if (containsCalendarPredicate && (query.select != null && (query.select.excludeAttributes))) {
            LOG.info("Asset query contains a calendar event predicate which requires the attribute values and types to be included in the select (as calendar event predicate is applied post DB query)");
            throw new IllegalArgumentException("Asset query contains a calendar event predicate which requires the attribute values and types to be included in the select (as calendar event predicate is applied post DB query)");
        }

        // Use a SqlResultSetMapping to allow auto hydration with retrieval of transient data as well
        // Using hibernate query object rather than JPA as postgres array parameter support doesn't work in JPQL without specifying the data type
        org.hibernate.query.Query<Object[]> jpql = em.createNativeQuery(querySql.querySql, "AssetMapping").unwrap(org.hibernate.query.Query.class);
        querySql.apply(em, jpql);

        List<Object[]> results = jpql.getResultList();

        Stream<Asset<?>> assetStream = results.stream().map(objArr -> {
            Asset<?> asset = (Asset<?>)objArr[0];

            if (objArr.length == 3) {
                // We have transient parent info
                String parentName = (String)objArr[1];
                String parentType = (String)objArr[2];
                try {
                    assetParentNameField.set(asset, parentName);
                    assetParentTypeField.set(asset, parentType);
                } catch (IllegalAccessException e) {
                    LOG.log(Level.WARNING, "Failed to set asset parent name and/or type fields", e);
                }
            }
            return asset;
        });

        if (containsCalendarPredicate) {
            assetStream = assetStream.filter(asset -> calendarEventPredicateMatches(timerService::getCurrentTimeMillis, query, asset));
        }

        return assetStream.collect(Collectors.toList());
    }

    protected boolean updateAttributeValue(EntityManager em, Asset<?> asset, Attribute<?> attribute) {

        try {

            // Detach the asset from the em so we can manually update the attribute
            em.detach(asset);
            String attributeName = attribute.getName();
            Object value = attribute.getValue();
            long timestamp = attribute.getTimestamp().orElseGet(timerService::getCurrentTimeMillis);

            return em.unwrap(Session.class).doReturningWork(connection -> {
                String jpql = "update Asset" +
                    " set attributes = jsonb_set(jsonb_set(attributes, ?, ?, true), ?, ?, true)" +
                    " where id = ? and attributes -> ? is not null";

                try (PreparedStatement statement = connection.prepareStatement(jpql)) {
                    Array attributeValuePath = connection.createArrayOf(
                        "text",
                        new String[]{attributeName, "value"}
                    );
                    statement.setArray(1, attributeValuePath);

                    PGobject pgJsonValue = new PGobject();
                    pgJsonValue.setType("jsonb");
                    // Careful, do not set Java null here! It will erase your whole SQL column!
                    pgJsonValue.setValue(ValueUtil.asJSON(value).orElse(ValueUtil.NULL_LITERAL));
                    statement.setObject(2, pgJsonValue);

                    // Bind the value timestamp
                    Array attributeValueTimestampPath = connection.createArrayOf(
                        "text",
                        new String[]{attributeName, "timestamp"}
                    );
                    statement.setArray(3, attributeValueTimestampPath);
                    PGobject pgJsonValueTimestamp = new PGobject();
                    pgJsonValueTimestamp.setType("jsonb");
                    pgJsonValueTimestamp.setValue(Long.toString(timestamp));
                    statement.setObject(4, pgJsonValueTimestamp);

                    // Bind asset ID and attribute name
                    statement.setString(5, asset.getId());
                    statement.setString(6, attributeName);

                    int updatedRows = statement.executeUpdate();
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("Stored asset '" + asset.getId()
                            + "' attribute '" + attributeName
                            + "' (affected rows: " + updatedRows + ") value: "
                            + (value != null ? ValueUtil.asJSON(value).orElse("null") : "null"));
                    }
                    return updatedRows == 1;
                }
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to store attribute value", e);
            return false;
        }
    }

    protected void publishModificationEvents(PersistenceEvent<Asset<?>> persistenceEvent) {
        Asset<?> asset = persistenceEvent.getEntity();
        switch (persistenceEvent.getCause()) {
            case CREATE:
                // Fully load the asset
                Asset<?> loadedAsset = find(new AssetQuery().ids(asset.getId()));

                if (loadedAsset == null) {
                    return;
                }

                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.CREATE, loadedAsset, null)
                );

//                // Raise attribute event for each attribute
//                asset.getAttributes().forEach(newAttribute ->
//                    clientEventService.publishEvent(
//                        new AttributeEvent(asset.getId(),
//                            newAttribute.getName(),
//                            newAttribute.getValue().orElse(null),
//                            newAttribute.getTimestamp().orElse(timerService.getCurrentTimeMillis()))
//                            .setParentId(asset.getParentId()).setRealm(asset.getRealm())
//                    ));
                break;
            case UPDATE:

                // Use simple equality check on each property
                String[] updatedProperties = Arrays.stream(persistenceEvent.getPropertyNames()).filter(propertyName -> {
                    Object oldValue = persistenceEvent.getPreviousState(propertyName);
                    Object newValue = persistenceEvent.getCurrentState(propertyName);
                    return !Objects.deepEquals(oldValue, newValue);
                }).toArray(String[]::new);

                // Fully load the asset
                loadedAsset = find(new AssetQuery().ids(asset.getId()));

                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.UPDATE, loadedAsset, updatedProperties)
                );

                // Did any attributes change if so raise attribute events on the event bus
                AttributeMap oldAttributes = persistenceEvent.getPreviousState("attributes");
                AttributeMap newAttributes = persistenceEvent.getCurrentState("attributes");

                // Get removed attributes and raise an attribute event with deleted flag in attribute state
                oldAttributes.stream()
                    .filter(oldAttribute ->
                        newAttributes.stream().noneMatch(newAttribute ->
                            oldAttribute.getName().equals(newAttribute.getName())
                    ))
                    .forEach(obsoleteAttribute ->
                        clientEventService.publishEvent(
                            AttributeEvent.deletedAttribute(asset.getId(), obsoleteAttribute.getName())
                    ));

                // Get new or modified attributes
                getAddedOrModifiedAttributes(oldAttributes.values(),
                    newAttributes.values())
                    .forEach(newOrModifiedAttribute ->
                        clientEventService.publishEvent(
                            new AttributeEvent(
                                asset.getId(),
                                newOrModifiedAttribute.getName(),
                                newOrModifiedAttribute.getValue().orElse(null),
                                newOrModifiedAttribute.getTimestamp().orElse(0L))
                                .setParentId(asset.getParentId()).setRealm(asset.getRealm())
                        ));
                break;
            case DELETE:

                clientEventService.publishEvent(
                    new AssetEvent(AssetEvent.Cause.DELETE, asset, null)
                );

                // Raise attribute event with deleted flag for each attribute
                AttributeMap deletedAttributes = asset.getAttributes();
                deletedAttributes.forEach(obsoleteAttribute ->
                    clientEventService.publishEvent(
                        AttributeEvent.deletedAttribute(asset.getId(), obsoleteAttribute.getName())
                    ));
                break;
        }
    }

    protected void replyWithAttributeEvents(String sessionKey, String subscriptionId, Asset<?> asset, String[] attributeNames) {
        List<String> names = attributeNames == null ? Collections.emptyList() : Arrays.asList(attributeNames);

        // Client may want to read a subset or all attributes of the asset
        List<AttributeEvent> events = asset.getAttributes().stream()
            .filter(attribute -> names.isEmpty() || names.contains(attribute.getName()))
            .map(attribute -> new AttributeEvent(new AttributeState(asset.getId(), attribute)))
            .collect(Collectors.toList());
        TriggeredEventSubscription<?> triggeredEventSubscription = new TriggeredEventSubscription<>(events, subscriptionId);
        clientEventService.sendToSession(sessionKey, triggeredEventSubscription);
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

        if (level == 3) {
            sb.append(", A.PARENT_NAME as PARENT_NAME, A.PARENT_TYPE as PARENT_TYPE");
        } else {
            if (select == null || !select.excludeParentInfo) {
                sb.append(", P.NAME as PARENT_NAME, P.TYPE as PARENT_TYPE");
            } else {
                sb.append(", NULL as PARENT_NAME, NULL as PARENT_TYPE");
            }
        }

        if (!query.recursive || level == 3) {
            if (select == null || !select.excludePath) {
                sb.append(", get_asset_tree_path(A.ID) as PATH");
            } else {
                sb.append(", NULL as PATH");
            }
        }

        if (select == null || !select.excludeAttributes) {
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
            binders.add((em, st) -> st.setParameter(pos, select.attributes, StringArrayType.INSTANCE));
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

            if (requiresParentJoin(query)) {
                sb.append("left outer join Asset P on A.PARENT_ID = P.id ");
            }
        } else if (level == 2) {
            sb.append(" from top_level_assets P ");
            sb.append("join Asset A on A.PARENT_ID = P.ID ");
        } else {
            sb.append(" from top_level_assets A ");
        }

        if ((!recursive || level == 3) && query.userIds != null && query.userIds.length > 0) {
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
                case CREATED_ON:
                    sb.append(" A.CREATED_ON ");
                    break;
                case ASSET_TYPE:
                    sb.append(" A.TYPE ");
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

    protected static String buildLimitString(AssetQuery query) {
        if (query.limit > 0) {
            return " LIMIT " + query.limit;
        }
        return "";
    }

    @SuppressWarnings("unchecked")
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

        if (level == 1 && query.ids != null && query.ids.length > 0) {
            final int pos = binders.size() + 1;
            sb.append(" and A.ID = ANY(?")
                .append(pos)
                .append(")");
            binders.add((em, st) -> st.setParameter(pos, query.ids, StringArrayType.INSTANCE));
        }

        if (level == 1 && query.names != null && query.names.length > 0) {

            sb.append(" and (");
            boolean isFirst = true;

            for (StringPredicate pred : query.names) {
                if (!isFirst) {
                    sb.append(" or ");
                }
                isFirst = false;
                final int pos = binders.size() + 1;
                sb.append(pred.caseSensitive ? "A.NAME " : "upper(A.NAME)");
                sb.append(buildMatchFilter(pred, pos));
                binders.add((em, st) -> st.setParameter(pos, pred.prepareValue()));
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
                    final int pos = binders.size() + 1;
                    sb.append("A.PARENT_ID = ?").append(pos);
                    binders.add((em, st) -> st.setParameter(pos, pred.id));
                } else if (level == 1 && pred.noParent) {
                    sb.append("A.PARENT_ID is null");
                } else if (pred.type != null || pred.name != null) {
                    if (pred.type != null) {
                        String[] resolvedTypes = getResolvedAssetTypes(new Class[]{pred.type});
                        final int pos = binders.size() + 1;
                        sb.append("P.TYPE = ANY(?")
                            .append(pos)
                            .append(")");
                        binders.add((em, st) -> st.setParameter(pos, resolvedTypes, StringArrayType.INSTANCE));
                    }
                    if (pred.name != null) {
                        if (pred.type != null) {
                            sb.append(" and ");
                        }
                        final int pos = binders.size() + 1;
                        sb.append("P.NAME = ?").append(pos);
                        binders.add((em, st) -> st.setParameter(pos, pred.name));
                    }
                } else {
                    sb.append("true");
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

                final int pos = binders.size() + 1;
                sb.append("?").append(pos).append(" <@ get_asset_tree_path(A.id)");
                binders.add((em, st) -> st.setParameter(pos, pred.path, StringArrayType.INSTANCE));
            }

            sb.append(")");
        }

        if (!recursive || level == 3) {
            if (query.tenant != null && !TextUtil.isNullOrEmpty(query.tenant.realm)) {
                final int pos = binders.size() + 1;
                sb.append(" and A.REALM = ?").append(pos);
                binders.add((em, st) -> st.setParameter(pos, query.tenant.realm));
            }

            if (query.userIds != null && query.userIds.length > 0) {
                final int pos = binders.size() + 1;
                sb.append(" and UA.USER_ID = ANY(?")
                    .append(pos)
                    .append(")");
                binders.add((em, st) -> st.setParameter(pos, query.userIds, StringArrayType.INSTANCE));
            }

            if (level == 1 && query.access == Access.PUBLIC) {
                sb.append(" and A.ACCESS_PUBLIC_READ is true");
            }

            if (query.types != null && query.types.length > 0) {
                String[] resolvedTypes = getResolvedAssetTypes(query.types);
                final int pos = binders.size() + 1;
                sb.append(" and A.TYPE = ANY(?")
                    .append(pos)
                    .append(")");
                binders.add((em, st) -> st.setParameter(pos, resolvedTypes, StringArrayType.INSTANCE));
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

    /**
     * Resolves the concrete {@link Asset} types that are covered by the supplied asset types
     */
    protected static String[] getResolvedAssetTypes(Class<? extends Asset<?>>[] assetClasses) {
        return Arrays.stream(assetClasses)
            .flatMap(assetClass ->
                Arrays.stream(ValueUtil.getAssetClasses(null)).filter(assetClass::isAssignableFrom))
            .map(Class::getSimpleName)
            .distinct()
            .toArray(String[]::new);
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

            if (nameValuePredicate instanceof AttributePredicate) {
                AttributePredicate attributePredicate = (AttributePredicate)nameValuePredicate;

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

    protected static boolean hasPathConstraint(PathPredicate[] pathPredicates) {
        if (pathPredicates == null || pathPredicates.length == 0) {
            return false;
        }

        return Arrays.stream(pathPredicates).anyMatch(p -> p.path != null);
    }

    protected static boolean requiresParentJoin(AssetQuery query) {
        ParentPredicate[] parentPredicates = query.parents;

        if (query.select == null || !query.select.excludeParentInfo) {
            return true;
        }

        if (parentPredicates == null || parentPredicates.length == 0) {
            return false;
        }

        return Arrays.stream(parentPredicates)
            .anyMatch(p -> !p.noParent && (p.type != null || p.name != null));
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
            attributeBuilder.append(buildMatchFilter(nameValuePredicate.name, pos));
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

            if (nameValuePredicate.path == null || nameValuePredicate.path.getPaths().length == 0) {
                valuePathInserter = (sb, b) ->
                    sb.append(isAttributePredicate ? "(" + jsonObjName + ".VALUE #> '{value}')" : jsonObjName + ".VALUE");
            } else {
                List<String> paths = new ArrayList<>();
                if (isAttributePredicate) {
                    paths.add("value");
                }
                paths.addAll(Arrays.stream(nameValuePredicate.path.getPaths()).map(Object::toString).collect(Collectors.toList()));

                valuePathInserter = (sb, b) -> {
                    final int pos = binders.size() + 1;
                    sb.append("(").append(jsonObjName).append(".VALUE #> ?").append(pos).append(")");
                    binders.add((em, st) -> st.setParameter(pos, paths.toArray(new String[0]), StringArrayType.INSTANCE));
                };
            }

            if (nameValuePredicate.value instanceof StringPredicate) {
                StringPredicate stringPredicate = (StringPredicate) nameValuePredicate.value;
                if (!stringPredicate.caseSensitive) {
                    attributeBuilder.append("upper(");
                }
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder.append(" #>> '{}'");
                if (!stringPredicate.caseSensitive) {
                    attributeBuilder.append(")");
                }
                final int pos = binders.size() + 1;
                attributeBuilder.append(buildMatchFilter(stringPredicate, pos));
                binders.add((em, st) -> st.setParameter(pos, stringPredicate.prepareValue()));
            } else if (nameValuePredicate.value instanceof BooleanPredicate) {
                BooleanPredicate booleanPredicate = (BooleanPredicate) nameValuePredicate.value;
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder
                    .append(" = to_jsonb(")
                    .append(booleanPredicate.value)
                    .append(")");
            } else if (nameValuePredicate.value instanceof DateTimePredicate) {
                DateTimePredicate dateTimePredicate = (DateTimePredicate) nameValuePredicate.value;
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
            } else if (nameValuePredicate.value instanceof NumberPredicate) {
                NumberPredicate numberPredicate = (NumberPredicate) nameValuePredicate.value;
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
            } else if (nameValuePredicate.value instanceof ArrayPredicate) {
                ArrayPredicate arrayPredicate = (ArrayPredicate) nameValuePredicate.value;
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
                if (nameValuePredicate.value instanceof RadialGeofencePredicate) {
                    RadialGeofencePredicate location = (RadialGeofencePredicate) nameValuePredicate.value;
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
                } else if (nameValuePredicate.value instanceof RectangularGeofencePredicate) {
                    RectangularGeofencePredicate location = (RectangularGeofencePredicate) nameValuePredicate.value;
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
                valuePathInserter.accept(attributeBuilder, binders);
                attributeBuilder.append(((ValueEmptyPredicate) nameValuePredicate.value).negate ? "\\:\\:text IS NOT NULL" : "\\:\\:text IS NULL");
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
            case EQUALS:
                if (negate) {
                    return " <> ?" + pos + " ";
                }
                return " = ?" + pos + " ";
            case GREATER_THAN:
                if (negate) {
                    return " <= ?" + pos + " ";
                }
                return " > ?" + pos + " ";
            case GREATER_EQUALS:
                if (negate) {
                    return " < ?" + pos + " ";
                }
                return " >= ?" + pos + " ";
            case LESS_THAN:
                if (negate) {
                    return " >= ?" + pos + " ";
                }
                return " < ?" + pos + " ";
            case LESS_EQUALS:
                if (negate) {
                    return " > ?" + pos + " ";
                }
                return " <= ?" + pos + " ";
            case BETWEEN:
                if (negate) {
                    return " NOT BETWEEN ?" + pos + " AND ?" + (pos+1) + " ";
                }
                return " BETWEEN ?" + pos + " AND ?" + (pos+1) + " ";
        }

        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }

    public static String buildMatchFilter(StringPredicate predicate, int pos) {
        switch (predicate.match) {
            case BEGIN:
            case END:
            case CONTAINS:
                if (predicate.negate) {
                    return " not like ?" + pos + " ";
                }
                return " like ?" + pos + " ";
            default:
                if (predicate.negate) {
                    return " <> ?" + pos + " ";
                }
                return " = ?" + pos + " ";
        }
    }
}
