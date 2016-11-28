package org.openremote.controller.event;

/**
 * Custom state event is a very basic event type that simply holds any arbitrary string
 * as its value. No semantics or rules are applied to the value string.
 * <p>
 * Custom state events are associated with 'custom' sensors in controller definition.
 */
public class CustomStateEvent extends Event<String> {

    private String eventValue;
    private String originalState;

    /**
     * Constructs a new custom state event with a given source sensor's ID, source sensors name
     * and event value.
     *
     * @param sourceSensorID   the integer ID of the sensor that originated this event
     * @param sourceSensorName the human-readable name of the sensor that originated this event
     * @param eventValue       an arbitrary string value of this event
     */
    public CustomStateEvent(int sourceSensorID, String sourceSensorName, String eventValue) {
        super(sourceSensorID, sourceSensorName);
        this.eventValue = eventValue;
        this.originalState = this.eventValue;
    }

    public CustomStateEvent(int sourceSensorID, String sourceSensorName, String eventValue, String originalState) {
        this(sourceSensorID, sourceSensorName, eventValue);
        this.originalState = originalState;
    }

    public String getOriginalState() {
        return originalState;
    }

    @Override
    public String getValue() {
        return eventValue;
    }

    @Override
    public void setValue(String value) {
        this.eventValue = value;
    }

    @Override
    public String serialize() {
        return eventValue;
    }

    @Override
    public CustomStateEvent clone(String newValue) {
        // TODO What is this?
        throw new Error("NYI");
    }

    @Override
    public boolean isEqual(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        CustomStateEvent cs = (CustomStateEvent) o;

        return cs.getSourceID().equals(this.getSourceID())
            && cs.getSource().equals(this.getSource())
            && cs.getValue().equals(this.getValue());
    }

    @Override
    public String toString() {
        return "CustomStateEvent{" +
            "sourceId=" + getSourceID() +
            ", source='" + getSource() + "'" +
            ", eventValue='" + eventValue + '\'' +
            ", originalState='" + originalState + '\'' +
            '}';
    }
}

