package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;

/**
 * The class that represents a light brightness changed event that occurred to an IKEA TRÅDFRI light
 */
public class LightChangeBrightnessEvent extends LightChangeEvent {

    /**
     * Construct the LightChangeBrightnessEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     */
    public LightChangeBrightnessEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light, oldProperties, newProperties);
    }

    /**
     * Get the old brightness of the light (from before the event occurred)
     * @return The old brightness of the light
     */
    public int getOldBrightness(){
        return getOldProperties().getBrightness();
    }

    /**
     * Get the new brightness of the light (from after the event occurred)
     * @return The new brightness of the light
     */
    public int getNewBrightness(){
        return getNewProperties().getBrightness();
    }

}
