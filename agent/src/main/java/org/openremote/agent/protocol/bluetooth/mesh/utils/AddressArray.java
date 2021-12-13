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
package org.openremote.agent.protocol.bluetooth.mesh.utils;

/**
 * Wrapper class for addresses
 */
public class AddressArray {

    final byte[] address = new byte[2];

    /**
     * Constructs the AddressArray
     *
     * @param b1 address byte1
     * @param b2 address byte2
     */
    public AddressArray(final byte b1, final byte b2) {
        address[0] = b1;
        address[1] = b2;
    }

    /**
     * Returns address used in the filter message
     */
    public byte[] getAddress() {
        return address;
    }
}
