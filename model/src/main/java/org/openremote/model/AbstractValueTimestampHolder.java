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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Optional;

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

    @JsonProperty("valueTimestamp")
    private Long getValueTimestampInternal() {
        return getValueTimestamp().orElse(null);
    }

    public Optional<Long> getValueTimestamp() {
        return getObjectValue().getNumber(VALUE_TIMESTAMP_FIELD_NAME).map(Double::longValue);
    }

    /**
     * Sets the value timestamp to given time.
     */
    @JsonProperty
    @SuppressWarnings("unchecked")
    public void setValueTimestamp(long timestamp) {
        getObjectValue().put(VALUE_TIMESTAMP_FIELD_NAME, Values.create(timestamp));
    }

    @Override
    public void clearValue() {
        super.clearValue();
        clearTimestamp();
    }

    public void clearTimestamp() {
        objectValue.remove(VALUE_TIMESTAMP_FIELD_NAME);
    }

    /**
     * Sets the value and the timestamp to given time.
     */
    public void setValue(Value value, long timestamp) {
        setValue(value);
        setValueTimestamp(timestamp);
    }
}
