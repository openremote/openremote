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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
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
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.query.filter.GeofencePredicate;
import org.openremote.model.query.filter.LocationAttributePredicate;
import org.openremote.model.rules.*;
import org.openremote.model.syslog.SyslogCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.rules.RulesetStatus.*;

public class RulesEngine<T extends Ruleset> {

    /**
     * Allows rule deployments to track changes to the {@link AttributeInfo}s in scope
     */
    public static final class AssetStateChangeEvent {
        public PersistenceEvent.Cause cause;
        public AttributeInfo assetState;

        public AssetStateChangeEvent(PersistenceEvent.Cause cause, AttributeInfo assetState) {
            this.cause = cause;
            this.assetState = assetState;
        }
    }

    /**
     * Identifies a set of {@link LocationAttributePredicate}s associated with a particular {@link Asset}
     */
    public static final class AssetLocationPredicates {

        final String assetId;
        final Set<GeofencePredicate> locationPredicates;

        public AssetLocationPredicates(String assetId, Set<GeofencePredicate> locationPredicates) {
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

    protected final Logger LOG;

    // Separate logger for periodic stats printer
    public static final Logger STATS_LOG = Logger.getLogger("org.openremote.rules.RulesEngineStats");
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

    final protected Map<Long, RulesetDeployment> deployments = new ConcurrentHashMap<>();
    final protected Map<Long, RulesetStatus> deploymentStatusMap = new ConcurrentHashMap<>();
    final protected RulesFacts facts;
    final protected AbstractRulesEngine engine;

    protected boolean running;
    protected long lastFireTimestamp;
    protected boolean trackLocationPredicates;
    protected ScheduledFuture<?> fireTimer;
    protected ScheduledFuture<?> statsTimer;

    // Only used to optimize toString(), contains the details of this engine
    protected String deploymentInfo;
    protected Timer rulesFiringTimer;

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
                       AssetLocationPredicateProcessor assetLocationPredicatesConsumer,
                       MeterRegistry meterRegistry) {
        this.timerService = timerService;
        this.rulesService = rulesService;
        this.executorService = executorService;
        this.assetStorageService = assetStorageService;
        this.clientEventService = clientEventService;
        this.id = id;

        String ruleEngineCategory = id.scope.getSimpleName().replace("Ruleset", "Engine-") + id.getId().orElse("");
        LOG = SyslogCategory.getLogger(SyslogCategory.RULES, RulesEngine.class.getName() + "." + ruleEngineCategory);

        AssetsFacade<T> assetsFacade = new AssetsFacade<>(id, assetStorageService, attributeEvent -> {
            try {
                assetProcessingService.sendAttributeEvent(attributeEvent, getClass().getSimpleName());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to dispatch attribute event");
            }
        });
        this.assetsFacade = assetsFacade;
        this.usersFacade = new UsersFacade<>(id, assetStorageService, notificationService, identityService);
        this.notificationFacade = new NotificationsFacade<>(id, notificationService);
        this.webhooksFacade = new WebhooksFacade<>(id, webhookService);
        this.historicFacade = new HistoricFacade<>(id, assetDatapointService);
        this.predictedFacade = new PredictedFacade<>(id, assetPredictedDatapointService);
        this.assetLocationPredicatesConsumer = assetLocationPredicatesConsumer;

        this.facts = new RulesFacts(timerService, assetStorageService, assetsFacade, this, LOG);
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

        if (meterRegistry != null) {
            meterRegistry.gauge("or.rules.facts", Tags.of("type", id.getScope().getSimpleName(), "id", getEngineId()), facts, (facts) -> (double) facts.getFactCount());
            rulesFiringTimer = meterRegistry.timer("or.rules.firing", Tags.of("type", id.getScope().getSimpleName(), "id", getEngineId()));
        }
    }

    public RulesEngineId<T> getId() {
        return id;
    }

    /**
     * @return a shallow copy of the asset state facts.
     */
    public synchronized Set<AttributeInfo> getAssetStates() {
        return new HashSet<>(facts.getAssetStates());
    }

