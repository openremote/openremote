/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.test.setup;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.agent.protocol.simulator.SimulatorAgent;
import org.openremote.agent.protocol.simulator.SimulatorAgentLink;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.security.ManagerIdentityProvider;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.security.Tenant;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.impl.ColourRGB;

import java.util.Arrays;

import static org.openremote.manager.datapoint.AssetDatapointService.DATA_POINTS_MAX_AGE_DAYS_DEFAULT;
import static org.openremote.model.Constants.*;
import static org.openremote.model.value.MetaItemType.*;
import static org.openremote.model.value.ValueType.*;

public class ManagerTestSetup extends ManagerSetup {

    // Update these numbers whenever you change a RULE_STATE flag in test data
    public static final int DEMO_RULE_STATES_APARTMENT_1 = 44;
    public static final int DEMO_RULE_STATES_APARTMENT_2 = 13;
    public static final int DEMO_RULE_STATES_APARTMENT_3 = 0;
    public static final int DEMO_RULE_STATES_SMART_OFFICE = 5;
    public static final int DEMO_RULE_STATES_SMART_BUILDING = DEMO_RULE_STATES_APARTMENT_1 + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_SMART_CITY = 27;
    public static final int DEMO_RULE_STATES_GLOBAL = DEMO_RULE_STATES_SMART_BUILDING + DEMO_RULE_STATES_SMART_OFFICE + DEMO_RULE_STATES_SMART_CITY;
    public static final int DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1 + 70;
    public static final int DEMO_RULE_STATES_SMART_BUILDING_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static GeoJSONPoint SMART_OFFICE_LOCATION = new GeoJSONPoint(5.460315214821094, 51.44541688237109);
    public static GeoJSONPoint SMART_BUILDING_LOCATION = new GeoJSONPoint(5.454027, 51.446308);
    public static GeoJSONPoint SMART_CITY_LOCATION = new GeoJSONPoint(5.3814711, 51.4484647);
    public static GeoJSONPoint AREA_1_LOCATION = new GeoJSONPoint(5.478478, 51.439272);
    public static GeoJSONPoint AREA_2_LOCATION = new GeoJSONPoint(5.473829, 51.438744);
    public static GeoJSONPoint AREA_3_LOCATION = new GeoJSONPoint(5.487478, 51.446979);
    public static final String thingLightToggleAttributeName = "light1Toggle";
    public String smartOfficeId;
    public String groundFloorId;
    public String lobbyId;
    public String agentId;
    public String thingId;
    public String smartBuildingId;
    public String apartment1Id;
    public String apartment1ServiceAgentId;
    public String apartment1LivingroomId = UniqueIdentifierGenerator.generateId("apartment1LivingroomId");
    public String apartment1KitchenId;
    public String apartment1HallwayId;
    public String apartment1Bedroom1Id;
    public String apartment1BathroomId;
    public String apartment2Id;
    public String apartment3Id;
    public String apartment2LivingroomId;
    public String apartment2BathroomId;
    public String apartment3LivingroomId;
    public String masterRealm;
    public String realmBuildingTenant;
    public String realmCityTenant;
    public String realmEnergyTenant;
    public String smartCityServiceAgentId;
    public String area1Id;
    public String microphone1Id;
    public String peopleCounter3AssetId;
    public String electricityOptimisationAssetId;
    public String electricityConsumerAssetId;
    public String electricitySolarAssetId;
    public String electricityWindAssetId;
    public String electricitySupplierAssetId;
    public String electricityBatteryAssetId;

    public ManagerTestSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        KeycloakTestSetup keycloakTestSetup = setupService.getTaskOfType(KeycloakTestSetup.class);
        Tenant masterTenant = keycloakTestSetup.masterTenant;
        Tenant tenantBuilding = keycloakTestSetup.tenantBuilding;
        Tenant tenantCity = keycloakTestSetup.tenantCity;
        masterRealm = masterTenant.getRealm();
        this.realmBuildingTenant = tenantBuilding.getRealm();
        this.realmCityTenant = tenantCity.getRealm();
        this.realmEnergyTenant = keycloakTestSetup.energyTenant.getRealm();

        // ################################ Assets for 'master' realm ###################################

