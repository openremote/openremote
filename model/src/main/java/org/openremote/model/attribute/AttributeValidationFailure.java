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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openremote.model.util.TsIgnore;

import java.util.Optional;

/**
 * Encapsulates a validation failure for a specific {@link Attribute}; constants should be used for {@link ReasonString}
 * values to allow human readable strings to be mapped for I18N. The parameter can be used to convey more specific
 * failure information (is dependent on I18N implementation),
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("attribute")
@Deprecated
// TODO: Replace with JSR-380 constraint violations
public class AttributeValidationFailure {

    public interface ReasonString {
        @JsonProperty
        String name();
    }

    public enum Reason implements ReasonString {
        ATTRIBUTE_MISSING,
        ATTRIBUTE_VALUE_MISSING,
        ATTRIBUTE_VALUE_TYPE_MISMATCH
    }

    protected String parameter;
    protected ReasonString reason;
    protected String attribute;

    public AttributeValidationFailure(String attribute, ReasonString reason) {
        this(attribute, reason, null);
    }

    public AttributeValidationFailure(String attribute, ReasonString reason, String parameter) {
        this.attribute = attribute;
        this.reason = reason;
        this.parameter = parameter;
    }

    public String getAttribute() {
        return attribute;
    }

    public ReasonString getReason() {
        return reason;
    }

    public Optional<String> getParameter() {
        return Optional.ofNullable(parameter);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "attributeName=" + attribute +
            ", reason=" + reason +
            ", param='" + parameter + '\'' +
            '}';
    }
}
