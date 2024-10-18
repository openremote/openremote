package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

import java.util.function.Consumer;

public class GatewayTunnelStartRequestEvent extends SharedEvent implements RespondableEvent {

    protected String sshHostname;
    protected int sshPort;
    protected GatewayTunnelInfo info;
    @JsonIgnore
    protected Consumer<Event> responseConsumer;

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
    public Consumer<Event> getResponseConsumer() {
        return responseConsumer;
    }

    @Override
    public void setResponseConsumer(Consumer<Event> responseConsumer) {
        this.responseConsumer = responseConsumer;
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
