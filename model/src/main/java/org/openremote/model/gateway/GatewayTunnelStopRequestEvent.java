package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

import java.util.function.Consumer;

public class GatewayTunnelStopRequestEvent extends SharedEvent  implements RespondableEvent {

    protected GatewayTunnelInfo info;
    @JsonIgnore
    protected Consumer<Event> responseConsumer;

    @JsonCreator
    public GatewayTunnelStopRequestEvent(GatewayTunnelInfo info) {
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
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
        return "GatewayTunnelStopRequestEvent{" +
                "info=" + info +
                ", timestamp=" + timestamp +
                '}';
    }
}
