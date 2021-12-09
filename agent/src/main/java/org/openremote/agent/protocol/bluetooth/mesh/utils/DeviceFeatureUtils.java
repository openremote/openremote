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

public class DeviceFeatureUtils {

    /**
     * Checks if relay feature is supported by node;
     *
     * @param feature 16-bit feature value
     * @return true if relay bit = 1 and false if relay bit = 0
     */
    public static boolean supportsRelayFeature(final int feature) {
        return ((feature & (1 << 0)) > 0);
    }

    /**
     * Checks if proxy feature is supported by node;
     *
     * @param feature 16-bit feature value
     * @return true if proxy bit = 1 and false if proxy bit = 0
     */
    public static boolean supportsProxyFeature(final int feature) {
        return ((feature & (1 << 1)) > 0);
    }

    /**
     * Checks if friend feature is supported by node;
     *
     * @param feature 16-bit feature value
     * @return true if friend bit = 1 and false if friend bit = 0
     */
    public static boolean supportsFriendFeature(final int feature) {
        return ((feature & (1 << 2)) > 0);
    }

    /**
     * Checks if low power feature is supported by node;
     *
     * @param feature 16-bit feature value
     * @return true if low power bit = 1 and false if low power bit = 0
     */
    public static boolean supportsLowPowerFeature(final int feature) {
        return ((feature & (1 << 3)) > 0);
    }

    /**
     * Returns the relay feature state value
     *
     * @param feature 16-bit feature value
     */
    public static int getRelayFeature(final int feature) {
        return feature & 1;
    }

    /**
     * Returns the proxy feature state value
     *
     * @param feature 16-bit feature value
     */
    public static int getProxyFeature(final int feature) {
        return (feature >> 1) & 1;
    }

    /**
     * Returns the friend feature state value
     *
     * @param feature 16-bit feature value
     */
    public static int getFriendFeature(final int feature) {
        return (feature >> 2) & 1;
    }

    /**
     * Returns the low power feature state value
     *
     * @param feature 16-bit feature value
     */
    public static int getLowPowerFeature(final int feature) {
        return (feature >> 3) & 1;
    }
}
