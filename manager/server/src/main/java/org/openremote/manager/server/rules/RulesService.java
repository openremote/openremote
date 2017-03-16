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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.util.Pair;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.AssetUpdate;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.rules.*;
import org.openremote.model.Consumer;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
// TODO: Listen to changes in tenant enabled status and start/stop engines accordingly
// TODO: Listen to asset changes and start/stop engines accordingly
// TODO: Listen to rule definition changes and update engines accordingly
// TODO: When restarting rules engine should we wait for executing rules to finish?
public class RulesService implements ContainerService, Consumer<AssetUpdate> {
    private static final Logger LOG = Logger.getLogger(RulesService.class.getName());
    protected PersistenceService persistenceService;
    protected RulesStorageService rulesStorageService;
    protected ManagerIdentityService identityService;
    protected AssetStorageService assetStorageService;
    protected RulesDeployment<GlobalRulesDefinition> globalDeployment;
    protected final Map<String, RulesDeployment<TenantRulesDefinition>> tenantDeployments = new HashMap<>();
    protected final Map<String, RulesDeployment<AssetRulesDefinition>> assetDeployments = new HashMap<>();
    protected String[] activeTenantNames;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        rulesStorageService = container.getService(RulesStorageService.class);
        identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
    }

    @Override
    public void start(Container container) throws Exception {
        // Get active tenants
        String[] activeTenantNames = identityService.getActiveTenantRealms();

        // Deploy global rules
        List<GlobalRulesDefinition> enabledGlobalDefinitions = rulesStorageService.findEnabledGlobalDefinitions();
        String[] globalRules = rulesStorageService.getRules(enabledGlobalDefinitions, GlobalRulesDefinition.class);

        if (globalRules.length != enabledGlobalDefinitions.size()) {
            LOG.warning("Failed to retrieve global rules definitions");
        } else {
            IntStream
                    .range(0, globalRules.length)
                    .forEach(i -> deployGlobalRulesDefinition(enabledGlobalDefinitions.get(i), globalRules[i]));

            // Start the global rule engine (it will prevent start if there was any error)
            if (globalDeployment != null) {
                globalDeployment.start();
            }
        }

        // Deploy tenant rules
        List<TenantRulesDefinition> enabledTenantDefinitions = rulesStorageService.findAllEnabledTenantDefinitions()
                .stream()
                .filter(rd ->
                        Arrays
                                .stream(activeTenantNames)
                                .anyMatch(activeTenant -> rd.getRealm().equals(activeTenant))
                ).collect(Collectors.toList());


        String[] tenantRules = rulesStorageService.getRules(enabledTenantDefinitions, TenantRulesDefinition.class);

        if (tenantRules.length != enabledTenantDefinitions.size()) {
            LOG.warning("Failed to retrieve tenant rules definitions");
        } else {
            IntStream
                    .range(0, tenantRules.length)
                    .forEach(i -> deployTenantRulesDefinition(enabledTenantDefinitions.get(i), tenantRules[i]));

            // Start the tenant rule engines (they will prevent start if there was any error)
            tenantDeployments.forEach((realm, td) -> td.start());
        }

        // Deploy asset rules - group by assetID then tenant then order hierarchically
        rulesStorageService.findAllEnabledAssetDefinitions()
                .stream()
                .collect(Collectors.groupingBy(AssetRulesDefinition::getAssetId))
                .entrySet()
                .stream()
                .map(es ->
                        new Pair<>(assetStorageService.find(es.getKey()), es.getValue())
                )
                .collect(Collectors.groupingBy(assetAndRules -> assetAndRules.key.getRealm()))
                .entrySet()
                .stream()
                .filter(es -> Arrays
                        .stream(activeTenantNames)
                        .anyMatch(at -> es.getKey().equals(at)))
                .forEach(e ->
                        deployTenantAssetRulesDefinitions(e.getValue())
                );

        // Start the asset rule engines (they will prevent start if there was any error)
        assetDeployments.forEach((id, ad) -> ad.start());
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

    public void processTenantChange() {

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

        globalDeployment.addRulesDefinition(rulesDefinition, rules);
    }

    protected synchronized void deployTenantRulesDefinition(TenantRulesDefinition rulesDefinition, String rules) {
        // Look for existing deployment for this tenant
        RulesDeployment<TenantRulesDefinition> deployment = tenantDeployments
                .computeIfAbsent(rulesDefinition.getRealm(), (realm) ->
                    new RulesDeployment<>(TenantRulesDefinition.class, rulesDefinition.getRealm())
                );

        deployment.addRulesDefinition(rulesDefinition, rules);
    }

    protected void deployTenantAssetRulesDefinitions(List<Pair<ServerAsset, List<AssetRulesDefinition>>> assetAndRules) {
        // Order rules definitions by asset hierarchy
        assetAndRules.stream()
                .sorted(Comparator.comparingInt(item -> item.key.getPath().length))
                .forEach(assetAndRule ->
                        deployAssetRulesDefinitions(assetAndRule.key.getId(), assetAndRule.value)
                );
    }

    protected void deployAssetRulesDefinitions(String assetId, List<AssetRulesDefinition> rulesDefinitions) {
        // Look for existing deployment for this asset

        RulesDeployment<AssetRulesDefinition> deployment = assetDeployments
                .computeIfAbsent(assetId, (id) ->
                    new RulesDeployment<>(AssetRulesDefinition.class, id)
                );

        String[] assetRules = rulesStorageService.getRules(rulesDefinitions, AssetRulesDefinition.class);

        if (assetRules.length != rulesDefinitions.size()) {
            LOG.warning("Failed to retrieve asset rules definitions");
        } else {
            IntStream
                    .range(0, assetRules.length)
                    .forEach(i -> deployment.addRulesDefinition(rulesDefinitions.get(i), assetRules[i]));
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
        RulesDeployment tenantDeployment = tenantDeployments.get(asset.getRealm());

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
