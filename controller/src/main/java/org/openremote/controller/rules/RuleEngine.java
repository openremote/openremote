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

import org.drools.core.builder.conf.impl.DecisionTableConfigurationImpl;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceConfiguration;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.TimedRuleExectionOption;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceTypeImpl;
import org.openremote.controller.event.CommandFacade;
import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventContext;
import org.openremote.controller.event.EventProcessor;
import org.openremote.controller.statuscache.LevelFacade;
import org.openremote.controller.statuscache.RangeFacade;
import org.openremote.controller.statuscache.SwitchFacade;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class RuleEngine extends EventProcessor {

    private static final Logger LOG = Logger.getLogger(RuleEngine.class.getName());

    private KieBase kb;
    private KieSession knowledgeSession;
    private Map<Integer, FactHandle> eventSources = new HashMap<>();
    private long factCount;

    private SwitchFacade switchFacade;
    private LevelFacade levelFacade;
    private RangeFacade rangeFacade;

    @Override
    public String getName() {
        return "Drools Rule Engine";
    }

    @Override
    public void push(EventContext ctx) {
        // if we got no rules, just push event back to next processor...
        if (kb == null) {
            LOG.fine("No knowledge base configured, skipping processing of event: " + ctx.getEvent());
            return;
        }

        Event evt = ctx.getEvent();

        switchFacade.pushEventContext(ctx);
        levelFacade.pushEventContext(ctx);
        rangeFacade.pushEventContext(ctx);

        try {
            long factNewCount;
            if (!knowledgeSession.getObjects().contains(evt)) {
                boolean debug = true;
                if (eventSources.keySet().contains(evt.getSourceID())) {
                    try {
                        knowledgeSession.delete(eventSources.get(evt.getSourceID()));
                    } finally {
                        // Doing this in the finally to make sure we don't keep a reference to a fact when we should not (ORCJAVA-407)
                        eventSources.remove(evt.getSourceID());
                    }
                    debug = false;
                }

                FactHandle handle = knowledgeSession.insert(evt);
                eventSources.put(evt.getSourceID(), handle);

                if (debug) {
                    LOG.fine("Inserted: " + evt);
                }
                factNewCount = knowledgeSession.getFactCount();
                LOG.fine("Fact count: " + factNewCount);
                if (factNewCount != factCount) {
                    LOG.fine("Fact count changed from " + factCount + " to " + factNewCount + " on '" + evt.getSource() + "'");
                }
                factCount = factNewCount;
            }

            knowledgeSession.fireAllRules();

            factNewCount = knowledgeSession.getFactCount();
            if (factNewCount >= 1000) // look for runaway insertion of facts
                if (factNewCount != factCount) {
                    LOG.fine("Fact count changed from " + factCount + " to " + factNewCount + " on '" + evt.getSource() + "'");
                }
            factCount = factNewCount;
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error in executing rule : evt.getSource() + \":\" + t.getMessage()\n\tEvent " + ctx.getEvent() + " not processed!", t);
            if (t.getCause() != null) {
                LOG.log(Level.SEVERE, "Root Cause: \n", t.getCause());
            }
        }
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void start(CommandFacade commandFacade) throws Exception {
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

        addResources(kieServices, kfs);

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

        try {
            knowledgeSession.setGlobal("execute", commandFacade);
        } catch (Throwable t) {
            // TODO So, we never see any problems?
        }

        try {
            knowledgeSession.setGlobal("switches", switchFacade);
        } catch (Throwable t) {
        }

        try {
            knowledgeSession.setGlobal("ranges", rangeFacade);
        } catch (Throwable t) {
        }

        try {
            knowledgeSession.setGlobal("levels", levelFacade);
        } catch (Throwable t) {
        }

        try {
            RuleExecutionLogger ruleExecutionLogger = new RuleExecutionLogger();
            knowledgeSession.addEventListener(ruleExecutionLogger);
        } catch (Throwable t) {
            LOG.fine("Exception in addEventListener");
        }

        LOG.fine("Rule engine started");

        knowledgeSession.fireAllRules();
    }

    @Override
    public void stop() {
        LOG.fine("Stopping RuleEngine");
        if (knowledgeSession != null) {
            knowledgeSession.dispose();
            LOG.fine("Knowledge session disposed");
        }
        eventSources.clear();

        // We're disposing of the knowledge base, don't keep references to any facts (ORCJAVA-407)
        eventSources.clear();

        kb = null;
    }

    protected void addResources(KieServices kieServices, KieFileSystem kfs) {
        getResources(kieServices).forEach(resource -> {
            String sourcePath = resource.getSourcePath();
            if (sourcePath == null) {
                throw new IllegalArgumentException("Resource definition must have source path: " + resource);
            }
            LOG.fine("Adding rule definition: " + resource);
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
        });
    }

    abstract protected Stream<Resource> getResources(KieServices kieServices);

    @Override
    public String toString() {
        return "RuleEngine{" +
            "factCount=" + factCount +
            "}";
    }
}

