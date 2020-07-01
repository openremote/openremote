package org.openremote.test.assets

import org.openremote.agent.protocol.AbstractProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.*
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.rules.RulesService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ProtocolConfiguration
import org.openremote.model.attribute.*
import org.openremote.model.value.Value
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.persistence.EntityManager
import java.util.logging.Logger

class AssetProcessingTest extends Specification implements ManagerContainerTrait {

    Logger LOG = Logger.getLogger(AssetProcessingTest.class.getName())

    def "Check processing of asset updates through the processing chain"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "a mock protocol"
        def mockProtocolName = "urn:myCustom:mockProtocol"
        def protocolDeployed = false
        List<AttributeEvent> sendToActuatorEvents = []
        def mockProtocol = new AbstractProtocol() {
            protected void responseReceived() {
                // Assume we've pushed the update to the actual device and it responded with OK
                // so now we want to cause a sensor update that will go through the processing
                // chain.
                updateLinkedAttribute(sendToActuatorEvents.last().getAttributeState())
            }

            @Override
            protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
                return null
            }

            @Override
            protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
                return null
            }

            @Override
            protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
                LOG.info("Mock Protocol: linkProtocol")
            }

            @Override
            protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
                LOG.info("Mock Protocol: unlinkProtocol")
            }

            @Override
            protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
                protocolDeployed = true
                LOG.info("Mock Protocol: linkAttribute")
            }

            @Override
            protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
                LOG.info("Mock Protocol: unlinkAttribute")
            }

            @Override
            protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
                LOG.info("Mock Protocol: processLinkedAttributeWrite")
                sendToActuatorEvents.add(event)
            }

            @Override
            String getVersion() {
                return "1.0"
            }

            @Override
            String getProtocolName() {
                return mockProtocolName
            }

            @Override
            String getProtocolDisplayName() {
                return "Mock"
            }
        }

        and: "mock attribute state consumers"
        List<Attribute> updatesPassedStartOfProcessingChain = []
        List<Attribute> updatesPassedAgentService = []
        List<Attribute> updatesPassedRulesService = []
        List<Attribute> updatesPassedDatapointService = []
        List<Attribute> updatesPassedAttributeLinkingService = []

        AssetUpdateProcessor firstProcessor = new AssetUpdateProcessor() {
            @Override
            boolean processAssetUpdate(EntityManager em, Asset asset, AssetAttribute attribute, AttributeEvent.Source source) throws AssetProcessingException {
                updatesPassedStartOfProcessingChain.add(attribute)
                false
            }
        }

        AssetUpdateProcessor afterAgentServiceProcessor = new AssetUpdateProcessor() {
            @Override
            boolean processAssetUpdate(EntityManager em, Asset asset, AssetAttribute attribute, AttributeEvent.Source source) throws AssetProcessingException {
                updatesPassedAgentService.add(attribute)
                false
            }
        }

        AssetUpdateProcessor afterRulesServiceProcessor = new AssetUpdateProcessor() {
            @Override
            boolean processAssetUpdate(EntityManager em, Asset asset, AssetAttribute attribute, AttributeEvent.Source source) throws AssetProcessingException {
                updatesPassedRulesService.add(attribute)
                false
            }
        }

        AssetUpdateProcessor afterDatapointServiceProcessor = new AssetUpdateProcessor() {
            @Override
            boolean processAssetUpdate(EntityManager em, Asset asset, AssetAttribute attribute, AttributeEvent.Source source) throws AssetProcessingException {
                updatesPassedDatapointService.add(attribute)
                false
            }
        }

        AssetUpdateProcessor afterAttributeLinkingServiceProcessor = new AssetUpdateProcessor() {
            @Override
            boolean processAssetUpdate(EntityManager em, Asset asset, AssetAttribute attribute, AttributeEvent.Source source) throws AssetProcessingException {
                updatesPassedAttributeLinkingService.add(attribute)
                false
            }
        }

        when: "the container is started with the mock protocol and consumers"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices(mockProtocol))
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        then: "the container should be running and initialised"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 500)
        }

        then: "register mock asset processors"
        assetProcessingService.processors.add(0, firstProcessor)
        assetProcessingService.processors.add(assetProcessingService.processors.findIndexOf {it instanceof AgentService}+1, afterAgentServiceProcessor)
        assetProcessingService.processors.add(assetProcessingService.processors.findIndexOf {it instanceof RulesService}+1, afterRulesServiceProcessor)
        assetProcessingService.processors.add(assetProcessingService.processors.findIndexOf {it instanceof AssetDatapointService}+1, afterDatapointServiceProcessor)
        assetProcessingService.processors.add(assetProcessingService.processors.findIndexOf {it instanceof AssetAttributeLinkingService}+1, afterAttributeLinkingServiceProcessor)

        when: "a mock agent that uses the mock protocol is created"
        def mockAgent = new Asset()
        mockAgent.setName("Mock Agent")
        mockAgent.setType(AssetType.AGENT)
        mockAgent.setAttributes(
                ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("mock123"), mockProtocolName)
        )
        mockAgent.setRealm(keycloakDemoSetup.masterTenant.realm)
        mockAgent = assetStorageService.merge(mockAgent)

        and: "a mock thing asset is created with a valid protocol attribute, an invalid protocol attribute and a plain attribute"
        def mockThing = new Asset("Mock Thing Asset", AssetType.THING, mockAgent)
        mockThing.setAttributes(
                new AssetAttribute("light1Toggle", AttributeValueType.BOOLEAN, Values.create(true))
                        .setMeta(
                        new MetaItem(
                                MetaItemType.DESCRIPTION,
                                Values.create("The switch for the light 1 in the living room")
                        ),
                        new MetaItem(
                                MetaItemType.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mock123").toArrayValue()
                        )
                ),
                new AssetAttribute("light2Toggle", AttributeValueType.BOOLEAN, Values.create(true))
                        .setMeta(
                        new MetaItem(
                                MetaItemType.DESCRIPTION,
                                Values.create("The switch for the light 2 in the living room")
                        ),
                        new MetaItem(
                                MetaItemType.AGENT_LINK,
                                new AttributeRef("INVALID AGENT ID", managerDemoSetup.agentProtocolConfigName).toArrayValue()
                        )
                ),
                new AssetAttribute("plainAttribute", AttributeValueType.STRING, Values.create("demo"))
                        .setMeta(
                        new MetaItem(
                                MetaItemType.DESCRIPTION,
                                Values.create("A plain string attribute for storing information")
                        )
                )
        )
        mockThing = assetStorageService.merge(mockThing)

        then: "the mock thing to be deployed to the protocol"
        conditions.eventually {
            assert protocolDeployed
        }
        when: "an attribute event occurs for a valid protocol linked attribute on the test asset"
        def light1toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light1Toggle"), Values.create(false))
        )
        assetProcessingService.sendAttributeEvent(light1toggleOn)

        then: "the attribute event should reach the protocol and stop at the agent service, not be in the database"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesPassedStartOfProcessingChain[0].nameOrThrow == "light1Toggle"
            assert !updatesPassedStartOfProcessingChain[0].valueAsBoolean.get()
            assert sendToActuatorEvents.size() == 1
            assert updatesPassedAgentService.size() == 0
            assert updatesPassedRulesService.size() == 0
            assert updatesPassedDatapointService.size() == 0
            assert updatesPassedAttributeLinkingService.size() == 0
            // Light toggle should still be on in database
            def asset = assetStorageService.find(mockThing.getId(), true)
            assert asset.getAttribute("light1Toggle").get().getValueAsBoolean().get()
        }

        when: "the protocol updates the attributes value with the value it just received"
        updatesPassedStartOfProcessingChain.clear()
        updatesPassedRulesService.clear()
        updatesPassedAgentService.clear()
        updatesPassedDatapointService.clear()
        updatesPassedAttributeLinkingService.clear()
        mockProtocol.responseReceived()

        then: "a new attribute event should occur and reach the end of the processing chain, be stored in database"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesPassedStartOfProcessingChain[0].nameOrThrow == "light1Toggle"
            assert !updatesPassedStartOfProcessingChain[0].valueAsBoolean.get()
            assert updatesPassedAgentService.size() == 1
            assert updatesPassedRulesService.size() == 1
            assert updatesPassedDatapointService.size() == 1
            assert updatesPassedAttributeLinkingService.size() == 1
            // Light toggle should be off in database
            def asset = assetStorageService.find(mockThing.getId(), true)
            assert !asset.getAttribute("light1Toggle").get().getValueAsBoolean().get()
        }

        when: "an attribute event occurs for the invalid protocol attribute"
        updatesPassedStartOfProcessingChain.clear()
        updatesPassedRulesService.clear()
        updatesPassedAgentService.clear()
        updatesPassedDatapointService.clear()
        updatesPassedAttributeLinkingService.clear()
        sendToActuatorEvents.clear()
        def light2toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light2Toggle"), Values.create(true))
        )
        assetProcessingService.sendAttributeEvent(light2toggleOn)

        then: "the attribute event should pass the start of the processing chain, but not reach the mock protocol or the end of the processing chain and error should be populated"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesPassedStartOfProcessingChain[0].nameOrThrow == "light2Toggle"
            assert sendToActuatorEvents.size() == 0
            assert updatesPassedAgentService.size() == 0
            assert updatesPassedRulesService.size() == 0
            assert updatesPassedDatapointService.size() == 0
            assert updatesPassedAttributeLinkingService.size() == 0
        }

        when: "an attribute event occurs for the plain attribute"
        updatesPassedStartOfProcessingChain.clear()
        updatesPassedRulesService.clear()
        updatesPassedAgentService.clear()
        updatesPassedDatapointService.clear()
        updatesPassedAttributeLinkingService.clear()
        sendToActuatorEvents.clear()
        def plainAttributeTest = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "plainAttribute"), Values.create("test"))
        )
        assetProcessingService.sendAttributeEvent(plainAttributeTest)

        then: "the attribute event should pass the start of the processing chain and reach the end of the processing chain"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesPassedAgentService.size() == 1
            assert updatesPassedRulesService.size() == 1
            assert updatesPassedDatapointService.size() == 1
            assert updatesPassedAttributeLinkingService.size() == 1
            assert updatesPassedAttributeLinkingService[0].nameOrThrow == "plainAttribute"
            assert updatesPassedAttributeLinkingService[0].valueAsString.orElse(null) == "test"
            assert sendToActuatorEvents.size() == 0
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
