package org.openremote.agent.protocol.snmp;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snmp.SnmpMessage;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.syslog.SyslogCategory;
import org.snmp4j.PDU;

import java.net.URI;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class SNMPClientProtocol extends AbstractProtocol<SNMPClientAgent, AgentLink.Default> {

    public static final String PROTOCOL_DISPLAY_NAME = "SNMP Client";
    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SNMPClientProtocol.class);

    public SNMPClientProtocol(SNMPClientAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    protected void doStart(Container container) throws Exception {

        String snmpHost = agent.getHost().orElseThrow(() -> {
            String msg = "No SNMP host provided for protocol: " + this;
            LOG.info(msg);
            return new IllegalArgumentException(msg);
        });

        Integer snmpPort = agent.getPort().orElse(162);

        String snmpUri = String.format("snmp:%s:%d?protocol=udp&type=TRAP", snmpHost, snmpPort);

        messageBrokerContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(snmpUri)
                        .routeId(getProtocolName() + getAgent().getId())
                        .process(exchange -> {

                            // Since we are using Snmp, we get SnmpMessage object, we need to typecast from Message
                            SnmpMessage msg = (SnmpMessage) exchange.getIn();
                            PDU pdu = msg.getSnmpMessage();
                            pdu.getVariableBindings().forEach(variableBinding -> LOG.info(variableBinding.toValueString()));
                        });
            }
        });
    }

    @Override
    protected void doStop(Container container) throws Exception {

    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, AgentLink.Default agentLink) throws RuntimeException {

    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, AgentLink.Default agentLink) {

    }

    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, AgentLink.Default agentLink, AttributeEvent event, Object processedValue) {

    }

    @Override
    public String getProtocolInstanceUri() {
        return null;
    }
}
