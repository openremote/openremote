package org.openremote.controller.deploy;

import java.util.HashMap;
import java.util.Map;

/**
 * The deployment definition model, source for building a {@link Deployment}.
 */
public class DeploymentDefinition {

    protected CommandDefinition[] commandDefinitions = new CommandDefinition[0];
    protected SensorDefinition[] sensorDefinitions = new SensorDefinition[0];
    protected Map<String, String> config = new HashMap<>();

    public DeploymentDefinition() {
    }

    public CommandDefinition[] getCommandDefinitions() {
        return commandDefinitions;
    }

    public void setCommandDefinitions(CommandDefinition[] commandDefinitions) {
        this.commandDefinitions = commandDefinitions;
    }

    public SensorDefinition[] getSensorDefinitions() {
        return sensorDefinitions;
    }

    public void setSensorDefinitions(SensorDefinition[] sensorDefinitions) {
        this.sensorDefinitions = sensorDefinitions;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
