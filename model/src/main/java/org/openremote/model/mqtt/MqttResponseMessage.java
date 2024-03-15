package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "error", value = MqttErrorResponseMessage.class),
        @JsonSubTypes.Type(name = "success", value = MqttSuccessResponseMessage.class),
})
public abstract class MqttResponseMessage {
}