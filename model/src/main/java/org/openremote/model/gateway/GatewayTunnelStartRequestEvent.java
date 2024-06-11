package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStartRequestEvent extends SharedEvent {

    protected String sshHostname;
    protected int sshPort;
    protected GatewayTunnelInfo info;

    @JsonCreator
    public GatewayTunnelStartRequestEvent(String sshHostname, int sshPort, GatewayTunnelInfo info) {
        this.sshHostname = sshHostname;
        this.sshPort = sshPort;
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
    }

    public String getSshHostname() {
        return sshHostname;
    }

    public int getSshPort() {
        return sshPort;
    }

    @Override
    public String toString() {
        return GatewayTunnelStartRequestEvent.class.getSimpleName() + "{" +
                "sshHostname='" + sshHostname + '\'' +
                ", sshPort=" + sshPort +
                ", info=" + info +
                '}';
    }
}
