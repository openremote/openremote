package org.openremote.agent.protocol.teltonika;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class TeltonikaAgent extends Agent<TeltonikaAgent, TeltonikaProtocol, DefaultAgentLink> {

    public static final AgentDescriptor<TeltonikaAgent, TeltonikaProtocol, DefaultAgentLink> DESCRIPTOR =
            new AgentDescriptor<>(TeltonikaAgent.class, TeltonikaProtocol.class, DefaultAgentLink.class);

    public static final AttributeDescriptor<String> TRANSPORT =
            new AttributeDescriptor<>("transport", ValueType.TEXT)
                    .withConstraints(new ValueConstraint.AllowedValues("TCP", "UDP", "BOTH"))
                    .withOptional(true);

    public static final AttributeDescriptor<Integer> BIND_PORT = Agent.BIND_PORT.withOptional(false);

    protected TeltonikaAgent() {
    }

    public TeltonikaAgent(String name) {
        super(name);
    }

    @Override
    public TeltonikaProtocol getProtocolInstance() {
        return new TeltonikaProtocol(this);
    }

    public Optional<String> getTransport() {
        return getAttributes().getValue(TRANSPORT);
    }

    public TeltonikaAgent setTransport(String transport) {
        getAttributes().getOrCreate(TRANSPORT).setValue(transport);
        return this;
    }
}
