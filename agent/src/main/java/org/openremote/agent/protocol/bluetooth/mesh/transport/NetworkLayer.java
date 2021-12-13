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
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.openremote.agent.protocol.bluetooth.mesh.MeshManagerApi;
import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.Provisioner;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ExtendedInvalidCipherTextException;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * NetworkLayer implementation of the mesh network architecture as per the mesh profile specification.
 * <p>
 * NetworkLayer encrypts/decrypts mesh messages to be sent or received by the nodes in a mesh network.
 * <p/>
 */
abstract class NetworkLayer extends LowerTransportLayer {

    public static final Logger LOG = Logger.getLogger(NetworkLayer.class.getName());

    NetworkLayerCallbacks mNetworkLayerCallbacks;
    private Map<Integer, byte[]> segmentedAccessMessagesMessages;
    private Map<Integer, byte[]> segmentedControlMessagesMessages;

    /**
     * Set network layer callbacks
     *
     * @param callbacks {@link NetworkLayerCallbacks} callbacks
     */
    abstract void setNetworkLayerCallbacks(final NetworkLayerCallbacks callbacks);

    /**
     * Creates a mesh message
     *
     * @param message Message could be of type access or control message.
     */
    protected final void createMeshMessage(final Message message) {
        if (message instanceof AccessMessage) {
            super.createMeshMessage(message);
        } else {
            super.createMeshMessage(message);
        }
        createNetworkLayerPDU(message);
    }

    /**
     * Creates a vendor model mesh message
     *
     * @param message Message could be of type access or control message.
     */
    protected final void createVendorMeshMessage(final Message message) {
        if (message instanceof AccessMessage) {
            super.createVendorMeshMessage(message);
        } else {
            super.createVendorMeshMessage(message);
        }
        createNetworkLayerPDU(message);
    }

