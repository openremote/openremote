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
 * Write a device resource value, this is a fire-and-forget operation.
 */
public class DeviceResourceWrite extends Event {

    protected String agentId;
    protected String deviceKey;
    protected String deviceResourceKey;
    protected String value;

    public DeviceResourceWrite() {
    }

    public DeviceResourceWrite(String agentId, String deviceKey, String deviceResourceKey, String value) {
        this.agentId = agentId;
        this.deviceKey = deviceKey;
        this.deviceResourceKey = deviceResourceKey;
        this.value = value;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public String getDeviceResourceKey() {
        return deviceResourceKey;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "agentId='" + agentId + '\'' +
            ", deviceKey='" + deviceKey + '\'' +
            ", deviceResourceKey='" + deviceResourceKey + '\'' +
            ", value='" + value + '\'' +
            "}";
    }
}
