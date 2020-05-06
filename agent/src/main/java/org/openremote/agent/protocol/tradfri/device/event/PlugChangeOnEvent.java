package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Plug;
import org.openremote.agent.protocol.tradfri.device.PlugProperties;
import org.openremote.agent.protocol.tradfri.device.event.PlugChangeEvent;

/**
 * The class that represents a plug on / off state changed event that occurred to an IKEA TRÅDFRI plug
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class PlugChangeOnEvent extends PlugChangeEvent {

    /**
     * Construct the PlugChangeOnEvent class
     * @param plug The plug for which the event occurred
     * @param oldProperties The old properties of the plug (from before the event occurred)
     * @param newProperties The new properties of the plug (from after the event occurred)
     * @since 1.0.0
     */
    public PlugChangeOnEvent(Plug plug, PlugProperties oldProperties, PlugProperties newProperties) {
        super(plug, oldProperties, newProperties);
    }

    /**
     * Get the old on / off state of the light (from before the event occurred)
     * @return The old on / off state of the light (true for on, false for off)
     * @since 1.0.0
     */
    public boolean getOldOn(){
        return getOldProperties().getOn();
    }

    /**
     * Get the new on / off state of the light (from after the event occurred)
     * @return The new on / off state of the light (true for on, false for off)
     * @since 1.0.0
     */
    public boolean getNewOn(){
        return getNewProperties().getOn();
    }

}
