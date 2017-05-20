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
package org.openremote.model.value;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Binary operators.
 */
public enum ValueComparator implements BiFunction<Value, Value, Boolean> {

    EQUALS(applyEquals(false, false), renderEquals(false, false), ValueType.STRING, ValueType.NUMBER, ValueType.BOOLEAN),

    NOT_EQUALS(applyEquals(false, true), renderEquals(false, true), ValueType.STRING, ValueType.NUMBER, ValueType.BOOLEAN),

    EQUALS_IGNORE_CASE(applyEquals(true, false), renderEquals(true, false), ValueType.STRING, ValueType.NUMBER, ValueType.BOOLEAN),

    NOT_EQUALS_IGNORE_CASE(applyEquals(true, true), renderEquals(true, true), ValueType.STRING, ValueType.NUMBER, ValueType.BOOLEAN),

    GREATER_THAN(applyNumberComparison((a, b) -> a > b), renderNumberComparison(">"), ValueType.NUMBER),

    GREATER_EQUAL_THAN(applyNumberComparison((a, b) -> a >= b), renderNumberComparison(">="), ValueType.NUMBER),

    LESS_THAN(applyNumberComparison((a, b) -> a < b), renderNumberComparison("<"), ValueType.NUMBER),

    LESS_EQUAL_THAN(applyNumberComparison((a, b) -> a <= b), renderNumberComparison("<="), ValueType.NUMBER);

    final protected ValueType[] applicableTypes;
    final protected BiFunction<Value, Value, Boolean> operation;
    final protected Function<Value, String> templateRenderer;

    ValueComparator(
        BiFunction<Value, Value, Boolean> operation,
        Function<Value, String> templateRenderer,
        ValueType... applicableTypes) {
        this.applicableTypes = applicableTypes;
        this.operation = operation;
        this.templateRenderer = templateRenderer;
    }

    static protected String escapeString(String value) {
        // Escape double quotes, remove control chars
        return value.replaceAll("\"", "\\\\\"").replaceAll("\\p{Cc}", "");
    }

    static protected BiFunction<Value, Value, Boolean> applyEquals(boolean ignoreCase, boolean negate) {
        return (a, b) -> {
            switch (a.getType()) {
                case STRING:
                    String s1 = Values.getString(a).orElseThrow(() -> new ValueException("Not a string: " + a));
                    String s2 = Values.getString(b).orElseThrow(() -> new ValueException("Not a string: " + b));
                    return negate != ignoreCase ? s1.toLowerCase(Locale.ROOT).equals(s2.toLowerCase(Locale.ROOT)) : s1.equals(s2);
                case NUMBER:
                    Double n1 = Values.getNumber(a).orElseThrow(() -> new ValueException("Not a number: " + a));
                    Double n2 = Values.getNumber(b).orElseThrow(() -> new ValueException("Not a number: " + b));
                    return negate != n1.equals(n2);
                case BOOLEAN:
                    Boolean b1 = Values.getBoolean(a).orElseThrow(() -> new ValueException("Not a boolean: " + a));
                    Boolean b2 = Values.getBoolean(b).orElseThrow(() -> new ValueException("Not a boolean: " + b));
                    return negate != b1.equals(b2);
                default:
                    return false;
            }
        };
    }

    static protected BiFunction<Value, Value, Boolean> applyNumberComparison(BiFunction<Double, Double, Boolean> comparator) {
        return (a, b) -> {
            Double n1 = Values.getNumber(a).orElseThrow(() -> new ValueException("Not a number: " + a));
            Double n2 = Values.getNumber(b).orElseThrow(() -> new ValueException("Not a number: " + b));
            return comparator.apply(n1, n2);
        };
    }

    static protected Function<Value, String> renderEquals(boolean ignoreCase, boolean negate) {
        return value -> {
            switch (value.getType()) {
                case STRING:
                    String s = Values.getString(value).orElseThrow(() -> new ValueException("Not a string: " + value));
                    return ignoreCase
                        ? (negate ? "!" : "") + "(\"" + escapeString(Values.getString(value).get()) + "\".equalsIgnoreCase(valueAsString))"
                        : "valueAsString " + (negate ? "!=" : "==") + " \"" + escapeString(s) + "\"";
                case NUMBER:
                    Double n = Values.getNumber(value).orElseThrow(() -> new ValueException("Not a number: " + value));
                    return "valueAsNumber " + (negate ? "!=" : "==") + " " + n;
                case BOOLEAN:
                    Boolean b = Values.getBoolean(value).orElseThrow(() -> new ValueException("Not a boolean: " + value));
                    return "valueAsBoolean " + (negate ? "!=" : "==") + " " + b;
                default:
                    return "";
            }
        };
    }

    static protected Function<Value, String> renderNumberComparison(String operator) {
        return value -> {
            switch (value.getType()) {
                case NUMBER:
                    return "valueAsNumber  " + operator + " " + Values.getNumber(value).get();
                default:
                    return "";
            }
        };
    }

    @Override
    public Boolean apply(Value value, Value value2) {
        return operation.apply(value, value2);
    }

    public boolean isApplicable(ValueType valueType) {
        for (ValueType applicableType : applicableTypes) {
            if (applicableType == valueType)
                return true;
        }
        return false;
    }

    public String renderRulesTemplatePattern(Value value) {
        Objects.requireNonNull(value);
        return templateRenderer.apply(value);
    }

    public static Optional<ValueComparator> fromString(String value) {
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

}
