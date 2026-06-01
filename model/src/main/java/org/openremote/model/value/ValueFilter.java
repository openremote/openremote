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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Interface for a filter that can be applied to a value, the filter can return a different value type to the supplied
 * value (i.e. value conversion as well as filtering). Filters can be chained and if a null value is supplied to a
 * filter then the filter must also return null.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RegexValueFilter.class),
    @JsonSubTypes.Type(value = SubStringValueFilter.class),
    @JsonSubTypes.Type(value = JsonPathFilter.class),
    @JsonSubTypes.Type(value = MathExpressionValueFilter.class)
})
@JsonSerialize(using = ValueFilter.ValueFilterSerializer.class)
@JsonDeserialize(using = ValueFilter.ValueFilterDeserializer.class)
// TODO: Standardise inbound/outbound value processing as ordered list of filters and/or converters
public abstract class ValueFilter implements Serializable {

    public static class ValueFilterSerializer extends StdSerializer<ValueFilter> {

        protected ValueFilterSerializer() {
            super(ValueFilter.class);
        }

        @Override
        public void serialize(ValueFilter value, JsonGenerator gen, SerializationContext context) throws JacksonException {
            gen.writeStartObject();

            if (value instanceof RegexValueFilter filter) {
                gen.writeName("type");
                gen.writeString(RegexValueFilter.NAME);
                if (filter.pattern != null) {
                    gen.writeName("pattern");
                    gen.writeString(filter.pattern.pattern());
                }
                if (filter.matchGroup != null) {
                    gen.writeName("matchGroup");
                    gen.writeNumber(filter.matchGroup);
                }
                if (filter.matchIndex != null) {
                    gen.writeName("matchIndex");
                    gen.writeNumber(filter.matchIndex);
                }
            } else if (value instanceof SubStringValueFilter filter) {
                gen.writeName("type");
                gen.writeString(SubStringValueFilter.NAME);
                gen.writeName("beginIndex");
                gen.writeNumber(filter.beginIndex);
                if (filter.endIndex != null) {
                    gen.writeName("endIndex");
                    gen.writeNumber(filter.endIndex);
                }
            } else if (value instanceof JsonPathFilter filter) {
                gen.writeName("type");
                gen.writeString(JsonPathFilter.NAME);
                gen.writeName("path");
                gen.writeString(filter.path);
                gen.writeName("returnFirst");
                gen.writeBoolean(filter.returnFirst);
                gen.writeName("returnLast");
                gen.writeBoolean(filter.returnLast);
            } else if (value instanceof MathExpressionValueFilter filter) {
                gen.writeName("type");
                gen.writeString(MathExpressionValueFilter.NAME);
                gen.writeName("expression");
                gen.writeString(filter.expression);
            }

            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(ValueFilter value, JsonGenerator gen, SerializationContext context, TypeSerializer typeSer) throws JacksonException {
            serialize(value, gen, context);
        }
    }

    public static class ValueFilterDeserializer extends StdDeserializer<ValueFilter> {

        protected ValueFilterDeserializer() {
            super(ValueFilter.class);
        }

        @Override
        public ValueFilter deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = ctxt.readTree(jp);
            String type = text(node, "type");
            if (type == null) {
                type = inferType(node);
            }

            return switch (type) {
                case RegexValueFilter.NAME -> new RegexValueFilter(Pattern.compile(text(node, "pattern")))
                    .setMatchGroup(integer(node, "matchGroup"))
                    .setMatchIndex(integer(node, "matchIndex"));
                case SubStringValueFilter.NAME -> new SubStringValueFilter(integer(node, "beginIndex", 0), integer(node, "endIndex"));
                case JsonPathFilter.NAME -> new JsonPathFilter(text(node, "path"), bool(node, "returnFirst"), bool(node, "returnLast"));
                case MathExpressionValueFilter.NAME -> new MathExpressionValueFilter(text(node, "expression"));
                default -> throw DatabindException.from(jp, "Unsupported value filter type: " + type);
            };
        }

        @Override
        public ValueFilter deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws JacksonException {
            return deserialize(jp, ctxt);
        }

        protected static String inferType(JsonNode node) {
            if (node.has("pattern")) {
                return RegexValueFilter.NAME;
            }
            if (node.has("beginIndex")) {
                return SubStringValueFilter.NAME;
            }
            if (node.has("path")) {
                return JsonPathFilter.NAME;
            }
            if (node.has("expression")) {
                return MathExpressionValueFilter.NAME;
            }
            return null;
        }

        protected static String text(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        }

        protected static Integer integer(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asInt();
        }

        protected static int integer(JsonNode node, String field, int defaultValue) {
            Integer value = integer(node, field);
            return value != null ? value : defaultValue;
        }

        protected static boolean bool(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value != null && !value.isNull() && value.asBoolean();
        }
    }

    public abstract Object filter(Object value);
}
