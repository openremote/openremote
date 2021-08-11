package org.openremote.agent.protocol.snmp;

import com.fasterxml.jackson.annotation.JsonValue;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class SNMPAgent extends Agent<SNMPAgent, SNMPProtocol, SNMPAgentLink> {

    public enum SNMPVersion
    {
        V1(0),
        V2c(1),
        V3(3);

        private final int version;

        SNMPVersion(int version) {
            this.version = version;
        }

        public int getVersion() {
            return version;
        }

        @JsonValue
        public String getValue() {
            return toString();
        }
    }

    public static final ValueDescriptor<SNMPVersion> VALUE_SNMP_VERSION = new ValueDescriptor<>("SNMPVersion", SNMPVersion.class);

    public static final AttributeDescriptor<SNMPVersion> SNMP_VERSION = new AttributeDescriptor<>("SNMPVersionValue", VALUE_SNMP_VERSION);
    public static final AttributeDescriptor<String> SNMP_BIND_HOST = BIND_HOST.withOptional(false);
    public static final AttributeDescriptor<Integer> SNMP_BIND_PORT = BIND_PORT.withOptional(false);


    public static final AgentDescriptor<SNMPAgent, SNMPProtocol, SNMPAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            SNMPAgent.class, SNMPProtocol.class, SNMPAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected SNMPAgent() {
    }

    public SNMPAgent(String name) {
        super(name);
    }

    @Override
    public SNMPProtocol getProtocolInstance() {
        return new SNMPProtocol(this);
    }

    public Optional<SNMPVersion> getSNMPVersion() {
        return getAttributes().getValue(SNMP_VERSION);
    }

    public SNMPAgent setSNMPVersion(SNMPVersion version) {
        getAttributes().getOrCreate(SNMP_VERSION).setValue(version);
        return this;
    }
}
