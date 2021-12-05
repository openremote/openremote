package org.openremote.agent.protocol.bluetooth.mesh.provisionerstates;

public class ProvisioningCompleteState extends ProvisioningState {

    private final UnprovisionedMeshNode unprovisionedMeshNode;

    public ProvisioningCompleteState(final UnprovisionedMeshNode unprovisionedMeshNode) {
        super();
        this.unprovisionedMeshNode = unprovisionedMeshNode;
        unprovisionedMeshNode.setIsProvisioned(true);
        unprovisionedMeshNode.setProvisionedTime(System.currentTimeMillis());
    }

    @Override
    public State getState() {
        return State.PROVISIONING_COMPLETE;
    }

    @Override
    public void executeSend() {

    }

    @Override
    public boolean parseData(final byte[] data) {
        return true;
    }

}

