/*
 * Copyright 2018, OpenRemote Inc.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.MediaType;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.notification.EmailNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.PushNotificationMessage;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.rules.*;
import org.openremote.model.rules.json.*;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.webhook.Webhook;
import org.quartz.CronExpression;
import org.shredzone.commons.suncalc.SunTimes;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.manager.rules.AssetQueryPredicate.groupIsEmpty;
import static org.openremote.model.query.filter.LocationAttributePredicate.getLocationPredicates;
import static org.openremote.model.util.ValueUtil.LOG;
import static org.openremote.model.util.ValueUtil.distinctByKey;

public class JsonRulesBuilder extends RulesBuilder {

    static class RuleActionExecution {
        Runnable runnable;
        long delay;

        public RuleActionExecution(Runnable runnable, long delay) {
            this.runnable = runnable;
            this.delay = delay;
        }
    }

    /**
     * Stores all state for a given {@link RuleCondition} and calculates which {@link AssetState}s match and don't
     * match the condition.
     */
    class RuleConditionState {

        RuleCondition ruleCondition;
        final TimerService timerService;
        boolean trackUnmatched;
        AssetQuery.OrderBy orderBy;
        int limit;
        LogicGroup<AttributePredicate> attributePredicates = null;
        Function<Collection<AssetState<?>>, Set<AssetState<?>>> assetPredicate = null;
        Set<AssetState<?>> unfilteredAssetStates = new HashSet<>();
        Set<AssetState<?>> previouslyMatchedAssetStates = new HashSet<>();
        Set<AssetState<?>> previouslyUnmatchedAssetStates;
        Predicate<Long> timePredicate;
        RuleConditionEvaluationResult lastEvaluationResult;

        @SuppressWarnings("ConstantConditions")
        public RuleConditionState(RuleCondition ruleCondition, boolean trackUnmatched, TimerService timerService) throws Exception {
            this.timerService = timerService;
            this.ruleCondition = ruleCondition;
            this.trackUnmatched = trackUnmatched;

            if (trackUnmatched) {
                previouslyUnmatchedAssetStates = new HashSet<>();
            }

            if (!TextUtil.isNullOrEmpty(ruleCondition.duration)) {
                try {
                    final long duration = TimeUtil.parseTimeDuration(ruleCondition.duration);
                    AtomicLong nextExecuteMillis = new AtomicLong(timerService.getCurrentTimeMillis());

                    timePredicate = (time) -> {
                        long nextExecute = nextExecuteMillis.get();
                        if (time >= nextExecute) {
                            nextExecuteMillis.set(nextExecute + duration);
                            return true;
                        }
                        return false;
                    };
                } catch (Exception e) {
                    log(Level.SEVERE, "Failed to parse rule condition duration expression: " + ruleCondition.duration, e);
                    throw e;
                }
            } else if (!TextUtil.isNullOrEmpty(ruleCondition.cron)) {
                try {
                    CronExpression timerExpression = new CronExpression(ruleCondition.cron);
                    timerExpression.setTimeZone(TimeZone.getTimeZone("UTC"));
                    AtomicLong nextExecuteMillis = new AtomicLong(timerExpression.getNextValidTimeAfter(new Date(timerService.getCurrentTimeMillis())).getTime());

                    timePredicate = (time) -> {
                        long nextExecute = nextExecuteMillis.get();
                        if (time >= nextExecute) {
                            nextExecuteMillis.set(timerExpression.getNextValidTimeAfter(timerExpression.getNextInvalidTimeAfter(new Date(nextExecute))).getTime());
                            return true;
                        }
                        return false;
                    };
                } catch (Exception e) {
                    log(Level.SEVERE, "Failed to parse rule condition cron expression: " + ruleCondition.cron, e);
                    throw e;
                }
            } else if (ruleCondition.sun != null) {
                SunTimes.Parameters sunCalculator = getSunCalculator(jsonRuleset, ruleCondition.sun, timerService);
                final long offsetMillis = ruleCondition.sun.getOffsetMins() != null ? ruleCondition.sun.getOffsetMins() * 60000 : 0;
                final boolean useRiseTime = ruleCondition.sun.getPosition() == SunPositionTrigger.Position.SUNRISE || ruleCondition.sun.getPosition().toString().startsWith(SunPositionTrigger.MORNING_TWILIGHT_PREFIX);

                // Calculate the next occurrence
                AtomicReference<SunTimes> sunTimes = new AtomicReference<>(sunCalculator.execute());

                Function<Long, Long> nextExecuteMillisCalculator = (time) -> {
                    ZonedDateTime occurrence = useRiseTime ? sunTimes.get().getRise() : sunTimes.get().getSet();

                    if (occurrence == null) {
                        log(Level.WARNING, "Rule condition requested sun position never occurs at the specified location: " + ruleCondition.sun);
                        return Long.MAX_VALUE;
                    }

                    long nextMillis = occurrence.toInstant().toEpochMilli() + offsetMillis;

                    // If occurrence is before requested time then advance the sun calculator to either reset occurrence or 5 mins before requested time (whichever is later)
                    if (nextMillis < time) {
                        ZonedDateTime resetOccurrence = useRiseTime ? sunTimes.get().getSet() : sunTimes.get().getRise();
                        sunTimes.set(sunCalculator.on(new Date(Math.max(resetOccurrence.toInstant().toEpochMilli(), time - 300000))).execute());
                    }

                    return nextMillis;
                };

                timePredicate = (time) -> {
                    long nextExecute = nextExecuteMillisCalculator.apply(time);

                    // Next execute must be within a minute of requested time
                    if (time >= nextExecute && time - nextExecute < 60000) {
                        log(Level.INFO, "Rule condition sun position has triggered at: " + timerService.getCurrentTimeMillis());
                        return true;
                    }
                    return false;
                };
            } else if (ruleCondition.assets != null) {

                // Pull out order, limit and attribute predicates so they can be applied at required times
                orderBy = ruleCondition.assets.orderBy;
                limit = ruleCondition.assets.limit;
                attributePredicates = ruleCondition.assets.attributes;

                if (attributePredicates != null && attributePredicates.items != null) {
                    // Only supports a single level or logic group for attributes (i.e. cannot nest groups in the UI so
                    // don't support it here either)
                    attributePredicates.groups = null;
                    assetPredicate = AssetQueryPredicate.asAssetStateMatcher(timerService::getCurrentTimeMillis, attributePredicates);
                }
                ruleCondition.assets.orderBy = null;
                ruleCondition.assets.limit = 0;
                ruleCondition.assets.attributes = null;
            } else {
                throw new IllegalStateException("Invalid rule condition either timer or asset query must be set");
            }
        }

        void updateUnfilteredAssetStates(RulesFacts facts, RulesEngine.AssetStateChangeEvent event) {

            // Only interested in this when condition is of type asset query
            if (ruleCondition.assets != null) {
                // Clear last trigger to ensure update runs again
                lastEvaluationResult = null;

                if (event == null || event.cause == PersistenceEvent.Cause.CREATE) {
                    // Do a complete refresh of unfiltered asset states based on the asset query (without attribute predicates)
                    unfilteredAssetStates = facts.matchAssetState(ruleCondition.assets).collect(Collectors.toSet());
                } else {
                    // Replace or remove asset state as required
                    switch (event.cause) {
                        case UPDATE:
                            // Only insert if fact was already in there (i.e. it matches the asset type constraints)
                            if (unfilteredAssetStates.remove(event.assetState)) {
                                unfilteredAssetStates.add(event.assetState);
                            }
                            break;
                        case DELETE:
                            unfilteredAssetStates.remove(event.assetState);
                            break;
                    }
                }

                // During startup notify RulesFacts about any location predicates
                if (facts.trackLocationRules) {
                    facts.storeLocationPredicates(getLocationPredicates(attributePredicates));
                }
            }
        }

        void update(Map<String, Long> nextRecurAssetIdMap) {

            // Last trigger is cleared by rule RHS execution if a match is already found then skip the update
            if (lastEvaluationResult != null && lastEvaluationResult.matches) {
                return;
            }

            // Apply time condition if it exists
            if (timePredicate != null) {
                lastEvaluationResult = null;

                if (timePredicate.test(timerService.getCurrentTimeMillis())) {
                    lastEvaluationResult = new RuleConditionEvaluationResult(true, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                }

                return;
            }

            if (unfilteredAssetStates.isEmpty()) {
                // Maybe assets have been deleted so remove any previous match data
                previouslyMatchedAssetStates.clear();
                if (trackUnmatched) {
                    previouslyUnmatchedAssetStates.clear();
                }
                log(Level.FINEST, "Rule trigger has no unfiltered asset states so no match");
                lastEvaluationResult = new RuleConditionEvaluationResult(false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                return;
            }

            List<AssetState<?>> matchedAssetStates;
            List<AssetState<?>> unmatchedAssetStates = Collections.emptyList();
            Collection<String> unmatchedAssetIds = Collections.emptyList();

            if (attributePredicates == null) {
                matchedAssetStates = new ArrayList<>(unfilteredAssetStates);
            } else {

                Map<Boolean, List<AssetState<?>>> results = new HashMap<>();
                ArrayList<AssetState<?>> matched = new ArrayList<>();
                ArrayList<AssetState<?>> unmatched = new ArrayList<>();
                results.put(true, matched);
                results.put(false, unmatched);

                unfilteredAssetStates.stream().collect(Collectors.groupingBy(AssetState::getId)).forEach((id, states) -> {
                    Set<AssetState<?>> matches = assetPredicate.apply(states);
                    if (matches != null) {
                        matched.addAll(matches);
                        unmatched.addAll(states.stream().filter(matches::contains).collect(Collectors.toSet()));
                    } else {
                        unmatched.addAll(states);
                    }
                });

                matchedAssetStates = results.getOrDefault(true, Collections.emptyList());
                unmatchedAssetStates = results.getOrDefault(false, Collections.emptyList());

                if (trackUnmatched) {

                    // Clear out previous unmatched that now match
                    previouslyUnmatchedAssetStates.removeIf(matchedAssetStates::contains);

                    // Filter out previous un-matches to avoid re-triggering
                    unmatchedAssetStates.removeIf(previouslyUnmatchedAssetStates::contains);

                }
            }

            // Remove previous matches where the asset state no longer matches
            previouslyMatchedAssetStates.removeIf(previousAssetState -> {

                Optional<AssetState<?>> matched = matchedAssetStates.stream()
                    .filter(matchedAssetState -> Objects.equals(previousAssetState, matchedAssetState))
                    .findFirst();

                boolean noLongerMatches = !matched.isPresent();

                if (!noLongerMatches) {
                    noLongerMatches = matched.map(matchedAssetState -> {
                        // If reset immediate meta item is set then remove previous state if timestamp is greater
                        boolean resetImmediately = matchedAssetState.getMeta().getValue(MetaItemType.RULE_RESET_IMMEDIATE).orElse(false);
                        return resetImmediately && matchedAssetState.getTimestamp() > previousAssetState.getTimestamp();
                    }).orElse(false);
                }

                if (noLongerMatches) {
                    log(Level.FINEST, "Rule trigger previously matched asset state no longer matches so resetting: " + previousAssetState);
                }

                return noLongerMatches;
            });

            // Remove matches that have an active recurrence timer
            matchedAssetStates.removeIf(matchedAssetState -> nextRecurAssetIdMap.containsKey(matchedAssetState.getId())
                && nextRecurAssetIdMap.get(matchedAssetState.getId()) > timerService.getCurrentTimeMillis());

            // Filter out previous matches to avoid re-triggering
            matchedAssetStates.removeIf(previouslyMatchedAssetStates::contains);

            // Select unique asset states based on asset id
            Stream<AssetState<?>> matchedAssetStateStream = matchedAssetStates.stream().filter(distinctByKey(AssetState::getId));

            // Order asset states before applying limit
            if (orderBy != null) {
                matchedAssetStateStream = matchedAssetStateStream.sorted(RulesFacts.asComparator(orderBy));
            }
            if (limit > 0) {
                matchedAssetStateStream = matchedAssetStateStream.limit(limit);
            }

            Collection<String> matchedAssetIds = matchedAssetStateStream.map(AssetState::getId).collect(Collectors.toList());

            if (trackUnmatched) {
                // Select unique asset states based on asset id
                Stream<AssetState<?>> unmatchedAssetStateStream = unmatchedAssetStates.stream().filter(distinctByKey(AssetState::getId));

                // Filter out unmatched asset ids that are in the matched list
                unmatchedAssetIds = unmatchedAssetStateStream
                        .filter(assetState -> !matchedAssetIds.contains(assetState.getId()))
                        .map(AssetState::getId)
                        .collect(Collectors.toList());
            }

            lastEvaluationResult = new RuleConditionEvaluationResult((!matchedAssetIds.isEmpty() || (trackUnmatched && !unmatchedAssetIds.isEmpty())), matchedAssetStates, matchedAssetIds, unmatchedAssetStates, unmatchedAssetIds);
            log(Level.FINEST, "Rule evaluation result: " + lastEvaluationResult);
        }

        Collection<String> getMatchedAssetIds() {

            if (lastEvaluationResult == null) {
                return Collections.emptyList();
            }
            return lastEvaluationResult.matchedAssetIds;
        }

        Collection<String> getUnmatchedAssetIds() {
            if (lastEvaluationResult == null) {
                return Collections.emptyList();
            }

            return lastEvaluationResult.unmatchedAssetIds;
        }
    }

    /**
     * This contains the results of a rule condition trigger evaluation.
     */
    static class RuleConditionEvaluationResult {
        boolean matches;
        Collection<AssetState<?>> matchedAssetStates;
        Collection<AssetState<?>> unmatchedAssetStates;
        Collection<String> matchedAssetIds;
        Collection<String> unmatchedAssetIds;

        public RuleConditionEvaluationResult(boolean matches, Collection<AssetState<?>> matchedAssetStates, Collection<String> matchedAssetIds, Collection<AssetState<?>> unmatchedAssetStates, Collection<String> unmatchedAssetIds) {
            this.matches = matches;
            this.matchedAssetStates = matchedAssetStates;
            this.matchedAssetIds = matchedAssetIds;
            this.unmatchedAssetStates = unmatchedAssetStates;
            this.unmatchedAssetIds = unmatchedAssetIds;
        }

        @Override
        public String toString() {
            return RuleConditionEvaluationResult.class.getSimpleName() + "{" +
                    "matches=" + matches +
                    ", matchedAssetStates=" + matchedAssetStates.size() +
                    ", unmatchedAssetStates=" + unmatchedAssetStates.size() +
                    ", matchedAssetIds=" + matchedAssetIds.size() +
                    ", unmatchedAssetIds=" + unmatchedAssetIds.size() +
                    '}';
        }
    }

    /**
     * Stores the state of the overall rule and each {@link RuleCondition}.
     */
    class RuleState {

        protected JsonRule rule;
        protected Map<String, RuleConditionState> conditionStateMap = new HashMap<>();
        protected Set<String> thenMatchedAssetIds;
        protected Set<String> otherwiseMatchedAssetIds;
        protected long nextRecur;
        protected boolean matched;
        protected Map<String, Long> nextRecurAssetIdMap = new HashMap<>();

        public RuleState(JsonRule rule) {
            this.rule = rule;
        }

        public void update(Supplier<Long> currentMillisSupplier) {

            matched = false;

            // Check if next recurrence in the future
            if (nextRecur > currentMillisSupplier.get()) {
                return;
            }

            // Clear out expired recurrence timers
            nextRecurAssetIdMap.entrySet().removeIf(entry -> entry.getValue() <= currentMillisSupplier.get());

            // Update each condition state
            log(Level.FINEST, "Updating rule condition states for rule: " + rule.name);
            conditionStateMap.values().forEach(ruleConditionState -> ruleConditionState.update(nextRecurAssetIdMap));

            thenMatchedAssetIds = new HashSet<>();
            otherwiseMatchedAssetIds = rule.otherwise != null ? new HashSet<>() : null;

            matched = updateMatches(rule.when, thenMatchedAssetIds, otherwiseMatchedAssetIds);

            if (!matched) {
                thenMatchedAssetIds.clear();
                if (otherwiseMatchedAssetIds != null) {
                    otherwiseMatchedAssetIds.clear();
                }
            }
        }

        public boolean thenMatched() {
            return (thenMatchedAssetIds != null && !thenMatchedAssetIds.isEmpty()) || !otherwiseMatched();
        }

        public boolean otherwiseMatched() {
            return otherwiseMatchedAssetIds != null && !otherwiseMatchedAssetIds.isEmpty();
        }

        protected boolean updateMatches(LogicGroup<RuleCondition> ruleConditionGroup, Set<String> thenMatchedAssetIds, Set<String> otherwiseMatchedAssetIds) {

            if (groupIsEmpty(ruleConditionGroup)) {
                return false;
            }

            LogicGroup.Operator operator = ruleConditionGroup.operator == null ? LogicGroup.Operator.AND : ruleConditionGroup.operator;
            boolean groupMatches = false;

            if (!ruleConditionGroup.getItems().isEmpty()) {

                if (operator == LogicGroup.Operator.AND) {
                    groupMatches = ruleConditionGroup.getItems().stream()
                        .map(ruleCondition -> conditionStateMap.get(ruleCondition.tag))
                        .allMatch(ruleConditionState -> ruleConditionState.lastEvaluationResult != null && ruleConditionState.lastEvaluationResult.matches);
                } else {
                    groupMatches = ruleConditionGroup.getItems().stream()
                        .map(ruleCondition -> conditionStateMap.get(ruleCondition.tag))
                        .anyMatch(ruleConditionState -> ruleConditionState.lastEvaluationResult != null && ruleConditionState.lastEvaluationResult.matches);
                }

                thenMatchedAssetIds.addAll(ruleConditionGroup.getItems().stream()
                    .map(ruleCondition -> conditionStateMap.get(ruleCondition.tag))
                    .filter(ruleConditionState -> ruleConditionState.lastEvaluationResult != null && ruleConditionState.lastEvaluationResult.matches)
                    .map(RuleConditionState::getMatchedAssetIds)//Get all matched assetIds
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet()));

                if (otherwiseMatchedAssetIds != null) {
                    otherwiseMatchedAssetIds.addAll(ruleConditionGroup.getItems().stream()
                        .map(ruleCondition -> conditionStateMap.get(ruleCondition.tag))
                        .filter(ruleConditionState -> ruleConditionState.trackUnmatched && ruleConditionState.lastEvaluationResult != null && ruleConditionState.lastEvaluationResult.matches)
                        .map(RuleConditionState::getUnmatchedAssetIds)//Get all unmatched assetIds
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet()));
                }
            }

            if (ruleConditionGroup.groups != null) {

                if (operator == LogicGroup.Operator.AND) {
                    if (!ruleConditionGroup.items.isEmpty() && !groupMatches) {
                        return false;
                    }

                    groupMatches = ruleConditionGroup.groups.stream()
                        .allMatch(group -> updateMatches(group, thenMatchedAssetIds, otherwiseMatchedAssetIds));

                } else {

                    // updateMatches has side effects which we need (inserts into then and otherwise)
                    //noinspection ReplaceInefficientStreamCount
                    groupMatches = ruleConditionGroup.groups.stream()
                        .filter(group -> updateMatches(group, thenMatchedAssetIds, otherwiseMatchedAssetIds))
                        .count() > 0;

                }
            }

            return groupMatches;
        }
    }

    public static final String PLACEHOLDER_RULESET_ID = "%RULESET_ID%";
    public static final String PLACEHOLDER_RULESET_NAME = "%RULESET_NAME%";
    public static final String PLACEHOLDER_TRIGGER_ASSETS = "%TRIGGER_ASSETS%";
    final static String TIMER_TEMPORAL_FACT_NAME_PREFIX = "TimerTemporalFact-";
    final static String LOG_PREFIX = "JSON Rule '";
    final protected AssetStorageService assetStorageService;
    final protected TimerService timerService;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected Notifications notificationsFacade;
    final protected Webhooks webhooksFacade;
    final protected HistoricDatapoints historicDatapointsFacade;
    final protected PredictedDatapoints predictedDatapointsFacade;
    final protected ScheduledExecutorService executorService;
    final protected BiConsumer<Runnable, Long> scheduledActionConsumer;
    final protected Map<String, RuleState> ruleStateMap = new HashMap<>();
    final protected JsonRule[] jsonRules;
    final protected Ruleset jsonRuleset;

    public JsonRulesBuilder(Ruleset ruleset, TimerService timerService,
                            AssetStorageService assetStorageService, ScheduledExecutorService executorService,
                            Assets assetsFacade, Users usersFacade, Notifications notificationsFacade, Webhooks webhooksFacade,
                            HistoricDatapoints historicDatapoints, PredictedDatapoints predictedDatapoints,
                            BiConsumer<Runnable, Long> scheduledActionConsumer) throws Exception {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.executorService = executorService;
        this.assetsFacade = assetsFacade;
        this.usersFacade = usersFacade;
        this.notificationsFacade = notificationsFacade;
        this.webhooksFacade = webhooksFacade;
        this.historicDatapointsFacade= historicDatapoints;
        this.predictedDatapointsFacade = predictedDatapoints;
        this.scheduledActionConsumer = scheduledActionConsumer;

        jsonRuleset = ruleset;
        String rulesStr = ruleset.getRules();
        rulesStr = rulesStr.replace(PLACEHOLDER_RULESET_ID, Long.toString(ruleset.getId()));
        rulesStr = rulesStr.replace(PLACEHOLDER_RULESET_NAME, ruleset.getName());

        JsonRulesetDefinition jsonRulesetDefinition = ValueUtil.parse(rulesStr, JsonRulesetDefinition.class).orElse(null);

        if (jsonRulesetDefinition == null || jsonRulesetDefinition.rules == null || jsonRulesetDefinition.rules.length == 0) {
            throw new IllegalArgumentException("No rules within ruleset so nothing to start: " + ruleset);
        }

        jsonRules = jsonRulesetDefinition.rules;

        for (JsonRule jsonRule : jsonRules) {
            add(jsonRule);
        }
    }

    public void stop(RulesFacts facts) {
        Arrays.stream(jsonRules).forEach(jsonRule ->
            executeRuleActions(jsonRule, jsonRule.onStop, "onStop", false, facts, null, assetsFacade, usersFacade, notificationsFacade, webhooksFacade, predictedDatapointsFacade, this.scheduledActionConsumer));

        // Remove temporal fact for timer rule evaluation
        String tempFactName = TIMER_TEMPORAL_FACT_NAME_PREFIX + jsonRuleset.getId();
        facts.remove(tempFactName);
    }

    public void start(RulesFacts facts) {

        Arrays.stream(jsonRules).forEach(jsonRule -> {
            executeRuleActions(jsonRule, jsonRule.onStart, "onStart", false, facts, null, assetsFacade, usersFacade, notificationsFacade, webhooksFacade, predictedDatapointsFacade, this.scheduledActionConsumer);
        });

        // Initialise asset states
        onAssetStatesChanged(facts, null);
    }

    public void onAssetStatesChanged(RulesFacts facts, RulesEngine.AssetStateChangeEvent event) {
        ruleStateMap.values().forEach(triggerStateMap -> triggerStateMap.conditionStateMap.values().forEach(ruleConditionState -> ruleConditionState.updateUnfilteredAssetStates(facts, event)));
    }

    protected JsonRulesBuilder add(JsonRule rule) throws Exception {

        if (ruleStateMap.containsKey(rule.name)) {
            throw new IllegalArgumentException("Rules must have a unique name within a ruleset, rule name '" + rule.name + "' already used");
        }

        RuleState ruleState = new RuleState(rule);
        ruleStateMap.put(rule.name, ruleState);
        addRuleConditionStates(rule.when, rule.otherwise != null, new AtomicInteger(0), ruleState.conditionStateMap);

        Condition condition = buildLhsCondition(rule, ruleState);
        Action action = buildRhsAction(rule, ruleState);

        if (condition == null || action == null) {
            throw new IllegalArgumentException("Error building JSON rule when or then is not defined: " + rule.name);
        }

        add().name(rule.name)
                .description(rule.description)
                .priority(rule.priority)
                .when(condition::evaluate)
                .then(action);

        return this;
    }

    protected void addRuleConditionStates(LogicGroup<RuleCondition> ruleConditionGroup, boolean trackUnmatched, AtomicInteger index, Map<String, RuleConditionState> triggerStateMap) throws Exception {
        if (ruleConditionGroup != null) {
            if (ruleConditionGroup.getItems().size() > 0) {
                for (RuleCondition ruleCondition : ruleConditionGroup.getItems()) {
                    if (TextUtil.isNullOrEmpty(ruleCondition.tag)) {
                        ruleCondition.tag = index.toString();
                    }

                    triggerStateMap.put(ruleCondition.tag, new RuleConditionState(ruleCondition, trackUnmatched, timerService));
                    index.incrementAndGet();
                }
            }
            if (ruleConditionGroup.groups != null && ruleConditionGroup.groups.size() > 0) {
                for (LogicGroup<RuleCondition> childRuleTriggerCondition : ruleConditionGroup.groups) {
                    addRuleConditionStates(childRuleTriggerCondition, trackUnmatched, index, triggerStateMap);
                }
            }
        }
    }

    protected Condition buildLhsCondition(JsonRule rule, RuleState ruleState) {
        if (rule.when == null) {
            return null;
        }

        return facts -> {
            ruleState.update(timerService::getCurrentTimeMillis);
            return ruleState.matched;
        };
    }

    protected Action buildRhsAction(JsonRule rule, RuleState ruleState) {

        if (rule.then == null) {
            return null;
        }

        return facts -> {

            try {
                if (ruleState.thenMatched()) {
                    log(Level.FINEST, "Triggered rule so executing 'then' actions for rule: " + rule.name);
                    executeRuleActions(rule, rule.then, "then", false, facts, ruleState, assetsFacade, usersFacade, notificationsFacade, webhooksFacade, predictedDatapointsFacade, scheduledActionConsumer);
                }

                if (rule.otherwise != null && ruleState.otherwiseMatched()) {
                    log(Level.FINEST, "Triggered rule so executing 'otherwise' actions for rule: " + rule.name);
                    executeRuleActions(rule, rule.otherwise, "otherwise", true, facts, ruleState, assetsFacade, usersFacade, notificationsFacade, webhooksFacade, predictedDatapointsFacade, scheduledActionConsumer);
                }
            } catch (Exception e) {
                log(Level.SEVERE, "Exception thrown during rule RHS execution", e);
                throw e;
            } finally {

                // Store recurrence times as required
                boolean recurPerAsset = rule.recurrence == null || rule.recurrence.scope != RuleRecurrence.Scope.GLOBAL;
                long currentTime = timerService.getCurrentTimeMillis();
                long nextRecur = rule.recurrence == null || rule.recurrence.mins == null ? Long.MAX_VALUE : currentTime + (rule.recurrence.mins * 60000);

                if (nextRecur > currentTime) {
                    if (recurPerAsset) {
                        ruleState.thenMatchedAssetIds.forEach(assetId -> ruleState.nextRecurAssetIdMap.put(assetId, nextRecur));
                    } else {
                        ruleState.nextRecur = nextRecur;
                    }
                }

                ruleState.conditionStateMap.values().forEach(ruleConditionState -> {
                    // Store last evaluation results in state
                    if (ruleConditionState.lastEvaluationResult != null) {

                        // Replace any stale matched asset states (values may have changed equality is by asset ID and attribute name)
                        // only need up to date values in the previously matched asset states previously unmatched asset states is only
                        // used to compare asset ID and attribute name.
                        ruleConditionState.previouslyMatchedAssetStates.removeAll(ruleConditionState.lastEvaluationResult.matchedAssetStates);
                        ruleConditionState.previouslyMatchedAssetStates.addAll(ruleConditionState.lastEvaluationResult.matchedAssetStates);

                        if (ruleConditionState.trackUnmatched) {
                            ruleConditionState.previouslyUnmatchedAssetStates.addAll(ruleConditionState.lastEvaluationResult.unmatchedAssetStates);
                        }
                    }

                    // Clear last results
                    ruleConditionState.lastEvaluationResult = null;
                });
            }
        };
    }

    public void executeRuleActions(JsonRule rule, RuleAction[] ruleActions, String actionsName, boolean useUnmatched, RulesFacts facts, RuleState ruleState, Assets assetsFacade, Users usersFacade, Notifications notificationsFacade, Webhooks webhooksFacade, PredictedDatapoints predictedDatapointsFacade, BiConsumer<Runnable, Long> scheduledActionConsumer) {

        if (ruleActions != null && ruleActions.length > 0) {

            long delay = 0L;

            for (int i = 0; i < ruleActions.length; i++) {

                RuleAction ruleAction = ruleActions[i];
                JsonRulesBuilder.RuleActionExecution actionExecution = buildRuleActionExecution(
                    rule,
                    ruleAction,
                    actionsName,
                    i,
                    useUnmatched,
                    facts,
                    ruleState,
                    assetsFacade,
                    usersFacade,
                    notificationsFacade,
                    webhooksFacade,
                    predictedDatapointsFacade
                );

                if (actionExecution != null) {
                    delay += actionExecution.delay;

                    if (delay > 0L) {
                        log(Level.FINE, "Delaying rule action for " + delay + "ms for rule action: " + rule.name + " '" + actionsName + "' action index " + i);
                        scheduledActionConsumer.accept(actionExecution.runnable, delay);
                    } else {
                        actionExecution.runnable.run();
                    }
                }
            }
        }
    }

    protected static Collection<String> getUserIds(Users users, UserQuery userQuery) {
        return users.getResults(userQuery).collect(Collectors.toList());
    }

    protected static Collection<String> getAssetIds(Assets assets, AssetQuery assetQuery) {
        return assets.getResults(assetQuery)
            .map(Asset::getId)
            .collect(Collectors.toList());
    }

    protected RuleActionExecution buildRuleActionExecution(JsonRule rule, RuleAction ruleAction, String actionsName, int index, boolean useUnmatched, RulesFacts facts, RuleState ruleState, Assets assetsFacade, Users usersFacade, Notifications notificationsFacade, Webhooks webhooksFacade, PredictedDatapoints predictedDatapointsFacade) {

        if (ruleAction instanceof RuleActionNotification notificationAction) {

            if (notificationAction.notification != null) {

                Notification notification = notificationAction.notification;

                if (notification.getMessage() != null) {
                    String body = null;

                    boolean isEmail = Objects.equals(notification.getMessage().getType(), EmailNotificationMessage.TYPE);
                    boolean isPush = Objects.equals(notification.getMessage().getType(), PushNotificationMessage.TYPE);

                    boolean isHtml = false;

                    if (isEmail) {
                        EmailNotificationMessage email = (EmailNotificationMessage) notification.getMessage();
                        isHtml = !TextUtil.isNullOrEmpty(email.getHtml());
                        body = isHtml ? email.getHtml() : email.getText();
                    } else if (isPush) {
                        PushNotificationMessage pushNotificationMessage = (PushNotificationMessage) notification.getMessage();
                        body = pushNotificationMessage.getBody();
                    }

                    if (!TextUtil.isNullOrEmpty(body)) {
                        if (body.contains(PLACEHOLDER_TRIGGER_ASSETS)) {
                            // Need to clone the notification
                            notification = ValueUtil.clone(notification);
                            String triggeredAssetInfo = buildTriggeredAssetInfo(useUnmatched, ruleState, isHtml, false);
                            body = body.replace(PLACEHOLDER_TRIGGER_ASSETS, triggeredAssetInfo);

                            if (isEmail) {
                                EmailNotificationMessage email = (EmailNotificationMessage) notification.getMessage();
                                if (isHtml) {
                                    email.setHtml(body);
                                } else {
                                    email.setText(body);
                                }
                            } else if (isPush) {
                                PushNotificationMessage pushNotificationMessage = (PushNotificationMessage) notification.getMessage();
                                pushNotificationMessage.setBody(body);
                            }
                        }
                    }
                }

                // Transfer the rule action target into notification targets
                Notification.TargetType targetType = Notification.TargetType.ASSET;
                if (ruleAction.target != null) {
                    if (ruleAction.target.users != null
                        && ruleAction.target.conditionAssets == null
                        && ruleAction.target.assets == null
                        && ruleAction.target.matchedAssets == null) {
                        targetType = Notification.TargetType.USER;
                    } else if (ruleAction.target.linkedUsers != null && ruleAction.target.linkedUsers) {
                        targetType = Notification.TargetType.USER;
                    } else if (ruleAction.target.custom != null
                        && ruleAction.target.conditionAssets == null
                        && ruleAction.target.assets == null
                        && ruleAction.target.matchedAssets == null) {
                        targetType = Notification.TargetType.CUSTOM;
                    }
                }

                Collection<String> ids = getRuleActionTargetIds(ruleAction.target, useUnmatched, ruleState, assetsFacade, usersFacade, facts);

                if (ids == null) {
                    notification.setTargets((List<Notification.Target>)null);
                } else {
                    Notification.TargetType finalTargetType = targetType;
                    notification.setTargets(ids.stream().map(id -> new Notification.Target(finalTargetType, id)).collect(Collectors.toList()));
                }

                log(Level.FINE, "Sending notification for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                Notification finalNotification = notification;
                return new RuleActionExecution(() -> notificationsFacade.send(finalNotification), 0);
            }
        }

        if (ruleAction instanceof RuleActionWebhook webhookAction) {
            Webhook webhook = webhookAction.webhook;
            if (webhook.getUrl() != null && webhook.getHttpMethod() != null) {

                // Replace %TRIGGER_ASSETS% with the triggered assets in JSON format.
                if (!TextUtil.isNullOrEmpty(webhook.getPayload()) && webhook.getPayload().contains(PLACEHOLDER_TRIGGER_ASSETS)) {
                    String triggeredAssetInfo = buildTriggeredAssetInfo(useUnmatched, ruleState, false, true);
                    webhook.setPayload(webhook.getPayload()
                            .replace(('"' + PLACEHOLDER_TRIGGER_ASSETS + '"'), triggeredAssetInfo)
                            .replace(PLACEHOLDER_TRIGGER_ASSETS, triggeredAssetInfo)
                    );
                }

                if (webhookAction.mediaType == null) {
                    Optional<Map.Entry<String, List<String>>> contentTypeHeader = webhook.getHeaders().entrySet().stream().filter((entry) -> entry.getKey().equalsIgnoreCase("content-type")).findFirst();
                    String contentType = contentTypeHeader.isPresent() ? contentTypeHeader.get().getValue().get(0) : MediaType.APPLICATION_JSON;
                    webhookAction.mediaType = MediaType.valueOf(contentType);
                }

                if(webhookAction.target == null) {
                    webhookAction.target = webhooksFacade.buildTarget(webhook);
                }

                return new RuleActionExecution(() -> webhooksFacade.send(webhook, webhookAction.mediaType, webhookAction.target), 0);
            }
        }

        if (ruleAction instanceof RuleActionWriteAttribute attributeAction) {

            if (targetIsNotAssets(ruleAction.target)) {
                return null;
            }

            if (TextUtil.isNullOrEmpty(attributeAction.attributeName)) {
                log(Level.WARNING, "Attribute name is missing for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            Collection<String> ids = getRuleActionTargetIds(ruleAction.target, useUnmatched, ruleState, assetsFacade, usersFacade, facts);

            if (ids == null || ids.isEmpty()) {
                log(Level.INFO, "No targets for write attribute rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            log(Level.FINE, "Writing attribute '" + attributeAction.attributeName + "' for " + ids.size() + " asset(s) for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
            return new RuleActionExecution(() ->
                    ids.forEach(id ->
                            assetsFacade.dispatch(id, attributeAction.attributeName, attributeAction.value)), 0);
        }

        if (ruleAction instanceof RuleActionWait) {
            long millis = ((RuleActionWait) ruleAction).millis;
            if (millis > 0) {
                return new RuleActionExecution(null, millis);
            }
            log(Level.FINEST, "Invalid delay for wait rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
        }

        if (ruleAction instanceof RuleActionUpdateAttribute attributeUpdateAction) {

            if (targetIsNotAssets(ruleAction.target)) {
                log(Level.FINEST, "Invalid target update attribute rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            if (TextUtil.isNullOrEmpty(attributeUpdateAction.attributeName)) {
                log(Level.WARNING, "Attribute name is missing for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            List<String> matchingAssetIds;

            if (ruleAction.target == null || ruleAction.target.assets == null) {
                if (targetIsNotAssets(ruleAction.target)) {
                    throw new IllegalStateException("Cannot use action type '" + RuleActionUpdateAttribute.class.getSimpleName() + "' with user target");
                }
                matchingAssetIds = new ArrayList<>(getRuleActionTargetIds(ruleAction.target, useUnmatched, ruleState, assetsFacade, usersFacade, facts));
            } else {
                matchingAssetIds = facts
                    .matchAssetState(ruleAction.target.assets)
                    .map(AssetState::getId)
                    .distinct()
                    .collect(Collectors.toList());
            }

            if (matchingAssetIds.isEmpty()) {
                log(Level.INFO, "No targets for update attribute rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            // Look for the current value within the asset state facts (asset/attribute has to be in scope of this rule engine and have a rule state meta item)
            List<AssetState<?>> matchingAssetStates = matchingAssetIds
                .stream()
                .map(assetId ->
                        facts.getAssetStates()
                                .stream()
                                .filter(state -> state.getId().equals(assetId) && state.getName().equals(attributeUpdateAction.attributeName))
                                .findFirst().orElseGet(() -> {
                                    log(Level.WARNING, "Failed to find attribute in rule states for attribute update: " + new AttributeRef(assetId, attributeUpdateAction.attributeName));
                                    return null;
                        }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (matchingAssetStates.isEmpty()) {
                log(Level.WARNING, "No asset states matched to apply update attribute action to");
                return null;
            }

            return new RuleActionExecution(() ->

                matchingAssetStates.forEach(assetState -> {
                    Object value = assetState.getValue().orElse(null);
                    Class<?> valueType = assetState.getType().getType();
                    boolean isArray = ValueUtil.isArray(valueType);

                    if (!isArray && !ValueUtil.isObject(valueType)) {
                        log(Level.WARNING, "Rule action target asset cannot determine value type or incompatible value type for attribute: " + assetState);
                    } else {

                        // Convert value to JSON Node to easily manipulate it
                        value = isArray ? ValueUtil.convert(value, ArrayNode.class) : ValueUtil.convert(value, ObjectNode.class);

                        switch (attributeUpdateAction.updateAction) {
                            case ADD:
                                if (isArray) {
                                    value = value == null ? ValueUtil.JSON.createArrayNode() : value;
                                    ((ArrayNode)value).add(ValueUtil.convert(attributeUpdateAction.value, JsonNode.class));
                                } else {
                                    value = value == null ? ValueUtil.JSON.createObjectNode() : value;
                                    ((ObjectNode) value).set(attributeUpdateAction.key, ValueUtil.convert(attributeUpdateAction.value, JsonNode.class));
                                }
                                break;
                            case ADD_OR_REPLACE:
                            case REPLACE:
                                if (isArray) {
                                    value = value == null ? ValueUtil.JSON.createArrayNode() : value;
                                    ArrayNode arrayValue = (ArrayNode) value;

                                    if (attributeUpdateAction.index != null && arrayValue.size() >= attributeUpdateAction.index) {
                                        arrayValue.set(attributeUpdateAction.index, ValueUtil.convert(attributeUpdateAction.value, JsonNode.class));
                                    } else {
                                        arrayValue.add(ValueUtil.convert(attributeUpdateAction.value, JsonNode.class));
                                    }
                                } else {
                                    value = value == null ? ValueUtil.JSON.createObjectNode() : value;
                                    if (!TextUtil.isNullOrEmpty(attributeUpdateAction.key)) {
                                        ((ObjectNode) value).set(attributeUpdateAction.key, ValueUtil.convert(attributeUpdateAction.value, JsonNode.class));
                                    } else {
                                        log(Level.WARNING, "JSON Rule: Rule action missing required 'key': " + ValueUtil.asJSON(attributeUpdateAction));
                                    }
                                }
                                break;
                            case DELETE:
                                if(value != null) {
                                    if (isArray) {
                                        ((ArrayNode) value).remove(attributeUpdateAction.index);
                                    } else {
                                        ((ObjectNode) value).remove(attributeUpdateAction.key);
                                    }
                                }
                                break;
                            case CLEAR:
                                if (isArray) {
                                    value = ValueUtil.JSON.createArrayNode();
                                } else {
                                    value = ValueUtil.JSON.createObjectNode();
                                }
                                break;
                        }

                        log(Level.FINE, "Updating attribute for rule action: " + rule.name + " '" + actionsName + "' action index " + index + ": " + assetState);
                        assetsFacade.dispatch(assetState.getId(), attributeUpdateAction.attributeName, value);
                    }
                }),
            0);
        }

        log(Level.FINE, "Unsupported rule action: " + rule.name + " '" + actionsName + "' action index " + index);
        return null;
    }

    private static String buildTriggeredAssetInfo(boolean useUnmatched, RuleState ruleEvaluationResult, boolean isHtml, boolean isJson) {

        Set<String> assetIds = useUnmatched ? ruleEvaluationResult.otherwiseMatchedAssetIds : ruleEvaluationResult.thenMatchedAssetIds;

        if (assetIds == null || assetIds.isEmpty()) {
            return "";
        }

        // Extract asset states for matched asset IDs
        Map<String, Set<AssetState<?>>> assetStates = ruleEvaluationResult.conditionStateMap.values().stream()
            .filter(conditionState -> conditionState.lastEvaluationResult.matches)
            .flatMap(conditionState -> {
                Collection<AssetState<?>> as = useUnmatched
                    ? conditionState.lastEvaluationResult.unmatchedAssetStates
                    : conditionState.lastEvaluationResult.matchedAssetStates;
                return as.stream();
            })
            .filter(assetState -> assetIds.contains(assetState.getId()))
            .collect(Collectors.groupingBy(AssetState::getId, Collectors.toSet()));

        StringBuilder sb = new StringBuilder();
        if (isHtml) {
            sb.append("<table cellpadding=\"30\">");
            sb.append("<tr><th>Asset ID</th><th>Asset Name</th><th>Attribute</th><th>Value</th></tr>");
            assetStates.forEach((key, value) -> value.forEach(assetState -> {
                sb.append("<tr><td>");
                sb.append(assetState.getId());
                sb.append("</td><td>");
                sb.append(assetState.getAssetName());
                sb.append("</td><td>");
                sb.append(assetState.getName());
                sb.append("</td><td>");
                sb.append(assetState.getValue().flatMap(ValueUtil::asJSON).orElse(""));
                sb.append("</td></tr>");
            }));
            sb.append("</table>");
        } else if (isJson) {
            try {
                return ValueUtil.JSON.writeValueAsString(assetStates);
            } catch (Exception e) {
                LOG.warning(e.getMessage());
            }
        } else {
            sb.append("Asset ID\t\tAsset Name\t\tAttribute\t\tValue");
            assetStates.forEach((key, value) -> value.forEach(assetState -> {
                sb.append(assetState.getId());
                sb.append("\t\t");
                sb.append(assetState.getAssetName());
                sb.append("\t\t");
                sb.append(assetState.getName());
                sb.append("\t\t");
                sb.append(assetState.getValue().map(v -> ValueUtil.convert(v, String.class)).orElse(""));
            }));
        }

        return sb.toString();
    }

    protected static Collection<String> getRuleActionTargetIds(RuleActionTarget target, boolean useUnmatched, RuleState ruleState, Assets assetsFacade, Users usersFacade, RulesFacts facts) {

        Map<String, RuleConditionState> conditionStateMap = ruleState.conditionStateMap;

        if (target != null) {
            if (!TextUtil.isNullOrEmpty(target.conditionAssets) && conditionStateMap != null) {
                RuleConditionState triggerState = conditionStateMap.get(target.conditionAssets);
                if (!useUnmatched) {
                    return triggerState != null ? triggerState.getMatchedAssetIds() : Collections.emptyList();
                }

                return triggerState != null ? triggerState.getUnmatchedAssetIds() : Collections.emptyList();
            }

            if (conditionStateMap != null && (target.matchedAssets != null || (target.linkedUsers != null && target.linkedUsers))) {
                List<String> compareAssetIds = conditionStateMap.values().stream()
                    .flatMap(triggerState ->
                        useUnmatched ? triggerState.getUnmatchedAssetIds().stream() : triggerState.getMatchedAssetIds().stream()).toList();

                if (target.matchedAssets != null) {
                    return facts.matchAssetState(target.matchedAssets)
                        .map(AssetState::getId)
                        .distinct()
                        .filter(compareAssetIds::contains)
                        .collect(Collectors.toList());
                }

                // Find linked users - apply any user query if it is present or create a new one
                UserQuery userQuery = target.users != null ? target.users : new UserQuery();
                if (userQuery.assets == null) {
                    userQuery.assets = compareAssetIds.toArray(String[]::new);
                } else {
                    List<String> assetIds = new ArrayList<>(Arrays.asList(userQuery.assets));
                    assetIds.addAll(compareAssetIds);
                    userQuery.assets = assetIds.toArray(String[]::new);
                }

                return usersFacade.getResults(userQuery).collect(Collectors.toList());
            }

            if (target.assets != null) {
                return getAssetIds(assetsFacade, target.assets);
            }

            if (target.users != null) {
                return getUserIds(usersFacade, target.users);
            }

            if (target.custom != null) {
                return Collections.singleton(target.custom);
            }
        }

        if (conditionStateMap != null) {
            if (!useUnmatched) {
                return conditionStateMap.values().stream().flatMap(triggerState ->
                        triggerState != null ? triggerState.getMatchedAssetIds().stream() : Stream.empty()
                ).distinct().collect(Collectors.toList());
            }

            return conditionStateMap.values().stream().flatMap(triggerState ->
                    triggerState != null ? triggerState.getUnmatchedAssetIds().stream() : Stream.empty()
            ).distinct().collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    protected static boolean targetIsNotAssets(RuleActionTarget target) {
        return target != null && (target.users != null || (target.linkedUsers != null && target.linkedUsers));
    }

    protected void log(Level level, String message) {
        RulesEngine.RULES_LOG.log(level, LOG_PREFIX + jsonRuleset.getName() + "': " + message);
    }

    protected void log(Level level, String message, Throwable t) {
        RulesEngine.RULES_LOG.log(level, LOG_PREFIX + jsonRuleset.getName() + "': " + message, t);
    }

    protected static SunTimes.Parameters getSunCalculator(Ruleset ruleset, SunPositionTrigger sunPositionTrigger, TimerService timerService) throws IllegalStateException {
        SunPositionTrigger.Position position = sunPositionTrigger.getPosition();
        GeoJSONPoint location = sunPositionTrigger.getLocation();

        if (position == null) {
            throw new IllegalStateException(LOG_PREFIX + ruleset.getName() + "': Rule condition sun requires a position value");
        }
        if (location == null) {
            throw new IllegalStateException(LOG_PREFIX + ruleset.getName() + "': Rule condition sun requires a location value");
        }

        SunTimes.Twilight twilight = null;
        if (position.name().startsWith(SunPositionTrigger.TWILIGHT_PREFIX)) {
            String lookupValue = position.name().replace(SunPositionTrigger.MORNING_TWILIGHT_PREFIX, "").replace(SunPositionTrigger.EVENING_TWILIGHT_PREFIX, "").replace(SunPositionTrigger.TWILIGHT_PREFIX, "");
            twilight = EnumUtil.enumFromString(SunTimes.Twilight.class, lookupValue).orElseThrow(() -> {
                throw new IllegalStateException(LOG_PREFIX + ruleset.getName() + "': Rule condition un-supported twilight position value '" + lookupValue + "'");
            });
        }

        SunTimes.Parameters sunCalculator = SunTimes.compute()
                .on(timerService.getNow())
                .utc()
                .at(location.getX(), location.getY());

        if (twilight != null) {
            sunCalculator.twilight(twilight);
        }

        return sunCalculator;
    }
}
