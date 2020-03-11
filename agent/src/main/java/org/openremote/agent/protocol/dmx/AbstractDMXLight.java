package org.openremote.agent.protocol.dmx;

import java.util.List;

public abstract class AbstractDMXLight {

    private int lightId;
    private int groupId;
    private int universe;
    private int amountOfLeds;
    private String[] requiredValues;
    private byte[] prefix;

    private AbstractDMXLightState lightState;


    public AbstractDMXLight(int lightId, int groupId, int universe, int amountOfLeds, String[] requiredValues, AbstractDMXLightState lightState, byte[] prefix) {
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

    public int getUniverse() {
        return this.universe;
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

    public void setLightState(AbstractDMXLightState lightState) {
        this.lightState = lightState;
    }

    public void setRequiredValues(String[] requiredValues) {
        this.requiredValues = requiredValues;
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
