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

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.util.Pair;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.AssetUpdate;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.rules.AssetRulesDefinition;
import org.openremote.manager.shared.rules.GlobalRulesDefinition;
import org.openremote.manager.shared.rules.RulesDefinition;
import org.openremote.manager.shared.rules.TenantRulesDefinition;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Attributes;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetMeta;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;

/**
 * Responsible for creating drools knowledge sessions for the rules definitions
 * and processing AssetUpdate events; an asset update is processed in the following
 * order: -
 * <p>
 * Global Definitions
 * Tenant Definitions
 * Asset Definitions (in hierarchical order from oldest ancestor down - ordering of
 * asset definitions with same parent asset are not guaranteed also processing order of definitions
 * with the same scope is not guaranteed)
 */
public class RulesService extends RouteBuilder implements ContainerService, Consumer<AssetUpdate> {
    private static final Logger LOG = Logger.getLogger(RulesService.class.getName());
    protected PersistenceService persistenceService;
    protected RulesStorageService rulesStorageService;
    protected ManagerIdentityService identityService;
    protected AssetStorageService assetStorageService;
    protected RulesDeployment<GlobalRulesDefinition> globalDeployment;
    protected final Map<String, RulesDeployment<TenantRulesDefinition>> tenantDeployments = new HashMap<>();
    protected final Map<String, RulesDeployment<AssetRulesDefinition>> assetDeployments = new HashMap<>();
    protected String[] activeTenantRealmIds;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        rulesStorageService = container.getService(RulesStorageService.class);
        identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void configure() throws Exception {
        // If any rules definition was modified in the database then check its' status and retract, insert or update it
        from(PERSISTENCE_TOPIC)
            .filter(isPersistenceEventForEntityType(RulesDefinition.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                processRulesDefinitionChange((RulesDefinition) persistenceEvent.getEntity(), persistenceEvent.getCause());
            });

        // If any tenant was modified in the database then check its' status and retract, insert or update any
        // associated rule definitions
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
                Asset asset = (Asset) persistenceEvent.getEntity();
                ServerAsset serverAsset = ServerAsset.map(asset, new ServerAsset());

                switch (persistenceEvent.getCause()) {
                    case INSERT:
                        // New asset has been created so get attributes that have RULES_FACT meta
                        Attributes addedAttributes = new Attributes(asset.getAttributes());
                        Arrays.stream(addedAttributes.get())
                            .filter(attribute -> attribute.hasMetaItem(AssetMeta.RULES_FACT))
                            .forEach(attribute -> {
                                AssetUpdate update = new AssetUpdate(serverAsset, attribute, attribute.getStateEvent(asset.getId()), null);
                                // TODO: What if a rule engine marks the fact as handled it should still go into other engines here???
                                insertFact(update);
                            });
                        break;
                    case UPDATE:
                        // TODO: Handle update case
                        break;
                    case DELETE:
                        // Retract any facts that were associated with this asset
                        Attributes removedAttributes = new Attributes(asset.getAttributes());
                        Arrays.stream(removedAttributes.get())
                            .filter(attribute -> attribute.hasMetaItem(AssetMeta.RULES_FACT))
                            .forEach(attribute -> {
                                AssetUpdate update = new AssetUpdate(serverAsset, attribute, attribute.getStateEvent(asset.getId()), null);
                                // TODO: What if a rule engine marks the fact as handled it should still go into other engines here???
                                retractFact(update);
                            });
                        break;
                }
                // TODO: Detect which attribute was created/updated in the database and handle it

            });
    }

    @Override
    public void start(Container container) throws Exception {
        // Deploy global rules
        rulesStorageService.findEnabledGlobalDefinitions().forEach(this::deployGlobalRulesDefinition);

        // Deploy tenant rules
        activeTenantRealmIds = identityService.getActiveTenantRealmIds();
        rulesStorageService.findEnabledTenantDefinitions()
            .stream()
            .filter(rd ->
                Arrays.stream(activeTenantRealmIds)
                    .anyMatch(activeTenantRealmId -> rd.getRealmId().equals(activeTenantRealmId))
            ).forEach(this::deployTenantRulesDefinition);

        // Deploy asset rules - group by assetID then tenant then order hierarchically
        deployAssetRulesDefinitions(rulesStorageService.findEnabledAssetDefinitions());

        // TODO: Look for attributes that contain RULES_FACT meta and push Attribute Events for them
        // into the rule engines.
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

        globalDeployment.stop();
        globalDeployment = null;
    }

    @Override
    public void accept(AssetUpdate assetUpdate) {
        insertFact(assetUpdate);
    }

    public synchronized void processTenantChange(Tenant tenant, PersistenceEvent.Cause cause) {
        // Check if enabled status has changed
        boolean wasEnabled = Arrays.asList(activeTenantRealmIds).contains(tenant.getId());
        boolean isEnabled = tenant.getEnabled() && cause != PersistenceEvent.Cause.DELETE;
        activeTenantRealmIds = identityService.getActiveTenantRealmIds();

        if (wasEnabled == isEnabled) {
            // Nothing to do here
            return;
        }

        if (wasEnabled) {
            // Remove tenant deployment for this tenant if it exists
            RulesDeployment<TenantRulesDefinition> tenantDeployment = tenantDeployments.get(tenant.getId());
            if (tenantDeployment != null) {
                Arrays.stream(tenantDeployment.getAllRulesDefinitions())
                    .forEach(this::retractTenantRulesDefinition);
            }

            // Remove any asset deployments for assets in this realm
            assetDeployments.values().stream().flatMap(
                assetRulesDeployment -> Arrays.stream(assetRulesDeployment.getAllRulesDefinitions())
            ).filter(assetRulesDefinition -> assetRulesDefinition.getRealmId().equals(tenant.getId()))
                .forEach(this::retractAssetRulesDefinition);

        } else {
            // Insert tenant deployment for this tenant if it has any rule definitions
            rulesStorageService
                .findEnabledTenantDefinitions(tenant.getId())
                .forEach(this::deployTenantRulesDefinition);

            // Insert any asset deployments for assets in this realm that have rule definitions
            deployAssetRulesDefinitions(rulesStorageService.findEnabledAssetDefinitions(tenant.getId()));
        }
    }

    protected void processRulesDefinitionChange(RulesDefinition rulesDefinition, PersistenceEvent.Cause cause) {
        if (cause == PersistenceEvent.Cause.DELETE || !rulesDefinition.isEnabled()) {
            if (rulesDefinition instanceof GlobalRulesDefinition) {
                retractGlobalRulesDefinition((GlobalRulesDefinition) rulesDefinition);
            } else if (rulesDefinition instanceof TenantRulesDefinition) {
                retractTenantRulesDefinition((TenantRulesDefinition) rulesDefinition);
            } else if (rulesDefinition instanceof AssetRulesDefinition) {
                retractAssetRulesDefinition((AssetRulesDefinition) rulesDefinition);
            }
        } else {
            if (rulesDefinition instanceof GlobalRulesDefinition) {
                deployGlobalRulesDefinition((GlobalRulesDefinition) rulesDefinition);
            } else if (rulesDefinition instanceof TenantRulesDefinition) {
                deployTenantRulesDefinition((TenantRulesDefinition) rulesDefinition);
            } else if (rulesDefinition instanceof AssetRulesDefinition) {
                deployAssetRulesDefinition((AssetRulesDefinition) rulesDefinition);
            }
        }
    }

    protected synchronized void deployGlobalRulesDefinition(GlobalRulesDefinition rulesDefinition) {
        // Global rules have access to everything in the system
        if (globalDeployment == null) {
            globalDeployment = new RulesDeployment<>(GlobalRulesDefinition.class, "GLOBAL");
        }

        globalDeployment.insertRulesDefinition(rulesDefinition);
    }

    protected synchronized void retractGlobalRulesDefinition(GlobalRulesDefinition rulesDefinition) {
        if (globalDeployment == null) {
            return;
        }

        globalDeployment.retractRulesDefinition(rulesDefinition);

        if (globalDeployment.isEmpty()) {
            globalDeployment = null;
        }
    }

    protected synchronized void deployTenantRulesDefinition(TenantRulesDefinition rulesDefinition) {
        // Look for existing deployment for this tenant
        RulesDeployment<TenantRulesDefinition> deployment = tenantDeployments
            .computeIfAbsent(rulesDefinition.getRealmId(), (realm) ->
                new RulesDeployment<>(TenantRulesDefinition.class, rulesDefinition.getRealmId())
            );

        deployment.insertRulesDefinition(rulesDefinition);
    }

    protected synchronized void retractTenantRulesDefinition(TenantRulesDefinition rulesDefinition) {
        RulesDeployment<TenantRulesDefinition> deployment = tenantDeployments.get(rulesDefinition.getRealmId());
        if (deployment == null) {
            return;
        }

        deployment.retractRulesDefinition(rulesDefinition);

        if (deployment.isEmpty()) {
            tenantDeployments.remove(rulesDefinition.getRealmId());
        }
    }

    protected void deployAssetRulesDefinitions(List<AssetRulesDefinition> assetRulesDefinitions) {
        assetRulesDefinitions
            .stream()
            .collect(Collectors.groupingBy(AssetRulesDefinition::getAssetId))
            .entrySet()
            .stream()
            .map(es ->
                new Pair<>(assetStorageService.find(es.getKey(), true), es.getValue())
            )
            .collect(Collectors.groupingBy(assetAndRules -> assetAndRules.key.getRealmId()))
            .entrySet()
            .stream()
            .filter(es -> Arrays
                .stream(activeTenantRealmIds)
                .anyMatch(at -> es.getKey().equals(at)))
            .forEach(es -> {
                List<Pair<ServerAsset, List<AssetRulesDefinition>>> tenantAssetAndRules = es.getValue();

                // RT: Not sure we need ordering here for starting engines so removing it
                // Order rules definitions by asset hierarchy within this tenant
                tenantAssetAndRules.stream()
                    //.sorted(Comparator.comparingInt(item -> item.key.getPath().length))
                    .flatMap(assetAndRules -> assetAndRules.value.stream())
                    .forEach(this::deployAssetRulesDefinition);
            });
    }

    protected void deployAssetRulesDefinition(AssetRulesDefinition rulesDefinition) {
        // Look for existing deployment for this asset
        RulesDeployment<AssetRulesDefinition> deployment = assetDeployments
            .computeIfAbsent(rulesDefinition.getAssetId(), (id) ->
                new RulesDeployment<>(AssetRulesDefinition.class, id)
            );

        deployment.insertRulesDefinition(rulesDefinition);
    }

    protected synchronized void retractAssetRulesDefinition(AssetRulesDefinition rulesDefinition) {
        RulesDeployment<AssetRulesDefinition> deployment = assetDeployments.get(rulesDefinition.getAssetId());
        if (deployment == null) {
            return;
        }

        deployment.retractRulesDefinition(rulesDefinition);

        if (deployment.isEmpty()) {
            assetDeployments.remove(rulesDefinition.getAssetId());
        }
    }

    // TODO: What should we do when inserting facts into engines that aren't running? should we update the status to error?
    protected synchronized void insertFact(AssetUpdate assetUpdate) {
        // TODO: implement rules processing error state handling

        // Get the chain of rule engines that we need to pass through
        ServerAsset asset = assetUpdate.getAsset();
        List<RulesDeployment> rulesDeployments = getEnginesInScope(asset.getRealmId(), asset.getReversePath());

        // Pass through each engine and try and insert the fact
        for (RulesDeployment deployment : rulesDeployments) {
            deployment.insertFact(assetUpdate);

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

    protected void retractFact(AssetUpdate assetUpdate) {
        // TODO: implement rules processing error state handling

        // Get the chain of rule engines that we need to pass through
        ServerAsset asset = assetUpdate.getAsset();
        List<RulesDeployment> rulesDeployments = getEnginesInScope(asset.getRealmId(), asset.getReversePath());

        // Pass through each engine and retract this fact
        for (RulesDeployment deployment : rulesDeployments) {
            deployment.retractFact(assetUpdate);
        }
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}