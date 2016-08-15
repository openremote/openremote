package org.openremote.manager.shared.agent;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.manager.shared.ngsi.Attribute;
import org.openremote.manager.shared.ngsi.AttributeType;
import org.openremote.manager.shared.ngsi.Entity;

public class Agent extends Entity {

    // Persisted properties
    public static final String TYPE = "urn:openremote:agent";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_DESCRIPTION = "description";
    public static final String ATTRIBUTE_CONNECTOR_TYPE = "connectorType";
    public static final String ATTRIBUTE_CONNECTOR_SETTINGS = "connectorSettings";
    public static final String ATTRIBUTE_ENABLED = "enabled";

    // State properties that must be read from the agent (agent is context provider for these values)
    protected boolean valid;
    protected boolean available;
    protected boolean connected;

    public Agent() {
        super();
        setType(TYPE);
    }

    public Agent(JsonObject jsonObject) {
        super(jsonObject);
    }

    public String getName() {
        return getAttributeValueAsString(ATTRIBUTE_NAME);
    }

    public void setName(String name) {
        Attribute attr = new Attribute(ATTRIBUTE_NAME, AttributeType.STRING, Json.create(name));
        super.addAttribute(attr);
    }

    public String getDescription() {
        return getAttributeValueAsString(ATTRIBUTE_DESCRIPTION);
    }

    public void setDescription(String description) {
        Attribute attr = new Attribute(ATTRIBUTE_DESCRIPTION, AttributeType.STRING, Json.create(description));
        super.addAttribute(attr);
    }

    public String getConnectorType() {
        return getAttributeValueAsString(ATTRIBUTE_CONNECTOR_TYPE);
    }

    public void setConnectorType(String connectorType) {
        Attribute attr = new Attribute(ATTRIBUTE_CONNECTOR_TYPE, AttributeType.STRING, Json.create(connectorType));
        super.addAttribute(attr);
    }

    public JsonObject getConnectorSettings() {
        return getAttributeValueAsObject(ATTRIBUTE_CONNECTOR_SETTINGS);
    }

    public void setConnectorSettings(JsonObject settings) {
        Attribute attr = new Attribute(ATTRIBUTE_CONNECTOR_SETTINGS, AttributeType.OBJECT, settings);
        super.addAttribute(attr);
    }

    public boolean isEnabled() {
        return getAttributeValueAsBoolean(ATTRIBUTE_ENABLED);
    }

    public void setEnabled(boolean enabled) {
        Attribute attr = new Attribute(ATTRIBUTE_ENABLED, AttributeType.BOOLEAN, Json.create(enabled));
        super.addAttribute(attr);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
