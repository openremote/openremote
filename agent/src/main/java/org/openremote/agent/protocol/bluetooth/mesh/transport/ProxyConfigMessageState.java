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

/**
 * This generic state class handles the proxy configuration messages received or sent.
 * <p>
 * Each message sent by the library has its own state.
 * </p>
 */
class ProxyConfigMessageState extends MeshMessageState {

    private static final String TAG = ProxyConfigMessageState.class.getSimpleName();

    /**
     * Constructs the ProxyConfigMessageState for sending/receiving proxy configuration messages
     *
     * @param src           Source address
     * @param dst           Destination address
     * @param meshMessage   {@link MeshMessage} Mesh proxy config message
     * @param meshTransport {@link MeshTransport} Mesh transport
     * @param callbacks     {@link InternalMeshMsgHandlerCallbacks} Internal callbacks
     */
    ProxyConfigMessageState(final int src,
                            final int dst,
                            final MeshMessage meshMessage,
                            final MeshTransport meshTransport,
                            final InternalMeshMsgHandlerCallbacks callbacks) {
        super(meshMessage, meshTransport, callbacks);
        this.mSrc = src;
        this.mDst = dst;
        createControlMessage();
    }

    @Override
    public synchronized MessageState getState() {
        return MessageState.PROXY_CONFIG_MESSAGE_STATE;
    }

    /**
     * Creates the control message to be sent to the node
     */
    private synchronized void createControlMessage() {
        final ProxyConfigMessage proxyConfigMessage = (ProxyConfigMessage) mMeshMessage;
        final int opCode = proxyConfigMessage.getOpCode();
        final byte[] parameters = proxyConfigMessage.getParameters();
        message = mMeshTransport.createProxyConfigurationMessage(mSrc, mDst, opCode, parameters);
        proxyConfigMessage.setMessage(message);
    }
}

