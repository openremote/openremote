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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.hivemq.client.internal.mqtt.MqttClientConnectionConfig
import com.hivemq.client.internal.mqtt.handler.disconnect.MqttDisconnectUtil
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3AsyncClientView
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3ClientConfigView
import com.hivemq.client.mqtt.MqttClientConfig
import io.netty.handler.codec.mqtt.MqttMessage
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
import org.openremote.agent.protocol.mqtt.*
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.TeltonikaMQTTHandler
import org.openremote.manager.security.KeyStoreServiceImpl
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.query.filter.NumberPredicate
import org.openremote.model.telematics.TrackerAsset
import org.openremote.model.telematics.teltonika.TeltonikaMqttMessage
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.JsonPathFilter
import org.openremote.model.value.ValueFilter
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.*

class TeltonikaMQTTClientProtocolTest extends Specification implements ManagerContainerTrait {

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
    }

    def "Check MQTT client"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.1)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def defaultMQTTHandler = mqttBrokerService.getCustomHandlers().find {it instanceof DefaultMQTTHandler} as DefaultMQTTHandler

        when: "a hiveMQ client is created to connect to our own broker"
        List<String> failedSubs = new CopyOnWriteArrayList()
        List<MQTTMessage<String>> received = new CopyOnWriteArrayList<>()
        def clientId = "teltonikatest1"
        def host = "localhost"
        def port = 1883
        def secure = false
        def counter = 0
        def imei = "123456789012345"
        def username = "${keycloakTestSetup.realmBuilding.name}:${keycloakTestSetup.serviceUser2.username}"
        def password = keycloakTestSetup.serviceUser2.secret
        def subscriptions = [
                "${keycloakTestSetup.realmMaster.name}/${clientId}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_TOKEN}/${imei}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_RECEIVE_TOPIC}".toString(),
                "${keycloakTestSetup.realmMaster.name}/${clientId}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_TOKEN}/${imei}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_SEND_TOPIC}".toString()
        ]
        Consumer<MQTTMessage<String>> consumer = { it ->
            LOG.info("Received on topic=${it.topic}")
            received.add(it)
        }
        def client = new MQTT_IOClient(
                clientId,
                host,
                port,
                secure,
                false,
                null,
                null,
                null,
                null,
                null
        )
        client.setResubscribeIfSessionPresent(false)
        client.setTopicSubscribeFailureConsumer {
            LOG.info("Subscription failed: $it")
            failedSubs.add(it)
        }
        client.addConnectionStatusConsumer {
            LOG.info("Connection status changed: $it")
        }

        and: "the client connects"
        client.connect()

        then: "the client should be connected"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
        }

        when: "more subscriptions are added"
        client.addMessageConsumer(subscriptions[0], consumer)
        client.addMessageConsumer(subscriptions[1], consumer)

        then: "all subscriptions should be successful"
        conditions.eventually {
            assert failedSubs.size() == 0
        }

        when: "a message is published to the send topic"

        String payload = """
                       {
                        "state": {
                            "reported": {
                                "11": 893116211,
                                "12": 275,
                                "13": 217,
                                "14": 1680671168,
                                "15": 0,
                                "16": 10876,
                                "17": 65,
                                "18": 64574,
                                "19": 65479,
                                "21": 5,
                                "24": 0,
                                "66": 11921,
                                "67": 3466,
                                "68": 0,
                                "69": 2,
                                "80": 0,
                                "113": 14,
                                "181": 0,
                                "182": 0,
                                "199": 0,
                                "200": 0,
                                "206": 30,
                                "237": 2,
                                "238": 0,
                                "239": 0,
                                "240": 0,
                                "241": 20416,
                                "250": 0,
                                "263": 1,
                                "303": 0,
                                "387": "1280219134765741938828798850837816695541021889564066133288786540739018312683368495",
                                "449": 0,
                                "636": 94054758,
                                "latlng": "0,0",
                                "ts": 1072915236000,
                                "alt": "0",
                                "ang": "0",
                                "sat": "0",
                                "sp": "0"
                            }
                        }
                    }
                """.stripIndent().trim()

        client.sendMessage(new MQTTMessage<String>(subscriptions[0], payload))


        then: "a message should be received on the receive topic"
        conditions.eventually {
            assert received.size() > 0
            assert received.find { it.topic == subscriptions[0] }
            def msg = received.find { it.topic == subscriptions[0] }
            assert msg.payload.contains('"11": 893116211')
        }

        when: "a new message comes in"
        def jsonNode = ValueUtil.JSON.readTree(payload)
        ((ObjectNode) jsonNode.get("state").get("reported")).put("11", 999999999)
        def modifiedPayload = ValueUtil.JSON.writeValueAsString(jsonNode)
        client.sendMessage(new MQTTMessage<String>(subscriptions[0], modifiedPayload))

        then: "a message should be received on the receive topic"
        conditions.eventually {
            TrackerAsset asset = assetStorageService.find(UniqueIdentifierGenerator.generateId(imei));

            assert asset.getAttribute("11").get().getValue().get() == "999999999"

        }

    }
}
