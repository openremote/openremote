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
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.Meta;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.BaseAssetQuery.NumberType;
import org.openremote.model.query.filter.*;
import org.openremote.model.rules.AssetState;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.openremote.model.query.BaseAssetQuery.Operator.LESS_EQUALS;
import static org.openremote.model.query.BaseAssetQuery.Operator.LESS_THAN;

/**
 * Test an {@link AssetState} with a {@link BaseAssetQuery}.
 */
public class AssetQueryPredicate implements Predicate<AssetState> {

    final protected BaseAssetQuery query;
    final protected TimerService timerService;

    public AssetQueryPredicate(TimerService timerService, BaseAssetQuery query) {
        this.timerService = timerService;
        this.query = query;
    }

    @Override
    public boolean test(AssetState assetState) {

        if (query.ids != null && !query.ids.contains(assetState.getId()))
            return false;

        if (query.name != null && !asPredicate(query.name).test(assetState.getName()))
            return false;

        if (query.parent != null && !asPredicate(query.parent).test(assetState))
            return false;

        if (query.path != null && !asPredicate(query.path).test(assetState.getPath()))
            return false;

        if (query.tenant != null && !asPredicate(query.tenant).test(assetState))
            return false;

        if (query.userId != null) {
            // TODO Would require linked user IDs in AbstractAssetUpdate
            throw new UnsupportedOperationException("Restriction by user ID not implemented in rules matching");
        }
        if (query.type != null && !asPredicate(query.type).test(assetState.getTypeString()))
            return false;

        if (query.attribute != null) {
            for (AttributePredicate p : query.attribute) {
                if (!asPredicate(timerService::getCurrentTimeMillis, p).test(assetState))
                    return false;
            }
        }

        if (query.attributeMeta != null) {
            for (AttributeMetaPredicate p : query.attributeMeta) {
                if (!asPredicate(timerService::getCurrentTimeMillis, p).test(assetState.getMeta())) {
                    return false;
                }
            }
        }

        if (query.select != null) {
            throw new UnsupportedOperationException("Projection with 'select' not supported in rules matching");
        }

        if (query.orderBy != null) {
            throw new UnsupportedOperationException("Sorting with 'orderBy' not supported in rules matching");
        }

        if (query.location != null) {
            GeoJSONPoint coords = AttributeType.LOCATION.getName().equals(assetState.getAttributeName()) ? assetState.getValue().flatMap(GeoJSONPoint::fromValue).orElse(null) : null;

            if (coords != null) {
                Coordinate coordinate = new Coordinate(coords.getY(), coords.getX());
                return asPredicate(query.location).test(coordinate);
            }
            return false;
        }

        return true;
    }

