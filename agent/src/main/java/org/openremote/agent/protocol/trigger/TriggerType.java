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
package org.openremote.agent.protocol.trigger;

import org.openremote.agent.protocol.trigger.attribute.AttributeTriggerHandler;
import org.openremote.agent.protocol.trigger.time.TimeTriggerHandler;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Objects;
import java.util.Optional;

public enum TriggerType implements TriggerHandlerProducer {

    /**
     * For cron based triggers
     */
    TIME(new TimeTriggerHandler()),

    /**
     * For {@link AttributeEvent} triggers
     */
    ATTRIBUTE(new AttributeTriggerHandler());

    private AbstractTriggerHandler handler;

    // Prevents cloning of values each time fromString is called
    private static final TriggerType[] copyOfValues = values();

    TriggerType(AbstractTriggerHandler handler) {
        this.handler = handler;
    }

    public AbstractTriggerHandler getHandler() {
        return handler;
    }

    public static Optional<TriggerType> fromString(String value) {
        for (TriggerType type : copyOfValues) {
            if (type.name().equalsIgnoreCase(value))
                return Optional.of(type);
        }
        return Optional.empty();
    }

    public static String toString(TriggerType triggerType) {
        Objects.requireNonNull(triggerType);
        return triggerType.toString();
    }

    public static Optional<TriggerType> fromValue(Value value) {
        return Values.getString(value).flatMap(TriggerType::fromString);
    }

    public static Value toValue(TriggerType triggerType) {
        return Values.create(toString(triggerType));
    }
}
