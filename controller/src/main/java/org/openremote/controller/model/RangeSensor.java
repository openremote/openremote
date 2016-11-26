package org.openremote.controller.model;

import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.event.Event;
import org.openremote.controller.event.RangeEvent;
import org.openremote.controller.statuscache.StatusCache;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RangeSensor extends Sensor {

    private static final Logger LOG = Logger.getLogger(RangeSensor.class.getName());

    private int minValue;
    private int maxValue;

    public RangeSensor(String name, int sensorID, StatusCache cache, EventProducerCommand eventProducerCommand, int commandID, int minValue, int maxValue) {
        super(name, sensorID, cache, eventProducerCommand, commandID, new HashMap<>());
        if (minValue > maxValue)
            throw new IllegalArgumentException("Sensor value min '" + minValue + "' is larger than max '" + maxValue + "'");
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    @Override
    protected Event processEvent(String value) {
        try {
            return new RangeEvent(getSensorID(), getName(), new Integer(value.trim()), getMinValue(), getMaxValue());
        } catch (NumberFormatException exception) {
            if (!isUnknownSensorValue(value)) {
                LOG.log(
                    Level.WARNING,
                    "Sensor ''{0}'' (ID = {1}) is RANGE type but produced a value that is not an integer : ''{2}''",
                    new Object[]{getName(), getSensorID(), value}
                );
            }
            return new UnknownEvent(this);
        }

    }

    @Override
    public String toString() {
        return "RangeSensor{" +
            ", sensorID=" + getSensorID() +
            ", commandID=" + getCommandID() +
            ", minValue=" + minValue +
            ", maxValue=" + maxValue +
            '}';
    }
}

