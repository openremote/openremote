/*
 * Copyright 2016, OpenRemote Inc.
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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test

import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.Session
import java.util.concurrent.TimeoutException

public class BlockingWebsocketEndpoint extends Endpoint {

    static final def DEFAULT_TIMEOUT_MILLIS = 5000
    
    def startTime
    def session
    List<String> messages = []
    
    protected int timeoutMillis
    protected int expectedMessageCount

    BlockingWebsocketEndpoint(int expectedMessageCount) {
        this(expectedMessageCount, DEFAULT_TIMEOUT_MILLIS)
    }

    BlockingWebsocketEndpoint(int expectedMessageCount, int timeoutMillis) {
        this.expectedMessageCount = expectedMessageCount
        this.timeoutMillis = timeoutMillis
    }

    @Override
    void onOpen(Session session, EndpointConfig config) {
        this.session = session
        session.addMessageHandler new MessageHandler.Whole<String>() {
            @Override
            void onMessage(String message) {
                BlockingWebsocketEndpoint.this.messages << message
            }
        }
    }

    @Override
    void onClose(Session session, CloseReason closeReason) {
        this.session = null
    }

    def reset(int expectedMessageCount) {
        this.expectedMessageCount = expectedMessageCount
        this.messages = []
        this.startTime = 0
    }

    def awaitMessages() {
        awaitMessages(true)
    }

    def awaitMessages(boolean closeSessionOnCompletion) {
        startTime = System.currentTimeMillis()
        try {
            while (!messages || messages.size() < expectedMessageCount) {
                if (isTimeout()) {
                    throw new TimeoutException(
                            "Timeout while waiting for expected websocket messages ("
                                    + expectedMessageCount
                                    + "), received: " + messages
                    )
                }
                Thread.sleep 250
            }
        } finally {
            if (session && closeSessionOnCompletion) {
                session.close()
                // Give the server a chance to end the session properly, we clear the member in onClose()
                Thread.sleep 250
                startTime = System.currentTimeMillis()
                while (session) {
                    if (isTimeout()) {
                        throw new TimeoutException("Timeout while waiting for session to close")
                    }
                    Thread.sleep 250
                }
            }
        }
    }

    def isTimeout() {
        System.currentTimeMillis() > (startTime + timeoutMillis)
    }
    
}
