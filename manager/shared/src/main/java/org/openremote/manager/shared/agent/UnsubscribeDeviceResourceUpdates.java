/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.agent;

import org.openremote.manager.shared.event.Event;

/**
 * Tell an agent that you are no longer interested in receiving
 * {@link DeviceResourceValueEvent}s for a given device.
 */
public class UnsubscribeDeviceResourceUpdates extends Event {

    protected String agentId;
    protected String deviceKey;

    public UnsubscribeDeviceResourceUpdates() {
    }

    public UnsubscribeDeviceResourceUpdates(String agentId, String deviceKey) {
        this.agentId = agentId;
        this.deviceKey = deviceKey;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"{" +
            "assetId='" + agentId + '\'' +
            ", deviceKey='" + deviceKey + '\'' +
            "}";
    }
}
