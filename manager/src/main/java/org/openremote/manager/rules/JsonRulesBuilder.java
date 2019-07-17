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
import org.openremote.container.Container;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.rules.facade.AssetsFacade;
import org.openremote.manager.rules.facade.NotificationsFacade;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeValueDescriptor;
import org.openremote.model.notification.Notification;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.NewAssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Users;
import org.openremote.model.rules.json.*;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.value.*;
import org.quartz.CronExpression;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.container.util.Util.distinctByKey;
import static org.openremote.manager.rules.AssetQueryPredicate.conditionIsEmpty;
import static org.openremote.model.query.filter.LocationAttributePredicate.getLocationPredicates;

public class JsonRulesBuilder extends RulesBuilder {

    static class Targets {
        public TargetType type;
        public List<String> ids;

        public Targets(TargetType type, List<String> ids) {
            this.type = type;
            this.ids = ids;
        }
    }

    static class RuleActionExecution {
        Runnable runnable;
        long delay;

        public RuleActionExecution(Runnable runnable, long delay) {
            this.runnable = runnable;
            this.delay = delay;
        }
    }

    static class RuleTriggerState {

        final TimerService timerService;
        boolean trackUnmatched;
        BaseAssetQuery.OrderBy orderBy;
        int limit;
        LogicGroup<AttributePredicate> attributePredicates = null;
        Collection<Predicate<AssetState>> attributeAndedPredicates = null;
        RuleCondition ruleCondition;
        Set<AssetState> unfilteredAssetStates = new HashSet<>();
        Set<AssetState> previouslyMatchedAssetStates = new HashSet<>();
        Set<AssetState> previouslyUnmatchedAssetStates;
        Map<AssetState, Long> previouslyMatchedExpiryTimes;
        long nextExecuteMillis;
        long resetDurationMillis;
        Runnable updateNextExecuteTime;
        CronExpression timerExpression;
        RuleTriggerResult lastTriggerResult;

        public RuleTriggerState(RuleCondition ruleCondition, boolean trackUnmatched, TimerService timerService) {
            this.timerService = timerService;
            this.ruleCondition = ruleCondition;
            this.trackUnmatched = trackUnmatched;

            if (trackUnmatched) {
                previouslyUnmatchedAssetStates = new HashSet<>();
            }

            if (ruleConditionResetHasTimer(ruleCondition.reset)) {
                previouslyMatchedExpiryTimes = new HashMap<>();
                if (!TextUtil.isNullOrEmpty(ruleCondition.reset.timer) && TimeUtil.isTimeDuration(ruleCondition.reset.timer)) {
                    resetDurationMillis = TimeUtil.parseTimeDuration(ruleCondition.reset.timer);
                }
            }

            if (!TextUtil.isNullOrEmpty(ruleCondition.timer)) {
                try {
                    if (TimeUtil.isTimeDuration(ruleCondition.timer)) {
                        nextExecuteMillis = timerService.getCurrentTimeMillis();
                        long duration = TimeUtil.parseTimeDuration(ruleCondition.timer);
                        updateNextExecuteTime = () -> nextExecuteMillis += duration;
                    }
                    if (CronExpression.isValidExpression(ruleCondition.timer)) {
                        timerExpression = new CronExpression(ruleCondition.timer);
                        nextExecuteMillis = timerExpression.getNextValidTimeAfter(new Date(timerService.getCurrentTimeMillis())).getTime();
                        updateNextExecuteTime = () ->
                                nextExecuteMillis = timerExpression.getNextInvalidTimeAfter(timerExpression.getNextInvalidTimeAfter(new Date(nextExecuteMillis))).getTime();
                    }
                } catch (Exception e) {
                    log(Level.SEVERE, "Failed to parse rule trigger timer expression: " + ruleCondition.timer, e);
                }
            }

            // Pull out order, limit and attribute predicates so they can be applied at required times
            if (ruleCondition.assets != null) {
                orderBy = ruleCondition.assets.orderBy;
                limit = ruleCondition.assets.limit;
                attributePredicates = ruleCondition.assets.attributes;

                if (attributePredicates != null && attributePredicates.items != null) {
                    // Note only supports a single level or logic group for attributes (i.e. cannot nest groups)
                    attributePredicates.groups = null;
                    if (attributePredicates.operator == null || attributePredicates.operator == RuleOperator.AND) {
                        attributeAndedPredicates = Arrays.stream(attributePredicates.items)
                                .map(attributePredicate -> AssetQueryPredicate.asPredicate(timerService::getCurrentTimeMillis, attributePredicate))
                                .collect(Collectors.toList());
                    }
                }
                ruleCondition.assets.orderBy = null;
                ruleCondition.assets.limit = 0;
                ruleCondition.assets.attributes = null;
            }
        }

