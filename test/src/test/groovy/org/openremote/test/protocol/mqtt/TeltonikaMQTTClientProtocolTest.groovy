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
import com.hivemq.client.mqtt.MqttClientConfig
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.mqtt.MqttMessage
import org.junit.Ignore
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.agent.protocol.teltonika.TeltonikaProtocolDecoder
import org.openremote.agent.protocol.teltonika.TeltonikaRecord
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.TeltonikaMQTTHandler
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.telematics.TrackerAsset
import org.openremote.model.telematics.teltonika.TeltonikaValueDescriptors
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.charset.Charset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class TeltonikaMQTTClientProtocolTest extends Specification implements ManagerContainerTrait {

    def "Check MQTT client"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.1)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        mqttBrokerService.getCustomHandlers().find { it instanceof DefaultMQTTHandler } as DefaultMQTTHandler

        when: "a hiveMQ client is created to connect to our own broker"
        List<String> failedSubs = new CopyOnWriteArrayList()
        List<MQTTMessage<String>> received = new CopyOnWriteArrayList<>()
        def clientId = "teltonikatest1"
        def host = "localhost"
        def port = 1883
        def secure = false
        def imei = "123456789012345"
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
            assert msg.payload.contains('"636": 94054758')
        }

        when: "a new message comes in"
        def jsonNode = ValueUtil.JSON.readTree(payload)
        ((ObjectNode) jsonNode.get("state").get("reported")).put("67", 23456)
        def modifiedPayload = ValueUtil.JSON.writeValueAsString(jsonNode)
        client.sendMessage(new MQTTMessage<String>(subscriptions[0], modifiedPayload))

        then: "a message should be received on the receive topic"
        conditions.eventually {
            TrackerAsset asset = assetStorageService.find(UniqueIdentifierGenerator.generateId(imei), TrackerAsset.class);

            assert asset.getAttributes().get(TrackerAsset.BATTERY_VOLTAGE).get().getValue().get() == 23.456

        }

        cleanup:
        container.stop();

    }

    def "Check Teltonika Protocol Decoder with UDP mode"() {
        given: "a Teltonika Protocol Decoder in UDP mode"
        def decoder = new TeltonikaProtocolDecoder(true)
        def channel = new EmbeddedChannel(decoder)

        and: "UDP packet format data"
        def buffer = Unpooled.buffer()
        def imei = "123456789012345"

        // UDP header
        buffer.writeShort(50)           // length
        buffer.writeShort(0x1234)       // packet id
        buffer.writeByte(0x01)          // packet type
        buffer.writeByte(0x05)          // location packet id

        // IMEI
        buffer.writeShort(imei.length())
        buffer.writeBytes(imei.getBytes())

        // Codec and count
        buffer.writeByte(0x08)          // CODEC 8
        buffer.writeByte(1)             // 1 record

        // Minimal AVL record
        buffer.writeLong(System.currentTimeMillis())  // timestamp
        buffer.writeByte(0)             // priority
        buffer.writeInt(0)              // longitude
        buffer.writeInt(0)              // latitude
        buffer.writeShort(0)            // altitude
        buffer.writeShort(0)            // direction
        buffer.writeByte(0)             // satellites
        buffer.writeShort(0)            // speed
        buffer.writeByte(0)             // event ID
        buffer.writeByte(0)             // total IO count
        buffer.writeByte(0)             // 1-byte IO count
        buffer.writeByte(0)             // 2-byte IO count
        buffer.writeByte(0)             // 4-byte IO count
        buffer.writeByte(0)             // 8-byte IO count

        // Count verification
        buffer.writeByte(1)

        when: "the UDP data is written to the channel"
        channel.writeInbound(buffer)

        then: "a decoded record with IMEI should be produced"
        def record = channel.readInbound() as TeltonikaRecord
        record != null
        record.imei == imei

        and: "a UDP acknowledgment should be sent"
        def ack = channel.readOutbound() as ByteBuf
        ack != null
        ack.readableBytes() == 7
        ack.release()

        cleanup:
        channel.finish()
    }

    def "Check Teltonika Protocol Decoder with identification packet"() {
        given: "a Teltonika Protocol Decoder in TCP mode"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)

        and: "an IMEI identification packet"
        def imei = "123456789012345"
        def buffer = Unpooled.buffer()
        buffer.writeShort(imei.length())
        buffer.writeBytes(imei.getBytes())

        when: "the identification packet is written to the channel"
        channel.writeInbound(buffer)

        then: "no record should be produced (identification only)"
        def record = channel.readInbound()
        record == null

        and: "an acknowledgment byte should be sent (1 = accept)"
        def ack = channel.readOutbound() as ByteBuf
        ack != null
        ack.readableBytes() == 1
        ack.readByte() == 1
        ack.release()

        cleanup:
        channel.finish()
    }

    def "Check Teltonika Protocol Decoder with ping message"() {
        given: "a Teltonika Protocol Decoder in TCP mode"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)

        and: "a ping message (single 0xFF byte)"
        def buffer = Unpooled.buffer()
        buffer.writeByte(0xFF)

        when: "the ping is written to the channel"
        channel.writeInbound(buffer)

        then: "no record should be produced"
        def record = channel.readInbound()
        record == null

        and: "no acknowledgment should be sent for ping"
        def ack = channel.readOutbound()
        ack == null

        cleanup:
        channel.finish()
    }

    def "Check Codec8 TCP example #1"() {
        given: "a TCP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(false) // connectionless = false for TCP
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "000000000000003608010000016B40D8EA30010000000000000000000000000000000105021503010101425E0F01F10000601A014E0000000000000000010000C7CF"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "the record attributes are correct"
        assert record.timestamp == 1560161086000L
        // all attribute timestamps must equal record timestamp
        record.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record.timestamp
        }
        record.getAttributes().get(TrackerAsset.GSM_SIGNAL).get().getValue().get() == 3
        record.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == true
        Math.abs((Double) record.getAttributes().get(TrackerAsset.EXTERNAL_VOLTAGE).get().getValue().get() - 24.079) < 0.001
        record.getAttributes().get(TrackerAsset.ACTIVE_GSM_OPERATOR).get().getValue().get() == 24602L
        record.getAttributes().get(TrackerAsset.IBUTTON).get().getValue().get() == "0000000000000000"
        record.getAttributes().get(TrackerAsset.SPEED).get().getValue().get() == 0

        and: "a TCP ack is sent"
        def ack = channel.readOutbound() as ByteBuf
        assert ack?.readableBytes() == 4
        ack.readInt() == 1
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec8 TCP example #2"() {
        given: "a TCP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "000000000000002808010000016B40D9AD80010000000000000000000000000000000103021503010101425E100000010000F22A"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "the record attributes are correct"
        record.timestamp == 1560161136000L
        
        record.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record.timestamp
        }
        record.getAttributes().get(TrackerAsset.GSM_SIGNAL).get().getValue().get() == 3
        record.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == true
        Math.abs((Double) record.getAttributes().get(TrackerAsset.EXTERNAL_VOLTAGE).get().getValue().get() - 24.080) < 0.001
        record.getAttributes().get(TrackerAsset.SPEED).get().getValue().get() == 0

        and: "a TCP ack is sent"
        def ack = channel.readOutbound() as ByteBuf
        assert ack?.readableBytes() == 4
        ack.readInt() == 1
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec8 TCP multi record"() {
        given: "a TCP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "000000000000004308020000016B40D57B480100000000000000000000000000000001010101000000000000016B40D5C198010000000000000000000000000000000101010101000000020000252C"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "two records are produced"
        records.size() == 2

        and: "the first record is correct"
        def record1 = records[0]
        record1.timestamp == 1560160861000L
        record1.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record1.timestamp
        }
        record1.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == false

        and: "the second record is correct"
        def record2 = records[1]
        record2.timestamp == 1560160879000L
        record2.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record2.timestamp
        }
        record2.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == true

        and: "a TCP ack is sent for 2 records"
        def ack = channel.readOutbound() as ByteBuf
        assert ack?.readableBytes() == 4
        ack.readInt() == 2
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec8 UDP example"() {
        given: "a UDP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(true) // connectionless = true for UDP
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "003DCAFE0105000F33353230393330383634303336353508010000016B4F815B30010000000000000000000000000000000103021503010101425DBC000001"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "the record attributes are correct"
        record.imei == "352093086403655"
        
        record.timestamp == 1560407006000L
        record.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record.timestamp
        }
        record.getAttributes().get(TrackerAsset.GSM_SIGNAL).get().getValue().get() == 3
        record.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == true
        Math.abs((Double) record.getAttributes().get(TrackerAsset.EXTERNAL_VOLTAGE).get().getValue().get() - 24.0) < 0.01

        and: "a UDP ack is sent"
        def ack = channel.readOutbound() as ByteBuf
        assert ack != null
        assert ack.readableBytes() == 7
        ack.readShort() == 5 // length
        ack.readString(2, Charset.defaultCharset()) // packet id
        ack.readByte() == 1 // not usable
        ack.readByte() == 5 // avl packet id
        ack.readByte() == 1 // num accepted
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec8E TCP example"() {
        given: "a TCP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "000000000000004A8E010000016B412CEE000100000000000000000000000000000000010005000100010100010011001D00010010015E2C880002000B000000003544C87A000E000000001DD7E06A00000100002994"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "the record attributes are correct"
        record.timestamp == 1560166592000L
        
        record.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record.timestamp
        }
        record.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == true
        record.getAttributes().get(TrackerAsset.AXIS_X).get().getValue().get() == 29
        record.getAttributes().get(TrackerAsset.TOTAL_ODOMETER).get().getValue().get() == 22949000
        record.getAttributes().get(TrackerAsset.ICCID_1).get().getValue().get() == "000000003544C87A"
        record.getAttributes().get(TrackerAsset.ICCID_2).get().getValue().get() == "000000001DD7E06A"

        and: "a TCP ack is sent"
        def ack = channel.readOutbound() as ByteBuf
        assert ack?.readableBytes() == 4
        ack.readInt() == 1
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec8E UDP example"() {
        given: "a UDP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(true)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "005FCAFE0107000F3335323039333038363430333635358E010000016B4F831C680100000000000000000000000000000000010005000100010100010011009D00010010015E2C880002000B000000003544C87A000E000000001DD7E06A000001"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "the record attributes are correct"
        record.imei == "352093086403655"
        
        record.timestamp == 1560407121000
        record.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record.timestamp
        }
        record.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == true
        record.getAttributes().get(TrackerAsset.AXIS_X).get().getValue().get() == 157
        record.getAttributes().get(TrackerAsset.TOTAL_ODOMETER).get().getValue().get() == 22949000
        record.getAttributes().get(TrackerAsset.ICCID_1).get().getValue().get() == "000000003544C87A"
        record.getAttributes().get(TrackerAsset.ICCID_2).get().getValue().get() == "000000001DD7E06A"

        and: "a UDP ack is sent"
        def ack = channel.readOutbound() as ByteBuf
        assert ack != null
        assert ack.readableBytes() == 7
        ack.readShort() == 5 // length
        ack.readString(2, Charset.defaultCharset()) // packet id
        ack.readByte() == 1 // not usable
        ack.readByte() == 7 // avl packet id
        ack.readByte() == 1 // num accepted
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec16 TCP example"() {
        given: "a TCP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "000000000000005F10020000016BDBC7833000000000000000000000000000000000000B05040200010000030002000B00270042563A00000000016BDBC7871800000000000000000000000000000000000B05040200010000030002000B00260042563A00000200005FB3"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "two records are produced"
        records.size() == 2

        and: "the first record is correct"
        def record1 = records[0]
        record1.timestamp == 1562760414000L
        record1.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record1.timestamp
        }
        record1.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == false
        record1.getAttributes().get("teltonika_3").get().getValue().get() == 0
        // ICCID1 (AVL ID 11) is defined as 8 bytes but is 2 bytes in this codec, so it's skipped by the decoder.
        !record1.getAttributes().containsKey(TrackerAsset.ICCID_1.getName())
        Math.abs((Double) record1.getAttributes().get(TrackerAsset.EXTERNAL_VOLTAGE).get().getValue().get() - 22.074) < 0.001

        and: "the second record is correct"
        def record2 = records[1]
        record2.timestamp == 1562760415000L
        record2.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record2.timestamp
        }
        record2.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == false
        record2.getAttributes().get("teltonika_3").get().getValue().get() == 0
        // ICCID1 (AVL ID 11) is defined as 8 bytes but is 2 bytes in this codec, so it's skipped by the decoder.
        !record2.getAttributes().containsKey(TeltonikaValueDescriptors.iccid1)
        Math.abs((Double) record2.getAttributes().get(TrackerAsset.EXTERNAL_VOLTAGE).get().getValue().get() - 22.074) < 0.001

        and: "a TCP ack is sent for 2 records"
        def ack = channel.readOutbound() as ByteBuf
        assert ack?.readableBytes() == 4
        ack.readInt() == 2
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec16 UDP example"() {
        given: "a UDP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(true)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "015BCAFE0107000F33353230393430383532333135393210010000015117E40FE80000000000000000000000000000000000EF05050400010000030000B40000EF00010042111A000001"
