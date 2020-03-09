package org.openremote.agent.protocol.dmx.artnet;

import org.openremote.agent.protocol.dmx.AbstractDMXLight;
import org.openremote.agent.protocol.dmx.AbstractDMXLightState;

public class ArtnetLight extends AbstractDMXLight {

    public ArtnetLight(int lightId, int groupId, int universe, int amountOfLeds, String[] requiredValues) {
        super(lightId, groupId, universe, amountOfLeds, requiredValues, null, null);
    }

    public ArtnetLight(int lightId, int groupId, int universe, int amountOfLeds, String[] requiredValues, AbstractDMXLightState lightState, byte[] prefix) {
        super(lightId, groupId, universe, amountOfLeds, requiredValues, lightState, null);
    }
}
