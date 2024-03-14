package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "error", value = ErrorResponseMessage.class),
        @JsonSubTypes.Type(name = "success", value = SuccessResponseMessage.class),
})
public abstract class MqttResponseMessage {
}