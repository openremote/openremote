package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;

public class GatewayCapabilitiesResponseEvent extends SharedEvent {

    public static final String TYPE = "gateway-capabilities-response";
    protected final boolean tunnelingSupported;

    @JsonCreator
    public GatewayCapabilitiesResponseEvent(@JsonProperty("timestamp") Date timestamp, @JsonProperty("tunnelingSupported") boolean tunnelingSupported) {
        super(timestamp != null ? timestamp.getTime() : new Date().getTime());
        this.tunnelingSupported = tunnelingSupported;
    }

    public GatewayCapabilitiesResponseEvent(final boolean tunnelingSupported) {
        this.tunnelingSupported = tunnelingSupported;
    }

    public boolean isTunnelingSupported() {
        return tunnelingSupported;
    }
}
