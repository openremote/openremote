/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.rules;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.flow.FlowResourceImpl;
import org.openremote.manager.rules.geofence.GeofenceAssetAdapter;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.RulesetQuery;
import org.openremote.model.query.filter.LocationAttributePredicate;
import org.openremote.model.rules.*;
import org.openremote.model.rules.geofence.GeofenceDefinition;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Tenant;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.toList;
import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.concurrent.GlobalLock.withLockReturning;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;

/**
 * Manages {@link RulesEngine}s for stored {@link Ruleset}s and processes asset attribute updates.
 * <p>
 * If an updated attribute doesn't have meta {@link MetaItemType#RULE_STATE} is false and the attribute has an {@link
 * org.openremote.model.asset.agent.AgentLink} meta, this implementation of {@link AssetUpdateProcessor} converts the
 * update message to an {@link AssetState} fact. This service keeps the facts and thus the state of rule facts are in
 * sync with the asset state changes that occur. If an asset attribute value changes, the {@link AssetState} in the
 * rules engines will be updated to reflect the change.
 * <p>
 * If an updated attribute's {@link MetaItemType#RULE_EVENT} is true, another temporary {@link AssetState} fact is
 * inserted in the rules engines in scope. This fact expires automatically if the lifetime set in {@link
 * RulesService#RULE_EVENT_EXPIRES} is reached, or if the lifetime set in the attribute {@link
 * MetaItemType#RULE_EVENT_EXPIRES} is reached.
 * <p>
 * Each asset attribute update is processed in the following order:
 * <ol>
 * <li>Global Rulesets</li>
 * <li>Tenant Rulesets</li>
 * <li>Asset Rulesets (in hierarchical order from oldest ancestor down)</li>
 * </ol>
 * Processing order of rulesets with the same scope or same parent is not guaranteed.
 */
public class RulesService extends RouteBuilder implements ContainerService, AssetUpdateProcessor {

