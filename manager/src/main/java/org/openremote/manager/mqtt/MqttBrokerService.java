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
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.InterceptHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;

public class MqttBrokerService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(MqttBrokerService.class.getName());

    public static final String MQTT_QUEUE = "seda://MqttQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";

    public static final String MQTT_CLIENT_ID_PREFIX = "mqtt-";
    public static final String MQTT_SERVER_LISTEN_HOST = "MQTT_SERVER_LISTEN_HOST";
    public static final String MQTT_SERVER_LISTEN_PORT = "MQTT_SERVER_LISTEN_PORT";

    public static final String ASSETS_TOPIC = "assets";
    public static final String TOPIC_SEPARATOR = "/";

    protected ManagerIdentityService identityService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected MessageBrokerService messageBrokerService;

    protected MqttConnector mqttConnector;

    protected boolean active;
    protected String host;
    protected int port;
    protected Server mqttBroker;

    public static boolean isMqttClientId(String clientId) {
        return clientId != null && clientId.startsWith(MQTT_CLIENT_ID_PREFIX);
    }

    public static String getMqttIdFromClientId(String clientId) {
        return clientId.substring(MQTT_CLIENT_ID_PREFIX.length());
    }

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        host = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST);
        port = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT);

        mqttConnector = new MqttConnector();

        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        identityService = container.getService(ManagerIdentityService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }

        mqttBroker = new Server();
    }

    @Override
    public void start(Container container) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(BrokerConstants.HOST_PROPERTY_NAME, host);
        properties.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(port));
        properties.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, String.valueOf(false));
        List<? extends InterceptHandler> interceptHandlers = Collections.singletonList(new AssetInterceptHandler(assetStorageService, assetProcessingService, identityService, identityProvider, messageBrokerService, mqttConnector, this::sendAssetAttributeUpdateMessage));
        mqttBroker.startServer(new MemoryConfig(properties), interceptHandlers, null, new KeycloakAuthenticator(identityProvider), new KeycloakAuthorizatorPolicy(identityProvider, assetStorageService, mqttConnector));
        LOG.fine("Started MQTT broker");
    }

    @Override
    public void stop(Container container) throws Exception {
        mqttBroker.stopServer();
        LOG.fine("Stopped MQTT broker");
    }

    public void sendAssetAttributeUpdateMessage(String clientId, AttributeRef attributeRef, Optional<Value> value) {
        ByteBuf payload = null;
        if (value.isPresent()) {
            Optional<String> stringValue = Values.getString(value.get());
            if (stringValue.isPresent()) {
                payload = Unpooled.copiedBuffer(stringValue.get(), Charset.defaultCharset());
            }
            Optional<Double> doubleValue = Values.getNumber(value.get());
            if (doubleValue.isPresent()) {
                payload = Unpooled.copyDouble(doubleValue.get());
            }
            Optional<Boolean> boolValue = Values.getBoolean(value.get());
            if (boolValue.isPresent()) {
                payload = Unpooled.copyBoolean(boolValue.get());
            }
        }
        if (payload == null) {
            payload = Unpooled.buffer();
        }
        MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                .qos(MqttQoS.AT_MOST_ONCE)
                .topicName(ASSETS_TOPIC + TOPIC_SEPARATOR + attributeRef.getEntityId() + TOPIC_SEPARATOR + attributeRef.getAttributeName())
                .payload(payload)
                .build();

        mqttBroker.internalPublish(publishMessage, clientId);
    }
}
