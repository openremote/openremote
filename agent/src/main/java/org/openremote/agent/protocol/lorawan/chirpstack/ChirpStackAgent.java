/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.lorawan.chirpstack;

import jakarta.persistence.Entity;
import org.openremote.agent.protocol.lorawan.LoRaWANAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.asset.agent.AgentDescriptor;

@Entity
public class ChirpStackAgent extends LoRaWANAgent<ChirpStackAgent, ChirpStackProtocol> {

    public static final AgentDescriptor<ChirpStackAgent, ChirpStackProtocol, MQTTAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        ChirpStackAgent.class, ChirpStackProtocol.class, MQTTAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ChirpStackAgent() {
    }

    public ChirpStackAgent(String name) {
        super(name);
    }

    @Override
    public ChirpStackProtocol getProtocolInstance() {
        return new ChirpStackProtocol(this);
    }
}
