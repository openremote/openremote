/*
 * Copyright 2017, OpenRemote Inc.
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

import io.netty.channel.ChannelHandler
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil
import io.netty.util.internal.SocketUtils
import org.openremote.agent.protocol.io.AbstractNettyIOClient
import org.openremote.agent.protocol.udp.UDPIOClient
import org.openremote.agent.protocol.udp.UDPStringServer
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

/**
 * This tests the {@link UDPIOClient} by creating a simple echo server that the client communicates with
 */
class UdpClientTest extends Specification implements ManagerContainerTrait {

    def "Check client"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.2)

        and: "the container is started"
        def clientPort = findEphemeralPort()
        def container = startContainer(defaultConfig(), [])

        and: "a simple UDP echo server"
        def echoServerPort = findEphemeralPort()
        def echoServer = new UDPStringServer(new InetSocketAddress("127.0.0.1", echoServerPort), ";", Integer.MAX_VALUE, true)
        echoServer.addMessageConsumer({
            message, channel, sender ->
                echoServer.sendMessage(message, sender)
        })

        and: "we add callback consumers on the server"
        def serverConnectionStatus = echoServer.connectionStatus
        echoServer.addConnectionStatusConsumer((Consumer<ConnectionStatus>) {
            status -> serverConnectionStatus = status
        })
        
        and: "a simple UDP broadcast client"
        UDPIOClient<String> client = new UDPIOClient<String>(
                "127.0.0.1",
                echoServerPort,
                clientPort)
        client.setEncoderDecoderProvider({
            [
                new StringEncoder(CharsetUtil.UTF_8),
                new StringDecoder(CharsetUtil.UTF_8),
                new AbstractNettyIOClient.MessageToMessageDecoder<String>(String.class, client)
            ].toArray(new ChannelHandler[0])
        })

        and: "we add callback consumers to the client"
        def connectionStatus = client.getConnectionStatus()
        String lastMessage
        client.addMessageConsumer({
            message ->
                lastMessage = message
        })
        client.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })

        when: "the server is started"
        echoServer.start()

        then: "the server should be running"
        conditions.eventually {
            assert echoServer.connectionStatus == ConnectionStatus.CONNECTED
            assert serverConnectionStatus == ConnectionStatus.CONNECTED
        }

        when: "we call connect on the client"
        client.connect()

        then: "the client status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "the server sends a broadcast message"
        echoServer.sendMessage("Hello world", SocketUtils.socketAddress("127.0.0.1", clientPort))

        then: "we should receive the message"
        conditions.eventually {
            assert lastMessage == "Hello world"
        }

        when: "we send a message to the server"
        client.sendMessage("Test;")

        then: "we should get the same message back"
        conditions.eventually {
            assert lastMessage == "Test"
        }

        when: "we request the client to disconnect"
        client.disconnect()

        then: "the client should become DISCONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.DISCONNECTED
            assert connectionStatus == ConnectionStatus.DISCONNECTED
        }

        when: "we reconnect the same client"
        client.connect()

        then: "the client status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "the server sends a message to the loopback interface"
        echoServer.sendMessage("Is there anyone there?", SocketUtils.socketAddress("127.0.0.1", clientPort))

        then: "we should receive the message"
        conditions.eventually {
            assert lastMessage == "Is there anyone there?"
        }

        when: "we send a message to the server"
        client.sendMessage("Yes there is!;")

        then: "we should get the same message back"
        conditions.eventually {
            assert lastMessage == "Yes there is!"
        }

        when: "we request the processor to disconnect"
        client.disconnect()

        then: "the client should become DISCONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.DISCONNECTED
            assert connectionStatus == ConnectionStatus.DISCONNECTED
        }

        cleanup: "the server should be stopped"
        client.disconnect()
        echoServer.stop()
    }
}
