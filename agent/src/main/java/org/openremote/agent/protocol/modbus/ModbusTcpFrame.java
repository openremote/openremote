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
package org.openremote.agent.protocol.modbus;

/**
 * Represents a Modbus TCP frame (MBAP header + PDU)
 */
public class ModbusTcpFrame implements AbstractModbusProtocol.ModbusResponse {
    private final int transactionId;
    private final int protocolId;
    private final int length;
    private final int unitId;
    private final byte[] pdu;

    public ModbusTcpFrame(int transactionId, int unitId, byte[] pdu) {
        this.transactionId = transactionId;
        this.protocolId = 0; // Always 0 for Modbus TCP
        this.length = 1 + pdu.length; // Unit ID + PDU
        this.unitId = unitId;
        this.pdu = pdu;
    }

    public ModbusTcpFrame(int transactionId, int protocolId, int length, int unitId, byte[] pdu) {
        this.transactionId = transactionId;
        this.protocolId = protocolId;
        this.length = length;
        this.unitId = unitId;
        this.pdu = pdu;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public int getProtocolId() {
        return protocolId;
    }

    public int getLength() {
        return length;
    }

    public int getUnitId() {
        return unitId;
    }

    public byte[] getPdu() {
        return pdu;
    }

    public byte getFunctionCode() {
        return pdu != null && pdu.length > 0 ? pdu[0] : 0;
    }

    public boolean isException() {
        return pdu != null && pdu.length > 0 && (pdu[0] & 0x80) != 0;
    }
}
