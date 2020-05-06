package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;

/**
 * The class that represents a X value of the light colour changed event that occurred to an IKEA TRÅDFRI light
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class LightChangeColourXEvent extends LightChangeEvent {

    /**
     * Construct the LightChangeColourXEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     * @since 1.0.0
     */
    public LightChangeColourXEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light, oldProperties, newProperties);
    }

    /**
     * Get the old X value of the colour of the light (from before the event occurred)
     * @return The old X value of the colour of the light
     * @since 1.0.0
     */
    public int getOldColourX(){
        return getOldProperties().getColourX();
    }

    /**
     * Get the new X value of the colour of the light (from after the event occurred)
     * @return The new X value of the colour of the light
     * @since 1.0.0
     */
    public int getNewColourX(){
        return getNewProperties().getColourX();
    }

}
