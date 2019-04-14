package demo.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetType
import org.openremote.model.attribute.AttributeType
import org.openremote.model.notification.Notification
import org.openremote.model.notification.PushNotificationMessage
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.LocationAttributePredicate
import org.openremote.model.query.filter.RadialGeofencePredicate
import org.openremote.model.query.filter.ValueEmptyPredicate
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications

import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Notifications notifications = binding.notifications
Assets assets = binding.assets

rules.add()
        .name("Welcome home")
        .when({
    facts ->

        def consoleIds = facts.matchAssetState(new AssetQuery()
                .type(AssetType.CONSOLE)
                .attributes(new LocationAttributePredicate(
                new RadialGeofencePredicate(100, ManagerDemoSetup.SMART_BUILDING_LOCATION.y, ManagerDemoSetup.SMART_BUILDING_LOCATION.x))))
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
        Notification notification = new Notification(
                "Welcome Home",
                new PushNotificationMessage("Welcome Home", "No new events to report", null, null, null),
                new Notification.Targets(Notification.TargetType.ASSET, consoleIds), null, null)

        notifications.send(notification)

        consoleIds.forEach({
            LOG.info("Welcome Home triggered: $it")
            facts.put("welcomeHome" + "_${it}", it)
        })
})

rules.add()
        .name("Welcome home reset")
        .when({
    facts ->

        def consoleIds = facts.matchAssetState(new AssetQuery()
                .type(AssetType.CONSOLE)
                .attributeValue(AttributeType.LOCATION.attributeName, new ValueEmptyPredicate()))
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
