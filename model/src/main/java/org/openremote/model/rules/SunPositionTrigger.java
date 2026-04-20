/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.model.rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.geo.GeoJSONPoint;

public class SunPositionTrigger {

    public static final String TWILIGHT_PREFIX = "TWILIGHT_";
    public static final String MORNING_TWILIGHT_PREFIX = "TWILIGHT_MORNING_";
    public static final String EVENING_TWILIGHT_PREFIX = "TWILIGHT_EVENING_";

    /**
     * The astronomical position of the sun
     */
    public enum Position {
        SUNRISE,
        SUNSET,
        TWILIGHT_MORNING_VISUAL,
        TWILIGHT_MORNING_VISUAL_LOWER,
        TWILIGHT_MORNING_HORIZON,
        TWILIGHT_MORNING_CIVIL,
        TWILIGHT_MORNING_NAUTICAL,
        TWILIGHT_MORNING_ASTRONOMICAL,
        TWILIGHT_MORNING_GOLDEN_HOUR,
        TWILIGHT_MORNING_BLUE_HOUR,
        TWILIGHT_MORNING_NIGHT_HOUR,
        TWILIGHT_EVENING_VISUAL,
        TWILIGHT_EVENING_VISUAL_LOWER,
        TWILIGHT_EVENING_HORIZON,
        TWILIGHT_EVENING_CIVIL,
        TWILIGHT_EVENING_NAUTICAL,
        TWILIGHT_EVENING_ASTRONOMICAL,
        TWILIGHT_EVENING_GOLDEN_HOUR,
        TWILIGHT_EVENING_BLUE_HOUR,
        TWILIGHT_EVENING_NIGHT_HOUR,
    }

    Position position;
    GeoJSONPoint location;
    Integer offsetMins;

    @JsonCreator
    public SunPositionTrigger(Position position, GeoJSONPoint location, Integer offsetMins) {
        this.position = position;
        this.location = location;
        this.offsetMins = offsetMins;
    }

    public Position getPosition() {
        return position;
    }

    public GeoJSONPoint getLocation() {
        return location;
    }

    public Integer getOffsetMins() {
        return offsetMins;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "position=" + position +
            ", location=" + location +
            ", offsetMins=" + offsetMins +
            '}';
    }
}
