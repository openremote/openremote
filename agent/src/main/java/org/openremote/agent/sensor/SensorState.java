package org.openremote.agent.sensor;

import org.openremote.agent.context.AgentContext;

/**
 * A state snapshot of a sensor, at a particular moment in time.
 *
 * @param <T> The type of value of this state snapshot.
 */
public abstract class SensorState<T> {

    public static final String UNKNOWN_VALUE = "N/A";

    /**
     * Helper method to allow subclasses to check if a given value matches the 'N/A' string that
     * is used for uninitialized or error states in sensor implementations.
     */
    public static boolean isUnknownState(String value) {
        return value.equals(UNKNOWN_VALUE);
    }

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
     * This string will be returned by {@link AgentContext#queryValue}.
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

