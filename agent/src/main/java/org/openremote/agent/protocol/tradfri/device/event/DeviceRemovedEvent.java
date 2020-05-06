package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Gateway;
import org.openremote.agent.protocol.tradfri.device.event.GatewayEvent;

/**
 * The class that represents a device removed event that occurred to an IKEA TRÅDFRI gateway
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class DeviceRemovedEvent extends GatewayEvent {

    /**
     * The removed device for which the event occurred
     */
    private Device device;

    /**
     * Construct the DeviceRemovedEvent class
     * @param gateway The IKEA TRÅDFRI gateway for which the event occurred
     * @param device The removed device for which the event occurred
     * @since 1.0.0
     */
    public DeviceRemovedEvent(Gateway gateway, Device device) {
        super(gateway);
        this.device = device;
    }

    /**
     * Get the removed device for which the event occurred
     * @return The removed device for which the event occurred
     * @since 1.0.0
     */
    public Device getDevice(){
        return this.device;
    }

    /**
     * Get the id of the removed device for which the event occurred
     * @return The id of the removed device for which the event occurred
     * @since 1.0.0
     */
    public int getDeviceId(){
        return this.device.getInstanceId();
    }

}
