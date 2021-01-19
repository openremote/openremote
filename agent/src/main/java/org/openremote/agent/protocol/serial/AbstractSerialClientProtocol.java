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
package org.openremote.agent.protocol.serial;

import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.io.IoAgent;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.model.asset.agent.AgentLink;

/**
 * This is an abstract TCP client protocol for communicating with TCP servers; concrete implementations must provide
 * an {@link IoClient<T> for handling over the wire communication}.
 */
public abstract class AbstractSerialClientProtocol<T extends AbstractIoClientProtocol<T, U, W, X, V>, U extends IoAgent<U, T, V>, V extends AgentLink<?>, W, X extends SerialIoClient<W>> extends AbstractIoClientProtocol<T, U, W, X, V> {

    protected AbstractSerialClientProtocol(U agent) {
        super(agent);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected X doCreateIoClient() throws Exception {

        String port = agent.getSerialPort().orElse(null);
        Integer baudrate = agent.getSerialBaudrate().orElse(null);

        return (X) new SerialIoClient<W>(port, baudrate);
    }
}
