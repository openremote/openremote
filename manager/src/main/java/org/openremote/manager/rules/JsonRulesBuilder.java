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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.rules.facade.NotificationsFacade;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.notification.EmailNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.Users;
import org.openremote.model.rules.json.*;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.value.*;
import org.quartz.CronExpression;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.container.Container.JSON;
import static org.openremote.container.Container.LOG;
import static org.openremote.container.util.Util.distinctByKey;
import static org.openremote.manager.rules.AssetQueryPredicate.groupIsEmpty;
import static org.openremote.model.query.filter.LocationAttributePredicate.getLocationPredicates;

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
    static class RuleConditionState {

        RuleCondition ruleCondition;
        final TimerService timerService;
        boolean trackUnmatched;
        AssetQuery.OrderBy orderBy;
        int limit;
        LogicGroup<AttributePredicate> attributePredicates = null;
        Predicate<AssetState> assetStatePredicate = null;
        Set<AssetState> unfilteredAssetStates = new HashSet<>();
        Set<AssetState> previouslyMatchedAssetStates = new HashSet<>();
        Set<AssetState> previouslyUnmatchedAssetStates;
        Predicate<Long> timePredicate;
        RuleConditionEvaluationResult lastEvaluationResult;

        public RuleConditionState(RuleCondition ruleCondition, boolean trackUnmatched, TimerService timerService) throws Exception {
            this.timerService = timerService;
            this.ruleCondition = ruleCondition;
            this.trackUnmatched = trackUnmatched;

            if (trackUnmatched) {
                previouslyUnmatchedAssetStates = new HashSet<>();
            }

            if (!TextUtil.isNullOrEmpty(ruleCondition.timer)) {

                try {
                    if (TimeUtil.isTimeDuration(ruleCondition.timer)) {

                        final long duration = TimeUtil.parseTimeDuration(ruleCondition.timer);
                        AtomicLong nextExecuteMillis = new AtomicLong(timerService.getCurrentTimeMillis());

                        timePredicate = (time) -> {
                            long nextExecute = nextExecuteMillis.get();
                            if (time >= nextExecute) {
                                nextExecuteMillis.set(nextExecute + duration);
                                return true;
                            }
                            return false;
                        };
                    }

                    if (CronExpression.isValidExpression(ruleCondition.timer)) {

                        CronExpression timerExpression = new CronExpression(ruleCondition.timer);
                        AtomicLong nextExecuteMillis = new AtomicLong(timerExpression.getNextValidTimeAfter(new Date(timerService.getCurrentTimeMillis())).getTime());

                        timePredicate = (time) -> {
                            long nextExecute = nextExecuteMillis.get();
                            if (time >= nextExecute) {
                                nextExecuteMillis.set(timerExpression.getNextInvalidTimeAfter(timerExpression.getNextInvalidTimeAfter(new Date(nextExecute))).getTime());
                                return true;
                            }
                            return false;
                        };
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "Failed to parse rule condition timer expression: " + ruleCondition.timer, e);
                    throw e;
                }
            } else if (ruleCondition.assets != null) {

                // Pull out order, limit and attribute predicates so they can be applied at required times
                orderBy = ruleCondition.assets.orderBy;
                limit = ruleCondition.assets.limit;
                attributePredicates = ruleCondition.assets.attributes;

                if (attributePredicates != null && attributePredicates.items != null) {
                    // Only supports a single level or logic group for attributes (i.e. cannot nest groups in the UI so
                    // don't support it here either)
                    attributePredicates.groups = null;
                    assetStatePredicate = AssetQueryPredicate.asPredicate(timerService::getCurrentTimeMillis, attributePredicates);
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
                            unfilteredAssetStates.remove(event.assetState);
                            unfilteredAssetStates.add(event.assetState);
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

            List<AssetState> matchedAssetStates;
            List<AssetState> unmatchedAssetStates = Collections.emptyList();
            Collection<String> unmatchedAssetIds = Collections.emptyList();

            if (attributePredicates == null) {
                matchedAssetStates = new ArrayList<>(unfilteredAssetStates);
            } else {

                Map<Boolean, List<AssetState>> results;
                boolean isAndGroup = attributePredicates.operator == null || attributePredicates.operator == LogicGroup.Operator.AND;

                if (isAndGroup) {

                    // ANDs need to be applied in the context of an entire asset as don't make any sense otherwise
                    results = new HashMap<>();
                    ArrayList<AssetState> matched = new ArrayList<>();
                    ArrayList<AssetState> unmatched = new ArrayList<>();
                    results.put(true, matched);
                    results.put(false, unmatched);

                    unfilteredAssetStates.stream().collect(Collectors.groupingBy(AssetState::getId)).forEach((id, states) -> {

                        Map<Boolean, List<AssetState>> assetResults = states.stream().collect(Collectors.groupingBy(assetStatePredicate::test));
                        matched.addAll(assetResults.getOrDefault(true, Collections.emptyList()));
                        unmatched.addAll(assetResults.getOrDefault(false, Collections.emptyList()));
                    });

                } else {

                    results = unfilteredAssetStates.stream().collect(Collectors.groupingBy(assetStatePredicate::test));

                }

                matchedAssetStates = results.getOrDefault(true, Collections.emptyList());
                unmatchedAssetStates = results.getOrDefault(false, Collections.emptyList());

                if (trackUnmatched) {

                    // Clear out previous unmatched that now match
                    previouslyUnmatchedAssetStates.removeIf(matchedAssetStates::contains);

                    // Filter out previous un-matches to avoid re-triggering
                    unmatchedAssetStates.removeIf(previouslyUnmatchedAssetStates::contains);

                }
            }

            // Remove matches that have an active recurrence timer
            matchedAssetStates.removeIf(matchedAssetState -> nextRecurAssetIdMap.containsKey(matchedAssetState.getId())
                && nextRecurAssetIdMap.get(matchedAssetState.getId()) > timerService.getCurrentTimeMillis());

            // Remove previous matches where the asset state no longer matches or the value has changed (depending on reset option)
            previouslyMatchedAssetStates.removeIf(previousAssetState -> {

                int matchIndex = matchedAssetStates.indexOf(previousAssetState);
                boolean valueChangedOrNoLongerMatches = matchIndex < 0;

                // If reset on value change and still matches check to see if the value has changed
                if (!valueChangedOrNoLongerMatches && ruleCondition.resetOnValueChange) {
                    valueChangedOrNoLongerMatches = !Objects.equals(previousAssetState.getValue().orElse(null), matchedAssetStates.get(matchIndex).getValue().orElse(null));
                }

                if (valueChangedOrNoLongerMatches) {
                    log(Level.FINER, "Rule trigger previously matched asset state value has changed or no longer matches so resetting: " + previousAssetState);
                }

                return valueChangedOrNoLongerMatches;
            });

            // Filter out previous matches to avoid re-triggering
            matchedAssetStates.removeIf(previouslyMatchedAssetStates::contains);

            // Select unique asset states based on asset id
            Stream<AssetState> matchedAssetStateStream = matchedAssetStates.stream().filter(distinctByKey(AssetState::getId));

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
                Stream<AssetState> unmatchedAssetStateStream = unmatchedAssetStates.stream().filter(distinctByKey(AssetState::getId));

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
        Collection<AssetState> matchedAssetStates;
        Collection<AssetState> unmatchedAssetStates;
        Collection<String> matchedAssetIds;
        Collection<String> unmatchedAssetIds;

        public RuleConditionEvaluationResult(boolean matches, Collection<AssetState> matchedAssetStates, Collection<String> matchedAssetIds, Collection<AssetState> unmatchedAssetStates, Collection<String> unmatchedAssetIds) {
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
    static class RuleState {

        protected Map<String, RuleConditionState> conditionStateMap;
        protected List<String> thenMatchedAssetIds = new ArrayList<>();
        protected List<String> otherwiseMatchedAssetIds = new ArrayList<>();
        protected long nextRecur;
        protected Map<String, Long> nextRecurAssetIdMap = new HashMap<>();

        public RuleState(Map<String, RuleConditionState> conditionStateMap) {
            this.conditionStateMap = conditionStateMap;
        }

        public boolean thenMatched() {
            return thenMatchedAssetIds != null && !thenMatchedAssetIds.isEmpty();
        }

        public boolean otherwiseMatched() {
            return otherwiseMatchedAssetIds != null && !otherwiseMatchedAssetIds.isEmpty();
        }
    }

    public static final String PLACEHOLDER_RULESET_ID = "%RULESET_ID%";
    public static final String PLACEHOLDER_RULESET_NAME = "%RULESET_NAME%";
    public static final String PLACEHOLDER_TRIGGER_ASSETS = "%TRIGGER_ASSETS%";
    final static String LOG_PREFIX = "JSON Rules: ";
    final protected AssetStorageService assetStorageService;
    final protected TimerService timerService;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected NotificationsFacade<?> notificationsFacade;
    final protected ManagerExecutorService executorService;
    final protected BiConsumer<Runnable, Long> scheduledActionConsumer;
    final protected Map<String, RuleState> ruleEvaluationMap = new HashMap<>();
    final protected JsonRule[] jsonRules;

    public JsonRulesBuilder(Ruleset ruleset, TimerService timerService, AssetStorageService assetStorageService, ManagerExecutorService executorService, Assets assetsFacade, Users usersFacade, NotificationsFacade<?> notificationsFacade, BiConsumer<Runnable, Long> scheduledActionConsumer) throws Exception {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.executorService = executorService;
        this.assetsFacade = assetsFacade;
        this.usersFacade = usersFacade;
        this.notificationsFacade = notificationsFacade;
        this.scheduledActionConsumer = scheduledActionConsumer;

        String rulesStr = ruleset.getRules();
        rulesStr = rulesStr.replace(PLACEHOLDER_RULESET_ID, Long.toString(ruleset.getId()));
        rulesStr = rulesStr.replace(PLACEHOLDER_RULESET_NAME, ruleset.getName());

        JsonRulesetDefinition jsonRulesetDefinition = JSON.readValue(rulesStr, JsonRulesetDefinition.class);

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
            executeRuleActions(jsonRule, jsonRule.onStop, "onStop", false, facts, null, assetsFacade, usersFacade, notificationsFacade, this.scheduledActionConsumer));
    }

    public void start(RulesFacts facts) {
        Arrays.stream(jsonRules).forEach(jsonRule ->
            executeRuleActions(jsonRule, jsonRule.onStart, "onStart", false, facts, null, assetsFacade, usersFacade, notificationsFacade, this.scheduledActionConsumer));

        // Initialise asset states
        onAssetStatesChanged(facts, null);
    }

    public void onAssetStatesChanged(RulesFacts facts, RulesEngine.AssetStateChangeEvent event) {
        ruleEvaluationMap.values().forEach(triggerStateMap -> triggerStateMap.conditionStateMap.values().forEach(ruleConditionState -> ruleConditionState.updateUnfilteredAssetStates(facts, event)));
    }

    protected JsonRulesBuilder add(JsonRule rule) throws Exception {

        if (ruleEvaluationMap.containsKey(rule.name)) {
            throw new IllegalArgumentException("Rules must have a unique name within a ruleset, rule name '" + rule.name + "' already used");
        }

        RuleState ruleEvaluationResult = new RuleState(new HashMap<>());
        ruleEvaluationMap.put(rule.name, ruleEvaluationResult);
        addRuleConditionStates(rule.when, rule.otherwise != null, 0, ruleEvaluationResult.conditionStateMap);

        Condition condition = buildLhsCondition(rule, ruleEvaluationResult);
        Action action = buildRhsAction(rule, ruleEvaluationResult);

        if (condition == null || action == null) {
            throw new IllegalArgumentException("Error building JSON rule when or then is not defined: " + rule.name);
        }

        add().name(rule.name)
                .description(rule.description)
                .priority(rule.priority)
                .when(facts -> {
                    Object result;
                    try {
                        result = condition.evaluate(facts);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error evaluating condition of rule '" + rule.name + "': " + ex.getMessage(), ex);
                    }
                    if (result instanceof Boolean) {
                        return result;
                    } else {
                        throw new IllegalArgumentException("Error evaluating condition of rule '" + rule.name + "': result is not boolean but " + result);
                    }
                })
                .then(action);

        return this;
    }

    protected void addRuleConditionStates(LogicGroup<RuleCondition> ruleConditionGroup, boolean trackUnmatched, int index, Map<String, RuleConditionState> triggerStateMap) throws Exception {
        if (ruleConditionGroup != null) {
            if (ruleConditionGroup.getItems().size() > 0) {
                for (RuleCondition ruleCondition : ruleConditionGroup.getItems()) {
                    if (TextUtil.isNullOrEmpty(ruleCondition.tag)) {
                        ruleCondition.tag = Integer.toString(index);
                    }
                    triggerStateMap.put(ruleCondition.tag, new RuleConditionState(ruleCondition, trackUnmatched, timerService));
                    index++;
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

            // Check if next recurrence in the future
            if (ruleState.nextRecur > timerService.getCurrentTimeMillis()) {
                return false;
            }

            // Clear out expired recurrence timers
            ruleState.nextRecurAssetIdMap.entrySet().removeIf(entry -> entry.getValue() <= timerService.getCurrentTimeMillis());

            // Update each condition state
            log(Level.FINEST, "Updating rule condition states for rule: " + rule.name);
            ruleState.conditionStateMap.values().forEach(ruleConditionState -> ruleConditionState.update(ruleState.nextRecurAssetIdMap));

            // Apply group operator and extract resulting matched asset IDs
            Pair<List<String>, List<String>> matchedUnmatchedGroupResults = getGroupLastTriggerResults(rule.when, ruleState.conditionStateMap, rule.otherwise != null);
            ruleState.thenMatchedAssetIds = matchedUnmatchedGroupResults.key;
            ruleState.otherwiseMatchedAssetIds = matchedUnmatchedGroupResults.value;

            return ruleState.thenMatched() || ruleState.otherwiseMatched();
        };
    }

    protected Action buildRhsAction(JsonRule rule, RuleState ruleState) {

        if (rule.then == null) {
            return null;
        }

        return facts -> {

            try {
                log(Level.FINER, "Triggered rule so executing 'then' actions for rule: " + rule.name);
                if (ruleState.thenMatched()) {
                    executeRuleActions(rule, rule.then, "then", false, facts, ruleState, assetsFacade, usersFacade, notificationsFacade, scheduledActionConsumer);
                }

                if (rule.otherwise != null && ruleState.otherwiseMatched()) {
                    log(Level.FINER, "Triggered rule so executing 'otherwise' actions for rule: " + rule.name);
                    executeRuleActions(rule, rule.otherwise, "otherwise", true, facts, ruleState, assetsFacade, usersFacade, notificationsFacade, scheduledActionConsumer);
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

                // Store last evaluation results in state
                ruleState.conditionStateMap.values().forEach(ruleConditionState -> {
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
                });

                // Clear last results
                ruleState.conditionStateMap.values().forEach(triggerState -> triggerState.lastEvaluationResult = null);
            }
        };
    }

    public static void executeRuleActions(JsonRule rule, RuleAction[] ruleActions, String actionsName, boolean useUnmatched, RulesFacts facts, RuleState ruleState, Assets assetsFacade, Users usersFacade, NotificationsFacade<?> notificationsFacade, BiConsumer<Runnable, Long> scheduledActionConsumer) {

        if (ruleActions != null && ruleActions.length > 0) {

            long delay = 0L;

            for (int i=0; i<ruleActions.length; i++) {

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
                        notificationsFacade
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

    protected static RuleActionExecution buildRuleActionExecution(JsonRule rule, RuleAction ruleAction, String actionsName, int index, boolean useUnmatched, RulesFacts facts, RuleState ruleState, Assets assetsFacade, Users usersFacade, NotificationsFacade<?> notificationsFacade) {

        if (ruleAction instanceof RuleActionNotification) {
            RuleActionNotification notificationAction = (RuleActionNotification) ruleAction;

            if (notificationAction.notification != null) {

                Notification notification = notificationAction.notification;

                if (notification.getMessage() != null && Objects.equals(notification.getMessage().getType(), EmailNotificationMessage.TYPE)) {
                    EmailNotificationMessage email = (EmailNotificationMessage) notification.getMessage();

                    boolean hasBody = !TextUtil.isNullOrEmpty(email.getHtml()) || !TextUtil.isNullOrEmpty(email.getText());
                    boolean isHtml = !TextUtil.isNullOrEmpty(email.getHtml());

                    if (hasBody) {
                        String body = isHtml ? email.getHtml() : email.getText();

                        if (body.contains(PLACEHOLDER_TRIGGER_ASSETS)) {

                            // Need to clone the notification
                            try {
                                notification = JSON.readValue(JSON.writeValueAsString(notification), Notification.class);
                                email = (EmailNotificationMessage) notification.getMessage();
                                String triggeredAssetInfo = buildTriggeredAssetInfo(useUnmatched, ruleState, isHtml);
                                body = body.replace(PLACEHOLDER_TRIGGER_ASSETS, triggeredAssetInfo);
                                if (isHtml) {
                                    email.setHtml(body);
                                } else {
                                    email.setText(body);
                                }
                            } catch (JsonProcessingException e) {
                                LOG.warning("Failed to clone notification so cannot insert asset info");
                            }
                        }
                    }
                }

                // Override the notification targets if set in the rule
                Notification.TargetType targetType = targetIsNotAssets(ruleAction.target) ? Notification.TargetType.USER : Notification.TargetType.ASSET;
                Collection<String> ids = getRuleActionTargetIds(ruleAction.target, useUnmatched, ruleState, assetsFacade, usersFacade, facts);

                if (ids != null && !ids.isEmpty()) {
                    notification.setTargets(ids.stream().map(id -> new Notification.Target(targetType, id)).collect(Collectors.toList()));
                }

                log(Level.FINE, "Sending notification for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                Notification finalNotification = notification;
                return new RuleActionExecution(() -> notificationsFacade.send(finalNotification), 0);
            }
        }

        if (ruleAction instanceof RuleActionWriteAttribute) {

            if (targetIsNotAssets(ruleAction.target)) {
                return null;
            }

            RuleActionWriteAttribute attributeAction = (RuleActionWriteAttribute) ruleAction;

            if (TextUtil.isNullOrEmpty(attributeAction.attributeName)) {
                log(Level.WARNING, "Attribute name is missing for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            Collection<String> ids = getRuleActionTargetIds(ruleAction.target, useUnmatched, ruleState, assetsFacade, usersFacade, facts);

            if (ids == null || ids.isEmpty()) {
                log(Level.FINEST, "No targets for write attribute rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
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

        if (ruleAction instanceof RuleActionUpdateAttribute) {

            if (targetIsNotAssets(ruleAction.target)) {
                log(Level.FINEST, "Invalid target update attribute rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            RuleActionUpdateAttribute attributeUpdateAction = (RuleActionUpdateAttribute) ruleAction;

            if (TextUtil.isNullOrEmpty(attributeUpdateAction.attributeName)) {
                log(Level.WARNING, "Attribute name is missing for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                return null;
            }

            List<String> matchingAssetIds;

            if (ruleAction.target == null || ruleAction.target.assets == null) {
                if (ruleAction.target != null && ruleAction.target.users != null) {
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
                log(Level.WARNING, "No assets matched to apply update attribute action to");
                return null;
            }

            // Look for the current value within the asset state facts (asset/attribute has to be in scope of this rule engine and have a rule state meta item)
            List<AssetState> matchingAssetStates = matchingAssetIds
                .stream()
                .map(assetId ->
                        facts.getAssetStates()
                                .stream()
                                .filter(state -> state.getId().equals(assetId) && state.getAttributeName().equals(attributeUpdateAction.attributeName))
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
                    ValueType valueType = assetState.getValue().map(Value::getType).orElseGet(() -> assetState.getAttributeValueType() != null ? assetState.getAttributeValueType().getValueType() : null);
                    Value value = assetState.getValue().orElse(null);

                    if (!(valueType == ValueType.ARRAY || valueType == ValueType.OBJECT)) {
                        log(Level.WARNING, "Rule action target asset cannot determine value type or incompatible value type for attribute: " + assetState);
                    } else {

                        switch (attributeUpdateAction.updateAction) {
                            case ADD:
                                if (valueType.equals(ValueType.ARRAY)) {
                                    value = value == null ? Values.createArray() : value;
                                    ((ArrayValue) value).add(attributeUpdateAction.value);
                                } else {
                                    value = value == null ? Values.createObject() : value;
                                    ((ObjectValue) value).put(attributeUpdateAction.key, attributeUpdateAction.value);
                                }
                                break;
                            case ADD_OR_REPLACE:
                            case REPLACE:
                                if (valueType.equals(ValueType.ARRAY)) {
                                    value = value == null ? Values.createArray() : value;
                                    ArrayValue arrayValue = (ArrayValue) value;

                                    if (attributeUpdateAction.index != null && arrayValue.length() >= attributeUpdateAction.index) {
                                        arrayValue.set(attributeUpdateAction.index, attributeUpdateAction.value);
                                    } else {
                                        arrayValue.add(attributeUpdateAction.value);
                                    }
                                } else {
                                    value = value == null ? Values.createObject() : value;
                                    if (!TextUtil.isNullOrEmpty(attributeUpdateAction.key)) {
                                        ((ObjectValue) value).put(attributeUpdateAction.key, attributeUpdateAction.value);
                                    } else {
                                        try {
                                            log(Level.WARNING, "JSON Rule: Rule action missing required 'key': " + JSON.writeValueAsString(attributeUpdateAction));
                                        } catch (JsonProcessingException ignored) {
                                        }
                                    }
                                }
                                break;
                            case DELETE:
                                if(value != null) {
                                    if (valueType.equals(ValueType.ARRAY)) {
                                        ((ArrayValue) value).remove(attributeUpdateAction.index);
                                    } else {
                                        ((ObjectValue) value).remove(attributeUpdateAction.key);
                                    }
                                }
                                break;
                            case CLEAR:
                                if (valueType.equals(ValueType.ARRAY)) {
                                    value = Values.createArray();
                                } else {
                                    value = Values.createObject();
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

    private static String buildTriggeredAssetInfo(boolean useUnmatched, RuleState ruleEvaluationResult, boolean isHtml) {

        List<String> assetIds = useUnmatched ? ruleEvaluationResult.otherwiseMatchedAssetIds : ruleEvaluationResult.thenMatchedAssetIds;

        if (assetIds == null || assetIds.isEmpty()) {
            return "";
        }

        // Extract asset states for matched asset IDs
        Map<String, Set<AssetState>> assetStates = ruleEvaluationResult.conditionStateMap.values().stream()
            .filter(conditionState -> conditionState.lastEvaluationResult.matches)
            .flatMap(conditionState -> {
                Collection<AssetState> as = useUnmatched
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
                sb.append(assetState.getName());
                sb.append("</td><td>");
                sb.append(assetState.getAttributeName());
                sb.append("</td><td>");
                sb.append(assetState.getValue().map(Value::toString).orElse(""));
                sb.append("</td></tr>");
            }));
            sb.append("</table>");
        } else {
            sb.append("Asset ID\t\tAsset Name\t\tAttribute\t\tValue");
            assetStates.forEach((key, value) -> value.forEach(assetState -> {
                sb.append(assetState.getId());
                sb.append("\t\t");
                sb.append(assetState.getName());
                sb.append("\t\t");
                sb.append(assetState.getAttributeName());
                sb.append("\t\t");
                sb.append(assetState.getValue().map(Value::toString).orElse(""));
            }));
        }

        return sb.toString();
    }

    protected static Collection<String> getRuleActionTargetIds(RuleActionTarget target, boolean useUnmatched, RuleState ruleState, Assets assetsFacade, Users usersFacade, RulesFacts facts) {

        Map<String, RuleConditionState> conditionStateMap = ruleState.conditionStateMap;

        if (target != null) {
            if (!TextUtil.isNullOrEmpty(target.ruleConditionTag) && conditionStateMap != null) {
                RuleConditionState triggerState = conditionStateMap.get(target.ruleConditionTag);
                if (!useUnmatched) {
                    return triggerState != null ? triggerState.getMatchedAssetIds() : Collections.emptyList();
                }

                return triggerState != null ? triggerState.getUnmatchedAssetIds() : Collections.emptyList();
            }

            if (conditionStateMap != null && target.matchedAssets != null) {
                List<String> compareAssetIds = conditionStateMap.values().stream()
                    .flatMap(triggerState ->
                        useUnmatched ? triggerState.getUnmatchedAssetIds().stream() : triggerState.getMatchedAssetIds().stream())
                    .collect(Collectors.toList());

                return facts.matchAssetState(target.matchedAssets)
                    .map(AssetState::getId)
                    .distinct()
                    .filter(matchedAssetId -> compareAssetIds.indexOf(matchedAssetId) >= 0)
                    .collect(Collectors.toList());
            }

            if (target.assets != null) {
                return getAssetIds(assetsFacade, target.assets);
            }

            if (target.users != null) {
                return getUserIds(usersFacade, target.users);
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

    protected static Pair<List<String>, List<String>> getGroupLastTriggerResults(LogicGroup<RuleCondition> ruleConditionGroup, Map<String, RuleConditionState> conditionStateMap, boolean trackUnmatched) {

        if (groupIsEmpty(ruleConditionGroup)) {
            return new Pair<>(Collections.emptyList(), Collections.emptyList());
        }

        List<String> matchedAssetIds = new ArrayList<>();
        List<String> unmatchedAssetIds = new ArrayList<>();
        LogicGroup.Operator operator = ruleConditionGroup.operator == null ? LogicGroup.Operator.AND : ruleConditionGroup.operator;

        if (!ruleConditionGroup.getItems().isEmpty()) {

            if (operator == LogicGroup.Operator.AND) {

                // Find asset IDs that are matched in all rule conditions
                matchedAssetIds.addAll(ruleConditionGroup.getItems().stream()
                    .map(ruleCondition -> conditionStateMap.get(ruleCondition.tag))
                    .filter(ruleConditionState -> ruleConditionState.lastEvaluationResult != null && ruleConditionState.lastEvaluationResult.matches) //Only trigger results which matches
                    .map(RuleConditionState::getMatchedAssetIds)//Get all matched assetIds
                    .flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(assetId -> assetId, Collectors.counting())) // Group them and count the number of times appearing in the rule conditions
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() == ruleConditionGroup.getItems().size())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()));

            } else if (operator == LogicGroup.Operator.OR) {

                matchedAssetIds.addAll(ruleConditionGroup.getItems().stream()
                    .map(ruleCondition -> conditionStateMap.get(ruleCondition.tag))
                    .filter(ruleConditionState -> ruleConditionState.lastEvaluationResult != null && ruleConditionState.lastEvaluationResult.matches)
                    .map(RuleConditionState::getMatchedAssetIds)//Get all unmatched assetIds
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()));

            }

            if (trackUnmatched) {
                // Unmatched we don't apply group AND/OR just collate them all
                unmatchedAssetIds.addAll(ruleConditionGroup.getItems().stream()
                    .map(ruleCondition -> conditionStateMap.get(ruleCondition.tag))
                    .filter(ruleConditionState -> ruleConditionState.trackUnmatched && ruleConditionState.lastEvaluationResult != null && ruleConditionState.lastEvaluationResult.matches)
                    .map(RuleConditionState::getUnmatchedAssetIds)//Get all unmatched assetIds
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()));
            }
        }

        if (ruleConditionGroup.groups != null) {
            for (LogicGroup<RuleCondition> childRuleConditionGroup : ruleConditionGroup.groups) {
                Pair<List<String>, List<String>> childGroupMatchedAndUnmatched = getGroupLastTriggerResults(childRuleConditionGroup, conditionStateMap, trackUnmatched);
                matchedAssetIds.addAll(childGroupMatchedAndUnmatched.key);
                unmatchedAssetIds.addAll(childGroupMatchedAndUnmatched.value);
            }
        }

        return new Pair<>(matchedAssetIds, unmatchedAssetIds);
    }

    protected static boolean targetIsNotAssets(RuleActionTarget target) {
        return target != null && target.users != null;
    }

    protected static void log(Level level, String message) {
        RulesEngine.RULES_LOG.log(level, LOG_PREFIX + message);
    }

    protected static void log(Level level, String message, Throwable t) {
        RulesEngine.RULES_LOG.log(level, LOG_PREFIX + message, t);
    }
}
