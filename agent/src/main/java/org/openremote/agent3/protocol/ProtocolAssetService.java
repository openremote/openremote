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
package org.openremote.agent3.protocol;

import org.openremote.container.ContainerService;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;

/**
 * Interface for protocols to perform limited asset
 * related operations.
 * <p>
 * <ul>
 *     <li>Send attribute events into the processing chain</li>
 *     <li>Update their own protocol configurations</li>
 * </ul>
 */
public interface ProtocolAssetService extends ContainerService {
    void sendAttributeEvent(AttributeEvent attributeEvent);

    // TODO: Find a way of limiting protocols to only modifying their own protocol configurations
    void updateProtocolConfiguration(AssetAttribute protocolConfiguration);
}
