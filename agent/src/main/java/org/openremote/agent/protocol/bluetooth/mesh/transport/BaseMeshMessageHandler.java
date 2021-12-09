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
import org.openremote.agent.protocol.bluetooth.mesh.InternalTransportCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.MeshManagerApi;
import org.openremote.agent.protocol.bluetooth.mesh.MeshNetwork;
import org.openremote.agent.protocol.bluetooth.mesh.MeshStatusCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ExtendedInvalidCipherTextException;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.bluetooth.mesh.transport.NetworkLayer.createNetworkNonce;
import static org.openremote.agent.protocol.bluetooth.mesh.transport.NetworkLayer.createProxyNonce;
import static org.openremote.agent.protocol.bluetooth.mesh.transport.NetworkLayer.deObfuscateNetworkHeader;

/**
 * Abstract class that handles mesh messages
 */
public abstract class BaseMeshMessageHandler implements MeshMessageHandlerApi, InternalMeshMsgHandlerCallbacks {

    public static final Logger LOG = Logger.getLogger(BaseMeshMessageHandler.class.getName());

    protected final InternalTransportCallbacks mInternalTransportCallbacks;
    private final NetworkLayerCallbacks networkLayerCallbacks;
    private final UpperTransportLayerCallbacks upperTransportLayerCallbacks;
    protected MeshStatusCallbacks mStatusCallbacks;
    private final Map<Integer, MeshTransport> transportSparseArray = new HashMap<>();
    private final Map<Integer, MeshMessageState> stateSparseArray = new HashMap<>();

    /**
     * Constructs BaseMessageHandler
     *
     * @param internalTransportCallbacks   {@link InternalTransportCallbacks} Callbacks
     * @param networkLayerCallbacks        {@link NetworkLayerCallbacks} network layer callbacks
     * @param upperTransportLayerCallbacks {@link UpperTransportLayerCallbacks} upper transport layer callbacks
     */
    protected BaseMeshMessageHandler(final InternalTransportCallbacks internalTransportCallbacks,
                                     final NetworkLayerCallbacks networkLayerCallbacks,
                                     final UpperTransportLayerCallbacks upperTransportLayerCallbacks) {
        this.mInternalTransportCallbacks = internalTransportCallbacks;
        this.networkLayerCallbacks = networkLayerCallbacks;
        this.upperTransportLayerCallbacks = upperTransportLayerCallbacks;
    }

    /**
     * Sets the mesh status callbacks.
     *
     * @param statusCallbacks {@link MeshStatusCallbacks} callbacks
     */
    protected abstract void setMeshStatusCallbacks(final MeshStatusCallbacks statusCallbacks);

