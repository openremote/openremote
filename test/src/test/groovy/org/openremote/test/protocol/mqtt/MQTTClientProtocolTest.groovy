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

import com.hivemq.client.internal.mqtt.handler.disconnect.MqttDisconnectUtil
import com.hivemq.client.internal.mqtt.message.connect.connack.MqttConnAck
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3AsyncClientView
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3ClientConfigView
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.internal.mqtt.MqttClientConnectionConfig
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.exceptions.ConnectionClosedException
import com.hivemq.client.mqtt.exceptions.ConnectionFailedException
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3DisconnectException
import com.hivemq.client.mqtt.mqtt3.lifecycle.Mqtt3ClientDisconnectedContext
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3ConnectBuilder
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAckReturnCode
import io.netty.channel.socket.SocketChannel
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.openremote.agent.protocol.mqtt.MQTTAgent
import org.openremote.agent.protocol.mqtt.MQTTAgentLink
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTTProtocol
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.container.Container
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.security.KeyStoreServiceImpl
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import org.opentest4j.TestAbortedException
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Level

import static org.openremote.agent.protocol.io.AbstractNettyIOClient.RECONNECT_DELAY_INITIAL_MILLIS
import static org.openremote.agent.protocol.io.AbstractNettyIOClient.RECONNECT_DELAY_MAX_MILLIS
import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.NUMBER

class MQTTClientProtocolTest extends Specification implements ManagerContainerTrait {

    def "Check HiveMQ client"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 180, delay: 1)

        when: "a hiveMQ client is created"
        Mqtt3AsyncClient client
        String username = "smartcity:rich"
        String password = "mGCtUhlocBFOKEroauH1hcRbyF5yXy9q"
        boolean cleanSession = false
        def subs = []
        def connect = false
        def subscriptions = [
                "smartcity/ortest/attribute/notes/2wzKB2j39144oTzAJnHpfs", // De Rotterdam
                "smartcity/ortest/attribute/notes/3b2U0Am8HrOqsp9Zozu9H9", // Erasmianum
                "smartcity/ortest/attribute/notes/4exKxPtZHl68sbAxHrBJwF", // Markthal
                "smartcity/ortest/attribute/notes/6A1jPBmMgatmIpCso9qhID", // Oostelijk
                "smartcity/ortest/attribute/notes/4vOTip49rph3bcFAOyl7yZ" // Stadhuis
        ]
        def errorCounter = 0

        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier("ortest")
                .sslConfig().applySslConfig()
                .serverHost("demo.openremote.app")
                .serverPort(8883)
                .addConnectedListener{
                    LOG.info("CONNECTION CONNECTED")
                    LOG.info("REDO SUBSCRIPTIONS")
                }
                .addDisconnectedListener{
                    ((Mqtt3ClientDisconnectedContext) it).reconnector
                        .resubscribeIfSessionExpired(!cleanSession)
                        .connectWith()
                        .simpleAuth()
                        .username(username)
                        .password(password.getBytes())
                        .applySimpleAuth()
                        .applyConnect()
                        .reconnect(connect)

                    if (!connect) {
                        return
                    }

                    if (it.cause instanceof Mqtt3DisconnectException) {
                        LOG.info("Connection disconnect error: source=" + it.source);
                    } else if (it.cause instanceof Mqtt3ConnAckException) {
                        LOG.info("Connection rejected: reasonCode=" + ((Mqtt3ConnAckException)it.cause).getMqttMessage().getReturnCode());
                    } else if (it.cause instanceof ConnectionClosedException) {
                        LOG.info("Connection closed: source=" + it.getSource());
                    } else if (it.cause instanceof ConnectionFailedException) {
                        LOG.info("Connection failed: source=" + it.source + ", message=" + it.cause.message);
                    }
                    LOG.info("Reconnecting delay=${it.reconnector.getDelay(TimeUnit.MILLISECONDS)}ms, attempt=${it.reconnector.attempts}")
                    errorCounter++
                }
                .automaticReconnect()
                .initialDelay(500, TimeUnit.MILLISECONDS)
                .maxDelay(120000, TimeUnit.MILLISECONDS)
                .applyAutomaticReconnect()
                .buildAsync()

        and: "subscriptions are added"
        // Doing subscription
