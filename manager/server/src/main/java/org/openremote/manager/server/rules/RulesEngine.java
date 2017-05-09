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

import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.time.InternalSchedulerService;
import org.drools.core.time.JobContext;
import org.drools.core.time.JobHandle;
import org.drools.core.time.impl.*;
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
import org.kie.api.event.rule.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.time.SessionClock;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.Util;
import org.openremote.manager.server.asset.AssetProcessingService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.concurrent.ManagerExecutorService;
import org.openremote.manager.server.notification.NotificationService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.rules.AssetRuleset;
import org.openremote.manager.shared.rules.GlobalRuleset;
import org.openremote.manager.shared.rules.Ruleset;
import org.openremote.manager.shared.rules.TenantRuleset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.asset.AbstractAssetUpdate;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.asset.AssetState;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Users;
import org.openremote.model.user.UserQuery;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RulesEngine<T extends Ruleset> {

    public static final Logger LOG = Logger.getLogger(RulesEngine.class.getName());

    private static final int AUTO_START_DELAY_SECONDS = 2;
    private static Long counter = 1L;

    static final protected Util UTIL = new Util();

    final protected TimerService timerService;
    final protected ManagerExecutorService executorService;
    final protected NotificationService notificationService;
    final protected AssetStorageService assetStorageService;
    final protected AssetProcessingService assetProcessingService;
    final protected ManagerIdentityService identityService;
    final protected Class<T> rulesetType;
    final protected String id;
    final protected Function<RulesEngine, AgendaEventListener> rulesEngineListeners;

    protected final Map<Long, T> rulesets = new LinkedHashMap<>();
    protected String rulesetsDebug;
    protected KieSession knowledgeSession;
    protected KieServices kieServices;
    protected KieFileSystem kfs;
    // We need to be able to reference the KieModule dynamically generated for this engine
    // from the singleton KieRepository to do this we need a pom.xml file with a release ID - crazy drools!!
    protected ReleaseId releaseId;
    protected Future runningFuture;
    protected Throwable error;
    final protected Map<AssetState, FactHandle> assetStates = new HashMap<>();
    // This consumer is useful in testing, as we can't have a reliable event fact
    // count from Drools session (events are expired automatically))
    protected Consumer<AssetEvent> assetEventsConsumer;
    protected ScheduledFuture startTimer;

    public RulesEngine(TimerService timerService,
                       ManagerExecutorService executorService,
                       AssetStorageService assetStorageService,
                       NotificationService notificationService,
                       AssetProcessingService assetProcessingService,
                       ManagerIdentityService identityService,
                       Class<T> rulesetType,
                       String id,
                       Function<RulesEngine, AgendaEventListener> rulesEngineListeners) {
        this.timerService = timerService;
        this.executorService = executorService;
        this.assetStorageService = assetStorageService;
        this.notificationService = notificationService; // shouldBeUser service or Identity Service ?
        this.assetProcessingService = assetProcessingService;
        this.identityService = identityService;
        this.rulesetType = rulesetType;
        this.id = id;
        this.rulesEngineListeners = rulesEngineListeners;
    }

    protected synchronized static Long getNextCounter() {
        return counter++;
    }

    @SuppressWarnings("unchecked")
    public synchronized T[] getAllRulesets() {
        T[] arr = Util.createArray(rulesets.size(), rulesetType);
        return rulesets.values().toArray(arr);
    }

    public String getId() {
        return id;
    }

    public boolean isRunning() {
        return runningFuture != null;
    }

    public boolean isError() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

    public KieSession getKnowledgeSession() {
        return knowledgeSession;
    }

    public SessionClock getSessionClock() {
        KieSession session = knowledgeSession;
        if (session != null) {
            return session.getSessionClock();
        }

        return null;
    }

    public synchronized boolean isEmpty() {
        return rulesets.isEmpty();
    }

    public void setAssetEventsConsumer(Consumer<AssetEvent> assetEventsConsumer) {
        this.assetEventsConsumer = assetEventsConsumer;
    }

    protected void setGlobal(String identifier, Object object) {
        try {
            knowledgeSession.setGlobal(identifier, object);
        } catch (Throwable t) {
            // Ignore, Drools complains if the DRL doesn't declare the global, but it works
        }
    }

    /**
     * Adds the ruleset to the engine by first stopping the engine and
     * then deploying new rules and then restarting the engine (after
     * {@link #AUTO_START_DELAY_SECONDS}) to prevent excessive engine stop/start.
     * <p>
     * If engine is in an error state (one of the rulesets failed to deploy
     * then the engine will not restart).
     *
     * @return Whether or not the ruleset deployed successfully
     */
    public synchronized boolean addRuleset(T ruleset) {
        if (ruleset == null || ruleset.getRules() == null || ruleset.getRules().isEmpty()) {
            // Assume it's a success if deploying an empty ruleset
            LOG.finest("Ruleset is empty so no rules to deploy");
            return true;
        }

        if (kfs == null) {
            initialiseEngine();
        }

        T existingRuleset = rulesets.get(ruleset.getId());

        if (existingRuleset != null && existingRuleset.getVersion() == ruleset.getVersion()) {
            LOG.fine("Ruleset version already deployed so ignoring");
            return true;
        }

        if (isRunning()) {
            stop();
        }

        // Stop any running start timer
        if (startTimer != null) {
            startTimer.cancel(false);
        }

        // Check if ruleset is already deployed (maybe an older version)
        if (existingRuleset != null) {
            // Remove this old rules file
            kfs.delete("src/main/resources/" + ruleset.getId());
            //noinspection SuspiciousMethodCalls
            rulesets.remove(existingRuleset);
            updateRulesetsDebug();
        }

        LOG.info("Adding ruleset: " + ruleset);

        boolean addSuccessful = false;

        try {
            // ID will be unique within the scope of a rules engine as ruleset will all be of same type
            kfs.write("src/main/resources/" + ruleset.getId() + ".drl", ruleset.getRules());
            // Unload the rules string from the ruleset we don't need it anymore and don't want it using memory
            ruleset.setRules(null);
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                Collection<Message> errors = kieBuilder.getResults().getMessages(Message.Level.ERROR);
                LOG.severe("Error in ruleset: " + ruleset);
                for (Message error : errors) {
                    LOG.severe(error.getText());
                }
                // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
                kfs.delete("src/main/resources/" + ruleset.getId());
            } else {
                LOG.info("Added ruleset: " + ruleset);
                addSuccessful = true;
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error in ruleset: " + ruleset, e);
            error = e;
            // If compilation failed, remove rules from FileSystem so it won't fail on next pass here if any
            kfs.delete("src/main/resources/" + ruleset.getId());
        }

        if (!addSuccessful) {
            error = new RuntimeException("Ruleset contains an error: " + ruleset);

            // Update status of each ruleset
            rulesets.forEach((id, rd) -> {
                if (rd.getDeploymentStatus() == Ruleset.DeploymentStatus.DEPLOYED) {
                    rd.setDeploymentStatus(Ruleset.DeploymentStatus.READY);
                }
            });
        } else {
            startTimer = executorService.schedule(this::start, AUTO_START_DELAY_SECONDS * 1000);
        }

        // Add new ruleset
        ruleset.setDeploymentStatus(addSuccessful ? Ruleset.DeploymentStatus.DEPLOYED : Ruleset.DeploymentStatus.FAILED);
        rulesets.put(ruleset.getId(), ruleset);
        updateRulesetsDebug();

        return addSuccessful;
    }

    protected synchronized void removeRuleset(Ruleset ruleset) {
        if (kfs == null) {
            return;
        }

        T matchedRuleset = rulesets.get(ruleset.getId());
        if (matchedRuleset == null) {
            LOG.finer("Ruleset cannot be retracted as it was never deployed: " + ruleset);
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
        kfs.delete("src/main/resources/" + ruleset.getId());
        rulesets.remove(ruleset.getId());
        updateRulesetsDebug();

        // Update status of each ruleset
        boolean anyFailed = rulesets
            .values()
            .stream()
            .anyMatch(rd -> rd.getDeploymentStatus() == Ruleset.DeploymentStatus.FAILED);

        if (!anyFailed) {
            error = null;
            rulesets.forEach((id, rd) -> {
                if (rd.getDeploymentStatus() == Ruleset.DeploymentStatus.READY) {
                    rd.setDeploymentStatus(Ruleset.DeploymentStatus.DEPLOYED);
                }
            });
        }

        if (!isError() && !isEmpty()) {
            // Queue engine start
            startTimer = executorService.schedule(this::start, AUTO_START_DELAY_SECONDS * 1000);
        }
    }

    protected void initialiseEngine() {
        // Initialise
        kieServices = KieServices.Factory.get();
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

        String versionId = getNextCounter().toString();
        releaseId = kieServices.newReleaseId("org.openremote", "openremote-kiemodule", versionId);
        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel("OpenRemoteKModule");

        kieBaseModel
            .setDefault(true)
            .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
            .setEventProcessingMode(EventProcessingOption.STREAM)
            .newKieSessionModel("ksession1")
            .setDefault(true)
            .setType(KieSessionModel.KieSessionType.STATEFUL);
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
            LOG.fine("Cannot start rules engine as an error occurred during initialisation");
            return;
        }

        if (isEmpty()) {
            LOG.finest("No rulesets loaded so nothing to start");
            return;
        }

        LOG.fine("Starting RuleEngine: " + this);

        // Note each rule engine has its' own KieModule which are stored in a singleton register by drools
        // we need to ensure we get the right module here otherwise we could be using the wrong rules
        KieContainer kieContainer = kieServices.newKieContainer(releaseId);

        KieSessionConfiguration kieSessionConfiguration = kieServices.newKieSessionConfiguration();

        // Which clock to use ("pseudo" for testing, "realtime" otherwise)
        switch (timerService.getClock()) {
            case PSEUDO:
                kieSessionConfiguration.setOption(ClockTypeOption.get("pseudo"));
                break;
            default:
                kieSessionConfiguration.setOption(ClockTypeOption.get("realtime"));
        }

        try {
            knowledgeSession = kieContainer.newKieSession(kieSessionConfiguration);

            // If the pseudo clock is enabled (we run a test environment?) then set current
            // time on startup of session, as real time is used in offset calculations for
            // automatic event expiration in Drools (probably a design mistake)
            if (timerService.getClock() == TimerService.Clock.PSEUDO) {
                ((PseudoClockScheduler) knowledgeSession.getSessionClock())
                    .setStartupTime(timerService.getCurrentTimeMillis());
            }

            setGlobal("assets", createAssetsFacade());
            setGlobal("users", createUsersFacade());
            setGlobal("LOG", LOG);

            // TODO Still need this UTIL?
            setGlobal("util", UTIL);

            knowledgeSession.addEventListener(new RuleExecutionLogger(this::toString));

            AgendaEventListener eventListener = rulesEngineListeners != null ? rulesEngineListeners.apply(this) : null;
            if (eventListener != null) {
                knowledgeSession.addEventListener(eventListener);
            }

            knowledgeSession.addEventListener(new RuleRuntimeEventListener() {
                @Override
                public void objectInserted(ObjectInsertedEvent event) {
                    LOG.fine("+++ On " + RulesEngine.this + ", object inserted: " + event.getObject());
                }

                @Override
                public void objectUpdated(ObjectUpdatedEvent event) {
                    LOG.fine("^^^ On " + RulesEngine.this + ", object updated: " + event.getObject());
                }

                @Override
                public void objectDeleted(ObjectDeletedEvent event) {
                    LOG.fine("--- On " + RulesEngine.this + ", object deleted: " + event.getOldObject());
                }
            });

            // Start engine in active mode
            fireUntilHalt();

            // Insert initial asset states
            try {
                Set<AssetState> initialState = assetStates.keySet();
                LOG.info("On " + this + ", inserting initial asset states: " + initialState.size());
                for (AssetState assetState : initialState) {
                    insertAssetState(assetState);
                }
            } catch (Exception ex) {
                // This is called in a background timer thread, we must log here or the exception is swallowed
                LOG.log(Level.SEVERE, "On " + this + ", inserting initial asset states failed", ex);
                // TODO Should we stop the engine here?
                error = ex;
                stop();
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "On " + this + ", creating the knowledge session failed", ex);
            error = ex;
            stop();
        }
    }

    protected synchronized void fireUntilHalt() {
        // Block a background thread
        runningFuture = executorService.getRulesExecutor().submit(() -> {
            boolean stoppedOnError = false;
            try {
                knowledgeSession.fireUntilHalt();
            } catch (Exception ex) {
                // Errors in rule RHS
                LOG.log(Level.SEVERE, "On " + RulesEngine.this + ", error firing rules", ex);
                stoppedOnError = true;
            } finally {
                if (stoppedOnError) {
                    // Keep running if stopped firing because of a RHS error
                    runningFuture.cancel(true);
                    runningFuture = null;
                    fireUntilHalt();
                }
            }
        });
    }

    protected synchronized void stop() {
        if (!isRunning()) {
            return;
        }
        LOG.fine("Stopping RuleEngine: " + this);
        if (knowledgeSession != null) {
            try {
                knowledgeSession.halt();
                knowledgeSession.dispose();
                LOG.fine("On " + this + ", knowledge session disposed");
            } finally {
                runningFuture.cancel(true);
                runningFuture = null;
            }
        }
    }

    protected synchronized void insertAssetState(AssetState newAssetState) {
        FactHandle factHandle = insertIntoSession(newAssetState);
        assetStates.put(newAssetState, factHandle);
    }

    protected synchronized void updateAssetState(AssetState assetState) {
        // Check if fact already exists using equals()
        if (!assetStates.containsKey(assetState)) {
            // Delete any existing fact for this attribute ref
            // Put the fact into working memory and store the handle
            retractAssetState(assetState);

            if (isRunning()) {
                insertAssetState(assetState);
            } else {
                assetStates.put(assetState, null);
            }
        }
    }

    protected synchronized void retractAssetState(AssetState assetState) {

        // If there already is a fact in working memory for this attribute then delete it
        AssetState update = assetStates.keySet()
            .stream()
            .filter(au -> au.attributeRefsEqual(assetState))
            .findFirst()
            .orElse(null);

        // Always remove from asset states
        FactHandle factHandle = update != null ? assetStates.remove(update) : null;

        if (factHandle != null) {
            if (isRunning()) {
                try {
                    // ... retract it from working memory ...
                    knowledgeSession.delete(factHandle);
                } catch (Exception e) {
                    LOG.warning("On " + this + ", failed to retract fact: " + update);
                }
            }
        }
    }

    protected synchronized void insertAssetEvent(long expirationOffset, AssetEvent assetEvent) {
        FactHandle factHandle = insertIntoSession(assetEvent);
        if (factHandle != null) {
            scheduleExpiration(assetEvent, factHandle, expirationOffset);
        }
        if (assetEventsConsumer != null) {
            assetEventsConsumer.accept(assetEvent);
        }
    }

    protected synchronized FactHandle insertIntoSession(AbstractAssetUpdate update) {
        if (!isRunning()) {
            LOG.fine("On " + this + ", engine is in error state or not running, ignoring: " + update);
            return null;
        }
        FactHandle fh = knowledgeSession.insert(update);
        LOG.fine("On " + this + ", fact count after insert: " + knowledgeSession.getFactCount());
        return fh;
    }

    protected Assets createAssetsFacade() {
        return new Assets() {
            @Override
            public RestrictedQuery query() {
                RestrictedQuery query = new RestrictedQuery() {

                    @Override
                    public RestrictedQuery select(Select select) {
                        throw new IllegalArgumentException("Overriding query projection is not allowed in this rules scope");
                    }

                    @Override
                    public RestrictedQuery id(String id) {
                        throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
                    }

                    @Override
                    public RestrictedQuery tenant(TenantPredicate tenantPredicate) {
                        if (GlobalRuleset.class.isAssignableFrom(rulesetType))
                            return super.tenant(tenantPredicate);
                        throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
                    }

                    @Override
                    public RestrictedQuery userId(String userId) {
                        throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
                    }

                    @Override
                    public RestrictedQuery orderBy(OrderBy orderBy) {
                        throw new IllegalArgumentException("Overriding query result order is not allowed in this rules scope");
                    }

                    @Override
                    public String getResult() {
                        ServerAsset asset = assetStorageService.find(this);
                        return asset != null ? asset.getId() : null;
                    }

                    @Override
                    public List<String> getResults() {
                        return assetStorageService.findAllIds(this);
                    }

                    @Override
                    public void applyResult(Consumer<String> assetIdConsumer) {
                        assetIdConsumer.accept(getResult());
                    }

                    @Override
                    public void applyResults(Consumer<List<String>> assetIdListConsumer) {
                        assetIdListConsumer.accept(getResults());
                    }
                };

                if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
                    query.tenantPredicate = new AssetQuery.TenantPredicate(id);
                }
                if (AssetRuleset.class.isAssignableFrom(rulesetType)) {
                    ServerAsset restrictedAsset = assetStorageService.find(id, true);
                    if (restrictedAsset == null) {
                        throw new IllegalStateException("Asset is no longer available for this deployment: " + id);
                    }
                    query.pathPredicate = new AssetQuery.PathPredicate(restrictedAsset.getPath());
                }
                return query;
            }

            @Override
            public void dispatch(AttributeEvent... events) {
                if (events == null)
                    return;
                for (AttributeEvent event : events) {

                    // Set event source time to current container time
                    event.timestamp = timerService.getCurrentTimeMillis();

                    // Check if the asset ID of the event can be found in the original query
                    AssetQuery checkQuery = query();
                    checkQuery.id = event.getEntityId();
                    if (assetStorageService.find(checkQuery) == null) {
                        throw new IllegalArgumentException(
                            "Access to asset not allowed for this rule engine scope: " + event
                        );
                    }

                    LOG.fine("Dispatching on " + RulesEngine.this + ": " + event);
                    assetProcessingService.sendAttributeEvent(event);
                }
            }
        };
    }

    protected Users createUsersFacade() {
        return new Users() {

            @Override
            public Users.RestrictedQuery query() {
                RestrictedQuery query = new RestrictedQuery() {
                    @Override
                    public Users.RestrictedQuery tenant(UserQuery.TenantPredicate tenantPredicate) {
                        if (GlobalRuleset.class.isAssignableFrom(rulesetType))
                            return super.tenant(tenantPredicate);
                        throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
                    }

                    @Override
                    public Users.RestrictedQuery asset(UserQuery.AssetPredicate assetPredicate) {
                        if (GlobalRuleset.class.isAssignableFrom(rulesetType))
                            return super.asset(assetPredicate);
                        if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
                            // TODO: should only be allowed if asset belongs to tenant
                        }
                        throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
                    }

                    @Override
                    public List<String> getResults() {
                        return notificationService.findAllUsersWithToken(this);
                    }

                    @Override
                    public void applyResults(Consumer<List<String>> usersIdListConsumer) {
                        usersIdListConsumer.accept(getResults());
                    }
                };

                if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
                    query.tenantPredicate = new UserQuery.TenantPredicate(id);
                }
                if (AssetRuleset.class.isAssignableFrom(rulesetType)) {
                    ServerAsset restrictedAsset = assetStorageService.find(id, true);
                    if (restrictedAsset == null) {
                        throw new IllegalStateException("Asset is no longer available for this deployment: " + id);
                    }
                    query.assetPredicate = new UserQuery.AssetPredicate(id);
                }
                return query;

            }

            @Override
            public void storeAndNotify(String userId, AlertNotification alert) {
                if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
                    boolean userIsInTenant = identityService.isUserInTenant(userId, id);
                    if (!userIsInTenant) {
                        throw new IllegalArgumentException("User not in tenant: " + id);
                    }
                 }
                if (AssetRuleset.class.isAssignableFrom(rulesetType)) {
                    boolean userIsLinkedToAsset = assetStorageService.isUserAsset(userId, id);
                    if (!userIsLinkedToAsset) {
                        throw new IllegalArgumentException("User not linked to asset: " + id);
                    }
                }

                notificationService.storeAndNotify(userId, alert);
            }
        };
    }

    /**
     * Use the internal scheduling of Drools to expire events, so we can coordinate with the internal clock.
     * Yes, this is a hack.
     */
    protected void scheduleExpiration(AssetEvent assetEvent, FactHandle factHandle, long expirationOffset) {
        if (!isRunning())
            return;
        InternalSchedulerService sessionScheduler = knowledgeSession.getSessionClock();
        JobHandle jobHandle = new JDKTimerService.JDKJobHandle(assetEvent.getId().hashCode());
        class AssetEventExpireJobContext implements JobContext {
            public JobHandle handle;

            @Override
            public void setJobHandle(JobHandle jobHandle) {
                this.handle = jobHandle;
            }

            @Override
            public JobHandle getJobHandle() {
                return handle;
            }

            @Override
            public InternalWorkingMemory getWorkingMemory() {
                return null;
            }
        }
        TimerJobInstance timerJobInstance = new DefaultTimerJobInstance(
            ctx -> {
                LOG.fine("On " + RulesEngine.this + ", fact expired, deleting: " + assetEvent);
                synchronized (RulesEngine.this) {
                    knowledgeSession.delete(factHandle);
                }
            },
            new AssetEventExpireJobContext(),
            new PointInTimeTrigger(knowledgeSession.getSessionClock().getCurrentTime() + expirationOffset, null, null),
            jobHandle,
            sessionScheduler
        );
        sessionScheduler.internalSchedule(timerJobInstance);

    }

    protected synchronized void updateRulesetsDebug() {
        rulesetsDebug = Arrays.toString(rulesets.values().stream().map(rd ->
            rd.getClass().getSimpleName()
                + " - "
                + rd.getName()
                + ": "
                + rd.getDeploymentStatus()).toArray(String[]::new)
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", running='" + isRunning() + '\'' +
            ", error='" + error + '\'' +
            ", rulesets='" + rulesetsDebug + '\'' +
            '}';
    }
}
