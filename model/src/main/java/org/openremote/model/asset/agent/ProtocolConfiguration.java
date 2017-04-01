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
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.*;
import org.openremote.model.asset.AbstractAssetAttribute;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.thing.ThingAttribute;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Agent attributes can be named protocol configurations, the value is a protocol URN.
 * <p>
 * Configuration details are managed as {@link Meta} of the attribute.
 */
public class ProtocolConfiguration extends AgentAttribute<ProtocolConfiguration> {

    public ProtocolConfiguration(String name, String protocol) {
        super(null, name, AttributeType.STRING, Json.create(protocol));
    }

    public ProtocolConfiguration(String assetId, String name, String protocol) {
        super(assetId, name, AttributeType.STRING, Json.create(protocol));
    }

    public ProtocolConfiguration(String assetId, String name, JsonObject jsonObject) {
        super(assetId, name, jsonObject);
    }

    public ProtocolConfiguration(AbstractAssetAttribute attribute) {
        this(attribute.getAssetId(), attribute.getName(), attribute.getJsonObject());
    }

    public boolean isEnabled() {
        MetaItem enabled = firstMetaItem(AssetMeta.ENABLED);
        return enabled != null ? enabled.getValueAsBoolean() : true;
    }

    public void setEnabled(boolean enabled) {
        Meta meta = getMeta();
        meta.removeAll(AssetMeta.ENABLED.getName());
        meta.add(new MetaItem(AssetMeta.ENABLED, Json.create(enabled)));
        setMeta(meta);
    }

    @Override
    public ProtocolConfiguration setValue(JsonValue value) throws IllegalArgumentException {
        super.setValue(value);
        if (!isValueValid()) {
            throw new IllegalArgumentException("Protocol configuration value should contain a protocol URN");
        }
        return this;
    }

    public String getProtocolName() {
        return getValueAsString();
    }

    public boolean isValueValid() {
        return getType() == AttributeType.STRING && getValueAsString() != null && getValueAsString().startsWith(Constants.PROTOCOL_NAMESPACE);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && isValueValid();
    }

    /**
     * A protocol configuration attribute value must be a URN string representation starting with {@link Constants#PROTOCOL_NAMESPACE}.
     */
    public static boolean isProtocolConfiguration(AbstractAssetAttribute attribute) {
        return attribute.getType() == AttributeType.STRING
                && attribute.hasValue()
                && attribute.getValueAsString().toLowerCase(Locale.ROOT).startsWith(Constants.PROTOCOL_NAMESPACE);
    }

    /**
     * Get all protocol configuration attributes from the asset
     */
    public static List<ProtocolConfiguration> getAll(Asset agent) {
        return getAll(new AgentAttributes(agent).get());
    }

    /**
     * Get all protocol configuration attributes from the agent attributes
     */
    public static List<ProtocolConfiguration> getAll(List<AgentAttribute> attributes) {
        return attributes
                .stream()
                .filter(ProtocolConfiguration::isProtocolConfiguration)
                .map(ProtocolConfiguration::new)
                .collect(Collectors.toList());
    }
}
