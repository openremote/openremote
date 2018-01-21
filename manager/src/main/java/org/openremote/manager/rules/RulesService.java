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
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.ServerAsset;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.facade.AssetsFacade;
import org.openremote.manager.rules.facade.UsersFacade;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.TenantRuleset;
import org.openremote.model.security.Tenant;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.logging.Level.FINEST;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.manager.asset.AssetRoute.isPersistenceEventForEntityType;
import static org.openremote.model.AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME;
import static org.openremote.model.asset.AssetAttribute.attributesFromJson;
import static org.openremote.model.asset.AssetAttribute.getAddedOrModifiedAttributes;

/**
 * Responsible for creating {@link RulesEngine}s for the rulesets
 * and processing {@link AssetState}  and {@link AssetEvent} messages.
 * <p>
 * Each message is processed in the following order:
 * <ol>
 * <li>Global Rulesets</li>
 * <li>Tenant Rulesets</li>
 * <li>Asset Rulesets (in hierarchical order from oldest ancestor down - ordering of
 * asset rulesets with same parent asset are not guaranteed also processing order of rulesets
 * with the same scope is not guaranteed)</li>
 * </ol>
 */
public class RulesService extends RouteBuilder implements ContainerService, Consumer<AssetState> {

    private static final Logger LOG = Logger.getLogger(RulesService.class.getName());

    public static final String RULE_EVENT_EXPIRES = "RULE_EVENT_EXPIRES";
    public static final String RULE_EVENT_EXPIRES_DEFAULT = "1h";

    protected TimerService timerService;
    protected ManagerExecutorService executorService;
    protected PersistenceService persistenceService;
    protected RulesetStorageService rulesetStorageService;
    protected ManagerIdentityService identityService;
    protected AssetStorageService assetStorageService;
    protected NotificationService notificationService;
    protected AssetProcessingService assetProcessingService;

    protected RulesEngine<GlobalRuleset> globalEngine;
    protected final Map<String, RulesEngine<TenantRuleset>> tenantEngines = new HashMap<>();
    protected final Map<String, RulesEngine<AssetRuleset>> assetEngines = new HashMap<>();
    protected String[] activeTenantIds;

    // Keep global list of asset states that have been pushed to any engines
    // The objects are already in memory inside the rule engines but keeping them
    // here means we can quickly insert facts into newly started engines
    protected Set<AssetState> assetStates = new HashSet<>();

    protected String configEventExpires;

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        executorService = container.getService(ManagerExecutorService.class);
        persistenceService = container.getService(PersistenceService.class);
        rulesetStorageService = container.getService(RulesetStorageService.class);
        identityService = container.getService(ManagerIdentityService.class);
        notificationService = container.getService(NotificationService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);

