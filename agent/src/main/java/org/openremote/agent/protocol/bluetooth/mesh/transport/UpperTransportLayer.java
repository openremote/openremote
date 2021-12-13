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

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.Group;
import org.openremote.agent.protocol.bluetooth.mesh.MeshManagerApi;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ExtendedInvalidCipherTextException;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.logging.Logger;

/**
 * UpperTransportLayer implementation of the mesh network architecture as per the mesh profile specification.
 * <p>
 * UpperTransportLayer class encrypts/decrypts Access PDUs created in the access layer.
 * </p>
 */
public abstract class UpperTransportLayer extends AccessLayer{

    public static final Logger LOG = Logger.getLogger(UpperTransportLayer.class.getName());

    private static final int PROXY_CONFIG_OPCODE_LENGTH = 1;
    static final int MAX_SEGMENTED_ACCESS_PAYLOAD_LENGTH = 12;
    public static final int MAX_UNSEGMENTED_CONTROL_PAYLOAD_LENGTH = 11;
    public static final int MAX_SEGMENTED_CONTROL_PAYLOAD_LENGTH = 8;

    //Nonce types
    public static final int NONCE_TYPE_NETWORK = 0x00;
    public static final int NONCE_TYPE_PROXY = 0x03;

    //Nonce paddings
    public static final int PAD_NETWORK_NONCE = 0x00;
    public static final int PAD_PROXY_NONCE = 0x00;
    public static final int APPLICATION_KEY_IDENTIFIER = 0; //Identifies that the device key is to be used
    private static final int MAX_UNSEGMENTED_ACCESS_PAYLOAD_LENGTH = 15;
    private static final int NONCE_TYPE_APPLICATION = 0x01;
    private static final int NONCE_TYPE_DEVICE = 0x02;
    private static final int PAD_APPLICATION_DEVICE_NONCE = 0b0000000;
    private static final int SZMIC = 1; //Transmic becomes 8 bytes
    private static final int TRANSPORT_SAR_SEQZERO_MASK = 8191;
    private static final int DEFAULT_UNSEGMENTED_MIC_LENGTH = 4; //octets
    private static final int MINIMUM_TRANSMIC_LENGTH = 4; // bytes
    private static final int MAXIMUM_TRANSMIC_LENGTH = 8; // bytes

    UpperTransportLayerCallbacks mUpperTransportLayerCallbacks;

    /**
     * Creates lower transport pdu
     */
    abstract void createLowerTransportAccessPDU(final AccessMessage message);

    /**
     * Creates lower transport pdu
     */
    abstract void createLowerTransportControlPDU(final ControlMessage message);

    /**
     * Removes the lower transport layer header and reassembles a segented lower transport access pdu in to one message
     *
     * @param accessMessage access message containing the lower transport pdus
     */
    abstract void reassembleLowerTransportAccessPDU(final AccessMessage accessMessage);

    /**
     * Removes the lower transport layer header and reassembles a segented lower transport control pdu in to one message
     *
     * @param controlMessage control message containing the lower transport pdus
     */
    abstract void reassembleLowerTransportControlPDU(final ControlMessage controlMessage);

    /**
     * Sets the upper transport layer callbacks
     *
     * @param callbacks {@link UpperTransportLayerCallbacks} callbacks
     */
    abstract void setUpperTransportLayerCallbacks(final UpperTransportLayerCallbacks callbacks);

    /**
     * Creates a mesh message containing an upper transport access pdu
     *
     * @param message The access message required to create the encrypted upper transport pdu
     */
    synchronized void createMeshMessage(final Message message) { //Access message
        if (message instanceof AccessMessage) {
            super.createMeshMessage(message);
            final AccessMessage accessMessage = (AccessMessage) message;
            final byte[] encryptedTransportPDU = encryptUpperTransportPDU(accessMessage);
            LOG.info("Encrypted upper transport pdu: " + MeshParserUtils.bytesToHex(encryptedTransportPDU, false));
            accessMessage.setUpperTransportPdu(encryptedTransportPDU);
        } else {
            createUpperTransportPDU(message);
        }
    }

