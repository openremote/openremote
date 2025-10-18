package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;

public class GatewayCapabilitiesResponseEvent extends SharedEvent {

    public static final String TYPE = "gateway-capabilities-response";
    public static final String CURRENT_VERSION = "1.0.0";
    protected final boolean tunnelingSupported;
    protected final boolean tunnelTimeoutManagementSupported;
    protected final String version;

    @JsonCreator
    public GatewayCapabilitiesResponseEvent(
        @JsonProperty("timestamp") Date timestamp,
        @JsonProperty("tunnelingSupported") boolean tunnelingSupported,
        @JsonProperty("version") String version,
        @JsonProperty("tunnelTimeoutManagementSupported") boolean tunnelTimeoutManagementSupported) {
        super(timestamp != null ? timestamp.getTime() : new Date().getTime());
        this.tunnelingSupported = tunnelingSupported;
        this.version = version != null ? version : "0.0.1";
        this.tunnelTimeoutManagementSupported = tunnelTimeoutManagementSupported;
    }

    public GatewayCapabilitiesResponseEvent(final boolean tunnelingSupported) {
        this(tunnelingSupported, false);
    }

    public GatewayCapabilitiesResponseEvent(final boolean tunnelingSupported, final boolean tunnelTimeoutManagementSupported) {
        this.tunnelingSupported = tunnelingSupported;
        this.tunnelTimeoutManagementSupported = tunnelTimeoutManagementSupported;
        this.version = tunnelTimeoutManagementSupported ? CURRENT_VERSION : "0.0.1";
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
