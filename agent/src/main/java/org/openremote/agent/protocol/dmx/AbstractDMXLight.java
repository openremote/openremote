package org.openremote.agent.protocol.dmx;

public abstract class AbstractDMXLight {

    private int lightId;
    private int universeId;
    private int amountOfLeds;
    private byte[] prefix;
    private AbstractDMXLightState lightState;

    public AbstractDMXLight(int lightId, int universeId, int amountOfLeds, byte[] prefix, AbstractDMXLightState lightState) {
        this.lightId = lightId;
        this.universeId = universeId;
        this.amountOfLeds = amountOfLeds;
        this.prefix = prefix;
        this.lightState = lightState;
    }

    public int getLightId() {
        return this.lightId;
    }

    public int getUniverseId() {
        return this.universeId;
    }

    public int getAmountOfLeds() {
        return this.amountOfLeds;
    }

    public byte[] getPrefix() {
        return this.prefix;
    }

    public AbstractDMXLightState getLightState() {
        return this.lightState;
    }

}
