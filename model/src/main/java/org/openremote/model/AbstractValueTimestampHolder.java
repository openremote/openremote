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

import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

/**
 * Base class for all model classes which have to internally store data in an
 * {@link ObjectValue} that has a <code>value</code> field that accepts any
 * {@link Value} and a <code>valueTimestamp</code> timestamp field, in
 * milliseconds since the Unix epoch, of the most recent value change.
 */
public abstract class AbstractValueTimestampHolder extends AbstractValueHolder {

    public static final String VALUE_TIMESTAMP_FIELD_NAME = "valueTimestamp";

    public AbstractValueTimestampHolder(ObjectValue objectValue) {
        super(objectValue);
    }

    public boolean hasValueTimestamp() {
        return getObjectValue().hasKey(VALUE_TIMESTAMP_FIELD_NAME);
    }

    /**
     * @return <code>-1</code> if there is no timestamp.
     */
    public long getValueTimestamp() {
        return getObjectValue().getNumber(VALUE_TIMESTAMP_FIELD_NAME).map(Double::longValue).orElse(-1L);
    }

    /**
     * Sets the value timestamp to given time.
     */
    @SuppressWarnings("unchecked")
    public void setValueTimestamp(long timestamp) {
        getObjectValue().put(VALUE_TIMESTAMP_FIELD_NAME, Values.create(timestamp));
    }

    /**
     * Sets the value timestamp to current system time.
     */
    public void setValueTimestamp() {
        setValueTimestamp(System.currentTimeMillis());
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
     * Sets the value and the timestamp to system time.
     */
    @Override
    public void setValue(Value value) throws IllegalArgumentException {
        super.setValue(value);
        setValueTimestamp();
    }

    /**
     * Sets the value and the timestamp to given time.
     */
    public void setValue(Value value, long timestamp) throws IllegalArgumentException {
        setValue(value);
        setValueTimestamp(timestamp);
    }
}
