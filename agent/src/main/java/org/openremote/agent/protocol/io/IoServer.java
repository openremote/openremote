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
package org.openremote.agent.protocol.io;

import org.openremote.model.asset.agent.ConnectionStatus;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents an IO server that accepts incoming clients.
 * <p>
 * Implementors are responsible for handling the low level communication and for providing consumers with a mechanism
 * for encoding/decoding messages of type &lt;T&gt;
 *
 * @param <T> Defines the message type that the server can send/receive
 * @param <U> Defines the client identifier type that can be used to uniquely identify clients connected to this server
 */
public interface IoServer<T, U> {

    /**
     * Send a message to a client
     */
    void sendMessage(T message, U client);

    /**
     * Send a message to all clients
     */
    void sendMessage(T message);

    /**
     * Add a consumer of received messages
     */
    void addMessageConsumer(BiConsumer<U, T> messageConsumer);

    /**
     * Remove a consumer of received messages
     */
    void removeMessageConsumer(BiConsumer<U, T> messageConsumer);

    /**
     * Add a consumer of client connection status changes
     */
    void addConnectionStatusConsumer(BiConsumer<U, ConnectionStatus> connectionStatusConsumer);

    /**
     * Remove a consumer of client connection status changes
     */
    void removeConnectionStatusConsumer(BiConsumer<U, ConnectionStatus> connectionStatusConsumer);

    /**
     * Get current connection status of a client (either {@link ConnectionStatus#CONNECTED} or
     * {@link ConnectionStatus#DISCONNECTED})
     */
    ConnectionStatus getConnectionStatus(U client);

    /**
     * Forcibly close the connection to a client
     */
    void disconnectClient(U client);

    /**
     * Start the server
     */
    void start();

    /**
     * Stop the server
     */
    void stop();
}
