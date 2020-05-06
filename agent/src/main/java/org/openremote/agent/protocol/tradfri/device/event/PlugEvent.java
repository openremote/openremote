package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Plug;

/**
 * The class that represents a plug event that occurred to an IKEA TRÅDFRI plug
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class PlugEvent extends DeviceEvent {

    /**
     * Construct the PlugEvent class
     * @param plug The plug for which the event occurred
     * @since 1.0.0
     */
    public PlugEvent(Plug plug) {
        super(plug);
    }

    /**
     * Get the plug for which the event occurred
     * @return The plug for which the event occurred
     * @since 1.0.0
     */
    public Plug getPlug(){
        return (Plug) getDevice();
    }

}
