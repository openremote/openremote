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
package org.openremote.manager.server.agent;

import org.openremote.model.Function;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeRef;
import org.openremote.model.Attributes;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.ProtocolConfiguration;
import org.openremote.model.asset.ThingAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ThingAttributes extends Attributes {

    private static final Logger LOG = Logger.getLogger(ThingAttributes.class.getName());

    final protected String thingId;

    public ThingAttributes(Asset thing) {
        super(thing.getAttributes());
        this.thingId = thing.getId();
    }

    public String getThingId() {
        return thingId;
    }

    public Map<String, List<ThingAttribute>> getLinkedAttributes(Function<AttributeRef, ProtocolConfiguration> linkResolver) {
        Attribute[] attributes = super.get();
        List<ThingAttribute> thingAttributes = new ArrayList<>();
        for (Attribute attribute : attributes) {
            ThingAttribute thingAttribute = getLinkedAttribute(linkResolver, attribute);
            if (thingAttribute != null) {
                thingAttributes.add(thingAttribute);
            }
        }

        Map<String, List<ThingAttribute>> result = new HashMap<>();
        for (ThingAttribute thingAttribute : thingAttributes) {
            String protocolName = thingAttribute.getProtocolConfiguration().getProtocolName();
            if (!result.containsKey(protocolName)) {
                result.put(protocolName, new ArrayList<>());
            }
            result.get(protocolName).add(thingAttribute);
        }
        return result;
    }

    public ThingAttribute getLinkedAttribute(Function<AttributeRef, ProtocolConfiguration> linkResolver,
                                             String attributeName) {
        return getLinkedAttribute(linkResolver, get(attributeName));
    }

    protected ThingAttribute getLinkedAttribute(Function<AttributeRef, ProtocolConfiguration> linkResolver,
                                                Attribute attribute) {
        if (attribute == null || !ThingAttribute.isLinkedAttribute(attribute))
            return null;

        AttributeRef agentLink = ThingAttribute.getAgentLink(attribute);
        if (agentLink == null)
            return null;

        ProtocolConfiguration protocolConfiguration = linkResolver.apply(agentLink);
        if (protocolConfiguration == null) {
            LOG.info("Protocol configuration not found in agent '" + agentLink + "', ignoring: " + attribute);
            return null;
        }

        return new ThingAttribute(getThingId(), protocolConfiguration, attribute);
    }
}
