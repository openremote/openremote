package org.openremote.controller.model;

import org.openremote.controller.command.Command;
import org.openremote.controller.command.CommandBuilder;
import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.deploy.CommandDefinition;
import org.openremote.controller.deploy.DeploymentDefinition;
import org.openremote.controller.deploy.SensorDefinition;

import java.util.HashMap;
import java.util.Map;

public class Deployment {

    final protected CommandBuilder commandBuilder;
    final protected Map<String, Device> devices = new HashMap<>();
    final protected Map<Integer, Sensor> sensors = new HashMap<>();

    public Deployment(DeploymentDefinition deploymentDefinition, CommandBuilder commandBuilder) {
        this.commandBuilder = commandBuilder;

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

    public CommandBuilder getCommandBuilder() {
        return commandBuilder;
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

        Command command = commandBuilder.build(sensorDefinition.getCommandDefinition());
        EventProducerCommand sensorCommand;
        if (command instanceof EventProducerCommand) {
            sensorCommand = (EventProducerCommand) command;
        } else {
            throw new IllegalStateException(
                "Sensor must reference EventProducerCommand, not '" + command + "': " + sensorDefinition
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
                sensor = new RangeSensor(sensorDefinition, sensorCommand, Integer.valueOf(minValue), Integer.valueOf(maxValue));
                break;
            case "level":
                sensor = new LevelSensor(sensorDefinition, sensorCommand);
                break;
            case "custom":
                CustomStateSensor.DistinctStates distinctStates = new CustomStateSensor.DistinctStates();
                for (Map.Entry<String, String> entry : sensorDefinition.getProperties().entrySet()) {
                    if (entry.getKey().startsWith("state-")) {
                        distinctStates.addStateMapping(entry.getKey().substring(6), entry.getValue());
                    }
                }
                sensor = new CustomStateSensor(sensorDefinition, sensorCommand, distinctStates, false);
                break;
            case "switch":
                distinctStates = new CustomStateSensor.DistinctStates();
                for (Map.Entry<String, String> entry : sensorDefinition.getProperties().entrySet()) {
                    if (entry.getKey().startsWith("state-")) {
                        distinctStates.addStateMapping(entry.getKey().substring(6), entry.getValue());
                    }
                }
                sensor = new SwitchSensor(sensorDefinition, sensorCommand, distinctStates);
                break;
            default:
                throw new IllegalArgumentException("Unsupported sensor type: " + sensorDefinition);
        }
        return sensor;
    }

}
