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
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;
import org.openremote.model.Constants;
import org.openremote.model.Meta;
import org.openremote.model.asset.AssetAttribute;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.openremote.model.Attribute.Functions.isOfType;
import static org.openremote.model.Attribute.Functions.isValid;
import static org.openremote.model.AttributeType.STRING;

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

    private ProtocolConfiguration() {
    }

    public static UnaryOperator<AssetAttribute> initProtocolConfiguration(String protocolName) {
        return attribute -> {
            attribute.setEnabled(true);
            attribute.setType(AttributeType.STRING);
            attribute.setValue(Json.create(protocolName));
            return attribute;
        };
    }

    public static <A extends Attribute> Predicate<A> isProtocolConfiguration() {
        return attribute -> isProtocolUrn().test(attribute.getValueAsString());
    }

    @SuppressWarnings("unchecked")
    public static <A extends Attribute> Predicate<A> isValidProtocolConfiguration() {
        return (Predicate<A>) isValid()
            .and(isOfType(STRING))
            .and(isProtocolConfiguration());
    }

    public static Predicate<String> isProtocolUrn() {
        return value -> value != null && value.toLowerCase(Locale.ROOT).startsWith(Constants.PROTOCOL_NAMESPACE);
    }

    public static UnaryOperator<AssetAttribute> setProtocolName(String protocolName) throws IllegalArgumentException {
        return attribute -> {
            if (!isProtocolUrn().test(protocolName)) {
                throw new IllegalArgumentException("Protocol configuration value should contain a protocol URN");
            }
            attribute.setValue(Json.create(protocolName));
            return attribute;
        };
    }

    public static Function<AssetAttribute, String> getProtocolName() {
        return Attribute::getValueAsString;
    }
}
