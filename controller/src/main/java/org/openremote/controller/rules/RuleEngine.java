/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2016, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
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
package org.openremote.controller.rules;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
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
import org.openremote.controller.deploy.Deployment;
import org.openremote.controller.sensor.SensorState;
import org.openremote.controller.sensor.SensorStateUpdate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RuleEngine {

    private static final Logger LOG = Logger.getLogger(RuleEngine.class.getName());

    final protected Deployment deployment;

    private KieBase kb;
    private KieSession knowledgeSession;
    private Map<Integer, FactHandle> stateSources = new HashMap<>();
    private long currentFactCount;

    private SwitchFacade switchFacade;
    private LevelFacade levelFacade;
    private RangeFacade rangeFacade;
    private RulePersistence rulePersistence;
    private RuleUtil ruleUtil;

    public RuleEngine(Deployment deployment) {
        this.deployment = deployment;
    }

    public void start() {
        KieServices kieServices = KieServices.Factory.get();
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel("OpenRemoteKBase")
            .setDefault(true)
            .setEqualsBehavior(EqualityBehaviorOption.EQUALITY);
        KieSessionModel kieSessionModel = kieBaseModel.newKieSessionModel("OpenRemoteKSession")
            .setDefault(true)
            .setType(KieSessionModel.KieSessionType.STATEFUL);

        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.writeKModuleXML(kieModuleModel.toXML());

        deployment.getRulesProvider().getResources(kieServices).forEach(resource ->
            addResource(resource, kieServices, kfs)
        );

        KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());

        kb = kieContainer.getKieBase();

        KieSessionConfiguration kieSessionConfiguration = kieServices.newKieSessionConfiguration();
        // Use this option to ensure timer rules are fired even in passive mode (triggered by fireAllRules.
        // This ensures compatibility with the behaviour of previous controller using drools 5.1
        kieSessionConfiguration.setOption(TimedRuleExectionOption.YES);
        knowledgeSession = kb.newKieSession(kieSessionConfiguration, null);

        switchFacade = new SwitchFacade();
        rangeFacade = new RangeFacade();
        levelFacade = new LevelFacade();
        rulePersistence = new RulePersistence();
        ruleUtil = new RuleUtil();

        setGlobal("commands", deployment.getCommands());
        setGlobal("switches", switchFacade);
        setGlobal("ranges", rangeFacade);
        setGlobal("levels", levelFacade);
        setGlobal("persistence", rulePersistence);
        setGlobal("util", ruleUtil);
        setGlobal("JSON", Container.JSON);
        setGlobal("LOG", LOG);

        try {
            RuleExecutionLogger ruleExecutionLogger = new RuleExecutionLogger();
            knowledgeSession.addEventListener(ruleExecutionLogger);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        LOG.fine("Rule engine started");

        knowledgeSession.fireAllRules();
    }

    public void stop() {
        LOG.fine("Stopping RuleEngine");
        if (knowledgeSession != null) {
            knowledgeSession.dispose();
            LOG.fine("Knowledge session disposed");
        }
        stateSources.clear();

        // We're disposing of the knowledge base, don't keep references to any facts (ORCJAVA-407)
        stateSources.clear();

        kb = null;
    }

    public void process(SensorStateUpdate sensorStateUpdate) {
        if (kb == null) {
            LOG.fine("No knowledge base configured, skipping processing of: " + sensorStateUpdate);
            return;
        }

        SensorState sensorState = sensorStateUpdate.getSensorState();

        switchFacade.process(sensorStateUpdate);
        levelFacade.process(sensorStateUpdate);
        rangeFacade.process(sensorStateUpdate);

        try {
            long newFactCount;

            // If this fact is not in working memory (considering its equals() method)...
            if (!knowledgeSession.getObjects().contains(sensorState)) {

                // ... update or new insert?
                boolean isUpdate = true;

                // If there already is a fact in wm with the same sensor ID ...
                if (stateSources.keySet().contains(sensorState.getSensorID())) {
                    try {
                        // ... retract it from working memory ...
                        knowledgeSession.delete(stateSources.get(sensorState.getSensorID()));
                    } finally {
                        // ... and make we don't keep a reference to a fact
                        stateSources.remove(sensorState.getSensorID());
                    }
                } else {
                    isUpdate = false;
                }

                // Put the fact into wm and remember the handle
                FactHandle handle = knowledgeSession.insert(sensorState);
                stateSources.put(sensorState.getSensorID(), handle);

                if (isUpdate) {
                    LOG.fine("Inserted: " + sensorState);
                } else {
                    LOG.fine("Updated: " + sensorState);
                }

                newFactCount = knowledgeSession.getFactCount();
                LOG.fine("New fact count: " + newFactCount);
                if (newFactCount != currentFactCount) {
                    LOG.fine("Fact count changed from " + currentFactCount + " to " + newFactCount + " on '" + sensorState.getSensorName() + "'");
                }
                currentFactCount = newFactCount;
            }

            LOG.fine("Firing all rules");
            knowledgeSession.fireAllRules();

            // TODO: This doesn't look right, why would it matter if there are 1000 facts?  Who says that
            newFactCount = knowledgeSession.getFactCount();
            if (newFactCount >= 1000) // look for runaway insertion of facts
                if (newFactCount != currentFactCount) {
                    LOG.fine("Fact count changed from " + currentFactCount + " to " + newFactCount + " on '" + sensorState.getSensorName() + "'");
                }
            currentFactCount = newFactCount;

        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error executing rules for: " + sensorStateUpdate.getSensorState(), t);
            if (t.getCause() != null) {
                LOG.log(Level.SEVERE, "Root Cause: \n", t.getCause());
            }
        }
    }

    protected void setGlobal(String identifier, Object object) {
        try {
            knowledgeSession.setGlobal(identifier, object);
        } catch (Throwable t) {
            // Ignore, Drools complains if the DRL doesn't declare the global, but it works
        }
    }

    protected void addResource(Resource resource, KieServices kieServices, KieFileSystem kfs) {
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
    }

    @Override
    public String toString() {
        return "RuleEngine{" +
            "factCount=" + currentFactCount +
            "}";
    }
}

