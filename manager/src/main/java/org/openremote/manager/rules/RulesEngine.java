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

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.RuleListener;
import org.jeasy.rules.api.RulesEngineParameters;
import org.jeasy.rules.core.AbstractRulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.facade.*;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.webhook.WebhookService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.filter.GeofencePredicate;
import org.openremote.model.query.filter.LocationAttributePredicate;
import org.openremote.model.rules.*;
import org.openremote.model.util.TextUtil;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    // Separate logger for periodic stats printer
    public static final Logger STATS_LOG = Logger.getLogger("org.openremote.rules.RulesEngineStats");
    protected static BiConsumer<RulesEngine<?>, RulesetDeployment> UNPAUSE_SCHEDULER = RulesEngine::scheduleUnpause;
    // Here to facilitate testing
    protected static BiConsumer<RulesEngine<?>, RulesetDeployment> PAUSE_SCHEDULER = RulesEngine::schedulePause;
    final protected TimerService timerService;
    final protected RulesService rulesService;
    final protected ScheduledExecutorService executorService;
    final protected AssetStorageService assetStorageService;
    final protected ClientEventService clientEventService;

    final protected RulesEngineId<T> id;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected Notifications notificationFacade;
    final protected Webhooks webhooksFacade;
    final protected PredictedDatapoints predictedFacade;
    final protected HistoricDatapoints historicFacade;
    final protected AssetLocationPredicateProcessor assetLocationPredicatesConsumer;

    final protected Map<Long, RulesetDeployment> deployments = new LinkedHashMap<>();
    final protected RulesFacts facts;
    final protected AbstractRulesEngine engine;

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
                       RulesService rulesService,
                       ManagerIdentityService identityService,
                       ScheduledExecutorService executorService,
                       AssetStorageService assetStorageService,
                       AssetProcessingService assetProcessingService,
                       NotificationService notificationService,
                       WebhookService webhookService,
                       ClientEventService clientEventService,
                       AssetDatapointService assetDatapointService,
                       AssetPredictedDatapointService assetPredictedDatapointService,
                       RulesEngineId<T> id,
                       AssetLocationPredicateProcessor assetLocationPredicatesConsumer) {
        this.timerService = timerService;
        this.rulesService = rulesService;
        this.executorService = executorService;
        this.assetStorageService = assetStorageService;
        this.clientEventService = clientEventService;
        this.id = id;
        AssetsFacade<T> assetsFacade = new AssetsFacade<>(id, assetStorageService, assetProcessingService::sendAttributeEvent);
        this.assetsFacade = assetsFacade;
        this.usersFacade = new UsersFacade<>(id, assetStorageService, notificationService, identityService);
        this.notificationFacade = new NotificationsFacade<>(id, notificationService);
        this.webhooksFacade = new WebhooksFacade<>(id, webhookService);
        this.historicFacade = new HistoricFacade<>(id, assetDatapointService);
        this.predictedFacade = new PredictedFacade<>(id, assetPredictedDatapointService);
        this.assetLocationPredicatesConsumer = assetLocationPredicatesConsumer;

        this.facts = new RulesFacts(timerService, assetStorageService, assetsFacade, this, RULES_LOG);
        engine = new DefaultRulesEngine(
            // Skip any other rules after the first failed rule (exception thrown in condition or action)
            new RulesEngineParameters(false, true, false, RulesEngineParameters.DEFAULT_RULE_PRIORITY_THRESHOLD)
        );
        engine.registerRuleListener(facts);

        // Add listener to rethrow runtime exceptions which are otherwise swallowed by the DefaultRulesEngine
        engine.registerRuleListener(new RuleListener() {
            @Override
            public void onEvaluationError(Rule rule, Facts facts, Exception exception) {
                RuntimeException ex;
                if (exception instanceof RuntimeException) {
                    ex = (RuntimeException) exception;
                } else {
                    ex = new RuntimeException(exception);
                }
                throw ex;
            }

            @Override
            public void onFailure(Rule rule, Facts facts, Exception exception) {
                RuntimeException ex;
                if (exception instanceof RuntimeException) {
                    ex = (RuntimeException) exception;
                } else {
                    ex = new RuntimeException(exception);
                }
                throw ex;
            }
        });
    }

    public RulesEngineId<T> getId() {
        return id;
    }

    /**
     * @return a shallow copy of the asset state facts.
     */
    public synchronized Set<AssetState<?>> getAssetStates() {
        return new HashSet<>(facts.getAssetStates());
    }

    /**
     * @return a shallow copy of the asset event facts.
     */
    public synchronized List<TemporaryFact<AssetState<?>>> getAssetEvents() {
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

        deployment = new RulesetDeployment(ruleset, timerService, assetStorageService, executorService, assetsFacade, usersFacade, notificationFacade, webhooksFacade, historicFacade, predictedFacade);
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
                LOG.finer("Ruleset validity period has expired: " + ruleset.getName());
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
            LOG.finer("No rulesets so nothing to start");
            return;
        }

        if (!canStart()) {
            LOG.info("Cannot start rules engine one or more rulesets in an error state");
            return;
        }

        LOG.info("Starting: " + this);
        running = true;
        trackLocationPredicates(true);

        deployments.values().forEach(this::startRuleset);

        updateDeploymentInfo();
        publishRulesEngineStatus();
        scheduleFire(true);

        // Start a background stats printer if INFO level logging is enabled
        if (STATS_LOG.isLoggable(Level.FINE) || STATS_LOG.isLoggable(Level.FINEST)) {
            if (STATS_LOG.isLoggable(Level.FINEST)) {
                LOG.info("On " + this + ", enabling periodic statistics output at FINEST level every 30 seconds on category: " + STATS_LOG.getName());
            } else {
                LOG.info("On " + this + ", enabling periodic full memory dump at FINE level every 30 seconds on category: " + STATS_LOG.getName());
            }
            statsTimer = executorService.scheduleAtFixedRate(this::printSessionStats, 3, 30, TimeUnit.SECONDS);
        }
    }

    protected synchronized void trackLocationPredicates(boolean track) {
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

    protected synchronized void startRuleset(RulesetDeployment deployment) {
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

    protected synchronized void stopRuleset(RulesetDeployment deployment) {
        if (!running) {
            return;
        }

        if (deployment.getStatus() == DEPLOYED) {
            deployment.stop(facts);
            deployment.setStatus(READY);
            publishRulesetStatus(deployment);
        }
    }

    // TODO: Add ability to subscribe to specific events from a ruleset so the ruleset triggers only when appropriate
    /**
     * Queues actual firing of rules; if facts have changed then firing occurs in a shorter time frame than if we just
     * need to re-evaluate {@link TemporaryFact}s. This effectively limits how often the rules engine will fire, only
     * once within the guaranteed minimum expiration time.
     */
    protected synchronized void scheduleFire(boolean quickFire) {
        boolean timerRunning = fireTimer != null && !fireTimer.isDone();

        if (timerRunning) {
            if (!quickFire) {
                // Firing is already scheduled
                return;
            } else {
                if (fireTimer.getDelay(TimeUnit.MILLISECONDS) <= rulesService.quickFireMillis) {
                    // Firing is already going to occur within time frame
                    return;
                } else {
                    // Cancel the existing timer
                    fireTimer.cancel(false);
                }
            }
        }

        long fireTimeMillis = quickFire ? rulesService.quickFireMillis : rulesService.tempFactExpirationMillis;

        LOG.finer("Scheduling rules firing in " + fireTimeMillis + "ms on: " + this);
        fireTimer = executorService.schedule(
            () -> {
                synchronized (RulesEngine.this) {
                    // Are temporary facts present before rules are fired?
                    boolean hadTemporaryFactsBefore = facts.hasTemporaryFacts();

                    // Process rules for all deployments
                    fireAllDeployments();

                    // If there are temporary facts, or if there were some before and
                    // now they are gone, schedule a new firing to guarantee processing
                    // of expired and removed temporary facts
                    if ((facts.hasTemporaryFacts() || (hadTemporaryFactsBefore && !facts.hasTemporaryFacts()))
                        && !disableTemporaryFactExpiration) {
                        // Schedule call to schedule fire so the fireTimer shows as done
                        executorService.schedule(() -> scheduleFire(false), 10, TimeUnit.MILLISECONDS);
                    } else if (!disableTemporaryFactExpiration) {
                        LOG.finest("No temporary facts present/changed when firing rules on: " + this);
                    }

                    fireTimer = null;
                }
            },
            fireTimeMillis,
            TimeUnit.MILLISECONDS
        );
    }

    private void doFire(Collection<RulesetDeployment> deploymentList) {
        if (!running) {
            return;
        }

        if (trackLocationPredicates && assetLocationPredicatesConsumer != null) {
            facts.startTrackingLocationRules();
        }

        // Remove any expired temporary facts
        facts.removeExpiredTemporaryFacts();
        long executionTotalMillis = 0L;

        for (RulesetDeployment deployment : deploymentList) {
            try {

                if (deployment.getStatus() == DEPLOYED) {

                    LOG.finer("Executing rules of: " + deployment);

                    // If full detail logging is enabled
                    // Log asset states and events before firing
                    facts.logFacts(RULES_LOG, Level.FINEST);

                    // Reset facts for this firing (loop detection etc.)
                    facts.reset();

                    long startTimestamp = timerService.getCurrentTimeMillis();
                    lastFireTimestamp = startTimestamp;
                    engine.fire(deployment.getRules(), facts);
                    long executionMillis = (timerService.getCurrentTimeMillis() - startTimestamp);
                    executionTotalMillis += executionMillis;
                    LOG.fine("Rules deployment '" + deployment.getName() + "' executed in: " + executionMillis + "ms");
                } else {
                    LOG.finer("Rules deployment '" + deployment.getName() + "' skipped as status is: " + deployment.getStatus());
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

        if (executionTotalMillis > 100) {
            LOG.warning("Rules firing took " + executionTotalMillis + "ms on: " + this);
        } else {
            LOG.fine("Rules firing took " + executionTotalMillis + "ms on: " + this);
        }
    }

    protected void fireAllDeployments() {
        doFire(deployments.values());
    }

    protected synchronized void notifyAssetStatesChanged(AssetStateChangeEvent event) {
        for (RulesetDeployment deployment : deployments.values()) {
            if (!deployment.isError()) {
                deployment.onAssetStatesChanged(facts, event);
            }
        }
    }

    public synchronized void updateOrInsertAssetState(AssetState<?> assetState, boolean insert) {
        facts.putAssetState(assetState);
        // Make sure location predicate tracking is activated before notifying the deployments otherwise they won't report location predicates
        trackLocationPredicates(trackLocationPredicates || (insert && assetState.getName().equals(Asset.LOCATION.getName())));
        notifyAssetStatesChanged(new AssetStateChangeEvent(insert ? PersistenceEvent.Cause.CREATE : PersistenceEvent.Cause.UPDATE, assetState));
        if (running) {
            scheduleFire(true);
        }
    }

    public synchronized void removeAssetState(AssetState<?> assetState) {
        facts.removeAssetState(assetState);
        // Make sure location predicate tracking is activated before notifying the deployments otherwise they won't report location predicates
        trackLocationPredicates(trackLocationPredicates || assetState.getName().equals(Asset.LOCATION.getName()));
        notifyAssetStatesChanged(new AssetStateChangeEvent(PersistenceEvent.Cause.DELETE, assetState));
        if (running) {
            scheduleFire(true);
        }
    }

    public synchronized void insertAssetEvent(long expiresMillis, AssetState<?> assetState) {
        facts.insertAssetEvent(expiresMillis, assetState);
        if (running) {
            scheduleFire(true);
        }
    }

    protected void updateDeploymentInfo() {
        deploymentInfo = Arrays.toString(
            deployments.values().stream()
                .map(RulesetDeployment::toString)
                .toArray(String[]::new)
        );
    }

    protected synchronized void printSessionStats() {
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
    }

    protected void publishRulesetStatus(RulesetDeployment deployment) {
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
    }

    protected void schedulePause(RulesetDeployment deployment) {
        long delay = deployment.getValidTo() - timerService.getCurrentTimeMillis();
        LOG.info("Scheduling pause of ruleset at '" + new Date(deployment.getValidTo()) + " for " + DurationFormatUtils.formatDurationHMS(delay) +": " + deployment.ruleset.getName());
        pauseTimers.put(deployment.getId(), executorService.schedule(() -> pauseRuleset(deployment), delay, TimeUnit.MILLISECONDS));
    }

    protected void pauseRuleset(RulesetDeployment deployment) {
        pauseTimers.remove(deployment.getId());

        if (!running) {
            return;
        }

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

        LOG.info("Un-pausing ruleset: " + deployment.getRuleset().getName());
        startRuleset(deployment);
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
