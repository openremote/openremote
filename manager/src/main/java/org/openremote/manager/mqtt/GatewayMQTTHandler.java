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
import jakarta.validation.ConstraintViolationException;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.security.AuthContext;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.gateway.GatewayV2Service;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.GatewayV2Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.mqtt.MQTTErrorResponse;
import org.openremote.model.mqtt.MQTTGatewayEventMessage;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.openremote.manager.asset.AssetProcessingService.ATTRIBUTE_EVENT_PROCESSOR;
import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString;
import static org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler.UNIQUE_ID_PLACEHOLDER;
import static org.openremote.model.Constants.*;
import static org.openremote.model.syslog.SyslogCategory.API;


/**
 * MQTT Gateway API Handler
 * Exposes CRUD operations for assets and attributes, and event subscriptions for assets and attributes
 * Can be used by service users to interact with the manager via MQTT
 * Gateway V2 Asset service users are restricted to their own descendants and need to acknowledge attribute events from the manager
 */
@SuppressWarnings("unused, rawtypes")
public class GatewayMQTTHandler extends MQTTHandler {

    // main topics
    public static final String EVENTS_TOPIC = "events"; 
    public static final String OPERATIONS_TOPIC = "operations"; 
    public static final String RESPONSE_TOPIC = "response"; 

    // hierarchy topics
    public static final String ATTRIBUTES_TOPIC = "attributes";
    public static final String ATTRIBUTES_VALUE_TOPIC = "attributes-value"; 
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
    public static final String GATEWAY_ACK_TOPIC = "acknowledge";

    // index tokens
    public static final int GATEWAY_TOKEN_INDEX = 2;
    public static final int OPERATIONS_TOKEN_INDEX = 2;
    public static final int EVENTS_TOKEN_INDEX = 2;
    public static final int GATEWAY_EVENTS_TOKEN_INDEX = 3;
    public static final int ASSETS_TOKEN_INDEX = 3;
    public static final int GATEWAY_PENDING_TOKEN_INDEX = 4;
    public static final int GATEWAY_ACKNOWLEDGE_TOKEN_INDEX = 4;
    public static final int ASSET_ID_TOKEN_INDEX = 4;
    public static final int RESPONSE_ID_TOKEN_INDEX = 4;
    public static final int ASSETS_METHOD_TOKEN = 5;
    public static final int ATTRIBUTES_TOKEN_INDEX = 5;
    public static final int ATTRIBUTE_NAME_TOKEN_INDEX = 6;
    public static final int ATTRIBUTES_METHOD_TOKEN_INDEX = 7;

    // topic length
    public static final int MIN_LENGTH_ATTRIBUTES_EVENTS_TOPIC = 6;
    public static final int MIN_LENGTH_ASSETS_EVENTS_TOPIC = 4;

    protected static final Logger LOG = SyslogCategory.getLogger(API, GatewayMQTTHandler.class);
    protected AssetStorageService assetStorageService;

    protected GatewayMQTTSubscriptionManager subscriptionManager;
    protected GatewayV2Service gatewayV2Service;
    protected AssetProcessingService assetProcessingService;


