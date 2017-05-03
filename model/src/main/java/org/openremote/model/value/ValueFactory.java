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

/**
 * Factory interface for parsing and creating values.
 */
public interface ValueFactory {

    StringValue create(String string);

    NumberValue create(double number);

    BooleanValue create(boolean bool);

    ArrayValue createArray();

    ObjectValue createObject();

    <T extends Value> Optional<T> parse(String jsonString) throws ValueException;
}
