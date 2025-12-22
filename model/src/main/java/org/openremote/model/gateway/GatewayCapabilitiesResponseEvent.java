package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;

public class GatewayCapabilitiesResponseEvent extends SharedEvent {

    public static final String TYPE = "gateway-capabilities-response";
    protected final boolean tunnelingSupported;
    protected final boolean tunnelTimeoutManagementSupported;
    protected final String version;

    @JsonCreator
    public GatewayCapabilitiesResponseEvent(Date timestamp, boolean tunnelingSupported, String version, boolean tunnelTimeoutManagementSupported) {
        super(timestamp != null ? timestamp.getTime() : new Date().getTime());
        this.tunnelingSupported = tunnelingSupported;
        this.version = version;
        this.tunnelTimeoutManagementSupported = tunnelTimeoutManagementSupported;
    }

    public GatewayCapabilitiesResponseEvent(final boolean tunnelingSupported, final boolean tunnelTimeoutManagementSupported, final String version) {
        this.tunnelingSupported = tunnelingSupported;
        this.tunnelTimeoutManagementSupported = tunnelTimeoutManagementSupported;
        this.version = version;
    }

    public boolean isTunnelingSupported() {
        return tunnelingSupported;
    }

    public boolean isTunnelTimeoutManagementSupported() {
        return tunnelTimeoutManagementSupported;
    }

    public String getVersion() {
        return version;
    }
}
