/*
 * Copyright 2020, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.event;

import org.apache.camel.Exchange;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.ProtocolClientEventService;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.security.ClientRole;
import org.openremote.model.syslog.SyslogCategory;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import static org.apache.camel.builder.Builder.body;
import static org.apache.camel.builder.Builder.header;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.agent.protocol.ProtocolClientEventService.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This protocol is for pub-sub devices to connect to the manager client event broker (MQTT or websocket);
 * attributes then linked to this protocol can be written to/read by the devices. The protocol handles the
 * creation of a Keycloak client that allows client secret 'authentication' for machines therefore this protocol
 * requires the Keycloak identity provider.
 */
public class ClientEventProtocol extends AbstractProtocol<ClientEventAgent, AgentLink.Default> {

    public static final String PROTOCOL_DISPLAY_NAME = "Client Event";
    public static final String CLIENT_ID_PREFIX = "ClientEventProtocol-";
    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ClientEventProtocol.class);

    public ClientEventProtocol(ClientEventAgent agent) {
        super(agent);
    }

    @Override
    public void doStart(Container container) throws Exception {
        LOG.info("Creating client credentials for: " + this);

        protocolClientEventService.addExchangeInterceptor(this::onMessageIntercept);

        String clientId = CLIENT_ID_PREFIX + agent.getId().substring(12);

        String clientSecret = agent.getClientSecret().orElse(UUID.randomUUID().toString());
        ClientRole[] roles = agent.getClientRoles().orElse(null);

        ProtocolClientEventService.ClientCredentials clientCredentials =
            new ProtocolClientEventService.ClientCredentials(
                agent.getRealm(),
                roles,
                clientId,
                clientSecret
            );

        protocolClientEventService.addClientCredentials(clientCredentials);
        setConnectionStatus(ConnectionStatus.CONNECTED);
        updateAgentAttribute(new AttributeState(agent.getId(), ClientEventAgent.CLIENT_SECRET.getName(), clientSecret));
    }

    @Override
    protected void doStop(Container container) throws Exception {
        LOG.info("Removing client credentials for: " + this);
        protocolClientEventService.removeExchangeInterceptor(this::onMessageIntercept);
        protocolClientEventService.removeClientCredentials(agent.getRealm(), CLIENT_ID_PREFIX + agent.getId());
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, AgentLink.Default agentLink) throws RuntimeException {
        // Nothing to do here
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, AgentLink.Default agentLink) {
        // Nothing to do here
    }

    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, AgentLink.Default agentLink, AttributeEvent event, Object processedValue) {
        // Nothing to do here
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "clientEvent://" + agent.getId();
    }

    protected void onMessageIntercept(Exchange exchange) {
        String clientId = getClientId(exchange);

        if (!isThisClient(clientId)) {
            return;
        }

        if (header(ConnectionConstants.SESSION_OPEN).matches(exchange)) {
            LOG.info("Client connected: " + this);
            return;
        }

        if (or(header(ConnectionConstants.SESSION_CLOSE), header(ConnectionConstants.SESSION_CLOSE_ERROR)).matches(exchange)) {
            LOG.info("Client disconnected: " + this);
            return;
        }

        if (isInbound(exchange)) {
            if (body().isInstanceOf(AttributeEvent.class).matches(exchange)) {
                // Inbound attribute event is essentially a protocol sensor update

                AttributeEvent attributeEvent = exchange.getIn().getBody(AttributeEvent.class);
                Attribute<?> linkedAttribute = getLinkedAttributes().get(attributeEvent.getAttributeRef());

                if (linkedAttribute == null) {
                    LOG.info("Message received from a client for an unlinked attribute, so ignoring");
                    stopMessage(exchange);
                    return;
                }

                // TODO: Can do some processing of the incoming value here
                updateLinkedAttribute(new AttributeState(attributeEvent.getAttributeRef(), attributeEvent.getValue().orElse(null)));
                stopMessage(exchange);
            }
        }
    }

    protected boolean isThisClient(String clientId) {
        return Objects.equals(CLIENT_ID_PREFIX + agent.getId(), clientId);
    }
}
