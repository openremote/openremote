package org.openremote.controller.deploy;

/**
 * The deployment model.
 */
public interface DeploymentDefinition {

    CommandDefinition[] getCommandDefinitions();

    SensorDefinition[] getSensorDefinitions();

}