    @Override
    public void init(Container container, Configuration serverConfiguration) throws Exception {
        super.init(container, serverConfiguration);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.gatewayV2Service = container.getService(GatewayV2Service.class);
        this.assetProcessingService = container.getService(AssetProcessingService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.subscriptionManager = new GatewayMQTTSubscriptionManager(this);

        assetProcessingService.addEventInterceptor(this::onAttributeEventIntercepted);
    }

    // Temporarily holds events that require gateway acknowledgement
    protected final Cache<String, AttributeEvent> eventsPendingGatewayAcknowledgement = CacheBuilder.newBuilder()
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


    /**
     * Intercepts gateway asset descendant attribute events and publishes them to the pending gateway events topic.
     *
     * @param em the entity manager
     * @param event the attribute event to intercept
     * @return true if the event was intercepted and published, false otherwise
     * @throws AssetProcessingException if an error occurs during processing
     */
    public boolean onAttributeEventIntercepted(EntityManager em, AttributeEvent event) throws AssetProcessingException {
        if (event.getSource() != null && event.getSource().equals(GatewayMQTTHandler.class.getSimpleName())) {
            LOG.finest("Intercepted attribute event from self: " + event.getId());
            return false;
        }

        String gatewayId = gatewayV2Service.getRegisteredGatewayId(event.getId(), event.getParentId());
        // If the event is from a descendant of a Gateway asset, then publish to pending gateway events topic
        if (gatewayId != null) {
            Asset<?> gateway = assetStorageService.find(gatewayId);
            if (gateway instanceof GatewayV2Asset gatewayAsset) {
                LOG.finest("Intercepted attribute event from Gateway asset descendant: " + gateway.getId());
                publishPendingGatewayEvent(event, gatewayAsset);
                return true;
            }
        }
        LOG.finest("Intercepted attribute event from non-gateway asset descendant: " + event.getId());
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
    public Set<String> getPublishListenerTopics() {
        //<realm>/<clientId>/
        String topicBase = TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/";
        return Set.of(
                // <realm>/<clientId>/gateway/events/acknowledge
                topicBase + GATEWAY_TOPIC + "/" + GATEWAY_EVENTS_TOPIC + "/" + GATEWAY_ACK_TOPIC,
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
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/update
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + UPDATE_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/get
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GET_TOPIC,
                // <realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/get-value
                topicBase + OPERATIONS_TOPIC + "/" + ASSETS_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + ATTRIBUTES_TOPIC + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + GET_VALUE_TOPIC
        );
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

        if (!isEventsTopic(topic) && !isOperationResponseTopic(topic) && !isGatewayEventsTopic(topic)) {
            LOG.finest("Invalid topic " + topic + " for subscribing");
            return false;
        }

        GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);
        if (gatewayAsset != null && gatewayAsset.getDisabled().orElse(false)) {
            LOG.finer("Subscription not allowed, gateway is disabled " + getConnectionIDString(connection));
            return false;
        }

        List<String> topicTokens = topic.getTokens();

        if (isEventsTopic(topic)) {
            // topics above 4 tokens require the assets token to be present
            if (topicTokens.size() > MIN_LENGTH_ASSETS_EVENTS_TOPIC && !topicTokens.get(ASSETS_TOKEN_INDEX).equals(ASSETS_TOPIC)) {
                LOG.finest("Invalid topic " + topic + " for subscribing, the assets token is missing");
                return false;
            }
            // topics above 6 tokens require the attributes token to be present
            if (topicTokens.size() > MIN_LENGTH_ATTRIBUTES_EVENTS_TOPIC 
                && (!topicTokens.get(ATTRIBUTES_TOKEN_INDEX).equals(ATTRIBUTES_TOPIC) && !topicTokens.get(ATTRIBUTES_TOKEN_INDEX).equals(ATTRIBUTES_VALUE_TOPIC))) {
                LOG.finest("Invalid topic " + topic + " for subscribing, the attributes token is missing");
                return false;
            }

            if (gatewayAsset != null) {
                String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
                if (assetId == null || !Pattern.matches(ASSET_ID_REGEXP, assetId)) {
                    return false;
                }
                // Check if the asset is a descendant of the gateway asset
                if (!gatewayV2Service.isGatewayDescendant(assetId, gatewayAsset.getId())) {
                    LOG.finest("Subscription not allowed, asset is not descendant of gateway " + assetId + " " + getConnectionIDString(connection));
                    return false;
                }
            }

            // build the asset filter from the topic, the gateway asset can be null
            AssetFilter<?> filter = GatewayMQTTSubscriptionManager.buildAssetFilter(topic, gatewayAsset);
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
        // Pending events for gateway acknowledgement, requires the gateway asset to be present
        else if (isGatewayEventsTopic(topic) && gatewayAsset != null) {
            if (topicTokens.size() != 5 || !topicTokens.get(GATEWAY_PENDING_TOKEN_INDEX).equals(GATEWAY_PENDING_TOPIC)) {
                LOG.finest("Invalid topic " + topic + " for subscribing, ensure the topic structure is correct");
                return false;
            }
        }
        // Operation response topic, requires the operations and response tokens to be present
        else {
            return isOperationResponseTopic(topic);
        }

        return true;
    }

    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
        if (isEventsTopic(topic)) {
            GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);
            var eventClass = isAssetsTopic(topic) ? AssetEvent.class : AttributeEvent.class;
            subscriptionManager.addSubscription(connection, topic, eventClass, gatewayAsset);
        }
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        if (isEventsTopic(topic)) {
            subscriptionManager.removeSubscription(connection, topic);
        }
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
            LOG.finest("Invalid topic provided " + topic + " for publishing");
            return false;
        }

        GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);
        if (gatewayAsset != null && gatewayAsset.getDisabled().orElse(false)) { // check if we have a gateway asset and it is disabled
            LOG.finer("Gateway is disabled " + getConnectionIDString(connection));
            return false;
        }

        String connectionID = getConnectionIDString(connection);

        // Check if user has been authorized for the topic before (cache, 30s expiration)
        boolean connectionIdIsPresent = authorizationCache.getIfPresent(connectionID) != null;
        boolean topicIsAuthorized = connectionIdIsPresent && authorizationCache.getIfPresent(connectionID).contains(topic.getString());

        if (connectionIdIsPresent && topicIsAuthorized) {
            return true;
        }

        List<String> topicTokens = topic.getTokens();

        // Gateway event acknowledgement (/gateway/events/acknowledge)
        if (isGatewayEventsTopic(topic) && gatewayAsset != null) {
            if (topicTokens.size() != 5 || !topicTokens.get(GATEWAY_ACKNOWLEDGE_TOKEN_INDEX).equals(GATEWAY_ACK_TOPIC)) {
                LOG.fine("Invalid topic " + topic + " for publishing, the acknowledge topic is missing");
                return false;
            }
        }
        // Asset operations authorization (/operations/assets/...)
        else if (isAssetsMethodTopic(topic)) {
            if (!authorizeAssetOperation(topic, authContext, gatewayAsset)) {
                LOG.info("Asset operation not authorized " + topic + " " + connectionID);
                return false;
            }
        }
        // Attribute operations (/operations/attributes/...)
        else if (isAttributesMethodTopic(topic)) {
            if (!authorizeAttributeOperation(topic, authContext, gatewayAsset)) {
                LOG.info("Attribute operation not authorized " + topic + " " + connectionID);
                return false;
            }
        }
        // Multi attribute update operation (/operations/assets/.../attributes/update)
        else if (isMultiAttributeUpdateTopic(topic)) {
            LOG.finest("Multi attribute update request " + topic + " " + connectionID);
        } else {
            return false;
        }

        ConcurrentHashSet<String> set;
        // Note: is the synchronization necessary? Guave indicates that its get, put and invalidate are atomic
        synchronized (authorizationCache) {
            ConcurrentHashSet<String> act = authorizationCache.getIfPresent(connectionID);
            if (act != null) {
                set = act;
            } else {
                set = new ConcurrentHashSet<>();
                authorizationCache.put(connectionID, set);
            }
        }
        set.add(topic.getString());

        return true;
    }

    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        // Asset operations
        if (isAssetsMethodTopic(topic)) {
            String method = topicTokenIndexToString(topic, ASSETS_METHOD_TOKEN);
            switch (Objects.requireNonNull(method)) {
                case CREATE_TOPIC -> handleCreateAsset(connection, topic, body);
                case GET_TOPIC -> handleGetAsset(connection, topic);
                case UPDATE_TOPIC -> handleUpdateAsset(connection, topic, body);
                case DELETE_TOPIC -> handleDeleteAsset(connection, topic);
                default -> LOG.warning("Unexpected asset operation method: " + Objects.requireNonNull(method));
            }
        }

        // Attribute operations
        if (isAttributesMethodTopic(topic)) {
            String method = topicTokenIndexToString(topic, ATTRIBUTES_METHOD_TOKEN_INDEX);
            switch (Objects.requireNonNull(method)) {
                case GET_TOPIC -> handleGetAttribute(connection, topic, false);
                case GET_VALUE_TOPIC -> handleGetAttribute(connection, topic, true);
                case UPDATE_TOPIC -> handleUpdateAttribute(topic, body);
                default -> LOG.warning("Unexpected attribute operation  method: " + Objects.requireNonNull(method));
            }
        }

        // Multi attribute operation (authorization in topic handler)
        if (isMultiAttributeUpdateTopic(topic)) {
            handleUpdateMultiAttribute(connection, topic, body);
        }

        // Handle gateway events acknowledgement
        if (isGatewayEventsTopic(topic)) {
            handleGatewayEventAcknowledgement(connection, body);
        }
    }

    @Override
    public void onConnectionLost(RemotingConnection connection) {
        handleDisconnect(connection);
    }

    @Override
    public void onDisconnect(RemotingConnection connection) {
        handleDisconnect(connection);
    }

    @Override
    public void onConnect(RemotingConnection connection) {
        super.onConnect(connection);
        GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);
        if (gatewayAsset != null) {
            if (gatewayAsset.getDisabled().orElse(false)) {
                LOG.fine("Gateway is disabled " + getConnectionIDString(connection));
                mqttBrokerService.doForceDisconnect(connection);
                return;
            }
            updateGatewayStatus(gatewayAsset, ConnectionStatus.CONNECTED);
        }


    }

    /**
     * Handles the disconnection of a client, updating the gateway status and removing subscriptions.
     *
     * @param connection the connection that was lost
     */
    protected void handleDisconnect(RemotingConnection connection) {
        
        GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);
        if (gatewayAsset != null) {
            updateGatewayStatus(gatewayAsset, ConnectionStatus.DISCONNECTED);
        }

        subscriptionManager.removeAllSubscriptions(connection);
        authorizationCache.invalidate(getConnectionIDString(connection));
    }

    /**
     * Authorizes a published asset operation (CRUD) based on the topic structure.
     *
     * @param topic the topic associated with the request
     * @param authContext the authentication context of the user
     * @param gatewayAsset the gateway asset for authorization context
     * @return true if the operation is authorized, false otherwise
     */
    protected boolean authorizeAssetOperation(Topic topic, AuthContext authContext, GatewayV2Asset gatewayAsset) {
        List<String> topicTokens = topic.getTokens();
        String method = topicTokens.get(ASSETS_METHOD_TOKEN);
        SharedEvent event;

        switch (method) {
            case CREATE_TOPIC -> event = buildAssetEvent(topicTokens, AssetEvent.Cause.CREATE);
            case GET_TOPIC -> event = buildReadAssetEvent(topicTokens);
            case UPDATE_TOPIC -> event = buildAssetEvent(topicTokens, AssetEvent.Cause.UPDATE);
            case DELETE_TOPIC -> event = buildAssetEvent(topicTokens, AssetEvent.Cause.DELETE);
            default -> {
                // Invalid method
                return false;
            }
        }

        if (event == null) {
            return false;
        }

        if (event instanceof AssetEvent assetEvent && assetEvent.getCause() != AssetEvent.Cause.CREATE) {
            if (gatewayAsset != null) {
                // ensure a gateway user cannot update/delete an asset that is not a descendant of the gateway
                if (!gatewayV2Service.isGatewayDescendant(assetEvent.getId(), gatewayAsset.getId())) {
                    LOG.fine(method + " asset request was not authorised for this gateway user and topic: topic=" + topic);
                    return false;
                }
            } else {
                if (gatewayV2Service.isGatewayDescendant(assetEvent.getId())) {
                    LOG.fine(method + " cannot modify the structure of gateway related assets as a non-gateway user, topic: topic=" + topic);
                    return false;
                }
            }
        }

        // perform the event write authorization check
        if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
            LOG.fine(method + " asset request was not authorised for this user and topic: topic=" + topic);
            return false;
        }

        return true;
    }


    /**
     * Authorizes a published attribute operation based on the topic structure.
     *
     * @param topic the topic associated with the request
     * @param authContext the authentication context of the user
     * @param gatewayAsset the gateway asset for authorization context
     * @return true if the operation is authorized, false otherwise
     */
    protected boolean authorizeAttributeOperation(Topic topic, AuthContext authContext, GatewayV2Asset gatewayAsset) {
        List<String> topicTokens = topic.getTokens();
        String method = topicTokens.get(ATTRIBUTES_METHOD_TOKEN_INDEX);
        SharedEvent event;
        switch (Objects.requireNonNull(method)) {
            case GET_TOPIC, GET_VALUE_TOPIC -> event = buildReadAttributeEvent(topicTokens);
            case UPDATE_TOPIC -> event = buildAttributeEvent(topicTokens, null);
            default -> {
                // Invalid method
                return false;
            }
        }

        if (event == null) {
            return false;
        }

        if (event instanceof AttributeEvent attributeEvent && gatewayAsset != null 
            && !gatewayV2Service.isGatewayDescendant(attributeEvent.getId(), gatewayAsset.getId())) {
                LOG.fine(method + " attribute request was not authorised for this gateway user and topic: topic=" + topic);
                return false;
            }
        

        if (event instanceof ReadAttributeEvent readAttributeEvent && gatewayAsset != null 
            && !gatewayV2Service.isGatewayDescendant(readAttributeEvent.getAttributeRef().getId(), gatewayAsset.getId())) {
                LOG.fine(method + " attribute request was not authorised for this gateway user and topic: topic=" + topic);
                return false;
            }
        

        if (!clientEventService.authorizeEventWrite(topicRealm(topic), authContext, event)) {
            LOG.fine(method + " attribute request was not authorised for this user and topic: topic=" + topic);
            return false;
        }

        return true;
    }

    /**
     * Handles the creation of an asset based on the provided topic and message body.
     *
     * @param connection the connection from which the request originated
     * @param topic the topic associated with the request
     * @param body the message body containing the asset data
     */
    protected void handleCreateAsset(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);
        String realm = topicRealm(topic);

        String assetTemplate = payloadContent;
        assetTemplate = assetTemplate.replaceAll(UNIQUE_ID_PLACEHOLDER, UniqueIdentifierGenerator.generateId());

        var optionalAsset = ValueUtil.parse(assetTemplate, Asset.class);
        if (optionalAsset.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Invalid asset template");
            LOG.info("Bad request, invalid asset template " + payloadContent + " in create asset request " + getConnectionIDString(connection));
            return;
        }
        Asset<?> asset = optionalAsset.get();
        // check for asset ID conflict
        if (asset.getId() != null && !asset.getId().isEmpty()) {
            Asset<?> existingAsset = assetStorageService.find(asset.getId());
            if (existingAsset != null && existingAsset.getRealm().equals(realm)) {
                publishErrorResponse(topic, MQTTErrorResponse.Error.CONFLICT, "Asset ID already exists");
                LOG.info("Conflict, asset ID already exists " + asset.getId() + " " + getConnectionIDString(connection));
                return;
            }
        } else {
            asset.setId(UniqueIdentifierGenerator.generateId());
            asset.setRealm(realm);
        }

        GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);

        // Check if the provided parent is allowed for the asset
        if (asset.getParentId() != null && !authorizeParentId(asset, gatewayAsset))
        {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Bad request, the asset cannot be created with the provided parent ID");
            LOG.info("The asset cannot be created with the provided parent ID " + getConnectionIDString(connection));
            return;
        }

        try {
            if (gatewayAsset != null) {
                if (asset.getParentId() == null) {
                    asset.setParentId(gatewayAsset.getId());
                }
                assetStorageService.merge(asset, true, true, null);
            } else {
                assetStorageService.merge(asset);
            }
        } catch (ConstraintViolationException e) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Asset validation failed: " + e.getMessage());
            LOG.warning("Asset validation failed: " + e.getMessage() + " " + getConnectionIDString(connection));
            return;
        } catch (Exception e) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.INTERNAL_SERVER_ERROR, "Failed to process asset");
            LOG.warning("Failed to merge asset " + asset + " " + getConnectionIDString(connection));
            return;
        }

        AssetEvent event = new AssetEvent(AssetEvent.Cause.CREATE, asset, null);
        publishResponse(topic, event);
    }

    /**
     * Handles the update of an asset based on the provided topic and message body.
     *
     * @param connection the connection from which the request originated
     * @param topic the topic associated with the request
     * @param body the message body containing the updated asset data
     */
    protected void handleUpdateAsset(RemotingConnection connection, Topic topic, ByteBuf body) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        Asset<?> storageAsset = assetStorageService.find(assetId);

        var asset = ValueUtil.parse(body.toString(StandardCharsets.UTF_8), Asset.class);
        if (asset.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Invalid asset template");
            LOG.info("Bad request, Invalid asset template " + body.toString(StandardCharsets.UTF_8) + " in update asset request " + getConnectionIDString(connection));
            return;
        }

        boolean hasParentId = asset.get().getParentId() != null;
        boolean parentIdChanged = hasParentId && !Objects.equals(storageAsset.getParentId(), asset.get().getParentId());

        GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);
        storageAsset.setName(asset.get().getName());
        storageAsset.setAttributes(asset.get().getAttributes());
        storageAsset.setParentId(asset.get().getParentId());


        // check whether the parent ID is allowed for the asset
        if (parentIdChanged && !authorizeParentId(storageAsset, gatewayAsset))
        {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Bad request, the asset cannot be updated with the provided parent ID");
            LOG.info("Asset cannot be updated with the provided parent ID " + getConnectionIDString(connection));
            return;
        }

        try {
            if (gatewayAsset != null) {
                assetStorageService.merge(storageAsset, true, true, null);
            } else {
                assetStorageService.merge(storageAsset);
            }
        } catch (ConstraintViolationException e) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Asset validation failed: " + e.getMessage());
            LOG.warning("Asset validation failed: " + e.getMessage() + " " + getConnectionIDString(connection));
            return;
        } catch (Exception e) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.INTERNAL_SERVER_ERROR, "Failed to process asset");
            LOG.warning("Failed to update asset " + asset + " " + getConnectionIDString(connection));
            return;
        }

        AssetEvent event = new AssetEvent(AssetEvent.Cause.UPDATE, storageAsset, null);
        publishResponse(topic, event);
    }

    /**
     * Handles the retrieval of an asset based on the provided topic.
     *
     * @param connection the connection from which the request originated
     * @param topic the topic associated with the request
     */
    protected void handleGetAsset(RemotingConnection connection, Topic topic) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        Asset<?> asset = assetStorageService.find(assetId);

        if (asset == null) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.NOT_FOUND, "Asset not found");
            LOG.info("Asset not found " + assetId + " " + getConnectionIDString(connection));
            return;
        }

        publishResponse(topic, asset);
    }

    /**
     * Handles the deletion of an asset based on the provided topic.
     *
     * @param connection the connection from which the request originated
     * @param topic the topic associated with the request
     */
    protected void handleDeleteAsset(RemotingConnection connection, Topic topic) {
        String assetId = topicTokenIndexToString(topic, ASSET_ID_TOKEN_INDEX);
        Asset<?> asset = assetStorageService.find(assetId);
        if (asset == null) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.NOT_FOUND, "Asset not found");
            LOG.info("Asset not found " + assetId + " " + getConnectionIDString(connection));
            return;
        }

        List<String> assetsToDelete = new ArrayList<>(assetStorageService.findAll(
                new AssetQuery()
                        .select(new AssetQuery.Select().excludeAttributes())
                        .recursive(true)
                        .parents(asset.getId())).stream().map(Asset::getId).toList(
        ));
        assetsToDelete.add(asset.getId()); // ensure the parent is also included in the deletion

        try {
            GatewayV2Asset gatewayAsset = gatewayV2Service.getGatewayFromMQTTConnection(connection);
            if (gatewayAsset != null) {
                assetStorageService.delete(assetsToDelete, true);
            } else {
                assetStorageService.delete(assetsToDelete);
            }
        } catch (Exception e) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.INTERNAL_SERVER_ERROR, "Failed to delete asset");
            LOG.warning("Failed to delete asset " + asset + " " + getConnectionIDString(connection));
            return;
        }

        assetStorageService.delete(assetsToDelete);

        AssetEvent event = new AssetEvent(AssetEvent.Cause.DELETE, asset, null);
        publishResponse(topic, event);
    }

    /**
     * Handles the update of an attribute based on the provided topic and message body.
     *
     * @param topic the topic associated with the request
     * @param body the message body containing the updated attribute data
     */
    protected void handleUpdateAttribute(Topic topic, ByteBuf body) {
        AttributeEvent event = buildAttributeEvent(topic.getTokens(), body.toString(StandardCharsets.UTF_8));
        sendAttributeEvent(event);
        publishResponse(topic, event);
    }

    /**
     * Handles the update of multiple attributes, including authorization checks for each attribute.
     *
     * @param connection the connection from which the request originated
     * @param topic the topic associated with the request
     * @param body the message body containing the attributes data
     */
    @SuppressWarnings({"unchecked"})
    protected void handleUpdateMultiAttribute(RemotingConnection connection, Topic topic, ByteBuf body) {
        String realm = topicRealm(topic);
        List<String> topicTokens = topic.getTokens();
        Optional<Object> attributes = ValueUtil.parse(body.toString(StandardCharsets.UTF_8));
        Optional<AuthContext> authContext = getAuthContextFromConnection(connection);

        if (authContext.isEmpty()) {
            return;
        }

        // Check if the attributes are valid
        if (attributes.isEmpty()) {
            publishErrorResponse(topic, MQTTErrorResponse.Error.BAD_REQUEST, "Invalid data provided");
            LOG.info("Bad request, invalid attribute template " + body.toString(StandardCharsets.UTF_8) + " in update attribute request " + getConnectionIDString(connection));
            return;
        }
        Map<String, Object> attributeMap = (Map<String, Object>) attributes.get();
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        GatewayV2Asset gateway = gatewayV2Service.getGatewayFromMQTTConnection(connection);

        for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();
            AttributeEvent event = new AttributeEvent(assetId, attributeName, attributeValue);

            // Gateways cannot modify anything besides their own descendants
            if (gateway != null && !gatewayV2Service.isGatewayDescendant(assetId, gateway.getId())) {
                publishErrorResponse(topic, MQTTErrorResponse.Error.UNAUTHORIZED, "One or more attribute updates were not authorized");
                LOG.info("Unauthorized, gateway users can only modify their own descendants " + getConnectionIDString(connection));
                return;
            }

            // cancel the request if any of the events cannot be authorized
            if (!clientEventService.authorizeEventWrite(realm, authContext.get(), event)) {
                publishErrorResponse(topic, MQTTErrorResponse.Error.UNAUTHORIZED, "One or more attribute updates were not authorized");
                LOG.info("Unauthorized, one or more attribute updates were not authorized " + getConnectionIDString(connection));
                return;
            }
        }

        // Temporary list to store the events for the response
        List<AttributeEvent> events = new ArrayList<>();
        for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();

            AttributeEvent event = new AttributeEvent(assetId, attributeName, attributeValue)
                .setSource(GatewayMQTTHandler.class.getSimpleName());

            sendAttributeEvent(event);
            events.add(event);
        }

        publishResponse(topic, events);
    }

    /**
     * Handles the retrieval of an attribute based on the provided topic.
     *
     * @param connection the connection from which the request originated
     * @param topic the topic associated with the request
     * @param publishValueOnly if true, only the attribute value is published
     */
    protected void handleGetAttribute(RemotingConnection connection, Topic topic, boolean publishValueOnly) {
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
     * Handles the acknowledgement of gateway events, publishing the event to the inbound queue when acknowledged.
     *
     * @param connection the connection from which the acknowledgement originated
     * @param body the message body containing the acknowledgement ID
     */
    protected void handleGatewayEventAcknowledgement(RemotingConnection connection, ByteBuf body) {
        // Acknowledgement id is the body of the message
        String ackId = body.toString(StandardCharsets.UTF_8);
        if (ackId == null) {
            LOG.fine("Invalid gateway acknowledgement, acknowledgement id is missing " + getConnectionIDString(connection));
            return;
        }

        AttributeEvent event = eventsPendingGatewayAcknowledgement.getIfPresent(ackId);
        if (event == null) {
            LOG.fine("Gateway event not found for the provided acknowledgement id " + ackId + " connection " + getConnectionIDString(connection));
            return;
        }

        event.setSource(GatewayMQTTHandler.class.getSimpleName());
        sendAttributeEvent(event);
        synchronized (eventsPendingGatewayAcknowledgement) {
            eventsPendingGatewayAcknowledgement.invalidate(ackId);
        }

    }

    /**
     * Updates the status of a gateway asset.
     *
     * @param gatewayAsset the gateway asset to update
     * @param status the new connection status
     */
    protected void updateGatewayStatus(GatewayV2Asset gatewayAsset, ConnectionStatus status) {
        sendAttributeEvent(new AttributeEvent(gatewayAsset.getId(), GatewayV2Asset.STATUS, status));
    }

    /**
     * Publishes an error response to the specified topic.
     *
     * @param topic the topic to publish the error response to
     * @param error the error type
     * @param message the error message
     */
    protected void publishErrorResponse(Topic topic, MQTTErrorResponse.Error error, String message) {
        publishMessage(getResponseTopic(topic), new MQTTErrorResponse(error, message), MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Publishes a response to the specified topic.
     *
     * @param topic the topic to publish the response to
     * @param response the response object
     */
    protected void publishResponse(Topic topic, Object response) {
        publishMessage(getResponseTopic(topic), response, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Publishes a pending gateway event to the specified gateway.
     *
     * @param event the attribute event to publish
     * @param gateway the gateway asset to publish the event to
     */
    protected void publishPendingGatewayEvent(AttributeEvent event, GatewayV2Asset gateway) {
        String eventId = UniqueIdentifierGenerator.generateId();
        MQTTGatewayEventMessage gatewayEvent = new MQTTGatewayEventMessage(eventId, event);

        String topic = String.join("/", gateway.getRealm(), gateway.getClientId().get(), GATEWAY_TOPIC, GATEWAY_EVENTS_TOPIC, GATEWAY_PENDING_TOPIC);
        publishMessage(topic, gatewayEvent, MqttQoS.AT_LEAST_ONCE);

        synchronized (eventsPendingGatewayAcknowledgement) {
            eventsPendingGatewayAcknowledgement.put(eventId, event);
        }
    }

    /**
     * Sends an attribute event to the message broker.
     *
     * @param event the attribute event to send
     */
    protected void sendAttributeEvent(AttributeEvent event) {
        messageBrokerService.getFluentProducerTemplate()
                .withBody(event)
                .to(ATTRIBUTE_EVENT_PROCESSOR)
                .asyncRequest();
    }

    /**
     * Builds an attribute event based on the topic tokens and value.
     *
     * @param topicTokens the tokens from the topic
     * @param value the value for the attribute event
     * @return the constructed AttributeEvent
     */
    protected AttributeEvent buildAttributeEvent(List<String> topicTokens, Object value) {
        String attributeName = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX);
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        return new AttributeEvent(assetId, attributeName, value).setSource(GatewayMQTTHandler.class.getSimpleName());
    }

    /**
     * Builds a read attribute event based on the topic tokens.
     *
     * @param topicTokens the tokens from the topic
     * @return the constructed ReadAttributeEvent
     */
    protected ReadAttributeEvent buildReadAttributeEvent(List<String> topicTokens) {
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        String attributeName = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX);
        return new ReadAttributeEvent(assetId, attributeName);
    }

    /**
     * Builds an asset event based on the topic tokens and cause.
     *
     * @param topicTokens the tokens from the topic
     * @param cause the cause of the asset event
     * @return the constructed AssetEvent, or null if invalid
     */
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
            LOG.fine("Failed to build AssetEvent, asset doesn't exist " + assetId);
            return null;
        }
        return new AssetEvent(cause, asset, null);
    }

    /**
     * Builds a read asset event based on the topic tokens.
     *
     * @param topicTokens the tokens from the topic
     * @return the constructed ReadAssetEvent
     */
    protected ReadAssetEvent buildReadAssetEvent(List<String> topicTokens) {
        String assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
        return new ReadAssetEvent(assetId);
    }


    /**
     * Authorizes the parent ID for an asset, ensuring it is valid based on the gateway asset.
     *
     * @param asset the asset to check
     * @param gatewayAsset the gateway asset for authorization context
     * @return true if the parent ID is authorized, false otherwise
     */
    protected boolean authorizeParentId(Asset<?> asset, GatewayV2Asset gatewayAsset) {
        if (gatewayAsset != null) {
            // Prevent assets from being moved to a parent that is not a descendant of the gateway or the gateway itself
            boolean parentIsDescendantOfGateway = asset.getParentId() != null 
                && gatewayV2Service.isGatewayDescendant(asset.getParentId(), gatewayAsset.getId());
            boolean parentIsGateway = asset.getParentId() != null && gatewayAsset.getId().equals(asset.getParentId());

            return parentIsDescendantOfGateway || parentIsGateway;
        } else {
            // Prevent assets from being moved to a parent that is a gateway or its descendants
            boolean parentIsGateway = asset.getParentId() != null && gatewayV2Service.isRegisteredGateway(asset.getParentId());
            boolean parentIsDescendantOfGateway = asset.getParentId() != null && gatewayV2Service.isGatewayDescendant(asset.getParentId());

            return !parentIsGateway && !parentIsDescendantOfGateway;
        }
    }

    public static boolean isAssetsTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, ASSETS_TOKEN_INDEX), ASSETS_TOPIC)
                && !isAttributesTopic(topic) && !isAttributesValueTopic(topic); 
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

    protected static boolean isAttributesValueTopic(Topic topic) {
        return Objects.equals(topicTokenIndexToString(topic, ASSETS_TOKEN_INDEX), ASSETS_TOPIC) &&
                Objects.equals(topicTokenIndexToString(topic, ATTRIBUTES_TOKEN_INDEX), ATTRIBUTES_VALUE_TOPIC);
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
        return Objects.equals(topicTokenIndexToString(topic, GATEWAY_TOKEN_INDEX), GATEWAY_TOPIC) 
            && Objects.equals(topicTokenIndexToString(topic, GATEWAY_EVENTS_TOKEN_INDEX), GATEWAY_EVENTS_TOPIC);
    }

    public static String getResponseTopic(Topic topic) {
        return topic.toString() + "/" + RESPONSE_TOPIC;
    }




}
