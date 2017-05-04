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

import org.openremote.model.util.Pair;
import org.openremote.model.value.*;

import java.util.*;
import java.util.stream.Stream;

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

    @Override
    public Optional<Value> get(String key) {
        return map.containsKey(key) ? Optional.of(map.get(key)) : Optional.empty();
    }

    @Override
    public Optional<String> getString(String key) {
        return get(key).flatMap(Values::getString);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return get(key).flatMap(Values::getBoolean);
    }

    @Override
    public Optional<Double> getNumber(String key) {
        return get(key).flatMap(Values::getNumber);
    }

    @Override
    public Optional<ArrayValue> getArray(String key) {
        return get(key).flatMap(Values::getArray);
    }

    @Override
    public Optional<ObjectValue> getObject(String key) {
        return get(key).flatMap(Values::getObject);
    }

    @Override
    public ValueType getType() {
        return ValueType.OBJECT;
    }

    @Override
    public String[] keys() {
        return map.keySet().toArray(new String[map.size()]);
    }

    @Override
    public Stream<Pair<String, Value>> stream() {
        return Arrays.stream(keys()).map(key -> new Pair<>(key, get(key).get()));
    }

    @Override
    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    @Override
    public ObjectValue put(String key, Value value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
        return this;
    }

    @Override
    public ObjectValue put(String key, String value) {
        if (value == null) {
            remove(key);
        } else {
            put(key, factory.create(value));
        }
        return this;
    }

    @Override
    public ObjectValue put(String key, double value) {
        put(key, factory.create(value));
        return this;
    }

    @Override
    public ObjectValue put(String key, boolean bool) {
        put(key, factory.create(bool));
        return this;
    }

    @Override
    public ObjectValue remove(String key) {
        map.remove(key);
        return this;
    }

    @Override
    public ObjectValue deepCopy() {
        return Values.<ObjectValue>parse(toJson()).orElseThrow(() -> new IllegalStateException("Error copying object value"));
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
        return equalsIgnoreKeys(that);
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result * 3;
        result = result * map.hashCode();
        return result;
    }

    @Override
    public boolean equalsIgnoreKeys(ObjectValue that, String... ignoreKeys) {
        if (!(that instanceof ObjectValueImpl))
            return false;
        ObjectValueImpl objectValueImpl = (ObjectValueImpl)that;

        if (!this.map.keySet().equals(objectValueImpl.map.keySet()))
            return false;

        for (Map.Entry<String, Value> entry : this.map.entrySet()) {
            if (Arrays.asList(ignoreKeys).contains(entry.getKey()))
                continue;
            Value mapAValue = entry.getValue();
            Value mapBValue = objectValueImpl.map.get(entry.getKey());
            if (!mapAValue.equals(mapBValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
