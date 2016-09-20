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
 * Tell an agent that you are interested in any state changes to readable device
 * resources of a given device. This registration must be repeated or the service
 * will expire the subscription after 60 seconds. While active, any value changes
 * detected on a device resource will be available through
 * {@link DeviceResourceValueEvent}s.
 */
public class SubscribeDeviceResourceUpdates extends Event {

    protected String agentId;
    protected String deviceKey;

    public SubscribeDeviceResourceUpdates() {
    }

    public SubscribeDeviceResourceUpdates(String agentId, String deviceKey) {
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
            "agentId='" + agentId + '\'' +
            ", deviceKey='" + deviceKey + '\'' +
            "}";
    }
}
