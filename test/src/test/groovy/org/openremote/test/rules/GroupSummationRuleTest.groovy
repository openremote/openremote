package org.openremote.test.rules

import org.openremote.container.util.UniqueIdentifierGenerator
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
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class GroupSummationRuleTest extends Specification implements ManagerContainerTrait {

    def "Group summation rule test"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 5, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine engine = null

        and: "a parent asset and child assets are added"
        def parentAsset = new ThingAsset("SummationParentAsset")
                .setId(UniqueIdentifierGenerator.generateId("SummationParentAsset"))
                .setRealm(Constants.MASTER_REALM)
                .addAttributes(
                        new Attribute<>("boolean", ValueType.BOOLEAN, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE)),
                        new Attribute<>("integer", ValueType.INTEGER, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE)),
                        new Attribute<>("number", ValueType.NUMBER, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE))
                )
        parentAsset = assetStorageService.merge(parentAsset)

        def childAsset1 = new ThingAsset("childAsset1")
                .setId(UniqueIdentifierGenerator.generateId("childAsset1"))
                .setParent(parentAsset)
                .addAttributes(
                        new Attribute<>("boolean", ValueType.BOOLEAN, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE)),
                        new Attribute<>("integer", ValueType.INTEGER, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE)),
                        new Attribute<>("number", ValueType.NUMBER, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE))
                )
        childAsset1 = assetStorageService.merge(childAsset1)

        def childAsset2 = new ThingAsset("childAsset2")
                .setId(UniqueIdentifierGenerator.generateId("childAsset2"))
                .setParent(parentAsset)
                .addAttributes(
                        new Attribute<>("boolean", ValueType.BOOLEAN, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE)),
                        new Attribute<>("integer", ValueType.INTEGER, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE)),
                        new Attribute<>("number", ValueType.NUMBER, null).addMeta(new MetaItem<>(MetaItemType.RULE_STATE))
                )
        childAsset2 = assetStorageService.merge(childAsset2)

        and: "the group summation ruleset is added"
        Ruleset ruleset = new RealmRuleset(
                Constants.MASTER_REALM,
                "Group summation rule",
                Ruleset.Lang.GROOVY,
                getClass().getResource("/org/openremote/test/rules/GroupSummationRule.groovy").text)
        ruleset = rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            engine = rulesService.realmEngines.get(Constants.MASTER_REALM)
            assert engine != null
            assert engine.isRunning()
            assert engine.assetStates.count { it.id == parentAsset.id } == 3
            assert engine.lastFireTimestamp > ruleset.createdOn.getTime()
            assert engine.deployments.get(ruleset.id) != null
        }

        when: "the integer attribute of the child assets is written to"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(childAsset1.id, "integer", 1))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(childAsset2.id, "integer", 1))

        then: "the corresponding attribute of the parent asset should be updated with the attribute sum of the child assets"
        conditions.eventually {
            parentAsset = assetStorageService.find(parentAsset.id, true) as ThingAsset
            assert parentAsset.getAttribute("integer").flatMap { it.value }.orElse(0) == 2
        }

        when: "the number attribute of the child assets is written to"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(childAsset1.id, "number", 1.0))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(childAsset2.id, "number", 1.0))

        then: "the corresponding attribute of the parent asset should be updated with the attribute sum of the child assets"
        conditions.eventually {
            parentAsset = assetStorageService.find(parentAsset.id, true) as ThingAsset
            assert parentAsset.getAttribute("number").flatMap { it.value }.orElse(0.0) == 2.0
        }

        when: "the boolean attribute of the child assets is written to"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(childAsset1.id, "boolean", true))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(childAsset2.id, "boolean", true))

        then: "the corresponding attribute of the parent asset should not be updated"
        conditions.eventually {
            parentAsset = assetStorageService.find(parentAsset.id, true) as ThingAsset
            assert !parentAsset.getAttribute("boolean").flatMap { it.value }.orElse(false)
        }
    }
}
