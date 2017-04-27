package org.openremote.test.rules

import elemental.json.Json
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
        RulesEngine customerAEngine

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
        }

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A
        }

        and: "the room lights in an apartment to be on"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert livingRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean

        when: "the ALL LIGHTS OFF push-button is pressed for an apartment"
        def apartment1AllLightsOffChange = new AttributeEvent(
                managerDemoSetup.apartment1Id, "allLightsOffSwitch", Json.create(true)
        )
        assetProcessingService.sendAttributeEvent(apartment1AllLightsOffChange)

        then: "the room lights in the apartment should be off"
        conditions.eventually {
            assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A + 1
            livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !livingRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean
        }

        when: "time advanced"
        withClockOf(customerAEngine) { it.advanceTime(15, SECONDS) }

        then: "event expired"
        conditions.eventually {
            assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A
        }

        cleanup: "stop the container"
        disablePseudoClock()
        stopContainer(container)
    }

    def "Vacation Mode"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 15, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine customerAEngine

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
        }

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert customerAEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_A
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
}
