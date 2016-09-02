package org.openremote.manager.shared.connector;

import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.Attributes;

public class Connector {

    protected String name;
    protected String type;
    protected Attributes settings;

    public Connector() {
    }

    public Connector(String name, String type, Attributes settings) {
        this.name = name;
        this.type = type;
        this.settings = settings;
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

    public Attributes getSettings() {
        return settings;
    }

    public void setSettings(Attributes settings) {
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
        Attributes settings = getSettings();
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

        Attributes settings = getSettings();
        Attributes agentConnectorSettings = new Attributes(agent.getConnectorSettings());
        for (Attribute agentConnectorAttribute : agentConnectorSettings.get()) {
            Attribute settingsAttribute = settings.get(agentConnectorAttribute.getName());
            if (settingsAttribute != null
                && settingsAttribute.getType().equals(agentConnectorAttribute.getType())) {
                settingsAttribute.setValue(agentConnectorAttribute.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", settings=" + settings +
            '}';
    }
}
