package org.openremote.controller.sensor;

import java.util.logging.Logger;

/**
 * Level sensor state is an integer value in the range of [0..100].
 */
public class LevelSensorState extends RangeSensorState {

    private static final Logger LOG = Logger.getLogger(LevelSensorState.class.getName());

    public LevelSensorState(int sourceSensorID, String sourceSensorName, int value) {
        super(sourceSensorID, sourceSensorName, validate(sourceSensorID, sourceSensorName, value), 0, 100);
    }

    public LevelSensorState clone(int newValue) {
        return new LevelSensorState(getSensorID(), getSensorName(), newValue);
    }

    /**
     * Validate range value to [0..100].
     *
     * @param sensorID   originating sensor ID
     * @param sensorName originating sensor name
     * @return sensor value unless it is exceeding LEVEL limits in which case it has been limited
     * to range [0..100]
     */
    static protected int validate(int sensorID, String sensorName, int value) {
        if (value > 100) {
            value = 100;
            LOG.log(java.util.logging.Level.WARNING,
                "A level sensor update was created with an invalid value : {0} (source = {1} - {2}). " +
                    "The value should be limited to maximum value of 100.", new Object[]{value, sensorID, sensorName}
            );
        }

        if (value < 0) {
            value = 0;
            LOG.log(java.util.logging.Level.WARNING,
                "A level sensor udpatewas created with an invalid value : {0} (source = {1} - {2}). " +
                    "The value should be limited to minimum value of 0.", new Object[]{value, sensorID, sensorName}
            );
        }
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorID=" + getSensorID() +
            ", sensorName='" + getSensorName() + "'" +
            ", timestamp=" + getTimestamp() +
            ", value='" + getValue() + "'" +
            "}";
    }
}

