/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.agent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interface for agent connectors that is used to connect and communicate with agents
 * in an agnostic way.
 */
public interface Connector {
    /**
     * Write a value to a specific device resource
     * @param deviceUri
     * @param resourceUri
     * @param value
     * @return
     */
    CompletableFuture<Boolean> writeValue(String deviceUri, String resourceUri, String value);

    /**
     * Async read value from a specific device resource
     * @param deviceUri
     * @param resourceUri
     * @return
     */
    CompletableFuture<String> readValue(String deviceUri, String resourceUri);

    /**
     * Async read values of multiple resources from a specific device
     * @param deviceUri
     * @param resourceUris
     * @return
     */
    CompletableFuture<Map<String,String>> readValues(String deviceUri, String[] resourceUris);

    /**
     * Async availability check (doesn't mean that the agent is connected but it is available
     * @return
     */
    CompletableFuture<Boolean> getAvailability();

    /**
     * Get the current connection state
     * @return
     */
    CompletableFuture<ConnectionState> getConnectionState();

    /**
     * Add a connection state change listener
     * @param callback
     * @return
     */
    long addConnectionListener(Consumer<ConnectionState> callback);

    /**
     * Remove an existing connection listerner
     * @param listenerId
     */
    void removeConnectionListener(long listenerId);

    /**
     * Add a value listener for a specific device resource; requires the resource to actively push
     * value changes; support is agent/protocol/resource dependent
     * @param deviceUri
     * @param resourceUri
     * @param callback
     * @return Handler ID (needed for removing the listener)
     */
    long addValueListener(String deviceUri, String resourceUri, Consumer<String> callback);

    /**
     * Remove an existing value listener
     * @param handerId
     */
    void removeValueListener(long handerId);
}
