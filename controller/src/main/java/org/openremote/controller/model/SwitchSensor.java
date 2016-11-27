package org.openremote.controller.model;

import org.bouncycastle.util.Strings;
import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.event.Event;
import org.openremote.controller.event.SwitchEvent;

import java.util.logging.Logger;

/**
 * A boolean switch sensor type. This is a specific case of a state sensor with two 'on' or 'off'
 * state strings. A switch sensor can be configured to map the default return values to other
 * values ("open/close", "running/stoppped", etc.) or to translate return values to local
 * languages.
 */
public class SwitchSensor extends CustomStateSensor {

    private static final Logger LOG = Logger.getLogger(SwitchSensor.class.getName());

    /**
     * Constructs a new switch sensor with given sensor ID, an event producer, and on/off state
     * mapping. The distinct states should contain mapping <b>only</b> for 'on' and 'off' states
     * in case of a switch.
     * <p>
     * The sensor implementation will return 'on or 'off' string values.
     *
     * @param producer  the data/event producing command implementation that backs this sensor
     * @param states    state string mappings for the default 'on' and 'off' values
     */
    public SwitchSensor(SensorDefinition sensorDefinition, EventProducerCommand producer, DistinctStates states) {
        super(sensorDefinition, producer, states, true);
    }

    /**
     * Constructs an event for this sensor instance with a given event value. Event value must be
     * either 'on' or 'off' -- any other value will cause a
     * {@link org.openremote.controller.model.Sensor.UnknownEvent} instance to be returned
     * instead.
     *
     * @param value event value 'on' or 'off' -- case insensitive
     * @return a new event instance for this sensor
     */
    @Override
    protected Event createEvent(String value) {
        return createEvent(value, value);
    }

    /**
     * Constructs an event for this sensor instance with a given mapped (translated) event value
     * and the original event value which was used for the translation.
     * <p>
     * The original value must be either 'on' or 'off' (case insensitive), otherwise a
     * {@link org.openremote.controller.model.Sensor.UnknownEvent} is returned instead. The
     * translated value can be any arbitrary string value.
     *
     * @param value         the translated (mapped) event value
     * @param originalValue the original event value ('on' or 'off') returned by the associated
     *                      event producer
     * @return a new event instance for this sensor
     */
    @Override
    protected Event createEvent(String value, String originalValue) {
        try {
            return new SwitchEvent(
                getSensorDefinition().getSensorID(),
                getSensorDefinition().getName(),
                value,
                SwitchEvent.State.valueOf(Strings.toUpperCase(originalValue.trim())) // TODO WTF
            );
        } catch (IllegalArgumentException e) {
            if (!isUnknownSensorValue(originalValue)) {
                LOG.warning(
                    "Switch event value not 'on' or 'off', got '" + originalValue + "' in: " + getSensorDefinition()
                );
            }
            return new UnknownEvent(this);
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

