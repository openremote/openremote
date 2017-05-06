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

import org.openremote.agent3.protocol.AbstractProtocol
import org.openremote.agent3.protocol.Protocol
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.model.Constants
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ProtocolConfiguration
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.AttributeType
import org.openremote.model.attribute.MetaItem
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
        List<AttributeEvent> protocolWriteAttributes = []
        List<String> protocolMethodCalls = []
        def mockProtocol = new AbstractProtocol() {
            protected void responseReceived() {
                // Assume we've pushed the update to the actual device and it responded with OK
                // so now we want to cause a sensor update that will go through the processing
                // chain.
                updateLinkedAttribute(protocolWriteAttributes.last().getAttributeState())
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
                protocolWriteAttributes.add(event)
            }

            @Override
            String getProtocolName() {
                return mockProtocolName
            }
        }

        and: "the container is started with the mock protocol"
        def serverPort = findEphemeralPort()
        def container = startContainerMinimal(defaultConfig(serverPort), defaultServices(mockProtocol))
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

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
                new AssetAttribute("light1Toggle", AttributeType.BOOLEAN, Values.create(false))
                    .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                            AssetMeta.AGENT_LINK,
                            new AttributeRef(mockAgent.getId(), "mockConfig1").toArrayValue()
                        )
                    ),
                new AssetAttribute("temp1Target", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                            AssetMeta.AGENT_LINK,
                            new AttributeRef(mockAgent.getId(), "mockConfig1").toArrayValue()
                        )
                    ),
                new AssetAttribute("invalid1Toggle", AttributeType.BOOLEAN, Values.create(false))
                    .setMeta(
                        new MetaItem(
                            AssetMeta.AGENT_LINK,
                            new AttributeRef(mockAgent.getId(), "mockConfig1").toArrayValue()
                        )
                    ),
                new AssetAttribute("light2Toggle", AttributeType.BOOLEAN, Values.create(false))
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig2").toArrayValue()
                        )
                ),
                new AssetAttribute("temp2Target", AttributeType.BOOLEAN)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig2").toArrayValue()
                        )
                ),
                new AssetAttribute("light3Toggle", AttributeType.BOOLEAN, Values.create(false))
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig3").toArrayValue()
                        )
                ),
                new AssetAttribute("temp3Target", AttributeType.BOOLEAN)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig3").toArrayValue()
                        )
                ),
                new AssetAttribute("light4Toggle", AttributeType.BOOLEAN, Values.create(false))
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig4").toArrayValue()
                        )
                ),
                new AssetAttribute("temp4Target", AttributeType.BOOLEAN)
                        .setMeta(
                        new MetaItem("MOCK_ATTRIBUTE_REQUIRED_META"),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mockConfig4").toArrayValue()
                        )
                ),
                new AssetAttribute("invalid5Toggle", AttributeType.BOOLEAN, Values.create(false))
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
                        )
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

        when: "a linked attribute is removed"
        protocolMethodCalls.clear()
        mockThing.removeAttribute("temp3Target")
        mockThing = assetStorageService.merge(mockThing)

        then: "the protocol should not be unlinked"
        conditions.eventually {
            assert protocolLinkedAttributes['mockConfig3'].size() == 1
            assert protocolMethodCalls.size() == 1
            assert protocolMethodCalls[0] == "UNLINK_ATTRIBUTE"
        }

        when: "a protocol configuration is removed"
        protocolMethodCalls.clear()
        mockAgent.removeAttribute("mockConfig4")
        mockAgent = assetStorageService.merge(mockAgent)

        then: "the attributes should be unlinked then the protocol configuration"
        conditions.eventually {
            assert protocolLinkedAttributes["mockConfig4"].size() == 0
            assert protocolMethodCalls.size() == 3
            assert protocolMethodCalls[0] == "UNLINK_ATTRIBUTE"
            assert protocolMethodCalls[1] == "UNLINK_ATTRIBUTE"
            assert protocolMethodCalls[2] == "UNLINK_PROTOCOL"
        }

        // TODO: Extend this test
    }
}