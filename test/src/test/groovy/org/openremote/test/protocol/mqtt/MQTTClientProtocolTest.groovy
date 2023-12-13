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

import org.openremote.agent.protocol.mqtt.MQTTAgent
import org.openremote.agent.protocol.mqtt.MQTTAgentLink
import org.openremote.agent.protocol.mqtt.MQTTProtocol
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.test.ManagerContainerTrait
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.NUMBER

class MQTTClientProtocolTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Check MQTT client protocol and linked attribute deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)
        def brokerService = container.getService(MQTTBrokerService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def clientEventService = container.getService(ClientEventService.class)
        def mqttHost = brokerService.host
        def mqttPort = brokerService.port

        when: "an MQTT client agent is created to connect to this tests manager"
        def clientId = UniqueIdentifierGenerator.generateId()
        def agent = new MQTTAgent("Test agent")
            .setRealm(Constants.MASTER_REALM)
            .setClientId(clientId)
            .setHost(mqttHost)
            .setPort(mqttPort)
            .setUsernamePassword(new UsernamePassword(keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser.username, keycloakTestSetup.serviceUser.secret))

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
                            .setSubscriptionTopic("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}")
                            .setPublishTopic("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}")
                            .setWriteValue("${Constants.DYNAMIC_VALUE_PLACEHOLDER}")
                    ))
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "the linked attributes should be correctly linked"
        conditions.eventually {
            assert !(agentService.getProtocolInstance(agent.id) as MQTTProtocol).protocolMessageConsumers.isEmpty()
            def connection = brokerService.getConnectionFromClientID(clientId)
            assert connection != null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        when: "the attribute referenced in the agent link is updated"
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(new AttributeEvent(managerTestSetup.apartment1LivingroomId, "targetTemperature", 99d))

        then: "the values should be stored in the database"
        conditions.eventually {
            def livingRoom = assetStorageService.find(managerTestSetup.apartment1LivingroomId)
            assert livingRoom != null
            assert livingRoom.getAttribute("targetTemperature").flatMap{it.value}.orElse(0d) == 99d
        }

        then: "the agent linked attribute should also have the updated value of the subscribed attribute"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("readWriteTargetTemp").get().getValue().orElse(null) == 99d
        }

        when: "the agent linked attribute is updated"
        def attributeEvent = new AttributeEvent(asset.id,
            "readWriteTargetTemp",
            19.5)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the linked targetTemperature attribute should contain this written value (it should have been written to the target temp attribute and then read back again)"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("readWriteTargetTemp").flatMap{it.getValue()}.orElse(null) == 19.5d
        }
    }
}
