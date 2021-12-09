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
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class ProvisioningDataState extends ProvisioningState {

    public static final Logger LOG = Logger.getLogger(ProvisioningDataState.class.getName());
    private final UnprovisionedMeshNode mUnprovisionedMeshNode;
    private final MeshProvisioningStatusCallbacks mStatusCallbacks;
    private final InternalProvisioningCallbacks provisioningCallbacks;
    private final InternalTransportCallbacks mInternalTransportCallbacks;

    public ProvisioningDataState(final InternalProvisioningCallbacks callbacks,
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
        return State.PROVISIONING_DATA;
    }

    @Override
    public void executeSend() {
        sendProvisioningData();
    }

    @Override
    public boolean parseData(final byte[] data) {
        return true;
    }

    private void sendProvisioningData() {
        final byte[] provisioningDataPDU = createProvisioningDataPDU();
        mStatusCallbacks.onProvisioningStateChanged(mUnprovisionedMeshNode, States.PROVISIONING_DATA_SENT, provisioningDataPDU);
        mInternalTransportCallbacks.sendProvisioningPdu(mUnprovisionedMeshNode, provisioningDataPDU);
    }

    private byte[] createProvisioningDataPDU() {

        final byte[] provisioningSalt = generateProvisioningSalt();
        LOG.info("Provisioning salt: " + MeshParserUtils.bytesToHex(provisioningSalt, false));

        final byte[] ecdh = mUnprovisionedMeshNode.getSharedECDHSecret();

        final byte[] t = SecureUtils.calculateCMAC(ecdh, provisioningSalt);
        /* Calculating the session key */
        final byte[] sessionKey = SecureUtils.calculateCMAC(SecureUtils.PRSK, t);
        LOG.info("Session key: " + MeshParserUtils.bytesToHex(sessionKey, false));

        /* Calculate the Session nonce */
        final byte[] sessionNonce = generateSessionNonce(ecdh, provisioningSalt);
        LOG.info("Session nonce: " + MeshParserUtils.bytesToHex(sessionNonce, false));

        /* Calculate the Device key */
        final byte[] deviceKey = SecureUtils.calculateCMAC(SecureUtils.PRDK, t);
        LOG.info("Device key: " + MeshParserUtils.bytesToHex(deviceKey, false));
        mUnprovisionedMeshNode.setDeviceKey(deviceKey);

        /* Generate 16 byte Random network key */
        final byte[] networkKey = mUnprovisionedMeshNode.getNetworkKey();
        LOG.info("Network key: " + MeshParserUtils.bytesToHex(networkKey, false));

        /* Generate random 2 byte Key index*/
        final byte[] keyIndex = MeshParserUtils.addKeyIndexPadding(mUnprovisionedMeshNode.getKeyIndex());
        LOG.info("Key index: " + MeshParserUtils.bytesToHex(keyIndex, false));

        /* Generate random 1 byte Flags */
        byte[] flags = mUnprovisionedMeshNode.getFlags();
        LOG.info("Flags: " + MeshParserUtils.bytesToHex(flags, false));

        /* Generate random 4 byte IV Index */
        final byte[] ivIndex = mUnprovisionedMeshNode.getIvIndex();
        LOG.info("IV index: " + MeshParserUtils.bytesToHex(ivIndex, false));

        /* Generate random 2 byte unicast address*/
        final byte[] unicastAddress = MeshAddress.addressIntToBytes(mUnprovisionedMeshNode.getUnicastAddress());

        LOG.info("Unicast address: " + MeshParserUtils.bytesToHex(unicastAddress, false));
        ByteBuffer buffer = ByteBuffer.allocate(networkKey.length + keyIndex.length + flags.length + ivIndex.length + unicastAddress.length);
        buffer.put(networkKey);
        buffer.put(keyIndex);
        buffer.put(flags);
        buffer.put(ivIndex);
        buffer.put(unicastAddress);

        final byte[] provisioningData = buffer.array();
        LOG.info("Provisioning data: " + MeshParserUtils.bytesToHex(provisioningData, false));

        final byte[] encryptedProvisioningData = SecureUtils.encryptCCM(provisioningData, sessionKey, sessionNonce, 8);
        LOG.info("Encrypted provisioning data: " + MeshParserUtils.bytesToHex(encryptedProvisioningData, false));

        buffer = ByteBuffer.allocate(2 + encryptedProvisioningData.length);
        buffer.put(MeshManagerApi.PDU_TYPE_PROVISIONING);
        buffer.put(TYPE_PROVISIONING_DATA);
        buffer.put(encryptedProvisioningData);

        final byte[] provisioningPDU = buffer.array();
        LOG.info("Prov Data: " + MeshParserUtils.bytesToHex(provisioningPDU, false));
        return provisioningPDU;
    }

    /**
     * Generate the provisioning salt.
     * This is done by calculating the salt containing array created by appending the confirmationSalt, provisionerRandom and the provisioneeRandom.
     *
     * @return a byte array
     */
    private byte[] generateProvisioningSalt() {

        final byte[] confirmationSalt = SecureUtils.calculateSalt(provisioningCallbacks.generateConfirmationInputs(mUnprovisionedMeshNode.getProvisionerPublicKeyXY(), mUnprovisionedMeshNode.getProvisioneePublicKeyXY()));
        final byte[] provisionerRandom = mUnprovisionedMeshNode.getProvisionerRandom();
        final byte[] provisioneeRandom = mUnprovisionedMeshNode.getProvisioneeRandom();

        final ByteBuffer buffer = ByteBuffer.allocate(confirmationSalt.length + provisionerRandom.length + provisioneeRandom.length);
        buffer.put(confirmationSalt);
        buffer.put(provisionerRandom);
        buffer.put(provisioneeRandom);

        /* After appending calculate the salt */
        return SecureUtils.calculateSalt(buffer.array());
    }

    /**
     * Calculate the Session nonce
     *
     * @param ecdh             shared ECDH secret
     * @param provisioningSalt provisioning salt
     * @return sessionNonce
     */
    private byte[] generateSessionNonce(final byte[] ecdh, final byte[] provisioningSalt) {
        final byte[] nonce = SecureUtils.calculateK1(ecdh, provisioningSalt, SecureUtils.PRSN);
        final ByteBuffer buffer = ByteBuffer.allocate(nonce.length - 3);
        buffer.put(nonce, 3, buffer.limit());
        return buffer.array();
    }
}

