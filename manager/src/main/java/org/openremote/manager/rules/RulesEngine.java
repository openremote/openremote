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

import org.jeasy.rules.core.InferenceRulesEngine;
import org.jeasy.rules.core.RulesEngineParameters;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.facade.AssetsFacade;
import org.openremote.manager.rules.facade.NotificationsFacade;
import org.openremote.manager.rules.facade.UsersFacade;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.query.filter.GeofencePredicate;
import org.openremote.model.query.filter.LocationAttributePredicate;
import org.openremote.model.rules.*;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.model.rules.RulesetStatus.*;

public class RulesEngine<T extends Ruleset> {

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

    final protected TimerService timerService;
    final protected ManagerExecutorService executorService;
    final protected AssetStorageService assetStorageService;
    final protected ClientEventService clientEventService;

    final protected RulesEngineId<T> id;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected NotificationsFacade notificationFacade;
    final protected AssetLocationPredicateProcessor assetLocationPredicatesConsumer;

    final protected Map<Long, RulesetDeployment> deployments = new LinkedHashMap<>();
    final protected RulesFacts facts;
    final protected InferenceRulesEngine engine;

    protected boolean running;
    protected boolean trackLocationPredicates;
    protected ScheduledFuture fireTimer;
    protected ScheduledFuture statsTimer;

    // Only used to optimize toString(), contains the details of this engine
    protected String deploymentInfo;

    // Only used in tests to prevent scheduled firing of engine
    protected boolean disableTemporaryFactExpiration = false;

