/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.query.filter;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import org.openremote.model.util.ValueUtil;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Predicate for boolean values; will evaluate the value as a boolean and match against this predicates value, any value
 * that is not a boolean will not match.
 */
@JsonSchemaDescription("Predicate for boolean values; will evaluate the value as a boolean and match against this predicates value, any value that is not a boolean will not match")
public class BooleanPredicate extends ValuePredicate {

    public static final String name = "boolean";
    public boolean value;

    public BooleanPredicate() {
    }

    public BooleanPredicate(boolean value) {
        this.value = value;
    }

    public BooleanPredicate value(boolean value) {
        this.value = value;
        return this;
    }

    @Override
    public Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier) {
        return obj -> {
            Boolean bool = ValueUtil.getValueCoerced(obj, Boolean.class).orElse(null);
            if (bool == null) {
                return false;
            }

            return bool == value;
        };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "predicate=" + value +
            '}';
    }
}
