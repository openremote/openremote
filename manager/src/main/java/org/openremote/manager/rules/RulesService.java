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

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
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
import org.openremote.manager.webhook.WebhookService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
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
import org.openremote.model.security.Realm;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.value.MetaHolder;
import org.openremote.model.value.MetaItemType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.logging.Level.SEVERE;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;

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
 * RulesService#OR_RULE_EVENT_EXPIRES} is reached, or if the lifetime set in the attribute {@link
 * MetaItemType#RULE_EVENT_EXPIRES} is reached.
 * <p>
 * Each asset attribute update is processed in the following order:
 * <ol>
 * <li>Global Rulesets</li>
 * <li>Realm Rulesets</li>
 * <li>Asset Rulesets (in hierarchical order from oldest ancestor down)</li>
 * </ol>
 * Processing order of rulesets with the same scope or same parent is not guaranteed.
 */
public class RulesService extends RouteBuilder implements ContainerService, AssetUpdateProcessor {

    public static final int PRIORITY = LOW_PRIORITY;
    public static final String OR_RULE_EVENT_EXPIRES = "OR_RULE_EVENT_EXPIRES";
    public static final String OR_RULE_EVENT_EXPIRES_DEFAULT = "PT1H";
    /**
     * This value defines the periodic firing of the rules engines, and therefore
     * has an impact on system load. If a temporary fact has a shorter expiration
     * time, it's not guaranteed to be removed within that time. Any time-based
     * operation, such as matching temporary facts in a sliding time window, must
     * be designed with this margin in mind.
     */
    public static final String OR_RULES_MIN_TEMP_FACT_EXPIRATION_MILLIS = "OR_RULES_MIN_TEMP_FACT_EXPIRATION_MILLIS";
    public static final int OR_RULES_MIN_TEMP_FACT_EXPIRATION_MILLIS_DEFAULT = 50000; // Just under a minute to catch 1 min timer rules
    public static final String OR_RULES_QUICK_FIRE_MILLIS = "OR_RULES_QUICK_FIRE_MILLIS";
    public static final int OR_RULES_QUICK_FIRE_MILLIS_DEFAULT = 3000;
    private static final Logger LOG = Logger.getLogger(RulesService.class.getName());
    protected final Map<String, RulesEngine<RealmRuleset>> realmEngines = new ConcurrentHashMap<>();
    protected final Map<String, RulesEngine<AssetRuleset>> assetEngines = new ConcurrentHashMap<>();
    protected List<GeofenceAssetAdapter> geofenceAssetAdapters = new ArrayList<>();
    protected TimerService timerService;
    protected ScheduledExecutorService executorService;
    protected PersistenceService persistenceService;
    protected RulesetStorageService rulesetStorageService;
    protected ManagerIdentityService identityService;
    protected AssetStorageService assetStorageService;
    protected NotificationService notificationService;
    protected WebhookService webhookService;
    protected AssetProcessingService assetProcessingService;
    protected AssetDatapointService assetDatapointService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;
    protected RulesEngine<GlobalRuleset> globalEngine;
    protected Realm[] realms;
    protected AssetLocationPredicateProcessor locationPredicateRulesConsumer;
    protected final ConcurrentMap<RulesEngine<?>, List<RulesEngine.AssetStateLocationPredicates>> engineAssetLocationPredicateMap = new ConcurrentHashMap<>();
    protected final Set<String> assetsWithModifiedLocationPredicates = new HashSet<>();
    // Keep global list of asset states that have been pushed to any engines
    // The objects are already in memory inside the rule engines but keeping them
    // here means we can quickly insert facts into newly started engines
    protected final Set<AssetState<?>> assetStates = new HashSet<>();
    protected final Set<AssetState<?>> preInitAssetStates = new HashSet<>();
    protected long defaultEventExpiresMillis = 1000*60*60;
    protected long tempFactExpirationMillis;
    protected long quickFireMillis;
    protected boolean initDone;
    protected boolean startDone;
    protected MeterRegistry meterRegistry;

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
        webhookService = container.getService(WebhookService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        clientEventService = container.getService(ClientEventService.class);
        gatewayService = container.getService(GatewayService.class);

        tempFactExpirationMillis = getInteger(container.getConfig(), OR_RULES_MIN_TEMP_FACT_EXPIRATION_MILLIS, OR_RULES_MIN_TEMP_FACT_EXPIRATION_MILLIS_DEFAULT);
        quickFireMillis = getInteger(container.getConfig(), OR_RULES_QUICK_FIRE_MILLIS, OR_RULES_QUICK_FIRE_MILLIS_DEFAULT);

        if (initDone) {
            return;
        }

        meterRegistry = container.getMeterRegistry();

        clientEventService.addSubscriptionAuthorizer((realm, auth, subscription) -> {

            if (subscription.isEventType(RulesEngineStatusEvent.class) || subscription.isEventType(RulesetChangedEvent.class)) {

                if (auth == null) {
                    return false;
                }

                if (auth.isSuperUser()) {
                    return true;
                }

                // Regular user must have role
                if (!auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
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
        String defaultEventExpires = getString(container.getConfig(), OR_RULE_EVENT_EXPIRES, OR_RULE_EVENT_EXPIRES_DEFAULT);

        if (!TextUtil.isNullOrEmpty(defaultEventExpires)) {
            try {
                defaultEventExpiresMillis = TimeUtil.parseTimeDuration(defaultEventExpires);
            } catch (RuntimeException exception) {
                LOG.log(Level.WARNING, "Failed to parse " + OR_RULE_EVENT_EXPIRES, exception);
            }
        }

        container.getService(ManagerWebService.class).addApiSingleton(
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
            .routeId("Persistence-Ruleset")
            .filter(isPersistenceEventForEntityType(Ruleset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                PersistenceEvent<?> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                processRulesetChange((Ruleset) persistenceEvent.getEntity(), persistenceEvent.getCause());
            });

        // If any realm was modified in the database then check its status and undeploy, deploy or update any
        // associated rulesets
        from(PERSISTENCE_TOPIC)
            .routeId("Persistence-RulesRealm")
            .filter(isPersistenceEventForEntityType(Realm.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                PersistenceEvent<?> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Realm realm = (Realm) persistenceEvent.getEntity();
                processRealmChange(realm, persistenceEvent.getCause());
            });

        // If any asset was modified in the database, detect changed attributes
        from(PERSISTENCE_TOPIC)
            .routeId("Persistence-RulesAsset")
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
            LOG.fine("GeoefenceAssetAdapters found: " + geofenceAssetAdapters.size());
            locationPredicateRulesConsumer = this::onEngineLocationRulesChanged;

            for (GeofenceAssetAdapter geofenceAssetAdapter : geofenceAssetAdapters) {
                geofenceAssetAdapter.start(container);
            }
        }

        LOG.fine("Deploying global rulesets");
        rulesetStorageService.findAll(
            GlobalRuleset.class,
            new RulesetQuery()
                .setEnabledOnly(true)
                .setFullyPopulate(true)
        ).forEach(this::deployGlobalRuleset);

        LOG.fine("Deploying realm rulesets");
        realms = Arrays.stream(identityService.getIdentityProvider().getRealms()).filter(Realm::getEnabled).toArray(Realm[]::new);
        rulesetStorageService.findAll(
            RealmRuleset.class,
            new RulesetQuery()
                .setEnabledOnly(true)
                .setFullyPopulate(true))
            .stream()
            .filter(rd ->
                Arrays.stream(realms)
                    .anyMatch(realm -> rd.getRealm().equals(realm.getName()))
            ).forEach(this::deployRealmRuleset);

        LOG.fine("Deploying asset rulesets");
        // Group by asset ID then realm and check realm is enabled
        //noinspection ResultOfMethodCallIgnored
        deployAssetRulesets(
            rulesetStorageService.findAll(
                AssetRuleset.class,
                new RulesetQuery()
                    .setEnabledOnly(true)
                    .setFullyPopulate(true)))
            .count();//Needed in order to execute the stream. TODO: can this be done differently?

        LOG.fine("Loading all assets with fact attributes to initialize state of rules engines");
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
        realmEngines.values().forEach(RulesEngine::start);
        assetEngines.values().forEach(RulesEngine::start);

        startDone = true;

        preInitAssetStates.forEach(this::doProcessAssetUpdate);
        preInitAssetStates.clear();
    }

    @Override
    public void stop(Container container) throws Exception {
        for (GeofenceAssetAdapter geofenceAssetAdapter : geofenceAssetAdapters) {
            try {
                geofenceAssetAdapter.stop(container);
            } catch (Exception e) {
                LOG.log(SEVERE, "Exception thrown whilst stopping geofence adapter", e);
            }
        }

        assetEngines.forEach((assetId, rulesEngine) -> rulesEngine.stop());
        assetEngines.clear();
        realmEngines.forEach((realm, rulesEngine) -> rulesEngine.stop());
        realmEngines.clear();

        if (globalEngine != null) {
            globalEngine.stop();
            globalEngine = null;
        }

        synchronized (assetStates) {
            assetStates.clear();
        }

        for (GeofenceAssetAdapter geofenceAssetAdapter : geofenceAssetAdapters) {
            geofenceAssetAdapter.stop(container);
        }
    }

    protected static boolean isRuleState(MetaHolder metaHolder) {
        if (metaHolder.getMeta() == null) {
            return false;
        }

        return metaHolder.getMeta().getValue(MetaItemType.RULE_STATE)
            .orElse(metaHolder.getMeta().has(MetaItemType.AGENT_LINK));
    }

    @Override
    public boolean processAssetUpdate(EntityManager em,
                                      Asset<?> asset,
                                      Attribute<?> attribute,
                                      Source source) throws AssetProcessingException {
        if (!startDone) {
            preInitAssetStates.add(new AssetState<>(asset, attribute, source));
        } else {
            doProcessAssetUpdate(new AssetState<>(asset, attribute, source));
        }

        return false;
    }

    protected void doProcessAssetUpdate(AssetState<?> assetState) {
        // We might process two facts for a single attribute update, if that is what the user wants

        // First as asset state
        if (isRuleState(assetState)) {
            updateAssetState(assetState);
        }

        // Then as asset event (if there wasn't an error), this will also fire the rules engines
        if (assetState.getMetaValue(MetaItemType.RULE_EVENT).orElse(false)) {

            long expireMillis = assetState.getMetaValue(MetaItemType.RULE_EVENT_EXPIRES).map(expires -> {
                long expMillis = defaultEventExpiresMillis;

                try {
                    expMillis = TimeUtil.parseTimeDuration(expires);
                } catch (RuntimeException exception) {
                    LOG.log(Level.WARNING, "Failed to parse '" + MetaItemType.RULE_EVENT_EXPIRES.getName() + "' value '" + expires + "' for attribute: " + assetState, exception);
                }
                return expMillis;
            }).orElse(defaultEventExpiresMillis);

            insertAssetEvent(assetState, expireMillis);
        }
    }

    public boolean isRulesetKnown(Ruleset ruleset) {
        if (ruleset instanceof GlobalRuleset) {
            return globalEngine != null
                && globalEngine.deployments != null
                && globalEngine.deployments.containsKey(ruleset.getId())
                && globalEngine.deployments.get(ruleset.getId()).ruleset.getRules().equals(ruleset.getRules());
        }
        if (ruleset instanceof RealmRuleset) {
            RealmRuleset realmRuleset = (RealmRuleset) ruleset;
            return realmEngines.get(realmRuleset.getRealm()) != null
                && realmEngines.get(realmRuleset.getRealm()).deployments != null
                && realmEngines.get(realmRuleset.getRealm()).deployments.containsKey(ruleset.getId())
                && realmEngines.get(realmRuleset.getRealm()).deployments.get(ruleset.getId()).ruleset.getRules().equals(ruleset.getRules());
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
        LOG.finest("Requesting geofences for asset: " + assetId);

        for (GeofenceAssetAdapter geofenceAdapter : geofenceAssetAdapters) {
            GeofenceDefinition[] geofences = geofenceAdapter.getAssetGeofences(assetId);
            if (geofences != null) {
                LOG.finest("Retrieved geofences from geofence adapter '" + geofenceAdapter.getName() + "' for asset: " + assetId);
                return geofences;
            }
        }

        return new GeofenceDefinition[0];
    }

    protected void processRealmChange(Realm realm, PersistenceEvent.Cause cause) {
        // Check if enabled status has changed
        boolean wasEnabled = Arrays.stream(realms).anyMatch(t -> realm.getName().equals(t.getName()) && realm.getId().equals(t.getId()));
        boolean isEnabled = realm.getEnabled() && cause != PersistenceEvent.Cause.DELETE;
        realms = Arrays.stream(identityService.getIdentityProvider().getRealms()).filter(Realm::getEnabled).toArray(Realm[]::new);

        if (wasEnabled == isEnabled) {
            // Nothing to do here
            return;
        }

        if (wasEnabled) {
            // Remove realm rules engine for this realm if it exists
            RulesEngine<RealmRuleset> realmRulesEngine = realmEngines.get(realm.getName());
            if (realmRulesEngine != null) {
                realmRulesEngine.stop();
                realmEngines.remove(realm.getName());
            }

            // Remove any asset rules engines for assets in this realm
            assetEngines.values().stream()
                .filter(re -> re.getId().getRealm().map(r -> r.equals(realm.getName())).orElse(false))
                .forEach(RulesEngine::stop);
            assetEngines.entrySet().removeIf(entry ->
                entry.getValue().getId().getRealm().map(r -> r.equals(realm.getName())).orElse(
                    false)
            );

        } else {
            // Create realm rules engines for this realm if it has any rulesets
            rulesetStorageService
                .findAll(
                    RealmRuleset.class,
                    new RulesetQuery()
                        .setRealm(realm.getName())
                        .setFullyPopulate(true)
                        .setEnabledOnly(true))
                .stream()
                .map(this::deployRealmRuleset)
                .filter(Objects::nonNull)
                .forEach(RulesEngine::start);

            // Create any asset rules engines for assets in this realm that have rulesets
            deployAssetRulesets(
                rulesetStorageService.findAll(
                    AssetRuleset.class,
                    new RulesetQuery()
                        .setRealm(realm.getName())
                        .setEnabledOnly(true)
                        .setFullyPopulate(true)))
                .forEach(RulesEngine::start);
        }
    }

    protected void processAssetChange(Asset<?> asset, PersistenceEvent<Asset<?>> persistenceEvent) {
        switch (persistenceEvent.getCause()) {
            case CREATE: {
                // Load rule state attributes into rules engine
                asset
                    .getAttributes()
                    .stream()
                    .filter(RulesService::isRuleState)
                    .forEach(attribute -> {
                        AssetState<?> assetState = new AssetState<>(asset, attribute, Source.INTERNAL);
                        LOG.finest("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + assetState);
                        updateAssetState(assetState);
                    });
                break;
            }
            case UPDATE: {

                boolean attributesChanged = persistenceEvent.hasPropertyChanged("attributes");
                AttributeMap oldAttributes = attributesChanged ? ((AttributeMap) persistenceEvent.getPreviousState("attributes")) : asset.getAttributes();
                AttributeMap currentAttributes = asset.getAttributes();

                List<Attribute<?>> oldStateAttributes = oldAttributes
                    .stream()
                    .filter(RulesService::isRuleState).toList();

                List<Attribute<?>> newStateAttributes = currentAttributes
                    .stream()
                    .filter(RulesService::isRuleState).toList();

                // Just retract all old attributes rather than compare every value that might cause asset state to mutate
                oldStateAttributes.forEach(attribute -> {
                    AssetState<?> assetState = new AssetState<>(asset, attribute, Source.INTERNAL);
                    LOG.finest("Asset was persisted (" + persistenceEvent.getCause() + "), retracting obsolete fact: " + assetState);
                    retractAssetState(assetState);
                });

                // Insert new states for new or changed attributes
                newStateAttributes.forEach(attribute -> {
                    AssetState<?> assetState = new AssetState<>(asset, attribute, Source.INTERNAL);
                    LOG.finest("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + assetState);
                    updateAssetState(assetState);
                });
                break;
            }
            case DELETE:
                // Retract any facts that were associated with this asset
                asset.getAttributes().stream()
                    .filter(RulesService::isRuleState)
                    .forEach(attribute -> {
                        AssetState<?> assetState = new AssetState<>(asset, attribute, Source.INTERNAL);
                        LOG.finest("Asset was persisted (" + persistenceEvent.getCause() + "), retracting fact: " + assetState);
                        retractAssetState(assetState);
                    });
                break;
        }
    }

    protected synchronized void processRulesetChange(Ruleset ruleset, PersistenceEvent.Cause cause) {
        if (cause == PersistenceEvent.Cause.DELETE || !ruleset.isEnabled()) {
            if (ruleset instanceof GlobalRuleset) {
                undeployGlobalRuleset((GlobalRuleset) ruleset);
            } else if (ruleset instanceof RealmRuleset) {
                undeployRealmRuleset((RealmRuleset) ruleset);
            } else if (ruleset instanceof AssetRuleset) {
                undeployAssetRuleset((AssetRuleset) ruleset);
            }
        } else {
            if (ruleset instanceof GlobalRuleset) {

                boolean isNewEngine = globalEngine == null;
                RulesEngine<GlobalRuleset> engine = deployGlobalRuleset((GlobalRuleset) ruleset);

                if (isNewEngine) {
                    synchronized (assetStates) {
                        // Push all existing facts into the engine
                        assetStates.forEach(assetState -> engine.updateOrInsertAssetState(assetState, true));
                    }
                }

                engine.start();

            } else if (ruleset instanceof RealmRuleset) {

                boolean isNewEngine = !realmEngines.containsKey(((RealmRuleset) ruleset).getRealm());
                RulesEngine<RealmRuleset> engine = deployRealmRuleset((RealmRuleset) ruleset);

                if (isNewEngine) {
                    // Push all existing facts into the engine
                    synchronized (assetStates) {
                        assetStates.forEach(assetState -> {
                            if (assetState.getRealm().equals(((RealmRuleset) ruleset).getRealm())) {
                                engine.updateOrInsertAssetState(assetState, true);
                            }
                        });
                    }
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
    }

    /**
     * Deploy the ruleset into the global engine creating the engine if necessary.
     */
    protected RulesEngine<GlobalRuleset> deployGlobalRuleset(GlobalRuleset ruleset) {

        synchronized (this) {
            // Global rules have access to everything in the system
            if (globalEngine == null) {
                globalEngine = new RulesEngine<>(
                    timerService,
                    this,
                    identityService,
                    executorService,
                    assetStorageService,
                    assetProcessingService,
                    notificationService,
                    webhookService,
                    clientEventService,
                    assetDatapointService,
                    assetPredictedDatapointService,
                    new RulesEngineId<>(),
                    locationPredicateRulesConsumer,
                    meterRegistry
                );
            }
        }

        globalEngine.addRuleset(ruleset);

        return globalEngine;
    }

    protected synchronized void undeployGlobalRuleset(GlobalRuleset ruleset) {
        if (globalEngine == null) {
            return;
        }

        if (globalEngine.removeRuleset(ruleset)) {
            globalEngine = null;
        }
    }

    protected RulesEngine<RealmRuleset> deployRealmRuleset(RealmRuleset ruleset) {
        RulesEngine<RealmRuleset> realmRulesEngine;

        synchronized (this) {
            // Look for existing rules engines for this realm
            realmRulesEngine = realmEngines
                .computeIfAbsent(ruleset.getRealm(), (realm) ->
                    new RulesEngine<>(
                        timerService,
                        this,
                        identityService,
                        executorService,
                        assetStorageService,
                        assetProcessingService,
                        notificationService,
                        webhookService,
                        clientEventService,
                        assetDatapointService,
                        assetPredictedDatapointService,
                        new RulesEngineId<>(realm),
                        locationPredicateRulesConsumer,
                        meterRegistry
                    ));
        }

        realmRulesEngine.addRuleset(ruleset);

        return realmRulesEngine;
    }

    protected synchronized void undeployRealmRuleset(RealmRuleset ruleset) {
        RulesEngine<RealmRuleset> rulesEngine = realmEngines.get(ruleset.getRealm());
        if (rulesEngine == null) {
            return;
        }

        if (rulesEngine.removeRuleset(ruleset)) {
            realmEngines.remove(ruleset.getRealm());
        }
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
                        .stream(realms)
                        .anyMatch(at -> es.getKey().equals(at.getName()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            })
            .flatMap(es -> {
                List<Pair<Asset<?>, List<AssetRuleset>>> realmAssetAndRules = es.getValue();

                // RT: Not sure we need ordering here for starting engines so removing it
                // Order rulesets by asset hierarchy within this realm
                return realmAssetAndRules.stream()
                    //.sorted(Comparator.comparingInt(item -> item.key.getPath().length))
                    .flatMap(assetAndRules -> assetAndRules.value.stream())
                    .map(this::deployAssetRuleset);
            });
    }

    protected RulesEngine<AssetRuleset> deployAssetRuleset(AssetRuleset ruleset) {
        RulesEngine<AssetRuleset> assetRulesEngine;

        synchronized (this) {
            // Look for existing rules engine for this asset
            assetRulesEngine = assetEngines
                .computeIfAbsent(ruleset.getAssetId(), (assetId) ->
                    new RulesEngine<>(
                        timerService,
                        this,
                        identityService,
                        executorService,
                        assetStorageService,
                        assetProcessingService,
                        notificationService,
                        webhookService,
                        clientEventService,
                        assetDatapointService,
                        assetPredictedDatapointService,
                        new RulesEngineId<>(ruleset.getRealm(), assetId),
                        locationPredicateRulesConsumer,
                        meterRegistry
                    ));
        }

        assetRulesEngine.addRuleset(ruleset);
        return assetRulesEngine;
    }

    protected synchronized void undeployAssetRuleset(AssetRuleset ruleset) {
        RulesEngine<AssetRuleset> rulesEngine = assetEngines.get(ruleset.getAssetId());
        if (rulesEngine == null) {
            return;
        }

        if (rulesEngine.removeRuleset(ruleset)) {
            assetEngines.remove(ruleset.getAssetId());
        }
    }

    protected void insertAssetEvent(AssetState<?> assetState, long expiresMillis) {
        // Get the chain of rule engines that we need to pass through
        List<RulesEngine<?>> rulesEngines = getEnginesInScope(assetState.getRealm(), assetState.getPath());

        // Pass through each engine
        for (RulesEngine<?> rulesEngine : rulesEngines) {
            rulesEngine.insertAssetEvent(expiresMillis, assetState);
        }
    }

    protected void updateAssetState(AssetState<?> assetState) {
        LOG.finest("Updating asset state: " + assetState);

        // Get the chain of rule engines that we need to pass through
        List<RulesEngine<?>> rulesEngines = getEnginesInScope(assetState.getRealm(), assetState.getPath());

        synchronized (assetStates) {
            // Remove asset state with same attribute ref as new state, add new state
            boolean inserted = !assetStates.remove(assetState);
            assetStates.add(assetState);

            // Pass through each rules engine
            for (RulesEngine<?> rulesEngine : rulesEngines) {
                rulesEngine.updateOrInsertAssetState(assetState, inserted);
            }
        }
    }

    protected void retractAssetState(AssetState<?> assetState) {
        // Get the chain of rule engines that we need to pass through
        List<RulesEngine<?>> rulesEngines = getEnginesInScope(assetState.getRealm(), assetState.getPath());

        synchronized (assetStates) {
            // Remove asset state with same attribute ref
            assetStates.remove(assetState);

            // Pass through each rules engine
            for (RulesEngine<?> rulesEngine : rulesEngines) {
                rulesEngine.removeAssetState(assetState);
            }
        }
    }

    protected List<AssetState<?>> getAssetStatesInScope(String assetId) {
        synchronized (assetStates) {
            return assetStates
                .stream()
                .filter(assetState -> Arrays.asList(assetState.getPath()).contains(assetId))
                .collect(Collectors.toList());
        }
    }

    protected List<RulesEngine<?>> getEnginesInScope(String realm, String[] assetPath) {
        List<RulesEngine<?>> rulesEngines = new ArrayList<>();

        // Add global engine (if it exists)
        if (globalEngine != null) {
            rulesEngines.add(globalEngine);
        }

        // Add realm engine (if it exists)
        RulesEngine<?> realmRulesEngine = realmEngines.get(realm);

        if (realmRulesEngine != null) {
            rulesEngines.add(realmRulesEngine);
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
                    .filter(RulesService::isRuleState))
            );
    }

    /**
     * Called when an engine's rules change identifying assets with location attributes that also have {@link
     * LocationAttributePredicate} in the rules. The job here is to identify the asset's (via {@link AssetState}) that
     * have modified {@link LocationAttributePredicate}s and to notify the {@link GeofenceAssetAdapter}s.
     */
    protected void onEngineLocationRulesChanged(RulesEngine<?> rulesEngine, List<RulesEngine.AssetStateLocationPredicates> newEngineAssetStateLocationPredicates) {

        synchronized (assetsWithModifiedLocationPredicates) {

            int initialModifiedCount = assetsWithModifiedLocationPredicates.size();

            if (newEngineAssetStateLocationPredicates == null) {
                engineAssetLocationPredicateMap.computeIfPresent(rulesEngine,
                    (re, existingAssetStateLocationPredicates) -> {
                        // All location predicates have been removed so record each asset state as modified
                        assetsWithModifiedLocationPredicates.addAll(
                            existingAssetStateLocationPredicates.stream().map(
                                RulesEngine.AssetStateLocationPredicates::getAssetId).toList());
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
                                    RulesEngine.AssetStateLocationPredicates::getAssetId).toList());
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
        }
    }

    protected void processModifiedGeofences() {

        synchronized (assetsWithModifiedLocationPredicates) {
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
        }
    }

    protected Optional<RulesetDeployment> getRulesetDeployment(Long rulesetId) {
        if (globalEngine != null) {
            if (globalEngine.deployments.containsKey(rulesetId)) {
                return Optional.of(globalEngine.deployments.get(rulesetId));
            }
        }

        for (Map.Entry<String, RulesEngine<RealmRuleset>> realmAndEngine : realmEngines.entrySet()) {
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
                rulesEngine.scheduleFire(false);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
