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
import com.google.api.client.util.Charsets;
import io.moquette.broker.subscriptions.Token;
import io.moquette.broker.subscriptions.Topic;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.*;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
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
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.Values;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.manager.mqtt.MqttBrokerService.*;
import static org.openremote.model.syslog.SyslogCategory.API;

public class ORInterceptHandler extends AbstractInterceptHandler {

    private static final Logger LOG = SyslogCategory.getLogger(API, ORInterceptHandler.class);
    protected final MqttBrokerService brokerService;
    protected final MessageBrokerService messageBrokerService;
    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final Map<String, MqttConnection> sessionIdConnectionMap;

    public ORInterceptHandler(MqttBrokerService brokerService,
                              ManagerKeycloakIdentityProvider identityProvider,
                              MessageBrokerService messageBrokerService,
                              Map<String, MqttConnection> sessionIdConnectionMap) {
        this.brokerService = brokerService;
        this.identityProvider = identityProvider;
        this.messageBrokerService = messageBrokerService;
        this.sessionIdConnectionMap = sessionIdConnectionMap;
    }

    @Override
    public String getID() {
        return ORInterceptHandler.class.getName();
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
    public void onConnect(InterceptConnectMessage msg) {
        String[] realmAndUsername = msg.getUsername().split(":");
        String realm = realmAndUsername[0];
        String username = realmAndUsername[1];
        String password = msg.getPassword() != null ? new String(msg.getPassword(), Charsets.UTF_8) : null;
        MqttConnection connection = new MqttConnection(identityProvider, msg.getClientID(), realm, username, password);

        sessionIdConnectionMap.put(connection.getSessionId(), connection);
        Map<String, Object> headers = prepareHeaders(connection);
        headers.put(ConnectionConstants.SESSION_OPEN, true);
        messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, null, headers);
        LOG.info("Connected: " + connection);

        // No topic info here (not sure what willTopic is??) so just notify all custom handlers
        brokerService.customHandlers.forEach(customHandler -> customHandler.onConnect(connection, msg));
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        MqttConnection connection = sessionIdConnectionMap.remove(msg.getClientID());

        if (connection != null) {
            Map<String, Object> headers = prepareHeaders(connection);
            headers.put(ConnectionConstants.SESSION_CLOSE, true);
            messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, null, headers);
            LOG.info("Connection closed: " + connection);

            // No topic info here so just notify all custom handlers
            brokerService.customHandlers.forEach(customHandler -> customHandler.onDisconnect(connection, msg));
        }
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        MqttConnection connection = sessionIdConnectionMap.get(msg.getClientID());

        if (connection != null) {
            Map<String, Object> headers = prepareHeaders(connection);
            headers.put(ConnectionConstants.SESSION_CLOSE_ERROR, true);
            messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, null, headers);
            LOG.info("Connection lost: " + connection);

