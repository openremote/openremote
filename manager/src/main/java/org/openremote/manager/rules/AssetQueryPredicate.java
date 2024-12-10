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

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.rules.JsonRulesBuilder.RuleConditionState.AttributeConditionTimeState;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.filter.*;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.NameValueHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Test an {@link AttributeInfo} with a {@link AssetQuery}.
 */
public class AssetQueryPredicate implements Predicate<AttributeInfo> {

    final protected AssetQuery query;
    final protected TimerService timerService;
    final protected AssetStorageService assetStorageService;
    final protected List<String> resolvedAssetTypes;

    public AssetQueryPredicate(TimerService timerService, AssetStorageService assetStorageService, AssetQuery query) {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.query = query;

        if (query.types != null && query.types.length > 0) {
            resolvedAssetTypes = Arrays.asList(AssetQuery.getResolvedAssetTypes(query.types));
        } else {
            resolvedAssetTypes = null;
        }
    }

    @Override
    public boolean test(AttributeInfo assetState) {

        if (query.ids != null && query.ids.length > 0) {
            if (Arrays.stream(query.ids).noneMatch(id -> assetState.getId().equals(id))) {
                return false;
            }
        }

        if (query.names != null && query.names.length > 0) {
            if (Arrays.stream(query.names)
                    .map(stringPredicate -> stringPredicate.asPredicate(timerService::getCurrentTimeMillis))
                    .noneMatch(np -> np.test(assetState.getAssetName()))) {
                return false;
            }
        }

        if (query.parents != null && query.parents.length > 0) {
            if (Arrays.stream(query.parents)
                    .map(AssetQueryPredicate::asPredicate)
                    .noneMatch(np -> np.test(assetState))) {
                return false;
            }
        }

        if (resolvedAssetTypes != null && !resolvedAssetTypes.contains(assetState.getAssetType())) {
            return false;
        }

        if (query.paths != null && query.paths.length > 0) {
            if (Arrays.stream(query.paths)
                    .map(AssetQueryPredicate::asPredicate)
                    .noneMatch(np -> np.test(assetState.getPath()))) {
                return false;
            }
        }

        if (query.realm != null) {
            if (!AssetQueryPredicate.asPredicate(query.realm).test(assetState)) {
                return false;
            }
        }

        if (query.attributes != null) {
            // TODO: LogicGroup AND doesn't make much sense when applying to a single asset state
            Set<AttributeInfo> matches = asAttributeMatcher(timerService::getCurrentTimeMillis, query.attributes, null).apply(Collections.singleton(assetState));
            if (matches == null) {
                return false;
            }
        }

        // Apply user ID predicate last as it is the most expensive
        if (query.userIds != null && query.userIds.length > 0) {
            return assetStorageService.isUserAsset(Arrays.asList(query.userIds), assetState.getId());
        }

        return true;
    }

    public static Predicate<AttributeInfo> asPredicate(ParentPredicate predicate) {
        return assetState ->
            Objects.equals(predicate.id, assetState.getParentId());
    }

    public static Predicate<String[]> asPredicate(PathPredicate predicate) {
        return givenPath -> Arrays.equals(predicate.path, givenPath);
    }

    public static Predicate<AttributeInfo> asPredicate(RealmPredicate predicate) {
        return assetState ->
            predicate == null || (predicate.name != null && predicate.name.equals(assetState.getRealm()));
    }

