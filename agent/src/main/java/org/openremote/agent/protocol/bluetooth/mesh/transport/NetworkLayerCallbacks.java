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

import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.Provisioner;

import java.util.List;

public interface NetworkLayerCallbacks {

    /**
     * Callback to retrieve the current provisioner of the network
     */
    Provisioner getProvisioner();

    /**
     * Callback to retrieve a provisioner of the mesh network
     *
     * @param unicastAddress address of the provisioner
     */
    Provisioner getProvisioner(final int unicastAddress);

    /**
     * Callback to retrieve the primary network key of the mesh network.
     * <p>This usually is the key with the 0th index in the netkey list</p>
     */
    NetworkKey getPrimaryNetworkKey();

    /**
     * Callback to retrieve the network key of the mesh network.
     *
     * @param keyIndex Index of the network key
     */
    NetworkKey getNetworkKey(final int keyIndex);

    /**
     * Callback to retrieve the list of {@link NetworkKey} belonging to this network.
     */
    List<NetworkKey> getNetworkKeys();
}
