package org.openremote.agent.protocol.bluetooth.mesh.models;

import org.openremote.agent.protocol.bluetooth.mesh.utils.HeartbeatPublication;
import org.openremote.agent.protocol.bluetooth.mesh.utils.HeartbeatSubscription;

public class ConfigurationServerModel extends SigModel {

    private HeartbeatPublication heartbeatPublication = null;
    private HeartbeatSubscription heartbeatSubscription = null;

    public ConfigurationServerModel(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Configuration Server";
    }

    /**
     * Returns the Heartbeat publication.
     */
    public HeartbeatPublication getHeartbeatPublication() {
        return heartbeatPublication;
    }

    /**
     * Sets the Heartbeat publication.
     *
     * @param heartbeatPublication Heartbeat publication.
     */
    public void setHeartbeatPublication(final HeartbeatPublication heartbeatPublication) {
        this.heartbeatPublication = heartbeatPublication;
    }

    /**
     * Returns the Heartbeat subscription.
     */
    public HeartbeatSubscription getHeartbeatSubscription() {
        return heartbeatSubscription;
    }

    /**
     * Sets the Heartbeat subscription.
     *
     * @param heartbeatSubscription Heartbeat subscription.
     */
    public void setHeartbeatSubscription(final HeartbeatSubscription heartbeatSubscription) {
        this.heartbeatSubscription = heartbeatSubscription;
    }
}
