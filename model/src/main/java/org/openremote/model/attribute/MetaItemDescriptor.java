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
package org.openremote.model.attribute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.HasUniqueResourceName;
import org.openremote.model.ValidationFailure;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.Optional;
import java.util.function.Function;

@JsonSerialize(as = MetaItemDescriptorImpl.class)
@JsonDeserialize(as = MetaItemDescriptorImpl.class)
public interface MetaItemDescriptor extends HasUniqueResourceName {

    enum PatternFailure {
        INTEGER,
        INTEGER_NON_ZERO,
        INTEGER_POSITIVE,
        INTEGER_NEGATIVE,
        INTEGER_POSITIVE_NON_ZERO,
        INTEGER_NEGATIVE_NON_ZERO,
        DOUBLE,
        DOUBLE_POSITIVE,
        DOUBLE_NEGATIVE,
        DOUBLE_POSITIVE_NON_ZERO,
        DOUBLE_NEGATIVE_NON_ZERO,
        CRON_EXPRESSION,
        DAYS_HOURS_MINS_SECONDS
    }

    String name();

    String getUrn();

    ValueType getValueType();

    /**
     * Indicates if this meta item is always required
     */
    boolean isRequired();

    /**
     * A regex pattern that can be used for basic validation
     */
    String getPattern();

    /**
     * Get a parameter to show as part of validation feedback for when the pattern matching fails
     * e.g. Meta Item value must be 'Positive Integer'.
     * <p>
     * Values defined in {@link PatternFailure} should support localisation.
     */
    String getPatternFailureMessage();

    /**
     * Gets the maximum number of these meta items allowed per attribute
     */
    Integer getMaxPerAttribute();

    /**
     * Gets the initial value for new instances of this meta item
     */
    Value getInitialValue();

    /**
     * Indicates if the value of this meta item is fixed
     */
    boolean isValueFixed();

    /**
     * Get optional validation function; this allows {@link MetaItemDescriptor} implementations to do more advanced
     * validation. If defined this takes priority over all other value checks
     */
    Optional<Function<Value, Optional<ValidationFailure>>> getValidator();
}
