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

import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress.UNASSIGNED_ADDRESS;

public class HeartbeatSubscription extends Heartbeat {


    private final int src;
    private int minHops;
    private int maxHops;

    /**
     * Heartbeat subscription.
     *
     * @param src       Source address for Heartbeat messages.
     * @param dst       Destination address for Heartbeat messages.
     * @param periodLog Remaining period for processing Heartbeat messages.
     * @param countLog  Number of Heartbeat messages received.
     * @param minHops   Minimum hops when receiving Heartbeat messages.
     * @param maxHops   Maximum hops when receiving Heartbeat messages.
     */
    public HeartbeatSubscription(final int src, final int dst, final byte periodLog, final byte countLog, final int minHops, final int maxHops) {
        super(dst, periodLog, countLog);
        this.src = src;
        this.minHops = minHops;
        this.maxHops = maxHops;
    }

    @Override
    public String toString() {
        return "Source address: " + Integer.toHexString(src) +
            "\nDestination address: " + Integer.toHexString(dst) +
            "\nPeriod Log: " + Integer.toHexString(periodLog) +
            "\nCount Log: " + Integer.toHexString(countLog) +
            "\nMin Hops: " + minHops +
            "\n Max Hops: " + maxHops;
    }

    /**
     * Returns the source address.
     */
    public int getSrc() {
        return src;
    }

    /**
     * Returns the minimum number of hopes when receiving heartbeat messages.
     */
    public int getMinHops() {
        return minHops;
    }

    /**
     * Returns the maximum number of hopes when receiving heartbeat messages.
     */
    public int getMaxHops() {
        return maxHops;
    }

    /**
     * Returns true if the heartbeat subscriptions are enabled.
     */
    public boolean isEnabled() {
        return src != UNASSIGNED_ADDRESS && dst != UNASSIGNED_ADDRESS;
    }

    public String getCountLogDescription() {
        if (countLog == 0x00 || countLog == 0x01)
            return String.valueOf(countLog);
        else if (countLog >= 0x02 && countLog <= 0x10) {
            final int lowerBound = (int) (Math.pow(2, countLog - 1));
            final int upperBound = Math.min(0xFFFE, (int) (Math.pow(2, countLog)) - 1);
            return lowerBound + " ... " + upperBound;
        } else {
            return "More than 65534"; //0xFFFE
        }
    }

    public String getPeriodLogDescription() {
        if (periodLog == 0x00)
            return "Disabled";
        else if (periodLog == 0x01)
            return "1";
        else if (periodLog >= 0x02 && periodLog < 0x11) {
            final int lowerBound = (int) (Math.pow(2, periodLog - 1));
            final int upperBound = (int) (Math.pow(2, periodLog) - 1);
            return periodToTime(lowerBound) + " ... " + periodToTime(upperBound);
        } else if (periodLog == 0x11)
            return "65535";
        else return "Invalid";
    }

    public Short getPeriodLog2Period() {
        if (periodLog == 0x00)
            return 0x0000;
        else if (periodLog >= 0x01 && periodLog <= 0x10) {
            return (short) (Math.pow(2, periodLog - 1));
        } else if (periodLog == 0x11)
            return (short) 0xFFFF;
        else throw new IllegalArgumentException("Period Log out of range");
    }
}

