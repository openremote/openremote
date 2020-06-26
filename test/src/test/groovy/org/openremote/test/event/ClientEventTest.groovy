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
package org.openremote.test.event

import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetFilter
import org.openremote.model.attribute.MetaItemType
import org.openremote.model.asset.agent.AgentStatusEvent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.value.Values
import org.openremote.test.ClientEventService
import org.openremote.test.GwtClientTrait
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.event.ClientEventService.CLIENT_EVENT_QUEUE
import static org.openremote.manager.event.ClientEventService.WEBSOCKET_EVENTS
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo

/**
 * This tests client event subscription and publishing.
 */
// TODO: Add publishing tests
class ClientEventTest extends Specification implements ManagerContainerTrait, GwtClientTrait {

    def "Event subscription"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)
        def internalClientEventService = container.getService(org.openremote.manager.event.ClientEventService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        expect: "all client events happening during startup to be consumed"
        noPendingExchangesOnMessageEndpoint(container, CLIENT_EVENT_QUEUE)

        when: "a client websocket connection and attached event bus and service"
        def accessToken = {
            authenticate(
                    container,
                    MASTER_REALM,
                    KEYCLOAK_CLIENT_ID,
                    MASTER_REALM_ADMIN_USER,
                    getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
            ).token
        }
        List<SharedEvent> collectedSharedEvents = []
        def eventBus = createEventBus(collectedSharedEvents)
        def clientEventService = new ClientEventService(eventBus, container.JSON)
        def websocketSession = connect(createWebsocketClient(), clientEventService.endpoint, serverUri(serverPort), WEBSOCKET_EVENTS, MASTER_REALM, accessToken.call())

        and: "a subscription is made to the agent status event"
        clientEventService.subscribe(AgentStatusEvent.class)

        and: "an existing protocol configuration is disabled"
        def agent = assetStorageService.find(managerDemoSetup.agentId, true)
        agent.getAttribute(managerDemoSetup.agentProtocolConfigName).get().addMeta(
                new MetaItem(MetaItemType.DISABLED)
        )
        agent = assetStorageService.merge(agent)

        then: "the agent status change event should have been received"
        conditions.eventually {
            assert collectedSharedEvents.size() == 3
            assert collectedSharedEvents[0] instanceof AgentStatusEvent
            assert (collectedSharedEvents[0] as AgentStatusEvent).protocolConfiguration.entityId == managerDemoSetup.agentId
            assert (collectedSharedEvents[0] as AgentStatusEvent).protocolConfiguration.attributeName == managerDemoSetup.agentProtocolConfigName
            assert (collectedSharedEvents[0] as AgentStatusEvent).connectionStatus == ConnectionStatus.DISCONNECTED
            assert collectedSharedEvents[1] instanceof AgentStatusEvent
            assert (collectedSharedEvents[1] as AgentStatusEvent).protocolConfiguration.entityId == managerDemoSetup.agentId
            assert (collectedSharedEvents[1] as AgentStatusEvent).protocolConfiguration.attributeName == managerDemoSetup.agentProtocolConfigName
            assert (collectedSharedEvents[1] as AgentStatusEvent).connectionStatus == ConnectionStatus.WAITING
            assert collectedSharedEvents[2] instanceof AgentStatusEvent
            assert (collectedSharedEvents[2] as AgentStatusEvent).protocolConfiguration.entityId == managerDemoSetup.agentId
            assert (collectedSharedEvents[2] as AgentStatusEvent).protocolConfiguration.attributeName == managerDemoSetup.agentProtocolConfigName
            assert (collectedSharedEvents[2] as AgentStatusEvent).connectionStatus == ConnectionStatus.DISABLED
        }

        when: "the subscription is removed"
        clientEventService.unsubscribe(AgentStatusEvent.class)
        collectedSharedEvents.clear()

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
            assert collectedSharedEvents.isEmpty()
        }

        when: "a subscription is made to the attribute event"
        clientEventService.subscribe(AttributeEvent.class, new AssetFilter().setAssetIds(managerDemoSetup.thingId))

        and: "an assets location is changed"
        def asset = assetStorageService.find(managerDemoSetup.thingId, true)
        asset.setCoordinates(new GeoJSONPoint(120.1d, 50.43d))
        asset = assetStorageService.merge(asset)

        then: "the attribute event should have been received"
        conditions.eventually {
            assert collectedSharedEvents.size() == 1
            assert collectedSharedEvents[0] instanceof AttributeEvent
            assert (collectedSharedEvents[0] as AttributeEvent).entityId == managerDemoSetup.thingId
            assert (collectedSharedEvents[0] as AttributeEvent).value.flatMap{GeoJSONPoint.fromValue(it)}.map{it.x}.orElse(null) == 120.1d
            assert (collectedSharedEvents[0] as AttributeEvent).value.flatMap{GeoJSONPoint.fromValue(it)}.map{it.y}.orElse(null) == 50.43d
        }

        when: "the assets location is set to null"
        asset.setCoordinates(null)
        asset = assetStorageService.merge(asset)

        then: "the attribute event should have been received with null coordinates"
        conditions.eventually {
            assert collectedSharedEvents.size() == 2
            assert collectedSharedEvents[1] instanceof AttributeEvent
            assert (collectedSharedEvents[1] as AttributeEvent).entityId == managerDemoSetup.thingId
            assert !(collectedSharedEvents[1] as AttributeEvent).value.isPresent()
        }

        when: "the location is changed"
        asset.setCoordinates(new GeoJSONPoint(-10.11d, -50.233d))
        asset = assetStorageService.merge(asset)

        then: "the attribute event should have been received"
        conditions.eventually {
            assert collectedSharedEvents.size() == 3
            assert collectedSharedEvents[2] instanceof AttributeEvent
            assert (collectedSharedEvents[2] as AttributeEvent).entityId == managerDemoSetup.thingId
            assert (collectedSharedEvents[2] as AttributeEvent).value.flatMap{GeoJSONPoint.fromValue(it)}.map{it.x}.orElse(null) == -10.11d
            assert (collectedSharedEvents[2] as AttributeEvent).value.flatMap{GeoJSONPoint.fromValue(it)}.map{it.y}.orElse(null) == -50.233d
        }

        when: "the location is set with the same value"
        asset.setCoordinates(new GeoJSONPoint(-10.11d, -50.233d))
        asset = assetStorageService.merge(asset)

        then: "no location event should have been received"
        Thread.sleep(300)
        assert collectedSharedEvents.size() == 3

        when: "the subscription is removed"
        clientEventService.unsubscribe(AttributeEvent.class)
        collectedSharedEvents.clear()

        and: "a new attribute event subscription is added"
        clientEventService.subscribe(AttributeEvent.class, new AssetFilter().setAssetIds(managerDemoSetup.thingId))

        and: "an internal attribute event subscription is made"
        List<AttributeEvent> internalReceivedEvents = []
        internalClientEventService.getEventSubscriptions().createOrUpdate(
                ClientEventTest.class.getName(),
                false,
                new EventSubscription<>(
                        AttributeEvent.class,
                        new AssetFilter().setAssetIds(managerDemoSetup.thingId),
                        { triggeredEventSubscription ->
                            internalReceivedEvents.add(triggeredEventSubscription.events[0] as AttributeEvent)
                        }
                ))

        and: "the subscribed asset is modified"
        def currentValue = asset.getAttribute(managerDemoSetup.thingLightToggleAttributeName).get().getValueAsBoolean().get()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.thingId,
                managerDemoSetup.thingLightToggleAttributeName,
                Values.create(!currentValue)))

        // TODO: Only 1 attribute event should reach the subscribers below
        then: "an attribute event should have been received by the internal subscriber"
        conditions.eventually {
            assert internalReceivedEvents.size() == 1
            assert internalReceivedEvents[0].entityId == managerDemoSetup.thingId
            assert internalReceivedEvents[0].attributeName == managerDemoSetup.thingLightToggleAttributeName
            assert Values.getBoolean(internalReceivedEvents[0].value.get()).get() == !currentValue
        }

        and: "an attribute event should have been received by the external subscriber"
        conditions.eventually {
            assert collectedSharedEvents.size() == 1
            assert collectedSharedEvents[0] instanceof AttributeEvent
            assert (collectedSharedEvents[0] as AttributeEvent).entityId == managerDemoSetup.thingId
            assert (collectedSharedEvents[0] as AttributeEvent).attributeName == managerDemoSetup.thingLightToggleAttributeName
            assert Values.getBoolean((collectedSharedEvents[0] as AttributeEvent).value.get()).get() == !currentValue
        }

        cleanup: "the client should be stopped"
        if (clientEventService != null) clientEventService.close()

        and: "the server should be stopped"
        stopContainer(container)
    }
}