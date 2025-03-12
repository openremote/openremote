package org.openremote.energy.setup.demo;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;

import org.openremote.agent.protocol.http.HTTPAgent;
import org.openremote.agent.protocol.http.HTTPAgentLink;
import org.openremote.agent.protocol.simulator.SimulatorAgent;
import org.openremote.agent.protocol.simulator.SimulatorAgentLink;
import org.openremote.energy.asset.*;
import org.openremote.energy.setup.EnergyManagerSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.BuildingAsset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeLink;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.security.Realm;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.value.JsonPathFilter;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueFilter;
import org.openremote.model.value.ValueType;

import static java.time.temporal.ChronoField.SECOND_OF_DAY;

public class EnergyManagerDemoSetup extends EnergyManagerSetup {
    public String realmMasterName;
    public String realmCityName;
    public String realmManufacturerName;
    public String smartcitySimulatorAgentId;
    public String energyManagementId;
    public String weatherHttpApiAgentId;

    private final long halfHourInMillis = Duration.ofMinutes(30).toMillis();

    public EnergyManagerDemoSetup(Container container) {
        super(container);
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

        // ################################ Realm smartcity - Energy Management ###################################


        ThingAsset energyManagement = new ThingAsset("Energy management");
        energyManagement.setRealm(this.realmCityName);
        energyManagement.getAttributes().addOrReplace(
                new Attribute<>("powerTotalProducers", ValueType.NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(MetaItemType.UNITS,
                                        Constants.units(Constants.UNITS_KILO, Constants.UNITS_WATT)),
                                new MetaItem<>(MetaItemType.READ_ONLY, true),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),
                                new MetaItem<>(MetaItemType.RULE_STATE, true)),
                new Attribute<>("powerTotalConsumers", ValueType.NUMBER).addOrReplaceMeta(
                        new MetaItem<>(MetaItemType.UNITS, Constants.units(Constants.UNITS_KILO, Constants.UNITS_WATT)),
                        new MetaItem<>(MetaItemType.READ_ONLY, true),
                        new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true),
                        new MetaItem<>(MetaItemType.RULE_STATE, true)));
        energyManagement.setId(UniqueIdentifierGenerator.generateId(energyManagement.getName()));
        energyManagement = assetStorageService.merge(energyManagement);
        energyManagementId = energyManagement.getId();

        // ### De Rotterdam ###
        BuildingAsset building1Asset = new BuildingAsset("De Rotterdam");
        building1Asset.setParent(energyManagement);
        building1Asset.getAttributes().addOrReplace(
                new Attribute<>(BuildingAsset.STREET, "Wilhelminakade 139"),
                new Attribute<>(BuildingAsset.POSTAL_CODE, "3072 AP"),
                new Attribute<>(BuildingAsset.CITY, "Rotterdam"),
                new Attribute<>(BuildingAsset.COUNTRY, "Netherlands"),
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(4.488324, 51.906577)),
                new Attribute<>("powerBalance", ValueType.NUMBER).addMeta(
                        new MetaItem<>(MetaItemType.UNITS, Constants.units(Constants.UNITS_KILO, Constants.UNITS_WATT)),
                        new MetaItem<>(MetaItemType.READ_ONLY),
                        new MetaItem<>(MetaItemType.RULE_STATE),
                        new MetaItem<>(MetaItemType.STORE_DATA_POINTS)));
        building1Asset.setId(UniqueIdentifierGenerator.generateId(building1Asset.getName() + "building"));
        building1Asset = assetStorageService.merge(building1Asset);

        ElectricityStorageAsset storage1Asset = createDemoElectricityStorageAsset("Battery De Rotterdam",
                building1Asset, new GeoJSONPoint(4.488324, 51.906577));
        storage1Asset.setManufacturer("Super-B");
        storage1Asset.setModel("Nomia");
        storage1Asset.setId(UniqueIdentifierGenerator.generateId(storage1Asset.getName()));
        storage1Asset = assetStorageService.merge(storage1Asset);

        ElectricityConsumerAsset consumption1Asset = createDemoElectricityConsumerAsset("Consumption De Rotterdam",
                building1Asset, new GeoJSONPoint(4.487519, 51.906544));
        consumption1Asset.getAttribute(ElectricityConsumerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 23),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 21),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 20),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 21),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 41),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 54),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 63),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 76),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 80),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 79),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 84),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 76),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 82),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 83),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 77),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 71),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 63),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 41),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 27),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 24),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 20)
                                    })));
        });

        consumption1Asset.setId(UniqueIdentifierGenerator.generateId(consumption1Asset.getName()));
        consumption1Asset = assetStorageService.merge(consumption1Asset);

        ElectricityProducerSolarAsset production1Asset = createDemoElectricitySolarProducerAsset("Solar De Rotterdam",
                building1Asset, new GeoJSONPoint(4.488592, 51.907047));
        production1Asset.setManufacturer("AEG");
        production1Asset.setModel("AS-P60");
        production1Asset.getAttribute(ElectricityProducerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), -1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), -10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), -15),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY),
                                                    -39),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY),
                                                    -52),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY),
                                                    -50),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY),
                                                    -48),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY),
                                                    -36),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY),
                                                    -23),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY),
                                                    -24),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY),
                                                    -18),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY),
                                                    -10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), -8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), -3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), -1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 0)
                                    })));
        });
        production1Asset.setEnergyExportTotal(152689d);
        production1Asset.setPowerExportMax(89.6);
        production1Asset.setEfficiencyExport(93);
        production1Asset.setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.EAST_WEST);
        production1Asset.setPanelAzimuth(30);
        production1Asset.setPanelPitch(20);
        production1Asset.setId(UniqueIdentifierGenerator.generateId(production1Asset.getName()));
        production1Asset = assetStorageService.merge(production1Asset);

        // ### Stadhuis ###

        BuildingAsset building2Asset = new BuildingAsset("Stadhuis");
        building2Asset.setParent(energyManagement);
        building2Asset.getAttributes().addOrReplace(
                new Attribute<>(BuildingAsset.STREET, "Coolsingel 40"),
                new Attribute<>(BuildingAsset.POSTAL_CODE, "3011 AD"),
                new Attribute<>(BuildingAsset.CITY, "Rotterdam"),
                new Attribute<>(BuildingAsset.COUNTRY, "Netherlands"),
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(4.47985, 51.92274)));
        building2Asset.setId(UniqueIdentifierGenerator.generateId(building2Asset.getName() + "building"));
        building2Asset = assetStorageService.merge(building2Asset);

        ElectricityStorageAsset storage2Asset = createDemoElectricityStorageAsset("Battery Stadhuis", building2Asset,
                new GeoJSONPoint(4.47985, 51.92274));
        storage2Asset.setManufacturer("LG Chem");
        storage2Asset.setModel("ESS Industrial");
        storage2Asset.setId(UniqueIdentifierGenerator.generateId(storage2Asset.getName()));
        storage2Asset = assetStorageService.merge(storage2Asset);

        ElectricityConsumerAsset consumption2Asset = createDemoElectricityConsumerAsset("Consumption Stadhuis",
                building2Asset, new GeoJSONPoint(4.47933, 51.92259));
        consumption2Asset.getAttribute(ElectricityConsumerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 22),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 30),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 36),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 39),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 32),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 36),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 44),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 47),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 44),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 38),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 38),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 34),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 33),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 23),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 8)
                                    })));
        });
        consumption2Asset.setId(UniqueIdentifierGenerator.generateId(consumption2Asset.getName()));
        consumption2Asset = assetStorageService.merge(consumption2Asset);

        ElectricityProducerSolarAsset production2Asset = createDemoElectricitySolarProducerAsset("Solar Stadhuis",
                building2Asset, new GeoJSONPoint(4.47945, 51.92301));
        production2Asset.getAttribute(ElectricityProducerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), -1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), -2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), -3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), -8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY),
                                                    -14),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY),
                                                    -12),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY),
                                                    -10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), -7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), -5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), -7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), -5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), -3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), -2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), -1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), -1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 0)
                                    })));
        });
        production2Asset.setEnergyExportTotal(88961d);
        production2Asset.setPowerExportMax(19.2);
        production2Asset.setEfficiencyExport(79);
        production2Asset.setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.SOUTH);
        production2Asset.setPanelAzimuth(10);
        production2Asset.setPanelPitch(40);
        production2Asset.setManufacturer("Solarwatt");
        production2Asset.setModel("EasyIn 60M");
        production2Asset.setId(UniqueIdentifierGenerator.generateId(production2Asset.getName()));
        production2Asset = assetStorageService.merge(production2Asset);

        // ### Markthal ###

        BuildingAsset building3Asset = new BuildingAsset("Markthal");
        building3Asset.setParent(energyManagement);
        building3Asset.getAttributes().addOrReplace(
                new Attribute<>(BuildingAsset.STREET, "Dominee Jan Scharpstraat 298"),
                new Attribute<>(BuildingAsset.POSTAL_CODE, "3011 GZ"),
                new Attribute<>(BuildingAsset.CITY, "Rotterdam"),
                new Attribute<>(BuildingAsset.COUNTRY, "Netherlands"),
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(4.47945, 51.92301)),
                new Attribute<>("allChargersInUse", ValueType.BOOLEAN)
                        .addMeta(
                                new MetaItem<>(MetaItemType.READ_ONLY)));
        building3Asset.setId(UniqueIdentifierGenerator.generateId(building3Asset.getName() + "building"));
        building3Asset = assetStorageService.merge(building3Asset);

        ElectricityProducerSolarAsset production3Asset = createDemoElectricitySolarProducerAsset("Solar Markthal",
                building3Asset, new GeoJSONPoint(4.47945, 51.92301));
        production3Asset.getAttribute(ElectricityProducerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), -2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), -6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY),
                                                    -10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY),
                                                    -13),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY),
                                                    -21),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY),
                                                    -14),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY),
                                                    -17),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY),
                                                    -10),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), -9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), -7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), -5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), -4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), -2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), -1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 0)
                                    })));
        });
        production3Asset.setEnergyExportTotal(24134d);
        production3Asset.setPowerExportMax(29.8);
        production3Asset.setEfficiencyExport(91);
        production3Asset.setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.SOUTH);
        production3Asset.setManufacturer("Sunpower");
        production3Asset.setModel("E20-327");
        production3Asset.setPanelAzimuth(10);
        production3Asset.setPanelPitch(5);
        production3Asset.setId(UniqueIdentifierGenerator.generateId(production3Asset.getName()));
        production3Asset = assetStorageService.merge(production3Asset);

        ElectricityChargerAsset charger1Asset = createDemoElectricityChargerAsset("Charger 1 Markthal", building3Asset,
                new GeoJSONPoint(4.486143, 51.920058));
        charger1Asset.setPower(0d);
        charger1Asset.getAttributes().getOrCreate(ElectricityChargerAsset.POWER).addMeta(
                new MetaItem<>(
                        MetaItemType.AGENT_LINK,
                        new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                new SimulatorReplayDatapoint[] {
                                        new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 2),
                                        new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 5),
                                        new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 10),
                                        new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 5),
                                        new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 3),
                                        new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 0),
                                        new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 15),
                                        new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 32),
                                        new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 35),
                                        new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 17),
                                        new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 9),
                                        new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 6),
                                        new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 3),
                                        new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 3),
                                        new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 0)
                                })));
        charger1Asset.setManufacturer("Allego");
        charger1Asset.setModel("HPC");
        charger1Asset.setId(UniqueIdentifierGenerator.generateId(charger1Asset.getName()));
        charger1Asset = assetStorageService.merge(charger1Asset);

        ElectricityChargerAsset charger2Asset = createDemoElectricityChargerAsset("Charger 2 Markthal", building3Asset,
                new GeoJSONPoint(4.486188, 51.919957));
        charger2Asset.setPower(0d);
        charger2Asset.getAttributes().getOrCreate(ElectricityChargerAsset.POWER)
                .addMeta(
                        new MetaItem<>(
                                MetaItemType.AGENT_LINK,
                                new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                        new SimulatorReplayDatapoint[] {
                                                new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 5),
                                                new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY),
                                                        11),
                                                new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY),
                                                        5),
                                                new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY),
                                                        10),
                                                new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY),
                                                        6),
                                                new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY),
                                                        3),
                                                new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY),
                                                        3),
                                                new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY),
                                                        17),
                                                new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY),
                                                        14),
                                                new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY),
                                                        9),
                                                new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY),
                                                        4),
                                                new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY),
                                                        28),
                                                new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY),
                                                        38),
                                                new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY),
                                                        32),
                                                new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY),
                                                        26),
                                                new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY),
                                                        13),
                                                new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY),
                                                        6),
                                                new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY),
                                                        3),
                                                new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY),
                                                        0)
                                        })));
        charger2Asset.setManufacturer("Bosch");
        charger2Asset.setModel("EV800");
        charger2Asset.setId(UniqueIdentifierGenerator.generateId(charger2Asset.getName()));
        charger2Asset = assetStorageService.merge(charger2Asset);

        ElectricityChargerAsset charger3Asset = createDemoElectricityChargerAsset("Charger 3 Markthal", building3Asset,
                new GeoJSONPoint(4.486232, 51.919856));
        charger3Asset.setPower(0d);
        charger3Asset.getAttributes().getOrCreate(ElectricityChargerAsset.POWER)
                .addMeta(
                        new MetaItem<>(
                                MetaItemType.AGENT_LINK,
                                new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                        new SimulatorReplayDatapoint[] {
                                                new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY),
                                                        4),
                                                new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY),
                                                        4),
                                                new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY),
                                                        7),
                                                new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY),
                                                        9),
                                                new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY),
                                                        6),
                                                new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY),
                                                        2),
                                                new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY),
                                                        6),
                                                new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY),
                                                        18),
                                                new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY),
                                                        4),
                                                new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY),
                                                        29),
                                                new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY),
                                                        34),
                                                new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY),
                                                        22),
                                                new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY),
                                                        14),
                                                new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY),
                                                        3),
                                                new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY),
                                                        0)
                                        })));
        charger3Asset.setManufacturer("Siemens");
        charger3Asset.setModel("CPC 50");
        charger3Asset.setId(UniqueIdentifierGenerator.generateId(charger3Asset.getName()));
        charger3Asset = assetStorageService.merge(charger3Asset);

        ElectricityChargerAsset charger4Asset = createDemoElectricityChargerAsset("Charger 4 Markthal", building3Asset,
                new GeoJSONPoint(4.486286, 51.919733));
        charger4Asset.setPower(0d);
        charger4Asset.getAttributes().getOrCreate(ElectricityChargerAsset.POWER)
                .addMeta(
                        new MetaItem<>(
                                MetaItemType.AGENT_LINK,
                                new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                        new SimulatorReplayDatapoint[] {
                                                new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 3),
                                                new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY),
                                                        4),
                                                new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY),
                                                        17),
                                                new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY),
                                                        15),
                                                new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY),
                                                        8),
                                                new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY),
                                                        16),
                                                new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY),
                                                        4),
                                                new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY),
                                                        0),
                                                new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY),
                                                        15),
                                                new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY),
                                                        34),
                                                new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY),
                                                        30),
                                                new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY),
                                                        11),
                                                new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY),
                                                        16),
                                                new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY),
                                                        7),
                                                new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY),
                                                        4)
                                        })));

        charger4Asset.setManufacturer("SemaConnect");
        charger4Asset.setModel("The Series 6");
        charger4Asset.setId(UniqueIdentifierGenerator.generateId(charger4Asset.getName()));
        charger4Asset = assetStorageService.merge(charger4Asset);

        // ### Erasmianum ###

        BuildingAsset building4Asset = new BuildingAsset("Erasmianum");
        building4Asset.setParent(energyManagement);
        building4Asset.getAttributes().addOrReplace(
                new Attribute<>(BuildingAsset.STREET, "Wytemaweg 25"),
                new Attribute<>(BuildingAsset.POSTAL_CODE, "3015 CN"),
                new Attribute<>(BuildingAsset.CITY, "Rotterdam"),
                new Attribute<>(BuildingAsset.COUNTRY, "Netherlands"),
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(4.468324, 51.912062)));
        building4Asset.setId(UniqueIdentifierGenerator.generateId(building4Asset.getName() + "building"));
        building4Asset = assetStorageService.merge(building4Asset);

        ElectricityConsumerAsset consumption4Asset = createDemoElectricityConsumerAsset("Consumption Erasmianum",
                building4Asset, new GeoJSONPoint(4.468324, 51.912062));
        consumption4Asset.getAttribute(ElectricityConsumerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 5),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 6),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 9),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 23),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 37),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 41),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 47),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 49),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 51),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 43),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 48),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 45),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 46),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 41),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 38),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 30),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 19),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 15),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 7),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 6)
                                    })));
        });
        consumption4Asset.setId(UniqueIdentifierGenerator.generateId(consumption4Asset.getName()));
        consumption4Asset = assetStorageService.merge(consumption4Asset);

        // ### Oostelijk zwembad ###

        BuildingAsset building5Asset = new BuildingAsset("Oostelijk zwembad");
        building5Asset.setParent(energyManagement);
        building5Asset.getAttributes().addOrReplace(
                new Attribute<>(BuildingAsset.STREET, "Gerdesiaweg 480"),
                new Attribute<>(BuildingAsset.POSTAL_CODE, "3061 RA"),
                new Attribute<>(BuildingAsset.CITY, "Rotterdam"),
                new Attribute<>(BuildingAsset.COUNTRY, "Netherlands"),
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(4.498048, 51.925770)));
        building5Asset.setId(UniqueIdentifierGenerator.generateId(building5Asset.getName() + "building"));
        building5Asset = assetStorageService.merge(building5Asset);

        ElectricityConsumerAsset consumption5Asset = createDemoElectricityConsumerAsset("Consumption Zwembad",
                building5Asset, new GeoJSONPoint(4.498048, 51.925770));
        consumption5Asset.getAttribute(ElectricityConsumerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 16),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 16),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 15),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 16),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 17),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 16),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 24),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), 35),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), 32),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), 33),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY), 34),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY), 33),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY), 34),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY), 31),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY), 36),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY), 34),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY), 32),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY), 37),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), 38),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), 37),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), 38),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 35),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 24),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 19)
                                    })));
        });
        consumption5Asset.setId(UniqueIdentifierGenerator.generateId(consumption5Asset.getName()));
        consumption5Asset = assetStorageService.merge(consumption5Asset);

        ElectricityProducerSolarAsset production5Asset = createDemoElectricitySolarProducerAsset("Solar Zwembad",
                building5Asset, new GeoJSONPoint(4.498281, 51.925507));
        production5Asset.getAttribute(ElectricityProducerAsset.POWER).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(
                            MetaItemType.AGENT_LINK,
                            new SimulatorAgentLink(smartcitySimulatorAgentId).setReplayData(
                                    new SimulatorReplayDatapoint[] {
                                            new SimulatorReplayDatapoint(midnight.get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(1).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(2).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(3).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(4).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(5).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(6).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(7).get(SECOND_OF_DAY), -1),
                                            new SimulatorReplayDatapoint(midnight.plusHours(8).get(SECOND_OF_DAY), -3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(9).get(SECOND_OF_DAY), -8),
                                            new SimulatorReplayDatapoint(midnight.plusHours(10).get(SECOND_OF_DAY),
                                                    -30),
                                            new SimulatorReplayDatapoint(midnight.plusHours(11).get(SECOND_OF_DAY),
                                                    -44),
                                            new SimulatorReplayDatapoint(midnight.plusHours(12).get(SECOND_OF_DAY),
                                                    -42),
                                            new SimulatorReplayDatapoint(midnight.plusHours(13).get(SECOND_OF_DAY),
                                                    -41),
                                            new SimulatorReplayDatapoint(midnight.plusHours(14).get(SECOND_OF_DAY),
                                                    -29),
                                            new SimulatorReplayDatapoint(midnight.plusHours(15).get(SECOND_OF_DAY),
                                                    -19),
                                            new SimulatorReplayDatapoint(midnight.plusHours(16).get(SECOND_OF_DAY),
                                                    -16),
                                            new SimulatorReplayDatapoint(midnight.plusHours(17).get(SECOND_OF_DAY),
                                                    -11),
                                            new SimulatorReplayDatapoint(midnight.plusHours(18).get(SECOND_OF_DAY), -4),
                                            new SimulatorReplayDatapoint(midnight.plusHours(19).get(SECOND_OF_DAY), -3),
                                            new SimulatorReplayDatapoint(midnight.plusHours(20).get(SECOND_OF_DAY), -2),
                                            new SimulatorReplayDatapoint(midnight.plusHours(21).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(22).get(SECOND_OF_DAY), 0),
                                            new SimulatorReplayDatapoint(midnight.plusHours(23).get(SECOND_OF_DAY), 0)
                                    })));
        });
        production5Asset.setEnergyExportTotal(23461d);
        production5Asset.setPowerExportMax(76.2);
        production5Asset.setEfficiencyExport(86);
        production5Asset.setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.SOUTH);
        production5Asset.setManufacturer("S-Energy");
        production5Asset.setModel("SN260P-10");
        production5Asset.setPanelAzimuth(50);
        production5Asset.setPanelPitch(15);
        production5Asset.setId(UniqueIdentifierGenerator.generateId(production5Asset.getName()));
        production5Asset = assetStorageService.merge(production5Asset);

        // ### Weather ###
        HTTPAgent weatherHttpApiAgent = new HTTPAgent("Weather Agent");
        weatherHttpApiAgent.setParent(energyManagement);
        weatherHttpApiAgent.setBaseURI("https://api.openweathermap.org/data/2.5/");

        ValueType.MultivaluedStringMap queryParams = new ValueType.MultivaluedStringMap();
        queryParams.put("appid", Collections.singletonList("c3ecbf09be5267cd280676a01acd3360"));
        queryParams.put("lat", Collections.singletonList("51.918849"));
        queryParams.put("lon", Collections.singletonList("4.463250"));
        queryParams.put("units", Collections.singletonList("metric"));
        weatherHttpApiAgent.setRequestQueryParameters(queryParams);

        ValueType.MultivaluedStringMap headers = new ValueType.MultivaluedStringMap();
        headers.put("Accept", Collections.singletonList("application/json"));
        weatherHttpApiAgent.setRequestHeaders(headers);

        weatherHttpApiAgent = assetStorageService.merge(weatherHttpApiAgent);
        weatherHttpApiAgentId = weatherHttpApiAgent.getId();

        WeatherAsset weather = new WeatherAsset("Weather");
        weather.setParent(energyManagement);
        weather.setId(UniqueIdentifierGenerator.generateId(weather.getName()));

        HTTPAgentLink agentLink = new HTTPAgentLink(weatherHttpApiAgentId);
        agentLink.setPath("weather");
        agentLink.setPollingMillis((int) halfHourInMillis);

        weather.getAttributes().addOrReplace(
                new Attribute<>("currentWeather")
                        .addMeta(
                                new MetaItem<>(MetaItemType.AGENT_LINK, agentLink),
                                new MetaItem<>(MetaItemType.LABEL, "Open Weather Map API weather end point"),
                                new MetaItem<>(MetaItemType.READ_ONLY, true),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS, false),
                                new MetaItem<>(MetaItemType.RULE_STATE, false),
                                new MetaItem<>(MetaItemType.ATTRIBUTE_LINKS, new AttributeLink[] {
                                        createWeatherApiAttributeLink(weather.getId(), "main", "temp", "temperature"),
                                        createWeatherApiAttributeLink(weather.getId(), "main", "humidity", "humidity"),
                                        createWeatherApiAttributeLink(weather.getId(), "wind", "speed", "windSpeed"),
                                        createWeatherApiAttributeLink(weather.getId(), "wind", "deg", "windDirection")
                                })));
        weather.getAttribute("windSpeed").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
                    new MetaItem<>(MetaItemType.RULE_STATE));
        });
        weather.getAttribute("temperature").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
                    new MetaItem<>(MetaItemType.RULE_STATE));
        });
        weather.getAttribute("windDirection").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
                    new MetaItem<>(MetaItemType.RULE_STATE));
        });
        weather.getAttribute("humidity").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(
                    new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
                    new MetaItem<>(MetaItemType.RULE_STATE));
        });
        new Attribute<>(Asset.LOCATION, new GeoJSONPoint(4.463250, 51.918849));
        weather = assetStorageService.merge(weather);
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
