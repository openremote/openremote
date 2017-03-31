package org.openremote.test.rules

import elemental.json.Json
import org.drools.core.time.impl.PseudoClockScheduler
import org.kie.api.runtime.conf.ClockTypeOption
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesDeployment
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.server.setup.builtin.RulesDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.model.asset.AssetAttributes
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.*

class ApartmentActionsTest extends Specification implements ManagerContainerTrait {

    def "All Lights Off"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 5, delay: 1)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesDemoSetup = container.getService(SetupService.class).getTaskOfType(RulesDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesDeployment customerAEngine

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
        }

        and: "the demo attributes marked with RULES_FACT = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.facts.size() == 8
            assert customerAEngine.facts.size() == 8
        }

        and: "the room lights in an apartment to be on"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert new AssetAttributes(livingRoomAsset.getAttributes()).get("lightSwitch").valueAsBoolean

        when: "the ALL LIGHTS OFF switch is set to off for an apartment"
        def apartment1AllLightsOffChange = new AttributeEvent(
                managerDemoSetup.apartment1Id, "allLightsOffSwitch", Json.create(false)
        )
        assetProcessingService.updateAttributeValue(apartment1AllLightsOffChange)

        then: "the room lights in the apartment should be off"
        conditions.eventually {
            livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !new AssetAttributes(livingRoomAsset.getAttributes()).get("lightSwitch").valueAsBoolean
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Presence Detection"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 5, delay: 1)

        and: "a pseudo rules engine clock"
        RulesDeployment.DefaultClockType = ClockTypeOption.get("pseudo")
        PseudoClockScheduler sessionClock

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesDemoSetup = container.getService(SetupService.class).getTaskOfType(RulesDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesDeployment customerAEngine = null

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.knowledgeSession.factCount == 8
        }

        and: "the presence latch of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !new AssetAttributes(livingRoomAsset.getAttributes()).get("presenceDetected").valueAsBoolean

        and: "the presence latch of the other room should not be set"
        def otherLivingRoomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
        assert !new AssetAttributes(otherLivingRoomAsset.getAttributes()).get("presenceDetected").valueAsBoolean

        when: "we have prepared a rules clock"
        sessionClock = customerAEngine.sessionClock as PseudoClockScheduler
        // Set the session clock to wall time, as event expiration offsets are based on wall time
        sessionClock.advanceTime(System.currentTimeMillis(), MILLISECONDS)

        and: "several presence sensor events are triggered in the room"
        for (i in 0..2) {
            def livingroomPresenceChange = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "presenceSensor", Json.create(true)
            )
            assetProcessingService.updateAttributeValue(livingroomPresenceChange)
            sessionClock.advanceTime(2, MINUTES)
        }

        and: "several presence sensor events are triggered in the other room (but not enough to trigger 'detection')"
        for (i in 0..2) {
            def livingroomPresenceChange = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "presenceSensor", Json.create(true)
            )
            assetProcessingService.updateAttributeValue(livingroomPresenceChange)
            sessionClock.advanceTime(10, MINUTES)
        }

        then: "the presence latch of the room should be set"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert new AssetAttributes(asset.getAttributes()).get("presenceDetected").valueAsBoolean
        }

        and: "the presence latch of the other room should NOT be set"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert !new AssetAttributes(asset.getAttributes()).get("presenceDetected").valueAsBoolean
        }

        when: "time is advanced enough to trigger event expiration"
        sessionClock.advanceTime(1, HOURS)
        customerAEngine.knowledgeSession.fireAllRules()

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert customerAEngine.knowledgeSession.factCount == 8
        }

        cleanup: "the server should be stopped"
        RulesDeployment.DefaultClockType = null
        stopContainer(container)
    }
}