//        doUnsubscribe(client, subscriptions[1])
//        subs.add(doSubscription(client, subscriptions[1], MqttQos.AT_LEAST_ONCE))
//        subs.add(doSubscription(client, subscriptions[1], MqttQos.AT_LEAST_ONCE))
//        subscriptions.forEach {
//            LOG.info("ADDING SUBSCRIPTION: ${it}")
//            subs.add(doSubscription(client, it, MqttQos.AT_LEAST_ONCE))
//            LOG.info("ADDED SUBSCRIPTION: ${it}")
//        }

        and: "the client connects"
        connect = true
        def connectFuture = client.connectWith()
            .cleanSession(cleanSession)
            .keepAlive(5)
            .simpleAuth()
            .username(username)
            .password(password.getBytes())
            .applySimpleAuth()
            .send()

            connectFuture.whenComplete(connAck, throwable) -> {
                if (connAck != null) {
                    LOG.info("Connected code=" + connAck.returnCode + ", sessionPresent=" + connAck.sessionPresent)
                } else if (throwable != null) {
                    if (throwable instanceof CancellationException) {
                        LOG.info("Connection cancelled")
                    } else {
                        LOG.info("Connection failed: " + throwable.getMessage())
                    }
                }
            }

        then: "the client should be connected"
        conditions.eventually {
            assert client.state.connected
        }

        //when: "subscription is removed"
        //doUnsubscribe(client, subscriptions[0])

        then: "subscriptions should exist"
        conditions.eventually {
            assert subs.size() == 5
        }

        when: "the client is disconnected"
        MqttDisconnectUtil.close(((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)client).clientConfig).delegate).connectionConfig.get()).channel, "Connection Error")

        then: "it should reconnect automatically"
        conditions.eventually {
            assert client.state.connected
        }

        then: "subscriptions should be recreated"
        conditions.eventually {
            assert subs.size() == 10
        }
    }

    def doDisconnect(Mqtt3AsyncClient client) {
        connect = false
        connectFuture.cancel(true)
        client.disconnect()
        LOG.info("WE DISCONNECTED")
    }

    def doSubscription(Mqtt3AsyncClient client, String topic, MqttQos qos) {
        def subAck = client.subscribeWith().topicFilter(topic).qos(qos).callback(
                publish -> {
                    String payload = new String(publish.getPayloadAsBytes())
                    LOG.info("PUBLISH: SUBSCRIBED TOPIC=${topic}, PUBLISHED=${publish.topic}, PAYLOAD=${payload}")
                }
        ).send().whenComplete(subAck, throwable) -> {
            if (throwable != null) {
                LOG.error("Failed to subscribe due to exception on topic '" + topic + "': " + throwable.getMessage())
                return false;
            }
            if (subAck.getReturnCodes().contains(Mqtt3SubAckReturnCode.FAILURE)) {
                LOG.error("Failed to subscribe due to server failure on topic: " + topic)
                return false
            }
            LOG.info("Subscribed to topic: " + topic)
            return true
        }
//        })
    }

    def doUnsubscribe(Mqtt3AsyncClient client, String topic) {
        LOG.info("Unsubscribing from topic: " + topic)
        client.unsubscribeWith()
            .topicFilter(topic)
            .send()
    }

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
        def defaultMQTTHandler = brokerService.getCustomHandlers().find{it instanceof DefaultMQTTHandler} as DefaultMQTTHandler
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
                            .setWriteValue("%VALUE%")
                    ))
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "the linked attributes should be correctly linked"
        conditions.eventually {
            assert !(agentService.getProtocolInstance(agent.id) as MQTTProtocol).protocolMessageConsumers.isEmpty()
            assert ((agentService.getProtocolInstance(agent.id) as MQTTProtocol).client as MQTT_IOClient).topicConsumerMap.size() == 1
            assert ((agentService.getProtocolInstance(agent.id) as MQTTProtocol).client as MQTT_IOClient).topicConsumerMap.get("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}".toString()) != null
            assert ((agentService.getProtocolInstance(agent.id) as MQTTProtocol).client as MQTT_IOClient).topicConsumerMap.get("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}".toString()).key == MqttQos.AT_LEAST_ONCE
            def connection = brokerService.getConnectionFromClientID(clientId)
            assert connection != null
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
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

        when: "the agent link is updated to use a different qos for the subscription"
        asset.getAttribute("readWriteTargetTemp").get().addOrReplaceMeta(
                                        new MetaItem<>(AGENT_LINK, new MQTTAgentLink(agent.id)
                                                .setSubscriptionTopic("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}")
                                                .setPublishTopic("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}")
                                                .setQos(2)
                                                .setWriteValue("%VALUE%")
                                        ))

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "the linked attributes should be correctly linked"
        conditions.eventually {
            assert !(agentService.getProtocolInstance(agent.id) as MQTTProtocol).protocolMessageConsumers.isEmpty()
            assert ((agentService.getProtocolInstance(agent.id) as MQTTProtocol).client as MQTT_IOClient).topicConsumerMap.size() == 1
            assert ((agentService.getProtocolInstance(agent.id) as MQTTProtocol).client as MQTT_IOClient).topicConsumerMap.get("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}".toString()) != null
            assert ((agentService.getProtocolInstance(agent.id) as MQTTProtocol).client as MQTT_IOClient).topicConsumerMap.get("${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC}/targetTemperature/${managerTestSetup.apartment1LivingroomId}".toString()).key == MqttQos.EXACTLY_ONCE
            def connection = brokerService.getConnectionFromClientID(clientId)
            assert connection != null
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        // The following verifies issues identified in #1864 around MQTT client subscription threading
        when: "an asset is created to assist with testing"
        def subscriptionCount = Runtime.getRuntime().availableProcessors()*2
        def testAsset = new ThingAsset("MQTTTest").setRealm(keycloakTestSetup.realmBuilding.name)
        for (i in 1..subscriptionCount) {
            testAsset.addAttributes(new Attribute<?>("attribute$i", ValueType.INTEGER))
        }
        testAsset = assetStorageService.merge(testAsset)

        then: "the asset should exist"
        testAsset.id != null

        when: "more subscriptions are added to the client than available CPU cores"
        List<Integer> messagesReceived = []
        List<String> subscriptions = []
        def clientSpy = Spy((agentService.getProtocolInstance(agent.id) as MQTTProtocol).client as MQTT_IOClient)
        clientSpy.doClientSubscription(_ as String) >> { topic ->
            subscriptions << topic
            callRealMethod()
        }
        (agentService.getProtocolInstance(agent.id) as MQTTProtocol).client = clientSpy

        for (i in 1..subscriptionCount) {
            def topic = "${keycloakTestSetup.realmBuilding.name}/$clientId/${DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC}/attribute$i/${testAsset.id}"
            clientSpy.addMessageConsumer(topic, msg -> messagesReceived.add(Integer.parseInt(msg.payload)))
        }

        then: "the subscriptions should be in place"
        conditions.eventually {
            assert clientSpy.topicConsumerMap.size() == 1 + subscriptionCount
            assert subscriptions.size() == subscriptionCount
            def connection = brokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1 + subscriptionCount
        }

        when: "events are published for each attribute"
        for (i in 1..subscriptionCount) {
            assetProcessingService.sendAttributeEvent(new AttributeEvent(testAsset.id, "attribute$i", i))
        }

        then: "they should all be received"
        conditions.eventually {
            messagesReceived.size() == subscriptionCount
        }

        when: "the client is disconnected"
        def oldConnection = brokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
        MqttDisconnectUtil.close(((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)clientSpy.client).clientConfig).delegate).connectionConfig.get()).channel, "Connection Error")

        then: "it should reconnect"
        !((SocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)clientSpy.client).clientConfig).delegate).connectionConfig.get()).channel).isOpen()
        conditions.eventually {
            assert ((SocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)clientSpy.client).clientConfig).delegate).connectionConfig.get()).channel).isOpen()
        }

        and: "subscriptions should be recreated on the broker"
        conditions.eventually {
            def connection = brokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert connection != oldConnection
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1 + subscriptionCount
        }

        when: "events are published for each attribute"
        for (i in 1..subscriptionCount) {
            assetProcessingService.sendAttributeEvent(new AttributeEvent(testAsset.id, "attribute$i", i))
        }

        then: "they should all be received"
        conditions.eventually {
            messagesReceived.size() == 2 * subscriptionCount
        }
    }


    /**
    * This test is not fully done right now, as a non-internet-requiring test would require mTLS functionality within the
    * OpenRemote broker, something that will eventually be done.
    *
    * For now, we will use test.mosquitto.org, an open broker that is provided by the creators of mosquitto. They provide
    * multiple ways of accessing the broker, including one that requires a client certificate and TLS. Using that broker,
    * we will test the mTLS and TLS functionalities.
    *
    * To access the broker, we need them to sign our certificate. To do that, we can request
    *
    * If the test cannot reach the host, then the test is passed.
    * */
    @Ignore
    @SuppressWarnings("GroovyAccessibility")
    def "Check MQTT client protocol mTLS support"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 100, delay: 0.2)

        when:
        Socket webSocket = new Socket("test.mosquitto.org", 443)
        Socket mqttSocket = new Socket("test.mosquitto.org", 8884)
        HttpClient testClient = HttpClient.newHttpClient()
        HttpRequest testRequest = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("https://test.mosquitto.org/ssl/index.php"))
                .build()

        HttpResponse<String> testResponse = testClient.send(testRequest, HttpResponse.BodyHandlers.ofString())

        then:
        if(webSocket == null) throw new TestAbortedException("Mosquitto web server unavailable");
        if(mqttSocket == null) throw new TestAbortedException("Mosquitto MQTT broker unavailable");
        if(testResponse.statusCode() != 200) throw new TestAbortedException("Mosquitto signing service unavailable");

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def keyStoreService = container.getService(KeyStoreServiceImpl.class)
        def mqttHost = "test.mosquitto.org"
        def mqttPort = 8884


        def keystorePassword = "secret"
        def keyPassword = "secret"

        def aliasName = "testalias"
        def keyAlias = Constants.MASTER_REALM + "." + aliasName

        when:
        KeyStore clientKeystore = keyStoreService.getKeyStore()
        KeyStore clientTruststore = keyStoreService.getTrustStore()

        // Create keystore and keypair
        clientKeystore = createKeystore(clientKeystore, keyAlias, keyPassword)

        // Get SSL certificate of test.mosquitto.org:8884
        def cert = getCertificate("https://test.mosquitto.org/ssl/mosquitto.org.crt")

        // Import X509 Certificate into the truststore
        clientTruststore.setCertificateEntry(keyAlias, cert)

        // Generate the CSR, to be sent to the Mosquitto server for signing, URLEncode it
        String csrPem = generateCSR(clientKeystore, keyAlias, keyPassword)
        csrPem = URLEncoder.encode(csrPem, StandardCharsets.UTF_8.toString())

        //Send the CSR to their webserver that automatically signs the certificate
        HttpClient client = HttpClient.newHttpClient()
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://test.mosquitto.org/ssl/index.php"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("csr="+csrPem))
                .build()

        // Take the response,
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

        String signedCertPem = response.body()

        if (response.statusCode() != 200) throw new Exception("Signing request failed")

        clientKeystore = importCertificate(clientKeystore, keyAlias, signedCertPem, keystorePassword.toCharArray())

        keyStoreService.storeKeyStore(clientKeystore)
        keyStoreService.storeTrustStore(clientTruststore)

        and: "an MQTT client agent is created to connect to this tests manager"
        def clientId = UniqueIdentifierGenerator.generateId()
        def agent = new MQTTAgent("Test agent")
                .setRealm(Constants.MASTER_REALM)
                .setClientId(clientId)
                .setPort(mqttPort)
                .setHost(mqttHost)
                .setSecureMode(true)
                .setCertificateAlias(aliasName)
