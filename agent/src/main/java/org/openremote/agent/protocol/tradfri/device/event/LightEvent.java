package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;

/**
 * The class that represents a light event that occurred to an IKEA TRÅDFRI light
 */
public class LightEvent extends DeviceEvent {

    /**
     * Construct the LightEvent class
     * @param light The light for which the event occurred
     */
    public LightEvent(Light light) {
        super(light);
    }

    /**
     * Get the light for which the event occurred
     * @return The light for which the event occurred
     */
    public Light getLight(){
        return (Light) getDevice();
    }

}
