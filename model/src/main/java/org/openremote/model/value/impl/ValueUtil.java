/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.openremote.model.value.impl;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Direct port of json2.js at http://www.json.org/json2.js to GWT.
 */
public class ValueUtil {

    private static class StringifyValueVisitor extends ValueVisitor {

        private static final Set<String> skipKeys;

        static {
            Set<String> toSkip = new HashSet<>();
            toSkip.add("$H");
            toSkip.add("__gwt_ObjectId");
            skipKeys = Collections.unmodifiableSet(toSkip);
        }

        private String indentLevel;

        private Set<Value> visited;

        private final String indent;

        private final StringBuilder sb;

        private final boolean pretty;

        public StringifyValueVisitor(String indent, StringBuilder sb,
                                     boolean pretty) {
            this.indent = indent;
            this.sb = sb;
            this.pretty = pretty;
            indentLevel = "";
            visited = new HashSet<>();
        }

        @Override
        public void endVisit(ArrayValue array, ValueContext ctx) {
            if (pretty) {
                indentLevel = indentLevel
                    .substring(0, indentLevel.length() - indent.length());
                sb.append('\n');
                sb.append(indentLevel);
            }
            sb.append("]");
            visited.remove(array);
        }

        @Override
        public void endVisit(ObjectValue object, ValueContext ctx) {
            if (pretty) {
                indentLevel = indentLevel
                    .substring(0, indentLevel.length() - indent.length());
                sb.append('\n');
                sb.append(indentLevel);
            }
            sb.append("}");
            visited.remove(object);
            assert !visited.contains(object);
        }

        @Override
        public void visit(double number, ValueContext ctx) {
            sb.append(Double.isInfinite(number) || Double.isNaN(number) ? "null" : format(number));
        }

        @Override
        public void visit(String string, ValueContext ctx) {
            sb.append(quote(string));
        }

        @Override
        public void visit(boolean bool, ValueContext ctx) {
            sb.append(bool);
        }

        @Override
        public boolean visit(ArrayValue array, ValueContext ctx) throws ValueException {
            checkCycle(array);
            sb.append("[");
            if (pretty) {
                sb.append('\n');
                indentLevel += indent;
                sb.append(indentLevel);
            }
            return true;
        }

        @Override
        public boolean visit(ObjectValue object, ValueContext ctx) throws ValueException {
            checkCycle(object);
            sb.append("{");
            if (pretty) {
                sb.append('\n');
                indentLevel += indent;
                sb.append(indentLevel);
            }
            return true;
        }

        @Override
        public boolean visitIndex(int index, ValueContext ctx) {
            commaIfNotFirst(ctx);
            return true;
        }

        @Override
        public boolean visitKey(String key, ValueContext ctx) {
            if ("".equals(key)) {
                return true;
            }
            // skip properties injected by GWT runtime on JSOs
            if (skipKeys.contains(key)) {
                return false;
            }
            commaIfNotFirst(ctx);
            sb.append(quote(key)).append(":");
            if (pretty) {
                sb.append(' ');
            }
            return true;
        }

        private void checkCycle(Value value) throws ValueException {
            if (visited.contains(value)) {
                throw new ValueException("Cycled detected during stringify");
            } else {
                visited.add(value);
            }
        }

        private void commaIfNotFirst(ValueContext ctx) {
            if (!ctx.isFirst()) {
                sb.append(",");
                if (pretty) {
                    sb.append('\n');
                    sb.append(indentLevel);
                }
            }
        }

        private String format(double number) {
            String n = String.valueOf(number);
            if (n.endsWith(".0")) {
                n = n.substring(0, n.length() - 2);
            }
            return n;
        }
    }

    /**
     * Convert special control characters into unicode escape format.
     */
    public static String escapeControlChars(String text) {
        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isControlChar(c)) {
                toReturn.append(escapeCharAsUnicode(c));
            } else {
                toReturn.append(c);
            }
        }
        return toReturn.toString();
    }

    /**
     * Safely escape an arbitrary string as a JSON string literal.
     */
    public static String quote(String value) {
        StringBuilder toReturn = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            String toAppend = String.valueOf(c);
            switch (c) {
                case '\b':
                    toAppend = "\\b";
                    break;
                case '\t':
                    toAppend = "\\t";
                    break;
                case '\n':
                    toAppend = "\\n";
                    break;
                case '\f':
                    toAppend = "\\f";
                    break;
                case '\r':
                    toAppend = "\\r";
                    break;
                case '"':
                    toAppend = "\\\"";
                    break;
                case '\\':
                    toAppend = "\\\\";
                    break;
                default:
                    if (isControlChar(c)) {
                        toAppend = escapeCharAsUnicode(c);
                    }
            }
            toReturn.append(toAppend);
        }
        toReturn.append("\"");
        return toReturn.toString();
    }

    /**
     * Converts an Object to Json format.
     *
     * @param jsonValue object to stringify
     * @return json formatted string
     * @throws ValueException when a cycle is detected
     */
    public static String stringify(Value jsonValue) throws ValueException {
        return stringify(jsonValue, 0);
    }

    /**
     * Converts an Object to Json format.
     *
     * @param value  object to stringify
     * @param spaces number of spaces to indent in pretty print mode
     * @return json formatted string
     * @throws ValueException when a cycle is detected
     */
    public static String stringify(Value value, int spaces) throws ValueException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            sb.append(' ');
        }
        return stringify(value, sb.toString());
    }

    /**
     * Converts a Json object to Json formatted String.
     *
     * @param value  object to stringify
     * @param indent optional indention prefix for pretty printing
     * @return json formatted string
     * @throws ValueException when a cycle is detected
     */
    public static String stringify(Value value, final String indent) throws ValueException {
        final StringBuilder sb = new StringBuilder();
        final boolean isPretty = indent != null && !"".equals(indent);

        new StringifyValueVisitor(indent, sb, isPretty).accept(value);
        return sb.toString();
    }

    /**
     * Turn a single unicode character into a 32-bit unicode hex literal.
     */
    private static String escapeCharAsUnicode(char toEscape) {
        String hexValue = Integer.toString(toEscape, 16);
        int padding = 4 - hexValue.length();
        return "\\u" + ("0000".substring(0, padding)) + hexValue;
    }

    private static boolean isControlChar(char c) {
        return (c >= 0x00 && c <= 0x1f)
            || (c >= 0x7f && c <= 0x9f)
            || c == '\u00ad' || c == '\u070f' || c == '\u17b4' || c == '\u17b5'
            || c == '\ufeff'
            || (c >= '\u0600' && c <= '\u0604')
            || (c >= '\u200c' && c <= '\u200f')
            || (c >= '\u2028' && c <= '\u202f')
            || (c >= '\u2060' && c <= '\u206f')
            || (c >= '\ufff0' && c <= '\uffff');
    }
}
