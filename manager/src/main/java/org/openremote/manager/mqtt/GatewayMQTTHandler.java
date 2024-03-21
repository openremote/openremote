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
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.ReadAssetEvent;
import org.openremote.model.asset.ReadAttributeEvent;
import org.openremote.model.asset.impl.GatewayV2Asset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.mqtt.MQTTErrorResponse;
import org.openremote.model.mqtt.MQTTSuccessResponse;
import org.openremote.model.query.AssetQuery;
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
    public static final String CREATE_TOPIC = "create";
    public static final String DELETE_TOPIC = "delete";
    public static final String UPDATE_TOPIC = "update";
    public static final String GET_TOPIC = "get";


    // index tokens
    public static final int OPERATIONS_TOKEN_INDEX = 2;
    public static final int EVENTS_TOKEN_INDEX = 2;
    public static final int ASSETS_TOKEN_INDEX = 3;
    public static final int ASSET_ID_TOKEN_INDEX = 4;
    public static final int RESPONSE_ID_TOKEN_INDEX = 4;
    public static final int ASSETS_METHOD_TOKEN = 5;
    public static final int ATTRIBUTES_TOKEN_INDEX = 5;
    public static final int ATTRIBUTE_NAME_TOKEN_INDEX = 6;
    public static final int ATTRIBUTES_METHOD_TOKEN_INDEX = 7;

    public static final String GATEWAY_CLIENT_ID_PREFIX = "gateway-";

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

        // TODO: Check for valid topic + authorization
        return true;
    }

    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
    }

    @Override
    public Set<String> getPublishListenerTopics() {
        String topicBase = TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + OPERATIONS_TOPIC + "/";

        return Set.of(
                // <realm>/<clientId>/operations/assets/<responseId>/create
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + CREATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/delete
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + DELETE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/get
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GET_TOPIC,

                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/update
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + UPDATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + UPDATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/get
                topicBase + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GET_TOPIC
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

        boolean isRestrictedUser = identityProvider.isRestrictedUser(authContext);
        boolean hasWriteAssetsRole = authContext.hasResourceRole(WRITE_ASSETS_ROLE, KEYCLOAK_CLIENT_ID);
        String connectionID = getConnectionIDString(connection);
        List<String> topicTokens = topic.getTokens();

        // Asset operations
        if (isAssetsMethodTopic(topic)) {
            String method = topicTokens.get(ASSETS_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case CREATE_TOPIC -> {
                    //TODO: authorizeEventWrite for AssetEvents
                    if (isRestrictedUser || !hasWriteAssetsRole) {
                        LOG.finer("User is unauthorized to create assets in realm " + topicRealm(topic) + " " + connectionID);
                        return false;
                    }
                }
                case DELETE_TOPIC -> {
                    if (isRestrictedUser || !hasWriteAssetsRole) {
                        LOG.finer("User is unauthorized to delete assets in realm " + topicRealm(topic) + " " + connectionID);
                        return false;
                    }
                    if (!hasValidAssetId(topic)) {
                        LOG.finer("Invalid delete asset request " + topic + " should include a valid asset ID <realm>/<clientId>/operations/assets/<assetId>/delete " + connectionID);
                        return false;
                    }
                }
                case GET_TOPIC -> {
                    ReadAssetEvent event = buildReadAssetEvent(topicTokens);
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
                        LOG.fine("Get asset request was not authorised for this user and topic: topic=" + topic + " " + connectionID);
                        return false;
                    }

                }

            }
        }
        // Attribute operations
        else if (isAttributesMethodTopic(topic)) {
            String method = topicTokens.get(ATTRIBUTES_METHOD_TOKEN_INDEX);
            switch (Objects.requireNonNull(method)) {
                case UPDATE_TOPIC -> {
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, buildAttributeEvent(topicTokens, null))) {
                        LOG.fine("Update attribute request was not authorised for this user and topic: topic=" + topic + " " + connectionID);
                        return false;
                    }
                }
                case GET_TOPIC -> {
                    ReadAttributeEvent event = buildReadAttributeEvent(topicTokens);
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
                        LOG.fine("Get attribute request was not authorised for this user and topic: topic=" + topic + ", " + connectionID);
                        return false;
                    }
                }
            }
        }
        // Multi attribute update request (Edge case) (authorization needs to be handled in the handler function)
        else if (isMultiAttributeUpdateTopic(topic)) {
            LOG.fine("Multi attribute update request " + topic + " " + connectionID);
        } else {
            return false;
        }


        return true;
    }

    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        List<String> topicTokens = topic.getTokens();

        // Asset operations
        if (isAssetsMethodTopic(topic)) {
            var method = topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case CREATE_TOPIC -> createAssetRequest(connection, topic, body);
                case DELETE_TOPIC -> LOG.fine("Received delete asset request");
                case GET_TOPIC -> getAssetRequest(connection, topic, body);
            }
        }

        // Attribute operations
        if (isAttributesMethodTopic(topic)) {
            var method = topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX);
            switch (Objects.requireNonNull(method)) {
                case UPDATE_TOPIC -> updateAttributeRequest(connection, topic, body);
                case GET_TOPIC -> LOG.fine("Received get attribute request");
            }
        }

        // Multi attribute operation
        if (isMultiAttributeUpdateTopic(topic)) {
            LOG.fine("Received multi attribute update request " + topic + " " + getConnectionIDString(connection));
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


    protected void createAssetRequest(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);
        String realm = topicRealm(topic);

        String assetTemplate = payloadContent;
        assetTemplate = assetTemplate.replaceAll(UNIQUE_ID_PLACEHOLDER, UniqueIdentifierGenerator.generateId());

        Optional<Asset> optionalAsset = ValueUtil.parse(assetTemplate, Asset.class);
        if (optionalAsset.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.MESSAGE_INVALID, "Invalid asset template");
            LOG.fine("Invalid asset template " + payloadContent + " in create asset request " + getConnectionIDString(connection));
            return;
        }
        Asset<?> asset = optionalAsset.get();
        asset.setId(UniqueIdentifierGenerator.generateId());
        asset.setRealm(realm);

        try {
            assetStorageService.merge(asset);
        } catch (Exception e) {
            LOG.warning("Failed to merge asset " + asset + " " + getConnectionIDString(connection));
            publishErrorResponse(topic, MQTTErrorResponse.Error.SERVER_ERROR, "Failed to process asset");
            return;
        }

        publishSuccessResponse(topic, realm, asset);
    }


    protected void getAssetRequest(RemotingConnection connection, Topic topic, ByteBuf body) {
        LOG.finest("Received get asset request " + topic + " " + getConnectionIDString(connection));
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        Asset<?> asset = assetStorageService.find(assetId);
        if (asset == null) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.NOT_FOUND, "Asset not found");
            LOG.fine("Asset not found " + assetId + " " + getConnectionIDString(connection));
            return;
        }
        publishSuccessResponse(topic, topicRealm(topic), asset);
    }

    protected void updateAttributeRequest(RemotingConnection connection, Topic topic, ByteBuf body) {
        LOG.finest("Received attribute update request " + topic + " " + getConnectionIDString(connection));
        String realm = topicRealm(topic);
        AttributeEvent event = buildAttributeEvent(topic.getTokens(), body.toString(StandardCharsets.UTF_8));

        Map<String, Object> headers = prepareHeaders(realm, connection);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .withBody(event)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncRequest();

        publishSuccessResponse(topic, realm, event);
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

    protected static AssetEvent buildAssetEvent(List<String> topicTokens, Asset<?> asset, AssetEvent.Cause cause) {
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        return new AssetEvent(cause, asset, null);
    }

    protected static ReadAssetEvent buildReadAssetEvent(List<String> topicTokens) {
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        return new ReadAssetEvent(assetId);
    }

    protected static ReadAttributeEvent buildReadAttributeEvent(List<String> topicTokens) {
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        String attributeName = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX);
        return new ReadAttributeEvent(assetId, attributeName);
    }


    protected boolean isAssetsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, ASSETS_TOKEN_INDEX), ASSETS_TOPIC);
    }

    protected boolean isAssetsMethodTopic(Topic topic) {
        return isAssetsTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), CREATE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), DELETE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), UPDATE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), GET_TOPIC);
    }

    protected boolean isAttributesMethodTopic(Topic topic) {
        return isAttributesTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX), UPDATE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX), GET_TOPIC);
    }

    protected boolean isMultiAttributeUpdateTopic(Topic topic) {
        return isAttributesTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTE_NAME_TOKEN_INDEX), UPDATE_TOPIC) &&
                Objects.equals(topicTokenIndexToString(topic, topic.getTokens().size() - 1), UPDATE_TOPIC);
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

    @SuppressWarnings("unchecked")
    protected Optional<Asset<GatewayV2Asset>> getGatewayAssetFromConnection(RemotingConnection connection) {
        if (connection.getClientID().startsWith(GATEWAY_CLIENT_ID_PREFIX)) {
            return Optional.ofNullable((Asset<GatewayV2Asset>) assetStorageService.find(new AssetQuery().types(GatewayV2Asset.class)
                    .attributeValue(GatewayV2Asset.CLIENT_ID.getName(), connection.getClientID())));
        }
        return Optional.empty();
    }


    protected void publishErrorResponse(Topic topic, MQTTErrorResponse.Error error, String message) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), new MQTTErrorResponse(error, message), MqttQoS.AT_MOST_ONCE);
    }

    protected void publishSuccessResponse(Topic topic, String realm, Object data) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), new MQTTSuccessResponse(realm, data), MqttQoS.AT_MOST_ONCE);
    }


}


