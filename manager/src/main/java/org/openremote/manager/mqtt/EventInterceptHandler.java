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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                InterceptUnsubscribeMessage.class
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
            String subscriptionId = connection.assetSubscriptions.get(assetId);
            if (subscriptionId != null) { //renew subscription
                RenewEventSubscriptions renewEventSubscriptions = new RenewEventSubscriptions(new String[]{subscriptionId});
                Map<String, Object> headers = prepareHeaders(connection);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, renewEventSubscriptions, headers);
            } else {
                EventSubscription<AttributeEvent> subscription = new EventSubscription<>(
                        AttributeEvent.class,
                        new AssetFilter<AttributeEvent>().setRealm(connection.realm).setAssetIds(assetId),
                        String.valueOf(connection.getNextSubscriptionId())
                );
                Map<String, Object> headers = prepareHeaders(connection);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, subscription, headers);
                connection.assetSubscriptions.put(assetId, subscription.getSubscriptionId());
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
            String subscriptionId = connection.assetSubscriptions.remove(assetId);
            if (subscriptionId != null) {
                Map<String, Object> headers = prepareHeaders(connection);
                CancelEventSubscription<AttributeEvent> cancelEventSubscription = new CancelEventSubscription<>(AttributeEvent.class, subscriptionId);
                messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, cancelEventSubscription, headers);
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
