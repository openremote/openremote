package org.openremote.agent.protocol.dmx;

public abstract class AbstractArtnetLight {

    private int lightId;
    private int groupId;
    private int universe;
    private int amountOfLeds;
    private String[] requiredValues;
    private byte[] prefix;

    private AbstractArtnetLightState lightState;


    public AbstractArtnetLight(int lightId, int groupId, int universe, int amountOfLeds, String[] requiredValues, AbstractArtnetLightState lightState, byte[] prefix) {
        this.lightId = lightId;
        this.groupId = groupId;
        this.universe = universe;
        this.amountOfLeds = amountOfLeds;
        this.requiredValues = requiredValues;
        this.prefix = prefix;
        this.lightState = lightState;
    }

    public int getLightId() {
        return this.lightId;
    }

    public void setLightId(int lightId) {
        this.lightId = lightId;
    }

    public int getUniverse() {
        return this.universe;
    }

    public void setUniverse(int universe) {
        this.universe = universe;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String[] getRequiredValues() {
        return requiredValues;
    }

    public void setRequiredValues(String[] requiredValues) {
        this.requiredValues = requiredValues;
    }

    public AbstractArtnetLightState getLightState() {
        return this.lightState;
    }

    public void setLightState(AbstractArtnetLightState lightState) {
        this.lightState = lightState;
    }

    public int getAmountOfLeds() {
        return this.amountOfLeds;
    }

    public void setAmountOfLeds(int amountOfLeds) {
        this.amountOfLeds = amountOfLeds;
    }

    public byte[] getPrefix() {
        return this.prefix;
    }

    public void setPrefix(byte[] prefix) {
        this.prefix = prefix;
    }


}
