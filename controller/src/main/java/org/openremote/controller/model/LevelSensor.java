package org.openremote.controller.model;

import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.event.Event;
import org.openremote.controller.event.LevelEvent;
import org.openremote.controller.statuscache.StatusCache;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LevelSensor extends RangeSensor {

    private static final Logger LOG = Logger.getLogger(LevelSensor.class.getName());

    public LevelSensor(String name, int sensorID, StatusCache cache, EventProducerCommand eventProducerCommand, int commandID) {
        super(name, sensorID, cache, eventProducerCommand, commandID, 0, 100);
    }

    @Override
    public Event processEvent(String value) {
        try {
            return new LevelEvent(getSensorID(), getName(), new Integer(value.trim()));
        } catch (NumberFormatException e) {
            if (!isUnknownSensorValue(value)) {
                LOG.log(
                    Level.WARNING,
                    "Sensor ''{0}'' (ID = {1}) is LEVEL type but produced a value that is not " +
                        " an integer : ''{2}''",
                    new Object[]{getName(), getSensorID(), value}
                );
            }
            return new UnknownEvent(this);
        }
    }

}

