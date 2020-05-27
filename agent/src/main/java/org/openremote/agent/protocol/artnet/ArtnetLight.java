package org.openremote.agent.protocol.artnet;

public class ArtnetLight {

    private int lightId;
    private int groupId;
    private int universe;
    private int amountOfLeds;
    private String[] requiredValues;
    private byte[] prefix;
    private ArtnetLightState lightState;

    public ArtnetLight(int lightId, int groupId, int universe, int amountOfLeds, String[] requiredValues, ArtnetLightState lightState, byte[] prefix) {
        this.lightId = lightId;
        this.groupId = groupId;
        this.universe = universe;
        this.amountOfLeds = amountOfLeds;
        this.requiredValues = requiredValues;
        this.prefix = prefix;
        this.lightState = lightState;
    }

    public int getLightId() {
        return lightId;
    }

    public void setLightId(int lightId) {
        this.lightId = lightId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getUniverse() {
        return universe;
    }

    public void setUniverse(int universe) {
        this.universe = universe;
    }

    public int getAmountOfLeds() {
        return amountOfLeds;
    }

    public void setAmountOfLeds(int amountOfLeds) {
        this.amountOfLeds = amountOfLeds;
    }

    public String[] getRequiredValues() {
        return requiredValues;
    }

    public void setRequiredValues(String[] requiredValues) {
        this.requiredValues = requiredValues;
    }

    public byte[] getPrefix() {
        return prefix;
    }

    public void setPrefix(byte[] prefix) {
        this.prefix = prefix;
    }

    public ArtnetLightState getLightState() {
        return lightState;
    }

    public void setLightState(ArtnetLightState lightState) {
        this.lightState = lightState;
    }
}
