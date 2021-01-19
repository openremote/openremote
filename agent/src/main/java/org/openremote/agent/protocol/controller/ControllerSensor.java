package org.openremote.agent.protocol.controller;

/**
 * ControllerSensor represents a sensor on a device
 */
public class ControllerSensor {
    private String deviceName;
    private String sensorName;

    public ControllerSensor() {

    }

    public ControllerSensor(String deviceName, String sensorName) {
        this.deviceName = deviceName;
        this.sensorName = sensorName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }
}
