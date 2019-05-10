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
        boolean notifiedLocationPredicates;
        BaseAssetQuery.OrderBy orderBy;
        int limit;
        RuleCondition<AttributePredicate> attributePredicates = null;
        RuleTrigger ruleTrigger;
        Set<AssetState> unfilteredAssetStates = new HashSet<>();
        Set<AssetState> previouslyMatchedAssetStates = new HashSet<>();
        Set<AssetState> previouslyUnmatchedAssetStates;
        Map<AssetState, Long> previouslyMatchedExpiryTimes;
        long nextExecuteMillis;
        long resetDurationMillis;
        Runnable updateNextExecuteTime;
        CronExpression timerExpression;
        RuleTriggerResult lastTriggerResult;

        public RuleTriggerState(RuleTrigger ruleTrigger, boolean trackUnmatched, TimerService timerService) {
            this.timerService = timerService;
            this.ruleTrigger = ruleTrigger;

            if (trackUnmatched) {
                previouslyUnmatchedAssetStates = new HashSet<>();
            }

            if (ruleTriggerResetHasTimer(ruleTrigger.reset)) {
                previouslyMatchedExpiryTimes = new HashMap<>();
                if (!TextUtil.isNullOrEmpty(ruleTrigger.reset.timer) && TimeUtil.isTimeDuration(ruleTrigger.reset.timer)) {
                    resetDurationMillis = TimeUtil.parseTimeDuration(ruleTrigger.reset.timer);
                }
            }

            if (!TextUtil.isNullOrEmpty(ruleTrigger.timer)) {
                try {
                    if (TimeUtil.isTimeDuration(ruleTrigger.timer)) {
                        nextExecuteMillis = timerService.getCurrentTimeMillis();
                        long duration = TimeUtil.parseTimeDuration(ruleTrigger.timer);
                        updateNextExecuteTime = () -> nextExecuteMillis += duration;
                    }
                    if (CronExpression.isValidExpression(ruleTrigger.timer)) {
                        timerExpression = new CronExpression(ruleTrigger.timer);
                        nextExecuteMillis = timerExpression.getNextValidTimeAfter(new Date(timerService.getCurrentTimeMillis())).getTime();
                        updateNextExecuteTime = () ->
                                nextExecuteMillis = timerExpression.getNextInvalidTimeAfter(timerExpression.getNextInvalidTimeAfter(new Date(nextExecuteMillis))).getTime();
                    }
                } catch (Exception e) {
                    RulesEngine.RULES_LOG.log(Level.SEVERE, "Failed to parse rule trigger timer expression: " + ruleTrigger.timer, e);
                }
            }

            // Pull out order, limit and attribute predicates so they can be applied at required times
            if (ruleTrigger.assets != null) {
                orderBy = ruleTrigger.assets.orderBy;
                limit = ruleTrigger.assets.limit;
                attributePredicates = ruleTrigger.assets.attributes;
                ruleTrigger.assets.orderBy = null;
                ruleTrigger.assets.limit = 0;
                ruleTrigger.assets.attributes = null;
            }
        }

        void updateUnfilteredAssetStates(RulesFacts facts) {
            if (ruleTrigger.timer != null) {
                unfilteredAssetStates = new HashSet<>(facts.getAssetStates());
            } else if (ruleTrigger.assets != null) {
                NewAssetQuery query = ruleTrigger.assets;
                unfilteredAssetStates = facts.matchAssetState(query).collect(Collectors.toSet());

                // Use this opportunity to notify RulesFacts about any location predicates
                if (!notifiedLocationPredicates) {
                    List<AttributePredicate> flattenedAttributePredicates = RuleCondition.flatten(Collections.singletonList(attributePredicates));
                    facts.storeLocationPredicates(getLocationPredicates(flattenedAttributePredicates));
                    notifiedLocationPredicates = true;
                }
            }
        }


        void update() {
            if (!TextUtil.isNullOrEmpty(ruleTrigger.timer)) {
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
                if (previouslyUnmatchedAssetStates != null) {
                    previouslyUnmatchedAssetStates.clear();
                }
                lastTriggerResult = new RuleTriggerResult(false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                return;
            }

            List<AssetState> matchedAssetStates;
            List<AssetState> unmatchedAssetStates = Collections.emptyList();

            if (attributePredicates == null) {
                matchedAssetStates = new ArrayList<>(unfilteredAssetStates);
            } else {
                Predicate<AssetState> predicate = AssetQueryPredicate.asPredicate(timerService::getCurrentTimeMillis, attributePredicates);
                Map<Boolean,List<AssetState>> results = unfilteredAssetStates.stream().collect(Collectors.groupingBy(predicate::test));
                matchedAssetStates = results.get(true);
                unmatchedAssetStates = results.get(false);

                if (previouslyUnmatchedAssetStates != null) {
                    // Clear out previous unmatched that now match
                    previouslyUnmatchedAssetStates.removeIf(matchedAssetStates::contains);

                    // Filter out previous un-matches to avoid re-triggering
                    unmatchedAssetStates.removeIf(previouslyUnmatchedAssetStates::contains);
                }
            }

            // Apply reset logic to make previously matched asset states eligible for matching again
            if (ruleTrigger.reset == null || ruleTrigger.reset.noLongerMatches) {
                previouslyMatchedAssetStates.removeIf(assetState -> !matchedAssetStates.contains(assetState));
            }

            if (ruleTriggerResetHasTimer(ruleTrigger.reset)) {
                previouslyMatchedAssetStates.removeIf(assetState -> {
                    Long timestamp = previouslyMatchedExpiryTimes.get(assetState);
                    boolean expired = timestamp != null && timerService.getCurrentTimeMillis() > timestamp;
                    if (expired) {
                        previouslyMatchedExpiryTimes.remove(assetState);
                    }
                    return expired;
                });
            }

            if (ruleTrigger.reset != null && ruleTrigger.reset.valueChanges) {
                previouslyMatchedAssetStates.removeIf(previousAssetState -> {
                    int index = matchedAssetStates.indexOf(previousAssetState);
                    if (index < 0) {
                        return false;
                    }

                    return !Objects.equals(previousAssetState.getValue().orElse(null), matchedAssetStates.get(index).getValue().orElse(null));
                });
            }

            // Filter out previous matches to avoid re-triggering
            matchedAssetStates.removeIf(assetState -> previouslyMatchedAssetStates.contains(assetState));

            // Select unique assets from the states
            Stream<AssetState> matchedAssetStateStream = matchedAssetStates.stream().filter(distinctByKey(AssetState::getId));
            Stream<AssetState> unmatchedAssetStateStream = unmatchedAssetStates.stream().filter(distinctByKey(AssetState::getId));

            // Order and limit
            if (orderBy != null) {
                matchedAssetStateStream = matchedAssetStateStream.sorted(RulesFacts.asComparator(orderBy));
            }

            if (limit > 0) {
                matchedAssetStateStream = matchedAssetStateStream.limit(limit);
            }

            Collection<String> matchedAssetIds = matchedAssetStateStream.map(AssetState::getId).collect(Collectors.toList());

            // Filter out unmatched asset ids that are in the matched list
            Collection<String> unmatchedAssetIds = unmatchedAssetStateStream
                    .filter(assetState -> !matchedAssetIds.contains(assetState.getId()))
                    .map(AssetState::getId)
                    .collect(Collectors.toList());

            lastTriggerResult = new RuleTriggerResult((!matchedAssetIds.isEmpty() || !unmatchedAssetIds.isEmpty()), matchedAssetStates, matchedAssetIds, unmatchedAssetStates, unmatchedAssetIds);
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
    }

    enum TargetType {
        ASSET,
        USER
    }

    final protected AssetStorageService assetStorageService;
    final protected TimerService timerService;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected NotificationsFacade notificationFacade;
    final protected ManagerExecutorService executorService;
    final protected BiConsumer<Runnable, Long> scheduledActionConsumer;
    final protected Map<String, Map<String, RuleTriggerState>> ruleTriggerStateMap = new HashMap<>();

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
        ruleTriggerStateMap.values().forEach(triggerStateMap -> triggerStateMap.values().forEach(ruleTriggerState -> ruleTriggerState.updateUnfilteredAssetStates(facts)));
    }

    public JsonRulesBuilder add(Rule rule) {

        if (ruleTriggerStateMap.containsKey(rule.name)) {
            throw new IllegalArgumentException("Rules must have a unique name within a ruleset, rule name '" + rule.name + "' already used");
        }

        Map<String, RuleTriggerState> triggerStateMap = new HashMap<>();
        ruleTriggerStateMap.put(rule.name, triggerStateMap);
        addRuleTriggerState(rule.when, rule.otherwise != null, 0, triggerStateMap, rule.reset);

        Condition condition = buildLhsCondition(rule, triggerStateMap);
        Action action = buildRhsAction(rule, triggerStateMap);

        if (condition == null || action == null) {
            throw new IllegalArgumentException("Error building JSON rule '" + rule.name + "'");
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

    protected void addRuleTriggerState(RuleCondition<RuleTrigger> ruleTriggerCondition, boolean trackUnmatched, int index, Map<String, RuleTriggerState> triggerStateMap, RuleTriggerReset defaultReset) {
        if (ruleTriggerCondition != null) {
            if (ruleTriggerCondition.predicates != null && ruleTriggerCondition.predicates.length > 0) {
                for (RuleTrigger ruleTrigger : ruleTriggerCondition.predicates) {
                    if (TextUtil.isNullOrEmpty(ruleTrigger.tag)) {
                        ruleTrigger.tag = Integer.toString(index);
                    }
                    if (ruleTrigger.reset == null) {
                        ruleTrigger.reset = defaultReset;
                    }
                    triggerStateMap.put(ruleTrigger.tag, new RuleTriggerState(ruleTrigger, trackUnmatched, timerService));
                    index++;
                }
            }
            if (ruleTriggerCondition.conditions != null && ruleTriggerCondition.conditions.length > 0) {
                for (RuleCondition<RuleTrigger> childRuleTriggerCondition : ruleTriggerCondition.conditions) {
                    addRuleTriggerState(childRuleTriggerCondition, trackUnmatched, index, triggerStateMap, defaultReset);
                }
            }
        }
    }

    protected Condition buildLhsCondition(Rule rule, Map<String, RuleTriggerState> triggerStateMap) {
        if (rule.when == null) {
            return facts -> false;
        }

        return facts -> {

            // Update each trigger state
            triggerStateMap.values().forEach(RuleTriggerState::update);

            // Check triggers for matches
            return matches(rule.when, triggerStateMap);
        };
    }

    protected Action buildRhsAction(Rule rule, Map<String, RuleTriggerState> triggerStateMap) {

        return facts -> {

            executeRuleActions(rule.then, false, facts, triggerStateMap, assetsFacade, usersFacade, notificationFacade, timerService, assetStorageService, scheduledActionConsumer);

            if (rule.otherwise != null) {
                executeRuleActions(rule.otherwise, true, facts, triggerStateMap, assetsFacade, usersFacade, notificationFacade, timerService, assetStorageService, scheduledActionConsumer);
            }
        };
    }

    public static void executeRuleActions(RuleAction[] ruleActions, boolean useUnmatched, RulesFacts facts, Map<String, RuleTriggerState> triggerStateMap, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationsFacade, TimerService timerService, AssetStorageService assetStorageService, BiConsumer<Runnable, Long> scheduledActionConsumer) {

        // Push rule trigger results into the trigger state for future runs and store timestamps where required
        if (triggerStateMap != null) {
            triggerStateMap.values().forEach(ruleTriggerState -> {
                if (ruleTriggerState.lastTriggerResult != null) {

                    if (!useUnmatched) {
                        // Remove any stale matched asset states
                        ruleTriggerState.previouslyMatchedAssetStates.removeAll(ruleTriggerState.lastTriggerResult.matchedAssetStates);
                        ruleTriggerState.previouslyMatchedAssetStates.addAll(ruleTriggerState.lastTriggerResult.matchedAssetStates);

                        RuleTriggerReset reset = ruleTriggerState.ruleTrigger.reset;
                        if (ruleTriggerResetHasTimer(reset)) {
                            if (reset.timestampChanges) {
                                ruleTriggerState.lastTriggerResult.matchedAssetStates.forEach(assetState ->
                                        ruleTriggerState.previouslyMatchedExpiryTimes.put(assetState, assetState.getTimestamp()));
                            } else if (ruleTriggerState.resetDurationMillis > 0) {
                                ruleTriggerState.lastTriggerResult.matchedAssetStates.forEach(assetState ->
                                        ruleTriggerState.previouslyMatchedExpiryTimes.put(assetState, timerService.getCurrentTimeMillis() + ruleTriggerState.resetDurationMillis));
                            }
                        }
                    } else {
                        if (ruleTriggerState.previouslyUnmatchedAssetStates != null) {
                            ruleTriggerState.previouslyUnmatchedAssetStates.addAll(ruleTriggerState.lastTriggerResult.unmatchedAssetStates);
                        }
                    }
                }
            });
        }

        if (ruleActions != null && ruleActions.length > 0) {

            long delay = 0L;

            for (RuleAction ruleAction : ruleActions) {
                JsonRulesBuilder.RuleActionExecution actionExecution = buildRuleActionExecution(
                        ruleAction,
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

    protected static Collection<String> getAssetIds(Assets assets, org.openremote.model.query.AssetQuery assetQuery) {
        return assets.query()
                .ids(assetQuery.ids)
                .name(assetQuery.name)
                .parent(assetQuery.parent)
                .path(assetQuery.path)
                .type(assetQuery.type)
                .attributes(assetQuery.attribute)
                .attributeMeta(assetQuery.attributeMeta)
                .stream()
                .map(Asset::getId)
                .collect(Collectors.toList());
    }

    protected static RuleActionExecution buildRuleActionExecution(RuleAction ruleAction, boolean useUnmatched, RulesFacts facts, Map<String, RuleTriggerState> triggerStateMap, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationsFacade, AssetStorageService assetStorageService) {

        if (ruleAction instanceof RuleActionNotification) {
            RuleActionNotification notificationAction = (RuleActionNotification) ruleAction;

            if (notificationAction.notification != null) {

                // Override the notification targets if set in the rule
                Notification.TargetType targetType = Notification.TargetType.ASSET;
                Collection<String> ids = getRuleActionTargetIds(ruleAction.target, useUnmatched, triggerStateMap, assetsFacade, usersFacade);
                if (targetIsNotAssets(ruleAction.target)) {
                    targetType = Notification.TargetType.USER;
                }

                if (ids != null) {
                    notificationAction.notification.setTargets(
                            new Notification.Targets(targetType, ids));
                }

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

                if (ids != null) {
                    return new RuleActionExecution(() ->
                            ids.forEach(id ->
                                    assetsFacade.dispatch(id, attributeAction.attributeName, attributeAction.value)), 0);
                }
            }
        }

        if (ruleAction instanceof RuleActionWait) {
            long millis = ((RuleActionWait) ruleAction).millis;
            if (millis > 0) {
                return new RuleActionExecution(null, millis);
            }
        }

        if (ruleAction instanceof RuleActionUpdateAttribute) {

            if (targetIsNotAssets(ruleAction.target)) {
                return null;
            }

            RuleActionUpdateAttribute attributeUpdateAction = (RuleActionUpdateAttribute) ruleAction;

            if (!TextUtil.isNullOrEmpty(attributeUpdateAction.attributeName)) {

                Map<String, Pair<ValueType, Value>> assetAttributeValueMap = new HashMap<>();
                AssetQuery assetQuery = null;

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
                                RulesEngine.RULES_LOG.warning("JSON Rule: Rule action target asset '" + assetState.get().getId() + "' cannot determine value type or incompatible value type for attribute: " + attributeUpdateAction.attributeName);
                            } else {
                                assetAttributeValueMap.put(assetState.get().getId(), new Pair<>(valueType, assetState.get().getValue().orElse(null)));
                            }
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toList());

                    if (!assetsToLookup.isEmpty()) {
                        assetQuery = new AssetQuery().ids(assetsToLookup);
                    }
                } else {
                    assetQuery = ruleAction.target.assets;
                }

                if (assetQuery != null) {
                    Stream<Asset> assets = getRuleActionAssets(ruleAction.target, useUnmatched, triggerStateMap, assetsFacade, usersFacade, assetStorageService);
                    assets.forEach(asset -> {
                        Attribute attribute = asset.getAttribute(attributeUpdateAction.attributeName).orElse(null);
                        if (attribute == null) {
                            RulesEngine.RULES_LOG.warning("JSON Rule: Rule action target asset '" + asset.getId() + "' doesn't have requested attribute: " + attributeUpdateAction.attributeName);
                        } else {
                            ValueType valueType = attribute.getValue().map(Value::getType).orElseGet(() -> attribute.getType().map(AttributeValueDescriptor::getValueType).orElse(null));
                            if (!(valueType == ValueType.ARRAY || valueType == ValueType.OBJECT)) {
                                RulesEngine.RULES_LOG.warning("JSON Rule: Rule action target asset '" + asset.getId() + "' cannot determine value type or incompatible value type for attribute: " + attributeUpdateAction.attributeName);
                            } else {
                                assetAttributeValueMap.put(asset.getId(), new Pair<>(valueType, attribute.getValue().orElse(null)));
                            }
                        }
                    });
                }

                return new RuleActionExecution(() ->
                        assetAttributeValueMap.forEach((id, valueAndType) -> {
                            ValueType valueType = valueAndType.key;
                            Value value = valueAndType.value;
                            boolean skip = false;

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
                                                RulesEngine.RULES_LOG.warning("JSON Rule: Rule action missing required 'key': " + Container.JSON.writeValueAsString(attributeUpdateAction));
                                            } catch (JsonProcessingException ignored) {
                                            }
                                        }
                                    }
                                    break;
                                case DELETE:
                                    if (value == null) {
                                        skip = true;
                                    } else {
                                        if (valueType.equals(ValueType.ARRAY)) {
                                            ((ArrayValue) value).remove(attributeUpdateAction.index);
                                        } else {
                                            ((ObjectValue) value).remove(attributeUpdateAction.key);
                                        }
                                    }
                                    break;
                                case CLEAR:
                                    if (value == null) {
                                        skip = true;
                                    } else {
                                        if (valueType.equals(ValueType.ARRAY)) {
                                            value = Values.createArray();
                                        } else {
                                            value = Values.createObject();
                                        }
                                    }
                                    break;
                            }

                            if (!skip) {
                                assetsFacade.dispatch(id, attributeUpdateAction.attributeName, value);
                            }
                        }), 0);
            }
        }

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
            query.ids(target.assets.ids)
                    .name(target.assets.name)
                    .parent(target.assets.parent)
                    .path(target.assets.path)
                    .type(target.assets.type)
                    .attributes(target.assets.attribute)
                    .attributeMeta(target.assets.attributeMeta);
        }

        query.select = new BaseAssetQuery.Select(BaseAssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES);
        return assetStorageService.findAll(query).stream();
    }

    protected static Collection<String> getRuleActionTargetIds(RuleActionTarget target, boolean useUnmatched, Map<String, RuleTriggerState> triggerStateMap, Assets assetsFacade, Users usersFacade) {
        if (target != null) {
            if (!TextUtil.isNullOrEmpty(target.ruleTriggerTag) && triggerStateMap != null) {
                RuleTriggerState triggerState = triggerStateMap.get(target.ruleTriggerTag);
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
                ).collect(Collectors.toList());
            }

            return triggerStateMap.values().stream().flatMap(triggerState ->
                    triggerState != null ? triggerState.getUnmatchedAssetIds().stream() : Stream.empty()
            ).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    protected static boolean matches(RuleCondition<RuleTrigger> ruleTriggerCondition, Map<String, RuleTriggerState> triggerStateMap) {

        boolean match = false;

        if (conditionIsEmpty(ruleTriggerCondition)) {
            return false;
        }

        RuleOperator operator = ruleTriggerCondition.operator == null ? RuleOperator.AND : ruleTriggerCondition.operator;

        if (operator == RuleOperator.AND) {
            if (ruleTriggerCondition.predicates != null) {
                match = Arrays.stream(ruleTriggerCondition.predicates)
                        .map(ruleTrigger -> triggerStateMap.get(ruleTrigger.tag))
                        .allMatch(ruleTriggerState -> ruleTriggerState.lastTriggerResult != null && ruleTriggerState.lastTriggerResult.matches);

                if (!match) {
                    return false;
                }
            }

            if (ruleTriggerCondition.conditions != null) {
                match = Arrays.stream(ruleTriggerCondition.conditions)
                        .allMatch(condition -> matches(condition, triggerStateMap));
            }
        }

        if (operator == RuleOperator.OR) {
            if (ruleTriggerCondition.predicates != null) {
                match = Arrays.stream(ruleTriggerCondition.predicates)
                        .map(ruleTrigger -> triggerStateMap.get(ruleTrigger.tag))
                        .anyMatch(ruleTriggerState -> ruleTriggerState.lastTriggerResult != null && ruleTriggerState.lastTriggerResult.matches);

                if (match) {
                    return true;
                }
            }

            if (ruleTriggerCondition.conditions != null) {
                match = Arrays.stream(ruleTriggerCondition.conditions)
                        .anyMatch(condition -> matches(condition, triggerStateMap));
            }
        }

        return match;
    }

    protected static boolean ruleTriggerResetHasTimer(RuleTriggerReset reset) {
        return reset != null && (!TextUtil.isNullOrEmpty(reset.timer) || reset.timestampChanges);
    }

    protected static boolean targetIsNotAssets(RuleActionTarget target) {
        return target != null && target.assets == null && TextUtil.isNullOrEmpty(target.ruleTriggerTag);
    }
}
