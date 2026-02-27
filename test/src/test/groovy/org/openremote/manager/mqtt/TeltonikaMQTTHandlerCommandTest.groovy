package org.openremote.manager.mqtt

import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.telematics.TeltonikaMQTTHandler
import org.openremote.manager.telematics.TelematicsService
import org.openremote.manager.telematics.TeltonikaVendor
import org.openremote.model.attribute.Attribute
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.telematics.session.DeviceConnection
import org.openremote.model.telematics.core.DeviceMessage
import org.openremote.model.telematics.protocol.DeviceProtocol
import org.openremote.model.telematics.protocol.DeviceCommand
import org.openremote.model.telematics.protocol.MessageContext
import org.openremote.model.telematics.teltonika.TeltonikaAttributeResolver
import org.openremote.model.telematics.teltonika.TeltonikaRegistry
import org.openremote.model.telematics.teltonika.TeltonikaTrackerAsset
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import io.netty.buffer.Unpooled

class TeltonikaMQTTHandlerCommandTest extends Specification implements ManagerContainerTrait {

    def "maps TEXT command to CMD payload"() {
        given:
        def handler = new TeltonikaMQTTHandler()

        when:
        def payload = handler.toCommandPayload(DeviceCommand.text("getinfo"))

        then:
        payload.CMD == "getinfo"
        payload.ts instanceof Long
    }

    def "maps OUTPUT_CONTROL command to setdigout"() {
        given:
        def handler = new TeltonikaMQTTHandler()

        when:
        def payload = handler.toCommandPayload(DeviceCommand.setOutput(1, true))

        then:
        payload.CMD == "setdigout 1 1"
    }

    def "builds commands topic from routing tokens"() {
        given:
        def handler = new TeltonikaMQTTHandler()

        expect:
        handler.getCommandTopic("master", "clientA", "123456789012345") ==
                "master/clientA/teltonika/123456789012345/commands"
    }

    def "decodes MQTT RSP payload into response attribute"() {
        given:
        DeviceProtocol protocol = TeltonikaVendor.getInstance().getProtocol(MessageContext.Transport.MQTT)
        def context = new MessageContext(MessageContext.Transport.MQTT)
                .setDeviceId("123456789012345")

        def payload = '{"RSP":"OK"}'
        def buffer = Unpooled.wrappedBuffer(payload.bytes)

        when:
        List<DeviceMessage> messages = protocol.decode(buffer, context)

        then:
        messages.size() == 1
        messages[0].getAttributeValue("teltonika_response", String.class).orElse(null) == "OK"
    }

    def "processes teltonika mqtt payload end-to-end and persists all resolved attributes"() {
        given:
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def setupService = container.getService(SetupService.class)
        def keycloakTestSetup = setupService.getTaskOfType(KeycloakTestSetup.class)
        setupService.getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def telematicsService = container.getService(TelematicsService.class)
        def timerService = container.getService(TimerService.class)

        def clientId = "teltonika-e2e-client"
        def imei = "123456789012345"
        def realm = keycloakTestSetup.realmMaster.name
        def dataTopic = "${realm}/${clientId}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_TOKEN}/${imei}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_RECEIVE_TOPIC}".toString()
        def subscriptions = [
                dataTopic,
                "${realm}/${clientId}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_TOKEN}/${imei}/${TeltonikaMQTTHandler.TELTONIKA_DEVICE_SEND_TOPIC}".toString()
        ]

        def failedSubs = new CopyOnWriteArrayList<String>()
        def received = new CopyOnWriteArrayList<MQTTMessage<String>>()
        Consumer<MQTTMessage<String>> consumer = { received.add(it) }

        def client = new MQTT_IOClient(
                clientId,
                "localhost",
                1883,
                false,
                false,
                null,
                null,
                null,
                null,
                null
        )
        client.setResubscribeIfSessionPresent(false)
        client.setTopicSubscribeFailureConsumer { failedSubs.add(it) }

        def payload = '''
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
        '''.stripIndent().trim()

        and: "change the payload's timestamp to reflect a value in the near future"
        payload = payload.replace('"ts": 1072915236000', "\"ts\": ${timerService.getClock().getCurrentTimeMillis() + 30 * 1000}")

        and: "expected attribute values from resolver"
        def resolver = new TeltonikaAttributeResolver()
        def rootNode = ValueUtil.JSON.readTree(payload.bytes)
        Map<String, Object> parsedPayload = ValueUtil.JSON.convertValue(rootNode.state.reported, Map)
        def timestampRaw = parsedPayload.get("ts")
        def timestamp = timestampRaw instanceof Number ? ((Number) timestampRaw).longValue() : Long.parseLong(timestampRaw.toString())
        Map<String, Object> expectedValues = [:]
        parsedPayload.entrySet().each { entry ->
            Attribute<?> resolved = resolver.resolveJson(entry.key.toString(), entry.value, timestamp)
            expectedValues.put(resolved.name, resolved.value.orElse(null))
        }

        when: "client connects and subscribes"
        client.connect()
        client.addMessageConsumer(subscriptions[0], consumer)
        client.addMessageConsumer(subscriptions[1], consumer)

        then:
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert failedSubs.isEmpty()
        }

        when: "Teltonika payload is published"
        client.sendMessage(new MQTTMessage<String>(dataTopic, payload))

        then: "asset is provisioned and all parsed attributes are persisted"
        conditions.eventually {
            TeltonikaTrackerAsset asset = assetStorageService.find(UniqueIdentifierGenerator.generateId(imei), TeltonikaTrackerAsset.class)
            assert asset != null

            expectedValues.each { name, expected ->
                def stored = asset.getAttribute(name)
                assert stored.present : "Missing attribute '${name}'"
                def storedValue = stored.get().value.orElse(null)
                assert valuesEqual(storedValue, expected) : "Attribute '${name}' mismatch. expected=${expected}, actual=${storedValue}"
            }

            assert asset.getAttribute(TeltonikaTrackerAsset.BATTERY_VOLTAGE.name).get().value.get() == 3.466d
            assert asset.getAttribute(TeltonikaTrackerAsset.EXTERNAL_VOLTAGE.name).get().value.get() == 11.921d
            assert asset.getAttribute(
                    TeltonikaRegistry.getInstance().findMatchingAttributeDescriptor(
                            asset.getClass(),
                            TeltonikaRegistry.getInstance().getById("636").orElseThrow()
                    ).orElseThrow()
            ).get().value.get() == 94054758
        }

        and: "connection state tracks transport/protocol/codec"
        conditions.eventually {
            Optional<DeviceConnection> stateOpt = telematicsService.getTrackerState(TeltonikaMQTTHandler.TELTONIKA_DEVICE_TOKEN, imei)
            assert stateOpt.present
            def state = stateOpt.get()
            assert state.connected
            assert state.transport.get().name() == "MQTT"
            assert state.protocolId.get() == TeltonikaVendor.PROTOCOL_MQTT_JSON
            assert state.codecId.get() == TeltonikaVendor.CODEC_JSON
            assert state.messageCount > 0
            assert state.assetId.present
        }

        cleanup:
        client?.disconnect()
        container?.stop()
    }

    private static boolean valuesEqual(Object actual, Object expected) {
        if (actual == null && expected == null) {
            return true
        }
        if (actual instanceof Number && expected instanceof Number) {
            return Math.abs(((Number) actual).doubleValue() - ((Number) expected).doubleValue()) < 0.000001d
        }
        if (actual instanceof GeoJSONPoint && expected instanceof GeoJSONPoint) {
            return actual == expected
        }
        return Objects.equals(actual, expected)
    }
}
