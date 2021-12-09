/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.ProvisioningState;
import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.UnprovisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;

/**
 * Callbacks to notify status during the provisioning process
 */
public interface MeshProvisioningStatusCallbacks {

    /**
     * Invoked when the provisioning state changed.
     *
     * @param meshNode {@link UnprovisionedMeshNode} unprovisioned node.
     * @param state    {@link ProvisioningState.State} each provisioning state.
     * @param data     data that was sent or received during each provisioning state.
     */
    void onProvisioningStateChanged(final UnprovisionedMeshNode meshNode, final ProvisioningState.States state, final byte[] data);

    /**
     * Invoked when the provisioning state changed.
     *
     * @param meshNode {@link UnprovisionedMeshNode} unprovisioned node.
     * @param state    {@link ProvisioningState.State} each provisioning state.
     * @param data     data that was sent or received during each provisioning state.
     */
    void onProvisioningFailed(final UnprovisionedMeshNode meshNode, final ProvisioningState.States state, final byte[] data);

    /**
     * Invoked when the provisioning state changed.
     *
     * @param meshNode {@link ProvisionedMeshNode} provisioned mesh node.
     * @param state    {@link ProvisioningState.State} each provisioning state.
     * @param data     data that was sent or received during each provisioning state.
     */
    void onProvisioningCompleted(final ProvisionedMeshNode meshNode, final ProvisioningState.States state, final byte[] data);

}

