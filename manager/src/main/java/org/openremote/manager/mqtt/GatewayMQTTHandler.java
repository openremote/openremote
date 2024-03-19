/*
 * Copyright 2021, OpenRemote Inc.
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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.camel.builder.RouteBuilder;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.mqtt.MqttErrorResponseMessage;
import org.openremote.model.mqtt.MqttSuccessResponseMessage;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.apache.camel.support.builder.PredicateBuilder.and;
import static org.openremote.manager.event.ClientEventService.*;
import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString;
import static org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler.UNIQUE_ID_PLACEHOLDER;
import static org.openremote.model.Constants.*;
import static org.openremote.model.syslog.SyslogCategory.API;


@SuppressWarnings("unused, rawtypes")
public class GatewayMQTTHandler extends MQTTHandler {

    // main topics
    public static final String EVENTS_TOPIC = "events"; // subscription topics
    public static final String OPERATIONS_TOPIC = "operations"; // publish topics (request-response)
    public static final String RESPONSE_TOPIC = "response"; // response suffix for operations topics
    public static final String ATTRIBUTES_TOPIC = "attributes";
    public static final String ASSETS_TOPIC = "assets";

    // method topics
    public static final String CREATE = "create";
    public static final String DELETE = "delete";
    public static final String UPDATE = "update";
    public static final String GET = "get";


    // index tokens
    public static final int OPERATIONS_TOKEN_INDEX = 2;
    public static final int EVENTS_TOKEN_INDEX = 2;
    public static final int ASSETS_TOKEN_INDEX = 3;
    public static final int ASSET_ID_TOKEN_INDEX = 4;
    public static final int RESPONSE_ID_TOKEN_INDEX = 4;
    public static final int ASSETS_METHOD_TOKEN = 5;
    public static final int ATTRIBUTES_TOKEN_INDEX = 5;
    public static final int ATTRIBUTE_NAME_TOKEN_INDEX = 6;
    public static final int ATTRIBUTES_METHOD_TOKEN = 7;

    protected static final Logger LOG = SyslogCategory.getLogger(API, GatewayMQTTHandler.class);
    protected AssetProcessingService assetProcessingService;
    protected TimerService timerService;
    protected AssetStorageService assetStorageService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected GatewayMQTTSubscriptionManager subscriptionManager;
    protected boolean isKeycloak;


    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
            isKeycloak = false;
        } else {
            isKeycloak = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }
    }

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        timerService = container.getService(TimerService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        subscriptionManager = new GatewayMQTTSubscriptionManager(messageBrokerService, mqttBrokerService);

        messageBrokerService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(CLIENT_OUTBOUND_QUEUE)
                        .routeId("ClientOutbound-GatewayMQTTHandler")
                        .filter(and(
                                header(HEADER_CONNECTION_TYPE).isEqualTo(HEADER_CONNECTION_TYPE_MQTT),
                                body().isInstanceOf(TriggeredEventSubscription.class)
                        ))
                        .process(exchange -> {
                            String connectionID = exchange.getIn().getHeader(SESSION_KEY, String.class);
                            GatewayMQTTSubscriptionManager.GatewaySubscriberInfo subscriberInfo = subscriptionManager.getConnectionSubscriberInfoMap().get(connectionID);
                            if (subscriberInfo != null) {
                                LOG.fine("Sending event to subscriber " + connectionID);
                                TriggeredEventSubscription<?> event = exchange.getIn().getBody(TriggeredEventSubscription.class);
                                Consumer<SharedEvent> eventConsumer = subscriberInfo.topicSubscriptionMap.get(event.getSubscriptionId());
                                if (eventConsumer != null) {
                                    eventConsumer.accept(event.getEvents().get(0));
                                }
                            }
                        });
            }
        });
    }


    @Override
    public boolean handlesTopic(Topic topic) {
        return topicMatches(topic);
    }

    @Override
    public boolean topicMatches(Topic topic) {
        return isOperationsTopic(topic) || isEventsTopic(topic);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (!isKeycloak) {
            LOG.fine("Identity provider is not keycloak");
            return false;
        }

        AuthContext authContext = getAuthContextFromSecurityContext(securityContext);
        if (authContext == null) {
            LOG.finest("Anonymous connection not supported: topic=" + topic + ", " + MQTTBrokerService.connectionToString(connection));
            return false;
        }

        if (!isEventsTopic(topic)) {
            if (!isOperationsTopic(topic) && isResponseTopic(topic)) {
                LOG.finest("Invalid topic " + topic + " for subscribing, must be " + OPERATIONS_TOPIC);
                return false;
            }
            LOG.finest("Invalid topic " + topic + " for subscribing, must be " + EVENTS_TOPIC + " or " + OPERATIONS_TOPIC);
            return false;
        }


        return true;
    }

    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
        Optional<AuthContext> authContext = getAuthContextFromConnection(connection);
        if (authContext.isEmpty()) {
            LOG.finer("Anonymous publish not supported: topic=" + topic + ", connection=" + MQTTBrokerService.connectionToString(connection));
            return;
        }
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        //subscriptionManager.unsubscribe(connection, topic);
    }

    @Override
    public Set<String> getPublishListenerTopics() {


        String topicBase = TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + OPERATIONS_TOPIC + "/";


        return Set.of(
                // <realm>/<clientId>/operations/assets/<responseId>/create
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + CREATE,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/update
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + UPDATE
        );
    }


    @Override
    public boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (!isKeycloak) {
            LOG.fine("Identity provider is not keycloak");
            return false;
        }

        AuthContext authContext = getAuthContextFromSecurityContext(securityContext);

        if (authContext == null) {
            LOG.finer("Anonymous publish not supported: topic=" + topic + ", connection=" + MQTTBrokerService.connectionToString(connection));
            return false;
        }

        if (!isOperationsTopic(topic)) {
            LOG.finest("Invalid topic " + topic + " for publishing, must be " + OPERATIONS_TOPIC);
            return false;
        }

        boolean isRestrictedUser = authContext.hasRealmRole(RESTRICTED_USER_REALM_ROLE);
        boolean hasWriteAssetsRole = authContext.hasResourceRole(WRITE_ASSETS_ROLE, KEYCLOAK_CLIENT_ID);

        List<String> topicTokens = topic.getTokens();

        // Asset operations
        if (isAssetsMethodTopic(topic)) {
            String method = topicTokens.get(ASSETS_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case CREATE -> {
                    if (isRestrictedUser || !hasWriteAssetsRole) {
                        LOG.finer("User is unauthorized to create assets in realm " + topicRealm(topic) + " " + MQTTBrokerService.connectionToString(connection));
                        return false;
                    }
                }
                case DELETE -> {
                    if (isRestrictedUser || !hasWriteAssetsRole) {
                        LOG.finer("User is unauthorized to delete assets in realm " + topicRealm(topic) + " " + MQTTBrokerService.connectionToString(connection));
                        return false;
                    }
                    if (!hasValidAssetId(topic)) {
                        LOG.finer("Invalid delete asset request " + topic + " should include a valid asset ID <realm>/<clientId>/operations/assets/<assetId>/delete " + MQTTBrokerService.connectionToString(connection));
                        return false;
                    }
                }
            }
            // Attribute operations
        } else if (isAttributesMethodTopic(topic)) {
            String method = topicTokens.get(ATTRIBUTES_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case UPDATE, GET -> {
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, buildAttributeEvent(topicTokens, null))) {
                        LOG.fine("Publish was not authorised for this user and topic: topic=" + topic + ", subject=" + authContext);
                        return false;
                    }
                }
            }
        } else { // unsupported publish topic
            return false;
        }

        return true;
    }

    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        // Asset operations
        if (isAssetsMethodTopic(topic)) {
            var method = topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case CREATE -> assetCreateRequest(connection, topic, body);
                case DELETE ->
                        LOG.fine("Received delete asset request " + topic + " " + getConnectionIDString(connection));
            }
        }

        // Attribute operations
        if (isAttributesMethodTopic(topic)) {
            var method = topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case UPDATE -> {
                    singleLineAttributeUpdateRequest(connection, topic, body);
                }
                case GET ->
                        LOG.fine("Received get attribute request " + topic + " " + getConnectionIDString(connection));
            }
        }
    }

    @Override
    public void onConnectionLost(RemotingConnection connection) {
        super.onConnectionLost(connection);
    }

    @Override
    public void onDisconnect(RemotingConnection connection) {
        super.onDisconnect(connection);
    }


    protected void assetCreateRequest(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);
        String realm = topicRealm(topic);

        String assetTemplate = payloadContent;
        assetTemplate = assetTemplate.replaceAll(UNIQUE_ID_PLACEHOLDER, UniqueIdentifierGenerator.generateId());

        Optional<Asset> optionalAsset = ValueUtil.parse(assetTemplate, Asset.class);
        if (optionalAsset.isEmpty()) {
            LOG.fine("Invalid asset template " + payloadContent + " in create asset request " + getConnectionIDString(connection));
            return;
        }
        Asset<?> asset = optionalAsset.get();
        asset.setId(UniqueIdentifierGenerator.generateId());
        asset.setRealm(realm);

        assetStorageService.merge(asset);
        publishSuccessResponse(topic, MqttSuccessResponseMessage.Success.CREATED, realm, asset);
    }

    protected void singleLineAttributeUpdateRequest(RemotingConnection connection, Topic topic, ByteBuf body) {

        LOG.fine("Received attribute update request " + topic + " " + getConnectionIDString(connection));
        String realm = topicRealm(topic);
        AttributeEvent event = buildAttributeEvent(topic.getTokens(), body.toString(StandardCharsets.UTF_8));

        Map<String, Object> headers = prepareHeaders(realm, connection);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .withBody(event)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncRequest();
    }


    protected boolean hasValidAssetId(Topic topic) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        if (assetId == null) {
            return false;
        }
        return Pattern.matches(ASSET_ID_REGEXP, assetId);
    }


    protected static AttributeEvent buildAttributeEvent(List<String> topicTokens, Object value) {
        String attributeName = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX);
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        return new AttributeEvent(assetId, attributeName, value).setSource(GatewayMQTTHandler.class.getSimpleName());
    }


    protected boolean isAssetsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, ASSETS_TOKEN_INDEX), ASSETS_TOPIC);
    }

    protected boolean isAssetsMethodTopic(Topic topic) {
        return isAssetsTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), CREATE) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), DELETE) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), UPDATE) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), GET);
    }

    protected boolean isAttributesMethodTopic(Topic topic) {
        return isAttributesTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN), UPDATE) ||
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN), GET);
    }


    protected boolean isAttributesTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, ASSETS_TOKEN_INDEX), ASSETS_TOPIC) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_TOKEN_INDEX), ATTRIBUTES_TOPIC);
    }

    protected boolean isOperationsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, OPERATIONS_TOKEN_INDEX), OPERATIONS_TOPIC);
    }

    protected boolean isResponseTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, topic.getTokens().size() - 1), RESPONSE_TOPIC);
    }

    protected boolean isEventsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, EVENTS_TOKEN_INDEX), EVENTS_TOPIC);
    }


    protected static Map<String, Object> prepareHeaders(String requestRealm, RemotingConnection connection) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(SESSION_KEY, getConnectionIDString(connection));
        headers.put(HEADER_CONNECTION_TYPE, ClientEventService.HEADER_CONNECTION_TYPE_MQTT);
        headers.put(REALM_PARAM_NAME, requestRealm);
        return headers;
    }

    public String getResponseTopic(Topic topic) {
        return topic.toString() + "/" + RESPONSE_TOPIC;
    }

    protected void publishErrorResponse(Topic topic, MqttErrorResponseMessage.Error error) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), new MqttErrorResponseMessage(error), MqttQoS.AT_MOST_ONCE);
    }

    protected void publishErrorResponse(Topic topic, MqttErrorResponseMessage.Error error, String message) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), new MqttErrorResponseMessage(error, message), MqttQoS.AT_MOST_ONCE);
    }

    protected void publishSuccessResponse(Topic topic, MqttSuccessResponseMessage.Success success, String realm) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), new MqttSuccessResponseMessage(success, realm), MqttQoS.AT_MOST_ONCE);
    }

    protected void publishSuccessResponse(Topic topic, MqttSuccessResponseMessage.Success success, String realm, Object data) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), new MqttSuccessResponseMessage(success, realm, data), MqttQoS.AT_MOST_ONCE);
    }


}


