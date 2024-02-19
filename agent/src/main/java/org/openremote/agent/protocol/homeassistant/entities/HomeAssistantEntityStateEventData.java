package org.openremote.agent.protocol.homeassistant.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeAssistantEntityStateEventData {

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("new_state")
    private HomeAssistantBaseEntity newBaseEntity;


    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setNewBaseEntity(HomeAssistantBaseEntity newBaseEntity) {
        this.newBaseEntity = newBaseEntity;
    }

    public HomeAssistantBaseEntity getNewBaseEntity() {
        return newBaseEntity;
    }


}
