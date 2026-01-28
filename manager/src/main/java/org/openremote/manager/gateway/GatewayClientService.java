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
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.gateway.GatewayConnectionStatusEvent;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;
import static org.openremote.model.util.MapAccess.getString;

/**
 * Handles outbound connections to central managers
 */
public class GatewayClientService extends RouteBuilder implements ContainerService {

    public static final int PRIORITY = MessageBrokerService.PRIORITY + 100; // Start quite late
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayClientService.class.getName());
    public static final String CLIENT_EVENT_SESSION_PREFIX = GatewayClientService.class.getSimpleName() + ":";
    public static final String OR_GATEWAY_TUNNEL_LOCALHOST_REWRITE = "OR_GATEWAY_TUNNEL_LOCALHOST_REWRITE";
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected PersistenceService persistenceService;
    protected ClientEventService clientEventService;
    protected TimerService timerService;
    protected ManagerIdentityService identityService;
    protected final Map<String, GatewayClientConnector> connectionRealmMap = new ConcurrentHashMap<>();
    protected GatewayTunnelFactory tunnelFactory;

    @Override
    public void init(Container container) throws Exception {
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        persistenceService = container.getService(PersistenceService.class);
        clientEventService = container.getService(ClientEventService.class);
        timerService = container.getService(TimerService.class);
        identityService = container.getService(ManagerIdentityService.class);

        String tunnelKeyFile = getString(container.getConfig(), GatewayService.OR_GATEWAY_TUNNEL_SSH_KEY_FILE, null);
        String localhostRewrite = getString(container.getConfig(), OR_GATEWAY_TUNNEL_LOCALHOST_REWRITE, null);

        if (!TextUtil.isNullOrEmpty(tunnelKeyFile)) {
            File f = new File(tunnelKeyFile);
            if (f.exists()) {
                LOG.info("Gateway tunnelling SSH key file found at: " + f.getAbsolutePath());
                if (!TextUtil.isNullOrEmpty(localhostRewrite)) {
                    LOG.info("Gateway tunnelling localhostRewrite set to: " + localhostRewrite);
                }
                tunnelFactory = new MINAGatewayTunnelFactory(org.openremote.container.Container.EXECUTOR, f, localhostRewrite);
            } else {
                LOG.warning("Gateway tunnelling SSH key file does not exist, tunnelling support disabled: " + f.getAbsolutePath());
            }
        }

        container.getService(ManagerWebService.class).addApiSingleton(
            new GatewayClientResourceImpl(timerService, identityService, this)
        );

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        clientEventService.addSubscriptionAuthorizer((realm, authContext, eventSubscription) -> {
            if (!eventSubscription.isEventType(GatewayConnectionStatusEvent.class)) {
                return false;
            }

            if (authContext == null) {
                return false;
            }

            // If not a super user force a filter for the users realm
            if (!authContext.isSuperUser()) {
                @SuppressWarnings("unchecked")
                EventSubscription<GatewayConnectionStatusEvent> subscription = (EventSubscription<GatewayConnectionStatusEvent>) eventSubscription;
                subscription.setFilter(new RealmFilter<>(authContext.getAuthenticatedRealmName()));
            }

            return true;
        });
    }

    @Override
    public void start(Container container) throws Exception {
        if (tunnelFactory != null) {
            tunnelFactory.start();
        }

        // Get existing connections
        connectionRealmMap.putAll(persistenceService.doReturningTransaction(entityManager ->
            entityManager
                .createQuery("select gc from GatewayConnection gc", GatewayConnection.class)
                .getResultList())
                .stream()
                .collect(Collectors.toMap(
                        GatewayConnection::getLocalRealm,
                        connection -> new GatewayClientConnector(
                                connection,
                                tunnelFactory,
                                clientEventService,
                                timerService,
                                assetStorageService,
                                assetProcessingService))));
    }

    @Override
    public void stop(Container container) throws Exception {
        connectionRealmMap.forEach((realm, client) -> {
            try {
                client.close();
            } catch (Exception ignored) {}
        });
        connectionRealmMap.clear();

        if (tunnelFactory != null) {
            tunnelFactory.stop();
        }
    }

    @Override
    public void configure() throws Exception {

        from(PERSISTENCE_TOPIC)
            .routeId("Persistence-GatewayConnection")
            .filter(isPersistenceEventForEntityType(GatewayConnection.class))
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                PersistenceEvent<GatewayConnection> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                GatewayConnection connection = persistenceEvent.getEntity();
                processConnectionChange(connection, persistenceEvent.getCause());
            });
    }

    @SuppressWarnings("resource")
    synchronized protected void processConnectionChange(GatewayConnection connection, PersistenceEvent.Cause cause) {

        LOG.info("Modified gateway client connection '" + cause + "': " + connection);

        connectionRealmMap.compute(connection.getLocalRealm(), (realm, existingConnection) -> {
            if (existingConnection != null) {
                try {
                    existingConnection.close();
                } catch (Exception ignored) {}
            }

            if (cause != PersistenceEvent.Cause.DELETE) {
                return new GatewayClientConnector(
                    connection,
                    tunnelFactory,
                    clientEventService,
                    timerService,
                    assetStorageService,
                    assetProcessingService);
            }
            return null;
        });
    }

    /** GATEWAY RESOURCE METHODS */
    protected List<GatewayConnection> getConnections() {
        return connectionRealmMap.values().stream().map(GatewayClientConnector::getConnection).toList();
    }

    public void setConnection(GatewayConnection connection) {
        LOG.info("Updating/creating gateway connection: " + connection);
        persistenceService.doTransaction(em -> em.merge(connection));
    }

    public boolean deleteConnections(List<String> realms) {
        LOG.info("Deleting gateway connections for the following realm(s): " + Arrays.toString(realms.toArray()));

        try {
            persistenceService.doTransaction(em -> {

                List<GatewayConnection> connections = em
                    .createQuery("select gc from GatewayConnection gc where gc.localRealm in :realms", GatewayConnection.class)
                    .setParameter("realms", realms)
                    .getResultList();

                if (connections.size() != realms.size()) {
                    throw new IllegalArgumentException("Cannot delete one or more requested gateway connections as they don't exist");
                }

                connections.forEach(em::remove);
            });
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    protected ConnectionStatus getConnectionStatus(String realm) {
        return Optional.ofNullable(connectionRealmMap.get(realm)).map(GatewayClientConnector::getConnectionStatus)
            .orElse(null);
    }
}
