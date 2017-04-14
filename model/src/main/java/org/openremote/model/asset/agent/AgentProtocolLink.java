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

import elemental.json.JsonArray;
import org.openremote.model.AttributeRef;
import org.openremote.model.Meta;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AbstractAttributeWrapper;
import org.openremote.model.asset.AttributeWrapperFilter;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;

import java.util.List;
import java.util.stream.Collectors;

public class AgentProtocolLink extends AbstractAttributeWrapper <AgentProtocolLink> {
    public static class Filter implements AttributeWrapperFilter<AgentProtocolLink> {
        @Override
        public List<AgentProtocolLink> getAllWrapped(List<AssetAttribute> assetAttributes, boolean excludeInvalid) {
            return getAll(assetAttributes, excludeInvalid)
                .stream()
                .map(assetAttribute -> new AgentProtocolLink(assetAttribute, false))
                .collect(Collectors.toList());
        }

        @Override
        public List<AssetAttribute> getAll(List<AssetAttribute> assetAttributes, boolean excludeInvalid) {
            // Agent Linked Attribute wrappers should have protocol URN as the attribute value
            return assetAttributes
                .stream()
                .filter(attribute ->
                    AgentProtocolLink.hasAgentProtocolLink(attribute) && (!excludeInvalid || AgentProtocolLink.isValid(attribute))
                )
                .collect(Collectors.toList());
        }
    }

    protected static final AttributeWrapperFilter<AgentProtocolLink> filter = new Filter();

    public AgentProtocolLink(AssetAttribute attribute) {
        super(attribute);
    }

    public AgentProtocolLink(AssetAttribute attribute, boolean initialise) {
        super(attribute, initialise);
    }

    public AttributeRef getLink() {
        return getLink(getAttribute());
    }

    protected void setLink(AttributeRef attributeRef) {
        if (attributeRef == null) {
            throw new IllegalArgumentException("AttributeRef cannot be null");
        }

        Meta meta = getMeta().removeAll(AssetMeta.AGENT_LINK);
        meta.add(new MetaItem(AssetMeta.AGENT_LINK, attributeRef.asJsonValue()));
        setMeta(meta);
    }

    protected void removeAgentProtocolLink() {
        Meta meta = getMeta().removeAll(AssetMeta.AGENT_LINK);
        setMeta(meta);
    }

    @Override
    public void initialise() {
        // Nothing to do here
    }

    @Override
    public AttributeWrapperFilter<AgentProtocolLink> getFilter() {
        return null;
    }

    /**
     * Does super class checks and also checks for valid Protocol Ref
     */
    @Override
    public boolean isValid() {
        return isValid(getAttribute());
    }

    public static boolean isValid(AssetAttribute assetAttribute) {
        return AbstractAttributeWrapper.isValid(assetAttribute) && hasAgentProtocolLink(assetAttribute);
    }

    public static AttributeRef getLink(AssetAttribute assetAttribute) {
        MetaItem metaItem = assetAttribute.firstMetaItem(AssetMeta.AGENT_LINK);
        JsonArray array = metaItem != null ? metaItem.getValueAsArray() : null;
        return array != null && array.length() == 2 ? new AttributeRef(array) : null;
    }

    public static boolean hasAgentProtocolLink(AssetAttribute assetAttribute) {
        return getLink(assetAttribute) != null;
    }
}
