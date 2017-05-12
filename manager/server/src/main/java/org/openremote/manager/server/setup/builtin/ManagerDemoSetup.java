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
package org.openremote.manager.server.setup.builtin;

import com.vividsolutions.jts.geom.Coordinate;
import org.openremote.agent.protocol.macro.MacroAction;
import org.openremote.agent.protocol.macro.MacroProtocol;
import org.openremote.agent.protocol.simulator.SimulatorProtocol;
import org.openremote.agent.protocol.simulator.element.ColorSimulatorElement;
import org.openremote.agent.protocol.simulator.element.DecimalSimulatorElement;
import org.openremote.agent.protocol.simulator.element.IntegerSimulatorElement;
import org.openremote.agent.protocol.simulator.element.SwitchSimulatorElement;
import org.openremote.agent.protocol.timer.TimerValue;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetState;
import org.openremote.model.attribute.*;
import org.openremote.model.value.Values;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.openremote.agent.protocol.macro.MacroProtocol.META_MACRO_ACTION_INDEX;
import static org.openremote.agent.protocol.timer.TimerConfiguration.initTimerConfiguration;
import static org.openremote.agent.protocol.timer.TimerProtocol.META_TIMER_VALUE_LINK;
import static org.openremote.model.asset.AssetMeta.*;
import static org.openremote.model.asset.AssetType.*;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;
import static org.openremote.model.attribute.AttributeType.*;

public class ManagerDemoSetup extends AbstractManagerSetup {

    final protected boolean importDemoScenes;

    // Update these numbers whenever you change a RULE_STATE flag in test data
    public static final int DEMO_RULE_STATES_APARTMENT_1 = 11;
    public static final int DEMO_RULE_STATES_APARTMENT_2 = 3;
    public static final int DEMO_RULE_STATES_APARTMENT_3 = 0;
    public static final int DEMO_RULE_STATES_SMART_HOME = DEMO_RULE_STATES_APARTMENT_1 + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_CUSTOMER_A = DEMO_RULE_STATES_SMART_HOME;
    public static final int DEMO_RULE_STATES_GLOBAL = DEMO_RULE_STATES_CUSTOMER_A;

    public static final int DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1 + 28;
    public static final int DEMO_RULE_STATES_SMART_HOME_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_CUSTOMER_A_WITH_SCENES = DEMO_RULE_STATES_SMART_HOME_WITH_SCENES;
    public static final int DEMO_RULE_STATES_GLOBAL_WITH_SCENES = DEMO_RULE_STATES_CUSTOMER_A_WITH_SCENES;

    public String smartOfficeId;
    public String groundFloorId;
    public String lobbyId;
    public String agentId;
    public final String agentProtocolConfigName = "simulator123";
    public String thingId;
    public String smartHomeId;
    public String apartment1Id;
    public String apartment1ServiceAgentId;
    public String apartment1LivingroomId;
    public String apartment2Id;
    public String apartment3Id;
    public String apartment2LivingroomId;
    public String apartment3LivingroomId;
    public String masterRealmId;
    public String customerARealmId;

    public ManagerDemoSetup(Container container, boolean importDemoScenes) {
        super(container);
        this.importDemoScenes = importDemoScenes;
    }

    @Override
    public void execute() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        Tenant customerATenant = keycloakDemoSetup.customerATenant;
        masterRealmId = masterTenant.getId();
        customerARealmId = customerATenant.getId();

        // ################################ Demo assets for 'master' realm ###################################


