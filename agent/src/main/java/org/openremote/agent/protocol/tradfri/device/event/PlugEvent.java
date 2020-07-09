package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Plug;

/**
 * The class that represents a plug event that occurred to an IKEA TRÅDFRI plug
 */
public class PlugEvent extends DeviceEvent {

    /**
     * Construct the PlugEvent class
     * @param plug The plug for which the event occurred
     */
    public PlugEvent(Plug plug) {
        super(plug);
    }

    /**
     * Get the plug for which the event occurred
     * @return The plug for which the event occurred
     */
    public Plug getPlug(){
        return (Plug) getDevice();
    }

}
