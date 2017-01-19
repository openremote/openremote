/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.asset;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;

import java.util.Locale;

/**
 * Agent attributes can be named protocol configurations, the value is a protocol URN.
 * <p>
 * Configuration details are managed as {@link org.openremote.model.Metadata} of the attribute.
 */
public class ProtocolConfiguration extends Attribute {

    /**
     * A protocol configuration attribute value must be a URN string representation starting with "urn:".
     */
    public static boolean isProtocolConfiguration(Attribute attribute) {
        return attribute.getType() == AttributeType.STRING
            && attribute.getValue() != null
            && attribute.getValueAsString().toLowerCase(Locale.ROOT).startsWith("urn:");
    }

    public ProtocolConfiguration(Attribute attribute) {
        this(attribute.getName(), attribute.getJsonObject());
    }

    public ProtocolConfiguration(String name, JsonObject jsonObject) {
        super(name, jsonObject);
    }

    public ProtocolConfiguration(String name, String protocol) {
        super(name, AttributeType.STRING, Json.create(protocol));
    }

    public String getProtocolName() {
        return getValue() != null ? getValueAsString() : null;
    }

}
