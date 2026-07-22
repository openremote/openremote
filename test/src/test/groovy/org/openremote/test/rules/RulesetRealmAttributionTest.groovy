package org.openremote.test.rules

import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.syslog.SyslogRealmRegistry
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.rules.Ruleset.Lang.GROOVY

class RulesetRealmAttributionTest extends Specification implements ManagerContainerTrait {

    private static final String OR_RULES_GROOVY_EXECUTION_ENABLED = "OR_RULES_GROOVY_EXECUTION_ENABLED"

    private static final String NOOP_RULE = '''
        package demo.rules

        rules.add()
                .name("noop")
                .when({ facts -> false })
                .then({ facts -> })
    '''.stripIndent()

    @SuppressWarnings("GroovyAccessibility")
    def "Realm ruleset deployment attributes its syslog logger to the realm"() {
        given: "the container is started"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def config = defaultConfig()
        config[OR_RULES_GROOVY_EXECUTION_ENABLED] = "true"
        def container = startContainer(config, defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def buildingRealm = keycloakTestSetup.realmBuilding.name
        RulesEngine engine = null

        when: "a realm ruleset is deployed to the building realm"
        def ruleset = rulesetStorageService.merge(new RealmRuleset(
                buildingRealm,
                "Realm attribution test",
                GROOVY,
                NOOP_RULE))

        then: "both the engine and the deployment syslog loggers are registered against the building realm"
        String loggerName = null
        conditions.eventually {
            engine = rulesService.realmEngines.get(buildingRealm)
            assert engine != null
            assert engine.deployments[ruleset.id] != null
            // Engine's own logger (subCategory RealmEngine-<realm>) must resolve to the realm
            assert SyslogRealmRegistry.getRealm(engine.LOG.name) == buildingRealm
            // Deployment's own logger (subCategory <RulesetClass>-<id>) must resolve to the realm
            loggerName = engine.deployments[ruleset.id].LOG.name
            assert SyslogRealmRegistry.getRealm(loggerName) == buildingRealm
        }

        when: "the ruleset is undeployed"
        rulesetStorageService.delete(RealmRuleset.class, ruleset.id)

        then: "the deployment's registry entry is removed so it does not leak"
        conditions.eventually {
            assert SyslogRealmRegistry.getRealm(loggerName) == null
        }
    }
}
