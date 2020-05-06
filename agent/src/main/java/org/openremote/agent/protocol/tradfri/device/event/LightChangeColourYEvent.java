package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;
import org.openremote.agent.protocol.tradfri.device.event.LightChangeEvent;

/**
 * The class that represents a Y value of the light colour changed event that occurred to an IKEA TRÅDFRI light
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class LightChangeColourYEvent extends LightChangeEvent {

    /**
     * Construct the LightChangeColourYEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     * @since 1.0.0
     */
    public LightChangeColourYEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light, oldProperties, newProperties);
    }

    /**
     * Get the old Y value of the colour of the light (from before the event occurred)
     * @return The old Y value of the colour of the light
     * @since 1.0.0
     */
    public int getOldColourY(){
        return getOldProperties().getColourY();
    }

    /**
     * Get the new Y value of the colour of the light (from after the event occurred)
     * @return The new Y value of the colour of the light
     * @since 1.0.0
     */
    public int getNewColourY(){
        return getNewProperties().getColourY();
    }

}
