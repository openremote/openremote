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

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

import java.util.Optional;

import static org.openremote.model.Constants.UNITS_GRAM;
import static org.openremote.model.Constants.UNITS_METRE;
import static org.openremote.model.Constants.UNITS_PER;
import static org.openremote.model.Constants.UNITS_SECOND;

@Entity
public class PollutantDispersionAgent extends Agent<PollutantDispersionAgent, PollutantDispersionProtocol, PollutantDispersionAgentLink> {

    public static final AgentDescriptor<PollutantDispersionAgent, PollutantDispersionProtocol, PollutantDispersionAgentLink> DESCRIPTOR =
        new AgentDescriptor<>(PollutantDispersionAgent.class, PollutantDispersionProtocol.class, PollutantDispersionAgentLink.class);

    public static final AttributeDescriptor<String> WEATHER_ASSET_ID =
        new AttributeDescriptor<>("weatherAssetId", ValueType.ASSET_ID);

    public static final AttributeDescriptor<Double> SOURCE_HEIGHT_METERS =
        new AttributeDescriptor<>("sourceHeightMeters", ValueType.POSITIVE_NUMBER).withOptional(true).withUnits(UNITS_METRE);

    public static final AttributeDescriptor<Double> RECEPTOR_HEIGHT_METERS =
        new AttributeDescriptor<>("receptorHeightMeters", ValueType.POSITIVE_NUMBER).withOptional(true).withUnits(UNITS_METRE);

    public static final AttributeDescriptor<Double> EMISSION_RATE_GRAMS_PER_SECOND =
        new AttributeDescriptor<>("emissionRateGramsPerSecond", ValueType.POSITIVE_NUMBER)
            .withOptional(true)
            .withUnits(UNITS_GRAM, UNITS_PER, UNITS_SECOND);

    public static final AttributeDescriptor<Double> EMISSION_SCALE_FACTOR =
        new AttributeDescriptor<>("emissionScaleFactor", ValueType.POSITIVE_NUMBER).withOptional(true);

    public static final AttributeDescriptor<Double> MIN_WIND_SPEED_MS =
        new AttributeDescriptor<>("minWindSpeedMs", ValueType.POSITIVE_NUMBER).withOptional(true);

    public static final AttributeDescriptor<String> STABILITY_CLASS =
        new AttributeDescriptor<>("stabilityClass", ValueType.TEXT)
            .withOptional(true)
            .withConstraints(new ValueConstraint.AllowedValues("AUTO", "A", "B", "C", "D", "E", "F"));

    public static final AttributeDescriptor<Integer> PREDICTION_INTERVAL_MINUTES =
        new AttributeDescriptor<>("predictionIntervalMinutes", ValueType.POSITIVE_INTEGER).withOptional(true);

    public static final AttributeDescriptor<Integer> PREDICTION_HORIZON_HOURS =
        new AttributeDescriptor<>("predictionHorizonHours", ValueType.POSITIVE_INTEGER).withOptional(true);

    public static final AttributeDescriptor<Integer> PREDICTION_DATAPOINT_COUNT =
        new AttributeDescriptor<>("predictionDatapointCount", ValueType.POSITIVE_INTEGER).withOptional(true);

    protected PollutantDispersionAgent() {
    }

    public PollutantDispersionAgent(String name) {
        super(name);
    }

    public Optional<String> getWeatherAssetId() {
        return getAttributes().getValue(WEATHER_ASSET_ID);
    }

    public PollutantDispersionAgent setWeatherAssetId(String value) {
        getAttributes().getOrCreate(WEATHER_ASSET_ID).setValue(value);
        return this;
    }

    public Optional<Double> getSourceHeightMeters() {
        return getAttributes().getValue(SOURCE_HEIGHT_METERS);
    }

    public PollutantDispersionAgent setSourceHeightMeters(Double value) {
        getAttributes().getOrCreate(SOURCE_HEIGHT_METERS).setValue(value);
        return this;
    }

    public Optional<Double> getReceptorHeightMeters() {
        return getAttributes().getValue(RECEPTOR_HEIGHT_METERS);
    }

    public PollutantDispersionAgent setReceptorHeightMeters(Double value) {
        getAttributes().getOrCreate(RECEPTOR_HEIGHT_METERS).setValue(value);
        return this;
    }

    public Optional<Double> getEmissionScaleFactor() {
        return getAttributes().getValue(EMISSION_SCALE_FACTOR);
    }

    public PollutantDispersionAgent setEmissionScaleFactor(Double value) {
        getAttributes().getOrCreate(EMISSION_SCALE_FACTOR).setValue(value);
        return this;
    }

    public Optional<Double> getEmissionRateGramsPerSecond() {
        return getAttributes().getValue(EMISSION_RATE_GRAMS_PER_SECOND);
    }

    public PollutantDispersionAgent setEmissionRateGramsPerSecond(Double value) {
        getAttributes().getOrCreate(EMISSION_RATE_GRAMS_PER_SECOND).setValue(value);
        return this;
    }

    public Optional<Double> getMinWindSpeedMs() {
        return getAttributes().getValue(MIN_WIND_SPEED_MS);
    }

    public PollutantDispersionAgent setMinWindSpeedMs(Double value) {
        getAttributes().getOrCreate(MIN_WIND_SPEED_MS).setValue(value);
        return this;
    }

    public Optional<String> getStabilityClass() {
        return getAttributes().getValue(STABILITY_CLASS);
    }

    public PollutantDispersionAgent setStabilityClass(String value) {
        getAttributes().getOrCreate(STABILITY_CLASS).setValue(value);
        return this;
    }

    public Optional<Integer> getPredictionIntervalMinutes() {
        return getAttributes().getValue(PREDICTION_INTERVAL_MINUTES);
    }

    public PollutantDispersionAgent setPredictionIntervalMinutes(Integer value) {
        getAttributes().getOrCreate(PREDICTION_INTERVAL_MINUTES).setValue(value);
        return this;
    }

    public Optional<Integer> getPredictionHorizonHours() {
        return getAttributes().getValue(PREDICTION_HORIZON_HOURS);
    }

    public PollutantDispersionAgent setPredictionHorizonHours(Integer value) {
        getAttributes().getOrCreate(PREDICTION_HORIZON_HOURS).setValue(value);
        return this;
    }

    public Optional<Integer> getPredictionDatapointCount() {
        return getAttributes().getValue(PREDICTION_DATAPOINT_COUNT);
    }

    public PollutantDispersionAgent setPredictionDatapointCount(Integer value) {
        getAttributes().getOrCreate(PREDICTION_DATAPOINT_COUNT).setValue(value);
        return this;
    }

    @Override
    public PollutantDispersionProtocol getProtocolInstance() {
        return new PollutantDispersionProtocol(this);
    }
}
