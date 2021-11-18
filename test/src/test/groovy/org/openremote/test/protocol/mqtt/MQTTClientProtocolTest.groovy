/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.test.protocol.mqtt

import io.moquette.BrokerConstants
import org.openremote.agent.protocol.mqtt.MQTTAgent
import org.openremote.agent.protocol.mqtt.MQTTAgentLink
import org.openremote.agent.protocol.mqtt.MQTTProtocol
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.agent.protocol.websocket.WebsocketAgentLink
import org.openremote.agent.protocol.websocket.WebsocketHTTPSubscription
import org.openremote.agent.protocol.websocket.WebsocketSubscription
import org.openremote.agent.protocol.websocket.WebsocketSubscriptionImpl
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.ReadAttributeEvent
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.agent.Protocol
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.event.TriggeredEventSubscription
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.JsonPathFilter
import org.openremote.model.value.RegexValueFilter
import org.openremote.model.value.SubStringValueFilter
import org.openremote.model.value.ValueFilter
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.core.MediaType

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_PORT
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.NUMBER

class MQTTClientProtocolTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Check websocket client protocol and linked attribute deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def clientEventService = container.getService(ClientEventService.class)
        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST)
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT)

        when: "an MQTT client agent is created to connect to this tests manager"
        def clientId = UniqueIdentifierGenerator.generateId()
        def agent = new MQTTAgent("Test agent")
            .setRealm(Constants.MASTER_REALM)
            .setClientId(clientId)
            .setHost(mqttHost)
            .setPort(mqttPort)
            .setUsernamePassword(new UsernamePassword(keycloakTestSetup.tenantBuilding.realm + ":" + keycloakTestSetup.serviceUser.username, keycloakTestSetup.serviceUser.secret))

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol should authenticate and the agent status should become CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.id, Agent.class)
            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "an asset is created with attributes linked to the agent"
        def asset = new ThingAsset("Test Asset")
            .setParent(agent)
            .addOrReplaceAttributes(
                // write attribute value
                new Attribute<>("readWriteTargetTemp", NUMBER)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new MQTTAgentLink(agent.id)
                            .setSubscriptionTopic("${keycloakTestSetup.tenantBuilding.realm}/$clientId/attributevalue/targetTemperature/${managerTestSetup.apartment1LivingroomId}")
                            .setPublishTopic("${keycloakTestSetup.tenantBuilding.realm}/$clientId/attributevalue/targetTemperature/${managerTestSetup.apartment1LivingroomId}")
                            .setWriteValue("${Protocol.DYNAMIC_VALUE_PLACEHOLDER}")
                    ))
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "the linked attributes should be correctly linked"
        conditions.eventually {
            assert !(agentService.getProtocolInstance(agent.id) as MQTTProtocol).protocolMessageConsumers.isEmpty()
        }

        when: "the source attribute are updated"
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", 99d))

        then: "the values should be stored in the database"
        conditions.eventually {
            def livingRoom = assetStorageService.find(managerTestSetup.apartment1LivingroomId)
            assert livingRoom != null
            assert livingRoom.getAttribute("targetTemperature", Double.class).flatMap{it.value}.orElse(0d) == 99d
        }

        then: "the linked attributes should also have the updated values of the subscribed attributes"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("readWriteTargetTemp").get().getValue().orElse(null) == 99d
        }

        when: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset.id,
            "readWriteTargetTemp",
            19.5)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the linked targetTemperature attribute should contain this written value (it should have been written to the target temp attribute and then read back again)"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("readWriteTargetTemp", Double.class).flatMap{it.getValue()}.orElse(null) == 19.5d
        }
    }
}
