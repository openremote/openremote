package org.openremote.setup.load1

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.impl.WeatherAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications
import org.openremote.model.rules.Users

import java.util.logging.Logger
import java.util.stream.Collectors

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Users users = binding.users
Notifications notifications = binding.notifications
Assets assets = binding.assets

// This rule updates an attribute called calculated for WeatherAssets by adding the rainfall and temperature attributes
// (no practical use for this calculation, it is just for testing purposes). Whenever either of these values change
// for an asset then the calculated attribute is updated.

rules.add()
        .name("Calculate weather asset calculated attribute")
        .when(
                { facts ->

                    List<AttributeEvent> updates = []

                    // Get weather asset rainfall and/or temperature states that have changed value
                    facts.matchAssetState(
                            new AssetQuery()
                                    .types(WeatherAsset)
                                    .attributeNames(WeatherAsset.TEMPERATURE.name, WeatherAsset.RAINFALL.name)
                    ).collect(Collectors.groupingBy{state -> state.id})
                    .forEach{id, states ->
                        // See if either attribute has changed for this asset
                        if (states.any {state -> !Objects.equals(state.value.orElse(null), state.oldValue.orElse(null))}) {
                            def value1 = states[0].value.orElse(0) as float
                            def value2 = states[1].value.orElse(0) as float
                            def calculated = value1+value2
                            LOG.fine("Calculated new value for '$id': $calculated")
                            updates.add(new AttributeEvent(id, "calculated", calculated))
                        }
                    }

                    if (!updates.isEmpty()) {
                        facts.bind("updates", updates)
                    }

                    // Trigger the rule action if we have one or more changes to process
                    return !updates.isEmpty()
                })
        .then(
                { facts ->
                    def updates = facts.bound("updates") as List<AttributeEvent>

                    updates.forEach {
                        assets.dispatch(it)
                    }
                })
