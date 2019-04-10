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
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.notification.Notification;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Users;
import org.openremote.model.rules.json.*;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    protected static List<String> getAssetIds(Assets assets, org.openremote.model.query.AssetQuery assetQuery) {
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

        Condition condition = buildLhsCondition(rule);
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

        Condition resetCondition = buildResetCondition(rule);
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

    protected Condition buildLhsCondition(Rule rule) {
        if (rule.when == null || (rule.when.asset == null && rule.when.timer == null)) {
            return facts -> false;
        }

        Predicate<RulesFacts> whenPredicate = facts -> false;
        Predicate<RulesFacts> andPredicate = facts -> true;

        if (rule.when.asset != null) {
            // Pull out order and limit so they can be applied after filtering already triggered asset states
            BaseAssetQuery.OrderBy orderBy = rule.when.asset.orderBy;
            int limit = rule.when.asset.limit;
            rule.when.asset.orderBy = null;
            rule.when.asset.limit = 0;

            whenPredicate = facts -> {

                Stream<AssetState> assetStates = facts.matchAssetState(rule.when.asset);

                // Apply reset predicate (to prevent re-running a rule on an asset state before the reset has triggered)
                assetStates = assetStates.filter(as -> {
                    String factName = buildResetFactName(rule, as);
                    return !facts.getOptional(factName).isPresent();
                });

                if (orderBy != null) {
                    assetStates = assetStates.sorted(RulesFacts.asComparator(rule.when.asset.orderBy));
                }

                if (limit > 0) {
                    assetStates = assetStates.limit(limit);
                }

                List<AssetState> assetStateList = assetStates.collect(Collectors.toList());

                if (!assetStateList.isEmpty()) {
                    // Push matched asset states into RHS
                    facts.bind("assetStates", assetStateList);
                    return true;
                }

                return false;
            };
        } else if (!TextUtil.isNullOrEmpty(rule.when.timer)) {
            // TODO: Create timer condition
        }

        if (rule.and != null) {
            andPredicate = AssetQueryPredicate.asPredicate(timerService, assetStorageService, rule.and);
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
                } else if (ruleAction instanceof RuleActionUpdateAttribute) {
                    RuleActionUpdateAttribute attributeUpdateAction = (RuleActionUpdateAttribute) ruleAction;

                    if (!TextUtil.isNullOrEmpty(attributeUpdateAction.attributeName)) {
                        RuleActionWithTarget.Target target = attributeUpdateAction.target;

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
                                finalIds.forEach(id -> {
                                    ValueType valueType = null;
                                    Value currentValue = null;
                                    if (assetStates != null) {
                                        Optional<AssetState> assetState = assetStates
                                            .stream()
                                            .filter(state -> state.getId().equals(id) && state.getAttributeName().equals(attributeUpdateAction.attributeName))
                                            .findFirst();
                                        if (assetState.isPresent()) {
                                            valueType = assetState.get().getAttributeValueType().getValueType();
                                            if (valueType.equals(ValueType.ARRAY)) {
                                                currentValue = assetState.get().getValue().orElse(Values.createArray());
                                            } else if (valueType.equals(ValueType.OBJECT)) {
                                                currentValue = assetState.get().getValue().orElse(Values.createObject());
                                            } else {
                                                throw new IllegalArgumentException("Only Attributes of type ArrayValue or ObjectValue are allowed for RuleActionUpdateAttribute");
                                            }
                                        }
                                    }

                                    if (valueType == null || currentValue == null) {
                                        Asset asset = assetStorageService.find(new AssetQuery().select(new BaseAssetQuery.Select(BaseAssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES)).id(id).attributeName(attributeUpdateAction.attributeName));
                                        if (asset != null) {
                                            valueType = asset.getAttribute(attributeUpdateAction.attributeName).flatMap(Attribute::getType).get().getValueType();
                                            if (valueType.equals(ValueType.ARRAY)) {
                                                currentValue = asset.getAttribute(attributeUpdateAction.attributeName).flatMap(AbstractValueHolder::getValueAsArray).orElse(Values.createArray());
                                            } else if (valueType.equals(ValueType.OBJECT)) {
                                                currentValue = asset.getAttribute(attributeUpdateAction.attributeName).flatMap(AbstractValueHolder::getValueAsObject).orElse(Values.createObject());
                                            } else {
                                                throw new IllegalArgumentException("Only Attributes of type ArrayValue or ObjectValue are allowed for RuleActionUpdateAttribute");
                                            }
                                        }
                                    }

                                    if (valueType != null && currentValue != null) {

                                        switch (attributeUpdateAction.updateAction) {
                                            case ADD:
                                                if (valueType.equals(ValueType.ARRAY)) {
                                                    ((ArrayValue) currentValue).add(attributeUpdateAction.value);
                                                } else {
                                                    ((ObjectValue) currentValue).put(attributeUpdateAction.key, attributeUpdateAction.value);
                                                }
                                                break;
                                            case ADD_OR_REPLACE:
                                            case REPLACE:
                                                if (valueType.equals(ValueType.ARRAY)) {
                                                    ((ArrayValue) currentValue).set(attributeUpdateAction.index, attributeUpdateAction.value);
                                                } else {
                                                    ((ObjectValue) currentValue).put(attributeUpdateAction.key, attributeUpdateAction.value);
                                                }
                                                break;
                                            case DELETE:
                                                if (valueType.equals(ValueType.ARRAY)) {
                                                    ((ArrayValue) currentValue).remove(attributeUpdateAction.index);
                                                } else {
                                                    ((ObjectValue) currentValue).remove(attributeUpdateAction.key);
                                                }
                                                break;
                                            case CLEAR:
                                                if (valueType.equals(ValueType.ARRAY)) {
                                                    currentValue = Values.createArray();
                                                } else {
                                                    currentValue = Values.createObject();
                                                }
                                                break;
                                        }
                                        assetsFacade.dispatch(id, attributeUpdateAction.attributeName, currentValue);
                                    }
                                });
                        }
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

    protected Condition buildResetCondition(Rule rule) {

        // Timer only reset is handled in rule action with temporary fact other resets require rule.when.asset to be set
        // so that we have
        if (rule.reset == null || (!rule.reset.triggerNoLongerMatches && !rule.reset.attributeTimestampChange
                && !rule.reset.attributeValueChange)) {

            return null;
        }

        RuleTriggerReset reset = rule.reset;

        Predicate<AssetState> noLongerMatchesPredicate = ruleFiredInfo -> false;
        BiPredicate<AssetState, RuleFiredInfo> attributeTimestampPredicate = (assetState, ruleFiredInfo) -> false;
        BiPredicate<AssetState, RuleFiredInfo> attributeValuePredicate = (assetState, ruleFiredInfo) -> false;

        if (reset.triggerNoLongerMatches && rule.when != null && rule.when.asset != null) {
            noLongerMatchesPredicate = new AssetQueryPredicate(timerService, assetStorageService, rule.when.asset).negate();
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
