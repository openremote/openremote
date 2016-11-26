package org.openremote.controller.event;

public abstract class Event<T> {

    /**
     * The ID of the originating sensor.
     */
    private int sourceSensorID;

    /**
     * The name of the originating sensor.
     */
    private String sourceSensorName;

    /**
     * Constructs a new event with a given sensor ID and sensor name.
     *
     * @param sourceSensorID   ID of the sensor that originated this event
     * @param sourceSensorName human-readable name of the sensor that originated this event
     */
    public Event(int sourceSensorID, String sourceSensorName) {
        this.sourceSensorID = sourceSensorID;
        this.sourceSensorName = sourceSensorName;
    }

    /**
     * Returns the ID of the sensor that originated this event.
     */
    public Integer getSourceID() {
        return sourceSensorID;
    }

    /**
     * Returns the human-readable name of the sensor that originated this event.
     */
    public String getSource() {
        return sourceSensorName;
    }

    public abstract Event clone(T newValue);

    /**
     * Returns the value of this event.
     *
     * @return event value
     */
    public abstract T getValue();

    public abstract void setValue(T value);

    public abstract boolean isEqual(Object o);

    /**
     *
     * This is an intermediate migration API -- at the end of the event processing chain
     * events are stored into the device state cache which panels consume. The API there is
     * still string-based.
     *
     * This method implementation should return an appropriate string representation of the
     * event value.
     *
     * @return string representation of event value
     */
    public abstract String serialize();

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        Event evt = (Event) o;

        return evt.getSourceID().equals(this.getSourceID()) &&
            evt.getSource().equals(this.getSource()) &&
            evt.getValue().equals(this.getValue());
    }

    @Override
    public int hashCode() {
        return getSourceID() + getSource().hashCode() + getValue().hashCode();
    }
}

