package org.openremote.test.rules

import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.Constants
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class RulesDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check basic rules engine deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

        when: "the demo assets and rule definitions have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)

        then: "a global rules engine should have been created and be running"
        conditions.eventually {
            assert rulesService.globalDeployment != null
            assert rulesService.globalDeployment.isRunning()
        }

        and: "the global rules engine should contain the global demo rules definition"
        conditions.eventually {
            assert rulesService.globalDeployment.allRulesDefinitions.length == 1
            assert rulesService.globalDeployment.allRulesDefinitions[0].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[0].name == "Some global demo rules"
        }

        and: "one tenant rules engines should have been created and be running"
        conditions.eventually {
            assert rulesService.tenantDeployments.size() == 1
            def masterEngine = rulesService.tenantDeployments.get(Constants.MASTER_REALM)
            assert masterEngine != null
            assert masterEngine.isRunning()
        }

        and: "the tenant rules engine should have the demo tenant rules definition"
        conditions.eventually {
            def masterEngine = rulesService.tenantDeployments.get(Constants.MASTER_REALM)
            assert masterEngine.allRulesDefinitions.length == 1
            assert masterEngine.allRulesDefinitions[0].enabled
            assert masterEngine.allRulesDefinitions[0].name == "Some master tenant demo rules"
        }

        and: "two asset rules engines should have been created and be running"
        conditions.eventually {
            assert rulesService.assetDeployments.size() == 2
            def apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment3Engine != null
            assert apartment3Engine.isRunning()
        }

        and: "each asset rules engine should have the demo asset rules definition"
        conditions.eventually {
            def apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment1Engine.allRulesDefinitions.length == 1
            assert apartment1Engine.allRulesDefinitions[0].enabled
            assert apartment1Engine.allRulesDefinitions[0].name == "Some apartment 1 demo rules"
            assert apartment3Engine.allRulesDefinitions.length == 1
            assert apartment3Engine.allRulesDefinitions[0].enabled
            assert apartment3Engine.allRulesDefinitions[0].name == "Some apartment 3 demo rules"
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
