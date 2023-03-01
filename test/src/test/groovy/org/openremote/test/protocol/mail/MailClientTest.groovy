package org.openremote.test.protocol.mail

import com.icegreen.greenmail.user.GreenMailUser
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest
import org.openremote.agent.protocol.mail.MailClientBuilder
import org.openremote.container.Container
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import jakarta.mail.Message
import jakarta.mail.event.ConnectionEvent
import jakarta.mail.internet.MimeMessage
import java.util.concurrent.CopyOnWriteArrayList

/*
 * Copyright 2023, OpenRemote Inc.
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

class MailClientTest extends Specification implements ManagerContainerTrait {

    @Shared
    static GreenMail greenMail
    @Shared
    static int messageCounter


    def setupSpec() {
        MailClientBuilder.MIN_CHECK_INTERVAL_MILLIS = 10000
        greenMail = new GreenMail(ServerSetupTest.ALL)
        greenMail.start()

        // Send a few messages to the mailbox
        sendMessage()
        sendMessage()
        sendMessage()
    }

    def cleanupSpec() {
        if (greenMail != null) {
            greenMail.stop()
        }
    }

    def sendMessage() {
        final String subject = "Test Message ${messageCounter++}"
        final String body = "Test body ${messageCounter}"
        MimeMessage message = GreenMailUtil.createTextEmail("to@localhost", "from@localhost", subject, body, greenMail.getImap().getServerSetup())
        GreenMailUser user = greenMail.setUser("or@localhost", "or", "secret")
        user.deliver(message)
    }

    def "POP3 mail receiving test"() {

        given: "an email client with callback handlers"
        List<ConnectionEvent> connectionEvents = new CopyOnWriteArrayList<>()
        List<Message> messages = new CopyOnWriteArrayList<>()
        def conditions = new PollingConditions(delay: 1, initialDelay: 1, timeout: 10)
        def emailClient = new MailClientBuilder(
                new Container.NoShutdownScheduledExecutorService("Scheduled task", 1),
                "pop3",
                "localhost",
                greenMail.getPop3().getServerSetup().getPort(), "or", "secret")
            .build()
        emailClient.addConnectionListener{ connectionEvents.add(it)}
        emailClient.addMessageListener{messages.add(it)}

        when: "the email client is connected"
        emailClient.connect()

        then: "the connection status should be connected and the 3 messages should have been received"
        conditions.eventually {
            assert connectionEvents.size() == 1
            assert connectionEvents[0].type == ConnectionEvent.OPENED
            assert messages.size() == 3
            assert messages.any {it.content == "Test body 1" && it.subject == "Test Message 1"}
            assert messages.any {it.content == "Test body 2" && it.subject == "Test Message 2"}
            assert messages.any {it.content == "Test body 3" && it.subject == "Test Message 3"}
        }

        cleanup: "clean up client"
        emailClient.disconnect()
    }
}
