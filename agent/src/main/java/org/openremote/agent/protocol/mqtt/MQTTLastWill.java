/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.agent.protocol.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents last will configuration for a client
 */
public class MQTTLastWill {
    String topic;
    String payload;
    boolean retain;

    @JsonCreator
    public MQTTLastWill(String topic, String payload, boolean retain) {
        this.topic = topic;
        this.payload = payload;
        this.retain = retain;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isRetain() {
        return retain;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "topic='" + topic + '\'' +
            ", retain=" + retain +
            ", hasPayload=" + (payload != null ? "true" : "false") +
            '}';
    }
}
