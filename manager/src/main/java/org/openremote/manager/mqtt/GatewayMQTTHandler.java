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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import jakarta.persistence.EntityManager;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.apache.camel.builder.RouteBuilder;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.GatewayV2Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.mqtt.MQTTErrorResponse;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.apache.camel.support.builder.PredicateBuilder.and;
import static org.openremote.manager.event.ClientEventService.*;
import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString;
import static org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler.UNIQUE_ID_PLACEHOLDER;
import static org.openremote.model.Constants.*;
import static org.openremote.model.syslog.SyslogCategory.API;


/**
 * MQTT handler for gateway connections.
 * This handler uses the ClientEventService to publish and subscribe to asset and attribute events;
 * converting subscription topics into AssetFilters to ensure only the correct events are returned for the subscription.
 * Provides CRUD operations for assets and attributes. Takes into account GatewayAsset Service Users.
 */
@SuppressWarnings("unused, rawtypes")
public class GatewayMQTTHandler extends MQTTHandler {

    public static final String GATEWAY_CLIENT_ID_PREFIX = "gateway-";

    // main topics
    public static final String EVENTS_TOPIC = "events"; // subscriptions
    public static final String OPERATIONS_TOPIC = "operations"; // publish
    public static final String RESPONSE_TOPIC = "response"; // response suffix for operations topics

    // hierarchy topics
    public static final String ATTRIBUTES_TOPIC = "attributes";
    public static final String ASSETS_TOPIC = "assets";

    // method topics
    public static final String CREATE_TOPIC = "create";
    public static final String DELETE_TOPIC = "delete";
    public static final String UPDATE_TOPIC = "update";
    public static final String GET_TOPIC = "get";
    public static final String GET_VALUE_TOPIC = "get-value";

    // Gateway topics
    public static final String GATEWAY_TOPIC = "gateway";
    public static final String GATEWAY_EVENTS_TOPIC = "events";
    public static final String GATEWAY_PENDING_TOPIC = "pending";
    public static final String GATEWAY_ACK_TOPIC = "ack";

    // index tokens
    public static final int GATEWAY_TOKEN_INDEX = 2;
    public static final int OPERATIONS_TOKEN_INDEX = 2;
    public static final int EVENTS_TOKEN_INDEX = 2;
    public static final int GATEWAY_EVENTS_TOKEN_INDEX = 3;
    public static final int ASSETS_TOKEN_INDEX = 3;
    public static final int GATEWAY_PENDING_TOKEN_INDEX = 4;
    public static final int ASSET_ID_TOKEN_INDEX = 4;
    public static final int RESPONSE_ID_TOKEN_INDEX = 4;
    public static final int GATEWAY_ACK_ID_TOKEN_INDEX = 5;
    public static final int GATEWAY_ACK_TOKEN_INDEX = 6;
    public static final int ASSETS_METHOD_TOKEN = 5;
    public static final int ATTRIBUTES_TOKEN_INDEX = 5;
    public static final int ATTRIBUTE_NAME_TOKEN_INDEX = 6;
    public static final int ATTRIBUTES_METHOD_TOKEN_INDEX = 7;

    protected static final Logger LOG = SyslogCategory.getLogger(API, GatewayMQTTHandler.class);
    protected AssetProcessingService assetProcessingService;
    protected TimerService timerService;
    protected AssetStorageService assetStorageService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected GatewayMQTTEventSubscriptionHandler eventSubscriptionManager;
    protected boolean isKeycloak;

