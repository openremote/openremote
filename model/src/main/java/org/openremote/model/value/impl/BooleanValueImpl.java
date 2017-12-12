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

import org.openremote.model.value.BooleanValue;
import org.openremote.model.value.ValueType;

public class BooleanValueImpl extends ValueImpl implements BooleanValue {

    private static final BooleanValueImpl FALSE = new BooleanValueImpl(false);
    private static final BooleanValueImpl TRUE = new BooleanValueImpl(true);
    private transient boolean bool;

    public static BooleanValueImpl create(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    private BooleanValueImpl(boolean bool) {
        this.bool = bool;
    }

    @Override
    public boolean getBoolean() {
        return bool;
    }

    @Override
    public ValueType getType() {
        return ValueType.BOOLEAN;
    }

    @Override
    public void traverse(ValueVisitor visitor, ValueContext ctx) {
        visitor.visit(getBoolean(), ctx);
    }

    @Override
    public String toJson() {
        return String.valueOf(bool);
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result * 7 + (getBoolean() ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
