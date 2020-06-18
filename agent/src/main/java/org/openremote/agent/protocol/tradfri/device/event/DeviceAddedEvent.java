package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Gateway;

/**
 * The class that represents a device added event that occurred to an IKEA TRÅDFRI gateway
 */
public class DeviceAddedEvent extends GatewayEvent {

    /**
     * The added device for which the event occurred
     */
    private Device device;

    /**
     * Construct the DeviceAddedEvent class
     * @param gateway The IKEA TRÅDFRI gateway for which the event occurred
     * @param device The added device for which the event occurred
     */
    public DeviceAddedEvent(Gateway gateway, Device device) {
        super(gateway);
        this.device = device;
    }

    /**
     * Get the added device for which the event occurred
     * @return The added device for which the event occurred
     */
    public Device getDevice(){
        return this.device;
    }

    /**
     * Get the id of the added device for which the event occurred
     * @return The id of the added device for which the event occurred
     */
    public int getDeviceId(){
        return this.device.getInstanceId();
    }

}
