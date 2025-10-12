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

import java.util.Optional;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

@Entity
public class OpenWeatherMapAgent extends Agent<OpenWeatherMapAgent, OpenWeatherMapProtocol, OpenWeatherMapAgentLink> {

    public static final AgentDescriptor<OpenWeatherMapAgent, OpenWeatherMapProtocol, OpenWeatherMapAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            OpenWeatherMapAgent.class, OpenWeatherMapProtocol.class, OpenWeatherMapAgentLink.class);

    public static final AttributeDescriptor<String> API_KEY = new AttributeDescriptor<>(
            "openweathermapAPIKey", ValueType.TEXT);

    public static final AttributeDescriptor<Integer> REQUEST_TIMEOUT_MILLIS = new AttributeDescriptor<>(
            "openweathermapRequestTimeoutMillis", ValueType.POSITIVE_INTEGER);

    // Read only attribution, required by OpenWeatherMap API
    public static final AttributeDescriptor<String> ATTRIBUTION = new AttributeDescriptor<>(
            "openweathermapAttribution", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));


    // Button to provision a weather asset with the appropriate agent links
    public static final AttributeDescriptor<Boolean> PROVISION_WEATHER_ASSET = new AttributeDescriptor<>(
            "openweathermapProvisionWeatherAsset", ValueType.BOOLEAN);

    protected OpenWeatherMapAgent() {
    }

    public OpenWeatherMapAgent(String name) {
        super(name);
        getAttributes().getOrCreate(ATTRIBUTION).setValue("Weather data provided by OpenWeather (https://openweathermap.org/)");
    }

    public Optional<String> getAPIKey() {
        return getAttributes().getValue(API_KEY);
    }

    public OpenWeatherMapAgent setAPIKey(String value) {
        getAttributes().getOrCreate(API_KEY).setValue(value);
        return this;
    }

    public Optional<Integer> getRequestTimeoutMillis() {
        return getAttributes().getValue(REQUEST_TIMEOUT_MILLIS);
    }

    public OpenWeatherMapAgent setRequestTimeoutMillis(Integer value) {
        getAttributes().getOrCreate(REQUEST_TIMEOUT_MILLIS).setValue(value);
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