            // No topic info here so just notify all custom handlers
            brokerService.customHandlers.forEach(customHandler -> customHandler.onConnectionLost(connection, msg));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {

        String[] realmAndUsername = msg.getUsername().split(":");
        String realm = realmAndUsername[0];
        String username = realmAndUsername[1]; 
        MqttConnection connection = sessionIdConnectionMap.get(msg.getClientID());

        if (connection == null) {
            LOG.info("No connection found: realm=" + realm + ", username=" + username + ", clientID=" + msg.getClientID());
            return;
        }

        Topic topic = Topic.asTopic(msg.getTopicFilter());

        MQTTCustomHandler customHandler;
        if ((customHandler = brokerService.getCustomInterceptHandler(msg.getTopicFilter())) != null) {
            customHandler.onSubscribe(connection, topic, msg);
            return;
        }

        List<String> topicTokens = topic.getTokens().stream().map(Token::toString).collect(Collectors.toList());
        boolean isAttributeTopic = MqttBrokerService.isAttributeTopic(topicTokens);
        boolean isAssetTopic = MqttBrokerService.isAssetTopic(topicTokens);
        boolean isValueSubscription = isAttributeTopic && topic.toString().endsWith(ATTRIBUTE_VALUE_TOPIC);
        String subscriptionId = msg.getTopicFilter(); // Use topic as subscription ID

        AssetFilter filter = buildAssetFilter(connection, topicTokens);
        Class subscriptionClass = isAssetTopic ? AssetEvent.class : AttributeEvent.class;

        if (filter == null) {
            LOG.info("Invalid event filter generated for topic '" + topic + "': " + connection);
            return;
        }

        EventSubscription subscription = new EventSubscription(
            subscriptionClass,
            filter,
            subscriptionId
        );

        Consumer<SharedEvent> eventConsumer = brokerService.getEventConsumer(connection, subscriptionId, isValueSubscription);
        connection.subscriptionHandlerMap.put(subscriptionId, eventConsumer);
        Map<String, Object> headers = prepareHeaders(connection);
        messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, subscription, headers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {

        String[] realmAndUsername = msg.getUsername().split(":");
        String realm = realmAndUsername[0];
        String username = realmAndUsername[1];
        MqttConnection connection = sessionIdConnectionMap.get(msg.getClientID());

        if (connection == null) {
            LOG.info("No connection found: realm=" + realm + ", username=" + username + ", clientID=" + msg.getClientID());
            return;
        }

        Topic topic = Topic.asTopic(msg.getTopicFilter());

        MQTTCustomHandler customHandler;
        if ((customHandler = brokerService.getCustomInterceptHandler(topic.toString())) != null) {
            customHandler.onUnsubscribe(connection, topic, msg);
            return;
        }

        String subscriptionId = topic.toString();
        Consumer<? extends SharedEvent> eventConsumer = connection.subscriptionHandlerMap.remove(subscriptionId);

        if (eventConsumer != null) {
            boolean isAssetTopic = subscriptionId.startsWith(ASSET_TOPIC);

            Map<String, Object> headers = prepareHeaders(connection);
            Class<SharedEvent> subscriptionClass = (Class) (isAssetTopic ? AssetEvent.class : AttributeEvent.class);
            CancelEventSubscription cancelEventSubscription = new CancelEventSubscription(subscriptionClass, subscriptionId);
            messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, cancelEventSubscription, headers);
        }
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        String[] realmAndUsername = msg.getUsername().split(":");
        String realm = realmAndUsername[0];
        String username = realmAndUsername[1];
        MqttConnection connection = sessionIdConnectionMap.get(msg.getClientID());

        if (connection == null) {
            LOG.info("No connection found: realm=" + realm + ", username=" + username + ", clientID=" + msg.getClientID());
            return;
        }

        Topic topic = Topic.asTopic(msg.getTopicName());

        MQTTCustomHandler customHandler;
        if ((customHandler = brokerService.getCustomInterceptHandler(topic.toString())) != null) {
            customHandler.onPublish(connection, topic, msg);
            return;
        }

        List<String> topicTokens = topic.getTokens().stream().map(Token::toString).collect(Collectors.toList());

        if (topicTokens.size() > 1) {
            String assetId = topicTokens.get(1);
            AttributeRef attributeRef = null;
            if (topicTokens.size() > 2) { //attribute specific
                attributeRef = new AttributeRef(assetId, topicTokens.get(2));
            }
            if (attributeRef == null) {
                String payloadContent = msg.getPayload().toString(StandardCharsets.UTF_8);
                Values.parse(payloadContent).flatMap(Values::asJSONObject).ifPresent(objectValue -> {
                    Map<String, Object> headers = prepareHeaders(connection);
                    Map.Entry<String, JsonNode> firstElem = objectValue.fields().next();
                    AttributeEvent attributeEvent = new AttributeEvent(assetId, firstElem.getKey(), firstElem.getValue());
                    messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, attributeEvent, headers);
                });
            } else {
                String payloadContent = msg.getPayload().toString(StandardCharsets.UTF_8);
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
            LOG.warning("Couldn't process message for topic: " + topic);
        }
    }

    protected Map<String, Object> prepareHeaders(MqttConnection connection) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ConnectionConstants.SESSION_KEY, connection.getSessionId());
        headers.put(ClientEventService.HEADER_CONNECTION_TYPE, ClientEventService.HEADER_CONNECTION_TYPE_MQTT);

        try {
            AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.getAccessToken(), identityProvider.getKeycloakDeployment(connection.realm, connection.getUsername()));
            headers.put(Constants.AUTH_CONTEXT, new AccessTokenAuthContext(connection.realm, accessToken));
        } catch (VerificationException e) {
            LOG.log(Level.WARNING, "Couldn't verify token", e);
        }
        return headers;
    }
}
