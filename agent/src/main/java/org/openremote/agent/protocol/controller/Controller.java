package org.openremote.agent.protocol.controller;

import org.openremote.model.attribute.AttributeRef;

import java.util.*;
import java.util.stream.Collectors;

import static org.openremote.container.concurrent.GlobalLock.withLock;

/**
 * Controller class represent a Controller defined in a Manager Agent and store all sensors and commands linked to that Controller agent
 */
public class Controller {

    /**
     * A unique ID to use for polling (see Controller 2.5 polling HTTP API)
     */
    public final String DEVICE_ID_BASE = "OR3ControllerProtocol";

    private String controllerConfigName;

    private String deviceId;

    private Map<AttributeRef, ControllerSensor> sensorsList = new HashMap<>();
    private Map<AttributeRef, ControllerCommand> commandsList = new HashMap<>();

    public Controller(String agentId) {
        this.controllerConfigName = agentId;
        this.deviceId = DEVICE_ID_BASE + "_" + agentId;
    }

    public void addSensor(AttributeRef attributeRef, ControllerSensor sensor) {
        this.sensorsList.put(attributeRef, sensor);
    }

    public void addCommand(AttributeRef attributeRef, ControllerCommand command) {
        this.commandsList.put(attributeRef, command);
    }

    public ControllerCommand getCommand(AttributeRef attributeRef) {
        return this.commandsList.get(attributeRef);
    }

    public Set<Map.Entry<AttributeRef, ControllerSensor>> getSensorsListForDevice(String deviceName) {
        return this.sensorsList.entrySet().stream().filter(entry -> entry.getValue().getDeviceName().equals(deviceName)).collect(Collectors.toSet());
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getControllerConfigName() {
        return this.controllerConfigName;
    }

    public void removeAttributeRef(AttributeRef attributeRef) {
        withLock(ControllerProtocol.PROTOCOL_DISPLAY_NAME + ":Controller::removeAttributeRef", () -> {
            this.commandsList.remove(attributeRef);
            this.sensorsList.remove(attributeRef);
        });
    }

    /**
     * Collect every sensorName linked to a deviceName. Look into {@link #sensorsList}
     */
    public List<String> collectSensorNameLinkedToDeviceName(String deviceName) {
        return sensorsList.values()
                .stream()
                .filter(sensor -> sensor.getDeviceName().equals(deviceName)).map(ControllerSensor::getSensorName)
                .distinct()
                .collect(Collectors.toList());
    }
}
