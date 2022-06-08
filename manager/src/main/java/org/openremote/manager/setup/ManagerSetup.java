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
package org.openremote.manager.setup;

import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.persistence.ManagerPersistenceService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.rules.RulesetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.*;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.setup.Setup;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueFormat;
import org.openremote.model.value.ValueType;

import jline.internal.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.openremote.model.Constants.*;
import static org.openremote.model.value.MetaItemType.*;
import static org.openremote.model.value.ValueType.*;
import static org.openremote.container.util.MapAccess.getString;

public class ManagerSetup implements Setup {

    public static final String OR_PROVISIONING_DOCROOT = "OR_PROVISIONING_DOCROOT";
    public static final String OR_PROVISIONING_DOCROOT_DEFAULT = "deployment/manager/provisioning";

    protected Path provisionDocRoot;

    final protected ScheduledExecutorService executorService;
    final protected ManagerPersistenceService persistenceService;
    final protected ManagerIdentityService identityService;
    final protected AssetStorageService assetStorageService;
    final protected AssetProcessingService assetProcessingService;
    final protected AssetDatapointService assetDatapointService;
    final protected AssetPredictedDatapointService assetPredictedDatapointService;
    final protected RulesetStorageService rulesetStorageService;
    final protected SetupService setupService;
    final protected MetaItem<?>[] EMPTY_META = new MetaItem<?>[0];

