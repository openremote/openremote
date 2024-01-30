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
package org.openremote.test.protocol.mail

import com.icegreen.greenmail.user.GreenMailUser
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest
import jakarta.mail.internet.MimeMessage
import org.openremote.agent.protocol.mail.*
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.query.filter.StringPredicate
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.TEXT

class MailClientProtocolTest extends Specification implements ManagerContainerTrait {

    @Shared
    static GreenMail greenMail
    @Shared
    static int messageCounter
    @Shared
    static GreenMailUser user


    def setupSpec() {
        MailClientBuilder.MIN_CHECK_INTERVAL_SECONDS = 2
        AbstractMailProtocol.INITIAL_CHECK_DELAY_SECONDS = 2
        greenMail = new GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        user = greenMail.setUser("or@localhost", "or", "secret")
    }

    def cleanupSpec() {
        if (greenMail != null) {
            greenMail.stop()
        }
    }

    def sendMessage(String fromAddress) {
        def subject = "Test Message ${++messageCounter}"
        def body = "Test body ${messageCounter}"
        MimeMessage message = GreenMailUtil.createTextEmail("to@localhost", fromAddress, subject, body, greenMail.getImap().getServerSetup())
        message.addHeader("Test-Header", "Test Header Value")
        user.deliver(message)
    }

    def sendMessage(String fromAddress, String subject, String body) {
        MimeMessage message = GreenMailUtil.createTextEmail("to@localhost", fromAddress, subject, body, greenMail.getImap().getServerSetup())
        message.addHeader("Test-Header", "Test Header Value")
        user.deliver(message)
    }

    def "Basic agent and attribute linking"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.2)

        and: "some mailbox messages"
        sendMessage("from@localhost")
        sendMessage("from@localhost")
        sendMessage("from@localhost")

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)
        def clientEventService = container.getService(ClientEventService.class)
        List<AttributeEvent> attributeEvents = []
        def eventSessionID = clientEventService.addInternalSubscription(AttributeEvent.class,  null, it -> {
            attributeEvents.add(it)
        })

        when: "a mail client agent is created"
        def agent = new MailAgent("Test agent")
        agent.setRealm(Constants.MASTER_REALM)
            .setProtocol("pop3")
            .setHost("127.0.0.1")
            .setPort(greenMail.getPop3().getServerSetup().getPort())
            .setUsernamePassword(new UsernamePassword("or", "secret"))
            .setCheckIntervalSeconds(2)
            .setDisabled(true)

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        and: "an asset is created with attributes linked to the agent"
        def asset = new ThingAsset("Test Asset")
                .setParent(agent)
                .addOrReplaceAttributes(
                        new Attribute<>("fromMatchUseBody", TEXT)
                                .addMeta(
                                        new MetaItem<>(AGENT_LINK, new MailAgentLink(agent.id)
                                            .setFromMatchPredicate(new StringPredicate("from@localhost"))
                                        )
                                ),
                        new Attribute<>("subjectMatchUseBody", TEXT)
                                .addMeta(
                                        new MetaItem<>(AGENT_LINK, new MailAgentLink(agent.id)
                                                .setSubjectMatchPredicate(new StringPredicate("Not A Test"))
                                        )
                                ),
                        new Attribute<>("subjectAndFromMatchUseBody", TEXT)
                                .addMeta(
                                        new MetaItem<>(AGENT_LINK, new MailAgentLink(agent.id)
                                                .setFromMatchPredicate(new StringPredicate("fromanother@localhost"))
                                                .setSubjectMatchPredicate(new StringPredicate("Not A Test"))
                                        )
                                ),
                        new Attribute<>("fromMatchUseSubject", TEXT)
                                .addMeta(
                                        new MetaItem<>(AGENT_LINK, new MailAgentLink(agent.id)
                                                .setFromMatchPredicate(new StringPredicate("from@localhost"))
                                                .setUseSubject(true)
                                        )
                                )
                )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "attribute events for the new asset should arrive"
        conditions.eventually {
            assert attributeEvents.count {it.id == asset.id} == 6
        }

        when: "the attribute events are cleared"
        attributeEvents.clear()

        and: "the agent is enabled"
        agent.setDisabled(false)
        agent = assetStorageService.merge(agent)

        then: "the attributes should be linked"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id).linkedAttributes.size() == 4
            assert ((MailProtocol)agentService.getProtocolInstance(agent.id)).attributeMessageProcessorMap.size() == 4
        }

        then: "the agent should become connected"
        conditions.eventually {
            assert attributeEvents.any {it.id == agent.id && it.name == Agent.STATUS.name && it.value.orElse(null) == ConnectionStatus.CONNECTED}
        }

        then: "the agent should have finished message checking"
        conditions.eventually {
            assert attributeEvents.any {it.id == agent.id && it.name == Agent.STATUS.name && it.value.orElse(null) == ConnectionStatus.WAITING}
        }

        then: "the linked attributes should not have been updated with mailbox messages (as creation time of agent is after message timestamps)"
        conditions.eventually {
            assert attributeEvents.count {it.id == asset.id} == 0
        }

        when: "more messages are received in the mailbox"
        sendMessage("from@localhost")
        sendMessage("1from@localhost")
        sendMessage("from@localhost")
        sendMessage("from@localhost", "Not A Test", "Not a test body 1")
        sendMessage("fromanother@localhost", "Not A Test", "Not a test body 2")

        then: "the matching linked attributes should have been updated with the new mailbox messages"
        conditions.eventually {
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseBody" && (it.value.orElse(null) as String).startsWith("Test body")} == 2
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseBody" && (it.value.orElse(null) as String).startsWith("Not a test body")} == 1
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseSubject" && (it.value.orElse(null) as String).startsWith("Test Message")} == 2
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseSubject" && it.value.orElse(null) == "Not A Test"} == 1
            assert attributeEvents.count {it.id == asset.id && it.name == "subjectMatchUseBody" && (it.value.orElse(null) as String).startsWith("Not a test body")} == 2
            assert attributeEvents.count {it.id == asset.id && it.name == "subjectAndFromMatchUseBody" && it.value.orElse(null) == "Not a test body 2"} == 1
        }

        when: "more messages are received in the mailbox"
        sendMessage("fromanother@localhost", "Really Not A Test", "Really not a test body 1")
        sendMessage("fromanotheranother@localhost", "Not A Test", "Not a test body 3")

        then: "the matching linked attributes should have been updated with the new mailbox messages"
        conditions.eventually {
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseBody" && (it.value.orElse(null) as String).startsWith("Test body")} == 2
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseBody" && (it.value.orElse(null) as String).startsWith("Not a test body")} == 1
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseSubject" && (it.value.orElse(null) as String).startsWith("Test Message")} == 2
            assert attributeEvents.count {it.id == asset.id && it.name == "fromMatchUseSubject" && it.value.orElse(null) == "Not A Test"} == 1
            assert attributeEvents.count {it.id == asset.id && it.name == "subjectMatchUseBody" && (it.value.orElse(null) as String).startsWith("Not a test body")} == 3
            assert attributeEvents.count {it.id == asset.id && it.name == "subjectAndFromMatchUseBody" && (it.value.orElse(null) as String).startsWith("Not a test body")} == 1
        }

        cleanup: "the event subscription is removed"
        if (eventSessionID != null) {
            clientEventService.cancelInternalSubscription(eventSessionID)
        }
        if (asset != null) {
            assetStorageService.delete([agent.id, asset.id])
        }
    }
}
