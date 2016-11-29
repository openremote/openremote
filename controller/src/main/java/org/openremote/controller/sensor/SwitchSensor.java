package org.openremote.controller.sensor;

import org.bouncycastle.util.Strings;
import org.openremote.controller.command.SensorUpdateCommand;
import org.openremote.controller.deploy.SensorDefinition;

import java.util.logging.Logger;

/**
 * A boolean switch sensor type. This is a specific case of a state sensor with two 'on' or 'off'
 * state strings. A switch sensor can be configured to map the default return values to other
 * values ("open/close", "running/stoppped", etc.) or to translate return values to local
 * languages.
 */
public class SwitchSensor extends CustomSensor {

    private static final Logger LOG = Logger.getLogger(SwitchSensor.class.getName());

    /**
     * Constructs a new switch sensor with given on/off state mapping. The distinct states should
     * contain mapping <b>only</b> for 'on' and 'off' value in case of a switch.
     * <p>
     * The sensor implementation will return 'on or 'off' string values.
     *
     * @param producer the udpate command implementation that backs this sensor
     * @param states   state string mappings for the default 'on' and 'off' values
     */
    public SwitchSensor(SensorDefinition sensorDefinition, SensorUpdateCommand producer, DistinctStates states) {
        super(sensorDefinition, producer, states, true);
    }

    /**
     * Constructs state with a given value, which must be either 'on' or 'off'.
     * Any other value will cause {@link UnknownState} instance to be returned instead.
     */
    @Override
    protected SensorState createSensorState(String value) {
        return createSensorState(value, value);
    }

    /**
     * Constructs state with a given mapped (translated) value and the original
     * value which was used for the translation.
     * <p>
     * The original value must be either 'on' or 'off' (case insensitive), otherwise a
     * {@link UnknownState} is returned instead. The translated value can be any
     * arbitrary string value.
     */
    @Override
    protected SensorState createSensorState(String value, String originalValue) {
        try {
            return new SwitchSensorState(
                getSensorDefinition().getSensorID(),
                getSensorDefinition().getName(),
                value,
                SwitchSensorState.State.valueOf(Strings.toUpperCase(originalValue.trim())) // TODO WTF
            );
        } catch (IllegalArgumentException e) {
            if (!isUnknownSensorValue(originalValue)) {
                LOG.warning(
                    "Switch sensor value must be 'on' or 'off', got '" + originalValue + "' in: " + getSensorDefinition()
                );
            }
            return new UnknownState(this);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorDefinition=" + getSensorDefinition() +
            ", states=" + states +
            '}';
    }
}

