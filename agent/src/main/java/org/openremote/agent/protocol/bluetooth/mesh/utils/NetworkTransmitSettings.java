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
package org.openremote.agent.protocol.bluetooth.mesh.utils;

import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;

/**
 * Class containing Network Transmit values in a {@link ProvisionedMeshNode}
 */
public class NetworkTransmitSettings {
    private final int networkTransmitCount;
    private final int networkIntervalSteps;

    /**
     * Constructs {@link NetworkTransmitSettings}
     *
     * @param networkTransmitCount Number of transmissions for each Network PDU originating from the node
     * @param networkIntervalSteps Number of 10-millisecond steps between transmissions
     */
    public NetworkTransmitSettings(final int networkTransmitCount, final int networkIntervalSteps) {
        if (networkTransmitCount < 1 || networkTransmitCount > 8) {
            throw new IllegalArgumentException("Network Transmit count must be in range 1-8.");
        }
        this.networkTransmitCount = networkTransmitCount;
        this.networkIntervalSteps = networkIntervalSteps;
    }

    /**
     * Returns the network transmit count
     */
    public int getNetworkTransmitCount() {
        return networkTransmitCount;
    }

    /**
     * Returns the number of transmissions.
     */
    public int getTransmissionCount() {
        return networkTransmitCount + 1;
    }

    /**
     * Returns the network interval steps
     */
    public int getNetworkIntervalSteps() {
        return networkIntervalSteps;
    }

    /**
     * Returns the Network transmission interval.
     */
    public int getNetworkTransmissionInterval() {
        return (networkIntervalSteps + 1) * 10;
    }

    /**
     * Decodes the Network Transmission Interval steps
     * @param interval Interval between 10-320 ms with a step of 10 ms
     * @return the interval as steps
     * @throws IllegalArgumentException if the Network Transmission Interval is not 10-320 ms with a step of 10 ms
     */
    public static int decodeNetworkTransmissionInterval(final int interval) {
        if ((interval >= 10 && interval <= 320) && (interval % 10 != 0))
            throw new IllegalArgumentException("Network Transmission Interval must be 10-320 ms with a step of 10 ms");
        return (interval / 10) - 1;
    }
}
