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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.geotools.referencing.GeodeticCalculator;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.attribute.Meta;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.AssetQuery.NumberType;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.filter.*;
import org.openremote.model.rules.AssetState;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Test an {@link AssetState} with a {@link AssetQuery}.
 */
public class AssetQueryPredicate implements Predicate<AssetState> {

    final protected AssetQuery query;
    final protected TimerService timerService;
    final protected AssetStorageService assetStorageService;

    public AssetQueryPredicate(TimerService timerService, AssetStorageService assetStorageService, AssetQuery query) {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.query = query;
    }

    @Override
    public boolean test(AssetState assetState) {

        if (query.ids != null && query.ids.length > 0) {
            if (Arrays.stream(query.ids).noneMatch(id -> assetState.getId().equals(id))) {
                return false;
            }
        }

        if (query.names != null && query.names.length > 0) {
            if (Arrays.stream(query.names)
                    .map(StringPredicate::asPredicate)
                    .noneMatch(np -> np.test(assetState.getName()))) {
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
            if (Arrays.stream(query.types)
                    .map(StringPredicate::asPredicate)
                    .noneMatch(np -> np.test(assetState.getTypeString()))) {
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

        if (query.tenant != null) {
            if (!AssetQueryPredicate.asPredicate(query.tenant).test(assetState)) {
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
            if (!assetStorageService.isUserAsset(Arrays.asList(query.userIds), assetState.getId())) {
                return false;
            }
        }

        return true;
    }

    public static Predicate<ArrayValue> asPredicate(ArrayPredicate predicate) {
        return arrayValue ->  {
            if (arrayValue == null) {
                return false;
            }

            boolean result = true;

            if (predicate.value != null) {
                if (predicate.index != null) {
                    result = arrayValue.length() >= predicate.index && Objects.equals(arrayValue.get(predicate.index).orElse(null), predicate.value);
                } else {
                    result = arrayValue.stream().anyMatch(av -> Objects.equals(av, predicate.value));
                }
            }

            if (result && predicate.lengthEquals != null) {
                result = arrayValue.length() == predicate.lengthEquals;
            }
            if (result && predicate.lengthGreaterThan != null) {
                result = arrayValue.length() > predicate.lengthGreaterThan;
            }
            if (result && predicate.lengthEquals != null) {
                result = arrayValue.length() < predicate.lengthLessThan;
            }
            if (predicate.negated) {
                return !result;
            }
            return result;
        };
    }

    public static Predicate<ObjectValue> asPredicate(ObjectValueKeyPredicate predicate) {
        return objectValue -> {
            if (objectValue == null) {
                return predicate.negated;
            }

            boolean result = objectValue.hasKey(predicate.key);
            if (predicate.negated) {
                return !result;
            }
            return result;
        };
    }

    public static Predicate<Boolean> asPredicate(BooleanPredicate predicate) {
        return b -> {
            // If given a null, we assume it's false!
            if (b == null)
                b = false;
            return b == predicate.value;
        };
    }

    public static Predicate<String[]> asPredicate(StringArrayPredicate predicate) {
        return strings -> {
            if (strings == null && predicate.predicates == null)
                return true;
            if (strings == null)
                return false;
            if (predicate.predicates == null)
                return false;
            if (strings.length != predicate.predicates.length)
                return false;
            for (int i = 0; i < predicate.predicates.length; i++) {
                StringPredicate p = predicate.predicates[i];
                if (!StringPredicate.asPredicate(p).test(strings[i]))
                    return false;
            }
            return true;
        };
    }

    public static Predicate<Long> asPredicate(Supplier<Long> currentMillisProducer, DateTimePredicate predicate) {
        return timestamp -> {
            Pair<Long, Long> fromAndTo = asFromAndTo(currentMillisProducer.get(), predicate);

            if (fromAndTo.key == null) {
                throw new IllegalArgumentException("Date time predicate 'value' is not valid: " + predicate);
            }

            switch (predicate.operator) {

                case EQUALS:
                    if (predicate.negate) {
                        return timestamp != fromAndTo.key.longValue();
                    }
                    return timestamp == fromAndTo.key.longValue();
                case GREATER_THAN:
                    if (predicate.negate) {
                        return timestamp <= fromAndTo.key;
                    }
                    return timestamp > fromAndTo.key;
                case GREATER_EQUALS:
                    if (predicate.negate) {
                        return timestamp < fromAndTo.key;
                    }
                    return timestamp >= fromAndTo.key;
                case LESS_THAN:
                    if (predicate.negate) {
                        return timestamp >= fromAndTo.key;
                    }
                    return timestamp < fromAndTo.key;
                case LESS_EQUALS:
                    if (predicate.negate) {
                        return timestamp > fromAndTo.key;
                    }
                    return timestamp <= fromAndTo.key;
                case BETWEEN:
                    if (fromAndTo.value == null) {
                        throw new IllegalArgumentException("Date time predicate 'rangeValue' is not valid: " + predicate);
                    }
                    if (predicate.negate) {
                        return !(timestamp > fromAndTo.key && timestamp < fromAndTo.value);
                    }
                    return timestamp > fromAndTo.key && timestamp < fromAndTo.value;
            }

            return false;
        };
    }

    public static Predicate<Double> asPredicate(NumberPredicate predicate) {
        return d -> {
            if (d == null) {

                // RT: Commented this out as shouldn't treat null like a value all operators should fail
//                // If given a null and we want to know if it's "less than x", it's always less than x
//                // TODO Should be consistent with BETWEEN behavior?
//                if (predicate.operator == LESS_THAN || predicate.operator == LESS_EQUALS) {
//                    return true;
//                }

                return false;
            }

            Number leftOperand = predicate.numberType == NumberType.DOUBLE ? d : d.intValue();
            Number rightOperand = predicate.numberType == NumberType.DOUBLE ? predicate.value : (int) predicate.value;

            switch (predicate.operator) {
                case EQUALS:
                    if (predicate.negate) {
                        return !leftOperand.equals(rightOperand);
                    }
                    return leftOperand.equals(rightOperand);
                case BETWEEN:
                    if (predicate.negate) {
                        return !(leftOperand.doubleValue() >= rightOperand.doubleValue() && leftOperand.doubleValue() <= predicate.rangeValue);
                    }
                    return leftOperand.doubleValue() >= rightOperand.doubleValue() && leftOperand.doubleValue() <= predicate.rangeValue;
                case LESS_THAN:
                    if (predicate.negate) {
                        return leftOperand.doubleValue() >= rightOperand.doubleValue();
                    }
                    return leftOperand.doubleValue() < rightOperand.doubleValue();
                case LESS_EQUALS:
                    if (predicate.negate) {
                        return leftOperand.doubleValue() > rightOperand.doubleValue();
                    }
                    return leftOperand.doubleValue() <= rightOperand.doubleValue();
                case GREATER_THAN:
                    if (predicate.negate) {
                        return leftOperand.doubleValue() <= rightOperand.doubleValue();
                    }
                    return leftOperand.doubleValue() > rightOperand.doubleValue();
                case GREATER_EQUALS:
                    if (predicate.negate) {
                        return leftOperand.doubleValue() < rightOperand.doubleValue();
                    }
                    return leftOperand.doubleValue() >= rightOperand.doubleValue();
            }
            return false;
        };
    }

    public static Predicate<AssetState> asPredicate(ParentPredicate predicate) {
        return assetState ->
            (predicate.id == null || predicate.id.equals(assetState.getParentId()))
                && (predicate.type == null || predicate.type.equals(assetState.getParentTypeString()))
                && (predicate.name == null || predicate.name.equals(assetState.getParentName()))
                && (!predicate.noParent || assetState.getParentId() == null);
    }

    public static Predicate<String[]> asPredicate(PathPredicate predicate) {
        return givenPath -> Arrays.equals(predicate.path, givenPath);
    }

    public static Predicate<AssetState> asPredicate(TenantPredicate predicate) {
        return assetState ->
            predicate == null || (predicate.realm != null && predicate.realm.equals(assetState.getRealm()));
    }

    public static Predicate<Coordinate> asPredicate(GeofencePredicate predicate) {
        return coordinate -> {
            if (coordinate == null) {
                return false;
            }

            if (predicate instanceof RadialGeofencePredicate) {
                //TODO geotools version to gradle properties
                RadialGeofencePredicate radialLocationPredicate = (RadialGeofencePredicate) predicate;
                GeodeticCalculator calculator = new GeodeticCalculator();
                calculator.setStartingGeographicPoint(radialLocationPredicate.lng, radialLocationPredicate.lat);
                calculator.setDestinationGeographicPoint(coordinate.y, coordinate.x);
                if (predicate.negated) {
                    return calculator.getOrthodromicDistance() > radialLocationPredicate.radius;
                }
                return calculator.getOrthodromicDistance() <= radialLocationPredicate.radius;
            } else if (predicate instanceof RectangularGeofencePredicate) {
                // Again this is a euclidean plane so doesn't work perfectly for WGS lat/lng - the bigger the rectangle to less accurate it is)
                RectangularGeofencePredicate rectangularLocationPredicate = (RectangularGeofencePredicate) predicate;
                Envelope envelope = new Envelope(rectangularLocationPredicate.latMin,
                    rectangularLocationPredicate.lngMin,
                    rectangularLocationPredicate.latMax,
                    rectangularLocationPredicate.lngMax);
                if (predicate.negated) {
                    return !envelope.contains(coordinate);
                }
                return envelope.contains(coordinate);
            } else {
                throw new UnsupportedOperationException("Location predicate '" + predicate.getClass().getSimpleName() + "' not supported in rules matching");
            }
        };
    }

    public static Predicate<AssetState> asPredicate(Supplier<Long> currentMillisProducer, AttributePredicate predicate) {

        Predicate<String> namePredicate = predicate.name != null
                ? StringPredicate.asPredicate(predicate.name) : str -> true;

        Predicate<Value> valuePredicate = value -> {
            if (predicate.value == null) {
                return true;
            }
            return asPredicate(currentMillisProducer, predicate.value).test(value);
        };

        return assetState -> namePredicate.test(assetState.getAttributeName())
                && valuePredicate.test(assetState.getValue().orElse(null));
    }

    public static Predicate<AssetState> asPredicate(Supplier<Long> currentMillisProducer, NewAttributePredicate predicate) {

        Predicate<AssetState> attributePredicate = asPredicate(currentMillisProducer, (AttributePredicate)predicate);

        Predicate<Meta> metaPredicate = meta -> {

            if (predicate.meta == null || predicate.meta.length == 0) {
                return true;
            }

            for (AttributeMetaPredicate p : predicate.meta) {
                if (!AssetQueryPredicate.asPredicate(currentMillisProducer, p).test(meta)) {
                    return false;
                }
            }
            return true;
        };

        Predicate<Value> oldValuePredicate = value -> {
            if (predicate.lastValue == null) {
                return true;
            }
            return AssetQueryPredicate.asPredicate(currentMillisProducer, predicate.lastValue).test(value);
        };

        return assetState -> attributePredicate.test(assetState)
                && metaPredicate.test(assetState.getMeta())
                && oldValuePredicate.test(assetState.getOldValue().orElse(null));
    }

    public static Predicate<Value> asPredicate(Supplier<Long> currentMillisProducer, ValuePredicate predicate) {
        return value -> {
            if (predicate == null)
                return true;

            if (predicate instanceof ValueEmptyPredicate) {

                return value == null;
            } else if (predicate instanceof ValueNotEmptyPredicate) {

                return value != null;

            } else if (predicate instanceof StringPredicate) {

                StringPredicate p = (StringPredicate) predicate;
                return StringPredicate.asPredicate(p).test(Values.getString(value).orElse(null));

            } else if (predicate instanceof BooleanPredicate) {

                BooleanPredicate p = (BooleanPredicate) predicate;
                return asPredicate(p).test(Values.getBoolean(value).orElse(null));

            } else if (predicate instanceof NumberPredicate) {

                NumberPredicate p = (NumberPredicate) predicate;
                return asPredicate(p).test(Values.getNumber(value).orElse(null));

            } else if (predicate instanceof DateTimePredicate) {

                DateTimePredicate p = (DateTimePredicate) predicate;
                return asPredicate(currentMillisProducer, p).test(Values.getNumber(value).map(Double::longValue).orElse(null));
            } else if (predicate instanceof GeofencePredicate) {

                GeofencePredicate p = (GeofencePredicate) predicate;
                return asPredicate(p).test(Optional.ofNullable(value)
                        .flatMap(GeoJSONPoint::fromValue)
                        .map(point -> new Coordinate(point.getY(), point.getX()))
                        .orElse(null));
            } else if (predicate instanceof ObjectValueKeyPredicate) {

                ObjectValueKeyPredicate p = (ObjectValueKeyPredicate) predicate;
                return asPredicate(p).test(Optional.ofNullable(value)
                        .flatMap(Values::getObject)
                        .orElse(null));
            }  else if (predicate instanceof ArrayPredicate) {

                ArrayPredicate p = (ArrayPredicate) predicate;
                return asPredicate(p).test(Optional.ofNullable(value)
                        .flatMap(Values::getArray)
                        .orElse(null));
            } else {
                // TODO Implement more
                throw new UnsupportedOperationException(
                        "Restriction by attribute value not implemented in rules matching for " + predicate.getClass()
                );
            }
        };
    }

    public static Predicate<Meta> asPredicate(Supplier<Long> currentMillisProducer, AttributeMetaPredicate predicate) {

        Predicate<MetaItem> metaItemPredicate = metaItem -> {
            if (predicate.itemNamePredicate != null) {
                if (!metaItem.getName().map(name -> StringPredicate.asPredicate(predicate.itemNamePredicate).test(name)).orElse(false)) {
                    return false;
                }
            }
            if (predicate.itemValuePredicate != null) {
                if (!metaItem.getValue().map(value -> asPredicate(currentMillisProducer, predicate.itemValuePredicate).test(value)).orElse(false)) {
                    return false;
                }
            }
            return true;
        };

        return meta -> meta.stream().anyMatch(metaItemPredicate);
    }

    public static Predicate<AssetState> asPredicate(Supplier<Long> currentMillisProducer, LogicGroup<AttributePredicate> condition) {
        if (conditionIsEmpty(condition)) {
            return as -> true;
        }

        LogicGroup.Operator operator = condition.operator == null ? LogicGroup.Operator.AND : condition.operator;

        List<Predicate<AssetState>> assetStatePredicates = new ArrayList<>();

        if (condition.getItems().size() > 0) {
            assetStatePredicates.addAll(
                condition.getItems().stream()
                            .map(p -> {
                                if (p instanceof NewAttributePredicate) {
                                    return asPredicate(currentMillisProducer, (NewAttributePredicate)p);
                                }
                                return asPredicate(currentMillisProducer, p);
                            }).collect(Collectors.toList())
            );
        }

        if (condition.groups != null && condition.groups.size() > 0) {
            assetStatePredicates.addAll(
                condition.groups.stream()
                            .map(c -> asPredicate(currentMillisProducer, c)).collect(Collectors.toList())
            );
        }

        return asPredicate(assetStatePredicates, operator);
    }

    protected static boolean conditionIsEmpty(LogicGroup condition) {
        return condition.getItems().size() == 0
            && (condition.groups != null && condition.groups.size() > 0);
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

    public static Pair<Long, Long> asFromAndTo(long currentMillis, DateTimePredicate dateTimePredicate) {

        Long from;
        Long to = null;

        try {
            if (TimeUtil.isTimeDuration(dateTimePredicate.value)) {
                from = currentMillis + TimeUtil.parseTimeDuration(dateTimePredicate.value);
            } else {
                from = ZonedDateTime.parse(dateTimePredicate.value).toInstant().toEpochMilli();
            }

            if (dateTimePredicate.operator == AssetQuery.Operator.BETWEEN) {
                if (TimeUtil.isTimeDuration(dateTimePredicate.rangeValue)) {
                    to = currentMillis + TimeUtil.parseTimeDuration(dateTimePredicate.rangeValue);
                } else {
                    to = ZonedDateTime.parse(dateTimePredicate.rangeValue).toInstant().toEpochMilli();
                }
            }
        } catch (IllegalArgumentException e) {
            from = null;
            to = null;
        }

        return new Pair<>(from, to);
    }
}
