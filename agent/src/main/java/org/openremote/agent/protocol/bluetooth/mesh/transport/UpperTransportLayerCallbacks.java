/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.Group;

import java.util.List;

/**
 * Upper transport layer call backs
 */
public interface UpperTransportLayerCallbacks {


    /**
     * Callback to get the mesh node from the list of provisioned mesh node.
     *
     * @param unicastAddress unicast address of the mesh node
     */
    ProvisionedMeshNode getNode(final int unicastAddress);

    /**
     * Returns the IV Index of the mesh network
     */
    byte[] getIvIndex();

    /**
     * Returns the application key with the specific application key identifier
     *
     * @param aid application key identifier
     */
    byte[] getApplicationKey(final int aid);

    /**
     * Returns a list of Application Keys matching the bound net key index and AID.
     *
     * @param boundNetKeyIndex Index of the bound network key.
     */
    List<ApplicationKey> getApplicationKeys(final int boundNetKeyIndex);

    /**
     * Returns the list of groups
     */
    List<Group> gerVirtualGroups();
}