    /**
     * Creates a vendor model mesh message containing an upper transport access pdu
     *
     * @param message The access message required to create the encrypted upper transport pdu
     */
    synchronized void createVendorMeshMessage(final Message message) { //Access message
        super.createVendorMeshMessage(message);
        final AccessMessage accessMessage = (AccessMessage) message;
        final byte[] encryptedTransportPDU = encryptUpperTransportPDU(accessMessage);
        LOG.info("Encrypted upper transport pdu: " + MeshParserUtils.bytesToHex(encryptedTransportPDU, false));
        accessMessage.setUpperTransportPdu(encryptedTransportPDU);
    }

    /**
     * Creates the upper transport access pdu
     *
     * @param message The message required to create the encrypted upper transport pdu
     */
    synchronized void createUpperTransportPDU(final Message message) {
        if (message instanceof AccessMessage) {
            //Access message
            final AccessMessage accessMessage = (AccessMessage) message;
            final byte[] encryptedTransportPDU = encryptUpperTransportPDU(accessMessage);
            LOG.info("Encrypted upper transport pdu: " + MeshParserUtils.bytesToHex(encryptedTransportPDU, false));
            accessMessage.setUpperTransportPdu(encryptedTransportPDU);
        } else {
            final ControlMessage controlMessage = (ControlMessage) message;
            final int opCode = controlMessage.getOpCode();
            final byte[] parameters = controlMessage.getParameters();
            final ByteBuffer accessMessageBuffer;
            if (parameters != null) {
                accessMessageBuffer = ByteBuffer.allocate(PROXY_CONFIG_OPCODE_LENGTH + parameters.length)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put((byte) opCode)
                    .put(parameters);
            } else {
                accessMessageBuffer = ByteBuffer.allocate(PROXY_CONFIG_OPCODE_LENGTH);
                accessMessageBuffer.put((byte) opCode);
            }
            final byte[] accessPdu = accessMessageBuffer.array();

            LOG.info("Created Transport Control PDU " + MeshParserUtils.bytesToHex(accessPdu, false));
            controlMessage.setTransportControlPdu(accessPdu);
        }
    }

    /**
     * Parse upper transport pdu
     *
     * @param message access message containing the upper transport pdu
     */
    synchronized final void parseUpperTransportPDU(final Message message) throws ExtendedInvalidCipherTextException {
        try {
            switch (message.getPduType()) {
                case MeshManagerApi.PDU_TYPE_NETWORK:
                    if (message instanceof AccessMessage) { //Access message
                        final AccessMessage accessMessage = (AccessMessage) message;
                        reassembleLowerTransportAccessPDU(accessMessage);
                        final byte[] decryptedUpperTransportControlPdu = decryptUpperTransportPDU(accessMessage);
                        accessMessage.setAccessPdu(decryptedUpperTransportControlPdu);
                    } else {
                        //TODO
                        //this where control messages such as heartbeat and friendship messages are to be implemented
                    }
                    break;
                case MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION:
                    final ControlMessage controlMessage = (ControlMessage) message;
                    if (controlMessage.getLowerTransportControlPdu().size() == 1) {
                        final byte[] lowerTransportControlPdu = controlMessage.getLowerTransportControlPdu().get(0);
                        final ByteBuffer buffer = ByteBuffer.wrap(lowerTransportControlPdu)
                            .order(ByteOrder.BIG_ENDIAN);
                        message.setOpCode(buffer.get());
                        final byte[] parameters = new byte[buffer.capacity() - 1];
                        buffer.get(parameters);
                        message.setParameters(parameters);
                    }
                    break;
            }
        } catch (InvalidCipherTextException ex) {
            throw new ExtendedInvalidCipherTextException(ex.getMessage(), ex.getCause(), UpperTransportLayer.class.getSimpleName());
        }
    }

