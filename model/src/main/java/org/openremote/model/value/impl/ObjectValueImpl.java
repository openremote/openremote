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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectValueImpl extends ValueImpl implements ObjectValue {

    private static List<String> stringifyOrder(String[] keys) {
        List<String> toReturn = new ArrayList<>();
        List<String> nonNumeric = new ArrayList<>();
        for (String key : keys) {
            if (key == null) {
                throw new IllegalStateException("Null key in JSON object: " + Arrays.toString(keys));
            }
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
        return map.containsKey(key) ? Optional.ofNullable(map.get(key)) : Optional.empty();
    }

    @Override
    public boolean keyContainsNull(String key) {
        return map.containsKey(key) && map.get(key) == null;
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
        return Arrays.stream(keys()).map(key -> new Pair<>(key, get(key).orElse(null)));
    }

    @Override
    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    @Override
    public boolean hasKeys() {
        return !map.isEmpty();
    }

    @Override
    public ObjectValue put(String key, Value value) {
        if (key == null) {
            throw new NullPointerException("Null keys are not allowed in JSON objects");
        }
        map.put(key, value);
        return this;
    }

    @Override
    public ObjectValue put(String key, String value) {
        if (key == null) {
            throw new NullPointerException("Null keys are not allowed in JSON objects");
        }
        map.put(key, factory.create(value));
        return this;
    }

    @Override
    public ObjectValue put(String key, double value) {
        if (key == null) {
            throw new NullPointerException("Null keys are not allowed in JSON objects");
        }
        put(key, factory.create(value));
        return this;
    }

    @Override
    public ObjectValue put(String key, boolean bool) {
        if (key == null) {
            throw new NullPointerException("Null keys are not allowed in JSON objects");
        }
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
                    visitor.accept(value.orElse(null), objCtx);
                    objCtx.setFirst(false);
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
        return equalsIgnoreKeys(that, key -> false);
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result * 3;
        result = result * map.hashCode();
        return result;
    }

    @Override
    public boolean equalsIgnoreKeys(ObjectValue that, Predicate<String> ignoreKeyPredicate) {
        if (!(that instanceof ObjectValueImpl))
            return false;
        ObjectValueImpl thatImpl = (ObjectValueImpl) that;

        Set<String> thisKeys = this.map.keySet().stream()
            .filter(key -> ignoreKeyPredicate == null || !ignoreKeyPredicate.test(key))
            .collect(Collectors.toSet());
        Set<String> thatKeys = thatImpl.map.keySet().stream()
            .filter(key -> ignoreKeyPredicate == null || !ignoreKeyPredicate.test(key))
            .collect(Collectors.toSet());

        if (!thisKeys.equals(thatKeys))
            return false;

        for (Map.Entry<String, Value> entry : this.map.entrySet()) {
            if (ignoreKeyPredicate != null && ignoreKeyPredicate.test(entry.getKey()))
                continue;
            Value mapAValue = entry.getValue();
            Value mapBValue = thatImpl.map.get(entry.getKey());
            if (mapAValue == mapBValue) {
                continue;
            }

            // If mapAValue is null then mapBValue cannot be null otherwise above equality check would return true
            if (mapAValue == null || !mapAValue.equals(mapBValue)) {
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
