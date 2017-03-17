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
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.TimedRuleExectionOption;
import org.kie.api.runtime.rule.FactHandle;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.AssetUpdate;
import org.openremote.manager.shared.rules.RulesDefinition;
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeRef;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RulesDeployment<T extends RulesDefinition> {
    private static final Logger LOG = Logger.getLogger(RulesDeployment.class.getName());
    private static final int AUTO_START_DELAY_SECONDS = 2;
    static final protected RuleUtil ruleUtil = new RuleUtil();
    protected Map<Long, T> ruleDefinitions = new LinkedHashMap<>();
    protected RuleExecutionLogger ruleExecutionLogger = new RuleExecutionLogger();
    protected KieBase kb;
    protected KieSession knowledgeSession;
    protected KieServices kieServices;
    protected KieFileSystem kfs;
    protected String id;
    protected boolean error;
    protected boolean running;
    protected long currentFactCount;
    protected Class<T> clazz;
    final protected Map<AttributeRef, FactHandle> attributeFacts = new HashMap<>();
    protected static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    protected ScheduledFuture startTimer;

    public RulesDeployment(Class<T> clazz, String id) {
        this.clazz = clazz;
        this.id = id;
    }

    public synchronized T[] getAllRulesDefinitions() {
        T[] arr = (T[])Array.newInstance(clazz, 0);
        return ruleDefinitions.values().toArray(arr);
    }

    public String getId() {
        return id;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isError() {
        return error;
    }

    public synchronized boolean isEmpty() {
        return ruleDefinitions.isEmpty();
    }

    protected void setGlobal(String identifier, Object object) {
        try {
            knowledgeSession.setGlobal(identifier, object);
        } catch (Throwable t) {
            // Ignore, Drools complains if the DRL doesn't declare the global, but it works
        }
    }

    /**
     * Adds the rules definition to the engine by first stopping the engine and
     * then deploying new rules and then restarting the engine (after
     * {@link #AUTO_START_DELAY_SECONDS}) to prevent excessive engine stop/start.
     *
     * If engine is in an error state (one of the rules definitions failed to deploy
     * then the engine will not restart).
     *
     * @param rulesDefinition
     * @param rules
     * @return Whether or not the rules definition deployed successfully
     */
    public synchronized boolean insertRulesDefinition(T rulesDefinition, String rules) {
        if (rulesDefinition == null || rules == null || rules.isEmpty()) {
            // Assume it's a success if deploying an empty rules definition
            LOG.finest("Rules definition is empty so no rules to deploy");
            return true;
        }

        if (kfs == null) {
            // Initialise
            kieServices = KieServices.Factory.get();
            KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

            KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel("OpenRemoteKBase")
                    .setDefault(true)
                    .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
                    .setEventProcessingMode(EventProcessingOption.STREAM);
            kfs = kieServices.newKieFileSystem();
            kfs.writeKModuleXML(kieModuleModel.toXML());

            LOG.fine("Initialised rules service for deployment '" + getId() + "'");
            LOG.info(kieBaseModel.toString());
        }

        T existingDefinition = ruleDefinitions.get(rulesDefinition.getId());

        if (existingDefinition != null && existingDefinition.getVersion() == rulesDefinition.getVersion()) {
            LOG.fine("Rules definition version already deployed so ignoring");
            return true;
        }

        // TODO: What is the best way to handle live deployment of new rules
        if (isRunning()) {
            stop();
        }

        // Stop any running start timer
        if (startTimer != null) {
            startTimer.cancel(false);
        }

        // Check if rules definition is already deployed (maybe an older version)
        if (existingDefinition != null) {
            // Remove this old rules file
            kfs.delete("src/main/resources/" + rulesDefinition.getId());
            ruleDefinitions.remove(existingDefinition);
        }

        LOG.info("Adding rule definition: " + rulesDefinition);

        boolean addSuccessful = false;

        try {
            // ID will be unique within the scope of a rules deployment as rules definition will all be of same type
            kfs.write("src/main/resources/" + rulesDefinition.getId() + ".drl", rules);
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                Collection<Message> errors = kieBuilder.getResults().getMessages(Message.Level.ERROR);
                LOG.severe("Error in rule definition: " + rulesDefinition);
                for (Message error : errors) {
                    LOG.severe(error.getText());
                }
                // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
                kfs.delete("src/main/resources/" + rulesDefinition.getId());
            } else {
                LOG.info("Added rule definition: " + rulesDefinition);
                addSuccessful = true;
            }
        } catch (Throwable t) {
            LOG.log(
                    Level.SEVERE,
                    "Error in rule definition: " + rulesDefinition,
                    t
            );
            // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
            kfs.delete("src/main/resources/" + rulesDefinition.getId());
        }

        if (!addSuccessful) {
            // Prevent knowledge session from running again
            error = true;

            // Update status of each rules definition
            ruleDefinitions.forEach((id, rd) -> {
                if (rd.getDeploymentStatus() == RulesDefinition.DeploymentStatus.DEPLOYED) {
                    rd.setDeploymentStatus(RulesDefinition.DeploymentStatus.READY);
                }
            });
        } else {
            startTimer = executorService.schedule(this::start, AUTO_START_DELAY_SECONDS, TimeUnit.SECONDS);
        }

        // Add new rules definition
        rulesDefinition.setDeploymentStatus(addSuccessful ? RulesDefinition.DeploymentStatus.DEPLOYED : RulesDefinition.DeploymentStatus.FAILED);
        ruleDefinitions.put(rulesDefinition.getId(), rulesDefinition);

        return addSuccessful;
    }

    protected synchronized void retractRulesDefinition(RulesDefinition rulesDefinition) {
        if (kfs == null) {
            return;
        }

        T matchedDefinition = ruleDefinitions.get(rulesDefinition.getId());
        if (matchedDefinition == null) {
            LOG.finer("Rules definition cannot be retracted as it was never deployed: " + rulesDefinition);
            return;
        }

        // TODO: What is the best way to handle live deployment of new rules
        if (isRunning()) {
            stop();
        }

        // Stop any running start timer
        if (startTimer != null) {
            startTimer.cancel(false);
        }

        // Remove this old rules file
        kfs.delete("src/main/resources/" + rulesDefinition.getId());
        ruleDefinitions.remove(rulesDefinition.getId());

        // Update status of each rules definition
        boolean anyFailed = ruleDefinitions
                .values()
                .stream()
                .anyMatch(rd -> rd.getDeploymentStatus() == RulesDefinition.DeploymentStatus.FAILED);

        error = anyFailed;

        if (!anyFailed) {
            ruleDefinitions.forEach((id, rd) -> {
                if (rd.getDeploymentStatus() == RulesDefinition.DeploymentStatus.READY) {
                    rd.setDeploymentStatus(RulesDefinition.DeploymentStatus.DEPLOYED);
                }
            });
        }

        if (!isError() && !isEmpty()) {
            // Queue engine start
            startTimer = executorService.schedule(this::start, AUTO_START_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    protected synchronized void start() {
        if (isRunning()) {
            return;
        }

        if (isError()) {
            LOG.fine("Cannot start rules engine as one or more rule definitions are in error state");
            return;
        }

        if (isEmpty()) {
            LOG.finest("No rule definitions loaded so nothing to start");
            return;
        }

        LOG.fine("Starting RuleEngine: " + this);

        KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        kb = kieContainer.getKieBase();
        KieSessionConfiguration kieSessionConfiguration = kieServices.newKieSessionConfiguration();
        // Use this option to ensure timer rules are fired even in passive mode (triggered by fireAllRules.
        // This ensures compatibility with the behaviour of previously used Drools 5.1
        kieSessionConfiguration.setOption(TimedRuleExectionOption.YES);

        // TODO: Add support for configuring the drools clock per deployment
        //kieSessionConfiguration.setOption(ClockTypeOption.get("pseudo"));
        knowledgeSession = kb.newKieSession(kieSessionConfiguration, null);

        setGlobal("util", ruleUtil);
        setGlobal("JSON", Container.JSON);
        setGlobal("LOG", LOG);

        try {
            knowledgeSession.addEventListener(ruleExecutionLogger);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        running = true;
        LOG.info("Rule engine started");
        knowledgeSession.fireAllRules();
    }

    protected synchronized void stop() {
        if (!isRunning()) {
            return;
        }

        LOG.fine("Stopping RuleEngine: " + this);

        if (knowledgeSession != null) {
            knowledgeSession.halt();
            knowledgeSession.dispose();
            kb = null;
            LOG.fine("Knowledge session disposed");
        }

        attributeFacts.clear();
        running = false;
    }

    protected void processUpdate(AssetUpdate assetUpdate) {
        long newFactCount;

        AttributeEvent attributeEvent = assetUpdate.getNewState();

        // If this fact is not in working memory (considering its equals() method)...
        if (!knowledgeSession.getObjects().contains(attributeEvent)) {
            // If there already is a fact in working memory for this attribute then delete it
            FactHandle factHandle = attributeFacts.get(attributeEvent.getAttributeRef());

            if (factHandle != null) {
                try {
                    // ... retract it from working memory ...
                    LOG.finest("Removed stale attribute fact: " + attributeEvent.getAttributeRef());
                    knowledgeSession.delete(factHandle);
                } finally {
                    // ... and make sure we don't keep a reference to the stale fact
                    attributeFacts.remove(attributeEvent.getAttributeRef());
                }
            }

            // Put the fact into working memory and store the handle
            boolean isUpdate = factHandle != null;
            factHandle = knowledgeSession.insert(attributeEvent);
            attributeFacts.put(attributeEvent.getAttributeRef(), factHandle);
            LOG.finest(isUpdate ? "Updated: " : "Inserted: " + attributeEvent);
        }

        LOG.finest("Firing all rules");
        knowledgeSession.fireAllRules();


        // TODO: Prevent run away fact creation (not sure how we can do this reliably as facts can be generated in rule RHS)
        // MR: this is heuristic number which comes good for finding facts memory leak in the drl file.
        // problem - when you are not careful then drl can insert new facts till memory exhaustion. As there
        // are usually few 100 facts in drl's I'm working with, putting arbitrary number gives me early feedback
        // that there is potential problem. Perhaps we should think about a better solution to this problem?
        newFactCount = knowledgeSession.getFactCount();
        LOG.finest("New fact count: " + newFactCount);
        if (newFactCount != currentFactCount) {
            LOG.finest("Fact count changed from " + currentFactCount + " to " + newFactCount + " on: " + attributeEvent);
        }

        currentFactCount = newFactCount;
    }

    @Override
    public synchronized String toString() {
        return getClass().getSimpleName() + "{" +
                "name='" + id + '\'' +
                "running='" + running + '\'' +
                "error='" + error + '\'' +
                ", definitions='" + Arrays.toString(ruleDefinitions.values().stream().map(rd -> rd.getName() + ": " + rd.getDeploymentStatus()).toArray(size -> new String[size])) + '\'' +
                '}';
    }
}
