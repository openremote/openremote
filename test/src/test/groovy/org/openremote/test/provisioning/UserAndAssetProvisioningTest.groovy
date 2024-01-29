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
import io.netty.channel.socket.SocketChannel
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.RoleRepresentation
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.provisioning.ProvisioningService
import org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.WeatherAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.provisioning.*
import org.openremote.model.security.ClientRole
import org.openremote.model.security.Realm
import org.openremote.model.security.User
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import org.openremote.setup.integration.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString
import static org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler.*
import static org.openremote.model.value.ValueType.BOOLEAN
import static org.openremote.model.value.ValueType.NUMBER

class UserAndAssetProvisioningTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Check basic functionality"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        MQTT_IOClient device1Client
        MQTT_IOClient deviceNClient

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def provisioningService = container.getService(ProvisioningService.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def clientEventService = container.getService(ClientEventService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def userAssetProvisioningMQTTHandler = mqttBrokerService.customHandlers.find {it instanceof UserAssetProvisioningMQTTHandler} as UserAssetProvisioningMQTTHandler
        def mqttHost = mqttBrokerService.host
        def mqttPort = mqttBrokerService.port

        and: "a realm is created with some custom realm roles"
        def realm = new Realm()
        realm.setName("test")
        realm.setDisplayName("Test")
        realm.setEnabled(true);
        realm.setDuplicateEmailsAllowed(true);
        realm.setRememberMe(true)
        realm = identityService.getIdentityProvider().createRealm(realm)
        (identityService.getIdentityProvider() as KeycloakIdentityProvider).getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm.getName())
            realmResource.roles().create(new RoleRepresentation("installer", "Installer", false))
            realmResource.roles().create(new RoleRepresentation("home-owner", "Home owner", false))
        })

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
                        .setCACertPEM(getClass().getResource("/org/openremote/test/provisioning/ca.pem").text)
        ).setAssetTemplate(
                ValueUtil.asJSON(
                        new WeatherAsset("Weather Asset")
                            .addAttributes(
                                new Attribute<>("customAttribute", NUMBER).addMeta(
                                        new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
                                        new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE)
                                ),
                                new Attribute<>("serialNumber", ValueType.TEXT, UNIQUE_ID_PLACEHOLDER),
                                new Attribute<>("connected", BOOLEAN).addMeta(
                                        new MetaItem<>(MetaItemType.USER_CONNECTED, PROVISIONING_USER_PREFIX + "device1")
                                )
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
        RemotingConnection connection = null
        def device1UniqueId = "device1"
        def mqttDevice1ClientId = UniqueIdentifierGenerator.generateId("device1")
        List<String> subscribeFailures = []
        List<ConnectionStatus> connectionStatuses = []
        Consumer<String> subscribeFailureCallback = {String topic -> subscribeFailures.add(topic)}
        device1Client = new MQTT_IOClient(mqttDevice1ClientId, mqttHost, mqttPort, false, false, null, null, null)
        device1Client.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        device1Client.addConnectionStatusConsumer({connectionStatus ->
            connectionStatuses.add(connectionStatus)})
        device1Client.connect()

        then: "mqtt client should be connected"
        conditions.eventually {
            assert device1Client.getConnectionStatus() == ConnectionStatus.CONNECTED
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
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config"
        def existingConnection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
        device1Client.sendMessage(
            new MQTTMessage<String>(device1RequestTopic, ValueUtil.asJSON(
                    new X509ProvisioningMessage(getClass().getResource("/org/openremote/test/provisioning/device1.pem").text)
            ).orElse(null))
        )

        then: "the broker should have published to the response topic a success message containing the provisioned asset"
        String weatherAssetID
        Asset<WeatherAsset> weatherAsset
        conditions.eventually {
            assert device1Responses.size() == 1
            assert device1Responses.get(0) instanceof SuccessResponseMessage
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingName
            weatherAssetID = ((SuccessResponseMessage)device1Responses.get(0)).asset.id
            weatherAsset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert weatherAsset != null
            assert weatherAsset instanceof WeatherAsset
            assert weatherAsset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        and: "the connection should have been maintained"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 0.5).eventually {
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) == existingConnection
        }

        when: "the client gets abruptly disconnected"
        device1Responses.clear()
        existingConnection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
//        ((NioSocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)device1Client.client).clientConfig).delegate).connectionConfig.get()).channel).config().setOption(ChannelOption.SO_LINGER, 0I)
        ((SocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)device1Client.client).clientConfig).delegate).connectionConfig.get()).channel).close()

        then: "the client should reconnect"
        conditions.eventually {
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != null
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) !== existingConnection
        }

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(device1ResponseTopic) != null
            assert device1Client.topicConsumerMap.get(device1ResponseTopic).size() == 1
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != null
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
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingName
            weatherAsset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert weatherAsset != null
            assert weatherAsset instanceof WeatherAsset
            assert weatherAsset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        and: "the connected attribute of the provisioned asset should show as connected"
        conditions.eventually {
            weatherAsset = assetStorageService.find(weatherAssetID)
            assert weatherAsset.getAttribute("connected").flatMap{it.value}.orElse(false)
        }

        when: "the connected attribute user name is changed to something invalid"
        weatherAsset.getAttribute("connected").ifPresent{it.addOrReplaceMeta(new MetaItem<>(MetaItemType.USER_CONNECTED, "invalidUser"))}
        weatherAsset = assetStorageService.merge(weatherAsset)

        then: "the connected attribute should now be disconnected"
        conditions.eventually {
            weatherAsset = assetStorageService.find(weatherAssetID)
            assert !weatherAsset.getAttribute("connected").flatMap{it.value}.orElse(true)
        }

        when: "the connected attribute user name is changed to upper case"
        weatherAsset.getAttribute("connected").ifPresent{it.addOrReplaceMeta(new MetaItem<>(MetaItemType.USER_CONNECTED, PROVISIONING_USER_PREFIX + device1UniqueId.toUpperCase()))}
        weatherAsset = assetStorageService.merge(weatherAsset)

        then: "the connected attribute should still find the right user and be connected"
        conditions.eventually {
            weatherAsset = assetStorageService.find(weatherAssetID)
                assert weatherAsset.getAttribute("connected").flatMap{it.value}.orElse(false)
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
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 2
        }

        when: "the client updates one of the provisioned asset's attributes"
        device1Client.sendMessage(
            new MQTTMessage<String>(
                    "$provisioningConfig.realm/$mqttDevice1ClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/customAttribute/${asset.id}",
                    "99"
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
            assert attributeEvents.get(0).id == asset.id
            assert attributeEvents.get(0).name == "customAttribute"
            assert attributeEvents.get(0).value.orElse(0d) == 99d
        }

        when: "the provisioned asset is modified"
        asset.name = "New Asset Name"
        asset = assetStorageService.merge(asset)

        then: "the client should have been notified about the asset change and all attributes should have generated an attribute event"
        conditions.eventually {
            assert assetEvents.size() == 1
            assert assetEvents.get(0).id == asset.id
            assert assetEvents.get(0).assetName == asset.name
        }

        when: "another asset's attribute is updated within the system"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment2LivingroomId, "lightSwitch", true))

        then: "the internal consumer should have been notified"
        conditions.eventually {
            assert internalAttributeEvents.find{it.id == managerTestSetup.apartment2LivingroomId && it.name == "lightSwitch" && it.value.orElse(false)} != null
        }

        and: "the client should not have been notified"
        conditions.eventually {
            assert attributeEvents.size() == 1
        }

        when: "the client disconnects"
        connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
        device1Client.disconnect()

        then: "all subscriptions should be removed and the client should be disconnected"
        conditions.eventually {
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert device1Client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        and: "the connected attribute of the provisioned asset should show as not connected"
        conditions.eventually {
            weatherAsset = assetStorageService.find (weatherAsset.id)
            assert !weatherAsset.getAttribute("connected").flatMap{it.value}.orElse(true)
        }

        when: "the client reconnects"
        device1Client.connect()

        then: "the client should be connected and subscriptions reset"
        conditions.eventually {
            assert device1Client.getConnectionStatus() == ConnectionStatus.CONNECTED
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
        }

        and: "the provisioning subscription should be re-instated but the attribute and asset subscriptions should fail"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic) == null
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic) == null
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != null
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
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
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingName
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
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 2
        }

        when: "a second device connects"
        def deviceNUniqueId = "device2"
        def deviceNRequestTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$REQUEST_TOKEN".toString()
        def deviceNResponseTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$RESPONSE_TOKEN".toString()
        def mqttDeviceNClientId = UniqueIdentifierGenerator.generateId("deviceN")
        deviceNClient = new MQTT_IOClient(mqttDeviceNClientId, mqttHost, mqttPort, false, false, null, null, null)
        deviceNClient.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
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
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
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
            assert mqttBrokerService.clientIDConnectionMap.get(mqttDeviceNClientId) == null
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "another device connects"
        deviceNResponses.clear()
        deviceNUniqueId = "device4"
        deviceNRequestTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$REQUEST_TOKEN".toString()
        deviceNResponseTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$RESPONSE_TOKEN".toString()
        deviceNClient = new MQTT_IOClient(mqttDeviceNClientId, mqttHost, mqttPort, false, false, null, null, null)
        deviceNClient.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
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
            assert mqttBrokerService.clientIDConnectionMap.get(mqttDeviceNClientId) == null
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "another device connects"
        deviceNResponses.clear()
        deviceNUniqueId = "device3"
        deviceNRequestTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$REQUEST_TOKEN".toString()
        deviceNResponseTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$RESPONSE_TOKEN".toString()
        deviceNClient = new MQTT_IOClient(mqttDeviceNClientId, mqttHost, mqttPort, false, false, null, null, null)
        deviceNClient.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
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
            assert ((SuccessResponseMessage)deviceNResponses.get(0)).realm == managerTestSetup.realmBuildingName
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
            assert mqttBrokerService.clientIDConnectionMap.get(mqttDeviceNClientId) == null
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
        }

        when: "the provisioning config is updated to disabled"
        existingConnection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
        device1Responses.clear()
        connectionStatuses.clear()
        provisioningConfig.setDisabled(true)
        provisioningConfig = provisioningService.merge(provisioningConfig)

        then: "already connected client that was authenticated should be disconnected, then reconnect and should fail to re-subscribe to asset and attribute events"
        conditions.eventually {
            assert connectionStatuses.size() == 2
            assert connectionStatuses.get(0) == ConnectionStatus.WAITING
            assert connectionStatuses.get(1) == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != null
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != existingConnection
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
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
        deviceNClient = new MQTT_IOClient(mqttDeviceNClientId, mqttHost, mqttPort, false, false, null, null, null)
        deviceNClient.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
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
            assert mqttBrokerService.clientIDConnectionMap.get(mqttDeviceNClientId) == null
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
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingName
            asset = ((SuccessResponseMessage)device1Responses.get(0)).asset
            assert asset != null
            assert asset instanceof WeatherAsset
            assert asset.getAttribute("serialNumber").flatMap{it.getValue()}.orElse(null) == device1UniqueId
        }

        when: "a connected device user is disabled and an un-connected device user is disabled"
        existingConnection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
        def device1User = identityService.getIdentityProvider().getUserByUsername(managerTestSetup.realmBuildingName, User.SERVICE_ACCOUNT_PREFIX + PROVISIONING_USER_PREFIX + device1UniqueId)
        def deviceNUser = identityService.getIdentityProvider().getUserByUsername(managerTestSetup.realmBuildingName, User.SERVICE_ACCOUNT_PREFIX + PROVISIONING_USER_PREFIX + deviceNUniqueId)
        device1User.setEnabled(false)
        deviceNUser.setEnabled(false)
        device1User = identityService.getIdentityProvider().createUpdateUser(managerTestSetup.realmBuildingName, device1User, null, true)
        deviceNUser = identityService.getIdentityProvider().createUpdateUser(managerTestSetup.realmBuildingName, deviceNUser, null, true)

        then: "already connected client that was authenticated should be disconnected, then reconnect"
        conditions.eventually {
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != null
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != existingConnection
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(existingConnection))
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
        deviceNClient = new MQTT_IOClient(mqttDeviceNClientId, mqttHost, mqttPort, false, false, null, null, null)
        deviceNClient.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).size() == 1
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
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
        existingConnection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
        def existingNConnection = mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId)
        device1User.setEnabled(true)
        device1User = identityService.getIdentityProvider().createUpdateUser(managerTestSetup.realmBuildingName, device1User, null, true)

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
            assert ((SuccessResponseMessage)device1Responses.get(0)).realm == managerTestSetup.realmBuildingName
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
