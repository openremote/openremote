package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Device;

/**
 * The class that represents a device event that occurred to an IKEA TRÅDFRI device
 */
public class DeviceEvent {

    /**
     * The device for which the event occurred
     */
    private Device device;

    /**
     * Construct the DeviceEvent class
     * @param device The device for which the event occurred
     */
    public DeviceEvent(Device device) {
        this.device = device;
    }

    /**
     * Get the device for which the event occurred
     * @return The device for which the event occurred
     */
    public Device getDevice(){
        return this.device;
    }

}
