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

import org.openremote.agent.protocol.AbstractProtocol
import org.openremote.agent.protocol.Protocol
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.model.Constants
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ProtocolConfiguration
import org.openremote.model.attribute.*
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.logging.Logger
/**
 * This tests the basic protocol interface and abstract protocol implementation.
 */
class BasicProtocolTest extends Specification implements ManagerContainerTrait {
    Logger LOG = Logger.getLogger(BasicProtocolTest.class.getName())

    def "Check processing of asset updates"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "a mock protocol"
        def mockProtocolName = "urn:myCustom:mockProtocol"
        Map<String, Integer> protocolExpectedLinkedAttributeCount = [:]
        protocolExpectedLinkedAttributeCount['mockConfig1'] = 2
        protocolExpectedLinkedAttributeCount['mockConfig2'] = 2
        protocolExpectedLinkedAttributeCount['mockConfig3'] = 2
        protocolExpectedLinkedAttributeCount['mockConfig4'] = 2
        List<AssetAttribute> protocolLinkedConfigurations = []
        Map<String, List<AssetAttribute>> protocolLinkedAttributes = [:]
        protocolLinkedAttributes['mockConfig1'] = []
        protocolLinkedAttributes['mockConfig2'] = []
        protocolLinkedAttributes['mockConfig3'] = []
        protocolLinkedAttributes['mockConfig4'] = []
        List<AttributeEvent> protocolWriteAttributeEvents = []
        List<String> protocolMethodCalls = []
        def mockProtocol = new AbstractProtocol() {
            protected void responseReceived() {
                // Assume we've pushed the update to the actual device and it responded with OK
                // so now we want to cause a sensor update that will go through the processing
                // chain.
                updateLinkedAttribute(protocolWriteAttributeEvents.last().getAttributeState())
            }

            protected void updateAttribute(AttributeState state) {
                sendAttributeEvent(state)
            }

            @Override
            protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
                protocolMethodCalls.add("LINK_PROTOCOL")
                protocolLinkedConfigurations.add(protocolConfiguration)
                if (!protocolConfiguration.getMetaItem("MOCK_REQUIRED_META").isPresent()) {
                    updateDeploymentStatus(protocolConfiguration.getReferenceOrThrow(), Protocol.DeploymentStatus.ERROR)
                } else if(protocolConfiguration.getMetaItem("MOCK_THROW_EXCEPTION").isPresent()) {
                    throw new IllegalStateException("Exception occurred whilst linking the protocol configuration")
                } else if (protocolConfiguration.isEnabled()) {
                    updateDeploymentStatus(protocolConfiguration.getReferenceOrThrow(), Protocol.DeploymentStatus.LINKED_ENABLED)
                } else {
                    updateDeploymentStatus(protocolConfiguration.getReferenceOrThrow(), Protocol.DeploymentStatus.LINKED_DISABLED)
                }
            }

