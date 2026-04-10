/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.rules

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class RulesServiceStateOrderingTest extends Specification implements ManagerContainerTrait {

    def "pre-init buffer should keep the newest rule state event for the same attribute"() {
        given:
        def rulesService = new RulesService()
        def ref = new AttributeRef(UniqueIdentifierGenerator.generateId("Buffered state asset"), "count")
        def ruleStateMeta = new MetaMap([new MetaItem<>(MetaItemType.RULE_STATE)])

        and:
        def olderEvent = new AttributeEvent(ref, 1, 1000L)
            .setMeta(ruleStateMeta)
            .setRealm(Constants.MASTER_REALM)
        def newerEvent = new AttributeEvent(ref, 2, 2000L)
            .setMeta(ruleStateMeta)
            .setRealm(Constants.MASTER_REALM)

        when:
        rulesService.onAttributeEvent(olderEvent)
        rulesService.onAttributeEvent(newerEvent)

        then:
        rulesService.preInitAttributeEvents.size() == 1
        def bufferedEvent = rulesService.preInitAttributeEvents.get(ref)
        bufferedEvent != null
        bufferedEvent.timestamp == 2000L
        bufferedEvent.value.get() == 2
    }

    def "pre-init buffer should keep a later retract even when its value timestamp is older"() {
        given:
        def rulesService = new RulesService()
        def ref = new AttributeRef(UniqueIdentifierGenerator.generateId("Buffered retract asset"), "count")
        def ruleStateMeta = new MetaMap([new MetaItem<>(MetaItemType.RULE_STATE)])

        and:
        def newerInsert = new AttributeEvent(ref, 2, 2000L)
            .setMeta(ruleStateMeta)
            .setRealm(Constants.MASTER_REALM)
        def laterRetract = new AttributeEvent(ref, null, 1000L)
            .setMeta(ruleStateMeta)
            .setRealm(Constants.MASTER_REALM)
            .setDeleted(true)

        when:
        rulesService.onAttributeEvent(newerInsert)
        rulesService.onAttributeEvent(laterRetract)

        then:
        rulesService.preInitAttributeEvents.size() == 1
        def bufferedEvent = rulesService.preInitAttributeEvents.get(ref)
        bufferedEvent != null
        bufferedEvent.deleted
        bufferedEvent.timestamp == 1000L
    }

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
    def "rules service should still retract a cached rule state when a later retract arrives with an older value timestamp"() {
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

        and: "a delete for the same rule state arrives afterwards carrying an older value timestamp"
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

        then: "the cached rule state should be removed"
        def latestRuleState = rulesService.attributeEvents.find { it.id == asset.id && it.name == "count" }
        assert latestRuleState == null

        and: "the rule engine fact should also be removed"
        conditions.eventually {
            def engineState = engine.facts.assetStates.find { it.id == asset.id && it.name == "count" }
            assert engineState == null
        }

        cleanup:
        stopContainer()
    }
}
