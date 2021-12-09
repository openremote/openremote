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

import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the ConfigNetworkTransmitStatus message.
 */
public final class ConfigNetworkTransmitStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNetworkTransmitStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETWORK_TRANSMIT_STATUS;
    private int mNetworkTransmitCount;
    private int mNetworkTransmitIntervalSteps;

    /**
     * Constructs a ConfigNetworkTransmitStatus message.
     *
     * @param message Access message received
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigNetworkTransmitStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    final void parseStatusParameters() {
        final byte[] payload = ((AccessMessage) mMessage).getAccessPdu();

        mNetworkTransmitCount = payload[2] & 0b111;
        mNetworkTransmitIntervalSteps = (payload[2] >> 3) & 0b11111;
    }

    /**
     * Returns the Network Transmit Count set in this message
     *
     * @return Network Transmit Count
     */
    public int getNetworkTransmitCount() {
        return mNetworkTransmitCount;
    }

    /**
     * Returns the Network Transmit Interval Steps set in this message
     *
     * @return Network Transmit Interval Steps
     */
    public int getNetworkTransmitIntervalSteps() {
        return mNetworkTransmitIntervalSteps;
    }
}
