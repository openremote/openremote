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

import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil
import org.openremote.agent.protocol.tcp.AbstractTcpClient
import org.openremote.agent.protocol.tcp.TcpStringServer
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.manager.concurrent.ManagerExecutorService
import org.openremote.test.ManagerContainerTrait

import spock.lang.Specification
import spock.util.concurrent.PollingConditions

/**
 * This tests the {@link AbstractTcpClient} by creating a simple echo server that the client communicates with
 */
class TcpClientTest extends Specification implements ManagerContainerTrait {

    def "Check socket client"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), Collections.singletonList(new ManagerExecutorService()))
        def protocolExecutorService = container.getService(ManagerExecutorService.class)

        and: "a simple TCP echo server"
        def echoServerPort = findEphemeralPort()
        def echoServer = new TcpStringServer(protocolExecutorService, new InetSocketAddress(echoServerPort), ";", Integer.MAX_VALUE, true)
        echoServer.addMessageConsumer({
            message, channel, sender -> echoServer.sendMessage(message)
        })

        and: "a simple TCP client"
        def client = new AbstractTcpClient<String>(
                "localhost",
                echoServerPort,
                protocolExecutorService) {

            @Override
            protected void decode(ByteBuf buf, List<String> messages) {
                ByteBuf bytes = buf.readBytes(buf.readableBytes())
                String msg = bytes.toString(CharsetUtil.UTF_8)
                bytes.release()
                messages.add(msg)
            }

            @Override
            protected void encode(String message, ByteBuf buf) {
                buf.writeBytes(message.getBytes(CharsetUtil.UTF_8))
            }
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
        echoServer.stop()

        then: "the client status should change to WAITING"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.WAITING
            assert connectionStatus == ConnectionStatus.WAITING
        }

        when: "the connection to the server is restored"
        echoServer.start()

        then: "the client status should become CONNECTED"
        conditions.eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
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
        stopContainer(container)
    }
}
