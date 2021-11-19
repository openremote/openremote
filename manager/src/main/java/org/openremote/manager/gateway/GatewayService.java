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
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.rules.RulesService;
import org.openremote.manager.rules.RulesetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.GatewayAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.AttributeWriteFailure;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.gateway.GatewayDisconnectEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import javax.persistence.EntityManager;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
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
import static org.openremote.manager.gateway.GatewayConnector.mapAssetId;
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * Manages {@link org.openremote.model.asset.impl.GatewayAsset}s in the local instance by creating Keycloak clients
 * for them and handles the connection logic of gateways; it is the gateways responsibility to connect to this instance,
 * it is then up to this instance to authenticate the gateway and to initiate synchronisation of gateway assets.
 */
public class GatewayService extends RouteBuilder implements ContainerService, AssetUpdateProcessor {

    public static final int PRIORITY = HIGH_PRIORITY + 100;
    public static final String GATEWAY_CLIENT_ID_PREFIX = "gateway-";
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayService.class.getName());
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected ManagerIdentityService identityService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected RulesetStorageService rulesetStorageService;
    protected RulesService rulesService;
    protected ScheduledExecutorService executorService;
    /**
     * Maps gateway asset IDs to connections; note that gateway asset IDs are stored lower case so that they can be
     * matched up to the service user client ID (which needs to be all lower case); this could technically cause an
     * ID collision but for now the odds of that are low enough to not be a concern.
     */
    protected final Map<String, GatewayConnector> gatewayConnectorMap = new HashMap<>();
    protected final Map<String, String> assetIdGatewayIdMap = new HashMap<>();
    protected boolean active;
    protected List<String> tenantIds = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static Predicate isNotForGateway(GatewayService gatewayService) {
        return exchange -> {
            if (isPersistenceEventForEntityType(Asset.class).matches(exchange)) {
                PersistenceEvent<Asset<?>> persistenceEvent = (PersistenceEvent<Asset<?>>)exchange.getIn().getBody(PersistenceEvent.class);
                Asset<?> asset = persistenceEvent.getEntity();

                // Check if asset parent is a gateway or a gateway descendant, if so ignore it
                // Need to look at parent as this asset may not have been acknowledged by the gateway service yet
                return gatewayService.getLocallyRegisteredGatewayId(asset.getId(), asset.getParentId()) == null;
            }
            if (isPersistenceEventForEntityType(Tenant.class).matches(exchange)) {
                PersistenceEvent<Tenant> persistenceEvent = (PersistenceEvent<Tenant>)exchange.getIn().getBody(PersistenceEvent.class);
                Tenant tenant = persistenceEvent.getEntity();

                if (persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE) {
                    // Ruleset won't exist in storage so check cache
                    return gatewayService.tenantIds.remove(tenant.getId());
                }
                Tenant localTenant = gatewayService.identityProvider.getTenant(tenant.getRealm());
                if (localTenant != null && localTenant.getId().equals(tenant.getId())) {
                    gatewayService.tenantIds.add(tenant.getId());
                    return true;
                }
                return false;
            }
            if (isPersistenceEventForEntityType(Ruleset.class).matches(exchange)) {
                PersistenceEvent<Ruleset> persistenceEvent = (PersistenceEvent<Ruleset>)exchange.getIn().getBody(PersistenceEvent.class);
                Ruleset ruleset = persistenceEvent.getEntity();

                if (persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE) {
                    // Ruleset won't exist in storage so need to check engines
                    return gatewayService.rulesService.isRulesetKnown(ruleset);
                }
                return gatewayService.rulesetStorageService.find(ruleset.getClass(), ruleset.getId()) != null;
            }
            return true;
        };
    }

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
        executorService = container.getExecutorService();
        rulesetStorageService = container.getService(RulesetStorageService.class);
        rulesService = container.getService(RulesService.class);

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

        List<GatewayAsset> gateways = assetStorageService.findAll(new AssetQuery().types(GatewayAsset.class))
            .stream()
            .map(asset -> (GatewayAsset)asset)
            .collect(Collectors.toList());
        List<String> gatewayIds = gateways.stream().map(Asset::getId).collect(Collectors.toList());
        gateways = gateways.stream()
            .filter(gateway ->
                Arrays.stream(gateway.getPath()).noneMatch(p -> !p.equals(gateway.getId()) && gatewayIds.contains(p)))
            .collect(Collectors.toList());

        if (!gateways.isEmpty()) {
            LOG.info("Directly registered gateways found = " + gateways.size());

            gateways.forEach(gateway -> {

                // Check if client has been created
                boolean hasClientId = gateway.getClientId().isPresent();
                boolean hasClientSecret = gateway.getClientSecret().isPresent();

                if (!hasClientId || !hasClientSecret) {
                    createUpdateGatewayServiceUser(gateway);
                }

                // Create connector
                GatewayConnector connector = new GatewayConnector(assetStorageService, assetProcessingService, executorService, gateway);
                gatewayConnectorMap.put(gateway.getId().toLowerCase(Locale.ROOT), connector);

                // Get IDs of all assets under this gateway
                List<Asset<?>> gatewayAssets = assetStorageService
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
                    PersistenceEvent<Asset<?>> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                    Asset<?> eventAsset = persistenceEvent.getEntity();

                    if (persistenceEvent.getCause() != PersistenceEvent.Cause.DELETE) {
                        eventAsset = assetStorageService.find(eventAsset.getId(), true);
                    }

                    if (eventAsset == null) {
                        return;
                    }

                    // Only gateways locally registered to this manager are of interest or gateway descendant assets
                    if (eventAsset instanceof GatewayAsset
                        && (isLocallyRegisteredGateway(eventAsset.getId()) || getLocallyRegisteredGatewayId(eventAsset.getId(), eventAsset.getParentId()) == null)) {

                        processGatewayChange((GatewayAsset)eventAsset, persistenceEvent);

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
        String clientId = ClientEventService.getClientId(exchange);

        if (!isGatewayClientId(clientId)) {
            return;
        }

        if (header(ConnectionConstants.SESSION_OPEN).matches(exchange)) {
            Session session = exchange.getIn().getHeader(ConnectionConstants.SESSION, Session.class);
            String sessionKey = ClientEventService.getSessionKey(exchange);
            processGatewayConnected(clientId, sessionKey, session);
            return;
        }

        if (or(header(ConnectionConstants.SESSION_CLOSE), header(ConnectionConstants.SESSION_CLOSE_ERROR)).matches(exchange)) {
            processGatewayDisconnected(clientId);
            return;
        }

        // Inbound shared events
        if (and(ClientEventService::isInbound, body().isInstanceOf(SharedEvent.class)).matches(exchange)) {
            ClientEventService.stopMessage(exchange);
            String gatewayId = getGatewayIdFromClientId(clientId);
            onGatewayClientEventReceived(gatewayId, exchange.getIn().getHeader(ClientEventService.HEADER_REQUEST_RESPONSE_MESSAGE_ID, String.class), exchange.getIn().getBody(SharedEvent.class));
        }
    }

    @Override
    public boolean processAssetUpdate(EntityManager em, Asset<?> asset, Attribute<?> attribute, AttributeEvent.Source source) throws AssetProcessingException {

        // If the update was generated by a gateway then we don't want to process it here
        if (source == AttributeEvent.Source.GATEWAY) {
            return false;
        }

        GatewayConnector connector = gatewayConnectorMap.get(asset.getId().toLowerCase(Locale.ROOT));

        if (connector != null) {
            LOG.fine("Attribute event for a locally registered gateway asset (Asset ID=" + asset.getId() + "): " + attribute);

            GatewayAsset gatewayAsset = (GatewayAsset)asset;

            // This is a change to a locally registered gateway
            if (GatewayAsset.DISABLED.getName().equals(attribute.getName())) {
                boolean disabled = attribute.getValueAs(Boolean.class).orElse(false);
                boolean isAlreadyDisabled = gatewayAsset.getDisabled().orElse(false);
                gatewayAsset.setDisabled(disabled); // Ensure we update state

                if (disabled != isAlreadyDisabled) {
                    createUpdateGatewayServiceUser(gatewayAsset);
                    if (disabled) {
                        connector.sendMessageToGateway(new GatewayDisconnectEvent(GatewayDisconnectEvent.Reason.DISABLED));
                    }
                    connector.setDisabled(disabled);
                }
            }

            if (GatewayAsset.CLIENT_SECRET.getName().equals(attribute.getName())) {
                String newSecret = attribute.getValueAs(String.class).orElse(null);
                if (!TextUtil.isNullOrEmpty(newSecret)) {
                    LOG.fine("Gateway client secret attribute updated so updating gateway service user secret to match: (Gateway ID=" + asset.getId() + ")");
                    User gatewayServiceUser = identityProvider.getUserByUsername(asset.getRealm(), User.SERVICE_ACCOUNT_PREFIX + ((GatewayAsset) asset).getClientId().orElse(""));
                    if (gatewayServiceUser != null) {
                        identityProvider.resetSecret(asset.getRealm(), gatewayServiceUser.getId(), newSecret);
                    } else {
                        LOG.info("Couldn't retrieve gateway service user to update secret: (Gateway ID=" + asset.getId() + ")");
                    }
                } else {
                    // Push old secret back
                    assetProcessingService.sendAttributeEvent(new AttributeEvent(asset.getId(), GatewayAsset.CLIENT_SECRET, gatewayAsset.getClientSecret().orElseThrow(() -> new IllegalStateException("Gateway client secret is null which was not expected"))));
                }
            }
        } else {
            String gatewayId = assetIdGatewayIdMap.get(asset.getId());

            if (gatewayId != null) {
                LOG.fine("Attribute event for a gateway descendant asset (Asset<?> ID=" + asset.getId() + ", Gateway ID=" + gatewayId + "): " + attribute);
                connector = gatewayConnectorMap.get(gatewayId.toLowerCase(Locale.ROOT));
                if (connector == null) {
                    LOG.warning("Gateway not found for descendant asset, this should not happen!!! (Asset<?> ID=" + asset.getId() + ", Gateway ID=" + gatewayId + ")");
                } else {
                    if (!connector.isConnected()) {
                        LOG.info("Gateway is not connected so attribute event for descendant asset will be dropped (Asset<?> ID=" + asset.getId() + ", Gateway ID=" + gatewayId + "): " + attribute);
                        throw new AssetProcessingException(AttributeWriteFailure.GATEWAY_DISCONNECTED, "Gateway is not connected: Gateway ID=" + connector.gatewayId);
                    }
                    LOG.fine("Attribute event for a gateway descendant asset being forwarded to the gateway (Asset<?> ID=" + asset.getId() + ", Gateway ID=" + gatewayId + "): " + attribute);
                    connector.sendMessageToGateway(
                        new AttributeEvent(
                            mapAssetId(gatewayId, asset.getId(), true),
                            attribute.getName(),
                            attribute.getValue().orElse(null),
                            attribute.getTimestamp().orElse(0L))
                            .setParentId(mapAssetId(gatewayId, asset.getParentId(), true)).setRealm(asset.getRealm()));
                }

                // Consume this event as it is for a gateway descendant and we've sent it to that gateway for processing
                return true;
            }
        }

        // Don't consume event for non gateway descendant assets
        return false;
    }

    public <T extends Asset<?>> T mergeGatewayAsset(String gatewayId, T asset) {
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId.toLowerCase(Locale.ROOT));

        if (connector == null) {
            String msg = "Gateway not found: Gateway ID=" + gatewayId;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }

        boolean isUpdate = asset.getId() != null && assetIdGatewayIdMap.containsKey(asset.getId());
        return connector.mergeGatewayAsset(asset, isUpdate);
    }

    public boolean deleteGateway(String gatewayId) {
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId.toLowerCase(Locale.ROOT));

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

        GatewayConnector connector = gatewayConnectorMap.get(gatewayId.toLowerCase(Locale.ROOT));

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
        return gatewayConnectorMap.containsKey(assetId.toLowerCase(Locale.ROOT));
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
            GatewayConnector connector = gatewayConnectorMap.get(parentId.toLowerCase(Locale.ROOT));

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
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId.toLowerCase(Locale.ROOT));

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
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId.toLowerCase(Locale.ROOT));

        if (connector == null) {
            return;
        }

        connector.disconnect();
    }

    protected void processGatewayChange(GatewayAsset gateway, PersistenceEvent<Asset<?>> persistenceEvent) {

        switch (persistenceEvent.getCause()) {

            case CREATE:
                createUpdateGatewayServiceUser(gateway);
                synchronized (gatewayConnectorMap) {
                    GatewayConnector connector = new GatewayConnector(assetStorageService, assetProcessingService, executorService, gateway);
                    gatewayConnectorMap.put(gateway.getId().toLowerCase(Locale.ROOT), connector);
                }
                break;
            case UPDATE:
                // Check if this gateway has a connector
                GatewayConnector connector = gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT));
                if (connector == null) {
                    break;
                }

                connector.gateway = gateway;

                // Check if disabled
                boolean isNowDisabled = gateway.getDisabled().orElse(false);
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
                    AttributeMap oldAttributes = persistenceEvent.getPreviousState("attributes");
                    boolean wasDisabled = oldAttributes.getValue(GatewayAsset.DISABLED).orElse(false);

                    if (wasDisabled != isNowDisabled) {
                        createUpdateGatewayServiceUser(gateway);
                    }
                }
                break;
            case DELETE:
                // Check if this gateway has a connector
                connector = gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT));
                if (connector == null) {
                    break;
                }

                synchronized (gatewayConnectorMap) {
                    connector = gatewayConnectorMap.remove(gateway.getId().toLowerCase(Locale.ROOT));

                    if (connector != null) {
                        connector.disconnect();
                    }
                }

                removeGatewayServiceUser(gateway);
                break;
        }
    }

    protected void processGatewayChildAssetChange(String gatewayId, Asset<?> childAsset, PersistenceEvent<Asset<?>> persistenceEvent) {
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

        gatewayConnectorMap.computeIfPresent(gatewayId.toLowerCase(Locale.ROOT), (id, gatewayConnector) -> {
            connected.set(gatewayConnector.isConnected());
            return gatewayConnector;
        });

        return connected.get();
    }

    public static String getGatewayClientId(String gatewayAssetId) {
        String clientId = GATEWAY_CLIENT_ID_PREFIX + gatewayAssetId.toLowerCase(Locale.ROOT);
        if (clientId.length() > 255) {
            clientId = clientId.substring(0, 254);
        }
        return clientId;
    }

    protected void createUpdateGatewayServiceUser(GatewayAsset gateway) {

        LOG.info("Creating/updating gateway service user for gateway id: " + gateway.getId());
        String clientId = getGatewayClientId(gateway.getId());
        String secret = gateway.getClientSecret().orElseGet(() -> UUID.randomUUID().toString());

        try {
            identityProvider.createUpdateUser(gateway.getRealm(), new User()
                .setServiceAccount(true)
                .setSystemAccount(true)
                .setUsername(clientId)
                .setEnabled(!gateway.getDisabled().orElse(false)), secret);

            if (!clientId.equals(gateway.getClientId().orElse(null)) || !secret.equals(gateway.getClientSecret().orElse(null))) {
                gateway.setClientId(clientId);
                gateway.setClientSecret(secret);
                assetStorageService.merge(gateway);
            }

            try {
                LOG.info("Created gateway keycloak client for gateway id: " + gateway.getId());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to merge registered gateway: " + gateway.getId(), e);
            }
        } catch (Exception e) {
            LOG.warning("Failed to create client for gateway '" + gateway.getId());
        }
    }

    protected void removeGatewayServiceUser(GatewayAsset gateway) {
        String id = gateway.getClientId().orElse(null);
        if (TextUtil.isNullOrEmpty(id)) {
            LOG.warning("Cannot find gateway keycloak client ID so cannot remove keycloak client for gateway: " + gateway.getId());
            return;
        }
        identityProvider.deleteClient(gateway.getRealm(), id);
    }

    protected Consumer<Object> createConnectorMessageConsumer(String sessionId) {
        return msg -> clientEventService.sendToSession(sessionId, msg);
    }

    protected void onGatewayClientEventReceived(String gatewayId, String messageId, SharedEvent event) {
        GatewayConnector connector = gatewayConnectorMap.get(gatewayId.toLowerCase(Locale.ROOT));
        if (connector != null) {
            connector.onGatewayEvent(messageId, event);
        }
    }
}
