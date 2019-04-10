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

import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Optional;

public final class EnumUtil {
    private EnumUtil() {}

    public static Value enumToValue(Enum<?> enumValue, ValueType valueType) {

        switch (valueType) {
            case OBJECT:
                return null;
            case ARRAY:
                return null;
            case STRING:
                return Values.create(enumValue.name());
            case NUMBER:
                return Values.create(enumValue.ordinal());
            case BOOLEAN:
                return null;
        }

        return null;
    }

    public static <T extends Enum<T>> Optional<T> enumFromValue(Class<T> enumClazz, Value value) {
        if (value == null) {
            return Optional.empty();
        }

        switch (value.getType()) {
            case OBJECT:
                return Optional.empty();
            case ARRAY:
                return Optional.empty();
            case STRING:
                return enumFromString(enumClazz, value.toString());
            case NUMBER:
                return Values
                    .getNumber(value)
                    .map(Double::intValue)
                    .flatMap(ordinal -> enumFromInteger(enumClazz, ordinal));
            case BOOLEAN:
                return Optional.empty();
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
