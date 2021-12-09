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

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;

import java.util.UUID;

/**
 * Abstract state class that handles Generic Message States
 */
class GenericMessageState extends MeshMessageState {

    UUID mLabel;

    /**
     * Constructs the generic message state
     *
     * @param src           Source address
     * @param dst           Destination address
     * @param meshMessage   {@link MeshMessage} to be sent
     * @param meshTransport {@link MeshTransport} transport
     * @param callbacks     {@link InternalMeshMsgHandlerCallbacks} callbacks
     * @throws IllegalArgumentException if src or dst address is invalid
     */
    GenericMessageState(final int src,
                        final int dst,
                        final MeshMessage meshMessage,
                        final MeshTransport meshTransport,
                        final InternalMeshMsgHandlerCallbacks callbacks) throws IllegalArgumentException {
        this(src, dst, null, meshMessage, meshTransport, callbacks);
    }

    /**
     * Constructs the generic message state
     *
     * @param src           Source address
     * @param dst           Destination address
     * @param label         Label UUID of destination address
     * @param meshMessage   {@link MeshMessage} to be sent
     * @param meshTransport {@link MeshTransport} transport
     * @param callbacks     {@link InternalMeshMsgHandlerCallbacks} callbacks
     * @throws IllegalArgumentException if src or dst address is invalid
     */
    GenericMessageState(final int src,
                        final int dst,
                        /* @Nullable */ final UUID label,
                        final MeshMessage meshMessage,
                        final MeshTransport meshTransport,
                        final InternalMeshMsgHandlerCallbacks callbacks) throws IllegalArgumentException {
        super(meshMessage, meshTransport, callbacks);
        this.mSrc = src;
        if (!MeshAddress.isAddressInRange(src)) {
            throw new IllegalArgumentException("Invalid address, a source address must be a valid 16-bit value!");
        }
        this.mDst = dst;
        if (!MeshAddress.isAddressInRange(dst)) {
            throw new IllegalArgumentException("Invalid address, a destination address must be a valid 16-bit value");
        }
        mLabel = label;
        createAccessMessage();
    }

    /**
     * Creates the access message to be sent
     */
    protected synchronized void createAccessMessage() {
        final GenericMessage genericMessage = (GenericMessage) mMeshMessage;
        final ApplicationKey key = genericMessage.getAppKey();
        final int akf = genericMessage.getAkf();
        final int aid = genericMessage.getAid();
        final int aszmic = genericMessage.getAszmic();
        final int opCode = genericMessage.getOpCode();
        final byte[] parameters = genericMessage.getParameters();
        message = mMeshTransport.createMeshMessage(mSrc, mDst, mLabel, genericMessage.messageTtl,
            key, akf, aid, aszmic, opCode, parameters);
        genericMessage.setMessage(message);
    }

    @Override
    public synchronized MessageState getState() {
        return MessageState.GENERIC_MESSAGE_STATE;
    }
}

