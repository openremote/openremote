/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.protocol.dispersion

import org.openremote.agent.protocol.dispersion.PollutantDispersionAgent
import org.openremote.agent.protocol.dispersion.PollutantDispersionAgentLink
import org.openremote.agent.protocol.dispersion.PollutantDispersionLinkRole
import org.openremote.agent.protocol.dispersion.PollutantDispersionProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.EnvironmentSensorAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.asset.impl.WeatherAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.NumberPredicate
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.NUMBER

class PollutantDispersionProtocolTest extends Specification implements ManagerContainerTrait {

    def "Dispersion protocol triggers on threshold crossing"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container environment is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)

        and: "a weather asset K"
        def weatherAsset = new WeatherAsset("Weather K")
            .setRealm(MASTER_REALM)
            .setLocation(new GeoJSONPoint(4.5000d, 51.9000d))
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.WIND_SPEED).setValue(18d)
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.WIND_DIRECTION).setValue(270)
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.CLOUD_COVERAGE).setValue(20)
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.SUN_ALTITUDE).setValue(35d)
        weatherAsset = assetStorageService.merge(weatherAsset)

        and: "predicted weather datapoints are available for the next day"
        long hourMillis = 60L * 60L * 1000L
        long baseTimestamp = System.currentTimeMillis()
        assetPredictedDatapointService.updateValues(weatherAsset.id, WeatherAsset.WIND_SPEED.name, [
            new ValueDatapoint<>(baseTimestamp, 10d),
            new ValueDatapoint<>(baseTimestamp + (6L * hourMillis), 28d),
            new ValueDatapoint<>(baseTimestamp + (12L * hourMillis), 14d),
            new ValueDatapoint<>(baseTimestamp + (18L * hourMillis), 24d),
            new ValueDatapoint<>(baseTimestamp + (24L * hourMillis), 18d)
        ])
        assetPredictedDatapointService.updateValues(weatherAsset.id, WeatherAsset.WIND_DIRECTION.name, [
            new ValueDatapoint<>(baseTimestamp, 270d),
            new ValueDatapoint<>(baseTimestamp + (6L * hourMillis), 270d),
            new ValueDatapoint<>(baseTimestamp + (12L * hourMillis), 270d),
            new ValueDatapoint<>(baseTimestamp + (18L * hourMillis), 270d),
            new ValueDatapoint<>(baseTimestamp + (24L * hourMillis), 270d)
        ])

        and: "a pollutant dispersion agent"
        def dispersionAgent = new PollutantDispersionAgent("Dispersion Agent")
            .setRealm(MASTER_REALM)
            .setWeatherAssetId(weatherAsset.id)
            .setEmissionScaleFactor(1d)
            .setSourceHeightMeters(5d)
            .setReceptorHeightMeters(1.5d)
            .setMinWindSpeedMs(0.5d)
            .setPredictionIntervalMinutes(30)
            .setPredictionHorizonHours(24)
        dispersionAgent = assetStorageService.merge(dispersionAgent)

        expect: "the protocol instance to be running"
        conditions.eventually {
            assert agentService.getProtocolInstance(dispersionAgent.id) != null
            assert agentService.getAgent(dispersionAgent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "source and output assets are linked"
        def sourceAsset = new EnvironmentSensorAsset("Source L")
            .setRealm(MASTER_REALM)
            .setLocation(new GeoJSONPoint(4.5000d, 51.9000d))
        sourceAsset.getAttribute(EnvironmentSensorAsset.PM2_5).ifPresent {
            it.addOrReplaceMeta(
                new MetaItem<>(
                    AGENT_LINK,
                    new PollutantDispersionAgentLink(dispersionAgent.id)
                        .setRole(PollutantDispersionLinkRole.SOURCE_TRIGGER)
                        .setTriggerPredicate(new NumberPredicate(999, AssetQuery.Operator.GREATER_THAN))
                )
            )
        }
        sourceAsset = assetStorageService.merge(sourceAsset)

        def outputAssetM = new ThingAsset("Output M")
            .setRealm(MASTER_REALM)
            .setLocation(new GeoJSONPoint(4.5010d, 51.9000d))
            .addOrReplaceAttributes(
                new Attribute<>("predictedConcentration", NUMBER)
                    .addOrReplaceMeta(
                        new MetaItem<>(
                            AGENT_LINK,
                            new PollutantDispersionAgentLink(dispersionAgent.id)
                                .setRole(PollutantDispersionLinkRole.OUTPUT_CONCENTRATION)
                        )
                    )
            )
        outputAssetM = assetStorageService.merge(outputAssetM)

        def outputAssetN = new ThingAsset("Output N")
            .setRealm(MASTER_REALM)
            .setLocation(new GeoJSONPoint(4.5006d, 51.9008d))
            .addOrReplaceAttributes(
                new Attribute<>("predictedConcentration", NUMBER)
                    .addOrReplaceMeta(
                        new MetaItem<>(
                            AGENT_LINK,
                            new PollutantDispersionAgentLink(dispersionAgent.id)
                                .setRole(PollutantDispersionLinkRole.OUTPUT_CONCENTRATION)
                        )
                    )
            )
        outputAssetN = assetStorageService.merge(outputAssetN)

        then: "all links are active"
        conditions.eventually {
            assert ((PollutantDispersionProtocol) agentService.getProtocolInstance(dispersionAgent.id)).linkedAttributes.size() == 3
        }

        when: "source PM2.5 is below threshold"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(sourceAsset.id, EnvironmentSensorAsset.PM2_5.name, 500, baseTimestamp))

        then: "propagation is not triggered"
        conditions.eventually {
            assert getPredictedDatapoints(assetPredictedDatapointService, outputAssetM.id, "predictedConcentration").isEmpty()
            assert getPredictedDatapoints(assetPredictedDatapointService, outputAssetN.id, "predictedConcentration").isEmpty()
        }

        when: "source PM2.5 crosses above threshold"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(sourceAsset.id, EnvironmentSensorAsset.PM2_5.name, 1200, baseTimestamp + 1_000L))

        then: "output concentration predictions are updated"
        Double firstM
        Double firstN
        Long firstStartTimestampM
        Long firstStartTimestampN
        conditions.eventually {
            def predictedM = getPredictedDatapoints(assetPredictedDatapointService, outputAssetM.id, "predictedConcentration")
            def predictedN = getPredictedDatapoints(assetPredictedDatapointService, outputAssetN.id, "predictedConcentration")

            assert predictedM.size() == 49
            assert predictedN.size() == 49

            firstM = getRepresentativeValue(predictedM)
            firstN = getRepresentativeValue(predictedN)
            firstStartTimestampM = predictedM*.timestamp.min() as Long
            firstStartTimestampN = predictedN*.timestamp.min() as Long

            assert firstM != null
            assert firstN != null
            assert firstM > 0d
            assert firstN > 0d
            assert firstM > firstN
            assert hasVariation(predictedM)
        }

        when: "source stays above threshold"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(sourceAsset.id, EnvironmentSensorAsset.PM2_5.name, 1300, baseTimestamp + 2_000L))

        then: "crossing logic does not re-trigger"
        conditions.eventually {
            def predictedM = getPredictedDatapoints(assetPredictedDatapointService, outputAssetM.id, "predictedConcentration")
            def predictedN = getPredictedDatapoints(assetPredictedDatapointService, outputAssetN.id, "predictedConcentration")

            assert predictedM.size() == 49
            assert predictedN.size() == 49
            assert getRepresentativeValue(predictedM) == firstM
            assert getRepresentativeValue(predictedN) == firstN
            assert (predictedM*.timestamp.min() as Long) == firstStartTimestampM
            assert (predictedN*.timestamp.min() as Long) == firstStartTimestampN
        }

        when: "source drops below and crosses above again"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(sourceAsset.id, EnvironmentSensorAsset.PM2_5.name, 200, baseTimestamp + 3_000L))
        conditions.eventually {
            assert getAttributeInteger(assetStorageService, sourceAsset.id, EnvironmentSensorAsset.PM2_5.name) == 200
        }
        assetProcessingService.sendAttributeEvent(new AttributeEvent(sourceAsset.id, EnvironmentSensorAsset.PM2_5.name, 1100, baseTimestamp + 4_000L))

        then: "a new propagation run refreshes predictions"
        conditions.eventually {
            def predictedM = getPredictedDatapoints(assetPredictedDatapointService, outputAssetM.id, "predictedConcentration")
            def predictedN = getPredictedDatapoints(assetPredictedDatapointService, outputAssetN.id, "predictedConcentration")
            def secondM = getRepresentativeValue(predictedM)
            def secondN = getRepresentativeValue(predictedN)

            assert predictedM.size() == 49
            assert predictedN.size() == 49

            assert secondM != null
            assert secondN != null
            assert secondM < firstM
            assert secondN < firstN
            assert (predictedM*.timestamp.min() as Long) > firstStartTimestampM
            assert (predictedN*.timestamp.min() as Long) > firstStartTimestampN
        }
    }

    protected static List<ValueDatapoint> getPredictedDatapoints(AssetPredictedDatapointService assetPredictedDatapointService,
                                                                 String assetId,
                                                                 String attributeName) {
        return assetPredictedDatapointService.getDatapoints(new AttributeRef(assetId, attributeName))
    }

    protected static Double getRepresentativeValue(List<ValueDatapoint> datapoints) {
        if (datapoints == null || datapoints.isEmpty()) {
            return null
        }

        def value = datapoints[0].value
        return value instanceof Number ? ((Number) value).doubleValue() : null
    }

    protected static boolean hasVariation(List<ValueDatapoint> datapoints) {
        List<Double> numericValues = datapoints
            .collect { it?.value instanceof Number ? ((Number) it.value).doubleValue() : null }
            .findAll { it != null }

        if (numericValues.size() < 2) {
            return false
        }

        return (numericValues.max() - numericValues.min()) > 1e-9d
    }

    protected static Integer getAttributeInteger(AssetStorageService assetStorageService, String assetId, String attributeName) {
        return assetStorageService.find(assetId, true)
            .getAttribute(attributeName)
            .flatMap { it.getValue(Integer.class) }
            .orElse(null)
    }
}