    @Override
    public synchronized final Message createNetworkLayerPDU(final Message message) {
        final SecureUtils.K2Output k2Output = getK2Output(message);
        final int nid = k2Output.getNid();
        final byte[] encryptionKey = k2Output.getEncryptionKey();
        LOG.info("Encryption key: " + MeshParserUtils.bytesToHex(encryptionKey, false));

        final byte[] privacyKey = k2Output.getPrivacyKey();
        LOG.info("Privacy key: " + MeshParserUtils.bytesToHex(privacyKey, false));
        final int ctl = message.getCtl();
        final int ttl = message.getTtl();
        final int ivi = message.getIvIndex()[3] & 0x01; // least significant bit of IV Index
        final byte iviNID = (byte) ((ivi << 7) | nid);
        final byte ctlTTL = (byte) ((ctl << 7) | ttl);

        final int src = message.getSrc();
        final Map<Integer, byte[]> lowerTransportPduMap;
        final Map<Integer, byte[]> encryptedPduPayload = new HashMap<>();
        final List<byte[]> sequenceNumbers = new ArrayList<>();

        final ProvisionedMeshNode node = mUpperTransportLayerCallbacks.getNode(message.getSrc());

        final int pduType = message.getPduType();
        switch (message.getPduType()) {
            case MeshManagerApi.PDU_TYPE_NETWORK:
                if (message instanceof AccessMessage) {
                    lowerTransportPduMap = ((AccessMessage) message).getLowerTransportAccessPdu();
                } else {
                    lowerTransportPduMap = ((ControlMessage) message).getLowerTransportControlPdu();
                }
                for (int i = 0; i < lowerTransportPduMap.size(); i++) {
                    final byte[] lowerTransportPdu = lowerTransportPduMap.get(i);
                    if (i != 0) {
                        node.setSequenceNumber(MeshParserUtils.convert24BitsToInt(message.getSequenceNumber()));
                        final byte[] sequenceNumber = MeshParserUtils.getSequenceNumberBytes(node.incrementSequenceNumber());
                        message.setSequenceNumber(sequenceNumber);
                    }
                    sequenceNumbers.add(message.getSequenceNumber());
                    LOG.info("Sequence Number: " + MeshParserUtils.bytesToHex(sequenceNumbers.get(i), false));
                    final byte[] nonce = createNetworkNonce(ctlTTL, sequenceNumbers.get(i), src, message.getIvIndex());
                    final byte[] encryptedPayload = encryptPdu(lowerTransportPdu, encryptionKey, nonce, message.getDst(), SecureUtils.getNetMicLength(message.getCtl()));
                    encryptedPduPayload.put(i, encryptedPayload);
                    LOG.info("Encrypted Network payload: " + MeshParserUtils.bytesToHex(encryptedPayload, false));
                }
                break;
            case MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION:
                lowerTransportPduMap = ((ControlMessage) message).getLowerTransportControlPdu();
                for (int i = 0; i < lowerTransportPduMap.size(); i++) {
                    final byte[] lowerTransportPdu = lowerTransportPduMap.get(i);
                    final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(node.incrementSequenceNumber());
                    message.setSequenceNumber(sequenceNum);
                    sequenceNumbers.add(message.getSequenceNumber());
                    final byte[] nonce = createProxyNonce(message.getSequenceNumber(), src, message.getIvIndex());
                    final byte[] encryptedPayload = encryptPdu(lowerTransportPdu, encryptionKey, nonce, message.getDst(), SecureUtils.getNetMicLength(message.getCtl()));
                    encryptedPduPayload.put(i, encryptedPayload);
                    LOG.info("Encrypted Network payload: " + MeshParserUtils.bytesToHex(encryptedPayload, false));
                }
                break;
        }

        final Map<Integer, byte[]> pduArray = new HashMap<>();
        for (int i = 0; i < encryptedPduPayload.size(); i++) {
            //Create the privacy random
            final byte[] encryptedPayload = encryptedPduPayload.get(i);
            final byte[] privacyRandom = createPrivacyRandom(encryptedPayload);
            //Next we create the PECB
            final byte[] pecb = createPECB(message.getIvIndex(), privacyRandom, privacyKey);

            final byte[] header = obfuscateNetworkHeader(ctlTTL, sequenceNumbers.get(i), src, pecb);
            final byte[] pdu = ByteBuffer.allocate(1 + 1 + header.length + encryptedPayload.length).order(ByteOrder.BIG_ENDIAN)
                .put((byte) pduType)
                .put(iviNID)
                .put(header)
                .put(encryptedPayload)
                .array();
            pduArray.put(i, pdu);
            message.setNetworkLayerPdu(pduArray);
        }

        return message;
    }

