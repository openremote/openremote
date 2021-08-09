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
package org.openremote.agent.protocol.udp;

import org.openremote.agent.protocol.io.IOAgent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.value.AttributeDescriptor;

import javax.persistence.Entity;

@Entity
public class UDPAgent extends IOAgent<UDPAgent, UDPProtocol, DefaultAgentLink> {

    public static final AttributeDescriptor<String> UDP_HOST = HOST.withOptional(false);
    public static final AttributeDescriptor<Integer> UDP_PORT = PORT.withOptional(false);

    public static final AgentDescriptor<UDPAgent, UDPProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        UDPAgent.class, UDPProtocol.class, DefaultAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected UDPAgent() {
    }

    public UDPAgent(String name) {
        super(name);
    }

    @Override
    public UDPProtocol getProtocolInstance() {
        return new UDPProtocol(this);
    }
}
