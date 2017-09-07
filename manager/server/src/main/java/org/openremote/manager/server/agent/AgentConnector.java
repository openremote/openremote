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
package org.openremote.manager.server.agent;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.file.FileInfo;

/**
 * This is an interface for communicating with an agent. It
 * should be stateless and re-usable.
 */
public interface AgentConnector {

    ProtocolDescriptor[] getProtocolDescriptors(Asset agent);

    AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration);

    /**
     * Ask the protocol for discovered linked {@link AssetAttribute}s.
     * <p>
     * <b>NOTE:</b>The protocol must implement {@link org.openremote.agent.protocol.ProtocolLinkedAttributeDiscovery}
     *
     * @throws IllegalArgumentException when the protocol cannot be found for this {@link org.openremote.model.asset.agent.ProtocolConfiguration} reference
     * @throws UnsupportedOperationException when the protocol doesn't implement {@link org.openremote.agent.protocol.ProtocolLinkedAttributeDiscovery}
     */
    Asset[] getDiscoveredLinkedAttributes(AttributeRef protocolConfigurationRef) throws IllegalArgumentException, UnsupportedOperationException;

    /**
     * Ask the protocol for discovered linked {@link AssetAttribute}s using the supplied file (protocol specific file).
     * <p>
     * <b>NOTE:</b>The protocol must implement {@link org.openremote.agent.protocol.ProtocolLinkedAttributeImport}
     *
     * @throws IllegalArgumentException when the protocol cannot be found for this {@link org.openremote.model.asset.agent.ProtocolConfiguration} reference
     * @throws UnsupportedOperationException when the protocol doesn't implement {@link org.openremote.agent.protocol.ProtocolLinkedAttributeDiscovery}
     * @throws IllegalStateException thrown by the protocol if an error occurs processing the supplied {@link FileInfo}
     */
    Asset[] getDiscoveredLinkedAttributes(AttributeRef protocolConfigurationRef, FileInfo fileInfo) throws  IllegalArgumentException, UnsupportedOperationException, IllegalStateException;
}
