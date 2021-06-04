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
package org.openremote.agent.protocol.zwave;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.AttributeDescriptor;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class ZWaveAgent extends Agent<ZWaveAgent, ZWaveProtocol, ZWaveAgent.ZWaveAgentLink> {

    public static class ZWaveAgentLink extends AgentLink<ZWaveAgentLink> {

        protected Integer deviceNodeId;
        protected Integer deviceEndpoint;
        protected String deviceValue;

        // For Hydrators
        protected ZWaveAgentLink() {}

        public ZWaveAgentLink(String id, Integer deviceNodeId, Integer deviceEndpoint, String deviceValue) {
            super(id);
            this.deviceNodeId = deviceNodeId;
            this.deviceEndpoint = deviceEndpoint;
            this.deviceValue = deviceValue;
        }

        public Optional<Integer> getDeviceNodeId() {
            return Optional.ofNullable(deviceNodeId);
        }

        public Optional<Integer> getDeviceEndpoint() {
            return Optional.ofNullable(deviceEndpoint);
        }

        public Optional<String> getDeviceValue() {
            return Optional.ofNullable(deviceValue);
        }
    }

    public static final AttributeDescriptor<String> ZWAVE_SERIAL_PORT = SERIAL_PORT.withOptional(false);

    public static AgentDescriptor<ZWaveAgent, ZWaveProtocol, ZWaveAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        ZWaveAgent.class, ZWaveProtocol.class, ZWaveAgentLink.class, null
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ZWaveAgent() {
    }

    public ZWaveAgent(String name) {
        super(name);
    }

    @Override
    public ZWaveProtocol getProtocolInstance() {
        return new ZWaveProtocol(this);
    }
}
