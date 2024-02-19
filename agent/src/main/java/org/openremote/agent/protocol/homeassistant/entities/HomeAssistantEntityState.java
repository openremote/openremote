package org.openremote.agent.protocol.homeassistant.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeAssistantEntityState {
    @JsonProperty("type")
    private String type;

    @JsonProperty("event")
    private HomeAssistantEntityStateEvent event;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public HomeAssistantEntityStateEvent getEvent() {
        return event;
    }

    public void setEvent(HomeAssistantEntityStateEvent event) {
        this.event = event;
    }

}

