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
import org.openremote.agent.protocol.AbstractSocketMessageProcessor
import org.openremote.agent.protocol.ConnectionStatus
import org.openremote.manager.server.concurrent.ManagerExecutorService
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.SimpleSocketServer
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

/**
 * This tests the {@link AbstractSocketMessageProcessor} by creating a simple echo socket
 * server that the message processor communicates with
 */
class SocketMessageProcessorTest extends Specification implements ManagerContainerTrait {

    def "Check socket message processor"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "a simple socket echo server"
        def socketServerPort = findEphemeralPort()
        def socketServer = new SimpleSocketServer(socketServerPort, true)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), Collections.singletonList(new ManagerExecutorService()))
        def protocolExecutorService = container.getService(ManagerExecutorService.class)

        and: "a simple socket message processor"
        def messageProcessor = new AbstractSocketMessageProcessor<String>(
                "localhost",
                socketServerPort,
                protocolExecutorService) {

            @Override
            protected void decode(ByteBuf buf, List<String> messages) throws Exception {
                ByteBuf bytes = buf.readBytes(buf.readableBytes())
                String msg = bytes.toString(CharsetUtil.UTF_8)
                bytes.release()
                messages.add(msg)
            }

            @Override
            protected void encode(String message, ByteBuf buf) throws Exception {
                buf.writeBytes(message.getBytes(CharsetUtil.UTF_8))
            }
        }

        and: "we add callback consumers to the message processor"
        def connectionStatus = messageProcessor.getConnectionStatus()
        String lastMessage
        messageProcessor.addMessageConsumer({
            message -> lastMessage = message
        })
        messageProcessor.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })

        when: "the server is started"
        socketServer.start()

        then: "the server should be connected"
        conditions.eventually {
            assert socketServer.channelFuture.isDone()
            assert socketServer.channelFuture.isSuccess()
        }

        when: "we call connect on the message processor"
        messageProcessor.connect()

        then: "the message processor status should become CONNECTED"
        conditions.eventually {
            assert messageProcessor.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "the server sends a message"
        socketServer.sendMessage("Hello world".getBytes(CharsetUtil.UTF_8))

        then: "we should receive the message"
        conditions.eventually {
            assert lastMessage == "Hello world"
        }

        when: "we send a message to the server"
        messageProcessor.sendMessage("Test")

        then: "we should get the same message back"
        conditions.eventually {
            assert lastMessage == "Test"
        }

        when: "we request the message processor to disconnect"
        messageProcessor.disconnect()

        then: "the message processor should become DISCONNECTED"
        conditions.eventually {
            assert messageProcessor.connectionStatus == ConnectionStatus.DISCONNECTED
            assert connectionStatus == ConnectionStatus.DISCONNECTED
        }

        when: "we reconnect the same message processor"
        messageProcessor.connect()

        then: "the message processor status should become CONNECTED"
        conditions.eventually {
            assert messageProcessor.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "the server sends a message"
        socketServer.sendMessage("Is there anyone there?".getBytes(CharsetUtil.UTF_8))

        then: "we should receive the message"
        conditions.eventually {
            assert lastMessage == "Is there anyone there?"
        }

        when: "we send a message to the server"
        messageProcessor.sendMessage("Yes there is!")

        then: "we should get the same message back"
        conditions.eventually {
            assert lastMessage == "Yes there is!"
        }

        when: "we lose connection to the server"
        socketServer.stop()

        then: "the message processor status should change to WAITING"
        conditions.eventually {
            assert messageProcessor.connectionStatus == ConnectionStatus.WAITING
            assert connectionStatus == ConnectionStatus.WAITING
        }

        when: "the connection to the server is restored"
        socketServer.start()

        then: "the message processor status should become CONNECTED"
        conditions.eventually {
            assert messageProcessor.connectionStatus == ConnectionStatus.CONNECTED
            assert connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "the server sends a message"
        socketServer.sendMessage("Is there anyone there?".getBytes(CharsetUtil.UTF_8))

        then: "we should receive the message"
        conditions.eventually {
            assert lastMessage == "Is there anyone there?"
        }

        when: "we send a message to the server"
        messageProcessor.sendMessage("Yes there is!")

        then: "we should get the same message back"
        conditions.eventually {
            assert lastMessage == "Yes there is!"
        }

        when: "we request the processor to disconnect"
        messageProcessor.disconnect()

        then: "the message processor should become DISCONNECTED"
        conditions.eventually {
            assert messageProcessor.connectionStatus == ConnectionStatus.DISCONNECTED
            assert connectionStatus == ConnectionStatus.DISCONNECTED
        }

        cleanup: "the server should be stopped"
        messageProcessor.disconnect()
        socketServer.stop()
        stopContainer(container)
    }
}