//                .setUsernamePassword(new UsernamePassword(keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser.username, keycloakTestSetup.serviceUser.secret))
        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol should authenticate and the agent status should become CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.id, Agent.class)
            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "the agent is connected, show the SSL certificates used"
        def protocol = agent.getProtocolInstance() as MQTTProtocol
        then: "test"
        protocol.properties
    }


    KeyStore createKeystore(KeyStore keyStore, String alias, String keyPassword) {
        // Create a new keypair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        def keyPair = keyGen.generateKeyPair()

        /// Generate a self-signed certificate
        X500Name subject = new X500Name("CN=mTLS Test,OU=OpenRemote,O=OpenRemote,L=Eindhoven,ST=Noord-Brabant,C=NL")
        long now = System.currentTimeMillis()
        Date startDate = new Date(now)
        Date endDate = new Date(now + 365 * 86400000L)
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(now),
                startDate,
                endDate,
                subject,
                keyPair.getPublic()
        )


        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate())
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner))

        keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(), new X509Certificate[] { cert })

        // Store the keystore
        return keyStore
    }

    X509Certificate getCertificate(String url) {
        URL pemUrl = new URL(url)
        CertificateFactory cf = CertificateFactory.getInstance("X.509")
        X509Certificate cert = (X509Certificate) cf.generateCertificate(pemUrl.openStream())

        return cert
    }

    String generateCSR(KeyStore keyStore, String alias, String keyPassword) {

        // Retrieve the Private Key and Certificate
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray())
        Certificate cert = keyStore.getCertificate(alias)
        X509Certificate x509Cert = (X509Certificate) cert

        // Build X500Name from the Certificate's Subject
        X500Name x500Name = new X500Name(x509Cert.getSubjectX500Principal().getName())

        // Build ContentSigner
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey)

        // Build PKCS10CertificationRequest
        JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(x500Name, x509Cert.getPublicKey())
        PKCS10CertificationRequest csr = csrBuilder.build(signer)

        String csrPEM
        try (StringWriter stringWriter = new StringWriter()
             PemWriter pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded()))
            pemWriter.flush()
            csrPEM = stringWriter.toString()

        }
        return csrPEM
    }

    KeyStore importCertificate(KeyStore keyStore, String alias, String signedCertPem, char[] keystorePassword) {
        try {
            // Convert PEM to X509Certificate
            byte[] certBytes = Base64.decoder.decode(signedCertPem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", ""))
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509")
            X509Certificate signedCert = certFactory.generateCertificate(new ByteArrayInputStream(certBytes))

            // Set the certificate chain
            Certificate[] certChain = [signedCert] as Certificate[]
            keyStore.setKeyEntry(alias, keyStore.getKey(alias, keystorePassword), keystorePassword, certChain)

            return keyStore
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}
