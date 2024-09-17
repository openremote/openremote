package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;

/**
 * Request the gateway to return a {@link GatewayCapabilitiesResponseEvent}
 */
public class GatewayCapabilitiesRequestEvent extends SharedEvent {

    public static final String TYPE = "gateway-capabilities-request";

    @JsonCreator
    public GatewayCapabilitiesRequestEvent(@JsonProperty("timestamp") Date timestamp) {
        super(timestamp != null ? timestamp.getTime() : new Date().getTime());
    }

    public GatewayCapabilitiesRequestEvent() {

    }

}