    public static Predicate<String> asPredicate(StringPredicate predicate) {
        return string -> {
            if (string == null && predicate.value == null)
                return true;
            if (string == null)
                return false;
            if (predicate.value == null)
                return false;

            String shouldMatch = predicate.caseSensitive ? predicate.value : predicate.value.toUpperCase(Locale.ROOT);
            String have = predicate.caseSensitive ? string : string.toUpperCase(Locale.ROOT);

            switch (predicate.match) {
                case BEGIN:
                    return have.startsWith(shouldMatch);
                case END:
                    return have.endsWith(shouldMatch);
                case CONTAINS:
                    return have.contains(shouldMatch);
            }
            return have.equals(shouldMatch);
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
                if (!asPredicate(p).test(strings[i]))
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
                    return timestamp == fromAndTo.key.longValue();
                case NOT_EQUALS:
                    return timestamp != fromAndTo.key.longValue();
                case GREATER_THAN:
                    return timestamp > fromAndTo.key;
                case GREATER_EQUALS:
                    return timestamp >= fromAndTo.key;
                case LESS_THAN:
                    return timestamp < fromAndTo.key;
                case LESS_EQUALS:
                    return timestamp <= fromAndTo.key;
                case BETWEEN:
                    if (fromAndTo.value == null) {
                        throw new IllegalArgumentException("Date time predicate 'rangeValue' is not valid: " + predicate);
                    }
                    return timestamp > fromAndTo.key && timestamp < fromAndTo.value;
            }

            return false;
        };
    }

    public static Predicate<Double> asPredicate(NumberPredicate predicate) {
        return d -> {
            if (d == null) {

                // If given a null and we want to know if it's "less than x", it's always less than x
                // TODO Should be consistent with BETWEEN behavior?
                if (predicate.operator == LESS_THAN || predicate.operator == LESS_EQUALS) {
                    return true;
                }

                return false;
            }

            Number leftOperand = predicate.numberType == NumberType.DOUBLE ? d : d.intValue();
            Number rightOperand = predicate.numberType == NumberType.DOUBLE ? predicate.value : (int) predicate.value;
            switch (predicate.operator) {
                case EQUALS:
                    return leftOperand.equals(rightOperand);
                case NOT_EQUALS:
                    return !leftOperand.equals(rightOperand);
                case BETWEEN:
                    return leftOperand.doubleValue() >= rightOperand.doubleValue() && leftOperand.doubleValue() <= predicate.rangeValue;
                case LESS_THAN:
                    return leftOperand.doubleValue() < rightOperand.doubleValue();
                case LESS_EQUALS:
                    return leftOperand.doubleValue() <= rightOperand.doubleValue();
                case GREATER_THAN:
                    return leftOperand.doubleValue() > rightOperand.doubleValue();
                case GREATER_EQUALS:
                    return leftOperand.doubleValue() >= rightOperand.doubleValue();
            }
            return false;
        };
    }

    public static Predicate<AssetState> asPredicate(ParentPredicate predicate) {
        return assetState ->
            (predicate.id == null || predicate.id.equals(assetState.getParentId()))
                && (predicate.type == null || predicate.type.equals(assetState.getParentTypeString()))
                && (!predicate.noParent || assetState.getParentId() == null);
    }

    public static Predicate<String[]> asPredicate(PathPredicate predicate) {
        return givenPath -> Arrays.equals(predicate.path, givenPath);
    }

    public static Predicate<AssetState> asPredicate(TenantPredicate predicate) {
        return assetState ->
            (predicate.realm == null || predicate.realm.equals(assetState.getTenantRealm()))
                && (predicate.realmId == null || predicate.realmId.equals(assetState.getRealmId()));
    }

    public static Predicate<Coordinate> asPredicate(LocationPredicate predicate) {
        return coordinate -> {
            if (predicate instanceof RadialLocationPredicate) {
                //TODO geotools version to gradle properties
                RadialLocationPredicate radialLocationPredicate = (RadialLocationPredicate) predicate;
                GeodeticCalculator calculator = new GeodeticCalculator();
                calculator.setStartingGeographicPoint(radialLocationPredicate.lng, radialLocationPredicate.lat);
                calculator.setDestinationGeographicPoint(coordinate.y, coordinate.x);
                return calculator.getOrthodromicDistance() < radialLocationPredicate.radius;
            } else if (predicate instanceof RectangularLocationPredicate) {
                // Again this is a euclidean plane so doesn't work perfectly for WGS lat/lng - the bigger the rectangle to less accurate it is)
                RectangularLocationPredicate rectangularLocationPredicate = (RectangularLocationPredicate) predicate;
                Envelope envelope = new Envelope(rectangularLocationPredicate.latMin,
                    rectangularLocationPredicate.lngMin,
                    rectangularLocationPredicate.latMax,
                    rectangularLocationPredicate.lngMax);
                return envelope.contains(coordinate);
            } else {
                throw new UnsupportedOperationException("Location predicate '" + predicate.getClass().getSimpleName() + "' not supported in rules matching");
            }
        };
    }

    public static Predicate<AssetState> asPredicate(Supplier<Long> currentMillisProducer, AttributePredicate predicate) {
        return assetState -> {
            if (predicate.name != null && !asPredicate(predicate.name).test(assetState.getAttributeName()))
                return false;

            return asPredicate(currentMillisProducer, predicate.value).test(assetState.getValue().orElse(null));
        };
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
                return asPredicate(p).test(Values.getString(value).orElse(null));

            } else if (predicate instanceof BooleanPredicate) {

                BooleanPredicate p = (BooleanPredicate) predicate;
                return asPredicate(p).test(Values.getBoolean(value).orElse(null));

            } else if (predicate instanceof NumberPredicate) {

                NumberPredicate p = (NumberPredicate) predicate;
                return asPredicate(p).test(Values.getNumber(value).orElse(null));

            } else if (predicate instanceof DateTimePredicate) {

                DateTimePredicate p = (DateTimePredicate) predicate;
                return asPredicate(currentMillisProducer, p).test(Values.getNumber(value).map(Double::longValue).orElse(null));

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
                if (!metaItem.getName().map(name -> asPredicate(predicate.itemNamePredicate).test(name)).orElse(false)) {
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

    public static Pair<Long, Long> asFromAndTo(long currentMillis, DateTimePredicate dateTimePredicate) {

        Long from;
        Long to = null;

        try {
            if (TimeUtil.isTimeDuration(dateTimePredicate.value)) {
                from = currentMillis + TimeUtil.parseTimeDuration(dateTimePredicate.value);
            } else {
                from = new SimpleDateFormat(dateTimePredicate.dateFormat).parse(dateTimePredicate.value).getTime();
            }

            if (dateTimePredicate.operator == BaseAssetQuery.Operator.BETWEEN) {
                if (TimeUtil.isTimeDuration(dateTimePredicate.rangeValue)) {
                    to = currentMillis + TimeUtil.parseTimeDuration(dateTimePredicate.rangeValue);
                } else {
                    to = new SimpleDateFormat(dateTimePredicate.dateFormat).parse(dateTimePredicate.rangeValue).getTime();
                }
            }
        } catch (ParseException e) {
            from = null;
            to = null;
        }

        return new Pair<>(from, to);
    }
}
