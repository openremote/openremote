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
package org.openremote.model.asset;

import org.openremote.model.asset.BaseAssetQuery.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Predicate;

import static org.openremote.model.asset.BaseAssetQuery.Operator.LESS_EQUALS;
import static org.openremote.model.asset.BaseAssetQuery.Operator.LESS_THAN;

/**
 * Test an {@link AbstractAssetUpdate} with a {@link BaseAssetQuery}.
 */
public class AssetQueryPredicate implements Predicate<AbstractAssetUpdate> {

    final protected BaseAssetQuery query;

    public AssetQueryPredicate(BaseAssetQuery query) {
        this.query = query;
    }

    @Override
    public boolean test(AbstractAssetUpdate assetUpdate) {

        if (query.id != null && !query.id.equals(assetUpdate.getId()))
            return false;

        if (query.name != null && !asPredicate(query.name).test(assetUpdate.getName()))
            return false;

        if (query.parent != null && !asPredicate(query.parent).test(assetUpdate))
            return false;

        if (query.path != null && !asPredicate(query.path).test(assetUpdate.getPath()))
            return false;

        if (query.tenant != null && !asPredicate(query.tenant).test(assetUpdate))
            return false;

        if (query.userId != null) {
            // TODO Would require linked user IDs in AbstractAssetUpdate
            throw new UnsupportedOperationException("Restriction by user ID not implemented in rules matching");
        }
        if (query.type != null && !asPredicate(query.type).test(assetUpdate.getTypeString()))
            return false;

        if (query.attribute != null) {
            for (BaseAssetQuery.AttributePredicate p : query.attribute) {
                if (!asPredicate(p).test(assetUpdate))
                    return false;
            }
        }

        if (query.attributeMeta != null) {
            // TODO Would require meta items in AbstractAssetUpdate
            throw new UnsupportedOperationException("Restriction by attribute meta not implemented in rules matching");
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

            String have = predicate.caseSensitive ? predicate.value : predicate.value.toUpperCase(Locale.ROOT);
            String given = predicate.caseSensitive ? string : string.toUpperCase(Locale.ROOT);

            switch (predicate.match) {
                case BEGIN:
                    return have.startsWith(given);
                case END:
                    return have.endsWith(given);
                case CONTAINS:
                    return have.contains(given);
            }
            return have.equals(given);
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

    protected Predicate<AbstractAssetUpdate> asPredicate(ParentPredicate predicate) {
        return assetUpdate ->
            (predicate.id == null || predicate.id.equals(assetUpdate.getParentId()))
                && (predicate.type == null || predicate.type.equals(assetUpdate.getParentTypeString()))
                && (!predicate.noParent || assetUpdate.getParentId() == null);
    }

    protected Predicate<String[]> asPredicate(PathPredicate predicate) {
        return givenPath -> Arrays.equals(predicate.path, givenPath);
    }

    protected Predicate<AbstractAssetUpdate> asPredicate(TenantPredicate predicate) {
        return assetUpdate ->
            (predicate.realm == null || predicate.realm.equals(assetUpdate.getTenantRealm()))
                && (predicate.realmId == null || predicate.realmId.equals(assetUpdate.getRealmId()));
    }

    protected Predicate<AbstractAssetUpdate> asPredicate(AttributePredicate predicate) {
        return assetUpdate -> {
            if (predicate.name != null && !asPredicate(predicate.name).test(assetUpdate.getAttributeName()))
                return false;

            if (predicate.value == null)
                return true;

            if (predicate.value instanceof AssetQuery.ValueNotEmptyPredicate) {
                return assetUpdate.getValue().isPresent();

            } else if (predicate.value instanceof AssetQuery.StringPredicate) {

                AssetQuery.StringPredicate p = (AssetQuery.StringPredicate) predicate.value;
                return asPredicate(p).test(assetUpdate.getValueAsString().orElse(null));

            } else if (predicate.value instanceof AssetQuery.BooleanPredicate) {

                AssetQuery.BooleanPredicate p = (AssetQuery.BooleanPredicate) predicate.value;
                return asPredicate(p).test(assetUpdate.getValueAsBoolean().orElse(null));

            } else if (predicate.value instanceof AssetQuery.NumberPredicate) {

                AssetQuery.NumberPredicate p = (AssetQuery.NumberPredicate) predicate.value;
                return asPredicate(p).test(assetUpdate.getValueAsNumber().orElse(null));

            } else {
                // TODO Implement more
                throw new UnsupportedOperationException(
                    "Restriction by attribute value not implemented in rules matching for " + predicate.value.getClass()
                );
            }
        };
    }


}
