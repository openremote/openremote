/*
 * Copyright 2017, OpenRemote Inc.
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

import java.util.Optional;

public final class EnumUtil {
    private EnumUtil() {}

    @SuppressWarnings("unchecked")
    public static <T> T enumToValue(Enum<?> enumValue, Class<T> clazz) {

        if (String.class == clazz) {
            return (T)enumValue.name();
        }
        if (Integer.class == clazz) {
            return (T)Integer.valueOf(enumValue.ordinal());
        }
        if (Double.class == clazz) {
            return (T)Double.valueOf(enumValue.ordinal());
        }

        return null;
    }

    public static <T extends Enum<T>> Optional<T> enumFromValue(Class<T> enumClazz, Object value) {
        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof String) {
            return enumFromString(enumClazz, (String)value);
        }

        if (Number.class.isAssignableFrom(value.getClass())) {
            return enumFromInteger(enumClazz, ((Number)value).intValue());
        }

        return Optional.empty();
    }

    public static <T extends Enum<T>> Optional<T> enumFromString(Class<T> enumClazz, String value) {
        for (T enumValue : enumClazz.getEnumConstants()) {
            if (enumValue.name().equalsIgnoreCase(value)) {
                return Optional.of(enumValue);
            }
        }

        return Optional.empty();
    }

    public static <T extends Enum<T>> Optional<T> enumFromInteger(Class<T> enumClazz, int ordinal) {
            for (T enumValue : enumClazz.getEnumConstants()) {
                if (enumValue.ordinal() == ordinal) {
                    return Optional.of(enumValue);
                }
            }
            return Optional.empty();
    }
}
