/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.transport.BaseMeshMessageHandler;
import org.openremote.agent.protocol.bluetooth.mesh.transport.NetworkLayerCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.transport.UpperTransportLayerCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ExtendedInvalidCipherTextException;

/** MeshMessageHandler class for handling mesh */
final class MeshMessageHandler extends BaseMeshMessageHandler {

  /**
   * Constructs MeshMessageHandler
   *
   * @param internalTransportCallbacks {@link InternalTransportCallbacks} Callbacks
   * @param networkLayerCallbacks {@link NetworkLayerCallbacks} network layer callbacks
   * @param upperTransportLayerCallbacks {@link UpperTransportLayerCallbacks} upper transport layer
   *     callbacks
   */
  MeshMessageHandler(
      final InternalTransportCallbacks internalTransportCallbacks,
      final NetworkLayerCallbacks networkLayerCallbacks,
      final UpperTransportLayerCallbacks upperTransportLayerCallbacks) {
    super(internalTransportCallbacks, networkLayerCallbacks, upperTransportLayerCallbacks);
  }

  @Override
  protected final synchronized void setMeshStatusCallbacks(
      final MeshStatusCallbacks statusCallbacks) {
    mStatusCallbacks = statusCallbacks;
  }

  @Override
  protected final synchronized void parseMeshPduNotifications(
      final byte[] pdu, final MeshNetwork network) throws ExtendedInvalidCipherTextException {
    super.parseMeshPduNotifications(pdu, network);
  }
}
