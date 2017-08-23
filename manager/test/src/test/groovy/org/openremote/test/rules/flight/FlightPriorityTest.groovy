package org.openremote.test.rules.flight

import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TenantRuleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_CUSTOMER_C

class FlightPriorityTest extends Specification implements ManagerContainerTrait {

    def "Mark flights as priority based on rules template"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine customerCEngine

        and: "some rules template and template asset are deployed"
        Ruleset ruleset = new TenantRuleset(
                "Demo Flights - Mark flights as priority",
                keycloakDemoSetup.customerCTenant.id,
                getClass().getResource("/demo/rules/flight/DemoFlightPriorityFilter.drlt").text,
                managerDemoSetup.flightPriorityFiltersId
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerCEngine = rulesService.tenantEngines.get(keycloakDemoSetup.customerCTenant.id)
            assert customerCEngine != null
            assert customerCEngine.isRunning()
            assert customerCEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_C
            assert customerCEngine.knowledgeSession.factCount == DEMO_RULE_STATES_CUSTOMER_C
        }

        and: "flight(s) with origin Switzerland and high capacity should now be priority"
        conditions.eventually {
            def flight1 = assetStorageService.find(managerDemoSetup.flight1Id, true)
            assert !flight1.getAttribute("priority").orElse(null).getValueAsBoolean().orElse(null)

            def flight4 = assetStorageService.find(managerDemoSetup.flight4Id, true)
            assert flight4.getAttribute("priority").orElse(null).getValueAsBoolean().orElse(null)
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
