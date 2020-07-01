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
import org.openremote.container.Container;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.security.ClientRole;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.logging.Logger;

import static org.apache.camel.builder.Builder.body;
import static org.apache.camel.builder.Builder.header;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.agent.protocol.ProtocolClientEventService.*;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemArray;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemString;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY;

/**
 * This protocol is for pub-sub devices to connect to the manager client event broker (MQTT or websocket);
 * attributes then linked to this protocol can be written to/read by the devices.
 */
public class ClientEventProtocol extends AbstractProtocol {

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":clientEvent";
    public static final String PROTOCOL_DISPLAY_NAME = "Client Event";
    public static final String PROTOCOL_VERSION = "1.0";
    public static final String CLIENT_ID_PREFIX = "ClientEvent-";
    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ClientEventProtocol.class);

    public static final MetaItemDescriptor META_PROTOCOL_CLIENT_SECRET = metaItemString(
        PROTOCOL_NAME + ":clientSecret",
        ACCESS_PRIVATE,
        true,
        REGEXP_PATTERN_STRING_NON_EMPTY,
        MetaItemDescriptor.PatternFailure.STRING_EMPTY);

    public static final MetaItemDescriptor META_PROTOCOL_CLIENT_ROLES = metaItemArray(
            PROTOCOL_NAME + ":clientRoles",
            ACCESS_PRIVATE,
            false,
            null);

    protected Map<String, AssetAttribute> clientIdProtocolConfigMap = new HashMap<>();

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        protocolClientEventService.addExchangeInterceptor(this::onMessageIntercept);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        super.doStop(container);
        protocolClientEventService.removeExchangeInterceptor(this::onMessageIntercept);
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return Collections.singletonList(
            META_PROTOCOL_CLIENT_SECRET
        );
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return Collections.emptyList();
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {

        LOG.info("Creating client credentials for: " + protocolConfiguration);
        AttributeRef attributeRef = protocolConfiguration.getReferenceOrThrow();
        String clientId = CLIENT_ID_PREFIX + attributeRef.getEntityId();

        String clientSecret = protocolConfiguration.getMetaItem(META_PROTOCOL_CLIENT_SECRET.getUrn())
            .flatMap(AbstractValueHolder::getValueAsString)
            .orElse(UUID.randomUUID().toString());

        ClientRole[] roles = protocolConfiguration.getMetaItem(META_PROTOCOL_CLIENT_ROLES.getUrn())
            .flatMap(AbstractValueHolder::getValueAsArray)
            .flatMap(arrayValue ->
                Values.getArrayElements(
                        arrayValue,
                        StringValue.class,
                        true,
                        false,
                        StringValue::getString))
            .map(list -> list.stream().map(ClientRole::valueOf).toArray(ClientRole[]::new))
            .orElse(null);

        ProtocolClientEventService.ClientCredentials clientCredentials =
            new ProtocolClientEventService.ClientCredentials(
                agent.getRealm(),
                roles,
                clientId,
                clientSecret
            );

        protocolClientEventService.addClientCredentials(clientCredentials);
        clientIdProtocolConfigMap.put(clientId, protocolConfiguration);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        LOG.info("Removing client credentials for: " + protocolConfiguration);
        clientIdProtocolConfigMap.values().remove(protocolConfiguration);
        AttributeRef attributeRef = protocolConfiguration.getReferenceOrThrow();
        protocolClientEventService.removeClientCredentials(agent.getRealm(), CLIENT_ID_PREFIX + attributeRef.getEntityId() + ":" + attributeRef.getAttributeName());
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) throws Exception {
        // Nothing to do here
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        // Nothing to do here
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        // We'll get here when an attribute event is pushed through the processing chain as with all protocols it won't
        // make it to the end of the processing chain, and won't reach the client so we write it through immediately
        // so it will be sent to the client
        updateLinkedAttribute(new AttributeState(event.getAttributeRef(), processedValue));
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return PROTOCOL_VERSION;
    }

    protected void onMessageIntercept(Exchange exchange) {
        String clientId = getClientId(exchange);

        if (!isThisClient(clientId)) {
            return;
        }

        AssetAttribute protocolConfiguration = clientIdProtocolConfigMap.get(clientId);

        if (protocolConfiguration == null) {
            LOG.info("Message received from a client for an unlinked protocol configuration, requesting disconnect");
            protocolClientEventService.closeSession(getSessionKey(exchange));
            stopMessage(exchange);
            return;
        }

        if (header(ConnectionConstants.SESSION_OPEN).matches(exchange)) {
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.CONNECTED);
            return;
        }

        if (or(header(ConnectionConstants.SESSION_CLOSE), header(ConnectionConstants.SESSION_CLOSE_ERROR)).matches(exchange)) {
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.DISCONNECTED);
            return;
        }

        if (isInbound(exchange)) {
            if (body().isInstanceOf(AttributeEvent.class).matches(exchange)) {
                // Inbound attribute event is essentially a protocol sensor update

                AttributeEvent attributeEvent = exchange.getIn().getBody(AttributeEvent.class);
                AssetAttribute linkedAttribute = getLinkedAttribute(attributeEvent.getAttributeRef());

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
        return clientId != null && clientId.startsWith(CLIENT_ID_PREFIX);
    }
}
