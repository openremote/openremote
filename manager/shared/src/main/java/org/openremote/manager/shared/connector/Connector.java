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
package org.openremote.manager.shared.connector;

import elemental.json.JsonObject;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.Attributes;

import java.util.logging.Logger;

/**
 * Interface for agent connectors that is used to connect and communicate with agents
 * in an agnostic way. (That is, a Camel component with the required endpoints.)
 */
public class Connector {

    private static final Logger LOG = Logger.getLogger(Connector.class.getName());

    // Camel components which are OR connectors must have this in component.properties
    public static final String PROPERTY_TYPE = "openremote-connector-type";
    public static final String PROPERTY_SUPPORTS_DISCOVERY = "openremote-connector-supports-discovery";
    public static final String PROPERTY_SUPPORTS_INVENTORY = "openremote-connector-supports-inventory";

    protected String id;
    protected String name;
    protected String type;
    protected String syntax;
    protected boolean supportsDiscovery;
    protected boolean supportsInventory;
    protected JsonObject settings;

    public Connector() {
    }

    public Connector(String id) {
        this.id = id;
    }

    public Connector(String id, String name, String type) {
        this(id);
        this.name = name;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public boolean isSupportsDiscovery() {
        return supportsDiscovery;
    }

    public void setSupportsDiscovery(boolean supportsDiscovery) {
        this.supportsDiscovery = supportsDiscovery;
    }

    public boolean isSupportsInventory() {
        return supportsInventory;
    }

    public void setSupportsInventory(boolean supportsInventory) {
        this.supportsInventory = supportsInventory;
    }

    public JsonObject getSettings() {
        return settings;
    }

    public void setSettings(JsonObject settings) {
        this.settings = settings;
    }

    /**
     * Copy all settings which have a value into the {@link Agent}, keeping the value
     * of each attribute but not the attribute metadata.
     */
    public void writeSettings(Agent agent) {
        if (getSettings() == null) {
            agent.setConnectorSettings(null);
            return;
        }
        Attributes settings = new Attributes(getSettings());
        Attributes agentConnectorSettings = new Attributes();
        for (Attribute attribute : settings.get()) {
            if (attribute.getValue() == null)
                continue;
            Attribute agentConnectorAttribute = new Attribute(attribute.getName(), attribute.getType());
            agentConnectorAttribute.setValue(attribute.getValue());
            agentConnectorSettings.add(agentConnectorAttribute);
        }
        agent.setConnectorSettings(agentConnectorSettings.getJsonObject());
    }

    /**
     * Read the value for each settings attribute from the {@link Agent}, skipping any
     * unknown attribute or attribute that doesn't have the right type. The
     * {@link Agent#connectorSettings} can be "cleaned up" so they match a {@link Connector}
     * by calling {@link #readSettings(Agent)} followed by {@link #writeSettings(Agent)}.
     */
    public void readSettings(Agent agent) {
        if (agent.getConnectorSettings() == null)
            return;

        Attributes settings = new Attributes(getSettings());
        Attributes agentConnectorSettings = new Attributes(agent.getConnectorSettings());
        for (Attribute agentConnectorAttribute : agentConnectorSettings.get()) {
            Attribute settingsAttribute = settings.get(agentConnectorAttribute.getName());
            if (settingsAttribute != null
                && settingsAttribute.getType().equals(agentConnectorAttribute.getType())) {
                settingsAttribute.setValue(agentConnectorAttribute.getValue());
            }
        }
    }

}
