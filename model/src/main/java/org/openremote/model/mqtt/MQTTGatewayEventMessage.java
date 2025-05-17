package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.attribute.AttributeEvent;

public class MQTTGatewayEventMessage {
    protected String ackId;
    protected AttributeEvent event;

    @JsonCreator()
    public MQTTGatewayEventMessage(String ackId, AttributeEvent event) {
        this.ackId = ackId;
        this.event = event;
    }

    public String getAckId() {
        return ackId;
    }

    public AttributeEvent getEvent() {
        return event;
    }

    public void setAckId(String ackId) {
        this.ackId = ackId;
    }

    public void setEvent(AttributeEvent event) {
        this.event = event;
    }
}
