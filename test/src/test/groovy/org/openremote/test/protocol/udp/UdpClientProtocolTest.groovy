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
package org.openremote.test.protocol.udp

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramChannel
import io.netty.handler.codec.FixedLengthFrameDecoder
import io.netty.handler.codec.MessageToMessageEncoder
import io.netty.handler.codec.bytes.ByteArrayDecoder
import org.openremote.agent.protocol.udp.UDPAgent
import org.openremote.model.asset.agent.Agent
import org.openremote.agent.protocol.udp.AbstractUDPServer
import org.openremote.agent.protocol.udp.UDPProtocol
import org.openremote.agent.protocol.udp.UDPStringServer
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.agent.DefaultAgentLink
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.*
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.query.filter.ValueAnyPredicate
import org.openremote.model.value.SubStringValueFilter
import org.openremote.model.value.ValueFilter
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.value.MetaItemType.*
import static org.openremote.model.value.ValueType.*

class UdpClientProtocolTest extends Specification implements ManagerContainerTrait {

    def "Check UDP client protocol and linked attribute deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        expect: "the system settles down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 300)
        }

        when: "a simple UDP echo server is started"
        def echoServerPort = findEphemeralPort()
        def clientPort = findEphemeralPort()
        AbstractUDPServer echoServer = new UDPStringServer(new InetSocketAddress("127.0.0.1", echoServerPort), ";", Integer.MAX_VALUE, true)
        def echoSkipCount = 0
        def clientActualPort = null
        def lastCommand = null
        def lastSend = null
        def receivedMessages = []
        echoServer.addMessageConsumer({
            message, channel, sender ->
                clientActualPort = sender.port
                lastCommand = message
                receivedMessages.add(message)

                if (echoSkipCount == 0) {
                    echoServer.sendMessage(message + ";", sender)
                    lastSend = message
                } else {
                    echoSkipCount--
                }
        })
        echoServer.start()

        then: "the UDP echo server should be connected"
        conditions.eventually {
            assert echoServer.connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "a UDP client agent is created"
        def agent = new UDPAgent("Test agent")
        agent.setRealm(Constants.MASTER_REALM)
            .setHost("127.0.0.1")
            .setPort(echoServerPort)
            .setBindPort(clientPort)
            .setMessageDelimiters([";"] as String[])
            .setMessageStripDelimiter(true)

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol instance should be created and should become connected"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert agentService.agentMap.get(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "an asset is created with attributes linked to the agent"
        def asset = new ThingAsset("Test Asset")
            .setParent(agent)
            .addOrReplaceAttributes(
            new Attribute<>("startHello", EXECUTION_STATUS)
                .addMeta(
                    new MetaItem<>(AGENT_LINK, new DefaultAgentLink(agent.id)
                        .setWriteValue("abcdef"))
                ),
            new Attribute<>("echoHello", TEXT)
                .addMeta(
                    new MetaItem<>(AGENT_LINK, new DefaultAgentLink(agent.id)
                        .setWriteValue('Hello {$value};'))
                ),
            new Attribute<>("echoWorld", TEXT)
                .addMeta(
                    new MetaItem<>(AGENT_LINK, new DefaultAgentLink(agent.id)
                    .setWriteValue("World;"))
                ),
            new Attribute<>("responseHello", TEXT)
                .addMeta(
                    new MetaItem<>(AGENT_LINK, new DefaultAgentLink(agent.id)
                        .setMessageMatchPredicate(
                            new StringPredicate(AssetQuery.Match.BEGIN, true, "Hello"))
                        )
                ),
            new Attribute<>("responseWorld", TEXT)
                .addMeta(
                    new MetaItem<>(AGENT_LINK, new DefaultAgentLink(agent.id)
                        .setMessageMatchPredicate(
                            new StringPredicate(AssetQuery.Match.BEGIN, true, "Hello"))
                    )
                ),
            new Attribute<>("updateOnWrite", TEXT)
                .addMeta(
                    new MetaItem<>(AGENT_LINK, new DefaultAgentLink(agent.id)
                        .setUpdateOnWrite(true)
                            .setValueFilters([
                                    new SubStringValueFilter(0, -1)
                            ] as ValueFilter[])
                    )
                ),
            new Attribute<>("anyValue", TEXT)
                .addMeta(
                    new MetaItem<>(AGENT_LINK, new DefaultAgentLink(agent.id)
                        .setMessageMatchPredicate(
                            new ValueAnyPredicate())
                    )
                )
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "the attributes should be linked"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id).linkedAttributes.size() == 7
            assert ((UDPProtocol)agentService.getProtocolInstance(agent.id)).protocolMessageConsumers.size() == 3
        }

        when: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset.id,
            "echoHello",
            "there")
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request"
        conditions.eventually {
            assert receivedMessages.indexOf("Hello there") >= 0
        }

        and: "the server should have responded and the responseWorld attribute should be updated"
        conditions.eventually {
            asset = assetStorageService.find(asset.id)
            assert asset.getAttribute("responseWorld").flatMap{it.getValue()}.orElse(null) == "Hello there"
        }

        when: "the update on write attribute is written to which wouldn't generate a response from the server"
        echoSkipCount = 1
        attributeEvent = new AttributeEvent(asset.id,
                "updateOnWrite",
                "No response;")
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request but not responded"
        conditions.eventually {
            assert receivedMessages.indexOf("No response") >= 0
        }

        and: "the attribute value should have been updated"
        conditions.eventually {
            asset = assetStorageService.find(asset.id)
            assert asset.getAttribute("updateOnWrite").flatMap{it.getValue()}.orElse(null) == "No response"
        }

        when: "the server sends a message to the client"
        echoServer.sendMessage("anyValueTest;", new InetSocketAddress("127.0.0.1", clientPort))

        then: "the any value message match attribute should be updated"
        conditions.eventually {
            asset = assetStorageService.find(asset.id)
            assert asset.getAttribute("anyValue").flatMap{it.getValue()}.orElse(null) == "anyValueTest"
        }

        when: "the agent is disabled"
        agent.setDisabled(true)
        agent = assetStorageService.merge(agent)

        then: "the protocol instance should be unlinked"
        conditions.eventually {
            assert !agentService.protocolInstanceMap.containsKey(agent.id)
        }

        when: "the received messages are cleared"
        receivedMessages.clear()

        then: "after a while no more messages should be received by the server"
        new PollingConditions(timeout: 5, initialDelay: 1).eventually {
            assert receivedMessages.isEmpty()
        }

        when: "the agent is re-enabled"
        agent.setDisabled(false)
        agent = assetStorageService.merge(agent)

        then: "the attributes should be re-linked"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert agentService.getProtocolInstance(agent.id).linkedAttributes.size() == 7
            assert ((UDPProtocol)agentService.getProtocolInstance(agent.id)).protocolMessageConsumers.size() == 3
        }

        when: "the echo server is changed to a byte based server"
        echoServer.stop()
        echoServer.removeAllMessageConsumers()
        echoServer = new AbstractUDPServer<byte[]>(new InetSocketAddress(echoServerPort)) {

            @Override
            protected void addDecoders(DatagramChannel channel) {
                addDecoder(channel, new FixedLengthFrameDecoder(3))
                addDecoder(channel, new ByteArrayDecoder())
            }

            @Override
            protected void addEncoders(DatagramChannel channel) {
                addEncoder(channel, new MessageToMessageEncoder<byte[]>() {

                    @Override
                    protected void encode(ChannelHandlerContext channelHandlerContext, byte[] bytes, List<Object> out) throws Exception {
                        out.add(Unpooled.copiedBuffer(bytes))
                    }
                })
            }
        }
        byte[] lastBytes = null
        echoServer.addMessageConsumer({
            message, channel, sender ->
                lastBytes = message
        })
        echoServer.start()

        then: "the server should be connected"
        conditions.eventually {
            assert echoServer.connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "the agent is updated to use HEX mode"
        agent.setMessageDelimiters(null)
        agent.setMessageConvertHex(true)
        agent = assetStorageService.merge(agent)

        then: "the protocol should be relinked"
        conditions.eventually {
            assert agentService.agentMap.get(agent.id) != null
            assert ((UDPAgent)agentService.agentMap.get(agent.id)).getMessageConvertHex().orElse(false)
            assert agentService.getProtocolInstance(agent.id) != null
            assert agentService.getProtocolInstance(agent.id).linkedAttributes.size() == 7
            assert ((UDPProtocol)agentService.getProtocolInstance(agent.id)).protocolMessageConsumers.size() == 3
        }

        and: "the protocol should become CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.id, Agent.class)
            assert agent.getAgentStatus().orElse(ConnectionStatus.DISCONNECTED) == ConnectionStatus.CONNECTED
        }

        when: "the echo world attribute is also updated to work with hex server"
        asset = assetStorageService.find(asset.id)
        asset.getAttribute("echoWorld").get().getMetaValue(AGENT_LINK).get().setWriteValue("123456")
        asset = assetStorageService.merge(asset)

        then: "the attribute should be updated"
        asset.getAttribute("echoWorld").get().getMetaValue(AGENT_LINK).get().getWriteValue().get() == "123456"

        then: "the attributes should be relinked"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert agentService.getProtocolInstance(agent.id).linkedAttributes.size() == 7
            assert ((UDPProtocol)agentService.getProtocolInstance(agent.id)).protocolMessageConsumers.size() == 3
            assert agentService.getProtocolInstance(agent.id).linkedAttributes.get(new AttributeRef(asset.getId(), "echoWorld")).getMetaItem(AGENT_LINK).flatMap{it.value}.flatMap{it.writeValue}.orElse(null) == "123456"
        }

        when: "the start hello linked attribute is executed"
        attributeEvent = new AttributeEvent(asset.id,
            "startHello",
            AttributeExecuteStatus.REQUEST_START)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the bytes should be received by the server"
        conditions.eventually {
            assert lastBytes != null
            assert lastBytes.length == 3
            assert (lastBytes[0] & 0xFF) == 171
            assert (lastBytes[1] & 0xFF) == 205
            assert (lastBytes[2] & 0xFF) == 239
        }

        cleanup: "the server should be stopped"
        if (echoServer != null) {
            echoServer.stop()
        }
    }
}
