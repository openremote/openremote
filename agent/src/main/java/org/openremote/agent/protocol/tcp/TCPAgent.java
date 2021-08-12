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
package org.openremote.agent.protocol.tcp;

import org.openremote.agent.protocol.io.IOAgent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.value.AttributeDescriptor;

import javax.persistence.Entity;

@Entity
public class TCPAgent extends IOAgent<TCPAgent, TCPProtocol, DefaultAgentLink> {

    public static final AttributeDescriptor<String> TCP_HOST = HOST.withOptional(false);
    public static final AttributeDescriptor<Integer> TCP_PORT = PORT.withOptional(false);

    public static final AgentDescriptor<TCPAgent, TCPProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        TCPAgent.class, TCPProtocol.class, DefaultAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected TCPAgent() {
    }

    public TCPAgent(String name) {
        super(name);
    }

    @Override
    public TCPProtocol getProtocolInstance() {
        return new TCPProtocol(this);
    }
}
