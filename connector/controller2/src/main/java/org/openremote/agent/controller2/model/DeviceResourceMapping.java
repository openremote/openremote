/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.controller2.model;

import org.openremote.entities.controller.Command;
import org.openremote.entities.controller.Sensor;
import org.openremote.manager.shared.device.DeviceResource;

/**
 * Contains the data to allow mapping between native command/sensor API and the component resource API
 */
public class DeviceResourceMapping {

    final protected SensorPropertyChangeListener sensorPropertyChangeListener = new SensorPropertyChangeListener();

    protected DeviceResource resource;
    protected Sensor gatewaySensor;
    protected Command sendCommand1;
    protected Command sendCommand2;

    public void addSensorListener(SensorListener listener) {
        sensorPropertyChangeListener.addListener(listener);
    }

    public void removeSensorListener(SensorListener listener) {
        sensorPropertyChangeListener.removeListener(listener);
    }

    public DeviceResource getResource() {
        return resource;
    }

    public void setResource(DeviceResource resource) {
        this.resource = resource;
    }

    public Sensor getGatewaySensor() {
        return gatewaySensor;
    }

    public void setGatewaySensor(Sensor gatewaySensor) {
        // Yeah, this is just fantastic... another level of callbacks and indirection
        gatewaySensor.addPropertyChangeListener(sensorPropertyChangeListener);
        this.gatewaySensor = gatewaySensor;
    }

    public Command getSendCommand1() {
        return sendCommand1;
    }

    public void setSendCommand1(Command sendCommand1) {
        this.sendCommand1 = sendCommand1;
    }

    public Command getSendCommand2() {
        return sendCommand2;
    }

    public void setSendCommand2(Command sendCommand2) {
        this.sendCommand2 = sendCommand2;
    }
}
