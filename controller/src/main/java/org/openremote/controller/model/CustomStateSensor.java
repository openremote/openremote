package org.openremote.controller.model;

import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.command.PullCommand;
import org.openremote.controller.command.PushCommand;
import org.openremote.controller.event.CustomStateEvent;
import org.openremote.controller.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A state sensor operates on a finite set of explicit state values that it returns.
 * <p>
 * Explicit state values returned from a command implementation may
 * be mapped to other values to accommodate human-consumable values for the panel UI
 * for example, or as a translation mechanism for localized interfaces.
 * <p>
 * By default the explicit state strings this sensor expects the event producers to return are
 * available as sensor properties through the {@link PullCommand} and {@link PushCommand} APIs.
 * <p>
 * See {@link DistinctStates} for more details.
 */
public class CustomStateSensor extends Sensor {

    private static final Logger LOG = Logger.getLogger(CustomStateSensor.class.getName());

    final protected DistinctStates states;
    final protected boolean strictStateMapping;

    /**
     * Constructs a new sensor with a given sensor ID, event producer and distinct state values
     * this sensor will return.
     * <p>
     * This constructor allows subclasses to determine whether the distinct states are passed
     * as sensor properties through the {@link PullCommand}
     * and {@link PushCommand} interface -- when the sensor's
     * type (such as {@link org.openremote.controller.model.SwitchSensor}) makes the available
     * states explicit, it may not be necessary to pass the additional property information to
     * event producer implementers.
     *
     * @param producer           the data/event producing command implementation that backs this sensor
     * @param states             distinct state values and their mappings this sensor will return
     * @param strictStateMapping Indicates whether this sensor instance should enforce strict state mapping rules -- when
     *                           enabled, only state values explicitly declared for this sensor will be accepted and returned.
     *                           If set to false, all values are allowed but those with value mappings will be converted.
     */
    protected CustomStateSensor(SensorDefinition sensorDefinition, EventProducerCommand producer, DistinctStates states, boolean strictStateMapping) {
        super(sensorDefinition, producer);
        if (states == null) {
            this.states = new DistinctStates();
        } else {
            this.states = states;
        }
        this.strictStateMapping = strictStateMapping;
    }

    /**
     * Constructs an event from the raw protocol output string. This implementation checks the
     * incoming value for possible state mappings and returns an event containing the mapped
     * state value where necessary. Arbitrary, non-declared states are rejected unless
     * {@link #strictStateMapping} has been set to false. When set to true, an
     * instance of {@link org.openremote.controller.model.Sensor.UnknownEvent} is returned
     * for any non-declared state.
     *
     * @return An event containing the state returned by the associated event producer or
     * a translated (mapped) version of the state string. Can return
     * {@link org.openremote.controller.model.Sensor.UnknownEvent} in case where
     * {@link #strictStateMapping} is true and a state value is returned from the
     * event producer that has not been declared as a distinct state for this sensor.
     */
    @Override
    public Event processEvent(String value) {
        if (!states.hasState(value)) {
            if (isUnknownSensorValue(value)) {
                return new UnknownEvent(this);
            }

            if (strictStateMapping) {
                LOG.warning("Event producer's value is not consistent with '" + getSensorDefinition() + "': " + value);
                return new UnknownEvent(this);
            } else {
                return createEvent(value);
            }
        }

        if (!states.hasMapping(value)) {
            return createEvent(value);
        } else {
            return createEvent(states.getMapping(value), value);
        }
    }

    /**
     * Constructs an event for this sensor with a given state value. Subclasses can override this
     * implementation to provide their own specific event types.
     *
     * @param value event value
     * @return new event instance associated with this sensor
     */
    protected Event createEvent(String value) {
        return new CustomStateEvent(getSensorDefinition().getSensorID(), getSensorDefinition().getName(), value);
    }

    /**
     * Constructs an event for this sensor with a given state value, and the originating value
     * when state mapping (translation) was used. Subclasses can override this implementation to
     * provide their own specific event types.
     *
     * @param value         the translated (mapped) event value
     * @param originalValue the original event value returned by the associated event producer
     * @return new event instance associated with this sensor
     */
    protected Event createEvent(String value, String originalValue) {
        return new CustomStateEvent(getSensorDefinition().getSensorID(), getSensorDefinition().getName(), value, originalValue);
    }

    /**
     * Helper class to store the distinct state values for a state sensor and possible value
     * mappings if configured.
     * <p>
     * Each state is available to {@link EventProducerCommand} implementers through the sensor
     * properties passed via the {@link PullCommand} and {@link PushCommand} APIs. The expected
     * state values can be found using a key 'state-1' for the first available state string to
     * 'state-n' to the last expected state value.
     */
    public static class DistinctStates {

        private Map<String, String> states = new HashMap<>();

        /**
         * Store an explicit state value without mapping.
         *
         * @param state explicit state string the event producers are expected to return from
         *              their {@link PullCommand} or
         *              {@link PushCommand} implementations.
         */
        public void addState(String state) {
            states.put(state, null);
        }

        /**
         * Stores an explicit state value with mapping. When the event producer returns the state
         * value, it is automatically mapped to a new value to be consumed by UI widgets and other
         * users of the sensor.
         *
         * @param state   the state string returned by event producer implementations
         * @param mapping the value the state string is translated to by the sensor
         */
        public void addStateMapping(String state, String mapping) {
            states.put(state, mapping);
        }

        /**
         * Indicates if the given state string is contained within this state collection
         *
         * @param value the requested state string
         * @return true if the state string has been added, false otherwise
         */
        public boolean hasState(String value) {
            return states.containsKey(value);
        }

        /**
         * Returns sensor's state mappings as a string. Implementation delegates to
         * {@link Map#toString()}.
         *
         * @return all state mappings as a string
         */
        @Override
        public String toString() {
            return states.toString();
        }

        /**
         * Indicates if the given state string has a mapping in this state collection
         *
         * @param state the state string which mapping is requested
         * @return true if the state string is mapped to another value in this state
         * collection, false otherwise
         */
        private boolean hasMapping(String state) {
            if (!hasState(state)) {
                return false;
            }
            String mapping = states.get(state);
            return mapping != null;
        }

        /**
         * Returns the mapped value of a state string.
         *
         * @param state the state string which mapping is requested
         * @return returns the translated value of a given event producer state string or {@link #UNKNOWN_STATUS}
         */
        private String getMapping(String state) {
            String mapping = states.get(state);
            if (mapping == null) {
                return UNKNOWN_STATUS;
            }
            return mapping;
        }

        private Set<String> getAllStates() {
            return states.keySet();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorDefinition=" + getSensorDefinition() +
            ", hasStrictStateMapping=" + strictStateMapping +
            ", states=" + states +
            '}';
    }
}