    final synchronized Message createRetransmitNetworkLayerPDU(final Message message, final int segment) {
        final SecureUtils.K2Output k2Output = getK2Output(message);
        final int nid = k2Output.getNid();
        final byte[] encryptionKey = k2Output.getEncryptionKey();
        LOG.info("Encryption key: " + MeshParserUtils.bytesToHex(encryptionKey, false));

        final byte[] privacyKey = k2Output.getPrivacyKey();
        LOG.info("Privacy key: " + MeshParserUtils.bytesToHex(privacyKey, false));
        final int ctl = message.getCtl();
        final int ttl = message.getTtl();
        final int ivi = message.getIvIndex()[3] & 0x01; // least significant bit of IV Index
        final byte iviNID = (byte) ((ivi << 7) | nid);
        final byte ctlTTL = (byte) ((ctl << 7) | ttl);

        final int src = message.getSrc();
        final Map<Integer, byte[]> lowerTransportPduMap;
        if (message instanceof AccessMessage) {
            lowerTransportPduMap = ((AccessMessage) message).getLowerTransportAccessPdu();
        } else {
            lowerTransportPduMap = ((ControlMessage) message).getLowerTransportControlPdu();
        }

        byte[] encryptedNetworkPayload = null;
        final int pduType = message.getPduType();
        if (message.getPduType() == MeshManagerApi.PDU_TYPE_NETWORK) {
            final ProvisionedMeshNode node = mUpperTransportLayerCallbacks.getNode(message.getSrc());
            final byte[] lowerTransportPdu = lowerTransportPduMap.get(segment);
            node.setSequenceNumber(MeshParserUtils.convert24BitsToInt(message.getSequenceNumber()));
            //final int sequenceNumber = node.incrementSequenceNumber();//incrementSequenceNumber(mNetworkLayerCallbacks.getProvisioner(), message.getSequenceNumber());
            final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(node.incrementSequenceNumber());
            message.setSequenceNumber(sequenceNum);

            LOG.info("Sequence Number: " + MeshParserUtils.bytesToHex(sequenceNum, false));

            final byte[] nonce = createNetworkNonce(ctlTTL, sequenceNum, src, message.getIvIndex());
            encryptedNetworkPayload = encryptPdu(lowerTransportPdu, encryptionKey, nonce, message.getDst(), SecureUtils.getNetMicLength(message.getCtl()));
            if (encryptedNetworkPayload == null)
                return null;
            LOG.info("Encrypted Network payload: " + MeshParserUtils.bytesToHex(encryptedNetworkPayload, false));
        }

        final Map<Integer, byte[]> pduArray = new HashMap<>();
        if (encryptedNetworkPayload == null)
            return null;

        final byte[] privacyRandom = createPrivacyRandom(encryptedNetworkPayload);
        //Next we create the PECB
        final byte[] pecb = createPECB(message.getIvIndex(), privacyRandom, privacyKey);

        final byte[] header = obfuscateNetworkHeader(ctlTTL, message.getSequenceNumber(), src, pecb);
        final byte[] pdu = ByteBuffer.allocate(1 + 1 + header.length + encryptedNetworkPayload.length).order(ByteOrder.BIG_ENDIAN)
            .put((byte) pduType)
            .put(iviNID)
            .put(header)
            .put(encryptedNetworkPayload)
            .array();
        pduArray.put(segment, pdu);
        message.setNetworkLayerPdu(pduArray);
        return message;
    }

    /**
     * Parse received mesh message
     * <p>
     * This method will drop messages with an invalid sequence number as all mesh messages are supposed to have a sequence
     * </p>
     *
     * @param key                     Network Key used to decrypt
     * @param node                    Mesh node.
     * @param data                    PDU received from the mesh node.
     * @param networkHeader           Network header.
     * @param decryptedNetworkPayload Decrypted network payload.
     * @param ivIndex                 IV Index of the network.
     * @return complete {@link Message} that was successfully parsed or null otherwise.
     */
    final synchronized Message parseMeshMessage(final NetworkKey key,
                                   final ProvisionedMeshNode node,
                                   final byte[] data,
                                   final byte[] networkHeader,
                                   final byte[] decryptedNetworkPayload,
                                   final int ivIndex,
                                   final byte[] sequenceNumber) throws ExtendedInvalidCipherTextException {
        mMeshNode = node;
        final Provisioner provisioner = mNetworkLayerCallbacks.getProvisioner();
        final int ctlTtl = networkHeader[0];
        final int ctl = (ctlTtl >> 7) & 0x01;
        final int ttl = ctlTtl & 0x7F;
        // LOG.info("TTL for received message: " + ttl);
        final int src = MeshParserUtils.unsignedBytesToInt(networkHeader[5], networkHeader[4]);
        if (ctl == 1) {
            return parseControlMessage(key, provisioner.getProvisionerAddress(), data, networkHeader, decryptedNetworkPayload, src, sequenceNumber);
        } else {
            return parseAccessMessage(key, data, networkHeader, decryptedNetworkPayload, src, sequenceNumber, ivIndex);
        }
    }

