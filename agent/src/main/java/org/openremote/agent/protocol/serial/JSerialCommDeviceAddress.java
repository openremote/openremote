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
package org.openremote.agent.protocol.serial;

import java.net.SocketAddress;

/**
 * A SocketAddress subclass to wrap the serial port address of a jSerialComm device (e.g. COM1, /dev/ttyUSB0).
 */
public class JSerialCommDeviceAddress extends SocketAddress {

    private static final long serialVersionUID = -2907820090993709523L;

    private final String value;

    public JSerialCommDeviceAddress(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
