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

import org.openremote.model.util.Pair;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Object values can not have duplicate keys. For equality operations, objects
 * are equal if their key sets are equal and each key has the same value in both
 * instances.
 */
public interface ObjectValue extends Value {

    Optional<Value> get(String key);

    /**
     * A <code>String</code> or if the value type is not {@link ValueType#STRING}, an empty {@link Optional}.
     */
    Optional<String> getString(String key);

    /**
     * A <code>String</code> or if the value type is not {@link ValueType#BOOLEAN}, an empty {@link Optional}.
     */
    Optional<Boolean> getBoolean(String key);

    /**
     * A <code>Double</code> or if the value type is not {@link ValueType#NUMBER}, an empty {@link Optional}.
     */
    Optional<Double> getNumber(String key);

    /**
     * An <code>ArrayValue</code> or if the value type is not {@link ValueType#ARRAY}, an empty {@link Optional}.
     */
    Optional<ArrayValue> getArray(String key);

    /**
     * An <code>ObjectValue</code> or if the value type is not {@link ValueType#OBJECT}, an empty {@link Optional}.
     */
    Optional<ObjectValue> getObject(String key);

    String[] keys();

    Stream<Pair<String, Value>> stream();

    boolean hasKey(String key);

    ObjectValue put(String key, Value value);

    ObjectValue put(String key, String value);

    ObjectValue put(String key, double value);

    ObjectValue put(String key, boolean bool);

    ObjectValue remove(String key);

    ObjectValue deepCopy();

    /**
     *
     * @param ignoreKeyPredicate Ignore given keys in the comparison.
     */
    boolean equalsIgnoreKeys(ObjectValue that, Predicate<String> ignoreKeyPredicate);
}