    public RulesEngine(TimerService timerService,
                       ManagerIdentityService identityService,
                       ManagerExecutorService executorService,
                       AssetStorageService assetStorageService,
                       AssetProcessingService assetProcessingService,
                       NotificationService notificationService,
                       ClientEventService clientEventService,
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
        this.assetLocationPredicatesConsumer = assetLocationPredicatesConsumer;

        this.facts = new RulesFacts(timerService, assetsFacade, this, RULES_LOG);
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
    public Set<AssetState> getAssetStates() {
        return new HashSet<>(facts.getAssetStates());
    }

    /**
     * @return a shallow copy of the asset event facts.
     */
    public List<TemporaryFact<AssetState>> getAssetEvents() {
        return new ArrayList<>(facts.getAssetEvents());
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isError() {
        for (RulesetDeployment deployment : deployments.values()) {
            if (deployment.status == COMPILATION_ERROR || deployment.getStatus() == EXECUTION_ERROR) {
                return true;
            }
        }
        return false;
    }

    public int getExecutionErrorDeploymentCount() {
        return (int) deployments.values().stream().filter(deployment -> deployment.getStatus() == EXECUTION_ERROR).count();
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
     * @return <code>true</code> if all rulesets are {@link RulesetStatus#DEPLOYED} and this engine can be
     * started.
     */
    public boolean isDeployed() {
        return deployments.values().stream().allMatch(rd -> rd.getStatus() == DEPLOYED);
    }

    public void addRuleset(T ruleset) {
        if (ruleset == null || ruleset.getRules() == null || ruleset.getRules().isEmpty()) {
            // Assume it's a success if deploying an empty ruleset
            LOG.finest("Ruleset is empty so no rules to deploy");
            return;
        }

        RulesetDeployment deployment = deployments.get(ruleset.getId());

        stop();

        // Check if ruleset is already deployed (maybe an older version)
        if (deployment != null) {
            LOG.info("Removing ruleset deployment: " + ruleset);
            deployments.remove(ruleset.getId());
            updateDeploymentInfo();
        }

        deployment = new RulesetDeployment(ruleset, timerService, assetStorageService);

        boolean compilationSuccessful = deployment.registerRules(ruleset, assetsFacade, usersFacade, notificationFacade);

        if (!compilationSuccessful) {
            // If any other ruleset is DEPLOYED in this scope, demote to READY
            for (RulesetDeployment rd : deployments.values()) {
                if (rd.getStatus() == DEPLOYED) {
                    rd.setStatus(READY);
                    publishRulesetStatus(
                        rd.getRuleset(),
                        rd.getStatus(),
                        rd.getErrorMessage()
                    );
                }
            }
        } else {
            // If any other ruleset is READY in this scope, promote to DEPLOYED
            for (RulesetDeployment rd : deployments.values()) {
                if (rd.getStatus() == READY) {
                    rd.setStatus(DEPLOYED);
                    publishRulesetStatus(
                        rd.getRuleset(),
                        rd.getStatus(),
                        rd.getErrorMessage()
                    );
                }
            }
        }

        // Add new ruleset and set its status to either DEPLOYED or COMPILATION_ERROR
        deployment.setStatus(compilationSuccessful ? DEPLOYED : COMPILATION_ERROR);
        deployments.put(ruleset.getId(), deployment);
        updateDeploymentInfo();

        publishRulesetStatus(
            ruleset,
            deployment.getStatus(),
            deployment.getErrorMessage()
        );

        start();
    }

    /**
     * @return <code>true</code> if this rules engine has no deployments.
     */
    public boolean removeRuleset(Ruleset ruleset) {
        if (!deployments.containsKey(ruleset.getId())) {
            LOG.finer("Ruleset cannot be retracted as it was never deployed: " + ruleset);
            return deployments.size() == 0;
        }

        stop();

        deployments.remove(ruleset.getId());
        updateDeploymentInfo();

        publishRulesetStatus(ruleset, ruleset.isEnabled() ? REMOVED : DISABLED, null);

        // If there are no deployments with COMPILATION_ERROR, promote all which are READY to DEPLOYED
        boolean anyDeploymentsHaveCompilationError = deployments
            .values()
            .stream()
            .anyMatch(rd -> rd.getStatus() == COMPILATION_ERROR);

        if (!anyDeploymentsHaveCompilationError) {
            deployments.values().forEach(rd -> {
                if (rd.getStatus() == READY) {
                    rd.setStatus(DEPLOYED);
                    publishRulesetStatus(
                        rd.getRuleset(),
                        rd.getStatus(),
                        rd.getErrorMessage()
                    );
                }
            });
        }

        if (deployments.size() > 0) {
            start();
            return false;
        } else {
            publishRulesEngineStatus();
            return true;
        }
    }

    public void start() {
        if (isRunning()) {
            return;
        }

        if (!isDeployed()) {
            LOG.fine("Cannot start rules engine, not all rulesets are status " + DEPLOYED);
            return;
        }

        if (deployments.size() == 0) {
            LOG.finest("No rulesets so nothing to start");
            return;
        }

        LOG.info("Starting: " + this);
        running = true;
        trackLocationPredicates = true;
        publishRulesEngineStatus();
        fire();

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

    public void fire() {
        withLock(toString() + "::scheduleFire", () -> {
            // Schedule a firing within the guaranteed expiration time (so not immediately), and
            // only if the last firing is done. This effectively limits how often the rules engine
            // will fire, only once within the guaranteed minimum expiration time.
            if (fireTimer == null || fireTimer.isDone()) {
                LOG.fine("Scheduling rules firing on: " + this);
                fireTimer = executorService.schedule(
                    () -> withLock(RulesEngine.this.toString() + "::fire", () -> {

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
                            executorService.schedule(this::fire, 0);
                        } else if (!disableTemporaryFactExpiration) {
                            LOG.fine("No temporary facts present/changed when firing rules on: " + this);
                        }

                    }),
                    TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
                );
            }
        });
    }

    protected void fireAllDeployments() {
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

        for (RulesetDeployment deployment : deployments.values()) {
            try {
                RULES_LOG.fine("Firing rules @" + clock + " of: " + deployment);

                // If full detail logging is enabled
                if (RULES_LOG.isLoggable(Level.FINEST)) {
                    // Log asset states and events before firing (note that this will log at INFO)
                    facts.logFacts(RULES_LOG);
                }

                // Reset facts for this firing (loop detection etc.)
                facts.reset();

                long startTimestamp = System.currentTimeMillis();
                engine.fire(deployment.getRules(), facts);
                RULES_LOG.fine("Rules executed in: " + (System.currentTimeMillis() - startTimestamp) + "ms");

            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "On " + RulesEngine.this + ", error firing rules of: " + deployment, ex);

                deployment.setStatus(EXECUTION_ERROR);
                deployment.setError(ex);
                publishRulesetStatus(deployment.getRuleset(), deployment.getStatus(), deployment.getErrorMessage());

                // TODO We always stop on any error, good idea?
                // TODO We only get here on LHS runtime errors, RHS runtime errors are in RuleFacts.onFailure()
                stop();

                // TODO We skip any other deployment when we hit the first error, good idea?
                break;
            } finally {
                // Reset facts after this firing (loop detection etc.)
                facts.reset();
            }
        }

        if (trackLocationPredicates) {
            trackLocationPredicates = false;
            if (assetLocationPredicatesConsumer != null) {
                processLocationRules(facts.stopTrackingLocationRules());
            }
        }
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean systemShutdownInProgress) {
        if (!isRunning()) {
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
        running = false;

        if (!systemShutdownInProgress && assetLocationPredicatesConsumer != null) {
            assetLocationPredicatesConsumer.accept(this, null);
        }

        publishRulesEngineStatus();
    }

    public void updateFact(AssetState assetState, boolean fireImmediately) {
        facts.putAssetState(assetState);
        trackLocationPredicates = trackLocationPredicates || assetState.getAttributeName().equals(AttributeType.LOCATION.getName());
        if (fireImmediately) {
            fire();
        }
    }

    public void removeFact(AssetState assetState) {
        facts.removeAssetState(assetState);
        trackLocationPredicates = trackLocationPredicates || assetState.getAttributeName().equals(AttributeType.LOCATION.getName());
        fire();
    }

    public void insertFact(String expires, AssetState assetState) {
        facts.insertAssetEvent(expires, assetState);
        trackLocationPredicates = trackLocationPredicates || assetState.getAttributeName().equals(AttributeType.LOCATION.getName());
        fire();
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
            Collection<AssetState> assetStateFacts = facts.getAssetStates();
            Collection<TemporaryFact<AssetState>> assetEventFacts = facts.getAssetEvents();
            Map<String, Object> namedFacts = facts.getNamedFacts();
            Collection<Object> anonFacts = facts.getAnonymousFacts();
            long temporaryFactsCount = facts.getTemporaryFacts().count();
            long total = assetStateFacts.size() + assetEventFacts.size() + namedFacts.size() + anonFacts.size();
            STATS_LOG.info("On " + this + ", in memory facts are Total: " + total
                + ", AssetState: " + assetStateFacts.size()
                + ", AssetEvent: " + assetEventFacts.size()
                + ", Named: " + namedFacts.size()
                + ", Anonymous: " + anonFacts.size()
                + ", Temporary: " + temporaryFactsCount);

            // Additional details if FINEST is enabled
            if (STATS_LOG.isLoggable(Level.FINEST)) {
                facts.logFacts(STATS_LOG);
            }
        });
    }

    /**
     * This is called with all the asset's that have a location attribute marked with {@link AssetMeta#RULE_STATE} and
     * that are in the scope of a rule containing a location predicate.
     */
    protected void processLocationRules(List<AssetStateLocationPredicates> assetStateLocationPredicates) {
        if (assetLocationPredicatesConsumer != null) {
            assetLocationPredicatesConsumer.accept(this, assetStateLocationPredicates);
        }
    }

    protected RulesEngineStatus getStatus() {
        return getStatus(getCompilationErrorDeploymentCount(), getExecutionErrorDeploymentCount());
    }

    protected RulesEngineStatus getStatus(int compilationErrors, int executionErrors) {
        if (isRunning()) {
            return compilationErrors > 0 || executionErrors > 0 ? RulesEngineStatus.ERROR : RulesEngineStatus.RUNNING;
        }

        return compilationErrors > 0 || executionErrors > 0 ? RulesEngineStatus.ERROR : deployments.isEmpty() ? null : RulesEngineStatus.STOPPED;
    }

    protected void publishRulesEngineStatus() {
        withLock(getClass().getSimpleName() + "::publishRulesEngineStatus", () -> {

            String engineId = id == null ? null : id.getRealmId().orElse(id.getAssetId().orElse(null));
            int compilationErrors = getCompilationErrorDeploymentCount();
            int executionErrors = getExecutionErrorDeploymentCount();
            RulesEngineInfo engineInfo = new RulesEngineInfo(
                getStatus(compilationErrors, executionErrors),
                compilationErrors,
                executionErrors);

            RulesEngineStatusEvent event = new RulesEngineStatusEvent(
                timerService.getCurrentTimeMillis(),
                engineId,
                engineInfo
            );

            LOG.fine("Publishing rules engine status event: " + event);

            // Notify clients
            clientEventService.publishEvent(event);
        });
    }

    protected void publishRulesetStatus(Ruleset ruleset, RulesetStatus status, String error) {
        withLock(getClass().getSimpleName() + "::publishRulesetStatus", () -> {

            String engineId = id == null ? null : id.getRealmId().orElse(id.getAssetId().orElse(null));

            ruleset.setStatus(status);
            ruleset.setError(error);

            RulesetChangedEvent event = new RulesetChangedEvent(
                timerService.getCurrentTimeMillis(),
                engineId,
                ruleset
            );

            LOG.fine("Publishing ruleset status event: " + event);

            // Notify clients
            clientEventService.publishEvent(event);
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
