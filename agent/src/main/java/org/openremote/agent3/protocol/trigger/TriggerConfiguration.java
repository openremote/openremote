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
package org.openremote.agent3.protocol.trigger;

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Optional;
import java.util.function.Function;

import static org.openremote.agent3.protocol.trigger.TriggerProtocol.*;
import static org.openremote.agent3.protocol.trigger.TriggerType.toValue;
import static org.openremote.model.asset.agent.ProtocolConfiguration.getProtocolName;
import static org.openremote.model.attribute.AttributeType.STRING;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.attribute.MetaItem.replaceMetaByName;

/**
 * Utility functions for working with trigger attributes for the {@link TriggerProtocol}
 */
final public class TriggerConfiguration {

    private TriggerConfiguration() {
    }

    public static Function<AssetAttribute, AssetAttribute> initTriggerConfiguration() {
        return attribute -> {
            attribute.setTypeAndClearValue(STRING);
            attribute.setValue(Values.create(TriggerProtocol.PROTOCOL_NAME));
            return attribute;
        };
    }

    public static boolean isTriggerConfiguration(AssetAttribute attribute) {
        return getProtocolName(attribute)
            .map(TriggerProtocol.PROTOCOL_NAME::equals)
            .orElse(false);
    }

    public static boolean isValidTriggerConfiguration(AssetAttribute attribute) {
        return isTriggerConfiguration(attribute)
            && isTriggerActionValid(attribute)
            && isTriggerTypeAndValueValid(attribute);
    }

    public static boolean hasTriggerType(AssetAttribute attribute) {
        return attribute != null && attribute
            .getMetaStream()
            .anyMatch(isMetaNameEqualTo(META_TRIGGER_TYPE));
    }

    public static boolean isTriggerTypeValid(AssetAttribute attribute) {
        return getTriggerType(attribute).isPresent();
    }

    public static Optional<TriggerType> getTriggerType(AssetAttribute attribute) {
        return attribute == null ? Optional.empty() : attribute
            .getMetaItem(META_TRIGGER_TYPE)
            .flatMap(AbstractValueHolder::getValueAsString)
            .flatMap(TriggerType::fromString);
    }

    public static void setTriggerType(AssetAttribute attribute, TriggerType triggerType) {
        if (attribute == null)
            return;

        replaceMetaByName(attribute.getMeta(), META_TRIGGER_TYPE, toValue(triggerType));
    }

    public static Optional<AbstractTriggerHandler> getTriggerTypeHandler(AssetAttribute attribute) {
        return getTriggerType(attribute)
            .map(TriggerType::getHandler);
    }

    public static boolean hasTriggerValue(AssetAttribute attribute) {
        return attribute != null && attribute.hasMetaItem(META_TRIGGER_VALUE);
    }

    public static boolean isTriggerTypeAndValueValid(AssetAttribute attribute) {
        Optional<Value> triggerValue = getTriggerValue(attribute);
        Optional<AbstractTriggerHandler> handler = getTriggerTypeHandler(attribute);
        return triggerValue.isPresent() && handler
            .map(h ->
                isTriggerValueValid(h,
                    triggerValue.get()
                )
            )
            .orElse(false);
    }

    public static boolean isTriggerValueValid(AbstractTriggerHandler handler, Value value) {
        return handler != null && value != null && handler.isValidValue(value);
    }

    public static Optional<Value> getTriggerValue(AssetAttribute attribute) {
        return attribute == null ? Optional.empty() : attribute
            .getMetaItem(META_TRIGGER_VALUE)
            .flatMap(AbstractValueHolder::getValue);
    }

    public static void setTriggerValue(AssetAttribute attribute, Value value) {
        if (attribute == null)
            return;

        replaceMetaByName(attribute.getMeta(), META_TRIGGER_VALUE, value);
    }

    public static boolean hasTriggerAction(AssetAttribute attribute) {
        return attribute != null && attribute.hasMetaItem(META_TRIGGER_ACTION);
    }

    public static boolean isTriggerActionValid(AssetAttribute attribute) {
        return attribute != null && attribute.getMetaItem(META_TRIGGER_ACTION)
            .flatMap(AbstractValueHolder::getValue)
            .map(AttributeState::isAttributeState)
            .orElse(false);
    }

    public static Optional<AttributeState> getTriggerAction(AssetAttribute attribute) {
        return attribute == null ? Optional.empty() : attribute
            .getMetaItem(META_TRIGGER_ACTION)
            .flatMap(AbstractValueHolder::getValueAsObject)
            .flatMap(AttributeState::fromValue);
    }

    public static void setTriggerAction(AssetAttribute attribute, AttributeState action) {
        if (attribute == null)
            return;
        replaceMetaByName(attribute.getMeta(), META_TRIGGER_ACTION, action.toObjectValue());
    }

    public static void removeTrigger(AssetAttribute attribute) {
        if (attribute == null)
            return;

        attribute
            .getMeta()
            .removeIf(
                isMetaNameEqualTo(META_TRIGGER_TYPE)
                    .or(isMetaNameEqualTo(META_TRIGGER_VALUE))
                    .or(isMetaNameEqualTo(META_TRIGGER_ACTION))
            );
    }

    public static Optional<String> getTriggerProperty(AssetAttribute attribute) {
        return attribute == null ? Optional.empty() : attribute
            .getMetaItem(AssetMeta.PROTOCOL_PROPERTY)
            .flatMap(AbstractValueHolder::getValueAsString);
    }
}
