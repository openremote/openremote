package org.openremote.setup.integration.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

RulesBuilder rules = binding.rules
Assets assets = binding.assets

rules.add()
        .name("Long running rule")
        .when(
                {facts ->
                    List<AttributeEvent> updates = Collections.synchronizedList(new ArrayList<>())
                    facts.matchFirstAssetState(new AssetQuery().names("CounterAsset")).ifPresent { attr ->
                        updates.add(new AttributeEvent(attr.id, attr.name, attr.value.orElse(0) + 1))
                    }
                    facts.bind("updates", updates)
                    return true
                })
        .then(
                { facts ->
                    def updates = facts.bound("updates") as List<AttributeEvent>
                    updates.forEach { event ->
                        assets.dispatch(event) }
                    Thread.sleep(600)
                })
