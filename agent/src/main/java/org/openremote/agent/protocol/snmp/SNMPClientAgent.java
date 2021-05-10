package org.openremote.agent.protocol.snmp;

import com.fasterxml.jackson.annotation.JsonValue;
import org.openremote.agent.protocol.knx.KNXAgent;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Entity
public class SNMPClientAgent extends Agent<SNMPClientAgent, SNMPClientProtocol, SNMPClientAgent.SNMPAgentLink> {

    public static class SNMPAgentLink extends AgentLink<SNMPClientAgent.SNMPAgentLink> {

        @NotNull
        protected String oid;

        // For Hydrators
        protected SNMPAgentLink() {}

        public SNMPAgentLink(String id, String oid) {
            super(id);

            this.oid = oid;
        }

        public Optional<String> getOID() {
            return Optional.ofNullable(oid);
        }

        public SNMPClientAgent.SNMPAgentLink setOID(String oid) {
            this.oid = oid;
            return this;
        }
    }

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

    public static final ValueDescriptor<SNMPVersion> VALUE_SNMP_VERSION = new ValueDescriptor<>("sNMPVersion", SNMPVersion.class);

    public static final AttributeDescriptor<SNMPVersion> SNMP_VERSION = new AttributeDescriptor<>("sNMPVersionValue", VALUE_SNMP_VERSION);
    public static final AttributeDescriptor<String> SNMP_BIND_HOST = BIND_HOST.withOptional(false);
    public static final AttributeDescriptor<Integer> SNMP_BIND_PORT = BIND_PORT.withOptional(false);


    public static final AgentDescriptor<SNMPClientAgent, SNMPClientProtocol, SNMPAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            SNMPClientAgent.class, SNMPClientProtocol.class, SNMPAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected SNMPClientAgent() {
    }

    public SNMPClientAgent(String name) {
        super(name);
    }

    @Override
    public SNMPClientProtocol getProtocolInstance() {
        return new SNMPClientProtocol(this);
    }

    public Optional<SNMPVersion> getSNMPVersion() {
        return getAttributes().getValue(SNMP_VERSION);
    }

    public SNMPClientAgent setSNMPVersion(SNMPVersion version) {
        getAttributes().getOrCreate(SNMP_VERSION).setValue(version);
        return this;
    }
}
