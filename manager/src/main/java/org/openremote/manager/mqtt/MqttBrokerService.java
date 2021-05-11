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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.interception.InterceptHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.agent.protocol.ProtocolClientEventService.getSessionKey;
import static org.openremote.model.syslog.SyslogCategory.API;

public class MqttBrokerService implements ContainerService {

    public static final int PRIORITY = MED_PRIORITY;
    private static final Logger LOG = SyslogCategory.getLogger(API, MqttBrokerService.class);

    public static final String MQTT_CLIENT_QUEUE = "seda://MqttClientQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    public static final String MQTT_SERVER_LISTEN_HOST = "MQTT_SERVER_LISTEN_HOST";
    public static final String MQTT_SERVER_LISTEN_PORT = "MQTT_SERVER_LISTEN_PORT";

    public static final String ASSET_TOPIC = "asset";
    public static final String ATTRIBUTE_TOPIC = "attribute";
    public static final String TOPIC_SEPARATOR = "/";
    public static final String ATTRIBUTE_VALUE_TOPIC = "value";
    public static final String SINGLE_LEVEL_WILDCARD = "+";
    public static final String MULTI_LEVEL_WILDCARD = "#";

    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected MessageBrokerService messageBrokerService;
    protected ORAuthorizatorPolicy mainAuthorizatorPolicy;

    protected Map<String, MqttConnection> mqttConnectionMap;

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

        mainAuthorizatorPolicy = new ORAuthorizatorPolicy();
        mqttConnectionMap = new HashMap<>();

        clientEventService = container.getService(ClientEventService.class);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }

        mqttBroker = new Server();

        messageBrokerService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from(MQTT_CLIENT_QUEUE)
                        .routeId("MqttClientEvents")
                        .choice()
                        .when(body().isInstanceOf(TriggeredEventSubscription.class))
                        .process(exchange -> {
                            String sessionKey = getSessionKey(exchange);
                            @SuppressWarnings("unchecked")
                            TriggeredEventSubscription<?> triggeredEventSubscription = exchange.getIn().getBody(TriggeredEventSubscription.class);
                            triggeredEventSubscription.getEvents()
                                    .forEach(event -> {
                                        if (event.getEventType().equals(Event.getEventType(AssetEvent.class))) {
                                            AssetEvent assetEvent = (AssetEvent) event;
                                            MqttConnection mqttConnection = mqttConnectionMap.get(sessionKey);
                                            if (mqttConnection != null) {
                                                Optional<String> topic = mqttConnection.assetSubscriptions.entrySet()
                                                        .stream()
                                                        .filter(entry -> triggeredEventSubscription.getSubscriptionId().equals(entry.getValue()))
                                                        .map(Map.Entry::getKey)
                                                        .findFirst();

                                                topic.ifPresent(topicValue -> sendAssetEvent(sessionKey, topicValue, assetEvent));
                                            }
                                        } else {
                                            AttributeEvent attributeEvent = (AttributeEvent) event;
                                            MqttConnection mqttConnection = mqttConnectionMap.get(sessionKey);
                                            if (mqttConnection != null) {
                                                Optional<String> topic = mqttConnection.assetSubscriptions.entrySet()
                                                        .stream()
                                                        .filter(entry -> triggeredEventSubscription.getSubscriptionId().equals(entry.getValue()))
                                                        .map(Map.Entry::getKey)
                                                        .findFirst();

                                                topic.ifPresent(topicValue -> sendAttributeEvent(sessionKey, topicValue, attributeEvent));

                                                topic = mqttConnection.attributeValueSubscriptions.entrySet()
                                                        .stream()
                                                        .filter(entry -> triggeredEventSubscription.getSubscriptionId().equals(entry.getValue()))
                                                        .map(Map.Entry::getKey)
                                                        .findFirst();

                                                topic.ifPresent(topicValue -> sendAttributeValue(sessionKey, topicValue, attributeEvent));
                                            }
                                        }
                                    });
                        })
                        .end();
            }
        });
    }

    @Override
    public void start(Container container) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(BrokerConstants.HOST_PROPERTY_NAME, host);
        properties.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(port));
        properties.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, String.valueOf(false));
        List<? extends InterceptHandler> interceptHandlers = Collections.singletonList(new EventInterceptHandler(identityProvider, messageBrokerService, mqttConnectionMap));

        AssetStorageService assetStorageService = container.getService(AssetStorageService.class);
        mainAuthorizatorPolicy.addAuthorizatorPolicy(new KeycloakAuthorizatorPolicy(identityProvider, assetStorageService, clientEventService, mqttConnectionMap));

        mqttBroker.startServer(new MemoryConfig(properties), interceptHandlers, null, new KeycloakAuthenticator(identityProvider), mainAuthorizatorPolicy);
        LOG.fine("Started MQTT broker");
    }

    @Override
    public void stop(Container container) throws Exception {
        mqttBroker.stopServer();
        LOG.fine("Stopped MQTT broker");
    }

    protected void sendAssetEvent(String clientId, String topic, AssetEvent assetEvent) {
        try {
            ByteBuf payload = Unpooled.copiedBuffer(Values.JSON.writeValueAsString(assetEvent), Charset.defaultCharset());

            MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                    .qos(MqttQoS.AT_MOST_ONCE)
                    .topicName(topic)
                    .payload(payload)
                    .build();

            mqttBroker.internalPublish(publishMessage, clientId);
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Couldn't send AttributeEvent to MQTT client", e);
        }
    }

    protected void sendAttributeEvent(String clientId, String topic, AttributeEvent attributeEvent) {
        try {
            ByteBuf payload = Unpooled.copiedBuffer(Values.JSON.writeValueAsString(attributeEvent), Charset.defaultCharset());
            MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                    .qos(MqttQoS.AT_MOST_ONCE)
                    .topicName(topic)
                    .payload(payload)
                    .build();

            mqttBroker.internalPublish(publishMessage, clientId);
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Couldn't send AttributeEvent to MQTT client", e);
        }
    }

    public void sendAttributeValue(String clientId, String topic, AttributeEvent attributeEvent) {
        ByteBuf payload = Unpooled.copiedBuffer(Values.asJSON(attributeEvent.getValue()).orElse(""), Charset.defaultCharset());

        MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                .qos(MqttQoS.AT_MOST_ONCE)
                .topicName(topic)
                .payload(payload)
                .build();

        mqttBroker.internalPublish(publishMessage, clientId);
    }

    public MqttBrokerService addInterceptHandler(InterceptHandler interceptHandler) {
        mqttBroker.addInterceptHandler(interceptHandler);
        return this;
    }

    public void addAuthorizerPolicy(IAuthorizatorPolicy authorizatorPolicy) {
        mainAuthorizatorPolicy.addAuthorizatorPolicy(authorizatorPolicy);
    }
}
