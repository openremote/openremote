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

import org.openremote.model.Attribute;
import org.openremote.model.AttributeRef;
import org.openremote.model.asset.AssetMeta;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.openremote.model.Attribute.*;
import static org.openremote.model.asset.AssetMeta.AGENT_LINK;

/**
 * An asset attribute can be linked to an agent's {@link ProtocolConfiguration},
 * connecting it to a sensor and/or actuator.
 * <p>
 * Value changes will be send to the protocol (actuators) and the protocol can update the attribute's
 * value (sensors).
 * <p>
 * The link is configured through an {@link AttributeRef} in {@link AssetMeta#AGENT_LINK}.
 */
final public class AgentLink {

    private AgentLink() {
    }

    public static Predicate<Attribute> isAgentLink() {
        return attribute -> getAgentLink().apply(attribute) != null;
    }

    public static Predicate<Attribute> isValidAgentLink() {
        return isAttributeValid().and(isAgentLink());
    }

    public static Function<Attribute, AttributeRef> getAgentLink() {
        return getAttributeLink(AGENT_LINK.getUrn());
    }

    public static Function<Attribute, Attribute> setAgentLink(AttributeRef protocolConfigurationRef) {
        return setAttributeLink(AGENT_LINK.getUrn(), protocolConfigurationRef);
    }

    public static Function<Attribute, Attribute> removeAgentLink() {
        return removeAttributeLink(AGENT_LINK.getUrn());
    }
}
