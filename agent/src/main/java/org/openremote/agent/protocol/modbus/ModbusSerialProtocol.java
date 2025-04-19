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

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;

public class ModbusSerialProtocol extends AbstractModbusProtocol<ModbusSerialProtocol, ModbusSerialAgent>{
    public ModbusSerialProtocol(ModbusSerialAgent agent) {
        super(agent);
    }

    private String connectionString;

    @Override
    protected PlcConnection createIoClient(ModbusSerialAgent agent) throws RuntimeException {

        connectionString = "modbus-rtu://"+agent.getSerialPort() +
                "?serial.baud-rate=" + agent.getBaudRate() +
                "&unit-identifier=" + agent.getUnitId() +
                "&serial.num-data-bits=" + agent.getDataBits() +
                "&serial.num-stop-bits=" + agent.getStopBits();
        PlcConnection plcConnection;
        try {
            plcConnection = PlcDriverManager.getDefault().getConnectionManager().getConnection(connectionString);
        } catch (PlcConnectionException e) {
            throw new RuntimeException(e);
        }
        return plcConnection;
    }

    @Override
    public String getProtocolName() {
        return "Modbus Serial Protocol";
    }

    @Override
    public String getProtocolInstanceUri() {
        return connectionString;
    }
}
