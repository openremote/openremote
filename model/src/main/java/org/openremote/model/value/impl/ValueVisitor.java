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

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueException;

/**
 * A visitor for {@link Value} objects. For each unique value type, a callback is
 * invoked with a {@link ValueContext} that can be used to replace a value
 * or remove it.
 * <p>
 * For {@link ObjectValue} and {@link ArrayValue} types, the {@link #visitKey}
 * and {@link #visitIndex} methods are invoked respectively for each contained
 * value to determine if they should be processed or not.
 * <p>
 * Finally, the visit methods for Object and Array types returns a boolean that
 * determines whether or not to process its contained values.
 */
class ValueVisitor {

    private class ImmutableValueContext extends ValueContext {

        public ImmutableValueContext(Value node) {
            super(node);
        }

        @Override
        public void removeMe() {
            immutableError();
        }

        @Override
        public void replaceMe(double d) {
            immutableError();
        }

        @Override
        public void replaceMe(String d) {
            immutableError();
        }

        @Override
        public void replaceMe(boolean d) {
            immutableError();
        }

        @Override
        public void replaceMe(Value value) {
            immutableError();
        }

        private void immutableError() {
            throw new UnsupportedOperationException("Immutable context");
        }
    }

    public void accept(Value node) throws ValueException {
        accept(node, new ImmutableValueContext(node));
    }

    /**
     * Accept array or object type and visit its members.
     */
    public void accept(Value node, ValueContext ctx) throws ValueException {
        if (node == null) {
            visitNull(ctx);
            return;
        }
        ((ValueImpl) node).traverse(this, ctx);
    }

    /**
     * Called after every element of array has been visited.
     */
    public void endVisit(ArrayValue array, ValueContext ctx) {
    }

    /**
     * Called after every field of an object has been visited.
     *
     * @param object
     * @param ctx
     */
    public void endVisit(ObjectValue object, ValueContext ctx) {
    }

    /**
     * Called for numbers present in an object.
     */
    public void visit(double number, ValueContext ctx) {
    }

    /**
     * Called for strings present in an object.
     */
    public void visit(String string, ValueContext ctx) {
    }

    /**
     * Called for boolean present in an object.
     */
    public void visit(boolean bool, ValueContext ctx) {
    }

    /**
     * Called for null present in an object.
     */
    public void visitNull(ValueContext ctx) {
    }

    /**
     * Called for arrays present in an object. Return true if array
     * elements should be visited.
     *
     * @param array an array
     * @param ctx   a context to replace or delete the array
     * @return true if the array elements should be visited
     */
    public boolean visit(ArrayValue array, ValueContext ctx) throws ValueException {
        return true;
    }

    /**
     * Called for objects present in an object. Return true if object
     * fields should be visited.
     *
     * @param object an object
     * @param ctx    a context to replace or delete the object
     * @return true if object fields should be visited
     */
    public boolean visit(ObjectValue object, ValueContext ctx) throws ValueException {
        return true;
    }

    /**
     * Return true if the value for a given array index should be visited.
     *
     * @param index an index in an array
     * @param ctx   a context object used to delete or replace values
     * @return true if the value associated with the index should be visited
     */
    public boolean visitIndex(int index, ValueContext ctx) {
        return true;
    }

    /**
     * Return true if the value for a given object key should be visited.
     *
     * @param key a key in an object
     * @param ctx a context object used to delete or replace values
     * @return true if the value associated with the key should be visited
     */
    public boolean visitKey(String key, ValueContext ctx) {
        return true;
    }

}
