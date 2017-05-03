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

public interface ArrayValue extends Value {

    /**
     * Return the ith element of the array.
     */
    <T extends Value> T get(int index);

    /**
     * Return the ith element of the array (uncoerced). If the type is not an array,
     * this can result in runtime errors.
     */
    ArrayValue getArray(int index);

    /**
     * Return the ith element of the array (uncoerced). If the type is not a boolean,
     * this can result in runtime errors.
     */
    boolean getBoolean(int index);

    /**
     * Return the ith element of the array (uncoerced). If the type is not a number, this
     * can result in runtime errors.
     */
    double getNumber(int index);

    /**
     * Return the ith element of the array (uncoerced). If the type is not an object,,
     * this can result in runtime errors.
     */
    ObjectValue getObject(int index);

    /**
     * Return the ith element of the array (uncoerced). If the type is not a String, this
     * can result in runtime errors.
     */
    String getString(int index);

    /**
     * Length of the array.
     */
    int length();

    /**
     * Remove an element of the array at a particular index.
     */
    void remove(int index);

    /**
     * Set the value at index to be a given value.
     */
    void set(int index, Value value);

    /**
     * Set the value at index to be a String value.
     */
    void set(int index, String string);

    /**
     * Set the value at index to be a number value.
     */
    void set(int index, double number);

    /**
     * Set the value at index to be a boolean value.
     */
    void set(int index, boolean bool);
}
