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

import java.util.function.Consumer;

/**
 * Represents an IO client that communicates with a server
 *
 * @param <T> Defines the message type that the instance will encode/decode
 */
public interface IOClient<T> {

    /**
     * Send a message over the wire
     */
    void sendMessage(T message);

    /**
     * Add a consumer of received messages
     */
    void addMessageConsumer(Consumer<T> messageConsumer);

    /**
     * Remove a consumer of received messages
     */
    void removeMessageConsumer(Consumer<T> messageConsumer);

    /**
     * Remove every consumer of received messages
     */
    void removeAllMessageConsumers();

    /**
     * Add a consumer of connection status
     */
    void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer);

    /**
     * Remove a consumer of connection status
     */
    void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer);

    /**
     * Remove every consumer of connection status
     */
    void removeAllConnectionStatusConsumers();

    /**
     * Get current connection status
     */
    ConnectionStatus getConnectionStatus();

    /**
     * Connect to the device
     */
    void connect();

    /**
     * Disconnect from the device
     */
    void disconnect();

    /**
     * Should return a URI that uniquely identifies this client instance
     */
    String getClientUri();
}
