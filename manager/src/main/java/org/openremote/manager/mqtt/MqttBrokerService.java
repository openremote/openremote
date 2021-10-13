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
import io.moquette.broker.security.IAuthenticator;
import io.moquette.interception.InterceptHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.manager.event.ClientEventService.getSessionKey;
import static org.openremote.model.syslog.SyslogCategory.API;

public class MqttBrokerService implements ContainerService, IAuthenticator {

    public static final int PRIORITY = MED_PRIORITY;
    private static final Logger LOG = SyslogCategory.getLogger(API, MqttBrokerService.class);

    public static final String MQTT_CLIENT_QUEUE = "seda://MqttClientQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    public static final String MQTT_SERVER_LISTEN_HOST = "MQTT_SERVER_LISTEN_HOST";
    public static final String MQTT_SERVER_LISTEN_PORT = "MQTT_SERVER_LISTEN_PORT";

    public static final String ASSET_TOPIC = "asset";
    public static final String ATTRIBUTE_TOPIC = "attribute";
    public static final String ATTRIBUTE_VALUE_TOPIC = "attributevalue";
    public static final String SINGLE_LEVEL_WILDCARD = "+";
    public static final String MULTI_LEVEL_WILDCARD = "#";

    protected ManagerKeycloakIdentityProvider identityProvider;
    protected ClientEventService clientEventService;
    protected MessageBrokerService messageBrokerService;
    protected Map<String, MqttConnection> clientIdConnectionMap = new HashMap<>();
    protected List<MQTTCustomHandler> customHandlers = new ArrayList<>();

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
                            TriggeredEventSubscription<SharedEvent> triggeredEventSubscription = exchange.getIn().getBody(TriggeredEventSubscription.class);
                            MqttConnection connection = clientIdConnectionMap.get(sessionKey);

                            if (connection == null) {
                                return;
                            }