    public static final int PRIORITY = LOW_PRIORITY;
    public static final String RULE_EVENT_EXPIRES = "RULE_EVENT_EXPIRES";
    public static final String RULE_EVENT_EXPIRES_DEFAULT = "PT1H";
    private static final Logger LOG = Logger.getLogger(RulesService.class.getName());
    protected final Map<String, RulesEngine<TenantRuleset>> tenantEngines = new HashMap<>();
    protected final Map<String, RulesEngine<AssetRuleset>> assetEngines = new HashMap<>();
    protected List<GeofenceAssetAdapter> geofenceAssetAdapters = new ArrayList<>();
    protected TimerService timerService;
    protected ScheduledExecutorService executorService;
    protected PersistenceService persistenceService;
    protected RulesetStorageService rulesetStorageService;
    protected ManagerIdentityService identityService;
    protected AssetStorageService assetStorageService;
    protected NotificationService notificationService;
    protected AssetProcessingService assetProcessingService;
    protected AssetDatapointService assetDatapointService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;
    protected RulesEngine<GlobalRuleset> globalEngine;
    protected Tenant[] tenants;
    protected AssetLocationPredicateProcessor locationPredicateRulesConsumer;
    protected Map<RulesEngine<?>, List<RulesEngine.AssetStateLocationPredicates>> engineAssetLocationPredicateMap = new HashMap<>();
    protected Set<String> assetsWithModifiedLocationPredicates = new HashSet<>();
    // Keep global list of asset states that have been pushed to any engines
    // The objects are already in memory inside the rule engines but keeping them
    // here means we can quickly insert facts into newly started engines
    protected Set<AssetState<?>> assetStates = new HashSet<>();
    protected Set<AssetState<?>> preInitassetStates = new HashSet<>();
    protected String configEventExpires;
    protected boolean initDone;
    protected boolean startDone;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        executorService = container.getExecutorService();
        timerService = container.getService(TimerService.class);
        persistenceService = container.getService(PersistenceService.class);
        rulesetStorageService = container.getService(RulesetStorageService.class);
        identityService = container.getService(ManagerIdentityService.class);
        notificationService = container.getService(NotificationService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        clientEventService = container.getService(ClientEventService.class);
        gatewayService = container.getService(GatewayService.class);

        if (initDone) {
            return;
        }

        clientEventService.addSubscriptionAuthorizer((auth, subscription) -> {

            if (subscription.isEventType(RulesEngineStatusEvent.class) || subscription.isEventType(RulesetChangedEvent.class)) {

                if (auth.isSuperUser()) {
                    return true;
                }

                // Regular user must have role
                if (!auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), auth.getClientId())) {
                    return false;
                }

                boolean isRestrictedUser = identityService.getIdentityProvider().isRestrictedUser(auth);

                return !isRestrictedUser;
            }

            return false;
        });

        ServiceLoader.load(GeofenceAssetAdapter.class).forEach(geofenceAssetAdapter -> {
            LOG.fine("Adding GeofenceAssetAdapter: " + geofenceAssetAdapter.getClass().getName());
            geofenceAssetAdapters.add(geofenceAssetAdapter);
        });

        geofenceAssetAdapters.addAll(container.getServices(GeofenceAssetAdapter.class));
        geofenceAssetAdapters.sort(Comparator.comparingInt(GeofenceAssetAdapter::getPriority));
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        configEventExpires = getString(container.getConfig(), RULE_EVENT_EXPIRES, RULE_EVENT_EXPIRES_DEFAULT);

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new FlowResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class)
            )
        );

        initDone = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        // If any ruleset was modified in the database then check its' status and undeploy, deploy, or update it
        from(PERSISTENCE_TOPIC)
            .routeId("RulesetPersistenceChanges")
            .filter(isPersistenceEventForEntityType(Ruleset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                PersistenceEvent<?> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                processRulesetChange((Ruleset) persistenceEvent.getEntity(), persistenceEvent.getCause());
            });

        // If any tenant was modified in the database then check its' status and undeploy, deploy or update any
        // associated rulesets
        from(PERSISTENCE_TOPIC)
            .routeId("RuleEngineTenantChanges")
            .filter(isPersistenceEventForEntityType(Tenant.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                PersistenceEvent<?> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Tenant tenant = (Tenant) persistenceEvent.getEntity();
                processTenantChange(tenant, persistenceEvent.getCause());
            });

        // If any asset was modified in the database, detect changed attributes
        from(PERSISTENCE_TOPIC)
            .routeId("RuleEngineAssetChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                PersistenceEvent<Asset<?>> persistenceEvent = (PersistenceEvent<Asset<?>>) exchange.getIn().getBody(PersistenceEvent.class);
                final Asset<?> eventAsset = persistenceEvent.getEntity();
                processAssetChange(eventAsset, persistenceEvent);
            });
    }

    @Override
    public void start(Container container) throws Exception {

        if (!geofenceAssetAdapters.isEmpty()) {
            LOG.info("GeoefenceAssetAdapters found: " + geofenceAssetAdapters.size());
            locationPredicateRulesConsumer = this::onEngineLocationRulesChanged;

            for (GeofenceAssetAdapter geofenceAssetAdapter : geofenceAssetAdapters) {
                geofenceAssetAdapter.start(container);
            }
        }

        LOG.info("Deploying global rulesets");
        rulesetStorageService.findAll(
            GlobalRuleset.class,
            new RulesetQuery()
                .setEnabledOnly(true)
                .setFullyPopulate(true)
        ).forEach(this::deployGlobalRuleset);

        LOG.info("Deploying tenant rulesets");
        tenants = Arrays.stream(identityService.getIdentityProvider().getTenants()).filter(Tenant::getEnabled).toArray(Tenant[]::new);
        rulesetStorageService.findAll(
            TenantRuleset.class,
            new RulesetQuery()
                .setEnabledOnly(true)
                .setFullyPopulate(true))
            .stream()
            .filter(rd ->
                Arrays.stream(tenants)
                    .anyMatch(tenant -> rd.getRealm().equals(tenant.getRealm()))
            ).forEach(this::deployTenantRuleset);

        LOG.info("Deploying asset rulesets");
        // Group by asset ID then tenant and check tenant is enabled
        //noinspection ResultOfMethodCallIgnored
        deployAssetRulesets(
            rulesetStorageService.findAll(
                AssetRuleset.class,
                new RulesetQuery()
                    .setEnabledOnly(true)
                    .setFullyPopulate(true)))
            .count();//Needed in order to execute the stream. TODO: can this be done differently?

        LOG.info("Loading all assets with fact attributes to initialize state of rules engines");
        Stream<Pair<Asset<?>, Stream<Attribute<?>>>> stateAttributes = findRuleStateAttributes();

        // Push each attribute as an asset update through the rule engine chain
        // that will ensure the insert only happens to the engines in scope
        stateAttributes
            .forEach(pair -> {
                Asset<?> asset = pair.key;
                pair.value.forEach(ruleAttribute -> {
                    AssetState<?> assetState = new AssetState<>(asset, ruleAttribute, Source.INTERNAL);
                    updateAssetState(assetState);
                });
            });

        // Start the engines
        if (globalEngine != null) {
            globalEngine.start();
        }
        tenantEngines.values().forEach(RulesEngine::start);
        assetEngines.values().forEach(RulesEngine::start);

        startDone = true;

        preInitassetStates.forEach(this::doProcessAssetUpdate);
        preInitassetStates.clear();
    }

    @Override
    public void stop(Container container) throws Exception {
        withLock(getClass().getSimpleName() + "::stop", () -> {
            for (GeofenceAssetAdapter geofenceAssetAdapter : geofenceAssetAdapters) {
                try {
                    geofenceAssetAdapter.stop(container);
                } catch (Exception e) {
                    LOG.log(SEVERE, "Exception thrown whilst stopping geofence adapter", e);
                }
            }

            assetEngines.forEach((assetId, rulesEngine) -> rulesEngine.stop(true));
            assetEngines.clear();
            tenantEngines.forEach((realm, rulesEngine) -> rulesEngine.stop(true));
            tenantEngines.clear();

            if (globalEngine != null) {
                globalEngine.stop(true);
                globalEngine = null;
            }

            assetStates.clear();
        });

        for (GeofenceAssetAdapter geofenceAssetAdapter : geofenceAssetAdapters) {
            geofenceAssetAdapter.stop(container);
        }
    }

    protected static boolean attributeIsRuleState(Attribute<?> attribute) {
        return attribute.getMetaValue(MetaItemType.RULE_STATE).orElse(attribute.hasMeta(MetaItemType.AGENT_LINK));
    }

    @Override
    public boolean processAssetUpdate(EntityManager em,
                                      Asset<?> asset,
                                      Attribute<?> attribute,
                                      Source source) throws AssetProcessingException {
        if (!startDone) {
            preInitassetStates.add(new AssetState<>(asset, attribute, source));
        } else {
            doProcessAssetUpdate(new AssetState<>(asset, attribute, source));
        }

        return false;
    }

    protected void doProcessAssetUpdate(AssetState<?> assetState) {
        // We might process two facts for a single attribute update, if that is what the user wants

        // First as asset state
        if (assetState.getMetaValue(MetaItemType.RULE_STATE).orElse(assetState.hasMeta(MetaItemType.AGENT_LINK))) {
            updateAssetState(assetState);
        }

        // Then as asset event (if there wasn't an error), this will also fire the rules engines
        if (assetState.getMetaValue(MetaItemType.RULE_EVENT).orElse(false)) {
            insertAssetEvent(
                    assetState,
                    assetState.getMetaValue(MetaItemType.RULE_EVENT_EXPIRES).orElse(configEventExpires)
            );
        }
    }

    public boolean isRulesetKnown(Ruleset ruleset) {
        if (ruleset instanceof GlobalRuleset) {
            return globalEngine != null
                && globalEngine.deployments != null
                && globalEngine.deployments.containsKey(ruleset.getId())
                && globalEngine.deployments.get(ruleset.getId()).ruleset.getRules().equals(ruleset.getRules());
        }
        if (ruleset instanceof TenantRuleset) {
            TenantRuleset tenantRuleset = (TenantRuleset) ruleset;
            return tenantEngines.get(tenantRuleset.getRealm()) != null
                && tenantEngines.get(tenantRuleset.getRealm()).deployments != null
                && tenantEngines.get(tenantRuleset.getRealm()).deployments.containsKey(ruleset.getId())
                && tenantEngines.get(tenantRuleset.getRealm()).deployments.get(ruleset.getId()).ruleset.getRules().equals(ruleset.getRules());
        }
        if (ruleset instanceof AssetRuleset) {
            AssetRuleset assetRuleset = (AssetRuleset) ruleset;
            return assetEngines.get(assetRuleset.getAssetId()) != null
                && assetEngines.get(assetRuleset.getAssetId()).deployments != null
                && assetEngines.get(assetRuleset.getAssetId()).deployments.containsKey(ruleset.getId())
                && assetEngines.get(assetRuleset.getAssetId()).deployments.get(ruleset.getId()).ruleset.getRules().equals(ruleset.getRules());
        }
        return false;
    }

    public GeofenceDefinition[] getAssetGeofences(String assetId) {
        return withLockReturning(getClass().getSimpleName() + "::getAssetGeofences", () -> {

            LOG.finest("Requesting geofences for asset: " + assetId);

            for (GeofenceAssetAdapter geofenceAdapter : geofenceAssetAdapters) {
                GeofenceDefinition[] geofences = geofenceAdapter.getAssetGeofences(assetId);
                if (geofences != null) {
                    LOG.finest("Retrieved geofences from geofence adapter '" + geofenceAdapter.getName() + "' for asset: " + assetId);
                    return geofences;
                }
            }

            return new GeofenceDefinition[0];
        });
    }

    protected void processTenantChange(Tenant tenant, PersistenceEvent.Cause cause) {
        withLock(getClass().getSimpleName() + "::processTenantChange", () -> {
            // Check if enabled status has changed
            boolean wasEnabled = Arrays.stream(tenants).anyMatch(t -> tenant.getRealm().equals(t.getRealm()) && tenant.getId().equals(t.getId()));
            boolean isEnabled = tenant.getEnabled() && cause != PersistenceEvent.Cause.DELETE;
            tenants = Arrays.stream(identityService.getIdentityProvider().getTenants()).filter(Tenant::getEnabled).toArray(Tenant[]::new);

            if (wasEnabled == isEnabled) {
                // Nothing to do here
                return;
            }

            if (wasEnabled) {
                // Remove tenant rules engine for this tenant if it exists
                RulesEngine<TenantRuleset> tenantRulesEngine = tenantEngines.get(tenant.getRealm());
                if (tenantRulesEngine != null) {
                    tenantRulesEngine.stop();
                    tenantEngines.remove(tenant.getRealm());
                }

                // Remove any asset rules engines for assets in this realm
                assetEngines.values().stream()
                    .filter(re -> re.getId().getRealm().map(realm -> realm.equals(tenant.getRealm())).orElse(false))
                    .forEach(RulesEngine::stop);
                assetEngines.entrySet().removeIf(entry ->
                    entry.getValue().getId().getRealm().map(realm -> realm.equals(tenant.getRealm())).orElse(
                        false)
                );

            } else {
                // Create tenant rules engines for this tenant if it has any rulesets
                rulesetStorageService
                    .findAll(
                        TenantRuleset.class,
                        new RulesetQuery()
                            .setRealm(tenant.getRealm())
                            .setFullyPopulate(true)
                            .setEnabledOnly(true))
                    .stream()
                    .map(this::deployTenantRuleset)
                    .filter(Objects::nonNull)
                    .forEach(RulesEngine::start);

                // Create any asset rules engines for assets in this realm that have rulesets
                deployAssetRulesets(
                    rulesetStorageService.findAll(
                        AssetRuleset.class,
                        new RulesetQuery()
                            .setRealm(tenant.getRealm())
                            .setEnabledOnly(true)
                            .setFullyPopulate(true)))
                    .forEach(RulesEngine::start);
            }
        });
    }

    protected void processAssetChange(Asset<?> asset, PersistenceEvent<Asset<?>> persistenceEvent) {
        withLock(getClass().getSimpleName() + "::processAssetChange", () -> {

            // We must load the asset from database (only when required), as the
            // persistence event might not contain a completely loaded asset
            BiFunction<Asset<?>, Attribute<?>, AssetState<?>> buildAssetState = (loadedAsset, attribute) ->
                new AssetState<>(loadedAsset, ValueUtil.clone(attribute), Source.INTERNAL);

            switch (persistenceEvent.getCause()) {
                case CREATE: {
                    // New asset has been created so get attributes that don't have RULE_STATE=false meta
                    List<Attribute<?>> ruleStateAttributes = asset
                        .getAttributes()
                        .stream()
                        .filter(RulesService::attributeIsRuleState).collect(Collectors.toList());

                    // Asset<?> used to be loaded for each attribute which is inefficient
                    Asset<?> loadedAsset = ruleStateAttributes.isEmpty() ? null : assetStorageService.find(asset.getId(),
                        true);

                    // Build an update with a fully loaded asset
                    ruleStateAttributes.forEach(attribute -> {

                        // If the asset is now gone it was deleted immediately after being inserted, nothing more to do
                        if (loadedAsset == null)
                            return;

                        AssetState<?> assetState = buildAssetState.apply(loadedAsset, attribute);
                        LOG.finer("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + assetState);
                        updateAssetState(assetState);
                    });
                    break;
                }
                case UPDATE: {
                    int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
                    if (attributesIndex < 0) {
                        return;
                    }

                    // Fully load the asset to get parent and path info
                    Asset<?> loadedAsset = assetStorageService.find(asset.getId(), true);

                    // If the asset is now gone it was deleted immediately after being updated, nothing more to do
                    if (loadedAsset == null)
                        return;

                    List<Attribute<?>> oldStateAttributes = ((AttributeMap) persistenceEvent.getPreviousState("attributes"))
                        .stream()
                        .filter(RulesService::attributeIsRuleState)
                        .collect(toList());

                    List<Attribute<?>> newStateAttributes = ((AttributeMap) persistenceEvent.getCurrentState("attributes"))
                        .stream()
                        .filter(RulesService::attributeIsRuleState)
                        .collect(Collectors.toList());

                    // Retract obsolete or modified attributes
                    List<Attribute<?>> obsoleteOrModified = getAddedOrModifiedAttributes(newStateAttributes, oldStateAttributes).collect(toList());

                    obsoleteOrModified.forEach(attribute -> {
                        AssetState<?> assetState = buildAssetState.apply(loadedAsset, attribute);
                        LOG.finer("Asset was persisted (" + persistenceEvent.getCause() + "), retracting fact: " + assetState);
                        retractAssetState(assetState);
                    });

                    // Insert new or modified attributes
                    newStateAttributes.stream().filter(attr ->
                        !oldStateAttributes.contains(attr) || obsoleteOrModified.contains(attr))
                        .forEach(attribute -> {
                            AssetState<?> assetState = buildAssetState.apply(loadedAsset, attribute);
                            LOG.finer("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + assetState);
                            updateAssetState(assetState);
                        });
                    break;
                }
                case DELETE:
                    // Retract any facts that were associated with this asset
                    asset.getAttributes().stream()
                        .filter(RulesService::attributeIsRuleState)
                        .forEach(attribute -> {
                            AssetState<?> assetState = new AssetState<>(asset, attribute, Source.INTERNAL);
                            LOG.finer("Asset was persisted (" + persistenceEvent.getCause() + "), retracting fact: " + assetState);
                            retractAssetState(assetState);
                        });
                    break;
            }
        });
    }

    protected void processRulesetChange(Ruleset ruleset, PersistenceEvent.Cause cause) {
        withLock(getClass().getSimpleName() + "::processRulesetChange", () -> {
            if (cause == PersistenceEvent.Cause.DELETE || !ruleset.isEnabled()) {
                if (ruleset instanceof GlobalRuleset) {
                    undeployGlobalRuleset((GlobalRuleset) ruleset);
                } else if (ruleset instanceof TenantRuleset) {
                    undeployTenantRuleset((TenantRuleset) ruleset);
                } else if (ruleset instanceof AssetRuleset) {
                    undeployAssetRuleset((AssetRuleset) ruleset);
                }
            } else {
                if (ruleset instanceof GlobalRuleset) {

                    boolean isNewEngine = globalEngine == null;
                    RulesEngine<GlobalRuleset> engine = deployGlobalRuleset((GlobalRuleset) ruleset);

                    if (isNewEngine) {
                        // Push all existing facts into the engine
                        assetStates.forEach(assetState -> engine.updateOrInsertAssetState(assetState, true));
                    }

                    engine.start();

                } else if (ruleset instanceof TenantRuleset) {

                    boolean isNewEngine = !tenantEngines.containsKey(((TenantRuleset) ruleset).getRealm());
                    RulesEngine<TenantRuleset> engine = deployTenantRuleset((TenantRuleset) ruleset);

                    if (isNewEngine) {
                        // Push all existing facts into the engine
                        assetStates.forEach(assetState -> {
                            if (assetState.getRealm().equals(((TenantRuleset) ruleset).getRealm())) {
                                engine.updateOrInsertAssetState(assetState, true);
                            }
                        });
                    }

                    engine.start();

                } else if (ruleset instanceof AssetRuleset) {

                    // Must reload from the database, the ruleset might not be completely hydrated on CREATE or UPDATE
                    AssetRuleset assetRuleset = rulesetStorageService.find(AssetRuleset.class, ruleset.getId());
                    boolean isNewEngine = !assetEngines.containsKey(((AssetRuleset) ruleset).getAssetId());
                    RulesEngine<AssetRuleset> engine = deployAssetRuleset(assetRuleset);

                    if (isNewEngine) {
                        // Push all existing facts for this asset (and it's children into the engine)
                        getAssetStatesInScope(((AssetRuleset) ruleset).getAssetId())
                            .forEach(assetState -> engine.updateOrInsertAssetState(assetState, true));
                    }

                    engine.start();
                }
            }
        });
    }

    /**
     * Deploy the ruleset into the global engine creating the engine if necessary.
     */
    protected RulesEngine<GlobalRuleset> deployGlobalRuleset(GlobalRuleset ruleset) {
        return withLockReturning(getClass().getSimpleName() + "::deployGlobalRuleset", () -> {

            // Global rules have access to everything in the system
            if (globalEngine == null) {
                globalEngine = new RulesEngine<>(
                    timerService,
                    identityService,
                    executorService,
                    assetStorageService,
                    assetProcessingService,
                    notificationService,
                    clientEventService,
                    assetDatapointService,
                    assetPredictedDatapointService,
                    new RulesEngineId<>(),
                    locationPredicateRulesConsumer
                );
            }

            globalEngine.addRuleset(ruleset);

            return globalEngine;
        });
    }

    protected void undeployGlobalRuleset(GlobalRuleset ruleset) {
        withLock(getClass().getSimpleName() + "::undeployGlobalRuleset", () -> {
            if (globalEngine == null) {
                return;
            }

            if (globalEngine.removeRuleset(ruleset)) {
                globalEngine.stop();
                globalEngine = null;
            }
        });
    }

    protected RulesEngine<TenantRuleset> deployTenantRuleset(TenantRuleset ruleset) {
        return withLockReturning(getClass().getSimpleName() + "::deployTenantRuleset", () -> {


            // Look for existing rules engines for this tenant
            RulesEngine<TenantRuleset> tenantRulesEngine = tenantEngines
                .computeIfAbsent(ruleset.getRealm(), (realm) ->
                    new RulesEngine<>(
                        timerService,
                        identityService,
                        executorService,
                        assetStorageService,
                        assetProcessingService,
                        notificationService,
                        clientEventService,
                        assetDatapointService,
                        assetPredictedDatapointService,
                        new RulesEngineId<>(realm),
                        locationPredicateRulesConsumer
                    ));

            tenantRulesEngine.addRuleset(ruleset);

            return tenantRulesEngine;
        });
    }

    protected void undeployTenantRuleset(TenantRuleset ruleset) {
        withLock(getClass().getSimpleName() + "::undeployTenantRuleset", () -> {
            RulesEngine<TenantRuleset> rulesEngine = tenantEngines.get(ruleset.getRealm());
            if (rulesEngine == null) {
                return;
            }

            if (rulesEngine.removeRuleset(ruleset)) {
                rulesEngine.stop();
                tenantEngines.remove(ruleset.getRealm());
            }
        });
    }

    protected Stream<RulesEngine<AssetRuleset>> deployAssetRulesets(List<AssetRuleset> rulesets) {
        return rulesets
            .stream()
            .collect(Collectors.groupingBy(AssetRuleset::getAssetId))
            .entrySet()
            .stream()
            .map(es ->
                new Pair<Asset<?>, List<AssetRuleset>>(assetStorageService.find(es.getKey(), true), es.getValue())
            )
            .filter(assetAndRules -> assetAndRules.key != null)
            .collect(Collectors.groupingBy(assetAndRules -> assetAndRules.key.getRealm()))
            .entrySet()
            .stream()
            .filter(es -> {
                try {
                    return Arrays
                        .stream(tenants)
                        .anyMatch(at -> es.getKey().equals(at.getRealm()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            })
            .flatMap(es -> {
                List<Pair<Asset<?>, List<AssetRuleset>>> tenantAssetAndRules = es.getValue();

                // RT: Not sure we need ordering here for starting engines so removing it
                // Order rulesets by asset hierarchy within this tenant
                return tenantAssetAndRules.stream()
                    //.sorted(Comparator.comparingInt(item -> item.key.getPath().length))
                    .flatMap(assetAndRules -> assetAndRules.value.stream())
                    .map(this::deployAssetRuleset);
            });
    }

    protected RulesEngine<AssetRuleset> deployAssetRuleset(AssetRuleset ruleset) {
        return withLockReturning(getClass().getSimpleName() + "::deployAssetRuleset", () -> {

            // Look for existing rules engine for this asset
            RulesEngine<AssetRuleset> assetRulesEngine = assetEngines
                .computeIfAbsent(ruleset.getAssetId(), (assetId) ->
                    new RulesEngine<>(
                        timerService,
                        identityService,
                        executorService,
                        assetStorageService,
                        assetProcessingService,
                        notificationService,
                        clientEventService,
                        assetDatapointService,
                        assetPredictedDatapointService,
                        new RulesEngineId<>(ruleset.getRealm(), assetId),
                        locationPredicateRulesConsumer
                    ));

            assetRulesEngine.addRuleset(ruleset);

            return assetRulesEngine;
        });
    }

    protected void undeployAssetRuleset(AssetRuleset ruleset) {
        withLock(getClass().getSimpleName() + "::undeployAssetRuleset", () -> {
            RulesEngine<AssetRuleset> rulesEngine = assetEngines.get(ruleset.getAssetId());
            if (rulesEngine == null) {
                return;
            }

            if (rulesEngine.removeRuleset(ruleset)) {
                rulesEngine.stop();
                assetEngines.remove(ruleset.getAssetId());
            }
        });
    }

    protected void insertAssetEvent(AssetState<?> assetState, String expires) {
        withLock(getClass().getSimpleName() + "::insertAssetEvent", () -> {
            // Get the chain of rule engines that we need to pass through
            List<RulesEngine<?>> rulesEngines = getEnginesInScope(assetState.getRealm(), assetState.getPath());

            // Check that all engines in the scope are available
            if (rulesEngines.stream().anyMatch(RulesEngine::isError)) {
                LOG.severe("At least one rules engine is in an error state, skipping: " + assetState);
                if (LOG.isLoggable(FINEST)) {
                    for (RulesEngine<?> rulesEngine : rulesEngines) {
                        if (rulesEngine.isError()) {
                            LOG.log(FINEST, "Rules engine error state: " + rulesEngine, rulesEngine.getError());
                        }
                    }
                }
                return;
            }

            // Pass through each engine
            for (RulesEngine<?> rulesEngine : rulesEngines) {
                rulesEngine.insertAssetEvent(expires, assetState);
            }
        });
    }

    protected void updateAssetState(AssetState<?> assetState) {
        withLock(getClass().getSimpleName() + "::updateAssetState", () -> {
            // TODO: implement rules processing error state handling

            LOG.finer("Updating asset state: " + assetState);

            // Get the chain of rule engines that we need to pass through
            List<RulesEngine<?>> rulesEngines = getEnginesInScope(assetState.getRealm(), assetState.getPath());

            // Remove asset state with same attribute ref as new state, add new state
            boolean inserted = !assetStates.remove(assetState);
            assetStates.add(assetState);

            // Pass through each rules engine
            for (RulesEngine<?> rulesEngine : rulesEngines) {
                rulesEngine.updateOrInsertAssetState(assetState, inserted);
            }
        });
    }

    protected void retractAssetState(AssetState<?> assetState) {
        // Get the chain of rule engines that we need to pass through
        List<RulesEngine<?>> rulesEngines = getEnginesInScope(assetState.getRealm(), assetState.getPath());

        // Remove asset state with same attribute ref
        assetStates.remove(assetState);

        if (rulesEngines.size() == 0) {
            LOG.finer("Ignoring as there are no matching rules engines: " + assetState);
        }

        // Pass through each rules engine
        for (RulesEngine<?> rulesEngine : rulesEngines) {
            rulesEngine.removeAssetState(assetState);
        }
    }

    protected List<AssetState<?>> getAssetStatesInScope(String assetId) {
        return withLockReturning(getClass().getSimpleName() + "::getAssetStatesInScope", () ->
                assetStates
                .stream()
                .filter(assetState -> Arrays.asList(assetState.getPath()).contains(assetId))
                .collect(Collectors.toList()));
    }

    protected List<RulesEngine<?>> getEnginesInScope(String realm, String[] assetPath) {
        List<RulesEngine<?>> rulesEngines = new ArrayList<>();

        // Add global engine (if it exists)
        if (globalEngine != null) {
            rulesEngines.add(globalEngine);
        }

        // Add tenant engine (if it exists)
        RulesEngine<?> tenantRulesEngine = tenantEngines.get(realm);

        if (tenantRulesEngine != null) {
            rulesEngines.add(tenantRulesEngine);
        }

        // Add asset engines, iterate through asset hierarchy using asset IDs from asset path
        for (String assetId : assetPath) {
            RulesEngine<?> assetRulesEngine = assetEngines.get(assetId);
            if (assetRulesEngine != null) {
                rulesEngines.add(assetRulesEngine);
            }
        }

        return rulesEngines;
    }

    protected Stream<Pair<Asset<?>, Stream<Attribute<?>>>> findRuleStateAttributes() {
        // Get all assets then filter out any attributes with RULE_STATE=false
        List<Asset<?>> assets = assetStorageService.findAll(new AssetQuery());

        return assets.stream()
            .map(asset ->
                new Pair<>(asset, asset.getAttributes().stream()
                    .filter(RulesService::attributeIsRuleState))
            );
    }

    /**
     * Called when an engine's rules change identifying assets with location attributes that also have {@link
     * LocationAttributePredicate} in the rules. The job here is to identify the asset's (via {@link AssetState}) that
     * have modified {@link LocationAttributePredicate}s and to notify the {@link GeofenceAssetAdapter}s.
     */
    protected void onEngineLocationRulesChanged(RulesEngine<?> rulesEngine, List<RulesEngine.AssetStateLocationPredicates> newEngineAssetStateLocationPredicates) {
        withLock(getClass().getSimpleName() + "::onEngineLocationRulesChanged", () -> {
            int initialModifiedCount = assetsWithModifiedLocationPredicates.size();

            if (newEngineAssetStateLocationPredicates == null) {
                engineAssetLocationPredicateMap.computeIfPresent(rulesEngine,
                    (re, existingAssetStateLocationPredicates) -> {
                        // All location predicates have been removed so record each asset state as modified
                        assetsWithModifiedLocationPredicates.addAll(
                            existingAssetStateLocationPredicates.stream().map(
                                RulesEngine.AssetStateLocationPredicates::getAssetId).collect(
                                Collectors.toList()));
                        // Remove this engine from the map
                        return null;
                    });
            } else {
                engineAssetLocationPredicateMap.compute(rulesEngine,
                    (re, existingEngineAssetStateLocationPredicates) -> {
                        // Check if this not the first time this engine has been seen with location predicates so we can check
                        // for any removed asset states
                        if (existingEngineAssetStateLocationPredicates == null) {
                            // All asset states are new so record them all as modified
                            assetsWithModifiedLocationPredicates.addAll(
                                newEngineAssetStateLocationPredicates.stream().map(
                                    RulesEngine.AssetStateLocationPredicates::getAssetId).collect(
                                    Collectors.toList()));
                        } else {
                            // Find obsolete and modified asset states
                            existingEngineAssetStateLocationPredicates.forEach(
                                existingAssetStateLocationPredicates -> {
                                    // Check if there are no longer any location predicates for this asset
                                    Optional<RulesEngine.AssetStateLocationPredicates> newAssetStateLocationPredicates = newEngineAssetStateLocationPredicates.stream()
                                        .filter(assetStateLocationPredicates ->
                                            assetStateLocationPredicates.getAssetId().equals(
                                                existingAssetStateLocationPredicates.getAssetId()))
                                        .findFirst();

                                    if (newAssetStateLocationPredicates.isPresent()) {
                                        // Compare existing and new location predicate sets if there is any change then record it
                                        if (!newAssetStateLocationPredicates.get().getLocationPredicates().equals(
                                            existingAssetStateLocationPredicates.getLocationPredicates())) {
                                            assetsWithModifiedLocationPredicates.add(
                                                existingAssetStateLocationPredicates.getAssetId());
                                        }
                                    } else {
                                        // This means that there are no longer any location predicates so old ones are obsolete
                                        assetsWithModifiedLocationPredicates.add(
                                            existingAssetStateLocationPredicates.getAssetId());
                                    }
                                });

                            // Check for asset states in the new map but not in the old one
                            newEngineAssetStateLocationPredicates.forEach(
                                newAssetStateLocationPredicates -> {
                                    boolean isNewAssetState = existingEngineAssetStateLocationPredicates.stream()
                                        .noneMatch(assetStateLocationPredicates ->
                                            assetStateLocationPredicates.getAssetId().equals(
                                                newAssetStateLocationPredicates.getAssetId()));

                                    if (isNewAssetState) {
                                        // This means that all predicates for this asset are new
                                        assetsWithModifiedLocationPredicates.add(
                                            newAssetStateLocationPredicates.getAssetId());
                                    }
                                });
                        }
                        return newEngineAssetStateLocationPredicates;
                    });
            }

            if (assetsWithModifiedLocationPredicates.size() != initialModifiedCount) {
                processModifiedGeofences();
            }
        });
    }

    protected void processModifiedGeofences() {
        withLock(getClass().getSimpleName() + "::processModifiedGeofences", () -> {
            LOG.finest("Processing geofence modifications: modified asset geofence count=" + assetsWithModifiedLocationPredicates.size());

            try {
                // Find all location predicates associated with modified assets and pass through to the geofence adapters
                List<RulesEngine.AssetStateLocationPredicates> assetLocationPredicates = new ArrayList<>(
                    assetsWithModifiedLocationPredicates.size());

                assetsWithModifiedLocationPredicates.forEach(assetId -> {

                    RulesEngine.AssetStateLocationPredicates locationPredicates = new RulesEngine.AssetStateLocationPredicates(
                        assetId,
                        new HashSet<>());

                    engineAssetLocationPredicateMap.forEach((rulesEngine, engineAssetStateLocationPredicates) ->
                        engineAssetStateLocationPredicates.stream().filter(
                            assetStateLocationPredicates ->
                                assetStateLocationPredicates.getAssetId().equals(
                                    assetId))
                            .findFirst()
                            .ifPresent(
                                assetStateLocationPredicate -> {
                                    locationPredicates.getLocationPredicates().addAll(
                                        assetStateLocationPredicate.getLocationPredicates());
                                }));

                    assetLocationPredicates.add(locationPredicates);
                });

                for (GeofenceAssetAdapter geofenceAssetAdapter : geofenceAssetAdapters) {
                    LOG.finest("Passing modified geofences to adapter: " + geofenceAssetAdapter.getName());
                    geofenceAssetAdapter.processLocationPredicates(assetLocationPredicates);

                    if (assetLocationPredicates.isEmpty()) {
                        LOG.finest("All modified geofences handled");
                        break;
                    }
                }

            } catch (Exception e) {
                LOG.log(SEVERE, "Exception thrown by geofence adapter whilst processing location predicates", e);
            } finally {
                // Clear modified assets ready for next batch
                assetsWithModifiedLocationPredicates.clear();
            }
        });
    }

    protected Optional<RulesetDeployment> getRulesetDeployment(Long rulesetId) {
        if (globalEngine != null) {
            if (globalEngine.deployments.containsKey(rulesetId)) {
                return Optional.of(globalEngine.deployments.get(rulesetId));
            }
        }

        for (Map.Entry<String, RulesEngine<TenantRuleset>> realmAndEngine : tenantEngines.entrySet()) {
            if (realmAndEngine.getValue().deployments.containsKey(rulesetId)) {
                return Optional.of(realmAndEngine.getValue().deployments.get(rulesetId));
            }
        }

        for (Map.Entry<String, RulesEngine<AssetRuleset>> realmAndEngine : assetEngines.entrySet()) {
            if (realmAndEngine.getValue().deployments.containsKey(rulesetId)) {
                return Optional.of(realmAndEngine.getValue().deployments.get(rulesetId));
            }
        }

        return Optional.empty();
    }

    /**
     * Trigger rules engines which have the {@link org.openremote.model.value.MetaItemDescriptor} {@link org.openremote.model.rules.Ruleset#TRIGGER_ON_PREDICTED_DATA}
     * and contain {@link AssetState} of the specified asset id. Use this when {@link PredictedDatapoints} has changed for this asset.
     * @param assetId of the asset which has new predicated data points.
     */
    public void fireDeploymentsWithPredictedDataForAsset(String assetId) {
        List<AssetState<?>> assetStates = getAssetStatesInScope(assetId);
        if (assetStates.size() > 0) {
            String realm = assetStates.get(0).getRealm();
            String[] assetPaths = assetStates.stream().flatMap(assetState -> Arrays.stream(assetState.getPath())).toArray(String[]::new);
            for (RulesEngine<?> rulesEngine : getEnginesInScope(realm, assetPaths)) {
                rulesEngine.fireAllDeploymentsWithPredictedData();
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
