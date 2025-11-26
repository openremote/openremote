package org.openremote.test.benchmark

import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.impl.ShipAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.MetaItemType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.temporal.ChronoUnit

import static org.openremote.model.rules.RulesetStatus.DEPLOYED

class FlowRulesBenchmarkTest extends Specification implements ManagerContainerTrait {

    def "Flow Rules Performance Benchmark"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, delay: 0.05)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class);
        def timerService = container.getService(TimerService.class);

        when: "An asset is created"
        def asset = new ShipAsset("Flow ship")
                .setRealm(Constants.MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<Object>(ShipAsset.SPEED, null, timerService.getNow().minus(10, ChronoUnit.HOURS).toEpochMilli()).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("historicOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("sumOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("minOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("avgOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("medOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("maxOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("derOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true)),
                        new Attribute<Object>("intOutput", null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),new MetaItem<>(MetaItemType.RULE_STATE, true))
                )
        asset = assetStorageService.merge(asset);

        and: "Some datapoints are added to the ship asset"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(new AttributeRef(asset.getId(), ShipAsset.SPEED.name), 30D, timerService.getNow().minus(30, ChronoUnit.MINUTES).toEpochMilli()));
        sleep(100)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(new AttributeRef(asset.getId(), ShipAsset.SPEED.name), 20D, timerService.getNow().minus(20, ChronoUnit.MINUTES).toEpochMilli()));
        sleep(100)
        def previousTime = timerService.getNow().minus(10, ChronoUnit.MINUTES);
        def previousValue = 10D
        assetProcessingService.sendAttributeEvent(new AttributeEvent(new AttributeRef(asset.getId(), ShipAsset.SPEED.name), previousValue, previousTime.toEpochMilli()));
        sleep(100)

        def currentTime = timerService.getNow();
        def currentValue = 1D;
        assetProcessingService.sendAttributeEvent(new AttributeEvent(new AttributeRef(asset.getId(), ShipAsset.SPEED.name), currentValue, currentTime.toEpochMilli()));
        sleep(100)

        then: "They are queryable"
        conditions.eventually {
            def dps = assetDatapointService.getDatapoints(new AttributeRef(asset.getId(), ShipAsset.SPEED.name));
            assert dps.size() == 4
        }

        when: "The flow rule is added"
        String json = getClass().getResource("/org/openremote/test/rules/HistoricAndMultiInputProcessorTest.flow").text
        json = json.replaceAll("%ASSETID%", asset.getId())
        json = json.replaceAll("%HISTORIC_ATTRIBUTE%", ShipAsset.SPEED.name)
        json = json.replaceAll("%HISTORIC_VALUE_OUTPUT%", "historicOutput")
        json = json.replaceAll("%SUM_OUTPUT%", "sumOutput")
        json = json.replaceAll("%MIN_OUTPUT%", "minOutput")
        json = json.replaceAll("%AVG_OUTPUT%", "avgOutput")
        json = json.replaceAll("%MED_OUTPUT%", "medOutput")
        json = json.replaceAll("%MAX_OUTPUT%", "maxOutput")
        json = json.replaceAll("%DER_OUTPUT%", "derOutput")
        json = json.replaceAll("%INT_OUTPUT%", "intOutput")

        def ruleset = (new GlobalRuleset(
                "HistoricValueAndProcessorsTest",
                Ruleset.Lang.FLOW,
                json
        ))
        rulesetStorageService.merge(ruleset)

        and: "the integral and derivative values are calculated"
        def derivative = (currentValue - previousValue) / (((currentTime.toEpochMilli() - previousTime.toEpochMilli())/1000.0))
        def integral = ((currentValue + previousValue) * ((currentTime.toEpochMilli() - previousTime.toEpochMilli())/1000.0)) / 2

        then: "The rule compiles successfully"
        conditions.eventually {
            assert rulesService.globalEngine.get() != null
            assert rulesService.globalEngine.get().isRunning()
            assert rulesService.globalEngine.get().deployments.values().any({ it.name == "HistoricValueAndProcessorsTest" && it.status == DEPLOYED})
        }

        and: "The rule runs and the various attributes are updated accordingly"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), ShipAsset.class);
            def currentValueSpeed = asset.getAttribute(ShipAsset.SPEED).get().getValue().get()
            assert asset.getAttribute("historicOutput").get().getValue().get() == 20
            assert asset.getAttribute("sumOutput").get().getValue().get() == 10
            assert asset.getAttribute("minOutput").get().getValue().get() == 1
            assert asset.getAttribute("avgOutput").get().getValue().get() == 2.5
            assert asset.getAttribute("medOutput").get().getValue().get() == 2.5
            assert asset.getAttribute("maxOutput").get().getValue().get() == 4
            assert asset.getAttribute("derOutput").get().getValue().get() == derivative
            assert asset.getAttribute("intOutput").get().getValue().get() == integral
        }
        sleep(100)

        and: "performance tracking"
        def executionTimes = []
        def iterations = 50
        def completedIterations = 0

        and: "Benchmark"
        iterations.times { i ->
            def value = 25.0 + i * 5
            println "Iteration ${i+1} of ${iterations}"

            def startTime = System.nanoTime()

            assetProcessingService.sendAttributeEvent(new AttributeEvent(new AttributeRef(asset.getId(), ShipAsset.SPEED.name), value, timerService.getNow().toEpochMilli()))

            conditions.eventually {
                asset = assetStorageService.find(asset.getId(), ShipAsset.class)
                def currentDerivative = asset.getAttribute("derOutput").get().getValue().get()
                def currentIntegral = asset.getAttribute("intOutput").get().getValue().get()
                assert currentDerivative != derivative
                assert currentIntegral != integral
                derivative = currentDerivative
                integral = currentIntegral
            }

            def endTime = System.nanoTime()
            executionTimes.add(endTime - startTime)
            completedIterations++
            sleep(new Random().nextInt(100, 300))
        }

        then: "Calculate simple metrics"
        def totalTimeNs = executionTimes.sum()
        def avgTimeMs = (totalTimeNs / iterations) / 1_000_000
        def opsPerSecond = 1_000_000_000 / (totalTimeNs / iterations)

        println "=== FLOW RULES BENCHMARK ==="
        println "Completed iterations: $completedIterations"
        println "Average execution time: ${String.format("%.3f", avgTimeMs)} ms"
        println "Operations per second: ${String.format("%.0f", opsPerSecond)}"
        println "============================"

        completedIterations == iterations

    }
}
