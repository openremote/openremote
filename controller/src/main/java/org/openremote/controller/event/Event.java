package org.openremote.controller.event;

public abstract class Event<T> {

    final protected int sourceSensorID;
    final protected String sourceSensorName;

    public Event(int sourceSensorID, String sourceSensorName) {
        this.sourceSensorID = sourceSensorID;
        this.sourceSensorName = sourceSensorName;
    }

    public Integer getSourceID() {
        return sourceSensorID;
    }

    public String getSource() {
        return sourceSensorName;
    }

    public abstract Event clone(T newValue);

    public abstract T getValue();

    public abstract void setValue(T value);

    public abstract boolean isEqual(Object o);

    /**
     * This method implementation should return an appropriate string representation of the event value.
     * This value will be returned by {@link org.openremote.controller.context.DataContext#queryValue(int)}.
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

