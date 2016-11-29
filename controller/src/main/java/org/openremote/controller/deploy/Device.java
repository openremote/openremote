package org.openremote.controller.deploy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A device is simply a collection of command definitions.
 */
public class Device {

    final protected int deviceID;
    final protected String name;
    final protected Map<Integer, CommandDefinition> commandDefinitions = new ConcurrentHashMap<>();

    public Device(int deviceID, String name) {
        this.deviceID = deviceID;
        this.name = name;
    }

    public int getDeviceID() {
        return deviceID;
    }

    public String getName() {
        return name;
    }

    public CommandDefinition getCommandDefinition(int commandID) {
        return commandDefinitions.get(commandID);
    }

    public CommandDefinition getCommandDefinition(String commandName) {
        for (CommandDefinition commandDefinition : commandDefinitions.values()) {
            if (commandDefinition.getName().equals(commandName))
                return commandDefinition;
        }
        return null;
    }

    public void addCommandDefinition(CommandDefinition commandDefinition) {
        commandDefinitions.put(commandDefinition.getCommandID(), commandDefinition);
    }
}