    /**
     * Encrypts upper transport pdu
     *
     * @param message access message object containing the upper transport pdu
     * @return encrypted upper transport pdu
     */
    private byte[] encryptUpperTransportPDU(final AccessMessage message) {
        final byte[] accessPDU = message.getAccessPdu();
        final int akf = message.getAkf();
        final int aszmic = message.getAszmic(); // upper transport layer will always have the aszmic as 0 because the mic is always 32bit

        final byte[] sequenceNumber = message.getSequenceNumber();
        final int src = message.getSrc();
        final int dst = message.getDst();
        final byte[] ivIndex = message.getIvIndex();
        final byte[] key;

        byte[] nonce;
        if (akf == APPLICATION_KEY_IDENTIFIER) {
            key = message.getDeviceKey();
            nonce = createDeviceNonce(aszmic, sequenceNumber, src, dst, ivIndex);
            LOG.info("Device nonce: " + MeshParserUtils.bytesToHex(nonce, false));
        } else {
            key = message.getApplicationKey().getKey();
            nonce = createApplicationNonce(aszmic, sequenceNumber, src, dst, ivIndex);
            LOG.info("Application nonce: " + MeshParserUtils.bytesToHex(nonce, false));
        }

        int transMicLength;
        final int encryptedPduLength = accessPDU.length + MINIMUM_TRANSMIC_LENGTH;

        if (encryptedPduLength <= MAX_UNSEGMENTED_ACCESS_PAYLOAD_LENGTH) {
            transMicLength = SecureUtils.getTransMicLength(message.getCtl());
        } else {
            transMicLength = SecureUtils.getTransMicLength(message.getAszmic());
        }
        if (MeshAddress.isValidVirtualAddress(dst)) {
            return SecureUtils.encryptCCM(accessPDU, key, nonce, MeshParserUtils.uuidToBytes(message.getLabel()), transMicLength);
        } else {
            return SecureUtils.encryptCCM(accessPDU, key, nonce, transMicLength);
        }
    }

    /**
     * Returns the decrypted upper transport pdu
     *
     * @param accessMessage Access message object containing the upper transport pdu
     */
    private byte[] decryptUpperTransportPDU(final AccessMessage accessMessage) throws InvalidCipherTextException {
        byte[] decryptedUpperTransportPDU;
        byte[] key;
        final int transportMicLength = accessMessage.getAszmic() == SZMIC ? MAXIMUM_TRANSMIC_LENGTH : MINIMUM_TRANSMIC_LENGTH;
        //Check if the key used for encryption is an application key or a device key
        final byte[] nonce;
        if (APPLICATION_KEY_IDENTIFIER == accessMessage.getAkf()) {
            key = mMeshNode.getDeviceKey();
            //If its a device key that was used to encrypt the message we need to create a device nonce to decrypt it
            nonce = createDeviceNonce(accessMessage.getAszmic(), accessMessage.getSequenceNumber(), accessMessage.getSrc(), accessMessage.getDst(), accessMessage.getIvIndex());
            decryptedUpperTransportPDU = SecureUtils.decryptCCM(accessMessage.getUpperTransportPdu(), key, nonce, transportMicLength);
        } else {
            final List<ApplicationKey> keys = mUpperTransportLayerCallbacks.getApplicationKeys(accessMessage.getNetworkKey().getKeyIndex());
            if (keys.isEmpty())
                throw new IllegalArgumentException("Unable to find the app key to decrypt the message");

            nonce = createApplicationNonce(accessMessage.getAszmic(), accessMessage.getSequenceNumber(), accessMessage.getSrc(),
                accessMessage.getDst(), accessMessage.getIvIndex());

            if (MeshAddress.isValidVirtualAddress(accessMessage.getDst())) {
                decryptedUpperTransportPDU = decrypt(accessMessage, mUpperTransportLayerCallbacks.gerVirtualGroups(), keys, nonce, transportMicLength);
            } else {
                decryptedUpperTransportPDU = decrypt(accessMessage, keys, nonce, transportMicLength);
            }
        }

        if (decryptedUpperTransportPDU == null)
            throw new IllegalArgumentException("Unable to decrypt the message, invalid application key identifier!");

        final byte[] tempBytes = new byte[decryptedUpperTransportPDU.length];
        ByteBuffer decryptedBuffer = ByteBuffer.wrap(tempBytes);
        decryptedBuffer.order(ByteOrder.LITTLE_ENDIAN);
        decryptedBuffer.put(decryptedUpperTransportPDU);
        decryptedUpperTransportPDU = decryptedBuffer.array();
        return decryptedUpperTransportPDU;
    }

    private byte[] decrypt(final AccessMessage accessMessage, final List<Group> groups, List<ApplicationKey> keys, final byte[] nonce, final int transportMicLength) {
        for (ApplicationKey key : keys) {
            for (Group group : groups) {
                if(group.getAddressLabel() != null) {
                    if (key.getAid() == accessMessage.getAid()) {
                        try {
                            return SecureUtils
                                .decryptCCM(accessMessage.getUpperTransportPdu(), key.getKey(), nonce, MeshParserUtils.uuidToBytes(group.getAddressLabel()), transportMicLength);
                        } catch (Exception ex) {
                            // Retrying decryption
                        }
                    }
                    if (key.getOldAid() == accessMessage.getAid()) {

                        try {
                            return SecureUtils
                                .decryptCCM(accessMessage.getUpperTransportPdu(), key.getOldKey(), nonce, MeshParserUtils.uuidToBytes(group.getAddressLabel()), transportMicLength);
                        } catch (Exception ex) {
                            // Retrying decryption
                        }
                    }
                }
            }
        }
        return null;
    }

