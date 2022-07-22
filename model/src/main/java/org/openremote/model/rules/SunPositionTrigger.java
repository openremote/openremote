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

import org.openremote.model.geo.GeoJSONPoint;

public class SunPositionTrigger {

    /**
     * The astronomical position of the sun
     */
    public enum Position {
        SUNRISE,
        SUNSET,
        TWILIGHT_VISUAL,
        TWILIGHT_VISUAL_LOWER,
        TWILIGHT_HORIZON,
        TWILIGHT_CIVIL,
        TWILIGHT_NAUTICAL,
        TWILIGHT_ASTRONOMICAL,
        TWILIGHT_GOLDEN_HOUR,
        TWILIGHT_BLUE_HOUR,
        TWILIGHT_NIGHT_HOUR
    }

    Position position;
    GeoJSONPoint location;
    Integer offsetMins;


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
}
