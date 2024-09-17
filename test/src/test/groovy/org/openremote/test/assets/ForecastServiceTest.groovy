package org.openremote.test.assets

import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.ForecastService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.AssetPredictedDatapoint
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.IntStream

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
    @Shared
    static ForecastConfigurationWeightedExponentialAverage forecastConfig = new ForecastConfigurationWeightedExponentialAverage(
            new ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration("P7D"),
            3,
            new ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration("PT1H"),
            4
    )
    @Shared
    // Outer list is hours
    // Inner list is weekly values
    static double[][] forecastHistoricalData = [
            [4.0, 3.0, 2.0],
            [4.1, 3.1, 2.1],
            [4.2, 3.2, 2.2],
            [4.3, 3.3, 2.3],
            [4.4, 3.4, 2.4]
    ]
    @Shared
    static ThingAsset forecastThing2
    @Shared
    static String forecastAttributeName = "forecastAttribute"

    // TODO: RT - Not too sure the intention with the offset and seems to be used only in ignored tests so skip for now
//    def setupSpec() {
//        forecastThing2 = addForecastAsset(assetStorageService, managerTestSetup.realmEnergyName, forecastAttributeName, forecastConfig)
//        Long offset = ((forecastConfig.getForecastPeriod().toMillis() * 2) / 3) * (-1)
//        insertHistoryTestDataToDb(new AttributeRef(forecastThing2.id, forecastAttributeName), forecastHistoricalData, offset, forecastConfig)
//
//        // Insert forecast test data
//        assetPredictedDatapointService.updateValues(forecastThing2.id, forecastAttributeName, [
//                new Pair<>(1.0d, LocalDateTime.ofInstant(Instant.ofEpochMilli(timerService.getCurrentTimeMillis() + forecastConfig.getForecastPeriod().toMillis() + offset), ZoneId.systemDefault())),
//                new Pair<>(2.0d, LocalDateTime.ofInstant(Instant.ofEpochMilli(timerService.getCurrentTimeMillis() + forecastConfig.getForecastPeriod().toMillis() * 2 + offset), ZoneId.systemDefault()))
//        ])
//    }

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
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        assetStorageService = container.getService(AssetStorageService.class)
        assetDatapointService = container.getService(AssetDatapointService.class)
        forecastService = container.getService(ForecastService.class)
        timerService = container.getService(TimerService.class)

        when: "the system time is stopped and set exactly on the next hour"
        stopPseudoClock()
        def now = Instant.ofEpochMilli(timerService.getCurrentTimeMillis())
        now = now.truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS)
        advancePseudoClock(now.toEpochMilli()-timerService.getCurrentTimeMillis(), MILLISECONDS, container)

        and: "forecast asset is added"
        def forecastThing1 = addForecastAsset(assetStorageService, managerTestSetup.realmEnergyName, forecastAttributeName, forecastConfig)
        def attributeRef = new AttributeRef(forecastThing1.id, forecastAttributeName)

        then: "attribute should be registered at the forecast service"
        conditions.eventually {
            assert forecastService.forecastTaskManager.containsAttribute(attributeRef)
        }

        when: "the forecast service is stopped"
        forecastService.forecastTaskManager.stop(3000)

        and: "historical data is added for the forecast attribute"
        // Offset historical data by two hours to match up with test intervals
        insertHistoryTestDataToDb(new AttributeRef(forecastThing1.id, forecastAttributeName), forecastHistoricalData, 2*3600000, forecastConfig)

        and: "time has passed"
        advancePseudoClock(
                forecastConfig.forecastPeriod.toMillis(), MILLISECONDS, container
        )

        and: "the forecast service is started again"
        forecastService.forecastTaskManager.start(timerService.currentTimeMillis)

        then: "initial forecast values should be calculated"
        conditions.eventually {
            List<AssetPredictedDatapoint> predictedDatapoints = assetPredictedDatapointService
                .getDatapoints(attributeRef)
                .sort {it.timestamp}
            assert predictedDatapoints.size() == forecastConfig.forecastCount
            for (int i = 0; i < predictedDatapoints.size(); i++) {
                assert predictedDatapoints[i].value == calculateForecast(forecastHistoricalData[i][0..forecastConfig.pastCount - 1]).get()
                assert predictedDatapoints[i].timestamp == timerService.currentTimeMillis + (i + 1) * forecastConfig.forecastPeriod.toMillis()
            }
        }

        when: "new forecast attribute is added"
        forecastThing1 = assetStorageService.find(forecastThing1.id)
        forecastThing1.getAttributes().addOrReplace(
                new Attribute<>("newForecastAttribute", NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(
                                        FORECAST,
                                        forecastConfig
                                )
                        )
        )
        forecastThing1 = assetStorageService.merge(forecastThing1)
        def newAttributeRef = new AttributeRef(forecastThing1.id, "newForecastAttribute")

        then: "new attribute should be registered at the forecast service"
        conditions.eventually {
            assert forecastService.forecastTaskManager.containsAttribute(newAttributeRef)
        }

        when: "forecast configuration is updated"
        forecastThing1 = assetStorageService.find(forecastThing1.id)
        def updatedPastCount = forecastConfig.pastCount + 1
        def updatedConfig = new ForecastConfigurationWeightedExponentialAverage(
                forecastConfig.pastPeriod, updatedPastCount, forecastConfig.forecastPeriod, forecastConfig.forecastCount
        )
        forecastThing1.getAttributes().addOrReplace(
                new Attribute<>("newForecastAttribute", NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(
                                        FORECAST,
                                        updatedConfig
                                )
                        )
        )
        forecastThing1 = assetStorageService.merge(forecastThing1)

        then: "forecast service should contain updated forecast configuration"
        conditions.eventually {
            ForecastService.ForecastAttribute forecastAttribute = forecastService.forecastTaskManager.getAttribute(newAttributeRef)
            assert forecastAttribute != null
            assert updatedPastCount == (forecastAttribute.config as ForecastConfigurationWeightedExponentialAverage).pastCount
        }

        when: "forecast configuration is deleted"
        forecastThing1 = assetStorageService.find(forecastThing1.id)
        forecastThing1.getAttributes().addOrReplace(
                new Attribute<>("newForecastAttribute", NUMBER)
        )
        forecastThing1 = assetStorageService.merge(forecastThing1)

        then: "attribute should be unregistered from forecast service"
        conditions.eventually {
            assert !forecastService.forecastTaskManager.containsAttribute(newAttributeRef)
        }

        when: "asset is deleted"
        assetStorageService.delete([forecastThing1.id])

        then: "attribute should be unregistered from forecast service"
        conditions.eventually {
            assert !forecastService.forecastTaskManager.containsAttribute(attributeRef)
        }
    }

    private Optional<Double> calculateForecast(List<Double> values) {
        double R = values.size()
        double a = 2 / (R + 1)
        return values.stream().reduce((result, periodValue) -> (periodValue * a) + (result * (1 - a)))
    }

    protected Asset<?>  addForecastAsset(AssetStorageService assetStorageService, String realm, String attributeName, ForecastConfigurationWeightedExponentialAverage config) {
        Asset<?> thing = new ThingAsset("Forecast Test Thing")
        thing.setRealm(realm)
        thing.getAttributes().addOrReplace(
                new Attribute<>(attributeName, NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(
                                        FORECAST,
                                        config
                                )
                        )
        );
        thing = assetStorageService.merge(thing)
        return thing
    }

    void insertHistoryTestDataToDb(AttributeRef attributeRef, double[][] data, long offsetMillis, ForecastConfigurationWeightedExponentialAverage config) {
        def values = IntStream.range(0, data.length).mapToObj { hourIndex ->
            {
                def hourData = data[hourIndex]

                return IntStream.range(0, config.getPastCount()).mapToObj { periodIndex ->
                    {
                        def periodHourValue = hourData[periodIndex]
                        def timeOffset =
                                config.getPastPeriod().toMillis() * (config.getPastCount() - periodIndex) - // THE DAY OF THE WEEK
                                config.getForecastPeriod().toMillis() * hourIndex - // THE HOUR OFFSET
                                offsetMillis

                        return new ValueDatapoint<>(
                            timerService.getCurrentTimeMillis() - timeOffset,
                            periodHourValue)
                    }
                }
            }
        }.flatMap{it}.toList()

        assetDatapointService.upsertValues(attributeRef.getId(), attributeRef.getName(), values)
    }
}
