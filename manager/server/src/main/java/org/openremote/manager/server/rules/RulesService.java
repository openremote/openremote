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
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.rules.AssetRuleset;
import org.openremote.manager.shared.rules.GlobalRuleset;
import org.openremote.manager.shared.rules.Ruleset;
import org.openremote.manager.shared.rules.TenantRuleset;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.asset.*;
import org.openremote.model.util.JsonUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
    protected RulesDeployment<GlobalRuleset> globalDeployment;
    protected final Map<String, RulesDeployment<TenantRuleset>> tenantDeployments = new HashMap<>();
    protected final Map<String, RulesDeployment<AssetRuleset>> assetDeployments = new HashMap<>();
    protected String[] activeTenantIds;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        rulesetStorageService = container.getService(RulesetStorageService.class);
        identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
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

                // We must load the asset from database (only when required), as the
                // persistence event might not contain a completely loaded asset
                Function<AssetAttribute, AssetUpdate> buildUpdateFunction = new Function<AssetAttribute, AssetUpdate>() {
                    final Asset loadedAsset = assetStorageService.find(eventAsset.getId(), true);

                    @Override
                    public AssetUpdate apply(AssetAttribute attribute) {
                        return new AssetUpdate(loadedAsset,
                            new AssetAttribute(loadedAsset.getId(),
                                attribute.getName(),
                                attribute.getJsonObject())
                        );
                    }
                };

                switch (persistenceEvent.getCause()) {
                    case INSERT:
                        // New asset has been created so get attributes that have RULES_FACT meta
                        AssetAttributes addedAttributes = new AssetAttributes(eventAsset);

                        addedAttributes.get().stream()
                            .filter(AssetAttribute::isRulesFact)
                            .forEach(attribute -> {
                                AssetUpdate update = buildUpdateFunction.apply(attribute);
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
                                    AssetUpdate update = buildUpdateFunction.apply(obsoleteFactAttribute);
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
                                    AssetUpdate update = buildUpdateFunction.apply(newFactAttribute);
                                    // Set the status to completed already so rules cannot interfere with this initial insert
                                    update.setStatus(AssetUpdate.Status.COMPLETED);
                                    LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), inserting fact: " + update);
                                    insertFact(update, true);
                                });
                        }
                        break;
                    case DELETE:
                        // Retract any facts that were associated with this asset
                        AssetAttributes removedAttributes = new AssetAttributes(eventAsset);
                        removedAttributes.get().stream()
                            .filter(AssetAttribute::isRulesFact)
                            .forEach(attribute -> {
                                // We can't load the asset again (it was deleted), so don't use buildUpdateFunction() and
                                // hope that the path of the event asset has been loaded before deletion
                                AssetUpdate update = new AssetUpdate(eventAsset, attribute);
                                LOG.fine("Asset was persisted (" + persistenceEvent.getCause() + "), retracting fact: " + update);
                                retractFact(update);
                            });
                        break;
                }
            });
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.info("Deploying global rulesets");
        rulesetStorageService.findEnabledGlobalRulesets().forEach(this::deployGlobalRuleset);

        LOG.info("Deploying tenant rulesets");
        activeTenantIds = identityService.getActiveTenantIds();
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
        List<ServerAsset> assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select(true))
                .attributeMeta(
                    new AssetQuery.AttributeMetaPredicate(
                        AssetMeta.RULES_FACT,
                        new AssetQuery.BooleanPredicate(true))
                ));

        assets.forEach(asset -> {
            AssetAttributes attributes = new AssetAttributes(asset);
            attributes.get().stream()
                .filter(AssetAttribute::isRulesFact)
                .forEach(attribute -> {
                    AssetUpdate update = new AssetUpdate(asset, attribute);
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

    public synchronized void processTenantChange(Tenant tenant, PersistenceEvent.Cause cause) {
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
                Arrays.stream(tenantDeployment.getAllRulesets())
                    .forEach(this::retractTenantRuleset);
            }

            // Remove any asset deployments for assets in this realm
            assetDeployments.values().stream().flatMap(
                assetRulesDeployment -> Arrays.stream(assetRulesDeployment.getAllRulesets())
            ).filter(ruleset -> ruleset.getRealmId().equals(tenant.getId()))
                .forEach(this::retractAssetRuleset);

        } else {
            // Insert tenant deployment for this tenant if it has any rulesets
            rulesetStorageService
                .findEnabledTenantRulesets(tenant.getId())
                .forEach(this::deployTenantRuleset);

            // Insert any asset deployments for assets in this realm that have rulesets
            deployAssetRulesets(rulesetStorageService.findEnabledAssetRulesets(tenant.getId()));
        }
    }

    protected void processRulesetChange(Ruleset ruleset, PersistenceEvent.Cause cause) {
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
                deployGlobalRuleset((GlobalRuleset) ruleset);
            } else if (ruleset instanceof TenantRuleset) {
                deployTenantRuleset((TenantRuleset) ruleset);
            } else if (ruleset instanceof AssetRuleset) {
                // Must reload from the database, the ruleset might not be completely hydrated on INSERT or UPDATE
                AssetRuleset assetRuleset = rulesetStorageService.findEnabledAssetRuleset(ruleset.getId());
                deployAssetRuleset(assetRuleset);
            }
        }
    }

    protected synchronized void deployGlobalRuleset(GlobalRuleset ruleset) {
        // Global rules have access to everything in the system
        if (globalDeployment == null) {
            globalDeployment = new RulesDeployment<>(GlobalRuleset.class, "GLOBAL");
        }

        globalDeployment.insertRuleset(ruleset);
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

    protected synchronized void deployTenantRuleset(TenantRuleset ruleset) {
        // Look for existing deployment for this tenant
        RulesDeployment<TenantRuleset> deployment = tenantDeployments
            .computeIfAbsent(ruleset.getRealmId(), (realm) ->
                new RulesDeployment<>(TenantRuleset.class, ruleset.getRealmId())
            );

        deployment.insertRuleset(ruleset);
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

    protected void deployAssetRuleset(AssetRuleset ruleset) {
        // Look for existing deployment for this asset
        RulesDeployment<AssetRuleset> deployment = assetDeployments
            .computeIfAbsent(ruleset.getAssetId(), (id) ->
                new RulesDeployment<>(AssetRuleset.class, id)
            );

        deployment.insertRuleset(ruleset);
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

    // TODO: What should we do when inserting facts into engines that aren't running? should we update the status to error?
    protected synchronized void insertFact(AssetUpdate assetUpdate, boolean skipStatusCheck) {
        // TODO: implement rules processing error state handling

        // Get the chain of rule engines that we need to pass through
        List<RulesDeployment> rulesDeployments = getEnginesInScope(assetUpdate.getRealmId(), assetUpdate.getPath());

        if (rulesDeployments.size() == 0) {
            LOG.fine("Ignoring asset update as there are no matching rules deployments: " + assetUpdate);
        }

        // Pass through each engine and try and insert the fact
        for (RulesDeployment deployment : rulesDeployments) {
            deployment.insertFact(assetUpdate);

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
        List<RulesDeployment> rulesDeployments = getEnginesInScope(assetUpdate.getRealmId(), assetUpdate.getPath());

        if (rulesDeployments.size() == 0) {
            LOG.fine("Ignoring asset update as there are no matching rules deployments: " + assetUpdate);
        }

        // Pass through each engine and retract this fact
        for (RulesDeployment deployment : rulesDeployments) {
            deployment.retractFact(assetUpdate);
        }
    }

    protected List<RulesDeployment> getEnginesInScope(String assetRealmId, String[] assetPath) {
        // We need to reverse the asset path to start from the top down
        assetPath = Util.reverseArray(assetPath, String.class);

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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}