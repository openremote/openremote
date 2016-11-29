package org.openremote.controller.event;

import java.util.logging.Logger;

/**
 * Level events are integer based events where values have been limited to a range of
 * [0..100]
 * <p>
 * Level events are associated with sensors of 'level' type in controller.xml model.
 */
public class LevelEvent extends RangeEvent {

    private static final Logger LOG = Logger.getLogger(LevelEvent.class.getName());

    /**
     * Validate range value to [0..100].
     *
     * @param sensorID   originating sensor ID
     * @param sensorName originating sensor name
     * @param value      event value
     * @return event value unless it is exceeding LEVEL limits in which case it has been limited
     * to range [0..100]
     */
    private static int validate(int sensorID, String sensorName, int value) {
        if (value > 100) {
            value = 100;
            LOG.log(java.util.logging.Level.WARNING,
                "A LEVEL event was created with an invalid value : {0} (Source Sensor = {1} - {2}). " +
                    "The event value has been limited to maximum value of 100.", new Object[]{value, sensorID, sensorName}
            );
        }

        if (value < 0) {
            value = 0;
            LOG.log(java.util.logging.Level.WARNING,
                "A LEVEL event was create with an invalid value : {0} (Source Sensor = {1} - {2}). " +
                    "The event value has been limited to minimum value of 0.", new Object[]{value, sensorID, sensorName}
            );
        }
        return value;
    }

    /**
     * Constructs a new LEVEL event with a given originating sensor ID, originating sensor name
     * and event value.
     * <p>
     * Level events are associated with sensors of 'level' type in controller.xml model.
     * <p>
     * Level event values must be restricted to a range of [0..100].
     *
     * @param sourceSensorID   originating sensor's ID
     * @param sourceSensorName originating sensor's name
     * @param value            level event value
     */
    public LevelEvent(int sourceSensorID, String sourceSensorName, int value) {
        super(sourceSensorID, sourceSensorName, validate(sourceSensorID, sourceSensorName, value), 0, 100);
    }

    public LevelEvent clone(int newValue) {
        return new LevelEvent(getSourceID(), getSource(), newValue);
    }

    @Override
    public String toString() {
        return "LevelEvent{" +
            "sourceId=" + getSourceID() +
            ", source='" + getSource() + "'" +
            "}";
    }
}