    /**
     * @return a shallow copy of the asset event facts.
     */
    public synchronized List<TemporaryFact<AttributeInfo>> getAssetEvents() {
        return new ArrayList<>(facts.getAssetEvents());
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isError() {
        for (RulesetDeployment deployment : deployments.values()) {
            if (deployment.isError()) {
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

    public void addRuleset(T ruleset) {

        // Check for previous version of this ruleset
        RulesetDeployment deployment = deployments.get(ruleset.getId());
        boolean wasRunning = this.running;

        stop();

        if (deployment != null) {
            removeRuleset(deployment.ruleset);
        }

        deployment = new RulesetDeployment(ruleset, timerService, assetStorageService, executorService, assetsFacade, usersFacade, notificationFacade, webhooksFacade, historicFacade, predictedFacade);
        deployment.init();
        deployments.put(ruleset.getId(), deployment);
        publishRulesetStatus(deployment);
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
            LOG.fine("Ruleset cannot be retracted as it was never deployed: " + ruleset);
        } else {
            boolean wasRunning = this.running;
            stop();
            stopRuleset(deployment);
            deployments.values().remove(deployment);
            updateDeploymentInfo();
            if (wasRunning && !deployments.isEmpty()) {
                start();
            }
        }

        return deployments.isEmpty();
    }

    public void start() {
        if (running) {
            return;
        }

        if (deployments.size() == 0) {
            LOG.finest("No rulesets so nothing to start");
            return;
        }

        boolean canAnyStart = deployments.values().stream().noneMatch(RulesetDeployment::canStart);

        if (canAnyStart) {
            LOG.info("Cannot start rules engine as no rulesets are able to be started");
            return;
        }

        LOG.info("Starting: " + id);
        running = true;
        trackLocationPredicates(true);

        deployments.values().forEach(this::startRuleset);

        updateDeploymentInfo();
        publishRulesEngineStatus();
        scheduleFire(true);

        // Start a background stats printer if INFO level logging is enabled
        if (STATS_LOG.isLoggable(Level.FINE) || STATS_LOG.isLoggable(Level.FINEST)) {
            if (STATS_LOG.isLoggable(Level.FINEST)) {
                LOG.info("Enabling periodic statistics output at FINEST level every 30 seconds on category: " + STATS_LOG.getName());
            } else {
                LOG.info("Enabling periodic full memory dump at FINE level every 30 seconds on category: " + STATS_LOG.getName());
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
        if (!running) {
            return;
        }
        running = false;
        LOG.info("Stopping: " + id);
        if (fireTimer != null) {
            fireTimer.cancel(true);
            fireTimer = null;
        }
        if (statsTimer != null) {
            statsTimer.cancel(true);
            statsTimer = null;
        }

        new HashSet<>(deployments.values()).forEach(this::stopRuleset);

        if (assetLocationPredicatesConsumer != null) {
            assetLocationPredicatesConsumer.accept(this, null);
        }

        publishRulesEngineStatus();
    }

    protected synchronized void startRuleset(RulesetDeployment deployment) {
        if (!running) {
            return;
        }

        if (deployment.start(facts)) {
            publishRulesetStatus(deployment);
        }
    }

    protected synchronized void stopRuleset(RulesetDeployment deployment) {
        if (deployment.stop(facts)) {
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

        LOG.finest("Scheduling rules firing in " + fireTimeMillis + "ms");
        fireTimer = executorService.schedule(
            () -> {
                synchronized (RulesEngine.this) {
                    // Process rules for all deployments
                    fireAllDeployments();
                    fireTimer = null;
                    scheduleFire(false);
                }
            },
            fireTimeMillis,
            TimeUnit.MILLISECONDS
        );
    }

    protected void fireAllDeployments() {
        if (!running) {
            return;
        }

        if (trackLocationPredicates && assetLocationPredicatesConsumer != null) {
            facts.startTrackingLocationRules();
        }

        // Remove any expired temporary facts
        facts.removeExpiredTemporaryFacts();
        long executionTotalMillis = timerService.getCurrentTimeMillis();

        if (rulesFiringTimer != null) {
            rulesFiringTimer.record(this::doFire);
        } else {
            doFire();
        }

        trackLocationPredicates(false);
        executionTotalMillis = (timerService.getCurrentTimeMillis() - executionTotalMillis);

        if (executionTotalMillis > 500) {
            LOG.warning("Rules firing took " + executionTotalMillis + "ms");
        } else {
            LOG.fine("Rules firing took " + executionTotalMillis + "ms");
        }
    }

    protected void doFire() {
        for (RulesetDeployment deployment : deployments.values()) {
            try {

                RulesetStatus status = deployment.getStatus();
                publishRulesetStatus(deployment);

                if (status == DEPLOYED) {

                    LOG.finest("Executing rules of: " + deployment);

                    // If full detail logging is enabled
                    // Log asset states and events before firing
                    facts.logFacts(LOG, Level.FINEST);

                    // Reset facts for this firing (loop detection etc.)
                    facts.reset();

                    long startTimestamp = timerService.getCurrentTimeMillis();
                    engine.fire(deployment.getRules(), facts);
                    long executionMillis = (timerService.getCurrentTimeMillis() - startTimestamp);
                    LOG.fine("Rules deployment '" + deployment.getName() + "' executed in: " + executionMillis + "ms");
                } else {
                    LOG.fine("Rules deployment '" + deployment.getName() + "' skipped as status is: " + status);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Error executing rules of: " + deployment, ex);

                deployment.setStatus(ex instanceof RulesLoopException ? LOOP_ERROR : EXECUTION_ERROR);
                deployment.setError(ex);
                publishRulesetStatus(deployment);

                if (ex instanceof RulesLoopException || !deployment.ruleset.isContinueOnError()) {
                    stop();
                    break;
                }
            } finally {
                // Reset facts after this firing (loop detection etc.)
                facts.reset();
                lastFireTimestamp = timerService.getCurrentTimeMillis();
            }
        }
    }

    protected String getEngineId() {
        if (id.scope == GlobalRuleset.class) {
            return "";
        }
        if (id.scope == RealmRuleset.class) {
            return id.realm;
        }
        return id.realm + ":" + id.assetId;
    }

    protected synchronized void notifyAssetStatesChanged(AssetStateChangeEvent event) {
        for (RulesetDeployment deployment : deployments.values()) {
            if (!deployment.isError()) {
                deployment.onAssetStatesChanged(facts, event);
            }
        }
    }

    public synchronized void updateOrInsertAttributeInfo(AttributeInfo attributeInfo, boolean insert) {
        facts.putAssetState(attributeInfo);
        // Make sure location predicate tracking is activated before notifying the deployments otherwise they won't report location predicates
        trackLocationPredicates(trackLocationPredicates || (insert && attributeInfo.getName().equals(Asset.LOCATION.getName())));
        notifyAssetStatesChanged(new AssetStateChangeEvent(insert ? PersistenceEvent.Cause.CREATE : PersistenceEvent.Cause.UPDATE, attributeInfo));
        if (running) {
            scheduleFire(true);
        }
    }

    public synchronized void removeAttributeInfo(AttributeInfo attributeInfo) {
        facts.removeAssetState(attributeInfo);
        // Make sure location predicate tracking is activated before notifying the deployments otherwise they won't report location predicates
        trackLocationPredicates(trackLocationPredicates || attributeInfo.getName().equals(Asset.LOCATION.getName()));
        notifyAssetStatesChanged(new AssetStateChangeEvent(PersistenceEvent.Cause.DELETE, attributeInfo));
        if (running) {
            scheduleFire(true);
        }
    }

    public synchronized void insertAttributeEvent(long expiresMillis, AttributeInfo attributeInfo) {
        facts.insertAttributeEvent(expiresMillis, attributeInfo);
        if (running) {
            scheduleFire(true);
        }
    }

    public synchronized void removeAttributeEvents(AttributeRef attributeRef) {
        facts.removeAttributeEvents(attributeRef);
    }

    protected void updateDeploymentInfo() {
        deploymentInfo = Arrays.toString(
            deployments.values().stream()
                .map(RulesetDeployment::toString)
                .toArray(String[]::new)
        );
    }

    protected synchronized void printSessionStats() {
        Collection<AttributeInfo> assetStateFacts = facts.getAssetStates();
        Collection<TemporaryFact<AttributeInfo>> assetEventFacts = facts.getAssetEvents();
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
    protected void processLocationRules(List<AssetLocationPredicates> assetStateLocationPredicates) {
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

        LOG.finest("Publishing rules engine status event: " + event);

        // Notify clients
        clientEventService.publishEvent(event);
    }

    protected void publishRulesetStatus(RulesetDeployment deployment) {
        if (!running) {
            return;
        }

        Ruleset ruleset = deployment.ruleset;
        RulesetStatus previousStatus = deploymentStatusMap.get(ruleset.getId());
        String engineId = id == null ? null : id.getRealm().orElse(id.getAssetId().orElse(null));
        RulesetStatus currentStatus = deployment.getStatus();

        if (currentStatus != previousStatus) {
            deploymentStatusMap.put(ruleset.getId(), currentStatus);
            ruleset.setStatus(deployment.getStatus());
            ruleset.setError(deployment.getErrorMessage());

            RulesetChangedEvent event = new RulesetChangedEvent(
                timerService.getCurrentTimeMillis(),
                engineId,
                ruleset
            );

            LOG.finest("Ruleset '" + deployment.getName() + "': status=" + currentStatus);

            // Notify clients
            clientEventService.publishEvent(event);
        }
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
