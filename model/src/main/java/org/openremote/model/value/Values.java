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

import jsinterop.base.Any;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.impl.ValueFactoryImpl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static native <T extends Value> Optional<T> fromAny(Any any) /*-{
        // TODO This makes a copy which is inefficient, need twice the memory. We need a better JSON API to share with Java and JS.
        return @org.openremote.model.value.Values::parse(Ljava/lang/String;)(JSON.stringify(any));
    }-*/;

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

        return convert(value, BooleanValue.class)
            .map(BooleanValue::getBoolean);
    }

    /**
     * Attempts to coerce the value into an integer (where it makes sense)
     */
    public static Optional<Integer> getIntegerCoerced(Value value) {

        return convert(value, NumberValue.class)
            .map(NumberValue::getNumber)
            .map(Double::intValue);
    }

    public static Optional<ObjectValue> getObject(Value value) {
        return cast(ObjectValue.class, value);
    }

    public static Optional<ArrayValue> getArray(Value value) {
        return cast(ArrayValue.class, value);
    }

    public static <T extends Value> Optional<List<T>> getArrayElements(ArrayValue arrayValue,
                                                                       Class<T> elementType,
                                                                       boolean throwOnError,
                                                                       boolean includeNulls) throws ClassCastException {
        return getArrayElements(arrayValue, elementType, throwOnError, includeNulls, value -> value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Value, U> Optional<List<U>> getArrayElements(ArrayValue arrayValue,
                                                                          Class<T> elementType,
                                                                          boolean throwOnError,
                                                                          boolean includeNulls,
                                                                          Function<T, U> converter)
        throws ClassCastException, IllegalArgumentException {

        if (arrayValue == null || arrayValue.isEmpty() || elementType == null) {
            return Optional.empty();
        }

        if (converter == null) {
            if (throwOnError) {
                throw new IllegalArgumentException("Converter cannot be null");
            }
            return Optional.empty();
        }

        Stream<Value> values = arrayValue.stream();
        if (!throwOnError) {
            values = values.filter(value -> value != null && value.getType().getModelType() == elementType);
        }

        Stream<U> stream = values.map(value -> (T)value).map(converter);

        if (!includeNulls) {
            stream = stream.filter(Objects::nonNull);
        }

        return Optional.of(stream.collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Value> Optional<T> getMetaItemValueOrThrow(AssetAttribute attribute,
                                                                        String name,
                                                                        Class<T> valueClazz,
                                                                        boolean throwIfMetaMissing,
                                                                        boolean throwIfValueMissing)
        throws IllegalArgumentException {

        Optional<MetaItem> metaItem = attribute.getMetaItem(name);

        if (!metaItem.isPresent()) {
            if (throwIfMetaMissing) {
                throw new IllegalArgumentException("Required meta item is missing: " + name);
            }

            return Optional.empty();
        }

        Optional<Value> value = metaItem.get().getValue();

        if (!value.isPresent()) {
            if (throwIfValueMissing) {
                throw new IllegalArgumentException("Meta item value is missing: " + name);
            }
            return Optional.empty();
        }

        if (value.get().getType().getModelType() != valueClazz) {
            throw new IllegalArgumentException("Meta item value is of incorrect type: expected="
                + valueClazz.getName() + "; actual=" + value.get().getType().getModelType().getName());
        }


        return Optional.of((T)value.get());
    }

    public static <T extends Value> Optional<T> convert(Value value, Class<T> toType) {
        return convert(value, ValueType.fromModelType(toType));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Value> Optional<T> convert(Value value, ValueType toType) {
        if (value == null) {
            return Optional.empty();
        }

        T outputValue = null;

        if (toType == value.getType()) {
            outputValue = (T) value;
        } else if (toType == ValueType.STRING) {
            outputValue = (T) Values.create(value.toString());
        } else {
            switch (value.getType()) {

                case STRING:
                    switch (toType) {

                        case NUMBER:
                            try {
                                double dbl = Double.parseDouble(value.toString());
                                outputValue = (T) Values.create(dbl);
                            } catch (NumberFormatException e) {
                                return Optional.empty();
                            }
                            break;
                        case BOOLEAN:
                            outputValue = (T) Values.create(Boolean.parseBoolean(value.toString()));
                            break;
                    }
                    break;
                case NUMBER:
                    switch (toType) {

                        case BOOLEAN:
                            outputValue = (T) Values.getNumber(value)
                                .map(Double::intValue)
                                .map(i -> i == 0 ? Boolean.FALSE : i == 1 ? Boolean.TRUE : null)
                                .map(Values::create)
                                .orElse(null);
                            break;
                    }
                    break;
                case BOOLEAN:

                    switch (toType) {

                        case NUMBER:
                            outputValue = (T) Values.create(((BooleanValue)value).getBoolean() ? 1 : 0);
                            break;
                    }
                    break;
            }
        }

        return Optional.ofNullable(outputValue);
    }
}
