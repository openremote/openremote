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
package org.openremote.agent.protocol.io;

import org.openremote.model.asset.agent.ConnectionStatus;

import java.util.function.Consumer;

public class ProtocolIOClient<W, X extends IOClient<W>> {
    public X ioClient;
    public Consumer<ConnectionStatus> connectionStatusConsumer;
    public Consumer<W> messageConsumer;

    public ProtocolIOClient(X ioClient, Consumer<ConnectionStatus> connectionStatusConsumer, Consumer<W> messageConsumer) {
        this.ioClient = ioClient;
        this.connectionStatusConsumer = connectionStatusConsumer;
        this.messageConsumer = messageConsumer;
    }

    public void connect() {
        ioClient.addConnectionStatusConsumer(status -> {
            if (connectionStatusConsumer != null) {
                connectionStatusConsumer.accept(status);
            }
        });

        ioClient.addMessageConsumer(msg -> {
            if (messageConsumer != null) {
                messageConsumer.accept(msg);
            }
        });

        AbstractIOClientProtocol.LOG.info("Connecting IO client");
        ioClient.connect();
    }

    public void disconnect() {
        ioClient.removeAllMessageConsumers();
        ioClient.removeAllConnectionStatusConsumers();
        AbstractIOClientProtocol.LOG.info("Disconnecting IO client");
        ioClient.disconnect();
    }

    public synchronized void send(W message) {
        AbstractIOClientProtocol.LOG.fine("Sending message to IO client: " + ioClient.getClientUri());
        ioClient.sendMessage(message);
    }
}
