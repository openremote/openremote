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
package org.openremote.manager.shared.util;

import java.util.Locale;

public class Util {

    public static String toLowerCaseDash(String camelCase) {
        // Transforms 'EXFooBar123' into 'ex-foo-bar-123 and "attributeX" into "attribute-x" without regex (GWT!)
        if (camelCase == null)
            return null;
        if (camelCase.length() == 0)
            return camelCase;
        StringBuilder sb = new StringBuilder();
        char[] chars = camelCase.toCharArray();
        boolean inNonLowerCase = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (!Character.isLowerCase(c)) {
                if (!inNonLowerCase) {
                    if (i > 0)
                        sb.append("-");
                } else if (i < chars.length -1 && Character.isLowerCase(chars[i+1])) {
                    sb.append("-");
                }
                inNonLowerCase = true;
            } else {
                inNonLowerCase = false;
            }
            sb.append(c);
        }
        String name = sb.toString();
        name = name.toLowerCase(Locale.ROOT);
        return name;
    }

    /**
     * Get an enum value from a string without throwing exception.
     * Enum should be in uppercase to use this method.
     *
     * @param c
     * @param string
     * @return
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
        if (commaWords == null)
            return new String[0];
        return commaWords.split(",");
    }

}
