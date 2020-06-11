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

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gwt.core.shared.GwtIncompatible;
import com.google.inject.internal.util.$FinalizableWeakReference;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.base.Any;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.value.impl.ValueFactoryImpl;

import java.io.IOException;
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

    public static final String NULL_LITERAL = "null";

    public static StringValue create(String string) {
        return instance().create(string);
    }

    public static BooleanValue create(boolean bool) {
        return instance().create(bool);
    }

    public static ArrayValue createArray(Value...values) {
        ArrayValue arrayValue = createArray();
        arrayValue.addAll(values);
        return arrayValue;
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

    @JsMethod(namespace = "Model.Values")
    public static Value parseOrNull(String jsonString) {
        try {
            return parse(jsonString).orElse(null);
        }
        catch (Exception ignored) {}
        return null;
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

        return convertToValue(value, BooleanValue.class)
            .map(BooleanValue::getBoolean);
    }

    /**
     * Attempts to coerce the value into an integer (where it makes sense)
     */
    public static Optional<Integer> getIntegerCoerced(Value value) {

        return convertToValue(value, NumberValue.class)
            .map(NumberValue::getNumber)
            .map(Double::intValue);
    }

    /**
     * Attempts to coerce the value into a long (where it makes sense)
     */
    public static Optional<Long> getLongCoerced(Value value) {

        return convertToValue(value, NumberValue.class)
            .map(NumberValue::getNumber)
            .map(Double::longValue);
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
    public static <T extends Value> Optional<T> getMetaItemValueOrThrow(Attribute attribute,
                                                                        MetaItemDescriptor metaItemDescriptor,
                                                                        boolean throwIfMetaMissing,
                                                                        boolean throwIfValueMissing)
        throws IllegalArgumentException {
        return getMetaItemValueOrThrow(
                attribute,
                metaItemDescriptor.getUrn(),
                metaItemDescriptor.getValueType().getModelType(),
                throwIfMetaMissing,
                throwIfValueMissing);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Value> Optional<T> getMetaItemValueOrThrow(Attribute attribute,
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

        if (valueClazz != Value.class && value.get().getType().getModelType() != valueClazz) {
            throw new IllegalArgumentException("Meta item value is of incorrect type: expected="
                + valueClazz.getName() + "; actual=" + value.get().getType().getModelType().getName());
        }

        return Optional.of((T)value.get());
    }

    public static <T extends Value> Optional<T> convertToValue(Value value, Class<T> toType) {
        return convertToValue(value, ValueType.fromModelType(toType));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Value> Optional<T> convertToValue(Value value, ValueType toType) {
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
                            if ("ON".equalsIgnoreCase(value.toString())) {
                                outputValue = (T) Values.create(true);
                            } else if ("OFF".equalsIgnoreCase(value.toString())) {
                                outputValue = (T) Values.create(false);
                            } else {
                                outputValue = (T) Values.create(Boolean.parseBoolean(value.toString()));
                            }
                            break;
                        case ARRAY:
                        case OBJECT:
                            try {
                                outputValue = (T) Values.parse(value.toString()).orElse(null);
                            } catch (ValueException e) {
                                return Optional.empty();
                            }
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
                case ARRAY:

                    // Only works when the array has a single value
                    ArrayValue arrayValue = (ArrayValue)value;
                    if (arrayValue.length() == 1) {

                        Value firstValue = arrayValue.get(0).orElse(null);

                        if (firstValue != null) {
                            return convertToValue(firstValue, toType);
                        }
                    }
            }
        }

        return Optional.ofNullable(outputValue);
    }

    @JsIgnore
    @GwtIncompatible
    public static <T extends Value> Optional<T> convertToValue(Object object, ObjectWriter writer) {
        try {
            return Optional.of(convertToValueOrThrow(object, writer));
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    @JsIgnore
    @GwtIncompatible
    public static <T extends Value> T convertToValueOrThrow(Object object, ObjectWriter writer) throws IOException {
        if (object == null || writer == null) {
            throw new IllegalArgumentException("Value and writer must be defined");
        }

        Value v;
        v = parse(writer.writeValueAsString(object)).orElse(null);
        return (T)v;
    }

    @JsIgnore
    @GwtIncompatible
    public static <T> Optional<T> convertFromValue(Value value, Class<T> clazz, ObjectReader reader) {
        try {
            return Optional.of(convertFromValueOrThrow(value, clazz, reader));
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    @JsIgnore
    @GwtIncompatible
    public static <T> T convertFromValueOrThrow(Value value, Class<T> clazz, ObjectReader reader) throws IOException {
        if (value == null || clazz == null || reader == null) {
            throw new IllegalArgumentException("Value, class and reader must be defined");
        }

        String str = value.toJson();
        return reader.forType(clazz).readValue(str);
    }
}
