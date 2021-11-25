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

import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3AsyncClientView
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3ClientConfigView
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientConnectionConfig
import io.moquette.BrokerConstants
import io.netty.channel.ChannelOption
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MqttBrokerService
import org.openremote.manager.provisioning.ProvisioningService
import org.openremote.manager.provisioning.UserAssetProvisioningMQTTHandler
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.WeatherAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.provisioning.*
import org.openremote.model.security.ClientRole
import org.openremote.model.security.User
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_PORT
import static org.openremote.manager.provisioning.UserAssetProvisioningMQTTHandler.*
import static org.openremote.model.value.ValueType.NUMBER

class UserAndAssetProvisioningTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Check basic functionality"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def provisioningService = container.getService(ProvisioningService.class)
        def mqttBrokerService = container.getService(MqttBrokerService.class)
        def clientEventService = container.getService(ClientEventService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def userAssetProvisioningMQTTHandler = mqttBrokerService.customHandlers.find {it instanceof UserAssetProvisioningMQTTHandler} as UserAssetProvisioningMQTTHandler
        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST)
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT)

        and: "an internal attribute event subscriber is added for test validation purposes"
        List<AttributeEvent> internalAttributeEvents = []
        Consumer<AttributeEvent> internalConsumer = { ev ->
            internalAttributeEvents.add(ev)
        }
        clientEventService.addInternalSubscription(AttributeEvent.class, null, internalConsumer)

        // TODO: Switch to use provisioning resource once implemented
        when: "a provisioning realm config is added to the system"
        def provisioningConfig = new X509ProvisioningConfig("Valid Test Config",
                new X509ProvisioningData()
                        .setCACertPEM(getClass().getResource("/org/openremote/test/provisioning/ca_long.pem").text)
        ).setAssetTemplate(
                ValueUtil.asJSON(
                        new WeatherAsset("Weather Asset")
                            .addAttributes(
                                new Attribute<>("customAttribute", NUMBER).addMeta(
                                        new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
                                        new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE)
                                ),
                                new Attribute<>("serialNumber", ValueType.TEXT, UNIQUE_ID_PLACEHOLDER)
                            )
                ).orElse("")
        ).setRealm("building")
                .setRestrictedUser(true)
                .setUserRoles([
                        ClientRole.WRITE_ASSETS,
                        ClientRole.WRITE_ATTRIBUTES,
                        ClientRole.READ_ASSETS
                ] as ClientRole[])
        provisioningConfig = provisioningService.merge(provisioningConfig)

        then: "then the config should be available in the system"
        conditions.eventually {
            def storedConfigs = provisioningService.getProvisioningConfigs()
            assert storedConfigs.size() == 1
            assert storedConfigs.get(0) instanceof X509ProvisioningConfig
            assert ((X509ProvisioningConfig)storedConfigs.get(0)).getData() != null
            assert ((X509ProvisioningConfig)storedConfigs.get(0)).getData().getCACertPEM() == provisioningConfig.getData().getCACertPEM()
        }

        when: "a mqtt client connects"
        def device1UniqueId = "device1"
        def mqttDevice1ClientId = UniqueIdentifierGenerator.generateId("device1")
        List<String> subscribeFailures = []
        List<ConnectionStatus> connectionStatuses = []
        Consumer<String> subscribeFailureCallback = {String topic -> subscribeFailures.add(topic)}
        MQTT_IOClient device1Client = new MQTT_IOClient(mqttDevice1ClientId, mqttHost, mqttPort, false, false, null, null)
        device1Client.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        device1Client.addConnectionStatusConsumer({connectionStatus ->
            connectionStatuses.add(connectionStatus)})
        device1Client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert device1Client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        List<ProvisioningMessage> device1Responses = []
        Consumer<MQTTMessage<String>> device1MessageConsumer = { MQTTMessage<String> msg ->
            device1Responses.add(ValueUtil.parse(msg.payload, ProvisioningMessage.class).orElse(null))
        }
        def device1RequestTopic = "$PROVISIONING_TOKEN/$device1UniqueId/$REQUEST_TOKEN".toString()
        def device1ResponseTopic = "$PROVISIONING_TOKEN/$device1UniqueId/$RESPONSE_TOKEN".toString()
        device1Client.addMessageConsumer(device1ResponseTopic, device1MessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(device1ResponseTopic) != null
            assert device1Client.topicConsumerMap.get(device1ResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config"
        device1Client.sendMessage(
            new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                    new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
            ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message containing the provisioned asset"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingTenant
            def asset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert asset != null
            assert asset instanceof WeatherAsset
            assert asset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        when: "the client gets abruptly disconnected"
        device1Responses.clear()
        def existingConnection = mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId)
//        ((NioSocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)device1Client.client).clientConfig).delegate).connectionConfig.get()).channel).config().setOption(ChannelOption.SO_LINGER, 0I)
        ((SocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)device1Client.client).clientConfig).delegate).connectionConfig.get()).channel).close()

        then: "the client should reconnect"
        conditions.eventually {
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != existingConnection
        }

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(device1ResponseTopic) != null
            assert device1Client.topicConsumerMap.get(device1ResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config"
        device1Client.sendMessage(
                new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message containing the provisioned asset"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingTenant
            def asset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert asset != null
            assert asset instanceof WeatherAsset
            assert asset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        when: "the client then subscribes to attribute events for the generated asset and asset events for all assets"
        def asset = ((SuccessResponseMessage)device1Responses.get(0)).asset
        def assetSubscriptionTopic = "$provisioningConfig.realm/$mqttDevice1ClientId/$DefaultMQTTHandler.ASSET_TOPIC/#".toString()
        def attributeSubscriptionTopic = "$provisioningConfig.realm/$mqttDevice1ClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/+/$asset.id".toString()
        List<AssetEvent> assetEvents = []
        List<AttributeEvent> attributeEvents = []
        Consumer<MQTTMessage<String>> eventConsumer = { MQTTMessage<String> msg ->
            def event = ValueUtil.parse(msg.payload, SharedEvent.class).orElse(null)
            if (event instanceof AssetEvent) {
                assetEvents.add(event as AssetEvent)
            } else {
                attributeEvents.add(event as AttributeEvent)
            }
        }
        device1Client.addMessageConsumer(assetSubscriptionTopic, eventConsumer)
        device1Client.addMessageConsumer(attributeSubscriptionTopic, eventConsumer)

        then: "the subscriptions should be in place"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic) != null
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic) != null
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic).size() == 1
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttDevice1ClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttDevice1ClientId).size() == 2
        }

        when: "the client updates one of the provisioned asset's attributes"
        device1Client.sendMessage(
            new MQTTMessage<String>(
                    "$provisioningConfig.realm/$mqttDevice1ClientId/$DefaultMQTTHandler.ATTRIBUTE_WRITE_TOPIC",
                    ValueUtil.asJSON(new AttributeEvent(asset.id, "customAttribute", 99d)).orElse(null)
            )
        )

        then: "the attribute should have been updated"
        conditions.eventually {
            asset = assetStorageService.find(asset.id)
            assert asset.getAttribute("customAttribute").flatMap{it.getValue()}.orElse(0d) == 99d
        }

        and: "the client should have been notified about the attribute change"
        conditions.eventually {
            assert attributeEvents.size() == 1
            assert attributeEvents.get(0).assetId == asset.id
            assert attributeEvents.get(0).attributeName == "customAttribute"
            assert attributeEvents.get(0).value.orElse(0d) == 99d
        }

        when: "the provisioned asset is modified"
        asset.name = "New Asset Name"
        asset = assetStorageService.merge(asset)

        then: "the client should have been notified about the asset change and all attributes should have generated an attribute event"
        conditions.eventually {
            assert assetEvents.size() == 1
            assert assetEvents.get(0).assetId == asset.id
            assert assetEvents.get(0).assetName == asset.name
        }

        when: "another asset's attribute is updated within the system"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerTestSetup.apartment2LivingroomId, "lightSwitch", true)
        )

        then: "the internal consumer should have been notified"
        conditions.eventually {
            assert internalAttributeEvents.find{it.assetId == managerTestSetup.apartment2LivingroomId && it.attributeName == "lightSwitch" && it.value.orElse(false)} != null
        }

        and: "the client should not have been notified"
        conditions.eventually {
            assert attributeEvents.size() == 1
        }

        when: "the client disconnects"
        device1Client.disconnect()

        then: "all subscriptions should be removed and the client should be disconnected"
        conditions.eventually {
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) == null
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttDevice1ClientId)
            assert device1Client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "the client reconnects"
        device1Client.connect()

        then: "the client should be connected and subscriptions reset"
        conditions.eventually {
            assert device1Client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
        }

        and: "the provisioning subscription should be re-instated but the attribute and asset subscriptions should fail"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic) == null
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic) == null
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttDevice1ClientId)
            assert subscribeFailures.size() == 2
            assert subscribeFailures.contains(assetSubscriptionTopic)
            assert subscribeFailures.contains(attributeSubscriptionTopic)
        }

        when: "the client re-authenticates"
        device1Responses.clear()
        device1Client.sendMessage(
                new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message containing the provisioned asset"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingTenant
            asset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert asset != null
            assert asset instanceof WeatherAsset
            assert asset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        and: "the connection should be recorded against the provisioning config"
        conditions.eventually {
            assert userAssetProvisioningMQTTHandler.provisioningConfigAuthenticatedConnectionMap.get(provisioningConfig.id) != null
            assert userAssetProvisioningMQTTHandler.provisioningConfigAuthenticatedConnectionMap.get(provisioningConfig.id).size() == 1
        }

        when: "the client subscribes again to the asset and attributes"
        device1Client.addMessageConsumer(assetSubscriptionTopic, eventConsumer)
        device1Client.addMessageConsumer(attributeSubscriptionTopic, eventConsumer)

        then: "the subscriptions should be in place"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic) != null
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic) != null
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic).size() == 1
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttDevice1ClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttDevice1ClientId).size() == 2
        }

        when: "a second device connects"
        def deviceNUniqueId = "device2"
        def deviceNRequestTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$REQUEST_TOKEN".toString()
        def deviceNResponseTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$RESPONSE_TOKEN".toString()
        def mqttDeviceNClientId = UniqueIdentifierGenerator.generateId("deviceN")
        MQTT_IOClient deviceNClient = new MQTT_IOClient(mqttDeviceNClientId, mqttHost, mqttPort, false, false, null, null)
        deviceNClient.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        List<ProvisioningMessage> deviceNResponses = []
        Consumer<MQTTMessage<String>> deviceNMessageConsumer = { MQTTMessage<String> msg ->
            deviceNResponses.add(ValueUtil.parse(msg.payload, ProvisioningMessage.class).orElse(null))
        }
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client publishes an expired but valid x509 certificate that has been signed by the CA stored in the provisioning config"
        deviceNClient.sendMessage(
            new MQTTMessage<String>(deviceNRequestTopic, ValueUtil.asJSON(
                    new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device2_expired.pem").text)
            ).orElse(null))
        )

        then: "the broker should have published to the response topic an error message"
        conditions.eventually {
            assert deviceNResponses.size() == 1
            assert deviceNResponses.get(0) instanceof ErrorResponseMessage
            assert ((ErrorResponseMessage)deviceNResponses.get(0)).error == ErrorResponseMessage.Error.UNAUTHORIZED
        }

        when: "the client disconnects"
        deviceNClient.removeAllMessageConsumers()
        deviceNClient.disconnect()

        then: "the connection should be removed from the broker"
        conditions.eventually {
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) == null
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "another device connects"
        deviceNResponses.clear()
        deviceNUniqueId = "device4"
        deviceNRequestTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$REQUEST_TOKEN".toString()
        deviceNResponseTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$RESPONSE_TOKEN".toString()
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client publishes a forged x509 certificate that has been signed by a forged CA"
        deviceNClient.sendMessage(
                new MQTTMessage<String>(deviceNRequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device4_forged.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic an error message"
        conditions.eventually {
            assert deviceNResponses.size() == 1
            assert deviceNResponses.get(0) instanceof ErrorResponseMessage
            assert ((ErrorResponseMessage)deviceNResponses.get(0)).error == ErrorResponseMessage.Error.UNAUTHORIZED
        }

        when: "the client disconnects"
        deviceNClient.removeAllMessageConsumers()
        deviceNClient.disconnect()

        then: "the connection should be removed from the broker"
        conditions.eventually {
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) == null
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "another device connects"
        deviceNResponses.clear()
        deviceNUniqueId = "device3"
        deviceNRequestTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$REQUEST_TOKEN".toString()
        deviceNResponseTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$RESPONSE_TOKEN".toString()
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config but against an expired certificate"
        deviceNClient.sendMessage(
                new MQTTMessage<String>(deviceNRequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device3_caexpired.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message containing the provisioned asset"
        conditions.eventually {
            assert deviceNResponses.size() == 1
            assert deviceNResponses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)deviceNResponses.get(0)).realm == managerTestSetup.realmBuildingTenant
            asset = ((SuccessResponseMessage)deviceNResponses.get(0)).asset
            assert asset != null
            assert asset instanceof WeatherAsset
            assert asset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == deviceNUniqueId
        }

        when: "the client disconnects"
        deviceNClient.removeAllMessageConsumers()
        deviceNClient.disconnect()

        then: "the connection should be removed from the broker"
        conditions.eventually {
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) == null
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "the provisioning config is updated to disabled"
        existingConnection = mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId)
        device1Responses.clear()
        connectionStatuses.clear()
        provisioningConfig.setDisabled(true)
        provisioningConfig = provisioningService.merge(provisioningConfig)

        then: "already connected client that was authenticated should be disconnected, then reconnect and should fail to re-subscribe to asset and attribute events"
        conditions.eventually {
            assert connectionStatuses.size() == 2
            assert connectionStatuses.get(0) == ConnectionStatus.WAITING
            assert connectionStatuses.get(1) == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != existingConnection
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttDevice1ClientId)
        }

        when: "the re-connected client re-authenticates"
        device1Responses.clear()
        device1Client.sendMessage(
                new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
                ).orElse(null))
        )
        
        then: "the broker should have published to the response topic an error message"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof ErrorResponseMessage
            assert ((ErrorResponseMessage)device1Responses.get(0)).error == ErrorResponseMessage.Error.CONFIG_DISABLED
        }

        when: "another device re-connects"
        deviceNResponses.clear()
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config but against an expired certificate"
        deviceNClient.sendMessage(
                new MQTTMessage<String>(deviceNRequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device3_caexpired.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic an error message"
        conditions.eventually {
            assert deviceNResponses.size() == 1
            assert deviceNResponses.get(0) instanceof ErrorResponseMessage
            assert ((ErrorResponseMessage)deviceNResponses.get(0)).error == ErrorResponseMessage.Error.CONFIG_DISABLED
        }

        when: "the client disconnects"
        deviceNClient.removeAllMessageConsumers()
        deviceNClient.disconnect()

        then: "the connection should be removed from the broker"
        conditions.eventually {
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) == null
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "the provisioning config is updated to enabled"
        provisioningConfig.setDisabled(false)
        provisioningConfig = provisioningService.merge(provisioningConfig)

        and: "the already connected device publishes its' valid client certificate"
        device1Responses.clear()
        device1Client.sendMessage(
                new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message containing the provisioned asset"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingTenant
            asset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert asset != null
            assert asset instanceof WeatherAsset
            assert asset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        when: "a connected device user is disabled and an un-connected device user is disabled"
        existingConnection = mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId)
        def device1User = identityService.getIdentityProvider().getUserByUsername(managerTestSetup.realmBuildingTenant, User.SERVICE_ACCOUNT_PREFIX + PROVISIONING_USER_PREFIX + device1UniqueId)
        def deviceNUser = identityService.getIdentityProvider().getUserByUsername(managerTestSetup.realmBuildingTenant, User.SERVICE_ACCOUNT_PREFIX + PROVISIONING_USER_PREFIX + deviceNUniqueId)
        device1User.setEnabled(false)
        deviceNUser.setEnabled(false)
        device1User = identityService.getIdentityProvider().createUpdateUser(managerTestSetup.realmBuildingTenant, device1User, null)
        deviceNUser = identityService.getIdentityProvider().createUpdateUser(managerTestSetup.realmBuildingTenant, deviceNUser, null)

        then: "already connected client that was authenticated should be disconnected, then reconnect"
        conditions.eventually {
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != null
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId) != existingConnection
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttDevice1ClientId)
        }

        when: "the re-connected client re-authenticates"
        device1Responses.clear()
        device1Client.sendMessage(
                new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic an error message"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof ErrorResponseMessage
            assert ((ErrorResponseMessage)device1Responses.get(0)).error == ErrorResponseMessage.Error.USER_DISABLED
        }

        when: "a device with disabled user account connects"
        deviceNResponses.clear()
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId) != null
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config but against an expired certificate"
        deviceNClient.sendMessage(
                new MQTTMessage<String>(deviceNRequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device3_caexpired.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic an error message"
        conditions.eventually {
            assert deviceNResponses.size() == 1
            assert deviceNResponses.get(0) instanceof ErrorResponseMessage
            assert ((ErrorResponseMessage)deviceNResponses.get(0)).error == ErrorResponseMessage.Error.USER_DISABLED
        }

        when: "a client user is re-enabled"
        existingConnection = mqttBrokerService.clientIdConnectionMap.get(mqttDevice1ClientId)
        def existingNConnection = mqttBrokerService.clientIdConnectionMap.get(mqttDeviceNClientId)
        device1User.setEnabled(true)
        device1User = identityService.getIdentityProvider().createUpdateUser(managerTestSetup.realmBuildingTenant, device1User, null)

        and: "the re-enabled client device publishes its' valid client certificate"
        device1Responses.clear()
        device1Client.sendMessage(
                new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                        new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
                ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message containing the provisioned asset"
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingTenant
            asset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert asset != null
            assert asset instanceof WeatherAsset
            assert asset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        cleanup: "disconnect the clients"
        if (device1Client != null) {
            device1Client.disconnect()
        }
        if (deviceNClient != null) {
            deviceNClient.disconnect()
        }
    }
}
