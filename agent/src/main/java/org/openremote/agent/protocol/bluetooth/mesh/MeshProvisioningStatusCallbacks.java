package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.ProvisioningState;
import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.UnprovisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;

/**
 * Callbacks to notify status during the provisioning process
 */
public interface MeshProvisioningStatusCallbacks {

    /**
     * Invoked when the provisioning state changed.
     *
     * @param meshNode {@link UnprovisionedMeshNode} unprovisioned node.
     * @param state    {@link ProvisioningState.State} each provisioning state.
     * @param data     data that was sent or received during each provisioning state.
     */
    void onProvisioningStateChanged(final UnprovisionedMeshNode meshNode, final ProvisioningState.States state, final byte[] data);

    /**
     * Invoked when the provisioning state changed.
     *
     * @param meshNode {@link UnprovisionedMeshNode} unprovisioned node.
     * @param state    {@link ProvisioningState.State} each provisioning state.
     * @param data     data that was sent or received during each provisioning state.
     */
    void onProvisioningFailed(final UnprovisionedMeshNode meshNode, final ProvisioningState.States state, final byte[] data);

    /**
     * Invoked when the provisioning state changed.
     *
     * @param meshNode {@link ProvisionedMeshNode} provisioned mesh node.
     * @param state    {@link ProvisioningState.State} each provisioning state.
     * @param data     data that was sent or received during each provisioning state.
     */
    void onProvisioningCompleted(final ProvisionedMeshNode meshNode, final ProvisioningState.States state, final byte[] data);

}

