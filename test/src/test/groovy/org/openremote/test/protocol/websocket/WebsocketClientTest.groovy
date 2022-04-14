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
import org.openremote.test.setup.ManagerTestSetup
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

import java.util.concurrent.TimeUnit

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

    protected static <T> T messageFromString(String message, String prefix, Class<T> clazz) {
        try {
            message = message.substring(prefix.length())
            return ValueUtil.JSON.readValue(message, clazz)
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
        client.addMessageConsumer({
            message -> receivedMessages.add(message)
        })
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
            def subscription = messageFromString(receivedMessages[0], EventSubscription.SUBSCRIBED_MESSAGE_PREFIX, EventSubscription.class)
            assert subscription != null
            assert subscription.subscriptionId == "1"
        }

        when: "when apartment 1 living room temp changes"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", 5))

        then: "the client receives the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            def triggeredEvent = messageFromString(receivedMessages[0], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).assetId == managerTestSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).attributeName == "targetTemperature"
            assert ((AttributeEvent)triggeredEvent.events[0]).value.orElse(0) == 5
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "when apartment 1 living room temp is set to the same value again"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", 5))

        then: "the client should receive the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            def triggeredEvent = messageFromString(receivedMessages[0], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).assetId == managerTestSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).attributeName == "targetTemperature"
            assert ((AttributeEvent)triggeredEvent.events[0]).value.orElse(0) == 5
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "when apartment 1 living room temp is set to null"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", null))

        then: "the client receives the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            def triggeredEvent = messageFromString(receivedMessages[0], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).assetId == managerTestSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).attributeName == "targetTemperature"
            assert !((AttributeEvent)triggeredEvent.events[0]).value.isPresent()
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "when apartment 1 bathroom temp changes"
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