    public ManagerSetup(Container container) {
        this.executorService = container.getExecutorService();
        this.persistenceService = container.getService(ManagerPersistenceService.class);
        this.identityService = container.getService(ManagerIdentityService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.assetProcessingService = container.getService(AssetProcessingService.class);
        this.assetDatapointService = container.getService(AssetDatapointService.class);
        this.assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        this.rulesetStorageService = container.getService(RulesetStorageService.class);
        this.setupService = container.getService(SetupService.class);

        provisionDocRoot = Paths.get(getString(container.getConfig(), OR_PROVISIONING_DOCROOT, OR_PROVISIONING_DOCROOT_DEFAULT));
    }

    @Override
    public void onStart() throws Exception {

        if (!Files.exists(Paths.get(provisionDocRoot.toString()))) {
            return;
        }

        provisionAssets();
    }

    @Override
    public void onInit() throws Exception {
    }

    // ################################ Demo apartment with complex scenes
    // ###################################

    protected BuildingAsset createDemoApartment(Asset<?> parent, String name, GeoJSONPoint location) {
        BuildingAsset apartment = new BuildingAsset(name);

        apartment.setParent(parent);
        apartment.addOrReplaceAttributes(
                new Attribute<>("alarmEnabled", BOOLEAN).addMeta(new MetaItem<>(LABEL, "Alarm enabled"),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(ACCESS_RESTRICTED_WRITE, true),
                        new MetaItem<>(RULE_STATE, true)),
                new Attribute<>("presenceDetected", BOOLEAN).addMeta(new MetaItem<>(LABEL, "Presence detected"),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(READ_ONLY, true),
                        new MetaItem<>(RULE_STATE, true), new MetaItem<>(STORE_DATA_POINTS, true)),
                new Attribute<>("vacationUntil", TIMESTAMP).addMeta(new MetaItem<>(LABEL, "Vacation until"),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(ACCESS_RESTRICTED_WRITE, true),
                        new MetaItem<>(RULE_STATE, true)),
                new Attribute<>("lastExecutedScene", ValueType.TEXT).addMeta(
                        new MetaItem<>(LABEL, "Last executed scene"), new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                        new MetaItem<>(READ_ONLY, true), new MetaItem<>(RULE_STATE, true)),
                new Attribute<>(Asset.LOCATION, location).addMeta(new MetaItem<>(ACCESS_RESTRICTED_READ, true))
        /*
         * TODO Unused, can be removed? Port schedule prediction from DRL... new
         * Attribute<>("autoSceneSchedule", ValueType.BOOLEAN) .addMeta( new
         * MetaItem<>(LABEL, "Automatic scene schedule"), new MetaItem<>(DESCRIPTION,
         * "Predict presence and automatically adjust scene schedule"), new
         * MetaItem<>(ACCESS_RESTRICTED_READ, true), new
         * MetaItem<>(ACCESS_RESTRICTED_WRITE, true), new MetaItem<>(RULE_STATE, true)
         * ), new Attribute<>("lastDetectedScene", ValueType.STRING) .addMeta( new
         * MetaItem<>(LABEL, "Last detected scene by rules"), new MetaItem<>(READ_ONLY,
         * true), new MetaItem<>(RULE_STATE, true) )
         */
        );
        return apartment;
    }

    protected RoomAsset createDemoApartmentRoom(Asset<?> apartment, String name) {
        RoomAsset room = new RoomAsset(name);
        room.setParent(apartment);
        return room;
    }

    protected void addDemoApartmentRoomMotionSensor(RoomAsset room, boolean shouldBeLinked,
            Supplier<AgentLink<?>> agentLinker) {
        room.getAttributes().addOrReplace(
                new Attribute<>("motionSensor", INTEGER).addMeta(new MetaItem<>(LABEL, "Motion sensor"),
                        new MetaItem<>(READ_ONLY, true), new MetaItem<>(RULE_STATE, true),
                        new MetaItem<>(STORE_DATA_POINTS)),
                new Attribute<>("presenceDetected", BOOLEAN).addMeta(new MetaItem<>(LABEL, "Presence detected"),
                        new MetaItem<>(RULE_STATE, true), new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                        new MetaItem<>(READ_ONLY, true), new MetaItem<>(STORE_DATA_POINTS, true)),
                new Attribute<>("firstPresenceDetected", TIMESTAMP).addMeta(
                        new MetaItem<>(LABEL, "First time movement was detected"), new MetaItem<>(READ_ONLY, true),
                        new MetaItem<>(RULE_STATE, true)),
                new Attribute<>("lastPresenceDetected", TIMESTAMP).addMeta(
                        new MetaItem<>(LABEL, "Last time movement was detected"), new MetaItem<>(READ_ONLY, true),
                        new MetaItem<>(RULE_STATE, true)));

        if (shouldBeLinked) {
            room.getAttribute("motionSensor")
                    .ifPresent(attr -> attr.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get())));
            room.getAttribute("presenceDetected")
                    .ifPresent(attr -> attr.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get())));
        }
    }

    protected void addDemoApartmentRoomCO2Sensor(RoomAsset room, boolean shouldBeLinked,
            Supplier<AgentLink<?>> agentLinker) {
        room.getAttributes().addOrReplace(new Attribute<>("co2Level", POSITIVE_INTEGER).addMeta(
                new MetaItem<>(UNITS, Constants.units(UNITS_PART_PER_MILLION)), new MetaItem<>(LABEL, "CO2 level"),
                new MetaItem<>(RULE_STATE, true), new MetaItem<>(RULE_EVENT, true),
                new MetaItem<>(RULE_EVENT_EXPIRES, "PT45M"), new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                new MetaItem<>(READ_ONLY, true), new MetaItem<>(STORE_DATA_POINTS)));

        if (shouldBeLinked) {
            room.getAttribute("co2Level")
                    .ifPresent(attr -> attr.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get())));
        }
    }

    protected void addDemoApartmentRoomHumiditySensor(RoomAsset room, boolean shouldBeLinked,
            Supplier<AgentLink<?>> agentLinker) {
        room.getAttributes()
                .addOrReplace(new Attribute<>("humidity", POSITIVE_INTEGER).addMeta(new MetaItem<>(LABEL, "Humidity"),
                        new MetaItem<>(RULE_STATE, true), new MetaItem<>(RULE_EVENT, true),
                        new MetaItem<>(RULE_EVENT_EXPIRES, "PT45M"), new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                        new MetaItem<>(READ_ONLY, true), new MetaItem<>(UNITS, Constants.units(UNITS_PERCENTAGE)),
                        new MetaItem<>(STORE_DATA_POINTS)));

        if (shouldBeLinked) {
            room.getAttribute("humidity")
                    .ifPresent(attr -> attr.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get())));
        }
    }

    protected void addDemoApartmentRoomThermometer(RoomAsset room, boolean shouldBeLinked,
            Supplier<AgentLink<?>> agentLinker) {
        room.getAttributes()
                .addOrReplace(new Attribute<>("currentTemperature", NUMBER).addMeta(
                        new MetaItem<>(LABEL, "Current temperature"), new MetaItem<>(RULE_STATE, true),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(READ_ONLY, true),
                        new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS)),
                        new MetaItem<>(FORMAT, ValueFormat.NUMBER_1_DP()), new MetaItem<>(STORE_DATA_POINTS)));

        if (shouldBeLinked) {
            room.getAttribute("currentTemperature")
                    .ifPresent(attr -> attr.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get())));
        }
    }

    protected void addDemoApartmentTemperatureControl(RoomAsset room, boolean shouldBeLinked,
            Supplier<AgentLink<?>> agentLinker) {
        room.getAttributes()
                .addOrReplace(new Attribute<>("targetTemperature", NUMBER).addMeta(
                        new MetaItem<>(LABEL, "Target temperature"), new MetaItem<>(RULE_STATE, true),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(ACCESS_RESTRICTED_WRITE, true),
                        new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS)),
                        new MetaItem<>(FORMAT, ValueFormat.NUMBER_1_DP()), new MetaItem<>(STORE_DATA_POINTS)));

        if (shouldBeLinked) {
            room.getAttribute("targetTemperature")
                    .ifPresent(attr -> attr.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get())));
        }
    }

    protected void addDemoApartmentSmartSwitch(RoomAsset room, String switchName, boolean shouldBeLinked,
            // Integer represents attribute:
            // 0 = Mode
            // 1 = Time
            // 2 = StartTime
            // 3 = StopTime
            // 4 = Enabled
            Function<Integer, MetaItem[]> agentLinker) {

        room.getAttributes().addOrReplace(
                // Mode
                new Attribute<>("smartSwitchMode" + switchName, TEXT).addMeta(
                        new MetaItem<>(LABEL, "Smart Switch mode " + switchName),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(ACCESS_RESTRICTED_WRITE, true),
                        new MetaItem<>(RULE_STATE, true), new MetaItem<>(RULE_EVENT, true),
                        new MetaItem<>(RULE_EVENT_EXPIRES, "PT48H")),
                // Time
                new Attribute<>("smartSwitchBeginEnd" + switchName, TIMESTAMP).addMeta(
                        new MetaItem<>(LABEL, "Smart Switch begin/end cycle " + switchName),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(ACCESS_RESTRICTED_WRITE, true),
                        new MetaItem<>(RULE_STATE, true)),
                // StartTime
                new Attribute<>("smartSwitchStartTime" + switchName, TIMESTAMP).addMeta(
                        new MetaItem<>(LABEL, "Smart Switch actuator earliest start time " + switchName),
                        new MetaItem<>(READ_ONLY, true), new MetaItem<>(UNITS, Constants.units(UNITS_SECOND)),
                        new MetaItem<>(RULE_STATE, true)),
                // StopTime
                new Attribute<>("smartSwitchStopTime" + switchName, TIMESTAMP).addMeta(
                        new MetaItem<>(LABEL, "Smart Switch actuator latest stop time " + switchName),
                        new MetaItem<>(READ_ONLY, true), new MetaItem<>(UNITS, Constants.units(UNITS_SECOND)),
                        new MetaItem<>(RULE_STATE, true)),
                // Enabled
                new Attribute<>("smartSwitchEnabled" + switchName, NUMBER).addMeta(
                        new MetaItem<>(LABEL, "Smart Switch actuator enabled " + switchName),
                        new MetaItem<>(READ_ONLY, true), new MetaItem<>(RULE_STATE, true)));

        if (shouldBeLinked) {
            room.getAttribute("smartSwitchBeginEnd").ifPresent(attr -> attr.addMeta(agentLinker.apply(0)));
            room.getAttribute("smartSwitchBeginEnd").ifPresent(attr -> attr.addMeta(agentLinker.apply(1)));
            room.getAttribute("smartSwitchStartTime").ifPresent(attr -> attr.addMeta(agentLinker.apply(2)));
            room.getAttribute("smartSwitchStopTime").ifPresent(attr -> attr.addMeta(agentLinker.apply(3)));
            room.getAttribute("smartSwitchEnabled").ifPresent(attr -> attr.addMeta(agentLinker.apply(4)));
        }
    }

    protected void addDemoApartmentVentilation(BuildingAsset apartment, boolean shouldBeLinked,
            Supplier<AgentLink<?>> agentLinker) {
        apartment.getAttributes().addOrReplace(
                new Attribute<>("ventilationLevel", POSITIVE_INTEGER).addMeta(
                        new MetaItem<>(LABEL, "Ventilation level"), new MetaItem<>(RULE_STATE, true),
                        new MetaItem<>(ACCESS_RESTRICTED_READ, true), new MetaItem<>(ACCESS_RESTRICTED_WRITE, true),
                        new MetaItem<>(STORE_DATA_POINTS)),
                new Attribute<>("ventilationAuto", BOOLEAN).addMeta(new MetaItem<>(LABEL, "Ventilation auto"),
                        new MetaItem<>(RULE_STATE, true), new MetaItem<>(ACCESS_RESTRICTED_READ, true),
                        new MetaItem<>(ACCESS_RESTRICTED_WRITE, true)));

        if (shouldBeLinked) {
            apartment.getAttribute("ventilationLevel")
                    .ifPresent(attr -> attr.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get())));
        }
    }


    protected PeopleCounterAsset createDemoPeopleCounterAsset(String name, Asset<?> area, GeoJSONPoint location,
            Supplier<AgentLink<?>> agentLinker) {
        PeopleCounterAsset peopleCounterAsset = new PeopleCounterAsset(name);
        peopleCounterAsset.setParent(area);
        peopleCounterAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        peopleCounterAsset.getAttribute("peopleCountIn").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS));
            if (agentLinker != null) {
                assetAttribute.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
            }
        });
        peopleCounterAsset.getAttribute("peopleCountOut").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS));
            if (agentLinker != null) {
                assetAttribute.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
            }
        });
        peopleCounterAsset.getAttribute("peopleCountInMinute").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS));
            if (agentLinker != null) {
                assetAttribute.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
            }
        });
        peopleCounterAsset.getAttribute("peopleCountOutMinute").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS));
            if (agentLinker != null) {
                assetAttribute.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
            }
        });
        peopleCounterAsset.getAttribute("peopleCountTotal").ifPresent(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(STORE_DATA_POINTS));
            if (agentLinker != null) {
                assetAttribute.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
            }
        });

        return peopleCounterAsset;
    }

    protected MicrophoneAsset createDemoMicrophoneAsset(String name, Asset<?> area, GeoJSONPoint location,
            Supplier<AgentLink<?>> agentLinker) {
        MicrophoneAsset microphoneAsset = new MicrophoneAsset(name);
        microphoneAsset.setParent(area);
        microphoneAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        microphoneAsset.getAttribute(MicrophoneAsset.SOUND_LEVEL).ifPresent(assetAttribute -> {
            assetAttribute.addMeta(new MetaItem<>(RULE_STATE), new MetaItem<>(READ_ONLY),
                    new MetaItem<>(STORE_DATA_POINTS));
            if (agentLinker != null) {
                assetAttribute.addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
            }
        });

        return microphoneAsset;
    }

    protected EnvironmentSensorAsset createDemoEnvironmentAsset(String name, Asset<?> area, GeoJSONPoint location,
            Supplier<AgentLink<?>> agentLinker) {
        EnvironmentSensorAsset environmentAsset = new EnvironmentSensorAsset(name);
        environmentAsset.setParent(area);
        environmentAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        environmentAsset.getAttributes().getOrCreate(EnvironmentSensorAsset.TEMPERATURE)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
        environmentAsset.getAttributes().getOrCreate(EnvironmentSensorAsset.NO2)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
        environmentAsset.getAttributes().getOrCreate(EnvironmentSensorAsset.RELATIVE_HUMIDITY)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
        environmentAsset.getAttributes().getOrCreate(EnvironmentSensorAsset.PM1)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
        environmentAsset.getAttributes().getOrCreate(EnvironmentSensorAsset.PM2_5)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));
        environmentAsset.getAttributes().getOrCreate(EnvironmentSensorAsset.PM10)
                .addMeta(new MetaItem<>(AGENT_LINK, agentLinker.get()));

        return environmentAsset;
    }

    protected LightAsset createDemoLightAsset(String name, Asset<?> area, GeoJSONPoint location) {
        LightAsset lightAsset = new LightAsset(name);
        lightAsset.setParent(area);
        lightAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        lightAsset.getAttributes().getOrCreate(LightAsset.ON_OFF).addMeta(new MetaItem<>(RULE_STATE),
                new MetaItem<>(STORE_DATA_POINTS));
        lightAsset.getAttributes().getOrCreate(LightAsset.BRIGHTNESS).addMeta(new MetaItem<>(RULE_STATE),
                new MetaItem<>(STORE_DATA_POINTS));
        lightAsset.getAttributes().getOrCreate(LightAsset.COLOUR_RGB).addMeta(new MetaItem<>(RULE_STATE),
                new MetaItem<>(STORE_DATA_POINTS));
        lightAsset.getAttributes().getOrCreate("groupNumber", POSITIVE_INTEGER).addMeta(new MetaItem<>(RULE_STATE),
                new MetaItem<>(STORE_DATA_POINTS));
        lightAsset.getAttributes().getOrCreate("scenario", TEXT).addMeta(new MetaItem<>(RULE_STATE));

        return lightAsset;
    }

    protected LightAsset createDemoLightControllerAsset(String name, Asset<?> area, GeoJSONPoint location) {
        LightAsset lightAsset = new LightAsset(name);
        lightAsset.setParent(area);
        lightAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));
        lightAsset.getAttributes().getOrCreate(LightAsset.ON_OFF).addMeta(new MetaItem<>(RULE_STATE),
                new MetaItem<>(STORE_DATA_POINTS));
        lightAsset.getAttributes().getOrCreate(LightAsset.BRIGHTNESS).addMeta(new MetaItem<>(RULE_STATE),
                new MetaItem<>(STORE_DATA_POINTS));
        lightAsset.getAttributes().getOrCreate(LightAsset.COLOUR_RGB).addMeta(new MetaItem<>(RULE_STATE),
                new MetaItem<>(STORE_DATA_POINTS));
        lightAsset.getAttributes().getOrCreate("scenario", TEXT).addMeta(new MetaItem<>(RULE_STATE));

        return lightAsset;
    }

    protected ElectricityStorageAsset createDemoElectricityStorageAsset(String name, Asset<?> area,
            GeoJSONPoint location) {
        ElectricityStorageAsset electricityStorageAsset = new ElectricityBatteryAsset(name);
        electricityStorageAsset.setParent(area);
        electricityStorageAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityStorageAsset;
    }

    protected ElectricityProducerSolarAsset createDemoElectricitySolarProducerAsset(String name, Asset<?> area,
            GeoJSONPoint location) {
        ElectricityProducerSolarAsset electricityProducerAsset = new ElectricityProducerSolarAsset(name);
        electricityProducerAsset.setParent(area);
        electricityProducerAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityProducerAsset;
    }

    protected ElectricityConsumerAsset createDemoElectricityConsumerAsset(String name, Asset<?> area,
            GeoJSONPoint location) {
        ElectricityConsumerAsset electricityConsumerAsset = new ElectricityConsumerAsset(name);
        electricityConsumerAsset.setParent(area);
        electricityConsumerAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityConsumerAsset;
    }

    protected ElectricityChargerAsset createDemoElectricityChargerAsset(String name, Asset<?> area,
            GeoJSONPoint location) {
        ElectricityChargerAsset electricityChargerAsset = new ElectricityChargerAsset(name);
        electricityChargerAsset.setParent(area);
        electricityChargerAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityChargerAsset;
    }

    protected GroundwaterSensorAsset createDemoGroundwaterAsset(String name, Asset<?> area, GeoJSONPoint location) {
        GroundwaterSensorAsset groundwaterAsset = new GroundwaterSensorAsset(name);
        groundwaterAsset.setParent(area);
        groundwaterAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return groundwaterAsset;
    }

    protected ParkingAsset createDemoParkingAsset(String name, Asset<?> area, GeoJSONPoint location) {
        ParkingAsset parkingAsset = new ParkingAsset(name);
        parkingAsset.setParent(area);
        parkingAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return parkingAsset;
    }

    protected ShipAsset createDemoShipAsset(String name, Asset<?> area, GeoJSONPoint location) {
        ShipAsset shipAsset = new ShipAsset(name);
        shipAsset.setParent(area);
        shipAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return shipAsset;
    }

    protected void provisionAssets() throws IOException {

        if (!Files.exists(Paths.get(provisionDocRoot.toString(), "assets"))) {
            return;
        }

        Log.info("Provisioning assets");

        Files.list(Paths.get(provisionDocRoot.toString(), "assets")).sorted()
                .forEach(file -> {
                    try {
                        Asset<?> asset =  ValueUtil.JSON.readValue(file.toFile(), Asset.class);
                        asset = assetStorageService.merge(asset);

                        Log.info("Asset merged: " + asset.toString());
                    } catch (IOException e) {
                        Log.warn("Processing of file " + file.getFileName() + " went wrong", e);
                    }
                });

    }
}
