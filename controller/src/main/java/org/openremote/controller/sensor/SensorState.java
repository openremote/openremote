package org.openremote.controller.sensor;

/**
 * A state snapshot of a sensor, at a particular moment in time.
 *
 * @param <T> The type of value of this state snapshot.
 */
public abstract class SensorState<T> {

    final protected long timestamp = System.currentTimeMillis();
    final public int sensorID;
    final public String sensorName;

    public SensorState(int sensorID, String sensorName) {
        this.sensorID = sensorID;
        this.sensorName = sensorName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Integer getSensorID() {
        return sensorID;
    }

    public String getSensorName() {
        return sensorName;
    }

    public abstract SensorState clone(T newValue);

    public abstract T getValue();

    public abstract void setValue(T value);

    /**
     * This method implementation should return an appropriate string representation of the state value.
     * This string will be returned by {@link org.openremote.controller.context.ControllerContext#queryValue}.
     */
    public abstract String serialize();

    /**
     * Two sensor states are equal if they have the same sensor ID, sensor name, and value.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        SensorState that = (SensorState) o;

        return that.getSensorID().equals(this.getSensorID()) &&
            that.getSensorName().equals(this.getSensorName()) &&
            that.getValue().equals(this.getValue());
    }

    @Override
    public int hashCode() {
        return getSensorID() + getSensorName().hashCode() + getValue().hashCode();
    }
}

