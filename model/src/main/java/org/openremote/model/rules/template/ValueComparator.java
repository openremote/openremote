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
package org.openremote.model.rules.template;

import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;

public enum ValueComparator {

    EQUALS((value, caseSensitive) -> {
        switch (value.getType()) {
            case STRING:
                return caseSensitive
                    ? "valueAsString == \"" + escapeString(Values.getString(value).get()) + "\""
                    : "(valueAsString.equalsIgnoreCase(\"" + escapeString(Values.getString(value).get()) + "\")";
            case NUMBER:
                return "valueAsNumber != " + Values.getNumber(value).get();
            default:
                return "";
        }
    },
        ValueType.STRING, ValueType.NUMBER),
    NOT_EQUALS((value, caseSensitive) -> {
        switch (value.getType()) {
            case STRING:
                return caseSensitive
                    ? "valueAsString != \"" + escapeString(Values.getString(value).get()) + "\""
                    : "!(valueAsString.equalsIgnoreCase(\"" + escapeString(Values.getString(value).get()) + "\")";
            case NUMBER:
                return "valueAsNumber != " + Values.getNumber(value).get();
            default:
                return "";
        }
    }, ValueType.STRING, ValueType.NUMBER),
    IS_TRUE((value, caseSensitive) -> {
        switch (value.getType()) {
            case BOOLEAN:
                return "valueAsBoolean == true";
            default:
                return "";
        }
    }, ValueType.BOOLEAN),
    IS_FALSE((value, caseSensitive) -> {
        switch (value.getType()) {
            case BOOLEAN:
                return "valueAsBoolean == false";
            default:
                return "";
        }
    }, ValueType.BOOLEAN),
    GREATER_THAN((value, caseSensitive) -> {
        switch (value.getType()) {
            case NUMBER:
                return "valueAsNumber > " + Values.getNumber(value).get();
            default:
                return "";
        }
    }, ValueType.NUMBER),
    LESS_THAN((value, caseSensitive) -> {
        switch (value.getType()) {
            case NUMBER:
                return "valueAsNumber < " + Values.getNumber(value).get();
            default:
                return "";
        }
    }, ValueType.NUMBER),
    GREATER_EQUAL_THAN((value, caseSensitive) -> {
        switch (value.getType()) {
            case NUMBER:
                return "valueAsNumber >= " + Values.getNumber(value).get();
            default:
                return "";
        }
    }, ValueType.NUMBER),
    LESS_EQUAL_THAN((value, caseSensitive) -> {
        switch (value.getType()) {
            case NUMBER:
                return "valueAsNumber <= " + Values.getNumber(value).get();
            default:
                return "";
        }
    }, ValueType.NUMBER);

    final protected ValueType[] applicableTypes;
    final protected BiFunction<Value, Boolean, String> renderFunction;

    ValueComparator(BiFunction<Value, Boolean, String> renderFunction, ValueType... applicableTypes) {
        this.applicableTypes = applicableTypes;
        this.renderFunction = renderFunction;
    }

    protected boolean isApplicableType(ValueType valueType) {
        for (ValueType applicableType : applicableTypes) {
            if (applicableType == valueType)
                return true;
        }
        return false;
    }

    static protected String escapeString(String value) {
        // Escape double quotes, remove control chars
        return value.replaceAll("\"", "\\\\\"").replaceAll("\\p{Cc}", "");
    }

    public boolean isValid(Value operand) {
        Objects.requireNonNull(operand);
        return isApplicableType(operand.getType());
    }

    public String render(Value operand, boolean caseSensitive) {
        Objects.requireNonNull(operand);
        return renderFunction.apply(operand, caseSensitive);
    }

    public static ValueComparator fromString(String value) {
        return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
    }

}
