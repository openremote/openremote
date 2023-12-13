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


import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.*
import org.openremote.model.value.RegexValueFilter
import org.openremote.model.value.SubStringValueFilter
import org.openremote.model.value.ValueFilter
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import org.openremote.setup.integration.protocol.MockAgent
import org.openremote.setup.integration.protocol.MockAgentLink
import org.openremote.setup.integration.protocol.MockProtocol

import java.util.regex.Pattern

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.ValueType.*
import static org.openremote.model.value.MetaItemType.*

/**
 * This tests the basic protocol interface and abstract protocol implementation.
 */
class BasicProtocolTest extends Specification implements ManagerContainerTrait {

    def "Check basic protocol linking/un-linking and value writing"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.3, delay: 0.2)
        Map<String, Integer> protocolExpectedLinkedAttributeCount = [:]
        protocolExpectedLinkedAttributeCount["mockAgent1"] = 5
        protocolExpectedLinkedAttributeCount["mockAgent2"] = 2
        protocolExpectedLinkedAttributeCount["mockAgent3"] = 2
        protocolExpectedLinkedAttributeCount['mockConfig4'] = 2

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        when: "several mock agents that uses the mock protocol are created"
        def mockAgent1 = new MockAgent("Mock agent 1")
            .setRealm(MASTER_REALM)
            .setRequired(true)
        mockAgent1 = assetStorageService.merge(mockAgent1)

        def mockAgent2 = new MockAgent("Mock agent 2")
            .setRealm(MASTER_REALM)
            .setRequired(true)
            .setDisabled(true)
        mockAgent2 = assetStorageService.merge(mockAgent2)

        def mockAgent3 = new MockAgent("Mock agent 3")
            .setRealm(MASTER_REALM)
        mockAgent3 = assetStorageService.merge(mockAgent3)

        then: "the protocol instances should have been created and the agent status attributes should be updated"
        conditions.eventually {
            assert agentService.agents.values().count {it instanceof MockAgent} == 3
            assert agentService.protocolInstanceMap.values().count {it instanceof MockProtocol} == 1
            assert agentService.getAgent(mockAgent1.id) != null
            assert agentService.getAgent(mockAgent2.id) != null
            assert agentService.getAgent(mockAgent3.id) != null
            assert agentService.getProtocolInstance(mockAgent1.id) != null
            assert agentService.getProtocolInstance(mockAgent2.id) == null
            assert agentService.getProtocolInstance(mockAgent3.id) == null
            assert agentService.getAgent(mockAgent1.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert agentService.getAgent(mockAgent2.id).getAgentStatus().orElse(null) == ConnectionStatus.DISABLED
            assert agentService.getAgent(mockAgent3.id).getAgentStatus().orElse(null) == ConnectionStatus.ERROR
        }

        when: "a mock thing asset is created that links to the mock agents"
        def mockThing = new ThingAsset("Mock Thing Asset")
            .setRealm(MASTER_REALM)
        
        mockThing.addOrReplaceAttributes(
            new Attribute<>("lightToggle1", BOOLEAN)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent1.id)
                            .setRequiredValue("true")
                    )
                ),
            new Attribute<>("tempTarget1", NUMBER)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent1.id)
                            .setRequiredValue("true")
                    )
                ),
            new Attribute<>("invalidToggle1", BOOLEAN)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent1.id)
                    )
                ),
            new Attribute<>("lightToggle2", BOOLEAN)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent2.id)
                            .setRequiredValue("true")
                    )
                ),
            new Attribute<>("tempTarget2", NUMBER)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent2.id)
                            .setRequiredValue("true")
                    )
                ),
            new Attribute<>("lightToggle3", BOOLEAN)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent3.id)
                            .setRequiredValue("true")
                    )
                ),
            new Attribute<>("tempTarget3", NUMBER)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent3.id)
                            .setRequiredValue("true")
                    )
                ),
            new Attribute<>("invalidToggle5", BOOLEAN, false)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink("invalid id")
                            .setRequiredValue("true")
                    )
                ),
            new Attribute<>("plainAttribute", TEXT, "demo")
                .addOrReplaceMeta(
                    new MetaItem<>(READ_ONLY, true)
                ),
            new Attribute<>("filterRegex", NUMBER)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent1.id)
                            .setRequiredValue("true")
                            .setValueFilters(
                                [
                                    new RegexValueFilter(Pattern.compile("\\w(\\d+)")).setMatchGroup(1).setMatchIndex(2)
                                ] as ValueFilter[]
                            )
                    )
                ),
            new Attribute<>("filterSubstring", TEXT)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent1.id)
                            .setRequiredValue("true")
                        .setValueFilters(
                            [
                                new SubStringValueFilter(10, 12)
                            ] as ValueFilter[]
                        )
                    )
                ),
            new Attribute<>("filterRegexSubstring", NUMBER)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        AGENT_LINK,
                        new MockAgentLink(mockAgent1.id)
                            .setRequiredValue("true")
                            .setValueFilters(
                                [
                                    new SubStringValueFilter(23),
                                    new RegexValueFilter(Pattern.compile("[a-z|\\s]+(\\d+)%\"}")).setMatchGroup(1)
                                ] as ValueFilter[]
                            )
                        )
            )
        )

        mockThing = assetStorageService.merge(mockThing)

        then: "the mock thing to be fully deployed in the correct order"
        conditions.eventually {
            assert agentService.getProtocolInstance(mockAgent1.id) != null
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.size() == 7
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.get(0) == "START"
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.get(1).startsWith("LINK_ATTRIBUTE")
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.get(2).startsWith("LINK_ATTRIBUTE")
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.get(3).startsWith("LINK_ATTRIBUTE")
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.get(4).startsWith("LINK_ATTRIBUTE")
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.get(5).startsWith("LINK_ATTRIBUTE")
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.get(6).startsWith("LINK_ATTRIBUTE")
            assert agentService.getProtocolInstance(mockAgent1.id).linkedAttributes.size() == protocolExpectedLinkedAttributeCount["mockAgent1"]
        }

        and: "invalid attribute should not actually have been linked"
        assert !((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).linkedAttributes.any {it.value.name == "invalidToggle1"}

        when: "values are sent to the linked attributes by the protocol"
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(new AttributeState(mockThing.id, "lightToggle1", true))
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(new AttributeState(mockThing.id, "tempTarget1", 25.5d))

        then: "the linked attributes values should have been updated by the protocol"
        conditions.eventually {
            def mockAsset = assetStorageService.find(mockThing.id, true)
            // Check all valid linked attributes have the new values
            assert mockAsset.getAttribute("lightToggle1").flatMap{it.getValue()}.orElse(false)
            assert mockAsset.getAttribute("tempTarget1").flatMap{it.getValue()}.orElse(0d) == 25.5d
            // Check invalid attributes don't have the new values
            assert !mockAsset.getAttribute("lightToggle2").get().getValue().isPresent()
            assert !mockAsset.getAttribute("tempTarget2").get().getValue().isPresent()
            assert !mockAsset.getAttribute("lightToggle3").get().getValue().isPresent()
            assert !mockAsset.getAttribute("tempTarget3").get().getValue().isPresent()
        }

        when: "the disabled agent is enabled"
        mockAgent2.setDisabled(false)
        mockAgent2 = assetStorageService.merge(mockAgent2)

        then: "a protocol instance should exist and attributes should be linked"
        conditions.eventually {
            assert agentService.getProtocolInstance(mockAgent2.id) != null
            assert agentService.getProtocolInstance(mockAgent2.id).linkedAttributes.size() == protocolExpectedLinkedAttributeCount["mockAgent2"]
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent2.id)).protocolMethodCalls.size() == 3
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent2.id)).protocolMethodCalls.get(0) == "START"
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent2.id)).protocolMethodCalls.get(1).startsWith("LINK_ATTRIBUTE")
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent2.id)).protocolMethodCalls.get(2).startsWith("LINK_ATTRIBUTE")
        }

        when: "a linked attribute is removed"
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.clear()
        mockThing = assetStorageService.find(mockThing.id, true)
        mockThing.getAttributes().remove("lightToggle1")
        mockThing = assetStorageService.merge(mockThing)

        then: "the protocol should not be stopped but the attribute should be unlinked"
        conditions.eventually {
            assert agentService.getProtocolInstance(mockAgent1.id).linkedAttributes.size() == protocolExpectedLinkedAttributeCount["mockAgent1"] - 1
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.size() == 1
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolMethodCalls.any {it == "UNLINK_ATTRIBUTE:${mockThing.id}:lightToggle1"}
            assert !((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).linkedAttributes.containsKey(new AttributeRef(mockThing.id, "invalidToggle1"))
        }

        when: "an agent is removed"
        def mockProtocol2 = (MockProtocol)agentService.getProtocolInstance(mockAgent2.id)
        mockProtocol2.protocolMethodCalls.clear()
        assetStorageService.delete([mockAgent2.id])

        then: "the attributes should be unlinked then the protocol stopped"
        conditions.eventually {
            assert mockProtocol2.protocolMethodCalls.size() == 3
            assert mockProtocol2.protocolMethodCalls.get(0).startsWith("UNLINK_ATTRIBUTE")
            assert mockProtocol2.protocolMethodCalls.get(1).startsWith("UNLINK_ATTRIBUTE")
            assert mockProtocol2.protocolMethodCalls.get(2) == "STOP"
        }
        
        and: "the protocol instance and agent should be removed"
        conditions.eventually {
            assert agentService.getProtocolInstance(mockAgent2.id) == null
            assert (MockProtocol)agentService.getProtocolInstance(mockAgent2.id) == null
        }

        when: "the mock protocol tries to update the plain readonly attribute"
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateAttribute(new AttributeState(mockThing.id,"plainAttribute", "UPDATE"))

        then: "the plain attributes value should be updated"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true)
            assert mockThing.getAttribute("plainAttribute").get().getValue().orElse("") == "UPDATE"
        }

        when: "a target temp linked attribute value is updated it should reach the protocol"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(mockThing.id, "tempTarget1", 30d))

        then: "the update should reach the protocol as an attribute write request"
        conditions.eventually {
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolWriteAttributeEvents.size() == 1
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolWriteAttributeEvents.get(0).name == "tempTarget1"
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolWriteAttributeEvents.get(0).ref.id == mockThing.id
            assert ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).protocolWriteAttributeEvents.get(0).value.orElse(null) == 30d
        }

        then: "the target temp attributes value should be updated"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true) as ThingAsset
            assert mockThing.getAttribute("tempTarget1").get().getValue(Double.class).orElse(0d) == 30d
        }

        when: "a sensor value is received that links to an attribute using a regex filter"
        def state = new AttributeState(mockThing.id, "filterRegex", "s100 d56 g1212")
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(state)

        then: "the linked attributes value should be updated with the filtered result"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true) as ThingAsset
            assert mockThing.getAttribute("filterRegex").get().getValue(Double.class).orElse(0d) == 1212d
        }

        when: "the same attribute receives a sensor value that doesn't match the regex filter (match index invalid)"
        state = new AttributeState(mockThing.id, "filterRegex", "s100")
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(state)

        then: "the linked attributes value should be updated to null"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true)
            assert !mockThing.getAttribute("filterRegex").get().getValue().isPresent()
        }

        when: "the same attribute receives a sensor value that doesn't match the regex filter (no match)"
        state = new AttributeState(mockThing.id, "filterRegex", "no match to be found!")
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(state)

        then: "the linked attributes value should be updated to null"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true)
            assert !mockThing.getAttribute("filterRegex").get().getValue().isPresent()
        }

        when: "a sensor value is received that links to an attribute using a substring filter"
        state = new AttributeState(mockThing.id, "filterSubstring", "Substring test value")
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(state)

        then: "the linked attributes value should be updated with the filtered result"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true)
            assert mockThing.getAttribute("filterSubstring").get().getValue().orElse(null) == "te"
        }

        when: "the same attribute receives a sensor value that doesn't match the substring filter"
        state = new AttributeState(mockThing.id, "filterSubstring", "Substring")
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(state)

        then: "the linked attributes value should be updated to null"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true)
            assert !mockThing.getAttribute("filterSubstring").get().getValue().isPresent()
        }

        when: "a sensor value is received that links to an attribute using a regex and substring filter"
        state = new AttributeState(mockThing.id, "filterRegexSubstring", '{"prop1":true,"prop2":"volume is at 90%"}')
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(state)

        then: "the linked attributes value should be updated with the filtered result"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true)
            assert mockThing.getAttribute("filterRegexSubstring").get().getValue().orElse(0d) == 90d
        }

        when: "the same attribute receives a sensor value that doesn't match the substring filter"
        state = new AttributeState(mockThing.id, "filterRegexSubstring", '"volume is at 90%"}')
        ((MockProtocol)agentService.getProtocolInstance(mockAgent1.id)).updateReceived(state)

        then: "the linked attributes value should be updated to null"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.id, true)
            assert !mockThing.getAttribute("filterRegexSubstring").get().getValue().isPresent()
        }
    }
}