        void updateUnfilteredAssetStates(RulesFacts facts) {
            // Clear last trigger to ensure update runs again
            lastTriggerResult = null;

            if (ruleCondition.timer != null) {
                unfilteredAssetStates = new HashSet<>(facts.getAssetStates());
            } else if (ruleCondition.assets != null) {
                NewAssetQuery query = ruleCondition.assets;
                unfilteredAssetStates = facts.matchAssetState(query).collect(Collectors.toSet());

                // Use this opportunity to notify RulesFacts about any location predicates
                if (facts.trackLocationRules) {
                    List<AttributePredicate> flattenedAttributePredicates = LogicGroup.flatten(Collections.singletonList(attributePredicates));
                    facts.storeLocationPredicates(getLocationPredicates(flattenedAttributePredicates));
                }
            }
        }

        void update() {

            // Last trigger is cleared by rule RHS execution
            if (lastTriggerResult != null && lastTriggerResult.matches) {
                return;
            }

            //Check if rule is still valid
            if (ruleCondition.datetime != null) {
                long currentMillis = timerService.getCurrentTimeMillis();

                Pair<Long, Long> fromAndTo = AssetQueryPredicate.asFromAndTo(currentMillis, ruleCondition.datetime);
                boolean isValid = false;
                switch (ruleCondition.datetime.operator) {
                    case BETWEEN:
                        isValid = currentMillis >= fromAndTo.key && currentMillis < fromAndTo.value;
                        break;
                    case EQUALS:
                        isValid = currentMillis == fromAndTo.key;
                        break;
                    case LESS_THAN:
                        isValid = currentMillis < fromAndTo.key;
                        break;
                    case LESS_EQUALS:
                        isValid = currentMillis <= fromAndTo.key;
                        break;
                    case GREATER_THAN:
                        isValid = currentMillis > fromAndTo.key;
                        break;
                    case GREATER_EQUALS:
                        isValid = currentMillis >= fromAndTo.key;
                        break;
                }
                if (!isValid) {
                    log(Level.FINEST, "Rule trigger is no longer valid so no match");
                    lastTriggerResult = new RuleTriggerResult(false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                    return;
                }
            }

            if (!TextUtil.isNullOrEmpty(ruleCondition.timer)) {
                lastTriggerResult = null;

                if (updateNextExecuteTime != null && nextExecuteMillis < timerService.getCurrentTimeMillis()) {
                    updateNextExecuteTime.run();
                    lastTriggerResult = new RuleTriggerResult(true, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
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
                lastTriggerResult = new RuleTriggerResult(false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                return;
            }

            List<AssetState> matchedAssetStates;
            List<AssetState> unmatchedAssetStates = Collections.emptyList();
            Collection<String> unmatchedAssetIds = Collections.emptyList();

            if (attributePredicates == null) {
                matchedAssetStates = new ArrayList<>(unfilteredAssetStates);
            } else {
                Map<Boolean, List<AssetState>> results;

                if (attributeAndedPredicates != null) {
                    // ANDs need to be applied in the context of an entire asset as don't make any sense otherwise
                    results = new HashMap<>();
                    ArrayList<AssetState> matched = new ArrayList<>();
                    ArrayList<AssetState> unmatched = new ArrayList<>();
                    results.put(true, matched);
                    results.put(false, unmatched);

                    unfilteredAssetStates.stream().collect(Collectors.groupingBy(AssetState::getId)).forEach((id, states) -> {
                        Set<AssetState> assetMatched = new HashSet<>();

                        for (Predicate<AssetState> attributePredicate : attributeAndedPredicates) {
                            Collection<AssetState> assetPredicateMatched = states.stream().filter(attributePredicate).collect(Collectors.toList());

                            if (assetPredicateMatched.isEmpty()) {
                                assetMatched.clear();
                                break;
                            }

                            assetMatched.addAll(assetPredicateMatched);
                        }

                        matched.addAll(assetMatched);
                        unmatched.addAll(states.stream().filter(as -> !assetMatched.contains(as)).collect(Collectors.toList()));
                    });

                } else {
                    Predicate<AssetState> predicate = AssetQueryPredicate.asPredicate(timerService::getCurrentTimeMillis, attributePredicates);
                    results = unfilteredAssetStates.stream().collect(Collectors.groupingBy(predicate::test));
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

            // Apply reset logic to make previously matched asset states eligible for matching again
            if (ruleCondition.reset == null || ruleCondition.reset.noLongerMatches) {
                previouslyMatchedAssetStates.removeIf(assetState -> {
                    boolean noLongerMatches = !matchedAssetStates.contains(assetState);
                    if (noLongerMatches) {
                        log(Level.FINER, "Rule trigger previously matched asset state no longer matches so resetting: " + assetState);
                    }
                    return noLongerMatches;
                });
            }

            if (ruleConditionResetHasTimer(ruleCondition.reset)) {
                previouslyMatchedAssetStates.removeIf(assetState -> {
                    Long timestamp = previouslyMatchedExpiryTimes.get(assetState);
                    boolean expired = timestamp != null && timerService.getCurrentTimeMillis() > timestamp;
                    if (expired) {
                        previouslyMatchedExpiryTimes.remove(assetState);
                        log(Level.FINER, "Rule trigger previously matched asset state timer has expired or timestamp has changed so resetting: " + assetState);
                    }
                    return expired;
                });

                if (ruleCondition.reset.timestampChanges) {
                    previouslyMatchedAssetStates.removeIf(previousAssetState -> {
                        int index = matchedAssetStates.indexOf(previousAssetState);
                        if (index < 0) {
                            return false;
                        }

                        boolean timestampChanged = previousAssetState.getTimestamp() != matchedAssetStates.get(index).getTimestamp();
                        if (timestampChanged) {
                            log(Level.FINER, "Rule trigger previously matched asset state timestamp has changed so resetting: " + previousAssetState);
                        }
                        return timestampChanged;
                    });
                }
            }

            if (ruleCondition.reset != null && ruleCondition.reset.valueChanges) {
                previouslyMatchedAssetStates.removeIf(previousAssetState -> {
                    int index = matchedAssetStates.indexOf(previousAssetState);
                    if (index < 0) {
                        return false;
                    }

                    boolean valueChanged = !Objects.equals(previousAssetState.getValue().orElse(null), matchedAssetStates.get(index).getValue().orElse(null));
                    if (valueChanged) {
                        log(Level.FINER, "Rule trigger previously matched asset state value has changed so resetting: " + previousAssetState);
                    }
                    return valueChanged;
                });
            }

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

            lastTriggerResult = new RuleTriggerResult((!matchedAssetIds.isEmpty() || (trackUnmatched && !unmatchedAssetIds.isEmpty())), matchedAssetStates, matchedAssetIds, unmatchedAssetStates, unmatchedAssetIds);
            log(Level.FINEST, "Rule trigger result: " + lastTriggerResult);
        }

        Collection<String> getMatchedAssetIds() {

            if (lastTriggerResult == null) {
                return Collections.emptyList();
            }
            return lastTriggerResult.matchedAssetIds;
        }

        Collection<String> getUnmatchedAssetIds() {
            if (lastTriggerResult == null) {
                return Collections.emptyList();
            }

            return lastTriggerResult.unmatchedAssetIds;
        }
    }

    static class RuleTriggerResult {
        boolean matches;
        Collection<AssetState> matchedAssetStates;
        Collection<AssetState> unmatchedAssetStates;
        Collection<String> matchedAssetIds;
        Collection<String> unmatchedAssetIds;

        public RuleTriggerResult(boolean matches, Collection<AssetState> matchedAssetStates, Collection<String> matchedAssetIds, Collection<AssetState> unmatchedAssetStates, Collection<String> unmatchedAssetIds) {
            this.matches = matches;
            this.matchedAssetStates = matchedAssetStates;
            this.matchedAssetIds = matchedAssetIds;
            this.unmatchedAssetStates = unmatchedAssetStates;
            this.unmatchedAssetIds = unmatchedAssetIds;
        }

        @Override
        public String toString() {
            return RuleTriggerResult.class.getSimpleName() + "{" +
                    "matches=" + matches +
                    ", matchedAssetStates=" + matchedAssetStates.size() +
                    ", unmatchedAssetStates=" + unmatchedAssetStates.size() +
                    ", matchedAssetIds=" + matchedAssetIds.size() +
                    ", unmatchedAssetIds=" + unmatchedAssetIds.size() +
                    '}';
        }
    }

    enum TargetType {
        ASSET,
        USER
    }

    final static String LOG_PREFIX = "JSON Rules: ";
    final protected AssetStorageService assetStorageService;
    final protected TimerService timerService;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected NotificationsFacade notificationFacade;
    final protected ManagerExecutorService executorService;
    final protected BiConsumer<Runnable, Long> scheduledActionConsumer;
    final protected Map<String, Map<String, RuleTriggerState>> ruleConditionStateMap = new HashMap<>();

    public JsonRulesBuilder(TimerService timerService, AssetStorageService assetStorageService, ManagerExecutorService executorService, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationFacade, BiConsumer<Runnable, Long> scheduledActionConsumer) {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.executorService = executorService;
        this.assetsFacade = assetsFacade;
        this.usersFacade = usersFacade;
        this.notificationFacade = notificationFacade;
        this.scheduledActionConsumer = scheduledActionConsumer;
    }

    public void onAssetStatesChanged(RulesFacts facts) {
        ruleConditionStateMap.values().forEach(triggerStateMap -> triggerStateMap.values().forEach(ruleConditionState -> ruleConditionState.updateUnfilteredAssetStates(facts)));
    }

    public JsonRulesBuilder add(Rule rule) {

        if (ruleConditionStateMap.containsKey(rule.name)) {
            throw new IllegalArgumentException("Rules must have a unique name within a ruleset, rule name '" + rule.name + "' already used");
        }

        Map<String, RuleTriggerState> triggerStateMap = new HashMap<>();
        ruleConditionStateMap.put(rule.name, triggerStateMap);
        addRuleTriggerStates(rule.when, rule.otherwise != null, 0, triggerStateMap, rule.reset);

        Condition condition = buildLhsCondition(rule, triggerStateMap);
        Action action = buildRhsAction(rule, triggerStateMap);

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

    protected void addRuleTriggerStates(LogicGroup<RuleCondition> ruleConditionCondition, boolean trackUnmatched, int index, Map<String, RuleTriggerState> triggerStateMap, RuleConditionReset defaultReset) {
        if (ruleConditionCondition != null) {
            if (ruleConditionCondition.items != null && ruleConditionCondition.items.length > 0) {
                for (RuleCondition ruleCondition : ruleConditionCondition.items) {
                    if (TextUtil.isNullOrEmpty(ruleCondition.tag)) {
                        ruleCondition.tag = Integer.toString(index);
                    }
                    if (ruleCondition.reset == null) {
                        ruleCondition.reset = defaultReset;
                    }
                    triggerStateMap.put(ruleCondition.tag, new RuleTriggerState(ruleCondition, trackUnmatched, timerService));
                    index++;
                }
            }
            if (ruleConditionCondition.groups != null && ruleConditionCondition.groups.length > 0) {
                for (LogicGroup<RuleCondition> childRuleTriggerCondition : ruleConditionCondition.groups) {
                    addRuleTriggerStates(childRuleTriggerCondition, trackUnmatched, index, triggerStateMap, defaultReset);
                }
            }
        }
    }

    protected Condition buildLhsCondition(Rule rule, Map<String, RuleTriggerState> triggerStateMap) {
        if (rule.when == null) {
            return null;
        }

        return facts -> {

            // Update each trigger state
            log(Level.FINEST, "Updating rule trigger states for rule: " + rule.name);
            triggerStateMap.values().forEach(RuleTriggerState::update);

            // Check triggers for matches
            return matches(rule.when, triggerStateMap);
        };
    }

    protected Action buildRhsAction(Rule rule, Map<String, RuleTriggerState> triggerStateMap) {

        if (rule.then == null) {
            return null;
        }

        return facts -> {

            try {
                log(Level.FINER, "Triggered rule so executing 'then' actions for rule: " + rule.name);
                executeRuleActions(rule, rule.then, "then", false, facts, triggerStateMap, assetsFacade, usersFacade, notificationFacade, timerService, assetStorageService, scheduledActionConsumer);

                if (rule.otherwise != null) {
                    log(Level.FINER, "Triggered rule so executing 'otherwise' actions for rule: " + rule.name);
                    executeRuleActions(rule, rule.otherwise, "otherwise", true, facts, triggerStateMap, assetsFacade, usersFacade, notificationFacade, timerService, assetStorageService, scheduledActionConsumer);
                }
            } catch (Exception e) {
                log(Level.SEVERE, "Exception thrown during rule RHS execution", e);
                throw e;
            } finally {
                triggerStateMap.values().forEach(triggerState -> triggerState.lastTriggerResult = null);
            }
        };
    }

    public static void executeRuleActions(Rule rule, RuleAction[] ruleActions, String actionsName, boolean useUnmatched, RulesFacts facts, Map<String, RuleTriggerState> triggerStateMap, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationsFacade, TimerService timerService, AssetStorageService assetStorageService, BiConsumer<Runnable, Long> scheduledActionConsumer) {

        // Push rule trigger results into the trigger state for future runs and store timestamps where required
        if (triggerStateMap != null) {
            triggerStateMap.values().forEach(ruleConditionState -> {
                if (ruleConditionState.lastTriggerResult != null) {

                    // Replace any stale matched asset states (values may have changed equality is by asset ID and attribute name)
                    // only need up to date values in the previously matched asset states previously unmatched asset states is only
                    // used to compare asset ID and attribute name.
                    ruleConditionState.previouslyMatchedAssetStates.removeAll(ruleConditionState.lastTriggerResult.matchedAssetStates);
                    ruleConditionState.previouslyMatchedAssetStates.addAll(ruleConditionState.lastTriggerResult.matchedAssetStates);

                    RuleConditionReset reset = ruleConditionState.ruleCondition.reset;
                    if (ruleConditionResetHasTimer(reset)) {
                        if (ruleConditionState.resetDurationMillis > 0) {
                            ruleConditionState.lastTriggerResult.matchedAssetStates.forEach(assetState ->
                                ruleConditionState.previouslyMatchedExpiryTimes.put(assetState, timerService.getCurrentTimeMillis() + ruleConditionState.resetDurationMillis));
                        } else if (reset.timestampChanges) {
                            ruleConditionState.lastTriggerResult.matchedAssetStates.forEach(assetState ->
                                ruleConditionState.previouslyMatchedExpiryTimes.put(assetState, assetState.getTimestamp()));
                        }
                    }
                    if (ruleConditionState.trackUnmatched) {
                        ruleConditionState.previouslyUnmatchedAssetStates.addAll(ruleConditionState.lastTriggerResult.unmatchedAssetStates);
                    }
                }
            });
        }

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
                        triggerStateMap,
                        assetsFacade,
                        usersFacade,
                        notificationsFacade,
                        assetStorageService);

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
        return users.query()
                .tenant(userQuery.tenantPredicate)
                .assetPath(userQuery.pathPredicate)
                .asset(userQuery.assetPredicate)
                .limit(userQuery.limit)
                .getResults();
    }

    protected static Collection<String> getAssetIds(Assets assets, NewAssetQuery newAssetQuery) {
        AssetQuery query = new AssetQuery();
        newAssetQueryToAssetQuery(newAssetQuery, query);
        return assets.query()
                .ids(query.ids)
                .name(query.name)
                .parent(query.parent)
                .path(query.path)
                .type(query.type)
                .attributes(query.attribute)
                .attributeMeta(query.attributeMeta)
                .stream()
                .map(Asset::getId)
                .collect(Collectors.toList());
    }

    protected static RuleActionExecution buildRuleActionExecution(Rule rule, RuleAction ruleAction, String actionsName, int index, boolean useUnmatched, RulesFacts facts, Map<String, RuleTriggerState> triggerStateMap, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationsFacade, AssetStorageService assetStorageService) {

        if (ruleAction instanceof RuleActionNotification) {
            RuleActionNotification notificationAction = (RuleActionNotification) ruleAction;

            if (notificationAction.notification != null) {

                // Override the notification targets if set in the rule
                Notification.TargetType targetType = Notification.TargetType.ASSET;
                Collection<String> ids = getRuleActionTargetIds(ruleAction.target, useUnmatched, triggerStateMap, assetsFacade, usersFacade);
                if (targetIsNotAssets(ruleAction.target)) {
                    targetType = Notification.TargetType.USER;
                }

                if (ids == null || ids.isEmpty()) {
                    log(Level.FINEST, "No targets for notification rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
                    return null;
                }

                notificationAction.notification.setTargets(new Notification.Targets(targetType, ids));
                log(Level.FINE, "Sending notification for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                return new RuleActionExecution(() -> notificationsFacade.send(notificationAction.notification), 0);
            }
        }

        if (ruleAction instanceof RuleActionWriteAttribute) {

            if (targetIsNotAssets(ruleAction.target)) {
                return null;
            }

            RuleActionWriteAttribute attributeAction = (RuleActionWriteAttribute) ruleAction;

            if (!TextUtil.isNullOrEmpty(attributeAction.attributeName)) {
                Collection<String> ids = getRuleActionTargetIds(ruleAction.target, useUnmatched, triggerStateMap, assetsFacade, usersFacade);

                if (ids == null || ids.isEmpty()) {
                    log(Level.FINEST, "No targets for write attribute rule action so skipping: " + rule.name + " '" + actionsName + "' action index " + index);
                    return null;
                }

                log(Level.FINE, "Writing attribute '" + attributeAction.attributeName + "' for " + ids.size() + " asset(s) for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                return new RuleActionExecution(() ->
                        ids.forEach(id ->
                                assetsFacade.dispatch(id, attributeAction.attributeName, attributeAction.value)), 0);
            }
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

            if (!TextUtil.isNullOrEmpty(attributeUpdateAction.attributeName)) {

                Map<String, Pair<ValueType, Value>> assetAttributeValueMap = new HashMap<>();
                NewAssetQuery assetQuery = null;

                if (ruleAction.target == null || ruleAction.target.assets == null) {
                    Collection<String> assetIds = getRuleActionTargetIds(ruleAction.target, useUnmatched, triggerStateMap, assetsFacade, usersFacade);

                    // Try and find the current value within the asset states in memory to avoid a DB call when possible
                    Collection<String> assetsToLookup = assetIds.stream().filter(assetId -> {
                        Optional<AssetState> assetState = facts.getAssetStates()
                                .stream()
                                .filter(state -> state.getId().equals(assetId) && state.getAttributeName().equals(attributeUpdateAction.attributeName))
                                .findFirst();

                        if (assetState.isPresent()) {
                            ValueType valueType = assetState.get().getValue().map(Value::getType).orElseGet(() -> assetState.get().getAttributeValueType() != null ? assetState.get().getAttributeValueType().getValueType() : null);
                            if (!(valueType == ValueType.ARRAY || valueType == ValueType.OBJECT)) {
                                log(Level.WARNING, "JSON Rule: Rule action target asset '" + assetState.get().getId() + "' cannot determine value type or incompatible value type for attribute: " + attributeUpdateAction.attributeName);
                            } else {
                                assetAttributeValueMap.put(assetState.get().getId(), new Pair<>(valueType, assetState.get().getValue().orElse(null)));
                            }
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toList());

                    if (!assetsToLookup.isEmpty()) {
                        assetQuery = new NewAssetQuery();
                        assetQuery.ids = assetsToLookup.toArray(new String[0]);
                    }
                } else {
                    assetQuery = ruleAction.target.assets;
                }

                if (assetQuery != null) {
                    Stream<Asset> assets = getRuleActionAssets(ruleAction.target, useUnmatched, triggerStateMap, assetsFacade, usersFacade, assetStorageService);
                    assets.forEach(asset -> {
                        Attribute attribute = asset.getAttribute(attributeUpdateAction.attributeName).orElse(null);
                        if (attribute == null) {
                            log(Level.WARNING, "JSON Rule: Rule action target asset '" + asset.getId() + "' doesn't have requested attribute: " + attributeUpdateAction.attributeName);
                        } else {
                            ValueType valueType = attribute.getValue().map(Value::getType).orElseGet(() -> attribute.getType().map(AttributeValueDescriptor::getValueType).orElse(null));
                            if (!(valueType == ValueType.ARRAY || valueType == ValueType.OBJECT)) {
                                log(Level.WARNING, "JSON Rule: Rule action target asset '" + asset.getId() + "' cannot determine value type or incompatible value type for attribute: " + attributeUpdateAction.attributeName);
                            } else {
                                assetAttributeValueMap.put(asset.getId(), new Pair<>(valueType, attribute.getValue().orElse(null)));
                            }
                        }
                    });
                }

                assetAttributeValueMap.keySet().removeIf(assetId -> {
                            Pair<ValueType, Value> valueTypeValue = assetAttributeValueMap.get(assetId);
                            return valueTypeValue.value == null
                                    && (attributeUpdateAction.updateAction == RuleActionUpdateAttribute.UpdateAction.DELETE
                                    || attributeUpdateAction.updateAction == RuleActionUpdateAttribute.UpdateAction.CLEAR);
                        });

                if (assetAttributeValueMap.isEmpty()) {
                    return null;
                }

                log(Level.FINE, "Updating attribute '" + attributeUpdateAction.attributeName + "' for " + assetAttributeValueMap.size() + " asset(s) for rule action: " + rule.name + " '" + actionsName + "' action index " + index);
                return new RuleActionExecution(() ->
                        assetAttributeValueMap.forEach((id, valueAndType) -> {
                            ValueType valueType = valueAndType.key;
                            Value value = valueAndType.value;

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
                                                log(Level.WARNING, "JSON Rule: Rule action missing required 'key': " + Container.JSON.writeValueAsString(attributeUpdateAction));
                                            } catch (JsonProcessingException ignored) {
                                            }
                                        }
                                    }
                                    break;
                                case DELETE:
                                    if (valueType.equals(ValueType.ARRAY)) {
                                        ((ArrayValue) value).remove(attributeUpdateAction.index);
                                    } else {
                                        ((ObjectValue) value).remove(attributeUpdateAction.key);
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

                            assetsFacade.dispatch(id, attributeUpdateAction.attributeName, value);
                        }), 0);
            }
        }

        log(Level.FINE, "Unsupported rule action: " + rule.name + " '" + actionsName + "' action index " + index);
        return null;
    }

    protected static Stream<Asset> getRuleActionAssets(RuleActionTarget target, boolean useUnmatched, Map<String, RuleTriggerState> triggerStateMap, Assets assetsFacade, Users usersFacade, AssetStorageService assetStorageService) {
        AssetsFacade.RestrictedQuery query = assetsFacade.query();

        if (target == null || target.assets == null) {
            Collection<String> ids = getRuleActionTargetIds(target, useUnmatched, triggerStateMap, assetsFacade, usersFacade);
            if (ids.isEmpty()) {
                return Stream.empty();
            }
            query.ids(ids);
        } else {
            newAssetQueryToAssetQuery(target.assets, query);
        }

        query.select = new BaseAssetQuery.Select(BaseAssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES);
        return assetStorageService.findAll(query).stream();
    }
    
    // TODO: This is a hack until AssetQuery and NewAssetQuery are merged
    protected static void newAssetQueryToAssetQuery(NewAssetQuery newQuery, BaseAssetQuery query) {
        query.ids(newQuery.ids)
                .name(newQuery.names != null && newQuery.names.length > 0 ? newQuery.names[0] : null)
                .parent(newQuery.parents != null && newQuery.parents.length > 0 ? newQuery.parents[0] : null)
                .path(newQuery.paths != null && newQuery.paths.length > 0 ? newQuery.paths[0] : null)
                .type(newQuery.types != null && newQuery.types.length > 0 ? newQuery.types[0] : null)
                .attributes(newQuery.attributes != null && newQuery.attributes.items != null ? newQuery.attributes.items : null);
    }
    
    protected static Collection<String> getRuleActionTargetIds(RuleActionTarget target, boolean useUnmatched, Map<String, RuleTriggerState> triggerStateMap, Assets assetsFacade, Users usersFacade) {
        if (target != null) {
            if (!TextUtil.isNullOrEmpty(target.ruleConditionTag) && triggerStateMap != null) {
                RuleTriggerState triggerState = triggerStateMap.get(target.ruleConditionTag);
                if (!useUnmatched) {
                    return triggerState != null ? triggerState.getMatchedAssetIds() : Collections.emptyList();
                }

                return triggerState != null ? triggerState.getUnmatchedAssetIds() : Collections.emptyList();
            }

            if (target.assets != null) {
                return getAssetIds(assetsFacade, target.assets);
            }

            if (target.users != null) {
                return getUserIds(usersFacade, target.users);
            }
        }

        if (triggerStateMap != null) {
            if (!useUnmatched) {
                return triggerStateMap.values().stream().flatMap(triggerState ->
                        triggerState != null ? triggerState.getMatchedAssetIds().stream() : Stream.empty()
                ).distinct().collect(Collectors.toList());
            }

            return triggerStateMap.values().stream().flatMap(triggerState ->
                    triggerState != null ? triggerState.getUnmatchedAssetIds().stream() : Stream.empty()
            ).distinct().collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    protected static boolean matches(LogicGroup<RuleCondition> ruleConditionGroup, Map<String, RuleTriggerState> triggerStateMap) {

        boolean match = false;

        if (conditionIsEmpty(ruleConditionGroup)) {
            return false;
        }

        RuleOperator operator = ruleConditionGroup.operator == null ? RuleOperator.AND : ruleConditionGroup.operator;

        if (operator == RuleOperator.AND) {
            if (ruleConditionGroup.items != null) {
                match = Arrays.stream(ruleConditionGroup.items)
                        .map(ruleCondition -> triggerStateMap.get(ruleCondition.tag))
                        .allMatch(ruleConditionState -> ruleConditionState.lastTriggerResult != null && ruleConditionState.lastTriggerResult.matches);

                if (!match) {
                    return false;
                }
            }

            if (ruleConditionGroup.groups != null) {
                match = Arrays.stream(ruleConditionGroup.groups)
                        .allMatch(condition -> matches(condition, triggerStateMap));
            }
        }

        if (operator == RuleOperator.OR) {
            if (ruleConditionGroup.items != null) {
                match = Arrays.stream(ruleConditionGroup.items)
                        .map(ruleCondition -> triggerStateMap.get(ruleCondition.tag))
                        .anyMatch(ruleConditionState -> ruleConditionState.lastTriggerResult != null && ruleConditionState.lastTriggerResult.matches);

                if (match) {
                    return true;
                }
            }

            if (ruleConditionGroup.groups != null) {
                match = Arrays.stream(ruleConditionGroup.groups)
                        .anyMatch(condition -> matches(condition, triggerStateMap));
            }
        }

        return match;
    }

    protected static boolean ruleConditionResetHasTimer(RuleConditionReset reset) {
        return reset != null && (!TextUtil.isNullOrEmpty(reset.timer) || reset.timestampChanges);
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
