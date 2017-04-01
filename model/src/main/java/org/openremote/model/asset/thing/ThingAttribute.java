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
import org.openremote.model.*;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.ProtocolConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ThingAttribute extends AbstractAssetAttribute<ThingAttribute> {

    protected ProtocolConfiguration protocolConfiguration;

    public ThingAttribute(ProtocolConfiguration protocolConfiguration, AssetAttribute attribute) {
        this(protocolConfiguration, attribute.getAssetId(), attribute.getName(), attribute.getJsonObject());
    }

    public ThingAttribute(ProtocolConfiguration protocolConfiguration, AttributeRef protocolRef, String assetId, String name, AttributeType type, JsonValue value) {
        super(assetId, name, type, value);
        setProtocolRef(protocolRef);
        this.protocolConfiguration = protocolConfiguration;
    }

    public ThingAttribute(ProtocolConfiguration protocolConfiguration, String assetId, String name, JsonObject jsonObject) {
        super(assetId, name, jsonObject);
        this.protocolConfiguration = protocolConfiguration;

        // Verify jsonObject is valid
        if (!isValid()) {
            throw new IllegalArgumentException("Supplied JSON object is not valid for this type of attribute: " + jsonObject.toJson());
        }
    }

    @Override
    public ThingAttribute copy() {
        return new ThingAttribute(protocolConfiguration.copy(), assetId, getName(), Json.parse(getJsonObject().toJson()));
    }

    public ProtocolConfiguration getProtocolConfiguration() {
        return protocolConfiguration;
    }

    /**
     * Get the referenced protocol configuration for this thing attribute; if it is not populated then use the supplied
     * resolver to go and populate it before returning the value
     */
    public ProtocolConfiguration getProtocolConfiguration(Function<AttributeRef, ProtocolConfiguration> protocolConfigurationResolver) {
        if (protocolConfiguration == null && protocolConfigurationResolver != null) {
            protocolConfiguration = protocolConfigurationResolver.apply(getProtocolRef(this));
        }
        return protocolConfiguration;
    }

    public AttributeRef getProtocolRef() {
        return getProtocolRef(this);
    }

    protected void setProtocolRef(AttributeRef attributeRef) {
        if (attributeRef == null) {
            throw new IllegalArgumentException("AttributeRef cannot be null");
        }

        Meta meta = hasMeta() ? getMeta() : new Meta();
        meta.removeAll(AssetMeta.AGENT_LINK);
        meta.add(new MetaItem(AssetMeta.AGENT_LINK, attributeRef.asJsonValue()));
        setMeta(meta);
        protocolConfiguration = null;
    }

    /**
     * Does super class checks and also checks for valid Protocol Ref
     */
    @Override
    public boolean isValid() {
        return super.isValid() && getProtocolRef(this) != null;
    }

    public static AttributeRef getProtocolRef(AbstractAssetAttribute attribute) {
        MetaItem metaItem = attribute.firstMetaItem(AssetMeta.AGENT_LINK);
        JsonArray array = metaItem != null ? metaItem.getValueAsArray() : null;
        return array != null && array.length() == 2 ? new AttributeRef(array) : null;
    }

    /**
     * Gets all thing attributes from the asset and groups them by ProtocolRef
     */
    public static Map<AttributeRef, List<ThingAttribute>> getAllGroupedByProtocolRef(Asset asset, Function<AttributeRef, ProtocolConfiguration> protocolConfigurationResolver) {
        return getAllGroupedByProtocolRef(new AssetAttributes(asset).get(), protocolConfigurationResolver);
    }

    /**
     * Gets all thing attributes from the asset and groups them by ProtocolRef
     */
    public static Map<AttributeRef, List<ThingAttribute>> getAllGroupedByProtocolRef(List<AssetAttribute> attributes, Function<AttributeRef, ProtocolConfiguration> protocolConfigurationResolver) {
        return attributes
                .stream()
                .filter(AssetAttribute::isAgentLinked)
                .map(attribute -> get(attribute, protocolConfigurationResolver))
                .filter(thingAttribute -> thingAttribute != null)
                .collect(Collectors.groupingBy(thingAttribute -> thingAttribute.getProtocolRef()));
    }

    /**
     * Gets all thing attributes from the asset
     */
    public static List<ThingAttribute> getAll(Asset asset, Function<AttributeRef, ProtocolConfiguration> protocolConfigurationResolver) {
        return getAll(new AssetAttributes(asset).get(), protocolConfigurationResolver);
    }

    /**
     * Gets all thing attributes from the asset
     */
    public static List<ThingAttribute> getAll(List<AssetAttribute> attributes, Function<AttributeRef, ProtocolConfiguration> protocolConfigurationResolver) {
        return attributes
                .stream()
                .filter(AssetAttribute::isAgentLinked)
                .filter(thingAttribute -> thingAttribute != null)
                .map(attribute -> get(attribute, protocolConfigurationResolver))
                .collect(Collectors.toList());
    }

    public static ThingAttribute get(AssetAttribute attribute, Function<AttributeRef, ProtocolConfiguration> protocolConfigurationResolver) {
        AttributeRef protocolRef = getProtocolRef(attribute);
        if (protocolRef == null) {
            return null;
        }
        ProtocolConfiguration protocolConfiguration = protocolConfigurationResolver.apply(protocolRef);

        if (protocolConfiguration == null) {
            return null;
        }

        return new ThingAttribute(protocolConfiguration, attribute);
    }
}
