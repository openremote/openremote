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
package org.openremote.manager.rules;

import org.jeasy.rules.api.RulesEngineParameters;
import org.jeasy.rules.core.InferenceRulesEngine;
import org.openremote.model.PersistenceEvent;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.rules.facade.*;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.filter.GeofencePredicate;
import org.openremote.model.query.filter.LocationAttributePredicate;
import org.openremote.model.rules.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.model.rules.RulesetStatus.*;

public class RulesEngine<T extends Ruleset> {

    /**
     * Allows rule deployments to track changes to the {@link AssetState}s in scope
     */
    public static final class AssetStateChangeEvent {
        public PersistenceEvent.Cause cause;
        public AssetState<?> assetState;

        public AssetStateChangeEvent(PersistenceEvent.Cause cause, AssetState<?> assetState) {
            this.cause = cause;
            this.assetState = assetState;
        }
    }

    /**
     * Identifies a set of {@link LocationAttributePredicate}s associated with a particular {@link Asset}
     */
    public static final class AssetStateLocationPredicates {

        protected final String assetId;
        protected final Set<GeofencePredicate> locationPredicates;

        public AssetStateLocationPredicates(String assetId, Set<GeofencePredicate> locationPredicates) {
            this.assetId = assetId;
            this.locationPredicates = locationPredicates;
        }

        public String getAssetId() {
            return assetId;
        }

        public Set<GeofencePredicate> getLocationPredicates() {
            return locationPredicates;
        }
    }

    public static final Logger LOG = Logger.getLogger(RulesEngine.class.getName());

    // Separate logger for execution of rules
    public static final Logger RULES_LOG = Logger.getLogger("org.openremote.rules.Rules");

    // Separate logger for fired rules
    public static final Logger RULES_FIRED_LOG = SyslogCategory.getLogger(SyslogCategory.RULES, "org.openremote.rules.RulesFired");

    // Separate logger for periodic stats printer
    public static final Logger STATS_LOG = Logger.getLogger("org.openremote.rules.RulesEngineStats");
    protected static BiConsumer<RulesEngine<?>, RulesetDeployment> UNPAUSE_SCHEDULER = RulesEngine::scheduleUnpause;
    // Here to facilitate testing
    protected static BiConsumer<RulesEngine<?>, RulesetDeployment> PAUSE_SCHEDULER = RulesEngine::schedulePause;
    final protected TimerService timerService;
    final protected ScheduledExecutorService executorService;
    final protected AssetStorageService assetStorageService;
    final protected ClientEventService clientEventService;

    final protected RulesEngineId<T> id;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected Notifications notificationFacade;
    final protected PredictedDatapoints predictedFacade;
    final protected HistoricDatapoints historicFacade;
    final protected AssetLocationPredicateProcessor assetLocationPredicatesConsumer;

    final protected Map<Long, RulesetDeployment> deployments = new LinkedHashMap<>();
    final protected RulesFacts facts;
    final protected InferenceRulesEngine engine;

    protected boolean running;
    protected long lastFireTimestamp;
    protected boolean trackLocationPredicates;
    protected ScheduledFuture<?> fireTimer;
    protected ScheduledFuture<?> statsTimer;
    protected Map<Long, ScheduledFuture<?>> pauseTimers = new HashMap<>();
    protected Map<Long, ScheduledFuture<?>> unpauseTimers = new HashMap<>();

    // Only used to optimize toString(), contains the details of this engine
    protected String deploymentInfo;

    // Only used in tests to prevent scheduled firing of engine
    protected boolean disableTemporaryFactExpiration = false;

