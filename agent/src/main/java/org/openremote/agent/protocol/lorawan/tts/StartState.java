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

public class StartState implements ConnectionState {
    private final ConnectionStateManager stateManager;
    private final TheThingsStackProtocol protocol;

    public StartState(ConnectionStateManager manager) {
        this.stateManager = manager;
        this.protocol = manager.getProtocol();
    }

    @Override
    public void start(Container container) throws Exception {
        try {
            protocol.startMqtt(container);
            stateManager.setState(stateManager.getConnectingMqttState());
        } catch (Exception e) {
            protocol.setConnectionStatus(ConnectionStatus.ERROR);
            stateManager.setState(stateManager.getErrorState());
            throw e;
        }
    }

    @Override
    public void stop(Container container) {

    }

    @Override
    public void onMqttConnectionStatusChanged(ConnectionStatus connectionStatus) {

    }

    @Override
    public void onGrpcConnectionStatusChanged(ConnectionStatus connectionStatus) {

    }
}
