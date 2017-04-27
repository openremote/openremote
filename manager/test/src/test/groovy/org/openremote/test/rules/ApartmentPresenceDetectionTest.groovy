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
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.*
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_CUSTOMER_A

class ApartmentPresenceDetectionTest extends Specification implements ManagerContainerTrait {

    def "Set and clear presence detection timestamp depending on motion sensor"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 5, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
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

        when: "several presence sensor events are triggered in the room"
        double expectedPresenceTimestampStart = getClockTimeOf(customerAEngine)
        for (i in 0..2) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionSensor", Json.create(true)
            )
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
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

        when: "several presence sensor events are triggered in the other room (but not enough to trigger 'detection')"
        for (i in 0..2) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "motionSensor", Json.create(true)
            )
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
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
        disablePseudoClock()
        stopContainer(container)
    }

    def "Set and clear presence detection timestamp depending on motion counter"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 5, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
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
                    managerDemoSetup.apartment1LivingroomId, "motionCounter", Json.create(i+1)
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
                    managerDemoSetup.apartment2LivingroomId, "motionCounter", Json.create(i+1)
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
        disablePseudoClock()
        stopContainer(container)
    }

}
