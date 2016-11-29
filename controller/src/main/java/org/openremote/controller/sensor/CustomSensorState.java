package org.openremote.controller.sensor;

/**
 * Holds the original raw value and the result of processing {@link CustomSensor.DistinctStates}.
 */
public class CustomSensorState extends SensorState<String> {

    private String value;
    private String originalValue;

    public CustomSensorState(int sourceSensorID, String sourceSensorName, String value) {
        super(sourceSensorID, sourceSensorName);
        this.value = value;
        this.originalValue = this.value;
    }

    public CustomSensorState(int sourceSensorID, String sourceSensorName, String value, String originalValue) {
        this(sourceSensorID, sourceSensorName, value);
        this.originalValue = originalValue;
    }

    public String getOriginalValue() {
        return originalValue;
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
        return value;
    }

    @Override
    public CustomSensorState clone(String newValue) {
        // TODO What is this?
        throw new Error("NYI");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorID=" + getSensorID() +
            ", sensorName='" + getSensorName() + "'" +
            ", timestamp=" + getTimestamp() +
            ", value='" + getValue() + '\'' +
            ", originalState='" + getOriginalValue() + '\'' +
            '}';
    }
}

