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
package org.openremote.agent.protocol.bluetooth.mesh.provisionerstates;

import java.util.logging.Logger;

import org.openremote.agent.protocol.bluetooth.mesh.MeshProvisioningStatusCallbacks;

public class ProvisioningCapabilitiesState extends ProvisioningState {
  public static final Logger LOG = Logger.getLogger(ProvisioningCapabilitiesState.class.getName());

  private final UnprovisionedMeshNode mUnprovisionedMeshNode;
  private final MeshProvisioningStatusCallbacks mCallbacks;

  private ProvisioningCapabilities capabilities;

  public ProvisioningCapabilitiesState(
      final UnprovisionedMeshNode unprovisionedMeshNode,
      final MeshProvisioningStatusCallbacks callbacks) {
    super();
    this.mCallbacks = callbacks;
    this.mUnprovisionedMeshNode = unprovisionedMeshNode;
  }

  @Override
  public State getState() {
    return State.PROVISIONING_CAPABILITIES;
  }

  public ProvisioningCapabilities getCapabilities() {
    return capabilities;
  }

  @Override
  public void executeSend() {}

  @Override
  public boolean parseData(final byte[] data) {
    final boolean flag = parseProvisioningCapabilities(data);
    // We store the provisioning capabilities pdu to be used when generating confirmation inputs
    mUnprovisionedMeshNode.setProvisioningCapabilitiesPdu(data);
    mUnprovisionedMeshNode.setProvisioningCapabilities(capabilities);
    mCallbacks.onProvisioningStateChanged(
        mUnprovisionedMeshNode, States.PROVISIONING_CAPABILITIES, data);
    return flag;
  }

  private boolean parseProvisioningCapabilities(final byte[] provisioningCapabilities) {
    this.capabilities = new ProvisioningCapabilities(provisioningCapabilities);
    return true;
  }
}
