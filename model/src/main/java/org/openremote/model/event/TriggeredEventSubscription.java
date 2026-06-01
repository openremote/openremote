/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.model.event;

import com.fasterxml.jackson.annotation.JsonCreator;
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
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;
import org.openremote.model.datapoint.AssetPredictedDatapointEvent;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonSerialize(using = TriggeredEventSubscription.TriggeredEventSubscriptionSerializer.class)
@JsonDeserialize(using = TriggeredEventSubscription.TriggeredEventSubscriptionDeserializer.class)
public class TriggeredEventSubscription<T extends SharedEvent> {

    public static class TriggeredEventSubscriptionSerializer extends StdSerializer<TriggeredEventSubscription> {

        protected TriggeredEventSubscriptionSerializer() {
            super(TriggeredEventSubscription.class);
        }

        @Override
        public void serialize(TriggeredEventSubscription value, JsonGenerator gen, SerializationContext context) throws JacksonException {
            gen.writeStartObject();
            gen.writeName("events");
            gen.writeStartArray();
            if (value.events != null) {
                for (Object event : value.events) {
                    if (event instanceof AssetPredictedDatapointEvent predictedDatapointEvent) {
                        gen.writeStartObject();
                        gen.writeName("eventType");
                        gen.writeString(predictedDatapointEvent.getEventType());
                        gen.writeName("ref");
                        gen.writeStartObject();
                        gen.writeName("id");
                        gen.writeString(predictedDatapointEvent.getRef().getId());
                        gen.writeName("name");
                        gen.writeString(predictedDatapointEvent.getRef().getName());
                        gen.writeEndObject();
                        gen.writeName("timestamp");
                        gen.writeNumber(predictedDatapointEvent.getTimestamp());
                        gen.writeEndObject();
                        continue;
                    }
                    JsonNode eventNode = context.valueToTree(event);
                    if (event instanceof SharedEvent sharedEvent && eventNode instanceof ObjectNode objectNode) {
                        objectNode.put("eventType", sharedEvent.getEventType());
                    }
                    context.writeTree(gen, eventNode);
                }
            }
            gen.writeEndArray();
            gen.writeName("subscriptionId");
            gen.writeString(value.getSubscriptionId());
            gen.writeEndObject();
        }
    }

    public static class TriggeredEventSubscriptionDeserializer extends StdDeserializer<TriggeredEventSubscription> {

        protected TriggeredEventSubscriptionDeserializer() {
            super(TriggeredEventSubscription.class);
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public TriggeredEventSubscription deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = ctxt.readTree(p);
            JsonNode eventsNode = node.get("events");
            JsonNode subscriptionIdNode = node.get("subscriptionId");
            List<SharedEvent> events = new ArrayList<>();

            if (eventsNode != null && eventsNode.isArray()) {
                for (JsonNode eventNode : eventsNode) {
                    events.add(ValueUtil.JSON.treeToValue(eventNode, SharedEvent.class));
                }
            }

            return new TriggeredEventSubscription(
                events,
                subscriptionIdNode != null && !subscriptionIdNode.isNull() ? subscriptionIdNode.asText() : null
            );
        }
    }

    public static final String MESSAGE_PREFIX = "TRIGGERED:";
    protected List<T> events;
    protected String subscriptionId;

    @JsonCreator
    public TriggeredEventSubscription(@JsonProperty("events") List<T> events, @JsonProperty("subscriptionId") String subscriptionId) {
        this.events = events;
        this.subscriptionId = subscriptionId;
    }

    public List<T> getEvents() {
        return events;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return "TriggeredEventSubscription{" +
                "events=" + Arrays.toString(events.toArray()) +
                ", subscriptionId='" + subscriptionId + '\'' +
                '}';
    }
}
