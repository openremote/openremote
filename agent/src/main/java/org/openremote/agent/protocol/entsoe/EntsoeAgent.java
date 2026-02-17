package org.openremote.agent.protocol.entsoe;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class EntsoeAgent extends Agent<EntsoeAgent, EntsoeProtocol, EntsoeAgentLink> {

    public static final AgentDescriptor<EntsoeAgent, EntsoeProtocol, EntsoeAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            EntsoeAgent.class, EntsoeProtocol.class, EntsoeAgentLink.class);

    public static final AttributeDescriptor<String> SECURITY_TOKEN = new AttributeDescriptor<>("securityToken", ValueType.TEXT);

    public static final AttributeDescriptor<String> BASE_URL = new AttributeDescriptor<>("baseURL", ValueType.TEXT).withOptional(true);

    public EntsoeAgent() {
    }

    public EntsoeAgent(String name) {
        super(name);
    }

    @Override
    public EntsoeProtocol getProtocolInstance() {
        return new EntsoeProtocol(this);
    }

    public Optional<String> getSecurityToken() {
        return getAttributes().getValue(SECURITY_TOKEN);
    }

    public EntsoeAgent setSecurityToken(String value) {
        getAttributes().getOrCreate(SECURITY_TOKEN).setValue(value);
        return this;
    }

    public Optional<String> getBaseURL() {
        return getAttributes().getValue(BASE_URL);
    }

    public EntsoeAgent setBaseURL(String value) {
        getAttributes().getOrCreate(BASE_URL).setValue(value);
        return this;
    }
}
