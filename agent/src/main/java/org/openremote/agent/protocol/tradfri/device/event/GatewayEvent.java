package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Gateway;

/**
 * The class that represents an event that occurred to an IKEA TRÅDFRI gateway
 */
public class GatewayEvent {

    /**
     * The IKEA TRÅDFRI gateway for which the event occurred
     */
    private Gateway gateway;

    /**
     * Construct the GatewayEvent class
     * @param gateway The IKEA TRÅDFRI gateway for which the event occurred
     */
    public GatewayEvent(Gateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Get the IKEA TRÅDFRI gateway for which the event occurred
     * @return The IKEA TRÅDFRI gateway for which the event occurred
     */
    public Gateway getGateway(){
        return this.gateway;
    }

}
