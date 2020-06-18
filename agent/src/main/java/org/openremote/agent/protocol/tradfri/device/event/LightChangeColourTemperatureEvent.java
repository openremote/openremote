package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;

/**
 * The class that represents a light colour temperature changed event that occurred to an IKEA TRÅDFRI light
 */
public class LightChangeColourTemperatureEvent extends LightChangeEvent {

    /**
     * Construct the LightChangeColourTemperatureEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     */
    public LightChangeColourTemperatureEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light, oldProperties, newProperties);
    }

    /**
     * Get the old colour temperature of the light (from before the event occurred)
     * @return The old colour temperature of the light
     */
    public int getOldColourTemperature(){
        return getOldProperties().getColourTemperature();
    }

    /**
     * Get the new colour temperature of the light (from after the event occurred)
     * @return The new colour temperature of the light
     */
    public int getNewColourTemperature(){
        return getNewProperties().getColourTemperature();
    }

}
