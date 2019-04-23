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

import jsinterop.base.Any;

/**
 * Base interface for all values. This is a Java API for a JSON model and instances
 * may be serialized into JSON text with {@link #toJson()} or parsed from JSON text
 * with {@link Values#parse(String)}.
 */
public interface Value {

    /**
     * Returns an enumeration representing the fundamental type.
     */
    ValueType getType();

    /**
     * Returns a serialized JSON string representing this value.
     */
    String toJson() throws ValueException;

    /**
     * If used in a GWT context, converts the object to a native
     * {@link Any} suitable for passing to native and {@link jsinterop.annotations.JsType} methods.
     * Otherwise, throws {@link ValueException}.
     */
    Any asAny() throws ValueException;
}
