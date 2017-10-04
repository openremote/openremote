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
package org.openremote.agent.protocol;

import org.openremote.model.asset.agent.ConnectionStatus;

import java.util.function.Consumer;

/**
 * This is an abstraction for encoding/decoding messages for communication 'over the wire' in a generic way.
 * <p>
 * Implementors are responsible for handling the low level communication and for providing consumers with a mechanism
 * for encoding/decoding messages of type &lt;T&gt;
 *
 * @param <T> Defines the message type that the instance will encode/decode
 */
public interface MessageProcessor<T> {

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
     * Add a consumer of connection status
     */
    void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer);

    /**
     * Remove a consumer of connection status
     */
    void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer);

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
}
