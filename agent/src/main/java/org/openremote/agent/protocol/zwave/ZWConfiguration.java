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
package org.openremote.agent.protocol.zwave;

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.attribute.AttributeValueType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import static org.openremote.agent.protocol.zwave.ZWProtocol.META_ZWAVE_DEVICE_ENDPOINT;
import static org.openremote.agent.protocol.zwave.ZWProtocol.META_ZWAVE_DEVICE_NODE_ID;
import static org.openremote.agent.protocol.zwave.ZWProtocol.META_ZWAVE_DEVICE_VALUE_LINK;
import static org.openremote.agent.protocol.zwave.ZWProtocol.META_ZWAVE_SERIAL_PORT;
import static org.openremote.model.attribute.MetaItemType.PROTOCOL_CONFIGURATION;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class ZWConfiguration {

    public static int getZWNodeId(AssetAttribute assetAttribute) {
        return assetAttribute
            .getMetaItem(META_ZWAVE_DEVICE_NODE_ID)
            .flatMap(AbstractValueHolder::getValueAsInteger)
            .orElse(0);
    }

    public static int getZWEndpoint(AssetAttribute assetAttribute) {
        return assetAttribute
            .getMetaItem(META_ZWAVE_DEVICE_ENDPOINT)
            .flatMap(AbstractValueHolder::getValueAsInteger)
            .orElse(0);
    }

    public static String getZWLinkName(AssetAttribute assetAttribute) {
        return assetAttribute
            .getMetaItem(META_ZWAVE_DEVICE_VALUE_LINK)
            .flatMap(AbstractValueHolder::getValueAsString)
            .map(String::toUpperCase)
            .orElse("");
    }

    public static String getEndpointIdAsString(AssetAttribute assetAttribute)
    {
      return getZWNodeId(assetAttribute) + ":" + getZWEndpoint(assetAttribute);
    }

    public static boolean validateSerialConfiguration(AssetAttribute protocolConfiguration, AttributeValidationResult result) {
        boolean failure = false;

        if (!isSerialConfiguration(protocolConfiguration)) {
            failure = true;
            if (result != null) {
                result.addAttributeFailure(
                    new ValidationFailure(
                        ValueHolder.ValueFailureReason.VALUE_MISMATCH,
                        ZWProtocol.PROTOCOL_NAME));
            }
        }

        boolean portFound = false;

        if (protocolConfiguration.getMeta() != null && !protocolConfiguration.getMeta().isEmpty()) {
            for (int i = 0; i < protocolConfiguration.getMeta().size(); i++) {
                MetaItem metaItem = protocolConfiguration.getMeta().get(i);
                if (isMetaNameEqualTo(metaItem, ZWProtocol.META_ZWAVE_SERIAL_PORT)) {
                    portFound = true;
                    if (isNullOrEmpty(metaItem.getValueAsString().orElse(null))) {
                        failure = true;
                        if (result == null) {
                            break;
                        }
                        result.addMetaFailure(i,
                            new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_VALUE_IS_REQUIRED, ValueType.STRING.name()));
                    }
                }
            }
        }

        if (!portFound) {
            failure = true;
            if (result != null) {
                result.addMetaFailure(
                    new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_MISSING, META_ZWAVE_SERIAL_PORT)
                );
            }
        }
        
        return !failure;
    }

    public static boolean isSerialConfiguration(AssetAttribute attribute) {
        return attribute != null && attribute.getValueAsString().map(value -> value.equals(ZWProtocol.PROTOCOL_NAME)).orElse(false);
    }

    public static boolean isValidProtocolName(String protocolName) {
        return TextUtil.isValidURN(protocolName);
    }

    public static void isValidProtocolNameOrThrow(String protocolName) throws IllegalArgumentException {
        if (!isValidProtocolName(protocolName)) {
            throw new IllegalArgumentException("Protocol name must start with 'urn:' but is: " + protocolName);
        }
    }

    public static AssetAttribute initProtocolConfiguration(AssetAttribute attribute, String protocolName) throws IllegalArgumentException {
        if (attribute == null) {
            return null;
        }
        isValidProtocolNameOrThrow(protocolName);
        attribute.setReadOnly(true);
        attribute.setType(AttributeValueType.STRING);
        attribute.setValue(Values.create(protocolName));
        attribute.getMeta().add(new MetaItem(PROTOCOL_CONFIGURATION, Values.create(true)));
        attribute.getMeta().add(new MetaItem(META_ZWAVE_SERIAL_PORT, Values.create("/dev/ttyACM0")));
        return attribute;
    }
}
