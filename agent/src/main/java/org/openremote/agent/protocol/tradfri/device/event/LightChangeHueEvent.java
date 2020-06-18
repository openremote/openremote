package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;

/**
 * The class that represents a light hue changed event that occurred to an IKEA TRÅDFRI light
 */
public class LightChangeHueEvent extends LightChangeEvent {

    /**
     * Construct the LightChangeHueEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     */
    public LightChangeHueEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light, oldProperties, newProperties);
    }

    /**
     * Get the old hue of the light (from before the event occurred)
     * @return The old hue of the light
     */
    public int getOldHue(){
        return getOldProperties().getHue();
    }

    /**
     * Get the new hue of the light (from after the event occurred)
     * @return The new hue of the light
     */
    public int getNewHue(){
        return getNewProperties().getHue();
    }

}
