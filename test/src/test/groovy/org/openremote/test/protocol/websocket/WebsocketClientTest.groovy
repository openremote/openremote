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
import org.openremote.container.timer.TimerService
import org.openremote.container.web.OAuthPasswordGrant
import org.openremote.agent.protocol.io.AbstractNettyIoClient
import org.openremote.agent.protocol.websocket.WebsocketIoClient
import org.openremote.container.Container
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.concurrent.ManagerExecutorService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetFilter
import org.openremote.model.asset.agent.AgentStatusEvent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaItemType
import org.openremote.model.event.TriggeredEventSubscription
import org.openremote.model.event.shared.CancelEventSubscription
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo

/**
 * This tests the {@link WebsocketIoClient} by connecting to the manager web socket API which means it also tests
 * the API itself
 */
class WebsocketClientTest extends Specification implements ManagerContainerTrait {

    protected static <T> T messageFromString(String message, String prefix, Class<T> clazz) {
        try {
            message = message.substring(prefix.length())
            return Container.JSON.readValue(message, clazz)
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse message")
        }
    }

    protected static String messageToString(String prefix, Object message) {
        try {
            String str = Container.JSON.writeValueAsString(message);
            return prefix + str;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialise message");
        }
    }

    def "Check client"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def protocolExecutorService = container.getService(ManagerExecutorService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "a simple Websocket client"
        def client = new WebsocketIoClient<String>(
                new URIBuilder("ws://localhost:$serverPort/websocket/events?Auth-Realm=master").build(),
                null,
                new OAuthPasswordGrant("http://localhost:$serverPort/auth/realms/master/protocol/openid-connect/token",
                    KEYCLOAK_CLIENT_ID,
                    null,
                    null,
                    MASTER_REALM_ADMIN_USER,
                    getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)),
                protocolExecutorService)
        client.setEncoderDecoderProvider({
            [new AbstractNettyIoClient.MessageToMessageDecoder<String>(String.class, client)].toArray(new ChannelHandler[0])
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

        when: "we subscribe to attribute events produced by the server"
        client.sendMessage(messageToString(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX,
            new EventSubscription(
                AttributeEvent.class,
                new AssetFilter<AttributeEvent>().setAssetIds(managerDemoSetup.apartment1LivingroomId),
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
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerDemoSetup.apartment1LivingroomId, "targetTemperature", Values.create(5)))

        then: "the client receives the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            def triggeredEvent = messageFromString(receivedMessages[0], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).entityId == managerDemoSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).attributeName == "targetTemperature"
            assert ((AttributeEvent)triggeredEvent.events[0]).value.flatMap{Values.getNumber(it)}.orElse(0) == 5
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "when apartment 1 living room temp is set to the same value again"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerDemoSetup.apartment1LivingroomId, "targetTemperature", Values.create(5)))

        then: "the client should receive the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            def triggeredEvent = messageFromString(receivedMessages[0], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).entityId == managerDemoSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).attributeName == "targetTemperature"
            assert ((AttributeEvent)triggeredEvent.events[0]).value.flatMap{Values.getNumber(it)}.orElse(0) == 5
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "when apartment 1 living room temp is set to null"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerDemoSetup.apartment1LivingroomId, "targetTemperature", null))

        then: "the client receives the event"
        conditions.eventually {
            assert receivedMessages.size() == 1
            def triggeredEvent = messageFromString(receivedMessages[0], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).entityId == managerDemoSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).attributeName == "targetTemperature"
            assert !((AttributeEvent)triggeredEvent.events[0]).value.isPresent()
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "when apartment 1 bathroom temp changes"
        receivedMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerDemoSetup.apartment1BathroomId, "targetTemperature", Values.create(10)))

        then: "the bathroom target temp should have changed"
        conditions.eventually {
            def bathroom = assetStorageService.find(managerDemoSetup.apartment1BathroomId)
            assert bathroom.getAttribute("targetTemperature").flatMap{it.getValueAsNumber()}.orElse(0d) == 10d
        }

        and: "the client should not have received the event"
        conditions.eventually {
            assert receivedMessages.isEmpty()
        }


        when: "a subscription is made to the agent status event"
        receivedMessages.clear()
        client.sendMessage(messageToString(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX,
            new EventSubscription(
                AgentStatusEvent.class,
                null,
                "2",
                null)))

        then: "a subscribed event should have been received"
        conditions.eventually {
            assert receivedMessages.size() == 1
            def subscribedEvent = messageFromString(receivedMessages[0], EventSubscription.SUBSCRIBED_MESSAGE_PREFIX, EventSubscription.class)
            assert subscribedEvent.subscriptionId == "2"
        }

        when: "an existing protocol configuration is disabled"
        receivedMessages.clear()
        def agent = assetStorageService.find(managerDemoSetup.agentId, true)
        agent.getAttribute(managerDemoSetup.agentProtocolConfigName).get().addMeta(
            new MetaItem(MetaItemType.DISABLED)
        )
        agent = assetStorageService.merge(agent)

        then: "the agent status change event should have been received"
        conditions.eventually {
            assert receivedMessages.size() == 2
            def waitingEvent = messageFromString(receivedMessages[0], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            def disabledEvent = messageFromString(receivedMessages[1], TriggeredEventSubscription.MESSAGE_PREFIX, TriggeredEventSubscription.class)
            assert waitingEvent.subscriptionId == "2"
            assert waitingEvent.events.size() == 1
            assert waitingEvent.events[0] instanceof AgentStatusEvent
            assert (waitingEvent.events[0] as AgentStatusEvent).protocolConfiguration.entityId == managerDemoSetup.agentId
            assert (waitingEvent.events[0] as AgentStatusEvent).protocolConfiguration.attributeName == managerDemoSetup.agentProtocolConfigName
            assert (waitingEvent.events[0] as AgentStatusEvent).connectionStatus == ConnectionStatus.WAITING
            assert disabledEvent.subscriptionId == "2"
            assert disabledEvent.events.size() == 1
            assert disabledEvent.events[0] instanceof AgentStatusEvent
            assert (disabledEvent.events[0] as AgentStatusEvent).protocolConfiguration.entityId == managerDemoSetup.agentId
            assert (disabledEvent.events[0] as AgentStatusEvent).protocolConfiguration.attributeName == managerDemoSetup.agentProtocolConfigName
            assert (disabledEvent.events[0] as AgentStatusEvent).connectionStatus == ConnectionStatus.DISABLED
        }

        when: "the agent status subscription is removed"
        receivedMessages.clear()
        client.sendMessage(CancelEventSubscription.MESSAGE_PREFIX + Container.JSON.writeValueAsString(
            new CancelEventSubscription(
                AgentStatusEvent.class,
                "2")))

        and: "the existing protocol configuration is re-enabled"
        agent.getAttribute(managerDemoSetup.agentProtocolConfigName).get().getMeta().removeIf(
            isMetaNameEqualTo(MetaItemType.DISABLED)
        )
        agent = assetStorageService.merge(agent)

        then: "the protocol should be CONNECTED but no new events should have been received"
        conditions.eventually {
            assert agentService.getProtocolConnectionStatus(
                new AttributeRef(managerDemoSetup.agentId, managerDemoSetup.agentProtocolConfigName)
            ) == ConnectionStatus.CONNECTED
            assert receivedMessages.isEmpty()
        }

        cleanup: "the server should be stopped"
        if (client != null) {
            client.disconnect()
        }
    }
}
