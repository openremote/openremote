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
import org.openremote.model.asset.BaseAssetQuery;
import org.openremote.model.asset.BaseAssetQuery.*;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.rules.AssetState;
import org.openremote.model.value.ObjectValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Predicate;

import static org.openremote.model.asset.BaseAssetQuery.Operator.LESS_EQUALS;
import static org.openremote.model.asset.BaseAssetQuery.Operator.LESS_THAN;

/**
 * Test an {@link AssetState} with a {@link BaseAssetQuery}.
 */
public class AssetQueryPredicate implements Predicate<AssetState> {

    final protected BaseAssetQuery query;

    public AssetQueryPredicate(BaseAssetQuery query) {
        this.query = query;
    }

    @Override
    public boolean test(AssetState assetState) {

        if (query.id != null && !query.id.equals(assetState.getId()))
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
            for (BaseAssetQuery.AttributePredicate p : query.attribute) {
                if (!asPredicate(p).test(assetState))
                    return false;
            }
        }

        if (query.attributeMeta != null) {
            // TODO Would require meta items in AbstractAssetUpdate
            throw new UnsupportedOperationException("Restriction by attribute meta not implemented in rules matching");
        }

        if (query.select != null) {
            throw new UnsupportedOperationException("Projection with 'select' not supported in rules matching");
        }

        if (query.orderBy != null) {
            throw new UnsupportedOperationException("Sorting with 'orderBy' not supported in rules matching");
        }

        if (query.location != null) {
            GeoJSONPoint coords = assetState.getValue().flatMap(GeoJSONPoint::fromValue).orElse(null);

            if (coords != null) {
                Coordinate coordinate = new Coordinate(coords.getY(), coords.getX());
                return asPredicate(query.location).test(coordinate);
            }
            return false;
        }

        return true;
    }

    protected Predicate<String> asPredicate(StringPredicate predicate) {
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

    protected Predicate<Boolean> asPredicate(BooleanPredicate predicate) {
        return b -> {
            // If given a null, we assume it's false!
            if (b == null)
                b = false;
            return b == predicate.value;
        };
    }

    protected Predicate<String[]> asPredicate(StringArrayPredicate predicate) {
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

    protected Predicate<Long> asPredicate(DateTimePredicate predicate) {
        return timestamp -> {
            throw new UnsupportedOperationException("NOT IMPLEMENTED");
        };
    }

    protected Predicate<Double> asPredicate(NumberPredicate predicate) {
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

    protected Predicate<AssetState> asPredicate(ParentPredicate predicate) {
        return assetState ->
            (predicate.id == null || predicate.id.equals(assetState.getParentId()))
                && (predicate.type == null || predicate.type.equals(assetState.getParentTypeString()))
                && (!predicate.noParent || assetState.getParentId() == null);
    }

    protected Predicate<String[]> asPredicate(PathPredicate predicate) {
        return givenPath -> Arrays.equals(predicate.path, givenPath);
    }

    protected Predicate<AssetState> asPredicate(TenantPredicate predicate) {
        return assetState ->
            (predicate.realm == null || predicate.realm.equals(assetState.getTenantRealm()))
                && (predicate.realmId == null || predicate.realmId.equals(assetState.getRealmId()));
    }

    protected Predicate<Coordinate> asPredicate(LocationPredicate predicate) {
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
                throw new UnsupportedOperationException("Location predicate '" + query.location.getClass().getSimpleName() + "' not supported in rules matching");
            }
        };
    }

    protected Predicate<AssetState> asPredicate(AttributePredicate predicate) {
        return assetState -> {
            if (predicate.name != null && !asPredicate(predicate.name).test(assetState.getAttributeName()))
                return false;

            if (predicate.value == null)
                return true;

            if (predicate.value instanceof BaseAssetQuery.ValueNotEmptyPredicate) {
                return assetState.getValue().isPresent();

            } else if (predicate.value instanceof BaseAssetQuery.StringPredicate) {

                StringPredicate p = (StringPredicate) predicate.value;
                return asPredicate(p).test(assetState.getValueAsString().orElse(null));

            } else if (predicate.value instanceof BaseAssetQuery.BooleanPredicate) {

                BooleanPredicate p = (BooleanPredicate) predicate.value;
                return asPredicate(p).test(assetState.getValueAsBoolean().orElse(null));

            } else if (predicate.value instanceof BaseAssetQuery.NumberPredicate) {

                NumberPredicate p = (NumberPredicate) predicate.value;
                return asPredicate(p).test(assetState.getValueAsNumber().orElse(null));
            } else {
                // TODO Implement more
                throw new UnsupportedOperationException(
                    "Restriction by attribute value not implemented in rules matching for " + predicate.value.getClass()
                );
            }
        };
    }

}
