/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.openremote.model.value;

import org.openremote.model.value.impl.ValueFactoryImpl;

import java.util.Optional;

/**
 * Vends out implementations of {@link Value} and {@link ValueFactory}.
 */
public class Values {

    public static StringValue create(String string) {
        return instance().create(string);
    }

    public static BooleanValue create(boolean bool) {
        return instance().create(bool);
    }

    public static ArrayValue createArray() {
        return instance().createArray();
    }

    public static NumberValue create(double number) {
        return instance().create(number);
    }

    public static ObjectValue createObject() {
        return instance().createObject();
    }

    public static ValueFactory instance() {
        return ValueFactoryImpl.INSTANCE;
    }

    public static <T extends Value> Optional<T> parse(String jsonString) throws ValueException {
        return instance().parse(jsonString);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Value> Optional<T> cast(Class<T> type, Value value) {
        return value != null && value.getType().getModelType() == type
            ? Optional.of((T) value)
            : Optional.empty();
    }

    public static Optional<String> getString(Value value) {
        return cast(StringValue.class, value).map(StringValue::getString);
    }

    public static Optional<Double> getNumber(Value value) {
        return cast(NumberValue.class, value).map(NumberValue::getNumber);
    }

    public static Optional<Boolean> getBoolean(Value value) {
        return cast(BooleanValue.class, value).map(BooleanValue::getBoolean);
    }

    /**
     * Will attempt to coerce the value into a boolean (where it makes sense)
     */
    public static Optional<Boolean> getBooleanCoerced(Value value) {
        if (value == null) {
            return Optional.empty();
        }

        switch (value.getType()) {
            case OBJECT:
                return Optional.empty();
            case ARRAY:
                return Optional.empty();
            case STRING:
                return Optional.of(Boolean.parseBoolean(value.toString().toLowerCase()));
            case NUMBER:
                int number = Values.getNumber(value).map(Double::intValue).orElse(2);
                return number == 0 ? Optional.of(false) : number == 1 ? Optional.of(true) : Optional.empty();
            case BOOLEAN:
                return Values.getBoolean(value);
        }

        return Optional.empty();
    }

    /**
     * Attempts to coerce the value into an integer (where it makes sense)
     */
    public static Optional<Integer> getIntegerCoerced(Value value) {
        if (value == null) {
            return Optional.empty();
        }

        switch (value.getType()) {
            case OBJECT:
                return Optional.empty();
            case ARRAY:
                return Optional.empty();
            case STRING:
                try {
                    Optional.of(Integer.parseInt(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            case NUMBER:
                return Values.getNumber(value).map(Double::intValue);
            case BOOLEAN:
                return Values.getBoolean(value).map(b -> b ? 1 : 0);
        }

        return Optional.empty();
    }

    public static Optional<ObjectValue> getObject(Value value) {
        return cast(ObjectValue.class, value);
    }

    public static Optional<ArrayValue> getArray(Value value) {
        return cast(ArrayValue.class, value);
    }

}
