package org.openremote.agent.sensor;

import org.openremote.agent.command.PullCommand;
import org.openremote.agent.command.PushCommand;
import org.openremote.agent.command.SensorUpdateCommand;
import org.openremote.agent.deploy.SensorDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A custom sensor operates on a finite set of explicit state values that it returns, see {@link DistinctStates}.
 * TODO: Write more docs
 */
public class CustomSensor extends Sensor {

    private static final Logger LOG = Logger.getLogger(CustomSensor.class.getName());

    final protected DistinctStates states;
    final protected boolean strictStateMapping;

    /**
     * This constructor allows subclasses to determine whether the distinct states are passed
     * as sensor properties through the {@link PullCommand} and {@link PushCommand} interface.
     * When the sensor's type (such as {@link SwitchSensor}) makes the available states explicit,
     * it may not be necessary to pass the additional property information to
     * {@link SensorUpdateCommand} implementations.
     *
     * @param updateCommand      the command implementation that backs this sensor and can produce the current state
     * @param states             distinct state values and their mappings this sensor will return
     * @param strictStateMapping Indicates whether this sensor instance should enforce strict state mapping rules -- when
     *                           enabled, only state values explicitly declared for this sensor will be accepted and returned.
     *                           If set to false, all values are allowed but those with value mappings will be converted.
     */
    public CustomSensor(SensorDefinition sensorDefinition, SensorUpdateCommand updateCommand, DistinctStates states, boolean strictStateMapping) {
        super(sensorDefinition, updateCommand);
        if (states == null) {
            this.states = new DistinctStates();
        } else {
            this.states = states;
        }
        this.strictStateMapping = strictStateMapping;
    }

    /**
     * Constructs the state from the raw command output string. This implementation checks the
     * incoming value for possible state mappings and returns the mapped state value where necessary.
     * Arbitrary, non-declared states are rejected unless {@link #strictStateMapping} has been set
     * to false. When set to true, an instance of {@link UnknownState} is returned for any
     * non-declared state.
     */
    @Override
    public SensorState process(String value) {
        if (!states.hasState(value)) {
            if (SensorState.isUnknownState(value)) {
                return new UnknownState(this);
            }

            if (strictStateMapping) {
                LOG.warning("Update command's value is not consistent with '" + getSensorDefinition() + "': " + value);
                return new UnknownState(this);
            } else {
                return createSensorState(value);
            }
        }

        if (!states.hasMapping(value)) {
            return createSensorState(value);
        } else {
            return createSensorState(states.getMapping(value), value);
        }
    }

    /**
     * Constructs state for this sensor with a given state value. Subclasses can override this
     * implementation to provide their own specific state types.
     *
     * @param value state value
     * @return new state instance associated with this sensor
     */
    protected SensorState createSensorState(String value) {
        return new CustomSensorState(getSensorDefinition().getSensorID(), getSensorDefinition().getName(), value);
    }

    /**
     * Constructs state for this sensor with a given state value, and the originating value
     * when state mapping (translation) was used. Subclasses can override this implementation to
     * provide their own specific state types.
     *
     * @param value         the translated (mapped) state value
     * @param originalValue the original state value returned by the associated update command
     * @return new state instance associated with this sensor
     */
    protected SensorState createSensorState(String value, String originalValue) {
        return new CustomSensorState(getSensorDefinition().getSensorID(), getSensorDefinition().getName(), value, originalValue);
    }

    /**
     * Holds the distinct state values for a custom sensor and possible value mappings if configured.
     * <p>
     * Each state is available to {@link SensorUpdateCommand} implementers through the sensor
     * properties passed via the {@link PullCommand} and {@link PushCommand} APIs. The expected
     * state values can be found using a key 'state-1' for the first available state string to
     * 'state-n' to the last expected state value.
     * TODO This is no longer true, why would the implementers need that info anyway? It's the job of this class to shield them from state mappings!
     */
    public static class DistinctStates {

        private Map<String, String> states = new HashMap<>();

        /**
         * Store an explicit state value without mapping.
         */
        public void addState(String state) {
            states.put(state, null);
        }

        /**
         * Stores an explicit state value with mapping. When the update command returns the state
         * value, it is automatically mapped to a new value to be consumed by UI widgets and other
         * users of the sensor.
         *
         * @param state   the state string returned by update command implementations
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
        public  boolean hasMapping(String state) {
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
         * @return returns the translated value of a given state string or {@link SensorState#UNKNOWN_VALUE}
         */
        public  String getMapping(String state) {
            String mapping = states.get(state);
            if (mapping == null) {
                return SensorState.UNKNOWN_VALUE;
            }
            return mapping;
        }

        public Set<String> getAllStates() {
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

