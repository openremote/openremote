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
package org.openremote.extension.demosetup;

import org.openremote.agent.protocol.dispersion.PollutantDispersionAgent;
import org.openremote.agent.protocol.dispersion.PollutantDispersionAgentLink;
import org.openremote.agent.protocol.dispersion.PollutantDispersionLinkRole;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.EnvironmentSensorAsset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.NumberPredicate;
import org.openremote.model.query.filter.RealmPredicate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.util.MapAccess.getBoolean;
import static org.openremote.model.util.MapAccess.getInteger;
import static org.openremote.model.util.MapAccess.getString;
import static org.openremote.model.value.MetaItemType.AGENT_LINK;
import static org.openremote.model.value.MetaItemType.HAS_PREDICTED_DATA_POINTS;
import static org.openremote.model.value.MetaItemType.READ_ONLY;
import static org.openremote.model.value.MetaItemType.RULE_STATE;
import static org.openremote.model.value.MetaItemType.STORE_DATA_POINTS;
import static org.openremote.model.value.ValueType.NUMBER;

public class RotterdamDispersionSetup extends org.openremote.manager.setup.ManagerSetup {

    public static final String OR_SETUP_DISPERSION_ROTTERDAM = "OR_SETUP_DISPERSION_ROTTERDAM";
    public static final String OR_SETUP_DISPERSION_REALM = "OR_SETUP_DISPERSION_REALM";
    public static final String OR_SETUP_DISPERSION_PM25_TRIGGER = "OR_SETUP_DISPERSION_PM25_TRIGGER";
    public static final String OR_SETUP_DISPERSION_INTERVAL_MINUTES = "OR_SETUP_DISPERSION_INTERVAL_MINUTES";
    public static final String OR_SETUP_DISPERSION_HORIZON_HOURS = "OR_SETUP_DISPERSION_HORIZON_HOURS";

    protected static final String ROOT_ASSET_NAME = "Rotterdam Dispersion Demo";
    protected static final String WEATHER_ASSET_NAME = "Rotterdam Weather K";
    protected static final String SOURCE_ASSET_NAME = "Rotterdam Source L (PM2.5)";
    protected static final String RECEPTOR_M_ASSET_NAME = "Rotterdam Receptor M";
    protected static final String RECEPTOR_N_ASSET_NAME = "Rotterdam Receptor N";
    protected static final String RECEPTOR_ATTRIBUTE_NAME = "predictedPollutantConcentration";

    protected final Container container;

    public RotterdamDispersionSetup(Container container) {
        super(container);
        this.container = container;
    }

