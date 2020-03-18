package org.openremote.agent.protocol.dmx.artnet;

import org.openremote.agent.protocol.dmx.AbstractArtnetLight;
import org.openremote.agent.protocol.dmx.AbstractArtnetLightState;

public class ArtnetLight extends AbstractArtnetLight {

    public ArtnetLight(int lightId, int groupId, int universe, int amountOfLeds, String[] requiredValues) {
        super(lightId, groupId, universe, amountOfLeds, requiredValues, null, null);
    }

    public ArtnetLight(int lightId, int groupId, int universe, int amountOfLeds, String[] requiredValues, AbstractArtnetLightState lightState, byte[] prefix) {
        super(lightId, groupId, universe, amountOfLeds, requiredValues, lightState, null);
    }
}
