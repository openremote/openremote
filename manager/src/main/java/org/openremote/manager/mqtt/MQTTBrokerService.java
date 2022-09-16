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

import io.moquette.BrokerConstants;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.client.impl.ClientSessionInternal;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil;
import org.apache.activemq.artemis.core.remoting.FailureListener;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerConnectionPlugin;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.jaas.GuestLoginModule;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.openremote.agent.protocol.mqtt.MQTTLastWill;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.AuthorisationService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.manager.security.MultiTenantDirectAccessGrantsLoginModule;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil.MQTT_QOS_LEVEL_KEY;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.syslog.SyslogCategory.API;

public class MQTTBrokerService extends RouteBuilder implements ContainerService, ActiveMQServerConnectionPlugin, FailureListener {

    public static final int PRIORITY = MED_PRIORITY;
    private static final Logger LOG = SyslogCategory.getLogger(API, MQTTBrokerService.class);

    public static final String MQTT_CLIENT_QUEUE = "seda://MqttClientQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    public static final String MQTT_SERVER_LISTEN_HOST = "MQTT_SERVER_LISTEN_HOST";
    public static final String MQTT_SERVER_LISTEN_PORT = "MQTT_SERVER_LISTEN_PORT";

    protected AuthorisationService authorisationService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected MessageBrokerService messageBrokerService;
    protected ScheduledExecutorService executorService;
    protected TimerService timerService;
    protected List<MQTTHandler> customHandlers = new ArrayList<>();

