package org.openremote.agent.protocol.bluetooth.mesh.models;

public class GenericOnOffClientModel extends SigModel {

    public GenericOnOffClientModel(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Generic On Off Client";
    }
}