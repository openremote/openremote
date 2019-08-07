package org.openremote.test.failure


import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesFacts
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.RulesetStatus
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class RulesExecutionFailureTest extends Specification implements ManagerContainerTrait {

    def "Rule condition invalid return"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment2Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Failure Ruleset", Ruleset.Lang.GROOVY, getClass().getResource("/org/openremote/test/failure/RulesFailureConditionInvalidReturn.groovy").text,
                managerDemoSetup.apartment2Id,
                false,
                false
        )
        ruleset = rulesetStorageService.merge(ruleset)

        expect: "the rule engine should have an error (first firing after initial asset state insert)"
        conditions.eventually {
            apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.deployments[ruleset.id].status == RulesetStatus.EXECUTION_ERROR
            assert apartment2Engine.deployments[ruleset.id].error instanceof IllegalArgumentException
            assert apartment2Engine.deployments[ruleset.id].error.message == "Error evaluating condition of rule 'The when condition is illegal, it's returning an Optional instead of a boolean': result is not boolean but Optional.empty"
            assert apartment2Engine.isError()
            assert apartment2Engine.getError() instanceof RuntimeException
            assert apartment2Engine.getError().message.startsWith("Ruleset deployments have errors, failed compilation: 0, failed execution: 1")
        }

        cleanup: "stop the container"
        stopContainer(container)
    }

    def "Rule condition throws exception"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment2Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Failure Ruleset", Ruleset.Lang.GROOVY, getClass().getResource("/org/openremote/test/failure/RulesFailureConditionThrowsException.groovy").text,
                managerDemoSetup.apartment2Id,
                false,
                false
        )
        ruleset = rulesetStorageService.merge(ruleset)

        expect: "the rule engine should have an error (first firing after initial asset state insert)"
        conditions.eventually {
            apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.deployments[ruleset.id].status == RulesetStatus.EXECUTION_ERROR
            assert apartment2Engine.deployments[ruleset.id].error instanceof RuntimeException
            assert apartment2Engine.deployments[ruleset.id].error.message == "Error evaluating condition of rule 'Condition always throws exception': Oops"
            assert apartment2Engine.isError()
            assert apartment2Engine.getError() instanceof RuntimeException
            assert apartment2Engine.getError().message.startsWith("Ruleset deployments have errors, failed compilation: 0, failed execution: 1")
        }

        cleanup: "stop the container"
        stopContainer(container)
    }

    def "Rule action throws exception"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment2Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Failure Ruleset", Ruleset.Lang.GROOVY, getClass().getResource("/org/openremote/test/failure/RulesFailureActionThrowsException.groovy").text,
                managerDemoSetup.apartment2Id,
                false,
                false
        )
        ruleset = rulesetStorageService.merge(ruleset)

        expect: "the rule engine should have an error (first firing after initial asset state insert)"
        conditions.eventually {
            apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.deployments[ruleset.id].status == RulesetStatus.EXECUTION_ERROR
            assert apartment2Engine.deployments[ruleset.id].error instanceof RuntimeException
            assert apartment2Engine.deployments[ruleset.id].error.message == "Error executing action of rule 'Action always throws exception': Oops"
            assert apartment2Engine.isError()
            assert apartment2Engine.getError() instanceof RuntimeException
            assert apartment2Engine.getError().message.startsWith("Ruleset deployments have errors, failed compilation: 0, failed execution: 1")
        }

        cleanup: "stop the container"
        stopContainer(container)
    }

    def "Rule condition loops"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment2Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Failure Ruleset", Ruleset.Lang.GROOVY, getClass().getResource("/org/openremote/test/failure/RulesFailureLoop.groovy").text,
                managerDemoSetup.apartment2Id,
                false,
                false
        )
        ruleset = rulesetStorageService.merge(ruleset)

        expect: "the rule engine should have an error (first firing after initial asset state insert)"
        conditions.eventually {
            apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.deployments[ruleset.id].status == RulesetStatus.EXECUTION_ERROR
            assert apartment2Engine.deployments[ruleset.id].error instanceof IllegalStateException
            assert apartment2Engine.deployments[ruleset.id].error.message == "Possible rules loop detected, exceeded max trigger count of " + RulesFacts.MAX_RULES_TRIGGERED_PER_EXECUTION +  " for rule: Condition loops"
            assert apartment2Engine.isError()
            assert apartment2Engine.getError() instanceof RuntimeException
            assert apartment2Engine.getError().message.startsWith("Ruleset deployments have errors, failed compilation: 0, failed execution: 1")
            assert apartment2Engine.facts.triggerCount == 0 // Ensure trigger count is reset after execution
        }

        cleanup: "stop the container"
        stopContainer(container)
    }
}
