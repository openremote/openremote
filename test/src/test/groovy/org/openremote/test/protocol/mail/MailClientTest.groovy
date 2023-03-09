package org.openremote.test.protocol.mail

import com.icegreen.greenmail.user.GreenMailUser
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest
import org.openremote.model.mail.MailMessage

import javax.mail.Message
import javax.mail.Multipart
import javax.mail.event.ConnectionEvent
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import org.openremote.agent.protocol.mail.MailClientBuilder
import org.openremote.container.Container
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.mail.internet.MimeMultipart
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
    @Shared
    static GreenMailUser user


    def setupSpec() {
        MailClientBuilder.MIN_CHECK_INTERVAL_MILLIS = 2000
        greenMail = new GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        user = greenMail.setUser("or@localhost", "or", "secret")

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
        def subject = "Test Message ${++messageCounter}"
        def body = "Test body ${messageCounter}"
        MimeMessage message = GreenMailUtil.createTextEmail("to@localhost", "from@localhost", subject, body, greenMail.getImap().getServerSetup())
        message.addHeader("Test-Header", "Test Header Value")
        user.deliver(message)
    }

    def "POP3 mail receiving test"() {

        given: "an email client with callback handlers"
        List<ConnectionEvent> connectionEvents = new CopyOnWriteArrayList<>()
        List<MailMessage> messages = new CopyOnWriteArrayList<>()
        def conditions = new PollingConditions(delay: 1, initialDelay: 1, timeout: 10)
        def mailClient = new MailClientBuilder(
                new Container.NoShutdownScheduledExecutorService("Scheduled task", 1),
                "pop3",
                "localhost",
                greenMail.getPop3().getServerSetup().getPort(), "or", "secret")
            .setCheckIntervalMillis(2000)
            .build()
        mailClient.addConnectionListener{ connectionEvents.add(it)}
        mailClient.addMessageListener{messages.add(it)}

        when: "the mail client is connected"
        mailClient.connect()

        then: "the connection status should be connected and the 3 messages should have been received"
        conditions.eventually {
            assert connectionEvents.size() == 1
            assert connectionEvents[0].type == ConnectionEvent.OPENED
            assert messages.size() == 3
            assert messages.any {it.content == "Test body 1\r\n" && it.subject == "Test Message 1" && it.sentDate != null && it.contentType == "text/plain; charset=us-ascii" && it.from[0] == "from@localhost" && it.headers.get("Test-Header").get(0) == "Test Header Value"}
            assert messages.any {it.content == "Test body 2\r\n" && it.subject == "Test Message 2" && it.sentDate != null && it.contentType == "text/plain; charset=us-ascii" && it.from[0] == "from@localhost" && it.headers.get("Test-Header").get(0) == "Test Header Value"}
            assert messages.any {it.content == "Test body 3\r\n" && it.subject == "Test Message 3" && it.sentDate != null && it.contentType == "text/plain; charset=us-ascii" && it.from[0] == "from@localhost" && it.headers.get("Test-Header").get(0) == "Test Header Value"}
        }

        when: "a multipart mail is sent to the mailbox with text and html parts"
        def sendEmail = () -> {
            def email = new MimeMessage(GreenMailUtil.getSession(greenMail.getImap().getServerSetup()))
            email.setSubject("Test Multipart Message")
            MimeBodyPart htmlBodyPart = new MimeBodyPart()
            htmlBodyPart.setContent("<p>Test html body</p>", "text/html; charset=utf-8")
            htmlBodyPart.addHeader("HTML-Header", "HTML Header Value")
            MimeBodyPart textBodyPart = new MimeBodyPart()
            textBodyPart.setContent("Test text body", "text/plain; charset=utf-8")
            textBodyPart.addHeader("Text-Header", "Text Header Value")
            Multipart multipart = new MimeMultipart("alternative")
            multipart.addBodyPart(htmlBodyPart)
            multipart.addBodyPart(textBodyPart)
            email.setContent(multipart)
            email.setRecipient(Message.RecipientType.TO, new InternetAddress("to@localhost"))
            email.setFrom("from@localhost")
            user.deliver(email)
        }
        sendEmail()

        then: "the new mail should be received with the HTML content"
        conditions.eventually {
            assert messages.size() == 4
            def message = messages.get(messages.size()-1)
            assert message.subject == "Test Multipart Message"
            assert message.contentType == "text/plain; charset=utf-8"
            assert message.content == "Test text body"
            assert message.from[0] == "from@localhost"
            assert message.headers.get("Text-Header").get(0) == "Text Header Value"
            assert message.headers.get("HTML-Header") == null
        }

        cleanup: "clean up client"
        mailClient.disconnect()
    }
}