        ServerAsset smartOffice = new ServerAsset();
        smartOffice.setRealmId(masterTenant.getId());
        smartOffice.setName("Smart Office");
        smartOffice.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        smartOffice.setType(BUILDING);
        List<AssetAttribute> smartOfficeAttributes = Arrays.asList(
            new AssetAttribute("geoStreet", STRING, Values.create("Torenallee 20"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Street")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoStreet"))
                ),
            new AssetAttribute("geoPostalCode", AttributeType.INTEGER, Values.create(5617))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Postal Code")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoPostalCode"))
                ),
            new AssetAttribute("geoCity", STRING, Values.create("Eindhoven"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("City")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoCity"))
                ),
            new AssetAttribute("geoCountry", STRING, Values.create("Netherlands"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Country")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoCountry"))
                )
        );

        smartOffice.setAttributes(smartOfficeAttributes);
        smartOffice = assetStorageService.merge(smartOffice);
        smartOfficeId = smartOffice.getId();

        ServerAsset groundFloor = new ServerAsset("Ground Floor", FLOOR, smartOffice);
        groundFloor.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        groundFloor = assetStorageService.merge(groundFloor);
        groundFloorId = groundFloor.getId();

        ServerAsset lobby = new ServerAsset("Lobby", ROOM, groundFloor);
        lobby.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        lobby = assetStorageService.merge(lobby);
        lobbyId = lobby.getId();

        ServerAsset agent = new ServerAsset("Demo Agent", AGENT, lobby);
        agent.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        agent.setAttributes(
            initProtocolConfiguration(new AssetAttribute(agentProtocolConfigName), SimulatorProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        SimulatorProtocol.CONFIG_MODE,
                        Values.create(SimulatorProtocol.Mode.WRITE_THROUGH_DELAYED.toString())
                    ),
                    new MetaItem(
                        SimulatorProtocol.CONFIG_WRITE_DELAY_MILLISECONDS,
                        Values.create(500)
                    ))
        );

        agent = assetStorageService.merge(agent);
        agentId = agent.getId();

        ServerAsset thing = new ServerAsset("Demo Thing", THING, agent);
        thing.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        thing.setAttributes(
            new AssetAttribute("light1Toggle", BOOLEAN, Values.create(true))
                .setMeta(new Meta(
                    new MetaItem(
                        LABEL,
                        Values.create("Light 1 Toggle")),
                    new MetaItem(
                        DESCRIPTION,
                        Values.create("Switch for living room light")),
                    new MetaItem(
                        AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).toArrayValue()),
                    new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(SwitchSimulatorElement.ELEMENT_NAME)
                    ))
                ),
            new AssetAttribute("light1Dimmer", PERCENTAGE) // No initial value!
                .setMeta(new Meta(
                        new MetaItem(
                            LABEL,
                            Values.create("Light 1 Dimmer")),
                        new MetaItem(
                            DESCRIPTION,
                            Values.create("Dimmer for living room light")),
                        new MetaItem(
                            RANGE_MIN,
                            Values.create(0)),
                        new MetaItem(
                            RANGE_MAX,
                            Values.create(100)),
                        new MetaItem(
                            AGENT_LINK,
                            new AttributeRef(agent.getId(), agentProtocolConfigName).toArrayValue()),
                        new MetaItem(
                            SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(IntegerSimulatorElement.ELEMENT_NAME_RANGE)),
                        new MetaItem(
                            SimulatorProtocol.CONFIG_MODE, Values.create(true))
                    )
                ),
            new AssetAttribute("light1Color", COLOR_RGB, new ColorRGB(88, 123, 88).asArrayValue())
                .setMeta(new Meta(
                        new MetaItem(
                            LABEL,
                            Values.create("Light 1 Color")),
                        new MetaItem(
                            DESCRIPTION,
                            Values.create("Color of living room light")),
                        new MetaItem(
                            AGENT_LINK,
                            new AttributeRef(agent.getId(), agentProtocolConfigName).toArrayValue()),
                        new MetaItem(
                            SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(ColorSimulatorElement.ELEMENT_NAME))
                    )
                ),
            new AssetAttribute("light1PowerConsumption", ENERGY_KWH, Values.create(12.345))
                .setMeta(new Meta(
                        new MetaItem(
                            LABEL,
                            Values.create("Light 1 Usage")),
                        new MetaItem(
                            DESCRIPTION,
                            Values.create("Total energy consumption of living room light")),
                        new MetaItem(
                            READ_ONLY,
                            Values.create(true)),
                        new MetaItem(
                            FORMAT,
                            Values.create("%3d kWh")),
                        new MetaItem(
                            AGENT_LINK,
                            new AttributeRef(agent.getId(), agentProtocolConfigName).toArrayValue()),
                        new MetaItem(
                            SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME)),
                        new MetaItem(
                            STORE_DATA_POINTS.getUrn(), Values.create(true))
                    )
                )
        );
        thing = assetStorageService.merge(thing);
        thingId = thing.getId();

        // Some sample datapoints
        thing = assetStorageService.find(thingId, true);
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.systemDefault());

        AssetAttribute light1PowerConsumptionAttribute = thing.getAttribute("light1PowerConsumption")
            .orElseThrow(() -> new RuntimeException("Invalid test data"));

        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(0.11), now.minusDays(80).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(1.22), now.minusDays(40).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(2.33), now.minusDays(20).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(3.44), now.minusDays(10).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(4.55), now.minusDays(8).toEpochSecond() * 1000);

        light1PowerConsumptionAttribute.setValue(Values.create(5.66), now.minusDays(6).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(6.77), now.minusDays(3).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(7.88), now.minusDays(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(8.99), now.minusHours(10).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(9.11), now.minusHours(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(10.22), now.minusHours(2).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(11.33), now.minusHours(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(11.44), now.minusMinutes(30).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(12.00), now.minusMinutes(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(12.11), now.minusSeconds(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        light1PowerConsumptionAttribute.setValue(Values.create(12.22), now.minusSeconds(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute, false, null));

        // ################################ Demo assets for 'customerA' realm ###################################

        ServerAsset smartHome = new ServerAsset();
        smartHome.setRealmId(customerATenant.getId());
        smartHome.setName("Smart Home");
        smartHome.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        smartHome.setType(BUILDING);
        smartHome.setAttributes(
            new AssetAttribute("geoStreet", STRING, Values.create("Wilhelminaplein 21C"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Street")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoStreet"))
                ),
            new AssetAttribute("geoPostalCode", AttributeType.INTEGER, Values.create(5611))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Postal Code")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoPostalCode"))
                ),
            new AssetAttribute("geoCity", STRING, Values.create("Eindhoven"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("City")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoCity"))
                ),
            new AssetAttribute("geoCountry", STRING, Values.create("Netherlands"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Country")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoCountry"))
                )
        );
        smartHome = assetStorageService.merge(smartHome);
        smartHomeId = smartHome.getId();

        // The "Apartment 1" is the demo apartment with complex scenes
        ServerAsset apartment1 = createDemoApartment(smartHome, "Apartment 1", new Coordinate(5.470945, 51.438000));
        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        ServerAsset apartment1Livingroom = createDemoApartmentRoom(apartment1, "Living Room");
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        ServerAsset apartment1ServiceAgent = createDemoApartmentServiceAgent(apartment1);
        apartment1ServiceAgent = assetStorageService.merge(apartment1ServiceAgent);
        apartment1ServiceAgentId = apartment1ServiceAgent.getId();

        linkDemoApartmentWithServiceAgent(apartment1, apartment1ServiceAgent);
        apartment1 = assetStorageService.merge(apartment1);

        linkDemoApartmentRoomWithServiceAgent(apartment1Livingroom, apartment1ServiceAgent);
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);

        if (importDemoScenes) {
            Scene[] scenes = new Scene[]{
                new Scene("homeScene", "Home scene", "HOME", "0 0 7 ? *", false, 21d),
                new Scene("awayScene", "Away scene", "AWAY", "0 30 8 ? *", true, 15d),
                new Scene("eveningScene", "Evening scene", "EVENING", "0 30 17 ? *", false, 22d),
                new Scene("nightScene", "Night scene", "NIGHT", "0 0 22 ? *", true, 19d)
            };

            ServerAsset demoApartmentSceneAgent = createDemoApartmentSceneAgent(
                apartment1, scenes, apartment1Livingroom
            );
            demoApartmentSceneAgent = assetStorageService.merge(demoApartmentSceneAgent);

            linkDemoApartmentWithSceneAgent(apartment1, demoApartmentSceneAgent, scenes);
            apartment1 = assetStorageService.merge(apartment1);
        }

        ServerAsset apartment2 = new ServerAsset("Apartment 2", RESIDENCE, smartHome);
        apartment2.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2.setAttributes(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Values.create(true))
                .setMeta(
                    new MetaItem(LABEL, Values.create("All Lights Off Switch")),
                    new MetaItem(DESCRIPTION, Values.create("When triggered, turns all lights in the apartment off")),
                    new MetaItem(RULE_EVENT, Values.create(true)),
                    new MetaItem(RULE_EVENT_EXPIRES, Values.create("10s"))
                )
        );
        apartment2 = assetStorageService.merge(apartment2);
        apartment2Id = apartment2.getId();

        ServerAsset apartment2Livingroom = new ServerAsset("Living Room", ROOM, apartment2);
        apartment2Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2Livingroom.setAttributes(
            new AssetAttribute("motionSensor", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Motion Sensor")),
                    new MetaItem(DESCRIPTION, Values.create("PIR sensor that sends 'true' when motion is sensed")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(RULE_EVENT, Values.create(true))
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Presence Detected")),
                    new MetaItem(DESCRIPTION, Values.create("Someone is currently present in the room")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lastPresenceDetected", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Last Presence Timestamp")),
                    new MetaItem(DESCRIPTION, Values.create("Timestamp of last detected presence")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Values.create(true))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Light Switch")),
                    new MetaItem(RULE_STATE, Values.create(false))
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Window Open"))
                )
        );
        apartment2Livingroom = assetStorageService.merge(apartment2Livingroom);
        apartment2LivingroomId = apartment2Livingroom.getId();

        ServerAsset apartment3 = new ServerAsset("Apartment 3", RESIDENCE, smartHome);
        apartment3.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));

        apartment3 = assetStorageService.merge(apartment3);
        apartment3Id = apartment3.getId();

        ServerAsset apartment3Livingroom = new ServerAsset("Living Room", ROOM, apartment3);
        apartment3Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment3Livingroom.addAttributes(
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN)
        );

        apartment3Livingroom = assetStorageService.merge(apartment3Livingroom);
        apartment3LivingroomId = apartment3Livingroom.getId();

        // ################################ Link demo users and assets ###################################

        identityService.setRestrictedUser(keycloakDemoSetup.testuser3Id, true);
        assetStorageService.storeUserAsset(keycloakDemoSetup.testuser3Id, apartment1Id);
        assetStorageService.storeUserAsset(keycloakDemoSetup.testuser3Id, apartment1LivingroomId);
        assetStorageService.storeUserAsset(keycloakDemoSetup.testuser3Id, apartment2Id);
    }

    // ################################ Demo apartment with complex scenes ###################################

    protected ServerAsset createDemoApartment(ServerAsset parent, String name, Coordinate location) {
        ServerAsset apartment = new ServerAsset(name, RESIDENCE, parent);
        apartment.setLocation(geometryFactory.createPoint(location));
        apartment.setAttributes(
            new AssetAttribute("alarmEnabled", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(new Meta(
                    new MetaItem(LABEL, Values.create("Alarm enabled")),
                    new MetaItem(READ_ONLY, Values.create(true)),
                    new MetaItem(DESCRIPTION, Values.create("Send notifications when presence is detected")),
                    new MetaItem(RULE_STATE, Values.create(true))
                )),
            new AssetAttribute("vacationDays", AttributeType.INTEGER)
                .setMeta(new Meta(
                    new MetaItem(LABEL, Values.create("Vacation days")),
                    new MetaItem(DESCRIPTION, Values.create("Enable vacation mode for given days")),
                    new MetaItem(RULE_STATE, Values.create(true))
                )),
            new AssetAttribute("autoSceneSchedule", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Automatic scene schedule")),
                    new MetaItem(DESCRIPTION, Values.create("Predict presence and automatically adjust scene schedule")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lastExecutedScene", AttributeType.STRING, Values.create("HOME"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Last executed scene")),
                    new MetaItem(READ_ONLY, Values.create(true)),
                    new MetaItem(RULE_STATE, Values.create(true))
                )
        );
        return apartment;
    }

    protected ServerAsset createDemoApartmentRoom(ServerAsset apartment, String name) {
        ServerAsset room = new ServerAsset(name, ROOM, apartment);
        room.setLocation(apartment.getLocation());
        room.setAttributes(
            new AssetAttribute("motionCount", AttributeType.INTEGER)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Motion Count")),
                    new MetaItem(DESCRIPTION, Values.create("Sensor that increments a counter when motion is detected")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(RULE_EVENT, Values.create(true))
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Presence Detected")),
                    new MetaItem(DESCRIPTION, Values.create("Someone is currently present in the room")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true)),
                    new MetaItem(READ_ONLY, Values.create(true))
                ),
            new AssetAttribute("lastPresenceDetected", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Last Presence Timestamp")),
                    new MetaItem(DESCRIPTION, Values.create("Timestamp of last detected presence")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true)),
                    new MetaItem(READ_ONLY, Values.create(true))
                ),
            new AssetAttribute("co2Level", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("CO2 Level")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true)),
                    new MetaItem(READ_ONLY, Values.create(true))
                ),
            new AssetAttribute("humidity", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Humidity")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true)),
                    new MetaItem(READ_ONLY, Values.create(true))
                ),
            new AssetAttribute("currentTemperature", DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Current temperature")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true)),
                    new MetaItem(READ_ONLY, Values.create(true))
                ),
            new AssetAttribute("targetTemperature", DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Target Temperature")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true))
                )
        );
        return room;
    }

    protected ServerAsset createDemoApartmentServiceAgent(ServerAsset apartment) {
        ServerAsset agent = new ServerAsset("Service Agent (Simulator)", AGENT, apartment);
        agent.setLocation(apartment.getLocation());
        agent.setAttributes(
            initProtocolConfiguration(new AssetAttribute("apartmentSimulator"), SimulatorProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        SimulatorProtocol.CONFIG_MODE,
                        Values.create(SimulatorProtocol.Mode.WRITE_THROUGH_DELAYED.toString())
                    ),
                    new MetaItem(
                        SimulatorProtocol.CONFIG_WRITE_DELAY_MILLISECONDS,
                        Values.create(500)
                    )),
            initProtocolConfiguration(new AssetAttribute("roomSimulator"), SimulatorProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        SimulatorProtocol.CONFIG_MODE,
                        Values.create(SimulatorProtocol.Mode.WRITE_THROUGH_DELAYED.toString())
                    ),
                    new MetaItem(
                        SimulatorProtocol.CONFIG_WRITE_DELAY_MILLISECONDS,
                        Values.create(500)
                    ))
        );
        return agent;
    }

    protected void linkDemoApartmentWithServiceAgent(ServerAsset apartment, ServerAsset agent) {
        for (AssetAttribute attribute : apartment.getAttributesList()) {
            String name = attribute.getName().orElseThrow(
                () -> new IllegalStateException("Missing apartment attribute name: " + attribute)
            );
            switch (name) {
                // TODO Any attributes of apartment to link with simulator?
            }
        }
    }

    protected void linkDemoApartmentRoomWithServiceAgent(ServerAsset room, ServerAsset agent) {
        for (AssetAttribute attribute : room.getAttributesList()) {
            String name = attribute.getName().orElseThrow(
                () -> new IllegalStateException("Missing apartment room attribute name: " + attribute)
            );
            switch (name) {
                case "motionCount":
                    attribute.getMeta().add(
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), "roomSimulator").toArrayValue()),
                        new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(IntegerSimulatorElement.ELEMENT_NAME_INTEGER))
                    );
                    break;
                case "co2Level":
                    attribute.getMeta().add(
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), "roomSimulator").toArrayValue()),
                        new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME))
                    );
                    break;
                case "humidity":
                    attribute.getMeta().add(
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), "roomSimulator").toArrayValue()),
                        new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME))
                    );
                    break;
                case "currentTemperature":
                    attribute.getMeta().add(
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), "roomSimulator").toArrayValue()),
                        new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME))
                    );
                    break;
                case "targetTemperature":
                    attribute.getMeta().add(
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), "roomSimulator").toArrayValue()),
                        new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME))
                    );
                    break;
            }
        }
    }

    public static class Scene {

        final String attributeName;
        final String attributeLabel;
        final String internalName;
        final String startTime;
        final boolean alarmEnabled;
        final double targetTemperature;

        public Scene(String attributeName,
                     String attributeLabel,
                     String internalName,
                     String startTime,
                     boolean alarmEnabled,
                     double targetTemperature) {
            this.attributeName = attributeName;
            this.attributeLabel = attributeLabel;
            this.internalName = internalName;
            this.startTime = startTime;
            this.alarmEnabled = alarmEnabled;
            this.targetTemperature = targetTemperature;
        }

        AssetAttribute createMacroAttribute(ServerAsset apartment, ServerAsset... rooms) {
            AssetAttribute attribute = initProtocolConfiguration(new AssetAttribute(attributeName), MacroProtocol.PROTOCOL_NAME)
                .addMeta(new MetaItem(LABEL, Values.create(attributeLabel)));
            attribute.getMeta().add(
                new MacroAction(new AttributeState(new AttributeRef(apartment.getId(), "alarmEnabled"), Values.create(alarmEnabled))).toMetaItem()
            );
            for (ServerAsset room : rooms) {
                attribute.getMeta().add(
                    new MacroAction(new AttributeState(new AttributeRef(room.getId(), "targetTemperature"), Values.create(targetTemperature))).toMetaItem()
                );
            }
            attribute.getMeta().add(
                new MacroAction(new AttributeState(new AttributeRef(apartment.getId(), "lastExecutedScene"), Values.create(internalName))).toMetaItem()
            );
            return attribute;
        }

        AssetAttribute[] createTimerAttributes(ServerAsset apartment) {
            List<AssetAttribute> attributes = new ArrayList<>();
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                // "MONDAY" => "Monday"
                String dayName = dayOfWeek.name().substring(0, 1) + dayOfWeek.name().substring(1).toLowerCase(Locale.ROOT);
                // "0 0 7 ? *" => "0 0 7 ? * MON *"
                String timePattern = startTime + " " + dayOfWeek.name().substring(0, 3).toUpperCase(Locale.ROOT) + " *";
                attributes.add(
                    initTimerConfiguration(new AssetAttribute(attributeName + dayName), timePattern,
                        new AttributeState(apartment.getId(), attributeName, Values.create("REQUEST_START")))
                        .addMeta(new MetaItem(LABEL, Values.create(attributeLabel + " trigger " + dayName)))
                );
            }
            return attributes.toArray(new AssetAttribute[attributes.size()]);
        }
    }

    protected ServerAsset createDemoApartmentSceneAgent(ServerAsset apartment, Scene[] scenes, ServerAsset... rooms) {
        ServerAsset agent = new ServerAsset("Scene Agent", AGENT, apartment);
        agent.setLocation(apartment.getLocation());
        for (Scene scene : scenes) {
            agent.addAttributes(scene.createMacroAttribute(apartment, rooms));
        }
        for (Scene scene : scenes) {
            agent.addAttributes(scene.createTimerAttributes(apartment));
        }
        return agent;
    }

    protected void linkDemoApartmentWithSceneAgent(ServerAsset apartment, ServerAsset agent, Scene[] scenes) {
        for (Scene scene : scenes) {
            apartment.addAttributes(
                new AssetAttribute(scene.attributeName, AttributeType.STRING, Values.create(AttributeExecuteStatus.READY.name()))
                    .setMeta(
                        new MetaItem(LABEL, Values.create(scene.attributeLabel)),
                        new MetaItem(EXECUTABLE, Values.create(true)),
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), scene.attributeName).toArrayValue())
                    ),
                new AssetAttribute(scene.attributeName + "AlarmEnabled", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem(LABEL, Values.create(scene.attributeLabel + " alarm enabled")),
                        new MetaItem(META_MACRO_ACTION_INDEX, Values.create(0)),
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), scene.attributeName).toArrayValue())
                    ),
                new AssetAttribute(scene.attributeName + "TargetTemperature", AttributeType.DECIMAL)
                    .setMeta(
                        new MetaItem(LABEL, Values.create(scene.attributeLabel + " target temperature")),
                        new MetaItem(META_MACRO_ACTION_INDEX, Values.create(1)),
                        new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), scene.attributeName).toArrayValue())
                    )
            );
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                // "MONDAY" => "Monday"
                String dayName = dayOfWeek.name().substring(0, 1) + dayOfWeek.name().substring(1).toLowerCase(Locale.ROOT);
                apartment.addAttributes(
                    new AssetAttribute(scene.attributeName + "Time" + dayOfWeek.name(), AttributeType.STRING)
                        .setMeta(
                            new MetaItem(LABEL, Values.create(scene.attributeLabel + " time " + dayName)),
                            new MetaItem(RULE_STATE, Values.create(true)),
                            new MetaItem(META_TIMER_VALUE_LINK, Values.create(TimerValue.TIME.toString())),
                            new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), scene.attributeName + dayName).toArrayValue())
                        ),
                    new AssetAttribute(scene.attributeName + "Enabled" + dayOfWeek.name(), AttributeType.BOOLEAN)
                        .setMeta(
                            new MetaItem(LABEL, Values.create(scene.attributeLabel + " enabled " + dayName)),
                            new MetaItem(META_TIMER_VALUE_LINK, Values.create(TimerValue.ENABLED.toString())),
                            new MetaItem(AGENT_LINK, new AttributeRef(agent.getId(), scene.attributeName + dayName).toArrayValue())
                        )
                );
            }
        }
    }
}