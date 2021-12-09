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

import org.openremote.agent.protocol.bluetooth.mesh.Features;

import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress.UNASSIGNED_ADDRESS;

public class HeartbeatPublication extends Heartbeat {

    private final int ttl;
    private final Features features;
    private final int netKeyIndex;

    /**
     * Heartbeat subscription.
     *
     * @param dst         Destination address identifies the Heartbeat Publication
     *                    destination where the address can only be an unassigned address,
     *                    unicast address or a group address. All other values are prohibited.
     * @param countLog    Number of Heartbeat messages to be sent.
     * @param periodLog   Period for sending Heartbeat messages.
     * @param ttl         TTL to be used when sending Heartbeat messages.
     * @param features    Bit field indicating features that trigger Heartbeat messages when changed.
     * @param netKeyIndex Net key index.
     */
    public HeartbeatPublication(final int dst,
                                final byte countLog,
                                final byte periodLog,
                                final int ttl,
                                final Features features,
                                final int netKeyIndex) {
        super(dst, periodLog, countLog);
        this.ttl = ttl;
        this.features = features;
        this.netKeyIndex = netKeyIndex;
    }

    @Override
    public String toString() {
        return "Destination address: " + Integer.toHexString(dst) +
            "\nCount Log: " + Integer.toHexString(countLog) +
            "\nPeriod Log: " + Integer.toHexString(periodLog) +
            "\nTTL: " + ttl +
            "\nFeatures: " + features == null ? "null" : features.toString() +
            "\nNet key index: " + Integer.toHexString(netKeyIndex);
    }

    public int getPeriod(){
        return calculateHeartbeatPeriod(periodLog);
    }

    /**
     * Returns the publication ttl.
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * Returns the features.
     */
    public Features getFeatures() {
        return features;
    }

    /**
     * Returns the Net key index.
     */
    public int getNetKeyIndex() {
        return netKeyIndex;
    }

    /**
     * Returns true if the heartbeat subscriptions are enabled.
     */
    public boolean isEnabled() {
        return dst != UNASSIGNED_ADDRESS;
    }

    public String getPeriodLogDescription() {
        if (periodLog == 0x00)
            return "Disabled";
        else if (periodLog >= 0x01 || periodLog <= 0x11)
            return (int) Math.pow(2, periodLog - 1) + " s";
        else
            return "Prohibited";
    }

    public Short getPeriodLog2Period(final int periodLog) {
        if (periodLog == 0x00)
            return 0x0000;
        else if (periodLog >= 0x01 && periodLog <= 0x10) {
            return (short) (Math.pow(2, periodLog - 1));
        } else return (short) 0xFFFF;
    }

    public String getCountLogDescription() {
        if (countLog == 0x00)
            return "Disabled";
        else if (countLog == 0xFF)
            return "Indefinite";
        else if (countLog == 0x01 || countLog == 0x02)
            return String.valueOf(countLog);
        else if (countLog == 0x11) {
            return "0x8001 ... 0xFFFE";
        } else if (countLog >= 0x03 && countLog <= 0x10) {
            final int lowerBound = (int) (Math.pow(2, countLog - 2)) + 1;
            final int upperBound = (int) Math.pow(2, countLog - 1);
            return lowerBound + " ... " + upperBound;
        } else {
            return "Invalid";
        }
    }
}

