package org.openremote.agent.protocol.homeassistant.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeAssistantBaseEntity {
    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("state")
    private String state;

    @JsonProperty("attributes")
    private Map<String, Object> homeAssistantAttributes;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Map<String, Object> getAttributes() {
        return homeAssistantAttributes;
    }
}
