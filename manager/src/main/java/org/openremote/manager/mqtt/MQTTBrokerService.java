/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.mqtt;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.api.core.management.CoreNotificationType;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;
import org.apache.activemq.artemis.core.client.impl.ClientSessionInternal;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.WildcardConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil;
import org.apache.activemq.artemis.core.remoting.FailureListener;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnection;
import org.apache.activemq.artemis.core.security.impl.SecurityStoreImpl;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerConnectionPlugin;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerSessionPlugin;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.jaas.GuestLoginModule;
import org.apache.activemq.artemis.spi.core.security.jaas.PrincipalConversionLoginModule;
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal;
import org.apache.activemq.artemis.spi.core.security.jaas.UserPrincipal;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.jaas.AbstractKeycloakLoginModule;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.AuthorisationService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.manager.security.MultiTenantClientCredentialsGrantsLoginModule;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.security.User;
import org.openremote.model.util.Debouncer;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.*;
import static java.util.stream.StreamSupport.stream;
import static org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil.MQTT_QOS_LEVEL_KEY;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.syslog.SyslogCategory.API;

public class MQTTBrokerService extends RouteBuilder implements ContainerService, ActiveMQServerConnectionPlugin, ActiveMQServerSessionPlugin {

    public static final String MQTT_FORCE_USER_DISCONNECT_DEBOUNCE_MILLIS = "MQTT_FORCE_USER_DISCONNECT_DEBOUNCE_MILLIS";
    public static int MQTT_FORCE_USER_DISCONNECT_DEBOUNCE_MILLIS_DEFAULT = 5000;
    public static final int PRIORITY = MED_PRIORITY;
    public static final String MQTT_SERVER_LISTEN_HOST = "MQTT_SERVER_LISTEN_HOST";
    public static final String MQTT_SERVER_LISTEN_PORT = "MQTT_SERVER_LISTEN_PORT";
    public static final String ANONYMOUS_USERNAME = "anonymous";
    protected final WildcardConfiguration wildcardConfiguration = new WildcardConfiguration();
    protected static final System.Logger LOG = System.getLogger(MQTTBrokerService.class.getName() + "." + API.name());

