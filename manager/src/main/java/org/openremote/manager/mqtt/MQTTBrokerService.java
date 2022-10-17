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

import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
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
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.jaas.GuestLoginModule;
import org.apache.activemq.artemis.spi.core.security.jaas.PrincipalConversionLoginModule;
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.jaas.AbstractKeycloakLoginModule;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.AuthorisationService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.manager.security.MultiTenantClientCredentialsGrantsLoginModule;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Debouncer;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil.MQTT_QOS_LEVEL_KEY;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.syslog.SyslogCategory.API;

public class MQTTBrokerService extends RouteBuilder implements ContainerService, ActiveMQServerConnectionPlugin, ActiveMQServerSessionPlugin {

    public static final int PRIORITY = MED_PRIORITY;
    public static final String MQTT_CLIENT_QUEUE = "seda://MqttClientQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    public static final String MQTT_SERVER_LISTEN_HOST = "MQTT_SERVER_LISTEN_HOST";
    public static final String MQTT_SERVER_LISTEN_PORT = "MQTT_SERVER_LISTEN_PORT";
    protected final WildcardConfiguration wildcardConfiguration = new WildcardConfiguration();
    protected static final Logger LOG = SyslogCategory.getLogger(API, MQTTBrokerService.class);

    protected AuthorisationService authorisationService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected MessageBrokerService messageBrokerService;
    protected ScheduledExecutorService executorService;
    protected TimerService timerService;
    protected List<MQTTHandler> customHandlers = new ArrayList<>();
    protected ConcurrentMap<String, RemotingConnection> clientIDConnectionMap = new ConcurrentHashMap<>();
    // TODO: Make auto provisioning clients disconnect and reconnect with credentials
    // Temp to prevent breaking existing auto provisioning API
    protected ConcurrentMap<Object, Triple<String, String, String>> transientCredentials = new ConcurrentHashMap<>();
    protected Debouncer<String> userAssetDisconnectDebouncer;

    protected boolean active;
    protected String host;
    protected int port;
    protected EmbeddedActiveMQ server;
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

        authorisationService = container.getService(AuthorisationService.class);
        clientEventService = container.getService(ClientEventService.class);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        executorService = container.getExecutorService();
        timerService = container.getService(TimerService.class);
        userAssetDisconnectDebouncer = new Debouncer<>(executorService, this::forceDisconnectUser, 10000);

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
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

        // Load custom handlers
        this.customHandlers = stream(ServiceLoader.load(MQTTHandler.class).spliterator(), false)
            .sorted(Comparator.comparingInt(MQTTHandler::getPriority))
            .collect(Collectors.toList());

        // Configure and start the broker
        Configuration config = new ConfigurationImpl();
        config.addAcceptorConfiguration("in-vm", "vm://0?protocols=core");
        String serverURI = new URIBuilder().setScheme("tcp").setHost(host).setPort(port).setParameter("protocols", "MQTT").build().toString();
        config.addAcceptorConfiguration("tcp", serverURI);

        // TODO: Make this configurable
        config.setMaxDiskUsage(-1);
        config.setSecurityInvalidationInterval(30000);
        config.registerBrokerPlugin(this);

        // Register notification plugin
        config.setWildCardConfiguration(wildcardConfiguration);
        config.setPersistenceEnabled(false);

