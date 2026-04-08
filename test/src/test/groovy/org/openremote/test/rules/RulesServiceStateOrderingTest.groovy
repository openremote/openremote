package org.openremote.test.rules

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

class RulesServiceStateOrderingTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "rules service should keep the newest rule state when committed events arrive out of order"() {
        given: "the container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "a rule-state asset exists"
        def asset = new ThingAsset("Ordered state asset")
            .setId(UniqueIdentifierGenerator.generateId("Ordered state asset"))
            .setRealm(Constants.MASTER_REALM)
            .addAttributes(
                new Attribute<>("count", ValueType.INTEGER, 0)
                    .addMeta(new MetaItem<>(MetaItemType.RULE_STATE))
            )
        asset = assetStorageService.merge(asset)

        expect: "the rule-state cache contains the asset attribute"
        conditions.eventually {
            def ruleState = rulesService.attributeEvents.find { it.id == asset.id && it.name == "count" }
            assert ruleState != null
            assert ruleState.timestamp > 0
        }

        when: "a realm ruleset is deployed"
        def ruleset = new RealmRuleset(
            Constants.MASTER_REALM,
            "Rule state ordering",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text
        )
        rulesetStorageService.merge(ruleset)
        RulesEngine engine = null

        then: "the rules engine starts with the cached attribute fact"
        conditions.eventually {
            engine = rulesService.realmEngines.get(Constants.MASTER_REALM)
            assert engine != null
            assert engine.isRunning()
            assert engine.facts.assetStates.find { it.id == asset.id && it.name == "count" } != null
        }

        when: "committed rule-state events for the same attribute arrive out of timestamp order"
        def storedAsset = assetStorageService.find(asset.id, true) as ThingAsset
        def storedAttribute = storedAsset.getAttribute("count").get()
        def initialRuleState = rulesService.attributeEvents.find { it.id == asset.id && it.name == "count" }
        long initialTimestamp = initialRuleState.timestamp
        long newerTimestamp = initialTimestamp + 2000
        long olderTimestamp = initialTimestamp + 1000

        def newerEvent = new AttributeEvent(
            storedAsset,
            storedAttribute,
            getClass().getSimpleName(),
            2,
            newerTimestamp,
            storedAttribute.getValue().orElse(null),
            initialTimestamp
        )
        def olderEvent = new AttributeEvent(
            storedAsset,
            storedAttribute,
            getClass().getSimpleName(),
            1,
            olderTimestamp,
            storedAttribute.getValue().orElse(null),
            initialTimestamp
        )

        rulesService.onAttributeEvent(newerEvent)
        rulesService.onAttributeEvent(olderEvent)

        then: "the cached rule state should still keep the newer timestamp and latest value"
        def latestRuleState = rulesService.attributeEvents.find { it.id == asset.id && it.name == "count" }
        assert latestRuleState != null
        assert latestRuleState.timestamp == newerTimestamp
        assert latestRuleState.value.get() == 2

        and: "the rule engine fact should also remain on the newer timestamp and latest value"
        conditions.eventually {
            def engineState = engine.facts.assetStates.find { it.id == asset.id && it.name == "count" }
            assert engineState != null
            assert engineState.timestamp == newerTimestamp
            assert engineState.value.get() == 2
        }

        cleanup:
        stopContainer()
    }

    @SuppressWarnings("GroovyAccessibility")
    def "rules service should ignore an older retract when a newer rule state is already cached"() {
        given: "the container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "a rule-state asset exists"
        def asset = new ThingAsset("Retract state asset")
            .setId(UniqueIdentifierGenerator.generateId("Retract state asset"))
            .setRealm(Constants.MASTER_REALM)
            .addAttributes(
                new Attribute<>("count", ValueType.INTEGER, 0)
                    .addMeta(new MetaItem<>(MetaItemType.RULE_STATE))
            )
        asset = assetStorageService.merge(asset)

        expect: "the initial rule state is cached"
        conditions.eventually {
            def ruleState = rulesService.attributeEvents.find { it.id == asset.id && it.name == "count" }
            assert ruleState != null
            assert ruleState.timestamp > 0
        }

        when: "a realm ruleset is deployed"
        def ruleset = new RealmRuleset(
            Constants.MASTER_REALM,
            "Rule state retract ordering",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text
        )
        rulesetStorageService.merge(ruleset)
        RulesEngine engine = null

        then: "the rules engine starts with the cached attribute fact"
        conditions.eventually {
            engine = rulesService.realmEngines.get(Constants.MASTER_REALM)
            assert engine != null
            assert engine.isRunning()
            assert engine.facts.assetStates.find { it.id == asset.id && it.name == "count" } != null
        }

        when: "a newer committed state is cached first"
        def storedAsset = assetStorageService.find(asset.id, true) as ThingAsset
        def storedAttribute = storedAsset.getAttribute("count").get()
        def initialRuleState = rulesService.attributeEvents.find { it.id == asset.id && it.name == "count" }
        long initialTimestamp = initialRuleState.timestamp
        long newerTimestamp = initialTimestamp + 2000
        long olderTimestamp = initialTimestamp + 1000

        def newerEvent = new AttributeEvent(
            storedAsset,
            storedAttribute,
            getClass().getSimpleName(),
            2,
            newerTimestamp,
            storedAttribute.getValue().orElse(null),
            initialTimestamp
        )
        rulesService.onAttributeEvent(newerEvent)

        and: "an older delete for the same rule state arrives afterwards"
        def olderRetractEvent = new AttributeEvent(
            storedAsset,
            storedAttribute,
            getClass().getSimpleName(),
            null,
            olderTimestamp,
            storedAttribute.getValue().orElse(null),
            initialTimestamp
        ).setDeleted(true)
        rulesService.onAttributeEvent(olderRetractEvent)

        then: "the newer rule state should remain cached"
        def latestRuleState = rulesService.attributeEvents.find { it.id == asset.id && it.name == "count" }
        assert latestRuleState != null
        assert latestRuleState.timestamp == newerTimestamp
        assert latestRuleState.value.get() == 2

        and: "the rule engine fact should also remain on the newer timestamp and latest value"
        conditions.eventually {
            def engineState = engine.facts.assetStates.find { it.id == asset.id && it.name == "count" }
            assert engineState != null
            assert engineState.timestamp == newerTimestamp
            assert engineState.value.get() == 2
        }

        cleanup:
        stopContainer()
    }
}
