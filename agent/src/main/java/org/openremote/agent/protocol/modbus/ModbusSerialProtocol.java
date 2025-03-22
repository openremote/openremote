package org.openremote.agent.protocol.modbus;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.jboss.resteasy.spi.NotImplementedYetException;

public class ModbusSerialProtocol extends AbstractModbusProtocol<ModbusSerialProtocol, ModbusSerialAgent>{
    public ModbusSerialProtocol(ModbusSerialAgent agent) {
        super(agent);
    }

    @Override
    protected PlcConnection createIoClient(ModbusSerialAgent agent) throws RuntimeException {
        PlcConnection plcConnection = null;
        try {
            plcConnection = PlcDriverManager.getDefault().getConnectionManager()
                    .getConnection("modbus-ascii://"+agent.getSerialPort() +
                            "?serial.baud-rate=" + agent.getBaudRate() +
                            "&serial.num-data-bits=" + agent.getDataBits() +
                            "&serial.num-stop-bits=" + agent.getStopBits()
//                            "&serial.parity=" + agent.getParity()
                    );
        } catch (PlcConnectionException e) {
            throw new RuntimeException(e);
        }
        return plcConnection;
    }
}