//        def hexPayload = "000000000000005F10020000016BDBC7833000000000000000000000000000000000000B05040200010000030002000B00270042563A00000000016BDBC78718"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "the record attributes are correct"
        record.imei == "352094085231592"
        
        record.timestamp == 1447804801000L
        record.attributes.values().each { attr ->
            assert attr.getTimestamp().get() == record.timestamp
        }
        record.getAttributes().get(TrackerAsset.DIGITAL_INPUT_1).get().getValue().get() == false
        record.getAttributes().get("teltonika_3").get().getValue().get() == 0
        record.getAttributes().get("teltonika_180").get().getValue().get() == 0
        record.getAttributes().get(TrackerAsset.IGNITION).get().getValue().get() == false
        Math.abs((Double) record.getAttributes().get(TrackerAsset.EXTERNAL_VOLTAGE).get().getValue().get() - 4.378) < 0.001

        and: "a UDP ack is sent"
        def ack = channel.readOutbound() as ByteBuf
        assert ack != null
        assert ack.readableBytes() == 7
        ack.readShort() == 5 // length
        def packetIdHex = ByteBufUtil.hexDump(ack, ack.readerIndex(), 2)
        assert packetIdHex.toUpperCase() == "CAFE" // packet id as hex string
        ack.skipBytes(2)
        ack.readByte() == 1 // not usable
        ack.readByte() == 7 // avl packet id
        ack.readByte() == 1 // num accepted
        ack?.release()

        cleanup:
        channel.finish()
    }

    def "Check Codec12 response example"() {
        given: "a TCP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "00000000000000900C010600000088494E493A323031392F372F323220373A3232205254433A323031392F372F323220373A3533205253543A32204552523A312053523A302042523A302043463A302046473A3020464C3A302054553A302F302055543A3020534D533A30204E4F4750533A303A3330204750533A31205341543A302052533A332052463A36352053463A31204D443A30010000C78F"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "it is a command response"
        assert record.getAttributes().get("serialData").isPresent()
        record.getAttributes().get("serialData").get().getValue().get() == "INI:2019/7/22 7:22 RTC:2019/7/22 7:53 RST:2 ERR:1 SR:0 BR:0 CF:0 FG:0 FL:0 TU:0/0 UT:0 SMS:0 NOGPS:0:30 GPS:1 SAT:0 RS:3 RF:65 SF:1 MD:0"

        and: "no ack is sent for command response"
        assert channel.readOutbound() == null

        cleanup:
        channel.finish()
    }

    def "Check Codec13 response example"() {
        given: "a TCP decoder and a payload"
        def decoder = new TeltonikaProtocolDecoder(false)
        def channel = new EmbeddedChannel(decoder)
        def hexPayload = "000000000000001D0D01060000001564E8328168656C6C6F206C65747320746573740D0A0100003548"
        def buffer = hexStringToByteBuf(hexPayload)

        when: "the payload is decoded"
        channel.writeInbound(buffer)
        def records = drainRecords(channel)

        then: "one record is produced"
        records.size() == 1
        def record = records[0]

        and: "it is a command response"
        assert record.getAttributes().get("textCommand").isPresent()
        record.getAttributes().get("textCommand").get().getValue().get() == "hello lets test"


        and: "no ack is sent for command response"
        assert channel.readOutbound() == null

        cleanup:
        channel.finish()
    }

    private static List<TeltonikaRecord> drainRecords(EmbeddedChannel channel) {
        def records = []
        Object payload
        while ((payload = channel.readInbound()) != null) {
            if (payload instanceof TeltonikaRecord) {
                records << payload
            }
        }
        return records
    }

    /**
     * Helper method to convert hex string to ByteBuf
     */
    private static ByteBuf hexStringToByteBuf(String hex) {
        int len = hex.length()
        byte[] data = new byte[len / 2]
        for (int i = 0; i < len; i += 2) {
            data[(int) (i / 2)] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16))
        }
        return Unpooled.wrappedBuffer(data)
    }
}
