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

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.GatewayV2Asset;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * Manages {@link GatewayV2Asset}s in the local instance by creating Keycloak clients
 * only used for generation the client id and secret for the gateway (V2) asset.
 */
public class GatewayV2Service extends RouteBuilder implements ContainerService {
    public static final int PRIORITY = HIGH_PRIORITY + 100;
    public static final String GATEWAY_CLIENT_ID_PREFIX = "gateway-";
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayV2Service.class.getName());
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected ManagerIdentityService identityService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected ScheduledExecutorService executorService;
    protected boolean active;

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
        executorService = container.getExecutorService();

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("Incoming edge gateway connections disabled: Not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
            container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        }
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }


    protected void processGatewayChange(GatewayV2Asset gateway, PersistenceEvent<Asset<?>> persistenceEvent) {
        switch (persistenceEvent.getCause()) {

            case CREATE -> {
                createUpdateGatewayServiceUser(gateway);
            }
            case UPDATE -> {
                break;
            }
            case DELETE -> {
                removeGatewayServiceUser(gateway);
            }
        }
    }

    protected void createUpdateGatewayServiceUser(GatewayV2Asset gateway) {
        LOG.info("Creating/updating gateway service user for gateway id: " + gateway.getId());
        String clientId = getGatewayClientId(gateway.getId());
        String secret = gateway.getClientSecret().orElseGet(() -> UUID.randomUUID().toString());

        try {
            User gatewayUser = identityProvider.getUserByUsername(gateway.getRealm(), User.SERVICE_ACCOUNT_PREFIX + clientId);
            boolean userExists = gatewayUser != null;

            if (gatewayUser == null || gatewayUser.getEnabled() == gateway.getDisabled().orElse(false) || Objects.equals(gatewayUser.getSecret(), gateway.getClientSecret().orElse(null))) {

                gatewayUser = identityProvider.createUpdateUser(gateway.getRealm(), new User()
                        .setServiceAccount(true)
                        .setSystemAccount(true)
                        .setUsername(clientId)
                        .setEnabled(!gateway.getDisabled().orElse(false)), secret, true);
            }

            if (!userExists && gatewayUser != null) {
                identityProvider.updateUserRoles(gateway.getRealm(), gatewayUser.getId(), Constants.KEYCLOAK_CLIENT_ID, ClientRole.WRITE.getValue());
            }

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

                                if (!(eventAsset instanceof GatewayV2Asset)) {
                                    return;
                                }
                                processGatewayChange((GatewayV2Asset) eventAsset, persistenceEvent);
                            }
                    );

        }
    }
}
