/*
 * Copyright 2020, OpenRemote Inc.
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

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Entity
public class ControllerAgent extends Agent<ControllerAgent, ControllerProtocol, ControllerAgent.ControllerAgentLink> {

    public static class ControllerAgentLink extends AgentLink<ControllerAgentLink> {

        protected String deviceName;
        protected String sensorName;
        protected String commandDeviceName;
        protected String commandName;
        protected Map<String, List<String>> commandsMap;

        // For Hydrators
        protected ControllerAgentLink() {}

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

    public static final AttributeDescriptor<String> CONTROLLER_URI = new AttributeDescriptor<>("controllerURI", ValueType.TEXT);

    public static final AgentDescriptor<ControllerAgent, ControllerProtocol, ControllerAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        ControllerAgent.class, ControllerProtocol.class, ControllerAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ControllerAgent() {
        this(null);
    }

    public ControllerAgent(String name) {
        super(name);
    }

    @Override
    public ControllerProtocol getProtocolInstance() {
        return new ControllerProtocol(this);
    }

    public Optional<String> getControllerURI() {
        return getAttributes().getValue(CONTROLLER_URI);
    }

    public ControllerAgent setControllerURI(String uri) {
        getAttributes().getOrCreate(CONTROLLER_URI).setValue(uri);
        return this;
    }
}
