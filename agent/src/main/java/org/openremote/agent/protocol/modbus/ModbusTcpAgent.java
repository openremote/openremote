package org.openremote.agent.protocol.modbus;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.AgentDescriptor;

@Entity
public class ModbusTcpAgent extends ModbusAgent<ModbusTcpAgent, ModbusTcpProtocol>{

    public static final AgentDescriptor<ModbusTcpAgent, ModbusTcpProtocol, ModbusAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            ModbusTcpAgent.class, ModbusTcpProtocol.class, ModbusAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ModbusTcpAgent() {
    }

    public ModbusTcpAgent(String name) {
        super(name);
    }

    @Override
    public ModbusTcpProtocol getProtocolInstance() {
        return new ModbusTcpProtocol(this);
    }
}