    /**
     * Parses access message
     *
     * @param key                     Network Key used to decrypt
     * @param data                    Received from the node.
     * @param networkHeader           De-obfuscated network header.
     * @param decryptedNetworkPayload Decrypted network payload.
     * @param src                     Source address.
     * @param sequenceNumber          Sequence number of the received message.
     * @param ivIndex                 IV Index used for decryption.
     * @return access message
     */
    private AccessMessage parseAccessMessage(final NetworkKey key,
                                             final byte[] data,
                                             final byte[] networkHeader,
                                             final byte[] decryptedNetworkPayload,
                                             final int src,
                                             final byte[] sequenceNumber,
                                             int ivIndex) throws ExtendedInvalidCipherTextException {
        try {
            int receivedTtl = networkHeader[0] & 0x7F;
            final int dst = MeshParserUtils.unsignedBytesToInt(decryptedNetworkPayload[1], decryptedNetworkPayload[0]);
            LOG.info("Dst: " + MeshAddress.formatAddress(dst, true));

            if (isSegmentedMessage(decryptedNetworkPayload[2])) {
                LOG.info("Received a segmented access message from: " + MeshAddress.formatAddress(src, false));

                //Check if the received segmented message is from the same src as the previous segment
                //Ideal case this check is not needed but let's leave it for now.
                if (!mMeshNode.hasUnicastAddress(src)) {
                    LOG.info("Segment received is from a different src than the one we are processing, let's drop it");
                    return null;
                }

                if (segmentedAccessMessagesMessages == null) {
                    segmentedAccessMessagesMessages = new HashMap<>();
                    segmentedAccessMessagesMessages.put(0, data);
                } else {
                    final int k = segmentedAccessMessagesMessages.size();
                    segmentedAccessMessagesMessages.put(k, data);
                }
                //Removing the mDst here
                final byte[] pdu = ByteBuffer.allocate(2 + networkHeader.length + decryptedNetworkPayload.length)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(data, 0, 2)
                    .put(networkHeader)
                    .put(decryptedNetworkPayload)
                    .array();

                // Spec states, section 3.5.2.4 page 77
                // If the received segments were sent with TTL set to 0, it is recommended that the
                // corresponding Segment Acknowledgment message is sent with TTL set to 0.
                final int ttl = receivedTtl == 0 ? receivedTtl : mNetworkLayerCallbacks.getProvisioner().getGlobalTtl();
                final AccessMessage message = parseSegmentedAccessLowerTransportPDU(ttl, pdu, ivIndex, sequenceNumber);

                if (message != null) {
                    // final SparseArray<byte[]> segmentedMessages = segmentedAccessMessagesMessages.clone();
                    Map<Integer, byte[]> segmentedMessages = new HashMap<>();
                    for (Integer index : segmentedAccessMessagesMessages.keySet()) {
                        segmentedMessages.put(index, segmentedAccessMessagesMessages.get(index).clone());
                    }
                    segmentedAccessMessagesMessages = null;
                    message.setNetworkKey(key);
                    message.setIvIndex(MeshParserUtils.intToBytes(ivIndex));
                    message.setNetworkLayerPdu(segmentedMessages);
                    message.setTtl(receivedTtl);
                    message.setSrc(src);
                    message.setDst(dst);
                    parseUpperTransportPDU(message);
                    parseAccessLayerPDU(message);
                }
                return message;

            } else {

                //Removing the mDst here
                final byte[] pdu = ByteBuffer.allocate(2 + networkHeader.length + decryptedNetworkPayload.length)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(data, 0, 2)
                    .put(networkHeader)
                    .put(decryptedNetworkPayload)
                    .array();
                final AccessMessage message = parseUnsegmentedAccessLowerTransportPDU(pdu, ivIndex, sequenceNumber);
                if (message == null)
                    return null;
                message.setNetworkKey(key);
                message.setIvIndex(MeshParserUtils.intToBytes(ivIndex));
                final Map<Integer, byte[]> pduArray = new HashMap<>();
                pduArray.put(0, data);
                message.setNetworkLayerPdu(pduArray);
                message.setTtl(receivedTtl);
                message.setSrc(src);
                message.setDst(dst);
                message.setSequenceNumber(sequenceNumber);
                parseUpperTransportPDU(message);
                parseAccessLayerPDU(message);
                return message;
            }
        } catch (InvalidCipherTextException ex) {
            throw new ExtendedInvalidCipherTextException(ex.getMessage(), ex.getCause(), NetworkLayer.class.getSimpleName());
        }
    }

