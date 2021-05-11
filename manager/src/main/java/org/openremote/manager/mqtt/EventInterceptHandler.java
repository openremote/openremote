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

import com.fasterxml.jackson.databind.JsonNode;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.*;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.MqttBrokerService.*;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.syslog.SyslogCategory.API;

public class EventInterceptHandler extends AbstractInterceptHandler {

    private static final Logger LOG = SyslogCategory.getLogger(API, EventInterceptHandler.class);

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final MessageBrokerService messageBrokerService;
    protected final Map<String, MqttConnection> mqttConnectionMap;

    public EventInterceptHandler(ManagerKeycloakIdentityProvider managerKeycloakIdentityProvider,
                                 MessageBrokerService messageBrokerService,
                                 Map<String, MqttConnection> mqttConnectionMap) {

        this.identityProvider = managerKeycloakIdentityProvider;
        this.messageBrokerService = messageBrokerService;
        this.mqttConnectionMap = mqttConnectionMap;
    }

    @Override
    public String getID() {
        return UniqueIdentifierGenerator.generateId(EventInterceptHandler.class.getName());
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return new Class[]{
                InterceptConnectMessage.class,
                InterceptDisconnectMessage.class,
                InterceptConnectionLostMessage.class,
                InterceptSubscribeMessage.class,
                InterceptUnsubscribeMessage.class,
                InterceptPublishMessage.class
        };
    }

