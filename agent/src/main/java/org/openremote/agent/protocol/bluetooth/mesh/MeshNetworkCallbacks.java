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
package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;

import java.util.List;

public interface MeshNetworkCallbacks {

    void onMeshNetworkUpdated();

    void onNetworkKeyAdded(final NetworkKey networkKey);

    void onNetworkKeyUpdated(final NetworkKey networkKey);

    void onNetworkKeyDeleted(final NetworkKey networkKey);

    void onApplicationKeyAdded(final ApplicationKey applicationKey);

    void onApplicationKeyUpdated(final ApplicationKey applicationKey);

    void onApplicationKeyDeleted(final ApplicationKey applicationKey);

    void onProvisionerAdded(final Provisioner provisioner);

    void onProvisionerUpdated(final Provisioner provisioner);

    void onProvisionersUpdated(final List<Provisioner> provisioner);

    void onProvisionerDeleted(final Provisioner provisioner);

    void onNodeDeleted(final ProvisionedMeshNode meshNode);

    void onNodeAdded(final ProvisionedMeshNode meshNode);

    void onNodeUpdated(final ProvisionedMeshNode meshNode);

    void onNodesUpdated();

    void onGroupAdded(final Group group);

    void onGroupUpdated(final Group group);

    void onGroupDeleted(final Group group);

    void onSceneAdded(final Scene scene);

    void onSceneUpdated(final Scene scene);

    void onSceneDeleted(final Scene scene);
}
