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

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

import java.util.Locale;

/**
 * Represents the type of the underlying value, the same as in JSON.
 */
@JsType(namespace = "Model", name = "ValueType")
public enum ValueType {

    ANY(Value.class),
    OBJECT(ObjectValue.class),
    ARRAY(ArrayValue.class),
    STRING(StringValue.class),
    NUMBER(NumberValue.class),
    BOOLEAN(BooleanValue.class);

    protected static final ValueType[] values = values();
    protected final Class<? extends Value> modelType;

    ValueType(Class<? extends Value> type) {
        this.modelType = type;
    }

    public Class getModelType() {
        return modelType;
    }

    public static ValueType fromModelType(Class<? extends Value> modelType) {
        for (ValueType valueType : values) {
            if (valueType.getModelType() == modelType) {
                return valueType;
            }
        }

        throw new IllegalStateException("Failed to get value type from model type");
    }

    @JsMethod
    public static ValueType fromString(String value) {
        return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
    }
}
