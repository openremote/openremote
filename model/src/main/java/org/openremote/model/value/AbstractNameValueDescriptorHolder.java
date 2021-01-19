/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Arrays;
import java.util.Objects;

public abstract class AbstractNameValueDescriptorHolder<T> implements ValueDescriptorHolder<T>, NameHolder {

    @JsonIgnore
    protected String name;
    @JsonIgnore
    protected ValueDescriptor<T> type;
    @JsonIgnore
    protected ValueConstraint[] constraints;
    @JsonIgnore
    protected ValueFormat format;
    @JsonIgnore
    protected String[] units;

    AbstractNameValueDescriptorHolder() {}

    public AbstractNameValueDescriptorHolder(String name, ValueDescriptor<T> type, ValueConstraint...constraints) {
        this.name = name;
        this.type = type;
        this.constraints = constraints;
    }

    @JsonProperty
    @Override
    public String getName() {
        return name;
    }

    @JsonProperty
    protected void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    @JsonSerialize(converter = ValueDescriptor.ValueDescriptorStringConverter.class)
    @JsonDeserialize(converter = ValueDescriptor.StringValueDescriptorConverter.class)
    @Override
    public ValueDescriptor<T> getType() {
        return type;
    }

    @JsonProperty
    @JsonSerialize(converter = ValueDescriptor.ValueDescriptorStringConverter.class)
    @JsonDeserialize(converter = ValueDescriptor.StringValueDescriptorConverter.class)
    protected void setType(ValueDescriptor<T> type) {
        this.type = type;
    }

    @Override
    public ValueFormat getFormat() {
        return format;
    }

    @Override
    public ValueConstraint[] getConstraints() {
        return constraints;
    }

    @Override
    public String[] getUnits() {
        return units;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    /**
     * Descriptor names are unique identifiers so can use this for equality purposes
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractNameValueDescriptorHolder<?> that = (AbstractNameValueDescriptorHolder<?>)obj;
        return Objects.equals(name, that.name);
    }

    @Override
    public String toString() {
        return "name='" + name + '\'' +
            ", type=" + type +
            ", constraints=" + Arrays.toString(constraints) +
            ", format=" + format +
            ", units=" + Arrays.toString(units);
    }
}
