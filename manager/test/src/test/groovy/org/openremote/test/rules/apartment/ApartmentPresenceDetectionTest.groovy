package org.openremote.test.rules.apartment

import elemental.json.Json
import elemental.json.JsonType
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.model.asset.AssetEvent
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.*
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_2

class ApartmentPresenceDetectionTest extends Specification implements ManagerContainerTrait {

    def "Detect presence with motion counter"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine apartment1Engine = null

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

        when: "we have an events consumer for collecting test data"
        List<AssetEvent> insertedAssetEvents = []
        apartment1Engine.assetEventsConsumer = { assetEvent->
            insertedAssetEvents << assetEvent
        }

        and: "several motion counter increments occur in the room, but not enough for presence detection"
        insertedAssetEvents = []
        setPseudoClocksToRealTime(container, apartment1Engine)
        // Send 5 triggers each 3 minutes apart
        for (i in 1..5) {
            def motionCounterIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCounter", Json.create(i), getClockTimeOf(apartment1Engine)
            )
            assetProcessingService.sendAttributeEvent(motionCounterIncrement)
            new PollingConditions(timeout: 3, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionCounterIncrement) }
            }
            advancePseudoClocks(3, MINUTES, container, apartment1Engine)
        }

        then: "presence should not be detected"
        new PollingConditions(initialDelay: 3).eventually {
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

        when: "several motion counter increments occur in the room, fast enough for presence detection"
        insertedAssetEvents = []
        double expectedLastPresenceTimestamp = 0
        // Send 5 increments each 1 minute apart
        for (i in 1..5) {
            def motionCounterIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCounter", Json.create(i), getClockTimeOf(apartment1Engine)
            )
            expectedLastPresenceTimestamp = motionCounterIncrement.timestamp
            assetProcessingService.sendAttributeEvent(motionCounterIncrement)
            new PollingConditions(timeout: 3, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionCounterIncrement) }
            }
            advancePseudoClocks(1, MINUTES, container, apartment1Engine)
        }

        then: "presence should be detected"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.asNumber() == expectedLastPresenceTimestamp
        }

        when: "time moves on and we keep incrementing the motion counter in short intervals "
        insertedAssetEvents = []
        // Send 20 increments each 90 seconds apart
        for (i in 1..20) {
            def motionCounterIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCounter", Json.create(i), getClockTimeOf(apartment1Engine)
            )
            expectedLastPresenceTimestamp = motionCounterIncrement.timestamp
            assetProcessingService.sendAttributeEvent(motionCounterIncrement)
            new PollingConditions(timeout: 3, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionCounterIncrement) }
            }
            advancePseudoClocks(90, SECONDS, container, apartment1Engine)
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

    def "Detect presence with motion sensor"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine apartment2Engine = null

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment2Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.isRunning()
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
        }

        and: "the presence detected flag and timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").get().valueAsBoolean
        assert livingRoomAsset.getAttribute("lastPresenceDetected").get().value.getType() == JsonType.NULL

        when: "we have an events consumer for collecting test data"
        List<AssetEvent> insertedAssetEvents = []
        apartment2Engine.assetEventsConsumer = { assetEvent->
            insertedAssetEvents << assetEvent
        }

        and: "several motion sensor events are triggered in the room, but not enough for presence detection"
        insertedAssetEvents = []
        setPseudoClocksToRealTime(container, apartment2Engine)
        // Send 5 triggers each 3 minutes apart
        for (i in 1..5) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "motionSensor", Json.create(true), getClockTimeOf(apartment2Engine)
            )
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
            new PollingConditions(timeout: 3, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionSensorTrigger) }
            }
            advancePseudoClocks(3, MINUTES, container, apartment2Engine)
        }

        then: "presence should not be detected"
        new PollingConditions(initialDelay: 3).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.getType() == JsonType.NULL
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment2Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
        }

        when: "several motion sensor events are triggered in the room, fast enough for presence detection"
        insertedAssetEvents = []
        double expectedLastPresenceTimestamp = 0
        // Send 5 triggers each 1 minute apart
        for (i in 1..5) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "motionSensor", Json.create(true), getClockTimeOf(apartment2Engine)
            )
            expectedLastPresenceTimestamp = motionSensorTrigger.timestamp
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
            new PollingConditions(timeout: 3, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionSensorTrigger) }
            }
            advancePseudoClocks(1, MINUTES, container, apartment2Engine)
        }

        then: "presence should be detected"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.asNumber() == expectedLastPresenceTimestamp
        }

        when: "time moves on and we keep triggering the motion sensor in short intervals "
        insertedAssetEvents = []
        // Send 20 triggers each 90 seconds apart
        for (i in 1..20) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "motionSensor", Json.create(true), getClockTimeOf(apartment2Engine)
            )
            expectedLastPresenceTimestamp = motionSensorTrigger.timestamp
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
            new PollingConditions(timeout: 3, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionSensorTrigger) }
            }
            advancePseudoClocks(90, SECONDS, container, apartment2Engine)
        }

        then: "presence should still be detected and the timestamp updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.asNumber() == expectedLastPresenceTimestamp
        }

        when: "time moves on without the motion sensor being triggered"
        advancePseudoClocks(20, MINUTES, container, apartment2Engine)

        then: "presence should be gone but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().valueAsBoolean
            assert asset.getAttribute("lastPresenceDetected").get().value.asNumber() == expectedLastPresenceTimestamp
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment2Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}
