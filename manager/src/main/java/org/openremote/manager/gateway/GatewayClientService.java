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

import io.netty.channel.ChannelHandler;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.openremote.container.web.OAuthClientCredentialsGrant;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.websocket.WebsocketIoClient;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.EventRequestResponseWrapper;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.gateway.GatewayConnectionStatusEvent;
import org.openremote.model.gateway.GatewayDisconnectEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.syslog.SyslogCategory;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * Handles outbound connections to central managers
 */
public class GatewayClientService extends RouteBuilder implements ContainerService {

    public static final int PRIORITY = ManagerWebService.PRIORITY - 300;
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayClientService.class.getName());
    public static final String CLIENT_EVENT_SESSION_PREFIX = GatewayClientService.class.getSimpleName() + ":";
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected PersistenceService persistenceService;
    protected ClientEventService clientEventService;
    protected TimerService timerService;
    protected ManagerExecutorService executorService;
    protected ManagerIdentityService identityService;
    protected final Map<String, GatewayConnection> connectionRealmMap = new HashMap<>();
    protected final Map<String, WebsocketIoClient<String>> clientRealmMap = new HashMap<>();

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        persistenceService = container.getService(PersistenceService.class);
        clientEventService = container.getService(ClientEventService.class);
        timerService = container.getService(TimerService.class);
        executorService = container.getService(ManagerExecutorService.class);
        identityService = container.getService(ManagerIdentityService.class);

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new GatewayClientResourceImpl(timerService, identityService, this)
        );

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        clientEventService.addSubscriptionAuthorizer((authContext, eventSubscription) -> {
            if (!eventSubscription.isEventType(GatewayConnectionStatusEvent.class)) {
                return false;
            }

            // If not a super user force a filter for the users realm
            if (!authContext.isSuperUser()) {
                @SuppressWarnings("unchecked")
                EventSubscription<GatewayConnectionStatusEvent> subscription = (EventSubscription<GatewayConnectionStatusEvent>) eventSubscription;
                subscription.setFilter(new TenantFilter<>(authContext.getAuthenticatedRealm()));
            }

            return true;
        });
    }

    @Override
    public void start(Container container) throws Exception {

        // Get existing connections
        connectionRealmMap.putAll(persistenceService.doReturningTransaction(entityManager ->
            entityManager
                .createQuery("select gc from GatewayConnection gc", GatewayConnection.class)
                .getResultList()).stream().collect(Collectors.toMap(GatewayConnection::getLocalRealm, gc -> gc)));

        // Create clients for enabled connections
        connectionRealmMap.forEach((realm, connection) -> {
            if (!connection.isDisabled()) {
                clientRealmMap.put(realm, createGatewayClient(connection));
            }
        });
    }

    @Override
    public void stop(Container container) throws Exception {
        clientRealmMap.forEach((realm, client) -> {
            if (client != null) {
                destroyGatewayClient(connectionRealmMap.get(realm), client);
            }
        });
        clientRealmMap.clear();
        connectionRealmMap.clear();
    }

    @Override
    public void configure() throws Exception {

        from(PERSISTENCE_TOPIC)
            .routeId("GatewayServiceConnectionChanges")
            .filter(isPersistenceEventForEntityType(GatewayConnection.class))
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                PersistenceEvent<GatewayConnection> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                GatewayConnection connection = persistenceEvent.getEntity();
                processConnectionChange(connection, persistenceEvent.getCause());
            });
    }

    synchronized protected void processConnectionChange(GatewayConnection connection, PersistenceEvent.Cause cause) {

        LOG.info("Modified gateway client connection '" + cause + "': " + connection);

        synchronized (clientRealmMap) {
            switch (cause) {

                case UPDATE:
                    WebsocketIoClient<String> client = clientRealmMap.remove(connection.getLocalRealm());
                    if (client != null) {
                        destroyGatewayClient(connection, client);
                    }
                case CREATE:
                    connectionRealmMap.put(connection.getLocalRealm(), connection);
                    if (!connection.isDisabled()) {
                        clientRealmMap.put(connection.getLocalRealm(), createGatewayClient(connection));
                    }
                    break;
                case DELETE:
                    connectionRealmMap.remove(connection.getLocalRealm());
                    client = clientRealmMap.remove(connection.getLocalRealm());
                    if (client != null) {
                        destroyGatewayClient(connection, client);
                    }
                    break;
            }
        }
    }

    protected WebsocketIoClient<String> createGatewayClient(GatewayConnection connection) {

        if (connection.isDisabled()) {
            LOG.info("Disabled gateway client connection so ignoring: " + connection);
            return null;
        }

        LOG.info("Creating gateway IO client: " + connection);

        try {
            WebsocketIoClient<String> client = new WebsocketIoClient<>(
                new URIBuilder()
                    .setScheme(connection.isSecured() ? "wss" : "ws")
                    .setHost(connection.getHost())
                    .setPort(connection.getPort() == null ? -1 : connection.getPort())
                .setPath("websocket/events")
                .setParameter("Auth-Realm", connection.getRealm()).build(),
                null,
                new OAuthClientCredentialsGrant(
                    new URIBuilder()
                        .setScheme(connection.isSecured() ? "https" : "http")
                        .setHost(connection.getHost())
                        .setPort(connection.getPort() == null ? -1 : connection.getPort())
                        .setPath("auth/realms/" + connection.getRealm() + "/protocol/openid-connect/token")
                        .build().toString(),
                    connection.getClientId(),
                    connection.getClientSecret(),
                    null).setBasicAuthHeader(true),
                executorService);

            client.setEncoderDecoderProvider(() ->
                new ChannelHandler[] {new AbstractNettyIoClient.MessageToMessageDecoder<>(String.class, client)}
            );

            client.addConnectionStatusConsumer(
                connectionStatus -> onGatewayClientConnectionStatusChanged(connection, connectionStatus)
            );

            client.addMessageConsumer(message -> onCentralManagerMessage(connection, message));

            // Subscribe to Asset and attribute events of local realm and pass through to connected manager
            clientEventService.getEventSubscriptions().createOrUpdate(
                getClientSessionKey(connection),
                false,
                new EventSubscription<>(
                    AssetEvent.class,
                    new AssetFilter<AssetEvent>().setRealm(connection.getLocalRealm()),
                    triggeredEventSubscription ->
                        triggeredEventSubscription.getEvents()
                            .forEach(event ->
                                sendCentralManagerMessage(connection.getLocalRealm(), messageToString(SharedEvent.MESSAGE_PREFIX, event)))));

            clientEventService.getEventSubscriptions().createOrUpdate(
                getClientSessionKey(connection),
                false,
                new EventSubscription<>(
                    AttributeEvent.class,
                    new AssetFilter<AttributeEvent>().setRealm(connection.getLocalRealm()),
                    triggeredEventSubscription ->
                        triggeredEventSubscription.getEvents()
                            .forEach(event ->
                                sendCentralManagerMessage(connection.getLocalRealm(), messageToString(SharedEvent.MESSAGE_PREFIX, event)))));

            client.connect();
            return client;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Creating gateway IO client failed so marking connection as disabled: " + connection, e);
            connection.setDisabled(true);
            setConnection(connection);
        }

        return null;
    }

    protected void destroyGatewayClient(GatewayConnection connection, WebsocketIoClient<String> client) {
        if (client == null) {
            return;
        }
        LOG.info("Destroying gateway IO client: " + connection);
        try {
            client.disconnect();
            client.removeAllConnectionStatusConsumers();
            client.removeAllMessageConsumers();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "An exception occurred whilst trying to disconnect the gateway IO client", e);
        }

        if (connection != null) {
            clientEventService.getEventSubscriptions().cancelAll(getClientSessionKey(connection));
        }
    }

    protected void onGatewayClientConnectionStatusChanged(GatewayConnection connection, ConnectionStatus connectionStatus) {
        LOG.info("Connection status change for gateway IO client '" + connectionStatus + "': " + connection);
        clientEventService.publishEvent(new GatewayConnectionStatusEvent(timerService.getCurrentTimeMillis(), connection.getLocalRealm(), connectionStatus));
    }

    protected void onCentralManagerMessage(GatewayConnection connection, String message) {
        String messageId = null;
        SharedEvent event = null;

        if (message.startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)) {
            EventRequestResponseWrapper<?> wrapper = messageFromString(
                message,
                EventRequestResponseWrapper.MESSAGE_PREFIX,
                EventRequestResponseWrapper.class);
            messageId = wrapper.getMessageId();
            event = wrapper.getEvent();
        }

        if (message.startsWith(SharedEvent.MESSAGE_PREFIX)) {
            event = messageFromString(message, SharedEvent.MESSAGE_PREFIX, SharedEvent.class);
        }

        if (event != null) {
            if (event instanceof GatewayDisconnectEvent) {
                if (((GatewayDisconnectEvent)event).getReason() == GatewayDisconnectEvent.Reason.PERMANENT_ERROR) {
                    LOG.info("Central manager requested disconnect due to permanent error (likely this version of the edge gateway software is not compatible with that manager version)");
                    destroyGatewayClient(connection, clientRealmMap.get(connection.getLocalRealm()));
                    clientRealmMap.put(connection.getLocalRealm(), null);
                }
            } else if (event instanceof AttributeEvent) {
                assetProcessingService.sendAttributeEvent((AttributeEvent)event, AttributeEvent.Source.INTERNAL);
            } else if (event instanceof AssetEvent) {
                AssetEvent assetEvent = (AssetEvent)event;
                if (assetEvent.getCause() == AssetEvent.Cause.CREATE || assetEvent.getCause() == AssetEvent.Cause.UPDATE) {
                    Asset asset = assetEvent.getAsset();
                    asset.setRealm(connection.getLocalRealm());
                    LOG.fine("Request from central manager to create/update an asset: Realm=" + connection.getLocalRealm() + ", Asset ID=" + asset.getId());
                    try {
                        asset = assetStorageService.merge(asset, true);
                    } catch (Exception e) {
                        LOG.log(Level.INFO, "Request from central manager to create/update an asset failed: Realm=" + connection.getLocalRealm() + ", Asset ID=" + asset.getId(), e);
                    }
                }
            } else if (event instanceof DeleteAssetsRequestEvent) {
                DeleteAssetsRequestEvent deleteRequest = (DeleteAssetsRequestEvent)event;
                LOG.fine("Request from central manager to delete asset(s): Realm=" + connection.getLocalRealm() + ", Asset IDs=" + Arrays.toString(deleteRequest.getAssetIds().toArray()));
                boolean success = false;
                try {
                    success = assetStorageService.delete(deleteRequest.getAssetIds());
                } catch (Exception e) {
                    LOG.log(Level.INFO, "Request from central manager to create/update an asset failed: Realm=" + connection.getLocalRealm() + ", Asset IDs=" + Arrays.toString(deleteRequest.getAssetIds().toArray()), e);
                } finally {
                    sendCentralManagerMessage(
                        connection.getLocalRealm(),
                        messageToString(
                            EventRequestResponseWrapper.MESSAGE_PREFIX,
                            new EventRequestResponseWrapper<>(
                                messageId,
                                new DeleteAssetsResponseEvent(success, deleteRequest.getAssetIds())
                            )
                    ));
                }
            } else if (event instanceof ReadAssetsEvent) {
                ReadAssetsEvent readAssets = (ReadAssetsEvent)event;
                AssetQuery query = readAssets.getAssetQuery();
                // Force realm to be the one that this client is associated with
                query.tenant(new TenantPredicate(connection.getLocalRealm()));
                List<Asset> assets = assetStorageService.findAll(readAssets.getAssetQuery());

                sendCentralManagerMessage(
                    connection.getLocalRealm(),
                    messageToString(
                        EventRequestResponseWrapper.MESSAGE_PREFIX,
                        new EventRequestResponseWrapper<>(
                            messageId,
                            new AssetsEvent(assets)
                        )));
            }
        }
    }

    protected void sendCentralManagerMessage(String realm, String message) {
        WebsocketIoClient<String> client;

        synchronized (clientRealmMap) {
            client = clientRealmMap.get(realm);
        }

        if (client != null) {
            client.sendMessage(message);
        }
    }

    protected String getClientSessionKey(GatewayConnection connection) {
        return CLIENT_EVENT_SESSION_PREFIX + connection.getLocalRealm();
    }

    protected <T> T messageFromString(String message, String prefix, Class<T> clazz) {
        try {
            message = message.substring(prefix.length());
            return Container.JSON.readValue(message, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse message");
        }
    }

    protected String messageToString(String prefix, Object message) {
        try {
            String str = Container.JSON.writeValueAsString(message);
            return prefix + str;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialise message");
        }
    }

    /** GATEWAY RESOURCE METHODS */
    protected List<GatewayConnection> getConnections() {
        return new ArrayList<>(connectionRealmMap.values());
    }

    public void setConnection(GatewayConnection connection) {
        LOG.info("Updating/creating gateway connection: " + connection);
        persistenceService.doTransaction(em -> em.merge(connection));
    }

    protected boolean deleteConnections(List<String> realms) {
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
        GatewayConnection connection = connectionRealmMap.get(realm);

        if (connection == null) {
            return null;
        }

        if (connection.isDisabled()) {
            return ConnectionStatus.DISABLED;
        }

        WebsocketIoClient<String> client = clientRealmMap.get(realm);
        return client != null ? client.getConnectionStatus() : null;
    }
}