    /**
     * Parse the mesh network/proxy pdus
     * <p>
     * This method will try to network layer de-obfuscation and decryption using the available network keys
     * </p>
     *
     * @param pdu     mesh pdu that was sent
     * @param network {@link MeshNetwork}
     */
    protected synchronized void parseMeshPduNotifications(final byte[] pdu, final MeshNetwork network) throws ExtendedInvalidCipherTextException {
        final List<NetworkKey> networkKeys = network.getNetKeys();
        final int ivi = ((pdu[1] & 0xFF) >>> 7) & 0x01;
        final int nid = pdu[1] & 0x7F;
        final int acceptedIvIndex = network.getIvIndex().getIvIndex();
        int ivIndex = acceptedIvIndex == 0 ? 0 : acceptedIvIndex - 1;
        NetworkKey networkKey;
        SecureUtils.K2Output k2Output;
        byte[] networkHeader;
        int ctlTtl;
        int ctl;
        int ttl;
        // while (ivIndex <= ivIndex + 1) {
         {
             ivIndex = 0;
            //Here we go through all the network keys and filter out network keys based on the nid.
            for (int i = 0; i < networkKeys.size(); i++) {
                networkKey = networkKeys.get(i);
                k2Output = getMatchingK2Output(networkKey, nid);
                if (k2Output != null) {
                    networkHeader = deObfuscateNetworkHeader(pdu, MeshParserUtils.intToBytes(ivIndex), k2Output.getPrivacyKey());
                    ctlTtl = networkHeader[0];
                    ctl = (ctlTtl >> 7) & 0x01;
                    ttl = ctlTtl & 0x7F;
                    LOG.info("ivIndex=" + ivIndex + "i(keyIndex)=" + i);
                    LOG.info("TTL for received message: " + ttl);
                    final int src = MeshParserUtils.unsignedBytesToInt(networkHeader[5], networkHeader[4]);
                    LOG.info("Source address of received message: " + src);
                    final ProvisionedMeshNode node = network.getNode(src);
                    if (node == null) {
                        LOG.warning("Couldn't find a node for source address: " + src);
                        continue;
                    }
                    final byte[] sequenceNumber = ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN).put(networkHeader, 1, 3).array();
                    LOG.info("Sequence number of received access message: " + MeshParserUtils.convert24BitsToInt(sequenceNumber));
                    //TODO validate ivi
                    byte[] nonce;
                    try {
                        final int networkPayloadLength = pdu.length - (2 + networkHeader.length);
                        final byte[] transportPdu = new byte[networkPayloadLength];
                        System.arraycopy(pdu, 8, transportPdu, 0, networkPayloadLength);
                        final byte[] decryptedPayload;
                        final MeshMessageState state;
                        if (pdu[0] == MeshManagerApi.PDU_TYPE_NETWORK) {
                            nonce = createNetworkNonce((byte) ctlTtl, sequenceNumber, src, MeshParserUtils.intToBytes(ivIndex));
                            decryptedPayload = SecureUtils.decryptCCM(transportPdu, k2Output.getEncryptionKey(), nonce, SecureUtils.getNetMicLength(ctl));
                            state = getState(src);
                        } else {
                            nonce = createProxyNonce(sequenceNumber, src, MeshParserUtils.intToBytes(ivIndex));
                            decryptedPayload = SecureUtils.decryptCCM(transportPdu, k2Output.getEncryptionKey(), nonce, SecureUtils.getNetMicLength(ctl));
                            state = getState(MeshAddress.UNASSIGNED_ADDRESS);
                        }
                        if (state != null) {
                            //TODO look in to proxy filter messages
                            ((DefaultNoOperationMessageState) state).parseMeshPdu(networkKey, node, pdu, networkHeader, decryptedPayload, ivIndex, sequenceNumber);
                            return;
                        }
                    } catch (InvalidCipherTextException ex) {
                        if (i == networkKeys.size() - 1) {
                            throw new ExtendedInvalidCipherTextException(ex.getMessage(), ex.getCause(), BaseMeshMessageHandler.class.getName());
                        }
                    }
                }
            }
            ivIndex++;
        }
    }

    /**
     * Returns the K2output for a given Network Key matching the received NID
     *
     * @param networkKey NetworkKey
     * @param nid        NID
     * @return {@link SecureUtils.K2Output} or null if the NID doesn't match.
     */
    private SecureUtils.K2Output getMatchingK2Output(final NetworkKey networkKey, final int nid) {
        if(nid == networkKey.getDerivatives().getNid())
            return networkKey.getDerivatives();
        else if(networkKey.getOldDerivatives() != null && nid == networkKey.getOldDerivatives().getNid())
            return networkKey.getOldDerivatives();
        return null;
    }

    @Override
    public synchronized final void onIncompleteTimerExpired(final int address) {
        //We switch no operation state if the incomplete timer has expired so that we don't wait on the same state if a particular message fails.
        stateSparseArray.put(address, toggleState(getTransport(address), getState(address).getMeshMessage()));
    }

    /**
     * Toggles the current state to default state of a node
     *
     * @param transport   mesh transport of the current state
     * @param meshMessage Mesh message
     */
    private DefaultNoOperationMessageState toggleState(final MeshTransport transport, /* @Nullable */ final MeshMessage meshMessage) {
        final DefaultNoOperationMessageState state = new DefaultNoOperationMessageState(meshMessage, transport, this);
        state.setTransportCallbacks(mInternalTransportCallbacks);
        state.setStatusCallbacks(mStatusCallbacks);
        return state;
    }

    /**
     * Returns the existing state or a new state if nothing exists for a node
     *
     * @param address address of the node
     */
    protected synchronized MeshMessageState getState(final int address) {
        MeshMessageState state = stateSparseArray.get(address);
        if (state == null) {
            state = new DefaultNoOperationMessageState(null, getTransport(address), this);
            state.setTransportCallbacks(mInternalTransportCallbacks);
            state.setStatusCallbacks(mStatusCallbacks);
            stateSparseArray.put(address, state);
        }
        return state;
    }

    /**
     * Returns the existing transport of the node or a new transport if nothing exists
     *
     * @param address address of the node
     */
    private MeshTransport getTransport(final int address) {
        MeshTransport transport = transportSparseArray.get(address);
        if (transport == null) {
            transport = new MeshTransport();
            transport.setNetworkLayerCallbacks(networkLayerCallbacks);
            transport.setUpperTransportLayerCallbacks(upperTransportLayerCallbacks);
            transportSparseArray.put(address, transport);
        }
        return transport;
    }

    /**
     * Resets the state and transport for a given node address
     *
     * @param address unicast address of the node
     */
    public synchronized void resetState(final int address) {
        stateSparseArray.remove(address);
        transportSparseArray.remove(address);
    }

    @Override
    public synchronized void createMeshMessage(final int src, final int dst, final UUID label, final MeshMessage meshMessage) {
        if (meshMessage instanceof ProxyConfigMessage) {
            createProxyConfigMeshMessage(src, dst, (ProxyConfigMessage) meshMessage);
        } else if (meshMessage instanceof ConfigMessage) {
            createConfigMeshMessage(src, dst, (ConfigMessage) meshMessage);
        } else if (meshMessage instanceof GenericMessage) {
            if (label == null) {
                createAppMeshMessage(src, dst, (GenericMessage) meshMessage);
            } else {
                createAppMeshMessage(src, dst, label, (GenericMessage) meshMessage);
            }
        }
    }

    /**
     * Sends a mesh message specified within the {@link MeshMessage} object
     *
     * @param configurationMessage {@link ProxyConfigMessage} Mesh message containing the message opcode and message parameters
     */
    private void createProxyConfigMeshMessage(final int src, final int dst, final ProxyConfigMessage configurationMessage) {
        final ProxyConfigMessageState currentState = new ProxyConfigMessageState(src, dst, configurationMessage, getTransport(dst), this);
        currentState.setTransportCallbacks(mInternalTransportCallbacks);
        currentState.setStatusCallbacks(mStatusCallbacks);
        stateSparseArray.put(dst, toggleState(currentState.getMeshTransport(), configurationMessage));
        currentState.executeSend();
    }

    /**
     * Sends a mesh message specified within the {@link MeshMessage} object
     *
     * @param configurationMessage {@link ConfigMessage} Mesh message containing the message opcode and message parameters
     */
    private void createConfigMeshMessage(final int src, final int dst, final ConfigMessage configurationMessage) {
        final ProvisionedMeshNode node = mInternalTransportCallbacks.getNode(dst);
        if (node == null) {
            return;
        }

        final ConfigMessageState currentState = new ConfigMessageState(src, dst, node.getDeviceKey(), configurationMessage, getTransport(dst), this);
        currentState.setTransportCallbacks(mInternalTransportCallbacks);
        currentState.setStatusCallbacks(mStatusCallbacks);
        if (MeshAddress.isValidUnicastAddress(dst)) {
            stateSparseArray.put(dst, toggleState(getTransport(dst), configurationMessage));
        }
        currentState.executeSend();
    }

    /**
     * Sends a mesh message specified within the {@link GenericMessage} object
     * <p> This method can be used specifically when sending an application message with a unicast address or a group address.
     * Application messages currently supported in the library are {@link GenericOnOffGet},{@link GenericOnOffSet}, {@link GenericOnOffSetUnacknowledged},
     * {@link GenericLevelGet},  {@link GenericLevelSet},  {@link GenericLevelSetUnacknowledged},
     * {@link VendorModelMessageAcked} and {@link VendorModelMessageUnacked}</p>
     *
     * @param src            source address where the message is originating from
     * @param dst            Destination to which the message must be sent to, this could be a unicast address or a group address.
     * @param genericMessage Mesh message containing the message opcode and message parameters.
     */
    private void createAppMeshMessage(final int src, final int dst, final GenericMessage genericMessage) {
        final GenericMessageState currentState;
        if (genericMessage instanceof VendorModelMessageAcked) {
            currentState = new VendorModelMessageAckedState(src, dst, (VendorModelMessageAcked) genericMessage, getTransport(dst), this);
        } else if (genericMessage instanceof VendorModelMessageUnacked) {
            currentState = new VendorModelMessageUnackedState(src, dst, (VendorModelMessageUnacked) genericMessage, getTransport(dst), this);
        } else {
            currentState = new GenericMessageState(src, dst, genericMessage, getTransport(dst), this);
        }
        currentState.setTransportCallbacks(mInternalTransportCallbacks);
        currentState.setStatusCallbacks(mStatusCallbacks);
        if (MeshAddress.isValidUnicastAddress(dst)) {
            stateSparseArray.put(dst, toggleState(getTransport(dst), genericMessage));
        }
        currentState.executeSend();
    }


    /**
     * Sends a mesh message specified within the {@link GenericMessage} object
     * <p> This method can be used specifically when sending an application message with a unicast address or a group address.
     * Application messages currently supported in the library are {@link GenericOnOffGet},{@link GenericOnOffSet}, {@link GenericOnOffSetUnacknowledged},
     * {@link GenericLevelGet},  {@link GenericLevelSet},  {@link GenericLevelSetUnacknowledged},
     * {@link VendorModelMessageAcked} and {@link VendorModelMessageUnacked}</p>
     *
     * @param src            source address where the message is originating from
     * @param dst            Destination to which the message must be sent to, this could be a unicast address or a group address.
     * @param label          Label UUID of destination address
     * @param genericMessage Mesh message containing the message opcode and message parameters.
     */
    private void createAppMeshMessage(final int src, final int dst, UUID label, final GenericMessage genericMessage) {
        final GenericMessageState currentState;
        if (genericMessage instanceof VendorModelMessageAcked) {
            currentState = new VendorModelMessageAckedState(src, dst, label, (VendorModelMessageAcked) genericMessage, getTransport(dst), this);
        } else if (genericMessage instanceof VendorModelMessageUnacked) {
            currentState = new VendorModelMessageUnackedState(src, dst, label, (VendorModelMessageUnacked) genericMessage, getTransport(dst), this);
        } else {
            currentState = new GenericMessageState(src, dst, label, genericMessage, getTransport(dst), this);
        }
        currentState.setTransportCallbacks(mInternalTransportCallbacks);
        currentState.setStatusCallbacks(mStatusCallbacks);
        if (MeshAddress.isValidUnicastAddress(dst)) {
            stateSparseArray.put(dst, toggleState(getTransport(dst), genericMessage));
        }
        currentState.executeSend();
    }
}

