package org.openremote.agent.protocol.bluetooth.mesh.provisionerstates;

import org.openremote.agent.protocol.bluetooth.mesh.MeshProvisioningStatusCallbacks;

import java.util.logging.Logger;

public class ProvisioningCapabilitiesState extends ProvisioningState {
    public static final Logger LOG = Logger.getLogger(ProvisioningCapabilitiesState.class.getName());

    private final UnprovisionedMeshNode mUnprovisionedMeshNode;
    private final MeshProvisioningStatusCallbacks mCallbacks;

    private ProvisioningCapabilities capabilities;

    public ProvisioningCapabilitiesState(final UnprovisionedMeshNode unprovisionedMeshNode, final MeshProvisioningStatusCallbacks callbacks) {
        super();
        this.mCallbacks = callbacks;
        this.mUnprovisionedMeshNode = unprovisionedMeshNode;
    }

    @Override
    public State getState() {
        return State.PROVISIONING_CAPABILITIES;
    }

    public ProvisioningCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public void executeSend() {

    }

    @Override
    public boolean parseData(final byte[] data) {
        final boolean flag = parseProvisioningCapabilities(data);
        //We store the provisioning capabilities pdu to be used when generating confirmation inputs
        mUnprovisionedMeshNode.setProvisioningCapabilitiesPdu(data);
        mUnprovisionedMeshNode.setProvisioningCapabilities(capabilities);
        mCallbacks.onProvisioningStateChanged(mUnprovisionedMeshNode, States.PROVISIONING_CAPABILITIES, data);
        return flag;
    }

    private boolean parseProvisioningCapabilities(final byte[] provisioningCapabilities) {
        this.capabilities = new ProvisioningCapabilities(provisioningCapabilities);
        return true;
    }
}

