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
package org.openremote.model;

import elemental.json.*;

import java.time.LocalDateTime;

/**
 * Base class for all model classes which have to internally store data in a
 * {@link JsonObject} that has a <code>value</code> field that accepts any
 * {@link JsonValue} and a <code>valueTimestamp</code> timestamp field, in
 * milliseconds since the Unix epoch, of the most recent value change.
 */
public abstract class AbstractValueTimestampHolder<CHILD extends AbstractValueTimestampHolder> extends AbstractValueHolder<CHILD> {

    public static final String VALUE_TIMESTAMP_FIELD_NAME = "valueTimestamp";

    public AbstractValueTimestampHolder(JsonObject jsonObject) {
        super(jsonObject);
    }

    public boolean hasValueTimestamp() {
        return jsonObject.hasKey(VALUE_TIMESTAMP_FIELD_NAME) && jsonObject.get(VALUE_TIMESTAMP_FIELD_NAME).getType() != JsonType.NULL;
    }

    /**
     * @return <code>-1</code> if there is no timestamp.
     */
    public long getValueTimestamp() {
        return hasValueTimestamp() ? new Double(jsonObject.get(VALUE_TIMESTAMP_FIELD_NAME).asNumber()).longValue() : -1;
    }

    /**
     * Sets the value timestamp to current system time.
     */
    public CHILD setValueTimestamp() {
        return setValueTimestamp(System.currentTimeMillis());
    }

    /**
     * Sets the value timestamp to given time.
     */
    @SuppressWarnings("unchecked")
    public CHILD setValueTimestamp(long timestamp) {
        jsonObject.put(VALUE_TIMESTAMP_FIELD_NAME, Json.create(timestamp));
        return (CHILD) this;
    }

    /**
     * Removes the value and sets the timestamp to current system time.
     */
    @Override
    public void clearValue() {
        super.clearValue();
        setValueTimestamp();
    }

    /**
     * Sets the value and the timestamp to given time.
     */
    public CHILD setValue(JsonValue value, long timestamp) throws IllegalArgumentException {
        CHILD result = setValue(value);
        setValueTimestamp(timestamp);
        return result;
    }

    /**
     * Sets the value and the timestamp to current system time.
     */
    @Override
    public CHILD setValue(JsonValue value) throws IllegalArgumentException {
        CHILD result = super.setValue(value);
        setValueTimestamp();
        return result;
    }

    /**
     * Use this in Javascript, can't do null checks on JsonValue.
     */
    public void setValueUnchecked(JsonValue value, long timestamp) {
        jsonObject.put(VALUE_FIELD_NAME, value);
        setValueTimestamp(timestamp);
    }

}
