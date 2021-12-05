package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;

interface InternalMeshManagerCallbacks {

    /**
     * Internal callback to notify the {@link MeshManagerApi} of provisioned nodes
     *
     * @param meshNode node that was provisioned
     */
    void onNodeProvisioned(final ProvisionedMeshNode meshNode);
}

