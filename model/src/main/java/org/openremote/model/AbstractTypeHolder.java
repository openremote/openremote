/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model;

import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import java.util.Objects;
import java.util.Optional;

/**
 * Base class for all model classes which have to internally store data in an
 * {@link ObjectValue} that has a <code>type</code> field that accepts a string.
 */
public abstract class AbstractTypeHolder {

    protected static final String TYPE_FIELD_NAME = "type";

    final protected ObjectValue objectValue;

    public AbstractTypeHolder() {
        this(Values.createObject());
    }

    public AbstractTypeHolder(String type) {
        this(Values.createObject(), type);
    }

    public AbstractTypeHolder(ObjectValue objectValue) {
        Objects.requireNonNull(objectValue);
        this.objectValue = objectValue;
    }

    public AbstractTypeHolder(ObjectValue objectValue, String type) {
        Objects.requireNonNull(objectValue);
        this.objectValue = objectValue;
        setType(type);
    }

    public ObjectValue getObjectValue() {
        return objectValue;
    }

    public void setType(String type) {
        objectValue.put(TYPE_FIELD_NAME, type);
    }

    public Optional<String> getType() {
        return objectValue.getString(TYPE_FIELD_NAME);
    }

    @Override
    public String toString() {
        return objectValue.toJson();
    }
}
