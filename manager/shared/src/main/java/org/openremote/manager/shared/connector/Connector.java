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
package org.openremote.manager.shared.connector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.manager.shared.attribute.Attributes;

@JsonSerialize
public interface Connector {
    /**
     * Get the unique type descriptor for this connector component
     */
    @JsonProperty
    String getType();

    /**
     * Get the friendly display name for this connector component
     */
    @JsonProperty
    String getDisplayName();

    /**
     * Indicates whether or not this connector component supports
     * discovery of Agents (connector instances).
     */
    @JsonProperty
    boolean supportsAgentDiscovery();

    /**
     * Get the attributes defining the configurable settings for
     * agent discovery on this connector (if discovery not supported or
     * no settings required then return null or empty attributes).
     */
    @JsonProperty
    Attributes getAgentDiscoverySettings();

    /**
     * Get the attributes defining the configurable settings for
     * an agent that uses this connector.
     */
    @JsonProperty
    Attributes getAgentSettings();
}
