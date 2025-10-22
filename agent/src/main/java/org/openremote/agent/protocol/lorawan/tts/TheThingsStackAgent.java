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
package org.openremote.agent.protocol.lorawan.tts;

import jakarta.persistence.Entity;
import org.openremote.agent.protocol.lorawan.LoRaWANAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class TheThingsStackAgent extends LoRaWANAgent<TheThingsStackAgent, TheThingsStackProtocol> {

    public static final AttributeDescriptor<String> HOST = Agent.HOST.withOptional(false);
    public static final AttributeDescriptor<Integer> PORT = Agent.PORT.withOptional(false);
    public static final AttributeDescriptor<String> TENANT_ID = new AttributeDescriptor<>("tenantId", ValueType.TEXT);

    public static final AgentDescriptor<TheThingsStackAgent, TheThingsStackProtocol, MQTTAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        TheThingsStackAgent.class, TheThingsStackProtocol.class, MQTTAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected TheThingsStackAgent() {
    }

    public TheThingsStackAgent(String name) {
        super(name);
    }

    @Override
    public TheThingsStackProtocol getProtocolInstance() {
        return new TheThingsStackProtocol(this);
    }

    public Optional<String> getTenantId() {
        return getAttributes().getValue(TENANT_ID);
    }

    public TheThingsStackAgent setTenantId(String tenantId) {
        getAttributes().getOrCreate(TENANT_ID).setValue(tenantId);
        return this;
    }
}
