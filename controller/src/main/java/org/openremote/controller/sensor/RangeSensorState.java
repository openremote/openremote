package org.openremote.controller.sensor;

public class RangeSensorState extends SensorState<Integer> {

    private Integer rangeValue;
    private Integer min;
    private Integer max;

    public RangeSensorState(int sourceID, String sourceName, Integer value, Integer min, Integer max) {
        super(sourceID, sourceName);
        this.min = min;
        this.max = max;
        setValue(value);
    }

    public Integer getMinValue() {
        return min;
    }

    public Integer getMaxValue() {
        return max;
    }

    @Override
    public Integer getValue() {
        return rangeValue;
    }

    @Override
    public void setValue(Integer value) {
        // TODO: The LevelSensorState logs when boundaries are breached, why don't we log here?
        if (value > max) {
            this.rangeValue = max;
        } else if (value < min) {
            this.rangeValue = min;
        } else {
            this.rangeValue = value;
        }
    }

    @Override
    public RangeSensorState clone(Integer newValue) {
        if (newValue < getMinValue()) {
            newValue = getMinValue();
        } else if (newValue > getMaxValue()) {
            newValue = getMaxValue();
        }

        return new RangeSensorState(this.getSensorID(), this.getSensorName(), newValue, getMinValue(), getMaxValue());
    }

    @Override
    public String serialize() {
        return Integer.toString(rangeValue);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorID=" + getSensorID() +
            ", sensorName='" + getSensorName() + "'" +
            ", timestamp=" + getTimestamp() +
            ", value=" + getValue() +
            ", min=" + getMinValue() +
            ", max=" + getMaxValue() +
            '}';
    }
}

