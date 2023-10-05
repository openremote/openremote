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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

@JsonFilter("excludeNameFilter")
public abstract class AbstractNameValueHolder<T> implements NameValueHolder<T>, Serializable {
    @JsonIgnore
    protected ValueDescriptor<T> type;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Valid
    protected T value;
    @JsonIgnore
    protected String valueStr; // This is for lazy value initialisation when deserialising
    @NotBlank(message = "{Asset.valueHolder.name.NotBlank}")
    @Pattern(regexp = "^\\w+$")
    @JsonIgnore
    protected String name;

    protected AbstractNameValueHolder() {
    }

    public AbstractNameValueHolder(@Nonnull String name, @Nonnull ValueDescriptor<T> type, T value) {
        if (TextUtil.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    @Nullable
    public ValueDescriptor<T> getType() {
        return type;
    }

    @Override
    public Class<?> getTypeClass() {
        return getType() != null ? getType().getType() : Object.class;
    }

    @SuppressWarnings("unchecked")
    @JsonProperty
    @Override
    public Optional<T> getValue() {
        return (Optional<T>)getValue(getTypeClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Optional<U> getValue(@Nonnull Class<U> valueType) {
        if (value == null) {
            return Optional.empty();
        }
        if (valueType.isAssignableFrom(value.getClass())) {
            return Optional.of((U)value);
        }
        return ValueUtil.getValueCoerced(value, valueType);
    }

    @JsonProperty
    public void setValue(T value) {
        this.value = value;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractNameValueHolder<?> that = (AbstractNameValueHolder<?>) o;
        return name.equals(that.name)
            && Objects.equals(type, that.type)
            && Objects.equals(ValueUtil.convert(value, JsonNode.class), ValueUtil.convert(that.value, JsonNode.class));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type, name);
    }
}
