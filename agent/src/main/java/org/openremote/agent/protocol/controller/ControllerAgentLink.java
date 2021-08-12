/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.controller;

import org.openremote.model.asset.agent.AgentLink;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ControllerAgentLink extends AgentLink<ControllerAgentLink> {

    protected String deviceName;
    protected String sensorName;
    protected String commandDeviceName;
    protected String commandName;
    protected Map<String, List<String>> commandsMap;

    // For Hydrators
    protected ControllerAgentLink() {
    }

    public ControllerAgentLink(String id, String deviceName) {
        super(id);
        this.deviceName = deviceName;
    }

    public Optional<String> getDeviceName() {
        return Optional.ofNullable(deviceName);
    }

    public ControllerAgentLink setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public Optional<String> getSensorName() {
        return Optional.ofNullable(sensorName);
    }

    public ControllerAgentLink setSensorName(String sensorName) {
        this.sensorName = sensorName;
        return this;
    }

    public Optional<String> getCommandDeviceName() {
        return Optional.ofNullable(commandDeviceName);
    }

    public ControllerAgentLink setCommandDeviceName(String commandDeviceName) {
        this.commandDeviceName = commandDeviceName;
        return this;
    }

    public Optional<String> getCommandName() {
        return Optional.ofNullable(commandName);
    }

    public ControllerAgentLink setCommandName(String commandName) {
        this.commandName = commandName;
        return this;
    }

    public Optional<Map<String, List<String>>> getCommandsMap() {
        return Optional.ofNullable(commandsMap);
    }

    public ControllerAgentLink setCommandsMap(Map<String, List<String>> commandsMap) {
        this.commandsMap = commandsMap;
        return this;
    }
}
