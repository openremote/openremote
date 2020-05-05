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

import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.InterceptHandler;
import io.netty.handler.codec.mqtt.*;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;

public class MqttBrokerService extends RouteBuilder implements ContainerService, AssetUpdateProcessor {

    private static final Logger LOG = Logger.getLogger(MqttBrokerService.class.getName());

    public static final String MQTT_KEYCLOAK_CLIENT_ID = "mqtt";
    public static final String MQTTSERVER_LISTEN_HOST = "MQTTSERVER_LISTEN_HOST";
    public static final String MQTTSERVER_LISTEN_PORT = "MQTTSERVER_LISTEN_PORT";

    protected ManagerIdentityService identityService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected ClientEventService clientEventService;
    protected MqttConnector mqttConnector;

    protected boolean active;
    protected String host;
    protected int port;
    protected Server mqttBroker;

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        host = getString(container.getConfig(), MQTTSERVER_LISTEN_HOST, BrokerConstants.HOST);
        port = getInteger(container.getConfig(), MQTTSERVER_LISTEN_PORT, BrokerConstants.PORT);

        mqttConnector = new MqttConnector();

        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        identityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
            active = false;
        } else {
            active = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
            container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
        }

        mqttBroker = new Server();
    }

    @Override
    public void start(Container container) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(BrokerConstants.HOST_PROPERTY_NAME, host);
        properties.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(port));
        properties.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, String.valueOf(false));
        List<? extends InterceptHandler> interceptHandlers = Collections.singletonList(new AssetInterceptHandler(assetStorageService, assetProcessingService, identityService, identityProvider, clientEventService, mqttConnector));
        mqttBroker.startServer(new MemoryConfig(properties), interceptHandlers, null, new KeycloakAuthenticator(identityProvider), new KeycloakAuthorizatorPolicy(identityProvider, assetStorageService, mqttConnector));
        LOG.fine("Started MQTT broker");
    }

    @Override
    public void stop(Container container) throws Exception {
        mqttBroker.stopServer();

        LOG.fine("Stopped MQTT broker");
    }

    @Override
    public void configure() throws Exception {

        if (active) {
            from(PERSISTENCE_TOPIC)
                    .routeId("MqttBrokerServiceAssetChanges")
                    .filter(isPersistenceEventForEntityType(Asset.class))
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        PersistenceEvent<Asset> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                        Asset eventAsset = persistenceEvent.getEntity();

                        if (persistenceEvent.getCause() != PersistenceEvent.Cause.DELETE) {
                            eventAsset = assetStorageService.find(eventAsset.getId(), true);
                        }


                    });
        }
    }

    @Override
    public boolean processAssetUpdate(EntityManager em, Asset asset, AssetAttribute attribute, AttributeEvent.Source source) throws AssetProcessingException {
        MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                .qos(MqttQoS.AT_MOST_ONCE)

                .topicName(asset.getId())
                .retained(true)
                .build();

//        mqttBroker.internalPublish();
        mqttBroker.listConnectedClients().forEach(clientDescriptor -> {
        });
        return false;
    }

    protected void acceptConnection(String clientId) {
        MqttConnAckMessage ackMessage = MqttMessageBuilders.connAck()
                .returnCode(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD)
                .build();
    }
}
