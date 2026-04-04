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
package org.openremote.agent.protocol.dispersion;

import org.geotools.referencing.GeodeticCalculator;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.query.filter.ValuePredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class PollutantDispersionProtocol extends AbstractProtocol<PollutantDispersionAgent, PollutantDispersionAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "PollutantDispersion";

    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, PollutantDispersionProtocol.class);

    protected static final double DEFAULT_SOURCE_HEIGHT_METERS = 5d;
    protected static final double DEFAULT_RECEPTOR_HEIGHT_METERS = 1.5d;
    protected static final double DEFAULT_EMISSION_SCALE_FACTOR = 1d;
    protected static final double DEFAULT_MIN_WIND_SPEED_MS = 0.5d;
    protected static final int DEFAULT_PREDICTION_INTERVAL_MINUTES = 30;
    protected static final int DEFAULT_PREDICTION_HORIZON_HOURS = 24;
    protected static final int MAX_PREDICTION_DATAPOINTS = 5000;

    protected final Map<AttributeRef, Boolean> sourcePredicateMatchMap = new ConcurrentHashMap<>();

    protected record WeatherSnapshot(double windSpeedMs,
                                     double windDirectionDeg,
                                     DispersionStabilityClass stabilityClass,
                                     Integer cloudCoverage,
                                     Double sunAltitude) {
    }

    protected record WeatherDataContext(WeatherSnapshot current,
                                        String weatherAssetId,
                                        List<ValueDatapoint<?>> predictedWindSpeedKmh,
                                        List<ValueDatapoint<?>> predictedWindDirection,
                                        List<ValueDatapoint<?>> predictedCloudCoverage,
                                        List<ValueDatapoint<?>> predictedSunAltitude) {
    }

    protected record RelativePosition(double eastMeters, double northMeters) {
    }

    protected record PredictionConfig(long startTimestampMillis, long intervalMillis, int datapointCount) {
    }

    public PollutantDispersionProtocol(PollutantDispersionAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "dispersion://" + agent.getId();
    }

    @Override
    protected void doStart(Container container) {
        setConnectionStatus(ConnectionStatus.CONNECTED);
        LOG.info(
            "Dispersion protocol started for agent='" + agent.getId()
                + "' weatherAssetId='" + agent.getWeatherAssetId().orElse("<unset>")
                + "' intervalMinutes=" + agent.getPredictionIntervalMinutes().orElse(DEFAULT_PREDICTION_INTERVAL_MINUTES)
                + " horizonHours=" + agent.getPredictionHorizonHours().orElse(DEFAULT_PREDICTION_HORIZON_HOURS)
                + " datapointCountOverride=" + agent.getPredictionDatapointCount().orElse(0)
        );
    }

    @Override
    protected void doStop(Container container) {
        LOG.info("Dispersion protocol stopping for agent='" + agent.getId() + "'");
        sourcePredicateMatchMap.clear();
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, PollutantDispersionAgentLink agentLink) {
        if (agentLink.getRole() != PollutantDispersionLinkRole.SOURCE_TRIGGER) {
            return;
        }
        sourcePredicateMatchMap.remove(new AttributeRef(assetId, attribute.getName()));
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, PollutantDispersionAgentLink agentLink) {
        if (agentLink.getRole() != PollutantDispersionLinkRole.SOURCE_TRIGGER) {
            return;
        }
        sourcePredicateMatchMap.remove(new AttributeRef(assetId, attribute.getName()));
    }

    @Override
    protected void doLinkedAttributeWrite(PollutantDispersionAgentLink agentLink, AttributeEvent event, Object processedValue) {
        updateLinkedAttribute(event.getRef(), processedValue, event.getTimestamp());

        if (agentLink.getRole() != PollutantDispersionLinkRole.SOURCE_TRIGGER) {
            return;
        }

        boolean predicateMatch = evaluateTriggerPredicate(agentLink.getTriggerPredicate().orElse(null), processedValue);
        Boolean previousPredicateMatch = sourcePredicateMatchMap.put(event.getRef(), predicateMatch);

        LOG.info(
            "Dispersion source update for " + event.getRef()
                + ": value=" + processedValue
                + ", predicateMatch=" + predicateMatch
                + ", previousMatch=" + previousPredicateMatch
        );

        if (!predicateMatch) {
            LOG.info("Dispersion not triggered for " + event.getRef() + " because predicate is false");
            return;
        }

        if (previousPredicateMatch != null && previousPredicateMatch) {
            LOG.info("Dispersion not triggered for " + event.getRef() + " because no false->true crossing occurred");
            return;
        }

        Optional<Double> sourceValue = ValueUtil.getValueCoerced(processedValue, Double.class);

        if (sourceValue.isEmpty()) {
            LOG.info("Dispersion propagation skipped because source attribute value is not numeric: " + event.getRef());
            return;
        }

        LOG.info("Dispersion triggered for " + event.getRef() + " with sourceValue=" + sourceValue.get());

        doPropagation(event.getRef(), sourceValue.get(), event.getTimestamp());
    }

    protected boolean evaluateTriggerPredicate(ValuePredicate predicate, Object value) {
        if (predicate == null) {
            return true;
        }

        try {
            return predicate.asPredicate(() -> timerService.getCurrentTimeMillis()).test(value);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to evaluate trigger predicate", e);
            return false;
        }
    }

    protected void doPropagation(AttributeRef sourceAttributeRef, double sourceValue, long timestamp) {
        if (predictedDatapointService == null) {
            LOG.warning("Predicted datapoint service is unavailable, propagation is skipped");
            return;
        }

        LOG.info(
            "Dispersion propagation started for source=" + sourceAttributeRef
                + ", sourceValue=" + sourceValue
                + ", timestamp=" + timestamp
        );

        Asset<?> sourceAsset = assetService.findAsset(sourceAttributeRef.getId());

        if (sourceAsset == null) {
            LOG.info("Dispersion propagation skipped because source asset no longer exists: " + sourceAttributeRef.getId());
            return;
        }

        GeoJSONPoint sourceLocation = sourceAsset.getLocation().orElse(null);

        if (sourceLocation == null) {
            LOG.info("Dispersion propagation skipped because source asset has no location: " + sourceAttributeRef.getId());
            return;
        }

        double emissionScaleFactor = agent.getEmissionScaleFactor().orElse(DEFAULT_EMISSION_SCALE_FACTOR);
        double q = Math.max(0d, sourceValue * emissionScaleFactor);

        if (q <= 0d) {
            LOG.info("Dispersion propagation skipped because emission rate is not positive (q=" + q + ")");
            return;
        }

        double sourceHeightMeters = agent.getSourceHeightMeters().orElse(DEFAULT_SOURCE_HEIGHT_METERS);
        double receptorHeightMeters = agent.getReceptorHeightMeters().orElse(DEFAULT_RECEPTOR_HEIGHT_METERS);
        PredictionConfig predictionConfig = getPredictionConfig(timestamp);
        WeatherDataContext weatherDataContext = getWeatherDataContext(predictionConfig);

        if (weatherDataContext == null) {
            LOG.info("Dispersion propagation skipped because weather snapshot is unavailable");
            return;
        }

        WeatherSnapshot weatherSnapshot = weatherDataContext.current;

        LOG.info(
            "Dispersion run config: q=" + q
                + ", sourceHeight=" + sourceHeightMeters
                + ", receptorHeight=" + receptorHeightMeters
                + ", windSpeedMs=" + weatherSnapshot.windSpeedMs
                + ", windDirectionDeg=" + weatherSnapshot.windDirectionDeg
                + ", stabilityClass=" + weatherSnapshot.stabilityClass
                + ", datapoints=" + predictionConfig.datapointCount
                + ", intervalMillis=" + predictionConfig.intervalMillis
        );

        int outputCandidates = 0;
        int outputsWritten = 0;
        int missingOutputAssets = 0;
        int missingOutputLocations = 0;

        for (Map.Entry<AttributeRef, Attribute<?>> linkedAttribute : getLinkedAttributes().entrySet()) {
            AttributeRef attributeRef = linkedAttribute.getKey();
            Attribute<?> attribute = linkedAttribute.getValue();
            PollutantDispersionAgentLink link = agent.getAgentLink(attribute);

            if (link.getRole() != PollutantDispersionLinkRole.OUTPUT_CONCENTRATION || attributeRef.equals(sourceAttributeRef)) {
                continue;
            }

            outputCandidates++;

            Asset<?> receptorAsset = assetService.findAsset(attributeRef.getId());

            if (receptorAsset == null) {
                missingOutputAssets++;
                LOG.info("Skipping output receptor because asset does not exist: " + attributeRef);
                continue;
            }

            GeoJSONPoint receptorLocation = receptorAsset.getLocation().orElse(null);

            if (receptorLocation == null) {
                missingOutputLocations++;
                LOG.info("Skipping output receptor because asset has no location: " + attributeRef);
                continue;
            }

            List<ValueDatapoint<?>> predictedValues = buildPredictionDatapoints(
                q,
                weatherDataContext,
                sourceLocation,
                receptorLocation,
                sourceHeightMeters,
                receptorHeightMeters,
                predictionConfig
            );

            if (predictedValues.isEmpty()) {
                LOG.info("Skipping output receptor because prediction list is empty: " + attributeRef);
                continue;
            }

            predictedDatapointService.purgeValues(attributeRef.getId(), attributeRef.getName());
            predictedDatapointService.updateValues(attributeRef.getId(), attributeRef.getName(), predictedValues);
            outputsWritten++;

            ValueDatapoint<?> firstDatapoint = predictedValues.get(0);
            LOG.info(
                "Wrote " + predictedValues.size() + " predicted datapoints to " + attributeRef
                    + " firstTimestamp=" + firstDatapoint.getTimestamp()
                    + " firstValue=" + firstDatapoint.getValue()
            );
        }

        LOG.info(
            "Dispersion propagation finished for source=" + sourceAttributeRef
                + ": outputCandidates=" + outputCandidates
                + ", outputsWritten=" + outputsWritten
                + ", missingOutputAssets=" + missingOutputAssets
                + ", missingOutputLocations=" + missingOutputLocations
        );
    }

    protected PredictionConfig getPredictionConfig(long eventTimestamp) {
        int intervalMinutes = Math.max(1, agent.getPredictionIntervalMinutes().orElse(DEFAULT_PREDICTION_INTERVAL_MINUTES));
        long intervalMillis = intervalMinutes * 60_000L;

        int configuredCount = agent.getPredictionDatapointCount().orElse(0);
        int datapointCount;

        if (configuredCount > 0) {
            datapointCount = configuredCount;
        } else {
            int horizonHours = Math.max(1, agent.getPredictionHorizonHours().orElse(DEFAULT_PREDICTION_HORIZON_HOURS));
            long horizonMillis = horizonHours * 3_600_000L;
            datapointCount = (int) (horizonMillis / intervalMillis) + 1;
        }

        if (datapointCount > MAX_PREDICTION_DATAPOINTS) {
            LOG.warning("Configured prediction datapoint count exceeds max; capping to " + MAX_PREDICTION_DATAPOINTS);
            datapointCount = MAX_PREDICTION_DATAPOINTS;
        }

        long startTimestampMillis = eventTimestamp > 0 ? eventTimestamp : timerService.getCurrentTimeMillis();
        LOG.info(
            "Resolved prediction config: startTimestamp=" + startTimestampMillis
                + ", intervalMinutes=" + intervalMinutes
                + ", datapointCount=" + Math.max(1, datapointCount)
        );
        return new PredictionConfig(startTimestampMillis, intervalMillis, Math.max(1, datapointCount));
    }

    protected List<ValueDatapoint<?>> buildPredictionDatapoints(double q,
                                                                WeatherDataContext weatherDataContext,
                                                                GeoJSONPoint sourceLocation,
                                                                GeoJSONPoint receptorLocation,
                                                                double sourceHeight,
                                                                double receptorHeight,
                                                                PredictionConfig predictionConfig) {

        List<ValueDatapoint<?>> values = new ArrayList<>(predictionConfig.datapointCount);

        for (int i = 0; i < predictionConfig.datapointCount; i++) {
            long timestamp = predictionConfig.startTimestampMillis + (predictionConfig.intervalMillis * i);
            WeatherSnapshot weatherAtTimestamp = resolveWeatherSnapshotForTimestamp(weatherDataContext, timestamp);
            double concentration = calculateConcentration(
                q,
                weatherAtTimestamp,
                sourceLocation,
                receptorLocation,
                sourceHeight,
                receptorHeight
            );
            values.add(new ValueDatapoint<>(timestamp, concentration));
        }

        return values;
    }

    protected WeatherDataContext getWeatherDataContext(PredictionConfig predictionConfig) {
        String weatherAssetId = agent.getWeatherAssetId().orElse(null);

        if (weatherAssetId == null) {
            LOG.info("Weather asset ID is not configured on dispersion agent");
            return null;
        }

        Asset<?> weatherAsset = assetService.findAsset(weatherAssetId);

        if (weatherAsset == null) {
            LOG.info("Configured weather asset cannot be found: " + weatherAssetId);
            return null;
        }

        Optional<Double> windSpeedKmh = getAttributeValueAsDouble(weatherAsset, WeatherAsset.WIND_SPEED.getName());
        Optional<Integer> windDirection = getAttributeValueAsInteger(weatherAsset, WeatherAsset.WIND_DIRECTION.getName());

        if (windSpeedKmh.isEmpty() || windDirection.isEmpty()) {
            LOG.info("Weather asset misses windSpeed and/or windDirection: " + weatherAssetId);
            return null;
        }

        double windSpeedMs = Math.max(agent.getMinWindSpeedMs().orElse(DEFAULT_MIN_WIND_SPEED_MS), windSpeedKmh.get() / 3.6d);
        double windDirectionDeg = DispersionProjection.normaliseDegrees(windDirection.get());
        Optional<Integer> cloudCoverage = getAttributeValueAsInteger(weatherAsset, WeatherAsset.CLOUD_COVERAGE.getName());
        Optional<Double> sunAltitude = getAttributeValueAsDouble(weatherAsset, WeatherAsset.SUN_ALTITUDE.getName());

        DispersionStabilityClass stabilityClass = resolveStabilityClass(agent.getStabilityClass().orElse(null), windSpeedMs, cloudCoverage, sunAltitude);
        WeatherSnapshot currentWeather = new WeatherSnapshot(
            windSpeedMs,
            windDirectionDeg,
            stabilityClass,
            cloudCoverage.orElse(null),
            sunAltitude.orElse(null)
        );

        List<ValueDatapoint<?>> predictedWindSpeedKmh = queryWeatherPredictedDatapoints(weatherAssetId, WeatherAsset.WIND_SPEED.getName(), predictionConfig);
        List<ValueDatapoint<?>> predictedWindDirection = queryWeatherPredictedDatapoints(weatherAssetId, WeatherAsset.WIND_DIRECTION.getName(), predictionConfig);
        List<ValueDatapoint<?>> predictedCloudCoverage = queryWeatherPredictedDatapoints(weatherAssetId, WeatherAsset.CLOUD_COVERAGE.getName(), predictionConfig);
        List<ValueDatapoint<?>> predictedSunAltitude = queryWeatherPredictedDatapoints(weatherAssetId, WeatherAsset.SUN_ALTITUDE.getName(), predictionConfig);

        LOG.info(
            "Weather snapshot resolved from asset='" + weatherAssetId
                + "': windSpeedKmh=" + windSpeedKmh.get()
                + ", windSpeedMs=" + windSpeedMs
                + ", windDirectionDeg=" + windDirectionDeg
                + ", cloudCoverage=" + cloudCoverage.map(Object::toString).orElse("<unset>")
                + ", sunAltitude=" + sunAltitude.map(Object::toString).orElse("<unset>")
                + ", stabilityClass=" + stabilityClass
                + ", predictedWindSpeedPoints=" + predictedWindSpeedKmh.size()
                + ", predictedWindDirectionPoints=" + predictedWindDirection.size()
                + ", predictedCloudCoveragePoints=" + predictedCloudCoverage.size()
                + ", predictedSunAltitudePoints=" + predictedSunAltitude.size()
        );

        return new WeatherDataContext(
            currentWeather,
            weatherAssetId,
            predictedWindSpeedKmh,
            predictedWindDirection,
            predictedCloudCoverage,
            predictedSunAltitude
        );
    }

    protected WeatherSnapshot resolveWeatherSnapshotForTimestamp(WeatherDataContext weatherDataContext, long timestamp) {
        WeatherSnapshot current = weatherDataContext.current;

        double windSpeedMs = getNearestPredictedDoubleValue(weatherDataContext.predictedWindSpeedKmh, timestamp)
            .map(valueKmh -> Math.max(agent.getMinWindSpeedMs().orElse(DEFAULT_MIN_WIND_SPEED_MS), valueKmh / 3.6d))
            .orElse(current.windSpeedMs);

        double windDirectionDeg = getNearestPredictedDoubleValue(weatherDataContext.predictedWindDirection, timestamp)
            .map(DispersionProjection::normaliseDegrees)
            .orElse(current.windDirectionDeg);

        Integer cloudCoverage = getNearestPredictedIntegerValue(weatherDataContext.predictedCloudCoverage, timestamp)
            .orElse(current.cloudCoverage);

        Double sunAltitude = getNearestPredictedDoubleValue(weatherDataContext.predictedSunAltitude, timestamp)
            .orElse(current.sunAltitude);

        DispersionStabilityClass stabilityClass = resolveStabilityClass(
            agent.getStabilityClass().orElse(null),
            windSpeedMs,
            Optional.ofNullable(cloudCoverage),
            Optional.ofNullable(sunAltitude)
        );

        return new WeatherSnapshot(windSpeedMs, windDirectionDeg, stabilityClass, cloudCoverage, sunAltitude);
    }

    protected List<ValueDatapoint<?>> queryWeatherPredictedDatapoints(String weatherAssetId,
                                                                      String attributeName,
                                                                      PredictionConfig predictionConfig) {
        if (predictedDatapointService == null) {
            return List.of();
        }

        long endTimestamp = predictionConfig.startTimestampMillis + (predictionConfig.intervalMillis * Math.max(0, predictionConfig.datapointCount - 1));

        try {
            return predictedDatapointService.queryDatapoints(
                weatherAssetId,
                attributeName,
                new AssetDatapointAllQuery(predictionConfig.startTimestampMillis, endTimestamp)
            );
        } catch (Exception e) {
            LOG.log(Level.FINE, "Failed to query predicted weather datapoints for " + weatherAssetId + "/" + attributeName, e);
            return List.of();
        }
    }

    protected Optional<Double> getNearestPredictedDoubleValue(List<ValueDatapoint<?>> datapoints, long timestamp) {
        return getNearestPredictedDatapoint(datapoints, timestamp)
            .flatMap(datapoint -> ValueUtil.getValueCoerced(datapoint.getValue(), Double.class));
    }

    protected Optional<Integer> getNearestPredictedIntegerValue(List<ValueDatapoint<?>> datapoints, long timestamp) {
        return getNearestPredictedDatapoint(datapoints, timestamp)
            .flatMap(datapoint -> ValueUtil.getValueCoerced(datapoint.getValue(), Integer.class));
    }

    protected Optional<ValueDatapoint<?>> getNearestPredictedDatapoint(List<ValueDatapoint<?>> datapoints, long timestamp) {
        if (datapoints == null || datapoints.isEmpty()) {
            return Optional.empty();
        }

        ValueDatapoint<?> nearest = null;
        long nearestDistance = Long.MAX_VALUE;

        for (ValueDatapoint<?> datapoint : datapoints) {
            long distance = Math.abs(datapoint.getTimestamp() - timestamp);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = datapoint;
            }
        }

        return Optional.ofNullable(nearest);
    }

    protected DispersionStabilityClass resolveStabilityClass(String configuredClass,
                                                             double windSpeedMs,
                                                             Optional<Integer> cloudCoverage,
                                                             Optional<Double> sunAltitude) {

        DispersionStabilityClass parsedConfiguredClass = parseStabilityClass(configuredClass);

        if (parsedConfiguredClass != null) {
            return parsedConfiguredClass;
        }

        if (cloudCoverage.isEmpty() || sunAltitude.isEmpty()) {
            return DispersionStabilityClass.D;
        }

        int cloud = Math.max(0, Math.min(100, cloudCoverage.get()));
        boolean day = sunAltitude.get() > 0d;

        if (day) {
            if (windSpeedMs < 2d) {
                if (cloud < 25) {
                    return DispersionStabilityClass.A;
                }
                if (cloud < 75) {
                    return DispersionStabilityClass.B;
                }
                return DispersionStabilityClass.C;
            }

            if (windSpeedMs < 3d) {
                if (cloud < 25) {
                    return DispersionStabilityClass.B;
                }
                if (cloud < 75) {
                    return DispersionStabilityClass.C;
                }
                return DispersionStabilityClass.D;
            }

            if (windSpeedMs < 5d) {
                if (cloud < 25) {
                    return DispersionStabilityClass.C;
                }
                return DispersionStabilityClass.D;
            }

            return DispersionStabilityClass.D;
        }

        if (cloud < 50) {
            if (windSpeedMs < 3d) {
                return DispersionStabilityClass.F;
            }
            if (windSpeedMs < 5d) {
                return DispersionStabilityClass.E;
            }
        }

        return DispersionStabilityClass.D;
    }

    protected DispersionStabilityClass parseStabilityClass(String value) {
        if (value == null || value.isBlank() || "AUTO".equalsIgnoreCase(value)) {
            return null;
        }

        try {
            return DispersionStabilityClass.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            LOG.fine("Invalid configured stability class, falling back to auto mode: " + value);
            return null;
        }
    }

    protected double calculateConcentration(double q,
                                            WeatherSnapshot weather,
                                            GeoJSONPoint sourceLocation,
                                            GeoJSONPoint receptorLocation,
                                            double sourceHeight,
                                            double receptorHeight) {

        RelativePosition relativePosition = calculateRelativePosition(sourceLocation, receptorLocation);

        DispersionProjection.PlumeCoordinates plumeCoordinates = DispersionProjection.projectToPlume(
            relativePosition.eastMeters,
            relativePosition.northMeters,
            weather.windDirectionDeg
        );

        double concentration = GaussianPlumeModel.concentration(
            q,
            weather.windSpeedMs,
            plumeCoordinates.downwindMeters(),
            plumeCoordinates.crosswindMeters(),
            receptorHeight,
            sourceHeight,
            weather.stabilityClass
        );

        if (!Double.isFinite(concentration) || concentration < 0d) {
            return 0d;
        }

        return concentration;
    }

    protected RelativePosition calculateRelativePosition(GeoJSONPoint source, GeoJSONPoint receptor) {
        GeodeticCalculator calculator = new GeodeticCalculator();
        calculator.setStartingGeographicPoint(source.getX(), source.getY());
        calculator.setDestinationGeographicPoint(receptor.getX(), receptor.getY());

        double distanceMeters = calculator.getOrthodromicDistance();
        double azimuthRad = Math.toRadians(calculator.getAzimuth());
        double eastMeters = distanceMeters * Math.sin(azimuthRad);
        double northMeters = distanceMeters * Math.cos(azimuthRad);

        return new RelativePosition(eastMeters, northMeters);
    }

    protected static Optional<Double> getAttributeValueAsDouble(Asset<?> asset, String attributeName) {
        return asset.getAttribute(attributeName).flatMap(attribute -> attribute.getValue(Double.class));
    }

    protected static Optional<Integer> getAttributeValueAsInteger(Asset<?> asset, String attributeName) {
        return asset.getAttribute(attributeName).flatMap(attribute -> attribute.getValue(Integer.class));
    }

}
