/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.test.benchmark

import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.MQTTHandler
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MQTTBrokerService.*

/**
 * This benchmark is intended to determine the throughput of AttributeEvents over MQTT which is a typical use case.
 * In this simple scenario an MQTT client has subscribed to attributes of an asset and also publishes attribute events
 * for this asset. The time taken for a response to arrive after a publish is a simple measure of the throughput of the
 * system; this can be used to monitor the performance change of the system over time on a given system specification.
 */

class AttributeEventBenchmarkTest extends Specification implements ManagerContainerTrait {

    @Ignore
    def "Attribute processing benchmark"() {

        given: "the container environment is started"
        def eventCount = 1000
        def startTime = -1l
        def endTime = -1l
        List<SharedEvent> receivedEvents = new CopyOnWriteArrayList<>()
        List<Object> receivedValues = new CopyOnWriteArrayList<>()
        MQTT_IOClient client = null
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def defaultMQTTHandler = mqttBrokerService.getCustomHandlers().find{it instanceof DefaultMQTTHandler} as DefaultMQTTHandler
        def assetStorageService = container.getService(AssetStorageService.class)
        def mqttClientId = UniqueIdentifierGenerator.generateId()
        def username = keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser.username // realm and OAuth client id
        def password = keycloakTestSetup.serviceUser.secret
        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, "0.0.0.0")
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, 1883)
        def assetIDs = new ArrayList<String>()
        def assetMultiID = ""

        and: "assets are added for testing purposes"
        for (i in 1..eventCount) {
            def asset = assetStorageService.merge(new ThingAsset("TestThing$i").setRealm(keycloakTestSetup.realmBuilding.name).addAttributes(
                    new Attribute<Object>("counter", ValueType.NUMBER)
            ))
            assetIDs.add(asset.id)
        }
        and: "one asset is added with many attributes"
        def multiAsset = new ThingAsset("TestThingMulti").setRealm(keycloakTestSetup.realmBuilding.name)
        for (i in 1..eventCount) {
            multiAsset.addAttributes(new Attribute<Object>("counter$i", ValueType.NUMBER))
        }
        multiAsset = assetStorageService.merge(multiAsset)
        assetMultiID = multiAsset.id

        when: "a mqtt client connects with valid credentials"
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username, password), null, null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
        }

        when: "a mqtt client subscribes to all attributes of all assets"
        def topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        Consumer<MQTTMessage<String>> eventConsumer = { msg ->
            def event = ValueUtil.parse(msg.payload, SharedEvent.class)
            receivedEvents.add(event.get())
            if (receivedEvents.size() == eventCount) {
                endTime = System.currentTimeMillis()
            }
        }
        def subscribed = client.addMessageConsumer(topic, eventConsumer)

        then: "A subscription should exist"
        assert subscribed
        assert client.topicConsumerMap.get(topic) != null
        assert client.topicConsumerMap.get(topic).size() == 1
        assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
        def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
        assert connection != null
        assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1

        when: "Attribute events are sent for each asset by the MQTT client"
        startTime = System.currentTimeMillis()
        for (i in 1..eventCount) {
            topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/counter/${assetIDs.get(i-1)}".toString()
            def payload = i.toString()
            client.sendMessage(new MQTTMessage<String>(topic, payload))
        }

        then: "all attribute updates should be received by the client"
        new PollingConditions(timeout: 300, initialDelay: 10, delay: 10).eventually {
            getLOG().info("Events processed = ${receivedEvents.size()}")
            assert endTime > 0
        }
        def processingTime = endTime-startTime
        getLOG().info("Time taken to process $eventCount events = ${endTime-startTime}ms")

        when: "Attribute events are again sent for each asset by the MQTT client"
        endTime = 0
        receivedEvents.clear()
        startTime = System.currentTimeMillis()
        for (i in 1..eventCount) {
            topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/counter/${assetIDs.get(i-1)}".toString()
            def payload = i.toString()
            client.sendMessage(new MQTTMessage<String>(topic, payload))
        }

        then: "all attribute updates should be received by the client in a shorter time due to auth caching"
        new PollingConditions(timeout: 300, initialDelay: 10, delay: 10).eventually {
            getLOG().info("Events processed = ${receivedEvents.size()}")
            assert endTime > 0
        }
        assert processingTime > endTime-startTime
        getLOG().info("Time taken to process $eventCount events (with hot cache) = ${endTime-startTime}ms")


        when: "Attribute events are sent for each attribute of the multi asset by the MQTT client"
        endTime = 0
        receivedEvents.clear()
        startTime = System.currentTimeMillis()
        for (i in 1..eventCount) {
            topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/counter$i/${assetMultiID}".toString()
            def payload = i.toString()
            client.sendMessage(new MQTTMessage<String>(topic, payload))
        }

        then: "all attribute updates should be received by the client"
        new PollingConditions(timeout: 300, initialDelay: 10, delay: 10).eventually {
            getLOG().info("Events processed = ${receivedEvents.size()}")
            assert endTime > 0
        }
        getLOG().info("Time taken to process $eventCount events for multi attribute asset = ${endTime-startTime}ms")

        cleanup: "output processing time"
        if (client != null) {
            client.disconnect()
        }
    }

    def "Attempt to saturate the asset processing chain"() {
        given: "the container environment is started"
        def eventCount = 5
        def startTime = -1l
        def endTime = -1l
        List<SharedEvent> receivedEvents = new CopyOnWriteArrayList<>()
        List<Object> receivedValues = new CopyOnWriteArrayList<>()
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assets = new ArrayList<Asset>()

        and: "assets are added for testing purposes"
        for (i in 1..eventCount) {
            def asset = assetStorageService.merge(new ThingAsset("TestThing$i").setRealm(keycloakTestSetup.realmBuilding.name).addAttributes(
                    new Attribute<Object>("counter", ValueType.NUMBER)
            ))
            assets.add(asset)
        }

        when: "attribute events are sent in a continuous stream with intermittent merge requests also"
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        for (def i=0; i<10000; i++) {
            getLOG().info("Event loop: $i")
            for (j in 1..eventCount) {
                executor.submit {assetProcessingService.sendAttributeEvent(new AttributeEvent(assets.get(j-1).id, "counter", i))}
            }
            if (i % 9 == 0) {
                for (j in 1..eventCount) {
                    getLOG().info("Modifying asset: $j")
                    def asset = assets.get(j - 1)
                    asset.setName("TestThing${j}_${i}")
                    executor.submit {
                        assets.set(j - 1, assetStorageService.merge(asset))
                    }
                }
            }
        }

        then: "the test is terminated"
        assert true
    }
}
