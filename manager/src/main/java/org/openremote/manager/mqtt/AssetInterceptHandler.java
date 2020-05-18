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

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.ClientCredentialsAuthFrom;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.RenewEventSubscriptions;
import org.openremote.model.interop.BiConsumer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.MqttBrokerService.TOPIC_SEPARATOR;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;

public class AssetInterceptHandler implements InterceptHandler {

    private static final Logger LOG = Logger.getLogger(AssetInterceptHandler.class.getName());

    protected final ManagerIdentityService identityService;
    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final AssetStorageService assetStorageService;
    protected final AssetProcessingService assetProcessingService;
    protected final MessageBrokerService messageBrokerService;
    protected final MqttConnector mqttConnector;
    protected final BiConsumer<String, AttributeEvent> attributeEventConsumer;

    AssetInterceptHandler(AssetStorageService assetStorageService,
                          AssetProcessingService assetProcessingService,
                          ManagerIdentityService managerIdentityService,
                          ManagerKeycloakIdentityProvider managerKeycloakIdentityProvider,
                          MessageBrokerService messageBrokerService, MqttConnector mqttConnector, BiConsumer<String, AttributeEvent> attributeEventConsumer) {

        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
        this.identityService = managerIdentityService;
        this.identityProvider = managerKeycloakIdentityProvider;
        this.messageBrokerService = messageBrokerService;
        this.mqttConnector = mqttConnector;
        this.attributeEventConsumer = attributeEventConsumer;
    }

    @Override
    public String getID() {
        return UniqueIdentifierGenerator.generateId(AssetInterceptHandler.class.getName());
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return InterceptHandler.ALL_MESSAGE_TYPES;
    }

    @Override
    public void onConnect(InterceptConnectMessage interceptConnectMessage) {
        MqttConnector.MqttConnection connection = mqttConnector.createConnection(interceptConnectMessage.getClientID(), interceptConnectMessage.getUsername(), interceptConnectMessage.getPassword());
        String suppliedClientSecret = new String(interceptConnectMessage.getPassword(), StandardCharsets.UTF_8);
        connection.accessToken = identityProvider.getKeycloak().getAccessToken(connection.realm, new ClientCredentialsAuthFrom(connection.username, suppliedClientSecret)).getToken();
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage interceptDisconnectMessage) {
        MqttConnector.MqttConnection connection = mqttConnector.removeConnection(interceptDisconnectMessage.getClientID());
        if (connection != null) {
            CancelEventSubscription<AttributeEvent> cancelEventSubscription = new CancelEventSubscription<>(AttributeEvent.class);
            messageBrokerService.getProducerTemplate().sendBodyAndHeader(ClientEventService.CLIENT_EVENT_QUEUE, cancelEventSubscription, WebsocketConstants.SESSION_KEY, connection.clientId);
        }
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage interceptConnectionLostMessage) {
        MqttConnector.MqttConnection connection = mqttConnector.removeConnection(interceptConnectionLostMessage.getClientID());
        if (connection != null) {
            CancelEventSubscription<AttributeEvent> cancelEventSubscription = new CancelEventSubscription<>(AttributeEvent.class);
            messageBrokerService.getProducerTemplate().sendBodyAndHeader(ClientEventService.CLIENT_EVENT_QUEUE, cancelEventSubscription, WebsocketConstants.SESSION_KEY, connection.clientId);
        }
        LOG.info("Connection lost for client: " + interceptConnectionLostMessage.getClientID());
    }

    @Override
    public void onPublish(InterceptPublishMessage message) {
        System.out.println("moquette mqtt broker message intercepted, topic: " + message.getTopicName()
                + ", content: " + new String(message.getPayload().array()));
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage interceptSubscribeMessage) {
        MqttConnector.MqttConnection connection = mqttConnector.getConnection(interceptSubscribeMessage.getClientID());
        if (connection != null) {

            String[] topicParts = interceptSubscribeMessage.getTopicFilter().split(TOPIC_SEPARATOR);
            AttributeRef attributeRef = new AttributeRef(topicParts[1], topicParts[2]);
            String subscriptionId = connection.attributeSubscriptions.get(attributeRef);
            if (subscriptionId != null) { //renew subscription
                RenewEventSubscriptions renewEventSubscriptions = new RenewEventSubscriptions(new String[]{subscriptionId});
                try {
                    Map<String, Object> headers = prepareHeaders(connection);
                    messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, renewEventSubscriptions, headers);
                } catch (VerificationException e) {
                    e.printStackTrace();
                }
            } else {
                EventSubscription<AttributeEvent> subscription = new EventSubscription<>(
                        AttributeEvent.class,
                        new AssetFilter<AttributeEvent>().setRealm(connection.realm).setAssetIds(attributeRef.getEntityId()),
                        String.valueOf(connection.getNextSubscriptionId()),
                        triggeredEventSubscription ->
                                triggeredEventSubscription.getEvents()
                                        .forEach(event ->
                                                attributeEventConsumer.accept(connection.clientId, event)
                                        )
                );
                try {
                    Map<String, Object> headers = prepareHeaders(connection);
                    messageBrokerService.getProducerTemplate().sendBodyAndHeaders(ClientEventService.CLIENT_EVENT_QUEUE, subscription, headers);
                    connection.attributeSubscriptions.put(attributeRef, subscription.getSubscriptionId());
                } catch (VerificationException e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw new IllegalStateException("Connection with clientId " + interceptSubscribeMessage.getClientID() + " not found.");
        }
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {
        MqttConnector.MqttConnection connection = mqttConnector.getConnection(interceptUnsubscribeMessage.getClientID());
        if (connection != null) {
            String[] topicParts = interceptUnsubscribeMessage.getTopicFilter().split(TOPIC_SEPARATOR);
            AttributeRef attributeRef = new AttributeRef(topicParts[1], topicParts[2]);
            String subscriptionId = connection.attributeSubscriptions.remove(attributeRef);
            if (subscriptionId != null) {
                CancelEventSubscription<AttributeEvent> cancelEventSubscription = new CancelEventSubscription<>(AttributeEvent.class, subscriptionId);
                messageBrokerService.getProducerTemplate().sendBodyAndHeader(ClientEventService.CLIENT_EVENT_QUEUE, cancelEventSubscription, WebsocketConstants.SESSION_KEY, connection.clientId);
            }
        }
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage interceptAcknowledgedMessage) {

    }

    private Map<String, Object> prepareHeaders(MqttConnector.MqttConnection connection) throws VerificationException {
        AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.AUTH_CONTEXT, new AccessTokenAuthContext(connection.realm, accessToken));
        headers.put(WebsocketConstants.SESSION_KEY, connection.clientId);
        headers.put(ClientEventService.HEADER_CONNECTION_TYPE, ClientEventService.HEADER_CONNECTION_TYPE_MQTT);
        return headers;
    }
}
