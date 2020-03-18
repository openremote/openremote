package org.openremote.agent.protocol.dmx;

import org.openremote.model.attribute.*;

public abstract class AbstractArtnetLightState {

    private int lightId;

    public AbstractArtnetLightState(int lightId) {
        this.lightId = lightId;
    }
    public int getLightId() {
        return this.lightId;
    }
    public abstract Byte[] getValues();

    public abstract void fromAttribute(AttributeEvent event, Attribute attribute);

}
