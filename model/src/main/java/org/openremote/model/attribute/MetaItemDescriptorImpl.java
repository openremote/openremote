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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import org.openremote.model.ValidationFailure;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@JsType(namespace = "Model", name = "MetaItemDescriptor")
public class MetaItemDescriptorImpl implements MetaItemDescriptor {

    protected final String urn;
    protected final ValueType valueType;
    protected final Access access;
    protected final boolean required;
    protected final String pattern;
    protected final Integer maxPerAttribute;
    protected final Value initialValue;
    protected final Value allowedMin;
    protected final Value allowedMax;
    protected final Value[] allowedValues;
    protected final boolean valueFixed;
    protected final String patternFailureMessage;

    @JsConstructor
    @JsonCreator
    public MetaItemDescriptorImpl(@JsonProperty("urn") String urn,
                                  @JsonProperty("valueType") ValueType valueType,
                                  @JsonProperty("access") Access access,
                                  @JsonProperty("required") boolean required,
                                  @JsonProperty("pattern") String pattern,
                                  @JsonProperty("patternFailureMessage") String patternFailureMessage,
                                  @JsonProperty("maxPerAttribute") Integer maxPerAttribute,
                                  @JsonProperty("initialValue") Value initialValue,
                                  @JsonProperty("valueFixed") boolean valueFixed,
                                  @JsonProperty("allowedMin")Value allowedMin,
                                  @JsonProperty("allowedMax")Value allowedMax,
                                  @JsonProperty("allowedValues")Value[] allowedValues) {
        this.urn = urn;
        this.valueType = valueType;
        this.access = access;
        this.required = required;
        this.pattern = pattern;
        this.patternFailureMessage = patternFailureMessage;
        this.maxPerAttribute = maxPerAttribute;
        this.initialValue = initialValue;
        this.valueFixed = valueFixed;
        this.allowedMin = allowedMin;
        this.allowedMax = allowedMax;
        this.allowedValues = allowedValues;
    }

    @JsIgnore
    public MetaItemDescriptorImpl(MetaItemDescriptor metaItemDescriptor) {
        this(metaItemDescriptor, metaItemDescriptor.getInitialValue());
    }

    @JsIgnore
    public MetaItemDescriptorImpl(MetaItemDescriptor metaItemDescriptor, Value initialValue) {
        this(metaItemDescriptor.getUrn(),
                metaItemDescriptor.getValueType(),
                metaItemDescriptor.isRequired(),
                metaItemDescriptor.getPattern(),
                metaItemDescriptor.getPatternFailureMessage(),
                metaItemDescriptor.getMaxPerAttribute(),
                initialValue,
                metaItemDescriptor.isValueFixed(), null, null, null);
    }

    @JsIgnore
    public MetaItemDescriptorImpl(String urn,
                                  ValueType valueType,
                                  boolean required,
                                  String pattern,
                                  String patternFailureMessage,
                                  Integer maxPerAttribute,
                                  Value initialValue,
                                  boolean valueFixed,
                                  Value allowedMin,
                                  Value allowedMax,
                                  Value[] allowedValues) {
        this(urn, valueType, Access.ACCESS_PRIVATE, required, pattern, patternFailureMessage, maxPerAttribute, initialValue, valueFixed, allowedMin, allowedMax, allowedValues);
    }

    @Override
    public String getUrn() {
        return urn;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public Access getAccess() {
        return access;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String getPatternFailureMessage() {
        return patternFailureMessage;
    }

    @Override
    public Integer getMaxPerAttribute() {
        return maxPerAttribute;
    }

    @Override
    public Value getInitialValue() {
        return initialValue;
    }

    @Override
    public boolean isValueFixed() {
        return valueFixed;
    }

    @Override
    public Value getAllowedMin() {
        return allowedMin;
    }

    @Override
    public Value getAllowedMax() {
        return allowedMax;
    }

    @Override
    public Value[] getAllowedValues() {
        return allowedValues;
    }

    @JsonIgnore
    @Override
    public Optional<Function<Value, Optional<ValidationFailure>>> getValidator() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                ", urn='" + urn + '\'' +
                ", valueType=" + valueType +
                ", access=" + access +
                ", required=" + required +
                ", pattern='" + pattern + '\'' +
                ", maxPerAttribute=" + maxPerAttribute +
                ", initialValue=" + initialValue +
                ", allowedMin=" + allowedMin +
                ", allowedMax=" + allowedMax +
                ", allowedValues=" + (allowedValues != null ? Arrays.toString(allowedValues) : "null") +
                ", valueFixed=" + valueFixed +
                ", patternFailureMessage='" + patternFailureMessage + '\'' +
                '}';
    }
}
