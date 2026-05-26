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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.openremote.model.asset.agent.AgentLink;

import jakarta.validation.constraints.NotNull;

public class OpenWeatherMapAgentLink extends AgentLink<OpenWeatherMapAgentLink> {

  @NotNull @JsonPropertyDescription(
      "Select which weather property (e.g. temperature, humidity) to use as the data source")
  private OpenWeatherMapProperty weatherProperty;

  // For Hydrators
  protected OpenWeatherMapAgentLink() {}

  public OpenWeatherMapAgentLink(String id) {
    super(id);
  }

  public OpenWeatherMapProperty getWeatherProperty() {
    return weatherProperty;
  }

  public OpenWeatherMapAgentLink setWeatherProperty(OpenWeatherMapProperty weatherProperty) {
    this.weatherProperty = weatherProperty;
    return this;
  }
}
