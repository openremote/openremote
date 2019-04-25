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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import org.openremote.model.HasUniqueResourceName;
import org.openremote.model.ValidationFailure;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonDeserialize(as = MetaItemDescriptorImpl.class)
public interface MetaItemDescriptor extends HasUniqueResourceName {

    /**
     * Control access to meta items when attributes are read/written. The default is
     * read/write access by regular and super-users only, meta items are considered private.
     */
    @JsType(namespace = "Model", name = "MetaItemDescriptorAccess")
    class Access {

        public static Access ACCESS_PRIVATE = new Access();

        /**
         * When restricted clients read attributes of the authenticated user's linked
         * assets, should this meta item be included?
         */
        public boolean restrictedRead;

        /**
         * When restricted clients write attributes of the authenticated user's linked
         * assets, should this meta item be included?
         */
        public boolean restrictedWrite;

        /**
         * When public clients read asset attributes, should this meta item be
         * included? Note that public clients can not write asset attributes.
         */
        public boolean publicRead;

        /**
         * Private access only.
         */
        @JsIgnore
        public Access() {
            this(false, false, false);
        }

        @JsConstructor
        public Access(boolean restrictedRead, boolean restrictedWrite, boolean publicRead) {
            this.restrictedRead = restrictedRead;
            this.restrictedWrite = restrictedWrite;
            this.publicRead = publicRead;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Access access = (Access) o;
            return restrictedRead == access.restrictedRead &&
                restrictedWrite == access.restrictedWrite &&
                publicRead == access.publicRead;
        }

        @Override
        public int hashCode() {
            return Objects.hash(restrictedRead, restrictedWrite, publicRead);
        }
    }

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
        DAYS_HOURS_MINS_SECONDS,
        STRING_EMPTY,
        STRING_EMPTY_OR_CONTAINS_WHITESPACE,
        HTTP_URL
    }

    @JsonProperty
    String getUrn();

    @JsonProperty
    ValueType getValueType();

    /**
     * Access permissions for read/write operations performed by restricted or public clients.
     */
    @JsonProperty
    Access getAccess();

    /**
     * Indicates if this meta item is always required.
     * <p>
     * TODO Currently not used.
     */
    @JsonProperty
    boolean isRequired();

    /**
     * A regex pattern that can be used for basic validation, can be <code>null</code> to disable validation.
     */
    @JsonProperty
    String getPattern();

    /**
     * Get a parameter to show as part of validation feedback for when the pattern matching fails
     * e.g. Meta Item value must be 'Positive Integer'. Can be <code>null</code> if validation is disabled.
     * <p>
     * Values defined in {@link PatternFailure} should support localisation.
     */
    @JsonProperty
    String getPatternFailureMessage();

    /**
     * Gets the maximum number of these meta items allowed per attribute.
     */
    @JsonProperty
    Integer getMaxPerAttribute();

    /**
     * Gets the initial value for new instances of this meta item, can be <code>null</code>.
     */
    @JsonProperty
    Value getInitialValue();

    /**
     * Indicates if the value of this meta item is fixed and can't be edited.
     */
    @JsonProperty
    boolean isValueFixed();

    /**
     * Get optional validation function; this allows {@link MetaItemDescriptor} implementations to do more advanced
     * validation. If defined this takes priority over all other value checks.
     */
    Optional<Function<Value, Optional<ValidationFailure>>> getValidator();
}
