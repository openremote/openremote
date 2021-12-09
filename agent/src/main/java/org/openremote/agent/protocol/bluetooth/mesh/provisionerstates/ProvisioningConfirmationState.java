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
import org.openremote.agent.protocol.bluetooth.mesh.utils.InputOOBAction;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.OutputOOBAction;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class ProvisioningConfirmationState extends ProvisioningState {

    public static final Logger LOG = Logger.getLogger(ProvisioningConfirmationState.class.getName());
    private static final byte[] NO_OOB_AUTH = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static final int AUTH_VALUE_LENGTH = 16;
    private final InternalProvisioningCallbacks provisioningCallbacks;
    private final UnprovisionedMeshNode mNode;
    private final MeshProvisioningStatusCallbacks mStatusCallbacks;
    private final InternalTransportCallbacks mInternalTransportCallbacks;
    private String authentication;
    private byte[] authenticationValue;

    public ProvisioningConfirmationState(final InternalProvisioningCallbacks callbacks,
                                         final UnprovisionedMeshNode node,
                                         final InternalTransportCallbacks internalTransportCallbacks,
                                         final MeshProvisioningStatusCallbacks provisioningStatusCallbacks) {
        super();
        this.provisioningCallbacks = callbacks;
        this.mNode = node;
        this.mInternalTransportCallbacks = internalTransportCallbacks;
        this.mStatusCallbacks = provisioningStatusCallbacks;
    }

    /**
     * Sets the provisioning confirmation
     *
     * @param authentication authentication value
     */
    public void setProvisioningAuthentication(final String authentication) {
        this.authentication = authentication;
    }

    @Override
    public State getState() {
        return State.PROVISIONING_CONFIRMATION;
    }

    @Override
    public void executeSend() {

        final byte[] provisioningConfirmationPDU = createProvisioningConfirmation();
        mStatusCallbacks.onProvisioningStateChanged(mNode, States.PROVISIONING_CONFIRMATION_SENT, provisioningConfirmationPDU);
        mInternalTransportCallbacks.sendProvisioningPdu(mNode, provisioningConfirmationPDU);
    }

    @Override
    public boolean parseData(final byte[] data) {
        mStatusCallbacks.onProvisioningStateChanged(mNode, States.PROVISIONING_CONFIRMATION_RECEIVED, data);
        parseProvisioneeConfirmation(data);
        return true;
    }

    private byte[] createProvisioningConfirmation() {

        final byte[] confirmationInputs = provisioningCallbacks.generateConfirmationInputs(mNode.getProvisionerPublicKeyXY(), mNode.getProvisioneePublicKeyXY());
        LOG.info("Confirmation inputs: " + MeshParserUtils.bytesToHex(confirmationInputs, false));

        //Generate a confirmation salt of the confirmation inputs
        final byte[] confirmationSalt = SecureUtils.calculateSalt(confirmationInputs);
        LOG.info("Confirmation salt: " + MeshParserUtils.bytesToHex(confirmationSalt, false));

        final byte[] ecdhSecret = mNode.getSharedECDHSecret();

        //Generate the confirmationKey by calculating the K1 of ECDH, confirmationSalt and ASCII value of "prck".
        final byte[] confirmationKey = SecureUtils.calculateK1(ecdhSecret, confirmationSalt, SecureUtils.PRCK);
        LOG.info("Confirmation key: " + MeshParserUtils.bytesToHex(confirmationKey, false));

        //Generate provisioner random number
        final byte[] provisionerRandom = SecureUtils.generateRandomNumber();
        mNode.setProvisionerRandom(provisionerRandom);
        LOG.info("Provisioner random: " + MeshParserUtils.bytesToHex(provisionerRandom, false));

        //Generate authentication value from the user input authentication
        final byte[] authenticationValue = generateAuthenticationValue();
        mNode.setAuthenticationValue(authenticationValue);
        LOG.info("Authentication value: " + MeshParserUtils.bytesToHex(authenticationValue, false));

        ByteBuffer buffer = ByteBuffer.allocate(provisionerRandom.length + 16);
        buffer.put(provisionerRandom);
        buffer.put(authenticationValue);
        final byte[] confirmationData = buffer.array();

        final byte[] confirmationValue = SecureUtils.calculateCMAC(confirmationData, confirmationKey);

        buffer = ByteBuffer.allocate(confirmationValue.length + 2);
        buffer.put(new byte[]{MeshManagerApi.PDU_TYPE_PROVISIONING, TYPE_PROVISIONING_CONFIRMATION});
        buffer.put(confirmationValue);
        final byte[] provisioningConfirmationPDU = buffer.array();
        LOG.info("Provisioning confirmation: " + MeshParserUtils.bytesToHex(provisioningConfirmationPDU, false));

        return provisioningConfirmationPDU;
    }

    private byte[] generateAuthenticationValue() {
        switch (mNode.authMethodUsed) {
            case NO_OOB_AUTHENTICATION:
                return NO_OOB_AUTH;
            case STATIC_OOB_AUTHENTICATION:
                return MeshParserUtils.toByteArray(authentication);
            case OUTPUT_OOB_AUTHENTICATION:
                final OutputOOBAction action = OutputOOBAction.fromValue(mNode.getAuthActionUsed());
                return OutputOOBAction.generateOutputOOBAuthenticationValue(action, authentication);
            case INPUT_OOB_AUTHENTICATION:
                final InputOOBAction inputOOBAction = InputOOBAction.fromValue(mNode.getAuthActionUsed());
                return InputOOBAction.generateInputOOBAuthenticationValue(inputOOBAction, mNode.getInputAuthentication());
        }
        return null;
    }

    private void parseProvisioneeConfirmation(final byte[] provisioneeConfirmation) {
        final ByteBuffer buffer = ByteBuffer.allocate(provisioneeConfirmation.length - 2);
        buffer.put(provisioneeConfirmation, 2, buffer.limit());
        mNode.setProvisioneeConfirmation(buffer.array());
    }
}