    public RulesEngine(TimerService timerService,
                       ManagerIdentityService identityService,
                       ScheduledExecutorService executorService,
                       AssetStorageService assetStorageService,
                       AssetProcessingService assetProcessingService,
                       NotificationService notificationService,
                       ClientEventService clientEventService,
                       AssetDatapointService assetDatapointService,
                       AssetPredictedDatapointService assetPredictedDatapointService,
                       RulesEngineId<T> id,
                       AssetLocationPredicateProcessor assetLocationPredicatesConsumer) {
        this.timerService = timerService;
        this.executorService = executorService;
        this.assetStorageService = assetStorageService;
        this.clientEventService = clientEventService;
        this.id = id;
        AssetsFacade<T> assetsFacade = new AssetsFacade<>(id, assetStorageService, assetProcessingService::sendAttributeEvent);
        this.assetsFacade = assetsFacade;
        this.usersFacade = new UsersFacade<>(id, assetStorageService, notificationService, identityService);
        this.notificationFacade = new NotificationsFacade<>(id, notificationService);
        this.historicFacade = new HistoricFacade<>(id, assetDatapointService);
        this.predictedFacade = new PredictedFacade<>(id, assetPredictedDatapointService);
        this.assetLocationPredicatesConsumer = assetLocationPredicatesConsumer;

        this.facts = new RulesFacts(timerService, assetStorageService, assetsFacade, this, RULES_LOG);
        engine = new InferenceRulesEngine(
            // Skip any other rules after the first failed rule (exception thrown in condition or action)
            new RulesEngineParameters(false, true, false, RulesEngineParameters.DEFAULT_RULE_PRIORITY_THRESHOLD)
        );
        engine.registerRuleListener(facts);
    }

    public RulesEngineId<T> getId() {
        return id;
    }

    /**
     * @return a shallow copy of the asset state facts.
     */
    public Set<AssetState<?>> getAssetStates() {
        return new HashSet<>(facts.getAssetStates());
    }