    /**
     * Parses control message
     *
     * @param key                     Network Key used to decrypt
     * @param provisionerAddress      Provisioner address.
     * @param data                    Data received from the node.
     * @param networkHeader           De-obfuscated network header.
     * @param decryptedNetworkPayload Decrypted network payload.
     * @param src                     Source address where the pdu originated from.
     * @param sequenceNumber          Sequence number of the received message.
     * @return a complete {@link ControlMessage} or null if the message was unable to parsed
     */
    private ControlMessage parseControlMessage(final NetworkKey key,
                                               /* @Nullable */ final Integer provisionerAddress,
                                               final byte[] data,
                                               final byte[] networkHeader,
                                               final byte[] decryptedNetworkPayload,
                                               final int src,
                                               final byte[] sequenceNumber) throws ExtendedInvalidCipherTextException {
        try {
            final int ttl = networkHeader[0] & 0x7F;
            final int dst = MeshParserUtils.unsignedBytesToInt(decryptedNetworkPayload[1], decryptedNetworkPayload[0]);

            //Removing the mDst here
            final byte[] decryptedProxyPdu = ByteBuffer.allocate(2 + networkHeader.length + decryptedNetworkPayload.length)
                .order(ByteOrder.BIG_ENDIAN)
                .put(data, 0, 2)
                .put(networkHeader)
                .put(decryptedNetworkPayload)
                .array();

            //We check the pdu type
            final int pduType = data[0];
            switch (pduType) {
                case MeshManagerApi.PDU_TYPE_NETWORK:

                    //This is not possible however let's return null
                    if (provisionerAddress == null) {
                        return null;
                    }

                    //Check if the message is directed to us, if its not ignore the message
                    if (provisionerAddress != dst) {
                        LOG.info("Received a control message that was not directed to us, so we drop it");
                        return null;
                    }

                    if (isSegmentedMessage(decryptedNetworkPayload[2])) {
                        return parseSegmentedControlMessage(key, data, decryptedProxyPdu, ttl, src, dst);
                    } else {
                        return parseUnsegmentedControlMessage(key, data, decryptedProxyPdu, ttl, src, dst, sequenceNumber);
                    }
                case MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION:
                    //Proxy configuration messages are segmented only at the gatt level
                    return parseUnsegmentedControlMessage(key, data, decryptedProxyPdu, ttl, src, dst, sequenceNumber);
                default:
                    return null;
            }
        } catch (InvalidCipherTextException ex) {
            throw new ExtendedInvalidCipherTextException(ex.getMessage(), ex.getCause(), NetworkLayer.class.getSimpleName());
        }
    }

    /**
     * Parses an unsegmented control message
     *
     * @param key                     Network Key used to decrypt
     * @param data              Received pdu data
     * @param decryptedProxyPdu Decrypted proxy pdu
     * @param ttl               TTL of the pdu
     * @param src               Source address where the pdu originated from
     * @param dst               Destination address to which the pdu was sent
     * @param sequenceNumber    Sequence number of the pdu
     * @return a complete {@link ControlMessage} or null if the message was unable to parsed
     */
    private ControlMessage parseUnsegmentedControlMessage(final NetworkKey key,
                                                          final byte[] data,
                                                          final byte[] decryptedProxyPdu,
                                                          final int ttl,
                                                          final int src,
                                                          final int dst,
                                                          final byte[] sequenceNumber) throws ExtendedInvalidCipherTextException {
        final ControlMessage message = new ControlMessage();
        message.setNetworkKey(key);
        message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
        final Map<Integer, byte[]> proxyPduArray = new HashMap<>();
        proxyPduArray.put(0, data);
        message.setNetworkLayerPdu(proxyPduArray);
        message.setTtl(ttl);
        message.setSrc(src);
        message.setDst(dst);
        message.setSequenceNumber(sequenceNumber);
        message.setSegmented(false);
        parseUnsegmentedControlLowerTransportPDU(message, decryptedProxyPdu);

        return message;
    }

