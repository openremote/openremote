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
package org.openremote.test.protocol.websocket

import io.netty.channel.ChannelHandler
import org.apache.http.client.utils.URIBuilder
import org.openremote.agent.protocol.io.AbstractNettyIOClient
import org.openremote.agent.protocol.websocket.WebsocketIOClient
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetEvent
import org.openremote.model.attribute.Attribute
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.asset.AssetFilter
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.OAuthPasswordGrant
import org.openremote.model.event.TriggeredEventSubscription
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER

/**
 * This tests the {@link WebsocketIOClient} by connecting to the manager web socket API which means it also tests
 * the API itself
 */
class WebsocketClientTest extends Specification implements ManagerContainerTrait {

    protected static <T> T messageFromString(String message) {
        try {
            def isSubscription = message.startsWith(EventSubscription.SUBSCRIBED_MESSAGE_PREFIX)
            def isTriggered = !isSubscription && message.startsWith(TriggeredEventSubscription.MESSAGE_PREFIX)
            message = message.substring(message.indexOf(":")+1)
            return ValueUtil.JSON.readValue(message, isSubscription ? EventSubscription.class : isTriggered ? TriggeredEventSubscription.class : SharedEvent.class)
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse message")
        }
    }

    protected static String messageToString(String prefix, Object message) {
        try {
            String str = ValueUtil.asJSON(message).orElse(null)
            return prefix + str;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialise message");
        }
    }

    def "Check restricted client"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "a simple Websocket client with restricted user"
        def client = new WebsocketIOClient<String>(
                new URIBuilder("ws://127.0.0.1:$serverPort/websocket/events?Realm=building").build(),
                null,
                new OAuthPasswordGrant("http://127.0.0.1:$serverPort/auth/realms/building/protocol/openid-connect/token",
                        KEYCLOAK_CLIENT_ID,
                        null,
                        null,
                        "testuser3",
                        "testuser3"))
        client.setEncoderDecoderProvider({
            [new AbstractNettyIOClient.MessageToMessageDecoder<String>(String.class, client)].toArray(new ChannelHandler[0])
        })

        and: "another Websocket client with another restricted user"
        def client2 = new WebsocketIOClient<String>(
                new URIBuilder("ws://127.0.0.1:$serverPort/websocket/events?Realm=building").build(),
                null,
                new OAuthPasswordGrant("http://127.0.0.1:$serverPort/auth/realms/building/protocol/openid-connect/token",
                        KEYCLOAK_CLIENT_ID,
                        null,
                        null,
                        "building",
                        "building"))
        client2.setEncoderDecoderProvider({
            [new AbstractNettyIOClient.MessageToMessageDecoder<String>(String.class, client2)].toArray(new ChannelHandler[0])
        })

