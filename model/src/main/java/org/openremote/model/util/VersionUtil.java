/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.model.util;

import java.util.Objects;

public class VersionUtil {

    // Compare two version strings like "1.2.3" by splitting on '.'
    // Returns a negative value if v1 < v2, zero if equal, positive if v1 > v2.
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

    // Ignore trailing non-digit qualifiers, for example "1-SNAPSHOT" -> 1.
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
