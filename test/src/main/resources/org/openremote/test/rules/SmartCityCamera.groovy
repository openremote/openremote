package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications

import java.util.logging.Logger
import java.util.stream.Collectors

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Notifications notifications = binding.notifications
Assets assets = binding.assets

def cameraTotals = new HashMap<String, Integer>()

rules.add()
        .name("Count persons in sight")
        .when({
    facts ->

        def totals = facts.matchAssetState(new AssetQuery()
                .names(new StringPredicate(AssetQuery.Match.BEGIN, "Camera"))
                .attributeName("cameraCountIn"))
                .map(
                {
                    new Tuple(it.id, it.getValueAs(Double.class).orElse(0d) - facts.matchFirstAssetState(new AssetQuery()
                            .names(new StringPredicate(AssetQuery.Match.BEGIN, "Camera"))
                            .attributeName("cameraCountOut"))
                            .map({ it.getValueAs(Double.class).orElse(0d)}).orElse(0d))
                })
                .filter(
                {
                    cameraTotals.getOrDefault(it.get(0), new Integer(0)) != it.get(1)
                })
                .collect(Collectors.toMap({ Tuple tuple -> tuple.get(0) as String }, { Tuple tuple -> tuple.get(1) as Integer }))

        if (!totals.isEmpty()) {
            facts.bind("totals", totals)
            true
        } else {
            false
        }
}).then({
    facts ->
        Map<String, Integer> totals = facts.bound("totals")
        def attributesEvents = []
        totals.forEach({ k, v ->
            cameraTotals.put(k, v)
            attributesEvents << new AttributeEvent(k, "cameraCountTotal", v)
        })
        assets.dispatch(attributesEvents.toArray(new AttributeEvent[attributesEvents.size()]))
})

rules.add()
        .name("Send alert when cameraCountTotalAlertLevel is passed")
        .when({
    facts ->

        def cameraIds = facts.matchAssetState(new AssetQuery()
                .names(new StringPredicate(AssetQuery.Match.BEGIN, "Camera"))
                .attributeName("cameraCountTotalAlert"))
                .filter(
                {
                    !it.value.orElse(false) &&
                    facts.matchFirstAssetState(new AssetQuery()
                            .ids(it.id)
                            .attributeName("cameraCountTotal"))
                            .map({ it.value.orElse(-1) })
                            .orElse(-1) >
                            facts.matchFirstAssetState(new AssetQuery()
                                    .ids(it.id)
                                    .attributeName("cameraCountTotalAlertLevel"))
                                    .map({ it.value.orElse(-1) })
                                    .orElse(-1)
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
        cameraIds.forEach({
            facts.updateAssetState(it, "cameraCountTotalAlert", true)
        })
})
