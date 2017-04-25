package org.openremote.test.rules

import elemental.json.Json
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesDeployment
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import spock.lang.Ignore

import static java.util.concurrent.TimeUnit.*
import static org.openremote.model.asset.Asset.getAttribute
import static org.openremote.test.RulesTestUtil.attachRuleExecutionLogger

class ApartmentActionsTest extends Specification implements ManagerContainerTrait {

    def "All Lights Off"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
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

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.assetStates.size() == 10
            assert customerAEngine.assetStates.size() == 10
            assert customerAEngine.knowledgeSession.factCount == 10
        }

        and: "the room lights in an apartment to be on"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert livingRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean

        when: "the ALL LIGHTS OFF switch is pressed for an apartment"
        def apartment1AllLightsOffChange = new AttributeEvent(
                managerDemoSetup.apartment1Id, "allLightsOffSwitch", Json.create(false)
        )
        assetProcessingService.sendAttributeEvent(apartment1AllLightsOffChange)

        then: "the room lights in the apartment should be off"
        conditions.eventually {
            assert customerAEngine.knowledgeSession.factCount == 11
            livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !livingRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean
        }

        when: "time advanced"
        withClockOf(customerAEngine) { it.advanceTime(15, SECONDS) }

        then: "event expired"
        conditions.eventually {
            assert customerAEngine.knowledgeSession.factCount == 10
        }

        cleanup: "stop the container"
        disablePseudoClock()
        stopContainer(container)
    }

    def "Presence Detection"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesDeployment customerAEngine = null

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.knowledgeSession.factCount == 10
        }

        and: "the presence latch of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").get().valueAsBoolean

        and: "the presence latch of the other room should not be set"
        def otherLivingRoomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
        assert !otherLivingRoomAsset.getAttribute("presenceDetected").get().valueAsBoolean

        and: "several presence sensor events are triggered in the room"
        for (i in 0..2) {
            def livingroomPresenceChange = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "presenceSensor", Json.create(true)
            )
            assetProcessingService.sendAttributeEvent(livingroomPresenceChange)
            conditions.eventually {
                // Wait until we have the facts
                assert customerAEngine.knowledgeSession.factCount == 11 + i
            }
            withClockOf(customerAEngine) { it.advanceTime(2, MINUTES) }
        }

        and: "several presence sensor events are triggered in the other room (but not enough to trigger 'detection')"
        for (i in 0..2) {
            def livingroomPresenceChange = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "presenceSensor", Json.create(true)
            )
            assetProcessingService.sendAttributeEvent(livingroomPresenceChange)
            conditions.eventually {
                // Wait until we have the facts
                assert customerAEngine.knowledgeSession.factCount == 14 + i
            }
            withClockOf(customerAEngine) { it.advanceTime(10, MINUTES) }
        }

        and: "the presence latch of the room should be set"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean
        }

        and: "the presence latch of the other room should NOT be set"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().valueAsBoolean
        }

        when: "time is advanced enough to trigger event expiration"
        withClockOf(customerAEngine) { it.advanceTime(1, HOURS) }

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert customerAEngine.knowledgeSession.factCount == 10
        }

        cleanup: "the server should be stopped"
        disablePseudoClock()
        stopContainer(container)
    }

    def "Vacation Mode"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 60, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
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

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.assetStates.size() >= 10
            assert customerAEngine.assetStates.size() >= 10
            assert customerAEngine.knowledgeSession.factCount >= 10
        }

        when: "the vacation days are set to 5"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1Id, "vacationDays", Json.create(5)
        ))

        then: "that value should be stored"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationDays").get().valueAsInteger == 5
            assert asset.getAttribute("lastScene").get().valueAsString == "AWAY"
        }

        when: "time advanced to the next day, which should trigger the cron rule"
        withClockOf(customerAEngine) { it.advanceTime(24, HOURS) }

        then: "the vacation days should be decremented"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationDays").get().valueAsInteger == 4
            assert asset.getAttribute("lastScene").get().valueAsString == "AWAY"
        }

        when: "time advanced again (to test that the rule only fires once per day)"
        withClockOf(customerAEngine) { it.advanceTime(10, SECONDS) }

        then: "the vacation days should NOT be decremented"
        new PollingConditions(initialDelay: 2).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationDays").get().valueAsInteger == 4
            assert asset.getAttribute("lastScene").get().valueAsString == "AWAY"
        }

        expect: "the remaining vacation days to be decremented with each passing day"
        int remainingDays = 4
        while (remainingDays > 0) {

            remainingDays--

            withClockOf(customerAEngine) { it.advanceTime(1, DAYS) }
            conditions.eventually {
                def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
                assert asset.getAttribute("vacationDays").get().valueAsInteger == remainingDays
                assert asset.getAttribute("lastScene").get().valueAsString == "AWAY"
            }
        }
        withClockOf(customerAEngine) { it.advanceTime(8, HOURS) }
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("lastScene").get().valueAsString == "HOME"
        }

        cleanup: "the server should be stopped"
        disablePseudoClock()
        stopContainer(container)
    }

    // The same behaviour is with the realtime and pseudo clock.
    // TODO see below for explanation.
    @Ignore
    def "Test Custom Fact Clock Real"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 60, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        RulesDeployment customerBEngine
        List<String> customerBEngineFiredRules = []

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert customerBEngine != null
            assert customerBEngine.isRunning()
        }

        when: "check if the custom clock is running"
        attachRuleExecutionLogger(customerBEngine, customerBEngineFiredRules)
        Thread.sleep(16000)

        then: "Check how many rules were triggered"
        conditions.eventually {
            assert customerBEngineFiredRules.size() >= 8 // The >= to be good even when we have slower machine
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    // This test shows a fundamental problem with current rule's engine. Custom facts inserted into the knowledge
    // session are not detected on the current delta and dissapear. Only when a rule has the delay in form of timer(int: x)
    // then they are triggered. This means that one must controll timing and racing by hand instead of Drools.
    // TODO fix this otherwise impossible to work on rules
    @Ignore
    def "Test Custom Fact Clock Pseudo"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 60, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesDeployment customerBEngine
        List<String> customerBEngineFiredRules = []

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert customerBEngine != null
            assert customerBEngine.isRunning()
        }

        when: "check if the custom clock is running"
        attachRuleExecutionLogger(customerBEngine, customerBEngineFiredRules)
        withClockOf(customerBEngine) { it.advanceTime(15, SECONDS) }
        Thread.sleep(16000)

        then: "Check how many rules were triggered"
        conditions.eventually {
            assert customerBEngineFiredRules.size() == 8
        }

        cleanup: "the server should be stopped"
        disablePseudoClock()
        stopContainer(container)
    }

}
