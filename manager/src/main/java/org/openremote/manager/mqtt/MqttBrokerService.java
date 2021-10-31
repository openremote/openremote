/*
 * Copyright 2017, OpenRemote Inc.
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
import io.moquette.broker.Server;
import io.moquette.broker.SessionRegistry;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.security.IAuthenticator;
import io.moquette.interception.InterceptHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.syslog.SyslogCategory.API;

public class MqttBrokerService implements ContainerService, IAuthenticator {

    public static final int PRIORITY = MED_PRIORITY;
    public static final String INTERNAL_CLIENT_ID = "ManagerInternal";
    private static final Logger LOG = SyslogCategory.getLogger(API, MqttBrokerService.class);

    public static final String MQTT_CLIENT_QUEUE = "seda://MqttClientQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    public static final String MQTT_SERVER_LISTEN_HOST = "MQTT_SERVER_LISTEN_HOST";
    public static final String MQTT_SERVER_LISTEN_PORT = "MQTT_SERVER_LISTEN_PORT";

    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected MessageBrokerService messageBrokerService;
    protected ScheduledExecutorService executorService;
    protected final Map<String, MqttConnection> clientIdConnectionMap = new HashMap<>();
    protected List<MQTTHandler> customHandlers = new ArrayList<>();

    protected boolean active;
    protected String host;
    protected int port;
    protected Server mqttBroker;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        host = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST);
        port = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT);

        clientEventService = container.getService(ClientEventService.class);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        executorService = container.getExecutorService();

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }
    }

    @Override
    public void start(Container container) throws Exception {
        AssetStorageService assetStorageService = container.getService(AssetStorageService.class);
        Properties properties = new Properties();
        properties.setProperty(BrokerConstants.HOST_PROPERTY_NAME, host);
        properties.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(port));
        properties.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, String.valueOf(true));
        List<? extends InterceptHandler> interceptHandlers = Collections.singletonList(new ORInterceptHandler(this, identityProvider, messageBrokerService));

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

        mqttBroker = new Server();
        mqttBroker.startServer(new MemoryConfig(properties), interceptHandlers, null, this, new ORAuthorizatorPolicy(identityProvider, this, assetStorageService, clientEventService));
        LOG.fine("Started MQTT broker");
    }

    @Override
    public void stop(Container container) throws Exception {
        mqttBroker.stopServer();
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

    /**
     * Validates an incoming authenticated connection and if rejected it will close the connection without calling
     * the intercept handler.
     */
    @Override
    public boolean checkValid(String clientId, String username, byte[] password) {
        String realm = null;
        if (username != null) {
            String[] realmAndClientId = username.split(":");
            realm = realmAndClientId[0];
            username = realmAndClientId[1]; // This is OAuth clientId
        }
        String suppliedClientSecret = password != null ? new String(password, StandardCharsets.UTF_8) : null;

        // Removed client ID check as it won't execute for anonymous connections; single connection per user is more
        // important

        if (TextUtil.isNullOrEmpty(realm)
            || TextUtil.isNullOrEmpty(username)
            || TextUtil.isNullOrEmpty(suppliedClientSecret)) {
            LOG.fine("Realm, client ID and/or client secret missing so this is an anonymous session: " + clientId);
            return true;
        }

        Tenant tenant = identityProvider.getTenant(realm);

        if (tenant == null || !tenant.getEnabled()) {
            LOG.warning("Realm not found or is inactive: " + realm);
            return false;
        }

        User user = identityProvider.getUserByUsername(realm, User.SERVICE_ACCOUNT_PREFIX + username);
        if (user == null || user.getEnabled() == null || !user.getEnabled() || TextUtil.isNullOrEmpty(user.getSecret())) {
            LOG.warning("User not found, disabled or doesn't support client credentials grant type: username=" + username);
            return false;
        }

        return suppliedClientSecret.equals(user.getSecret());
    }

    public Iterable<MQTTHandler> getCustomHandlers() {
        return customHandlers;
    }

    public MqttConnection getConnection(String clientId) {
        synchronized (clientIdConnectionMap) {
            return clientIdConnectionMap.get(clientId);
        }
    }

    public MqttConnection removeConnection(String clientId) {
        synchronized (clientIdConnectionMap) {
            MqttConnection connection = clientIdConnectionMap.remove(clientId);
            if (connection != null && !connection.isCleanSession()) {
                forceClearSession(clientId);
            }
            return connection;
        }
    }

    /**
     * We have this as there's no way to configure expiry time of sessions in Moquette so to make things simple we
     * clear the session whenever the client disconnects; the client can then re-subscribe as needed
     */
    public void forceClearSession(String clientId) {
        if (mqttBroker == null) {
            return;
        }

        try {
            Field sessionsField = Server.class.getDeclaredField("sessions");
            Field poolField = SessionRegistry.class.getDeclaredField("pool");
            sessionsField.setAccessible(true);
            poolField.setAccessible(true);
            SessionRegistry sessions = (SessionRegistry) sessionsField.get(mqttBroker);
            ConcurrentMap<?,?> pool = (ConcurrentMap<?, ?>) poolField.get(sessions);
            Object session = pool.get(clientId);
            pool.remove(clientId, session);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to clear session using reflection", e);
        }
    }

    public void addConnection(String clientId, MqttConnection connection) {
        synchronized (clientIdConnectionMap) {
            clientIdConnectionMap.put(clientId, connection);
        }
    }

    public void publishMessage(String topic, Object data, MqttQoS qoS) {
        try {
            ByteBuf payload = Unpooled.copiedBuffer(ValueUtil.asJSON(data).orElseThrow(() -> new IllegalStateException("Failed to convert payload to JSON string: " + data)), Charset.defaultCharset());

            MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                .qos(qoS)
                .topicName(topic)
                .payload(payload)
                .build();

            mqttBroker.internalPublish(publishMessage, INTERNAL_CLIENT_ID);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Couldn't send AttributeEvent to MQTT client", e);
        }
    }
}
