package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayCapabilitiesResponseEvent extends SharedEvent {

    protected final boolean tunnelingSupported;

    public GatewayCapabilitiesResponseEvent(final boolean tunnelingSupported) {
        this.tunnelingSupported = tunnelingSupported;
    }

    public boolean isTunnelingSupported() {
        return tunnelingSupported;
    }
}
