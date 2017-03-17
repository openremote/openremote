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
import org.openremote.manager.shared.rules.*;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Consumer;
import org.openremote.model.asset.Asset;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;

/**
 * Responsible for creating drools knowledge sessions for the rules definitions
 * and processing AssetUpdate events; an asset update is processed in the following
 * order: -
 *
 * Global Definitions
 * Tenant Definitions
 * Asset Definitions (in hierarchical order from oldest ancestor down - ordering of
 * asset definitions with same parent asset are not guaranteed also processing of definitions
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
                    processRulesDefinitionChange((RulesDefinition)persistenceEvent.getEntity(), persistenceEvent.getCause());
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
    }

    @Override
    public void start(Container container) throws Exception {
        // Get active tenants
        activeTenantRealmIds = identityService.getActiveTenantRealmIds();

        // Deploy global rules
        List<GlobalRulesDefinition> enabledGlobalDefinitions = rulesStorageService.findEnabledGlobalDefinitions();
        String[] globalRules = rulesStorageService.getRules(enabledGlobalDefinitions, GlobalRulesDefinition.class);

        if (globalRules.length != enabledGlobalDefinitions.size()) {
            LOG.warning("Failed to retrieve global rules definitions");
        } else {
            IntStream
                    .range(0, globalRules.length)
                    .forEach(i -> deployGlobalRulesDefinition(enabledGlobalDefinitions.get(i), globalRules[i]));
        }

        // Deploy tenant rules
        List<TenantRulesDefinition> enabledTenantDefinitions = rulesStorageService.findAllEnabledTenantDefinitions()
                .stream()
                .filter(rd ->
                        Arrays
                                .stream(activeTenantRealmIds)
                                .anyMatch(activeTenantRealmId -> rd.getRealmId().equals(activeTenantRealmId))
                ).collect(Collectors.toList());


        String[] tenantRules = rulesStorageService.getRules(enabledTenantDefinitions, TenantRulesDefinition.class);

        if (tenantRules.length != enabledTenantDefinitions.size()) {
            LOG.warning("Failed to retrieve tenant rules definitions");
        } else {
            IntStream
                    .range(0, tenantRules.length)
                    .forEach(i -> deployTenantRulesDefinition(enabledTenantDefinitions.get(i), tenantRules[i]));
        }

        // Deploy asset rules - group by assetID then tenant then order hierarchically
        deployAssetRulesDefinitions(rulesStorageService.findAllEnabledAssetDefinitions());
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
        processAssetUpdate(assetUpdate);
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
                        .forEach(rd -> retractTenantRulesDefinition(rd));
            }

            // Remove any asset deployments for assets in this realm
            List<String> assetIds = new ArrayList<>(assetDeployments.keySet());
            String[] realmIds = assetStorageService.getAssetRealmIds(assetIds);

            if (realmIds.length != assetDeployments.size()) {
                LOG.warning("Failed to retrieve asset realm ids");
            } else {
                for (int i=0; i<realmIds.length; i++) {
                    String realmId = realmIds[i];
                    if (realmId != null && realmId.equals(tenant.getId())) {
                        RulesDeployment<AssetRulesDefinition> assetDeployment = assetDeployments.get(assetIds.get(i));
                        Arrays.stream(assetDeployment.getAllRulesDefinitions())
                                .forEach(rd -> retractAssetRulesDefinition(rd));
                    }
                }
            }
        } else {
            // Insert tenant deployment for this tenant if it has any rule definitions
            List<TenantRulesDefinition> enabledTenantDefinitions = rulesStorageService
                    .findTenantDefinitions(tenant.getId())
                    .stream()
                    .filter(rd -> rd.isEnabled())
                    .collect(Collectors.toList());
            String[] tenantRules = rulesStorageService.getRules(enabledTenantDefinitions, TenantRulesDefinition.class);

            if (tenantRules.length != enabledTenantDefinitions.size()) {
                LOG.warning("Failed to retrieve tenant rules definitions");
            } else {
                IntStream
                        .range(0, tenantRules.length)
                        .forEach(i -> deployTenantRulesDefinition(enabledTenantDefinitions.get(i), tenantRules[i]));
            }

            // Insert any asset deployments for assets in this realm that have rule definitions
            deployAssetRulesDefinitions(rulesStorageService.findAllEnabledAssetDefinitions(tenant.getId()));
        }
    }

    protected void processRulesDefinitionChange(RulesDefinition rulesDefinition, PersistenceEvent.Cause cause) {
        if (cause == PersistenceEvent.Cause.DELETE || !rulesDefinition.isEnabled()) {
            if (rulesDefinition instanceof GlobalRulesDefinition) {
                retractGlobalRulesDefinition((GlobalRulesDefinition)rulesDefinition);
            } else if (rulesDefinition instanceof TenantRulesDefinition) {
                retractTenantRulesDefinition((TenantRulesDefinition)rulesDefinition);
            } else if (rulesDefinition instanceof AssetRulesDefinition) {
                retractAssetRulesDefinition((AssetRulesDefinition)rulesDefinition);
            }
        } else {
            if (rulesDefinition instanceof GlobalRulesDefinition) {
                deployGlobalRulesDefinition((GlobalRulesDefinition) rulesDefinition, rulesStorageService.getRules((GlobalRulesDefinition) rulesDefinition, GlobalRulesDefinition.class));
            } else if (rulesDefinition instanceof TenantRulesDefinition) {
                deployTenantRulesDefinition((TenantRulesDefinition)rulesDefinition, rulesStorageService.getRules((TenantRulesDefinition)rulesDefinition, TenantRulesDefinition.class));
            } else if (rulesDefinition instanceof AssetRulesDefinition) {
                deployAssetRulesDefinition((AssetRulesDefinition)rulesDefinition, rulesStorageService.getRules((AssetRulesDefinition)rulesDefinition, AssetRulesDefinition.class));
            }
        }
    }

//    protected boolean addResource(Resource resource, KieServices kieServices, KieFileSystem kfs) {
//        String sourcePath = resource.getSourcePath();
//        if (sourcePath == null) {
//            throw new IllegalArgumentException("Resource definition must have source path: " + resource);
//        }
//        LOG.info("Adding rule definition: " + resource);
//        try {
//
//            // TODO Drools config API is shit, this is hidden in some internal code
//            if (ResourceType.DTABLE.matchesExtension(sourcePath)) {
//                int lastDot = sourcePath.lastIndexOf('.');
//                if (lastDot >= 0 && sourcePath.length() > lastDot + 1) {
//                    String extension = sourcePath.substring(lastDot + 1);
//                    DecisionTableConfiguration tableConfig = KnowledgeBuilderFactory.newDecisionTableConfiguration();
//                    tableConfig.setInputType(DecisionTableInputType.valueOf(extension.toUpperCase()));
//                    resource.setConfiguration(tableConfig);
//                }
//            }
//
//            LOG.fine("Rule definition resource config: " + resource.getConfiguration().toProperties());
//            kfs.write("src/main/resources/" + sourcePath, resource);
//            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
//
//            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
//                Collection<Message> errors = kieBuilder.getResults().getMessages(Message.Level.ERROR);
//                LOG.severe("Error in rule definition: " + resource);
//                for (Message error : errors) {
//                    LOG.severe(error.getText());
//                }
//                // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
//                kfs.delete("src/main/resources/" + sourcePath);
//            } else {
//                LOG.info("Added rule definition: " + resource);
//                return true;
//            }
//        } catch (Throwable t) {
//            LOG.log(
//                Level.SEVERE,
//                "Error in rule definition: " + resource,
//                t
//            );
//            // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
//            kfs.delete("src/main/resources/" + sourcePath);
//        }
//        return false;
//    }

    protected synchronized void deployGlobalRulesDefinition(GlobalRulesDefinition rulesDefinition, String rules) {
        // Global rules have access to everything in the system
        if (globalDeployment == null) {
            globalDeployment = new RulesDeployment<>(GlobalRulesDefinition.class, "GLOBAL");
        }

        globalDeployment.insertRulesDefinition(rulesDefinition, rules);
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

    protected synchronized void deployTenantRulesDefinition(TenantRulesDefinition rulesDefinition, String rules) {
        // Look for existing deployment for this tenant
        RulesDeployment<TenantRulesDefinition> deployment = tenantDeployments
                .computeIfAbsent(rulesDefinition.getRealmId(), (realm) ->
                    new RulesDeployment<>(TenantRulesDefinition.class, rulesDefinition.getRealmId())
                );

        deployment.insertRulesDefinition(rulesDefinition, rules);
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
                        new Pair<>(assetStorageService.find(es.getKey()), es.getValue())
                )
                .collect(Collectors.groupingBy(assetAndRules -> assetAndRules.key.getRealmId()))
                .entrySet()
                .stream()
                .filter(es -> Arrays
                        .stream(activeTenantRealmIds)
                        .anyMatch(at -> es.getKey().equals(at)))
                .forEach(es -> {
                    List<Pair<ServerAsset, List<AssetRulesDefinition>>> tenantAssetAndRules = es.getValue();

                    // Order rules definitions by asset hierarchy within this tenant
                    tenantAssetAndRules.stream()
                            .sorted(Comparator.comparingInt(item -> item.key.getPath().length))
                            .forEach(assetAndRules -> {
                                List<AssetRulesDefinition> rulesDefinitions = assetAndRules.value;
                                String[] assetRules = rulesStorageService.getRules(rulesDefinitions, AssetRulesDefinition.class);

                                if (assetRules.length != rulesDefinitions.size()) {
                                    LOG.warning("Failed to retrieve asset rules definitions");
                                } else {
                                    IntStream
                                            .range(0, assetRules.length)
                                            .forEach(i -> deployAssetRulesDefinition(rulesDefinitions.get(i), assetRules[i]));
                                }
                            });
                });
    }


    protected void deployAssetRulesDefinition(AssetRulesDefinition rulesDefinition, String rules) {
        // Look for existing deployment for this asset
        RulesDeployment<AssetRulesDefinition> deployment = assetDeployments
                .computeIfAbsent(rulesDefinition.getAssetId(), (id) ->
                        new RulesDeployment<>(AssetRulesDefinition.class, id)
                );

        deployment.insertRulesDefinition(rulesDefinition, rules);
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

    protected synchronized void processAssetUpdate(AssetUpdate assetUpdate) {
        // TODO: implement rules processing error state handling

        // Get the chain of rule engines that we need to pass through
        ServerAsset asset = assetUpdate.getAsset();
        List<RulesDeployment> rulesDeployments = new ArrayList<>();

        // Add global engine (if it exists)
        if (globalDeployment != null) {
            rulesDeployments.add(globalDeployment);
        }

        // Add tenant engine (if it exists)
        RulesDeployment tenantDeployment = tenantDeployments.get(asset.getRealmId());

        if (tenantDeployment != null) {
         rulesDeployments.add(tenantDeployment);
        }

        // Add asset engines
        // Iterate through asset hierarchy using asset IDs from getPath
        for (String assetId : asset.getPath()) {
            RulesDeployment assetDeployment = assetDeployments.get(assetId);
            if (assetDeployment != null) {
                rulesDeployments.add(assetDeployment);
            }
        }

        // Pass through each engine if an engine is in error state then log and skip it
        for (RulesDeployment deployment : rulesDeployments) {
            synchronized (deployment) {
                if (deployment.isError()) {
                    LOG.warning("Rules engine is in error state so cannot process update event: " + deployment);
                } else if (!deployment.isRunning()) {
                    LOG.warning("Rules engine is not running so cannot process update event:" + deployment);
                } else {
                    // TODO: Handle any exceptions in rule RHS
                    deployment.processUpdate(assetUpdate);
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
    }
}
