package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

import java.util.function.Consumer;

public class GatewayTunnelStartRequestEvent extends SharedEvent implements RespondableEvent {

    @Deprecated
    protected String sshHostname;
    @Deprecated
    protected int sshPort;
    protected GatewayTunnelInfo info;
    @JsonIgnore
    protected Consumer<Event> responseConsumer;

    @JsonCreator
    public GatewayTunnelStartRequestEvent(GatewayTunnelInfo info) {
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
    }

    @Deprecated
    public String getSshHostname() {
        return sshHostname;
    }

    @Deprecated
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
