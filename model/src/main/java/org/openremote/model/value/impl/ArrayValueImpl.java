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

import org.openremote.model.value.*;

import java.util.ArrayList;
import java.util.List;

public class ArrayValueImpl extends ValueImpl implements ArrayValue {

    final private transient ValueFactory factory;
    private transient ArrayList<Value> values = new ArrayList<>();

    public ArrayValueImpl(ValueFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean asBoolean() {
        return true;
    }

    @Override
    public double asNumber() {
        switch (length()) {
            case 0:
                return 0;
            case 1:
                return get(0).asNumber();
            default:
                return Double.NaN;
        }
    }

    @Override
    public String asString() {
        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < length(); i++) {
            if (i > 0) {
                toReturn.append(", ");
            }
            toReturn.append(get(i).asString());
        }
        return toReturn.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value get(int index) {
        return values.get(index);
    }

    @Override
    public ArrayValue getArray(int index) {
        return (ArrayValue) get(index);
    }

    @Override
    public boolean getBoolean(int index) {
        return ((BooleanValue) get(index)).getBoolean();
    }

    @Override
    public double getNumber(int index) {
        return ((NumberValue) get(index)).getNumber();
    }

    @Override
    public ObjectValue getObject(int index) {
        return (ObjectValue) get(index);
    }

    @Override
    public List<Object> getObject() {
        List<Object> objs = new ArrayList<>();
        for (Value val : values) {
            objs.add(((ValueImpl) val).getObject());
        }
        return objs;
    }

    @Override
    public String getString(int index) {
        return ((StringValue) get(index)).getString();
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
    public void remove(int index) {
        values.remove(index);
    }

    @Override
    public void set(int index, Value value) {
        if (value == null) {
            value = factory.createNull();
        }
        if (index == values.size()) {
            values.add(index, value);
        } else {
            values.set(index, value);
        }
    }

    @Override
    public void set(int index, String string) {
        set(index, factory.create(string));
    }

    @Override
    public void set(int index, double number) {
        set(index, factory.create(number));
    }

    @Override
    public void set(int index, boolean bool) {
        set(index, factory.create(bool));
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
                    visitor.accept(get(i), arrayCtx);
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

}
