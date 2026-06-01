/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.asset.agent;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.filter.ValuePredicate;
import org.openremote.model.util.JSONSchemaUtil.*;
import org.openremote.model.util.TsIgnoreTypeParams;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueFilter;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the configuration of an {@link Attribute} linked to an {@link Agent}; each {@link Agent} should have its'
 * own concrete implementation of this class or use {@link DefaultAgentLink} with fields describing each configuration
 * item and standard JSR-380 annotations should be used to provide validation logic.
 */
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, defaultImpl = DefaultAgentLink.class)
@JsonSerialize(using = AgentLink.AgentLinkSerializer.class)
@JsonDeserialize(using = AgentLink.AgentLinkDeserializer.class)
@JsonSchemaDefault("{\"id\":\"\",\"type\":\"DefaultAgentLink\"}")
@TsIgnoreTypeParams
public abstract class AgentLink<T extends AgentLink<?>> implements Serializable {

    public static class AgentLinkSerializer extends StdSerializer<AgentLink<?>> {

        protected AgentLinkSerializer() {
            super((Class<AgentLink<?>>) (Class<?>) AgentLink.class);
        }

        @Override
        public void serialize(AgentLink<?> value, JsonGenerator gen, SerializationContext context) throws JacksonException {
            gen.writeStartObject();
            gen.writeName("type");
            gen.writeString(value.getClass().getSimpleName());
            writeFields(value, AgentLink.class, gen, context);

            for (Class<?> type = value.getClass(); type != null && type != AgentLink.class; type = type.getSuperclass()) {
                writeFields(value, type, gen, context);
            }

            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(AgentLink<?> value, JsonGenerator gen, SerializationContext context, TypeSerializer typeSer) throws JacksonException {
            serialize(value, gen, context);
        }

        protected void writeFields(AgentLink<?> value, Class<?> type, JsonGenerator gen, SerializationContext context) throws JacksonException {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    if (fieldValue != null) {
                        gen.writeName(field.getName());
                        context.writeValue(gen, fieldValue);
                    }
                } catch (IllegalAccessException e) {
                    throw DatabindException.from(gen, "Failed to serialize agent link field: " + field.getName());
                }
            }
        }
    }

    public static class AgentLinkDeserializer extends StdDeserializer<AgentLink<?>> {

        protected AgentLinkDeserializer() {
            super((Class<AgentLink<?>>) (Class<?>) AgentLink.class);
        }

        @Override
        public AgentLink<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = ctxt.readTree(jp);
            String type = node.has("type") && !node.get("type").isNull() ? node.get("type").asText() : DefaultAgentLink.class.getSimpleName();
            Class<? extends AgentLink<?>> agentLinkClass = resolveAgentLinkClass(type);

            try {
                AgentLink<?> agentLink = newAgentLink(agentLinkClass, node);
                readFields(agentLink, AgentLink.class, node);

                for (Class<?> fieldType = agentLinkClass; fieldType != null && fieldType != AgentLink.class; fieldType = fieldType.getSuperclass()) {
                    readFields(agentLink, fieldType, node);
                }

                return agentLink;
            } catch (ReflectiveOperationException e) {
                throw DatabindException.from(jp, "Failed to deserialize agent link type '" + type + "': " + e.getMessage());
            }
        }

        @Override
        public AgentLink<?> deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws JacksonException {
            return deserialize(jp, ctxt);
        }

        protected Class<? extends AgentLink<?>> resolveAgentLinkClass(String type) {
            return ValueUtil.getAgentLinkClass(type).orElse(DefaultAgentLink.class);
        }

        protected AgentLink<?> newAgentLink(Class<? extends AgentLink<?>> agentLinkClass, JsonNode node) throws ReflectiveOperationException {
            try {
                Constructor<? extends AgentLink<?>> constructor = agentLinkClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (NoSuchMethodException ignored) {
                String id = node.has("id") && !node.get("id").isNull() ? node.get("id").asText() : null;
                Constructor<? extends AgentLink<?>> constructor = agentLinkClass.getDeclaredConstructor(String.class);
                constructor.setAccessible(true);
                return constructor.newInstance(id);
            }
        }

        protected void readFields(AgentLink<?> agentLink, Class<?> type, JsonNode node) throws IllegalAccessException {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }

                JsonNode fieldNode = node.get(field.getName());
                if (fieldNode == null || fieldNode.isNull()) {
                    continue;
                }

                field.setAccessible(true);
                field.set(agentLink, ValueUtil.JSON.treeToValue(fieldNode, field.getType()));
            }
        }
    }

    @JsonSchemaFormat("or-agent-id")
    @JsonProperty
    protected String id;
    @JsonSchemaDescription("Defines ValueFilters to apply to an incoming value before it is written to a" +
        " protocol linked attribute; this is particularly useful for generic protocols. The message should pass through" +
        " the filters in array order")
    @JsonProperty
    protected ValueFilter[] valueFilters;
    @JsonSchemaDescription("Defines a value converter map to allow for basic value type conversion; the incoming value" +
        " will be converted to JSON and if this string matches a key in the converter then the value of that key will be" +
        " pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but" +
        " you want to connect this to a Boolean attribute")
    @JsonSchemaSupplier(supplier = SchemaNodeMapper.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE)
    @JsonProperty
    protected Map<String, Object> valueConverter;
    @JsonSchemaDescription("Similar to valueFilter but will be applied to outgoing values allowing for inverse filtering.")
    @JsonProperty
    protected ValueFilter[] writeValueFilters;
    @JsonSchemaDescription("Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion")
    @JsonSchemaSupplier(supplier = SchemaNodeMapper.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE)
    @JsonProperty
    protected Map<String, Object> writeValueConverter;
    @JsonSchemaDescription("String to be used for attribute writes and can contain dynamic placeholders to allow dynamic" +
            " value and/or time injection with formatting (see documentation for details) into the string or alternatively" +
            " write the string through to the protocol as is (static string)")
    @JsonSchemaFormat("or-multiline")
    @JsonProperty
    protected String writeValue;
    @JsonSchemaDescription("The predicate to apply to incoming messages to determine if the message is intended for the" +
        " linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to" +
        " enable attributes to be updated by the linked agent.")
    @JsonProperty
    protected ValuePredicate messageMatchPredicate;
    @JsonSchemaDescription("ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate")
    @JsonProperty
    protected ValueFilter[] messageMatchFilters;
    @JsonSchemaDescription("Don't expect a response from the protocol just update the attribute immediately on write")
    @JsonProperty
    protected Boolean updateOnWrite;

    // For Hydrators
    protected AgentLink() {
    }

    protected AgentLink(String id) {
        this.id = id;
    }

    public static <T> T getOrThrowAgentLinkProperty(Optional<T> value, String name) {
        return value.orElseThrow(() -> {
            String msg = "Required agent link property is undefined: " + name;
            ValueUtil.LOG.warning(msg);
            return new IllegalStateException("msg");
        });
    }

    @JsonSerialize
    protected String getType() {
        return getClass().getSimpleName();
    }

    public String getId() {
        return id;
    }

    public Optional<ValueFilter[]> getValueFilters() {
        return Optional.ofNullable(valueFilters);
    }

    @SuppressWarnings("unchecked")
    public T setValueFilters(ValueFilter[] valueFilters) {
        this.valueFilters = valueFilters;
        return (T) this;
    }

    public Optional<Map<String, Object>> getValueConverter() {
        return Optional.ofNullable(valueConverter);
    }

    @SuppressWarnings("unchecked")
    public T setValueConverter(Map<String, Object> valueConverter) {
        this.valueConverter = valueConverter;
        return (T) this;
    }

    public Optional<ValueFilter[]> getWriteValueFilters() {
        return Optional.ofNullable(writeValueFilters);
    }

    @SuppressWarnings("unchecked")
    public T setWriteValueFilters(ValueFilter[] writeValueFilters) {
        this.writeValueFilters = writeValueFilters;
        return (T) this;
    }

    public Optional<Map<String, Object>> getWriteValueConverter() {
        return Optional.ofNullable(writeValueConverter);
    }

    @SuppressWarnings("unchecked")
    public T setWriteValueConverter(Map<String, Object> writeValueConverter) {
        this.writeValueConverter = writeValueConverter;
        return (T) this;
    }

    public Optional<String> getWriteValue() {
        return Optional.ofNullable(writeValue);
    }

    @SuppressWarnings("unchecked")
    public T setWriteValue(String writeValue) {
        this.writeValue = writeValue;
        return (T) this;
    }

    public Optional<ValuePredicate> getMessageMatchPredicate() {
        return Optional.ofNullable(messageMatchPredicate);
    }

    @SuppressWarnings("unchecked")
    public T setMessageMatchPredicate(ValuePredicate messageMatchPredicate) {
        this.messageMatchPredicate = messageMatchPredicate;
        return (T) this;
    }

    public Optional<ValueFilter[]> getMessageMatchFilters() {
        return Optional.ofNullable(messageMatchFilters);
    }

    @SuppressWarnings("unchecked")
    public T setMessageMatchFilters(ValueFilter[] messageMatchFilters) {
        this.messageMatchFilters = messageMatchFilters;
        return (T) this;
    }

    public Optional<Boolean> getUpdateOnWrite() {
        return Optional.ofNullable(updateOnWrite);
    }

    @SuppressWarnings("unchecked")
    public T setUpdateOnWrite(Boolean updateOnWrite) {
        this.updateOnWrite = updateOnWrite;
        return (T) this;
    }
}
