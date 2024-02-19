package org.openremote.agent.protocol.homeassistant.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeAssistantEntityStateEvent {
    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("data")
    private HomeAssistantEntityStateEventData data;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public HomeAssistantEntityStateEventData getData() {
        return data;
    }

    public void setData(HomeAssistantEntityStateEventData data) {
        this.data = data;
    }


}

