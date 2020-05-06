package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Gateway;
import org.openremote.agent.protocol.tradfri.device.event.Event;

/**
 * The class that represents an event that occurred to an IKEA TRÅDFRI gateway
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class GatewayEvent extends Event {

    /**
     * The IKEA TRÅDFRI gateway for which the event occurred
     */
    private Gateway gateway;

    /**
     * Construct the GatewayEvent class
     * @param gateway The IKEA TRÅDFRI gateway for which the event occurred
     * @since 1.0.0
     */
    public GatewayEvent(Gateway gateway) {
        super();
        this.gateway = gateway;
    }

    /**
     * Get the IKEA TRÅDFRI gateway for which the event occurred
     * @return The IKEA TRÅDFRI gateway for which the event occurred
     * @since 1.0.0
     */
    public Gateway getGateway(){
        return this.gateway;
    }

}
