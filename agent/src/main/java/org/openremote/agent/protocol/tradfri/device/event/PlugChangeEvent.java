package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Plug;
import org.openremote.agent.protocol.tradfri.device.PlugProperties;

/**
 * The class that represents a change event that occurred to an IKEA TRÅDFRI plug
 */
public class PlugChangeEvent extends PlugEvent {

    /**
     * The old properties of the plug (from before the event occurred)
     */
    private PlugProperties oldProperties;

    /**
     * The new properties of the plug (from after the event occurred)
     */
    private PlugProperties newProperties;

    /**
     * Construct the PlugChangeEvent class
     * @param plug The plug for which the event occurred
     * @param oldProperties The old properties of the plug (from before the event occurred)
     * @param newProperties The new properties of the plug (from after the event occurred)
     */
    public PlugChangeEvent(Plug plug, PlugProperties oldProperties, PlugProperties newProperties) {
        super(plug);
        this.oldProperties = oldProperties;
        this.newProperties = newProperties;
    }

    /**
     * Get the old properties of the plug (from before the event occurred)
     * @return The old properties of the plug
     */
    public PlugProperties getOldProperties(){
        return oldProperties;
    }

    /**
     * Get the new properties of the plug (from after the event occurred)
     * @return The new properties of the plug
     */
    public PlugProperties getNewProperties(){
        return newProperties;
    }

}
