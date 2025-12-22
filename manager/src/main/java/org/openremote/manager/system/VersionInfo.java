package org.openremote.manager.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class VersionInfo {

    public static final String VERSION = loadVersion();
    public static final String GATEWAY_API_VERSION = "1.0.0";

    public static String getManagerVersion() {
        return VERSION;
    }

    public static String getGatewayApiVersion() {
        return GATEWAY_API_VERSION;
    }

    protected static String loadVersion() {
        try (InputStream resourceStream = VersionInfo.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (resourceStream != null) {
                Properties versionProps = new Properties();
                versionProps.load(resourceStream);
                return versionProps.getProperty("version");
            } else {
                throw new RuntimeException("Could not load version.properties");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Compare two version strings like "1.2.3" by splitting on '.'
    // Returns a negative value if v1 < v2, zero if equal, positive if v1 > v2
    public static int compareVersions(String v1, String v2) {
        Objects.requireNonNull(v1, "v1 must not be null");
        Objects.requireNonNull(v2, "v2 must not be null");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length && !parts1[i].isEmpty() ? parsePart(parts1[i]) : 0;
            int p2 = i < parts2.length && !parts2[i].isEmpty() ? parsePart(parts2[i]) : 0;

            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    // Convenience methods
    public static boolean isVersionGreater(String v1, String v2) {
        return compareVersions(v1, v2) > 0;
    }

    public static boolean isVersionGreaterOrEqual(String v1, String v2) {
        return compareVersions(v1, v2) >= 0;
    }

    public static boolean isVersionEqual(String v1, String v2) {
        return compareVersions(v1, v2) == 0;
    }

    public static boolean isVersionLess(String v1, String v2) {
        return compareVersions(v1, v2) < 0;
    }

    public static boolean isVersionLessOrEqual(String v1, String v2) {
        return compareVersions(v1, v2) <= 0;
    }

    // Helper that safely parses a numeric version segment, ignoring any trailing
    // non-digit qualifiers (e.g. "1-SNAPSHOT" -> 1)
    protected static int parsePart(String part) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(part.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
