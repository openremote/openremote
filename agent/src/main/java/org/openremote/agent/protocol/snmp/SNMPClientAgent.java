package org.openremote.agent.protocol.snmp;

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
public class SNMPClientAgent extends Agent<SNMPClientAgent, SNMPClientProtocol, AgentLink.Default> {

    public static class SNMPAgentLink extends AgentLink<SNMPClientAgent.SNMPAgentLink> {

        @NotNull
        protected String community;
        @NotNull
        protected String oid;

        // For Hydrators
        protected SNMPAgentLink() {}

        public SNMPAgentLink(String id, String community, String oid) {
            super(id);
            this.community = community;
            this.oid = oid;
        }

        public Optional<String> getCommunity() {
            return Optional.ofNullable(community);
        }

        public SNMPClientAgent.SNMPAgentLink setCommunity(String community) {
            this.community = community;
            return this;
        }

        public Optional<String> getOID() {
            return Optional.ofNullable(oid);
        }

        public SNMPClientAgent.SNMPAgentLink setOID(String oid) {
            this.oid = oid;
            return this;
        }
    }

    public enum SNMP_VERSION
    {
        V1,
        V2c,
        V3
    }

    public static final ValueDescriptor<SNMP_VERSION> VALUE_SNMP_VERSION = new ValueDescriptor<>("sNMPVersion", SNMP_VERSION.class);

    public static final AttributeDescriptor<SNMP_VERSION> SNMP_VERSION = new AttributeDescriptor<>("sNMPVersionValue", VALUE_SNMP_VERSION);


    public static final AgentDescriptor<SNMPClientAgent, SNMPClientProtocol, AgentLink.Default> DESCRIPTOR = new AgentDescriptor<>(
            SNMPClientAgent.class, SNMPClientProtocol.class, AgentLink.Default.class
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
}
