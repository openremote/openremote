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

import org.openremote.agent.protocol.bluetooth.mesh.InternalTransportCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.MeshStatusCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.List;
import java.util.logging.Logger;

/**
 * This generic class handles the mesh messages received or sent.
 * <p>
 * This class handles sending, resending and parsing mesh messages. Each message sent by the library has its own state.
 * {@link ConfigMessageState} and {@link GenericMessageState} extends this class based on the type of the message.
 * Currently the library supports basic Configuration and Generic Messages.
 * </p>
 */
abstract class MeshMessageState implements LowerTransportLayerCallbacks {

    public static final Logger LOG = Logger.getLogger(MeshMessageState.class.getName());

    MeshMessage mMeshMessage;
    final MeshTransport mMeshTransport;
    private final InternalMeshMsgHandlerCallbacks meshMessageHandlerCallbacks;
    protected InternalTransportCallbacks mInternalTransportCallbacks;
    MeshStatusCallbacks mMeshStatusCallbacks;
    int mSrc;
    int mDst;
    protected Message message;

    /**
     * Constructs the base mesh message state class
     *
     * @param meshMessage   {@link MeshMessage} Mesh message
     * @param meshTransport {@link MeshTransport} Mesh transport
     * @param callbacks     {@link InternalMeshMsgHandlerCallbacks} Internal mesh message handler callbacks
     */
    MeshMessageState(/* @Nullable */ final MeshMessage meshMessage,
                     final MeshTransport meshTransport,
                     final InternalMeshMsgHandlerCallbacks callbacks) {
        this.mMeshMessage = meshMessage;
        if (meshMessage != null) {
            this.message = meshMessage.getMessage();
        }
        this.meshMessageHandlerCallbacks = callbacks;
        this.mMeshTransport = meshTransport;
        this.mMeshTransport.setLowerTransportLayerCallbacks(this);
    }

    /**
     * Set transport callbacks
     *
     * @param callbacks callbacks
     */
    synchronized void setTransportCallbacks(final InternalTransportCallbacks callbacks) {
        this.mInternalTransportCallbacks = callbacks;
    }

    /**
     * Set mesh status call backs
     *
     * @param callbacks callbacks
     */
    synchronized void setStatusCallbacks(final MeshStatusCallbacks callbacks) {
        this.mMeshStatusCallbacks = callbacks;
    }

    /**
     * Returns the current mesh state
     */
    abstract MessageState getState();

    /**
     * Returns the mesh transport
     */
    synchronized MeshTransport getMeshTransport() {
        return mMeshTransport;
    }

    /**
     * Returns the mesh message relating to the state
     */
    public synchronized MeshMessage getMeshMessage() {
        return mMeshMessage;
    }

    /**
     * Starts sending the mesh pdu
     */
    public synchronized void executeSend() {
        if (message.getNetworkLayerPdu().size() > 0) {
            for (int i = 0; i < message.getNetworkLayerPdu().size(); i++) {
                mInternalTransportCallbacks.onMeshPduCreated(mDst, message.getNetworkLayerPdu().get(i));
            }

            if (mMeshStatusCallbacks != null) {
                mMeshStatusCallbacks.onMeshMessageProcessed(mDst, mMeshMessage);
            }
        }
    }

    /**
     * Re-sends the mesh pdu segments that were lost in flight
     *
     * @param retransmitPduIndexes list of indexes of the messages to be
     */
    final synchronized void executeResend(final List<Integer> retransmitPduIndexes) {
        if (message.getNetworkLayerPdu().size() > 0 && !retransmitPduIndexes.isEmpty()) {
            for (int i = 0; i < retransmitPduIndexes.size(); i++) {
                final int segO = retransmitPduIndexes.get(i);
                if (message.getNetworkLayerPdu().get(segO) != null) {
                    final byte[] pdu = message.getNetworkLayerPdu().get(segO);
                    LOG.info("Resending segment " + segO + " : " + MeshParserUtils.bytesToHex(pdu, false));
                    final Message retransmitMeshMessage = mMeshTransport.createRetransmitMeshMessage(message, segO);
                    mInternalTransportCallbacks.onMeshPduCreated(mDst, retransmitMeshMessage.getNetworkLayerPdu().get(segO));
                }
            }
        }
    }

    @Override
    public synchronized void onIncompleteTimerExpired() {
        LOG.info("Incomplete timer has expired, all segments were not received!");
        if (meshMessageHandlerCallbacks != null) {
            meshMessageHandlerCallbacks.onIncompleteTimerExpired(mDst);

            if (mMeshStatusCallbacks != null) {
                mMeshStatusCallbacks.onTransactionFailed(mDst, true);
            }
        }
    }

    @Override
    public synchronized int getTtl() {
        return message.getTtl();
    }

    @Override
    public synchronized void sendSegmentAcknowledgementMessage(final ControlMessage controlMessage) {
        //We don't send acknowledgements here
        final ControlMessage message = mMeshTransport.createSegmentBlockAcknowledgementMessage(controlMessage);
        LOG.info("Sending acknowledgement: " + MeshParserUtils.bytesToHex(message.getNetworkLayerPdu().get(0), false));
        mInternalTransportCallbacks.onMeshPduCreated(message.getDst(), message.getNetworkLayerPdu().get(0));
        mMeshStatusCallbacks.onBlockAcknowledgementProcessed(message.getDst(), controlMessage);
    }

    public enum MessageState {

        //Proxy configuration message
        PROXY_CONFIG_MESSAGE_STATE(500),

        //Configuration message States
        CONFIG_MESSAGE_STATE(501),

        //Application message States
        GENERIC_MESSAGE_STATE(502),
        VENDOR_MODEL_ACKNOWLEDGED_STATE(1000),
        VENDOR_MODEL_UNACKNOWLEDGED_STATE(1001);

        private int state;

        MessageState(final int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }
    }
}

