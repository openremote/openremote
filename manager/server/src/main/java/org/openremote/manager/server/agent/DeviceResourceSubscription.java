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
package org.openremote.manager.server.agent;

import org.openremote.manager.shared.agent.DeviceResourceValueEvent;
import org.openremote.manager.shared.agent.UnsubscribeDeviceResourceUpdates;

import java.util.Objects;

public class DeviceResourceSubscription {

    final protected String agentId;
    final protected String deviceKey;
    final protected long timestamp;

    public DeviceResourceSubscription(String agentId, String deviceKey, long timestamp) {
        Objects.requireNonNull(agentId, "Agent asset identifier must be provided when creating a subscription");
        Objects.requireNonNull(deviceKey, "Device key must be provided when creating a subscription");
        this.agentId = agentId;
        this.deviceKey = deviceKey;
        this.timestamp = timestamp;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean matches(DeviceResourceValueEvent event) {
        return event.getAgentId().equals(agentId) && event.getDeviceKey().equals(deviceKey);
    }

    public boolean matches(UnsubscribeDeviceResourceUpdates event) {
        return event.getAgentId().equals(agentId) && event.getDeviceKey().equals(deviceKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceResourceSubscription that = (DeviceResourceSubscription) o;

        if (!agentId.equals(that.agentId)) return false;
        return deviceKey.equals(that.deviceKey);

    }

    @Override
    public int hashCode() {
        int result = agentId.hashCode();
        result = 31 * result + deviceKey.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "agentId='" + agentId + '\'' +
            ", deviceKey='" + deviceKey + '\'' +
            ", timestamp=" + timestamp +
            '}';
    }
}