    protected AssetStorageService assetStorageService;
    protected AuthorisationService authorisationService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected MessageBrokerService messageBrokerService;
    protected ScheduledExecutorService executorService;
    protected TimerService timerService;
    protected AssetProcessingService assetProcessingService;
    protected List<MQTTHandler> customHandlers = new ArrayList<>();
    protected ConcurrentMap<String, RemotingConnection> clientIDConnectionMap = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, RemotingConnection> connectionIDConnectionMap = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, List<PersistenceEvent<UserAssetLink>>> userAssetLinkChangeMap = new ConcurrentHashMap<>();
    protected Debouncer<String> userAssetDisconnectDebouncer;
    // Stores disconnected connections for a short period to allow last will publishes to be processed
    protected Cache<String, RemotingConnection> disconnectedConnectionCache;
    protected boolean active;
    protected String host;
    protected int port;
    protected EmbeddedActiveMQ server;
    protected ActiveMQORSecurityManager securityManager;
    protected ClientProducer producer;
    protected ClientSessionInternal internalSession;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        host = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, "0.0.0.0");
        port = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, 1883);
        int debounceMillis = getInteger(container.getConfig(), MQTT_FORCE_USER_DISCONNECT_DEBOUNCE_MILLIS, MQTT_FORCE_USER_DISCONNECT_DEBOUNCE_MILLIS_DEFAULT);
        assetStorageService = container.getService(AssetStorageService.class);
        authorisationService = container.getService(AuthorisationService.class);
        clientEventService = container.getService(ClientEventService.class);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        executorService = container.getExecutorService();
        timerService = container.getService(TimerService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);

        userAssetDisconnectDebouncer = new Debouncer<>(executorService, id -> processUserAssetLinkChange(id, userAssetLinkChangeMap.remove(id)), debounceMillis);
        disconnectedConnectionCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10000, TimeUnit.MILLISECONDS)
                .build();

        if (!identityService.isKeycloakEnabled()) {
            LOG.log(WARNING, "MQTT connections are not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
            container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        }

        // Load custom handlers
        this.customHandlers = stream(ServiceLoader.load(MQTTHandler.class).spliterator(), false)
                .sorted(Comparator.comparingInt(MQTTHandler::getPriority))
                .collect(Collectors.toList());

        // Init each custom handler
        for (MQTTHandler handler : customHandlers) {
            try {
                handler.init(container);
            } catch (Exception e) {
                LOG.log(WARNING, "MQTT custom handler threw an exception whilst initialising: handler=" + handler.getName(), e);
                throw e;
            }
        }
    }

    @Override
    public void start(Container container) throws Exception {

        if (!active) {
            return;
        }

        // Configure and start the broker
        Configuration config = new ConfigurationImpl();
        config.addAcceptorConfiguration("in-vm", "vm://0?protocols=core");
        String serverURI = new URIBuilder().setScheme("tcp").setHost(host).setPort(port)
            .setParameter("protocols", "MQTT")
            .setParameter("allowLinkStealing", "true")
            .setParameter("defaultMqttSessionExpiryInterval", "0") // Don't support retained sessions
            .build().toString();
        config.addAcceptorConfiguration("tcp", serverURI);

        config.registerBrokerPlugin(this);
        config.setWildCardConfiguration(wildcardConfiguration);

        // Force all addresses to aggressively cleanup queues (don't support resumable sessions)
        config.addAddressSetting(wildcardConfiguration.getAnyWordsString(),
            new AddressSettings()
                .setDeadLetterAddress(SimpleString.toSimpleString("ActiveMQ.DLQ"))
                .setExpiryAddress(SimpleString.toSimpleString("ActiveMQ.expired"))
                .setAutoDeleteCreatedQueues(true)
                .setAutoDeleteAddresses(true)
                // Auto delete MQTT addresses after 1 day as they never get flagged as used so will linger otherwise
                .setAutoDeleteAddressesSkipUsageCheck(true)
                .setAutoDeleteAddressesDelay(86400000)
                .setAutoDeleteQueuesDelay(0)
                .setAutoDeleteQueuesMessageCount(-1L)
        );

        config.setPersistenceEnabled(false);

        // TODO: Make auto provisioning clients disconnect and reconnect with credentials or pass through X.509 certificates for auth
        // Cannot use authentication or authorisation cache as auto provisioning MQTT clients will authenticate as anonymous and this is then baked into the created ServerSession and cannot be modified
        // so all anonymous sessions will use the same username/password for key lookups in the caches - Can possibly use caching if ActiveMQ makes changes and/or we move to using X.509 TLS with ActiveMQ
        //config.setSecurityInvalidationInterval(600000); // Long cache as we force clear it when needed
        config.setAuthenticationCacheSize(0);
        config.setAuthorizationCacheSize(0);

        server = new EmbeddedActiveMQ();
        server.setConfiguration(config);

        securityManager = new ActiveMQORSecurityManager(authorisationService, this, realm -> identityProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID), "", new SecurityConfiguration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[]{
                    new AppConfigurationEntry(GuestLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, Map.of("debug", "true", "credentialsInvalidate", "true", "org.apache.activemq.jaas.guest.user", ANONYMOUS_USERNAME, "org.apache.activemq.jaas.guest.role", ANONYMOUS_USERNAME)),
                    new AppConfigurationEntry(MultiTenantClientCredentialsGrantsLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, Map.of(
                        MultiTenantClientCredentialsGrantsLoginModule.INCLUDE_REALM_ROLES_OPTION, "true",
                        AbstractKeycloakLoginModule.ROLE_PRINCIPAL_CLASS_OPTION, RolePrincipal.class.getName()
                    )),
                    new AppConfigurationEntry(PrincipalConversionLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, Map.of(PrincipalConversionLoginModule.PRINCIPAL_CLASS_LIST, KeycloakPrincipal.class.getName()))
                };
            }
        });

        server.setSecurityManager(securityManager);
        server.start();
        LOG.log(DEBUG, "Started MQTT broker");

        // Add notification handler for subscribe/unsubscribe and publish events
        server.getActiveMQServer().getManagementService().addNotificationListener(notification -> {
            if (notification.getType() == CoreNotificationType.CONSUMER_CREATED || notification.getType() == CoreNotificationType.CONSUMER_CLOSED) {
                boolean isSubscribe = notification.getType() == CoreNotificationType.CONSUMER_CREATED;
                String sessionId = notification.getProperties().getSimpleStringProperty(ManagementHelper.HDR_SESSION_NAME).toString();
                String topic = notification.getProperties().getSimpleStringProperty(ManagementHelper.HDR_ADDRESS).toString();
                ServerSession session = server.getActiveMQServer().getSessionByID(sessionId);

                // Ignore internal subscriptions
                boolean isInternal = session.getRemotingConnection().getTransportConnection() instanceof InVMConnection;
                if (isInternal) {
                    return;
                }

                if (isSubscribe) {
                    onSubscribe(session.getRemotingConnection(), MQTTUtil.convertCoreAddressToMqttTopicFilter(topic, wildcardConfiguration));
                } else {
                    onUnsubscribe(session.getRemotingConnection(), MQTTUtil.convertCoreAddressToMqttTopicFilter(topic, wildcardConfiguration));
                }
            }
        });

        // Create internal producer for producing messages
        ServerLocator serverLocator = ActiveMQClient.createServerLocator("vm://0");
        ClientSessionFactory factory = serverLocator.createSessionFactory();
        String internalClientID = UniqueIdentifierGenerator.generateId("Internal client");
        internalSession = (ClientSessionInternal) factory.createSession(null, null, false, true, true, true, serverLocator.getAckBatchSize(), internalClientID);
        internalSession.addMetaData(ClientSession.JMS_SESSION_IDENTIFIER_PROPERTY, "Internal session");
        ServerSession serverSession = server.getActiveMQServer().getSessionByID(internalSession.getName());
        serverSession.disableSecurity();
        internalSession.start();

        // Create producer
        producer = internalSession.createProducer();

        // Start each custom handler
        for (MQTTHandler handler : customHandlers) {
            try {
                handler.start(container);
            } catch (Exception e) {
                LOG.log(WARNING, "MQTT custom handler threw an exception whilst starting: handler=" + handler.getName(), e);
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
                .routeId("Persistence-UserAndAssetLink")
                .filter(body().isInstanceOf(PersistenceEvent.class))
                .process(exchange -> {
                    PersistenceEvent<?> persistenceEvent = (PersistenceEvent<?>) exchange.getIn().getBody(PersistenceEvent.class);

                    if (persistenceEvent.getEntity() instanceof User user) {

                        if (!user.isServiceAccount()) {
                            return;
                        }

                        boolean forceDisconnect = persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE;

                        if (persistenceEvent.getCause() == PersistenceEvent.Cause.UPDATE) {
                            // Force disconnect if certain properties have changed
                            forceDisconnect = persistenceEvent.hasPropertyChanged("enabled")
                                || persistenceEvent.hasPropertyChanged("username")
                                || persistenceEvent.hasPropertyChanged("secret");
                        }

                        if (forceDisconnect) {
                            LOG.log(TRACE, "User modified or deleted so force closing any sessions for this user: " + user);
                            // Find existing connection for this user
                            getUserConnections(user.getId()).forEach(this::doForceDisconnect);
                        }

                    } else if (persistenceEvent.getEntity() instanceof UserAssetLink userAssetLink) {
                        String userID = userAssetLink.getId().getUserId();
                        // Debounce force disconnect check of this user's sessions as there could be many asset links changing
                        List<PersistenceEvent<UserAssetLink>> changedUserAssetLinks = userAssetLinkChangeMap.computeIfAbsent(userID, id -> Collections.synchronizedList(new ArrayList<>()));
                        changedUserAssetLinks.add((PersistenceEvent<UserAssetLink>) persistenceEvent);
                        userAssetDisconnectDebouncer.call(userID);
                    }
                });
    }

    @Override
    public void stop(Container container) throws Exception {

        userAssetDisconnectDebouncer.cancelAll(true);

        server.stop();
        LOG.log(DEBUG, "Stopped MQTT broker");

        stream(ServiceLoader.load(MQTTHandler.class).spliterator(), false)
                .sorted(Comparator.comparingInt(MQTTHandler::getPriority).reversed())
                .forEach(handler -> {
                    try {
                        handler.stop();
                    } catch (Exception e) {
                        LOG.log(WARNING, "MQTT custom handler threw an exception whilst stopping: handler=" + handler.getName(), e);
                    }
                });
    }

    @Override
    public void afterCreateConnection(RemotingConnection connection) throws ActiveMQException {

        // MQTT seems to only use failure callback even if closed gracefully (need to look at the exception for details)
        connection.addFailureListener(new FailureListener() {
            @Override
            public void connectionFailed(ActiveMQException exception, boolean failedOver) {
                connectionFailed(exception, failedOver, null);
            }

            @Override
            public void connectionFailed(ActiveMQException exception, boolean failedOver, String scaleDownTargetNodeID) {
                // TODO: Force delete session (don't allow retained/durable sessions)

                connectionIDConnectionMap.remove(getConnectionIDString(connection));

                if (connection.getClientID() != null) {
                    RemotingConnection remotingConnection = clientIDConnectionMap.remove(connection.getClientID());
                    if (remotingConnection != null) {
                        disconnectedConnectionCache.put(connection.getClientID(), remotingConnection);
                    }
                }

                if (exception.getType() == ActiveMQExceptionType.REMOTE_DISCONNECT) {// Seems to be the type for graceful close of connection
                    // Notify handlers of connection close
                    LOG.log(DEBUG, () -> "Client disconnected: " + connectionToString(connection));
                    for (MQTTHandler handler : getCustomHandlers()) {
                        handler.onDisconnect(connection);
                    }
                } else {
                    // Notify handlers of connection failure
                    LOG.log(DEBUG, () -> "Client disconnected (failure=" + exception.getMessage() + "): " + connectionToString(connection));
                    for (MQTTHandler handler : getCustomHandlers()) {
                        handler.onConnectionLost(connection);
                    }
                }
            }
        });
    }

    // We do this here as connection plugin afterCreateConnection callback fires before client ID and subject are populated
    // For each connection multiple sessions are created but we only want to call custom handlers onConnect once
    @Override
    public void afterCreateSession(ServerSession session) throws ActiveMQException {
        RemotingConnection remotingConnection = session.getRemotingConnection();

        // Ignore internal connections or ones without a client ID
        if (remotingConnection == null || remotingConnection.getClientID() == null || remotingConnection.getTransportConnection() instanceof InVMConnection) {
            return;
        }

        String connectionID = getConnectionIDString(remotingConnection);
        clientIDConnectionMap.put(remotingConnection.getClientID(), remotingConnection);

        if (!connectionIDConnectionMap.containsKey(connectionID)) {
            LOG.log(DEBUG, () -> "Client connected: " + connectionToString(remotingConnection));
            connectionIDConnectionMap.put(connectionID, remotingConnection);
            for (MQTTHandler handler : getCustomHandlers()) {
                handler.onConnect(remotingConnection);
            }
        }
    }

    @Override
    public void afterDestroyConnection(RemotingConnection connection) throws ActiveMQException {
    }

    public void onSubscribe(RemotingConnection connection, String topicStr) {
        Topic topic = Topic.parse(topicStr);

        for (MQTTHandler handler : getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                String connectionStr = LOG.isLoggable(DEBUG) ? connectionToString(connection) : null;
                LOG.log(DEBUG, "Client subscribed '" + topicStr + "': " + connectionStr);
                handler.onSubscribe(connection, topic);
                break;
            }
        }
    }

    public void onUnsubscribe(RemotingConnection connection, String topicStr) {
        Topic topic = Topic.parse(topicStr);

        for (MQTTHandler handler : getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                String connectionStr = LOG.isLoggable(DEBUG) ? connectionToString(connection) : null;
                LOG.log(DEBUG, "Client unsubscribed '" + topicStr + "': " + connectionStr);
                handler.onUnsubscribe(connection, topic);
                break;
            }
        }
    }

    public Iterable<MQTTHandler> getCustomHandlers() {
        return customHandlers;
    }

    public void processUserAssetLinkChange(String userID, List<PersistenceEvent<UserAssetLink>> changes) {
        if (TextUtil.isNullOrEmpty(userID)) {
            return;
        }

        // Check if user has any active connections
        Set<RemotingConnection> userConnections = getUserConnections(userID);
        Subject subject = userConnections.stream().filter(connection -> connection.getSubject() != null).findFirst().map(RemotingConnection::getSubject).orElse(null);

        // Only notify handlers if subject is a restricted user
        if (subject != null && KeycloakIdentityProvider.getSecurityContext(subject).getToken().getRealmAccess().isUserInRole(Constants.RESTRICTED_USER_REALM_ROLE)) {
            LOG.log(TRACE, "User asset links modified for connected restricted user so passing to handlers to decide what to do: user=" + subject);
            // Pass to handlers to decide what to do
            userConnections.forEach(connection -> {
                for (MQTTHandler handler : customHandlers) {
                    connection.setSubject(subject);
                    handler.onUserAssetLinksChanged(connection, changes);
                }
            });
        }
    }

    /**
     * Get active connections for the specified user ID
     */
    public Set<RemotingConnection> getUserConnections(String userID) {
        if (TextUtil.isNullOrEmpty(userID)) {
            return Collections.emptySet();
        }

        return server.getActiveMQServer().getRemotingService().getConnections().stream().filter(connection -> {
            Subject subject = connection.getSubject();
            String subjectID = KeycloakIdentityProvider.getSubjectId(subject);
            return userID.equals(subjectID);
        }).collect(Collectors.toSet());
    }

    protected void doForceDisconnect(RemotingConnection connection) {
        LOG.log(DEBUG, "Force disconnecting client connection: " + connectionToString(connection));
        connection.disconnect(false);
        ((SecurityStoreImpl)server.getActiveMQServer().getSecurityStore()).invalidateAuthorizationCache();
    }

    public boolean disconnectSession(String sessionID) {
        RemotingConnection connection = connectionIDConnectionMap.get(sessionID);
        if (connection != null) {
            LOG.log(DEBUG, "Force disconnecting client connection: " + connectionToString(connection));
            doForceDisconnect(connection);
            return true;
        }

        return false;
    }

    public void publishMessage(String topic, Object data, MqttQoS qoS) {
        try {
            if (internalSession != null) {
                // Artemis' sessions are not threadsafe
                synchronized (internalSession) {
                    ClientMessage message = internalSession.createMessage(false);
                    message.putIntProperty(MQTT_QOS_LEVEL_KEY, qoS.value());
                    message.writeBodyBufferBytes(ValueUtil.asJSON(data).map(String::getBytes).orElseThrow(() -> new IllegalStateException("Failed to convert payload to JSON string: " + data)));
                    producer.send(MQTTUtil.convertMqttTopicFilterToCoreAddress(topic, server.getConfiguration().getWildcardConfiguration()), message);
                }
            }
        } catch (Exception e) {
            LOG.log(WARNING, "Couldn't publish to MQTT client: topic=" + topic, e);
        }
    }

    public WildcardConfiguration getWildcardConfiguration() {
        return wildcardConfiguration;
    }

    public static String getConnectionIDString(RemotingConnection connection) {
        if (connection == null) {
            return null;
        }

        Object ID = connection.getID();
        return ID instanceof ChannelId ? ((ChannelId) ID).asLongText() : ID.toString();
    }

    public static String connectionToString(RemotingConnection connection) {
        if (connection == null) {
            return "";
        }

        String username = null;
        Subject subject = connection.getSubject();

        if (subject != null) {
            username = getSubjectName(subject);
        }

        return "connection=" + connection.getRemoteAddress() + ", clientID=" + connection.getClientID() + ", subject=" + username;
    }

    public static String getSubjectName(Subject subject) {
        return subject.getPrincipals().stream().filter(principal -> principal instanceof UserPrincipal)
            .findFirst()
            .map(Principal::getName)
            .orElse(KeycloakIdentityProvider.getSubjectNameAndRealm(subject));
    }

    public RemotingConnection getConnectionFromClientID(String clientID) {
        if (TextUtil.isNullOrEmpty(clientID)) {
            return null;
        }

        // This logic is needed because the connection clientID isn't populated when afterCreateConnection is called
        RemotingConnection connection = clientIDConnectionMap.get(clientID);

        if (connection == null) {
            // Try and find the client ID from the sessions
            for (RemotingConnection remotingConnection : server.getActiveMQServer().getRemotingService().getConnections()) {
                if (Objects.equals(clientID, remotingConnection.getClientID())) {
                    connection = remotingConnection;
                    clientIDConnectionMap.put(clientID, connection);
                    break;
                }
            }
        }

        if (connection == null) {
            // Look in the recently disconnected cache
            connection = disconnectedConnectionCache.getIfPresent(clientID);
        }

        return connection;
    }

    public void notifyConnectionAuthenticated(RemotingConnection connection) {
        if (connection.getSubject() != null) {
            // Notify handlers that connection authenticated
            LOG.log(DEBUG, "Client connection authenticated: " + connectionToString(connection));
            for (MQTTHandler handler : getCustomHandlers()) {
                handler.onConnectionAuthenticated(connection);
            }
        }
    }
}
