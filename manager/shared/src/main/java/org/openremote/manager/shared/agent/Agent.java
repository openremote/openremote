/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.agent;

import elemental.json.Json;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.attribute.*;
import org.openremote.manager.shared.connector.Connector;

import java.util.logging.Logger;

import static org.openremote.manager.shared.connector.Connector.ASSET_ATTRIBUTE_CONNECTOR;

/**
 * Runtime instance of an agent {@link org.openremote.manager.shared.asset.Asset}.
 * <p>
 * This is a message routing table that can be dynamically built from {@link Connector} settings.
 * This information is combined with calls to a {@link org.openremote.manager.shared.connector.ConnectorComponent}
 * when routes and endpoint URIs are established.
 * <p>
 * Communication with an agent is available through the messaging endpoint {@link #TOPIC_TRIGGER_DISCOVERY}.
 */
public class Agent {

    private static final Logger LOG = Logger.getLogger(Agent.class.getName());

    /**
     * Send messages to this endpoint to trigger discovery of the assets available through an agent. If the
     * message body is empty, all running agent's will execute discovery. If an agent asset identifier is
     * present in the message body, only that agent will execute discovery.
     */
    public static String TOPIC_TRIGGER_DISCOVERY = "seda://urn:openremote:agent:discovery?multipleConsumers=true&waitForTaskToComplete=NEVER";

    final protected Attributes attributes;

    public Agent(Attributes attributes) {
        this(attributes, false);
    }

    public Agent(Attributes attributes, boolean initialize) {
        this.attributes = attributes;
        if (initialize)
            initialize();
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void initialize() {
        setEnabled(false);
    }

    public boolean isEnabled() {
        return attributes.hasAttribute("enabled") && attributes.get("enabled").isValueTrue();
    }

    public Agent setEnabled(boolean enabled) {
        if (attributes.hasAttribute("enabled")) {
            attributes.get("enabled").setValue(Json.create(enabled));
        } else {
            attributes.put(new Attribute("enabled", AttributeType.BOOLEAN, Json.create(enabled)));
        }
        return this;
    }

    public Attribute getOrCreateConnectorTypeAttribute() {
        if (attributes.hasAttribute(ASSET_ATTRIBUTE_CONNECTOR)) {
            return attributes.get(ASSET_ATTRIBUTE_CONNECTOR);
        } else {
            Attribute attribute = new Attribute(ASSET_ATTRIBUTE_CONNECTOR, AttributeType.STRING);
            attributes.put(attribute);
            return attribute;
        }
    }

    public String getConnectorType() {
        return getOrCreateConnectorTypeAttribute().getValueAsString();
    }

    public Agent setConnectorType(String connectorType) {
        getOrCreateConnectorTypeAttribute().setValue(connectorType);
        return this;
    }

    public Agent removeConnectorType() {
        attributes.remove(ASSET_ATTRIBUTE_CONNECTOR);
        return this;
    }

    public Agent removeConnectorSettings(Connector connector) {
        if (connector == null)
            return this;
        for (Attribute setting : connector.getSettings().get()) {
            attributes.remove(setting.getName());
        }
        return this;
    }

    public Agent writeConnectorSettings(Connector connector) {
        if (connector == null)
            return this;
        for (Attribute setting : connector.getSettings().get()) {
            Attribute attribute = attributes.get(setting.getName());
            // If the connector setting still has the same type as the agent's attribute, only update metadata
            if (attribute != null && attribute.getType().equals(setting.getType())) {
                attribute.setMetadata(setting.getMetadata().copy());
            } else {
                // The agent does not have the connector setting or it's now a different type, replace/create attribute
                attribute = new Attribute(setting.getName(), setting.getType());
                attribute.setMetadata(setting.getMetadata().copy());
                attributes.put(attribute);
            }
        }
        return this;
    }

}
