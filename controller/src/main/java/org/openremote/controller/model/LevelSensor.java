package org.openremote.controller.model;

import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.deploy.SensorDefinition;
import org.openremote.controller.event.Event;
import org.openremote.controller.event.LevelEvent;

import java.util.logging.Logger;

public class LevelSensor extends RangeSensor {

    private static final Logger LOG = Logger.getLogger(LevelSensor.class.getName());

    public LevelSensor(SensorDefinition sensorDefinition, EventProducerCommand eventProducerCommand) {
        super(sensorDefinition, eventProducerCommand, 0, 100);
    }

    @Override
    public Event processEvent(String value) {
        try {
            return new LevelEvent(
                getSensorDefinition().getSensorID(),
                getSensorDefinition().getName(),
                new Integer(value.trim())
            );
        } catch (NumberFormatException e) {
            if (!isUnknownSensorValue(value)) {
                LOG.warning("Level sensor '" + getSensorDefinition() + "' produced a non-integer value: " + value);
            }
            return new UnknownEvent(this);
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