    private byte[] decrypt(final AccessMessage accessMessage, List<ApplicationKey> keys, final byte[] nonce, final int transportMicLength) {
        for (ApplicationKey key : keys) {
            if (key.getAid() == accessMessage.getAid()) {
                try {
                    return SecureUtils
                        .decryptCCM(accessMessage.getUpperTransportPdu(), key.getKey(), nonce, transportMicLength);
                } catch (Exception ex) {
                    // Retrying decryption.
                }

            }
            if (key.getOldAid() == accessMessage.getAid()) {
                try {
                    return SecureUtils
                        .decryptCCM(accessMessage.getUpperTransportPdu(), key.getKey(), nonce, transportMicLength);
                } catch (Exception ex) {
                    // Retrying decryption.
                }
            }
        }
        return null;
    }

    /**
     * Creates the application nonce
     *
     * @param aszmic         aszmic (szmic if a segmented access message)
     * @param sequenceNumber sequence number of the message
     * @param src            source address
     * @param dst            destination address
     * @return Application nonce
     */
    private byte[] createApplicationNonce(final int aszmic,
                                          final byte[] sequenceNumber,
                                          final int src,
                                          final int dst,
                                          final byte[] ivIndex) {
        final ByteBuffer applicationNonceBuffer = ByteBuffer.allocate(13);
        applicationNonceBuffer.put((byte) NONCE_TYPE_APPLICATION); //Nonce type
        applicationNonceBuffer.put((byte) ((aszmic << 7) | PAD_APPLICATION_DEVICE_NONCE)); //ASZMIC (SZMIC if a segmented access message) and PAD
        applicationNonceBuffer.put(sequenceNumber);
        applicationNonceBuffer.putShort((short) src);
        applicationNonceBuffer.putShort((short) dst);
        applicationNonceBuffer.put(ivIndex);
        return applicationNonceBuffer.array();
    }

    /**
     * Creates the device nonce
     *
     * @param aszmic         aszmic (szmic if a segmented access message)
     * @param sequenceNumber sequence number of the message
     * @param src            source address
     * @param dst            destination address
     * @return Device  nonce
     */
    private byte[] createDeviceNonce(final int aszmic,
                                     final byte[] sequenceNumber,
                                     final int src,
                                     final int dst,
                                     final byte[] ivIndex) {
        final ByteBuffer deviceNonceBuffer = ByteBuffer.allocate(13);
        deviceNonceBuffer.put((byte) NONCE_TYPE_DEVICE); //Nonce type
        deviceNonceBuffer.put((byte) ((aszmic << 7) | PAD_APPLICATION_DEVICE_NONCE)); //ASZMIC (SZMIC if a segmented access message) and PAD
        deviceNonceBuffer.put(sequenceNumber);
        deviceNonceBuffer.putShort((short) src);
        deviceNonceBuffer.putShort((short) dst);
        deviceNonceBuffer.put(ivIndex);
        return deviceNonceBuffer.array();
    }

    /**
     * Derives the original transport layer sequence number from the network layer sequence number that was received with every segment
     *
     * @param networkLayerSequenceNumber sequence number on network layer which is a part of the original pdu received
     * @param seqZero                    the lower 13 bits of the sequence number. This is a part of the lower transport pdu header and is the same value for all segments
     * @return original transport layer sequence number that was used to encrypt the transport layer pdu
     */
    final int getTransportLayerSequenceNumber(final int networkLayerSequenceNumber, final int seqZero) {
        if ((networkLayerSequenceNumber & TRANSPORT_SAR_SEQZERO_MASK) < seqZero) {
            return ((networkLayerSequenceNumber - ((networkLayerSequenceNumber & TRANSPORT_SAR_SEQZERO_MASK) - seqZero) - (TRANSPORT_SAR_SEQZERO_MASK + 1)));
        } else {
            return ((networkLayerSequenceNumber - ((networkLayerSequenceNumber & TRANSPORT_SAR_SEQZERO_MASK) - seqZero)));
        }
    }
}