        configEventExpires = getString(container.getConfig(), RULE_EVENT_EXPIRES, RULE_EVENT_EXPIRES_DEFAULT);
    }

    @Override
    public void configure() throws Exception {
        // If any ruleset was modified in the database then check its' status and undeploy, deploy, or update it
        from(PERSISTENCE_TOPIC)
            .routeId("RulesetPersistenceChanges")
            .filter(isPersistenceEventForEntityType(Ruleset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                processRulesetChange((Ruleset) persistenceEvent.getEntity(), persistenceEvent.getCause());
            });

        // If any tenant was modified in the database then check its' status and undeploy, deploy or update any
        // associated rulesets
        from(PERSISTENCE_TOPIC)
            .routeId("RuleEngineTenantChanges")
            .filter(isPersistenceEventForEntityType(Tenant.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Tenant tenant = (Tenant) persistenceEvent.getEntity();
                processTenantChange(tenant, persistenceEvent.getCause());
            });

        // If any asset was modified in the database, detect changed attributes
        from(PERSISTENCE_TOPIC)
            .routeId("RuleEngineAssetChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                final ServerAsset eventAsset = (ServerAsset) persistenceEvent.getEntity();
                processAssetChange(eventAsset, persistenceEvent);
            });
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.info("Deploying global rulesets");
        rulesetStorageService.findEnabledGlobalRulesets().forEach(this::deployGlobalRuleset);

        LOG.info("Deploying tenant rulesets");
        activeTenantIds = identityService.getIdentityProvider().getActiveTenantIds();
        rulesetStorageService.findEnabledTenantRulesets()
            .stream()
            .filter(rd ->
                Arrays.stream(activeTenantIds)
                    .anyMatch(tenantId -> rd.getRealmId().equals(tenantId))
            ).forEach(this::deployTenantRuleset);

        LOG.info("Deploying asset rulesets");
        // Group by asset ID then tenant and check tenant is enabled
        deployAssetRulesets(rulesetStorageService.findEnabledAssetRulesets());

        LOG.info("Loading all assets with fact attributes to initialize state of rules engines");
        Stream<Pair<ServerAsset, Stream<AssetAttribute>>> assetRuleAttributes = findRuleStateAttributes();

        // Push each rule attribute as an asset update through the rule engine chain
        // that will ensure the insert only happens to the engines in scope
        assetRuleAttributes
            .forEach(pair -> {
                ServerAsset asset = pair.key;
                pair.value.forEach(ruleAttribute -> {
                    AssetState assetState = new AssetState(asset, ruleAttribute, AttributeEvent.Source.INTERNAL);
                    // Set the status to completed already so rules cannot interfere with this initial insert
                    assetState.setProcessingStatus(AssetState.ProcessingStatus.COMPLETED);
                    updateAssetState(assetState, true, true);
                });
            });
    }

    @Override
    public void stop(Container container) throws Exception {
        synchronized (assetEngines) {
            assetEngines.forEach((assetId, rulesEngine) -> rulesEngine.stop());
            assetEngines.clear();
        }

        synchronized (tenantEngines) {
            tenantEngines.forEach((realm, rulesEngine) -> rulesEngine.stop());
            tenantEngines.clear();
        }

        if (globalEngine != null) {
            globalEngine.stop();
            globalEngine = null;
        }
    }

    @Override
    public void accept(AssetState assetState) {
        // We might process two facts for a single attribute update, if that is what the user wants

        // First as asset state
        if (assetState.getAttribute().isRuleState()) {
            updateAssetState(
                assetState,
                false, // Don't skip the error check on the rules engines
                !assetState.getAttribute().isRuleEvent() // If it's not a rule event, fire immediately
            );
        }

        // Then as asset event (if there wasn't an error), this will also fire the rules engines
        if (assetState.getProcessingStatus() == AssetState.ProcessingStatus.CONTINUE
            && assetState.getAttribute().isRuleEvent()) {
            process(new AssetEvent(
                assetState,
                assetState.getAttribute().getRuleEventExpires().orElse(configEventExpires)
            ));
        }
    }

    protected synchronized void processTenantChange(Tenant tenant, PersistenceEvent.Cause cause) {
        // Check if enabled status has changed
        boolean wasEnabled = Arrays.asList(activeTenantIds).contains(tenant.getId());
        boolean isEnabled = tenant.getEnabled() && cause != PersistenceEvent.Cause.DELETE;
        activeTenantIds = identityService.getIdentityProvider().getActiveTenantIds();

        if (wasEnabled == isEnabled) {
            // Nothing to do here
            return;
        }

        if (wasEnabled) {
            // Remove tenant rules engine for this tenant if it exists
            RulesEngine<TenantRuleset> tenantRulesEngine = tenantEngines.get(tenant.getId());
            if (tenantRulesEngine != null) {
                tenantRulesEngine.stop();
                tenantEngines.remove(tenant.getId());
            }

            // Remove any asset rules engines for assets in this realm
            assetEngines.values().stream()
                .filter(re -> tenant.getId().equals(re.getRealmId()))
                .forEach(RulesEngine::stop);
            assetEngines.entrySet().removeIf(entry -> tenant.getId().equals(entry.getValue().getRealmId()));

        } else {
            // Create tenant rules engines for this tenant if it has any rulesets
            rulesetStorageService
                .findEnabledTenantRulesets(tenant.getId())
                .forEach(this::deployTenantRuleset);

            // Create any asset rules engines for assets in this realm that have rulesets
            deployAssetRulesets(rulesetStorageService.findEnabledAssetRulesets(tenant.getId()));
        }
    }

    protected synchronized void processAssetChange(ServerAsset asset, PersistenceEvent persistenceEvent) {

        // We must load the asset from database (only when required), as the
        // persistence event might not contain a completely loaded asset
        BiFunction<Asset, AssetAttribute, AssetState> buildAssetState = (loadedAsset, attribute) ->
            new AssetState(loadedAsset, attribute.deepCopy(), AttributeEvent.Source.INTERNAL);

        switch (persistenceEvent.getCause()) {
            case INSERT:

                // New asset has been created so get attributes that have RULE_STATE meta
                List<AssetAttribute> ruleStateAttributes =
                    asset.getAttributesStream().filter(AssetAttribute::isRuleState).collect(Collectors.toList());

                // Build an update with a fully loaded asset
                ruleStateAttributes.forEach(attribute -> {
                    ServerAsset loadedAsset = assetStorageService.find(asset.getId(), true);
                    // If the asset is now gone it was deleted immediately after being inserted, nothing more to do
                    if (loadedAsset == null)
                        return;

                    AssetState assetState = buildAssetState.apply(loadedAsset, attribute);
                    // Set the status to completed already so rules cannot interfere with this initial insert
                    assetState.setProcessingStatus(AssetState.ProcessingStatus.COMPLETED);
                    LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + assetState);
                    updateAssetState(assetState, true, true);
                });
                break;

            case UPDATE:

                int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
                if (attributesIndex < 0) {
                    return;
                }

                // Fully load the asset
                final Asset loadedAsset = assetStorageService.find(asset.getId(), true);
                // If the asset is now gone it was deleted immediately after being updated, nothing more to do
                if (loadedAsset == null)
                    return;

                // Attributes have possibly changed so need to compare old and new attributes
                // to determine which facts to retract and which to insert
                List<AssetAttribute> oldRuleStateAttributes =
                    attributesFromJson(
                        (ObjectValue) persistenceEvent.getPreviousState()[attributesIndex],
                        asset.getId()
                    ).filter(AssetAttribute::isRuleState).collect(Collectors.toList());

                List<AssetAttribute> newRuleStateAttributes =
                    attributesFromJson(
                        (ObjectValue) persistenceEvent.getCurrentState()[attributesIndex],
                        asset.getId()
                    ).filter(AssetAttribute::isRuleState).collect(Collectors.toList());

                // Retract facts for attributes that are obsolete
                getAddedOrModifiedAttributes(newRuleStateAttributes, oldRuleStateAttributes, key -> key.equals(VALUE_TIMESTAMP_FIELD_NAME))
                    .forEach(obsoleteFactAttribute -> {
                        AssetState update = buildAssetState.apply(loadedAsset, obsoleteFactAttribute);
                        LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), retracting: " + update);
                        retractAssetState(update);
                    });

                // Insert facts for attributes that are new
                getAddedOrModifiedAttributes(oldRuleStateAttributes, newRuleStateAttributes, key -> key.equals(VALUE_TIMESTAMP_FIELD_NAME))
                    .forEach(newFactAttribute -> {
                        AssetState assetState = buildAssetState.apply(loadedAsset, newFactAttribute);
                        // Set the status to completed already so rules cannot interfere with this initial insert
                        assetState.setProcessingStatus(AssetState.ProcessingStatus.COMPLETED);
                        LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), updating: " + assetState);
                        updateAssetState(assetState, true, true);
                    });
                break;

            case DELETE:
                // Retract any facts that were associated with this asset
                asset.getAttributesStream()
                    .filter(AssetAttribute::isRuleState)
                    .forEach(attribute -> {
                        // We can't load the asset again (it was deleted), so don't use buildAssetState() and
                        // hope that the path of the event asset has been loaded before deletion, although it is
                        // "unlikely" anybody will access it during retraction...
                        AssetState assetState = new AssetState(asset, attribute, AttributeEvent.Source.INTERNAL);
                        LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), retracting fact: " + assetState);
                        retractAssetState(assetState);
                    });
                break;
        }

        if (persistenceEvent.getCause() != PersistenceEvent.Cause.INSERT) {

            int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
            if (attributesIndex < 0) {
                return;
            }

            // Get old template filter attributes and new ones
            // If new collection doesn't contain them, then they are deleted and need to be processed too
            List<AssetAttribute> oldTemplateFilterAttributes = persistenceEvent.getPreviousState() != null ? //can be null when insert
                attributesFromJson(
                    (ObjectValue) persistenceEvent.getPreviousState()[attributesIndex],
                    asset.getId()
                ).filter(attribute -> attribute.getTypeOrThrow() == AttributeType.RULES_TEMPLATE_FILTER)
                    .collect(Collectors.toList()) : new ArrayList<>();

            List<AssetAttribute> newTemplateFilterAttributes = persistenceEvent.getCurrentState() != null ? // can be null when delete
                attributesFromJson(
                    (ObjectValue) persistenceEvent.getCurrentState()[attributesIndex],
                    asset.getId()
                ).filter(attribute -> attribute.getTypeOrThrow() == AttributeType.RULES_TEMPLATE_FILTER)
                    .collect(Collectors.toList()) : new ArrayList<>();

            if (!oldTemplateFilterAttributes.isEmpty() || !newTemplateFilterAttributes.isEmpty()) {
                rulesetStorageService.findTemplatedAssetRulesets(asset.getRealmId(), asset.getId())
                    .forEach(assetRuleset -> processRulesetChange(assetRuleset, persistenceEvent.getCause()));

                rulesetStorageService.findTemplatedTenantRulesets(asset.getRealmId(), asset.getId())
                    .forEach(tenantRuleset -> processRulesetChange(tenantRuleset, persistenceEvent.getCause()));

                rulesetStorageService.findTemplatedGlobalRulesets(asset.getId())
                    .forEach(globalRuleset -> processRulesetChange(globalRuleset, persistenceEvent.getCause()));
            }
        }
    }

    protected synchronized void processRulesetChange(Ruleset ruleset, PersistenceEvent.Cause cause) {
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

                RulesEngine newEngine = deployGlobalRuleset((GlobalRuleset) ruleset);
                if (newEngine != null) {
                    // Push all existing facts into the engine, this is an initial import of state so fire delayed
                    assetStates.forEach(assetState -> newEngine.updateFact(assetState, false));
                    newEngine.fire();
                }

            } else if (ruleset instanceof TenantRuleset) {

                RulesEngine newEngine = deployTenantRuleset((TenantRuleset) ruleset);
                if (newEngine != null) {
                    // Push all existing facts into the engine, this is an initial import of state so fire delayed
                    assetStates.forEach(assetState -> {
                        if (assetState.getRealmId().equals(((TenantRuleset) ruleset).getRealmId())) {
                            newEngine.updateFact(assetState, false);
                        }
                    });
                    newEngine.fire();
                }

            } else if (ruleset instanceof AssetRuleset) {

                // Must reload from the database, the ruleset might not be completely hydrated on INSERT or UPDATE
                AssetRuleset assetRuleset = rulesetStorageService.findEnabledAssetRuleset(ruleset.getId());
                RulesEngine newEngine = deployAssetRuleset(assetRuleset);
                if (newEngine != null) {
                    // Push all existing facts for this asset (and it's children into the engine), this is an
                    // initial import of state so fire delayed
                    getAssetStatesInScope(((AssetRuleset) ruleset).getAssetId())
                        .forEach(assetState -> newEngine.updateFact(assetState, false));
                    newEngine.fire();
                }
            }
        }
    }

    /**
     * Deploy the ruleset into the global engine creating the engine if necessary; if the engine was created then it
     * is returned from the method.
     */
    protected synchronized RulesEngine<GlobalRuleset> deployGlobalRuleset(GlobalRuleset ruleset) {
        boolean created = globalEngine == null;

        // Global rules have access to everything in the system
        if (globalEngine == null) {
            globalEngine = new RulesEngine<>(
                timerService,
                executorService,
                assetStorageService,
                new AssetsFacade<>(GlobalRuleset.class, null, assetStorageService, event -> assetProcessingService.sendAttributeEvent(event)),
                new UsersFacade<>(GlobalRuleset.class, null, assetStorageService, notificationService, identityService),
                null,
                null
            );
        }

        globalEngine.addRuleset(ruleset);
        return created ? globalEngine : null;
    }

    protected synchronized void undeployGlobalRuleset(GlobalRuleset ruleset) {
        if (globalEngine == null) {
            return;
        }

        if (globalEngine.removeRuleset(ruleset)) {
            globalEngine = null;
        }
    }

    protected synchronized RulesEngine<TenantRuleset> deployTenantRuleset(TenantRuleset ruleset) {
        final boolean[] created = {false};

        // Look for existing rules engines for this tenant
        RulesEngine<TenantRuleset> tenentRulesEngine = tenantEngines
            .computeIfAbsent(ruleset.getRealmId(), (realmId) -> {
                created[0] = true;
                return new RulesEngine<>(
                    timerService,
                    executorService,
                    assetStorageService,
                    new AssetsFacade<>(TenantRuleset.class, realmId, assetStorageService, event -> assetProcessingService.sendAttributeEvent(event)),
                    new UsersFacade<>(TenantRuleset.class, realmId, assetStorageService, notificationService, identityService),
                    realmId,
                    null
                );
            });

        tenentRulesEngine.addRuleset(ruleset);

        return created[0] ? tenentRulesEngine : null;
    }

    protected synchronized void undeployTenantRuleset(TenantRuleset ruleset) {
        RulesEngine<TenantRuleset> rulesEngine = tenantEngines.get(ruleset.getRealmId());
        if (rulesEngine == null) {
            return;
        }

        if (rulesEngine.removeRuleset(ruleset)) {
            tenantEngines.remove(ruleset.getRealmId());
        }
    }

    protected void deployAssetRulesets(List<AssetRuleset> rulesets) {
        rulesets
            .stream()
            .collect(Collectors.groupingBy(AssetRuleset::getAssetId))
            .entrySet()
            .stream()
            .map(es ->
                new Pair<>(assetStorageService.find(es.getKey(), true), es.getValue())
            )
            .filter(assetAndRules -> assetAndRules.key != null)
            .collect(Collectors.groupingBy(assetAndRules -> assetAndRules.key.getRealmId()))
            .entrySet()
            .stream()
            .filter(es -> Arrays
                .stream(activeTenantIds)
                .anyMatch(at -> es.getKey().equals(at)))
            .forEach(es -> {
                List<Pair<ServerAsset, List<AssetRuleset>>> tenantAssetAndRules = es.getValue();

                // RT: Not sure we need ordering here for starting engines so removing it
                // Order rulesets by asset hierarchy within this tenant
                tenantAssetAndRules.stream()
                    //.sorted(Comparator.comparingInt(item -> item.key.getPath().length))
                    .flatMap(assetAndRules -> assetAndRules.value.stream())
                    .forEach(this::deployAssetRuleset);
            });
    }

    protected synchronized RulesEngine<AssetRuleset> deployAssetRuleset(AssetRuleset ruleset) {
        final boolean[] created = {false};

        // Look for existing rules engine for this asset
        RulesEngine<AssetRuleset> assetRulesEngine = assetEngines
            .computeIfAbsent(ruleset.getAssetId(), (assetId) -> {
                created[0] = true;
                return new RulesEngine<>(
                    timerService,
                    executorService,
                    assetStorageService,
                    new AssetsFacade<>(AssetRuleset.class, assetId, assetStorageService, event -> assetProcessingService.sendAttributeEvent(event)),
                    new UsersFacade<>(AssetRuleset.class, assetId, assetStorageService, notificationService, identityService),
                    ruleset.getRealmId(),
                    ruleset.getAssetId()
                );
            });

        assetRulesEngine.addRuleset(ruleset);
        return created[0] ? assetRulesEngine : null;
    }

    protected synchronized void undeployAssetRuleset(AssetRuleset ruleset) {
        RulesEngine<AssetRuleset> assetRulesEngine = assetEngines.get(ruleset.getAssetId());
        if (assetRulesEngine == null) {
            return;
        }

        if (assetRulesEngine.removeRuleset(ruleset)) {
            assetEngines.remove(ruleset.getAssetId());
        }
    }

    protected synchronized void process(AssetEvent assetEvent) {
        // Get the chain of rule engines that we need to pass through
        List<RulesEngine> rulesEngines = getEnginesInScope(assetEvent.getRealmId(), assetEvent.getPath());

        // Check that all engines in the scope are available
        if (rulesEngines.stream().anyMatch(RulesEngine::isError)) {
            LOG.severe("At least one rules engine is in an error state, skipping: " + assetEvent);
            if (LOG.isLoggable(FINEST)) {
                for (RulesEngine rulesEngine : rulesEngines) {
                    if (rulesEngine.isError()) {
                        LOG.log(FINEST, "Rules engine error state: " + rulesEngine, rulesEngine.getError());
                    }
                }
            }
            return;
        }

        // Pass through each engine
        for (RulesEngine rulesEngine : rulesEngines) {
            rulesEngine.insertFact(assetEvent);
        }
    }

    protected synchronized void updateAssetState(AssetState assetState, boolean skipStatusCheck, boolean fireImmediately) {
        // TODO: implement rules processing error state handling

        // Get the chain of rule engines that we need to pass through
        List<RulesEngine> rulesEngines = getEnginesInScope(assetState.getRealmId(), assetState.getPath());

        if (!skipStatusCheck) {
            // Check that all engines in the scope are available
            // TODO This is not very useful without locking the engines until we are done with the udpate
            for (RulesEngine rulesEngine : rulesEngines) {
                if (rulesEngine.isError()) {
                    assetState.setProcessingStatus(AssetState.ProcessingStatus.ERROR);
                    assetState.setError(rulesEngine.getError());
                    return;
                }
            }
        }

        // Remove asset state with same attribute ref as new state, add new state
        assetStates.remove(assetState);
        assetStates.add(assetState);

        // Pass through each rules engine
        for (RulesEngine rulesEngine : rulesEngines) {
            rulesEngine.updateFact(assetState, fireImmediately);
        }
    }

    protected void retractAssetState(AssetState assetState) {
        // Get the chain of rule engines that we need to pass through
        List<RulesEngine> rulesEngines = getEnginesInScope(assetState.getRealmId(), assetState.getPath());

        // Remove asset state with same attribute ref
        assetStates.remove(assetState);

        if (rulesEngines.size() == 0) {
            LOG.fine("Ignoring as there are no matching rules engines: " + assetState);
        }

        // Pass through each rules engine
        for (RulesEngine rulesEngine : rulesEngines) {
            rulesEngine.removeFact(assetState);
        }
    }

    protected List<AssetState> getAssetStatesInScope(String assetId) {
        return assetStates
            .stream()
            .filter(assetState -> Arrays.asList(assetState.getPath()).contains(assetId))
            .collect(Collectors.toList());
    }

    protected List<RulesEngine> getEnginesInScope(String realmId, String[] assetPath) {
        List<RulesEngine> rulesEngines = new ArrayList<>();

        // Add global engine (if it exists)
        if (globalEngine != null) {
            rulesEngines.add(globalEngine);
        }

        // Add tenant engine (if it exists)
        RulesEngine tenantRulesEngine = tenantEngines.get(realmId);

        if (tenantRulesEngine != null) {
            rulesEngines.add(tenantRulesEngine);
        }

        // Add asset engines, iterate through asset hierarchy using asset IDs from asset path
        for (String assetId : assetPath) {
            RulesEngine assetRulesEngine = assetEngines.get(assetId);
            if (assetRulesEngine != null) {
                rulesEngines.add(assetRulesEngine);
            }
        }

        return rulesEngines;
    }

    protected Stream<Pair<ServerAsset, Stream<AssetAttribute>>> findRuleStateAttributes() {
        List<ServerAsset> assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select(AssetQuery.Include.ALL))
                .attributeMeta(
                    new AssetQuery.AttributeMetaPredicate(
                        AssetMeta.RULE_STATE,
                        new AssetQuery.BooleanPredicate(true))
                ));

        return assets.stream()
            .map((ServerAsset asset) ->
                new Pair<>(asset, asset.getAttributesStream().filter(AssetAttribute::isRuleState))
            );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}