        BuildingAsset smartOffice = new BuildingAsset("Smart office");
        smartOffice.setRealm(masterRealm);
        smartOffice.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, SMART_OFFICE_LOCATION),
                new Attribute<>(BuildingAsset.STREET, "Torenallee 20"),
                new Attribute<>(BuildingAsset.POSTAL_CODE, "5617"),
                new Attribute<>(BuildingAsset.CITY, "Eindhoven"),
                new Attribute<>(BuildingAsset.COUNTRY, "Netherlands")
        );
        smartOffice = assetStorageService.merge(smartOffice);
        smartOfficeId = smartOffice.getId();

        Asset<?> groundFloor = new ThingAsset("Ground floor");
        groundFloor.setParent(smartOffice);
        groundFloor.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, SMART_OFFICE_LOCATION)
        );
        groundFloor = assetStorageService.merge(groundFloor);
        groundFloorId = groundFloor.getId();

        RoomAsset lobby = new RoomAsset("Lobby");
        lobby.setParent(groundFloor);
        lobby.getAttributes().addOrReplace(
            new Attribute<>(Asset.LOCATION, SMART_OFFICE_LOCATION),
            new Attribute<>("lobbyLocations", JSON_OBJECT.asArray())
        );
        lobby = assetStorageService.merge(lobby);
        lobbyId = lobby.getId();

        SimulatorAgent agent = new SimulatorAgent("Demo Agent");
        agent.setParent(lobby);
        agent.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, SMART_OFFICE_LOCATION)
        );

        agent = assetStorageService.merge(agent);
        agentId = agent.getId();

        Asset<?> thing = new ThingAsset("Demo Thing");
        thing.setParent(agent);
        thing.getAttributes().addOrReplace(
            new Attribute<>(Asset.LOCATION, SMART_OFFICE_LOCATION).addMeta(new MetaItem<>(RULE_STATE, true)),
            new Attribute<>(thingLightToggleAttributeName, BOOLEAN, true)
                    .addOrReplaceMeta(
                        new MetaItem<>(
                                LABEL,
                                "Light 1 Toggle"),
                        new MetaItem<>(
                                STORE_DATA_POINTS,
                                true),
                        new MetaItem<>(
                                DATA_POINTS_MAX_AGE_DAYS,
                                DATA_POINTS_MAX_AGE_DAYS_DEFAULT*7
                        ),
                        new MetaItem<>(
                                AGENT_LINK,
                                new SimulatorAgentLink(agent.getId()))
                    ),
            new Attribute<>("light1Dimmer", POSITIVE_INTEGER)
                    .addOrReplaceMeta(
                        new MetaItem<>(LABEL, "Light 1 Dimmer"),
                        new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId())),
                        new MetaItem<>(UNITS, Constants.units(UNITS_PERCENTAGE)),
                        new MetaItem<>(CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100)))
                    ),
            new Attribute<>("light1Color", COLOUR_RGB, new ColourRGB(88, 123, 88))
                    .addOrReplaceMeta(
                        new MetaItem<>(
                                LABEL,
                                "Light 1 Color"),
                        new MetaItem<>(
                                AGENT_LINK,
                                new SimulatorAgentLink(agent.getId()))
                    ),
            new Attribute<>("light1PowerConsumption", POSITIVE_NUMBER, 12.345)
                    .addOrReplaceMeta(
                        new MetaItem<>(LABEL, "Light 1 Usage"),
                        new MetaItem<>(READ_ONLY, true),
                        new MetaItem<>(UNITS, Constants.units(UNITS_KILO, UNITS_WATT, UNITS_HOUR)),
                        new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()))
                    )
        );
        thing = assetStorageService.merge(thing);
        thingId = thing.getId();

        // ################################ Assets for 'energy' realm ###################################
        EnergyOptimisationAsset electricityOptimisationAsset = new EnergyOptimisationAsset("Optimisation");
        electricityOptimisationAsset.setIntervalSize(3d);
        electricityOptimisationAsset.setRealm(keycloakTestSetup.energyTenant.getRealm());
        electricityOptimisationAsset.setFinancialWeighting(100);
        electricityOptimisationAsset = assetStorageService.merge(electricityOptimisationAsset);
        electricityOptimisationAssetId = electricityOptimisationAsset.getId();

        ElectricityConsumerAsset electricityConsumerAsset = new ElectricityConsumerAsset("Consumer");
        electricityConsumerAsset.setParent(electricityOptimisationAsset);
        electricityConsumerAsset.getAttribute(ElectricityAsset.POWER).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        );
        electricityConsumerAsset = assetStorageService.merge(electricityConsumerAsset);
        electricityConsumerAssetId = electricityConsumerAsset.getId();

        ElectricityProducerSolarAsset electricitySolarAsset = new ElectricityProducerSolarAsset("Producer");
        electricitySolarAsset.setParent(electricityOptimisationAsset);
        electricitySolarAsset.getAttribute(ElectricityAsset.POWER).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        );
        electricitySolarAsset.setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.SOUTH);
        electricitySolarAsset.setPanelAzimuth(0);
        electricitySolarAsset.setPanelPitch(30);
        electricitySolarAsset.setEfficiencyExport(100);
        electricitySolarAsset.setPowerExportMax(2.5);
        electricitySolarAsset.setLocation(new GeoJSONPoint(9.195285, 48.787418));
        electricitySolarAsset.setSetActualValueWithForecast(true);
        electricitySolarAsset.setIncludeForecastSolarService(true);
        electricitySolarAsset = assetStorageService.merge(electricitySolarAsset);
        electricitySolarAssetId = electricitySolarAsset.getId();

        ElectricityProducerWindAsset electricityWindAsset = new ElectricityProducerWindAsset("Wind Turbine");
        electricityWindAsset.setParent(electricityOptimisationAsset);
        electricityWindAsset.getAttribute(ElectricityAsset.POWER).ifPresent(attr ->
                attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        );
        electricityWindAsset.setWindSpeedMax(18d);
        electricityWindAsset.setWindSpeedMin(2d);
        electricityWindAsset.setWindSpeedReference(12d);
        electricityWindAsset.setPowerExportMax(9000d);
        electricityWindAsset.setEfficiencyExport(100);
        electricityWindAsset.setPowerExportMax(2.5);
        electricityWindAsset.setLocation(new GeoJSONPoint(9.195285, 48.787418));
        electricityWindAsset.setSetActualValueWithForecast(true);
        electricityWindAsset.setIncludeForecastWindService(true);
        electricityWindAsset = assetStorageService.merge(electricityWindAsset);
        electricityWindAssetId = electricityWindAsset.getId();

        ElectricityBatteryAsset electricityBatteryAsset = new ElectricityBatteryAsset("Battery");
        electricityBatteryAsset.setParent(electricityOptimisationAsset);
        electricityBatteryAsset.setEnergyCapacity(200d);
        electricityBatteryAsset.setEnergyLevelPercentageMin(20);
        electricityBatteryAsset.setEnergyLevelPercentageMax(80);
        electricityBatteryAsset.setEnergyLevel(100d);
        electricityBatteryAsset.setPowerImportMax(7d);
        electricityBatteryAsset.setPowerExportMax(20d);
        electricityBatteryAsset.setPowerSetpoint(0d);
        electricityBatteryAsset.setEfficiencyImport(95);
        electricityBatteryAsset.setEfficiencyExport(98);
        electricityBatteryAsset.setSupportsExport(true);
        electricityBatteryAsset.setSupportsImport(true);
        electricityBatteryAsset.getAttribute(ElectricityAsset.POWER_SETPOINT).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        );
        electricityBatteryAsset = assetStorageService.merge(electricityBatteryAsset);
        electricityBatteryAssetId = electricityBatteryAsset.getId();

        ElectricitySupplierAsset electricitySupplierAsset = new ElectricitySupplierAsset("Supplier");
        electricitySupplierAsset.setParent(electricityOptimisationAsset);
        electricitySupplierAsset.setTariffExport(-0.05);
        electricitySupplierAsset.setTariffImport(0.08);
        electricitySupplierAsset.getAttribute(ElectricityAsset.TARIFF_IMPORT).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        );
        electricitySupplierAsset.getAttribute(ElectricityAsset.TARIFF_EXPORT).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        );
        electricitySupplierAsset = assetStorageService.merge(electricitySupplierAsset);
        electricitySupplierAssetId = electricitySupplierAsset.getId();



        // ################################ Assets for 'building' realm ###################################

        BuildingAsset smartBuilding = new BuildingAsset("Smart building");
        smartBuilding.setRealm(this.realmBuildingTenant);
        smartBuilding.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, SMART_BUILDING_LOCATION),
                new Attribute<>(BuildingAsset.STREET, "Kastanjelaan 500"),
                new Attribute<>(BuildingAsset.POSTAL_CODE, "5616"),
                new Attribute<>(BuildingAsset.CITY, "Eindhoven"),
                new Attribute<>(BuildingAsset.COUNTRY, "Netherlands")
        );
        smartBuilding = assetStorageService.merge(smartBuilding);
        smartBuildingId = smartBuilding.getId();

        // The "Apartment 1" is the demo apartment with complex scenes
        BuildingAsset apartment1 = createDemoApartment(smartBuilding, "Apartment 1", new GeoJSONPoint(5.454233, 51.446800));
        apartment1.setParent(smartBuilding);
        apartment1.setAccessPublicRead(true);
        apartment1.getAttribute(Asset.LOCATION).ifPresent(locationAttr ->
            locationAttr.getMeta().addOrReplace(
                new MetaItem<>(ACCESS_PUBLIC_READ),
                new MetaItem<>(ACCESS_PUBLIC_WRITE)
            ));
        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        SimulatorAgent apartment1ServiceAgent = new SimulatorAgent("Service Agent (Simulator)");
        apartment1ServiceAgent.setParent(apartment1);
        apartment1ServiceAgent = assetStorageService.merge(apartment1ServiceAgent);
        apartment1ServiceAgentId = apartment1ServiceAgent.getId();

        /* ############################ ROOMS ############################## */

        RoomAsset apartment1Livingroom = createDemoApartmentRoom(apartment1, "Living Room 1");
        apartment1Livingroom.getAttributes().addOrReplace(
            new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454213, 51.446884)),
            new Attribute<>("lightsCeiling", NUMBER, 0d)
                .addMeta(
                    new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                    new MetaItem<>(ACCESS_RESTRICTED_WRITE, true)
                ),
            new Attribute<>("lightsStand", BOOLEAN, true)
                .addMeta(
                    new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                    new MetaItem<>(ACCESS_RESTRICTED_WRITE, true)
                )
        );
        addDemoApartmentRoomMotionSensor(apartment1Livingroom, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId)
        );
        addDemoApartmentRoomCO2Sensor(apartment1Livingroom, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId)
        );
        addDemoApartmentRoomHumiditySensor(apartment1Livingroom, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId)
        );
        addDemoApartmentRoomThermometer(apartment1Livingroom, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId)
        );
        addDemoApartmentTemperatureControl(apartment1Livingroom, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId)
        );

        apartment1Livingroom.setId(apartment1LivingroomId);
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        RoomAsset apartment1Kitchen = createDemoApartmentRoom(apartment1, "Kitchen 1");
        apartment1Kitchen.getAttributes().addOrReplace(
                    new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454122, 51.446800)),
                    new Attribute<>("lights", BOOLEAN, true)
                            .addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ, true))
                            .addMeta(new MetaItem<>(ACCESS_RESTRICTED_WRITE, true))
            );
        addDemoApartmentRoomMotionSensor(apartment1Kitchen, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId)
        );

        for (String switchName : new String[]{"A", "B", "C"}) {
            addDemoApartmentSmartSwitch(apartment1Kitchen, switchName, true, attributeIndex -> {
                switch (attributeIndex) {
                    case 2:
                    case 3:
                    case 4:
                        return new MetaItem[]{
                                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(apartment1ServiceAgentId))
                        };
                }
                return null;
            });
        }

        apartment1Kitchen = assetStorageService.merge(apartment1Kitchen);
        apartment1KitchenId = apartment1Kitchen.getId();

        RoomAsset apartment1Hallway = createDemoApartmentRoom(apartment1, "Hallway 1");
        apartment1Hallway.getAttributes().addOrReplace(
                        new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454342, 51.446762)),
                        new Attribute<>("lights", BOOLEAN, true)
                                .addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ, true))
                                .addMeta(new MetaItem<>(ACCESS_RESTRICTED_WRITE, true))
                );
        addDemoApartmentRoomMotionSensor(apartment1Hallway, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId)
        );

        apartment1Hallway = assetStorageService.merge(apartment1Hallway);
        apartment1HallwayId = apartment1Hallway.getId();

        RoomAsset apartment1Bedroom1 = createDemoApartmentRoom(apartment1, "Bedroom 1");
        apartment1Bedroom1.getAttributes().addOrReplace(
                        new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454332, 51.446830)),
                        new Attribute<>("lights", BOOLEAN, true)
                                .addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ, true))
                                .addMeta(new MetaItem<>(ACCESS_RESTRICTED_WRITE, true))
                );
        addDemoApartmentRoomCO2Sensor(apartment1Bedroom1, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId));
        addDemoApartmentRoomHumiditySensor(apartment1Bedroom1, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId));
        addDemoApartmentRoomThermometer(apartment1Bedroom1, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId));
        addDemoApartmentTemperatureControl(apartment1Bedroom1, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId));

        apartment1Bedroom1 = assetStorageService.merge(apartment1Bedroom1);
        apartment1Bedroom1Id = apartment1Bedroom1.getId();

        RoomAsset apartment1Bathroom = new RoomAsset("Bathroom 1");
        apartment1Bathroom.setParent(apartment1);
        apartment1Bathroom.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454227,51.446753)),
                new Attribute<>("lights", BOOLEAN, true)
                        .addMeta(
                                new MetaItem<>(RULE_STATE, true),
                                new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                                new MetaItem<>(ACCESS_RESTRICTED_WRITE, true)
                        )
        );
        addDemoApartmentRoomThermometer(apartment1Bathroom, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId));
        addDemoApartmentTemperatureControl(apartment1Bathroom, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId));
        apartment1Bathroom = assetStorageService.merge(apartment1Bathroom);
        apartment1BathroomId = apartment1Bathroom.getId();


        addDemoApartmentVentilation(apartment1, true, () ->
            new SimulatorAgentLink(apartment1ServiceAgentId));

        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        BuildingAsset apartment2 = new BuildingAsset("Apartment 2");
        apartment2.setParent(smartBuilding);
        apartment2.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454053, 51.446603)),
                new Attribute<>("allLightsOffSwitch", BOOLEAN, true)
                        .addMeta(
                                new MetaItem<>(LABEL, "All Lights Off Switch"),
                                new MetaItem<>(RULE_EVENT, true),
                                new MetaItem<>(RULE_EVENT_EXPIRES, "PT3S")
                        )
        );
        apartment2 = assetStorageService.merge(apartment2);
        apartment2Id = apartment2.getId();

        RoomAsset apartment2Livingroom = new RoomAsset("Living Room 2");
        apartment2Livingroom.setAccessPublicRead(true);
        apartment2Livingroom.setParent(apartment2);

        ObjectNode objectMap = ValueUtil.createJsonObject();
        objectMap.put("cactus", 0.8);

        apartment2Livingroom.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454109, 51.446631)).addMeta(
                    new MetaItem<>(ACCESS_PUBLIC_READ)
                ),
                new Attribute<>("motionSensor", BOOLEAN, false)
                    .addMeta(
                            new MetaItem<>(LABEL, "Motion Sensor"),
                            new MetaItem<>(RULE_STATE, true),
                            new MetaItem<>(RULE_EVENT, true)
                    ),
                new Attribute<>("presenceDetected", BOOLEAN, false)
                        .addMeta(
                                new MetaItem<>(LABEL, "Presence Detected"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("firstPresenceDetected", ValueType.TIMESTAMP)
                        .addMeta(
                                new MetaItem<>(LABEL, "First Presence Timestamp"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("lastPresenceDetected", ValueType.TIMESTAMP)
                        .addMeta(
                                new MetaItem<>(LABEL, "Last Presence Timestamp"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("co2Level", POSITIVE_INTEGER, 350)
                        .addMeta(
                                new MetaItem<>(LABEL, "CO2 Level"),
                                new MetaItem<>(UNITS, Constants.units(UNITS_PART_PER_MILLION)),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("lightSwitch", BOOLEAN, true)
                        .addMeta(
                                new MetaItem<>(LABEL, "Light Switch"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("windowOpen", BOOLEAN, false)
                        .addMeta(
                                new MetaItem<>(ACCESS_RESTRICTED_READ, true)
                        ),
                new Attribute<>("lightSwitchTriggerTimes", TEXT.asArray(), new String[] {"1800", "0830"})
                        .addMeta(
                                new MetaItem<>(LABEL, "Lightswitch Trigger Times"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("plantsWaterLevels", JSON_OBJECT, objectMap)
                        .addMeta(
                                new MetaItem<>(LABEL, "Water levels of the plants"),
                                new MetaItem<>(RULE_STATE, true)
                        )
        );
        apartment2Livingroom = assetStorageService.merge(apartment2Livingroom);
        apartment2LivingroomId = apartment2Livingroom.getId();

        RoomAsset apartment2Bathroom = new RoomAsset("Bathroom 2");
        apartment2Bathroom.setParent(apartment2);
        apartment2Bathroom.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.454015, 51.446665)),
                new Attribute<>("motionSensor", BOOLEAN, false)
                        .addMeta(
                                new MetaItem<>(LABEL, "Motion Sensor"),
                                new MetaItem<>(RULE_STATE, true),
                                new MetaItem<>(RULE_EVENT, true)
                        ),
                new Attribute<>("presenceDetected", BOOLEAN, false)
                        .addMeta(
                                new MetaItem<>(LABEL, "Presence Detected"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("firstPresenceDetected", ValueType.TIMESTAMP)
                        .addMeta(
                                new MetaItem<>(LABEL, "First Presence Timestamp"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("lastPresenceDetected", ValueType.TIMESTAMP)
                        .addMeta(
                                new MetaItem<>(LABEL, "Last Presence Timestamp"),
                                new MetaItem<>(RULE_STATE, true)
                        ),
                new Attribute<>("lightSwitch", BOOLEAN, true)
                        .addMeta(
                                new MetaItem<>(LABEL, "Light Switch"),
                                new MetaItem<>(RULE_STATE, true)
                        )
        );
        apartment2Bathroom = assetStorageService.merge(apartment2Bathroom);
        apartment2BathroomId = apartment2Bathroom.getId();

        BuildingAsset apartment3 = new BuildingAsset("Apartment 3");
        apartment3.setParent(smartBuilding);
        apartment3.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.453859, 51.446379)));
        apartment3 = assetStorageService.merge(apartment3);
        apartment3Id = apartment3.getId();

        RoomAsset apartment3Livingroom = new RoomAsset("Living Room 3");
        apartment3Livingroom.setParent(apartment3);
        apartment3Livingroom.getAttributes().addOrReplace(
            new Attribute<>(Asset.LOCATION, new GeoJSONPoint(5.453932, 51.446422)),
            new Attribute<>("lightSwitch", BOOLEAN)
        );

        apartment3Livingroom = assetStorageService.merge(apartment3Livingroom);
        apartment3LivingroomId = apartment3Livingroom.getId();

        // ################################ Link users and assets ###################################

        assetStorageService.storeUserAssetLinks(Arrays.asList(
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.testuser3Id,
                apartment1Id),
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.testuser3Id,
                apartment1LivingroomId),
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.testuser3Id,
                apartment1KitchenId),
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.testuser3Id,
                apartment1Bedroom1Id),
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.testuser3Id,
                apartment1BathroomId),
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.testuser3Id,
                apartment1HallwayId)));

        assetStorageService.storeUserAssetLinks(Arrays.asList(
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.buildingUserId,
                apartment2Id),
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.buildingUserId,
                apartment2LivingroomId),
            new UserAssetLink(keycloakTestSetup.tenantBuilding.getRealm(),
                keycloakTestSetup.buildingUserId,
                apartment2BathroomId)));

        // ################################ Make users restricted ###################################
        ManagerIdentityProvider identityProvider = identityService.getIdentityProvider();
        identityProvider.updateUserRealmRoles(tenantBuilding.getRealm(), keycloakTestSetup.testuser3Id, identityProvider.addRealmRoles(tenantBuilding.getRealm(), keycloakTestSetup.testuser3Id, RESTRICTED_USER_REALM_ROLE));
        identityProvider.updateUserRealmRoles(tenantBuilding.getRealm(), keycloakTestSetup.buildingUserId, identityProvider.addRealmRoles(tenantBuilding.getRealm(), keycloakTestSetup.buildingUserId, RESTRICTED_USER_REALM_ROLE));

        // ################################ Realm smartcity ###################################

        CityAsset smartCity = new CityAsset("Smart city");
        smartCity.setRealm(this.realmCityTenant);
        smartCity.addOrReplaceAttributes(
                new Attribute<>(Asset.LOCATION, SMART_CITY_LOCATION),
                new Attribute<>(CityAsset.CITY, "Eindhoven"),
                new Attribute<>(CityAsset.COUNTRY, "Netherlands")
        );
        smartCity = assetStorageService.merge(smartCity);

        SimulatorAgent smartCityServiceAgent = new SimulatorAgent("Service Agent (Simulator)");
        smartCityServiceAgent.setParent(smartCity);
        smartCityServiceAgent = assetStorageService.merge(smartCityServiceAgent);
        smartCityServiceAgentId = smartCityServiceAgent.getId();

        // ################################ Realm B Area 1 ###################################

        Asset<?> assetArea1 = new ThingAsset("Area 1");
        assetArea1.setParent(smartCity);
        assetArea1.getAttributes().addOrReplace(
            new Attribute<>(Asset.LOCATION, AREA_1_LOCATION)
        );
        assetArea1 = assetStorageService.merge(assetArea1);
        area1Id = assetArea1.getId();

        PeopleCounterAsset peopleCounter1Asset = createDemoPeopleCounterAsset("PeopleCounter 1", assetArea1, new GeoJSONPoint(5.477126, 51.439137), () ->
            new SimulatorAgentLink(smartCityServiceAgentId));
        peopleCounter1Asset = assetStorageService.merge(peopleCounter1Asset);

        Asset<?> microphone1Asset = createDemoMicrophoneAsset("Microphone 1", assetArea1, new GeoJSONPoint(5.478092, 51.438655), () ->
            new SimulatorAgentLink(smartCityServiceAgentId));
        microphone1Asset = assetStorageService.merge(microphone1Asset);
        microphone1Id = microphone1Asset.getId();

        Asset<?> enviroment1Asset = createDemoEnvironmentAsset("Environment 1", assetArea1, new GeoJSONPoint(5.478907, 51.438943),() ->
            new SimulatorAgentLink(smartCityServiceAgentId));
        enviroment1Asset = assetStorageService.merge(enviroment1Asset);

        Asset<?> light1Asset = createDemoLightAsset("Light 1", assetArea1, new GeoJSONPoint(5.476111, 51.438492));
        light1Asset = assetStorageService.merge(light1Asset);

        Asset<?> light2Asset = createDemoLightAsset("Light 2", assetArea1, new GeoJSONPoint(5.477272, 51.439214));
        light2Asset = assetStorageService.merge(light2Asset);

        // ################################ Realm B Area 2 ###################################

        Asset<?> assetArea2 = new ThingAsset("Area 2");
        assetArea2.setParent(smartCity);
        assetArea2.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, AREA_2_LOCATION)
        );
        assetArea2 = assetStorageService.merge(assetArea2);

        Asset<?> peopleCounter2Asset = createDemoPeopleCounterAsset("PeopleCounter 2", assetArea2, new GeoJSONPoint(5.473686, 51.438603), () ->
            new SimulatorAgentLink(smartCityServiceAgentId));
        peopleCounter2Asset = assetStorageService.merge(peopleCounter2Asset);

        Asset<?> environment2Asset = createDemoEnvironmentAsset("Environment 2", assetArea2, new GeoJSONPoint(5.473552, 51.438412), () ->
            new SimulatorAgentLink(smartCityServiceAgentId));
        environment2Asset = assetStorageService.merge(environment2Asset);


        // ################################ Realm B Area 3 ###################################

        Asset<?> assetArea3 = new ThingAsset("Area 3");
        assetArea3.setParent(smartCity);
        assetArea3.getAttributes().addOrReplace(
                new Attribute<>(Asset.LOCATION, AREA_3_LOCATION)
        );
        assetArea3 = assetStorageService.merge(assetArea3);

        Asset<?> peopleCounter3Asset = createDemoPeopleCounterAsset("PeopleCounter 3", assetArea3, new GeoJSONPoint(5.487234, 51.447065), () ->
            new SimulatorAgentLink(smartCityServiceAgentId));
        peopleCounter3Asset = assetStorageService.merge(peopleCounter3Asset);
        peopleCounter3AssetId = peopleCounter3Asset.getId();

        LightAsset lightController_3Asset = createDemoLightControllerAsset("LightController 3", assetArea3, new GeoJSONPoint(5.487478, 51.446979));
        lightController_3Asset = assetStorageService.merge(lightController_3Asset);
    }
}
