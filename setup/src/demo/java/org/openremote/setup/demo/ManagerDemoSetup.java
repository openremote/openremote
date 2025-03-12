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
package org.openremote.setup.demo;

import org.openremote.agent.protocol.http.HTTPAgent;
import org.openremote.agent.protocol.http.HTTPAgentLink;
import org.openremote.agent.protocol.simulator.SimulatorAgent;
import org.openremote.agent.protocol.simulator.SimulatorAgentLink;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.manager.security.ManagerIdentityProvider;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeLink;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.security.Realm;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.value.*;
import org.openremote.setup.demo.model.HarvestRobotAsset;
import org.openremote.setup.demo.model.HarvestRobotAsset.OperationMode;
import org.openremote.setup.demo.model.HarvestRobotAsset.VegetableType;
import org.openremote.setup.demo.model.IrrigationAsset;
import org.openremote.setup.demo.model.SoilSensorAsset;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.function.Supplier;

import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static org.openremote.model.Constants.*;
import static org.openremote.model.value.MetaItemType.*;
import static org.openremote.model.value.ValueType.MultivaluedStringMap;

public class ManagerDemoSetup extends ManagerSetup {

    public static GeoJSONPoint STATIONSPLEIN_LOCATION = new GeoJSONPoint(4.470175, 51.923464);
    public String realmMasterName;
    public String realmCityName;
    public String realmManufacturerName;
    public String area1Id;
    public String smartcitySimulatorAgentId;
    public String manufacturerSimulatorAgentId;

    public String paprikaId;
    public String irrigation9Id;
    public String harvestRobot5Id;
    public String irrigation10Id;
    public String irrigation11Id;
    public String soilSensor4Id;

    private final long halfHourInMillis = Duration.ofMinutes(30).toMillis();

    public ManagerDemoSetup(Container container) {
        super(container);
    }

