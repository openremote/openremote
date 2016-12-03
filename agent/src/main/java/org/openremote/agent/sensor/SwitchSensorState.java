package org.openremote.agent.sensor;

public class SwitchSensorState extends SensorState<String> {

    public enum State {

        ON("on"),
        OFF("off");

        final private String serializedValue;

        State(String serializedValue) {
            this.serializedValue = serializedValue;
        }

        public String serialize() {
            return serializedValue;
        }
    }

    private String value;
    private State switchState;

    public SwitchSensorState(int sourceID, String sourceName, String value, State switchState) {
        super(sourceID, sourceName);

        this.switchState = switchState;
        this.value = value;
    }

    public SwitchSensorState(int sourceID, String sourceName, State switchState) {
        this(sourceID, sourceName, switchState.serialize(), switchState);
    }

    public State getState() {
        return switchState;
    }

    public SwitchSensorState clone(String value, SwitchSensorState.State state) {
        if (value == null) {
            value = state.serialize();
        }
        return new SwitchSensorState(getSensorID(), getSensorName(), value, state);
    }

    @Override
    public SwitchSensorState clone(String newValue) {
        if (newValue.equalsIgnoreCase(SwitchSensorState.State.ON.serialize())) {
            return clone(null, SwitchSensorState.State.ON);
        } else {
            return clone(null, SwitchSensorState.State.OFF);
        }
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String serialize() {
        return getValue();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorID=" + getSensorID() +
            ", sensorName='" + getSensorName() + "'" +
            ", value='" + getValue() + '\'' +
            ", switchState=" + switchState +
            '}';
    }
}

