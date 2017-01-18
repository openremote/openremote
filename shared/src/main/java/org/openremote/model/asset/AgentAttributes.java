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
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;
import org.openremote.model.Attributes;

import java.util.ArrayList;
import java.util.List;

public class AgentAttributes extends Attributes {

    public AgentAttributes() {
        setEnabled(false);
    }

    public AgentAttributes(Asset agentAsset) {
        super(agentAsset.getAttributes());
    }

    public ProtocolConfiguration[] getProtocolConfigurations() {
        Attribute[] attributes = super.get();
        List<ProtocolConfiguration> list = new ArrayList<>();
        for (Attribute attribute : attributes) {
            if (ProtocolConfiguration.isProtocolConfiguration(attribute)) {
                list.add(new ProtocolConfiguration(attribute));
            }
        }
        return list.toArray(new ProtocolConfiguration[list.size()]);
    }

    public ProtocolConfiguration getProtocolConfiguration(String name) {
        Attribute attribute = super.get(name);
        if (attribute == null || !ProtocolConfiguration.isProtocolConfiguration(attribute))
            return null;
        return new ProtocolConfiguration(attribute);
    }

    public boolean isEnabled() {
        return hasAttribute("enabled") && get("enabled").isValueTrue();
    }

    public AgentAttributes setEnabled(boolean enabled) {
        if (hasAttribute("enabled")) {
            get("enabled").setValue(Json.create(enabled));
        } else {
            put(new Attribute("enabled", AttributeType.BOOLEAN, Json.create(enabled)));
        }
        return this;
    }

}
