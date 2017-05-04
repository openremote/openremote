/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Objects;
import java.util.Optional;

/**
 * Base class for all model classes which have to internally store data in an
 * {@link ObjectValue} that has a <code>value</code> field that accepts any
 * {@link Value}.
 */
public abstract class AbstractValueHolder {

    protected static final String VALUE_FIELD_NAME = "value";

    final protected ObjectValue objectValue;

    public AbstractValueHolder(ObjectValue objectValue) {
        this.objectValue = Objects.requireNonNull(objectValue);
    }

    public ObjectValue getObjectValue() {
        return objectValue;
    }

    public void clearValue() {
        objectValue.remove(VALUE_FIELD_NAME);
    }

    public Optional<Value> getValue() {
        return objectValue.get(VALUE_FIELD_NAME);
    }

    public Optional<String> getValueAsString() {
        return objectValue.getString(VALUE_FIELD_NAME);
    }

    public Optional<Double> getValueAsNumber() {
        return objectValue.getNumber(VALUE_FIELD_NAME);
    }

    public Optional<Integer> getValueAsInteger() {
        return objectValue.getNumber(VALUE_FIELD_NAME).map(Double::intValue);
    }

    public Optional<Boolean> getValueAsBoolean() {
        return objectValue.getBoolean(VALUE_FIELD_NAME);
    }

    public Optional<ObjectValue> getValueAsObject() {
        return objectValue.getObject(VALUE_FIELD_NAME);
    }

    public Optional<ArrayValue> getValueAsArray() {
        return objectValue.getArray(VALUE_FIELD_NAME);
    }

    /**
     * @throws IllegalArgumentException if the given value is invalid and failed constraint checking.
     */
    public void setValue(Value value) throws IllegalArgumentException {
        if (!isValidValue(value)) {
            throw new IllegalArgumentException("Invalid value: " + (value != null ? value.toJson() : "null"));
        }
        if (value != null) {
            objectValue.put(VALUE_FIELD_NAME, value);
        } else {
            clearValue();
        }
    }

    public void setValue(Value... values) throws IllegalArgumentException {
        setValue(Values.createArray().addAll(values));
    }

    public void setValue(String string) throws IllegalArgumentException {
        setValue(Values.create(string));
    }

    public void setValue(Double number) throws IllegalArgumentException {
        setValue(Values.create(number));
    }

    public void setValue(Boolean b) throws IllegalArgumentException {
        setValue(Values.create(b));
    }

    /**
     * Override to implement constraints.
     */
    public boolean isValidValue(Value value) {
        return true;
    }

    public boolean hasValue() {
        return getValue().isPresent();
    }

    public boolean isValid() {
        return true;
    }

    @Override
    public String toString() {
        return objectValue.toJson();
    }
}
