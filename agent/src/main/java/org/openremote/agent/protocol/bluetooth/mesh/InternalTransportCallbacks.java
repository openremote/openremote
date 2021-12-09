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

import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.UnprovisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshMessage;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ProxyFilter;

import java.util.List;

public interface InternalTransportCallbacks {


    /**
     * Returns an application key with a given key index
     *
     * @param boundNetKeyIndex NetKey index
     */
    List<ApplicationKey> getApplicationKeys(final int boundNetKeyIndex);

    /**
     * Returns the node with the corresponding unicast address
     *
     * @param unicast unicast address
     */
    ProvisionedMeshNode getNode(final int unicast);

    /**
     * Returns the Provisioner with the corresponding unicast address
     *
     * @param unicast unicast address
     */
    Provisioner getProvisioner(final int unicast);

    /**
     * Send mesh pdu
     *
     * @param meshNode mesh node to send to
     * @param pdu      mesh pdu to be sent
     */
    void sendProvisioningPdu(final UnprovisionedMeshNode meshNode, final byte[] pdu);

    /**
     * Callback that is invoked when a mesh pdu is created
     *
     * @param dst Destination address to be sent
     * @param pdu mesh pdu to be sent
     */
    void onMeshPduCreated(final int dst, final byte[] pdu);


    ProxyFilter getProxyFilter();

    void setProxyFilter(final ProxyFilter filter);

    /**
     * Update mesh network
     *
     * @param message mesh message
     */
    void updateMeshNetwork(final MeshMessage message);

    /**
     * This callback is invoked when the mesh node is successfully reset
     *
     * @param meshNode mesh to be updated
     */
    void onMeshNodeReset(final ProvisionedMeshNode meshNode);


    /**
     * Returns the mesh network
     */
    MeshNetwork getMeshNetwork();

    void storeScene(final int address, final int currentScene, final List<Integer> scenes);

    void deleteScene(final int address, final int currentScene, final List<Integer> scenes);
}

