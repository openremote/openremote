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

import org.openremote.model.value.NumberValue;
import org.openremote.model.value.ValueType;

public class NumberValueImpl extends ValueImpl implements NumberValue {

    private transient double number;

    public NumberValueImpl(double number) {
        this.number = number;
    }

    @Override
    public boolean asBoolean() {
        return !(Double.isNaN(getNumber()) || Math.abs(getNumber()) == 0.0);
    }

    @Override
    public double asNumber() {
        return getNumber();
    }

    @Override
    public String asString() {
        return toJson();
    }

    @Override
    public double getNumber() {
        return number;
    }

    @Override
    public Double getObject() {
        return getNumber();
    }

    @Override
    public ValueType getType() {
        return ValueType.NUMBER;
    }

    @Override
    public void traverse(ValueVisitor visitor, ValueContext ctx) {
        visitor.visit(getNumber(), ctx);
    }

    @Override
    public String toJson() {
        if (Double.isInfinite(number) || Double.isNaN(number)) {
            return "null";
        }
        String toReturn = String.valueOf(number);
        if (toReturn.endsWith(".0")) {
            toReturn = toReturn.substring(0, toReturn.length() - 2);
        }
        return toReturn;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof NumberValueImpl))
            return false;
        NumberValueImpl that = (NumberValueImpl) o;
        return getNumber() == that.getNumber();
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result * 11 + Double.hashCode(getNumber());
        return result;
    }
}