    // Cache for pending gateway events, used to store events that need to be acknowledged by the gateway
    // Stored maximum of 24 hours, prevent large memory usage, events processed after 24 hours are considered lost
    protected final Cache<String, SharedEvent> eventsPendingGatewayAcknowledgement = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    // Cache for authorization checks, used by the canPublish method, publishes are frequent
    protected final Cache<String, ConcurrentHashSet<String>> authorizationCache = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(300000, TimeUnit.MILLISECONDS)
            .build();

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
        eventSubscriptionManager = new GatewayMQTTEventSubscriptionHandler(messageBrokerService, mqttBrokerService);
        assetProcessingService.addEventInterceptor(this::onAttributeEventIntercepted);

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
                            GatewayMQTTEventSubscriptionHandler.GatewayEventSubscriberInfo eventSubscriberInfo = eventSubscriptionManager.getEventSubscriberInfoMap().get(connectionID);
                            if (eventSubscriberInfo != null) {
                                TriggeredEventSubscription<?> event = exchange.getIn().getBody(TriggeredEventSubscription.class);
                                Consumer<SharedEvent> eventConsumer = eventSubscriberInfo.topicSubscriptionMap.get(event.getSubscriptionId());
                                if (eventConsumer != null) {
                                    eventConsumer.accept(event.getEvents().get(0));
                                }
                            }
                        });
            }
        });
    }

    // Intercepts events from the AssetProcessingService, if the event is from a descendant of a Gateway asset
    public boolean onAttributeEventIntercepted(EntityManager em, AttributeEvent event) throws AssetProcessingException {
        // If the source is the GatewayMQTTHandler, then don't intercept the event
        if (event.getSource() != null) {
            if (event.getSource().equals(GatewayMQTTHandler.class.getSimpleName())) {
                return false;
            }
        }
        // If the event is from a descendant of a Gateway asset, then the event published to pending gateway events
        if (event.getParentId() != null) {
            Asset<?> gateway = assetStorageService.find(event.getParentId());
            if (gateway instanceof GatewayV2Asset) {
                LOG.finest("Intercepted attribute event from Gateway asset descendant: " + gateway.getId());
                publishPendingGatewayEvent(event, (GatewayV2Asset) gateway);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handlesTopic(Topic topic) {
        return topicMatches(topic);
    }

    @Override
    public boolean topicMatches(Topic topic) {
        return isOperationsTopic(topic) || isEventsTopic(topic) || isGatewayEventsTopic(topic);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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

        GatewayV2Asset gatewayAsset = null;
        if (isGatewayConnection(connection)) {
            gatewayAsset = findGatewayFromConnection(connection).orElse(null);
            if (gatewayAsset == null) {
                LOG.finer("Gateway not found for connection " + getConnectionIDString(connection));
                return false;
            }

            if (gatewayAsset.getDisabled().orElse(false)) {
                LOG.finer("Gateway is disabled " + getConnectionIDString(connection));
                return false;
            }
        }

        if (!isEventsTopic(topic) && !isOperationResponseTopic(topic) && !isGatewayEventsTopic(topic)) {
            LOG.finest("Invalid topic " + topic + " for subscribing");
            return false;
        }

        var topicTokens = topic.getTokens();

        if (isEventsTopic(topic)) {
            // topics above 4 tokens require the assets token to be present
            if (topicTokens.size() > 4 && !topicTokens.get(ASSETS_TOKEN_INDEX).equals(ASSETS_TOPIC)) {
                LOG.finest("Invalid topic " + topic + " for subscribing, based on length the assets token is missing");
                return false;
            }
            // topics above 6 tokens require the attributes token to be present
            if (topicTokens.size() > 6 && !topicTokens.get(ATTRIBUTES_TOKEN_INDEX).equals(ATTRIBUTES_TOPIC)) {
                LOG.finest("Invalid topic " + topic + " for subscribing, based on length the attributes token is missing");
                return false;
            }

            // If the gatewayAsset is found the assetId provided has to be a descendant of the gateway asset
            if (gatewayAsset != null) {
                if (topicHasValidAssetId(topic)) {
                    var assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
                    if (!isAssetDescendantOfGateway(assetId, gatewayAsset)) {
                        LOG.finest("Asset not descendant of gateway " + assetId + " " + getConnectionIDString(connection));
                        return false;
                    }
                }
            }

            // build the asset filter from the topic, the gateway asset can be null
            AssetFilter<?> filter = GatewayMQTTEventSubscriptionHandler.buildAssetFilter(topic, gatewayAsset);
            if (filter == null) {
                LOG.finest("Failed to process subscription topic: topic=" + topic + ", " + MQTTBrokerService.connectionToString(connection));
                return false;
            }
            EventSubscription<?> subscription = new EventSubscription(
                    isAssetsTopic(topic) ? AssetEvent.class : AttributeEvent.class,
                    filter
            );
            if (!clientEventService.authorizeEventSubscription(topicRealm(topic), authContext, subscription)) {
                LOG.finest("Subscription was not authorised for this user and topic: topic=" + topic + ", subject=" + authContext);
                return false;
            }
        }

        // Pending events for gateway acknowledgement
        if (isGatewayEventsTopic(topic) && gatewayAsset != null) {
            if (topicTokens.size() > 4 && !topicTokens.get(GATEWAY_PENDING_TOKEN_INDEX).equals(GATEWAY_PENDING_TOPIC)) {
                LOG.finest("Invalid topic " + topic + " for subscribing, the pending topic is missing");
                return false;
            }

            // last topic has to be single level wildcard (e.g. ../pending/+)
            if (!topicTokens.get(topicTokens.size() - 1).equals(TOKEN_SINGLE_LEVEL_WILDCARD)) {
                LOG.finest("Invalid topic " + topic + " for subscribing, the last token is not a single level wildcard");
                return false;
            }
        }

        return true;
    }

    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
        if (isEventsTopic(topic)) {
            GatewayV2Asset gatewayAsset = findGatewayFromConnection(connection).orElse(null);
            var eventClass = isAssetsTopic(topic) ? AssetEvent.class : AttributeEvent.class;
            // if the gateway asset is found, the subscription will be relative to the asset, otherwise realm relative.
            eventSubscriptionManager.addSubscription(connection, topic, eventClass, gatewayAsset);
        }
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        if (isEventsTopic(topic)) {
            eventSubscriptionManager.removeSubscription(connection, topic);
        }
    }

    @Override
    public Set<String> getPublishListenerTopics() {

        //<realm>/<clientId>/
        String topicBase = TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/";

        return Set.of(
                // <realm>/<clientId>/gateway/events/pending/<ackId>/ack
                topicBase + GATEWAY_TOPIC + "/" + GATEWAY_EVENTS_TOPIC + "/" + GATEWAY_PENDING_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GATEWAY_ACK_TOPIC,
                // <realm>/<clientId>/operations/assets/<responseId>/create
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + CREATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/delete
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + DELETE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/get
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GET_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/update
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + UPDATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/update
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + UPDATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + UPDATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/get
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GET_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/get-value
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GET_VALUE_TOPIC
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

        if (!isOperationsTopic(topic) && !isGatewayEventsTopic(topic)) {
            LOG.finest("Invalid topic " + topic + " for publishing, must be " + OPERATIONS_TOPIC);
            return false;
        }

        GatewayV2Asset gatewayAsset = null;
        if (isGatewayConnection(connection)) {
            gatewayAsset = findGatewayFromConnection(connection).orElse(null);

            if (gatewayAsset == null) {
                LOG.finer("Gateway not found for connection " + getConnectionIDString(connection));
                return false;
            }

            if (gatewayAsset.getDisabled().orElse(false)) {
                LOG.finer("Gateway is disabled " + getConnectionIDString(connection));
                return false;
            }
        }

        // Check if user has been authorized for the topic before (cache, 30s expiration)
        String cacheKey = getConnectionIDString(connection);
        if (isCacheAuthorized(cacheKey, topic)) {
            return true;
        }

        boolean isRestrictedUser = identityProvider.isRestrictedUser(authContext);
        boolean hasWriteAssetsRole = authContext.hasResourceRole(WRITE_ASSETS_ROLE, KEYCLOAK_CLIENT_ID);
        String connectionID = getConnectionIDString(connection);
        List<String> topicTokens = topic.getTokens();


        // Acknowledgement of gateway events
        if (isGatewayEventsTopic(topic)) {
            if (gatewayAsset == null) {
                LOG.finest("Gateway not found for connection, cannot publish to gateway events topic " + connectionID);
                return false;
            }
            if (topicTokens.size() > 6 && !topicTokens.get(GATEWAY_ACK_TOKEN_INDEX).equals(GATEWAY_ACK_TOPIC)) {
                LOG.finest("Invalid topic " + topic + " for publishing, the ack suffix topic is missing");
                return false;
            }
            var ackId = topicTokenIndexToString(topic, GATEWAY_ACK_ID_TOKEN_INDEX);
            if (ackId == null) {
                LOG.finest("Invalid topic " + topic + " for publishing, the acknowledgement id is missing");
                return false;
            }
        }
        // Asset operations
        else if (isAssetsMethodTopic(topic)) {
            String method = topicTokens.get(ASSETS_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case CREATE_TOPIC -> {
                    AssetEvent event = buildAssetEvent(topicTokens, AssetEvent.Cause.CREATE);
                    if (event == null) {
                        return false;
                    }
                    if (isGatewayConnection(connection)) {
                        if (!authorizeGatewayEvent(gatewayAsset, authContext, event)) {
                            LOG.fine("Create asset request was not authorised for this gateway user and topic: topic=" + topic + " " + connectionID);
                            return false;
                        }
                    }
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
                        LOG.fine("Create asset request was not authorised for this user and topic: topic=" + topic + " " + connectionID);
                        return false;
                    }
                }
                case GET_TOPIC -> {
                    ReadAssetEvent event = buildReadAssetEvent(topicTokens);
                    if (isGatewayConnection(connection)) {
                        if (!authorizeGatewayEvent(gatewayAsset, authContext, event)) {
                            LOG.fine("Get asset request was not authorised for this gateway user and topic: topic=" + topic + " " + connectionID);
                            return false;
                        }
                    }
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
                        LOG.fine("Get asset request was not authorised for this user and topic: topic=" + topic + " " + connectionID);
                        return false;
                    }
                }
                case UPDATE_TOPIC -> {
                    AssetEvent event = buildAssetEvent(topicTokens, AssetEvent.Cause.UPDATE);
                    if (event == null) {
                        return false;
                    }
                    if (isGatewayConnection(connection)) {
                        if (!authorizeGatewayEvent(gatewayAsset, authContext, event)) {
                            LOG.fine("Update asset request was not authorised for this gateway user and topic: topic=" + topic + " " + connectionID);
                            return false;
                        }
                    }
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
                        LOG.fine("Update asset request was not authorised for this user and topic: topic=" + topic + " " + connectionID);
                        return false;
                    }

                }
                case DELETE_TOPIC -> {
                    AssetEvent event = buildAssetEvent(topicTokens, AssetEvent.Cause.DELETE);
                    if (event == null) {
                        return false;
                    }
                    if (isGatewayConnection(connection)) {
                        if (!authorizeGatewayEvent(gatewayAsset, authContext, event)) {
                            LOG.fine("Delete asset request was not authorised for this gateway user and topic: topic=" + topic + " " + connectionID);
                            return false;
                        }
                    }
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
                        LOG.fine("Delete asset request was not authorised for this user and topic: topic=" + topic + " " + connectionID);
                        return false;
                    }
                }
            }
        }
        // Attribute operations
        else if (isAttributesMethodTopic(topic)) {
            String method = topicTokens.get(ATTRIBUTES_METHOD_TOKEN_INDEX);
            switch (Objects.requireNonNull(method)) {
                case GET_TOPIC, GET_VALUE_TOPIC -> {
                    ReadAttributeEvent event = buildReadAttributeEvent(topicTokens);
                    if (isGatewayConnection(connection)) {
                        if (!authorizeGatewayEvent(gatewayAsset, authContext, event)) {
                            LOG.fine("Get attribute request was not authorised for this gateway user and topic: topic=" + topic + " " + connectionID);
                            return false;
                        }
                    }
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
                        LOG.fine("Get attribute request was not authorised for this user and topic: topic=" + topic + ", " + connectionID);
                        return false;
                    }
                }
                case UPDATE_TOPIC -> {
                    if (isGatewayConnection(connection)) {
                        if (!authorizeGatewayEvent(gatewayAsset, authContext, buildAttributeEvent(topicTokens, null))) {
                            LOG.fine("Update attribute request was not authorised for this gateway user and topic: topic=" + topic + " " + connectionID);
                            return false;
                        }
                    }
                    if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, buildAttributeEvent(topicTokens, null))) {
                        LOG.fine("Update attribute request was not authorised for this user and topic: topic=" + topic + " " + connectionID);
                        return false;
                    }
                }
            }
        }
        // Multi attribute update request (edge case) - Acknowledge the publish request. Authorization is done in the topic handler
        else if (isMultiAttributeUpdateTopic(topic)) {
            LOG.fine("Multi attribute update request " + topic + " " + connectionID);
        } else {
            return false;
        }

        ConcurrentHashSet<String> set;
        synchronized (authorizationCache) {
            ConcurrentHashSet<String> act = authorizationCache.getIfPresent(cacheKey);
            if (act != null) {
                set = act;
            } else {
                set = new ConcurrentHashSet<>();
                authorizationCache.put(cacheKey, set);
            }
        }
        set.add(topic.getString());
        return true;
    }

    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        List<String> topicTokens = topic.getTokens();

        // Asset operations
        if (isAssetsMethodTopic(topic)) {
            var method = topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case CREATE_TOPIC -> handleCreateAsset(connection, topic, body);
                case GET_TOPIC -> handleGetAsset(connection, topic, body);
                case UPDATE_TOPIC -> handleUpdateAsset(connection, topic, body);
                case DELETE_TOPIC -> handleDeleteAsset(connection, topic, body);
            }
        }

        // Attribute operations
        if (isAttributesMethodTopic(topic)) {
            var method = topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX);
            switch (Objects.requireNonNull(method)) {
                case GET_TOPIC -> handleGetAttribute(connection, topic, body, false);
                case GET_VALUE_TOPIC -> handleGetAttribute(connection, topic, body, true);
                case UPDATE_TOPIC -> handleUpdateAttribute(connection, topic, body);
            }
        }

        // Multi attribute operation (authorization in topic handler)
        if (isMultiAttributeUpdateTopic(topic)) {
            handleUpdateMultiAttribute(connection, topic, body);
        }

        // Handle gateway events acknowledgement
        if (isGatewayEventsTopic(topic)) {
            handleGatewayEventAcknowledgement(connection, topic);
        }
    }

    @Override
    public void onConnectionLost(RemotingConnection connection) {
        if (isGatewayConnection(connection)) {
            findGatewayFromConnection(connection).ifPresent(gatewayAsset -> {
                updateGatewayStatus(gatewayAsset, ConnectionStatus.DISCONNECTED);
            });
        }

        var headers = prepareHeaders(null, connection);
        headers.put(SESSION_CLOSE_ERROR, true);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();

        // Remove all event subscriptions for the connection
        eventSubscriptionManager.removeAllSubscriptions(connection);

        // Invalidate the authorization cache for the connection
        authorizationCache.invalidate(getConnectionIDString(connection));
    }

    @Override
    public void onDisconnect(RemotingConnection connection) {
        if (isGatewayConnection(connection)) {
            findGatewayFromConnection(connection).ifPresent(gatewayAsset -> {
                updateGatewayStatus(gatewayAsset, ConnectionStatus.DISCONNECTED);
            });
        }

        var headers = prepareHeaders(null, connection);
        headers.put(SESSION_CLOSE, true);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();

        // Remove all event subscriptions for the connection
        eventSubscriptionManager.removeAllSubscriptions(connection);

        // Invalidate the authorization cache for the connection
        authorizationCache.invalidate(getConnectionIDString(connection));
    }

    @Override
    public void onConnect(RemotingConnection connection) {
        if (isGatewayConnection(connection)) {
            findGatewayFromConnection(connection).ifPresent(gatewayAsset -> {
                updateGatewayStatus(gatewayAsset, ConnectionStatus.CONNECTED);
            });
        }
        var headers = prepareHeaders(null, connection);
        headers.put(SESSION_OPEN, true);

        Runnable closeRunnable = () -> {
            if (mqttBrokerService != null) {
                LOG.fine("Calling client session closed force disconnect runnable: " + MQTTBrokerService.connectionToString(connection));
                mqttBrokerService.doForceDisconnect(connection);
            }
        };
        headers.put(SESSION_TERMINATOR, closeRunnable);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();
    }

    /**
     * Create asset request topic handler
     * Creates an asset from the provided template and stores it in the asset storage service.
     * If the connection is a gateway connection, the asset is associated with the gateway asset.
     */
    protected void handleCreateAsset(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);
        String realm = topicRealm(topic);

        String assetTemplate = payloadContent;
        // replace placeholders with unique identifiers
        assetTemplate = assetTemplate.replaceAll(UNIQUE_ID_PLACEHOLDER, UniqueIdentifierGenerator.generateId());

        Optional<Asset> optionalAsset = ValueUtil.parse(assetTemplate, Asset.class);
        if (optionalAsset.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Invalid asset template");
            LOG.fine("Invalid asset template " + payloadContent + " in create asset request " + getConnectionIDString(connection));
            return;
        }
        Asset<?> asset = optionalAsset.get();

        // ensure the asset ID is unique and not conflicting.
        if (asset.getId() != null && !asset.getId().isEmpty()) {
            Asset<?> existingAsset = assetStorageService.find(asset.getId());
            if (existingAsset != null && existingAsset.getRealm().equals(realm)) {
                publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Asset ID conflict");
                return;
            }
        } else {
            asset.setId(UniqueIdentifierGenerator.generateId());
            asset.setRealm(realm);
        }

        // If it a gateway user connection, the associated gateway asset will always be the parent.
        if (isGatewayConnection(connection)) {
            Optional<GatewayV2Asset> gateway = findGatewayFromConnection(connection);
            gateway.ifPresent(gatewayAsset -> asset.setParentId(gatewayAsset.getId()));
        }

        try {
            assetStorageService.merge(asset);
        } catch (Exception e) {
            LOG.warning("Failed to merge asset " + asset + " " + getConnectionIDString(connection));
            publishErrorResponse(topic, MQTTErrorResponse.Error.INTERNAL_SERVER_ERROR, "Failed to process asset");
            return;
        }

        AssetEvent event = new AssetEvent(AssetEvent.Cause.CREATE, asset, null);
        publishResponse(topic, event);
    }

    /**
     * Update asset request topic handler
     * Updates the asset with the provided template and merges it in the asset storage service.
     */
    protected void handleUpdateAsset(RemotingConnection connection, Topic topic, ByteBuf body) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        Asset<?> storageAsset = assetStorageService.find(assetId);

        var asset = ValueUtil.parse(body.toString(StandardCharsets.UTF_8), Asset.class);
        if (asset.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Invalid asset template");
            LOG.fine("Invalid asset template " + body.toString(StandardCharsets.UTF_8) + " in update asset request " + getConnectionIDString(connection));
            return;
        }

        storageAsset.setName(asset.get().getName());
        storageAsset.setAttributes(asset.get().getAttributes());
        assetStorageService.merge(storageAsset);

        AssetEvent event = new AssetEvent(AssetEvent.Cause.UPDATE, storageAsset, null);
        publishResponse(topic, event);
    }

    /**
     * Get asset request topic handler
     * Publishes the asset object based on the request.
     * If the asset is not found, an error response is published.
     */
    protected void handleGetAsset(RemotingConnection connection, Topic topic, ByteBuf body) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        Asset<?> asset = assetStorageService.find(assetId);
        if (asset == null) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.NOT_FOUND, "Asset not found");
            LOG.fine("Asset not found " + assetId + " " + getConnectionIDString(connection));
            return;
        }

        publishResponse(topic, asset);
    }


    /**
     * Delete asset request topic handler
     * Deletes the asset and all its descendants from the asset storage service.
     * If the asset is not found, an error response is published.
     */
    protected void handleDeleteAsset(RemotingConnection connection, Topic topic, ByteBuf body) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        Asset<?> asset = assetStorageService.find(assetId);
        if (asset == null) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.NOT_FOUND, "Asset not found");
            LOG.fine("Asset not found " + assetId + " " + getConnectionIDString(connection));
            return;
        }

        List<String> assetsToDelete = new ArrayList<>(assetStorageService.findAll(
                new AssetQuery()
                        .select(new AssetQuery.Select().excludeAttributes())
                        .recursive(true)
                        .parents(asset.getId())).stream().map(Asset::getId).toList(
        ));
        assetsToDelete.add(asset.getId()); // ensure the parent is also included in the deletion
        assetStorageService.delete(assetsToDelete);

        AssetEvent event = new AssetEvent(AssetEvent.Cause.DELETE, asset, null);
        publishResponse(topic, event);
    }

    /**
     * Update attribute request topic handler
     * Updates the attribute with the provided value and publishes the attribute event.
     */
    protected void handleUpdateAttribute(RemotingConnection connection, Topic topic, ByteBuf body) {
        String realm = topicRealm(topic);
        AttributeEvent event = buildAttributeEvent(topic.getTokens(), body.toString(StandardCharsets.UTF_8));
        sendAttributeEvent(event);
        publishResponse(topic, event);
    }


    /**
     * Update multiple attributes request topic handler.
     * Includes authorization checks for each attribute.
     * Request is cancelled if any attribute is unauthorized.
     */
    @SuppressWarnings({"unchecked"})
    protected void handleUpdateMultiAttribute(RemotingConnection connection, Topic topic, ByteBuf body) {
        String realm = topicRealm(topic);
        var topicTokens = topic.getTokens();
        var attributes = ValueUtil.parse(body.toString(StandardCharsets.UTF_8));
        var authContext = getAuthContextFromConnection(connection);

        if (authContext.isEmpty()) {
            return;
        }

        // Check if the attributes are valid
        if (attributes.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Invalid data provided");
            LOG.fine("Invalid attribute template " + body.toString(StandardCharsets.UTF_8) + " in update attribute request " + getConnectionIDString(connection));
            return;
        }

        var attributeMap = (Map<String, Object>) attributes.get();
        var assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        var isAuthorized = true;

        // check each event for authorization (clientEventService)
        for (var entry : attributeMap.entrySet()) {
            var attributeName = entry.getKey();
            var attributeValue = entry.getValue();
            var event = new AttributeEvent(assetId, attributeName, attributeValue);

            // Check if the user is authorized to write the attribute
            if (!clientEventService.authorizeEventWrite(realm, authContext.get(), event)) {
                isAuthorized = false;
                break;
            }
        }

        // If the user is not authorized to update any of the attributes, cancel the request
        if (!isAuthorized) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.UNAUTHORIZED, "Unauthorized to update attributes");
            return;
        }

        // Temporary list to store the events for the response
        List<AttributeEvent> events = new ArrayList<>();
        // Send the attribute events
        for (var entry : attributeMap.entrySet()) {
            var attributeName = entry.getKey();
            var attributeValue = entry.getValue();
            var event = new AttributeEvent(assetId, attributeName, attributeValue);
            event.setSource(GatewayMQTTHandler.class.getSimpleName());
            sendAttributeEvent(event);
            events.add(event);
        }
        publishResponse(topic, events);
    }

    /**
     * Get attribute request topic handler
     * Publishes the attribute value or the full attribute object based on the request. (get or get-value)
     * If the attribute is not found, an error response is published.
     */
    protected void handleGetAttribute(RemotingConnection connection, Topic topic, ByteBuf body, boolean publishValueOnly) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        String attributeName = topicTokenIndexToString(topic, ATTRIBUTE_NAME_TOKEN_INDEX);
        Optional<Attribute<Object>> attribute = assetStorageService.find(assetId).getAttribute(attributeName);

        if (attribute.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.NOT_FOUND, "Attribute not found");
            LOG.fine("Attribute not found " + assetId + " " + attributeName + " " + getConnectionIDString(connection));
            return;
        }

        if (publishValueOnly && attribute.get().getValue().isPresent()) {
            publishResponse(topic, attribute.get().getValue().get());
        } else {
            publishResponse(topic, attribute.get());
        }
    }

    /**
     * Handle gateway events acknowledgement
     * Acknowledges the gateway event and publishes the intercepted attribute event.
     */
    protected void handleGatewayEventAcknowledgement(RemotingConnection connection, Topic topic) {
        String ackId = topicTokenIndexToString(topic, GATEWAY_ACK_ID_TOKEN_INDEX);
        if (ackId == null) {
            LOG.fine("Invalid acknowledgement id " + getConnectionIDString(connection));
            return;
        }

        SharedEvent event = eventsPendingGatewayAcknowledgement.getIfPresent(ackId);
        if (event == null) {
            LOG.fine("Gateway event not found for the provided acknowledgement id " + ackId);
            return;
        }

        if (event instanceof AttributeEvent) {
            // We set the source to the MQTT handler to prevent the event from being intercepted again
            ((AttributeEvent) event).setSource(GatewayMQTTHandler.class.getSimpleName());
            sendAttributeEvent((AttributeEvent) event);
            synchronized (eventsPendingGatewayAcknowledgement) {
                eventsPendingGatewayAcknowledgement.invalidate(ackId);
            }
        }
    }

    protected boolean topicHasValidAssetId(Topic topic) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        if (assetId == null) {
            return false;
        }
        return Pattern.matches(ASSET_ID_REGEXP, assetId);
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean authorizeGatewayEvent(GatewayV2Asset gateway, AuthContext authContext, SharedEvent event) {
        // check whether the Asset associated with the read event is a descendant of the gateway
        if (event instanceof ReadAssetEvent readEvent) {
            return isAssetDescendantOfGateway(readEvent.getAssetId(), gateway);
        }
        // check whether the Asset associated with the event is a descendant of the gateway
        else if (event instanceof AssetEvent assetEvent) {
            // create events allowed by default for the gateway, we don't know the assetId yet.
            if (assetEvent.getCause() == AssetEvent.Cause.CREATE) {
                return true;
            }
            return isAssetDescendantOfGateway(assetEvent.getId(), gateway);
        }
        // check whether the asset associated with the read attribute event is a descendant of the gateway
        else if (event instanceof ReadAttributeEvent readAttributeEvent) {
            Asset<?> asset = assetStorageService.find(readAttributeEvent.getAttributeRef().getId());
            if (asset == null) {
                return false;
            }
            return isAssetDescendantOfGateway(asset.getId(), gateway);

        }
        // check whether the asset associated with the attribute event is a descendant of the gateway
        else if (event instanceof AttributeEvent attributeEvent) {
            Asset<?> asset = assetStorageService.find(attributeEvent.getId());
            if (asset == null) {
                return false;
            }
            return isAssetDescendantOfGateway(asset.getId(), gateway);

        }
        return false;
    }

    protected Optional<GatewayV2Asset> findGatewayFromConnection(RemotingConnection connection) {
        if (isGatewayConnection(connection)) {
            return Optional.ofNullable((GatewayV2Asset) assetStorageService.find(new AssetQuery().types(GatewayV2Asset.class)
                    .attributeValue("clientId", connection.getClientID())));
        }
        return Optional.empty();
    }

    protected boolean isAssetDescendantOfGateway(String assetId, GatewayV2Asset gatewayAsset) {
        return assetStorageService.isDescendantAsset(gatewayAsset.getId(), assetId);
    }

    protected void updateGatewayStatus(GatewayV2Asset gatewayAsset, ConnectionStatus status) {
        sendAttributeEvent(new AttributeEvent(gatewayAsset.getId(), GatewayV2Asset.STATUS, status));
    }

    protected void publishErrorResponse(Topic topic, MQTTErrorResponse.Error error, String message) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), new MQTTErrorResponse(error, message), MqttQoS.AT_MOST_ONCE);
    }

    protected void publishResponse(Topic topic, Object response) {
        mqttBrokerService.publishMessage(getResponseTopic(topic), response, MqttQoS.AT_MOST_ONCE);
    }

    protected void publishPendingGatewayEvent(SharedEvent event, GatewayV2Asset gateway) {
        String eventKey = UniqueIdentifierGenerator.generateId();
        String topic = String.join("/", gateway.getRealm(), gateway.getClientId().get(), GATEWAY_TOPIC, GATEWAY_EVENTS_TOPIC, GATEWAY_PENDING_TOPIC, eventKey);
        mqttBrokerService.publishMessage(topic, event, MqttQoS.AT_LEAST_ONCE);

        synchronized (eventsPendingGatewayAcknowledgement) {
            eventsPendingGatewayAcknowledgement.put(eventKey, event);
        }
    }

    protected void sendAttributeEvent(AttributeEvent event) {
        Map<String, Object> headers = prepareHeaders(event.getRealm(), null);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .withBody(event)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncRequest();
    }

    protected AttributeEvent buildAttributeEvent(List<String> topicTokens, Object value) {
        String attributeName = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX);
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        return new AttributeEvent(assetId, attributeName, value).setSource(GatewayMQTTHandler.class.getSimpleName());
    }

    protected ReadAttributeEvent buildReadAttributeEvent(List<String> topicTokens) {
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        String attributeName = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX);
        return new ReadAttributeEvent(assetId, attributeName);
    }

    protected AssetEvent buildAssetEvent(List<String> topicTokens, AssetEvent.Cause cause) {
        if (cause == AssetEvent.Cause.CREATE) {
            return new AssetEvent(cause, null, null);
        }

        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        if (!Pattern.matches(ASSET_ID_REGEXP, assetId)) {
            LOG.fine("Failed to build AssetEvent, invalid asset id " + assetId);
            return null;
        }

        Asset<?> asset = assetStorageService.find(assetId);
        if (asset == null) {
            LOG.fine("Failed to build AssetEvent, Asset not found " + assetId);
            return null;
        }
        return new AssetEvent(cause, asset, null);
    }

    protected ReadAssetEvent buildReadAssetEvent(List<String> topicTokens) {
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        return new ReadAssetEvent(assetId);
    }


    public static boolean isAssetsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, ASSETS_TOKEN_INDEX), ASSETS_TOPIC)
                && !isAttributesTopic(topic); // it is considered an asset topic as long it does not contain attributes
    }

    protected static boolean isAssetsMethodTopic(Topic topic) {
        return isAssetsTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), CREATE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), DELETE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), UPDATE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN), GET_TOPIC);
    }

    protected static boolean isAttributesMethodTopic(Topic topic) {
        return isAttributesTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX), UPDATE_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX), GET_TOPIC) ||
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX), GET_VALUE_TOPIC);
    }

    protected static boolean isMultiAttributeUpdateTopic(Topic topic) {
        return isAttributesTopic(topic) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTE_NAME_TOKEN_INDEX), UPDATE_TOPIC) &&
                Objects.equals(topicTokenIndexToString(topic, topic.getTokens().size() - 1), UPDATE_TOPIC);
    }

    protected static boolean isAttributesTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, ASSETS_TOKEN_INDEX), ASSETS_TOPIC) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_TOKEN_INDEX), ATTRIBUTES_TOPIC);
    }

    protected static boolean isOperationsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, OPERATIONS_TOKEN_INDEX), OPERATIONS_TOPIC);
    }

    protected static boolean isOperationResponseTopic(Topic topic) {
        return isOperationsTopic(topic) && Objects.equals(topicTokenIndexToString(topic, topic.getTokens().size() - 1), RESPONSE_TOPIC);
    }

    protected static boolean isEventsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, EVENTS_TOKEN_INDEX), EVENTS_TOPIC);
    }

    protected static boolean isGatewayEventsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, GATEWAY_TOKEN_INDEX), GATEWAY_TOPIC) && Objects.equals(topicTokenIndexToString(topic, GATEWAY_EVENTS_TOKEN_INDEX), GATEWAY_EVENTS_TOPIC);
    }


    public static String getResponseTopic(Topic topic) {
        return topic.toString() + "/" + RESPONSE_TOPIC;
    }

    protected static boolean isGatewayConnection(RemotingConnection connection) {
        return connection.getClientID().startsWith(GATEWAY_CLIENT_ID_PREFIX);
    }


    protected boolean isCacheAuthorized(String cacheKey, Topic topic) {
        ConcurrentHashSet<String> set = authorizationCache.getIfPresent(cacheKey);
        return set != null && set.contains(topic.getString());
    }

    protected static Map<String, Object> prepareHeaders(String requestRealm, RemotingConnection connection) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(SESSION_KEY, getConnectionIDString(connection));
        headers.put(HEADER_CONNECTION_TYPE, ClientEventService.HEADER_CONNECTION_TYPE_MQTT);
        headers.put(REALM_PARAM_NAME, requestRealm);
        return headers;
    }


}