    /**
     * Parses a unsegmented control message
     *
     * @param key                     Network Key used to decrypt
     * @param data              Received pdu data
     * @param decryptedProxyPdu Decrypted proxy pdu
     * @param ttl               TTL of the pdu
     * @param src               Source address where the pdu originated from
     * @param dst               Destination address to which the pdu was sent
     * @return a complete {@link ControlMessage} or null if the message was unable to parsed
     */
    private ControlMessage parseSegmentedControlMessage(final NetworkKey key, final byte[] data, final byte[] decryptedProxyPdu, final int ttl, final int src, final int dst) {
        if (segmentedControlMessagesMessages == null) {
            segmentedControlMessagesMessages = new HashMap<>();
            segmentedControlMessagesMessages.put(0, data);
        } else {
            final int k = segmentedControlMessagesMessages.size();
            segmentedAccessMessagesMessages.put(k, data);
        }

        final ControlMessage message = parseSegmentedControlLowerTransportPDU(decryptedProxyPdu);
        if (message != null) {
            // final SparseArray<byte[]> segmentedMessages = segmentedControlMessagesMessages.clone();
            Map<Integer, byte[]> segmentedMessages = new HashMap<>();
            for (Integer index : segmentedControlMessagesMessages.keySet()) {
                segmentedMessages.put(index, segmentedControlMessagesMessages.get(index).clone());
            }
            segmentedControlMessagesMessages = null;
            message.setNetworkKey(key);
            message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
            message.setNetworkLayerPdu(segmentedMessages);
            message.setTtl(ttl);
            message.setSrc(src);
            message.setDst(dst);
        }
        return message;
    }

    /**
     * Returns the master credentials {@link SecureUtils.K2Output}
     *
     * @param message Message
     */
    private SecureUtils.K2Output getK2Output(final Message message) {
        final NetworkKey networkKey;
        if (message.getAkf() == APPLICATION_KEY_IDENTIFIER) {
            networkKey = mNetworkLayerCallbacks.getPrimaryNetworkKey();
        } else {
            final int netKeyIndex = message.getApplicationKey().getBoundNetKeyIndex();
            networkKey = mNetworkLayerCallbacks.getNetworkKey(netKeyIndex);
        }
        return networkKey.getTxDerivatives();
    }

    /**
     * Obfuscates the network header
     *
     * @param ctlTTL         Message type and ttl bit
     * @param sequenceNumber Sequence number of the message
     * @param src            Source address
     * @param pecb           Value derived from the privacy random
     * @return Obfuscated network header
     */
    private byte[] obfuscateNetworkHeader(final byte ctlTTL, final byte[] sequenceNumber, final int src, final byte[] pecb) {

        final ByteBuffer buffer = ByteBuffer.allocate(1 + sequenceNumber.length + 2).order(ByteOrder.BIG_ENDIAN);
        buffer.put(ctlTTL);
        buffer.put(sequenceNumber);   //sequence number
        buffer.putShort((short) src);       //source address

        final byte[] headerBuffer = buffer.array();

        final ByteBuffer bufferPECB = ByteBuffer.allocate(6);
        bufferPECB.put(pecb, 0, 6);

        final byte[] obfuscated = new byte[6];
        for (int i = 0; i < 6; i++)
            obfuscated[i] = (byte) (headerBuffer[i] ^ pecb[i]);

        return obfuscated;
    }

    /**
     * De-obfuscates the network header
     *
     * @param pdu Received from the node
     * @return Obfuscated network header
     */
    static byte[] deObfuscateNetworkHeader(final byte[] pdu,
                                           final byte[] ivIndex,
                                           final byte[] privacyKey) {
        final ByteBuffer obfuscatedNetworkBuffer = ByteBuffer.allocate(6);
        obfuscatedNetworkBuffer.order(ByteOrder.BIG_ENDIAN);
        obfuscatedNetworkBuffer.put(pdu, 2, 6);
        final byte[] obfuscatedData = obfuscatedNetworkBuffer.array();

        final ByteBuffer privacyRandomBuffer = ByteBuffer.allocate(7);
        privacyRandomBuffer.order(ByteOrder.BIG_ENDIAN);
        privacyRandomBuffer.put(pdu, 8, 7);
        final byte[] privacyRandom = createPrivacyRandom(privacyRandomBuffer.array());

        final byte[] pecb = createPECB(ivIndex, privacyRandom, privacyKey);
        final byte[] deObfuscatedData = new byte[6];

        for (int i = 0; i < 6; i++)
            deObfuscatedData[i] = (byte) (obfuscatedData[i] ^ pecb[i]);

        return deObfuscatedData;
    }

