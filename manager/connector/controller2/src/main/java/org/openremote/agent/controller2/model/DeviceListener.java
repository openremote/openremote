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
package org.openremote.agent.controller2.model;

import org.openremote.manager.shared.asset.Asset;

/**
 * Used by IOT connectors to announce changes to devices.
 * When a gateway component starts it should announce all devices
 * that it is already aware of and connected to.
 */
public interface DeviceListener {

    void onDeviceAdded(Asset device);

    // TODO Not used, implement!
    void onDeviceRemoved(Asset device);

    // TODO Not used, implement!
    void onDeviceUpdated(Asset device);
}
