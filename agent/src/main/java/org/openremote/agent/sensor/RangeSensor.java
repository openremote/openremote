package org.openremote.agent.sensor;

import org.openremote.agent.command.SensorUpdateCommand;
import org.openremote.agent.deploy.SensorDefinition;

import java.util.logging.Logger;

public class RangeSensor extends Sensor {

    private static final Logger LOG = Logger.getLogger(RangeSensor.class.getName());

    private int minValue;
    private int maxValue;

    public RangeSensor(SensorDefinition sensorDefinition, SensorUpdateCommand sensorUpdateCommand, int minValue, int maxValue) {
        super(sensorDefinition, sensorUpdateCommand);
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
    protected SensorState process(String value) {
        try {
            return new RangeSensorState(
                getSensorDefinition().getSensorID(),
                getSensorDefinition().getName(),
                new Integer(value.trim()),
                getMinValue(),
                getMaxValue()
            );
        } catch (NumberFormatException exception) {
            if (!SensorState.isUnknownState(value)) {
                LOG.warning("Range sensor '" + getSensorDefinition() + "' produced a non-integer value: " + value);
            }
            return new UnknownState(this);
        }

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorDefinition=" + getSensorDefinition() +
            ", minValue=" + minValue +
            ", maxValue=" + maxValue +
            '}';
    }
}


