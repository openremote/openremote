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
package org.openremote.model.event.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;
import org.openremote.model.event.Event;
import org.openremote.model.util.ValueUtil;

/**
 * A consumer can subscribe to {@link Event}s on the server, providing the
 * type of event it wants to receive as well as filter criteria to restrict the
 * events to an interesting subset.
 * <p>
 * A subscription can optionally contain a {@link #subscriptionId} which allows a client
 * to have multiple subscriptions for the same event type.
 */
@JsonSerialize(using = EventSubscription.EventSubscriptionSerializer.class)
@JsonDeserialize(using = EventSubscription.EventSubscriptionDeserializer.class)
public class EventSubscription<E extends Event> {

    public static class EventSubscriptionSerializer extends StdSerializer<EventSubscription> {

        protected EventSubscriptionSerializer() {
            super(EventSubscription.class);
        }

        @Override
        public void serialize(EventSubscription value, JsonGenerator gen, SerializationContext context) throws JacksonException {
            gen.writeStartObject();
            gen.writeName("eventType");
            gen.writeString(value.getEventType());
            context.defaultSerializeProperty("filter", value.filter, gen);
            gen.writeName("subscriptionId");
            gen.writeString(value.getSubscriptionId());
            gen.writeName("subscribed");
            gen.writeBoolean(value.isSubscribed());
            gen.writeEndObject();
        }
    }

    public static class EventSubscriptionDeserializer extends StdDeserializer<EventSubscription> {

        protected EventSubscriptionDeserializer() {
            super(EventSubscription.class);
        }

        @Override
        public EventSubscription deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = ctxt.readTree(p);
            EventSubscription subscription = new EventSubscription();
            JsonNode eventType = node.get("eventType");
            JsonNode filter = node.get("filter");
            JsonNode subscriptionId = node.get("subscriptionId");
            JsonNode subscribed = node.get("subscribed");

            if (eventType != null && !eventType.isNull()) {
                subscription.eventType = eventType.asText();
            }
            if (filter != null && !filter.isNull()) {
                subscription.filter = (EventFilter) ValueUtil.convert(filter, EventFilter.class);
            }
            if (subscriptionId != null && !subscriptionId.isNull()) {
                subscription.subscriptionId = subscriptionId.asText();
            }
            if (subscribed != null && !subscribed.isNull()) {
                subscription.subscribed = subscribed.asBoolean();
            }
            return subscription;
        }
    }

    public static final String SUBSCRIBE_MESSAGE_PREFIX = "SUBSCRIBE:";
    public static final String SUBSCRIBED_MESSAGE_PREFIX = "SUBSCRIBED:";

    /**
     * Event type of the subscription to cancel.
     *
     * <p>Must not contain the reserved delimiter {@code "::"}.
     */
    protected String eventType;

    protected EventFilter<E> filter;

    /**
     * Client-defined subscription identifier.
     *
     * <p>Must not contain the reserved delimiter {@code "::"}.
     */
    protected String subscriptionId;

    @JsonIgnore
    protected boolean subscribed;

    protected EventSubscription() {
    }

    public EventSubscription(String eventType) {
        this.eventType = eventType;
    }

    public EventSubscription(Class<E> eventClass) {
        this(Event.getEventType(eventClass));
    }

    public EventSubscription(String eventType, EventFilter<E> filter) {
        this.eventType = eventType;
        this.filter = filter;
    }

    public EventSubscription(Class<E> eventClass, EventFilter<E> filter) {
        this.eventType = Event.getEventType(eventClass);
        this.filter = filter;
    }

    public EventSubscription(Class<E> eventClass, EventFilter<E> filter, String subscriptionId) {
        this.eventType = Event.getEventType(eventClass);
        this.filter = filter;
        this.subscriptionId = subscriptionId;
    }

    @JsonProperty
    public String getEventType() {
        return eventType == null ? "" : eventType;
    }

    @JsonProperty
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @JsonProperty
    public EventFilter<E> getFilter() {
        return filter;
    }

    @JsonProperty
    public void setFilter(EventFilter<E> filter) {
        this.filter = filter;
    }

    public boolean isEventType(Class<? extends Event> eventClass) {
        return Event.getEventType(eventClass).equals(getEventType());
    }

    @JsonProperty
    public String getSubscriptionId() {
        return subscriptionId == null ? "" : subscriptionId;
    }

    @JsonProperty
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "eventType='" + eventType + '\'' +
            ", filter=" + filter +
            ", subscriptionId='" + subscriptionId + '\'' +
            '}';
    }
}
