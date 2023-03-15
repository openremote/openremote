package org.openremote.test.protocol.mail

import com.icegreen.greenmail.user.GreenMailUser
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest
import org.openremote.agent.protocol.mail.AbstractMailProtocol
import org.openremote.agent.protocol.mail.MailAgent
import org.openremote.agent.protocol.mail.MailClientBuilder
import org.openremote.agent.protocol.mail.MailProtocol
import org.openremote.agent.protocol.udp.UDPAgent
import org.openremote.agent.protocol.udp.UDPProtocol
import org.openremote.container.concurrent.ContainerScheduledExecutor
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.agent.DefaultAgentLink
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.mail.MailMessage
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.query.filter.ValueAnyPredicate
import org.openremote.model.value.SubStringValueFilter
import org.openremote.model.value.ValueFilter
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.mail.Message
import javax.mail.Multipart
import javax.mail.event.ConnectionEvent
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList

import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.EXECUTION_STATUS
import static org.openremote.model.value.ValueType.TEXT
import static org.openremote.model.value.ValueType.TEXT
import static org.openremote.model.value.ValueType.TEXT
import static org.openremote.model.value.ValueType.TEXT
import static org.openremote.model.value.ValueType.TEXT
import static org.openremote.model.value.ValueType.TEXT

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

class MailClientProtocolTest extends Specification implements ManagerContainerTrait {

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

    def "Basic agent and attribute linking"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        AbstractMailProtocol.INITIAL_CHECK_DELAY_MILLIS = 1000

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        when: "a mail client agent is created"
        def agent = new MailAgent("Test agent")
        agent.setRealm(Constants.MASTER_REALM)
            .setProtocol("pop3")
            .setHost("127.0.0.1")
            .setPort(greenMail.getPop3().getServerSetup().getPort())
            .setUsernamePassword(new UsernamePassword("or", "secret"))
            .setCheckIntervalSeconds(2000)

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol instance should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert (agentService.getProtocolInstance(agent.id) as MailProtocol).mailClient != null
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
    }
}
