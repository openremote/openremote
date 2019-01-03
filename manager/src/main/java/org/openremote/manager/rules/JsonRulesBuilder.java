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

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.rules.facade.NotificationsFacade;
import org.openremote.model.attribute.Meta;
import org.openremote.model.notification.Notification;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.AttributeMetaPredicate;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Users;
import org.openremote.model.rules.json.*;
import org.openremote.model.rules.json.predicate.AssetPredicate;
import org.openremote.model.rules.json.predicate.AttributePredicate;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class JsonRulesBuilder extends RulesBuilder {

    static class RuleFiredInfo {
        AssetState firedAssetState;

        RuleFiredInfo(AssetState firedAssetState) {
            this.firedAssetState = firedAssetState;
        }
    }

    static class Targets {
        public TargetType type;
        public List<String> ids;

        public Targets(TargetType type, List<String> ids) {
            this.type = type;
            this.ids = ids;
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

    public JsonRulesBuilder(TimerService timerService, AssetStorageService assetStorageService, ManagerExecutorService executorService, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationFacade, BiConsumer<Runnable, Long> scheduledActionConsumer) {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.executorService = executorService;
        this.assetsFacade = assetsFacade;
        this.usersFacade = usersFacade;
        this.notificationFacade = notificationFacade;
        this.scheduledActionConsumer = scheduledActionConsumer;
    }

    protected static Predicate<RulesFacts> asPredicate(TimerService timerService, AssetStorageService assetStorageService, RuleCondition<AssetPredicate> condition) {

        if ((condition.predicates == null || condition.predicates.length == 0)
                && (condition.conditions == null || condition.conditions.length == 0)) {
            return facts -> true;
        }

        RuleOperator operator = condition.operator == null ? RuleOperator.AND : condition.operator;

        List<Predicate<RulesFacts>> assetPredicates = new ArrayList<>();

        if (condition.predicates != null && condition.predicates.length > 0) {
            assetPredicates.addAll(
                    Arrays.stream(condition.predicates)
                            .map(p -> {
                                Predicate<AssetState> assetStatePredicate = asPredicate(timerService, assetStorageService, p);
                                return (Predicate<RulesFacts>) facts -> {
                                    List<AssetState> assetStates = new ArrayList<>(facts.getAssetStates());
                                    assetStates.removeIf(as -> assetStatePredicate.negate().test(as));
                                    return !assetStates.isEmpty();
                                };
                            }).collect(Collectors.toList())
            );
        }

        if (condition.conditions != null && condition.conditions.length > 0) {
            assetPredicates.addAll(
                    Arrays.stream(condition.conditions)
                            .map(c -> asPredicate(timerService, assetStorageService, c)).collect(Collectors.toList())
            );
        }

        return asPredicate(assetPredicates, operator);
    }

    protected static Predicate<AssetState> asPredicate(Supplier<Long> currentMillisProducer, RuleCondition<AttributePredicate> condition) {
        if ((condition.predicates == null || condition.predicates.length == 0)
                && (condition.conditions == null || condition.conditions.length == 0)) {
            return as -> true;
        }

        RuleOperator operator = condition.operator == null ? RuleOperator.AND : condition.operator;

        List<Predicate<AssetState>> assetStatePredicates = new ArrayList<>();

        if (condition.predicates != null && condition.predicates.length > 0) {
            assetStatePredicates.addAll(
                    Arrays.stream(condition.predicates)
                            .map(p -> asPredicate(currentMillisProducer, p)).collect(Collectors.toList())
            );
        }

        if (condition.conditions != null && condition.conditions.length > 0) {
            assetStatePredicates.addAll(
                    Arrays.stream(condition.conditions)
                            .map(c -> asPredicate(currentMillisProducer, c)).collect(Collectors.toList())
            );
        }

        return asPredicate(assetStatePredicates, operator);
    }

    public static Predicate<AssetState> asPredicate(TimerService timerService, AssetStorageService assetStorageService, AssetPredicate pred) {

        return assetState -> {
            if (pred.ids != null && pred.ids.length > 0) {
                if (Arrays.stream(pred.ids).noneMatch(id -> assetState.getId().equals(id))) {
                    return false;
                }
            }

            if (pred.names != null && pred.names.length > 0) {
                if (Arrays.stream(pred.names)
                        .map(AssetQueryPredicate::asPredicate)
                        .noneMatch(np -> np.test(assetState.getName()))) {
                    return false;
                }
            }

            if (pred.parents != null && pred.parents.length > 0) {
                if (Arrays.stream(pred.parents)
                        .map(AssetQueryPredicate::asPredicate)
                        .noneMatch(np -> np.test(assetState))) {
                    return false;
                }
            }

            if (pred.types != null && pred.types.length > 0) {
                if (Arrays.stream(pred.types)
                        .map(AssetQueryPredicate::asPredicate)
                        .noneMatch(np -> np.test(assetState.getTypeString()))) {
                    return false;
                }
            }

            if (pred.paths != null && pred.paths.length > 0) {
                if (Arrays.stream(pred.paths)
                        .map(AssetQueryPredicate::asPredicate)
                        .noneMatch(np -> np.test(assetState.getPath()))) {
                    return false;
                }
            }

            if (pred.tenant != null) {
                if (!AssetQueryPredicate.asPredicate(pred.tenant).test(assetState)) {
                    return false;
                }
            }

            if (pred.attributes != null) {
                if (!asPredicate(timerService::getCurrentTimeMillis, pred.attributes).test(assetState)) {
                    return false;
                }
            }

            // Apply user ID predicate last as it is the most expensive
            if (pred.userIds != null && pred.userIds.length > 0) {
                if (!assetStorageService.isUserAsset(Arrays.asList(pred.userIds), assetState.getId())) {
                    return false;
                }
            }

            return true;
        };
    }

    public static Predicate<AssetState> asPredicate(Supplier<Long> currentMillsProducer, AttributePredicate pred) {

        Predicate<String> namePredicate = pred.name != null
                ? AssetQueryPredicate.asPredicate(pred.name) : str -> true;

        Predicate<Meta> metaPredicate = meta -> {

            if (pred.meta == null || pred.meta.length == 0) {
                return true;
            }

            for (AttributeMetaPredicate p : pred.meta) {
                if (!AssetQueryPredicate.asPredicate(currentMillsProducer, p).test(meta)) {
                    return false;
                }
            }
            return true;
        };

        Predicate<Value> valuePredicate = value -> {
            if (pred.value == null) {
                return true;
            }
            return AssetQueryPredicate.asPredicate(currentMillsProducer, pred.value).test(value);
        };

        Predicate<Value> oldValuePredicate = value -> {
            if (pred.lastValue == null) {
                return true;
            }
            return AssetQueryPredicate.asPredicate(currentMillsProducer, pred.lastValue).test(value);
        };

        return assetState -> namePredicate.test(assetState.getAttributeName())
                && metaPredicate.test(assetState.getMeta())
                && valuePredicate.test(assetState.getValue().orElse(null))
                && oldValuePredicate.test(assetState.getOldValue().orElse(null));
    }

    public static Comparator<AssetState> asComparator(AssetPredicate.MatchOrder matchOrder) {

        Function<AssetState, String> keyExtractor = AssetState::getName;
        boolean reverse = false;

        switch (matchOrder) {
            case NAME_REVERSED:
                reverse = true;
            case NAME:
                keyExtractor = AssetState::getName;
                break;
            case ATTRIBUTE_REVERSE:
                reverse = true;
            case ATTRIBUTE:
                keyExtractor = AssetState::getAttributeName;
                break;
            case NAME_AND_ATTRIBUTE_REVERSED:
                reverse = true;
            case NAME_AND_ATTRIBUTE:
                keyExtractor = as -> as.getName() + as.getAttributeName();
                break;
            case ATTRIBUTE_AND_NAME_REVERSED:
                reverse = true;
            case ATTRIBUTE_AND_NAME:
                keyExtractor = as -> as.getAttributeName() + as.getName();
                break;
        }

        Comparator<AssetState> comparator = Comparator.comparing(keyExtractor);

        if (reverse) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    protected static <T> Predicate<T> asPredicate(List<Predicate<T>> predicates, RuleOperator operator) {
        return in -> {
            boolean matched = false;

            for (Predicate<T> p : predicates) {

                if (p.test(in)) {
                    matched = true;

                    if (operator == RuleOperator.OR) {
                        break;
                    }
                } else {
                    matched = false;

                    if (operator == RuleOperator.AND) {
                        break;
                    }
                }
            }

            return matched;
        };
    }

    protected static String buildResetFactName(Rule rule, AssetState assetState) {
        return rule.name + "_" + assetState.getId() + "_" + assetState.getAttributeName();
    }

    protected static List<String> getUserIds(Users users, UserQuery userQuery) {
        return users.query()
                .tenant(userQuery.tenantPredicate)
                .assetPath(userQuery.pathPredicate)
                .asset(userQuery.assetPredicate)
                .limit(userQuery.limit)
                .getResults();
    }

    protected static List<String> getAssetIds(Assets assets, AssetQuery assetQuery) {
        return assets.query()
                .ids(assetQuery.ids)
                .name(assetQuery.name)
                .parent(assetQuery.parent)
                .path(assetQuery.path)
                .type(assetQuery.type)
                .attributes(assetQuery.attribute)
                .attributeMeta(assetQuery.attributeMeta)
                .stream()
                .collect(Collectors.toList());
    }

    public JsonRulesBuilder add(Rule rule) {

        final Predicate<AssetState> assetStatePredicate = rule.when != null && rule.when.asset != null
                ? asPredicate(timerService, assetStorageService, rule.when.asset) : null;

        Condition condition = buildLhsCondition(rule, assetStatePredicate);
        Action action = buildAction(rule);

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

        Condition resetCondition = buildResetCondition(rule, assetStatePredicate);
        Action resetAction = buildResetAction();

        if (resetCondition != null && resetAction != null) {
            add().name(rule.name + " RESET")
                    .description(rule.description + " Reset")
                    .priority(rule.priority + 1)
                    .when(facts -> {
                        Object result;
                        try {
                            result = resetCondition.evaluate(facts);
                        } catch (Exception ex) {
                            throw new RuntimeException("Error evaluating condition of rule '" + rule.name + " RESET': " + ex.getMessage(), ex);
                        }
                        if (result instanceof Boolean) {
                            return result;
                        } else {
                            throw new IllegalArgumentException("Error evaluating condition of rule '" + rule.name + " RESET': result is not boolean but " + result);
                        }
                    })
                    .then(resetAction);
        }

        return this;
    }

    protected Condition buildLhsCondition(Rule rule, Predicate<AssetState> assetStatePredicate) {
        if (rule.when == null || (rule.when.asset == null && rule.when.timer == null)) {
            return facts -> false;
        }

        Predicate<RulesFacts> whenPredicate = facts -> false;
        Predicate<RulesFacts> andPredicate = facts -> true;

        if (rule.when.asset != null) {


            whenPredicate = facts -> {

                List<AssetState> assetStates = new ArrayList<>(facts.getAssetStates());

                assetStates.removeIf(as -> assetStatePredicate.negate().test(as));

                if (assetStates.isEmpty()) {
                    return false;
                }

                // Apply reset predicate (to prevent re-running a rule on an asset state before the reset has triggered)
                assetStates.removeIf(as -> {
                    String factName = buildResetFactName(rule, as);
                    return facts.getOptional(factName).isPresent();
                });
                if (assetStates.isEmpty()) {
                    return false;
                }

                if (rule.when.asset.matchOrder != null) {
                    assetStates.sort(asComparator(rule.when.asset.matchOrder));
                }

                if (rule.when.asset.matchLimit > 0) {
                    int limit = Math.min(assetStates.size(), rule.when.asset.matchLimit);
                    assetStates = assetStates.subList(0, limit);
                }

                // Push matched asset states into RHS
                facts.bind("assetStates", assetStates);

                if (!assetStates.isEmpty()) {
                    // Push matched asset states into RHS
                    facts.bind("assetStates", assetStates);
                    return true;
                }

                return false;
            };
        } else if (!TextUtil.isNullOrEmpty(rule.when.timer)) {
            // TODO: Create timer condition
        }

        if (rule.and != null) {
            andPredicate = asPredicate(timerService, assetStorageService, rule.and);
        }


        Predicate<RulesFacts> finalWhenPredicate = whenPredicate;
        Predicate<RulesFacts> finalAndPredicate = andPredicate;

        return facts -> {
            if (!finalWhenPredicate.test(facts)) {
                return false;
            }

            if (!finalAndPredicate.test(facts)) {
                return false;
            }

            return true;
        };
    }

    protected Action buildAction(Rule rule) {

        return facts -> {

            List<AssetState> assetStates = facts.bound("assetStates");

            if (assetStates != null) {
                for (AssetState assetState : assetStates) {

                    // Add RuleFiredInfo fact to limit re-triggering of the rule for a given asset state
                    if (rule.reset == null || TextUtil.isNullOrEmpty(rule.reset.timer)) {
                        facts.put(buildResetFactName(rule, assetState), new RuleFiredInfo(assetState));
                    } else if (!TextUtil.isNullOrEmpty(rule.reset.timer)) {
                        facts.putTemporary(buildResetFactName(rule, assetState), rule.reset.timer, new RuleFiredInfo(assetState));
                    }
                }
            }

            if (rule.then == null) {
                return;
            }

            long delay = 0;

            for (RuleAction ruleAction : rule.then) {

                Runnable action = null;

                if (ruleAction instanceof RuleActionNotification) {

                    RuleActionNotification notificationAction = (RuleActionNotification) ruleAction;

                    if (notificationAction.notification != null) {

                        // Override the notification targets if set in the rule
                        if (notificationAction.target != null) {
                            RuleActionWithTarget.Target target = notificationAction.target;
                            Notification.TargetType targetType = Notification.TargetType.ASSET;
                            List<String> ids = null;

                            if (target.useAssetsFromWhen && assetStates != null) {
                                ids = assetStates.stream().map(AssetState::getId).collect(Collectors.toList());
                            } else if (target.assets != null) {
                                ids = getAssetIds(assetsFacade, target.assets);
                            } else if (target.users != null) {
                                targetType = Notification.TargetType.USER;
                                ids = getUserIds(usersFacade, target.users);
                            }

                            if (ids != null) {
                                notificationAction.notification.setTargets(
                                        new Notification.Targets(
                                                targetType,
                                                ids));
                            }
                        }

                        action = () -> notificationFacade.send(notificationAction.notification);
                    }
                } else if (ruleAction instanceof RuleActionWriteAttribute) {

                    RuleActionWriteAttribute attributeAction = (RuleActionWriteAttribute) ruleAction;

                    if (!TextUtil.isNullOrEmpty(attributeAction.attributeName)) {

                        RuleActionWithTarget.Target target = attributeAction.target;
                        List<String> ids = null;

                        if (target != null) {

                            // Only assets make sense as the target
                            if (target.useAssetsFromWhen && assetStates != null) {
                                ids = assetStates.stream().map(AssetState::getId).collect(Collectors.toList());
                            } else if (target.assets != null) {
                                ids = getAssetIds(assetsFacade, target.assets);
                            }
                        }

                        if (ids != null) {
                            List<String> finalIds = ids;
                            action = () ->
                                    finalIds.forEach(id ->
                                        assetsFacade.dispatch(id, attributeAction.attributeName, attributeAction.value));
                        }
                    }
                } else if (ruleAction instanceof RuleActionWait) {
                    long millis = ((RuleActionWait) ruleAction).millis;
                    if (millis > 0) {
                        delay += millis;
                    }
                }

                if (action != null) {
                    if (delay > 0) {
                        scheduledActionConsumer.accept(action, delay);
                    } else {
                        action.run();
                    }
                }
            }

        };
    }

    protected Condition buildResetCondition(Rule rule, Predicate<AssetState> assetStatePredicate) {

        // Timer only reset is handled in rule action with temporary fact other resets require rule.when.asset to be set
        // so that we have
        if (rule.reset == null || assetStatePredicate == null
                || (!rule.reset.triggerNoLongerMatches && !rule.reset.attributeTimestampChange
                && !rule.reset.attributeValueChange)) {

            return null;
        }

        RuleTriggerReset reset = rule.reset;

        Predicate<AssetState> noLongerMatchesPredicate = ruleFiredInfo -> false;
        BiPredicate<AssetState, RuleFiredInfo> attributeTimestampPredicate = (assetState, ruleFiredInfo) -> false;
        BiPredicate<AssetState, RuleFiredInfo> attributeValuePredicate = (assetState, ruleFiredInfo) -> false;

        if (reset.triggerNoLongerMatches) {
            noLongerMatchesPredicate = assetState -> assetStatePredicate.negate().test(assetState);
        }

        if (reset.attributeTimestampChange) {
            attributeTimestampPredicate = (assetState, ruleFiredInfo) -> {
                long firedTimestamp = ruleFiredInfo.firedAssetState.getTimestamp();
                long currentTimestamp = assetState.getTimestamp();
                return firedTimestamp != currentTimestamp;
            };
        }

        if (reset.attributeValueChange) {
            attributeValuePredicate = (assetState, ruleFiredInfo) -> {
                Value firedValue = ruleFiredInfo.firedAssetState.getValue().orElse(null);
                Value currentValue = assetState.getValue().orElse(null);
                return !Objects.equals(firedValue, currentValue);
            };
        }

        Predicate<AssetState> finalNoLongerMatchesPredicate = noLongerMatchesPredicate;
        BiPredicate<AssetState, RuleFiredInfo> finalAttributeTimestampPredicate = attributeTimestampPredicate;
        BiPredicate<AssetState, RuleFiredInfo> finalAttributeValuePredicate = attributeValuePredicate;

        BiPredicate<AssetState, RuleFiredInfo> resetPredicate = (assetState, ruleFiredInfo) ->
                finalNoLongerMatchesPredicate.test(assetState)
                        || finalAttributeTimestampPredicate.test(assetState, ruleFiredInfo)
                        || finalAttributeValuePredicate.test(assetState, ruleFiredInfo);

        return facts -> {
            List<String> resetAssetStates = facts.getAssetStates()
                    .stream()
                    .filter(as -> {

                        String factName = buildResetFactName(rule, as);
                        Optional<RuleFiredInfo> firedInfo = facts.getOptional(factName);

                        return firedInfo.filter(ruleFiredInfo -> resetPredicate.test(as, ruleFiredInfo)).isPresent();

                    })
                    .map(as -> buildResetFactName(rule, as))
                    .collect(Collectors.toList());

            if (resetAssetStates.isEmpty()) {
                return false;
            }

            facts.bind("resetAssetStates", resetAssetStates);
            return true;
        };
    }

    protected Action buildResetAction() {
        return facts -> {
            List<String> resetAssets = facts.bound("resetAssetStates");
            if (resetAssets != null) {
                resetAssets.forEach(facts::remove);
            }
        };
    }
}
