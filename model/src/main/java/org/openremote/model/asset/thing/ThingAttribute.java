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
package org.openremote.model.asset.thing;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeType;
import org.openremote.model.asset.AbstractAssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.agent.ProtocolConfiguration;

public class ThingAttribute extends AbstractAssetAttribute<ThingAttribute> {

    protected ProtocolConfiguration protocolConfiguration;

    public static boolean isLinkedAttribute(ThingAttribute attribute) {
        return getAgentLink(attribute) != null;
    }

    static public AttributeRef getAgentLink(ThingAttribute attribute) {
        JsonArray array = attribute.hasMetaItem(AssetMeta.AGENT_LINK)
            ? attribute.firstMetaItem(AssetMeta.AGENT_LINK).getValueAsArray()
            : null;
        if (array == null || array.length() != 2)
            return null;
        return new AttributeRef(array);
    }

    public ThingAttribute() {
    }

    public ThingAttribute(String assetId) {
        super(assetId);
    }

    public ThingAttribute(String name, AttributeType type) {
        super(name, type);
    }

    public ThingAttribute(String name, JsonObject jsonObject) {
        super(name, jsonObject);
    }

    public ThingAttribute(String name, AttributeType type, JsonValue value) {
        super(name, type, value);
    }

    public ThingAttribute(String assetId, String name) {
        super(assetId, name);
    }

    public ThingAttribute(String assetId, String name, AttributeType type) {
        super(assetId, name, type);
    }

    public ThingAttribute(String assetId, String name, JsonObject jsonObject) {
        super(assetId, name, jsonObject);
    }

    public ThingAttribute(String assetId, String name, AttributeType type, JsonValue value) {
        super(assetId, name, type, value);
    }

    public ThingAttribute(AbstractAssetAttribute attribute) {
        this(attribute, null);
    }

    public ThingAttribute(AbstractAssetAttribute attribute, ProtocolConfiguration protocolConfiguration) {
        this(attribute.assetId, protocolConfiguration, attribute.getName(), attribute.getJsonObject());
    }

    public ThingAttribute(String assetId, ProtocolConfiguration protocolConfiguration, String name, JsonObject jsonObject) {
        super(assetId, name, jsonObject);
        this.protocolConfiguration = protocolConfiguration;
    }

    @Override
    public ThingAttribute copy() {
        return new ThingAttribute(assetId, protocolConfiguration, getName(), Json.parse(getJsonObject().toJson()));
    }

    /**
     * Linked thing attributes have a protocol configuration of their agent after they are resolved.
     */
    public ProtocolConfiguration getProtocolConfiguration() {
        return protocolConfiguration;
    }
}
