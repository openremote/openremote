package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.UnprovisionedMeshNode;

/**
 * Implement this class in order to get the transport callbacks from the {@link MeshManagerApi}
 */
public interface MeshManagerCallbacks {

    /**
     * Returns the network that was loaded
     *
     * @param meshNetwork{@link MeshNetwork that was loaded}
     */
    void onNetworkLoaded(final MeshNetwork meshNetwork);

    /**
     * Returns the network that was updated
     * <p>
     * This callback is invoked for every message that was sent or received as it changes the contents of the network
     * </p>
     *
     * @param meshNetwork{@link MeshNetwork that was loaded}
     */
    void onNetworkUpdated(final MeshNetwork meshNetwork);

    /**
     * Callback that notifies in case the mesh network was unable to load
     *
     * @param error error
     */
    void onNetworkLoadFailed(final String error);

    /**
     * Callbacks notifying the network was imported
     *
     * @param meshNetwork{@link MeshNetwork that was loaded}
     */
    void onNetworkImported(final MeshNetwork meshNetwork);

    /**
     * Callback that notifies in case the mesh network was unable to imported
     *
     * @param error error
     */
    void onNetworkImportFailed(final String error);

    /**
     * Send mesh pdu
     *
     * @param meshNode {@link UnprovisionedMeshNode}
     * @param pdu      mesh pdu to be sent
     */
    void sendProvisioningPdu(final UnprovisionedMeshNode meshNode, final byte[] pdu);

    /**
     * Send mesh pdu
     *
     * @param pdu mesh pdu to be sent
     */
    void onMeshPduCreated(final byte[] pdu);

    /**
     * Get mtu size supported by the peripheral node
     * <p>
     * This is used to get the supported mtu size from the ble module, so that the messages
     * that are larger than the supported mtu size could be segmented
     * </p>
     *
     * @return mtu size
     */
    int getMtu();
}

