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

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.TimedRuleExectionOption;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.model.AttributeRef;
import org.openremote.model.Consumer;
import org.openremote.model.asset.AssetStateChange;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AssetRulesService implements ContainerService,
    Consumer<AssetStateChange<ServerAsset>> {

    private static final Logger LOG = Logger.getLogger(AssetRulesService.class.getName());

    protected KieBase kb;
    protected KieSession knowledgeSession;
    final protected Map<AttributeRef, FactHandle> attributeFacts = new HashMap<>();
    final protected RuleUtil ruleUtil = new RuleUtil();
    protected long currentFactCount;

    @Override
    public void init(Container container) throws Exception {

    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {
        stopEngine();
    }

    protected void stopEngine() {
        LOG.fine("Stopping RuleEngine");
        if (knowledgeSession != null) {
            knowledgeSession.dispose();
            LOG.fine("Knowledge session disposed");
        }

        attributeFacts.clear();
        kb = null;
    }

    // TODO: Support runtime re-deployment
    public void deploy(RulesProvider rulesProvider) {
        if (rulesProvider == null) {
            return;
        }

        stopEngine();

        KieServices kieServices = KieServices.Factory.get();
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel("OpenRemoteKBase")
            .setDefault(true)
            .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
            .setEventProcessingMode(EventProcessingOption.STREAM);
        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.writeKModuleXML(kieModuleModel.toXML());

        rulesProvider.getResources(kieServices).forEach(resource -> {
                if (!addResource(resource, kieServices, kfs)) {
                    // Fail hard and fast
                    LOG.severe("Failed to deploy rules resource '" + resource.getSourcePath() + "' so rules engine won't run");
                    throw new RuntimeException();
                }
            }
        );

        KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());

        kb = kieContainer.getKieBase();

        KieSessionConfiguration kieSessionConfiguration = kieServices.newKieSessionConfiguration();
        // Use this option to ensure timer rules are fired even in passive mode (triggered by fireAllRules.
        // This ensures compatibility with the behaviour of previously used Drools 5.1
        kieSessionConfiguration.setOption(TimedRuleExectionOption.YES);

        if (rulesProvider.getClockType() != null) {
            kieSessionConfiguration.setOption(rulesProvider.getClockType());
        }

        knowledgeSession = kb.newKieSession(kieSessionConfiguration, null);

        setGlobal("util", ruleUtil);
        setGlobal("JSON", Container.JSON);
        setGlobal("LOG", LOG);

        try {
            RuleExecutionLogger ruleExecutionLogger = new RuleExecutionLogger();
            knowledgeSession.addEventListener(ruleExecutionLogger);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        LOG.info("Rule engine started");
        LOG.info(kieBaseModel.toString());

        knowledgeSession.fireAllRules();
    }

    @Override
    public void accept(AssetStateChange<ServerAsset> stateChange) {
        if (kb == null) {
            LOG.fine("No knowledge base configured, skipping processing of: " + stateChange);
            return;
        }

        long newFactCount;

        // If this fact is not in working memory (considering its equals() method)...
        if (!knowledgeSession.getObjects().contains(stateChange)) {

            // ... update or new insert?
            boolean isUpdate = true;

            AttributeRef attributeRef = stateChange.getOriginalState().getAttributeRef();

            // If there already is a fact in working memory for this attribute
            if (attributeFacts.keySet().contains(attributeRef)) {
                try {
                    // ... retract it from working memory ...
                    knowledgeSession.delete(attributeFacts.get(attributeRef));
                } finally {
                    // ... and make sure we don't keep a reference to a fact
                    attributeFacts.remove(attributeRef);
                }
            } else {
                isUpdate = false;
            }

            // Put the fact into working memory and remember the handle
            FactHandle handle = knowledgeSession.insert(stateChange);
            attributeFacts.put(attributeRef, handle);

            if (isUpdate) {
                LOG.finest("Inserted: " + stateChange);
            } else {
                LOG.finest("Updated: " + stateChange);
            }

            // TODO: Prevent run away fact creation (not sure how we can do this reliably as facts can be generated in rule RHS)
            // MR: this is heuristic number which comes good for finding facts memory leak in the drl file.
            // problem - when you are not careful then drl can insert new facts till memory exhaustion. As there
            // are usually few 100 facts in drl's I'm working with, putting arbitrary number gives me early feedback
            // that there is potential problem. Perhaps we should think about a better solution to this problem?
            newFactCount = knowledgeSession.getFactCount();
            LOG.finest("New fact count: " + newFactCount);
            if (newFactCount != currentFactCount) {
                LOG.finest("Fact count changed from " + currentFactCount + " to " + newFactCount + " on: " + stateChange);
            }
            currentFactCount = newFactCount;
        }

        LOG.finest("Firing all rules");
        knowledgeSession.fireAllRules();
    }

    public KieSession getKnowledgeSession() {
        return knowledgeSession;
    }

    protected void setGlobal(String identifier, Object object) {
        try {
            knowledgeSession.setGlobal(identifier, object);
        } catch (Throwable t) {
            // Ignore, Drools complains if the DRL doesn't declare the global, but it works
        }
    }

    protected boolean addResource(Resource resource, KieServices kieServices, KieFileSystem kfs) {
        String sourcePath = resource.getSourcePath();
        if (sourcePath == null) {
            throw new IllegalArgumentException("Resource definition must have source path: " + resource);
        }
        LOG.info("Adding rule definition: " + resource);
        try {

            // TODO Drools config API is shit, this is hidden in some internal code
            if (ResourceType.DTABLE.matchesExtension(sourcePath)) {
                int lastDot = sourcePath.lastIndexOf('.');
                if (lastDot >= 0 && sourcePath.length() > lastDot + 1) {
                    String extension = sourcePath.substring(lastDot + 1);
                    DecisionTableConfiguration tableConfig = KnowledgeBuilderFactory.newDecisionTableConfiguration();
                    tableConfig.setInputType(DecisionTableInputType.valueOf(extension.toUpperCase()));
                    resource.setConfiguration(tableConfig);
                }
            }

            LOG.fine("Rule definition resource config: " + resource.getConfiguration().toProperties());
            kfs.write("src/main/resources/" + sourcePath, resource);
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                Collection<Message> errors = kieBuilder.getResults().getMessages(Message.Level.ERROR);
                LOG.severe("Error in rule definition: " + resource);
                for (Message error : errors) {
                    LOG.severe(error.getText());
                }
                // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
                kfs.delete("src/main/resources/" + sourcePath);
            } else {
                LOG.info("Added rule definition: " + resource);
                return true;
            }
        } catch (Throwable t) {
            LOG.log(
                Level.SEVERE,
                "Error in rule definition: " + resource,
                t
            );
            // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
            kfs.delete("src/main/resources/" + sourcePath);
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "factCount=" + currentFactCount +
            "}";
    }
}
