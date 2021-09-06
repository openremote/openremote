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
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

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
        LOG.fine("Connected: " + connection);

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
            LOG.fine("Connection closed: " + connection);

            // No topic info here so just notify all custom handlers
            brokerService.customHandlers.forEach(customHandler -> customHandler.onDisconnect(connection, msg));
        }
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        MqttConnection connection = sessionIdConnectionMap.remove(msg.getClientID());

        if (connection != null) {
            Map<String, Object> headers = prepareHeaders(connection);
            headers.put(ConnectionConstants.SESSION_CLOSE_ERROR, true);
            messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, null, headers);
            LOG.fine("Connection lost: " + connection);

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
        boolean isValueSubscription = ATTRIBUTE_VALUE_TOPIC.equals(topicTokens.get(1));
        String subscriptionId = msg.getTopicFilter(); // Use topic as subscription ID

        AssetFilter filter = buildAssetFilter(connection, topicTokens);
        Class subscriptionClass = isAssetTopic ? AssetEvent.class : AttributeEvent.class;

        if (filter == null) {
            LOG.fine("Invalid event filter generated for topic '" + topic + "': " + connection);
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
            LOG.warning("No connection found: realm=" + realm + ", username=" + username + ", clientID=" + msg.getClientID());
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
            LOG.warning("No connection found: realm=" + realm + ", username=" + username + ", clientID=" + msg.getClientID());
            return;
        }

        Topic topic = Topic.asTopic(msg.getTopicName());

        MQTTCustomHandler customHandler;
        if ((customHandler = brokerService.getCustomInterceptHandler(topic.toString())) != null) {
            customHandler.onPublish(connection, topic, msg);
            return;
        }

        List<String> topicTokens = topic.getTokens().stream().map(Token::toString).collect(Collectors.toList());
        boolean isValueWrite = topicTokens.get(1).equals(ATTRIBUTE_VALUE_TOPIC);
        String payloadContent = msg.getPayload().toString(StandardCharsets.UTF_8);
        AttributeEvent attributeEvent = null;

        if (isValueWrite) {
            String assetId = topicTokens.get(2);
            String attributeName = topicTokens.get(3);
            Object value = ValueUtil.parse(payloadContent).orElse(null);
            attributeEvent = new AttributeEvent(assetId, attributeName, value);
        } else {
            attributeEvent = ValueUtil.parse(payloadContent, AttributeEvent.class).orElse(null);
        }

        if (attributeEvent == null) {
            LOG.fine("Failed to parse payload for publish topic '" + topic + "': " + connection);
            return;
        }

        Map<String, Object> headers = prepareHeaders(connection);
        messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, attributeEvent, headers);
    }

    protected Map<String, Object> prepareHeaders(MqttConnection connection) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ConnectionConstants.SESSION_KEY, connection.getSessionId());
        headers.put(ClientEventService.HEADER_CONNECTION_TYPE, ClientEventService.HEADER_CONNECTION_TYPE_MQTT);

        try {
            String token = connection.getAccessToken();
            if (token != null) {
                AccessToken accessToken = AdapterTokenVerifier.verifyToken(token, identityProvider.getKeycloakDeployment(connection.realm, connection.getUsername()));
                headers.put(Constants.AUTH_CONTEXT, new AccessTokenAuthContext(connection.realm, accessToken));
            }
        } catch (VerificationException e) {
            LOG.log(Level.FINE, "Couldn't verify token", e);
        }
        return headers;
    }
}
