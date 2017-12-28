package org.openremote.test.assets

import org.openremote.agent.protocol.AbstractProtocol
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetState
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ProtocolConfiguration
import org.openremote.model.attribute.*
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer
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
            protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
                LOG.info("Mock Protocol: linkProtocol")
            }

            @Override
            protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
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
            protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
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
        List<AssetState> updatesPassedStartOfProcessingChain = []
        List<AssetState> updatesPassedRulesService = []
        List<AssetState> updatesPassedAgentService = []
        List<AssetState> updatesPassedAssetStorageService = []
        List<AssetState> updatesPassedDatapointService = []

        Consumer<AssetState> mockStartConsumer = { assetUpdate ->
            updatesPassedStartOfProcessingChain.add(assetUpdate)
        }

        Consumer<AssetState> mockRulesServiceConsumer = { assetState ->
            updatesPassedRulesService.add(assetState)
        }

        Consumer<AssetState> mockAgentServiceConsumer = { assetState ->
            updatesPassedAgentService.add(assetState)
        }

        Consumer<AssetState> mockAssetStorageConsumer = { assetState ->
            updatesPassedAssetStorageService.add(assetState)
        }

        Consumer<AssetState> mockDatapointServiceConsumer = { assetState ->
            updatesPassedDatapointService.add(assetState)
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
        assetProcessingService.processors.add(mockDatapointServiceConsumer)
        assetProcessingService.processors.add(3, mockAssetStorageConsumer)
        assetProcessingService.processors.add(2, mockRulesServiceConsumer)
        assetProcessingService.processors.add(1, mockAgentServiceConsumer)
        assetProcessingService.processors.add(0, mockStartConsumer)

        when: "a mock agent that uses the mock protocol is created"
        def mockAgent = new ServerAsset()
        mockAgent.setName("Mock Agent")
        mockAgent.setType(AssetType.AGENT)
        mockAgent.setAttributes(
                ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("mock123"), mockProtocolName)
        )
        mockAgent.setRealmId(keycloakDemoSetup.masterTenant.id)
        mockAgent = assetStorageService.merge(mockAgent)

        and: "a mock thing asset is created with a valid protocol attribute, an invalid protocol attribute and a plain attribute"
        def mockThing = new ServerAsset("Mock Thing Asset", AssetType.THING, mockAgent)
        mockThing.setAttributes(
                new AssetAttribute("light1Toggle", AttributeType.BOOLEAN, Values.create(false))
                        .setMeta(
                        new MetaItem(
                                AssetMeta.DESCRIPTION,
                                Values.create("The switch for the light 1 in the living room")
                        ),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef(mockAgent.getId(), "mock123").toArrayValue()
                        )
                ),
                new AssetAttribute("light2Toggle", AttributeType.BOOLEAN, Values.create(false))
                        .setMeta(
                        new MetaItem(
                                AssetMeta.DESCRIPTION,
                                Values.create("The switch for the light 2 in the living room")
                        ),
                        new MetaItem(
                                AssetMeta.AGENT_LINK,
                                new AttributeRef("INVALID AGENT ID", managerDemoSetup.agentProtocolConfigName).toArrayValue()
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

        then: "the mock thing to be deployed to the protocol"
        conditions.eventually {
            assert protocolDeployed
        }
        when: "an attribute event occurs for a valid protocol linked attribute on the test asset"
        def light1toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light1Toggle"), Values.create(false))
        )
        assetProcessingService.sendAttributeEvent(light1toggleOn)

        then: "the attribute event should reach the protocol and stop at the agent service with a status of COMPLETED"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesPassedAgentService.size() == 0
            assert updatesPassedRulesService.size() == 0
            assert updatesPassedAssetStorageService.size() == 0
            assert updatesPassedDatapointService.size() == 0
            assert updatesPassedStartOfProcessingChain[0].attribute.getAssetId().orElse("") == mockThing.getId()
            assert updatesPassedStartOfProcessingChain[0].attribute.getName().orElse("") == "light1Toggle"
            assert !updatesPassedStartOfProcessingChain[0].getValueAsBoolean()
            assert updatesPassedStartOfProcessingChain[0].error == null
            assert updatesPassedStartOfProcessingChain[0].processingStatus == AssetState.ProcessingStatus.COMPLETED
            assert sendToActuatorEvents.size() == 1
        }

        when: "the protocol updates the attributes value with the value it just received"
        updatesPassedStartOfProcessingChain.clear()
        updatesPassedRulesService.clear()
        updatesPassedAgentService.clear()
        updatesPassedAssetStorageService.clear()
        updatesPassedDatapointService.clear()
        mockProtocol.responseReceived()

        then: "a new attribute event should occur and reach the end of the processing chain"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesPassedAgentService.size() == 1
            assert updatesPassedRulesService.size() == 1
            assert updatesPassedAssetStorageService.size() == 1
            assert updatesPassedDatapointService.size() == 1
            assert updatesPassedStartOfProcessingChain[0].attribute.getAssetId().orElse("") == mockThing.getId()
            assert updatesPassedStartOfProcessingChain[0].attribute.getName().orElse("") == "light1Toggle"
            assert !Values.getBoolean(updatesPassedStartOfProcessingChain[0].getOldValue()).orElse(true)
            assert !updatesPassedStartOfProcessingChain[0].getValueAsBoolean()
            assert updatesPassedStartOfProcessingChain[0].error == null
            assert updatesPassedStartOfProcessingChain[0].processingStatus == AssetState.ProcessingStatus.COMPLETED
        }

        when: "an attribute event occurs for the invalid protocol attribute"
        updatesPassedStartOfProcessingChain.clear()
        updatesPassedRulesService.clear()
        updatesPassedAgentService.clear()
        updatesPassedAssetStorageService.clear()
        updatesPassedDatapointService.clear()
        sendToActuatorEvents.clear()
        def light2toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light2Toggle"), Values.create(true))
        )
        assetProcessingService.sendAttributeEvent(light2toggleOn)

        then: "the attribute event should pass the start of the processing chain, but not reach the mock protocol or the end of the processing chain and error should be populated"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesPassedAgentService.size() == 0
            assert updatesPassedRulesService.size() == 0
            assert updatesPassedAssetStorageService.size() == 0
            assert updatesPassedDatapointService.size() == 0
            assert sendToActuatorEvents.size() == 0
            assert updatesPassedStartOfProcessingChain[0].id == mockThing.id
            assert updatesPassedStartOfProcessingChain[0].attributeName == "light2Toggle"
            assert updatesPassedStartOfProcessingChain[0].error != null
            assert updatesPassedStartOfProcessingChain[0].error instanceof RuntimeException
            assert updatesPassedStartOfProcessingChain[0].processingStatus == AssetState.ProcessingStatus.ERROR
        }

        when: "an attribute event occurs for the plain attribute"
        updatesPassedStartOfProcessingChain.clear()
        updatesPassedRulesService.clear()
        updatesPassedAgentService.clear()
        updatesPassedAssetStorageService.clear()
        updatesPassedDatapointService.clear()
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
            assert updatesPassedAssetStorageService.size() == 1
            assert updatesPassedDatapointService.size() == 1
            assert sendToActuatorEvents.size() == 0
            assert updatesPassedDatapointService[0].name == "Mock Thing Asset"
            assert updatesPassedDatapointService[0].attributeName == "plainAttribute"
            assert Values.getString(updatesPassedDatapointService[0].oldValue).orElse(null) == "demo"
            assert Values.getString(updatesPassedDatapointService[0].value).orElse(null) == "test"
            assert updatesPassedDatapointService[0].error == null
            assert updatesPassedDatapointService[0].processingStatus == AssetState.ProcessingStatus.COMPLETED
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
