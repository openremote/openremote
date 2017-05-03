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

import org.openremote.model.value.*;

import java.util.Optional;

public class ValueFactoryImpl implements ValueFactory {

    public static final ValueFactoryImpl INSTANCE = new ValueFactoryImpl();

    @Override
    public StringValue create(String string) {
        assert string != null;
        return new StringValueImpl(string);
    }

    @Override
    public NumberValue create(double number) {
        return new NumberValueImpl(number);
    }

    @Override
    public BooleanValue create(boolean bool) {
        return new BooleanValueImpl(bool);
    }

    @Override
    public ArrayValue createArray() {
        return new ArrayValueImpl(this);
    }

    @Override
    public ObjectValue createObject() {
        return new ObjectValueImpl(this);
    }

    @Override
    public <T extends Value> Optional<T> parse(String jsonString) throws ValueException {
        if (jsonString.startsWith("(") && jsonString.endsWith(")")) {
            // some clients send in (json) expecting an eval is required
            jsonString = jsonString.substring(1, jsonString.length() - 1);
        }
        return new ValueTokenizer(this, jsonString).nextValue();
    }
}
