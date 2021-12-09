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

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 * Creates the ConfigNodeIdentityStatus message.
 */
public class ConfigNodeIdentityStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNodeIdentityStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_BEACON_STATUS;
    private int netKeyIndex;
    /* @NodeIdentityState */
    private int nodeIdentityState;

    /**
     * Constructs ConfigNodeIdentityStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigNodeIdentityStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        mStatusCode = mParameters[0];
        final byte[] netKeyIndex = new byte[]{(byte) (mParameters[2] & 0x0F), mParameters[1]};
        this.netKeyIndex = ByteBuffer.wrap(netKeyIndex).order(ByteOrder.BIG_ENDIAN).getShort();
        nodeIdentityState = MeshParserUtils.unsignedByteToInt(mParameters[3]);
        LOG.info("Status: " + mStatusCode);
        LOG.info("Node Identity State: " + nodeIdentityState);
    }


    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns if the message was successful.
     *
     * @return true if the message was successful or false otherwise.
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
    }

    /**
     * Returns the NodeIdentityState
     */
    /* @NodeIdentityState */
    public int getNodeIdentityState() {
        return nodeIdentityState;
    }

    /**
     * Returns the net key index.
     */
    public int getNetKeyIndex() {
        return netKeyIndex;
    }
}

