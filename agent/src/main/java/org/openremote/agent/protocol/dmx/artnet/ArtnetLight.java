package org.openremote.agent.protocol.dmx.artnet;

import org.openremote.agent.protocol.dmx.AbstractDMXLight;
import org.openremote.agent.protocol.dmx.AbstractDMXLightState;

public class ArtnetLight extends AbstractDMXLight {

    public ArtnetLight(int lightId, int universeId, int amountOfLeds, byte[] prefix) {
        super(lightId, universeId, amountOfLeds, prefix, null);
    }

    public ArtnetLight(int lightId, int universeId, int amountOfLeds, byte[] prefix, AbstractDMXLightState lightState) {
        super(lightId, universeId, amountOfLeds, prefix, lightState);
    }
}
