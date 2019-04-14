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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.ModelModule;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.ValueException;
import org.openremote.model.value.ValueType;

import java.util.Objects;

@JsonSerialize(using = ModelModule.ValueJsonSerializer.class)
public class StringValueImpl extends ValueImpl implements StringValue {

    private transient String string;

    public StringValueImpl(String string) {
        Objects.requireNonNull(string);
        this.string = string;
    }

    @Override
    public String getString() {
        return string;
    }

    @Override
    public ValueType getType() {
        return ValueType.STRING;
    }

    @Override
    public void traverse(ValueVisitor visitor, ValueContext ctx) {
        visitor.visit(getString(), ctx);
    }

    public String toJson() throws ValueException {
        return ValueUtil.quote(getString());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof StringValueImpl))
            return false;
        StringValueImpl that = (StringValueImpl) o;
        return getString().equals(that.getString());
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result * 13 + getString().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getString();
    }
}
