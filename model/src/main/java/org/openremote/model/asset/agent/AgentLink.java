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

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.asset.MetaItemType;

import java.util.Optional;

import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.attribute.MetaItem.replaceMetaByName;
import static org.openremote.model.asset.MetaItemType.AGENT_LINK;

/**
 * An asset attribute can be linked to an agent's {@link ProtocolConfiguration},
 * connecting it to a sensor and/or actuator.
 * <p>
 * Value changes will be send to the protocol (actuators) and the protocol can update the attribute's
 * value (sensors).
 * <p>
 * The link is configured through an {@link AttributeRef} in {@link MetaItemType#AGENT_LINK}.
 */
final public class AgentLink {

    private AgentLink() {
    }

    public static <A extends Attribute> boolean hasAgentLink(A attribute) {
        return attribute != null && attribute.getMetaStream().anyMatch(isMetaNameEqualTo(AGENT_LINK));
    }

    public static boolean isAgentLink(MetaItem metaItem) {
        return metaItem != null && isMetaNameEqualTo(metaItem, AGENT_LINK);
    }

    public static MetaItem asAgentLinkMetaItem(AttributeRef attributeRef) {
        return new MetaItem(AGENT_LINK, attributeRef.toArrayValue());
    }

    public static <A extends Attribute> Optional<AttributeRef> getAgentLink(A attribute) {
        return attribute == null ? Optional.empty() :
            attribute.getMetaItem(AGENT_LINK)
                .flatMap(AbstractValueHolder::getValue)
                .flatMap(AttributeRef::fromValue);
    }

    public static <A extends Attribute> void setAgentLink(A attribute, AttributeRef attributeRef) {
        if (attribute == null)
            return;

        replaceMetaByName(attribute.getMeta(), AGENT_LINK, asAgentLinkMetaItem(attributeRef));
    }

    public static <A extends Attribute> void removeAgentLink(A attribute) {
        if (attribute == null)
            return;

        attribute.getMeta().removeIf(isMetaNameEqualTo(AGENT_LINK));
    }
}
