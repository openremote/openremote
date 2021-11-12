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

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueType;

import java.util.Date;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Can be applied to {@link org.openremote.model.attribute.Attribute}s of type {@link ValueType#CALENDAR_EVENT}.
 */
@JsonSchemaTitle("Calendar")
@JsonSchemaDescription("Predicate for calendar event values; will match based on whether the calendar event is active for the specified time.")
public class CalendarEventPredicate extends ValuePredicate {

    public static final String name = "calendar-event";
    public Date timestamp;

    @JsonCreator
    public CalendarEventPredicate(@JsonProperty("timestamp") Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier) {
        return obj -> {

            if (obj == null) {
                return true;
            }

            return ValueUtil.getValueCoerced(obj, CalendarEvent.class).map(calendarEvent -> {
                Date when = timestamp;

                Pair<Long, Long> nextOrActive = calendarEvent.getNextOrActiveFromTo(when);
                if (nextOrActive == null) {
                    return false;
                }

                return nextOrActive.key <= when.getTime() && nextOrActive.value > when.getTime();
            }).orElse(true);
        };
    }
}
