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
import org.openremote.model.event.shared.SharedEvent;

import java.util.Arrays;
import java.util.List;

public class TriggeredEventSubscription<T extends SharedEvent> {

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
