package demo.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.rules.facade.ConsolesFacade
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.BaseAssetQuery
import org.openremote.model.attribute.AttributeType
import org.openremote.model.notification.AlertNotification
import org.openremote.model.rules.Assets

import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
ConsolesFacade consolesFacade = binding.consoles
Assets assets = binding.assets

rules.add()
     .name("Welcome home")
     .when({
        facts ->

            def consoleIds = facts.matchAssetState(new AssetQuery()
                                                     .type(AssetType.CONSOLE)
                                                     .location(
                new BaseAssetQuery.RadialLocationPredicate(100, ManagerDemoSetup.SMART_HOME_LOCATION.y, ManagerDemoSetup.SMART_HOME_LOCATION.x)))
                                .filter({!facts.getOptional("welcomeHome" + "_${it.id}").isPresent()})
                                .map({it.id})
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

            AlertNotification alert = new AlertNotification(
                title: "Welcome Home",
                message: "No new events to report"
            )

            consoleIds.forEach({
                LOG.info("Welcome Home triggered: $it")
                consolesFacade.notify(it, alert)
                facts.put("welcomeHome" + "_${it}", it)
            })
    })

rules.add()
    .name("Welcome home reset")
    .when({
        facts ->

            def consoleIds = facts.matchAssetState(new AssetQuery()
                                                     .type(AssetType.CONSOLE)
                                                     .attributeValue(AttributeType.LOCATION.name, new BaseAssetQuery.ValueEmptyPredicate()))
                                .filter({facts.getOptional("welcomeHome" + "_${it.id}").isPresent()})
                                .map({it.id})
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