    /**
     * @return a shallow copy of the asset event facts.
     */
    public List<TemporaryFact<AssetState<?>>> getAssetEvents() {
        return new ArrayList<>(facts.getAssetEvents());
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isError() {
        for (RulesetDeployment deployment : deployments.values()) {
            if (deployment.isError() || deployment.getError() instanceof RulesLoopException) {
                return true;
            }
        }
        return false;
    }

    public int getExecutionErrorDeploymentCount() {
        return (int) deployments.values().stream().filter(deployment -> deployment.getStatus() == EXECUTION_ERROR || deployment.getStatus() == LOOP_ERROR).count();
    }

    public int getCompilationErrorDeploymentCount() {
        return (int) deployments.values().stream().filter(deployment -> deployment.getStatus() == COMPILATION_ERROR).count();
    }

    public RuntimeException getError() {
        long executionErrorCount = getExecutionErrorDeploymentCount();
        long compilationErrorCount = getCompilationErrorDeploymentCount();

        if (executionErrorCount > 0 || compilationErrorCount > 0) {
            return new RuntimeException(
                "Ruleset deployments have errors, failed compilation: "
                    + compilationErrorCount
                    + ", failed execution: "
                    + executionErrorCount + " - on: " + this
            );
        }
        return null;
    }

    /**
     * @return <code>true</code> if all rulesets are not in an error state.
     */
    public boolean canStart() {
        return deployments.values().stream().noneMatch(rd -> rd.getStatus() == COMPILATION_ERROR && !rd.isContinueOnError());
    }

    public void addRuleset(T ruleset) {

        // Check for previous version of this ruleset
        RulesetDeployment deployment = deployments.get(ruleset.getId());
        boolean wasRunning = this.running;

        stop();

        if (deployment != null) {
            removeRuleset(deployment.ruleset);
        }

        deployment = new RulesetDeployment(ruleset, timerService, assetStorageService, executorService, assetsFacade, usersFacade, notificationFacade, historicFacade, predictedFacade);
        boolean compiled;

        if (TextUtil.isNullOrEmpty(ruleset.getRules())) {
            LOG.finest("Ruleset is empty so no rules to deploy: " + ruleset.getName());
            deployment.setStatus(EMPTY);
            publishRulesetStatus(deployment);
        } else if (!ruleset.isEnabled()) {
            LOG.finest("Ruleset is disabled: " + ruleset.getName());
            deployment.setStatus(DISABLED);
            publishRulesetStatus(deployment);
        } else {
            deployment.updateValidity();
            if (deployment.hasExpired()) {
                LOG.fine("Ruleset validity period has expired: " + ruleset.getName());
                deployment.setStatus(EXPIRED);
                publishRulesetStatus(deployment);
                compiled = true;
            } else {
                compiled = deployment.compile();
            }

            if (!compiled) {
                LOG.log(Level.SEVERE, "Ruleset compilation error: " + ruleset.getName(), deployment.getError());
                deployment.setStatus(COMPILATION_ERROR);
                publishRulesetStatus(deployment);
            } else if (running) {
                startRuleset(deployment);
            }
        }

        deployments.put(ruleset.getId(), deployment);
        updateDeploymentInfo();

        if (wasRunning) {
            start();
        }
    }

    /**
     * @return <code>true</code> if this rules engine has no deployments.
     */
    public boolean removeRuleset(Ruleset ruleset) {
        RulesetDeployment deployment = deployments.get(ruleset.getId());

        if (deployment == null) {
            LOG.finer("Ruleset cannot be retracted as it was never deployed: " + ruleset);
            return deployments.size() == 0;
        }

        stopRuleset(deployment);

        deployment.setStatus(REMOVED);
        publishRulesetStatus(deployment);
        deployments.remove(ruleset.getId());

        ScheduledFuture<?> timer = pauseTimers.remove(ruleset.getId());
        if (timer != null) timer.cancel(true);
        timer = unpauseTimers.remove(ruleset.getId());
        if (timer != null) timer.cancel(true);

        updateDeploymentInfo();
        start();

        return deployments.size() == 0;
    }

    public void start() {
        if (running) {
            return;
        }

        if (deployments.size() == 0) {
            LOG.finest("No rulesets so nothing to start");
            return;
        }

        if (!canStart()) {
            LOG.fine("Cannot start rules engine one or more rulesets in an error state");
            return;
        }

        LOG.info("Starting: " + this);
        running = true;
        trackLocationPredicates(true);

        deployments.values().forEach(this::startRuleset);

        updateDeploymentInfo();
        publishRulesEngineStatus();
        scheduleFire();

        // Start a background stats printer if INFO level logging is enabled
        if (STATS_LOG.isLoggable(Level.INFO) || STATS_LOG.isLoggable(Level.FINEST)) {
            if (STATS_LOG.isLoggable(Level.FINEST)) {
                LOG.info("On " + this + ", enabling periodic statistics output at INFO level every 30 seconds on category: " + STATS_LOG.getName());
            } else {
                LOG.info("On " + this + ", enabling periodic full memory dump at FINEST level every 30 seconds on category: " + STATS_LOG.getName());
            }
            statsTimer = executorService.scheduleAtFixedRate(this::printSessionStats, 3, 30, TimeUnit.SECONDS);
        }
    }

    protected void trackLocationPredicates(boolean track) {
        if (trackLocationPredicates == track) {
            return;
        }

        trackLocationPredicates = track;
        if (track) {
            facts.startTrackingLocationRules();
        } else {
            if (assetLocationPredicatesConsumer != null) {
                processLocationRules(facts.stopTrackingLocationRules());
            }
        }
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean systemShutdownInProgress) {
        if (!running) {
            return;
        }
        LOG.info("Stopping: " + this);
        if (fireTimer != null) {
            fireTimer.cancel(true);
            fireTimer = null;
        }
        if (statsTimer != null) {
            statsTimer.cancel(true);
            statsTimer = null;
        }
        pauseTimers.values().forEach(pauseTimer -> pauseTimer.cancel(true));
        pauseTimers.clear();
        unpauseTimers.values().forEach(unpauseTimer -> unpauseTimer.cancel(true));
        unpauseTimers.clear();

        deployments.values().forEach(this::stopRuleset);
        running = false;

        if (!systemShutdownInProgress && assetLocationPredicatesConsumer != null) {
            assetLocationPredicatesConsumer.accept(this, null);
        }

        updateDeploymentInfo();
        publishRulesEngineStatus();
    }

    protected void startRuleset(RulesetDeployment deployment) {
        if (!running) {
            return;
        }

        if (deployment.getStatus() == COMPILATION_ERROR || deployment.getStatus() == DISABLED || deployment.getStatus() == EXPIRED) {
            return;
        }

        deployment.updateValidity();

        if (deployment.hasExpired()) {
            deployment.setStatus(EXPIRED);
            publishRulesetStatus(deployment);
            return;
        }

        if (deployment.getValidFrom() > timerService.getCurrentTimeMillis()) {
            // Not ready to start yet so pause
            pauseRuleset(deployment);
        } else {
            deployment.setStatus(DEPLOYED);
            publishRulesetStatus(deployment);
            deployment.start(facts);

            if (deployment.getValidTo() != Long.MAX_VALUE) {
                PAUSE_SCHEDULER.accept(this, deployment);
            }
        }
    }

    protected void stopRuleset(RulesetDeployment deployment) {
        if (!running) {
            return;
        }

        if (deployment.getStatus() == DEPLOYED) {
            deployment.stop(facts);
            deployment.setStatus(READY);
            publishRulesetStatus(deployment);
        }
    }

    public void scheduleFire() {
        withLock(toString() + "::scheduleFire", () -> {
            // Schedule a firing within the guaranteed expiration time (so not immediately), and
            // only if the last firing is done. This effectively limits how often the rules engine
            // will fire, only once within the guaranteed minimum expiration time.
            if (fireTimer == null || fireTimer.isDone()) {
                LOG.fine("Scheduling rules firing on: " + this);
                fireTimer = executorService.schedule(
                    () -> withLock(RulesEngine.this.toString() + "::fire", () -> {

                        fireTimer = null;

                        // Are temporary facts present before rules are fired?
                        boolean hadTemporaryFactsBefore = facts.hasTemporaryFacts();

                        // Process rules for all deployments
                        fireAllDeployments();

                        // If there are temporary facts, or if there were some before and
                        // now they are gone, schedule a new firing to guarantee processing
                        // of expired and removed temporary facts
                        if ((facts.hasTemporaryFacts() || (hadTemporaryFactsBefore && !facts.hasTemporaryFacts()))
                            && !disableTemporaryFactExpiration) {
                            LOG.fine("Temporary facts require firing rules on: " + this);
                            executorService.submit(this::scheduleFire);
                        } else if (!disableTemporaryFactExpiration) {
                            LOG.fine("No temporary facts present/changed when firing rules on: " + this);
                        }

                    }),
                    TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS,
                    TimeUnit.MILLISECONDS
                );
            }
        });
    }

    private void fireDeployments(Collection<RulesetDeployment> deploymentList) {
        if (!running) {
            return;
        }

        if (trackLocationPredicates && assetLocationPredicatesConsumer != null) {
            facts.startTrackingLocationRules();
        }

        // Set the current clock
        RulesClock clock = new RulesClock(timerService);
        facts.setClock(clock);

        // Remove any expired temporary facts
        facts.removeExpiredTemporaryFacts();

        for (RulesetDeployment deployment : deploymentList) {
            try {

                if (deployment.getStatus() == DEPLOYED) {

                    RULES_LOG.fine("Executing rules @" + clock + " of: " + deployment);

                    // If full detail logging is enabled
                    // Log asset states and events before firing
                    facts.logFacts(RULES_LOG, Level.FINEST);

                    // Reset facts for this firing (loop detection etc.)
                    facts.reset();

                    long startTimestamp = System.currentTimeMillis();
                    lastFireTimestamp = startTimestamp;
                    engine.fire(deployment.getRules(), facts);
                    RULES_FIRED_LOG.fine("Rules deployment '" + deployment.getName() + "' executed in: " + (System.currentTimeMillis() - startTimestamp) + "ms");
                }

            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "On " + RulesEngine.this + ", error executing rules of: " + deployment, ex);

                deployment.setStatus(ex instanceof RulesLoopException ? LOOP_ERROR : EXECUTION_ERROR);
                deployment.setError(ex);
                publishRulesetStatus(deployment);

                // TODO We only get here on LHS runtime errors, RHS runtime errors are in RuleFacts.onFailure()
                if (ex instanceof RulesLoopException || !deployment.ruleset.isContinueOnError()) {
                    stop();
                    break;
                }
            } finally {
                // Reset facts after this firing (loop detection etc.)
                facts.reset();
            }
        }

        trackLocationPredicates(false);
    }