    @Override
    public void onConnect(InterceptConnectMessage interceptConnectMessage) {
        MqttConnection connection = new MqttConnection(interceptConnectMessage.getClientID(), interceptConnectMessage.getUsername(), interceptConnectMessage.getPassword());
        mqttConnectionMap.put(connection.clientId, connection);

        Map<String, Object> headers = prepareHeaders(connection);
        headers.put(ConnectionConstants.SESSION_OPEN, true);
        messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, null, headers);
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage interceptDisconnectMessage) {
        MqttConnection connection = mqttConnectionMap.remove(interceptDisconnectMessage.getClientID());
        if (connection != null) {
            Map<String, Object> headers = prepareHeaders(connection);
            headers.put(ConnectionConstants.SESSION_CLOSE, true);
            messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, null, headers);
        }
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage interceptConnectionLostMessage) {
        MqttConnection connection = mqttConnectionMap.remove(interceptConnectionLostMessage.getClientID());
        if (connection != null) {
            Map<String, Object> headers = prepareHeaders(connection);
            headers.put(ConnectionConstants.SESSION_CLOSE_ERROR, true);
            messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, null, headers);

        }
        LOG.info("Connection lost for client: " + interceptConnectionLostMessage.getClientID());
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage interceptSubscribeMessage) {
        MqttConnection connection = mqttConnectionMap.get(interceptSubscribeMessage.getClientID());
        if (connection != null) {
            String[] topicParts = interceptSubscribeMessage.getTopicFilter().split(TOPIC_SEPARATOR);
            if (topicParts.length > 1) {
                boolean isAttributeTopic = topicParts[0].equals(ATTRIBUTE_TOPIC);
                boolean isValueSubscription = false;

                String secondTopicLevel = topicParts[1];
                String assetId = null;
                if (!secondTopicLevel.equals(SINGLE_LEVEL_WILDCARD) && !secondTopicLevel.equals(MULTI_LEVEL_WILDCARD)) {
                    assetId = secondTopicLevel;
                }

                if (topicParts[topicParts.length - 1].equals(ATTRIBUTE_VALUE_TOPIC)) {
                    isValueSubscription = true;
                }
                String subscriptionId;
                if (isValueSubscription) {
                    subscriptionId = connection.attributeValueSubscriptions.remove(interceptSubscribeMessage.getTopicFilter());
                } else {
                    subscriptionId = connection.assetSubscriptions.remove(interceptSubscribeMessage.getTopicFilter());
                }
                if (TextUtil.isNullOrEmpty(subscriptionId)) {
                    subscriptionId = String.valueOf(connection.getNextSubscriptionId());
                }

                EventSubscription<?> subscription;
                if (isAttributeTopic) {
                    AssetFilter<AttributeEvent> attributeAssetFilter = new AssetFilter<AttributeEvent>().setRealm(connection.realm);
                    int singleLevelIndex = Arrays.asList(topicParts).indexOf(SINGLE_LEVEL_WILDCARD);

                    if (assetId != null) {
                        int multiLevelIndex = Arrays.asList(topicParts).indexOf(MULTI_LEVEL_WILDCARD);
                        if (multiLevelIndex == -1) {
                            if (singleLevelIndex == -1) { //attribute/assetId/
                                attributeAssetFilter.setAssetIds(assetId);
                            } else {
                                attributeAssetFilter.setParentIds(assetId);
                            }
                            if (topicParts.length > 2) { //attribute/assetId/attributeName
                                if (singleLevelIndex == -1) { //attribute/assetId/attributeName
                                    attributeAssetFilter.setAttributeNames(topicParts[2]);
                                } else if (singleLevelIndex == 2 && topicParts.length > 3 && !topicParts[3].equals(SINGLE_LEVEL_WILDCARD)) { // else attribute/assetId/+/attributeName
                                    attributeAssetFilter.setAttributeNames(topicParts[3]);
                                } // else attribute/assetId/+ which should return all attributes
                            }
                        } else if (multiLevelIndex == 2) { //attribute/assetId/#
                            attributeAssetFilter.setParentIds(assetId);
                        }
                    } else {
                        if (singleLevelIndex == 1) { //attribute/+
                            if (topicParts.length > 2) { //attribute/+/attributeName
                                attributeAssetFilter.setAttributeNames(topicParts[2]);
                            }
                        }
                    }

                    subscription = new EventSubscription<>(
                            AttributeEvent.class,
                            attributeAssetFilter,
                            subscriptionId
                    );
                } else {
                    AssetFilter<AssetEvent> assetFilter = new AssetFilter<AssetEvent>().setRealm(connection.realm);
                    if (assetId != null) {
                        int multiLevelIndex = Arrays.asList(topicParts).indexOf(MULTI_LEVEL_WILDCARD);
                        int singleLevelIndex = Arrays.asList(topicParts).indexOf(SINGLE_LEVEL_WILDCARD);

                        if (multiLevelIndex == -1) {
                            assetFilter.setAssetIds(assetId);
                            if (singleLevelIndex == 2) { //asset/assetId/+
                                assetFilter.setParentIds(assetId);
                            } else if (singleLevelIndex == -1) { //asset/assetId
                                assetFilter.setAssetIds(assetId);
                            }
                        } else if (multiLevelIndex == 2) { //asset/assetId/#
                            assetFilter.setParentIds(assetId);
                        }
                    }

                    subscription = new EventSubscription<>(
                            AssetEvent.class,
                            assetFilter,
                            subscriptionId
                    );
                }

                if (isValueSubscription) {
                    connection.attributeValueSubscriptions.put(interceptSubscribeMessage.getTopicFilter(), subscription.getSubscriptionId());
                } else {
                    connection.assetSubscriptions.put(interceptSubscribeMessage.getTopicFilter(), subscription.getSubscriptionId());
                }

                Map<String, Object> headers = prepareHeaders(connection);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, subscription, headers);
            } else {
                LOG.warning("Couldn't process message for topic: " + interceptSubscribeMessage.getTopicFilter());
            }
        } else {
            LOG.info("No connection found for clientId: " + interceptSubscribeMessage.getClientID());
        }
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {
        MqttConnection connection = mqttConnectionMap.get(interceptUnsubscribeMessage.getClientID());
        if (connection != null) {
            String[] topicParts = interceptUnsubscribeMessage.getTopicFilter().split(TOPIC_SEPARATOR);
            if (topicParts.length > 1) {
                String subscriptionId;

                boolean isValueSubscription = topicParts[topicParts.length - 1].equals(ATTRIBUTE_VALUE_TOPIC);
                if (isValueSubscription) {
                    subscriptionId = connection.attributeValueSubscriptions.remove(interceptUnsubscribeMessage.getTopicFilter());
                } else {
                    subscriptionId = connection.assetSubscriptions.remove(interceptUnsubscribeMessage.getTopicFilter());
                }
                if (subscriptionId != null) {
                    Map<String, Object> headers = prepareHeaders(connection);
                    CancelEventSubscription cancelEventSubscription;
                    if (topicParts[0].equals(ATTRIBUTE_TOPIC)) {
                        cancelEventSubscription = new CancelEventSubscription(AttributeEvent.class, subscriptionId);
                    } else {
                        cancelEventSubscription = new CancelEventSubscription(AssetEvent.class, subscriptionId);
                    }
                    messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, cancelEventSubscription, headers);
                }
            } else {
                LOG.warning("Couldn't process message for topic: " + interceptUnsubscribeMessage.getTopicFilter());
            }
        } else {
            LOG.info("No connection found for clientId: " + interceptUnsubscribeMessage.getClientID());
        }
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        MqttConnection connection = mqttConnectionMap.get(msg.getClientID());
        if (connection != null) {
            String[] topicParts = msg.getTopicName().split(TOPIC_SEPARATOR);
            if (topicParts.length > 1) {
                String assetId = topicParts[1];
                AttributeRef attributeRef = null;
                if (topicParts.length > 2) { //attribute specific
                    attributeRef = new AttributeRef(assetId, topicParts[2]);
                }
                if (attributeRef == null) {
                    String payloadContent = msg.getPayload().toString(Charset.defaultCharset());
                    Values.parse(payloadContent).flatMap(Values::asJSONObject).ifPresent(objectValue -> {
                        Map<String, Object> headers = prepareHeaders(connection);
                        Map.Entry<String, JsonNode> firstElem = objectValue.fields().next();
                        AttributeEvent attributeEvent = new AttributeEvent(assetId, firstElem.getKey(), firstElem.getValue());
                        messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, attributeEvent, headers);
                    });
                } else {
                    String payloadContent = msg.getPayload().toString(Charset.defaultCharset());
                    Object value = null;
                    if (Character.isLetter(payloadContent.charAt(0))) {
                        if (payloadContent.equals(Boolean.TRUE.toString())) {
                            value = true;
                        }
                        if (payloadContent.equals(Boolean.FALSE.toString())) {
                            value = false;
                        }
                        payloadContent = '"' + payloadContent + '"';
                    }
                    if (value == null) {
                        value = Values.parse(payloadContent);
                    }
                    if (value == null) {
                        value = payloadContent;
                    }

                    Map<String, Object> headers = prepareHeaders(connection);
                    AttributeEvent attributeEvent = new AttributeEvent(assetId, attributeRef.getName(), value);
                    messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, attributeEvent, headers);
                }
            } else {
                LOG.warning("Couldn't process message for topic: " + msg.getTopicName());
            }
        } else {
            LOG.info("No connection found for clientId: " + msg.getClientID());
        }
    }

    private Map<String, Object> prepareHeaders(MqttConnection connection) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ConnectionConstants.SESSION_KEY, connection.clientId);
        headers.put(ClientEventService.HEADER_CONNECTION_TYPE, ClientEventService.HEADER_CONNECTION_TYPE_MQTT);
        try {
            AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
            headers.put(Constants.AUTH_CONTEXT, new AccessTokenAuthContext(connection.realm, accessToken));
        } catch (VerificationException e) {
            String suppliedClientSecret = new String(connection.password, StandardCharsets.UTF_8);
            connection.accessToken = identityProvider.getAccessToken(connection.realm, connection.username, suppliedClientSecret);
            try {
                AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
                headers.put(Constants.AUTH_CONTEXT, new AccessTokenAuthContext(connection.realm, accessToken));
            } catch (VerificationException verificationException) {
                LOG.log(Level.WARNING, "Couldn't verify token", verificationException);
            }
        }
        return headers;
    }
}
