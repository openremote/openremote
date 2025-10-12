/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.openweathermap;

/*
 * These are the core weather fields that are returned by the OpenWeatherMap API
 * It is used by the {@link OpenWeatherMapAgentLink} to determine which field to use as the data source for
 * updating the attribute.
 */
public enum OpenWeatherMapField {
    TEMP,                  // main.temp
    TEMP_FEELS_LIKE,       // main.feels_like
    TEMP_MIN,              // main.temp_min
    TEMP_MAX,              // main.temp_max
    PRESSURE,              // main.pressure
    HUMIDITY,              // main.humidity
    CLOUD_COVERAGE,        // clouds.all (%)
    WIND_SPEED,            // wind.speed (m/s)
    WIND_DEG,              // wind.deg (degrees)
    WIND_GUST;             // wind.gust (m/s)

    public String getProperty() {
        return name().toLowerCase();
    }

}
