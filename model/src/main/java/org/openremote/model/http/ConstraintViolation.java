/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.http;

import jsinterop.annotations.*;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class ConstraintViolation {

    @JsType
    public enum Type {CLASS, FIELD, PROPERTY, PARAMETER, RETURN_VALUE}

    @JsProperty
    protected Type constraintType;

    @JsProperty
    protected String path;

    @JsProperty
    protected String message;

    @JsProperty
    protected String value;

    @JsOverlay
    final public Type getConstraintType() {
        return constraintType;
    }

    @JsOverlay
    final public void setConstraintType(Type constraintType) {
        this.constraintType = constraintType;
    }

    @JsOverlay
    final public String getPath() {
        return path;
    }

    @JsOverlay
    final public void setPath(String path) {
        this.path = path;
    }

    @JsOverlay
    final public String getMessage() {
        return message;
    }

    @JsOverlay
    final public void setMessage(String message) {
        this.message = message;
    }

    @JsOverlay
    final public String getValue() {
        return value;
    }

    @JsOverlay
    final public void setValue(String value) {
        this.value = value;
    }

    @JsOverlay
    @Override
    final public String toString() {
        return getClass().getSimpleName() + "{" +
            "constraintType=" + constraintType +
            ", path='" + path + '\'' +
            ", message='" + message + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}