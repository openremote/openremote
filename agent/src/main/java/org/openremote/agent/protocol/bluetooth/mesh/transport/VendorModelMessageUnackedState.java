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

import java.util.UUID;
import java.util.logging.Logger;

class VendorModelMessageUnackedState extends GenericMessageState {

    public static final Logger LOG = Logger.getLogger(VendorModelMessageUnackedState.class.getName());

    /**
     * Constructs {@link VendorModelMessageAckedState}
     *
     * @param src                       Source address
     * @param dst                       Destination address to which the message must be sent to
     * @param vendorModelMessageUnacked Wrapper class {@link VendorModelMessageStatus} containing the
     *                                  opcode and parameters for {@link VendorModelMessageStatus} message
     * @param callbacks                 {@link InternalMeshMsgHandlerCallbacks} for internal callbacks
     * @throws IllegalArgumentException exception for invalid arguments
     */
    VendorModelMessageUnackedState(final int src,
                                   final int dst,
                                   final VendorModelMessageUnacked vendorModelMessageUnacked,
                                   final MeshTransport meshTransport,
                                   final InternalMeshMsgHandlerCallbacks callbacks) throws IllegalArgumentException {
        this(src, dst, null, vendorModelMessageUnacked, meshTransport, callbacks);
    }

    /**
     * Constructs {@link VendorModelMessageAckedState}
     *
     * @param src                       Source address
     * @param dst                       Destination address to which the message must be sent to
     * @param label                     Label UUID of destination address
     * @param vendorModelMessageUnacked Wrapper class {@link VendorModelMessageStatus} containing the
     *                                  opcode and parameters for {@link VendorModelMessageStatus} message
     * @param callbacks                 {@link InternalMeshMsgHandlerCallbacks} for internal callbacks
     * @throws IllegalArgumentException exception for invalid arguments
     */
    VendorModelMessageUnackedState(final int src,
                                   final int dst,
                                   /* @Nullable */ UUID label,
                                   final VendorModelMessageUnacked vendorModelMessageUnacked,
                                   final MeshTransport meshTransport,
                                   final InternalMeshMsgHandlerCallbacks callbacks) throws IllegalArgumentException {
        super(src, dst, vendorModelMessageUnacked, meshTransport, callbacks);
    }

    @Override
    public synchronized MeshMessageState.MessageState getState() {
        return MessageState.VENDOR_MODEL_UNACKNOWLEDGED_STATE;
    }

    @Override
    protected synchronized final void createAccessMessage() {
        final VendorModelMessageUnacked message = (VendorModelMessageUnacked) mMeshMessage;
        final ApplicationKey key = message.getAppKey();
        final int akf = message.getAkf();
        final int aid = message.getAid();
        final int aszmic = message.getAszmic();
        final int opCode = message.getOpCode();
        final byte[] parameters = message.getParameters();
        final int companyIdentifier = message.getCompanyIdentifier();
        this.message = mMeshTransport.createVendorMeshMessage(companyIdentifier, mSrc, mDst, mLabel, message.messageTtl,
            key, akf, aid, aszmic, opCode, parameters);
        message.setMessage(this.message);
    }

    @Override
    public synchronized void executeSend() {
        LOG.info("Sending acknowledged vendor model message");
        super.executeSend();
    }
}

