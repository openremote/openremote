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
package org.openremote.agent.protocol.simulator;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.value.AttributeDescriptor;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class SimulatorAgent extends Agent<SimulatorAgent, SimulatorProtocol, SimulatorAgent.SimulatorAgentLink> {

    public static class SimulatorAgentLink extends AgentLink<SimulatorAgentLink> {

        @JsonPropertyDescription("Used to store 24h dataset of values that should be replayed (i.e. written to the" +
            " linked attribute) in a continuous loop.")
        protected SimulatorReplayDatapoint[] replayData;

        // For Hydrators
        protected SimulatorAgentLink() {}

        public SimulatorAgentLink(String id) {
            super(id);
        }

        public Optional<SimulatorReplayDatapoint[]> getReplayData() {
            return Optional.ofNullable(replayData);
        }

        public SimulatorAgentLink setReplayData(SimulatorReplayDatapoint[] replayData) {
            this.replayData = replayData;
            return this;
        }
    }

    public static final AttributeDescriptor<String> SERIAL_SERIAL_PORT = SERIAL_PORT.withOptional(false);

    public static final AgentDescriptor<SimulatorAgent, SimulatorProtocol, SimulatorAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        SimulatorAgent.class, SimulatorProtocol.class, SimulatorAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected SimulatorAgent() {
    }

    public SimulatorAgent(String name) {
        super(name);
    }

    @Override
    public SimulatorProtocol getProtocolInstance() {
        return new SimulatorProtocol(this);
    }
}
