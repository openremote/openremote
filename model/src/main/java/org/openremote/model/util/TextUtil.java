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
package org.openremote.model.util;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TextUtil {

    public static final String REGEXP_PATTERN_INTEGER = "^(-?[1-9]\\d*|0)$";
    public static final String REGEXP_PATTERN_INTEGER_POSITIVE = "^\\d+$";
    public static final String REGEXP_PATTERN_INTEGER_NEGATIVE = "^(-[1-9]\\d*|0)$";
    public static final String REGEXP_PATTERN_INTEGER_NON_ZERO = "^-?[1-9]\\d*$";
    public static final String REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO = "^[1-9]\\d*$";
    public static final String REGEXP_PATTERN_INTEGER_NEGATIVE_NON_ZERO = "^-[1-9]\\d*$";
    public static final String REGEXP_PATTERN_DOUBLE = "^(-?([0-9]+$(\\.[0-9]+)|[1-9]\\d*)|0)$";
    public static final String REGEXP_PATTERN_DOUBLE_POSITIVE = "^(([0-9]+(\\.[0-9]+)|[1-9]\\d*)|0)$";
    public static final String REGEXP_PATTERN_DOUBLE_NEGATIVE = "^(-([0-9]+(\\.[0-9]+)|[1-9]\\d*)|0)$";
    public static final String REGEXP_PATTERN_DOUBLE_NON_ZERO = "^-?(?!0*(\\.0+)?$)([0-9]+\\.[0-9]+|[1-9]\\d*)$";
    public static final String REGEXP_PATTERN_DOUBLE_POSITIVE_NON_ZERO = "^(?!0*(\\.0+)?$)([0-9]+\\.[0-9]+|[1-9]\\d*)$";
    public static final String REGEXP_PATTERN_DOUBLE_NEGATIVE_NON_ZERO = "^-(?!0*(\\.0+)?$)([0-9]+\\.[0-9]+|[1-9]\\d*)$";
    public static final String REGEXP_PATTERN_BASIC_HTTP_URL = "^(http|https)://.+$";
    public static final String REGEXP_PATTERN_STRING_NON_EMPTY = "(.|\\s)*\\S(.|\\s)*";
    public static final String REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE = "^\\S+$";
    public static final Pattern REGEXP_INTEGER_POSITIVE = Pattern.compile(REGEXP_PATTERN_INTEGER_POSITIVE);
    public static final Predicate<String> INTEGER_POSITIVE_VALIDATOR = name -> !isNullOrEmpty(name) && REGEXP_INTEGER_POSITIVE.matcher(name).matches();

    // TODO Unused, doesn't work for "0 30 8 ? * FRI *
    public static final String REGEXP_PATTERN_CRON_EXPRESSION = "^\\\\s*($|#|\\\\w+\\\\s*=|(\\\\?|\\\\*|(?:[0-5]?\\\\d)(?:(?:-|\\/|\\\\,)(?:[0-5]?\\\\d))?(?:,(?:[0-5]?\\\\d)(?:(?:-|\\/|\\\\,)(?:[0-5]?\\\\d))?)*)\\\\s+(\\\\?|\\\\*|(?:[0-5]?\\\\d)(?:(?:-|\\/|\\\\,)(?:[0-5]?\\\\d))?(?:,(?:[0-5]?\\\\d)(?:(?:-|\\/|\\\\,)(?:[0-5]?\\\\d))?)*)\\\\s+(\\\\?|\\\\*|(?:[01]?\\\\d|2[0-3])(?:(?:-|\\/|\\\\,)(?:[01]?\\\\d|2[0-3]))?(?:,(?:[01]?\\\\d|2[0-3])(?:(?:-|\\/|\\\\,)(?:[01]?\\\\d|2[0-3]))?)*)\\\\s+(\\\\?|\\\\*|(?:0?[1-9]|[12]\\\\d|3[01])(?:(?:-|\\/|\\\\,)(?:0?[1-9]|[12]\\\\d|3[01]))?(?:,(?:0?[1-9]|[12]\\\\d|3[01])(?:(?:-|\\/|\\\\,)(?:0?[1-9]|[12]\\\\d|3[01]))?)*)\\\\s+(\\\\?|\\\\*|(?:[1-9]|1[012])(?:(?:-|\\/|\\\\,)(?:[1-9]|1[012]))?(?:L|W)?(?:,(?:[1-9]|1[012])(?:(?:-|\\/|\\\\,)(?:[1-9]|1[012]))?(?:L|W)?)*|\\\\?|\\\\*|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?(?:,(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?)*)\\\\s+(\\\\?|\\\\*|(?:[0-6])(?:(?:-|\\/|\\\\,|#)(?:[0-6]))?(?:L)?(?:,(?:[0-6])(?:(?:-|\\/|\\\\,|#)(?:[0-6]))?(?:L)?)*|\\\\?|\\\\*|(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?(?:,(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?)*)(|\\\\s)+(\\\\?|\\\\*|(?:|\\\\d{4})(?:(?:-|\\/|\\\\,)(?:|\\\\d{4}))?(?:,(?:|\\\\d{4})(?:(?:-|\\/|\\\\,)(?:|\\\\d{4}))?)*))$";

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
            sb.deleteCharAt(sb.length() - 1);
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
                // Ignore
            }
        }
        return null;
    }

    /**
     * Transforms <code>EXFooBar123</code> into <code>ex-foo-bar-123</code> and
     * <code>attributeX</code> into <code>attribute-x</code> without regex.
     */
    public static String toLowerCaseDash(String camelCase) {
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
                } else if (i < chars.length - 1 && Character.isLowerCase(chars[i + 1])) {
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

    private final static String PROPORTIONAL_FONT_THIN_CHARS = "[^iIl1\\.,']";

    private static int textWidth(String str) {
        return str.length() - str.replaceAll(PROPORTIONAL_FONT_THIN_CHARS, "").length() / 2;
    }

    public static String ellipsize(String text, int max) {
        if (textWidth(text) <= max)
            return text;
        // Start by chopping off at the word before max
        // This is an over-approximation due to thin-characters...
        int end = text.lastIndexOf(' ', max - 3);
        // Just one long word. Chop it off.
        if (end == -1)
            return text.substring(0, max - 3) + "...";
        // Step forward as long as textWidth allows.
        int newEnd = end;
        do {
            end = newEnd;
            newEnd = text.indexOf(' ', end + 1);

            // No more spaces.
            if (newEnd == -1)
                newEnd = text.length();
        } while (textWidth(text.substring(0, newEnd) + "...") < max);
        return text.substring(0, end) + "...";
    }

    public static String pad(String s, int size) {
        while (s.length() < size) {
            s = s + " ";
        }
        return s;
    }

    public static String truncate(String s, int maxLength, boolean alignRight) {
        if (s.length() <= maxLength)
            return s;

        return alignRight
            ? s.substring(s.length() - maxLength, s.length())
            : s.substring(0, maxLength);
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static Optional<String> asNonNullAndNonEmpty(String str) {
        return isNullOrEmpty(str) ? Optional.empty() : Optional.of(str);
    }

    public static String requireNonNullAndNonEmpty(String str) {
        return requireNonNullAndNonEmpty(str, "String cannot be empty");
    }

    public static String requireNonNullAndNonEmpty(String str, String exceptionMessage) {
        Objects.requireNonNull(str);

        if (isNullOrEmpty(str)) {
            throw new IllegalArgumentException(exceptionMessage);
        }

        return str;
    }

    public static String toLowerCamelCase(String s) {
        return toCamelCase(s, false, false);
    }

    public static String toUpperCamelCase(String s) {
        return toCamelCase(s, true, false);
    }

    private static String toCamelCase(String s, boolean upper, boolean convertUnderscoreToSpace) {
        String[] parts = s.split("_");
        StringBuilder camelCaseString = new StringBuilder();
        int i=0;
        for (String part : parts) {
            if (!isNullOrEmpty(part)) {
                if (!upper && i == 0) {
                    camelCaseString.append(s.toLowerCase(Locale.ROOT));
                } else {
                    camelCaseString.append(toProperCase(part));
                }
                if (convertUnderscoreToSpace && i>0) {
                    camelCaseString.append(" ");
                }
                i++;
            }
        }
        return camelCaseString.toString();
    }

    public static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
            s.substring(1).toLowerCase();
    }

    public static String toProperCase(String s, boolean convertUnderscoreToSpace) {
        return toCamelCase(s, true, convertUnderscoreToSpace);
    }

    public static Optional<Integer> asInteger(String str) {
        if (isNullOrEmpty(str)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(str));
        } catch(NumberFormatException e) {
            return Optional.empty();
        }
    }
}
