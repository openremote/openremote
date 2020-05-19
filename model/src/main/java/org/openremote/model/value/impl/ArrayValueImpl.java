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
package org.openremote.model.value.impl;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.ModelModule;
import org.openremote.model.value.*;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

@JsonSerialize(using = ModelModule.ValueJsonSerializer.class)
public class ArrayValueImpl extends ValueImpl implements ArrayValue {

    final private transient ValueFactory factory;
    private transient ArrayList<Value> values = new ArrayList<>();

    public ArrayValueImpl(ValueFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean indexContainsNull(int index) {
        return index >= 0 && values.size() > index && values.get(index) == null;
    }

    @Override
    public Optional<Value> get(int index) {
        return index >= 0 && values.size() > index ? Optional.ofNullable(values.get(index)) : Optional.empty();
    }

    @Override
    public Optional<String> getString(int index) {
        return get(index)
                .filter(value -> value.getType() == ValueType.STRING && value instanceof StringValue)
                .map(value -> ((StringValue) value).getString());
    }

    @Override
    public Optional<Boolean> getBoolean(int index) {
        return get(index)
                .filter(value -> value.getType() == ValueType.BOOLEAN && value instanceof BooleanValue)
                .map(value -> ((BooleanValue) value).getBoolean());
    }

    @Override
    public Optional<Double> getNumber(int index) {
        return get(index)
                .filter(value -> value.getType() == ValueType.NUMBER && value instanceof NumberValue)
                .map(value -> ((NumberValue) value).getNumber());
    }

    @Override
    public Optional<ArrayValue> getArray(int index) {
        return get(index)
                .filter(value -> value.getType() == ValueType.ARRAY && value instanceof ArrayValue)
                .map(value -> ((ArrayValue) value));
    }

    @Override
    public Optional<ObjectValue> getObject(int index) {
        return get(index)
                .filter(value -> value.getType() == ValueType.OBJECT && value instanceof ObjectValue)
                .map(value -> ((ObjectValue) value));
    }

    @Override
    public Stream<Value> stream() {
        return values.stream();
    }

    @Override
    public ValueType getType() {
        return ValueType.ARRAY;
    }

    @Override
    public int length() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public ArrayValue remove(int index) {
        values.remove(index);
        return this;
    }

    @Override
    public ArrayValue set(int index, Value value) {
        if (value == null && index >= 0 && index < values.size()) {
            values.remove(index);
        } else if (index == values.size()) {
            values.add(value);
        } else {
            values.set(index, value);
        }
        return this;
    }

    @Override
    public ArrayValue set(int index, String string) {
        set(index, factory.create(string));
        return this;
    }

    @Override
    public ArrayValue set(int index, double number) {
        set(index, factory.create(number));
        return this;
    }

    @Override
    public ArrayValue set(int index, boolean bool) {
        set(index, factory.create(bool));
        return this;
    }

    @Override
    public ArrayValue add(Value value) {
        values.add(value);
        return this;
    }

    @Override
    public ArrayValue add(String string) {
        return add(factory.create(string));
    }

    @Override
    public ArrayValue add(double number) {
        return add(factory.create(number));
    }

    @Override
    public ArrayValue add(boolean bool) {
        return add(factory.create(bool));
    }

    @Override
    public ArrayValue add(int index, Value value) {
        values.add(index, value);
        return this;
    }

    @Override
    public ArrayValue addAll(Value... values) {
        if (values != null) {
            for (Value value : values) {
                set(length(), value);
            }
        }
        return this;
    }

    @Override
    public ArrayValue addAll(String... strings) {
        if (strings != null) {
            for (String string : strings) {
                set(length(), factory.create(string));
            }
        }
        return this;
    }

    @Override
    public ArrayValue addAll(double... numbers) {
        if (numbers != null) {
            for (double number : numbers) {
                set(length(), factory.create(number));
            }
        }
        return this;
    }

    @Override
    public ArrayValue addAll(boolean... bools) {
        if (bools != null) {
            for (boolean bool : bools) {
                set(length(), factory.create(bool));
            }
        }
        return this;
    }

    @Override
    public ArrayValue deepCopy() {
        return Values.<ArrayValue>parse(toJson()).orElseThrow(() -> new IllegalStateException("Error copying array value"));
    }

    @Override
    public boolean contains(String string, boolean ignoreCase) {
        if (ignoreCase) {
            return values.stream().anyMatch(val -> Values.getString(val).orElseThrow(() -> new ValueException("Value should be string")).equalsIgnoreCase(string));
        } else {
            return values.contains(factory.create(string));
        }
    }

    @Override
    public boolean contains(String string) {
        return contains(string, false);
    }

    @Override
    public boolean contains(double number) {
        return values.contains(factory.create(number));
    }

    @Override
    public int indexOf(String string) {
        return values.indexOf(factory.create(string));
    }

    @Override
    public String toJson() throws ValueException {
        return ValueUtil.stringify(this);
    }

    @Override
    public void traverse(ValueVisitor visitor, ValueContext ctx) throws ValueException {
        if (visitor.visit(this, ctx)) {
            ArrayValueContext arrayCtx = new ArrayValueContext(this);
            for (int i = 0; i < length(); i++) {
                arrayCtx.setCurrentIndex(i);
                if (visitor.visitIndex(arrayCtx.getCurrentIndex(), arrayCtx)) {
                    Optional<Value> value = get(i);
                    visitor.accept(value.orElse(null), arrayCtx);
                    arrayCtx.setFirst(false);
                }
            }
        }
        visitor.endVisit(this, ctx);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof ArrayValueImpl))
            return false;
        ArrayValueImpl that = (ArrayValueImpl) o;

        if (length() != that.length())
            return false;
        for (int i = 0; i < length(); i++) {
            if (!get(i).equals(that.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result * 5;
        for (int i = 0; i < length(); i++) {
            result = result * 5 + (i + get(i).hashCode());
        }
        return result;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
