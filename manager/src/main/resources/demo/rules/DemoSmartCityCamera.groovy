package demo.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.BaseAssetQuery
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications
import org.openremote.model.value.Values

import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Notifications notifications = binding.notifications
Assets assets = binding.assets

rules.add()
        .name("Send alert when cameraCountTotalAlertLevel is passed")
        .when({
    facts ->

        def cameraIds = facts.matchAssetState(new AssetQuery()
                .name(new StringPredicate(BaseAssetQuery.Match.BEGIN, "Camera"))
                .attributeValue("cameraCountTotalAlert", false))
                .filter(
                {
                    facts.matchAssetState(new AssetQuery()
                            .id(it.id)
                            .attributeName("cameraCountTotal"))
                            .map({ it.valueAsNumber.orElse(0) }) >=
                            facts.matchAssetState(new AssetQuery()
                                    .id(it.id)
                                    .attributeName("cameraCountTotalAlertLevel"))
                                    .map({ it.valueAsNumber.orElse(-1) })
                })
                .map({ it.id })
                .collect()

        if (cameraIds.size() > 0) {
            facts.bind("cameraIds", cameraIds)
            true
        } else {
            false
        }
}).then({
    facts ->
        List<String> cameraIds = facts.bound("cameraIds")
        def attributesEvents = cameraIds.collect {
            new AttributeEvent(it, "cameraCountTotalAlert", Values.create(true))
        } as AttributeEvent[]
        assets.dispatch(attributesEvents)
})