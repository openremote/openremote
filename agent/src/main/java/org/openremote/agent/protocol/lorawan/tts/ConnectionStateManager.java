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

public class ConnectionStateManager implements ConnectionState {
    private final TheThingsStackProtocol protocol;
    private final StartState startState;
    private final StopState stopState;
    private final ConnectingGrpcState connectingGrpcState;
    private final ConnectingMqttState connectingMqttState;
    private final ConnectedState connectedState;
    private final ErrorState errorState;
    private volatile ConnectionState state;

    public ConnectionStateManager(TheThingsStackProtocol protocol) {
        this.protocol = protocol;

        this.startState = new StartState(this);
        this.stopState = new StopState(this);
        this.connectingGrpcState = new ConnectingGrpcState(this);
        this.connectingMqttState = new ConnectingMqttState(this);
        this.connectedState = new ConnectedState(this);
        this.errorState = new ErrorState(this);

        this.state = this.startState;
    }

    @Override
    public synchronized void start(Container container) throws Exception {
        state.start(container);
    }

    @Override
    public synchronized void stop(Container container) {
        state.stop(container);
    }

    @Override
    public synchronized void onMqttConnectionStatusChanged(ConnectionStatus connectionStatus) {
        state.onMqttConnectionStatusChanged(connectionStatus);
    }

    @Override
    public synchronized void onGrpcConnectionStatusChanged(ConnectionStatus connectionStatus) {
        state.onGrpcConnectionStatusChanged(connectionStatus);
    }

    public synchronized void init() {
        this.state = getStartState();
    }

    StartState getStartState() {
        return startState;
    }

    StopState getStopState() {
        return stopState;
    }

    ConnectingGrpcState getConnectingGrpcState() {
        return connectingGrpcState;
    }

    ConnectingMqttState getConnectingMqttState() {
        return connectingMqttState;
    }

    ConnectedState getConnectedState() {
        return connectedState;
    }

    ErrorState getErrorState() {
        return errorState;
    }

    void setState(ConnectionState state) {
        this.state = state;
    }

    TheThingsStackProtocol getProtocol() {
        return this.protocol;
    }
}
