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
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AbstractAttributeWrapper;
import org.openremote.model.asset.AttributeWrapperFilter;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Agent attributes can be named protocol configurations, the value is a protocol URN.
 * <p>
 * Configuration details are managed as {@link Meta} of the attribute.
 */
public class ProtocolConfiguration extends AbstractAttributeWrapper<ProtocolConfiguration> {
    public static class Filter implements AttributeWrapperFilter<ProtocolConfiguration> {
        @Override
        public List<ProtocolConfiguration> getAllWrapped(List<AssetAttribute> assetAttributes, boolean excludeInvalid) {
            return getAll(assetAttributes, excludeInvalid)
                .stream()
                .map(assetAttribute -> new ProtocolConfiguration(assetAttribute, false))
                .collect(Collectors.toList());
        }

        @Override
        public List<AssetAttribute> getAll(List<AssetAttribute> assetAttributes, boolean excludeInvalid) {
            // Macro Attribute wrappers should have macro protocol name as the attribute value
            return assetAttributes
                .stream()
                .filter(attribute ->
                    ProtocolConfiguration.valueIsProtocolUrn(attribute)
                        && (!excludeInvalid || ProtocolConfiguration.isValid(attribute)))
                .collect(Collectors.toList());
        }
    }

    public static final AttributeWrapperFilter<ProtocolConfiguration> filter = new Filter();

    @Override
    public void initialise() {
        // Push enabled flag as true by default
        if (!getAttribute().hasMetaItem(AssetMeta.ENABLED)) {
            Meta meta = getAttribute().getMeta().add(new MetaItem(AssetMeta.ENABLED, Json.create(true)));
            getAttribute().setMeta(meta);
        }
    }

    @Override
    public AttributeWrapperFilter<ProtocolConfiguration> getFilter() {
        return null;
    }

    public ProtocolConfiguration(String attributeName, String protocolName) {
        this(new AssetAttribute(attributeName, AttributeType.STRING));
        setProtocolName(protocolName);
    }

    public ProtocolConfiguration(AssetAttribute attribute) {
        super(attribute);
    }

    public ProtocolConfiguration(AssetAttribute attribute, boolean initialise) {
        super(attribute, initialise);
    }

    public boolean isEnabled() {
        MetaItem enabled = getMeta().first(AssetMeta.ENABLED);
        return enabled != null ? enabled.getValueAsBoolean() : true;
    }

    public void setEnabled(boolean enabled) {
        Meta meta = getMeta();
        meta.removeAll(AssetMeta.ENABLED.getName());
        meta.add(new MetaItem(AssetMeta.ENABLED, Json.create(enabled)));
        setMeta(meta);
    }

    public ProtocolConfiguration setProtocolName(String protocolName) throws IllegalArgumentException {

        if (!valueIsProtocolUrn(protocolName)) {
            throw new IllegalArgumentException("Protocol configuration value should contain a protocol URN");
        }

        getAttribute().setValue(Json.create(protocolName));
        return this;
    }

    public String getProtocolName() {
        return getAttribute().getValueAsString();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && isValid(getAttribute());
    }

    /**
     * A protocol configuration attribute value must be a URN string representation starting with {@link Constants#PROTOCOL_NAMESPACE}.
     */
    public static boolean isValid(AssetAttribute assetAttribute) {
        // Value must be protocol name and must be at least one macro action
        return assetAttribute.getType() == AttributeType.STRING && valueIsProtocolUrn(assetAttribute);
    }

    protected static boolean valueIsProtocolUrn(AssetAttribute assetAttribute) {
        String value = assetAttribute.getValueAsString();
        return valueIsProtocolUrn(value);
    }

    protected static boolean valueIsProtocolUrn(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith(Constants.PROTOCOL_NAMESPACE);
    }
}
