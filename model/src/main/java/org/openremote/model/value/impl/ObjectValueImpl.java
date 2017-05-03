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

import java.util.*;

public class ObjectValueImpl extends ValueImpl implements ObjectValue {

    private static List<String> stringifyOrder(String[] keys) {
        List<String> toReturn = new ArrayList<>();
        List<String> nonNumeric = new ArrayList<>();
        for (String key : keys) {
            if (key.matches("\\d+")) {
                toReturn.add(key);
            } else {
                nonNumeric.add(key);
            }
        }
        Collections.sort(toReturn);
        toReturn.addAll(nonNumeric);
        return toReturn;
    }

    private transient ValueFactory factory;
    private transient Map<String, Value> map = new LinkedHashMap<>();

    public ObjectValueImpl(ValueFactory factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Value> Optional<T> get(String key) {
        return map.containsKey(key) ? Optional.of((T) map.get(key)) : Optional.empty();
    }

    @Override
    public Map<String, Object> getObject() {
        Map<String, Object> obj = new HashMap<>();
        for (Map.Entry<String, Value> e : map.entrySet()) {
            obj.put(e.getKey(), ((ValueImpl) e.getValue()).getObject());
        }
        return obj;
    }

    @Override
    public ValueType getType() {
        return ValueType.OBJECT;
    }

    @Override
    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    @Override
    public String[] keys() {
        return map.keySet().toArray(new String[map.size()]);
    }

    @Override
    public void put(String key, Value value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    @Override
    public void put(String key, String value) {
        if (value == null) {
            remove(key);
        } else {
            put(key, factory.create(value));
        }
    }

    @Override
    public void put(String key, double value) {
        put(key, factory.create(value));
    }

    @Override
    public void put(String key, boolean bool) {
        put(key, factory.create(bool));
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public String toJson() throws ValueException {
        return ValueUtil.stringify(this);
    }

    @Override
    public void traverse(ValueVisitor visitor, ValueContext ctx) throws ValueException {
        if (visitor.visit(this, ctx)) {
            ObjectValueContext objCtx = new ObjectValueContext(this);
            for (String key : stringifyOrder(keys())) {
                objCtx.setCurrentKey(key);
                if (visitor.visitKey(objCtx.getCurrentKey(), objCtx)) {
                    Optional<Value> value = get(key);
                    if (value.isPresent()) {
                        visitor.accept(value.get(), objCtx);
                        objCtx.setFirst(false);
                    }
                }
            }
        }
        visitor.endVisit(this, ctx);
    }

    /**
     * Compare two {@link ObjectValue} instances by actual value.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof ObjectValueImpl))
            return false;
        ObjectValueImpl that = (ObjectValueImpl) o;

        if (!this.map.keySet().equals(that.map.keySet()))
            return false;

        for (Map.Entry<String, Value> entry : this.map.entrySet()) {
            Value mapAValue = entry.getValue();
            Value mapBValue = that.map.get(entry.getKey());
            if (!mapAValue.equals(mapBValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result * 3;
        result = result * getObject().hashCode();
        return result;
    }

}
