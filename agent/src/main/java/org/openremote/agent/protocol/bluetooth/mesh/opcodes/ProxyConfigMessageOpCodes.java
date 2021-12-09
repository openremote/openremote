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
package org.openremote.agent.protocol.bluetooth.mesh.opcodes;

public class ProxyConfigMessageOpCodes {

    /**
     * Opcode sent by a Proxy Client to set the proxy filter type.
     */
    public static final int SET_FILTER_TYPE = 0x00;

    /**
     * Opcode sent by a Proxy Client to add addresses to the proxy filter list.
     */
    public static final int ADD_ADDRESS = 0x01;

    /**
     * Opcode sent by a Proxy Client to remove addresses from the proxy filter list.
     */
    public static final int REMOVE_ADDRESS = 0x02;

    /**
     * Acknowledgment opcode sent by a Proxy Server to a Proxy Client to report the status of the proxy filter list.
     */
    public static final int FILTER_STATUS = 0x03;

}
