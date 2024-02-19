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
package org.openremote.agent.protocol.homeassistant;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

/**
 * This is an example of a custom {@link Agent} type; this must be registered via an
 * {@link org.openremote.model.AssetModelProvider} and must conform to the same requirements as custom {@link Asset}s and
 * in addition the following requirements:
 *
 * <ul>
 * <li>Optionally add a custom {@link org.openremote.model.asset.agent.AgentLink} (the {@link Class#getSimpleName} must
 * be unique compared to all other registered {@link org.openremote.model.asset.agent.AgentLink}s)
 * <li>Must define a {@link org.openremote.model.asset.agent.Protocol} implementation that corresponds to this {@link Agent}
 * <li>Must have a public static final {@link AgentDescriptor} rather than an
 * {@link org.openremote.model.asset.AssetDescriptor}
 * </ul>
 */
@Entity
public class HomeAssistantAgent extends Agent<HomeAssistantAgent, HomeAssistantProtocol, HomeAssistantAgentLink> {

    public static final AttributeDescriptor<String> ACCESS_TOKEN = new AttributeDescriptor<>("AccessToken", ValueType.TEXT, new MetaItem<>(MetaItemType.SECRET));
    public static final AttributeDescriptor<String> HOME_ASSISTANT_URL = new AttributeDescriptor<>("HomeAssistantURL", ValueType.HTTP_URL);
    public static final AttributeDescriptor<Boolean> IMPORTED_LIGHT = new AttributeDescriptor<>("ImportLights", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.LABEL, "Import Lights"));
    public static final AttributeDescriptor<Boolean> IMPORTED_SENSOR = new AttributeDescriptor<>("ImportSensors", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.LABEL, "Import Sensors"));
    public static final AttributeDescriptor<Boolean> IMPORTED_BINARY_SENSOR = new AttributeDescriptor<>("ImportBinarySensors", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.LABEL, "Import Binary Sensors"));
    public static final AttributeDescriptor<Boolean> IMPORTED_SWITCH = new AttributeDescriptor<>("ImportSwitches", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.LABEL, "Import Switches"));
    public static final AttributeDescriptor<String> IMPORTED_OTHER_ENTITY_TYPES = new AttributeDescriptor<>("ImportOthers", ValueType.TEXT, new MetaItem<>(MetaItemType.LABEL, "Import Other Entity Types"));

    public static final AgentDescriptor<HomeAssistantAgent, HomeAssistantProtocol, HomeAssistantAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            HomeAssistantAgent.class, HomeAssistantProtocol.class, HomeAssistantAgentLink.class
    );

    protected HomeAssistantAgent() {
        getAttributes().setValue(IMPORTED_OTHER_ENTITY_TYPES, "automation,button,camera,media_player,number,persistent_notification,select,update,zone");
    }

    public HomeAssistantAgent(String name) {
        super(name);
        getAttributes().setValue(IMPORTED_OTHER_ENTITY_TYPES, "automation,button,camera,media_player,number,persistent_notification,select,update,zone");
    }

    @Override
    public HomeAssistantProtocol getProtocolInstance() {
        return new HomeAssistantProtocol(this);
    }

    public Optional<String> getAccessToken() {
        return getAttributes().getValue(ACCESS_TOKEN);
    }

    public Optional<String> getHomeAssistantUrl() {
        return getAttributes().getValue(HOME_ASSISTANT_URL);
    }

    public Optional<Boolean> getImportedLight() {
        return getAttributes().getValue(IMPORTED_LIGHT);
    }

    public Optional<Boolean> getImportedSensor() {
        return getAttributes().getValue(IMPORTED_SENSOR);
    }

    public Optional<Boolean> getImportedBinarySensor() {
        return getAttributes().getValue(IMPORTED_BINARY_SENSOR);
    }

    public Optional<Boolean> getImportedSwitch() {
        return getAttributes().getValue(IMPORTED_SWITCH);
    }

    public Optional<String> getImportedOtherEntityTypes() {
        return getAttributes().getValue(IMPORTED_OTHER_ENTITY_TYPES);
    }

    public void setImportedEntityTypes(String value) {
        getAttributes().setValue(IMPORTED_OTHER_ENTITY_TYPES, value);
    }


}

