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
package org.openremote.manager.server.rules;

import elemental.json.JsonObject;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.util.Pair;
import org.openremote.container.util.Util;
import org.openremote.manager.server.asset.AssetProcessingService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.notification.NotificationService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.rules.AssetRuleset;
import org.openremote.manager.shared.rules.GlobalRuleset;
import org.openremote.manager.shared.rules.Ruleset;
import org.openremote.manager.shared.rules.TenantRuleset;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.Attribute;
import org.openremote.model.asset.*;
import org.openremote.model.util.JsonUtil;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;

/**
 * Responsible for creating drools knowledge sessions for the rulesets
 * and processing AssetUpdate events; an asset update is processed in the following
 * order: -
 * <p>
 * Global Rulesets
 * Tenant Rulesets
 * Asset Rulesets (in hierarchical order from oldest ancestor down - ordering of
 * asset rulesets with same parent asset are not guaranteed also processing order of rulesets
 * with the same scope is not guaranteed)
 */
public class RulesService extends RouteBuilder implements ContainerService, Consumer<AssetUpdate> {
    private static final Logger LOG = Logger.getLogger(RulesService.class.getName());
    protected PersistenceService persistenceService;
    protected RulesetStorageService rulesetStorageService;
    protected ManagerIdentityService identityService;
    protected AssetStorageService assetStorageService;
    protected NotificationService notificationService;
    protected AssetProcessingService assetProcessingService;
    protected RulesDeployment<GlobalRuleset> globalDeployment;
    protected final Map<String, RulesDeployment<TenantRuleset>> tenantDeployments = new HashMap<>();
    protected final Map<String, RulesDeployment<AssetRuleset>> assetDeployments = new HashMap<>();
    protected String[] activeTenantIds;
    // Keep global list of asset updates that have been pushed to any engines
    // The objects are already in memory inside the rule engines but keeping them
    // here means we can quickly insert facts into newly started engines
    protected List<AssetUpdate> facts = new ArrayList<>();

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        rulesetStorageService = container.getService(RulesetStorageService.class);
        identityService = container.getService(ManagerIdentityService.class);
        notificationService = container.getService(NotificationService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void configure() throws Exception {
        // If any ruleset was modified in the database then check its' status and retract, insert or update it
        from(PERSISTENCE_TOPIC)
            .filter(isPersistenceEventForEntityType(Ruleset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                processRulesetChange((Ruleset) persistenceEvent.getEntity(), persistenceEvent.getCause());
            });

        // If any tenant was modified in the database then check its' status and retract, insert or update any
        // associated rulesets
        from(PERSISTENCE_TOPIC)
            .filter(isPersistenceEventForEntityType(Tenant.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Tenant tenant = (Tenant) persistenceEvent.getEntity();
                processTenantChange(tenant, persistenceEvent.getCause());
            });

        // If any asset was modified in the database, detect changed attributes
        from(PERSISTENCE_TOPIC)
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
        rulesetStorageService.findEnabledGlobalRulesets().forEach(this::insertGlobalRuleset);

        LOG.info("Deploying tenant rulesets");
        activeTenantIds = identityService.getActiveTenantIds();
        rulesetStorageService.findEnabledTenantRulesets()
            .stream()
            .filter(rd ->
                Arrays.stream(activeTenantIds)
                    .anyMatch(tenantId -> rd.getRealmId().equals(tenantId))
            ).forEach(this::insertTenantRuleset);

        LOG.info("Deploying asset rulesets");
        // Group by asset ID then tenant and check tenant is enabled
        insertAssetRulesets(rulesetStorageService.findEnabledAssetRulesets());

        LOG.info("Loading all assets with fact attributes to initialize state of rules engines");
        List<Pair<ServerAsset, List<AssetAttribute>>> assetRuleAttributesList = findRuleAttributes(null, null);

        // Push each rule attribute as an asset update through the rule engine chain
        // that will ensure the insert only happens to the engines in scope
        assetRuleAttributesList
                .forEach(assetRuleAttributes -> {
                    ServerAsset asset = assetRuleAttributes.key;
                    List<AssetAttribute> ruleAttributes = assetRuleAttributes.value;
                    ruleAttributes.forEach(ruleAttribute -> {
                        AssetUpdate update = new AssetUpdate(asset, ruleAttribute);
                        // Set the status to completed already so rules cannot interfere with this initial insert
                        update.setStatus(AssetUpdate.Status.COMPLETED);
                        LOG.fine("Inserting initial rules engine state: " + update);
                        insertFact(update, true);
                    });
                });
    }

    @Override
    public void stop(Container container) throws Exception {
        synchronized (assetDeployments) {
            assetDeployments.forEach((assetId, deployment) -> deployment.stop());
            assetDeployments.clear();
        }

        synchronized (tenantDeployments) {
            tenantDeployments.forEach((realm, deployment) -> deployment.stop());
            tenantDeployments.clear();
        }

        if (globalDeployment != null) {
            globalDeployment.stop();
            globalDeployment = null;
        }
    }

    @Override
    public void accept(AssetUpdate assetUpdate) {
        if (assetUpdate.getAttribute().isRulesFact()) {
            insertFact(assetUpdate, false);
        } else {
            LOG.finest("Ignoring update as attribute is not a rules fact: " + assetUpdate);
        }
    }

    protected synchronized void processTenantChange(Tenant tenant, PersistenceEvent.Cause cause) {
        // Check if enabled status has changed
        boolean wasEnabled = Arrays.asList(activeTenantIds).contains(tenant.getId());
        boolean isEnabled = tenant.getEnabled() && cause != PersistenceEvent.Cause.DELETE;
        activeTenantIds = identityService.getActiveTenantIds();

        if (wasEnabled == isEnabled) {
            // Nothing to do here
            return;
        }

        if (wasEnabled) {
            // Remove tenant deployment for this tenant if it exists
            RulesDeployment<TenantRuleset> tenantDeployment = tenantDeployments.get(tenant.getId());
            if (tenantDeployment != null) {
                // Use a copy of the list to avoid concurrent modification problems in retract
                new ArrayList<>(Arrays.asList(tenantDeployment.getAllRulesets()))
                    .forEach(this::retractTenantRuleset);
            }

            // Remove any asset deployments for assets in this realm
            // Use a copy of the list to avoid concurrent modification problems in retract
            new ArrayList<>(assetDeployments.values()).stream().flatMap(
                assetRulesDeployment -> Arrays.stream(assetRulesDeployment.getAllRulesets())
            ).filter(ruleset -> ruleset.getRealmId().equals(tenant.getId()))
                .forEach(this::retractAssetRuleset);

        } else {
            // Insert tenant deployment for this tenant if it has any rulesets
            rulesetStorageService
                .findEnabledTenantRulesets(tenant.getId())
                .forEach(this::insertTenantRuleset);

            // Insert any asset deployments for assets in this realm that have rulesets
            insertAssetRulesets(rulesetStorageService.findEnabledAssetRulesets(tenant.getId()));
        }
    }

    protected synchronized void processAssetChange(ServerAsset asset, PersistenceEvent persistenceEvent) {

        // We must load the asset from database (only when required), as the
        // persistence event might not contain a completely loaded asset
        BiFunction<String, AssetAttribute, AssetUpdate> buildUpdateFunction = (assetId, attribute) -> {
            final Asset loadedAsset = assetStorageService.find(assetId, true);
            return new AssetUpdate(loadedAsset,
                    new AssetAttribute(loadedAsset.getId(),
                            attribute.getName(),
                            attribute.getJsonObject()));
        };

        switch (persistenceEvent.getCause()) {
            case INSERT:

                // New asset has been created so get attributes that have RULES_FACT meta
                AssetAttributes addedAttributes = new AssetAttributes(asset);

                addedAttributes.get().stream()
                        .filter(AssetAttribute::isRulesFact)
                        .forEach(attribute -> {
                            AssetUpdate update = buildUpdateFunction.apply(asset.getId(), attribute);
                            // Set the status to completed already so rules cannot interfere with this initial insert
                            update.setStatus(AssetUpdate.Status.COMPLETED);
                            LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + update);
                            insertFact(update, true);
                        });
                break;

            case UPDATE:

                int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
                if (attributesIndex >= 0) {
                    // Attributes have possibly changed so need to compare old and new state to determine
                    // which facts to retract and which to insert
                    AssetAttributes oldAttributes = new AssetAttributes((JsonObject) persistenceEvent.getPreviousState()[attributesIndex]);
                    AssetAttributes newAttributes = new AssetAttributes((JsonObject) persistenceEvent.getCurrentState()[attributesIndex]);

                    List<AssetAttribute> oldFactAttributes = oldAttributes.get().stream()
                            .filter(AssetAttribute::isRulesFact)
                            .collect(Collectors.toList());

                    List<AssetAttribute> newFactAttributes = newAttributes.get().stream()
                            .filter(AssetAttribute::isRulesFact)
                            .collect(Collectors.toList());

                    // Compare attributes by JSON value
                    // Retract facts for attributes that are in oldFactAttributes but not in newFactAttributes

                    oldFactAttributes
                            .stream()
                            .filter(oldFactAttribute -> newFactAttributes
                                    .stream()
                                    .noneMatch(newFactAttribute -> JsonUtil.equals( // Ignore the timestamp in comparison
                                            oldFactAttribute.getJsonObject(),
                                            newFactAttribute.getJsonObject(),
                                            Collections.singletonList(AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME))
                                    )
                            )
                            .forEach(obsoleteFactAttribute -> {
                                AssetUpdate update = buildUpdateFunction.apply(asset.getId(), obsoleteFactAttribute);
                                LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), retracting fact: " + update);
                                retractFact(update);
                            });

                    // Insert facts for attributes that are in newFactAttributes but not in oldFactAttributes
                    newFactAttributes
                            .stream()
                            .filter(newFactAttribute -> oldFactAttributes
                                    .stream()
                                    .noneMatch(oldFactAttribute ->
                                            JsonUtil.equals( // Ignore the timestamp in comparison
                                                    oldFactAttribute.getJsonObject(),
                                                    newFactAttribute.getJsonObject(),
                                                    Collections.singletonList(AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME)
                                            )
                                    )
                            )
                            .forEach(newFactAttribute -> {
                                AssetUpdate update = buildUpdateFunction.apply(asset.getId(), newFactAttribute);
                                // Set the status to completed already so rules cannot interfere with this initial insert
                                update.setStatus(AssetUpdate.Status.COMPLETED);
                                LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + update);
                                insertFact(update, true);
                            });
                }
                break;
            case DELETE:
                // Retract any facts that were associated with this asset
                AssetAttributes removedAttributes = new AssetAttributes(asset);
                removedAttributes.get().stream()
                        .filter(AssetAttribute::isRulesFact)
                        .forEach(attribute -> {
                            // We can't load the asset again (it was deleted), so don't use buildUpdateFunction() and
                            // hope that the path of the event asset has been loaded before deletion
                            AssetUpdate update = new AssetUpdate(asset, attribute);
                            LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), retracting fact: " + update);
                            retractFact(update);
                        });
                break;
        }
    }

    protected synchronized void processRulesetChange(Ruleset ruleset, PersistenceEvent.Cause cause) {
        if (cause == PersistenceEvent.Cause.DELETE || !ruleset.isEnabled()) {
            if (ruleset instanceof GlobalRuleset) {
                retractGlobalRuleset((GlobalRuleset) ruleset);
            } else if (ruleset instanceof TenantRuleset) {
                retractTenantRuleset((TenantRuleset) ruleset);
            } else if (ruleset instanceof AssetRuleset) {
                retractAssetRuleset((AssetRuleset) ruleset);
            }
        } else {
            if (ruleset instanceof GlobalRuleset) {

                RulesDeployment newEngine = insertGlobalRuleset((GlobalRuleset)ruleset);
                if (newEngine != null) {
                    // Push all existing facts into the engine
                    facts.forEach(assetUpdate -> newEngine.insertFact(assetUpdate, true));
                }

            } else if (ruleset instanceof TenantRuleset) {

                RulesDeployment newEngine = insertTenantRuleset((TenantRuleset) ruleset);
                if (newEngine != null) {
                    // Push all existing facts for this tenant into the engine
                    facts.forEach(assetUpdate -> {
                        if (assetUpdate.getRealmId().equals(((TenantRuleset) ruleset).getRealmId())) {
                            newEngine.insertFact(assetUpdate, true);
                        }
                    });
                }

            } else if (ruleset instanceof AssetRuleset) {

                // Must reload from the database, the ruleset might not be completely hydrated on INSERT or UPDATE
                AssetRuleset assetRuleset = rulesetStorageService.findEnabledAssetRuleset(ruleset.getId());
                RulesDeployment newEngine = insertAssetRuleset(assetRuleset);
                if (newEngine != null) {
                    // Push all existing facts for this asset (and it's children into the engine)
                    getFactsInScope(((AssetRuleset) ruleset).getAssetId())
                            .forEach(assetUpdate -> newEngine.insertFact(assetUpdate, true));

                }
            }
        }
    }

    /**
     * Inserts the ruleset into the global engine creating the engine if necessary; if the engine was created then it
     * is returned from the method.
     */
    protected synchronized RulesDeployment<GlobalRuleset> insertGlobalRuleset(GlobalRuleset ruleset) {
        boolean created = globalDeployment == null;

        // Global rules have access to everything in the system
        if (globalDeployment == null) {
            globalDeployment = new RulesDeployment<>(assetStorageService, notificationService, assetProcessingService, GlobalRuleset.class, "GLOBAL");
        }

        globalDeployment.insertRuleset(ruleset);
        return created ? globalDeployment : null;
    }

    protected synchronized void retractGlobalRuleset(GlobalRuleset ruleset) {
        if (globalDeployment == null) {
            return;
        }

        globalDeployment.retractRuleset(ruleset);

        if (globalDeployment.isEmpty()) {
            globalDeployment = null;
        }
    }

    protected synchronized RulesDeployment<TenantRuleset> insertTenantRuleset(TenantRuleset ruleset) {
        final boolean[] created = {false};

        // Look for existing deployment for this tenant
        RulesDeployment<TenantRuleset> deployment = tenantDeployments
            .computeIfAbsent(ruleset.getRealmId(), (realmId) -> {
                created[0] = true;
                return new RulesDeployment<>(assetStorageService,notificationService, assetProcessingService, TenantRuleset.class, realmId);
            });

        deployment.insertRuleset(ruleset);

        return created[0] ? deployment : null;
    }

    protected synchronized void retractTenantRuleset(TenantRuleset ruleset) {
        RulesDeployment<TenantRuleset> deployment = tenantDeployments.get(ruleset.getRealmId());
        if (deployment == null) {
            return;
        }

        deployment.retractRuleset(ruleset);

        if (deployment.isEmpty()) {
            tenantDeployments.remove(ruleset.getRealmId());
        }
    }

    protected void insertAssetRulesets(List<AssetRuleset> rulesets) {
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
                    .forEach(this::insertAssetRuleset);
            });
    }

    protected synchronized RulesDeployment<AssetRuleset> insertAssetRuleset(AssetRuleset ruleset) {
        final boolean[] created = {false};

        // Look for existing deployment for this asset
        RulesDeployment<AssetRuleset> deployment = assetDeployments
            .computeIfAbsent(ruleset.getAssetId(), (assetId) -> {
                created[0] = true;
                return new RulesDeployment<>(assetStorageService,notificationService, assetProcessingService, AssetRuleset.class, assetId);
            });

        deployment.insertRuleset(ruleset);
        return created[0] ? deployment : null;
    }

    protected synchronized void retractAssetRuleset(AssetRuleset ruleset) {
        RulesDeployment<AssetRuleset> deployment = assetDeployments.get(ruleset.getAssetId());
        if (deployment == null) {
            return;
        }

        deployment.retractRuleset(ruleset);

        if (deployment.isEmpty()) {
            assetDeployments.remove(ruleset.getAssetId());
        }
    }

    protected synchronized void insertFact(AssetUpdate assetUpdate, boolean skipStatusCheck) {
        // TODO: implement rules processing error state handling

        // Get the chain of rule engines that we need to pass through
        List<RulesDeployment> rulesDeployments = getEnginesInScope(assetUpdate.getRealmId(), assetUpdate.getPathFromRoot());

        if (rulesDeployments.size() == 0) {
            LOG.fine("Ignoring asset update as there are no matching rules deployments: " + assetUpdate);
        }

        if (!skipStatusCheck) {
            // Check that all engines in the scope are not in ERROR state
            if (rulesDeployments.stream().anyMatch(rd -> rd.isError())) {
                LOG.severe("At least one rule engine is in an error state so cannot process update:" + assetUpdate);
                assetUpdate.setStatus(AssetUpdate.Status.ERROR);
                return;
            }
        }

        // Remove any stale fact that has equality with this new one or the attribute ref matches
        // (this is what rules deployment does also)
        if (!facts.remove(assetUpdate)) {
            facts.removeIf(storedUpdate -> storedUpdate.attributeRefsEqual(assetUpdate));
        }
        // Insert the new fact
        facts.add(assetUpdate);

        // Pass through each engine and try and insert the fact
        for (RulesDeployment deployment : rulesDeployments) {

            // Any exceptions in rule RHS will bubble up and the engine would be marked as in ERROR so future
            // updates will be blocked
            deployment.insertFact(assetUpdate, skipStatusCheck);

            if (!skipStatusCheck) {
                // We have to check status between each engine insert as a rule in the engine could have updated
                // the status
                if (assetUpdate.getStatus() != AssetUpdate.Status.CONTINUE) {
                    LOG.info("Rules engine has marked update event as '" + assetUpdate.getStatus() + "' so not processing anymore");
                    if (assetUpdate.getStatus() == AssetUpdate.Status.RULES_HANDLED) {
                        assetUpdate.setStatus(AssetUpdate.Status.CONTINUE);
                    }
                    break;
                }
            }
        }
    }

    protected void retractFact(AssetUpdate assetUpdate) {
        // Get the chain of rule engines that we need to pass through
        List<RulesDeployment> rulesDeployments = getEnginesInScope(assetUpdate.getRealmId(), assetUpdate.getPathFromRoot());

        if (!facts.remove(assetUpdate)) {
            facts.removeIf(storedUpdate -> storedUpdate.attributeRefsEqual(assetUpdate));
        }

        if (rulesDeployments.size() == 0) {
            LOG.fine("Ignoring asset update as there are no matching rules deployments: " + assetUpdate);
        }

        // Pass through each engine and retract this fact
        for (RulesDeployment deployment : rulesDeployments) {
            deployment.retractFact(assetUpdate);
        }
    }

    protected List<AssetUpdate> getFactsInScope(String assetId) {
        return facts
                .stream()
                .filter(assetUpdate -> Arrays.asList(assetUpdate.getPathFromRoot()).contains(assetId))
                .collect(Collectors.toList());
    }

    protected List<RulesDeployment> getEnginesInScope(String assetRealmId, String[] assetPath) {
        List<RulesDeployment> rulesDeployments = new ArrayList<>();

        // Add global engine (if it exists)
        if (globalDeployment != null) {
            rulesDeployments.add(globalDeployment);
        }

        // Add tenant engine (if it exists)
        RulesDeployment tenantDeployment = tenantDeployments.get(assetRealmId);

        if (tenantDeployment != null) {
            rulesDeployments.add(tenantDeployment);
        }

        // Add asset engines
        // Iterate through asset hierarchy using asset IDs from getPath
        for (String assetId : assetPath) {
            RulesDeployment assetDeployment = assetDeployments.get(assetId);
            if (assetDeployment != null) {
                rulesDeployments.add(assetDeployment);
            }
        }

        return rulesDeployments;
    }

    protected List<Pair<ServerAsset, List<AssetAttribute>>> findRuleAttributes(AssetQuery.TenantPredicate tenantPredicate, AssetQuery.ParentPredicate parentPredicate) {
        List<ServerAsset> assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new AssetQuery.Select(true))
                        .parent(parentPredicate)
                        .tenant(tenantPredicate)
                        .attributeMeta(
                                new AssetQuery.AttributeMetaPredicate(
                                        AssetMeta.RULES_FACT,
                                        new AssetQuery.BooleanPredicate(true))
                        ));

        return assets.stream().map(asset -> {
            AssetAttributes attributes = new AssetAttributes(asset);
            List<AssetAttribute> ruleAttributes = attributes.get().stream()
                    .filter(AssetAttribute::isRulesFact).collect(Collectors.toList());
            return new Pair<>(asset, ruleAttributes);

        }).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}