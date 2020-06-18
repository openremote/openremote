package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;
import org.openremote.agent.protocol.tradfri.util.ColourXY;

/**
 * The class that represents a XY colour of the light changed event that occurred to an IKEA TRÅDFRI light
 */
public class LightChangeColourXYEvent extends LightChangeEvent {

    /**
     * Construct the LightChangeColourXYEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     */
    public LightChangeColourXYEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light, oldProperties, newProperties);
    }

    /**
     * Get the old X value of the colour of the light (from before the event occurred)
     * @return The old X value of the colour of the light
     */
    public int getOldColourX(){
        return getOldProperties().getColourX();
    }

    /**
     * Get the new X value of the colour of the light (from after the event occurred)
     * @return The new X value of the colour of the light
     */
    public int getNewColourX(){
        return getNewProperties().getColourX();
    }

    /**
     * Get the old Y value of the colour of the light (from before the event occurred)
     * @return The old Y value of the colour of the light
     */
    public int getOldColourY(){
        return getOldProperties().getColourY();
    }

    /**
     * Get the new Y value of the colour of the light (from after the event occurred)
     * @return The new Y value of the colour of the light
     */
    public int getNewColourY(){
        return getNewProperties().getColourY();
    }

    /**
     * Get the old XY colour of the light (from before the event occurred)
     * @return The old XY colour of the light
     */
    public ColourXY getOldColourXY(){
        return new ColourXY(getOldProperties().getColourX(), getOldProperties().getColourY());
    }

    /**
     * Get the new XY colour of the light (from after the event occurred)
     * @return The new XY colour of the light
     */
    public ColourXY getNewColourXY(){
        return new ColourXY(getNewProperties().getColourX(), getNewProperties().getColourY());
    }

}