    public static Predicate<NameValueHolder<?>> asPredicate(Supplier<Long> currentMillisSupplier, NameValuePredicate predicate) {

        Predicate<Object> namePredicate = predicate.name != null
            ? predicate.name.asPredicate(currentMillisSupplier) : str -> true;

        Predicate<Object> valuePredicate = value -> {
            if (predicate.value == null) {
                return true;
            }
            return predicate.value.asPredicate(currentMillisSupplier).test(value);
        };

        AtomicReference<Function<NameValueHolder<?>, Object>> valueExtractor = new AtomicReference<>(nameValueHolder -> nameValueHolder.getValue().orElse(null));

        if (predicate.path != null && predicate.path.getPaths().length > 0) {
            valueExtractor.set(nameValueHolder -> {
                if (!nameValueHolder.getValue().isPresent()) {
                    return null;
                }
                Object rawValue = nameValueHolder.getValue().get();

                if (!ValueUtil.isArray(rawValue.getClass()) && !ValueUtil.isObject(rawValue.getClass())) {
                    return null;
                }

                JsonNode jsonNode = ValueUtil.convert(nameValueHolder.getValue(), JsonNode.class);
                for (Object path : predicate.path.getPaths()) {
                    if (path == null) {
                        return null;
                    }
                    if (path instanceof Integer) {
                        jsonNode = jsonNode.get((int)path);
                    } else if (path instanceof String) {
                        jsonNode = jsonNode.get((String)path);
                    }
                    if (jsonNode == null) {
                        break;
                    }
                }
                return jsonNode;
            });
        }

        return nameValueHolder -> namePredicate.test(nameValueHolder.getName())
            && valuePredicate.test(valueExtractor.get().apply(nameValueHolder));
    }



  
    /**
     * A function for matching {@link AttributeInfo}s of an asset; the infos must be related to the same asset to allow
     * {@link LogicGroup.Operator#AND} to be applied.
     * @return The matched asset states or null if there is no match
     */
    @SuppressWarnings("unchecked")
    public static Function<Collection<AttributeInfo>, Set<AttributeInfo>> asAttributeMatcher(
            Supplier<Long> currentMillisProducer, 
            LogicGroup<AttributePredicate> condition, 
            AttributeConditionTimeState attributeConditionTimeState) {

        if (groupIsEmpty(condition)) {
            return as -> Collections.emptySet();
        }

        LogicGroup.Operator operator = condition.operator == null ? LogicGroup.Operator.AND : condition.operator;
        List<Function<Collection<AttributeInfo>, Set<AttributeInfo>>> assetStateMatchers = new ArrayList<>();
        Map<Integer, List<Predicate<AttributeInfo>>> attributePredicates = new HashMap<>();

        if (!condition.getItems().isEmpty()) {
            condition.getItems().stream().forEach(p -> {
                int index = condition.getItems().indexOf(p);
                attributePredicates.put(index, new ArrayList<>());
                attributePredicates.get(index).add((Predicate<AttributeInfo>) (Predicate) asPredicate(currentMillisProducer, p));

                AtomicReference<Predicate<AttributeInfo>> metaPredicate = new AtomicReference<>(nameValueHolder -> true);
                AtomicReference<Predicate<AttributeInfo>> oldValuePredicate = new AtomicReference<>(value -> true);

                if (p.meta != null) {
                    final Predicate<NameValueHolder<?>> innerMetaPredicate = Arrays.stream(p.meta)
                            .map(metaPred -> asPredicate(currentMillisProducer, metaPred))
                            .reduce(x -> true, Predicate::and);

                    metaPredicate.set(assetState -> {
                        MetaMap metaItems = (assetState).getMeta();
                        return metaItems.stream().anyMatch(metaItem -> innerMetaPredicate.test(assetState));
                    });
                    attributePredicates.get(index).add(metaPredicate.get());
                }

                if (p.previousValue != null) {
                    Predicate<Object> innerOldValuePredicate = p.previousValue.asPredicate(currentMillisProducer);
                    oldValuePredicate.set(nameValueHolder -> innerOldValuePredicate.test((nameValueHolder).getOldValue()));
                    attributePredicates.get(index).add(oldValuePredicate.get());
                }
            });
        }



        List<Predicate<AttributeInfo>> combinedAttributePredicates = new ArrayList<>();
        attributePredicates.values().forEach(combinedAttributePredicates::addAll);

        if (operator == LogicGroup.Operator.AND) {
            assetStateMatchers.add(assetStates -> {
                Set<AttributeInfo> matchedAssetStates = new HashSet<>();
                boolean durationsMet = true; 

                if (attributeConditionTimeState != null) {
                    for (int i = 0; i < attributePredicates.size(); i++) {
                        String duration = attributeConditionTimeState.durationMap().get(i);
        
                        if (duration == null) {
                            continue; // skip to the next iteration if there is no duration for the predicate group
                        }
                        Long durationMillis = TimeUtil.parseTimeDuration(duration);

                        // check whether the predicates are all a match
                        boolean predicateMatch = attributePredicates.get(i).stream().allMatch(attributePredicate -> {
                            return assetStates.stream().filter(attributePredicate).findFirst().map(matchedAssetState -> {
                                matchedAssetStates.add(matchedAssetState);
                                return true;
                            }).orElse(false);
                        });

                        // if the predicate is a match then set the match start time
                        if (predicateMatch) {
                            if (attributeConditionTimeState.matchStartTimes().get(i) == null) { // set the match start time if it is not already set
                                long currentTime = currentMillisProducer.get();
                                attributeConditionTimeState.matchStartTimes().put(i, currentTime);
                            }

                            if (currentMillisProducer.get() - attributeConditionTimeState.matchStartTimes().get(i) < durationMillis) {
                                durationsMet = false; 
                            }  
                        } else {
                            attributeConditionTimeState.matchStartTimes().remove(i);
                        }
                    }   
                }
   
                boolean allPredicatesMatch = combinedAttributePredicates.stream().allMatch(attributePredicate -> {
                    // Find the first match as an attribute predicate shouldn't match more than one asset state
                    return assetStates.stream().filter(attributePredicate).findFirst().map(matchedAssetState -> {
                        matchedAssetStates.add(matchedAssetState);
                        return true;
                    }).orElse(false);
                });

                return  allPredicatesMatch && durationsMet ? matchedAssetStates : null;
            });
        } else {
            assetStateMatchers.add(assetStates -> {
                AtomicReference<AttributeInfo> firstMatch = new AtomicReference<>();
                boolean anyPredicateMatch = combinedAttributePredicates.stream().anyMatch(attributePredicate -> {
                    // Find the first match as an attribute predicate shouldn't match more than one asset state
                    return assetStates.stream().filter(attributePredicate).findFirst().map(matchedAssetState -> {
                        firstMatch.set(matchedAssetState);
                        return true;
                    }).orElse(false);
                });
                return anyPredicateMatch ? Collections.singleton(firstMatch.get()) : null;
            });
        }

        if (condition.groups != null && condition.groups.size() > 0) {
            assetStateMatchers.addAll(
                condition.groups.stream()
                    .map(c -> asAttributeMatcher(currentMillisProducer, c, attributeConditionTimeState)).toList()
            );
        }

        return assetStates -> {
            Set<AttributeInfo> matchedStates = new HashSet<>();

            for (Function<Collection<AttributeInfo>, Set<AttributeInfo>> matcher : assetStateMatchers) {
                Set<AttributeInfo> matcherMatchedStates = matcher.apply(assetStates);

                if (matcherMatchedStates != null) {
                    // We have a match
                    if (operator == LogicGroup.Operator.OR) {
                        return matcherMatchedStates;
                    }
                    matchedStates.addAll(matcherMatchedStates);
                } else {
                    // No match
                    if (operator == LogicGroup.Operator.AND) {
                        return null;
                    }
                }
            }

            return operator == LogicGroup.Operator.OR ? null : matchedStates;
        };
    }

    protected static boolean groupIsEmpty(LogicGroup<?> condition) {
        return condition.getItems().size() == 0
            && (condition.groups == null || condition.groups.isEmpty());
    }
}
