/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.openweathermap;

/*
 * These are the core weather properties that are returned by the OpenWeatherMap API
 * It is used by the {@link OpenWeatherMapAgentLink} to determine which property to use as the data source for
 * updating the attribute.
 */
public enum OpenWeatherMapProperty {
  TEMPERATURE, // Temperature
  ATMOSPHERIC_PRESSURE, // Atmospheric pressure
  HUMIDITY, // Humidity percentage
  CLOUD_COVERAGE, // Cloud coverage percentage
  WIND_SPEED, // Wind speed (m/s)
  WIND_DIRECTION, // Wind direction (degrees)
  WIND_GUST_SPEED, // Wind gust (m/s)
  PROBABILITY_OF_PRECIPITATION, // Probability of precipitation
  RAIN_AMOUNT, // Rain amount (mm)
  ULTRAVIOLET_INDEX // Ultraviolet index
}
