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

import org.openremote.agent.protocol.bluetooth.mesh.InternalProvisioningCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.InternalTransportCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.MeshManagerApi;
import org.openremote.agent.protocol.bluetooth.mesh.MeshProvisioningStatusCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Logger;

public class ProvisioningRandomConfirmationState extends ProvisioningState {

    public static final Logger LOG = Logger.getLogger(ProvisioningRandomConfirmationState.class.getName());
    private final UnprovisionedMeshNode mUnprovisionedMeshNode;
    private final MeshProvisioningStatusCallbacks mStatusCallbacks;
    private final InternalProvisioningCallbacks provisioningCallbacks;
    private final InternalTransportCallbacks mInternalTransportCallbacks;

    public ProvisioningRandomConfirmationState(final InternalProvisioningCallbacks callbacks,
                                               final UnprovisionedMeshNode unprovisionedMeshNode,
                                               final InternalTransportCallbacks mInternalTransportCallbacks,
                                               final MeshProvisioningStatusCallbacks meshProvisioningStatusCallbacks) {
        super();
        this.provisioningCallbacks = callbacks;
        this.mUnprovisionedMeshNode = unprovisionedMeshNode;
        this.mInternalTransportCallbacks = mInternalTransportCallbacks;
        this.mStatusCallbacks = meshProvisioningStatusCallbacks;
    }

    @Override
    public State getState() {
        return State.PROVISIONING_RANDOM;
    }

    @Override
    public void executeSend() {
        final byte[] provisionerRandomConfirmationPDU = createProvisionerRandomPDU();
        mStatusCallbacks.onProvisioningStateChanged(mUnprovisionedMeshNode, States.PROVISIONING_CONFIRMATION_SENT, provisionerRandomConfirmationPDU);
        mInternalTransportCallbacks.sendProvisioningPdu(mUnprovisionedMeshNode, provisionerRandomConfirmationPDU);
    }

    @Override
    public boolean parseData(final byte[] data) {
        mStatusCallbacks.onProvisioningStateChanged(mUnprovisionedMeshNode, States.PROVISIONING_RANDOM_RECEIVED, data);
        parseProvisioneeRandom(data);
        return provisioneeMatches();
    }

    private byte[] createProvisionerRandomPDU() {
        final byte[] provisionerRandom = mUnprovisionedMeshNode.getProvisionerRandom();
        final ByteBuffer buffer = ByteBuffer.allocate(provisionerRandom.length + 2);
        buffer.put(new byte[]{MeshManagerApi.PDU_TYPE_PROVISIONING, TYPE_PROVISIONING_RANDOM_CONFIRMATION});
        buffer.put(provisionerRandom);
        final byte[] data = buffer.array();
        LOG.info("Provisioner random PDU: " + MeshParserUtils.bytesToHex(data, false));
        return data;
    }

    private boolean provisioneeMatches() {
        final byte[] provisioneeRandom = mUnprovisionedMeshNode.getProvisioneeRandom();

        final byte[] confirmationInputs = provisioningCallbacks.generateConfirmationInputs(mUnprovisionedMeshNode.getProvisionerPublicKeyXY(),
            mUnprovisionedMeshNode.getProvisioneePublicKeyXY());
        LOG.info("Confirmation inputs: " + MeshParserUtils.bytesToHex(confirmationInputs, false));

        //Generate a confirmation salt of the confirmation inputs
        final byte[] confirmationSalt = SecureUtils.calculateSalt(confirmationInputs);
        LOG.info("Confirmation salt: " + MeshParserUtils.bytesToHex(confirmationSalt, false));

        final byte[] ecdhSecret = mUnprovisionedMeshNode.getSharedECDHSecret();

        //Generate the confirmationKey by calculating the K1 of ECDH, confirmationSalt and ASCII value of "prck".
        final byte[] confirmationKey = SecureUtils.calculateK1(ecdhSecret, confirmationSalt, SecureUtils.PRCK);
        LOG.info("Confirmation key: " + MeshParserUtils.bytesToHex(confirmationKey, false));

        //Generate authentication value from the user input pin
        final byte[] authenticationValue = mUnprovisionedMeshNode.getAuthenticationValue();
        LOG.info("Authentication value: " + MeshParserUtils.bytesToHex(authenticationValue, false));

        ByteBuffer buffer = ByteBuffer.allocate(provisioneeRandom.length + authenticationValue.length);
        buffer.put(provisioneeRandom);
        buffer.put(authenticationValue);
        final byte[] confirmationData = buffer.array();

        final byte[] confirmationValue = SecureUtils.calculateCMAC(confirmationData, confirmationKey);

        if (Arrays.equals(confirmationValue, mUnprovisionedMeshNode.getProvisioneeConfirmation())) {
            LOG.info("Confirmation values match!!!!: " + MeshParserUtils.bytesToHex(confirmationValue, false));
            return true;
        }

        return false;
    }

    private void parseProvisioneeRandom(final byte[] provisioneeRandomPDU) {
        final ByteBuffer buffer = ByteBuffer.allocate(provisioneeRandomPDU.length - 2);
        buffer.put(provisioneeRandomPDU, 2, buffer.limit());
        mUnprovisionedMeshNode.setProvisioneeRandom(buffer.array());
    }
}

