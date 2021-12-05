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