    protected boolean active;
    protected String host;
    protected int port;
    protected EmbeddedActiveMQ server;
    protected ClientSessionInternal internalSession;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        host = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST);
        port = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT);

        authorisationService = container.getService(AuthorisationService.class);
        clientEventService = container.getService(ClientEventService.class);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        executorService = container.getExecutorService();
        timerService = container.getService(TimerService.class);

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

        // Start each custom handler
        for (MQTTHandler handler : customHandlers) {
            try {
                handler.start(container);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "MQTT custom handler threw an exception whilst starting: handler=" + handler.getName(), e);
                throw e;
            }
        }

        // Configure and start the broker
        Configuration config = new ConfigurationImpl();
        config.addAcceptorConfiguration("in-vm", "vm://0");
        String serverURI = new URIBuilder().setScheme("tcp").setHost(host).setPort(port).setParameter("protocols", "MQTT").build().toString();
        config.addAcceptorConfiguration("tcp", serverURI);
        // TODO: Make this configurable
        config.setMaxDiskUsage(-1);
        config.setSecurityInvalidationInterval(30000);
        config.registerBrokerPlugin(this);

        server = new EmbeddedActiveMQ();
        server.setConfiguration(config);
        server.setSecurityManager(new ActiveMQORSecurityManager(authorisationService, this, realm -> identityProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID), "", new SecurityConfiguration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[] {
                    new AppConfigurationEntry(GuestLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, Map.of("debug", "true", "credentialsInvalidate", "true")),
                    new AppConfigurationEntry(MultiTenantDirectAccessGrantsLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, Map.of(MultiTenantDirectAccessGrantsLoginModule.INCLUDE_REALM_ROLES_OPTION, "true"))
                };
            }
        }));
        server.start();

        LOG.fine("Started MQTT broker");

        // Create internal producer for sending messages
        ServerLocator serverLocator = ActiveMQClient.createServerLocator("vm://0");
        ClientSessionFactory factory = serverLocator.createSessionFactory();
        String internalClientID = UniqueIdentifierGenerator.generateId("Internal client");
        internalSession = (ClientSessionInternal) factory.createSession(null, null, false, true, true, serverLocator.isPreAcknowledge(), serverLocator.getAckBatchSize(), internalClientID);
        ServerSession serverSession = server.getActiveMQServer().getSessionByID(internalSession.getName());
        serverSession.disableSecurity();

        executorService.scheduleWithFixedDelay(() -> {
            try {
                ClientProducer producer = internalSession.createProducer("master.client123.asset.1234");
                ClientMessage message = internalSession.createMessage(false);
                message.writeBodyBufferString("AssetEvent{\"id\": \"1234\", \"prop1\": true}");
                producer.send(message);

                producer = internalSession.createProducer("master.client123.asset.5678");
                message = internalSession.createMessage(false);
                message.writeBodyBufferString("AssetEvent{\"id\": \"5678\", \"prop1\": true}");
                producer.send(message);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Something went wrong", e);
            }
        }, 20, 20, TimeUnit.SECONDS);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("UserPersistenceChanges")
            .filter(isPersistenceEventForEntityType(User.class))
            .process(exchange -> {
                PersistenceEvent<User> persistenceEvent = (PersistenceEvent<User>)exchange.getIn().getBody(PersistenceEvent.class);
                User user = persistenceEvent.getEntity();

                if (!user.isServiceAccount()) {
                    return;
                }

                boolean forceDisconnect = persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE;

                if (persistenceEvent.getCause() == PersistenceEvent.Cause.UPDATE) {
                    // Force disconnect if certain properties have changed
                    forceDisconnect = Arrays.stream(persistenceEvent.getPropertyNames()).anyMatch((propertyName) ->
                        (propertyName.equals("enabled") && !user.getEnabled())
                            || propertyName.equals("username"));
                }

                if (forceDisconnect) {
                    LOG.info("User modified or deleted so force closing any sessions for this user: " + user);
                    // Find existing connection for this user
                    forceDisconnectUser(user.getUsername());
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
        // Attach listeners
        connection.addFailureListener(this);

        // Notify handlers
        for (MQTTHandler handler : getCustomHandlers()) {
            handler.onConnect(connection);
        }
    }

    @Override
    public void afterDestroyConnection(RemotingConnection connection) throws ActiveMQException {
        connection.removeFailureListener(this);
        // TODO: Force delete session (don't allow retained/durable sessions)
    }

    @Override
    public void connectionFailed(ActiveMQException exception, boolean failedOver) {
        connectionFailed(exception, failedOver, null);
    }

    @Override
    public void connectionFailed(ActiveMQException exception, boolean failedOver, String scaleDownTargetNodeID) {
        // TODO: Decide if we need this or is afterDestroyConnection enough
    }

    public Iterable<MQTTHandler> getCustomHandlers() {
        return customHandlers;
    }

    public Runnable getForceDisconnectRunnable(RemotingConnection connection) {
        return () -> doForceDisconnect(connection);
    }

    public void forceDisconnectUser(String username) {
        if (TextUtil.isNullOrEmpty(username)) {
            return;
        }
        server.getActiveMQServer().getSessions().forEach(session -> {
            Subject subject = session.getRemotingConnection().getSubject();
            if (username.equals(KeycloakIdentityProvider.getSubjectName(subject))) {
                doForceDisconnect(session.getRemotingConnection());
            }
        });
    }

    protected void doForceDisconnect(RemotingConnection connection) {
        LOG.fine("Force disconnecting session: " + connection);
        connection.disconnect(false);
    }

    public void publishMessage(String topic, Object data, MqttQoS qoS) {
        try {
            if (internalSession != null) {
                ClientProducer producer = internalSession.createProducer(MQTTUtil.convertMqttTopicFilterToCoreAddress(topic, server.getConfiguration().getWildcardConfiguration()));
                ClientMessage message = internalSession.createMessage(false);
                message.putIntProperty(MQTT_QOS_LEVEL_KEY, qoS.value());
                message.writeBodyBufferString(ValueUtil.asJSON(data).orElseThrow(() -> new IllegalStateException("Failed to convert payload to JSON string: " + data)));
                producer.send(message);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Couldn't send AttributeEvent to MQTT client", e);
        }
    }
}
