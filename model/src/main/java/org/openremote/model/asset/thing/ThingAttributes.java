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
import elemental.json.JsonObject;
import org.openremote.model.AttributeRef;
import org.openremote.model.Function;
import org.openremote.model.asset.AbstractAssetAttributes;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ProtocolConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThingAttributes extends AbstractAssetAttributes<ThingAttributes, ThingAttribute> {

    public ThingAttributes() {
    }

    public ThingAttributes(String assetId) {
        super(assetId);
    }

    public ThingAttributes(String assetId, JsonObject jsonObject) {
        super(assetId, jsonObject);
    }

    public ThingAttributes(ThingAttributes attributes) {
        super(attributes);
    }

    public ThingAttributes(Asset asset) {
        super(asset);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ThingAttributes copy() {
        return new ThingAttributes(assetId, Json.parse(getJsonObject().toJson()));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ThingAttribute createAttribute(String name, JsonObject jsonObject) {
        return new ThingAttribute(assetId, null, name, jsonObject);
    }

    /**
     * Get all attributes which are linked to an agent, grouped by protocol name.
     */
    public Map<String, List<ThingAttribute>> getLinkedAttributes(Function<AttributeRef, ProtocolConfiguration> linkResolver) {
        List<ThingAttribute> attributes = super.get();

        // Resolve all which are linked to an agent
        List<ThingAttribute> linkedAttributes = new ArrayList<>();
        for (ThingAttribute attribute : attributes) {
            ThingAttribute linkedAttribute = getLinkedAttribute(linkResolver, attribute.getName());
            if (linkedAttribute != null) {
                linkedAttributes.add(linkedAttribute);
            }
        }

        // Group by protocol name
        Map<String, List<ThingAttribute>> result = new HashMap<>();
        for (ThingAttribute linkedAttribute : linkedAttributes) {
            String protocolName = linkedAttribute.getProtocolConfiguration().getProtocolName();
            if (!result.containsKey(protocolName)) {
                result.put(protocolName, new ArrayList<>());
            }
            result.get(protocolName).add(linkedAttribute);
        }
        return result;
    }

    public ThingAttribute getLinkedAttribute(Function<AttributeRef, ProtocolConfiguration> linkResolver,
                                             String attributeName) {
        ThingAttribute attribute = get(attributeName);
        if (attribute == null || !ThingAttribute.isLinkedAttribute(attribute))
            return null;

        AttributeRef agentLink = ThingAttribute.getAgentLink(attribute);
        if (agentLink == null)
            return null;

        ProtocolConfiguration protocolConfiguration = linkResolver.apply(agentLink);
        if (protocolConfiguration == null) {
            return null;
        }

        return new ThingAttribute(attribute, protocolConfiguration);
    }
}
