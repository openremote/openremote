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
package org.openremote.model.asset;

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;

import java.util.Optional;

import static org.openremote.model.Constants.NAMESPACE;

/**
 * An {@link Attribute} that can be added to an {@link Asset} to indicate that the asset is time sensitive. This is to
 * be used for informational purposes to; for example, allow console apps to only show Assets that are 'active' at a
 * particular time.
 * <p>
 * The attribute name must exactly match {@value CALENDAR_EVENT_ATTRIBUTE_NAME} and its value should be an
 * {@link ObjectValue} that represents a {@link CalendarEvent}.
 */
public final class CalendarEventConfiguration {

    public static final String CALENDAR_EVENT_ATTRIBUTE_NAME = "urnOpenremoteCalendarEvent";

    private CalendarEventConfiguration() {
    }

    public static boolean isCalendarEventConfiguration(AssetAttribute attribute) {
        return attribute != null
            && attribute.getName().map(CALENDAR_EVENT_ATTRIBUTE_NAME::equals).orElse(false);
    }

    public static Optional<CalendarEvent> getCalendarEvent(Asset asset) {
        if (asset == null) {
            return Optional.empty();
        }

        return asset.getAttribute(CALENDAR_EVENT_ATTRIBUTE_NAME)
            .flatMap(AbstractValueHolder::getValue)
            .flatMap(CalendarEventConfiguration::getCalendarEvent);
    }

    public static Optional<CalendarEvent> getCalendarEvent(AssetAttribute attribute) {
        if (!isCalendarEventConfiguration(attribute)) {
            return Optional.empty();
        }

        return attribute.getValue().flatMap(CalendarEventConfiguration::getCalendarEvent);
    }


    public static Optional<CalendarEvent> getCalendarEvent(Value value) {
        if (value == null) {
            return Optional.empty();
        }

        return CalendarEvent.fromValue(value);
    }

    public static AssetAttribute toAttribute(CalendarEvent calendarEvent) {
        return new AssetAttribute(CALENDAR_EVENT_ATTRIBUTE_NAME, AttributeType.OBJECT, calendarEvent.toValue());
    }
}