    @Override
    public void onStart() {
        if (!getBoolean(container.getConfig(), OR_SETUP_DISPERSION_ROTTERDAM, false)) {
            return;
        }

        String realm = getString(container.getConfig(), OR_SETUP_DISPERSION_REALM, MASTER_REALM);
        int triggerThreshold = getInteger(container.getConfig(), OR_SETUP_DISPERSION_PM25_TRIGGER, 999);
        int predictionIntervalMinutes = Math.max(1, getInteger(container.getConfig(), OR_SETUP_DISPERSION_INTERVAL_MINUTES, 30));
        int predictionHorizonHours = Math.max(1, getInteger(container.getConfig(), OR_SETUP_DISPERSION_HORIZON_HOURS, 24));

        Asset<?> existingRoot = assetStorageService.find(
            new AssetQuery().types(ThingAsset.class).names(ROOT_ASSET_NAME).realm(new RealmPredicate(realm))
        );

        if (existingRoot != null) {
            Asset<?> existingWeather = assetStorageService.find(
                new AssetQuery()
                    .types(WeatherAsset.class)
                    .parents(existingRoot.getId())
                    .names(WEATHER_ASSET_NAME)
                    .realm(new RealmPredicate(realm))
            );

            if (existingWeather instanceof WeatherAsset weatherAsset) {
                weatherAsset.getAttribute(WeatherAsset.WIND_SPEED).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
                weatherAsset.getAttribute(WeatherAsset.WIND_DIRECTION).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
                weatherAsset.getAttribute(WeatherAsset.CLOUD_COVERAGE).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
                weatherAsset.getAttribute(WeatherAsset.SUN_ALTITUDE).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
                weatherAsset = assetStorageService.merge(weatherAsset);
                seedWeatherPredictedDatapoints(weatherAsset, predictionIntervalMinutes, predictionHorizonHours);
            }

            ensureExistingAssetMeta(existingRoot, realm);

            LOG.log(System.Logger.Level.INFO,
                "Rotterdam dispersion setup already exists in realm '" + realm + "': " + existingRoot.getId());
            return;
        }

        ThingAsset root = new ThingAsset(ROOT_ASSET_NAME)
            .setRealm(realm);
        root = assetStorageService.merge(root);

        WeatherAsset weatherAsset = new WeatherAsset(WEATHER_ASSET_NAME)
            .setRealm(realm)
            .setParent(root)
            .setLocation(new GeoJSONPoint(4.47917d, 51.92442d));
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.WIND_SPEED).setValue(18d);
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.WIND_DIRECTION).setValue(270);
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.CLOUD_COVERAGE).setValue(25);
        weatherAsset.getAttributes().getOrCreate(WeatherAsset.SUN_ALTITUDE).setValue(35d);
        weatherAsset.getAttribute(WeatherAsset.WIND_SPEED).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
        weatherAsset.getAttribute(WeatherAsset.WIND_DIRECTION).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
        weatherAsset.getAttribute(WeatherAsset.CLOUD_COVERAGE).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
        weatherAsset.getAttribute(WeatherAsset.SUN_ALTITUDE).ifPresent(attribute -> attribute.addOrReplaceMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true), new MetaItem<>(STORE_DATA_POINTS, true)));
        weatherAsset = assetStorageService.merge(weatherAsset);
        seedWeatherPredictedDatapoints(weatherAsset, predictionIntervalMinutes, predictionHorizonHours);

        PollutantDispersionAgent dispersionAgent = new PollutantDispersionAgent("Rotterdam Pollutant Dispersion Agent")
            .setRealm(realm)
            .setParent(root)
            .setWeatherAssetId(weatherAsset.getId())
            .setSourceHeightMeters(20d)
            .setReceptorHeightMeters(1.5d)
            .setEmissionScaleFactor(1d)
            .setMinWindSpeedMs(0.5d)
            .setStabilityClass("AUTO")
            .setPredictionIntervalMinutes(predictionIntervalMinutes)
            .setPredictionHorizonHours(predictionHorizonHours);
        dispersionAgent = assetStorageService.merge(dispersionAgent);
        final String dispersionAgentId = dispersionAgent.getId();

        EnvironmentSensorAsset sourceAsset = new EnvironmentSensorAsset(SOURCE_ASSET_NAME)
            .setRealm(realm)
            .setParent(root)
            .setLocation(new GeoJSONPoint(4.44900d, 51.90900d));

        sourceAsset.getAttribute(EnvironmentSensorAsset.PM2_5).ifPresent(attribute ->
            attribute.addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK,
                    new PollutantDispersionAgentLink(dispersionAgentId)
                        .setRole(PollutantDispersionLinkRole.SOURCE_TRIGGER)
                        .setTriggerPredicate(new NumberPredicate(triggerThreshold, AssetQuery.Operator.GREATER_THAN))
                ),
                new MetaItem<>(STORE_DATA_POINTS, true),
                new MetaItem<>(RULE_STATE, true)
            )
        );
        sourceAsset.getAttribute(EnvironmentSensorAsset.PM2_5).ifPresent(attribute -> attribute.getMeta().remove(READ_ONLY));
        sourceAsset.getAttributes().getOrCreate(EnvironmentSensorAsset.PM2_5).setValue(Math.max(0, triggerThreshold - 100));
        sourceAsset = assetStorageService.merge(sourceAsset);

        ThingAsset receptorM = createReceptor(
            realm,
            root,
            RECEPTOR_M_ASSET_NAME,
            new GeoJSONPoint(4.48900d, 51.90900d),
            dispersionAgentId
        );
        receptorM = assetStorageService.merge(receptorM);

        ThingAsset receptorN = createReceptor(
            realm,
            root,
            RECEPTOR_N_ASSET_NAME,
            new GeoJSONPoint(4.47800d, 51.92000d),
            dispersionAgentId
        );
        receptorN = assetStorageService.merge(receptorN);

        LOG.log(System.Logger.Level.INFO,
            "Created Rotterdam dispersion setup in realm '" + realm + "' with assets: "
                + "weather=" + weatherAsset.getId() + ", source=" + sourceAsset.getId() + ", receptorM=" + receptorM.getId() + ", receptorN=" + receptorN.getId());
        LOG.log(System.Logger.Level.INFO,
            "To trigger predictions, set source attribute '" + EnvironmentSensorAsset.PM2_5.getName() + "' above " + triggerThreshold +
                " on asset '" + sourceAsset.getName() + "'.");
    }

    protected void seedWeatherPredictedDatapoints(WeatherAsset weatherAsset, int predictionIntervalMinutes, int predictionHorizonHours) {
        int intervalMinutes = Math.max(1, predictionIntervalMinutes);
        int horizonHours = Math.max(1, predictionHorizonHours);
        int datapointCount = ((horizonHours * 60) / intervalMinutes) + 1;
        long intervalMillis = intervalMinutes * 60_000L;

        ZonedDateTime now = timerService.getNow().atZone(ZoneId.systemDefault());
        int minuteBucket = now.getMinute() / intervalMinutes;
        ZonedDateTime rounded = now.withMinute(minuteBucket * intervalMinutes).withSecond(0).withNano(0);
        long startTimestamp = rounded.toInstant().toEpochMilli();

        List<ValueDatapoint<?>> windSpeedForecast = new ArrayList<>(datapointCount);
        List<ValueDatapoint<?>> windDirectionForecast = new ArrayList<>(datapointCount);
        List<ValueDatapoint<?>> cloudCoverageForecast = new ArrayList<>(datapointCount);
        List<ValueDatapoint<?>> sunAltitudeForecast = new ArrayList<>(datapointCount);

        for (int i = 0; i < datapointCount; i++) {
            long timestamp = startTimestamp + (i * intervalMillis);
            double dayFraction = (double) i / Math.max(1d, datapointCount - 1d);

            double windSpeedKmh = 18d + (10d * Math.sin((dayFraction * 2d * Math.PI) - 0.8d)) + (6d * Math.sin(dayFraction * 6d * Math.PI));
            windSpeedKmh = Math.max(4d, Math.min(42d, windSpeedKmh));

            double windDirection = 210d + (40d * Math.sin(dayFraction * 2d * Math.PI)) + (20d * Math.sin((dayFraction * 4d * Math.PI) + 0.7d));
            windDirection = ((windDirection % 360d) + 360d) % 360d;

            int cloudCoverage = (int) Math.round(45d + (30d * Math.sin((dayFraction * 2d * Math.PI) + 1.4d)) + (10d * Math.sin(dayFraction * 8d * Math.PI)));
            cloudCoverage = Math.max(0, Math.min(100, cloudCoverage));

            double sunAltitude = 55d * Math.sin((dayFraction * 2d * Math.PI) - (Math.PI / 2d));
            sunAltitude = Math.max(-20d, Math.min(65d, sunAltitude));

            windSpeedForecast.add(new ValueDatapoint<>(timestamp, windSpeedKmh));
            windDirectionForecast.add(new ValueDatapoint<>(timestamp, windDirection));
            cloudCoverageForecast.add(new ValueDatapoint<>(timestamp, cloudCoverage));
            sunAltitudeForecast.add(new ValueDatapoint<>(timestamp, sunAltitude));
        }

        assetPredictedDatapointService.purgeValues(weatherAsset.getId(), WeatherAsset.WIND_SPEED.getName());
        assetPredictedDatapointService.purgeValues(weatherAsset.getId(), WeatherAsset.WIND_DIRECTION.getName());
        assetPredictedDatapointService.purgeValues(weatherAsset.getId(), WeatherAsset.CLOUD_COVERAGE.getName());
        assetPredictedDatapointService.purgeValues(weatherAsset.getId(), WeatherAsset.SUN_ALTITUDE.getName());

        assetPredictedDatapointService.updateValues(weatherAsset.getId(), WeatherAsset.WIND_SPEED.getName(), windSpeedForecast);
        assetPredictedDatapointService.updateValues(weatherAsset.getId(), WeatherAsset.WIND_DIRECTION.getName(), windDirectionForecast);
        assetPredictedDatapointService.updateValues(weatherAsset.getId(), WeatherAsset.CLOUD_COVERAGE.getName(), cloudCoverageForecast);
        assetPredictedDatapointService.updateValues(weatherAsset.getId(), WeatherAsset.SUN_ALTITUDE.getName(), sunAltitudeForecast);

        LOG.log(System.Logger.Level.INFO,
            "Seeded weather predicted datapoints for asset '" + weatherAsset.getName() + "'"
                + ": intervalMinutes=" + intervalMinutes
                + ", horizonHours=" + horizonHours
                + ", datapoints=" + datapointCount);
    }

    protected ThingAsset createReceptor(String realm, Asset<?> parent, String name, GeoJSONPoint location, String dispersionAgentId) {
        return new ThingAsset(name)
            .setRealm(realm)
            .setParent(parent)
            .setLocation(location)
            .addOrReplaceAttributes(
                new Attribute<>(RECEPTOR_ATTRIBUTE_NAME, NUMBER)
                    .addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK,
                            new PollutantDispersionAgentLink(dispersionAgentId)
                                .setRole(PollutantDispersionLinkRole.OUTPUT_CONCENTRATION)
                        ),
                        new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true),
                        new MetaItem<>(STORE_DATA_POINTS, true),
                        new MetaItem<>(READ_ONLY, true)
                    )
            );
    }

    protected void ensureExistingAssetMeta(Asset<?> root, String realm) {
        Asset<?> source = assetStorageService.find(
            new AssetQuery().types(EnvironmentSensorAsset.class).parents(root.getId()).names(SOURCE_ASSET_NAME).realm(new RealmPredicate(realm))
        );

        if (source instanceof EnvironmentSensorAsset sourceAsset) {
            sourceAsset.getAttribute(EnvironmentSensorAsset.PM2_5).ifPresent(attribute -> {
                attribute.getMeta().remove(READ_ONLY);
                attribute.addOrReplaceMeta(new MetaItem<>(STORE_DATA_POINTS, true), new MetaItem<>(RULE_STATE, true));
            });
            assetStorageService.merge(sourceAsset);
        }

        ensureExistingReceptorMeta(root, realm, RECEPTOR_M_ASSET_NAME);
        ensureExistingReceptorMeta(root, realm, RECEPTOR_N_ASSET_NAME);
    }

    protected void ensureExistingReceptorMeta(Asset<?> root, String realm, String receptorName) {
        Asset<?> receptorAsset = assetStorageService.find(
            new AssetQuery().types(ThingAsset.class).parents(root.getId()).names(receptorName).realm(new RealmPredicate(realm))
        );

        if (!(receptorAsset instanceof ThingAsset thingAsset)) {
            return;
        }

        thingAsset.getAttribute(RECEPTOR_ATTRIBUTE_NAME).ifPresent(attribute ->
            attribute.addOrReplaceMeta(
                new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true),
                new MetaItem<>(STORE_DATA_POINTS, true)
            )
        );
        assetStorageService.merge(thingAsset);
    }
}