    protected void fireAllDeployments() {
        fireDeployments(deployments.values());
    }

    protected void fireAllDeploymentsWithPredictedData() {
        fireDeployments(deployments.values().stream().filter(RulesetDeployment::isTriggerOnPredictedData).collect(Collectors.toList()));
    }

    protected void notifyAssetStatesChanged(AssetStateChangeEvent event) {
        for (RulesetDeployment deployment : deployments.values()) {
            if (!deployment.isError()) {
                deployment.onAssetStatesChanged(facts, event);
            }
        }
    }

    public void updateOrInsertAssetState(AssetState<?> assetState, boolean insert) {
        facts.putAssetState(assetState);
        // Make sure location predicate tracking is activated before notifying the deployments otherwise they won't report location predicates
        trackLocationPredicates(trackLocationPredicates || (insert && assetState.getName().equals(Asset.LOCATION.getName())));
        notifyAssetStatesChanged(new AssetStateChangeEvent(insert ? PersistenceEvent.Cause.CREATE : PersistenceEvent.Cause.UPDATE, assetState));
        if (running) {
            scheduleFire();
        }
    }

    public void removeAssetState(AssetState<?> assetState) {
        facts.removeAssetState(assetState);
        // Make sure location predicate tracking is activated before notifying the deployments otherwise they won't report location predicates
        trackLocationPredicates(trackLocationPredicates || assetState.getName().equals(Asset.LOCATION.getName()));
        notifyAssetStatesChanged(new AssetStateChangeEvent(PersistenceEvent.Cause.DELETE, assetState));
        if (running) {
            scheduleFire();
        }
    }

