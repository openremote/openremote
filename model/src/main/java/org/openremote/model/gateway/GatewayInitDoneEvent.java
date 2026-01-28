package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;

/**
 * Indicates that the gateway synchronisation has finished
 */
public class GatewayInitDoneEvent extends SharedEvent {
    public static final String TYPE = "gateway-init-done";

    @JsonCreator
    public GatewayInitDoneEvent() {
    }
}
