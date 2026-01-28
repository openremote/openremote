/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.modbus.util;

/**
 * Interface representing a Modbus frame (response).
 * Implemented by both ModbusTcpFrame and ModbusSerialFrame.
 */
public interface ModbusFrame {

    /**
     * Check if this frame represents a Modbus exception response.
     * @return true if the function code has the exception bit set (0x80)
     */
    boolean isException();

    /**
     * Get the Protocol Data Unit (PDU) of the frame.
     * @return the PDU bytes (function code + data)
     */
    byte[] getPdu();

    /**
     * Get the unit/slave ID this frame is addressed to.
     * @return the unit ID
     */
    int getUnitId();

    /**
     * Get the Modbus function code from the PDU.
     * @return the function code byte
     */
    byte getFunctionCode();
}
