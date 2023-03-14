package org.openremote.test.assets

import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.ForecastService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.AssetPredictedDatapoint
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.openremote.model.value.MetaItemType.FORECAST
import static org.openremote.model.value.ValueType.NUMBER
import static spock.util.matcher.HamcrestMatchers.closeTo

class ForecastServiceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static ManagerTestSetup managerTestSetup
    @Shared
    static AssetPredictedDatapointService assetPredictedDatapointService
    @Shared
    static AssetStorageService assetStorageService
    @Shared
    static AssetDatapointService assetDatapointService
    @Shared
    static ForecastService forecastService
    @Shared
    static TimerService timerService

    def setupSpec() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        assetStorageService = container.getService(AssetStorageService.class)
        assetDatapointService = container.getService(AssetDatapointService.class)
        forecastService = container.getService(ForecastService.class)
        timerService = container.getService(TimerService.class)
    }

    @Ignore
    def "Test forecast calculation after server restart - no initial forecast data"() {
        given: "attribute with forecast configuration"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def attributeRef = new AttributeRef(managerTestSetup.forecastThing1Id, managerTestSetup.forecastAttributeName)
        def config = managerTestSetup.forecastConfig

        and:
        stopPseudoClock()

        expect: "test datapoints have been added to the database"
        conditions.eventually {
            def count = assetDatapointService.getDatapointsCount(attributeRef)
            assert count == config.pastCount * managerTestSetup.forecastTestHistoryData.size()
        }

        and: "asset attribute has been registered at the forecast service"
        conditions.eventually {
            assert forecastService.forecastTaskManager.containsAttribute(attributeRef)
        }

        and: "initial forecast values have been calculated"
        conditions.eventually {
            List<AssetPredictedDatapoint> predictedDatapoints = assetPredictedDatapointService
                .getDatapoints(attributeRef)
                .sort {it.timestamp}
            assert predictedDatapoints.size() == config.forecastCount
            for (int i = 0; i < predictedDatapoints.size(); i++) {
                assert predictedDatapoints[i].value == calculateForecast(managerTestSetup.forecastTestHistoryData[i][0..config.pastCount - 1]).get()
                assert predictedDatapoints[i].timestamp, closeTo(timerService.currentTimeMillis + (i + 1) * config.forecastPeriod.toMillis(), Duration.ofSeconds(5).toMillis())
            }
        }
    }

    @Ignore
    def "Test forecast calculation after server restart - forecast data partially available"() {
        given: "attribute with forecast configuration"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def attributeRef = new AttributeRef(managerTestSetup.forecastThing2Id, managerTestSetup.forecastAttributeName)
        def config = managerTestSetup.forecastConfig

        and:
        stopPseudoClock()

        expect: "test datapoints have been added to the database"
        conditions.eventually {
            def count = assetDatapointService.getDatapointsCount(attributeRef)
            assert count == config.pastCount * managerTestSetup.forecastTestHistoryData.size()
        }

        and: "asset attribute has been registered at the forecast service"
        conditions.eventually {
            assert forecastService.forecastTaskManager.containsAttribute(attributeRef)
        }

        and: "partially existing forecast values should have have been completed and updated"
        conditions.eventually {
            List<AssetPredictedDatapoint> predictedDatapoints = assetPredictedDatapointService
                .getDatapoints(attributeRef)
                .sort {it.timestamp}
            assert predictedDatapoints.size() == config.forecastCount
            Long offset = ((config.forecastPeriod.toMillis() * 2) / 3) * (-1)
            for (int i = 0; i < predictedDatapoints.size(); i++) {
                assert predictedDatapoints[i].value == calculateForecast(managerTestSetup.forecastTestHistoryData[i][0..config.pastCount - 1]).get()
                assert predictedDatapoints[i].timestamp, closeTo(timerService.currentTimeMillis + (i + 1) * config.forecastPeriod.toMillis() + offset, Duration.ofSeconds(10).toMillis())
            }
        }
    }

    def "Test adding updating and deleting an asset"() {
        given: "no assets with forecast configuration"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def config = managerTestSetup.forecastConfig
        def attributeName = "forecastTestAttribute"

        and:
        stopPseudoClock()

        when: "asset is added"
        String assetId = managerTestSetup.addForecastTestAsset(
            assetStorageService, managerTestSetup.realmEnergyName, attributeName, config
        )
        AttributeRef attributeRef = new AttributeRef(assetId, attributeName)

        then: "attribute should be registered at the forecast service"
        conditions.eventually {
            assert forecastService.forecastTaskManager.containsAttribute(attributeRef)
        }

        when: "test data is inserted to the database"
        forecastService.forecastTaskManager.stop(3000)
        managerTestSetup.insertHistoryTestDataToDb(
            attributeRef, managerTestSetup.forecastTestHistoryData, config.forecastPeriod.toMillis(), config
        )

        and: "time has passed"
        advancePseudoClock(
            config.forecastPeriod.toMillis(), MILLISECONDS, container
        )

        and: "forecast service is restarted"
        forecastService.forecastTaskManager.start(timerService.currentTimeMillis)

        then: "initial forecast values should be calculated"
        conditions.eventually {
            List<AssetPredictedDatapoint> predictedDatapoints = assetPredictedDatapointService
                .getDatapoints(attributeRef)
                .sort {it.timestamp}
            assert predictedDatapoints.size() == config.forecastCount
            for (int i = 0; i < predictedDatapoints.size(); i++) {
                assert predictedDatapoints[i].value == calculateForecast(managerTestSetup.forecastTestHistoryData[i][0..config.pastCount - 1]).get()
                assert predictedDatapoints[i].timestamp == timerService.currentTimeMillis + (i + 1) * config.forecastPeriod.toMillis()
            }
        }

        when: "time has passed"
        forecastService.forecastTaskManager.stop(3000)
        advancePseudoClock(
            config.forecastPeriod.toMillis(), MILLISECONDS, container
        )
        forecastService.forecastTaskManager.start(timerService.currentTimeMillis)

        then: "forecast values should be updated"
        conditions.eventually {
            List<AssetPredictedDatapoint> predictedDatapoints = assetPredictedDatapointService
                .getDatapoints(attributeRef)
                .sort {it.timestamp}
            assert predictedDatapoints.size() == config.forecastCount + 1
            for (int i = 1; i < predictedDatapoints.size(); i++) {
                assert predictedDatapoints[i].value == calculateForecast(managerTestSetup.forecastTestHistoryData[i][0..config.pastCount - 1]).get()
                assert predictedDatapoints[i].timestamp == timerService.currentTimeMillis + i * config.forecastPeriod.toMillis()
            }
        }

        when: "new forecast attribute is added"
        def asset = assetStorageService.find(assetId)
        asset.getAttributes().addOrReplace(
            new Attribute<>("newForecastAttribute", NUMBER)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        FORECAST,
                        config
                    )
                )
        )
        assetStorageService.merge(asset)
        def newAttributeRef = new AttributeRef(assetId, "newForecastAttribute")

        then: "new attribute should be registered at the forecast service"
        conditions.eventually {
            assert forecastService.forecastTaskManager.containsAttribute(newAttributeRef)
        }

        when: "forecast configuration is updated"
        asset = assetStorageService.find(assetId)
        def updatedPastCount = config.pastCount + 1
        def updatedConfig = new ForecastConfigurationWeightedExponentialAverage(
            config.pastPeriod, updatedPastCount, config.forecastPeriod, config.forecastCount
        )
        asset.getAttributes().addOrReplace(
            new Attribute<>("newForecastAttribute", NUMBER)
                .addOrReplaceMeta(
                    new MetaItem<>(
                        FORECAST,
                        updatedConfig
                    )
                )
        )
        assetStorageService.merge(asset)

        then: "forecast service should contain updated forecast configuration"
        conditions.eventually {
            assert forecastService.forecastTaskManager.containsAttribute(newAttributeRef)
            assert updatedPastCount == forecastService.forecastTaskManager.configMap.get(newAttributeRef).pastCount
        }

        when: "forecast configuration is deleted"
        asset = assetStorageService.find(assetId)
        asset.getAttributes().addOrReplace(
            new Attribute<>("newForecastAttribute", NUMBER)
        )
        assetStorageService.merge(asset)

        then: "attribute should be unregistered from forecast service"
        conditions.eventually {
            assert !forecastService.forecastTaskManager.containsAttribute(newAttributeRef)
        }

        when: "asset is deleted"
        assetStorageService.delete([assetId])

        then: "attribute should be unregistered from forecast service"
        conditions.eventually {
            assert !forecastService.forecastTaskManager.containsAttribute(attributeRef)
        }
    }

    private Optional<Double> calculateForecast(List<Double> values) {
        double R = values.size();
        double a = 2 / (R + 1);
        return values.stream().reduce((olderValue, oldValue) -> {
            return (oldValue * a) + (olderValue * (1 - a));
        });
    }
}
