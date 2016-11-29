package org.openremote.controller.deploy;

import java.util.HashMap;
import java.util.Map;

/**
 * Sensor deployment model
 */
public class SensorDefinition {

    final protected int sensorID;
    final protected String name;
    final protected String type;
    final protected CommandDefinition updateCommandDefinition;
    final protected Map<String, String> properties;

    /**
     * @param name                    Human readable name of the sensor.
     * @param sensorID                A unique sensor ID. Must be unique per controller deployment.
     * @param type                    A sensor type.
     * @param updateCommandDefinition A command definition used to update sensor values.
     * @param properties              Additional sensor properties. These properties can be used by the protocol
     *                                implementors to direct their implementation according to sensor configuration.
     */
    public SensorDefinition(int sensorID, String name, String type, CommandDefinition updateCommandDefinition, Map<String, String> properties) {
        if (properties == null) {
            properties = new HashMap<>(0);
        }
        this.sensorID = sensorID;
        this.name = name;
        this.type = type;
        this.updateCommandDefinition = updateCommandDefinition;
        this.properties = properties;
    }

    public int getSensorID() {
        return sensorID;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public CommandDefinition getUpdateCommandDefinition() {
        return updateCommandDefinition;
    }

    /**
     * Returns sensor's properties. Properties are simply string based name-value mappings.
     * Concrete sensor implementations may specify which particular properties they expose.
     * <p>
     * The returned map does not reference this sensor instance and can be modified freely.
     *
     * @return sensor properties or an empty collection
     */
    public Map<String, String> getProperties() {
        HashMap<String, String> props = new HashMap<>(5);
        props.putAll(properties);
        return props;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "ID=" + getSensorID() +
            ", name=" + getName() +
            ", type=" + getType() +
            ", properties=" + properties +
            ", updateCommandDefinition=" + getUpdateCommandDefinition() +
            '}';
    }

}
