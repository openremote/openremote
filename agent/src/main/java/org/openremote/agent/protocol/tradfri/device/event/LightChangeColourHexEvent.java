package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.LightProperties;
import org.openremote.agent.protocol.tradfri.device.event.LightChangeEvent;

/**
 * The class that represents a light hexadecimal colour changed event that occurred to an IKEA TRÅDFRI light
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class LightChangeColourHexEvent extends LightChangeEvent {

    /**
     * Construct the LightChangeColourHexEvent class
     * @param light The light for which the event occurred
     * @param oldProperties The old properties of the light (from before the event occurred)
     * @param newProperties The new properties of the light (from after the event occurred)
     * @since 1.0.0
     */
    public LightChangeColourHexEvent(Light light, LightProperties oldProperties, LightProperties newProperties) {
        super(light, oldProperties, newProperties);
    }

    /**
     * Get the old hexadecimal colour of the light (from before the event occurred)
     * @return The old hexadecimal colour of the light
     * @since 1.0.0
     */
    public String getOldColourHex(){
        return getOldProperties().getColourHex();
    }

    /**
     * Get the new hexadecimal colour of the light (from after the event occurred)
     * @return The new hexadecimal colour of the light
     * @since 1.0.0
     */
    public String getNewColourHex(){
        return getNewProperties().getColourHex();
    }

}
