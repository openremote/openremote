package org.openremote.test.assets

import elemental.json.Json
import org.openremote.agent3.protocol.AbstractProtocol
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.*
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetState
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ProtocolConfiguration
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer
import java.util.logging.Logger

class AssetProcessingTest extends Specification implements ManagerContainerTrait {
    Logger LOG = Logger.getLogger(AssetStorageService.class.getName())

    def "Check processing of asset updates"() {

        //region given: "expected conditions"
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        //endregion

        //region and: "a mock protocol"
        // TODO: Update this to use Simulator Protocol
        and: "a mock protocol"
        def mockProtocolName = Constants.PROTOCOL_NAMESPACE + ":mockProtocol"
        def protocolDeployed = false
        List<AttributeEvent> sendToActuatorEvents = []
        def mockProtocol = new AbstractProtocol() {
            protected void responseReceived() {
                // Assume we've pushed the update to the actual device and it responded with OK
                // so now we want to cause a sensor update that will go through the processing
                // chain.
                onSensorUpdate(sendToActuatorEvents.last().getAttributeState())
            }

            @Override
            protected void sendToActuator(AttributeEvent event) {
                LOG.info("Mock Protocol: sendToActuator")
                sendToActuatorEvents.add(event)
            }

            @Override
            protected void onAttributeAdded(AssetAttribute attribute, ProtocolConfiguration protocolConfiguration) {
                protocolDeployed = true
                LOG.info("Mock Protocol: onAttributeAdded")
            }

            @Override
            protected void onAttributeUpdated(AssetAttribute attribute, ProtocolConfiguration protocolConfiguration) {
                LOG.info("Mock Protocol: onAttributeUpdated")
            }

            @Override
            protected void onAttributeRemoved(AssetAttribute attribute, ProtocolConfiguration protocolConfiguration) {
                LOG.info("Mock Protocol: onAttributeRemoved")
            }

            @Override
            String getProtocolName() {
                return mockProtocolName
            }
        }
        //endregion

        //region and: "a mock attribute event consumer"
        and: "a mock attribute event consumer"
        List<AssetState> updatesPassedStartOfProcessingChain = []
        List<AssetState> updatesReachedEndOfProcessingChain = []

        Consumer<AssetState> mockEndConsumer = { assetUpdate ->
            updatesReachedEndOfProcessingChain.add(assetUpdate)
        }

        Consumer<AssetState> mockStartConsumer = { assetUpdate ->
            updatesPassedStartOfProcessingChain.add(assetUpdate)
        }
        //endregion

        //region and: "the container is started with the mock consumers and mock protocol"
        and: "the container is started with the mock consumers and mock protocol"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices(mockProtocol))
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesService = container.getService(RulesService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        assetProcessingService.processors.add(0, mockStartConsumer)
        assetProcessingService.processors.add(mockEndConsumer)
        //endregion

        // The conditions here depend on how we handle the initial insert of facts into the rule engines
        //expect: "the demo rule engine should start and trigger the 'Switch room lights off when apartment ALL LIGHTS OFF switch is off' rule and cause attribute events to go through the system"
        expect: "the demo rule engine should start and trigger the 'Switch room lights off when apartment ALL LIGHTS OFF switch is off' rule and cause attribute events to go through the system"
        conditions.eventually {
            def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.rulesets.size() == 3
            assert updatesPassedStartOfProcessingChain.size() == 0
            assert updatesReachedEndOfProcessingChain.size() == 0
        }

        when: "the processing chain counters are reset"
        updatesPassedStartOfProcessingChain.clear()
        updatesReachedEndOfProcessingChain.clear()

        //region and: "a mock agent that uses the mock protocol is created"
        and: "a mock agent that uses the mock protocol is created"
        def mockAgent = new ServerAsset()
        mockAgent.setName("Mock Agent")
        mockAgent.setType(AssetType.AGENT)
        def mockProtocolConfig = new ProtocolConfiguration("mock123", mockProtocolName)
        mockAgent.setAttributes(Collections.singletonList(mockProtocolConfig.getAttribute()))
        mockAgent.setRealmId(keycloakDemoSetup.masterTenant.id)
        mockAgent = assetStorageService.merge(mockAgent)
        //endregion

        //region and: "a mock thing asset is created with a valid protocol attribute, an invalid protocol attribute and a plain attribute"
        and: "a mock thing asset is created with a valid protocol attribute, an invalid protocol attribute and a plain attribute"
        def mockThing = new ServerAsset(mockAgent)
        mockThing.setName("Mock Thing Asset")
        mockThing.setType(AssetType.THING)
        def mockThingAttributes = [
                new AssetAttribute("light1Toggle", AttributeType.BOOLEAN, Json.create(false))
                        .setMeta(new Meta()
                        .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The switch for the light 1 in the living room"))
                )

                        .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(mockAgent.getId(), "mock123").asJsonValue()
                )
                )
                ),
                new AssetAttribute("light2Toggle", AttributeType.BOOLEAN, Json.create(false))
                        .setMeta(new Meta()
                        .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The switch for the light 2 in the living room"))
                )

                        .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef("INVALID AGENT ID", managerDemoSetup.agentProtocolConfigName).asJsonValue()
                )
                )
                ),
                new AssetAttribute("plainAttribute", AttributeType.STRING, Json.create("demo"))
                        .setMeta(new Meta()
                        .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("A plain string attribute for storing information"))
                )
                )
        ]
        mockThing.setAttributes(mockThingAttributes)
        mockThing = assetStorageService.merge(mockThing)
        //endregion


        then: "the mock thing to be deployed to the protocol"
        conditions.eventually {
            assert protocolDeployed
        }

        when: "an attribute event occurs for the valid protocol attribute"
        def light1toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light1Toggle"), Json.create(true))
        )
        assetProcessingService.sendAttributeEvent(light1toggleOn)

        then: "the attribute event should pass the start of the processing chain, reach the mock protocol and but not reach the end of the processing chain"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesReachedEndOfProcessingChain.size() == 0
            assert sendToActuatorEvents.size() == 1
            assert sendToActuatorEvents[0].entityId == mockThing.id
            assert sendToActuatorEvents[0].attributeName == "light1Toggle"
            assert sendToActuatorEvents[0].attributeState.value.asBoolean() == true
            assert updatesPassedStartOfProcessingChain[0].processingStatus == AssetState.ProcessingStatus.COMPLETED
        }

        when: "the protocol gets a response from the server"
        mockProtocol.responseReceived()

        //region then: "the protocol should have sent a new attribute event for the sensor change which should pass the start of the processing chain skip the protocol but still reach the end of the processing chain
        then: "the protocol should have sent a new attribute event for the sensor change which should pass the start of the processing chain skip the protocol but still reach the end of the processing chain"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 2
            assert sendToActuatorEvents.size() == 1
            assert updatesReachedEndOfProcessingChain.size() == 1
            assert updatesReachedEndOfProcessingChain[0].name == "Mock Thing Asset"
            assert updatesReachedEndOfProcessingChain[0].attributeName == "light1Toggle"
            assert updatesReachedEndOfProcessingChain[0].oldValue.asBoolean() == false
            assert updatesReachedEndOfProcessingChain[0].value.asBoolean() == true
            assert updatesReachedEndOfProcessingChain[0].processingStatus == AssetState.ProcessingStatus.COMPLETED
        }
        //endregion

        when: "an attribute event occurs for the invalid protocol attribute"
        updatesPassedStartOfProcessingChain.clear()
        updatesReachedEndOfProcessingChain.clear()
        sendToActuatorEvents.clear()
        def light2toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light2Toggle"), Json.create(true))
        )
        assetProcessingService.sendAttributeEvent(light2toggleOn)

        then: "the attribute event should pass the start of the processing chain, but not reach the mock protocol or the end of the processing chain and error should be populated"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesReachedEndOfProcessingChain.size() == 0
            assert sendToActuatorEvents.size() == 0
            assert updatesPassedStartOfProcessingChain[0].id == mockThing.id
            assert updatesPassedStartOfProcessingChain[0].attributeName == "light2Toggle"
            assert updatesPassedStartOfProcessingChain[0].error != null
            assert updatesPassedStartOfProcessingChain[0].error instanceof RuntimeException
            assert updatesPassedStartOfProcessingChain[0].processingStatus == AssetState.ProcessingStatus.COMPLETED
        }

        when: "an attribute event occurs for the plain attribute"
        updatesPassedStartOfProcessingChain.clear()
        updatesReachedEndOfProcessingChain.clear()
        sendToActuatorEvents.clear()
        def plainAttributeTest = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "plainAttribute"), Json.create("test"))
        )
        assetProcessingService.sendAttributeEvent(plainAttributeTest)

        then: "the attribute event should pass the start of the processing chain and reach the end of the processing chain"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert sendToActuatorEvents.size() == 0
            assert updatesReachedEndOfProcessingChain.size() == 1
            assert updatesReachedEndOfProcessingChain[0].name == "Mock Thing Asset"
            assert updatesReachedEndOfProcessingChain[0].attributeName == "plainAttribute"
            assert updatesReachedEndOfProcessingChain[0].oldValue.asString() == "demo"
            assert updatesReachedEndOfProcessingChain[0].value.asString() == "test"
            assert updatesReachedEndOfProcessingChain[0].error == null
            assert updatesReachedEndOfProcessingChain[0].processingStatus == AssetState.ProcessingStatus.COMPLETED
        }
    }
}