                            onSubscriptionTriggered(connection, triggeredEventSubscription);
                        })
                        .end();
            }
        });
    }

    @Override
    public void start(Container container) throws Exception {
        AssetStorageService assetStorageService = container.getService(AssetStorageService.class);
        Properties properties = new Properties();
        properties.setProperty(BrokerConstants.HOST_PROPERTY_NAME, host);
        properties.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(port));
        properties.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, String.valueOf(false));
        List<? extends InterceptHandler> interceptHandlers = Collections.singletonList(new ORInterceptHandler(this, identityProvider, messageBrokerService, clientIdConnectionMap));

        // Load custom handlers
        this.customHandlers = stream(ServiceLoader.load(MQTTCustomHandler.class).spliterator(), false)
            .sorted(Comparator.comparingInt(MQTTCustomHandler::getPriority))
            .collect(Collectors.toList());

        // Start each custom handler
        for (MQTTCustomHandler handler : customHandlers) {
            try {
                handler.start(container);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "MQTT custom handler threw an exception whilst starting: handler=" + handler.getName(), e);
                throw e;
            }
        }

        mqttBroker.startServer(new MemoryConfig(properties), interceptHandlers, null, this, new ORAuthorizatorPolicy(identityProvider, this, assetStorageService, clientEventService));
        LOG.fine("Started MQTT broker");
    }

    @Override
    public void stop(Container container) throws Exception {
        mqttBroker.stopServer();
        LOG.fine("Stopped MQTT broker");

        stream(ServiceLoader.load(MQTTCustomHandler.class).spliterator(), false)
            .sorted(Comparator.comparingInt(MQTTCustomHandler::getPriority).reversed())
            .forEach(handler -> {
                try {
                    handler.stop();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "MQTT custom handler threw an exception whilst stopping: handler=" + handler.getName(), e);
                }
            });
    }

    public Iterable<MQTTCustomHandler> getCustomHandlers() {
        return customHandlers;
    }

    public void sendToSession(String sessionId, String topic, Object data, MqttQoS qoS) {
        try {
            ByteBuf payload = Unpooled.copiedBuffer(ValueUtil.asJSON(data).orElseThrow(() -> new IllegalStateException("Failed to convert payload to JSON string: " + data)), Charset.defaultCharset());

            MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                .qos(qoS)
                .topicName(topic)
                .payload(payload)
                .build();

            mqttBroker.internalPublish(publishMessage, sessionId);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Couldn't send AttributeEvent to MQTT client", e);
        }
    }

    /**
     * Validates an incoming connection and if rejected it will close the connection without calling intercept handler
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

        if (clientIdConnectionMap.containsKey(clientId)) {
            LOG.info("Client with this ID already connected");
            return false;
        }

        if (TextUtil.isNullOrEmpty(realm)
            || TextUtil.isNullOrEmpty(username)
            || TextUtil.isNullOrEmpty(suppliedClientSecret)) {
            LOG.fine("Realm, client ID and/or client secret missing so this is an anonymous session with limited capabilities: " + clientId);
            return true;
        }

        Tenant tenant = identityProvider.getTenant(realm);

        if (tenant == null || !tenant.getEnabled()) {
            LOG.warning("Realm not found or is inactive: " + realm);
            return false;
        }

        User user = identityProvider.getUserByUsername(realm, User.SERVICE_ACCOUNT_PREFIX + username);
        if (user == null || user.getEnabled() == null || !user.getEnabled() || TextUtil.isNullOrEmpty(user.getSecret())) {
            LOG.warning("User not found, disabled or doesn't support client credentials grant type");
            return false;
        }

        return suppliedClientSecret.equals(user.getSecret());
    }

    public static boolean isAttributeTopic(List<String> tokens) {
        return tokens.get(2).equals(ATTRIBUTE_TOPIC) || tokens.get(2).equals(ATTRIBUTE_VALUE_TOPIC);
    }

    public static boolean isAssetTopic(List<String> tokens) {
        return tokens.get(2).equals(ASSET_TOPIC);
    }

    public static AssetFilter<?> buildAssetFilter(MqttConnection connection, List<String> topicTokens) {
        if (topicTokens == null || topicTokens.isEmpty()) {
            return null;
        }

        boolean isAttributeTopic = MqttBrokerService.isAttributeTopic(topicTokens);
        boolean isAssetTopic = MqttBrokerService.isAssetTopic(topicTokens);

        String realm = connection.getRealm();
        List<String> assetIds = new ArrayList<>();
        List<String> parentIds = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();

        String assetId = Pattern.matches(Constants.ASSET_ID_REGEXP, topicTokens.get(3)) ?  topicTokens.get(3) : null;
        int multiLevelIndex = topicTokens.indexOf(MULTI_LEVEL_WILDCARD);
        int singleLevelIndex = topicTokens.indexOf(SINGLE_LEVEL_WILDCARD);

        if (!isAssetTopic && !isAttributeTopic) {
            return null;
        }

        if (topicTokens.size() == 4) {
            if (isAssetTopic) {
                if (multiLevelIndex == 3) {
                    //realm/clientId/.../#
                    // No asset filtering required
                } else if (singleLevelIndex == 3) {
                    //realm/clientId/.../+
                    parentIds.add(null);
                } else {
                    //realm/clientId/.../assetId
                    assetIds.add(assetId);
                }
            } else {
                if (assetId != null) {
                    //realm/clientId/attribute/assetId
                    assetIds.add(assetId);
                } else if (singleLevelIndex == 3) {
                    //realm/clientId/attribute/+
                    parentIds.add(null);
                } else if (multiLevelIndex != 3) {
                    //realm/clientId/attribute/attributeName
                    attributeNames.add(topicTokens.get(3));
                }
            }
        } else if (topicTokens.size() == 5) {
            if (isAssetTopic) {
                if (multiLevelIndex == 4) {
                    //realm/clientId/asset/assetId/#
                    paths.add(assetId);
                } else if (singleLevelIndex == 4) {
                    //realm/clientId/asset/assetId/+
                    parentIds.add(assetId);
                } else {
                    return null;
                }
            } else {
                if (assetId != null) {
                    if (multiLevelIndex == 4) {
                        //realm/clientId/attribute/assetId/#
                        paths.add(assetId);
                    } else if (singleLevelIndex == 4) {
                        //realm/clientId/attribute/assetId/+
                        parentIds.add(assetId);
                    } else {
                        assetIds.add(assetId);

                        String attributeName = SINGLE_LEVEL_WILDCARD.equals(topicTokens.get(4)) || MULTI_LEVEL_WILDCARD.equals(topicTokens.get(4)) ? null : topicTokens.get(4);
                        if (attributeName != null) {
                            //realm/clientId/attribute/assetId/attributeName
                            attributeNames.add(topicTokens.get(4));
                        } else {
                            return null;
                        }
                    }
                } else {
                    String attributeName = SINGLE_LEVEL_WILDCARD.equals(topicTokens.get(4)) || MULTI_LEVEL_WILDCARD.equals(topicTokens.get(4)) ? null : topicTokens.get(4);
                    if (attributeName != null) {
                        attributeNames.add(attributeName);
                        if (multiLevelIndex == 4) {
                           return null; //no topic allowed after multilevel wildcard
                        } else if (singleLevelIndex == 4) {
                            //realm/clientId/attribute/+/attributeName
                            parentIds.add(null);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
            }
        } else if (topicTokens.size() == 6) {
            if (isAssetTopic || assetId == null) {
                return null;
            }
            String attributeName = SINGLE_LEVEL_WILDCARD.equals(topicTokens.get(5)) || MULTI_LEVEL_WILDCARD.equals(topicTokens.get(5)) ? null : topicTokens.get(5);
            if (attributeName != null) {
                attributeNames.add(attributeName);
                if (multiLevelIndex == 4) {
                    return null; //no topic allowed after multilevel wildcard
                } else if (singleLevelIndex == 4) {
                    //realm/clientId/attribute/assetId/+/attributeName
                    parentIds.add(assetId);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }

        AssetFilter<?> assetFilter = new AssetFilter<>().setRealm(realm);
        if (!assetIds.isEmpty()) {
            assetFilter.setAssetIds(assetIds.toArray(new String[0]));
        }
        if (!parentIds.isEmpty()) {
            assetFilter.setParentIds(parentIds.toArray(new String[0]));
        }
        if(!paths.isEmpty()) {
            assetFilter.setPath(paths.toArray(new String[0]));
        }
        if (!attributeNames.isEmpty()) {
            assetFilter.setAttributeNames(attributeNames.toArray(new String[0]));
        }
        return assetFilter;
    }

    protected void onSubscriptionTriggered(MqttConnection connection, TriggeredEventSubscription<SharedEvent> triggeredEventSubscription) {
        triggeredEventSubscription.getEvents()
            .forEach(event -> {
                Consumer<SharedEvent> eventConsumer = connection.subscriptionHandlerMap.get(triggeredEventSubscription.getSubscriptionId());
                if (eventConsumer != null) {
                    eventConsumer.accept(event);
                }
            });
    }

    protected Consumer<SharedEvent> getEventConsumer(MqttConnection connection, String topic, boolean isValueSubscription, MqttQoS mqttQoS) {
        return ev -> {
            List<String> topicTokens = Arrays.asList(topic.split("/"));
            int wildCardIndex = Math.max(topicTokens.indexOf(MULTI_LEVEL_WILDCARD), topicTokens.indexOf(SINGLE_LEVEL_WILDCARD));

            if (ev instanceof AssetEvent) {
                AssetEvent assetEvent = (AssetEvent) ev;
                if (wildCardIndex > 0) {
                    topicTokens.set(wildCardIndex, assetEvent.getAssetId());
                }
                sendToSession(connection.getClientId(), String.join("/", topicTokens), ev, mqttQoS);
            }

            if (ev instanceof AttributeEvent) {
                AttributeEvent attributeEvent = (AttributeEvent) ev;
                if (wildCardIndex > 0) {
                    if (wildCardIndex == 1) { // attribute/<wildcard>
                        topicTokens.set(wildCardIndex, attributeEvent.getAssetId());
                    } else if (wildCardIndex == 2) {
                        if (topicTokens.size() == 3) { // attribute/assetId/<wildcard>
                            topicTokens.set(wildCardIndex, attributeEvent.getAttributeName());
                        } else { // attribute/parentId/<wildcard>/attributeName
                            topicTokens.set(wildCardIndex, attributeEvent.getAssetId());
                        }
                    } else if (wildCardIndex == 3) { //attribute/parentId/assetId/<wildcard>
                        topicTokens.set(wildCardIndex, attributeEvent.getAttributeName());
                    }
                }
                if (isValueSubscription) {
                    sendToSession(connection.getClientId(), String.join("/", topicTokens), attributeEvent.getValue().orElse(null), mqttQoS);
                } else {
                    sendToSession(connection.getClientId(), String.join("/", topicTokens), ev, mqttQoS);
                }
            }
        };
    }
}
