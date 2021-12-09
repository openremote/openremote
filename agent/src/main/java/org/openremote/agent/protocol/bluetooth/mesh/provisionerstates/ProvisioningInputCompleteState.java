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
package org.openremote.agent.protocol.bluetooth.mesh.provisionerstates;

import org.openremote.agent.protocol.bluetooth.mesh.InternalTransportCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.MeshManagerApi;
import org.openremote.agent.protocol.bluetooth.mesh.MeshProvisioningStatusCallbacks;

public class ProvisioningInputCompleteState extends ProvisioningState {

    private final UnprovisionedMeshNode mNode;
    private final InternalTransportCallbacks mInternalTransportCallbacks;
    private final MeshProvisioningStatusCallbacks mMeshProvisioningStatusCallbacks;

    /**
     * Constructs the provisioning input complete state
     *
     * @param node                        {@link UnprovisionedMeshNode} node
     * @param internalTransportCallbacks  {@link InternalTransportCallbacks} callbacks
     * @param provisioningStatusCallbacks {@link MeshProvisioningStatusCallbacks} callbacks
     */
    public ProvisioningInputCompleteState(final UnprovisionedMeshNode node,
                                          final InternalTransportCallbacks internalTransportCallbacks,
                                          final MeshProvisioningStatusCallbacks provisioningStatusCallbacks) {
        super();
        this.mNode = node;
        this.mInternalTransportCallbacks = internalTransportCallbacks;
        this.mMeshProvisioningStatusCallbacks = provisioningStatusCallbacks;
    }

    @Override
    public State getState() {
        return State.PROVISIONING_INPUT_COMPLETE;
    }

    @Override
    public void executeSend() {
        //Do nothing here
    }

    @Override
    public boolean parseData(final byte[] data) {
        if (data.length == 2 &&
            data[0] == MeshManagerApi.PDU_TYPE_PROVISIONING &&
            data[1] == TYPE_PROVISIONING_INPUT_COMPLETE) {
            mMeshProvisioningStatusCallbacks.onProvisioningStateChanged(mNode, States.PROVISIONING_AUTHENTICATION_INPUT_ENTERED, null);
            return true;
        }
        return false;
    }
}
