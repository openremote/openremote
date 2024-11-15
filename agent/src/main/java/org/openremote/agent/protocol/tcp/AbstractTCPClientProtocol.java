/*
 * Copyright 2017, OpenRemote Inc.
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

import org.openremote.agent.protocol.io.AbstractNettyIOClientProtocol;
import org.openremote.agent.protocol.io.IOAgent;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;

/**
 * This is an abstract TCP client protocol for communicating with TCP servers; concrete implementations must provide
 * an {@link IOClient<T>} for handling over the wire communication.
 */
public abstract class AbstractTCPClientProtocol<T extends AbstractNettyIOClientProtocol<T, U, W, X, V>, U extends IOAgent<U, T, V>, V extends AgentLink<?>, W, X extends TCPIOClient<W>> extends AbstractNettyIOClientProtocol<T, U, W, X, V> {

    protected AbstractTCPClientProtocol(U agent) {
        super(agent);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected X doCreateIoClient() throws Exception {

        String host = agent.getAttributes().getValue(Agent.HOST).orElse(null);
        int port = agent.getAttributes().getValue(Agent.PORT).orElse(0);

        return (X) new TCPIOClient<W>(host, port);
    }
}
