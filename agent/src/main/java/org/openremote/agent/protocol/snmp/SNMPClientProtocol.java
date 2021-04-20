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
 * To use this protocol create a {@link SNMPClientAgent}.
 */
public class SNMPClientProtocol extends AbstractProtocol<SNMPClientAgent, SNMPClientAgent.SNMPAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "SNMP Client";
    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SNMPClientProtocol.class);
    protected final Map<String, AttributeRef> oidMap = new HashMap<>();

    public SNMPClientProtocol(SNMPClientAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return String.format("snmp:%s:%d?protocol=udp&type=TRAP&snmpVersion=%d",
                agent.getHost().orElse(""),
                agent.getPort().orElse(162),
                agent.getSNMPVersion().orElse(SNMPClientAgent.SNMPVersion.V2c).getVersion());
    }

    @Override
    protected void doStart(Container container) throws Exception {

        String snmpHost = agent.getHost().orElseThrow(() -> {
            String msg = "No SNMP host provided for protocol: " + this;
            LOG.info(msg);
            return new IllegalArgumentException(msg);
        });

        Integer snmpPort = agent.getPort().orElse(162);
        SNMPClientAgent.SNMPVersion snmpVersion = agent.getSNMPVersion().orElse(SNMPClientAgent.SNMPVersion.V2c);
        String snmpUri = String.format("snmp:%s:%d?protocol=udp&type=TRAP&snmpVersion=%d", snmpHost, snmpPort, snmpVersion.getVersion());

        messageBrokerContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(snmpUri)
                        .routeId(getProtocolName() + getAgent().getId())
                        .process(exchange -> {
                            // Since we are using Snmp, we get SnmpMessage object, we need to typecast from Message
                            SnmpMessage msg = (SnmpMessage) exchange.getIn();
                            PDU pdu = msg.getSnmpMessage();
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
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, SNMPClientAgent.SNMPAgentLink agentLink) throws RuntimeException {
        String oid = agentLink.getOID().orElseThrow(() -> {
            String msg = "No OID provided for protocol: " + this;
            LOG.info(msg);
            return new IllegalArgumentException(msg);
        });

        oidMap.put(oid, new AttributeRef(assetId, attribute.getName()));
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, SNMPClientAgent.SNMPAgentLink agentLink) {
        agentLink.getOID().ifPresent(oidMap::remove);
    }

    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, SNMPClientAgent.SNMPAgentLink agentLink, AttributeEvent event, Object processedValue) {
        // Nothing to do here
    }
}