            @Override
            protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
                protocolMethodCalls.add("UNLINK_PROTOCOL")
                protocolLinkedConfigurations.removeAll { it.getReferenceOrThrow().equals(protocolConfiguration.getReferenceOrThrow()) }
            }

            @Override
            protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
                protocolMethodCalls.add("LINK_ATTRIBUTE")

                if (!attribute.getMetaItem("MOCK_ATTRIBUTE_REQUIRED_META").isPresent()) {
                    // This tests exception handling during linking of attributes
                    throw new IllegalStateException("Attribute is not valid");
                }
                protocolLinkedAttributes[protocolConfiguration.getName().orElse("")] << attribute

                def deploymentStatus = getDeploymentStatus(protocolConfiguration);
                if (deploymentStatus == Protocol.DeploymentStatus.LINKED_ENABLED) {
                    String attributeName = attribute.getName().orElse("");
                    if (attributeName.startsWith("lightToggle")) {
                        // Set all lights to on
                        updateLinkedAttribute(new AttributeState(attribute.getReferenceOrThrow(), Values.create(true)))
                    } else if (attributeName.startsWith("tempTarget")) {
                        // Set target temps to 25.5
                        updateLinkedAttribute(new AttributeState(attribute.getReferenceOrThrow(), Values.create(25.5)))
                    }
                }
            }

            @Override
            protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
                protocolMethodCalls.add("UNLINK_ATTRIBUTE")
                (protocolLinkedAttributes[protocolConfiguration.getName().orElse("")])
                        .removeAll { it.getReferenceOrThrow().equals(attribute.getReferenceOrThrow()) }
            }

            @Override
            protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
                protocolMethodCalls.add("ATTRIBUTE_WRITE")
                if (!(protocolLinkedAttributes[protocolConfiguration.getName().orElse("")])
                        .any {it.getReferenceOrThrow().equals(event.attributeRef)}) {
                    throw new IllegalStateException("Attribute is not linked")
                }
                protocolWriteAttributeEvents.add(event)
            }

            @Override
            String getProtocolName() {
                return mockProtocolName
            }
        }

        and: "the container is started with the mock protocol"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices(mockProtocol))
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        when: "a mock agent that uses the mock protocol is created with a valid protocol configuration"
        def mockAgent = new ServerAsset()
        mockAgent.setName("Mock Agent")
        mockAgent.setType(AssetType.AGENT)
        mockAgent.setAttributes(
            ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("mockConfig1"), mockProtocolName)
                .addMeta(
                    new MetaItem("MOCK_REQUIRED_META", Values.create(true))
                ),
            ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("mockConfig2"), mockProtocolName),
            ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("mockConfig3"), mockProtocolName)
                .addMeta(
                    new MetaItem("MOCK_THROW_EXCEPTION", Values.create(""))
                ),
            ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("mockConfig4"), mockProtocolName)
                .addMeta(
                    new MetaItem("MOCK_REQUIRED_META", Values.create(true)),
                    new MetaItem(AssetMeta.ENABLED, Values.create(false))
                )
        )
        mockAgent.setRealmId(Constants.MASTER_REALM)
        mockAgent = assetStorageService.merge(mockAgent)

        then: "the protocol configurations should be linked and their deployment status should be available in the agent service"
        conditions.eventually {
            assert protocolLinkedConfigurations.size() == 4
            def config1 = protocolLinkedConfigurations.find { it.getName().orElse("").equals("mockConfig1") }
            def config2 = protocolLinkedConfigurations.find { it.getName().orElse("").equals("mockConfig2") }
            def config3 = protocolLinkedConfigurations.find { it.getName().orElse("").equals("mockConfig3") }
            def config4 = protocolLinkedConfigurations.find { it.getName().orElse("").equals("mockConfig4") }
            assert config1 != null
            assert config2 != null
            assert config3 != null
            assert config4 != null
            assert agentService.getProtocolDeploymentStatus(config1.getReferenceOrThrow()) == Protocol.DeploymentStatus.LINKED_ENABLED
            assert agentService.getProtocolDeploymentStatus(config2.getReferenceOrThrow()) == Protocol.DeploymentStatus.ERROR
            assert agentService.getProtocolDeploymentStatus(config3.getReferenceOrThrow()) == Protocol.DeploymentStatus.ERROR
            assert agentService.getProtocolDeploymentStatus(config4.getReferenceOrThrow()) == Protocol.DeploymentStatus.LINKED_DISABLED
        }

        when: "a mock thing asset is created that links to the mock protocol configurations"
        def mockThing = new ServerAsset("Mock Thing Asset", AssetType.THING, mockAgent)
        mockThing.setAttributes(
                new AssetAttribute("lightToggle1", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                            AssetMeta.AGENT_LINK,
                            new AttributeRef(mockAgent.getId(), "mockConfig1").toArrayValue()
                        )
                    ),
                new AssetAttribute("tempTarget1", AttributeType.DECIMAL)
                    .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                            AssetMeta.AGENT_LINK,
                            new AttributeRef(mockAgent.getId(), "mockConfig1").toArrayValue()
                        )
                    ),
                new AssetAttribute("invalidToggle1", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem(
                            AssetMeta.AGENT_LINK,
                            new AttributeRef(mockAgent.getId(), "mockConfig1").toArrayValue()
                        )
                    ),
                new AssetAttribute("lightToggle2", AttributeType.BOOLEAN)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig2").toArrayValue()
                        )
                ),
                new AssetAttribute("tempTarget2", AttributeType.DECIMAL)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig2").toArrayValue()
                        )
                ),
                new AssetAttribute("lightToggle3", AttributeType.BOOLEAN)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig3").toArrayValue()
                        )
                ),
                new AssetAttribute("tempTarget3", AttributeType.DECIMAL)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig3").toArrayValue()
                        )
                ),
                new AssetAttribute("lightToggle4", AttributeType.BOOLEAN)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig4").toArrayValue()
                        )
                ),
                new AssetAttribute("tempTarget4", AttributeType.DECIMAL)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META", Values.create(true)),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig4").toArrayValue()
                        )
                ),
                new AssetAttribute("invalidToggle5", AttributeType.BOOLEAN, Values.create(false))
                    .setMeta(
                        new MetaItem(
                            AssetMeta.AGENT_LINK,
                            new AttributeRef(mockAgent.getId(), "INVALID CONFIG").toArrayValue()
                        )
                    ),
                new AssetAttribute("plainAttribute", AttributeType.STRING, Values.create("demo"))
                        .setMeta(
                        new MetaItem(
                                AssetMeta.DESCRIPTION,
                                Values.create("A plain string attribute for storing information")
                        ),
                        new MetaItem(AssetMeta.READ_ONLY, Values.create(true))
                )
        )
        mockThing = assetStorageService.merge(mockThing)

        then: "the mock thing to be fully deployed"
        conditions.eventually {
            assert protocolLinkedAttributes['mockConfig1'].size() == protocolExpectedLinkedAttributeCount['mockConfig1']
            assert protocolLinkedAttributes['mockConfig2'].size() == protocolExpectedLinkedAttributeCount['mockConfig2']
            assert protocolLinkedAttributes['mockConfig3'].size() == protocolExpectedLinkedAttributeCount['mockConfig3']
            assert protocolLinkedAttributes['mockConfig4'].size() == protocolExpectedLinkedAttributeCount['mockConfig4']
        }

        and: "the deployment should have occurred in the correct order"
        assert protocolMethodCalls.size() == 13
        assert protocolMethodCalls[0] == "LINK_PROTOCOL"
        assert protocolMethodCalls[1] == "LINK_PROTOCOL"
        assert protocolMethodCalls[2] == "LINK_PROTOCOL"
        assert protocolMethodCalls[3] == "LINK_PROTOCOL"
        assert protocolMethodCalls[4] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[5] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[6] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[7] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[8] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[9] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[10] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[11] == "LINK_ATTRIBUTE"
        assert protocolMethodCalls[12] == "LINK_ATTRIBUTE"

        when: "the mock thing is retrieved from the DB"
        mockThing = assetStorageService.find(mockThing.getId(), true)

        then: "the linked attributes values should have been updated by the protocol"
        // Check all valid linked attributes have the new values
        assert mockThing.getAttribute("lightToggle1").get().getValueAsBoolean().orElse(false)
        assert mockThing.getAttribute("tempTarget1").get().getValueAsNumber().orElse(0d) == 25.5d
        // Check disabled linked attributes don't have the new values
        assert !mockThing.getAttribute("lightToggle4").get().getValue().isPresent()
        assert !mockThing.getAttribute("tempTarget4").get().getValue().isPresent()
        // Check invalid attributes don't have the new values
        assert !mockThing.getAttribute("lightToggle2").get().getValue().isPresent()
        assert !mockThing.getAttribute("tempTarget2").get().getValue().isPresent()
        assert !mockThing.getAttribute("lightToggle3").get().getValue().isPresent()
        assert !mockThing.getAttribute("tempTarget3").get().getValue().isPresent()

        when: "a linked attribute is removed"
        protocolMethodCalls.clear()
        mockThing.removeAttribute("tempTarget3")
        mockThing = assetStorageService.merge(mockThing)

        then: "the protocol should not be unlinked"
        conditions.eventually {
            assert protocolLinkedAttributes['mockConfig3'].size() == 1
            assert protocolMethodCalls.size() == 1
            assert protocolMethodCalls[0] == "UNLINK_ATTRIBUTE"
        }

        when: "a protocol configuration is removed"
        protocolMethodCalls.clear()
        mockAgent.removeAttribute("mockConfig3")
        mockAgent = assetStorageService.merge(mockAgent)

        then: "the attributes should be unlinked then the protocol configuration"
        conditions.eventually {
            assert protocolLinkedAttributes["mockConfig3"].size() == 0
            assert protocolMethodCalls.size() == 2
            assert protocolMethodCalls[0] == "UNLINK_ATTRIBUTE"
            assert protocolMethodCalls[1] == "UNLINK_PROTOCOL"
        }

        when: "the mock protocol tries to update the plain readonly attribute"
        mockProtocol.updateAttribute(new AttributeState(mockThing.getId(),"plainAttribute", Values.create("UPDATE")))

        then: "the plain attributes value should be updated"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.getId(), true)
            assert mockThing.getAttribute("plainAttribute").get().getValueAsString().orElse("") == "UPDATE"
        }

        when: "a target temp linked attribute value is updated it should reach the protocol"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(mockThing.getId(), "tempTarget1", Values.create(30d)))

        then: "the update should reach the protocol as an attribute write request"
        conditions.eventually {
            assert protocolWriteAttributeEvents.size() == 1
            assert protocolWriteAttributeEvents[0].attributeName == "tempTarget1"
            assert protocolWriteAttributeEvents[0].attributeRef.entityId == mockThing.getId()
            Values.getNumber(protocolWriteAttributeEvents[0].value.orElse(null)).orElse(0d) == 30d
        }

        when: "the protocol has finished processing the attribute write"
        mockProtocol.responseReceived()

        then: "the target temp attributes value should be updated"
        conditions.eventually {
            mockThing = assetStorageService.find(mockThing.getId(), true)
            assert mockThing.getAttribute("tempTarget1").get().getValueAsNumber().orElse(0d) == 30d
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}