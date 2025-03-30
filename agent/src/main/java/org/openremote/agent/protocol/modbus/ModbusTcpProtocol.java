package org.openremote.agent.protocol.modbus;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;

public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent>{
    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
    }

    @Override
    protected PlcConnection createIoClient(ModbusTcpAgent agent) throws RuntimeException {
        PlcConnection plcConnection;
        try {
            plcConnection = PlcDriverManager.getDefault().getConnectionManager()
                    .getConnection("modbus-tcp://" + agent.getHost().orElseThrow() + ":" + agent.getPort().orElseThrow()+"?unit-identifier=" + agent.getUnitId());
        } catch (PlcConnectionException e) {
            throw new RuntimeException(e);
        }
        return plcConnection;
    }
}
