package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;
import java.util.function.Consumer;

/**
 * Request the gateway to return a {@link GatewayCapabilitiesResponseEvent}
 */
public class GatewayCapabilitiesRequestEvent extends SharedEvent implements RespondableEvent {

    public static final String TYPE = "gateway-capabilities-request";
    @JsonIgnore
    protected Consumer<Event> responseConsumer;

    @JsonCreator
    public GatewayCapabilitiesRequestEvent(@JsonProperty("timestamp") Date timestamp) {
        super(timestamp != null ? timestamp.getTime() : new Date().getTime());
    }

    public GatewayCapabilitiesRequestEvent() {

    }

    @Override
    public Consumer<Event> getResponseConsumer() {
        return responseConsumer;
    }

    @Override
    public void setResponseConsumer(Consumer<Event> responseConsumer) {
        this.responseConsumer = responseConsumer;
    }
}
