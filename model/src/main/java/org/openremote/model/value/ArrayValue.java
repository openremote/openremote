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

import java.util.Optional;
import java.util.stream.Stream;

public interface ArrayValue extends Value {

    boolean indexContainsNull(int index);

    Optional<Value> get(int index);

    Optional<String> getString(int index);

    Optional<Boolean> getBoolean(int index);

    Optional<Double> getNumber(int index);

    Optional<ArrayValue> getArray(int index);

    Optional<ObjectValue> getObject(int index);

    Stream<Value> stream();

    int length();

    boolean isEmpty();

    ArrayValue remove(int index);

    ArrayValue set(int index, Value value);

    ArrayValue set(int index, String string);

    ArrayValue set(int index, double number);

    ArrayValue set(int index, boolean bool);

    ArrayValue add(Value value);

    ArrayValue add(String string);

    ArrayValue add(double number);

    ArrayValue add(boolean bool);

    ArrayValue add(int index, Value value);

    ArrayValue addAll(Value... values);

    ArrayValue addAll(String... strings);

    ArrayValue addAll(double... numbers);

    ArrayValue addAll(boolean... bools);

    ArrayValue deepCopy();

    boolean contains(String string, boolean ignoreCase);

    boolean contains(String string);

    boolean contains(double number);

    int indexOf(String string);
}
