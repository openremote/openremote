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
package org.openremote.agent.controller2;

import org.openremote.agent.ConnectionState;
import org.openremote.agent.Connector;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Controller2Connector implements Connector {
    @Override
    public CompletableFuture<Boolean> writeValue(String deviceUri, String resourceUri, String value) {
        return null;
    }

    @Override
    public CompletableFuture<String> readValue(String deviceUri, String resourceUri) {
        return null;
    }

    @Override
    public CompletableFuture<Map<String, String>> readValues(String deviceUri, String[] resourceUris) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> getAvailability() {
        return null;
    }

    @Override
    public CompletableFuture<ConnectionState> getConnectionState() {
        return null;
    }

    @Override
    public long addConnectionListener(Consumer<ConnectionState> callback) {
        return 0;
    }

    @Override
    public void removeConnectionListener(long listenerId) {

    }

    @Override
    public long addValueListener(String deviceUri, String resourceUri, Consumer<String> callback) {
        return 0;
    }

    @Override
    public void removeValueListener(long handlerId) {

    }
}
