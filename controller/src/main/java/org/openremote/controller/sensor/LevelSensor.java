package org.openremote.controller.sensor;

import org.openremote.controller.command.SensorUpdateCommand;
import org.openremote.controller.deploy.SensorDefinition;

import java.util.logging.Logger;

public class LevelSensor extends RangeSensor {

    private static final Logger LOG = Logger.getLogger(LevelSensor.class.getName());

    public LevelSensor(SensorDefinition sensorDefinition, SensorUpdateCommand sensorUpdateCommand) {
        super(sensorDefinition, sensorUpdateCommand, 0, 100);
    }

    @Override
    public SensorState process(String value) {
        try {
            return new LevelSensorState(
                getSensorDefinition().getSensorID(),
                getSensorDefinition().getName(),
                new Integer(value.trim())
            );
        } catch (NumberFormatException e) {
            if (!isUnknownSensorValue(value)) {
                LOG.warning("Level sensor '" + getSensorDefinition() + "' produced a non-integer value: " + value);
            }
            return new UnknownState(this);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorDefinition=" + getSensorDefinition() +
            ", minValue=" + getMinValue() +
            ", maxValue=" + getMaxValue() +
            '}';
    }
}

