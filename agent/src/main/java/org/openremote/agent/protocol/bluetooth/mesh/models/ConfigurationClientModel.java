package org.openremote.agent.protocol.bluetooth.mesh.models;

public class ConfigurationClientModel extends SigModel {

    public ConfigurationClientModel(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Configuration Client";
    }
}
