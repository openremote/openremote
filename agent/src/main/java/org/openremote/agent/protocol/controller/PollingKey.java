package org.openremote.agent.protocol.controller;

import org.openremote.model.attribute.AttributeRef;

import java.util.Objects;

/**
 * Couple used for polling request by device name/defined controller
 * <p>
 * Date : 19-Sep-18
 *
 * @author jerome.vervier
 */
public class PollingKey {
    private String deviceName;
    private AttributeRef controllerAgentRef;

    public PollingKey() {
    }

    public PollingKey(String deviceName, AttributeRef controllerAgentRef) {
        this.deviceName = deviceName;
        this.controllerAgentRef = controllerAgentRef;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public AttributeRef getControllerAgentRef() {
        return controllerAgentRef;
    }

    public void setControllerAgentRef(AttributeRef controllerAgentRef) {
        this.controllerAgentRef = controllerAgentRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PollingKey that = (PollingKey) o;
        return Objects.equals(deviceName, that.deviceName) && Objects.equals(controllerAgentRef, that.controllerAgentRef);
    }

    @Override
    public int hashCode() {

        return Objects.hash(deviceName, controllerAgentRef);
    }
}