    private static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    // ################################ Realm manufacturer methods ###################################
    protected HarvestRobotAsset createDemoHarvestRobotAsset(String name, Asset<?> parent, GeoJSONPoint location,
            OperationMode operationMode, VegetableType vegetableType, int direction, int harvestedTotal, Supplier<AgentLink<?>> agentLinker) {
        HarvestRobotAsset harvestRobotAsset = new HarvestRobotAsset(name);
        harvestRobotAsset.setParent(parent);
        harvestRobotAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        harvestRobotAsset.getAttributes().getOrCreate(HarvestRobotAsset.OPERATION_MODE)
                .addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY))
                .setValue(operationMode);
        harvestRobotAsset.getAttributes().getOrCreate(HarvestRobotAsset.VEGETABLE_TYPE)
                .addMeta(new MetaItem<>(RULE_STATE))
                .setValue(vegetableType);
        harvestRobotAsset.getAttributes().getOrCreate(HarvestRobotAsset.DIRECTION)
                .addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY))
                .setValue(direction);;
        harvestRobotAsset.getAttributes().getOrCreate(HarvestRobotAsset.SPEED)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
                        new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY));
        harvestRobotAsset.getAttributes().getOrCreate(HarvestRobotAsset.HARVESTED_SESSION)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
                        new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY));
        harvestRobotAsset.getAttributes().getOrCreate(HarvestRobotAsset.HARVESTED_TOTAL)
                .addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY))
                .setValue(harvestedTotal);;

        return harvestRobotAsset;
    }

    protected IrrigationAsset createDemoIrrigationAsset(String name, Asset<?> parent, GeoJSONPoint location, Supplier<AgentLink<?>> agentLinker) {
        IrrigationAsset irrigationAsset = new IrrigationAsset(name);
        irrigationAsset.setParent(parent);
        irrigationAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        irrigationAsset.getAttributes().getOrCreate(IrrigationAsset.FLOW_WATER)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
                        new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY));
        irrigationAsset.getAttributes().getOrCreate(IrrigationAsset.FLOW_NUTRIENTS)
                .addMeta(new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY));
        irrigationAsset.getAttributes().getOrCreate(IrrigationAsset.FLOW_TOTAL)
                .addMeta(new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY));
        irrigationAsset.getAttributes().getOrCreate(IrrigationAsset.TANK_LEVEL)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
                        new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY));

        return irrigationAsset;
    }
    protected SoilSensorAsset createDemoSoilSensorAsset(String name, Asset<?> parent, GeoJSONPoint location,
                                                        int soilTensionMin, int soilTensionMax, Supplier<AgentLink<?>> agentLinker) {
        SoilSensorAsset soilSensorAsset = new SoilSensorAsset(name);
        soilSensorAsset.setParent(parent);
        soilSensorAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        soilSensorAsset.getAttributes().getOrCreate(SoilSensorAsset.SOIL_TENSION_MEASURED)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
                        new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(READ_ONLY));
        soilSensorAsset.getAttributes().getOrCreate(SoilSensorAsset.SOIL_TENSION_MIN)
                .addMeta(new MetaItem<>(RULE_STATE))
                .setValue(soilTensionMin);
        soilSensorAsset.getAttributes().getOrCreate(SoilSensorAsset.SOIL_TENSION_MAX)
                .addMeta(new MetaItem<>(RULE_STATE))
                .setValue(soilTensionMax);
        soilSensorAsset.getAttributes().getOrCreate(SoilSensorAsset.TEMPERATURE)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
                        new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(READ_ONLY));
        soilSensorAsset.getAttributes().getOrCreate(SoilSensorAsset.SALINITY)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()),
                        new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS), new MetaItem<>(READ_ONLY));

        return soilSensorAsset;
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Realm realmMaster = keycloakDemoSetup.realmMaster;
        Realm realmCity = keycloakDemoSetup.realmCity;
        Realm realmManufacturer = KeycloakDemoSetup.realmManufacturer;
        realmMasterName = realmMaster.getName();
        this.realmCityName = realmCity.getName();
        realmManufacturerName = realmManufacturer.getName();

        // ################################ Realm smartcity ###################################

        SimulatorAgent smartcitySimulatorAgent = new SimulatorAgent("Simulator agent");
        smartcitySimulatorAgent.setRealm(this.realmCityName);

        smartcitySimulatorAgent = assetStorageService.merge(smartcitySimulatorAgent);
        smartcitySimulatorAgentId = smartcitySimulatorAgent.getId();

        LocalTime midnight = LocalTime.of(0, 0);

        // ################################ Realm smartcity - Environment monitor ###################################

        Asset<?> environmentMonitor = new ThingAsset("Environment monitor");
        environmentMonitor.setRealm(this.realmCityName);
        environmentMonitor.setId(UniqueIdentifierGenerator.generateId(environmentMonitor.getName()));
        environmentMonitor = assetStorageService.merge(environmentMonitor);

        EnvironmentSensorAsset environment1Asset = createDemoEnvironmentAsset("Oudehaven", environmentMonitor, new GeoJSONPoint(4.49313, 51.91885), () ->
            new SimulatorAgentLink(smartcitySimulatorAgentId));
        EnvironmentSensorAsset environment2Asset = createDemoEnvironmentAsset("Kaappark", environmentMonitor, new GeoJSONPoint(4.480434, 51.899287), () ->
            new SimulatorAgentLink(smartcitySimulatorAgentId));
        EnvironmentSensorAsset environment3Asset = createDemoEnvironmentAsset("Museumpark", environmentMonitor, new GeoJSONPoint(4.472457, 51.912047), () ->
            new SimulatorAgentLink(smartcitySimulatorAgentId));
        EnvironmentSensorAsset environment4Asset = createDemoEnvironmentAsset("Eendrachtsplein", environmentMonitor, new GeoJSONPoint(4.473599, 51.916292), () ->
            new SimulatorAgentLink(smartcitySimulatorAgentId));

        EnvironmentSensorAsset[] environmentArray = {environment1Asset, environment2Asset, environment3Asset, environment4Asset};
        for (EnvironmentSensorAsset asset : environmentArray) {
            asset.setManufacturer("Intemo");
            asset.setModel("Josene outdoor");
            asset.getAttribute(EnvironmentSensorAsset.OZONE).ifPresent(assetAttribute -> {
                assetAttribute.addMeta(
                        new MetaItem<>(
                                MetaItemType.AGENT_LINK,
                                new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                        new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), getRandomNumberInRange(80,90)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), getRandomNumberInRange(75,90)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), getRandomNumberInRange(75,90)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), getRandomNumberInRange(75,90)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), getRandomNumberInRange(75,95)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), getRandomNumberInRange(75,95)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), getRandomNumberInRange(75,95)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), getRandomNumberInRange(80,95)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), getRandomNumberInRange(80,95)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), getRandomNumberInRange(80,110)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), getRandomNumberInRange(85,110)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), getRandomNumberInRange(85,115)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), getRandomNumberInRange(85,115)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), getRandomNumberInRange(85,115)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), getRandomNumberInRange(105,120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), getRandomNumberInRange(105,125)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), getRandomNumberInRange(110,125)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), getRandomNumberInRange(90,120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), getRandomNumberInRange(90,115)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), getRandomNumberInRange(90,110)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), getRandomNumberInRange(80,95)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), getRandomNumberInRange(80,95)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), getRandomNumberInRange(80,90)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), getRandomNumberInRange(80,90))
                                    }
                                )                            
                        )
                );
            });
            asset.setId(UniqueIdentifierGenerator.generateId(asset.getName()));
            asset = assetStorageService.merge(asset);
        }

        GroundwaterSensorAsset groundwater1Asset = createDemoGroundwaterAsset("Leuvehaven", environmentMonitor, new GeoJSONPoint(4.48413, 51.91431));
        GroundwaterSensorAsset groundwater2Asset = createDemoGroundwaterAsset("Steiger", environmentMonitor, new GeoJSONPoint(4.482887, 51.920082));
        GroundwaterSensorAsset groundwater3Asset = createDemoGroundwaterAsset("Stadhuis", environmentMonitor, new GeoJSONPoint(4.480876, 51.923212));

        GroundwaterSensorAsset[] groundwaterArray = {groundwater1Asset, groundwater2Asset, groundwater3Asset};
        for (GroundwaterSensorAsset asset : groundwaterArray) {
            asset.setManufacturer("Eijkelkamp");
            asset.setModel("TeleControlNet");
            asset.getAttribute(GroundwaterSensorAsset.SOIL_TEMPERATURE).ifPresent(assetAttribute -> {
                assetAttribute.addMeta(
                        new MetaItem<>(
                                MetaItemType.AGENT_LINK,
                                new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                        new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 12.2),
                                        new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 12.1),
                                        new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 12.0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 11.8),
                                        new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 11.7),
                                        new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 11.7),
                                        new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 11.9),
                                        new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 12.1),
                                        new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 12.8),
                                        new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 13.5),
                                        new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 13.9),
                                        new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 15.2),
                                        new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 15.3),
                                        new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 15.5),
                                        new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 15.5),
                                        new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 15.4),
                                        new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 15.2),
                                        new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 15.2),
                                        new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 14.6),
                                        new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 14.2),
                                        new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 13.8),
                                        new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 13.4),
                                        new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 12.8),
                                        new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 12.3)
                                    }
                                )
                        )
                );
            });
            asset.getAttribute(GroundwaterSensorAsset.WATER_LEVEL).ifPresent(assetAttribute -> {
                assetAttribute.addMeta(
                        new MetaItem<>(
                                MetaItemType.AGENT_LINK,
                                new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                        new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), getRandomNumberInRange(100, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), getRandomNumberInRange(100, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), getRandomNumberInRange(90, 110)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), getRandomNumberInRange(100, 110)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), getRandomNumberInRange(100, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120)),
                                        new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), getRandomNumberInRange(110, 120))
                                    }
                                )
                        )
                );
            });
            asset.setId(UniqueIdentifierGenerator.generateId(asset.getName()));
            asset = assetStorageService.merge(asset);
        }

        // ################################ Realm smartcity - Mobility and Safety ###################################

        Asset<?> mobilityAndSafety = new ThingAsset("Mobility and safety");
        mobilityAndSafety.setRealm(this.realmCityName);
        mobilityAndSafety.setId(UniqueIdentifierGenerator.generateId(mobilityAndSafety.getName()));
        mobilityAndSafety = assetStorageService.merge(mobilityAndSafety);

        // ### Parking ###

        GroupAsset parkingGroupAsset = new GroupAsset("Parking group", ParkingAsset.class);
        parkingGroupAsset.setParent(mobilityAndSafety);
        parkingGroupAsset.getAttributes().addOrReplace(
                new Attribute<>("totalOccupancy", ValueType.POSITIVE_INTEGER)
                        .addMeta(
                                new MetaItem<>(MetaItemType.UNITS, Constants.units(Constants.UNITS_PERCENTAGE)),
                                new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100))),
                                new MetaItem<>(MetaItemType.READ_ONLY),
                                new MetaItem<>(MetaItemType.RULE_STATE),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
                                ));
        parkingGroupAsset.setId(UniqueIdentifierGenerator.generateId(parkingGroupAsset.getName()));
        parkingGroupAsset = assetStorageService.merge(parkingGroupAsset);

        ParkingAsset parking1Asset = createDemoParkingAsset("Markthal", parkingGroupAsset, new GeoJSONPoint(4.48527, 51.91984))
            .setManufacturer("SKIDATA")
            .setModel("Barrier.Gate");
        parking1Asset.getAttribute(ParkingAsset.SPACES_OCCUPIED).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                new SimulatorReplayDatapoint[]{
                                    new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 34),
                                    new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 37),
                                    new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 31),
                                    new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 36),
                                    new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 32),
                                    new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 39),
                                    new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 47),
                                    new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 53),
                                    new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 165),
                                    new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 301),
                                    new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 417),
                                    new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 442),
                                    new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 489),
                                    new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 467),
                                    new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 490),
                                    new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 438),
                                    new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 457),
                                    new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 402),
                                    new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 379),
                                    new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 336),
                                    new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 257),
                                    new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 204),
                                    new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 112),
                                    new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 75)
                                }
                            )
                    )
            );
        });
        parking1Asset.setPriceHourly(3.75);
        parking1Asset.setPriceDaily(25.00);
        parking1Asset.setSpacesTotal(512);
        parking1Asset.getAttribute(ParkingAsset.SPACES_TOTAL).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                new MetaItem<>(MetaItemType.RULE_STATE));
        });
        parking1Asset.setId(UniqueIdentifierGenerator.generateId(parking1Asset.getName()));
        parking1Asset = assetStorageService.merge(parking1Asset);

        ParkingAsset parking2Asset = createDemoParkingAsset("Lijnbaan", parkingGroupAsset, new GeoJSONPoint(4.47681, 51.91849));
        parking2Asset.setManufacturer("SKIDATA");
        parking2Asset.setModel("Barrier.Gate");
        parking2Asset.getAttribute(ParkingAsset.SPACES_OCCUPIED).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                new SimulatorReplayDatapoint[]{
                                    new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 31),
                                    new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 24),
                                    new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 36),
                                    new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 38),
                                    new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 46),
                                    new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 48),
                                    new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 52),
                                    new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 89),
                                    new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 142),
                                    new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 187),
                                    new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 246),
                                    new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 231),
                                    new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 367),
                                    new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 345),
                                    new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 386),
                                    new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 312),
                                    new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 363),
                                    new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 276),
                                    new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 249),
                                    new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 256),
                                    new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 123),
                                    new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 153),
                                    new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 83),
                                    new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 25)
                                }
                            )
                    )
            );
        });
        parking2Asset.setPriceHourly(3.50);
        parking2Asset.setPriceDaily(23.00);
        parking2Asset.setSpacesTotal(390);
        parking2Asset.getAttribute(ParkingAsset.SPACES_TOTAL).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                new MetaItem<>(MetaItemType.RULE_STATE));
        });
        parking2Asset.setId(UniqueIdentifierGenerator.generateId(parking2Asset.getName()));
        parking2Asset = assetStorageService.merge(parking2Asset);

        ParkingAsset parking3Asset = createDemoParkingAsset("Erasmusbrug", parkingGroupAsset, new GeoJSONPoint(4.48207, 51.91127));
        parking3Asset.setManufacturer("Kiestra");
        parking3Asset.setModel("Genius Rainbow");
        parking3Asset.getAttribute(ParkingAsset.SPACES_OCCUPIED).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                new SimulatorReplayDatapoint[]{
                                    new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 25),
                                    new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 23),
                                    new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 23),
                                    new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 21),
                                    new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 18),
                                    new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 13),
                                    new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 29),
                                    new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 36),
                                    new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 119),
                                    new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 257),
                                    new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 357),
                                    new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 368),
                                    new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 362),
                                    new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 349),
                                    new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 370),
                                    new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 367),
                                    new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 355),
                                    new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 314),
                                    new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 254),
                                    new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 215),
                                    new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 165),
                                    new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 149),
                                    new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 108),
                                    new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 47)
                                }
                            )
                    )
            );
        });
        parking3Asset.setPriceHourly(3.40);
        parking3Asset.setPriceDaily(20.00);
        parking3Asset.setSpacesTotal(373);
        parking3Asset.getAttribute(ParkingAsset.SPACES_TOTAL).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                new MetaItem<>(MetaItemType.RULE_STATE));
            });
        parking3Asset.setId(UniqueIdentifierGenerator.generateId(parking3Asset.getName()));
        parking3Asset = assetStorageService.merge(parking3Asset);

        // ### Crowd control ###

        ThingAsset assetAreaStation = new ThingAsset("Stationsplein");
        assetAreaStation.setParent(mobilityAndSafety)
                .getAttributes().addOrReplace(
                        new Attribute<>(Asset.LOCATION, STATIONSPLEIN_LOCATION),
                        new Attribute<>(BuildingAsset.POSTAL_CODE, "3013 AK"),
                        new Attribute<>(BuildingAsset.CITY, "Rotterdam"),
                        new Attribute<>(BuildingAsset.COUNTRY, "Netherlands")
                );
        assetAreaStation.setId(UniqueIdentifierGenerator.generateId(assetAreaStation.getName()));
        assetAreaStation = assetStorageService.merge(assetAreaStation);
        area1Id = assetAreaStation.getId();

        PeopleCounterAsset peopleCounter1Asset = createDemoPeopleCounterAsset("People Counter South", assetAreaStation, new GeoJSONPoint(4.470147, 51.923171), () ->
            new SimulatorAgentLink(smartcitySimulatorAgentId));
        peopleCounter1Asset.setManufacturer("ViNotion");
        peopleCounter1Asset.setModel("ViSense");
        peopleCounter1Asset.getAttribute(PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                new SimulatorReplayDatapoint[]{
                                    new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 0.3),
                                    new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 0.1),
                                    new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 0.0),
                                    new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 0.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 0.4),
                                    new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 0.5),
                                    new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 0.7),
                                    new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 1.8),
                                    new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 2.1),
                                    new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 2.4),
                                    new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 1.9),
                                    new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 1.8),
                                    new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 2.1),
                                    new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 1.8),
                                    new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 1.7),
                                    new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 2.3),
                                    new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 3.1),
                                    new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 2.8),
                                    new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 2.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 1.6),
                                    new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 1.7),
                                    new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 1.1),
                                    new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 0.8)
                                }
                            )
                    )
            );
        });
        peopleCounter1Asset.setId(UniqueIdentifierGenerator.generateId(peopleCounter1Asset.getName()));
        peopleCounter1Asset = assetStorageService.merge(peopleCounter1Asset);

        Asset<?> peopleCounter2Asset = createDemoPeopleCounterAsset("People Counter North", assetAreaStation, new GeoJSONPoint(4.469329, 51.923700), () ->
            new SimulatorAgentLink(smartcitySimulatorAgentId));
        peopleCounter2Asset.setManufacturer("Axis");
        peopleCounter2Asset.setModel("P1375-E");
        peopleCounter2Asset.getAttribute(PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                new SimulatorReplayDatapoint[]{
                                    new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0.3),
                                    new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 0.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 0.3),
                                    new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 0.1),
                                    new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 0.0),
                                    new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 0.3),
                                    new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 0.7),
                                    new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 0.6),
                                    new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 1.9),
                                    new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 2.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 2.8),
                                    new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 1.6),
                                    new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 1.9),
                                    new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 2.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 1.9),
                                    new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 1.6),
                                    new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 2.4),
                                    new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 3.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 2.9),
                                    new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 2.3),
                                    new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 1.7),
                                    new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 1.4),
                                    new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 1.2),
                                    new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 0.7)
                                }
                            )
                    )
            );
        });
        peopleCounter2Asset.setId(UniqueIdentifierGenerator.generateId(peopleCounter2Asset.getName()));
        peopleCounter2Asset = assetStorageService.merge(peopleCounter2Asset);

        MicrophoneAsset microphone1Asset = createDemoMicrophoneAsset("Microphone South", assetAreaStation, new GeoJSONPoint(4.470362, 51.923201), () ->
            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                new SimulatorReplayDatapoint[] {
                    new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), getRandomNumberInRange(50,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), getRandomNumberInRange(50,55)),
                    new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), getRandomNumberInRange(50,55)),
                    new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), getRandomNumberInRange(50,55)),
                    new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), getRandomNumberInRange(60,70)),
                    new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), getRandomNumberInRange(60,70)),
                    new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), getRandomNumberInRange(50,60))
                }
            ));
        microphone1Asset.setManufacturer("Sorama");
        microphone1Asset.setModel("CAM1K");
        microphone1Asset.setId(UniqueIdentifierGenerator.generateId(microphone1Asset.getName()));
        microphone1Asset = assetStorageService.merge(microphone1Asset);

        MicrophoneAsset microphone2Asset = createDemoMicrophoneAsset("Microphone North", assetAreaStation, new GeoJSONPoint(4.469190, 51.923786), () -> 
            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                new SimulatorReplayDatapoint[] {
                    new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), getRandomNumberInRange(50,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), getRandomNumberInRange(45,50)),
                    new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), getRandomNumberInRange(50,55)),
                    new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), getRandomNumberInRange(50,55)),
                    new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), getRandomNumberInRange(50,55)),
                    new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), getRandomNumberInRange(60,70)),
                    new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), getRandomNumberInRange(55,60)),
                    new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), getRandomNumberInRange(60,70)),
                    new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), getRandomNumberInRange(60,65)),
                    new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), getRandomNumberInRange(50,60))
                }
            ));
        microphone2Asset.setManufacturer("Sorama");
        microphone2Asset.setModel("CAM1K");
        microphone2Asset.setId(UniqueIdentifierGenerator.generateId(microphone2Asset.getName()));
        microphone2Asset = assetStorageService.merge(microphone2Asset);

        LightAsset lightStation1Asset = createDemoLightAsset("Station Light NW", assetAreaStation, new GeoJSONPoint(4.468874, 51.923881));
        lightStation1Asset.setManufacturer("Philips");
        lightStation1Asset.setModel("CityTouch");
        lightStation1Asset.setId(UniqueIdentifierGenerator.generateId(lightStation1Asset.getName()));
        lightStation1Asset = assetStorageService.merge(lightStation1Asset);

        LightAsset lightStation2Asset = createDemoLightAsset("Station Light NE", assetAreaStation, new GeoJSONPoint(4.470539, 51.923991));
        lightStation2Asset.setManufacturer("Philips");
        lightStation2Asset.setModel("CityTouch");
        lightStation2Asset.setId(UniqueIdentifierGenerator.generateId(lightStation2Asset.getName()));
        lightStation2Asset = assetStorageService.merge(lightStation2Asset);

        LightAsset lightStation3Asset = createDemoLightAsset("Station Light S", assetAreaStation, new GeoJSONPoint(4.470558, 51.923186));
        lightStation3Asset.setManufacturer("Philips");
        lightStation3Asset.setModel("CityTouch");
        lightStation3Asset.setId(UniqueIdentifierGenerator.generateId(lightStation3Asset.getName()));
        lightStation3Asset = assetStorageService.merge(lightStation3Asset);

        // ### Lighting controller ###

        LightAsset lightingControllerOPAsset = createDemoLightControllerAsset("Lighting Noordereiland", mobilityAndSafety, new GeoJSONPoint(4.496177, 51.915060));
        lightingControllerOPAsset.setManufacturer("Pharos");
        lightingControllerOPAsset.setModel("LPC X");
        lightingControllerOPAsset.setId(UniqueIdentifierGenerator.generateId(lightingControllerOPAsset.getName()));
        lightingControllerOPAsset = assetStorageService.merge(lightingControllerOPAsset);

        LightAsset lightOP1Asset = createDemoLightAsset("Ons Park 1", lightingControllerOPAsset, new GeoJSONPoint(4.49626, 51.91516));
        lightOP1Asset.setManufacturer("Schréder");
        lightOP1Asset.setModel("Axia 2");
        lightOP1Asset.setId(UniqueIdentifierGenerator.generateId(lightOP1Asset.getName()));
        lightOP1Asset = assetStorageService.merge(lightOP1Asset);

        LightAsset lightOP2Asset = createDemoLightAsset("Ons Park 2", lightingControllerOPAsset, new GeoJSONPoint(4.49705, 51.91549));
        lightOP2Asset.setManufacturer("Schréder");
        lightOP2Asset.setModel("Axia 2");
        lightOP2Asset.setId(UniqueIdentifierGenerator.generateId(lightOP2Asset.getName()));
        lightOP2Asset = assetStorageService.merge(lightOP2Asset);

        LightAsset lightOP3Asset = createDemoLightAsset("Ons Park 3", lightingControllerOPAsset, new GeoJSONPoint(4.49661, 51.91495));
        lightOP3Asset.setManufacturer("Schréder");
        lightOP3Asset.setModel("Axia 2");
        lightOP3Asset.setId(UniqueIdentifierGenerator.generateId(lightOP3Asset.getName()));
        lightOP3Asset = assetStorageService.merge(lightOP3Asset);

        LightAsset lightOP4Asset = createDemoLightAsset("Ons Park 4", lightingControllerOPAsset, new GeoJSONPoint(4.49704, 51.91520));
        lightOP4Asset.setManufacturer("Schréder");
        lightOP4Asset.setModel("Axia 2");
        lightOP4Asset.setId(UniqueIdentifierGenerator.generateId(lightOP4Asset.getName()));
        lightOP4Asset = assetStorageService.merge(lightOP4Asset);

        LightAsset lightOP5Asset = createDemoLightAsset("Ons Park 5", lightingControllerOPAsset, new GeoJSONPoint(4.49758, 51.91440));
        lightOP5Asset.setManufacturer("Schréder");
        lightOP5Asset.setModel("Axia 2");
        lightOP5Asset.setId(UniqueIdentifierGenerator.generateId(lightOP5Asset.getName()));
        lightOP5Asset = assetStorageService.merge(lightOP5Asset);

        LightAsset lightOP6Asset = createDemoLightAsset("Ons Park 6", lightingControllerOPAsset, new GeoJSONPoint(4.49786, 51.91452));
        lightOP6Asset.setManufacturer("Schréder");
        lightOP6Asset.setModel("Axia 2");
        lightOP6Asset.setId(UniqueIdentifierGenerator.generateId(lightOP6Asset.getName()));
        lightOP6Asset = assetStorageService.merge(lightOP6Asset);

        // ### Ships ###

        GroupAsset shipGroupAsset = new GroupAsset("Ship group", ShipAsset.class);
        shipGroupAsset.setParent(mobilityAndSafety);
        shipGroupAsset.setId(UniqueIdentifierGenerator.generateId(shipGroupAsset.getName()));
        shipGroupAsset = assetStorageService.merge(shipGroupAsset);

        ShipAsset ship1Asset = createDemoShipAsset("Hotel New York", shipGroupAsset, new GeoJSONPoint(4.482669, 51.916436));
        ship1Asset.setLength(12);
        ship1Asset.setShipType("Passenger");
        ship1Asset.setIMONumber(9183527);
        ship1Asset.setMSSINumber(244650537);
        ship1Asset.getAttribute(ShipAsset.DIRECTION).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                new MetaItem<>(
                    MetaItemType.AGENT_LINK,
                    new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                        new SimulatorReplayDatapoint[]{
                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 7),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 187),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 7)
                        }
                    )
                )
            );
        });
        ship1Asset.getAttribute(Asset.LOCATION).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                new MetaItem<>(
                    MetaItemType.AGENT_LINK,
                    new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                        new SimulatorReplayDatapoint[]{
                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(5).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(10).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(15).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(20).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(25).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), new GeoJSONPoint(4.484374, 51.903518)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(35).get(SECOND_OF_DAY), new GeoJSONPoint(4.479779, 51.904404)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(40).get(SECOND_OF_DAY), new GeoJSONPoint(4.482914, 51.906769)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(45).get(SECOND_OF_DAY), new GeoJSONPoint(4.486156, 51.908570)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(50).get(SECOND_OF_DAY), new GeoJSONPoint(4.483362, 51.911897)),
                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(55).get(SECOND_OF_DAY), new GeoJSONPoint(4.482669, 51.916436))
                        }
                    )
                )
            );
        });
        ship1Asset.setId(UniqueIdentifierGenerator.generateId(ship1Asset.getName()));
        ship1Asset = assetStorageService.merge(ship1Asset);

        // ################################ Realm Manufaturer Simulator ###################################

        SimulatorAgent manufacturerSimulatorAgent = new SimulatorAgent("Simulator");
        manufacturerSimulatorAgent.setRealm(this.realmManufacturerName);

        manufacturerSimulatorAgent = assetStorageService.merge(manufacturerSimulatorAgent);
        manufacturerSimulatorAgentId = manufacturerSimulatorAgent.getId();
        
        // ################################ Manufacturer realm assets ###################################
        
        // ### Greenhouse equipment distribution ###

        Asset<?> distributor1 = new ThingAsset("GreenEquipment Distribution");
        distributor1.setRealm(this.realmManufacturerName);
        distributor1.setId(UniqueIdentifierGenerator.generateId(distributor1.getName()));
        distributor1 = assetStorageService.merge(distributor1);

        BuildingAsset vegetablesAndMore = new BuildingAsset("Vegetables & More");
        vegetablesAndMore.setParent(distributor1);
        vegetablesAndMore.setId(UniqueIdentifierGenerator.generateId(vegetablesAndMore.getName()));
        vegetablesAndMore = assetStorageService.merge(vegetablesAndMore);

        HarvestRobotAsset harvestRobot1 = createDemoHarvestRobotAsset("Robot 1", vegetablesAndMore, new GeoJSONPoint(4.279166, 51.978078), OperationMode.CUTTING, VegetableType.BELL_PEPPER, 26, 45, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        harvestRobot1.setId(UniqueIdentifierGenerator.generateId(harvestRobot1.getName()));
        harvestRobot1 = assetStorageService.merge(harvestRobot1);   
        HarvestRobotAsset harvestRobot2 = createDemoHarvestRobotAsset("Robot 2", vegetablesAndMore, new GeoJSONPoint(4.277852, 51.977487), OperationMode.SCANNING, VegetableType.BELL_PEPPER, 26, 45, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        harvestRobot2.setId(UniqueIdentifierGenerator.generateId(harvestRobot2.getName()));
        harvestRobot2 = assetStorageService.merge(harvestRobot2); 
        SoilSensorAsset soilSensor1 = createDemoSoilSensorAsset("Water sensor", vegetablesAndMore, new GeoJSONPoint(4.279010, 51.977391), 41, 53, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        soilSensor1.setId(UniqueIdentifierGenerator.generateId(soilSensor1.getName()));
        soilSensor1 = assetStorageService.merge(soilSensor1);

        BuildingAsset moreauHorticulture = new BuildingAsset("Qoreau Horticulture");
        moreauHorticulture.setParent(distributor1);
        moreauHorticulture.setId(UniqueIdentifierGenerator.generateId(moreauHorticulture.getName()));
        moreauHorticulture = assetStorageService.merge(moreauHorticulture);

        IrrigationAsset irrigation1 = createDemoIrrigationAsset("Soil drip 1", moreauHorticulture, new GeoJSONPoint(4.311493, 51.961554), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        IrrigationAsset irrigation2 = createDemoIrrigationAsset("Soil drip 2", moreauHorticulture, new GeoJSONPoint(4.310893, 51.961354), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        IrrigationAsset irrigation3 = createDemoIrrigationAsset("Soil drip 3", moreauHorticulture, new GeoJSONPoint(4.310293, 51.961154), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        IrrigationAsset irrigation4 = createDemoIrrigationAsset("Soil drip 4", moreauHorticulture, new GeoJSONPoint(4.309617, 51.960975), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        IrrigationAsset irrigation5 = createDemoIrrigationAsset("Soil drip 5", moreauHorticulture, new GeoJSONPoint(4.309153, 51.960901), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        irrigation1.setId(UniqueIdentifierGenerator.generateId(irrigation1.getName()));
        irrigation1 = assetStorageService.merge(irrigation1);
        irrigation2.setId(UniqueIdentifierGenerator.generateId(irrigation2.getName()));
        irrigation2 = assetStorageService.merge(irrigation2);
        irrigation3.setId(UniqueIdentifierGenerator.generateId(irrigation3.getName()));
        irrigation3 = assetStorageService.merge(irrigation3);
        irrigation4.setId(UniqueIdentifierGenerator.generateId(irrigation4.getName()));
        irrigation4 = assetStorageService.merge(irrigation4);
        irrigation5.setId(UniqueIdentifierGenerator.generateId(irrigation5.getName()));
        irrigation5 = assetStorageService.merge(irrigation5);
        SoilSensorAsset soilSensor2 = createDemoSoilSensorAsset("Soil measurement", moreauHorticulture, new GeoJSONPoint(4.310436, 51.961354), 27, 35, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        soilSensor2.setId(UniqueIdentifierGenerator.generateId(soilSensor2.getName()));
        soilSensor2 = assetStorageService.merge(soilSensor2);

        BuildingAsset paprika = new BuildingAsset("Paprika Perfect BV");
        paprika.setParent(distributor1);
        paprika.getAttributes().getOrCreate("flowPerMeter", ValueType.NUMBER)
                .addMeta(new MetaItem<>(READ_ONLY), new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS),
                        new MetaItem<>(UNITS, Constants.units(UNITS_LITRE, UNITS_PER, UNITS_METRE, UNITS_SQUARED, UNITS_HOUR)));
        paprika.getAttribute(BuildingAsset.AREA).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(RULE_STATE))
                    .setValue(1800);});
        paprika.getAttributes().stream().forEach(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ), new MetaItem<>(ACCESS_RESTRICTED_WRITE));});
        paprika.setId(UniqueIdentifierGenerator.generateId(paprika.getName()));
        paprikaId = paprika.getId();
        paprika = assetStorageService.merge(paprika);

        HarvestRobotAsset harvestRobot5 = createDemoHarvestRobotAsset("Harvest Robot 1", paprika, new GeoJSONPoint(4.282415, 51.975951), OperationMode.UNLOADING, VegetableType.TOMATO, 26, 45, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        harvestRobot5.getAttributes().stream().forEach(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ), new MetaItem<>(ACCESS_RESTRICTED_WRITE));});
        harvestRobot5.getAttribute(HarvestRobotAsset.HARVESTED_SESSION).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 814),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 814),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 815),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 816),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 816),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 816),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 816),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 817),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 818),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 819),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 820),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 821),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 34),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 86),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 112),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 156),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 289),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 348),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 456),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 516),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 624),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 684),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 713),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 762),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 787),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 792),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 798),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 804),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 808),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 812),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 813),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 813),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 814),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 814)
                                    }
                            )
                    )
            );
        });
        harvestRobot5.getAttribute(HarvestRobotAsset.SPEED).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 1)
                                    }
                            )
                    )
            );
        });
        harvestRobot5.setId(UniqueIdentifierGenerator.generateId(harvestRobot5.getName()));
        harvestRobot5Id = harvestRobot5.getId();
        harvestRobot5 = assetStorageService.merge(harvestRobot5);

        IrrigationAsset irrigation9 = createDemoIrrigationAsset("Irrigation 1", paprika, new GeoJSONPoint(4.283731, 51.976526), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        irrigation9.getAttributes().stream().forEach(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ), new MetaItem<>(ACCESS_RESTRICTED_WRITE));});
        irrigation9.getAttribute(IrrigationAsset.TANK_LEVEL).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 1000),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 995),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 990),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 986),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 980),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 975),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 970),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 965),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 960),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 956),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 950),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 945),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 941),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 936),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 929),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 925),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 919),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 914),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 910),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 906),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 900),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 895),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 891),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 1200),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 1195),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 1190),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 1184),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 1173),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 1145),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 1105),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 1074),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 1032),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 1027),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 1022),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 1017),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 1006)
                                    }
                            )
                    )
            );
        });
        irrigation9.getAttribute(IrrigationAsset.FLOW_WATER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 10.5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 10.8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 11.3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 11.9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 12.5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 10.8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 9.6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 9.4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 10.4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 11.8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 12.3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 12.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 11.3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 11.7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 10.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 10.3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 10.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 10.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 10.1)
                                    }
                            )
                    )
            );
        });
        irrigation9.setId(UniqueIdentifierGenerator.generateId(irrigation9.getName()));
        irrigation9Id = irrigation9.getId();
        irrigation9 = assetStorageService.merge(irrigation9);
        IrrigationAsset irrigation10 = createDemoIrrigationAsset("Irrigation 2", paprika, new GeoJSONPoint(4.285047, 51.975652), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        irrigation10.getAttributes().stream().forEach(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ), new MetaItem<>(ACCESS_RESTRICTED_WRITE));});
        irrigation10.getAttribute(IrrigationAsset.TANK_LEVEL).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 1200),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 1193),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 1185),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 1178),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 1170),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 1162),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 1155),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 1148),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 1140),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 1132),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 1124),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 1117),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 1110),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 1102),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 1093),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 1085),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 1076),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 1070),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 1063),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 1055),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 1048),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 1041),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 1035),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 1028),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 1022),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 1021),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 1018),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 1015),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 1010),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 1002),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 995),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 1050),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 1150),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 1290),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 1250),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 1210)
                                    }
                            )
                    )
            );
        });
        irrigation10.getAttribute(IrrigationAsset.FLOW_WATER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 12.5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 12.6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 14.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 13.3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 14),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 14.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 14.5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 12.8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 13.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 11.6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 11.4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 11.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 12.3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 12.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 13.8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 12.3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 13.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 14.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 13.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 12.9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 13.7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 11.8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 12.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 12.6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 12.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 12.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 12.1)
                                    }
                            )
                    )
            );
        });
        irrigation10.setId(UniqueIdentifierGenerator.generateId(irrigation10.getName()));
        irrigation10Id = irrigation10.getId();
        irrigation10 = assetStorageService.merge(irrigation10);
        IrrigationAsset irrigation11 = createDemoIrrigationAsset("Irrigation 3", paprika, new GeoJSONPoint(4.286504, 51.974613), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        irrigation11.getAttributes().stream().forEach(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ), new MetaItem<>(ACCESS_RESTRICTED_WRITE));});
        irrigation11.getAttribute(IrrigationAsset.TANK_LEVEL).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 1210),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 1190),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 1180),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 1172),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 1160),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 1152),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 1145),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 1141),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 1132),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 1125),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 1120),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 1110),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 1100),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 1102),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 1083),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 1071),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 1066),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 1060),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 1055),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 1035),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 1028),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 1021),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 1015),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 1006),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 1002),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 1001),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 997),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 985),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 985),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 982),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 975),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 1040),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 1200),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 1250),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 1250),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 1220)
                                    }
                            )
                    )
            );
        });
        irrigation11.getAttribute(IrrigationAsset.FLOW_WATER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 13.5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 14.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 13.5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 15),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 14.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 13.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 15.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 15),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 15.4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 14.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 13.8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 14.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 12.6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 12.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 12.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 13.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 13.4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 14.7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 14),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 13.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 14),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 15),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 14.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 14),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 14.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 14.6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 12.9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 13.4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 13.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 12.9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 12.1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 13.2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 13.1)
                                    }
                            )
                    )
            );
        });
        irrigation11.setId(UniqueIdentifierGenerator.generateId(irrigation11.getName()));
        irrigation11Id = irrigation11.getId();
        irrigation11 = assetStorageService.merge(irrigation11);

        SoilSensorAsset soilSensor4 = createDemoSoilSensorAsset("Soil sensor", paprika, new GeoJSONPoint(4.285034, 51.973881), 34, 47, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        soilSensor4.getAttributes().stream().forEach(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ), new MetaItem<>(ACCESS_RESTRICTED_WRITE));});
        soilSensor4.getAttribute(SoilSensorAsset.SALINITY).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(manufacturerSimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[]{
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).plusMinutes(30).get(SECOND_OF_DAY), 5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).plusMinutes(30).get(SECOND_OF_DAY), 12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 17),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).plusMinutes(30).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 28),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).plusMinutes(30).get(SECOND_OF_DAY), 33),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 32),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).plusMinutes(30).get(SECOND_OF_DAY), 38),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 36),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).plusMinutes(30).get(SECOND_OF_DAY), 31),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 25),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).plusMinutes(30).get(SECOND_OF_DAY), 28),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).plusMinutes(30).get(SECOND_OF_DAY), 21),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).plusMinutes(30).get(SECOND_OF_DAY), 23),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).plusMinutes(30).get(SECOND_OF_DAY), 21),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 20),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).plusMinutes(30).get(SECOND_OF_DAY), 18),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 21),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).plusMinutes(30).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 19),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).plusMinutes(30).get(SECOND_OF_DAY), 20),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 15),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).plusMinutes(30).get(SECOND_OF_DAY), 11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).plusMinutes(30).get(SECOND_OF_DAY), 7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).plusMinutes(30).get(SECOND_OF_DAY), 5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).plusMinutes(30).get(SECOND_OF_DAY), 4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).plusMinutes(30).get(SECOND_OF_DAY), 1)
                                    }
                            )
                    )
            );
        });
        soilSensor4.setId(UniqueIdentifierGenerator.generateId(soilSensor4.getName()));
        soilSensor4Id = soilSensor4.getId();
        soilSensor4 = assetStorageService.merge(soilSensor4);

        // ### Distributor 2 ###

        Asset<?> distributor2 = new ThingAsset("High-Tech Greenhouse Distribution");
        distributor2.setRealm(this.realmManufacturerName);
        distributor2.setId(UniqueIdentifierGenerator.generateId(distributor2.getName()));
        distributor2 = assetStorageService.merge(distributor2);

        BuildingAsset bertHaanen = new BuildingAsset("Haanen Vegetables BV");
        bertHaanen.setParent(distributor2);
        bertHaanen.setId(UniqueIdentifierGenerator.generateId(bertHaanen.getName()));
        bertHaanen = assetStorageService.merge(bertHaanen);

        HarvestRobotAsset harvestRobot3 = createDemoHarvestRobotAsset("Harvest", bertHaanen, new GeoJSONPoint(4.286209, 51.983544), OperationMode.UNLOADING, VegetableType.TOMATO, 26, 45, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        harvestRobot3.setId(UniqueIdentifierGenerator.generateId(harvestRobot3.getName()));
        harvestRobot3 = assetStorageService.merge(harvestRobot3);

        BuildingAsset rtd = new BuildingAsset("RTD Vegetable Grower");
        rtd.setParent(distributor2);
        rtd.setId(UniqueIdentifierGenerator.generateId(rtd.getName()));
        rtd = assetStorageService.merge(rtd);

        HarvestRobotAsset harvestRobot4 = createDemoHarvestRobotAsset("Harvester", rtd, new GeoJSONPoint(4.408010, 51.986839), OperationMode.UNLOADING, VegetableType.TOMATO, 26, 45, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        harvestRobot4.setId(UniqueIdentifierGenerator.generateId(harvestRobot4.getName()));
        harvestRobot4 = assetStorageService.merge(harvestRobot4);

        IrrigationAsset irrigation6 = createDemoIrrigationAsset("Irrigation N", rtd, new GeoJSONPoint(4.408287, 51.987239), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        irrigation6.setId(UniqueIdentifierGenerator.generateId(irrigation6.getName()));
        irrigation6 = assetStorageService.merge(irrigation6);
        IrrigationAsset irrigation7 = createDemoIrrigationAsset("Irrigation S", rtd, new GeoJSONPoint(4.408313, 51.986357), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        irrigation7.setId(UniqueIdentifierGenerator.generateId(irrigation7.getName()));
        irrigation7 = assetStorageService.merge(irrigation7);
        IrrigationAsset irrigation8 = createDemoIrrigationAsset("Irrigation W", rtd, new GeoJSONPoint(4.407037, 51.986598), () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        irrigation8.setId(UniqueIdentifierGenerator.generateId(irrigation8.getName()));
        irrigation8 = assetStorageService.merge(irrigation8);

        SoilSensorAsset soilSensor3 = createDemoSoilSensorAsset("Soil monitor", rtd, new GeoJSONPoint(4.408088, 51.986793), 44, 68, () -> new SimulatorAgentLink(manufacturerSimulatorAgentId));
        soilSensor3.setId(UniqueIdentifierGenerator.generateId(soilSensor3.getName()));
        soilSensor3 = assetStorageService.merge(soilSensor3);

        // ################################ Link users and assets ###################################

        assetStorageService.storeUserAssetLinks(Arrays.asList(
                new UserAssetLink(this.realmManufacturerName,
                        KeycloakDemoSetup.customerUserId,
                        paprikaId),
                new UserAssetLink(this.realmManufacturerName,
                        KeycloakDemoSetup.customerUserId,
                        irrigation9Id),
                new UserAssetLink(this.realmManufacturerName,
                        KeycloakDemoSetup.customerUserId,
                        irrigation10Id),
                new UserAssetLink(this.realmManufacturerName,
                        KeycloakDemoSetup.customerUserId,
                        irrigation11Id),
                new UserAssetLink(this.realmManufacturerName,
                        KeycloakDemoSetup.customerUserId,
                        harvestRobot5Id),
                new UserAssetLink(this.realmManufacturerName,
                        KeycloakDemoSetup.customerUserId,
                        soilSensor4Id)));

        // ################################ Make user restricted ###################################
        ManagerIdentityProvider identityProvider = identityService.getIdentityProvider();
        identityProvider.updateUserRealmRoles(realmManufacturer.getName(), KeycloakDemoSetup.customerUserId, identityProvider
                .addRealmRoles(realmManufacturer.getName(), 
                        KeycloakDemoSetup.customerUserId, RESTRICTED_USER_REALM_ROLE));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    protected static AttributeLink createWeatherApiAttributeLink(String assetId, String jsonParentName, String jsonName, String parameter) {
        return new AttributeLink(
                new AttributeRef(assetId, parameter),
                null,
                new ValueFilter[]{
                        new JsonPathFilter("$." + jsonParentName + "." + jsonName, true, false),
                }
        );
    }
}
