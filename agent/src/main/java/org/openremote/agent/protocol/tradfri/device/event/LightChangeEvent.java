package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;

/**
 * The class that represents a light changed event that occurred to an IKEA TRÅDFRI light
 */
public class LightChangeEvent extends LightEvent {

    /**
     * The old properties of the light (from before the event occurred)
     */
    private LightProperties oldProperties;

    /**
     * The new properties of the light (from after the event occurred)
     */
    private LightProperties newProperties;

    /**
     * Construct the LightChangeEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     */
    public LightChangeEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light);
        this.oldProperties = oldProperties;
        this.newProperties = newProperties;
    }

    /**
     * Get the old properties of the light (from before the event occurred)
     * @return The old properties of the light
     */
    public LightProperties getOldProperties(){
        return oldProperties;
    }

    /**
     * Get the new properties of the light (from after the event occurred)
     * @return The new properties of the light
     */
    public LightProperties getNewProperties(){
        return newProperties;
    }

}
