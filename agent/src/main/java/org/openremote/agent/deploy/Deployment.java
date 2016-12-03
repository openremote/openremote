package org.openremote.agent.deploy;

import org.openremote.agent.command.Command;
import org.openremote.agent.command.CommandBuilder;
import org.openremote.agent.command.Commands;
import org.openremote.agent.command.SensorUpdateCommand;
import org.openremote.agent.context.SensorStateHandler;
import org.openremote.agent.rules.RuleEngine;
import org.openremote.agent.rules.RulesProvider;
import org.openremote.agent.sensor.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates:
 * <pre>
 * - General configuration properties
 * - How protocol-specific commands are build
 * - How sensor state is handled
 * - Which rules process sensor state updates
 * - All known sensors, commands and their devices
 * </pre>
 */
public class Deployment {

    final protected Map<String, String> config;
    final protected CommandBuilder commandBuilder;
    final protected Commands commands;
    final protected SensorStateHandler sensorStateHandler;
    final protected RulesProvider rulesProvider;
    final protected RuleEngine ruleEngine;
    final protected Map<String, Device> devices = new ConcurrentHashMap<>();
    final protected Map<Integer, Sensor> sensors = new ConcurrentHashMap<>();

    public Deployment(DeploymentDefinition deploymentDefinition,
                      CommandBuilder commandBuilder,
                      SensorStateHandler sensorStateHandler,
                      RulesProvider rulesProvider) {

        this.config = deploymentDefinition.getConfig();
        this.commandBuilder = commandBuilder;
        this.commands = new Commands(this);
        this.sensorStateHandler = sensorStateHandler;
        this.rulesProvider = rulesProvider;
        this.ruleEngine = new RuleEngine();

        for (SensorDefinition sensorDefinition : deploymentDefinition.getSensorDefinitions()) {
            Sensor sensor = buildSensor(sensorDefinition);
            sensors.put(sensor.getSensorDefinition().getSensorID(), sensor);
        }

        for (CommandDefinition commandDefinition : deploymentDefinition.getCommandDefinitions()) {
            Device device = devices.get(commandDefinition.getDeviceName());
            if (device == null) {
                device = new Device(commandDefinition.getDeviceID(), commandDefinition.getDeviceName());
                devices.put(device.getName(), device);
            } else if (device.getDeviceID() != commandDefinition.getDeviceID()) {
                throw new IllegalStateException(
                    "Device '" + device.getName() + "' with ID " + device.getDeviceID() + " conflicts with: " + commandDefinition
                );
            }
            device.addCommandDefinition(commandDefinition);
        }
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public CommandBuilder getCommandBuilder() {
        return commandBuilder;
    }

    public Commands getCommands() {
        return commands;
    }

    public SensorStateHandler getSensorStateHandler() {
        return sensorStateHandler;
    }

    public RulesProvider getRulesProvider() {
        return rulesProvider;
    }

    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    public CommandDefinition getCommandDefinition(int commandID) {
        for (Device device : devices.values()) {
            CommandDefinition commandDefinition = device.getCommandDefinition(commandID);
            if (commandDefinition != null)
                return commandDefinition;
        }
        return null;
    }

    public CommandDefinition getCommandDefinition(String deviceName, String commandName) {
        if (deviceName == null || commandName == null || deviceName.length() == 0 || commandName.length() == 0) {
            return null;
        }
        Device device = devices.get(deviceName);
        return device != null ? device.getCommandDefinition(commandName) : null;
    }

    public CommandDefinition getCommandDefinition(String commandName) {
        if (commandName == null || commandName.length() == 0) {
            return null;
        }
        for (Device device : devices.values()) {
            CommandDefinition commandDefinition = device.getCommandDefinition(commandName);
            if (commandDefinition != null)
                return commandDefinition;
        }
        return null;
    }

    public Device getDevice(String deviceName) {
        return devices.get(deviceName);
    }

    public Device[] getDevices() {
        return devices.values().toArray(new Device[devices.values().size()]);
    }

    public Sensor[] getSensors() {
        return sensors.values().toArray(new Sensor[sensors.values().size()]);
    }

    public Sensor getSensor(int sensorID) {
        return sensors.get(sensorID);
    }

    public Sensor getSensor(String sensorName) {
        for (Sensor sensor : sensors.values()) {
            if (sensor.getSensorDefinition().getName().equals(sensorName))
                return sensor;
        }
        return null;
    }

    public Integer getSensorID(String sensorName) {
        Sensor sensor = getSensor(sensorName);
        return sensor != null ? sensor.getSensorDefinition().getSensorID() : null;
    }

    protected Sensor buildSensor(SensorDefinition sensorDefinition) {
        Sensor sensor;

        Command command = commandBuilder.build(sensorDefinition.getUpdateCommandDefinition());
        SensorUpdateCommand updateCommand;
        if (command instanceof SensorUpdateCommand) {
            updateCommand = (SensorUpdateCommand) command;
        } else {
            throw new IllegalStateException(
                "Sensor must reference " + SensorUpdateCommand.class.getSimpleName() + ", not '" + command + "': " + sensorDefinition
            );
        }

        switch (sensorDefinition.getType()) {
            case "range":
                String minValue = sensorDefinition.getProperties().get("range-min");
                if (minValue == null)
                    throw new IllegalStateException("Sensor of range type must have min/max value defined: " + sensorDefinition);
                String maxValue = sensorDefinition.getProperties().get("range-max");
                if (maxValue == null)
                    throw new IllegalStateException("Sensor of range type must have min/max value defined: " + sensorDefinition);
                sensor = new RangeSensor(sensorDefinition, updateCommand, Integer.valueOf(minValue), Integer.valueOf(maxValue));
                break;
            case "level":
                sensor = new LevelSensor(sensorDefinition, updateCommand);
                break;
            case "custom":
                CustomSensor.DistinctStates distinctStates = new CustomSensor.DistinctStates();
                for (Map.Entry<String, String> entry : sensorDefinition.getProperties().entrySet()) {
                    if (entry.getKey().startsWith("state-")) {
                        distinctStates.addStateMapping(entry.getKey().substring(6), entry.getValue());
                    }
                }
                sensor = new CustomSensor(sensorDefinition, updateCommand, distinctStates, false);
                break;
            case "switch":
                distinctStates = new CustomSensor.DistinctStates();
                for (Map.Entry<String, String> entry : sensorDefinition.getProperties().entrySet()) {
                    if (entry.getKey().startsWith("state-")) {
                        distinctStates.addStateMapping(entry.getKey().substring(6), entry.getValue());
                    }
                }
                sensor = new SwitchSensor(sensorDefinition, updateCommand, distinctStates);
                break;
            default:
                throw new IllegalArgumentException("Unsupported sensor type: " + sensorDefinition);
        }
        return sensor;
    }

}
