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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.filter.ValuePredicate;
import org.openremote.model.util.AssetModelUtil;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.value.ValueFilter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

/**
 * Represents the configuration of an {@link Attribute} linked to an {@link Agent}; each {@link Agent} should have its'
 * own concrete implementation of this class with fields describing each configuration item and standard JSR-380
 * annotations should be used to provide validation logic.
 */
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM, defaultImpl = AgentLink.Default.class)
@JsonTypeIdResolver(AgentLink.AgentLinkTypeIdResolver.class)
public abstract class AgentLink<T extends AgentLink<?>> implements Serializable {

    /**
     * Resolves agent link type as agent type strings using {@link org.openremote.model.util.AssetModelUtil}
     */
    @TsIgnore
    public static class AgentLinkTypeIdResolver extends TypeIdResolverBase {
        @Override
        public String idFromValue(Object value) {
            if (value == null) {
                return null;
            }
            if (!(value instanceof AgentLink)) {
                throw new IllegalArgumentException("Type must be an agent link type");
            }
            return value.getClass().getSimpleName().replaceAll("\\$", ".");
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return idFromValue(value);
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.CUSTOM;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) throws IOException {
            Class<? extends AgentLink<?>> agentLinkClass = AssetModelUtil.getAgentLinkClass(id)
                .orElse(AgentLink.Default.class);
            return context.constructType(agentLinkClass);
        }
    }

    /**
     * Does nothing other than hide the generic type parameter which causes problems with inference from class references
     */
    @JsonTypeName("Default") // Needed for typescript generator - doesn't seem to use type id resolver
    public static class Default extends AgentLink<Default> {

        protected Default() {}

        public Default(String id) {
            super(id);
        }
    }

    protected String id;
    protected ValueFilter[] valueFilters;
    protected ObjectNode valueConverter;
    protected ObjectNode writeValueConverter;
    protected String writeValue;
    protected ValuePredicate messageMatchPredicate;
    protected ValueFilter[] messageMatchFilters;

    // For Hydrators
    protected AgentLink() {}

    protected AgentLink(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @JsonPropertyDescription("Defines ValueFilters to apply to an incoming value before it is written to a" +
        " protocol linked attribute; this is particularly useful for generic protocols. The message should pass through" +
        " the filters in array order")
    public Optional<ValueFilter[]> getValueFilters() {
        return Optional.ofNullable(valueFilters);
    }

    @JsonPropertyDescription("Defines a value converter map to allow for basic value type conversion; the incoming value" +
        " will be converted to JSON and if this string matches a key in the converter then the value of that key will be" +
        " pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but" +
        " you want to connect this to a Boolean attribute")
    public Optional<ObjectNode> getValueConverter() {
        return Optional.ofNullable(valueConverter);
    }

    @JsonPropertyDescription("Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion")
    public Optional<ObjectNode> getWriteValueConverter() {
        return Optional.ofNullable(writeValueConverter);
    }

    @JsonPropertyDescription("String to be used for attribute writes and can contain '" + Protocol.DYNAMIC_VALUE_PLACEHOLDER +
        "' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the" +
        " protocol")
    public Optional<String> getWriteValue() {
        return Optional.ofNullable(writeValue);
    }

    @JsonPropertyDescription("The predicate to apply to incoming messages to determine if the message is intended for the" +
        " linked attribute")
    public Optional<ValuePredicate> getMessageMatchPredicate() {
        return Optional.ofNullable(messageMatchPredicate);
    }

    @JsonPropertyDescription("ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate")
    public Optional<ValueFilter[]> getMessageMatchFilters() {
        return Optional.ofNullable(messageMatchFilters);
    }

    @SuppressWarnings("unchecked")
    public T setValueFilters(ValueFilter[] valueFilters) {
        this.valueFilters = valueFilters;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T setValueConverter(ObjectNode valueConverter) {
        this.valueConverter = valueConverter;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T setWriteValueConverter(ObjectNode writeValueConverter) {
        this.writeValueConverter = writeValueConverter;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T setWriteValue(String writeValue) {
        this.writeValue = writeValue;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T setMessageMatchPredicate(ValuePredicate messageMatchPredicate) {
        this.messageMatchPredicate = messageMatchPredicate;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T setMessageMatchFilters(ValueFilter[] messageMatchFilters) {
        this.messageMatchFilters = messageMatchFilters;
        return (T)this;
    }

    public static <T> T getOrThrowAgentLinkProperty(Optional<T> value, String name) {
        return value.orElseThrow(() -> {
            String msg = "Required agent link property is undefined: " + name;
            AssetModelUtil.LOG.warning(msg);
            return new IllegalStateException("msg");
        });
    }
}
