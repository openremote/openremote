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
package org.openremote.test.provisioning

import io.moquette.BrokerConstants
import org.junit.Ignore
import org.openremote.agent.protocol.mqtt.MQTTAgent
import org.openremote.agent.protocol.mqtt.MQTTAgentLink
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTTProtocol
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.MqttBrokerService
import org.openremote.manager.provisioning.ProvisioningService
import org.openremote.manager.provisioning.UserAssetProvisioningMQTTHandler
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.agent.Protocol
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.asset.impl.WeatherAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.provisioning.ErrorResponseMessage
import org.openremote.model.provisioning.ProvisioningMessage
import org.openremote.model.provisioning.SuccessResponseMessage
import org.openremote.model.provisioning.X509ProvisioningConfig
import org.openremote.model.provisioning.X509ProvisioningData
import org.openremote.model.provisioning.X509ProvisioningMessage
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_PORT
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.NUMBER
import static org.openremote.manager.provisioning.UserAssetProvisioningMQTTHandler.*

@Ignore
class UserAndAssetProvisioningTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Check user and asset provisioning functionality"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def provisioningService = container.getService(ProvisioningService.class)
        def mqttBrokerService = container.getService(MqttBrokerService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST)
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT)

        when: "a provisioning realm config is added to the system"
        // TODO: implement config persistence and resource implementation
//        def provisioningConfig = new X509ProvisioningConfig().setData(
//                new X509ProvisioningData()
//                    .setCACertPEM("")
//            ).setAssetTemplate(
//                ValueUtil.asJSON(
//                        new WeatherAsset("Weather Asset")
//                ).orElse("")
//            )

        then: "then the config should be available in the system"
        conditions.eventually {
            assert provisioningService.getProvisioningConfigs().size() == 1
            assert provisioningService.getProvisioningConfigs().get(0) instanceof X509ProvisioningConfig
            assert ((X509ProvisioningConfig)provisioningService.getProvisioningConfigs().get(0)).getData() != null
            //assert ((X509ProvisioningConfig)provisioningService.getProvisioningConfigs().get(0)).getData().getCACertPEM() == provisioningConfig.getData().getCACertPEM()
        }

        when: "a mqtt client connects"
        def device1UniqueId = "Device1SerialNumber"
        def mqttClientId = UniqueIdentifierGenerator.generateId()
        MQTT_IOClient client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, null, null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        List<ProvisioningMessage> device1Responses = []
        Consumer<MQTTMessage<String>> device1MessageConsumer = { MQTTMessage<String> msg ->
            device1Responses.add(ValueUtil.parse(msg.payload, ProvisioningMessage.class).orElse(null))
        }
        def device1RequestTopic = "$PROVISIONING_TOKEN/$device1UniqueId/$REQUEST_TOKEN".toString()
        def device1ResponseTopic = "$PROVISIONING_TOKEN/$device1UniqueId/$RESPONSE_TOKEN".toString()
        client.addMessageConsumer(device1ResponseTopic, device1MessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert client.topicConsumerMap.get(device1ResponseTopic) != null
            assert client.topicConsumerMap.get(device1ResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttClientId) != null
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config"
        client.sendMessage(
            new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                    new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
            ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingTenant
            assert ((SuccessResponseMessage)device1Responses.get(0)).asset != null
        }


    }
}