        server = new EmbeddedActiveMQ();
        server.setConfiguration(config);
        server.setSecurityManager(new ActiveMQORSecurityManager(authorisationService, this, realm -> identityProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID), "", new SecurityConfiguration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[] {
                    new AppConfigurationEntry(GuestLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, Map.of("debug", "true", "credentialsInvalidate", "true")),
                    new AppConfigurationEntry(MultiTenantClientCredentialsGrantsLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, Map.of(
                        MultiTenantClientCredentialsGrantsLoginModule.INCLUDE_REALM_ROLES_OPTION, "true",
                        AbstractKeycloakLoginModule.ROLE_PRINCIPAL_CLASS_OPTION, RolePrincipal.class.getName()
                    )),
                    new AppConfigurationEntry(PrincipalConversionLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, Map.of(PrincipalConversionLoginModule.PRINCIPAL_CLASS_LIST, KeycloakPrincipal.class.getName()))
                };
            }
        }));

        server.start();

        LOG.fine("Started MQTT broker");


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

        // Create internal producer for producing and consuming messages
        ServerLocator serverLocator = ActiveMQClient.createServerLocator("vm://0");
        ClientSessionFactory factory = serverLocator.createSessionFactory();
        String internalClientID = UniqueIdentifierGenerator.generateId("Internal client");
        internalSession = (ClientSessionInternal) factory.createSession(null, null, false, true, true, serverLocator.isPreAcknowledge(), serverLocator.getAckBatchSize(), internalClientID);
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
                LOG.log(Level.WARNING, "MQTT custom handler threw an exception whilst starting: handler=" + handler.getName(), e);
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("UserPersistenceChanges")
            .filter(body().isInstanceOf(PersistenceEvent.class))
            .process(exchange -> {
                PersistenceEvent<?> persistenceEvent = (PersistenceEvent<?>)exchange.getIn().getBody(PersistenceEvent.class);

                if (persistenceEvent.getEntity() instanceof User user) {

                    if (!user.isServiceAccount()) {
                        return;
                    }

                    boolean forceDisconnect = persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE;

                    if (persistenceEvent.getCause() == PersistenceEvent.Cause.UPDATE) {
                        // Force disconnect if certain properties have changed
                        forceDisconnect = Arrays.stream(persistenceEvent.getPropertyNames()).anyMatch((propertyName) ->
                            (propertyName.equals("enabled") && !user.getEnabled())
                                || propertyName.equals("username")
                                || propertyName.equals("secret"));
                    }

                    if (forceDisconnect) {
                        LOG.fine("User modified or deleted so force closing any sessions for this user: " + user);
                        // Find existing connection for this user
                        forceDisconnectUser(user.getId());
                    }

                } else if (persistenceEvent.getEntity() instanceof UserAssetLink userAssetLink) {
                    String userID = userAssetLink.getId().getUserId();

                    // Debounce force disconnect of this user's sessions
                    userAssetDisconnectDebouncer.call(userID);
                }
            });
    }

    @Override
    public void stop(Container container) throws Exception {
        server.stop();
        LOG.fine("Stopped MQTT broker");

        stream(ServiceLoader.load(MQTTHandler.class).spliterator(), false)
            .sorted(Comparator.comparingInt(MQTTHandler::getPriority).reversed())
            .forEach(handler -> {
                try {
                    handler.stop();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "MQTT custom handler threw an exception whilst stopping: handler=" + handler.getName(), e);
                }
            });
    }

    @Override
    public void afterCreateConnection(RemotingConnection connection) throws ActiveMQException {

        LOG.fine("New client connection: "+ connectionToString(connection));

        // MQTT seems to only use failure callback even if closed gracefully (need to look at the exception for details)
        connection.addFailureListener(new FailureListener() {
            @Override
            public void connectionFailed(ActiveMQException exception, boolean failedOver) {
                connectionFailed(exception, failedOver, null);
            }

            @Override
            public void connectionFailed(ActiveMQException exception, boolean failedOver, String scaleDownTargetNodeID) {
                // TODO: Force delete session (don't allow retained/durable sessions)

                if (exception.getType() == ActiveMQExceptionType.REMOTE_DISCONNECT) {// Seems to be the type for graceful close of connection
                    // Notify handlers of connection close
                    for (MQTTHandler handler : getCustomHandlers()) {
                        handler.onDisconnect(connection);
                    }
                } else {
                    // Notify handlers of connection failure
                    for (MQTTHandler handler : getCustomHandlers()) {
                        handler.onConnectionLost(connection);
                    }
                }

                if (connection.getClientID() != null) {
                    clientIDConnectionMap.remove(connection.getClientID());
                }
            }
        });
    }

    @Override
    public void afterCreateSession(ServerSession session) throws ActiveMQException {
        RemotingConnection remotingConnection = session.getRemotingConnection();

        if (remotingConnection == null || remotingConnection.getClientID() == null) {
            return;
        }

        if (!clientIDConnectionMap.containsKey(remotingConnection.getClientID())) {
            clientIDConnectionMap.put(remotingConnection.getClientID(), remotingConnection);

            // We do this here as connection plugin afterCreateConnection callback fires before client ID and subject are populated
            for (MQTTHandler handler : getCustomHandlers()) {
                handler.onConnect(remotingConnection);
            }
        }
    }

    @Override
    public void afterDestroyConnection(RemotingConnection connection) throws ActiveMQException {
        LOG.fine("Destroyed client connection: " + connectionToString(connection));
    }

    public void onSubscribe(RemotingConnection connection, String topicStr) {
        Topic topic = Topic.parse(topicStr);

        for (MQTTHandler handler : getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                LOG.fine("Handler has handled subscribe: handler=" + handler.getName() + ", topic=" + topic + ", " + connectionToString(connection));
                handler.onSubscribe(connection, topic);
                break;
            }
        }
    }

    public void onUnsubscribe(RemotingConnection connection, String topicStr) {
        Topic topic = Topic.parse(topicStr);

        for (MQTTHandler handler : getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                LOG.fine("Handler has handled unsubscribe: handler=" + handler.getName() + ", topic=" + topic + ", " + connectionToString(connection));
                handler.onUnsubscribe(connection, topic);
                break;
            }
        }
    }

    public Iterable<MQTTHandler> getCustomHandlers() {
        return customHandlers;
    }

    public Runnable getForceDisconnectRunnable(RemotingConnection connection) {
        return () -> doForceDisconnect(connection);
    }

    public void forceDisconnectUser(String userID) {
        if (TextUtil.isNullOrEmpty(userID)) {
            return;
        }
        getUserConnections(userID).forEach(this::doForceDisconnect);
    }

    public Set<RemotingConnection> getUserConnections(String userID) {
        if (TextUtil.isNullOrEmpty(userID)) {
            return Collections.emptySet();
        }

        return server.getActiveMQServer().getRemotingService().getConnections().stream().filter(connection -> {
            Subject subject = connection.getSubject();
            String subjectID = KeycloakIdentityProvider.getSubjectId(subject);

            if (subjectID == null) {
                // Could be an auto provisioning client
                Triple<String, String, String> userIDNameAndPassword = transientCredentials.get(connection.getID());
                return userIDNameAndPassword != null && Objects.equals(userID, userIDNameAndPassword.getLeft());
            } else {
                return userID.equals(subjectID);
            }
        }).collect(Collectors.toSet());
    }

    protected void doForceDisconnect(RemotingConnection connection) {
        LOG.fine("Force disconnecting client connection: " + connectionToString(connection));
        connection.disconnect(false);
    }

    public void publishMessage(String topic, Object data, MqttQoS qoS) {
        try {
            if (internalSession != null) {
                ClientMessage message = internalSession.createMessage(false);
                message.putIntProperty(MQTT_QOS_LEVEL_KEY, qoS.value());
                message.writeBodyBufferBytes(ValueUtil.asJSON(data).map(String::getBytes).orElseThrow(() -> new IllegalStateException("Failed to convert payload to JSON string: " + data)));
                producer.send(MQTTUtil.convertMqttTopicFilterToCoreAddress(topic, server.getConfiguration().getWildcardConfiguration()), message);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Couldn't send AttributeEvent to MQTT client", e);
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
        return ID instanceof ChannelId ? ((ChannelId)ID).asLongText() : ID.toString();
    }

    public static String connectionToString(RemotingConnection connection) {
        return "connection=" + connection.getRemoteAddress() + ", subject=" + connection.getSubject();
    }

    public RemotingConnection getConnectionFromClientID(String clientID) {
        if (TextUtil.isNullOrEmpty(clientID)) {
            return null;
        }

        // This logic is needed because the connection clientID isn't populated when afterCreateConnection is called
        RemotingConnection connection = clientIDConnectionMap.get(clientID);

        if (connection == null) {
            // Try and find the client ID from the sessions
            for (ServerSession serverSession : server.getActiveMQServer().getSessions()) {
                if (serverSession.getRemotingConnection() != null && Objects.equals(clientID, serverSession.getRemotingConnection().getClientID())) {
                    connection = serverSession.getRemotingConnection();
                    clientIDConnectionMap.put(clientID, connection);
                    break;
                }
            }
        }

        return connection;
    }

    // TODO: Remove this
    public void addTransientCredentials(RemotingConnection connection, Triple<String, String, String> userIDNameAndPassword) {
        LOG.fine("Adding transient user credentials: connection ID=" + connection.getID() + " ,username=" + userIDNameAndPassword.getMiddle());
        transientCredentials.put(connection.getID(), userIDNameAndPassword);
        ((SecurityStoreImpl)server.getActiveMQServer().getSecurityStore()).invalidateAuthenticationCache();
    }

    // TODO: Remove this
    public void removeTransientCredentials(RemotingConnection connection) {
        LOG.fine("Removing transient user credentials: connection ID=" + connection.getID());
        transientCredentials.remove(connection.getID());
        ((SecurityStoreImpl)server.getActiveMQServer().getSecurityStore()).invalidateAuthenticationCache();
        ((SecurityStoreImpl)server.getActiveMQServer().getSecurityStore()).invalidateAuthorizationCache();
    }
}
