package org.openremote.agent.protocol.bluetooth.mesh.models;

import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshModel;

public abstract class SigModel extends MeshModel {

    SigModel(final int sigModelId) {
        super(sigModelId);
    }

    @Override
    public int getModelId() {
        return mModelId;
    }

}
