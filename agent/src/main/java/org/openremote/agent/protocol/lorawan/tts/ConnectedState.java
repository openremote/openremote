/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.lorawan.tts;

import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;

public class ConnectedState implements ConnectionState{
    private final ConnectionStateManager stateManager;
    private final TheThingsStackProtocol protocol;

    public ConnectedState(ConnectionStateManager manager) {
        this.stateManager = manager;
        this.protocol = manager.getProtocol();
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) {
        protocol.stopSync();
        protocol.stopMqtt(container);
        stateManager.setState(stateManager.getStopState());
    }

    @Override
    public void onMqttConnectionStatusChanged(ConnectionStatus connectionStatus) {
        if (ConnectionStatus.CONNECTED != connectionStatus) {
            protocol.stopSync();
            stateManager.setState(stateManager.getConnectingMqttState());
            stateManager.onMqttConnectionStatusChanged(connectionStatus);
        }
    }

    @Override
    public void onGrpcConnectionStatusChanged(ConnectionStatus connectionStatus) {
        if (ConnectionStatus.CONNECTED != connectionStatus) {
            stateManager.setState(stateManager.getConnectingGrpcState());
            stateManager.onGrpcConnectionStatusChanged(connectionStatus);
        }
    }
}
