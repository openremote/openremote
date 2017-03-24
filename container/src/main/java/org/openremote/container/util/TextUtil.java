/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.util;

public class TextUtil {

    /**
     * Converts a String which represents a pollingInterval into an int which can be used as delay
     * for Thread.sleep();  <p>
     * <p>
     * Following conversion will happen:  <p>
     * <p>
     * null || "" = -1                  <br>
     * 500 = 500                        <br>
     * 1s = 1000 * 1 = 1000             <br>
     * 1m = 1000 * 60 = 60000           <br>
     * 1h = 1000 * 60 * 60 = 3600000    <br>
     *
     * @param pollingInterval interval string, such as '1m' as millisecond value
     * @return interval in milliseconds
     */
    public static int convertPollingIntervalString(String pollingInterval) {
        if ((pollingInterval != null) && (pollingInterval.trim().length() != 0)) {
            pollingInterval = pollingInterval.trim();
            char lastChar = pollingInterval.charAt(pollingInterval.length() - 1);
            String timePart = pollingInterval.substring(0, pollingInterval.length() - 1);

            switch (lastChar) {
                case 's':
                    return Integer.parseInt(timePart) * 1000;
                case 'm':
                    return Integer.parseInt(timePart) * 1000 * 60;
                case 'h':
                    return Integer.parseInt(timePart) * 1000 * 60 * 60;
                default:
                    return Integer.parseInt(pollingInterval);
            }
        }

        return -1;
    }

    public static String toCommaSeparated(String... strings) {
        if (strings == null || strings.length == 0)
            return null;
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(s).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

    public static String[] fromCommaSeparated(String commaWords) {
        if (commaWords == null || commaWords.length() == 0)
            return new String[0];
        return commaWords.split(",");
    }

    /**
     * Get an enum value from a string without throwing exception.
     * Enum should be in uppercase to use this method.
     */
    public static <T extends Enum<T>> T getEnumFromString(Class<T> c, String string) {
        if (c != null && string != null) {
            try {
                return Enum.valueOf(c, string.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
            }
        }
        return null;
    }

}
