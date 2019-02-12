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
                .name(new StringPredicate(BaseAssetQuery.Match.BEGIN, "Camera"))
                .attributeName("cameraCountIn"))
                .map(
                {
                    new Tuple(it.id, it.valueAsNumber.orElse(0) - facts.matchFirstAssetState(new AssetQuery()
                            .name(new StringPredicate(BaseAssetQuery.Match.BEGIN, "Camera"))
                            .attributeName("cameraCountOut"))
                            .map({ it.valueAsNumber.orElse(0) }).orElse(0))
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
            attributesEvents << new AttributeEvent(k, "cameraCountTotal", Values.create(v))
        })
        assets.dispatch(attributesEvents.toArray(new AttributeEvent[attributesEvents.size()]))
})

rules.add()
        .name("Send alert when cameraCountTotalAlertLevel is passed")
        .when({
    facts ->

        LOG.info("cam: "+ facts.matchAssetState(new AssetQuery()
                .name(new StringPredicate(BaseAssetQuery.Match.BEGIN, "Camera"))).collect())

        def cameraIds = facts.matchAssetState(new AssetQuery()
                .name(new StringPredicate(BaseAssetQuery.Match.BEGIN, "Camera"))
                .attributeName("cameraCountTotalAlert"))
                .filter(
                {
                    LOG.info("id: " + it.id)
                    LOG.info("cameraCountTotalAlert: " + it.valueAsBoolean)
                    LOG.info("OrElse: " + it.valueAsBoolean.orElse(false))

                    !it.valueAsBoolean.orElse(false) &&
                    facts.matchFirstAssetState(new AssetQuery()
                            .id(it.id)
                            .attributeName("cameraCountTotal"))
                            .map({ it.valueAsNumber.orElse(-1) })
                            .orElse(-1) >=
                            facts.matchFirstAssetState(new AssetQuery()
                                    .id(it.id)
                                    .attributeName("cameraCountTotalAlert"))
                                    .map({ it.valueAsNumber.orElse(0) })
                                    .orElse(0)
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