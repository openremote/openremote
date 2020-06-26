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

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.*;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.ClientCredentialsAuthForm;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.RenewEventSubscriptions;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.MqttBrokerService.ASSET_ATTRIBUTE_VALUE_TOPIC;
import static org.openremote.manager.mqtt.MqttBrokerService.TOPIC_SEPARATOR;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;

public class EventInterceptHandler extends AbstractInterceptHandler {

    private static final Logger LOG = Logger.getLogger(EventInterceptHandler.class.getName());

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final MessageBrokerService messageBrokerService;
    protected final Map<String, MqttConnection> mqttConnectionMap;

    EventInterceptHandler(ManagerKeycloakIdentityProvider managerKeycloakIdentityProvider,
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
        String suppliedClientSecret = new String(interceptConnectMessage.getPassword(), StandardCharsets.UTF_8);
        connection.accessToken = identityProvider.getExternalKeycloak().getAccessToken(connection.realm, new ClientCredentialsAuthForm(connection.username, suppliedClientSecret)).getToken();

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
            String assetId = topicParts[1];
            AttributeRef attributeRef = null;
            boolean isValueSubscription = false;
            if (topicParts.length > 2) { //attribute specific
                attributeRef = new AttributeRef(assetId, topicParts[2]);
            }
            if (topicParts.length == 4 && topicParts[3].equals(ASSET_ATTRIBUTE_VALUE_TOPIC)) {
                isValueSubscription = true;
            }
            String subscriptionId;
            if (attributeRef == null) {
                subscriptionId = connection.assetSubscriptions.remove(assetId);
            } else {
                if (isValueSubscription) {
                    subscriptionId = connection.assetAttributeValueSubscriptions.remove(attributeRef);
                } else {
                    subscriptionId = connection.assetAttributeSubscriptions.remove(attributeRef);
                }
            }
            if (subscriptionId != null) { //renew subscription
                RenewEventSubscriptions renewEventSubscriptions = new RenewEventSubscriptions(new String[]{subscriptionId});
                Map<String, Object> headers = prepareHeaders(connection);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, renewEventSubscriptions, headers);
            } else {
                AssetFilter<AttributeEvent> attributeAssetFilter = new AssetFilter<AttributeEvent>().setRealm(connection.realm).setAssetIds(assetId);
                EventSubscription<AttributeEvent> subscription = new EventSubscription<>(
                        AttributeEvent.class,
                        attributeAssetFilter,
                        String.valueOf(connection.getNextSubscriptionId())
                );

                if (attributeRef == null) { //attribute specific
                    connection.assetSubscriptions.put(assetId, subscription.getSubscriptionId());
                } else {
                    attributeAssetFilter.setAttributeNames(attributeRef.getAttributeName());
                    if (isValueSubscription) {
                        connection.assetAttributeValueSubscriptions.put(attributeRef, subscription.getSubscriptionId());
                    } else {
                        connection.assetAttributeSubscriptions.put(attributeRef, subscription.getSubscriptionId());
                    }
                }

                Map<String, Object> headers = prepareHeaders(connection);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, subscription, headers);
            }
        } else {
            throw new IllegalStateException("Connection with clientId " + interceptSubscribeMessage.getClientID() + " not found.");
        }
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {
        MqttConnection connection = mqttConnectionMap.get(interceptUnsubscribeMessage.getClientID());
        if (connection != null) {
            String[] topicParts = interceptUnsubscribeMessage.getTopicFilter().split(TOPIC_SEPARATOR);
            String assetId = topicParts[1];
            String subscriptionId;

            if (topicParts.length > 2) { //attribute specific
                if (topicParts.length == 4 && topicParts[3].equals(ASSET_ATTRIBUTE_VALUE_TOPIC)) {
                    subscriptionId = connection.assetAttributeValueSubscriptions.remove(new AttributeRef(assetId, topicParts[2]));
                } else {
                    subscriptionId = connection.assetAttributeSubscriptions.remove(new AttributeRef(assetId, topicParts[2]));
                }
            } else {
                subscriptionId = connection.assetSubscriptions.remove(assetId);
            }
            if (subscriptionId != null) {
                Map<String, Object> headers = prepareHeaders(connection);
                CancelEventSubscription<AttributeEvent> cancelEventSubscription = new CancelEventSubscription<>(AttributeEvent.class, subscriptionId);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, cancelEventSubscription, headers);
            }
        }
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        MqttConnection connection = mqttConnectionMap.get(msg.getClientID());
        if (connection != null) {
            String[] topicParts = msg.getTopicName().split(TOPIC_SEPARATOR);
            String assetId = topicParts[1];
            AttributeRef attributeRef = null;
            if (topicParts.length > 2) { //attribute specific
                attributeRef = new AttributeRef(assetId, topicParts[2]);
            }
            if (attributeRef == null) {
                String payloadContent = msg.getPayload().toString(Charset.defaultCharset());
                Values.parse(payloadContent).flatMap(Values::getObject).ifPresent(objectValue -> {
                    Map<String, Object> headers = prepareHeaders(connection);
                    AttributeEvent attributeEvent = new AttributeEvent(assetId, objectValue.keys()[0], objectValue.get(objectValue.keys()[0]).orElse(null));
                    messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, attributeEvent, headers);
                });
            } else {
                String payloadContent = msg.getPayload().toString(Charset.defaultCharset());
                Value value = null;
                if (Character.isLetter(payloadContent.charAt(0))) {
                    if (payloadContent.equals(Boolean.TRUE.toString())) {
                        value = Values.create(true);
                    }
                    if (payloadContent.equals(Boolean.FALSE.toString())) {
                        value = Values.create(false);
                    }
                    payloadContent = '"' + payloadContent + '"';
                }
                if (value == null) {
                    value = Values.parse(payloadContent).orElse(Values.create(payloadContent));
                }

                Map<String, Object> headers = prepareHeaders(connection);
                AttributeEvent attributeEvent = new AttributeEvent(assetId, attributeRef.getAttributeName(), value);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, attributeEvent, headers);
            }
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
            if (e instanceof TokenNotActiveException) {
                String suppliedClientSecret = new String(connection.password, StandardCharsets.UTF_8);
                connection.accessToken = identityProvider.getExternalKeycloak().getAccessToken(connection.realm, new ClientCredentialsAuthForm(connection.username, suppliedClientSecret)).getToken();
                try {
                    AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
                    headers.put(Constants.AUTH_CONTEXT, new AccessTokenAuthContext(connection.realm, accessToken));
                } catch (VerificationException verificationException) {
                    LOG.log(Level.WARNING, "Couldn't verify token", verificationException);
                }
            }
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        return headers;
    }
}