    public void insertAssetEvent(long expiresMillis, AssetState<?> assetState) {
        facts.insertAssetEvent(expiresMillis, assetState);
        if (running) {
            scheduleFire();
        }
    }

    protected void updateDeploymentInfo() {
        deploymentInfo = Arrays.toString(
            deployments.values().stream()
                .map(RulesetDeployment::toString)
                .toArray(String[]::new)
        );
    }

    protected void printSessionStats() {
        withLock(toString() + "::printSessionStats", () -> {
            Collection<AssetState<?>> assetStateFacts = facts.getAssetStates();
            Collection<TemporaryFact<AssetState<?>>> assetEventFacts = facts.getAssetEvents();
            Map<String, Object> namedFacts = facts.getNamedFacts();
            Collection<Object> anonFacts = facts.getAnonymousFacts();
            long temporaryFactsCount = facts.getTemporaryFacts().count();
            long total = assetStateFacts.size() + assetEventFacts.size() + namedFacts.size() + anonFacts.size();
            STATS_LOG.fine("Engine stats for '" + this + "', in memory facts are Total: " + total
                + ", AssetState: " + assetStateFacts.size()
                + ", AssetEvent: " + assetEventFacts.size()
                + ", Named: " + namedFacts.size()
                + ", Anonymous: " + anonFacts.size()
                + ", Temporary: " + temporaryFactsCount);

            // Additional details if FINEST is enabled
            facts.logFacts(STATS_LOG, Level.FINEST);
        });
    }

    /**
     * This is called with all the asset's that have a location attribute currently loaded into this engine.
     */
    protected void processLocationRules(List<AssetStateLocationPredicates> assetStateLocationPredicates) {
        if (assetLocationPredicatesConsumer != null) {
            assetLocationPredicatesConsumer.accept(this, assetStateLocationPredicates);
        }
    }

