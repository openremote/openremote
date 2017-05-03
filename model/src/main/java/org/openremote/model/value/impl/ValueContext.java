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

import org.openremote.model.value.Value;

/**
 * Represents the current location where a value is stored, and allows
 * the value's replacement or deletion.
 */
abstract class ValueContext {

    private Value value;

    private boolean isFirst = true;

    ValueContext(Value value) {
        this.value = value;
    }

    /**
     * Return the underlying Value (Array or Object) that backs the
     * context.
     */
    public Value getValue() {
        return value;
    }

    /**
     * Whether or not the current context location within the value is the first
     * key or array index.
     */
    public boolean isFirst() {
        return isFirst;
    }

    /**
     * Remove the current array index or key.
     */
    public abstract void removeMe();

    /**
     * Replace the current location's value with a double.
     */
    public abstract void replaceMe(double d);

    /**
     * Replace the current location's value with a String.
     */
    public abstract void replaceMe(String d);

    /**
     * Replace the current location's value with a boolean.
     */
    public abstract void replaceMe(boolean d);

    /**
     * Replace the current location's value with a value.
     */
    public abstract void replaceMe(Value value);

    public void setFirst(boolean first) {
        isFirst = first;
    }

    void setValue(Value value) {
        this.value = value;
    }
}
