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
 * ConfigMessageState class that handles configuration message state.
 */
class ConfigMessageState extends MeshMessageState {

    private final byte[] mDeviceKey;

    /**
     * Constructs the ConfigMessageState
     *
     * @param src           Source address
     * @param dst           Destination address
     * @param deviceKey     Device key
     * @param meshMessage   {@link MeshMessage} Mesh message to be sent
     * @param meshTransport {@link MeshTransport} Mesh transport
     * @param callbacks     {@link InternalMeshMsgHandlerCallbacks} callbacks
     */
    ConfigMessageState(final int src,
                       final int dst,
                       final byte[] deviceKey,
                       final MeshMessage meshMessage,
                       final MeshTransport meshTransport,
                       final InternalMeshMsgHandlerCallbacks callbacks) {
        super(meshMessage, meshTransport, callbacks);
        this.mSrc = src;
        this.mDst = dst;
        this.mDeviceKey = deviceKey;
        createAccessMessage();
    }

    @Override
    public synchronized MessageState getState() {
        return MessageState.CONFIG_MESSAGE_STATE;
    }

    private synchronized void createAccessMessage() throws IllegalArgumentException {
        final ConfigMessage configMessage = (ConfigMessage) mMeshMessage;
        final int akf = configMessage.getAkf();
        final int aid = configMessage.getAid();
        final int aszmic = configMessage.getAszmic();
        final int opCode = configMessage.getOpCode();
        final byte[] parameters = configMessage.getParameters();
        message = mMeshTransport.createMeshMessage(mSrc, mDst, configMessage.messageTtl,
            mDeviceKey, akf, aid, aszmic, opCode, parameters);
        configMessage.setMessage(message);
    }
}

