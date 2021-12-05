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

