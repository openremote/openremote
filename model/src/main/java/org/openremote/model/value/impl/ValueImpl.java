/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueException;

public abstract class ValueImpl implements Value {

    @Override
    public JavaScriptObject asNativeObject() throws ValueException {
        if (GWT.isClient())
            return JsonUtils.safeEval(this.toJson());
        else
            throw new ValueException("Not a GWT/JavaScript runtime environment");
    }

    public abstract void traverse(ValueVisitor visitor, ValueContext ctx) throws ValueException;

}
