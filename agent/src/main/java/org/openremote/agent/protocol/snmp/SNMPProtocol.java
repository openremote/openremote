package org.openremote.agent.protocol.snmp;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snmp.SnmpMessage;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.syslog.SyslogCategory;
import org.snmp4j.PDU;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a SNMP client protocol for receiving SNMP traps.
 * <p>
 * To use this protocol create a {@link SNMPAgent}.
 */
public class SNMPProtocol extends AbstractProtocol<SNMPAgent, SNMPAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "SNMP Client";
    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SNMPProtocol.class);
    protected final Map<String, AttributeRef> oidMap = new HashMap<>();

    public SNMPProtocol(SNMPAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return String.format("snmp:%s:%d?protocol=udp&type=TRAP&snmpVersion=%s",
                agent.getBindHost().orElse(""),
                agent.getBindPort().orElse(162),
                agent.getSNMPVersion().orElse(SNMPAgent.SNMPVersion.V2c).getValue());
    }

    @Override
    protected void doStart(Container container) throws Exception {

        String snmpBindHost = agent.getBindHost().orElseThrow(() -> {
            String msg = "No SNMP bind host provided for protocol: " + this;
            LOG.info(msg);
            return new IllegalArgumentException(msg);
        });

        Integer snmpBindPort = agent.getBindPort().orElse(162);
        SNMPAgent.SNMPVersion snmpVersion = agent.getSNMPVersion().orElse(SNMPAgent.SNMPVersion.V2c);
        String snmpUri = String.format("snmp:%s:%d?protocol=udp&type=TRAP&snmpVersion=%d", snmpBindHost, snmpBindPort, snmpVersion.getVersion());

        messageBrokerContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(snmpUri)
                        .routeId(getProtocolName() + getAgent().getId())
                        .process(exchange -> {
                            SnmpMessage msg = exchange.getIn(SnmpMessage.class);
                            LOG.fine(String.format("Message received: %s", msg));

                            PDU pdu = msg.getSnmpMessage();

                            AttributeRef wildCardAttributeRef;
                            if ((wildCardAttributeRef = oidMap.get("*")) != null) {
                                Map<String, Object> wildCardValue = new HashMap<>();
                                pdu.getVariableBindings().forEach(variableBinding -> {
                                    wildCardValue.put(variableBinding.getOid().format(), variableBinding.toValueString());
                                });
                                updateLinkedAttribute(new AttributeState(wildCardAttributeRef, wildCardValue));
                            }

                            pdu.getVariableBindings().forEach(variableBinding -> {
                                AttributeRef attributeRef = oidMap.get(variableBinding.getOid().format());
                                if (attributeRef != null) {
                                    updateLinkedAttribute(new AttributeState(attributeRef, variableBinding.toValueString()));
                                }
                            });
                        });
            }
        });

        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.STOPPED);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, SNMPAgentLink agentLink) throws RuntimeException {
        String oid = agentLink.getOID().orElseThrow(() -> {
            String msg = "No OID provided for protocol: " + this;
            LOG.info(msg);
            return new IllegalArgumentException(msg);
        });

        if (oid.equals("*") && oidMap.get("*") != null) {
            String msg = "Attribute with wildcard OID already provided for protocol: " + this;
            LOG.info(msg);
            throw new IllegalArgumentException(msg);
        }

        oidMap.put(oid, new AttributeRef(assetId, attribute.getName()));
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, SNMPAgentLink agentLink) {
        agentLink.getOID().ifPresent(oidMap::remove);
    }

    @Override
    protected void doLinkedAttributeWrite(SNMPAgentLink agentLink, AttributeEvent event, Object processedValue) {
        // Nothing to do here
    }
}
