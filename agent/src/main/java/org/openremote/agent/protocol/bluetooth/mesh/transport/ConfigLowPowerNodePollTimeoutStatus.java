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

/**
 * Creates the ConfigLowPowerNodePollTimeoutStatus message.
 */
public class ConfigLowPowerNodePollTimeoutStatus extends ConfigStatusMessage {

    private static final String TAG = ConfigLowPowerNodePollTimeoutStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_LOW_POWER_NODE_POLLTIMEOUT_STATUS;

    private int address;
    private int pollTimeout;

    /**
     * Constructs ConfigLowPowerNodePollTimeoutStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigLowPowerNodePollTimeoutStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        address = MeshParserUtils.unsignedBytesToInt(mParameters[0], mParameters[1]);
        pollTimeout = MeshParserUtils.convert24BitsToInt(new byte[] {mParameters[2], mParameters[3], mParameters[4]});
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the Unicast address of the low power node
     */
    public int getAddress() {
        return address;
    }

    /**
     * Returns the Poll Timeout value for Low Power Node.
     */
    public int getPollTimeout() {
        return pollTimeout;
    }
}

