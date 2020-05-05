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
import org.openremote.container.security.ClientCredentialsAuthFrom;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.EventSubscription;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.KeycloakAuthenticator.MQTT_CLIENT_ID_SEPARATOR;

public class AssetInterceptHandler implements InterceptHandler {

    private static final Logger LOG = Logger.getLogger(KeycloakAuthorizatorPolicy.class.getName());


    protected final ManagerIdentityService identityService;
    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final AssetStorageService assetStorageService;
    protected final AssetProcessingService assetProcessingService;
    protected final ClientEventService clientEventService;
    protected final MqttConnector mqttConnector;

    AssetInterceptHandler(AssetStorageService assetStorageService,
                          AssetProcessingService assetProcessingService,
                          ManagerIdentityService managerIdentityService,
                          ManagerKeycloakIdentityProvider managerKeycloakIdentityProvider,
                          ClientEventService clientEventService, MqttConnector mqttConnector) {

        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
        this.identityService = managerIdentityService;
        this.identityProvider = managerKeycloakIdentityProvider;
        this.clientEventService = clientEventService;
        this.mqttConnector = mqttConnector;
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return InterceptHandler.ALL_MESSAGE_TYPES;
    }

    @Override
    public void onConnect(InterceptConnectMessage interceptConnectMessage) {
        MqttConnector.MqttConnection connection = mqttConnector.createConnection(interceptConnectMessage.getClientID(), interceptConnectMessage.getUsername(), interceptConnectMessage.getPassword());
        String suppliedClientSecret = new String(interceptConnectMessage.getPassword(), StandardCharsets.UTF_8);
        connection.accessToken = identityProvider.getKeycloak().getAccessToken(connection.realm, new ClientCredentialsAuthFrom(MqttBrokerService.MQTT_KEYCLOAK_CLIENT_ID, suppliedClientSecret)).getToken();
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage interceptDisconnectMessage) {
        mqttConnector.removeConnection(interceptDisconnectMessage.getClientID());
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage interceptConnectionLostMessage) {

    }

    @Override
    public void onPublish(InterceptPublishMessage message) {
        System.out.println("moquette mqtt broker message intercepted, topic: " + message.getTopicName()
                + ", content: " + new String(message.getPayload().array()));
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage interceptSubscribeMessage) {
        MqttConnector.MqttConnection connection = mqttConnector.getConnection(interceptSubscribeMessage.getClientID());
//        if (connection != null) {
//            clientEventService.getEventSubscriptions().createOrUpdate(
//                    connection.clientId,
//                    false,
//                    new EventSubscription<>(
//                            AttributeEvent.class,
//                            new AssetFilter<AttributeEvent>().setRealm(connection.realm),
//                            triggeredEventSubscription ->
//                                    triggeredEventSubscription.getEvents()
//                                            .forEach(event ->
//                                                    sendCentralManagerMessage(connection.getLocalRealm(), messageFromSharedEvent(event)))));
//            );
//        } else {
//            throw new IllegalStateException("Connection with clientId " + interceptSubscribeMessage.getClientID() + " not found.");
//        }
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage interceptAcknowledgedMessage) {

    }
}
