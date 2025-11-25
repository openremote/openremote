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
import io.undertow.security.idm.X509CertificateCredential
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal
import org.apache.activemq.artemis.spi.core.security.jaas.UserPrincipal
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.keycloak.KeycloakPrincipal
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.RoleRepresentation
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.container.persistence.PersistenceService
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler
import org.openremote.manager.provisioning.ProvisioningService
import org.openremote.manager.security.KeyStoreServiceImpl
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
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
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.security.auth.Subject
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString
import static org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler.*
import static org.openremote.model.value.ValueType.BOOLEAN
import static org.openremote.model.value.ValueType.NUMBER

class UserAndAssetProvisioningTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Check basic functionality"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.2)
        MQTT_IOClient device1Client
        MQTT_IOClient device1SnoopClient
        MQTT_IOClient deviceNClient

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def provisioningService = container.getService(ProvisioningService.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def defaultMQTTHandler = mqttBrokerService.getCustomHandlers().find{it instanceof DefaultMQTTHandler} as DefaultMQTTHandler
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
        realm.setEnabled(true)
        realm.setDuplicateEmailsAllowed(true)
        realm.setRememberMe(true)
        realm = identityService.getIdentityProvider().createRealm(realm)
        (identityService.getIdentityProvider() as KeycloakIdentityProvider).getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm.getName())
            realmResource.roles().create(new RoleRepresentation("installer", "Installer", false))
            realmResource.roles().create(new RoleRepresentation("home-owner", "Home owner", false))
        })

        and: "an internal attribute event subscriber is added for test validation purposes"
        List<AttributeEvent> internalAttributeEvents = new CopyOnWriteArrayList<>()
        Consumer<AttributeEvent> internalConsumer = { ev ->
            internalAttributeEvents.add(ev)
        }
        clientEventService.addSubscription(AttributeEvent.class, null, internalConsumer)

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
        def mqttDevice1SnoopClientId = UniqueIdentifierGenerator.generateId("device1snoop")
        List<String> subscribeFailures = new CopyOnWriteArrayList<>()
        List<ConnectionStatus> connectionStatuses = new CopyOnWriteArrayList<>()
        Consumer<String> subscribeFailureCallback = {String topic ->
            subscribeFailures.add(topic)
            LOG.info("device1Client failed to subscribe to topic: ${topic}")
        }
        Consumer<String> deviceNSubscribeFailureCallback = { String topic ->
            subscribeFailures.add(topic)
            LOG.info("deviceNClient failed to subscribe to topic: ${topic}")
        }
        device1Client = new MQTT_IOClient(mqttDevice1ClientId, mqttHost, mqttPort, false, false, null, null, null)
        device1SnoopClient = new MQTT_IOClient(mqttDevice1SnoopClientId, mqttHost, mqttPort, false, false, null, null, null)
        device1Client.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        device1Client.setRemoveConsumersOnSubscriptionFailure(true)
        device1Client.addConnectionStatusConsumer({connectionStatus ->
            LOG.info("Device 1 connection status changed: $connectionStatus")
            connectionStatuses.add(connectionStatus)})
        device1Client.connect()

        then: "mqtt client should be connected"
        conditions.eventually {
            assert device1Client.getConnectionStatus() == ConnectionStatus.CONNECTED
        }

        when: "the client subscribes to the provisioning endpoints"
        List<ProvisioningMessage> device1Responses = new CopyOnWriteArrayList<>()
        Consumer<MQTTMessage<String>> device1MessageConsumer = { MQTTMessage<String> msg ->
            device1Responses.add(ValueUtil.parse(msg.payload, ProvisioningMessage.class).orElse(null))
        }
        def device1RequestTopic = "$PROVISIONING_TOKEN/$device1UniqueId/$REQUEST_TOKEN".toString()
        def device1ResponseTopic = "$PROVISIONING_TOKEN/$device1UniqueId/$RESPONSE_TOKEN".toString()
        device1Client.addMessageConsumer(device1ResponseTopic, device1MessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert device1Client.topicConsumerMap.get(device1ResponseTopic) != null
            assert device1Client.topicConsumerMap.get(device1ResponseTopic).consumers.size() == 1
        }

        when: "an eavesdropping client connects"
        device1SnoopClient.setTopicSubscribeFailureConsumer(subscribeFailureCallback)
        device1SnoopClient.setRemoveConsumersOnSubscriptionFailure(true)
        device1SnoopClient.connect()

        then: "it should be connected"
        conditions.eventually {
            assert device1SnoopClient.getConnectionStatus() == ConnectionStatus.CONNECTED
        }

        when: "the eavesdropping client subscribes to the provisioning response topic"
        device1SnoopClient.addMessageConsumer(device1ResponseTopic, device1MessageConsumer)

        then: "the subscription should have failed"
        conditions.eventually {
            assert subscribeFailures.last == device1ResponseTopic
            assert device1SnoopClient.topicConsumerMap.get(device1ResponseTopic) == null
            assert subscribeFailures.size() == 1
        }

        and: "the actual client should still be connected and subscribed"
        conditions.eventually {
            assert device1Client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert device1Client.topicConsumerMap.get(device1ResponseTopic) != null
            assert device1Client.topicConsumerMap.get(device1ResponseTopic).consumers.size() == 1
        }

        when: "the client publishes a valid x509 certificate that has been signed by the CA stored in the provisioning config"
        subscribeFailures.clear()
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
            assert device1Client.topicConsumerMap.get(device1ResponseTopic).consumers.size() == 1
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
        List<AssetEvent> assetEvents = new CopyOnWriteArrayList<>()
        List<AttributeEvent> attributeEvents = new CopyOnWriteArrayList<>()
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
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic).consumers.size() == 1
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic).consumers.size() == 1
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 2
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
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert device1Client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert userAssetProvisioningMQTTHandler.responseSubscribedConnections.isEmpty()
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
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
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
            assert device1Client.topicConsumerMap.get(assetSubscriptionTopic).consumers.size() == 1
            assert device1Client.topicConsumerMap.get(attributeSubscriptionTopic).consumers.size() == 1
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 2
        }

        when: "a second device connects"
        def deviceNUniqueId = "device2"
        def deviceNRequestTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$REQUEST_TOKEN".toString()
        def deviceNResponseTopic = "$PROVISIONING_TOKEN/$deviceNUniqueId/$RESPONSE_TOKEN".toString()
        def mqttDeviceNClientId = UniqueIdentifierGenerator.generateId("deviceN")
        deviceNClient = new MQTT_IOClient(mqttDeviceNClientId, mqttHost, mqttPort, false, false, null, null, null)
        deviceNClient.setTopicSubscribeFailureConsumer(deviceNSubscribeFailureCallback)
        deviceNClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert deviceNClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId) != null
        }

        when: "the client subscribes to the provisioning endpoints"
        List<ProvisioningMessage> deviceNResponses = new CopyOnWriteArrayList<>()
        Consumer<MQTTMessage<String>> deviceNMessageConsumer = { MQTTMessage<String> msg ->
            deviceNResponses.add(ValueUtil.parse(msg.payload, ProvisioningMessage.class).orElse(null))
        }
        deviceNClient.addMessageConsumer(deviceNResponseTopic, deviceNMessageConsumer)

        then: "the subscriptions should succeed"
        conditions.eventually {
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic) != null
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).consumers.size() == 1
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
        deviceNClient.setTopicSubscribeFailureConsumer(deviceNSubscribeFailureCallback)
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
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).consumers.size() == 1
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
        deviceNClient.setTopicSubscribeFailureConsumer(deviceNSubscribeFailureCallback)
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
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).consumers.size() == 1
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
        subscribeFailures.clear()
        provisioningConfig.setDisabled(true)
        provisioningConfig = provisioningService.merge(provisioningConfig)

        then: "already connected client that was authenticated should be disconnected, then reconnect and should fail to re-subscribe to asset and attribute events"
        conditions.eventually {
            assert connectionStatuses.size() >= 1
            assert connectionStatuses.get(0) == ConnectionStatus.CONNECTING
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != null
            assert mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId) != existingConnection
            connection = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId)
            assert connection != null
            subscribeFailures.size() == 2
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
        deviceNClient.setTopicSubscribeFailureConsumer(deviceNSubscribeFailureCallback)
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
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).consumers.size() == 1
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
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(existingConnection))
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
        deviceNClient.setTopicSubscribeFailureConsumer(deviceNSubscribeFailureCallback)
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
            assert deviceNClient.topicConsumerMap.get(deviceNResponseTopic).consumers.size() == 1
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
        mqttBrokerService.getConnectionFromClientID(mqttDeviceNClientId)
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
        if (device1SnoopClient != null) {
            device1SnoopClient.disconnect()
        }
        if (deviceNClient != null) {
            deviceNClient.disconnect()
        }
        if (internalConsumer != null) {
            clientEventService.removeSubscription {internalConsumer}
        }
    }

    @SuppressWarnings("GroovyAccessibility")
    def "mTLS MQTT Autoprovisioning integration test"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.1)
        MQTT_IOClient device1Client

        and: "temporary directories are created"
        def tempDir = new File(System.getProperty("java.io.tmpdir"), "openremote-mtls-test-keystores-" + System.currentTimeMillis())
        def tempManagerDir = new File(System.getProperty("java.io.tmpdir"), "openremote-tmp-" + System.currentTimeMillis())
        tempDir.mkdirs()
        tempManagerDir.mkdirs()
        getLOG().info("KeyStore TempDir: $tempDir.absoluteFile")
        getLOG().info("Manager TempDir: $tempManagerDir.absoluteFile")

        and: "test configuration is set up"
        def serverKeystorePath = new File(tempDir, "server-keystore.p12").absolutePath
        def serverTruststorePath = new File(tempDir, "server-truststore.p12").absolutePath
        def keystorePassword = "secret1"
        def provisionedAccountAliasName = "mtlsclient"
        def provisionedAccountKeyAlias = "$Constants.MASTER_REALM.$provisionedAccountAliasName"
        def ProvisionedAccountUserName = "mtlstest2"

        and: "mTLS certificate helper is initialized"
        def mtlsHelper = new MTLSCertificateHelper()

        and: "server and client certificates are generated"
        def (serverKeyPair, serverCert) = mtlsHelper.generateServerCertificate()
        def (clientKeyPair, clientCert) = mtlsHelper.generateClientCertificate(ProvisionedAccountUserName, Constants.MASTER_REALM)

        and: "server keystores are created and saved to disk"
        mtlsHelper.createAndSaveServerKeystores(
            serverKeystorePath,
            serverTruststorePath,
            keystorePassword,
            provisionedAccountKeyAlias,
            serverKeyPair,
            serverCert
        )

        and: "the container configuration is set up with MTLS environment variables"
        def config = defaultConfig()
        config.put(Constants.OR_MQTT_MTLS_SERVER_LISTEN_HOST, "localhost")
        config.put(Constants.OR_MQTT_MTLS_SERVER_LISTEN_PORT, "8884")
        config.put(Constants.OR_MQTT_MTLS_DISABLED, "false")
        config.put(Constants.OR_MQTT_MTLS_KEYSTORE_PATH, serverKeystorePath)
        config.put(Constants.OR_MQTT_MTLS_KEYSTORE_PASSWORD, keystorePassword)
        config.put(Constants.OR_MQTT_MTLS_TRUSTSTORE_PATH, serverTruststorePath)
        config.put(Constants.OR_MQTT_MTLS_TRUSTSTORE_PASSWORD, keystorePassword)
        config.put(KeyStoreServiceImpl.OR_KEYSTORE_PASSWORD, keystorePassword)
        config.put(PersistenceService.OR_STORAGE_DIR, tempManagerDir.getAbsolutePath())


        and: "the container starts"
        def container = startContainer(config, defaultServices())
        def keystoreService = container.getService(KeyStoreServiceImpl.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def provisioningService = container.getService(ProvisioningService.class)
        def mqttHost = "localhost"
        def mqttPort = 8884
        when: "A new keypair, properly signed, is created, but the service user for it is not created"
        def unprovisionedUsername = "unprovisioneduser"
        def (unprovisionedKeyPair, unprovisionedCert) = mtlsHelper.generateClientCertificate(
            unprovisionedUsername,
            Constants.MASTER_REALM,
            4 // serial offset
        )

        and: "It is added and saved to the KeyStoreService keystore"
        def unprovisionedKeyAlias = Constants.MASTER_REALM + "unprovisionedclient"
        mtlsHelper.addClientCertificateToKeyStoreService(
            keystoreService,
            unprovisionedKeyAlias,
            keystorePassword,
            unprovisionedKeyPair,
            unprovisionedCert
        )

        and: "a client with unprovisioned certificate connects"
        String unprovisionedClientId = "unprovisionedDevice"
        MQTT_IOClient unprovisionedClient = new MQTT_IOClient(
                unprovisionedClientId,
                mqttHost,
                mqttPort,
                true,
                false,
                null,
                null,
                null,
                keystoreService.getKeyManagerFactory(unprovisionedKeyAlias),
                keystoreService.getTrustManagerFactory()
        )

        and: "the client connects"
        unprovisionedClient.connect();

        then: "The client connects with 3 principals"
        conditions.eventually {
            Subject subject = mqttBrokerService.getConnectionFromClientID(unprovisionedClientId).getSubject();
            assert subject.getPrincipals().size() == 3
            assert subject.getPrivateCredentials(X509CertificateCredential.class).size() == 1
        }

        when: "the provisioning config is created to allow autoprovisioning using mTLS"

        def provisioningConfig = new X509ProvisioningConfig("Valid Test Config",
                new X509ProvisioningData()
                        .setCACertPEM(MTLSCertificateHelper.getPemString(mtlsHelper.getRootCACertificate()))
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
        ).setRealm(Constants.MASTER_REALM)
                .setRestrictedUser(true)
                .setUserRoles([
                        ClientRole.WRITE_ASSETS,
                        ClientRole.WRITE_ATTRIBUTES,
                        ClientRole.READ_ASSETS
                ] as ClientRole[])
        provisioningConfig = provisioningService.merge(provisioningConfig)


        and: "We publish a message to the autoprovisioning message topic"

        def autoProvisioningRequestTopic = "$PROVISIONING_TOKEN/$unprovisionedUsername/$REQUEST_TOKEN".toString()
        def autoProvisioningResponseTopic = "$PROVISIONING_TOKEN/$unprovisionedUsername/$RESPONSE_TOKEN".toString()
        List<ProvisioningMessage> autoProvisioningResponses = new CopyOnWriteArrayList<>();
        Consumer<MQTTMessage<String>> autoProvisioningMessageConsumer = { MQTTMessage<String> msg ->
            autoProvisioningResponses.add(ValueUtil.parse(msg.payload, ProvisioningMessage.class).orElse(null))
        }
        unprovisionedClient.addMessageConsumer(autoProvisioningResponseTopic, autoProvisioningMessageConsumer);
        unprovisionedClient.sendMessage(
            new MQTTMessage<String>(autoProvisioningRequestTopic, ValueUtil.JSON.writeValueAsString(new MTLSProvisioningMessage()))
        )

        then: "we should receive a success response provisioning message"
        conditions.eventually {
            assert autoProvisioningResponses.size() == 1
            assert autoProvisioningResponses.get(0) instanceof SuccessResponseMessage
        }

        when: "the device disconnects"
        unprovisionedClient.disconnect()

        and: "reconnects again"
        autoProvisioningResponses.clear()
        unprovisionedClient.connect()

        then: "the connection should be created with the provisioned user and roles"
        conditions.eventually {
            assert unprovisionedClient.getConnectionStatus() == ConnectionStatus.CONNECTED
        }

        and: "the authenticated client should have the correct subject with roles"
        conditions.eventually {
            Subject unprovisionedJaasSubject = mqttBrokerService.getConnectionFromClientID(unprovisionedClientId).getSubject()
            assert unprovisionedJaasSubject != null
            // Should have role principals since service user should now exist
            assert unprovisionedJaasSubject.getPrincipals(RolePrincipal.class).size() == 3
            assert unprovisionedJaasSubject.getPrincipals(RolePrincipal.class).stream().map {rp -> rp.getName()}.toList().containsAll(
                    Constants.WRITE_ASSETS_ROLE,
                    Constants.WRITE_ATTRIBUTES_ROLE,
                    Constants.READ_ASSETS_ROLE
            )
            // Should have a UserPrincipal and KeycloakPrincipal
            assert unprovisionedJaasSubject.getPrincipals(UserPrincipal.class).size() == 1
            assert unprovisionedJaasSubject.getPrincipals(UserPrincipal.class)[0].getName() == "$User.SERVICE_ACCOUNT_PREFIX$unprovisionedUsername"
            assert unprovisionedJaasSubject.getPrincipals(KeycloakPrincipal.class).size() == 1
            assert unprovisionedJaasSubject.getPrincipals(KeycloakPrincipal.class)[0].getName() == "$User.SERVICE_ACCOUNT_PREFIX$unprovisionedUsername"
            // Should have a ProvisioningPrincipal with the certificate
            assert unprovisionedJaasSubject.getPrivateCredentials(X509CertificateCredential.class).size() == 1
            def provisioningPrincipal = unprovisionedJaasSubject.getPrivateCredentials(X509CertificateCredential.class)[0]
            assert provisioningPrincipal.getCertificate() != null
            assert provisioningPrincipal.getCertificate().getSubjectX500Principal().getName().contains("CN=$unprovisionedUsername")
        }

        cleanup: "disconnect the client and cleanup temporary files"
        unprovisionedClient.disconnect()

        and: "delete the created temporary directories"
        tempDir.deleteDir()
        tempManagerDir.deleteDir()
    }
}

