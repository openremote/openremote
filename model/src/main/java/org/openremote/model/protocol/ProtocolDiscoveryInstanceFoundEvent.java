/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.event.shared.SharedEvent;

/**
 * Raised when a protocol instance has been discovered for the specified agent type
 */
public class ProtocolDiscoveryInstanceFoundEvent extends SharedEvent {

    protected String agentDescriptor;
    protected String instanceName;
    protected Attribute<?>[] attributes;

    @JsonCreator
    public ProtocolDiscoveryInstanceFoundEvent(@JsonProperty("agentDescriptor") String agentDescriptor, @JsonProperty("instanceName") String instanceName, @JsonProperty("attributes") Attribute<?>[] attributes) {
        this.agentDescriptor = agentDescriptor;
        this.instanceName = instanceName;
        this.attributes = attributes;
    }

    public String getAgentDescriptor() {
        return agentDescriptor;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Attribute<?>[] getAttributes() {
        return attributes;
    }
}
