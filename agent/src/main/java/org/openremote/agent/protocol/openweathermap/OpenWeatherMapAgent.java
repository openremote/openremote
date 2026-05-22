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

import java.util.Optional;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;

@Entity
public class OpenWeatherMapAgent
    extends Agent<OpenWeatherMapAgent, OpenWeatherMapProtocol, OpenWeatherMapAgentLink> {

  public static final AgentDescriptor<
          OpenWeatherMapAgent, OpenWeatherMapProtocol, OpenWeatherMapAgentLink>
      DESCRIPTOR =
          new AgentDescriptor<>(
              OpenWeatherMapAgent.class,
              OpenWeatherMapProtocol.class,
              OpenWeatherMapAgentLink.class);

  public static final AttributeDescriptor<String> API_KEY =
      new AttributeDescriptor<>("ApiKey", ValueType.TEXT);

  // Read only attribution, required by OpenWeatherMap API
  // Set during protocol start
  public static final AttributeDescriptor<String> ATTRIBUTION =
      new AttributeDescriptor<>(
          "Attribution", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));

  // Button to provision a weather asset with the appropriate agent links
  public static final AttributeDescriptor<Boolean> PROVISION_WEATHER_ASSET =
      new AttributeDescriptor<>("ProvisionWeatherAsset", ValueType.BOOLEAN);

  protected OpenWeatherMapAgent() {}

  public OpenWeatherMapAgent(String name) {
    super(name);
  }

  public Optional<String> getAPIKey() {
    return getAttributes().getValue(API_KEY);
  }

  public OpenWeatherMapAgent setAPIKey(String value) {
    getAttributes().getOrCreate(API_KEY).setValue(value);
    return this;
  }

  public Optional<Boolean> getProvisionWeatherAsset() {
    return getAttributes().getValue(PROVISION_WEATHER_ASSET);
  }

  public OpenWeatherMapAgent setProvisionWeatherAsset(Boolean value) {
    getAttributes().getOrCreate(PROVISION_WEATHER_ASSET).setValue(value);
    return this;
  }

  @Override
  public OpenWeatherMapProtocol getProtocolInstance() {
    return new OpenWeatherMapProtocol(this);
  }
}
