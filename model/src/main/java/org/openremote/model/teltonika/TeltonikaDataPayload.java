package org.openremote.model.teltonika;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "payload"
})
public class TeltonikaDataPayload {

    @JsonProperty("state")
    public State state;

}
