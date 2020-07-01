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
package org.openremote.manager.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.ProtocolClientEventService;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.gateway.GatewayDisconnectEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Values;

import javax.persistence.EntityManager;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
import static org.openremote.manager.event.ClientEventService.*;
import static org.openremote.model.asset.AssetAttribute.attributeFromJson;
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

public class GatewayService extends RouteBuilder implements ContainerService, AssetUpdateProcessor {

    public static final int PRIORITY = HIGH_PRIORITY + 100;
    public static final String GATEWAY_CLIENT_ID_PREFIX = "gateway-";
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayService.class.getName());
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected ManagerIdentityService identityService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected ManagerExecutorService executorService;
    protected final Map<String, GatewayConnector> gatewayConnectorMap = new HashMap<>();
    protected final Map<String, String> assetIdGatewayIdMap = new HashMap<>();
    protected boolean active;

    protected static boolean isGatewayClientId(String clientId) {
        return clientId != null && clientId.startsWith(GATEWAY_CLIENT_ID_PREFIX);
    }

    public static String getGatewayIdFromClientId(String clientId) {
        return clientId.substring(GATEWAY_CLIENT_ID_PREFIX.length());
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        identityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);
        executorService = container.getService(ManagerExecutorService.class);

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("Incoming edge gateway connections disabled: Not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
            container.getService(MessageBrokerService.class).getContext().addRoutes(this);
            clientEventService.addExchangeInterceptor(this::onMessageIntercept);
        }
    }

    @Override
    public void start(Container container) throws Exception {

        if (!active) {
            return;
        }

        List<Asset> gateways = assetStorageService.findAll(new AssetQuery().types(AssetType.GATEWAY));
        List<String> gatewayIds = gateways.stream().map(Asset::getId).collect(Collectors.toList());
        gateways = gateways.stream()
            .filter(gateway ->
                Arrays.stream(gateway.getPath()).noneMatch(p -> !p.equals(gateway.getId()) && gatewayIds.contains(p)))
            .collect(Collectors.toList());

        if (!gateways.isEmpty()) {
            LOG.info("Directly registered gateways found = " + gateways.size());

            gateways.forEach(gateway -> {

                // Check if client has been created
                boolean hasClientId = gateway.getAttribute("clientId").map(a -> a.getValueAsString().isPresent()).orElse(false);
                boolean hasClientSecret = gateway.getAttribute("clientSecret").map(a -> a.getValueAsString().isPresent()).orElse(false);

                if (!hasClientId || !hasClientSecret) {
                    createGatewayClient(gateway);
                }

                // Create connector
                GatewayConnector connector = new GatewayConnector(assetStorageService, assetProcessingService, executorService, gateway);
                gatewayConnectorMap.put(gateway.getId(), connector);

                // Get IDs of all assets under this gateway
                List<Asset> gatewayAssets = assetStorageService
                    .findAll(
                        new AssetQuery()
                            .parents(gateway.getId())
                            .select(AssetQuery.Select.selectExcludeAll())
                            .recursive(true));

                gatewayAssets.forEach(asset -> assetIdGatewayIdMap.put(asset.getId(), gateway.getId()));
            });
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        // TODO: Stop all connectors
        gatewayConnectorMap.values().forEach(GatewayConnector::disconnect);
        gatewayConnectorMap.clear();
        assetIdGatewayIdMap.clear();
    }

    @Override
    public void configure() throws Exception {

        if (active) {
            from(PERSISTENCE_TOPIC)
                .routeId("GatewayServiceAssetChanges")
                .filter(isPersistenceEventForEntityType(Asset.class))
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    PersistenceEvent<Asset> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                    Asset eventAsset = persistenceEvent.getEntity();

                    if (persistenceEvent.getCause() != PersistenceEvent.Cause.DELETE) {
                        eventAsset = assetStorageService.find(eventAsset.getId(), true);
                    }

                    // Only gateways locally registered to this manager are of interest or gateway descendant assets
                    if (eventAsset.getWellKnownType() == AssetType.GATEWAY
                        && (isLocallyRegisteredGateway(eventAsset.getId()) || getLocallyRegisteredGatewayId(eventAsset.getId(), eventAsset.getParentId()) == null)) {
                        processGatewayChange(eventAsset, persistenceEvent);
                    } else {

                        String gatewayId = getLocallyRegisteredGatewayId(eventAsset.getId(), eventAsset.getParentId());

                        if (gatewayId != null) {
                            processGatewayChildAssetChange(gatewayId, eventAsset, persistenceEvent);
                        }
                    }
                });
        }
    }

    protected void onMessageIntercept(Exchange exchange) {
        String clientId = ProtocolClientEventService.getClientId(exchange);

        if (!isGatewayClientId(clientId)) {
            return;
        }

        if (header(ConnectionConstants.SESSION_OPEN).matches(exchange)) {
            Session session = exchange.getIn().getHeader(ConnectionConstants.SESSION, Session.class);
            String sessionKey = ProtocolClientEventService.getSessionKey(exchange);
            processGatewayConnected(clientId, sessionKey, session);
            return;
        }

        if (or(header(ConnectionConstants.SESSION_CLOSE), header(ConnectionConstants.SESSION_CLOSE_ERROR)).matches(exchange)) {
            processGatewayDisconnected(clientId);
            return;
        }

        // Inbound shared events
        if (and(ProtocolClientEventService::isInbound, body().isInstanceOf(SharedEvent.class)).matches(exchange)) {
            ProtocolClientEventService.stopMessage(exchange);
            String gatewayId = getGatewayIdFromClientId(clientId);
            onGatewayClientEventReceived(gatewayId, exchange.getIn().getHeader(HEADER_REQUEST_RESPONSE_MESSAGE_ID, String.class), exchange.getIn().getBody(SharedEvent.class));
        }
    }

    @Override
    public boolean processAssetUpdate(EntityManager em, Asset asset, AssetAttribute attribute, AttributeEvent.Source source) throws AssetProcessingException {

        // If the update was generated by a gateway then we don't want to process it here
        if (source == AttributeEvent.Source.GATEWAY) {
            return false;
        }

        GatewayConnector connector = gatewayConnectorMap.get(asset.getId());

        if (connector != null) {
            LOG.fine("Attribute event for a locally registered gateway asset (Asset ID=" + asset.getId() + "): " + attribute);

            // This is a change to a locally registered gateway
            if (attribute.getNameOrThrow().equals("disabled")) {
                boolean disabled = attribute.getValueAsBoolean().orElse(false);
                boolean isAlreadyDisabled = asset.getAttribute("disabled").flatMap(AssetAttribute::getValueAsBoolean).orElse(false);
                if (disabled != isAlreadyDisabled) {
                    if (disabled) {
                        connector.sendMessageToGateway(new GatewayDisconnectEvent(GatewayDisconnectEvent.Reason.DISABLED));
                        destroyGatewayClient(asset);
                    } else {
                        createGatewayClient(asset);
                    }
                    connector.setDisabled(disabled);
                }
            }
        } else {
            String gatewayId = assetIdGatewayIdMap.get(asset.getId());

            if (gatewayId != null) {
                LOG.fine("Attribute event for a gateway descendant asset (Asset ID=" + asset.getId() + ", Gateway ID=" + gatewayId + "): " + attribute);
                connector = gatewayConnectorMap.get(gatewayId);
                if (connector == null) {
                    LOG.warning("Gateway not found for descendant asset, this should not happen!!! (Asset ID=" + asset.getId() + ", Gateway ID=" + gatewayId + ")");
                } else {
                    if (!connector.isConnected()) {
                        LOG.info("Gateway is not connected so attribute event for descendant asset will be dropped (Asset ID=" + asset.getId() + ", Gateway ID=" + gatewayId + "): " + attribute);
                        throw new AssetProcessingException(AssetProcessingException.Reason.GATEWAY_DISCONNECTED, "Gateway is not connected: Gateway ID=" + connector.gatewayId);
                    }
                    LOG.fine("Attribute event for a gateway descendant asset being forwarded to the gateway (Asset ID=" + asset.getId() + ", Gateway ID=" + gatewayId + "): " + attribute);
                    connector.sendMessageToGateway(
                        new AttributeEvent(
                            asset.getId(),
                            attribute.getNameOrThrow(),
                            attribute.getValue().orElse(null),
                            attribute.getValueTimestamp().orElse(0L))
                            .setParentId(asset.getParentId()).setRealm(asset.getRealm()));
                }

                // Consume this event as it is for a gateway descendant and we've sent it to that gateway for processing
                return true;
            }
        }

        // Don't consume event for non gateway descendant assets
        return false;
    }

    public Asset mergeGatewayAsset(String gatewayId, Asset asset) {
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId);

        if (connector == null) {
            String msg = "Gateway not found: Gateway ID=" + gatewayId;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }

        boolean isUpdate = asset.getId() != null && assetIdGatewayIdMap.containsKey(asset.getId());
        return connector.mergeGatewayAsset(asset, isUpdate);
    }

    public boolean deleteGateway(String gatewayId) {
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId);

        if (connector == null) {
            String msg = "Gateway is not known: Gateway ID=" + gatewayId;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }

        if (connector.isConnected()) {
            connector.setDisabled(true);
        }

        // Get all child gateway assets
        List<String> gatewayAssetIds = assetIdGatewayIdMap.entrySet().stream()
            .filter(entry -> entry.getValue().equals(gatewayId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        gatewayAssetIds.add(gatewayId);

        return assetStorageService.delete(gatewayAssetIds, true);
    }

    public boolean deleteGatewayAssets(String gatewayId, List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return false;
        }

        GatewayConnector connector = gatewayConnectorMap.get(gatewayId);

        if (connector == null || !connector.isConnected()) {
            String msg = "Gateway is not connected: Gateway ID=" + gatewayId;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }

        return connector.deleteGatewayAssets(assetIds);
    }

    /**
     * Check if asset ID is a gateway asset registered locally on this manager
     */
    public boolean isLocallyRegisteredGateway(String assetId) {
        return gatewayConnectorMap.containsKey(assetId);
    }

    /**
     * Check if the specified asset ID or parent ID is a known gateway or descendant of a gateway.
     */
    public String getLocallyRegisteredGatewayId(String assetId, String parentId) {
        if (!active) {
            return null;
        }

        String gatewayId = assetIdGatewayIdMap.get(assetId);

        if (gatewayId != null) {
            return gatewayId;
        }

        if (parentId != null) {
            GatewayConnector connector = gatewayConnectorMap.get(parentId);

            if (connector != null) {
                return connector.gatewayId;
            }

            return getLocallyRegisteredGatewayId(parentId, null);
        }

        return null;
    }

    protected void processGatewayConnected(String gatewayClientId, String sessionId, Session session) {

        if (!active) {
            return;
        }

        String gatewayId = getGatewayIdFromClientId(gatewayClientId);
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId);

        if (connector == null) {
            LOG.warning("Gateway connected but not recognised which shouldn't happen: Gateway ID=" + gatewayId);
            clientEventService.sendToSession(sessionId, new GatewayDisconnectEvent(GatewayDisconnectEvent.Reason.UNRECOGNISED));
            clientEventService.closeSession(sessionId);
            return;
        }

        if (connector.isConnected()) {
            LOG.warning("Gateway already connected so requesting disconnect on new connection: Gateway ID=" + gatewayId);
            clientEventService.sendToSession(sessionId, new GatewayDisconnectEvent(GatewayDisconnectEvent.Reason.ALREADY_CONNECTED));
            clientEventService.closeSession(sessionId);
            return;
        }

        if (connector.isDisabled()) {
            LOG.warning("Gateway is currently disabled so will be ignored: Gateway ID=" + gatewayId);
            // Should not have got passed keycloak ensure keycloak client is removed
            destroyGatewayClient(connector.gateway);
            clientEventService.sendToSession(sessionId, new GatewayDisconnectEvent(GatewayDisconnectEvent.Reason.DISABLED));
            clientEventService.closeSession(sessionId);
            return;
        }

        connector.connect(createConnectorMessageConsumer(sessionId), () -> clientEventService.closeSession(sessionId));
    }

    protected void processGatewayDisconnected(String gatewayClientId) {

        if (!active) {
            return;
        }

        String gatewayId = getGatewayIdFromClientId(gatewayClientId);
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId);

        if (connector == null) {
            return;
        }

        connector.disconnect();
    }

    protected void processGatewayChange(Asset gateway, PersistenceEvent<Asset> persistenceEvent) {

        switch (persistenceEvent.getCause()) {

            case CREATE:
                createGatewayClient(gateway);
                synchronized (gatewayConnectorMap) {
                    GatewayConnector connector = new GatewayConnector(assetStorageService, assetProcessingService, executorService, gateway);
                    gatewayConnectorMap.put(gateway.getId(), connector);
                }
                break;
            case UPDATE:
                // Check if this gateway has a connector
                GatewayConnector connector = gatewayConnectorMap.get(gateway.getId());
                if (connector == null) {
                    break;
                }

                connector.gateway = gateway;

                // Check if disabled
                boolean isNowDisabled = gateway.getAttribute("disabled").flatMap(AssetAttribute::getValueAsBoolean).orElse(false);
                if (isNowDisabled) {
                    connector.sendMessageToGateway(new GatewayDisconnectEvent(GatewayDisconnectEvent.Reason.DISABLED));
                }
                connector.setDisabled(isNowDisabled);

                int attributeIndex = persistenceEvent.getPropertyNames() != null
                    ? IntStream.range(0, persistenceEvent.getPropertyNames().length)
                        .filter(i -> "attributes".equals(persistenceEvent.getPropertyNames()[i]))
                        .findFirst()
                        .orElse(-1)
                    : -1;
                if (attributeIndex >= 0) {
                    // Check if disabled attribute has changed
                    boolean wasDisabled = attributeFromJson(persistenceEvent.getPreviousState("attributes"),
                        gateway.getId(), "disabled").flatMap(AbstractValueHolder::getValueAsBoolean).orElse(false);
                    boolean isDisabled = attributeFromJson(persistenceEvent.getCurrentState("attributes"),
                        gateway.getId(), "disabled").flatMap(AbstractValueHolder::getValueAsBoolean).orElse(false);
                    if (wasDisabled != isDisabled) {
                        if (isNowDisabled) {
                            destroyGatewayClient(gateway);
                        } else {
                            createGatewayClient(gateway);
                        }
                    }
                }
                break;
            case DELETE:
                // Check if this gateway has a connector
                connector = gatewayConnectorMap.get(gateway.getId());
                if (connector == null) {
                    break;
                }

                synchronized (gatewayConnectorMap) {
                    connector = gatewayConnectorMap.remove(gateway.getId());

                    if (connector != null) {
                        connector.disconnect();
                    }
                }

                destroyGatewayClient(gateway);
                break;
        }
    }

    protected void processGatewayChildAssetChange(String gatewayId, Asset childAsset, PersistenceEvent<Asset> persistenceEvent) {
        // The asset would have been modified by the gateway connector so all we need to do here is update the id map
        switch (persistenceEvent.getCause()) {

            case CREATE:
            case UPDATE:
                synchronized (assetIdGatewayIdMap) {
                    assetIdGatewayIdMap.put(childAsset.getId(), gatewayId);
                }
                break;
            case DELETE:
                synchronized (assetIdGatewayIdMap) {
                    assetIdGatewayIdMap.remove(childAsset.getId());
                }
                break;
        }
    }

    protected boolean isGatewayConnected(String gatewayId) {
        AtomicBoolean connected = new AtomicBoolean(false);

        gatewayConnectorMap.computeIfPresent(gatewayId, (id, gatewayConnector) -> {
            connected.set(gatewayConnector.isConnected());
            return gatewayConnector;
        });

        return connected.get();
    }

    protected void createGatewayClient(Asset gateway) {

        LOG.info("Creating gateway keycloak client for gateway id: " + gateway.getId());

        String clientId = GATEWAY_CLIENT_ID_PREFIX + gateway.getId();
        String secret = gateway.getAttribute("clientSecret").flatMap(ValueHolder::getValueAsString).orElseGet(() -> UUID.randomUUID().toString());

        try {
            clientEventService.addClientCredentials(new ClientCredentials(gateway.getRealm(), null, clientId, secret));
            gateway.getAttribute("clientId").ifPresent(assetAttribute -> assetAttribute.setValue(Values.create(clientId)));
            gateway.getAttribute("clientSecret").ifPresent(assetAttribute -> assetAttribute.setValue(Values.create(secret)));
            try {
                assetStorageService.merge(gateway);
                LOG.info("Created gateway keycloak client for gateway id: " + gateway.getId());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to merge registered gateway: " + gateway.getId(), e);
            }
        } catch (Exception e) {
            LOG.warning("Failed to create client for gateway '" + gateway.getId());
        }
    }

    protected void destroyGatewayClient(Asset gateway) {
        String id = gateway.getAttribute("clientId").flatMap(AssetAttribute::getValueAsString).orElse(null);
        if (TextUtil.isNullOrEmpty(id)) {
            LOG.warning("Cannot find gateway keycloak client ID so cannot remove keycloak client for gateway: " + gateway.getId());
            return;
        }

        try {
            clientEventService.removeClientCredentials(gateway.getRealm(), id);
        } catch (Exception e) {
            LOG.warning("Failed to delete client for gateway '" + gateway.getId());
        }
    }

    protected Consumer<Object> createConnectorMessageConsumer(String sessionId) {
        return msg -> clientEventService.sendToSession(sessionId, msg);
    }

    protected void onGatewayClientEventReceived(String gatewayId, String messageId, SharedEvent event) {
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId);
        if (connector != null) {
            connector.onGatewayEvent(messageId, event);
        }
    }
}
