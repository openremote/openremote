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
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.filter.*;
import org.openremote.model.rules.AssetState;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaHolder;
import org.openremote.model.value.NameValueHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Test an {@link AssetState} with a {@link AssetQuery}.
 */
public class AssetQueryPredicate implements Predicate<AssetState<?>> {

    final protected AssetQuery query;
    final protected TimerService timerService;
    final protected AssetStorageService assetStorageService;

    public AssetQueryPredicate(TimerService timerService, AssetStorageService assetStorageService, AssetQuery query) {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.query = query;
    }

    @Override
    public boolean test(AssetState<?> assetState) {

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

        if (query.types != null && query.types.length > 0) {
            if (Arrays.stream(query.types).noneMatch(type ->
                        type.isAssignableFrom(
                            ValueUtil.getAssetDescriptor(assetState.getAssetType())
                                .orElse(ThingAsset.DESCRIPTOR).getType()))
                    ) {
                return false;
            }
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
            if (!asPredicate(timerService::getCurrentTimeMillis, query.attributes).test(assetState)) {
                return false;
            }
        }

        // Apply user ID predicate last as it is the most expensive
        if (query.userIds != null && query.userIds.length > 0) {
            return assetStorageService.isUserAsset(Arrays.asList(query.userIds), assetState.getId());
        }

        return true;
    }

    public static Predicate<AssetState<?>> asPredicate(ParentPredicate predicate) {
        return assetState ->
            Objects.equals(predicate.id, assetState.getParentId());
    }

    public static Predicate<String[]> asPredicate(PathPredicate predicate) {
        return givenPath -> Arrays.equals(predicate.path, givenPath);
    }

    public static Predicate<AssetState<?>> asPredicate(RealmPredicate predicate) {
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

    @SuppressWarnings("unchecked")
    public static Predicate<AssetState<?>> asPredicate(Supplier<Long> currentMillisProducer, LogicGroup<AttributePredicate> condition) {
        if (groupIsEmpty(condition)) {
            return as -> true;
        }

        LogicGroup.Operator operator = condition.operator == null ? LogicGroup.Operator.AND : condition.operator;

        List<Predicate<AssetState<?>>> assetStatePredicates = new ArrayList<>();

        if (condition.getItems().size() > 0) {

            condition.getItems().stream()
                .forEach(p -> {
                    assetStatePredicates.add((Predicate<AssetState<?>>)(Predicate)asPredicate(currentMillisProducer, p));

                    AtomicReference<Predicate<AssetState<?>>> metaPredicate = new AtomicReference<>(nameValueHolder -> true);
                    AtomicReference<Predicate<AssetState<?>>> oldValuePredicate = new AtomicReference<>(value -> true);

                    if (p.meta != null) {
                        final Predicate<NameValueHolder<?>> innerMetaPredicate = Arrays.stream(p.meta)
                            .map(metaPred -> asPredicate(currentMillisProducer, metaPred))
                            .reduce(x->true, Predicate::and);

                        metaPredicate.set(assetState -> {
                            MetaMap metaItems = ((MetaHolder)assetState).getMeta();
                            return metaItems.stream().anyMatch(metaItem ->
                                innerMetaPredicate.test(assetState)
                            );
                        });
                        assetStatePredicates.add(metaPredicate.get());
                    }

                    if (p.previousValue != null) {
                        Predicate<Object> innerOldValuePredicate = p.previousValue.asPredicate(currentMillisProducer);
                        oldValuePredicate.set(nameValueHolder -> innerOldValuePredicate.test((nameValueHolder).getOldValue()));
                        assetStatePredicates.add(oldValuePredicate.get());
                    }
                });
        }

        if (condition.groups != null && condition.groups.size() > 0) {
            assetStatePredicates.addAll(
                condition.groups.stream()
                            .map(c -> asPredicate(currentMillisProducer, c)).collect(Collectors.toList())
            );
        }

        return asPredicate(assetStatePredicates, operator);
    }

    protected static boolean groupIsEmpty(LogicGroup<?> condition) {
        return condition.getItems().size() == 0
            && (condition.groups == null || condition.groups.isEmpty());
    }

    protected static <T> Predicate<T> asPredicate(Collection<Predicate<T>> predicates, LogicGroup.Operator operator) {
        return in -> {
            boolean matched = false;

            for (Predicate<T> p : predicates) {

                if (p.test(in)) {
                    matched = true;

                    if (operator == LogicGroup.Operator.OR) {
                        break;
                    }
                } else {
                    matched = false;

                    if (operator == LogicGroup.Operator.AND) {
                        break;
                    }
                }
            }

            return matched;
        };
    }

}