/**
 * Helper class for managing mTLS certificates and keystores in tests
 */
@SuppressWarnings("GroovyAccessibility")
class MTLSCertificateHelper {

    KeyPair rootCAKeyPair
    X509Certificate rootCACert
    X500Name rootIssuer
    long timestamp

    /**
     * Initialize the helper and generate the Root CA
     */
    MTLSCertificateHelper() {
        this.timestamp = System.currentTimeMillis()
        generateRootCA()
    }

    /**
     * Generate Root CA key pair and certificate (self-signed)
     */
    private void generateRootCA() {
        KeyPairGenerator rootKeyGen = KeyPairGenerator.getInstance("RSA")
        rootKeyGen.initialize(4096)
        rootCAKeyPair = rootKeyGen.generateKeyPair()

        X500Name rootCASubject = new X500Name("CN=OpenRemote Root CA")
        Date rootStartDate = new Date(timestamp)
        Date rootEndDate = new Date(timestamp + 3650L * 86400000L) // ~10 years
        BigInteger rootSerialNumber = BigInteger.valueOf(timestamp)

        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(
                rootCASubject,
                rootSerialNumber,
                rootStartDate,
                rootEndDate,
                rootCASubject,
                rootCAKeyPair.getPublic()
        )

        // Add CA extensions
        rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
        rootCertBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))

        ContentSigner rootSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(rootCAKeyPair.getPrivate())
        rootCACert = new JcaX509CertificateConverter().getCertificate(rootCertBuilder.build(rootSigner))
        rootIssuer = new X500Name(rootCACert.getSubjectX500Principal().getName())
    }

    /**
     * Generate a server certificate signed by the root CA with SANs for localhost and auth.local
     */
    Tuple2<KeyPair, X509Certificate> generateServerCertificate() {
        KeyPairGenerator serverKeyGen = KeyPairGenerator.getInstance("RSA")
        serverKeyGen.initialize(2048)
        KeyPair serverKeyPair = serverKeyGen.generateKeyPair()

        X500Name serverSubject = new X500Name("CN=auth.local")
        Date serverStartDate = new Date(timestamp)
        Date serverEndDate = new Date(timestamp + 825L * 86400000L) // ~27 months
        BigInteger serverSerialNumber = BigInteger.valueOf(timestamp + 1)

        X509v3CertificateBuilder serverCertBuilder = new JcaX509v3CertificateBuilder(
                rootIssuer,
                serverSerialNumber,
                serverStartDate,
                serverEndDate,
                serverSubject,
                serverKeyPair.getPublic()
        )

        // Add server extensions (SANs + serverAuth)
        GeneralName[] sans = [
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.dNSName, "auth.local")
        ]
        serverCertBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(sans))
        serverCertBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        )
        serverCertBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth))

        ContentSigner serverSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(rootCAKeyPair.getPrivate())
        X509Certificate serverCert = new JcaX509CertificateConverter().getCertificate(serverCertBuilder.build(serverSigner))

        return new Tuple2<>(serverKeyPair, serverCert)
    }

    /**
     * Generate a client certificate signed by the root CA with specified CN and OU
     */
    Tuple2<KeyPair, X509Certificate> generateClientCertificate(String commonName, String organizationalUnit, long serialOffset = 2) {
        KeyPairGenerator clientKeyGen = KeyPairGenerator.getInstance("RSA")
        clientKeyGen.initialize(2048)
        KeyPair clientKeyPair = clientKeyGen.generateKeyPair()

        X500Name clientSubject = new X500Name("CN=$commonName,OU=$organizationalUnit")
        Date clientStartDate = new Date(timestamp)
        Date clientEndDate = new Date(timestamp + 365L * 86400000L) // 1 year
        BigInteger clientSerialNumber = BigInteger.valueOf(timestamp + serialOffset)

        X509v3CertificateBuilder clientCertBuilder = new JcaX509v3CertificateBuilder(
                rootIssuer,
                clientSerialNumber,
                clientStartDate,
                clientEndDate,
                clientSubject,
                clientKeyPair.getPublic()
        )

        // Add client extensions (clientAuth)
        clientCertBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(
                        KeyUsage.digitalSignature |
                                KeyUsage.keyEncipherment
                )
        )
        clientCertBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(
                        KeyPurposeId.id_kp_clientAuth
                )
        )

        ContentSigner clientSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(rootCAKeyPair.getPrivate())
        X509Certificate clientCert = new JcaX509CertificateConverter().getCertificate(clientCertBuilder.build(clientSigner))

        return new Tuple2<>(clientKeyPair, clientCert)
    }

    /**
     * Generate a self-signed (invalid) client certificate NOT signed by the root CA
     */
    Tuple2<KeyPair, X509Certificate> generateSelfSignedCertificate(String commonName, String organizationalUnit, long serialOffset = 3) {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        KeyPair keyPair = keyGen.generateKeyPair()

        X500Name subject = new X500Name("CN=$commonName,OU=$organizationalUnit")
        Date startDate = new Date(timestamp)
        Date endDate = new Date(timestamp + 365L * 86400000L)
        BigInteger serialNumber = BigInteger.valueOf(timestamp + serialOffset)

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, // self-signed, same subject and issuer
                serialNumber,
                startDate,
                endDate,
                subject,
                keyPair.getPublic()
        )

        // Add client extensions
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(
                        KeyUsage.digitalSignature |
                                KeyUsage.keyEncipherment
                )
        )
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(
                        KeyPurposeId.id_kp_clientAuth
                )
        )

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate())
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

        return new Tuple2<>(keyPair, cert)
    }

    /**
     * Create and save server keystores (keystore and truststore) to disk
     */
    void createAndSaveServerKeystores(
            String keystorePath,
            String truststorePath,
            String password,
            String keyAlias,
            KeyPair serverKeyPair,
            X509Certificate serverCert
    ) {
        // Create server keystore
        KeyStore serverKeystore = KeyStore.getInstance("PKCS12")
        serverKeystore.load(null, null)
        Certificate[] serverCertChain = [serverCert, rootCACert] as Certificate[]
        serverKeystore.setKeyEntry(keyAlias, serverKeyPair.getPrivate(), password.toCharArray(), serverCertChain)

        // Save server keystore to file
        new FileOutputStream(keystorePath).withCloseable { fos ->
            serverKeystore.store(fos, password.toCharArray())
        }

        // Create server truststore with the root CA
        KeyStore serverTruststore = KeyStore.getInstance("PKCS12")
        serverTruststore.load(null, null)
        serverTruststore.setCertificateEntry("client-ca", rootCACert)

        // Save server truststore to file
        new FileOutputStream(truststorePath).withCloseable { fos ->
            serverTruststore.store(fos, password.toCharArray())
        }
    }

    /**
     * Add a client certificate to the KeyStoreService's keystore and truststore
     */
    void addClientCertificateToKeyStoreService(
            KeyStoreServiceImpl keystoreService,
            String keyAlias,
            String password,
            KeyPair clientKeyPair,
            X509Certificate clientCert
    ) {
        KeyStore clientKeystore = keystoreService.getKeyStore()
        KeyStore clientTruststore = keystoreService.getTrustStore()

        // Add root CA to truststore
        clientTruststore.setCertificateEntry(keyAlias, rootCACert)

        // Add client certificate chain to keystore
        Certificate[] certChain = [clientCert, rootCACert] as Certificate[]
        clientKeystore.setKeyEntry(keyAlias, clientKeyPair.getPrivate(), password.toCharArray(), certChain)

        // Store back to KeyStoreService
        keystoreService.storeKeyStore(clientKeystore)
        keystoreService.storeTrustStore(clientTruststore)
    }

    /**
     * Add a certificate (with optional chain) to the KeyStoreService's keystore and truststore
     * This is useful for adding invalid or self-signed certificates for testing
     */
    void addCertificateToKeyStoreService(
            KeyStoreServiceImpl keystoreService,
            String keyAlias,
            String password,
            KeyPair keyPair,
            X509Certificate cert,
            boolean includeRootCA = true
    ) {
        KeyStore clientKeystore = keystoreService.getKeyStore()
        KeyStore clientTruststore = keystoreService.getTrustStore()

        // Add root CA to truststore
        clientTruststore.setCertificateEntry(keyAlias, rootCACert)

        // Add certificate chain to keystore
        Certificate[] certChain
        if (includeRootCA) {
            certChain = [cert, rootCACert] as Certificate[]
        } else {
            certChain = [cert] as Certificate[]
        }
        clientKeystore.setKeyEntry(keyAlias, keyPair.getPrivate(), password.toCharArray(), certChain)

        // Store back to KeyStoreService
        keystoreService.storeKeyStore(clientKeystore)
        keystoreService.storeTrustStore(clientTruststore)
    }

    /**
     * Get the root CA certificate (for provisioning config, etc.)
     */
    X509Certificate getRootCACertificate() {
        return rootCACert
    }

    /**
     * Get the root issuer X500Name
     */
    X500Name getRootIssuer() {
        return rootIssuer
    }
    public static String getPemString(X509Certificate certificate) throws Exception {
        StringWriter sw = new StringWriter();
        sw.write("-----BEGIN CERTIFICATE-----\n");

        // Encode the binary certificate data to Base64, with line wrapping at 64 chars
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(certificate.getEncoded());
        sw.write(base64);
        sw.write("\n-----END CERTIFICATE-----\n");

        return sw.toString();
    }
}

