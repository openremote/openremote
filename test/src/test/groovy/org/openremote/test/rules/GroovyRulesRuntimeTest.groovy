package org.openremote.test.rules

import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.model.Constants
import org.openremote.model.rules.RealmRuleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.rules.RulesService.OR_RULES_QUICK_FIRE_MILLIS
import static org.openremote.model.rules.Ruleset.Lang.GROOVY
import static org.openremote.model.rules.RulesetStatus.DEPLOYED
import static org.openremote.model.rules.RulesetStatus.DISABLED

class GroovyRulesRuntimeTest extends Specification implements ManagerContainerTrait {

    private static final String OR_RULES_GROOVY_EXECUTION_ENABLED = "OR_RULES_GROOVY_EXECUTION_ENABLED"

    private static final String MODULO_RULE = '''
        package demo.rules

        rules.add()
                .name("Modulo operator")
                .when({
            facts ->
                !facts.matchFirst("Modulo operator").isPresent() &&
                        (10 % 4) == 2
            })
                .then({
            facts ->
                facts.put("Modulo operator", "fired")
        })
    '''.stripIndent()

    @SuppressWarnings("GroovyAccessibility")
    def "Groovy modulo operator evaluates during rule execution"() {
        given: "the container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def config = defaultConfig()
        config[OR_RULES_QUICK_FIRE_MILLIS] = "60000"
        def container = startContainer(config, defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine engine = null

        and: "a Groovy ruleset uses the modulo operator in a condition"
        def ruleset = rulesetStorageService.merge(new RealmRuleset(
                Constants.MASTER_REALM,
                "Modulo operator runtime failure",
                GROOVY,
                MODULO_RULE))

        expect: "the ruleset is compiled and deployed before it is evaluated"
        conditions.eventually {
            engine = rulesService.realmEngines.get(Constants.MASTER_REALM)
            assert engine != null
            assert engine.isRunning()
            assert engine.deployments[ruleset.id].status == DEPLOYED
        }

        when: "the ruleset is evaluated"
        engine.fireAllDeployments()

        then: "the rule fires without recording a runtime error"
        def deployment = engine.deployments[ruleset.id]
        deployment.getError() == null
        engine.facts.get("Modulo operator") == "fired"
        engine.isRunning()
    }

    @SuppressWarnings("GroovyAccessibility")
    def "Groovy rulesets are not compiled or executed when Groovy execution is disabled"() {
        given: "the container is started with Groovy ruleset execution disabled"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def config = defaultConfig()
        config[OR_RULES_GROOVY_EXECUTION_ENABLED] = "false"
        def container = startContainer(config, defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine engine = null

        and: "a Groovy ruleset which would fail if the manager attempted to compile it"
        def ruleset = rulesetStorageService.merge(new RealmRuleset(
                Constants.MASTER_REALM,
                "Disabled Groovy ruleset",
                GROOVY,
                "this is not valid Groovy"))

        expect: "the ruleset is tracked but marked disabled instead of compiled"
        conditions.eventually {
            engine = rulesService.realmEngines.get(Constants.MASTER_REALM)
            assert engine != null
            assert engine.isRunning()
            assert engine.deployments[ruleset.id].status == DISABLED
            assert engine.deployments[ruleset.id].getError() == null
        }

        when: "the rules engine is evaluated"
        engine.fireAllDeployments()

        then: "the Groovy ruleset is still not executed and the engine remains healthy"
        def deployment = engine.deployments[ruleset.id]
        deployment.status == DISABLED
        deployment.getError() == null
        engine.isRunning()
    }
}