        and: "we add callback consumers to the clients"
        def connectionStatus = client.getConnectionStatus()
        def connectionStatus2 = client2.getConnectionStatus()
        List<Object> receivedMessages = []
        List<Object> receivedMessages2 = []
        client.addMessageConsumer({
            message -> receivedMessages.add(messageFromString(message))
        })
        client.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })
        client2.addMessageConsumer({
            message -> receivedMessages2.add(messageFromString(message))
        })
        client2.addConnectionStatusConsumer({
            status -> connectionStatus2 = status
        })

        and: "the system settles down"
        noEventProcessedIn(assetProcessingService, 500)

        when: "we call connect on the clients"
        client.connect()
        client2.connect()

        then: "the clients status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert client2.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus2 == ConnectionStatus.CONNECTED
        }

        // TODO: Remove this once client supports some better connection logic
        and: "some time passes to allow the connection to be fully initialised"
        sleep(3000)

        when: "we subscribe to attribute events produced by the server"
        client.sendMessage(messageToString(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX,
                new EventSubscription(
                        AttributeEvent.class,
                        null,
                        "attributes",
                        null)))
        client2.sendMessage(messageToString(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX,
                new EventSubscription(
                        AttributeEvent.class,
                        null,
                        "attributes2",
                        null)))

        and: "we subscribe to asset events produced by the server"
        client.sendMessage(messageToString(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX,
                new EventSubscription(
                        AssetEvent.class,
                        null,
                        "assets",
                        null)))
        client2.sendMessage(messageToString(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX,
                new EventSubscription(
                        AssetEvent.class,
                        null,
                        "assets2",
                        null)))

        then: "the server should confirm the subscriptions"
        conditions.eventually {
            assert receivedMessages.size() == 2
            assert receivedMessages.any {it instanceof EventSubscription && it.subscriptionId == "attributes"}
            assert receivedMessages.any {it instanceof EventSubscription && it.subscriptionId == "assets"}
            assert receivedMessages2.size() == 2
            assert receivedMessages2.any {it instanceof EventSubscription && it.subscriptionId == "attributes2"}
            assert receivedMessages2.any {it instanceof EventSubscription && it.subscriptionId == "assets2"}
        }

        when: "apartment 1 living room temp changes"
        receivedMessages.clear()
        receivedMessages2.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", 5))

        then: "the testuser3 client receives the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            TriggeredEventSubscription triggeredEvent = receivedMessages[0] as TriggeredEventSubscription
            assert triggeredEvent.subscriptionId == "attributes"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent) triggeredEvent.events[0]).id == managerTestSetup.apartment1LivingroomId
            assert ((AttributeEvent) triggeredEvent.events[0]).name == "targetTemperature"
            assert ((AttributeEvent) triggeredEvent.events[0]).value.orElse(0) == 5
        }

        then: "the building user should not have received the event"
        conditions.eventually {
            assert receivedMessages2.isEmpty()
        }

        when: "apartment 2 living room temp changes"
        receivedMessages.clear()
        receivedMessages2.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment2LivingroomId, "targetTemperature", 15))

        then: "the building client receives the event"
        conditions.eventually {
            assert receivedMessages2.size() == 1
            TriggeredEventSubscription triggeredEvent = receivedMessages2[0] as TriggeredEventSubscription
            assert triggeredEvent.subscriptionId == "attributes2"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent) triggeredEvent.events[0]).id == managerTestSetup.apartment2LivingroomId
            assert ((AttributeEvent) triggeredEvent.events[0]).name == "targetTemperature"
            assert ((AttributeEvent) triggeredEvent.events[0]).value.orElse(0) == 15
        }

        then: "the testuser3 user should not have received the event"
        assert receivedMessages.isEmpty()

        when: "apartment 1 living room asset is modified"
        receivedMessages.clear()
        receivedMessages2.clear()
        def apartment1Livingroom = assetStorageService.find(managerTestSetup.apartment1LivingroomId)
        apartment1Livingroom.addAttributes(
                new Attribute<>("testAttribute", ValueType.BOOLEAN)
        )
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom)

        then: "the testuser3 client should be notified with only accessible attributes"
        conditions.eventually {
            assert receivedMessages.size() == 1
            assert receivedMessages.get(0) instanceof TriggeredEventSubscription
            assert (receivedMessages.get(0) as TriggeredEventSubscription).subscriptionId == "assets"
            assert (receivedMessages.get(0) as TriggeredEventSubscription).events.size() == 1
            assert (receivedMessages.get(0) as TriggeredEventSubscription).events.get(0) instanceof AssetEvent
            assert ((receivedMessages.get(0) as TriggeredEventSubscription).events.get(0) as AssetEvent).id == managerTestSetup.apartment1LivingroomId
            assert ((receivedMessages.get(0) as TriggeredEventSubscription).events.get(0) as AssetEvent).attributeNames.size() == 7
            assert !((receivedMessages.get(0) as TriggeredEventSubscription).events.get(0) as AssetEvent).attributeNames.contains("testAttribute")
        }

        then: "the building user should not have received the event"
        assert receivedMessages2.isEmpty()

        cleanup: "the server should be stopped"
        if (client != null) {
            client.disconnect()
        }
        if (client2 != null) {
            client2.disconnect()
        }
    }

    def "Check client"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)

        and: "a simple Websocket client"
        def client = new WebsocketIOClient<String>(
                new URIBuilder("ws://127.0.0.1:$serverPort/websocket/events?Realm=master").build(),
                null,
                new OAuthPasswordGrant("http://127.0.0.1:$serverPort/auth/realms/master/protocol/openid-connect/token",
                    KEYCLOAK_CLIENT_ID,
                    null,
                    null,
                    MASTER_REALM_ADMIN_USER,
                    getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)))
        client.setEncoderDecoderProvider({
            [new AbstractNettyIOClient.MessageToMessageDecoder<String>(String.class, client)].toArray(new ChannelHandler[0])
        })

        and: "we add callback consumers to the client"
        def connectionStatus = client.getConnectionStatus()
        List<String> receivedMessages = []
        client.addMessageConsumer(
            message -> receivedMessages.add(messageFromString(message))
        )
        client.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })

        and: "the system settles down"
        noEventProcessedIn(assetProcessingService, 500)

        when: "we call connect on the client"
        client.connect()

        then: "the client status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
        }

        // TODO: Remove this once client supports some better connection logic
        and: "some time passes to allow the connection to be fully initialised"
        sleep(3000)

        when: "we subscribe to attribute events produced by the server"
        client.sendMessage(messageToString(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX,
            new EventSubscription(
                AttributeEvent.class,
                new AssetFilter<AttributeEvent>().setAssetIds(managerTestSetup.apartment1LivingroomId),
                "1",
                null)))

        then: "the server should return a subscribed event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            EventSubscription subscription = receivedMessages[0] as EventSubscription
            assert subscription != null
            assert subscription.subscriptionId == "1"
        }

        when: "when apartment 1 living room temp changes"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", 5))

        then: "the client receives the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            TriggeredEventSubscription triggeredEvent = receivedMessages[0] as TriggeredEventSubscription
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).id == managerTestSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).name == "targetTemperature"
            assert ((AttributeEvent)triggeredEvent.events[0]).value.orElse(0) == 5
        }

        when: "when apartment 1 living room temp is set to the same value again"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", 5))

        then: "the client should receive the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            TriggeredEventSubscription triggeredEvent = receivedMessages[0] as TriggeredEventSubscription
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).id == managerTestSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).name == "targetTemperature"
            assert ((AttributeEvent)triggeredEvent.events[0]).value.orElse(0) == 5
        }

        when: "when apartment 1 living room temp is set to null"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", null))

        then: "the client receives the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            TriggeredEventSubscription triggeredEvent = receivedMessages[0] as TriggeredEventSubscription
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).id == managerTestSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).name == "targetTemperature"
            assert !((AttributeEvent)triggeredEvent.events[0]).value.isPresent()
        }

        when: "when apartment 1 bathroom temp changes"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1BathroomId, "targetTemperature", 10))

        then: "the bathroom target temp should have changed"
        conditions.eventually {
            def bathroom = assetStorageService.find(managerTestSetup.apartment1BathroomId)
            assert bathroom.getAttribute("targetTemperature").flatMap{it.value}.orElse(0d) == 10d
        }

        and: "the client should not have received the event"
        conditions.eventually {
            assert receivedMessages.isEmpty()
        }

        cleanup: "the server should be stopped"
        if (client != null) {
            client.disconnect()
        }
    }
}
