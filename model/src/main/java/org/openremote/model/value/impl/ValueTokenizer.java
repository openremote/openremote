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

import org.openremote.model.value.*;

/**
 * Implementation of parsing a JSON string into instances
 * of {@link org.openremote.model.value.Value}.
 */
class ValueTokenizer {

    private static final int INVALID_CHAR = -1;

    private static final String STOPCHARS = ",:]}/\\\"[{;=#";

    private ValueFactory valueFactory;

    private boolean lenient = true;

    private int pushBackBuffer = INVALID_CHAR;

    private final String json;
    private int position = 0;

    ValueTokenizer(ValueFactoryImpl valueFactory, String json) {
        this.valueFactory = valueFactory;
        this.json = json;
    }

    void back(char c) {
        assert pushBackBuffer == INVALID_CHAR;
        pushBackBuffer = c;
    }

    void back(int c) {
        back((char) c);
    }

    int next() {
        if (pushBackBuffer != INVALID_CHAR) {
            final int c = pushBackBuffer;
            pushBackBuffer = INVALID_CHAR;
            return c;
        }

        return position < json.length() ? json.charAt(position++) : INVALID_CHAR;
    }

    String next(int n) throws ValueException {
        if (n == 0) {
            return "";
        }

        char[] buffer = new char[n];
        int pos = 0;

        if (pushBackBuffer != INVALID_CHAR) {
            buffer[0] = (char) pushBackBuffer;
            pos = 1;
            pushBackBuffer = INVALID_CHAR;
        }

        int len;
        while ((pos < n) && ((len = read(buffer, pos, n - pos)) != -1)) {
            pos += len;
        }

        if (pos < n) {
            throw new ValueException("TODO"/* TODO(knorton): Add message. */);
        }

        return String.valueOf(buffer);
    }

    int nextNonWhitespace() {
        while (true) {
            final int c = next();
            if (!Character.isWhitespace((char) c)) {
                return c;
            }
        }
    }

    String nextString(int startChar) throws ValueException {
        final StringBuilder buffer = new StringBuilder();
        int c = next();
        assert c == '"' || (lenient && c == '\'');
        while (true) {
            c = next();
            switch (c) {
                case INVALID_CHAR:
                    throw new ValueException("Invalid string: closing " + startChar + " is not found");
                case '\\':
                    c = next();
                    switch (c) {
                        case 'b':
                            buffer.append('\b');
                            break;
                        case 't':
                            buffer.append('\t');
                            break;
                        case 'n':
                            buffer.append('\n');
                            break;
                        case 'f':
                            buffer.append('\f');
                            break;
                        case 'r':
                            buffer.append('\r');
                            break;
                        // TODO(knorton): I'm not sure I should even support this escaping mode since JSON is always UTF-8.
                        case 'u':
                            buffer.append((char) Integer.parseInt(next(4), 16));
                            break;
                        default:
                            buffer.append((char) c);
                    }
                    break;
                default:
                    if (c == startChar) {
                        return buffer.toString();
                    }
                    buffer.append((char) c);
            }
        }
    }

    String nextUntilOneOf(String chars) {
        final StringBuilder buffer = new StringBuilder();
        int c = next();
        while (c != INVALID_CHAR) {
            if (Character.isWhitespace((char) c) || chars.indexOf((char) c) >= 0) {
                back(c);
                break;
            }
            buffer.append((char) c);
            c = next();
        }
        return buffer.toString();
    }

    @SuppressWarnings("unchecked")
    <T extends Value> T nextValue() throws ValueException {
        final int c = nextNonWhitespace();
        back(c);
        switch (c) {
            case '"':
            case '\'':
                String s = nextString(c);
                return (T)valueFactory.create(s);
            case '{':
                return (T) parseObject();
            case '[':
                return (T) parseArray();
            default:
                return (T) getValueForLiteral(nextUntilOneOf(STOPCHARS));
        }
    }

    ArrayValue parseArray() throws ValueException {
        final ArrayValue array = valueFactory.createArray();
        int c = nextNonWhitespace();
        assert c == '[';
        while (true) {
            c = nextNonWhitespace();
            switch (c) {
                case ']':
                    return array;
                default:
                    back(c);
                    Value v = nextValue();
                    array.set(array.length(), v);
                    final int d = nextNonWhitespace();
                    switch (d) {
                        case ']':
                            return array;
                        case ',':
                            break;
                        default:
                            throw new ValueException("Invalid array: expected , or ]");
                    }
            }
        }
    }

    ObjectValue parseObject() throws ValueException {
        final ObjectValue object = valueFactory.createObject();
        int c = nextNonWhitespace();
        if (c != '{') {
            throw new ValueException("Payload does not begin with '{'.  Got " + c + "(" + (char) c + ")");
        }

        while (true) {
            c = nextNonWhitespace();
            switch (c) {
                case '}':
                    // We're done.
                    return object;
                case '"':
                case '\'':
                    back(c);
                    // Ready to start a key.
                    final String key = nextString(c);
                    if (nextNonWhitespace() != ':') {
                        throw new ValueException("Invalid object: expecting \":\"");
                    }
                    Value v = nextValue();
                    object.put(key, v);
                    switch (nextNonWhitespace()) {
                        case ',':
                            break;
                        case '}':
                            return object;
                        default:
                            throw new ValueException("Invalid object: expecting } or ,");
                    }
                    break;
                case ',':
                    break;
                default:
                    if (lenient && (Character.isDigit((char) c) || Character.isLetterOrDigit((char) c))) {
                        StringBuilder keyBuffer = new StringBuilder();
                        keyBuffer.append((char) c);
                        while (true) {
                            c = next();
                            if (Character.isDigit((char) c) || Character.isLetterOrDigit((char) c)) {
                                keyBuffer.append((char) c);
                            } else {
                                back(c);
                                break;
                            }
                        }
                        if (nextNonWhitespace() != ':') {
                            throw new ValueException("Invalid object: expecting \":\"");
                        }
                        v = nextValue();
                        object.put(keyBuffer.toString(), v);
                        switch (nextNonWhitespace()) {
                            case ',':
                                break;
                            case '}':
                                return object;
                            default:
                                throw new ValueException("Invalid object: expecting } or ,");
                        }

                    } else {
                        throw new ValueException("Invalid object: ");
                    }
            }
        }
    }

    private NumberValue getNumberForLiteral(String literal)
        throws ValueException {
        try {
            return valueFactory.create(Double.parseDouble(literal));
        } catch (NumberFormatException e) {
            throw new ValueException("Invalid number literal: " + literal);
        }
    }

    private Value getValueForLiteral(String literal) throws ValueException {
        if ("".equals(literal)) {
            throw new ValueException("Missing value");
        }

        if ("null".equals(literal) || "undefined".equals(literal)) {
            return null;
        }

        if ("true".equals(literal)) {
            return valueFactory.create(true);
        }

        if ("false".equals(literal)) {
            return valueFactory.create(false);
        }

        // Be tolerant of bad JSON with NaN
        if ("NaN".equalsIgnoreCase(literal)) {
            return null;
        }

        final char c = literal.charAt(0);
        if (c == '-' || Character.isDigit(c)) {
            return getNumberForLiteral(literal);
        }

        throw new ValueException("Invalid literal: \"" + literal + "\"");
    }

    private int read(char[] buffer, int pos, int len) {
        int maxLen = Math.min(json.length() - position, len);
        String src = json.substring(position, position + maxLen);
        char result[] = src.toCharArray();
        System.arraycopy(result, 0, buffer, pos, maxLen);
        position += maxLen;
        return maxLen;
    }
}
