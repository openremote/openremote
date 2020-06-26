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
import org.openremote.container.web.OAuthPasswordGrant
import org.openremote.agent.protocol.io.AbstractNettyIoClient
import org.openremote.agent.protocol.websocket.WebsocketIoClient
import org.openremote.container.Container
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.concurrent.ManagerExecutorService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetFilter
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.event.TriggeredEventSubscription
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER

/**
 * This tests the {@link WebsocketIoClient} by connecting to the manager web socket
 */
class WebsocketClientTest extends Specification implements ManagerContainerTrait {

    def "Check client"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def protocolExecutorService = container.getService(ManagerExecutorService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
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
        String lastMessage
        client.addMessageConsumer({
            message -> lastMessage = message
        })
        client.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })

        when: "we call connect on the client"
        client.connect()

        then: "the client status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "we subscribe to events produced by the server"
        client.sendMessage(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX + Container.JSON.writeValueAsString(
            new EventSubscription(
                AttributeEvent.class,
                new AssetFilter<AttributeEvent>().setAssetIds(managerDemoSetup.apartment1LivingroomId),
                "1",
                null)))

        then: "the server should return a subscribed event"
        conditions.eventually {
            assert lastMessage != null
            assert lastMessage.indexOf(EventSubscription.SUBSCRIBED_MESSAGE_PREFIX) == 0
            def subscription = Container.JSON.readValue(lastMessage.substring(EventSubscription.SUBSCRIBED_MESSAGE_PREFIX.length()), EventSubscription.class)
            assert subscription.subscriptionId == "1"
        }

        when: "when an event occurs that should be sent to the client"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerDemoSetup.apartment1LivingroomId, "targetTemperature", Values.create(5)))

        then: "the client receives the event"
        conditions.eventually {
            assert lastMessage != null
            assert lastMessage.indexOf(TriggeredEventSubscription.MESSAGE_PREFIX) == 0
            def triggeredEvent = Container.JSON.readValue(lastMessage.substring(TriggeredEventSubscription.MESSAGE_PREFIX.length()), TriggeredEventSubscription.class)
            assert triggeredEvent.subscriptionId == "1"
            assert triggeredEvent.events.size() == 1
            assert ((AttributeEvent)triggeredEvent.events[0]).entityId == managerDemoSetup.apartment1LivingroomId
            assert ((AttributeEvent)triggeredEvent.events[0]).attributeName == "targetTemperature"
            assert ((AttributeEvent)triggeredEvent.events[0]).value.flatMap{Values.getNumber(it)}.orElse(0) == 5
        }

        cleanup: "the server should be stopped"
        if (client != null) {
            client.disconnect()
        }
        stopContainer(container)
    }
}
