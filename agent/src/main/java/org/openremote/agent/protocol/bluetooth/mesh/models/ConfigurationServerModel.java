/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
