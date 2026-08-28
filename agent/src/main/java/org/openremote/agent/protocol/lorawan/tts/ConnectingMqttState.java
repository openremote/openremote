/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.lorawan.tts;

import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;

public class ConnectingMqttState implements ConnectionState {
  private final ConnectionStateManager stateManager;
  private final TheThingsStackProtocol protocol;

  public ConnectingMqttState(ConnectionStateManager manager) {
    this.stateManager = manager;
    this.protocol = manager.getProtocol();
  }

  @Override
  public void start(Container container) throws Exception {}

  @Override
  public void stop(Container container) {
    protocol.stopMqtt(container);
    stateManager.setState(stateManager.getStopState());
  }

  @Override
  public void onMqttConnectionStatusChanged(ConnectionStatus connectionStatus) {
    if (ConnectionStatus.CONNECTED != connectionStatus) {
      protocol.setConnectionStatus(connectionStatus);
    }

    if (ConnectionStatus.CONNECTED == connectionStatus) {
      protocol.startSync();
      stateManager.setState(stateManager.getConnectingGrpcState());
    } else if (ConnectionStatus.ERROR == connectionStatus) {
      stateManager.setState(stateManager.getErrorState());
    }
  }

  @Override
  public void onGrpcConnectionStatusChanged(ConnectionStatus connectionStatus) {}
}
