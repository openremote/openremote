package org.openremote.controller.model;

import org.bouncycastle.util.Strings;
import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.event.Event;
import org.openremote.controller.event.SwitchEvent;
import org.openremote.controller.statuscache.StatusCache;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A boolean switch sensor type. This is a specific case of a state sensor with two 'on' or 'off'
 * state strings. A switch sensor can be configured to map the default return values to other
 * values ("open/close", "running/stoppped", etc.) or to translate return values to local
 * languages.
 */
public class SwitchSensor extends StateSensor {

    private static final Logger LOG = Logger.getLogger(SwitchSensor.class.getName());

    /**
     * Set up the distinct on/off states for the superclass constructor
     *
     * @return distinct states for the state sensor
     */
    private static DistinctStates createSwitchStates() {
        DistinctStates states = new DistinctStates();
        states.addState("on");
        states.addState("off");
        return states;
    }

    /**
     * Constructs a new switch sensor with given sensor ID and event producer. The sensor
     * implementation will return 'on' or 'off' string values.
     *
     * @param name      human-readable name of this sensor
     * @param sensorID  controller unique identifier
     * @param cache     reference to the device state cache this sensor is associated with
     * @param producer  the protocol handler that backs this sensor either with a read command
     *                  or event listener implementation
     * @param commandID controller unique identifier of related command
     */
    public SwitchSensor(String name, int sensorID, StatusCache cache, EventProducerCommand producer, int commandID) {
        this(name, sensorID, cache, producer, commandID, createSwitchStates());
    }

    /**
     * Constructs a new switch sensor with given sensor ID, an event producer, and on/off state
     * mapping. The distinct states should contain mapping <b>only</b> for 'on' and 'off' states
     * in case of a switch.
     *
     * The sensor implementation will return 'on or 'off' string values.
     *
     * @param name      human-readable name of this sensor
     * @param sensorID  controller unique identifier
     * @param cache     reference to the device state cache this sensor is associated with
     * @param producer  the protocol handler that backs this sensor either with a read command
     *                  or event listener implementation
     * @param commandID controller unique identifier of related command
     * @param states    state string mappings for the default 'on' and 'off' values
     */
    public SwitchSensor(String name, int sensorID, StatusCache cache, EventProducerCommand producer, int commandID, DistinctStates states) {
        super(name, sensorID, cache, producer, commandID, states, false);
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
     *
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
                getSensorID(), getName(), value, SwitchEvent.State.valueOf(Strings.toUpperCase(originalValue.trim()))
            );
        } catch (IllegalArgumentException e) {
            if (!isUnknownSensorValue(originalValue)) {
                LOG.log(
                    Level.WARNING,
                    "Switch event value must be either 'on' or 'off', got ''{0}'' in {1}",
                    new Object[]{originalValue, this}
                );
            }

            return new UnknownEvent(this);
        }
    }

    @Override
    public String toString() {
        return "SwitchSensor{" +
            ", sensorID=" + getSensorID() +
            ", commandID=" + getCommandID()
            + "}";
    }
}

