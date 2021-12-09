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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.bytesToHex;
import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.createVendorOpCode;
import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.getOpCode;
import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.getOpCodeLength;


/**
 * AccessLayer implementation of the mesh network architecture as per the mesh profile specification.
 * <p>
 * AccessLayer class generates/parses a raw mesh message containing the specific OpCode and Parameters.
 * </p>
 */
abstract class AccessLayer {

    public static final Logger LOG = Logger.getLogger(AccessLayer.class.getName());
    //protected Context mContext;
    //Handler mHandler;
    ProvisionedMeshNode mMeshNode;

    //protected abstract void initHandler();

    /**
     * Creates an access message
     *
     * @param message Access message containing the required opcodes and parameters to create access message pdu.
     */
    synchronized void createMeshMessage(final Message message) {
        createAccessMessage((AccessMessage) message);
    }

    /**
     * Creates a vendor model access message
     *
     * @param message Access message containing the required opcodes and parameters to create access message pdu.
     */
    synchronized void createVendorMeshMessage(final Message message) {
        createCustomAccessMessage((AccessMessage) message);
    }

    /**
     * Creates an access message
     *
     * @param accessMessage Access message containing the required opcodes and parameters to create access message pdu.
     */
    synchronized final void createAccessMessage(final AccessMessage accessMessage) {
        final byte[] opCodes = getOpCode(accessMessage.getOpCode());
        final byte[] parameters = accessMessage.getParameters();
        final ByteBuffer accessMessageBuffer;
        if (parameters != null) {
            accessMessageBuffer = ByteBuffer.allocate(opCodes.length + parameters.length);
            accessMessageBuffer.put(opCodes).put(parameters);
        } else {
            accessMessageBuffer = ByteBuffer.allocate(opCodes.length);
            accessMessageBuffer.put(opCodes);
        }
        final byte[] accessPdu = accessMessageBuffer.array();

        LOG.info("Created Access PDU " + bytesToHex(accessPdu, false));
        accessMessage.setAccessPdu(accessMessageBuffer.array());
    }

    /**
     * Creates an access message
     *
     * @param accessMessage Access message containing the required opcodes and parameters to create access message pdu.
     */
    synchronized final void createCustomAccessMessage(final AccessMessage accessMessage) {
        final byte[] parameters = accessMessage.getParameters();
        final byte[] vendorOpcode = createVendorOpCode(accessMessage.getOpCode(),
            accessMessage.getCompanyIdentifier());
        final ByteBuffer accessMessageBuffer;
        if (parameters != null) {
            accessMessageBuffer = ByteBuffer.allocate(vendorOpcode.length + parameters.length);
            accessMessageBuffer.put(vendorOpcode);
            accessMessageBuffer.put(parameters);
        } else {
            accessMessageBuffer = ByteBuffer.allocate(vendorOpcode.length);
            accessMessageBuffer.put(vendorOpcode);
        }
        final byte[] accessPdu = accessMessageBuffer.array();
        LOG.info("Created Access PDU " + bytesToHex(accessPdu, false));
        accessMessage.setAccessPdu(accessPdu);
    }

    /**
     * Parse access pdu
     *
     * @param message underlying message containing the access pdu
     */
    synchronized final void parseAccessLayerPDU(final AccessMessage message) {
        //MSB of the first octet defines the length of opcodes.
        //if MSB = 0 length is 1 and so forth
        final byte[] accessPayload = message.getAccessPdu();
        final int opCodeLength = getOpCodeLength(accessPayload[0] & 0xFF);
        message.setOpCode(getOpCode(accessPayload, opCodeLength));
        final int length = accessPayload.length - opCodeLength;
        final ByteBuffer paramsBuffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        paramsBuffer.put(accessPayload, opCodeLength, length);
        message.setParameters(paramsBuffer.array());
        LOG.info("Received Access PDU " + bytesToHex(accessPayload, false));
    }
}