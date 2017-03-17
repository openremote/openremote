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

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeRef;

public class ThingAttribute extends Attribute {

    final protected ProtocolConfiguration protocolConfiguration;
    final protected String thingId;

    public static boolean isLinkedAttribute(Attribute attribute) {
        return getAgentLink(attribute) != null;
    }

    static public AttributeRef getAgentLink(Attribute attribute) {
        JsonArray array = attribute.hasMetaItem(AssetMeta.AGENT_LINK)
            ? attribute.firstMetaItem(AssetMeta.AGENT_LINK).getValueAsArray()
            : null;
        if (array == null || array.length() != 2)
            return null;
        return new AttributeRef(array);
    }

    public ThingAttribute(String thingId, ProtocolConfiguration protocolConfiguration, Attribute attribute) {
        this(thingId, protocolConfiguration, attribute.getName(), attribute.getJsonObject());
    }

    public ThingAttribute(String thingId, ProtocolConfiguration protocolConfiguration, String name, JsonObject jsonObject) {
        super(name, jsonObject);
        this.thingId = thingId;
        this.protocolConfiguration = protocolConfiguration;
    }

    public ProtocolConfiguration getProtocolConfiguration() {
        return protocolConfiguration;
    }

    public AttributeRef getAttributeRef() {
        return new AttributeRef(thingId, getName());
    }

    public String getThingId() {
        return thingId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "protocolConfiguration=" + protocolConfiguration +
            ", thingId='" + thingId + '\'' +
            "} " + super.toString();
    }
}
