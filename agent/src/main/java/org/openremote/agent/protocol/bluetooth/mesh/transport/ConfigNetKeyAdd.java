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

import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public class ConfigNetKeyAdd extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNetKeyAdd.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETKEY_ADD;

    private final NetworkKey mNetKey;

    /**
     * Constructs ConfigNetKeyAdd message.
     *
     * @param networkKey Network key for this message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigNetKeyAdd(final NetworkKey networkKey) throws IllegalArgumentException {
        if (networkKey.getKey().length != 16)
            throw new IllegalArgumentException("Network key must be 16 bytes");

        this.mNetKey = networkKey;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }


    @Override
    void assembleMessageParameters() {
        LOG.info("NetKeyIndex: " + mNetKey.getKeyIndex());
        final byte[] netKeyIndex = MeshParserUtils.addKeyIndexPadding(mNetKey.getKeyIndex());

        final ByteBuffer paramsBuffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
        paramsBuffer.put(netKeyIndex[1]);
        paramsBuffer.put((byte) ((netKeyIndex[0] & 0xFF) & 0x0F));
        paramsBuffer.put(mNetKey.getKey());
        mParameters = paramsBuffer.array();
    }
}

