package org.openremote.agent.protocol.dmx;

import org.openremote.model.attribute.*;

public abstract class AbstractDMXLightState {

    private int lightId;

    public AbstractDMXLightState(int lightId) {
        this.lightId = lightId;
    }

    public int getLightId() {
        return this.lightId;
    }

    public abstract Byte[] getValues();

    public abstract void FromAttribute(AttributeEvent event);

}
