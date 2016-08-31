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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gwt.json.client.JSONObject;
import elemental.json.JsonObject;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.Attributes;

public class ConnectorImpl implements Connector {
    protected String type;
    protected String displayName;
    protected boolean agentDiscovery;
    protected Attributes agentSettings;
    protected Attributes discoverySettings;

    public ConnectorImpl() {
    }

    public ConnectorImpl(String type, String displayName, boolean agentDiscovery, Attributes agentSettings, Attributes discoverySettings) {
        this.type = type;
        this.displayName = displayName;
        this.agentDiscovery = agentDiscovery;
        this.agentSettings = agentSettings;
        this.discoverySettings = discoverySettings;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean supportsAgentDiscovery() {
        return agentDiscovery;
    }

    @Override
    public Attributes getAgentDiscoverySettings() {
        return discoverySettings;
    }

    @Override
    public Attributes getAgentSettings() {
        return agentSettings;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAgentDiscovery(boolean agentDiscovery) {
        this.agentDiscovery = agentDiscovery;
    }

    public void setAgentSettings(Attributes agentSettings) {
        this.agentSettings = agentSettings;
    }

    public void setDiscoverySettings(Attributes discoverySettings) {
        this.discoverySettings = discoverySettings;
    }

    /**
     * Copy all settings which have a value into the {@link Agent}, keeping the value
     * of each attribute but not the attribute metadata.
     */
    public void writeSettings(Agent agent) {
        if (getAgentSettings() == null) {
            agent.setConnectorSettings(null);
            return;
        }
        Attributes settings = getAgentSettings();
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

        Attributes settings = getAgentSettings();
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
