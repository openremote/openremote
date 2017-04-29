package org.openremote.test.rules

import elemental.json.Json
import elemental.json.JsonType
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.model.asset.AssetState
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.MINUTES
import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_CUSTOMER_A

class ApartmentPresenceDetectionTest extends Specification implements ManagerContainerTrait {

    def "Set and clear presence detection timestamp depending on motion sensor"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 5, delay: 0.2)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "a mock attribute event consumer"
        List<AssetState> updatesCompletedProcessing = []
        Consumer<AssetState> assetProcessingCompleteConsumer = { assetUpdate ->
            updatesCompletedProcessing.add(assetUpdate)
        }
        assetProcessingService.processors.add(assetProcessingCompleteConsumer)


        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the presence detected flag and timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").get().valueAsBoolean
        assert livingRoomAsset.getAttribute("lastPresenceDetected").get().value.getType() == JsonType.NULL

        when: "several motion sensor events are triggered in the room, but not enough for presence detection"
        updatesCompletedProcessing = []
        setPseudoClocksToRealTime(container, apartment1Engine)
        // Send 5 triggers each 3 minutes apart
        for (i in 1..5) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionSensor", Json.create(true), getClockTimeOf(apartment1Engine)
            )
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
            conditions.eventually {
                assert updatesCompletedProcessing.findAll { it.attributeName == "motionSensor" }.size() == i
            }
            advancePseudoClocks(3, MINUTES, container, apartment1Engine)
        }

        then: "presence should not be detected"
        new PollingConditions(initialDelay: 5).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.getType() == JsonType.NULL
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment1Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        when: "several motion sensor events are triggered in the room, fast enough for presence detection"
        updatesCompletedProcessing = []
        double expectedLastPresenceTimestamp = 0
        // Send 5 triggers each 1 minute apart
        for (i in 1..5) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionSensor", Json.create(true), getClockTimeOf(apartment1Engine)
            )
            expectedLastPresenceTimestamp = motionSensorTrigger.timestamp
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
            conditions.eventually {
                assert updatesCompletedProcessing.findAll { it.attributeName == "motionSensor" }.size() == i
            }
            advancePseudoClocks(1, MINUTES, container, apartment1Engine)
        }

        then: "presence should be detected"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.asNumber() == expectedLastPresenceTimestamp
        }

        when: "time moves on and we keep triggering the motion sensor in short intervals "
        updatesCompletedProcessing = []
        // Send 20 triggers each 90 seconds apart
        for (i in 1..20) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionSensor", Json.create(true), getClockTimeOf(apartment1Engine)
            )
            expectedLastPresenceTimestamp = motionSensorTrigger.timestamp
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
            conditions.eventually {
                assert updatesCompletedProcessing.findAll { it.attributeName == "motionSensor" }.size() == i
            }
            advancePseudoClocks(10, SECONDS, container, apartment1Engine)
        }

        then: "presence should still be detected and the timestamp updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.asNumber() == expectedLastPresenceTimestamp
        }

        when: "time moves on without the motion sensor being triggered"
        advancePseudoClocks(20, MINUTES, container, apartment1Engine)

        then: "presence should be gone but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.asNumber() == expectedLastPresenceTimestamp
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment1Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    // TODO This needs more work
    @Ignore
    def "Set and clear presence detection timestamp depending on motion counter"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 5, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine customerAEngine = null

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A
        }

        and: "the presence detected timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert livingRoomAsset.getAttribute("presenceDetected").get().value.getType() == JsonType.NULL

        and: "the presence detected timestamp of the other room should not be set"
        def otherLivingRoomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
        assert otherLivingRoomAsset.getAttribute("presenceDetected").get().value.getType() == JsonType.NULL

        when: "the motion counter sensor of the room is incremented"
        double expectedPresenceTimestampStart = getClockTimeOf(customerAEngine)
        for (i in 0..2) {
            def motionCounterIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCounter", Json.create(i + 1)
            )
            assetProcessingService.sendAttributeEvent(motionCounterIncrement)
            conditions.eventually {
                // Wait until we have the facts
                assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A + 1 + i
            }
            withClockOf(customerAEngine) { it.advanceTime(2, MINUTES) }
        }
        double expectedPresenceTimestampEnd = getClockTimeOf(customerAEngine)

        then: "the presence detected timestamp of the room should be set"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            def presenceDetectedTimestamp = asset.getAttribute("presenceDetected").get().value.asNumber()
            assert presenceDetectedTimestamp > expectedPresenceTimestampStart && presenceDetectedTimestamp < expectedPresenceTimestampEnd
        }

        when: "the motion counter sensor in the other room is incremented (but not enough to trigger 'detection')"
        for (i in 0..2) {
            def motionCounterIncrement = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "motionCounter", Json.create(i + 1)
            )
            assetProcessingService.sendAttributeEvent(motionCounterIncrement)
            conditions.eventually {
                // Wait until we have the facts
                assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A + 4 + i
            }
            withClockOf(customerAEngine) { it.advanceTime(10, MINUTES) }
        }

        then: "the presence detected timestamp of the other room should NOT be set"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().value.getType() == JsonType.NULL
        }

        and: "meanwhile presence detected timestamp of the first room should have been cleared"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().value.getType() == JsonType.NULL
        }

        when: "time is advanced enough to trigger event expiration"
        withClockOf(customerAEngine) { it.advanceTime(1, HOURS) }

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}
