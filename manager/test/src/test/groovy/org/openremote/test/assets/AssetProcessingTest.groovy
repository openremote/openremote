package org.openremote.test.assets

import elemental.json.Json
import org.openremote.agent3.protocol.simulator.SimulatorProtocol
import org.openremote.agent3.protocol.AbstractProtocol
import org.openremote.manager.server.agent.AgentAttributes
import org.openremote.manager.server.agent.ThingAttributes
import org.openremote.manager.server.asset.AssetUpdate
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.model.Attribute
import org.openremote.model.AttributeEvent
import org.openremote.model.AttributeRef
import org.openremote.model.AttributeState
import org.openremote.model.Meta
import org.openremote.model.Constants
import org.openremote.model.MetaItem
import org.openremote.model.asset.ProtocolConfiguration
import org.openremote.model.asset.ThingAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.AttributeType
import org.openremote.test.ManagerContainerTrait
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetType
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer
import java.util.logging.Logger

class AssetProcessingTest extends Specification implements ManagerContainerTrait {
    Logger LOG = Logger.getLogger(AssetStorageService.class.getName());

    def "Check processing of attribute events"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "a mock protocol"
        def mockProtocolName =  Constants.PROTOCOL_NAMESPACE + ":mockProtocol"
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
            protected void onAttributeAdded(ThingAttribute attribute) {
                protocolDeployed = true
                LOG.info("Mock Protocol: onAttributeAdded")
            }

            @Override
            protected void onAttributeUpdated(ThingAttribute attribute) {
                LOG.info("Mock Protocol: onAttributeUpdated")
            }

            @Override
            protected void onAttributeRemoved(ThingAttribute attribute) {
                LOG.info("Mock Protocol: onAttributeRemoved")
            }

            @Override
            String getProtocolName() {
                return mockProtocolName
            }
        }

        and: "a mock attribute event consumer"
        List<AssetUpdate> updatesPassedStartOfProcessingChain = []
        List<AssetUpdate> updatesReachedEndOfProcessingChain = []

        Consumer<AssetUpdate> mockEndConsumer = {assetUpdate ->
            updatesReachedEndOfProcessingChain.add(assetUpdate)
        }

        Consumer<AssetUpdate> mockStartConsumer = {assetUpdate ->
            updatesPassedStartOfProcessingChain.add(assetUpdate)
        }

        and: "the container is started with the mock consumer and mock protocol"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices(mockProtocol))
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        assetProcessingService.processors.add(0, mockStartConsumer)
        assetProcessingService.processors.add(mockEndConsumer)

        and: "a mock agent that uses the mock protocol is created"
        def mockAgent = new ServerAsset();
        mockAgent.setName("Mock Agent");
        mockAgent.setType(AssetType.AGENT);
        AgentAttributes agentAttributes = new AgentAttributes();
        agentAttributes.setEnabled(false);
        ProtocolConfiguration mockProtocolConfig = new ProtocolConfiguration("mock123", mockProtocolName);
        agentAttributes.put(mockProtocolConfig);
        mockAgent.setAttributes(agentAttributes.getJsonObject());
        mockAgent.setRealmId(managerDemoSetup.masterRealmId)
        mockAgent = assetStorageService.merge(mockAgent);

        and: "a mock thing asset is created with a valid protocol attribute, a invalid protocol attribute and a plain attribute"
        def mockThing = new ServerAsset(mockAgent)
        mockThing.setName("Mock Thing Asset")
        mockThing.setType(AssetType.THING)
        def mockThingAttributes = new ThingAttributes(mockThing);
        mockThingAttributes.put(
                new Attribute("light1Toggle", AttributeType.BOOLEAN, Json.create(false))
                        .setMeta(new Meta()
                        .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The switch for the light 1 in the living room"))
                )

                        .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(mockAgent.getId(),"mock123").asJsonValue()
                )
                )
                ),
                new Attribute("light2Toggle", AttributeType.BOOLEAN, Json.create(false))
                        .setMeta(new Meta()
                        .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The switch for the light 2 in the living room"))
                )

                        .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef("INVALID AGENT ID", managerDemoSetup.demoAgentProtocolConfigName).asJsonValue()
                )
                )
                ),
                new Attribute("plainAttribute", AttributeType.STRING, Json.create("Demo"))
                        .setMeta(new Meta()
                        .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("A plain string attribute for storing information"))
                )
                )
        )
        mockThing.setAttributes(mockThingAttributes.getJsonObject());
        mockThing = assetStorageService.merge(mockThing);

        expect: "the mock thing to be deployed to the protocol"
        conditions.eventually {
            assert protocolDeployed
        }

        when: "an attribute event occurs for the valid protocol attribute"
        def light1toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light1Toggle"), Json.create(true)), getClass()
        )
        assetProcessingService.updateAttributeValue(light1toggleOn)

        then: "the attribute event should pass the start of the processing chain, reach the mock protocol and but not reach the end of the processing chain and update status should be HANDLED"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesReachedEndOfProcessingChain.size() == 0
            assert sendToActuatorEvents.size() == 1
            assert sendToActuatorEvents[0].entityId == mockThing.id
            assert sendToActuatorEvents[0].attributeName == "light1Toggle"
            assert sendToActuatorEvents[0].attributeState.value.asBoolean() == true
            assert updatesPassedStartOfProcessingChain[0].status == AssetUpdate.Status.HANDLED
        }

        when: "the protocol gets a response from the server"
        mockProtocol.responseReceived()

        then: "the protocol should have sent a new attribute event for the sensor change which should pass the start of the processing chain skip the protocol but still reach the end of the processing chain and update status should be CONTINUE"
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 2
            assert sendToActuatorEvents.size() == 1
            assert updatesReachedEndOfProcessingChain.size() == 1
            assert updatesReachedEndOfProcessingChain[0].getEntityName() == "Mock Thing Asset"
            assert updatesReachedEndOfProcessingChain[0].getOldState().getAttributeName() == "light1Toggle"
            assert updatesReachedEndOfProcessingChain[0].getOldState().getValue().asBoolean() == false
            assert updatesReachedEndOfProcessingChain[0].getNewState().getAttributeName() == "light1Toggle"
            assert updatesReachedEndOfProcessingChain[0].getNewState().getValue().asBoolean() == true
            assert updatesReachedEndOfProcessingChain[0].status == AssetUpdate.Status.CONTINUE
        }

        when: "an attribute event occurs for the invalid protocol attribute"
        updatesPassedStartOfProcessingChain.clear()
        updatesReachedEndOfProcessingChain.clear()
        sendToActuatorEvents.clear()
        def light2toggleOn = new AttributeEvent(
                new AttributeState(new AttributeRef(mockThing.getId(), "light2Toggle"), Json.create(true)), getClass()
        )
        assetProcessingService.updateAttributeValue(light2toggleOn)

        then: "the attribute event should pass the start of the processing chain, but not reach the mock protocol or the end of the processing chain and update status should be ERROR"
        thrown(RuntimeException)
        conditions.eventually {
            assert updatesPassedStartOfProcessingChain.size() == 1
            assert updatesReachedEndOfProcessingChain.size() == 0
            assert sendToActuatorEvents.size() == 0
            assert updatesPassedStartOfProcessingChain[0].entityId == mockThing.id
            assert updatesPassedStartOfProcessingChain[0].attribute.name == "light2Toggle"
            assert updatesPassedStartOfProcessingChain[0].status == AssetUpdate.Status.COMPLETED
        }
    }
}