    /**
     * Creates the privacy random.
     *
     * @param encryptedUpperTransportPDU Encrypted transport pdu
     * @return Privacy random
     */
    private static byte[] createPrivacyRandom(final byte[] encryptedUpperTransportPDU) {
        final byte[] privacyRandom = new byte[7];
        System.arraycopy(encryptedUpperTransportPDU, 0, privacyRandom, 0, privacyRandom.length);
        return privacyRandom;
    }

    private static byte[] createPECB(final byte[] ivIndex, final byte[] privacyRandom, final byte[] privacyKey) {
        final ByteBuffer buffer = ByteBuffer.allocate(5 + privacyRandom.length + ivIndex.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        buffer.put(ivIndex);
        buffer.put(privacyRandom);
        final byte[] temp = buffer.array();
        return SecureUtils.encryptWithAES(temp, privacyKey);
    }

    /**
     * Creates the network nonce
     *
     * @param ctlTTL         Combined ctl and ttl value
     * @param sequenceNumber Sequence number of the message
     * @param src            Source address
     * @return Network nonce
     */
    static byte[] createNetworkNonce(final byte ctlTTL, final byte[] sequenceNumber, final int src, final byte[] ivIndex) {
        final ByteBuffer networkNonce = ByteBuffer.allocate(13);
        networkNonce.put((byte) NONCE_TYPE_NETWORK); //Nonce type
        networkNonce.put(ctlTTL); // CTL and TTL
        networkNonce.put(sequenceNumber);
        networkNonce.putShort((short) src);
        networkNonce.put(new byte[]{PAD_NETWORK_NONCE, PAD_NETWORK_NONCE}); //PAD
        networkNonce.put(ivIndex);
        return networkNonce.array();
    }

    /**
     * Creates the proxy nonce
     *
     * @param sequenceNumber Sequence number of the message
     * @param src            Source address
     * @return Proxy nonce
     */
    static byte[] createProxyNonce(final byte[] sequenceNumber, final int src, final byte[] ivIndex) {
        final ByteBuffer applicationNonceBuffer = ByteBuffer.allocate(13);
        applicationNonceBuffer.put((byte) NONCE_TYPE_PROXY); //Nonce type
        applicationNonceBuffer.put((byte) PAD_PROXY_NONCE); //PAD
        applicationNonceBuffer.put(sequenceNumber);
        applicationNonceBuffer.putShort((short) src);
        applicationNonceBuffer.put(new byte[]{PAD_PROXY_NONCE, PAD_PROXY_NONCE});
        applicationNonceBuffer.put(ivIndex);
        return applicationNonceBuffer.array();
    }

    /**
     * Encrypts the pdu
     *
     * @param lowerTransportPdu lower transport pdu to be encrypted
     * @param encryptionKey     Encryption key
     * @param nonce             nonce depending on the pdu type
     * @param dst               Destination address
     * @param micLength         Message integrity check length
     */
    private byte[] encryptPdu(final byte[] lowerTransportPdu,
                              final byte[] encryptionKey,
                              final byte[] nonce,
                              final int dst,
                              final int micLength) {
        //Adding the destination address on network layer
        final byte[] unencryptedNetworkPayload = ByteBuffer.allocate(2 + lowerTransportPdu.length).order(ByteOrder.BIG_ENDIAN)
            .putShort((short) dst)
            .put(lowerTransportPdu).array();
        //Network layer encryption
        return SecureUtils.encryptCCM(unencryptedNetworkPayload, encryptionKey, nonce, micLength);
    }
}

