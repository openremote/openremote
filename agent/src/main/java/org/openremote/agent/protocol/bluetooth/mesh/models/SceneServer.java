package org.openremote.agent.protocol.bluetooth.mesh.models;

import java.util.Collections;
import java.util.List;

public class SceneServer extends SigModel {

    public SceneServer(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Scene Server";
    }

    public List<Integer> getScenesNumbers() {
        return Collections.unmodifiableList(sceneNumbers);
    }

    public int getCurrentScene() {
        return currentScene;
    }
}