    protected RulesEngineStatus getStatus() {
        if (isRunning()) {
            return RulesEngineStatus.RUNNING;
        }

        return deployments.values().stream().anyMatch(RulesetDeployment::isError) ? RulesEngineStatus.ERROR : RulesEngineStatus.STOPPED;
    }

    protected void publishRulesEngineStatus() {
        withLock(getClass().getSimpleName() + "::publishRulesEngineStatus", () -> {

            String engineId = id == null ? null : id.getRealm().orElse(id.getAssetId().orElse(null));
            int compilationErrors = getCompilationErrorDeploymentCount();
            int executionErrors = getExecutionErrorDeploymentCount();
            RulesEngineInfo engineInfo = new RulesEngineInfo(
                getStatus(),
                compilationErrors,
                executionErrors);

            RulesEngineStatusEvent event = new RulesEngineStatusEvent(
                timerService.getCurrentTimeMillis(),
                engineId,
                engineInfo
            );

            LOG.finer("Publishing rules engine status event: " + event);

            // Notify clients
            clientEventService.publishEvent(event);
        });
    }

    protected void publishRulesetStatus(RulesetDeployment deployment) {
        withLock(getClass().getSimpleName() + "::publishRulesetStatus", () -> {

            Ruleset ruleset = deployment.ruleset;
            String engineId = id == null ? null : id.getRealm().orElse(id.getAssetId().orElse(null));

            ruleset.setStatus(deployment.getStatus());
            ruleset.setError(deployment.getErrorMessage());

            RulesetChangedEvent event = new RulesetChangedEvent(
                timerService.getCurrentTimeMillis(),
                engineId,
                ruleset
            );

            LOG.finer("Publishing ruleset status event: " + event);

            // Notify clients
            clientEventService.publishEvent(event);
        });
    }

    protected void schedulePause(RulesetDeployment deployment) {
        long delay = deployment.getValidTo() - timerService.getCurrentTimeMillis();
        LOG.info("Scheduling pause of ruleset at '" + new Date(deployment.getValidTo()).toString() + "' (" + delay + "ms): " + deployment.ruleset.getName());
        pauseTimers.put(deployment.getId(), executorService.schedule(() -> pauseRuleset(deployment), delay, TimeUnit.MILLISECONDS));
    }

    protected void pauseRuleset(RulesetDeployment deployment) {
        pauseTimers.remove(deployment.getId());

        if (!running) {
            return;
        }

        withLock(getClass().getSimpleName() + ":pauseRuleset", () -> {
            LOG.info("Pausing ruleset: " + deployment.getRuleset().getName());
            stopRuleset(deployment);
            deployment.updateValidity();

            if (deployment.hasExpired()) {
                deployment.setStatus(EXPIRED);
                publishRulesetStatus(deployment);
            } else {
                deployment.setStatus(PAUSED);
                publishRulesetStatus(deployment);
                UNPAUSE_SCHEDULER.accept(this, deployment);
            }
        });

    }

    protected void scheduleUnpause(RulesetDeployment deployment) {
        long delay = deployment.getValidFrom() - timerService.getCurrentTimeMillis();
        LOG.info("Scheduling un-pause of ruleset at '" + new Date(deployment.getValidFrom()).toString() + "' (" + delay + "ms): " + deployment.ruleset.getName());
        unpauseTimers.put(deployment.getId(), executorService.schedule(() -> unPauseRuleset(deployment), delay, TimeUnit.MILLISECONDS));
    }

    protected void unPauseRuleset(RulesetDeployment deployment) {
        unpauseTimers.remove(deployment.getId());

        if (!running) {
            return;
        }

        withLock(getClass().getSimpleName() + "::unpauseRuleset", () -> {
            LOG.info("Un-pausing ruleset: " + deployment.getRuleset().getName());
            startRuleset(deployment);
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", running='" + isRunning() + '\'' +
            ", deployments='" + deploymentInfo + '\'' +
            '}';
    }
}
