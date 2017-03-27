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
import org.openremote.model.asset.AbstractAssetAttributes;
import org.openremote.model.asset.Asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgentAttributes extends AbstractAssetAttributes<AgentAttributes, AgentAttribute> {

    public AgentAttributes() {
    }

    public AgentAttributes(String assetId) {
        super(assetId);
    }

    public AgentAttributes(String assetId, JsonObject jsonObject) {
        super(assetId, jsonObject);
    }

    public AgentAttributes(AgentAttributes attributes) {
        super(attributes);
    }

    public AgentAttributes(Asset asset) {
        super(asset);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AgentAttributes copy() {
        return new AgentAttributes(assetId, Json.parse(getJsonObject().toJson()));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected AgentAttribute createAttribute(String name, JsonObject jsonObject) {
        return new AgentAttribute(assetId, name, jsonObject);
    }

    public List<ProtocolConfiguration> getProtocolConfigurations() {
        List<AgentAttribute> attributes = super.get();
        List<ProtocolConfiguration> protocolConfigurations = new ArrayList<>();
        for (AgentAttribute attribute : attributes) {
            if (ProtocolConfiguration.isProtocolConfiguration(attribute)) {
                protocolConfigurations.add(new ProtocolConfiguration(attribute));
            }
        }
        return Collections.unmodifiableList(protocolConfigurations);
    }

    public ProtocolConfiguration getProtocolConfiguration(String name) {
        AgentAttribute attribute = super.get(name);
        if (attribute == null || !ProtocolConfiguration.isProtocolConfiguration(attribute))
            return null;
        return new ProtocolConfiguration(attribute);
    }

}
