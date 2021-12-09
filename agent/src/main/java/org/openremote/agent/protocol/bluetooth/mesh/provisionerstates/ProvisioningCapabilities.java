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

import org.openremote.agent.protocol.bluetooth.mesh.utils.AlgorithmType;
import org.openremote.agent.protocol.bluetooth.mesh.utils.AuthenticationOOBMethods;
import org.openremote.agent.protocol.bluetooth.mesh.utils.InputOOBAction;
import org.openremote.agent.protocol.bluetooth.mesh.utils.OutputOOBAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class ProvisioningCapabilities {
    private static final int PUBLIC_KEY_INFORMATION_AVAILABLE = 0x01;
    private static final int STATIC_OOB_INFO_AVAILABLE = 0x01;


    private static final Logger LOG = java.util.logging.Logger.getLogger(ProvisioningCapabilities.class.getName());    
    private byte numberOfElements;
    private short rawAlgorithm;
    private List<AlgorithmType> supportedAlgorithmTypes;
    private byte rawPublicKeyType;
    private boolean publicKeyInformationAvailable;
    private byte rawStaticOOBType;
    private boolean staticOOBInformationAvailable;
    private byte outputOOBSize;
    private short rawOutputOOBAction;
    private List<OutputOOBAction> supportedOutputOOBActions;
    private byte inputOOBSize;
    private short rawInputOOBAction;
    private List<InputOOBAction> supportedInputOOBActions;
    private AuthenticationOOBMethods supportedOOBMethods;
    private final List<AuthenticationOOBMethods> availableOOBTypes = new ArrayList<>();

    /**
     * Constructs the provisioning capabilities received from a mesh node
     *
     * @param capabilities capabilities pdu
     */
    ProvisioningCapabilities(final byte[] capabilities) {
        if (capabilities[2] == 0) {
            throw new IllegalArgumentException("Number of elements cannot be zero");
        }

        final byte numberOfElements = (capabilities[2]);
        this.numberOfElements = numberOfElements;
        LOG.info("Number of elements: " + numberOfElements);

        final short algorithm = (short) (((capabilities[3] & 0xff) << 8) | (capabilities[4] & 0xff));
        this.rawAlgorithm = algorithm;
        this.supportedAlgorithmTypes = AlgorithmType.getAlgorithmTypeFromBitMask(algorithm);

        this.rawPublicKeyType = capabilities[5];
        this.publicKeyInformationAvailable = rawPublicKeyType == PUBLIC_KEY_INFORMATION_AVAILABLE;
        LOG.info("Public key information available: " + publicKeyInformationAvailable);

        this.rawStaticOOBType = capabilities[6];
        this.staticOOBInformationAvailable = rawStaticOOBType == STATIC_OOB_INFO_AVAILABLE;
        LOG.info("Static OOB information available: : " + staticOOBInformationAvailable);

        final byte outputOOBSize = capabilities[7];
        this.outputOOBSize = outputOOBSize;
        LOG.info("Output OOB size: " + outputOOBSize);

        final short outputOOBAction = (short) (((capabilities[8] & 0xff) << 8) | (capabilities[9] & 0xff));
        this.rawOutputOOBAction = outputOOBAction;
        this.supportedOutputOOBActions = outputOOBSize == 0 ? new ArrayList<>() : OutputOOBAction.parseOutputActionsFromBitMask(outputOOBAction);

        final byte inputOOBSize = capabilities[10];
        this.inputOOBSize = inputOOBSize;
        LOG.info("Input OOB size: " + inputOOBSize);

        final short inputOOBAction = (short) (((capabilities[11] & 0xff) << 8) | (capabilities[12] & 0xff));
        this.rawInputOOBAction = inputOOBAction;
        this.supportedInputOOBActions = inputOOBSize == 0 ? new ArrayList<>() : InputOOBAction.parseInputActionsFromBitMask(inputOOBAction);
        generateAvailableOOBTypes();
    }

    private void generateAvailableOOBTypes() {
        availableOOBTypes.clear();
        availableOOBTypes.add(AuthenticationOOBMethods.NO_OOB_AUTHENTICATION);
        if (isStaticOOBInformationAvailable()) {
            availableOOBTypes.add(AuthenticationOOBMethods.STATIC_OOB_AUTHENTICATION);
        }

        if (!supportedOutputOOBActions.isEmpty()) {
            availableOOBTypes.add(AuthenticationOOBMethods.OUTPUT_OOB_AUTHENTICATION);
        }
        if (!supportedInputOOBActions.isEmpty()) {
            availableOOBTypes.add(AuthenticationOOBMethods.INPUT_OOB_AUTHENTICATION);
        }
    }

    /**
     * Returns the number of elements in the mesh node
     */
    public byte getNumberOfElements() {
        return numberOfElements;
    }

    /**
     * Sets the number of elements in the node
     */
    void setNumberOfElements(final byte numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    /**
     * Returns the raw supported algorithm value received by the node
     */
    public short getRawAlgorithm() {
        return rawAlgorithm;
    }

    void setRawAlgorithm(final short algorithm) {
        this.rawAlgorithm = algorithm;
    }

    /**
     * Returns a list of algorithm types supported by the node
     */
    public List<AlgorithmType> getSupportedAlgorithmTypes() {
        return Collections.unmodifiableList(supportedAlgorithmTypes);
    }

    /**
     * Returns the raw public key type received by the node
     */
    public byte getRawPublicKeyType() {
        return rawPublicKeyType;
    }

    void setRawPublicKeyType(final byte rawPublicKeyType) {
        this.rawPublicKeyType = rawPublicKeyType;
    }

    /**
     * Returns true if public key information is available
     */
    public boolean isPublicKeyInformationAvailable() {
        return publicKeyInformationAvailable;
    }

    /**
     * Returns the raw static OOB type received by the node
     */
    public byte getRawStaticOOBType() {
        return rawStaticOOBType;
    }

    void setRawStaticOOBType(final byte rawStaticOOBType) {
        this.rawStaticOOBType = rawStaticOOBType;
    }

    /**
     * Returns true if Static OOB information is available
     */
    public boolean isStaticOOBInformationAvailable() {
        return staticOOBInformationAvailable;
    }

    /**
     * Returns the output oob size received by the node. This is the length of
     */
    public byte getOutputOOBSize() {
        return outputOOBSize;
    }

    void setOutputOOBSize(final byte outputOOBSize) {
        this.outputOOBSize = outputOOBSize;
    }

    /**
     * Returns the raw output oob action value received by the node
     */
    public short getRawOutputOOBAction() {
        return rawOutputOOBAction;
    }

    void setRawOutputOOBAction(final short rawOutputOOBAction) {
        this.rawOutputOOBAction = rawOutputOOBAction;
    }

    /**
     * Returns the list of supported {@link OutputOOBAction} actions or an empty list if no oob is supported
     */
    public List<OutputOOBAction> getSupportedOutputOOBActions() {
        return Collections.unmodifiableList(supportedOutputOOBActions);
    }

    public byte getInputOOBSize() {
        return inputOOBSize;
    }

    /**
     * Returns the raw input oob size value received by the node
     */
    void setInputOOBSize(final byte inputOOBSize) {
        this.inputOOBSize = inputOOBSize;
    }

    /**
     * Returns the raw input oob action value received by the node
     */
    public short getRawInputOOBAction() {
        return rawInputOOBAction;
    }

    void setRawInputOOBAction(final short rawInputOOBAction) {
        this.rawInputOOBAction = rawInputOOBAction;
    }

    /**
     * Returns the list of supported {@link InputOOBAction} actions or an empty list if no oob is supported
     */
    public List<InputOOBAction> getSupportedInputOOBActions() {
        return Collections.unmodifiableList(supportedInputOOBActions);
    }

    /**
     * Returns a list of available OOB methods that can be used during provisioning
     */
    public List<AuthenticationOOBMethods> getAvailableOOBTypes() {
        return Collections.unmodifiableList(availableOOBTypes);
    }
}
