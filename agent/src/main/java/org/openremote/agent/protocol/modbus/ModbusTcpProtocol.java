package org.openremote.agent.protocol.modbus;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.agent.protocol.velbus.VelbusPacket;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;

public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent>{
    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
    }

    @Override
    protected PlcConnection createIoClient(ModbusTcpAgent agent) throws RuntimeException {
        PlcConnection plcConnection = null;
        try {
            plcConnection = PlcDriverManager.getDefault().getConnectionManager()
                    .getConnection("modbus-tcp://" + agent.getHost() + ":" + agent.getPort());
        } catch (PlcConnectionException e) {
            throw new RuntimeException(e);
        }
        return plcConnection;



    }
}
