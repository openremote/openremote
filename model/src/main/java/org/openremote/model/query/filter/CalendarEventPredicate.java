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
package org.openremote.model.query.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import java.util.Date;

/**
 * Can be applied to {@link org.openremote.model.attribute.Attribute}s of type
 * {@link org.openremote.model.attribute.AttributeValueType#CALENDAR_EVENT}.
 */
public class CalendarEventPredicate implements ValuePredicate {

    public static final String name = "calendar-event";
    public Date timestamp;

    @JsonCreator
    public CalendarEventPredicate(@JsonProperty("timestamp") Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public ObjectValue toModelValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("predicateType", name);
        objectValue.put("timestamp", Values.create(timestamp.getTime()));
        return objectValue;
    }
}
