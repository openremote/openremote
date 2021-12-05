package org.openremote.agent.protocol.bluetooth.mesh.models;

public class UnknownModel extends SigModel {

    public UnknownModel(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Unknown";
    }
}
