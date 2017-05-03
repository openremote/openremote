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
package org.openremote.model.asset.agent;

import elemental.json.Json;
import org.openremote.model.AttributeType;
import org.openremote.model.Constants;
import org.openremote.model.Meta;
import org.openremote.model.asset.AssetAttribute;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Agent attributes can be named protocol configurations.
 * <p>
 * A protocol configuration attribute value must be a URN string representation
 * starting with {@link Constants#PROTOCOL_NAMESPACE}.
 * <p>
 * TODO: How can we integrate 3rd party protocol namespaces?
 * <p>
 * Configuration details are managed as {@link Meta} of the attribute.
 */
final public class ProtocolConfiguration {
    public static final Predicate<String> PROTOCOL_NAME_VALIDATOR = value ->
        value != null && value
            .toLowerCase(Locale.ROOT)
            .startsWith(Constants.PROTOCOL_NAMESPACE);

    private ProtocolConfiguration() {
    }

    public static AssetAttribute initProtocolConfiguration(AssetAttribute attribute, String protocolName) throws IllegalArgumentException {
        if (attribute == null) {
            return null;
        }

        isValidProtocolNameOrThrow(protocolName);
        attribute.setEnabled(true);
        attribute.setType(AttributeType.STRING);
        attribute.setValue(Json.create(protocolName));
        return attribute;
    }

    public static UnaryOperator<AssetAttribute> initProtocolConfiguration(String protocolName) throws IllegalArgumentException {
        return attribute -> initProtocolConfiguration(attribute, protocolName);
    }

    public static boolean isValidProtocolName(String protocolName) {
        return PROTOCOL_NAME_VALIDATOR.test(protocolName);
    }

    public static void isValidProtocolNameOrThrow(String protocolName) throws IllegalArgumentException {
        if (!isValidProtocolName(protocolName)) {
            throw new IllegalArgumentException("Protocol name must start with the protocol URN prefix: " + Constants.PROTOCOL_NAMESPACE);
        }
    }

    public static boolean isProtocolConfiguration(AssetAttribute attribute) {
        return getProtocolName(attribute)
            .isPresent();
    }

    // TODO: Should this be able to validate the protocol name
    public static boolean isValidProtocolConfiguration(AssetAttribute attribute) {
        return isProtocolConfiguration(attribute);
    }

    public static Optional<String> getProtocolName(AssetAttribute attribute) {
        return attribute
            .getValueAsString()
            .map(name -> isValidProtocolName(name) ? name : null);
    }

    public static AssetAttribute setProtocolName(AssetAttribute attribute, String protocolName) throws IllegalArgumentException {
        if (attribute == null) {
            return null;
        }
        isValidProtocolNameOrThrow(protocolName);
        attribute.setValue(Json.create(protocolName));
        return attribute;
    }

    public static UnaryOperator<AssetAttribute> setProtocolName(String protocolName) throws IllegalArgumentException {
        return attribute -> setProtocolName(attribute, protocolName);
    }
}
