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

import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.GatewayV2Asset;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.util.*;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * Manages {@link GatewayV2Asset}s in the local instance by creating Keycloak clients
 * only used for generation the client id and secret for the gateway (V2) asset.
 */
public class GatewayV2Service extends RouteBuilder implements ContainerService {
    public static final int PRIORITY = HIGH_PRIORITY + 100;
    public static final String GATEWAY_CLIENT_ID_PREFIX = "gateway-";
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayV2Service.class.getName());
    protected final Map<GatewayV2Asset, List<String>> gatewayAssetsMap = new HashMap<>();
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected ManagerIdentityService identityService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected boolean active;

    /**
     * Generates a client ID for a gateway asset by appending the asset ID to a prefix.
     * Ensures the client ID does not exceed 255 characters.
     *
     * @param gatewayAssetId the ID of the gateway asset
     * @return the generated client ID
     */
    public static String getGatewayClientId(String gatewayAssetId) {
        String clientId = GATEWAY_CLIENT_ID_PREFIX + gatewayAssetId.toLowerCase(Locale.ROOT);
        if (clientId.length() > 255) {
            clientId = clientId.substring(0, 254);
        }
        return clientId;
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

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("Gateway connections disabled: Not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
            container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        }
    }

    @Override
    public void start(Container container) throws Exception {
        if (!active) {
            return;
        }
        List<GatewayV2Asset> gateways = assetStorageService.findAll(new AssetQuery().types(GatewayV2Asset.class))
                .stream()
                .map(GatewayV2Asset.class::cast)
                .toList();

        if (gateways.isEmpty()) {
            return;
        }

        // Fill the map with the gateway id and the list of asset ids for each gateway
        for (GatewayV2Asset gateway : gateways) {
            List<Asset<?>> gatewayAssets = assetStorageService
                    .findAll(
                            new AssetQuery()
                                    .parents(gateway.getId())
                                    .select(new AssetQuery.Select().excludeAttributes())
                                    .recursive(true));

            gatewayAssetsMap.put(gateway, gatewayAssets.stream().map(Asset::getId).toList());
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        gatewayAssetsMap.clear();
    }

    /**
     * Processes changes to a gateway asset, handling creation and deletion events.
     * Updates the gateway's enabled status and manages service user creation or removal.
     *
     * @param gateway the gateway asset being processed
     * @param persistenceEvent the persistence event triggering the change
     */
    protected void processGatewayChange(GatewayV2Asset gateway, PersistenceEvent<Asset<?>> persistenceEvent) {
        if (Objects.requireNonNull(persistenceEvent.getCause()) == PersistenceEvent.Cause.CREATE) {
            gateway.setDisabled(false); // Ensure gateway is enabled when created
            createUpdateGatewayServiceUser(gateway);
            synchronized (gatewayAssetsMap) {
                gatewayAssetsMap.put(gateway, new ArrayList<>());
            }

        } else if (persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE) {
            removeGatewayServiceUser(gateway);
            synchronized (gatewayAssetsMap) {
                gatewayAssetsMap.remove(gateway);
            }
        }
    }

    /**
     * Processes changes to child assets of a gateway, handling creation and deletion events.
     * Updates the list of child asset IDs associated with the gateway.
     *
     * @param gatewayId the ID of the gateway asset
     * @param childAsset the child asset being processed
     * @param persistenceEvent the persistence event triggering the change
     */
    protected void processGatewayChildAssetChange(String gatewayId, Asset<?> childAsset, PersistenceEvent<Asset<?>> persistenceEvent) {

        switch (persistenceEvent.getCause()) {
            case CREATE -> {
                synchronized (gatewayAssetsMap) {
                    gatewayAssetsMap.keySet().stream()
                            .filter(gateway -> gateway.getId().equals(gatewayId))
                            .findFirst()
                            .ifPresent(gateway -> gatewayAssetsMap.get(gateway).add(childAsset.getId()));
                }
            }
            case DELETE -> {
                synchronized (gatewayAssetsMap) {
                    gatewayAssetsMap.keySet().stream()
                            .filter(gateway -> gateway.getId().equals(gatewayId))
                            .findFirst()
                            .ifPresent(gateway -> gatewayAssetsMap.get(gateway).remove(childAsset.getId()));
                }
            }
            default -> {
                // Ignore
            }
        }
    }

    /**
     * Creates or updates a service user for a gateway asset in Keycloak.
     * Ensures the user is enabled and has the correct roles and credentials.
     *
     * @param gateway the gateway asset for which the service user is created or updated
     */
    protected void createUpdateGatewayServiceUser(GatewayV2Asset gateway) {
        LOG.info("Creating/updating gateway service user for gateway id: " + gateway.getId());
        String clientId = getGatewayClientId(gateway.getId());
        String secret = gateway.getClientSecret().orElseGet(() -> UUID.randomUUID().toString());

        try {
            User gatewayUser = identityProvider.getUserByUsername(gateway.getRealm(), User.SERVICE_ACCOUNT_PREFIX + clientId);
            boolean userExists = gatewayUser != null;

            if (gatewayUser == null || Objects.equals(gatewayUser.getEnabled(), gateway.getDisabled().orElse(false)) || Objects.equals(gatewayUser.getSecret(), gateway.getClientSecret().orElse(null))) {

                gatewayUser = identityProvider.createUpdateUser(gateway.getRealm(), new User()
                        .setServiceAccount(true)
                        .setSystemAccount(true)
                        .setUsername(clientId)
                        .setEnabled(!gateway.getDisabled().orElse(false)), secret, true);
            }

            if (!userExists && gatewayUser != null) {
                identityProvider.updateUserRoles(gateway.getRealm(), gatewayUser.getId(), KEYCLOAK_CLIENT_ID, ClientRole.WRITE.getValue());
            }

            if (!clientId.equals(gateway.getClientId().orElse(null)) || !secret.equals(gateway.getClientSecret().orElse(null))) {
                gateway.setClientId(clientId);
                gateway.setClientSecret(secret);
                assetStorageService.merge(gateway);
            }

            LOG.info("Created gateway keycloak client for gateway id: " + gateway.getId());

        } catch (Exception e) {
            LOG.warning("Failed to create client for gateway '" + gateway.getId());
        }
    }

    /**
     * Removes the service user associated with a gateway asset from Keycloak.
     *
     * @param gateway the gateway asset for which the service user is removed
     */
    protected void removeGatewayServiceUser(GatewayV2Asset gateway) {
        String id = gateway.getClientId().orElse(null);
        if (TextUtil.isNullOrEmpty(id)) {
            LOG.warning("Cannot find gateway keycloak client ID so cannot remove keycloak client for gateway: " + gateway.getId());
            return;
        }
        identityProvider.deleteClient(gateway.getRealm(), id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "active=" + active +
                '}';
    }

    /**
     * Retrieves the registered gateway ID for a given asset ID, checking both direct and descendant relationships.
     *
     * @param assetId the ID of the asset
     * @param parentId the ID of the parent asset, can be null
     * @return the ID of the registered gateway, or null if not found
     */
    public String getRegisteredGatewayId(String assetId, String parentId) {
        String gatewayId = null;
        // check whether the assetId can be found in the gateway child assets
        for (Map.Entry<GatewayV2Asset, List<String>> entry : gatewayAssetsMap.entrySet()) {
            if (entry.getValue().contains(assetId)) {
                gatewayId = entry.getKey().getId();
            }
        }

        if (gatewayId != null) {
            return gatewayId;
        }

        // check whether the parentId can be found in one of the gateway keys
        if (parentId != null) {
            gatewayId = gatewayAssetsMap.keySet().stream()
                    .filter(gateway -> gateway.getId().equals(parentId))
                    .findFirst()
                    .map(GatewayV2Asset::getId)
                    .orElse(null);

            if (gatewayId != null)
            {
                return gatewayId;
            }
            // check whether the parentId is a descendant of a gateway asset
            return getRegisteredGatewayId(parentId, null);
        }

        return null;
    }

    /**
     * Checks if an asset is a registered gateway.
     *
     * @param assetId the ID of the asset to check
     * @return true if the asset is a registered gateway, false otherwise
     */
    public boolean isRegisteredGateway(String assetId) {
        return gatewayAssetsMap.keySet().stream().anyMatch(gateway -> gateway.getId().equals(assetId));
    }

    /**
     * Checks if an asset is a descendant of any registered gateway.
     *
     * @param assetId the ID of the asset to check
     * @return true if the asset is a descendant of a gateway, false otherwise
     */
    public boolean isGatewayDescendant(String assetId) {
        return gatewayAssetsMap.values().stream().anyMatch(list -> list.contains(assetId));
    }

    /**
     * Checks if an asset is either a registered gateway or a descendant of one.
     *
     * @param assetId the ID of the asset to check
     * @return true if the asset is a gateway or a descendant, false otherwise
     */
    public boolean isGatewayOrDescendant(String assetId) {
        return isRegisteredGateway(assetId) || isGatewayDescendant(assetId);
    }

    /**
     * Checks if an asset is a descendant of a specified gateway.
     *
     * @param assetId the ID of the asset to check
     * @param gatewayId the ID of the gateway
     * @return true if the asset is a descendant of the specified gateway, false otherwise
     */
    public boolean isGatewayDescendant(String assetId, String gatewayId) {
        return gatewayAssetsMap.entrySet().stream()
                .filter(entry -> entry.getKey().getId().equals(gatewayId))
                .anyMatch(entry -> entry.getValue().contains(assetId));
    }

    /**
     * Retrieves the gateway asset associated with an MQTT connection, if it is a gateway connection.
     *
     * @param connection the MQTT connection
     * @return the associated GatewayV2Asset, or null if not a gateway connection
     */
    public GatewayV2Asset getGatewayFromMQTTConnection(RemotingConnection connection) {
        if (isGatewayConnection(connection)) {
            return (GatewayV2Asset) assetStorageService.find(new AssetQuery().types(GatewayV2Asset.class)
                    .attributeValue("clientId", connection.getClientID()));
        }
        return null;
    }

    /**
     * Determines if a connection is a gateway connection based on its client ID.
     *
     * @param connection the MQTT connection
     * @return true if the connection is a gateway connection, false otherwise
     */
    protected static boolean isGatewayConnection(RemotingConnection connection) {
        return connection.getClientID().startsWith(GATEWAY_CLIENT_ID_PREFIX);
    }

    @Override
    public void configure() throws Exception {
        if (active) {
            from(PERSISTENCE_TOPIC)
                    .routeId("Persistence-GatewayV2Asset")
                    .filter(isPersistenceEventForEntityType(Asset.class))
                    .process(exchange -> {
                                @SuppressWarnings("unchecked")
                                PersistenceEvent<Asset<?>> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                                Asset<?> eventAsset = persistenceEvent.getEntity();
                                if (eventAsset instanceof GatewayV2Asset gatewayAsset) {
                                    processGatewayChange(gatewayAsset, persistenceEvent);
                                } else {
                                    String gatewayId = getRegisteredGatewayId(eventAsset.getId(), eventAsset.getParentId());
                                    if (gatewayId != null) {
                                        processGatewayChildAssetChange(gatewayId, eventAsset, persistenceEvent);
                                    }
                                }

                            }
                    );
        }
    }
}
