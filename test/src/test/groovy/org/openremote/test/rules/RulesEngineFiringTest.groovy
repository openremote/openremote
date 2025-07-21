package org.openremote.test.rules

import jakarta.persistence.EntityManager
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.atomic.AtomicBoolean

import static org.openremote.model.rules.Ruleset.Lang.GROOVY

class RulesEngineFiringTest extends Specification implements ManagerContainerTrait {

    /*
     * See https://github.com/openremote/openremote/issues/1953
     * When rules evaluation takes longer than the firing period AND an attribute event occurs during that time,
     * the rules engine would fire even if rules where still being evaluated.
     */
    @SuppressWarnings("GroovyAccessibility")
    def "Check rules engine are not running in parallel"() {
        given: "the container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine engine = null

        and: "some rulesets with a long running rule"
        Ruleset ruleset = new RealmRuleset(
                Constants.MASTER_REALM,
                "Parallel Engine check rules",
                GROOVY,
                getClass().getResource("/org/openremote/test/rules/ParallelEngineCheck.groovy").text)
        Long masterRulesetId = rulesetStorageService.merge(ruleset).id

        and: "assets are added"
        def counterAsset = new ThingAsset("CounterAsset")
                .setId(UniqueIdentifierGenerator.generateId("CounterAsset"))
                .setRealm(Constants.MASTER_REALM)
                .addAttributes(
                        new Attribute<>("count", ValueType.INTEGER, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE)),
                )
        counterAsset = assetStorageService.merge(counterAsset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            engine = rulesService.realmEngines.get(Constants.MASTER_REALM)
            assert engine != null
            assert engine.isRunning()
        }

        when: "a mock rules engine is created"
        // We want to stop rules evaluation while doing this setup, otherwise our count of attribute event change is biased
        rulesService.stop(container)
        RulesEngine mockRulesEngine = Spy(engine)
        AtomicBoolean rulesExecuting = new AtomicBoolean(false)
        AtomicBoolean parallelExecutionOccurred = new AtomicBoolean(false)
        mockRulesEngine.fireAllDeployments() >> {
            if (rulesExecuting.getAndSet(true)) {
                parallelExecutionOccurred.set(true)
            }
            callRealMethod()
            rulesExecuting.set(false)
        }
        rulesService.realmEngines.put(Constants.MASTER_REALM, mockRulesEngine)
        rulesService.start(container)

        and: "An attribute has changed value"
        def receivedEvents = new ArrayList<AttributeEvent>()
        assetProcessingService.addEventInterceptor { EntityManager em, AttributeEvent event ->
            receivedEvents.add(event)
            false
        }

        then: "A new attribute event is sent"
        conditions.eventually {
            assert receivedEvents.size() == 4
        }
        assert !parallelExecutionOccurred.get(): "Rules engine triggered while rules are still executing"
    }
}
