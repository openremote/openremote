package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.notification.*
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.impl.*
import org.openremote.model.query.*
import org.openremote.model.query.filter.*
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications
import org.openremote.model.rules.Users

import java.util.logging.Logger
import java.util.stream.Collectors

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Notifications notifications = binding.notifications
Users users = binding.users
Assets assets = binding.assets

rules.add()
        .name("Welcome home")
        .when({
    facts ->

        def consoleIds = facts.matchAssetState(new AssetQuery()
                .types(ConsoleAsset.class)
                .attributes(new LocationAttributePredicate(
                new RadialGeofencePredicate(100, ManagerTestSetup.SMART_BUILDING_LOCATION.y, ManagerTestSetup.SMART_BUILDING_LOCATION.x))))
                .filter({ !facts.getOptional("welcomeHome" + "_${it.id}").isPresent() })
                .map({ it.id })
                .collect()

        if (consoleIds.size() > 0) {
            facts.bind("consoleIds", consoleIds)
            true
        } else {
            false
        }
})
        .then({
    facts ->

        List<String> consoleIds = facts.bound("consoleIds")
        if (consoleIds != null) {
            List<Notification.Target> targets = consoleIds.stream().map{new Notification.Target(Notification.TargetType.ASSET, it)}.collect(Collectors.toList())
            Notification notification = new Notification(
                "Welcome Home",
                new PushNotificationMessage("Welcome Home", "No new events to report", null, null, null), targets, null, null)

            notifications.send(notification)

            consoleIds.forEach({
                LOG.info("Welcome Home triggered: $it")
                facts.put("welcomeHome" + "_${it}", it)
            })
        }
})

rules.add()
        .name("Welcome home reset")
        .when({
    facts ->

        def consoleIds = facts.matchAssetState(new AssetQuery()
                .types(ConsoleAsset.class)
                .attributeValue(Asset.LOCATION.name, new ValueEmptyPredicate()))
                .filter({ facts.getOptional("welcomeHome" + "_${it.id}").isPresent() })
                .map({ it.id })
                .collect()

        if (consoleIds.size() > 0) {
            facts.bind("consoleIds", consoleIds)
            true
        } else {
            false
        }

})
        .then(
        {
            facts ->

                List<String> consoleIds = facts.bound("consoleIds")

                consoleIds.forEach({
                    LOG.info("Welcome Home Reset Triggered: $it")
                    facts.remove("welcomeHome" + "_${it}")
                })
        })
