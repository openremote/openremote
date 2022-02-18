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
package org.openremote.test.protocol

import io.netty.channel.ChannelHandler
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil
import org.openremote.agent.protocol.io.AbstractNettyIOClient
import org.openremote.agent.protocol.tcp.TCPIOClient
import org.openremote.agent.protocol.tcp.TCPStringServer
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.notification.AbstractNotificationMessage
import org.openremote.model.notification.Notification
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

/**
 * This tests the {@link TCPIOClient} by creating a simple echo server that the client communicates with
 */
class TcpClientTest extends Specification implements ManagerContainerTrait {

    def "Check client"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), [])

        and: "a simple TCP echo server"
        def echoServerPort = findEphemeralPort()
        def echoServer = new TCPStringServer(new InetSocketAddress("127.0.0.1", echoServerPort), ";", Integer.MAX_VALUE, true)
        echoServer.addMessageConsumer({
            message, channel, sender -> echoServer.sendMessage(message)
        })

        and: "a simple TCP client"
        def connectAttempts = 0
        TCPIOClient<String> client = new TCPIOClient<String>(
                "127.0.0.1",
                echoServerPort)
        client.setEncoderDecoderProvider({
            [new StringEncoder(CharsetUtil.UTF_8),
            new StringDecoder(CharsetUtil.UTF_8),
            new AbstractNettyIOClient.MessageToMessageDecoder<String>(String.class, client)].toArray(new ChannelHandler[0])
        })
        client = Spy(client)
        client.doConnect() >> {
            connectAttempts++
            callRealMethod()
        }

        and: "we add callback consumers to the client"
        def connectionStatus = client.getConnectionStatus()
        String lastMessage
        client.addMessageConsumer({
            message -> lastMessage = message
        })
        client.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })

        when: "the server is started"
        echoServer.start()

        then: "the server should be running"
        conditions.eventually {
            assert echoServer.channelFuture.isDone()
            assert echoServer.channelFuture.isSuccess()
        }

        when: "we call connect on the client"
        client.connect()

        then: "the client status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert echoServer.allChannels.size() == 1
        }

        when: "the server sends a message"
        echoServer.sendMessage("Hello world")

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
            assert echoServer.allChannels.size() == 1
        }

        when: "the server sends a message"
        echoServer.sendMessage("Is there anyone there?")

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

        when: "we lose connection to the server"
        connectAttempts = 0
        echoServer.stop()

        then: "the server status should be DISCONNECTED"
        conditions.eventually {
            assert echoServer.connectionStatus == ConnectionStatus.DISCONNECTED
        }

        then: "the client status should change to CONNECTING and several re-connection attempts should be made"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTING
            assert connectionStatus == ConnectionStatus.CONNECTING
            assert connectAttempts > 2
        }

        when: "the connection to the server is restored"
        // need to retry here as the previous connected socket might not have been released
        def retries = 0
        while (echoServer.connectionStatus != ConnectionStatus.CONNECTED && retries < 10) {
            echoServer.start()
            Thread.sleep(500)
            retries++
        }

        then: "the server is connected"
        assert echoServer.connectionStatus == ConnectionStatus.CONNECTED

        then: "the client status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert echoServer.allChannels.size() == 1
        }

        when: "the server sends a message"
        echoServer.sendMessage("Is there anyone there?")

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
