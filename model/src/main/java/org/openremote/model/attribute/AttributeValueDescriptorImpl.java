/*
 * Copyright 2019, OpenRemote Inc.
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
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.openremote.model.ValidationFailure;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.Optional;
import java.util.function.Function;

@JsType(namespace = "Model", name = "AttributeValueDescriptor")
public class AttributeValueDescriptorImpl implements AttributeValueDescriptor {

    protected String name;
    protected String icon;
    protected ValueType valueType;
    protected MetaItemDescriptor[] metaItemDescriptors;
    @JsonIgnore
    protected Function<Value, Optional<ValidationFailure>> validator;

    @JsIgnore
    @JsonCreator
    public AttributeValueDescriptorImpl(@JsonProperty("name") String name,
                                        @JsonProperty("icon") String icon,
                                        @JsonProperty("valueType") ValueType valueType,
                                        @JsonProperty("metaItemDescriptors") MetaItemDescriptor[] metaItemDescriptors) {
        this(name, icon, valueType, metaItemDescriptors, null);
    }

    @JsConstructor
    public AttributeValueDescriptorImpl(String name, String icon, ValueType valueType, MetaItemDescriptor[] metaItemDescriptors, Function<Value, Optional<ValidationFailure>> validator) {
        this.name = name;
        this.icon = icon;
        this.valueType = valueType;
        this.metaItemDescriptors = metaItemDescriptors;
        this.validator = validator;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public MetaItemDescriptor[] getMetaItemDescriptors() {
        return metaItemDescriptors;
    }

    @Override
    public Optional<Function<Value, Optional<ValidationFailure>>> getValidator() {
        return Optional.ofNullable(validator);
    }
}
