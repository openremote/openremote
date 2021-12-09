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

public abstract class Heartbeat {
    public static final int DO_NOT_SEND_PERIODICALLY = 0x00;
    public static final int PERIOD_MIN = 0x0000;
    public static final int PERIOD_MAX = 0x10000;
    public static final int PERIOD_LOG_MIN = 0x01;
    public static final int PERIOD_LOG_MAX = 0x11;
    public static final int COUNT_MIN = 0x00;
    public static final int COUNT_MAX = 0x11;
    public static final int SEND_INDEFINITELY = 0xFF;
    public static final int DEFAULT_PUBLICATION_TTL = 0x05;


    protected int dst;
    protected byte periodLog;
    protected byte countLog;

    Heartbeat(final int dst, final byte periodLog, final byte countLog) {
        this.dst = dst;
        this.periodLog = periodLog;
        this.countLog = countLog;
    }

    /**
     * Returns true if the heartbeats are enabled.
     */
    public abstract boolean isEnabled();

    /**
     * Returns the destination address.
     */
    public int getDst() {
        return dst;
    }

    /**
     * Returns the period for processing.
     */
    public byte getPeriodLog() {
        return periodLog;
    }

    /**
     * Calculates the heart beat period interval in seconds
     *
     * @param periodLog period value
     */
    public static int calculateHeartbeatPeriod(final short periodLog) {
        return (int) Math.pow(2, periodLog - 1);
    }

    /**
     * Decodes the period and returns the period log value
     *
     * @param period period value
     */
    public static byte decodeHeartbeatPeriod(final int period) {
        switch (period) {
            case 0x0000:
                return 0x00;
            case 0xFFFF:
                return 0x11;
            default:
                return log2(period);
        }
    }

    private static byte log2(final int period) {
        return (byte) ((Math.log(period) / Math.log(2)) + 1);
    }

    /**
     * Validates heart beat period.
     *
     * @param period Heartbeat publication period.
     * @return true if valid or false otherwise.
     * @throws IllegalArgumentException if the value does not range from 0 to 17.
     */
    public static boolean isValidHeartbeatPeriod(final int period) {
        if (period >= 0x0000 && period < 0xFFFF)
            return true;
        throw new IllegalArgumentException("Period must be within the range of 0x0000 to 0xFFFF!");
    }

    /**
     * Validates heart beat period log.
     *
     * @param period Heartbeat publication period.
     * @return true if valid or false otherwise.
     * @throws IllegalArgumentException if the value does not range from 0 to 17.
     */
    public static boolean isValidHeartbeatPeriodLog(final byte period) {
        if (period >= 0x00 && period < 0x11)
            return true;
        throw new IllegalArgumentException("Period log must be within the range of 0x00 to 0x11!");
    }

    public byte getCountLog() {
        return countLog;
    }

    /**
     * Calculates the heart beat publication count which is the number of publications to be sent
     *
     * @param countLog count value
     */
    public static int calculateHeartbeatCount(final int countLog) {
        if (countLog > 0x11 && countLog < 0xFF)
            throw new IllegalArgumentException("Prohibited, count log must be a value from 0x00 to 0x11 and 0xFF");
        if (countLog == 0x11)
            return (int) Math.pow(2, countLog - 1) - 2;
        return (int) Math.pow(2, countLog - 1);
    }

    /**
     * Converts the perio to time
     *
     * @param seconds PeriodLog
     */
    public static String periodToTime(final int seconds) {
        if (seconds == 1)
            return seconds + " second";
        else if (seconds > 1 && seconds < 60) {
            return seconds + " seconds";
        } else if (seconds >= 60 && seconds < 3600) {
            return seconds / 60 + " min " + (seconds % 60) + " sec";
        } else if (seconds >= 3600 && seconds <= 65535) {
            return seconds / 3600 + " h " + ((seconds % 3600) / 60) + " min " + (seconds % 3600 % 60) + " sec";
        } else
            return seconds / 3600 + " h " + ((seconds % 3600) / 60) + " min " + ((seconds % 3600 % 60) - 1) + " sec";
    }
}

