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
import org.openremote.agent.protocol.bluetooth.mesh.utils.AlgorithmType;
import org.openremote.agent.protocol.bluetooth.mesh.utils.AuthenticationOOBMethods;
import org.openremote.agent.protocol.bluetooth.mesh.utils.InputOOBAction;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.OutputOOBAction;
import org.openremote.agent.protocol.bluetooth.mesh.utils.StaticOOBType;

import java.util.logging.Logger;

public class ProvisioningStartState extends ProvisioningState {

    public static final Logger LOG = Logger.getLogger(ProvisioningStartState.class.getName());
    private final UnprovisionedMeshNode mNode;
    private final MeshProvisioningStatusCallbacks mMeshProvisioningStatusCallbacks;
    private final InternalTransportCallbacks mInternalTransportCallbacks;

    private int numberOfElements;
    private int algorithm;
    private int publicKeyType;
    private int staticOOBType;
    private int outputOOBSize;
    private int outputOOBAction;
    private int inputOOBSize;
    private int inputOOBAction;
    private short outputActionType;
    private short inputActionType;

    public ProvisioningStartState(final UnprovisionedMeshNode node,
                                  final InternalTransportCallbacks internalTransportCallbacks,
                                  final MeshProvisioningStatusCallbacks provisioningStatusCallbacks) {
        super();
        this.mNode = node;
        this.mInternalTransportCallbacks = internalTransportCallbacks;
        this.mMeshProvisioningStatusCallbacks = provisioningStatusCallbacks;
    }

    public void setUseStaticOOB(final StaticOOBType actionType) {
        mNode.setAuthMethodUsed(AuthenticationOOBMethods.STATIC_OOB_AUTHENTICATION);
        mNode.setAuthActionUsed(actionType.getStaticOobType());
    }

    public void setUseOutputOOB(final OutputOOBAction actionType) {
        mNode.setAuthMethodUsed(AuthenticationOOBMethods.OUTPUT_OOB_AUTHENTICATION);
        this.outputActionType = actionType.getOutputOOBAction();
        mNode.setAuthActionUsed(actionType.getOutputOOBAction());
    }

    public void setUseInputOOB(final InputOOBAction actionType) {
        mNode.setAuthMethodUsed(AuthenticationOOBMethods.INPUT_OOB_AUTHENTICATION);
        this.inputActionType = actionType.getInputOOBAction();
        mNode.setAuthActionUsed(actionType.getInputOOBAction());
    }

    @Override
    public State getState() {
        return State.PROVISIONING_START;
    }

    @Override
    public void executeSend() {
        final byte[] provisioningStartPDU = createProvisioningStartPDU();
        //We store the provisioning start pdu to be used when generating confirmation inputs
        mNode.setProvisioningStartPdu(provisioningStartPDU);
        mMeshProvisioningStatusCallbacks.onProvisioningStateChanged(mNode, States.PROVISIONING_START, provisioningStartPDU);
        mInternalTransportCallbacks.sendProvisioningPdu(mNode, provisioningStartPDU);
    }

    @Override
    public boolean parseData(final byte[] data) {
        return true;
    }

    private byte[] createProvisioningStartPDU() {
        final byte[] provisioningPDU = new byte[7];
        provisioningPDU[0] = MeshManagerApi.PDU_TYPE_PROVISIONING;
        provisioningPDU[1] = TYPE_PROVISIONING_START;
        provisioningPDU[2] = AlgorithmType.getAlgorithmValue((short) algorithm);
        provisioningPDU[3] = 0; // (byte) publicKeyType;
        provisioningPDU[4] = (byte) mNode.getAuthMethodUsed().ordinal();
        switch (mNode.getAuthMethodUsed()) {
            case NO_OOB_AUTHENTICATION:
                provisioningPDU[5] = 0;
                provisioningPDU[6] = 0;
                break;
            case STATIC_OOB_AUTHENTICATION:
                provisioningPDU[5] = 0;
                provisioningPDU[6] = 0;
                break;
            case OUTPUT_OOB_AUTHENTICATION:
                provisioningPDU[5] = (byte) OutputOOBAction.getOutputOOBActionValue(this.outputActionType);
                provisioningPDU[6] = (byte) outputOOBSize;
                break;
            case INPUT_OOB_AUTHENTICATION:
                provisioningPDU[5] = (byte) InputOOBAction.getInputOOBActionValue(this.inputActionType);
                provisioningPDU[6] = (byte) inputOOBSize;
                mNode.setInputAuthentication(InputOOBAction.getInputOOOBAuthenticationValue(inputActionType, (byte) inputOOBSize));
                break;

        }
        LOG.info("Provisioning start PDU: " + MeshParserUtils.bytesToHex(provisioningPDU, true));

        return provisioningPDU;
    }

    public void setProvisioningCapabilities(final int numberOfElements,
                                            final int algorithm,
                                            final int publicKeyType,
                                            final int staticOOBType,
                                            final int outputOOBSize,
                                            final int outputOOBAction,
                                            final int inputOOBSize,
                                            final int inputOOBAction) {
        this.numberOfElements = numberOfElements;
        this.algorithm = algorithm;
        this.publicKeyType = publicKeyType;
        this.staticOOBType = staticOOBType;
        this.outputOOBSize = outputOOBSize;
        this.outputOOBAction = outputOOBAction;
        this.inputOOBSize = inputOOBSize;
        this.inputOOBAction = inputOOBAction;
    }
}

