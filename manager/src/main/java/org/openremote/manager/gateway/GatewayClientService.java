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
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.auth.OAuthClientCredentialsGrant;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.websocket.WebsocketIOClient;
import org.openremote.model.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.EventRequestResponseWrapper;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.gateway.GatewayConnectionStatusEvent;
import org.openremote.model.gateway.GatewayDisconnectEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
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
    protected ScheduledExecutorService executorService;
    protected ManagerIdentityService identityService;
    protected final Map<String, GatewayConnection> connectionIdMap = new HashMap<>();
    protected final Map<String, WebsocketIOClient<String>> clientIdMap = new HashMap<>();

    @Override
    public void init(Container container) throws Exception {
        executorService = container.getExecutorService();
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        persistenceService = container.getService(PersistenceService.class);
        clientEventService = container.getService(ClientEventService.class);
        timerService = container.getService(TimerService.class);
        identityService = container.getService(ManagerIdentityService.class);

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

        // Get existing connections
        connectionIdMap.putAll(persistenceService.doReturningTransaction(entityManager ->
            entityManager
                .createQuery("select gc from GatewayConnection gc", GatewayConnection.class)
                .getResultList()).stream().collect(Collectors.toMap(GatewayConnection::getId, gc -> gc)));

        // Create clients for enabled connections
        connectionIdMap.forEach((id, connection) -> {
            if (!connection.isDisabled()) {
                clientIdMap.put(id, createGatewayClient(connection));
            }
        });
    }

    @Override
    public void stop(Container container) throws Exception {
        clientIdMap.forEach((id, client) -> {
            if (client != null) {
                destroyGatewayClient(connectionIdMap.get(id), client);
            }
        });
        clientIdMap.clear();
        connectionIdMap.clear();
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

        synchronized (clientIdMap) {
            switch (cause) {

                case UPDATE:
                    WebsocketIOClient<String> client = clientIdMap.remove(connection.getId());
                    if (client != null) {
                        destroyGatewayClient(connection, client);
                    }
                case CREATE:
                    connectionIdMap.put(connection.getId(), connection);
                    if (!connection.isDisabled()) {
                        clientIdMap.put(connection.getId(), createGatewayClient(connection));
                    }
                    break;
                case DELETE:
                    connectionIdMap.remove(connection.getId());
                    client = clientIdMap.remove(connection.getId());
                    if (client != null) {
                        destroyGatewayClient(connection, client);
                    }
                    break;
            }
        }
    }

    protected WebsocketIOClient<String> createGatewayClient(GatewayConnection connection) {

        if (connection.isDisabled()) {
            LOG.info("Disabled gateway client connection so ignoring: " + connection);
            return null;
        }

        LOG.info("Creating gateway IO client: " + connection);

        try {
            WebsocketIOClient<String> client = new WebsocketIOClient<>(
                new URIBuilder()
                    .setScheme(connection.isSecured() ? "wss" : "ws")
                    .setHost(connection.getHost())
                    .setPort(connection.getPort() == null ? -1 : connection.getPort())
                .setPath("websocket/events")
                .setParameter(Constants.REALM_PARAM_NAME, connection.getRealm()).build(),
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
                    null).setBasicAuthHeader(true)
            );

            client.setEncoderDecoderProvider(() ->
                new ChannelHandler[] {new AbstractNettyIOClient.MessageToMessageDecoder<>(String.class, client)}
            );

            client.addConnectionStatusConsumer(
                connectionStatus -> onGatewayClientConnectionStatusChanged(connection, connectionStatus)
            );

            client.addMessageConsumer(message -> onCentralManagerMessage(connection, message));

            // Subscribe to Asset<?> and attribute events of local realm and pass through to connected manager
            clientEventService.addInternalSubscription(
                getClientSessionKey(connection)+"Asset",
                AssetEvent.class,
                new AssetFilter<AssetEvent>().setRealm(connection.getLocalRealm()),
                assetEvent ->
                    sendCentralManagerMessage(connection.getId(), messageToString(SharedEvent.MESSAGE_PREFIX, assetEvent)));

            clientEventService.addInternalSubscription(
                getClientSessionKey(connection)+"Attribute",
                AttributeEvent.class,
                new AssetFilter<AttributeEvent>().setRealm(connection.getLocalRealm()),
                attributeEvent ->
                    sendCentralManagerMessage(connection.getId(), messageToString(SharedEvent.MESSAGE_PREFIX, attributeEvent)));

            client.connect();
            return client;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Creating gateway IO client failed so marking connection as disabled: " + connection, e);
            connection.setDisabled(true);
            setConnection(connection);
        }

        return null;
    }

    protected void destroyGatewayClient(GatewayConnection connection, WebsocketIOClient<String> client) {
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
            clientEventService.cancelInternalSubscription(getClientSessionKey(connection)+"Asset");
            clientEventService.cancelInternalSubscription(getClientSessionKey(connection)+"Attribute");
        }
    }

    protected void onGatewayClientConnectionStatusChanged(GatewayConnection connection, ConnectionStatus connectionStatus) {
        LOG.info("Connection status change for gateway IO client '" + connectionStatus + "': " + connection);
        clientEventService.publishEvent(new GatewayConnectionStatusEvent(timerService.getCurrentTimeMillis(), connection.getId(), connectionStatus));
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
                    destroyGatewayClient(connection, clientIdMap.get(connection.getId()));
                    clientIdMap.put(connection.getId(), null);
                }
            } else if (event instanceof AttributeEvent) {
                assetProcessingService.sendAttributeEvent((AttributeEvent)event, AttributeEvent.Source.INTERNAL);
            } else if (event instanceof AssetEvent) {
                AssetEvent assetEvent = (AssetEvent)event;
                if (assetEvent.getCause() == AssetEvent.Cause.CREATE || assetEvent.getCause() == AssetEvent.Cause.UPDATE) {
                    Asset asset = assetEvent.getAsset();
                    asset.setRealm(connection.getLocalRealm());
                    LOG.finer("Request from central manager to create/update an asset: ID=" + connection.getId() + ", Realm=" + connection.getLocalRealm() + ", Asset<?> ID=" + asset.getId());
                    try {
                        asset = assetStorageService.merge(asset, true);
                    } catch (Exception e) {
                        LOG.log(Level.INFO, "Request from central manager to create/update an asset failed: ID=" + connection.getId() + ", Realm=" + connection.getLocalRealm() + ", Asset<?> ID=" + asset.getId(), e);
                    }
                }
            } else if (event instanceof DeleteAssetsRequestEvent) {
                DeleteAssetsRequestEvent deleteRequest = (DeleteAssetsRequestEvent)event;
                LOG.finer("Request from central manager to delete asset(s): ID=" + connection.getId() + ", Realm=" + connection.getLocalRealm() + ", Asset<?> IDs=" + Arrays.toString(deleteRequest.getAssetIds().toArray()));
                boolean success = false;
                try {
                    success = assetStorageService.delete(deleteRequest.getAssetIds());
                } catch (Exception e) {
                    LOG.log(Level.INFO, "Request from central manager to create/update an asset failed: ID=" + connection.getId() + ", Realm=" + connection.getLocalRealm() + ", Asset<?> IDs=" + Arrays.toString(deleteRequest.getAssetIds().toArray()), e);
                } finally {
                    sendCentralManagerMessage(
                        connection.getId(),
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
                query.realm(new RealmPredicate(connection.getLocalRealm()));
                List<Asset<?>> assets = assetStorageService.findAll(readAssets.getAssetQuery());

                sendCentralManagerMessage(
                    connection.getId(),
                    messageToString(
                        EventRequestResponseWrapper.MESSAGE_PREFIX,
                        new EventRequestResponseWrapper<>(
                            messageId,
                            new AssetsEvent(assets)
                        )));
            }
        }
    }

    protected void sendCentralManagerMessage(String id, String message) {
        WebsocketIOClient<String> client;

        synchronized (clientIdMap) {
            client = clientIdMap.get(id);
        }

        if (client != null) {
            client.sendMessage(message);
        }
    }

    protected String getClientSessionKey(GatewayConnection connection) {
        return CLIENT_EVENT_SESSION_PREFIX + connection.getId();
    }

    protected <T> T messageFromString(String message, String prefix, Class<T> clazz) {
        message = message.substring(prefix.length());
        return ValueUtil.parse(message, clazz).orElse(null);
    }

    protected String messageToString(String prefix, Object message) {
        String str = ValueUtil.asJSON(message).orElse("null");
        return prefix + str;
    }

    /** GATEWAY RESOURCE METHODS */
    protected List<GatewayConnection> getConnections() {
        return new ArrayList<>(connectionIdMap.values());
    }

    public void setConnection(GatewayConnection connection) {
        LOG.info("Updating/creating gateway connection: " + connection);
        persistenceService.doTransaction(em -> em.merge(connection));
    }

    public boolean deleteConnection(String realm, String id) {
        LOG.info("Deleting gateway connection for the following realm: " + realm);

        try {
            persistenceService.doTransaction(em -> {

                List<GatewayConnection> connections = em
                    .createQuery("select gc from GatewayConnection gc where gc.id=:id AND gc.localRealm=:realm", GatewayConnection.class)
                    .setParameter("id", id)
                    .setParameter("realm", realm)
                    .getResultList();

                connections.forEach(em::remove);
            });
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean deleteConnections(List<String> realms) {
        LOG.info("Deleting gateway connections for the following realm(s): " + Arrays.toString(realms.toArray()));

        try {
            persistenceService.doTransaction(em -> {

                List<GatewayConnection> connections = em
                    .createQuery("select gc from GatewayConnection gc where gc.localRealm in :realms", GatewayConnection.class)
                    .setParameter("realms", realms)
                    .getResultList();

                connections.forEach(em::remove);
            });
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    protected ConnectionStatus getConnectionStatus(String realm, String id) {
        GatewayConnection connection = connectionIdMap.get(id);

        if (connection == null) {
            return null;
        }

        if (connection.getLocalRealm().equals(realm) == false) {
            return null;
        }

        if (connection.isDisabled()) {
            return ConnectionStatus.DISABLED;
        }

        WebsocketIOClient<String> client = clientIdMap.get(id);
        return client != null ? client.getConnectionStatus() : null;
    }
}
