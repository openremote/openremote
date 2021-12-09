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

import java.util.logging.Logger;

public class ProvisioningInviteState extends ProvisioningState {

    public static final Logger LOG = Logger.getLogger(ProvisioningInviteState.class.getName());
    private final UnprovisionedMeshNode mUnprovisionedMeshNode;
    private final int attentionTimer;
    private final MeshProvisioningStatusCallbacks mStatusCallbacks;
    private final InternalTransportCallbacks mInternalTransportCallbacks;

    public ProvisioningInviteState(final UnprovisionedMeshNode unprovisionedMeshNode, final int attentionTimer, final InternalTransportCallbacks mInternalTransportCallbacks, final MeshProvisioningStatusCallbacks meshProvisioningStatusCallbacks) {
        super();
        this.mUnprovisionedMeshNode = unprovisionedMeshNode;
        this.attentionTimer = attentionTimer;
        this.mStatusCallbacks = meshProvisioningStatusCallbacks;
        this.mInternalTransportCallbacks = mInternalTransportCallbacks;
    }

    @Override
    public State getState() {
        return State.PROVISIONING_INVITE;
    }

    @Override
    public void executeSend() {
        final byte[] invitePDU = createInvitePDU();
        //We store the provisioning invite pdu to be used when generating confirmation inputs
        mUnprovisionedMeshNode.setProvisioningInvitePdu(invitePDU);
        mStatusCallbacks.onProvisioningStateChanged(mUnprovisionedMeshNode, States.PROVISIONING_INVITE, invitePDU);
        mInternalTransportCallbacks.sendProvisioningPdu(mUnprovisionedMeshNode, invitePDU);
    }

    @Override
    public boolean parseData(final byte[] data) {
        return true;
    }

    /**
     * Generates the invitePDU for provisioning based on the attention timer provided by the user.
     */
    private byte[] createInvitePDU() {

        final byte[] data = new byte[3];
        data[0] = MeshManagerApi.PDU_TYPE_PROVISIONING; //Provisioning Opcode;
        //noinspection ConstantConditions
        data[1] = TYPE_PROVISIONING_INVITE; //PDU type in
        data[2] = (byte) attentionTimer;
        return data;
    }
}

