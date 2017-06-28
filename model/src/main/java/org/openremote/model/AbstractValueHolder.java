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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class for all model classes which have to internally store data in an
 * {@link ObjectValue} that has a <code>value</code> field that accepts any
 * {@link Value}.
 */
public abstract class AbstractValueHolder implements ValueHolder {

    protected static final String VALUE_FIELD_NAME = "value";

    final protected ObjectValue objectValue;

    public AbstractValueHolder(ObjectValue objectValue) {
        this.objectValue = Objects.requireNonNull(objectValue);
    }

    public ObjectValue getObjectValue() {
        return objectValue;
    }

    @Override
    public void clearValue() {
        objectValue.remove(VALUE_FIELD_NAME);
    }

    @Override
    public Optional<Value> getValue() {
        return objectValue.get(VALUE_FIELD_NAME);
    }

    @Override
    public Optional<String> getValueAsString() {
        return objectValue.getString(VALUE_FIELD_NAME);
    }

    @Override
    public Optional<Double> getValueAsNumber() {
        return objectValue.getNumber(VALUE_FIELD_NAME);
    }

    @Override
    public Optional<Integer> getValueAsInteger() {
        return objectValue.getNumber(VALUE_FIELD_NAME).map(Double::intValue);
    }

    @Override
    public Optional<Boolean> getValueAsBoolean() {
        return objectValue.getBoolean(VALUE_FIELD_NAME);
    }

    @Override
    public Optional<ObjectValue> getValueAsObject() {
        return objectValue.getObject(VALUE_FIELD_NAME);
    }

    @Override
    public Optional<ArrayValue> getValueAsArray() {
        return objectValue.getArray(VALUE_FIELD_NAME);
    }

    @Override
    public void setValue(Value value) {
        if (value != null) {
            objectValue.put(VALUE_FIELD_NAME, value);
        } else {
            clearValue();
        }
    }

    /**
     * Override to implement constraint checking.
     */
    @Override
    public List<ValidationFailure> getValidationFailures() {
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return objectValue.toJson();
    }
}
