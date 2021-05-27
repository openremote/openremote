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

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class ZWaveAgent extends Agent<ZWaveAgent, ZWaveProtocol, ZWaveAgent.ZWAgentLink> {

    public static class ZWAgentLink extends AgentLink<ZWAgentLink> {

        protected Integer deviceNodeId;
        protected Integer deviceEndpoint;
        protected String deviceValue;

        // For Hydrators
        protected ZWAgentLink() {}

        public ZWAgentLink(String id, Integer deviceNodeId, Integer deviceEndpoint, String deviceValue) {
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

    public static AgentDescriptor<ZWaveAgent, ZWaveProtocol, ZWAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        ZWaveAgent.class, ZWaveProtocol.class, ZWAgentLink.class, null
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
