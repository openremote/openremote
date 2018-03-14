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
package org.openremote.model.validation;

import java.io.Serializable;

public class ConstraintViolation implements Serializable {

    public enum Type {CLASS, FIELD, PROPERTY, PARAMETER, RETURN_VALUE}

    protected Type constraintType;
    protected String path;
    protected String message;
    protected String value;

    public ConstraintViolation() {
    }

    public ConstraintViolation(Type constraintType, String path, String message) {
        this.constraintType = constraintType;
        this.path = path;
        this.message = message;
    }

    public ConstraintViolation(Type constraintType, String message) {
        this.constraintType = constraintType;
        this.message = message;
    }

    public Type getConstraintType() {
        return constraintType;
    }

    public void setConstraintType(Type constraintType) {
        this.constraintType = constraintType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "constraintType=" + constraintType +
            ", path='" + path + '\'' +
            ", message='" + message + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}