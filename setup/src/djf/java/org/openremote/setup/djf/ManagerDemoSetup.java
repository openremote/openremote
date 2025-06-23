/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.setup.djf;

import org.openremote.agent.protocol.http.HTTPAgent;
import org.openremote.agent.protocol.http.HTTPAgentLink;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.security.Realm;
import org.openremote.model.value.*;

import java.time.Duration;
import java.util.function.Supplier;

import static org.openremote.model.Constants.*;
import static org.openremote.model.value.MetaItemType.*;

public class ManagerDemoSetup extends ManagerSetup {

    public String realmMasterName;

    public ManagerDemoSetup(Container container) {
        super(container);
    }

    protected ThermostatAsset createDemoThermostatAsset(String name, Asset<?> parent, GeoJSONPoint location,
                                                        int soilTensionMin, int soilTensionMax, Supplier<AgentLink<?>> agentLinker) {
        ThermostatAsset thermostatAsset = new ThermostatAsset(name);
        thermostatAsset.setParent(parent);
        // thermostatAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        // thermostatAsset.getAttributes().getOrCreate(ThermostatAsset.SOIL_TENSION_MEASURED)
        //         .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
        //                 new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(READ_ONLY));
        // thermostatAsset.getAttributes().getOrCreate(ThermostatAsset.SOIL_TENSION_MIN)
        //         .addMeta(new MetaItem<>(RULE_STATE))
        //         .setValue(soilTensionMin);
        // thermostatAsset.getAttributes().getOrCreate(ThermostatAsset.SOIL_TENSION_MAX)
        //         .addMeta(new MetaItem<>(RULE_STATE))
        //         .setValue(soilTensionMax);
        // thermostatAsset.getAttributes().getOrCreate(ThermostatAsset.TEMPERATURE)
        //         .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
        //                 new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(READ_ONLY));
        // thermostatAsset.getAttributes().getOrCreate(ThermostatAsset.SALINITY)
        //         .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
        //                 new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(READ_ONLY));

        return thermostatAsset;
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Realm realmMaster = keycloakDemoSetup.realmMaster;
        realmMasterName = realmMaster.getName();

        // ################################ Realm master ###################################

        // SimulatorAgent smartcitySimulatorAgent = new SimulatorAgent("Simulator agent");
        // smartcitySimulatorAgent.setRealm(this.realmMaster);

        // smartcitySimulatorAgent = assetStorageService.merge(smartcitySimulatorAgent);
        // smartcitySimulatorAgentId = smartcitySimulatorAgent.getId();

        // LocalTime midnight = LocalTime.of(0, 0);

        // ################################ Realm master - thermostat ###################################

        ThermostatAsset thermostatAsset = new ThermostatAsset("Thermostat");
        thermostatAsset.setRealm(realmMasterName);
        for (int i = 0; i < 100; i++) {
            thermostatAsset.getAttributes().addOrReplace(
                new Attribute<>("temperature" + i, ValueType.NUMBER)
                    .addOrReplaceMeta(
                        // new MetaItem<>(MetaItemType.AGENT_LINK, ),
                        // new MetaItem<>(MetaItemType.ATTRIBUTE_LINKS, {}),
                        new MetaItem<>(MetaItemType.ACCESS_PUBLIC_READ, true),
                        new MetaItem<>(MetaItemType.ACCESS_PUBLIC_WRITE, true),
                        new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true),
                        new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE, true),
                        new MetaItem<>(MetaItemType.READ_ONLY, true),
                        new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),
                        new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 100),
                        new MetaItem<>(MetaItemType.HAS_PREDICTED_DATA_POINTS, true),
                        // new MetaItem<>(MetaItemType.FORECAST, ),
                        new MetaItem<>(MetaItemType.RULE_STATE, true),
                        new MetaItem<>(MetaItemType.RULE_RESET_IMMEDIATE, true),
                        new MetaItem<>(MetaItemType.LABEL, ""),
                        // new MetaItem<>(MetaItemType.FORMAT, ),
                        new MetaItem<>(MetaItemType.UNITS, Constants.unitsUnits.CELSIUS, Constants.Units.FAHRENHEIT, Constants.Units.KELVIN)),
                        // new MetaItem<>(MetaItemType.CONSTRAINTS, {}),
                        new MetaItem<>(MetaItemType.SECRET, true),
                        new MetaItem<>(MetaItemType.MULTILINE, true),
                        new MetaItem<>(MetaItemType.SHOW_ON_DASHBOARD, true),
                        new MetaItem<>(MetaItemType.MOMENTARY, true)
                    )
            );
        }
        thermostatAsset.setId(UniqueIdentifierGenerator.generateId(thermostatAsset.getName()));
        thermostatAsset = assetStorageService.merge(thermostatAsset);

        // ### Weather ###
        // HTTPAgent weatherHttpApiAgent = new HTTPAgent("Weather Agent");
        // weatherHttpApiAgent.setParent(energyManagement);
        // weatherHttpApiAgent.setBaseURI("https://api.openweathermap.org/data/2.5/");

        // MultivaluedStringMap queryParams = new MultivaluedStringMap();
        // queryParams.put("appid", Collections.singletonList("c3ecbf09be5267cd280676a01acd3360"));
        // queryParams.put("lat", Collections.singletonList("51.918849"));
        // queryParams.put("lon", Collections.singletonList("4.463250"));
        // queryParams.put("units", Collections.singletonList("metric"));
        // weatherHttpApiAgent.setRequestQueryParameters(queryParams);

        // MultivaluedStringMap headers = new MultivaluedStringMap();
        // headers.put("Accept", Collections.singletonList("application/json"));
        // weatherHttpApiAgent.setRequestHeaders(headers);

        // weatherHttpApiAgent = assetStorageService.merge(weatherHttpApiAgent);
        // weatherHttpApiAgentId = weatherHttpApiAgent.getId();

        // WeatherAsset weather = new WeatherAsset("Weather");
        // weather.setParent(energyManagement);
        // weather.setId(UniqueIdentifierGenerator.generateId(weather.getName()));

        // HTTPAgentLink agentLink = new HTTPAgentLink(weatherHttpApiAgentId);
        // agentLink.setPath("weather");
        // agentLink.setPollingMillis((int)halfHourInMillis);

        // weather.getAttributes().addOrReplace(
        //         new Attribute<>("currentWeather")
        //                 .addMeta(
        //                         new MetaItem<>(MetaItemType.AGENT_LINK, agentLink),
        //                         new MetaItem<>(MetaItemType.LABEL, "Open Weather Map API weather end point"),
        //                         new MetaItem<>(MetaItemType.READ_ONLY, true),
        //                         new MetaItem<>(MetaItemType.STORE_DATA_POINTS, false),
        //                         new MetaItem<>(MetaItemType.RULE_STATE, false),
        //                         new MetaItem<>(MetaItemType.ATTRIBUTE_LINKS, new AttributeLink[] {
        //                             createWeatherApiAttributeLink(weather.getId(), "main", "temp", "temperature"),
        //                             createWeatherApiAttributeLink(weather.getId(), "main", "humidity", "humidity"),
        //                             createWeatherApiAttributeLink(weather.getId(), "wind", "speed", "windSpeed"),
        //                             createWeatherApiAttributeLink(weather.getId(), "wind", "deg", "windDirection")
        //                         })
        //                 ));
        // weather.getAttribute("windSpeed").ifPresent(assetAttribute -> {
        //     assetAttribute.addMeta(
        //         new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
        //         new MetaItem<>(MetaItemType.RULE_STATE)
        //     );
        // });
        // weather.getAttribute("temperature").ifPresent(assetAttribute -> {
        //     assetAttribute.addMeta(
        //         new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
        //         new MetaItem<>(MetaItemType.RULE_STATE)
        //     );
        // });
        // weather.getAttribute("windDirection").ifPresent(assetAttribute -> {
        //     assetAttribute.addMeta(
        //         new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
        //         new MetaItem<>(MetaItemType.RULE_STATE)
        //     );
        // });
        // weather.getAttribute("humidity").ifPresent(assetAttribute -> {
        //     assetAttribute.addMeta(
        //         new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
        //         new MetaItem<>(MetaItemType.RULE_STATE)
        //     );
        // });
        // new Attribute<>(Asset.LOCATION, new GeoJSONPoint(4.463250, 51.918849));
        // weather = assetStorageService.merge(weather);

        // ################################ Link users and assets ###################################

        // assetStorageService.storeUserAssetLinks(Arrays.asList(
        //         new UserAssetLink(this.realmManufacturerName,
        //                 KeycloakDemoSetup.customerUserId,
        //                 paprikaId),
        //         new UserAssetLink(this.realmManufacturerName,
        //                 KeycloakDemoSetup.customerUserId,
        //                 irrigation9Id),
        //         new UserAssetLink(this.realmManufacturerName,
        //                 KeycloakDemoSetup.customerUserId,
        //                 irrigation10Id),
        //         new UserAssetLink(this.realmManufacturerName,
        //                 KeycloakDemoSetup.customerUserId,
        //                 irrigation11Id),
        //         new UserAssetLink(this.realmManufacturerName,
        //                 KeycloakDemoSetup.customerUserId,
        //                 harvestRobot5Id),
        //         new UserAssetLink(this.realmManufacturerName,
        //                 KeycloakDemoSetup.customerUserId,
        //                 soilSensor4Id)));

        // ################################ Make user restricted ###################################
        // ManagerIdentityProvider identityProvider = identityService.getIdentityProvider();
        // identityProvider.updateUserRealmRoles(realmManufacturer.getName(), KeycloakDemoSetup.customerUserId, identityProvider
        //         .addRealmRoles(realmManufacturer.getName(),
        //                 KeycloakDemoSetup.customerUserId, RESTRICTED_USER_REALM_ROLE));
    }
}
