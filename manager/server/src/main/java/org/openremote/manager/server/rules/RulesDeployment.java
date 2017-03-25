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

import elemental.json.Json;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.conf.TimedRuleExectionOption;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.time.SessionClock;
import org.openremote.container.util.Util;
import org.openremote.model.asset.AssetUpdate;
import org.openremote.manager.shared.rules.RulesDefinition;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RulesDeployment<T extends RulesDefinition> {
    public static final Logger LOG = Logger.getLogger(RulesDeployment.class.getName());
    // This is here so Clock Type can be set to pseudo from tests
    protected static ClockTypeOption DefaultClockType;
    private static final int AUTO_START_DELAY_SECONDS = 2;
    private static Long counter = 1L;
    static final protected Util UTIL = new Util();
    protected final Map<Long, T> ruleDefinitions = new LinkedHashMap<>();
    protected final RuleExecutionLogger ruleExecutionLogger = new RuleExecutionLogger();
    protected KieSession knowledgeSession;
    protected KieServices kieServices;
    protected KieFileSystem kfs;
    // We need to be able to reference the KieModule dynamically generated for this engine
    // from the singleton KieRepository to do this we need a pom.xml file with a release ID - crazy drools!!
    protected ReleaseId releaseId;
    protected final String id;
    protected boolean error;
    protected boolean running;
    protected long currentFactCount;
    protected final Class<T> clazz;
    final protected Map<AssetUpdate, FactHandle> facts = new HashMap<>();
    protected static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    protected ScheduledFuture startTimer;

    public RulesDeployment(Class<T> clazz, String id) {
        this.clazz = clazz;
        this.id = id;
    }

    protected synchronized static Long getNextCounter() {
        return counter++;
    }

    @SuppressWarnings("unchecked")
    public synchronized T[] getAllRulesDefinitions() {
        T[]arr = Util.createArray(ruleDefinitions.size(), clazz);
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

    public KieSession getKnowledgeSession() {
        return knowledgeSession;
    }

    public SessionClock getSessionClock() {
        KieSession session = getKnowledgeSession();
        if (session != null) {
            return session.getSessionClock();
        }

        return null;
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
     * <p>
     * If engine is in an error state (one of the rules definitions failed to deploy
     * then the engine will not restart).
     *
     * @return Whether or not the rules definition deployed successfully
     */
    public synchronized boolean insertRulesDefinition(T rulesDefinition) {
        if (rulesDefinition == null || rulesDefinition.getRules() == null || rulesDefinition.getRules().isEmpty()) {
            // Assume it's a success if deploying an empty rules definition
            LOG.finest("Rules definition is empty so no rules to deploy");
            return true;
        }

        if (kfs == null) {
            initialiseEngine();
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
            //noinspection SuspiciousMethodCalls
            ruleDefinitions.remove(existingDefinition);
        }

        LOG.info("Adding rule definition: " + rulesDefinition);

        boolean addSuccessful = false;

        try {
            // ID will be unique within the scope of a rules deployment as rules definition will all be of same type
            kfs.write("src/main/resources/" + rulesDefinition.getId() + ".drl", rulesDefinition.getRules());
            // Unload the rules string from the definition we don't need it anymore and don't want it using memory
            rulesDefinition.setRules(null);
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
            LOG.log(Level.SEVERE, "Error in rule definition: " + rulesDefinition, t);
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

    protected void initialiseEngine() {
        // Initialise
        kieServices = KieServices.Factory.get();
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

        String versionId = getNextCounter().toString();
        releaseId = kieServices.newReleaseId("org.openremote", "openremote-kiemodule", versionId);
        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel("OpenRemoteKModule");
        ClockTypeOption clockType = DefaultClockType != null ? DefaultClockType : ClockTypeOption.get("realtime");

        kieBaseModel
                .setDefault(true)
                .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
                .setEventProcessingMode(EventProcessingOption.STREAM)
                .newKieSessionModel("ksession1")
                .setDefault(true)
                .setType(KieSessionModel.KieSessionType.STATEFUL)
                .setClockType(clockType);
        kfs = kieServices.newKieFileSystem();
        kfs.generateAndWritePomXML(releaseId);
        kfs.writeKModuleXML(kieModuleModel.toXML());

        LOG.fine("Initialised rules service for deployment '" + getId() + "'");
        LOG.info(kieBaseModel.toString());
    }

    protected synchronized void start() {
        if (isRunning()) {
            return;
        }

        if (isError()) {
            LOG.fine("Cannot start rules engine as an error occurred during startup");
            return;
        }

        if (isEmpty()) {
            LOG.finest("No rule definitions loaded so nothing to start");
            return;
        }

        LOG.fine("Starting RuleEngine: " + this);

        // Note each rule engine has its' own KieModule which are stored in a singleton register by drools
        // we need to ensure we get the right module here otherwise we could be using the wrong rules
        KieContainer kieContainer = kieServices.newKieContainer(releaseId);
        KieSessionConfiguration kieSessionConfiguration = kieServices.newKieSessionConfiguration();
        // Use this option to ensure timer rules are fired even in passive mode (triggered by fireAllRules.
        // This ensures compatibility with the behaviour of previously used Drools 5.1
        kieSessionConfiguration.setOption(TimedRuleExectionOption.YES);

        try {
            knowledgeSession = kieContainer.newKieSession(kieSessionConfiguration);

            setGlobal("util", UTIL);
            setGlobal("JSON", Json.instance());
            setGlobal("LOG", LOG);

            knowledgeSession.addEventListener(ruleExecutionLogger);

            // Insert all the facts into the session
            Set<Map.Entry<AssetUpdate, FactHandle>> factEntries = facts.entrySet();
            facts.clear();
            factEntries.forEach(factEntry -> insertFactIntoSession(factEntry.getKey()));
        } catch (Throwable t) {
            error = true;
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
            LOG.fine("Knowledge session disposed");
        }

        facts.clear();
        running = false;
    }

    protected synchronized void insertFact(AssetUpdate assetUpdate) {
        // Check if fact already exists using equals()
        if (!facts.containsKey(assetUpdate)) {
            // Delete any existing fact for this attribute ref
            // Put the fact into working memory and store the handle
            retractFact(assetUpdate);
            insertFactIntoSession(assetUpdate);
        }
    }

    protected synchronized void retractFact(AssetUpdate assetUpdate) {

        // If there already is a fact in working memory for this attribute then delete it
        AssetUpdate update = facts.keySet()
                .stream()
                .filter(au -> au.getAssetId().equals(assetUpdate.getAssetId()) && au.getAttributeName().equals(assetUpdate.getAttributeName()))
                .findFirst()
                .orElse(null);

        FactHandle factHandle = update != null ? facts.get(update) : null;

        if (factHandle != null && knowledgeSession != null) try {
            // ... retract it from working memory ...
            LOG.finest("Removed stale attribute fact: " + update);
            knowledgeSession.delete(factHandle);
        } finally {
            // ... and make sure we don't keep a reference to the stale fact
            facts.remove(update);
        }
    }

    protected FactHandle insertFactIntoSession(AssetUpdate assetUpdate) {
        FactHandle factHandle = null;

        if (!isError() && isRunning()) {
            long newFactCount;
            factHandle = knowledgeSession.insert(assetUpdate);
            facts.put(assetUpdate, factHandle);
            LOG.finest("Inserted fact into session");

            LOG.finest("On " + this + ", firing all rules");
            int fireCount = knowledgeSession.fireAllRules();
            LOG.finest("On " + this + ", fired rules count: " + fireCount);

            // TODO: Prevent run away fact creation (not sure how we can do this reliably as facts can be generated in rule RHS)
            // MR: this is heuristic number which comes good for finding facts memory leak in the drl file.
            // problem - when you are not careful then drl can insert new facts till memory exhaustion. As there
            // are usually few 100 facts in drl's I'm working with, putting arbitrary number gives me early feedback
            // that there is potential problem. Perhaps we should think about a better solution to this problem?
            newFactCount = knowledgeSession.getFactCount();
            LOG.finest("On " + this + ", new fact count: " + newFactCount);
            if (newFactCount != currentFactCount) {
                LOG.finest("On " + this + ", fact count changed from " + currentFactCount + " to " + newFactCount + " after: " + assetUpdate);
            }

            currentFactCount = newFactCount;
        }

        return factHandle;
    }

    @Override
    public synchronized String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", running='" + running + '\'' +
            ", error='" + error + '\'' +
            ", definitions='" + Arrays.toString(ruleDefinitions.values().stream().map(rd -> rd.getName() + ": " + rd.getDeploymentStatus()).toArray(String[]::new)) + '\'' +
            '}';
    }
}
