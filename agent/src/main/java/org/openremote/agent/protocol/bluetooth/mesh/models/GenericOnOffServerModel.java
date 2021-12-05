package org.openremote.agent.protocol.bluetooth.mesh.models;

public class GenericOnOffServerModel extends SigModel {

    public GenericOnOffServerModel(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Generic On Off Server";
    }
}
