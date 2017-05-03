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

import org.openremote.model.value.Null;
import org.openremote.model.value.ValueType;

public class NullImpl extends ValueImpl implements Null {

    public static final Null NULL_INSTANCE = new NullImpl();

    @Override
    public double asNumber() {
        return 0;
    }

    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public String asString() {
        return "null";
    }

    public Null getObject() {
        return NULL_INSTANCE;
    }

    public ValueType getType() {
        return ValueType.NULL;
    }

    @Override
    public void traverse(ValueVisitor visitor, ValueContext ctx) {
        visitor.visitNull(ctx);
    }

    public String toJson() {
        return "null";
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof NullImpl;
    }

    @Override
    public int hashCode() {
        return 31 * 31;
    }
}
