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
package org.openremote.manager.setup.builtin;

import org.openremote.agent.protocol.simulator.SimulatorProtocol;
import org.openremote.container.Container;
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter;
import org.openremote.manager.setup.AbstractManagerSetup;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.UserAsset;
import org.openremote.model.attribute.*;
import org.openremote.model.console.ConsoleConfigration;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.security.Tenant;
import org.openremote.model.simulator.element.ColorSimulatorElement;
import org.openremote.model.simulator.element.NumberSimulatorElement;
import org.openremote.model.simulator.element.SwitchSimulatorElement;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.openremote.model.asset.AssetMeta.*;
import static org.openremote.model.asset.AssetType.*;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;
import static org.openremote.model.attribute.AttributeValueType.*;

public class ManagerDemoSetup extends AbstractManagerSetup {

    // Update these numbers whenever you change a RULE_STATE flag in test data
    public static final int DEMO_RULE_STATES_APARTMENT_1 = 37;
    public static final int DEMO_RULE_STATES_APARTMENT_2 = 11;
    public static final int DEMO_RULE_STATES_APARTMENT_3 = 0;
    public static final int DEMO_RULE_STATES_SMART_OFFICE = 2;
    public static final int DEMO_RULE_STATES_SMART_HOME = DEMO_RULE_STATES_APARTMENT_1 + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_CUSTOMER_A = DEMO_RULE_STATES_SMART_HOME;
    public static final int DEMO_RULE_STATES_GLOBAL = DEMO_RULE_STATES_CUSTOMER_A + DEMO_RULE_STATES_SMART_OFFICE;
    public static final int DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1 + 28;
    public static final int DEMO_RULE_STATES_SMART_HOME_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_CUSTOMER_A_WITH_SCENES = DEMO_RULE_STATES_SMART_HOME_WITH_SCENES;
    public static final int DEMO_RULE_STATES_GLOBAL_WITH_SCENES = DEMO_RULE_STATES_CUSTOMER_A_WITH_SCENES;
    public final String agentProtocolConfigName = "simulator123";
    public final String thingLightToggleAttributeName = "light1Toggle";
    final protected boolean importDemoScenes;
    public String smartOfficeId;
    public String groundFloorId;
    public String lobbyId;
    public String agentId;
    public String thingId;
    public String smartHomeId;
    public String apartment1Id;
    public String apartment1ServiceAgentId;
    public String apartment1LivingroomId;
    public String apartment1KitchenId;
    public String apartment1HallwayId;
    public String apartment2Id;
    public String apartment3Id;
    public String apartment2LivingroomId;
    public String apartment2BathroomId;
    public String apartment3LivingroomId;
    public String masterRealmId;
    public String customerARealmId;
    public String consoleId;

    public ManagerDemoSetup(Container container, boolean importDemoScenes) {
        super(container);
        this.importDemoScenes = importDemoScenes;
    }

    @Override
    public void onStart() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        Tenant customerATenant = keycloakDemoSetup.customerATenant;
        masterRealmId = masterTenant.getId();
        customerARealmId = customerATenant.getId();

        // ################################ Demo assets for 'master' realm ###################################

        ObjectValue locationValue = Values.createObject().put("latitude", 51.44541688237109).put("longitude",
                                                                                                 5.460315214821094);

        Asset smartOffice = new Asset();
        smartOffice.setRealmId(masterTenant.getId());
        smartOffice.setName("Smart Office");
        smartOffice.setType(BUILDING);
        List<AssetAttribute> smartOfficeAttributes = Arrays.asList(
            AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValue),
            new AssetAttribute("geoStreet", STRING, Values.create("Torenallee 20"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Street")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoStreet"))
                ),
            new AssetAttribute("geoPostalCode", AttributeValueType.NUMBER, Values.create(5617))
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

        Asset groundFloor = new Asset("Ground Floor", FLOOR, smartOffice)
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValue));
        groundFloor = assetStorageService.merge(groundFloor);
        groundFloorId = groundFloor.getId();

        Asset lobby = new Asset("Lobby", ROOM, groundFloor)
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValue));
        lobby.addAttributes(
            new AssetAttribute("lobbyLocations", AttributeValueType.ARRAY)
        );

        lobby = assetStorageService.merge(lobby);
        lobbyId = lobby.getId();

        Asset agent = new Asset("Demo Agent", AGENT, lobby);
        agent.addAttributes(
            AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValue),
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

        Asset thing = new Asset("Demo Thing", THING, agent)
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValue)
                               .setMeta(new MetaItem(RULE_STATE, Values.create(true))));
        thing.addAttributes(
            new AssetAttribute(thingLightToggleAttributeName, BOOLEAN, Values.create(true))
                .setMeta(new Meta(
                    new MetaItem(
                        LABEL,
                        Values.create("Light 1 Toggle")),
                    new MetaItem(
                        DESCRIPTION,
                        Values.create("Switch for living room light")),
                    new MetaItem(
                        STORE_DATA_POINTS,
                        Values.create(true)),
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
                                 SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME_RANGE)),
                             new MetaItem(
                                 SimulatorProtocol.CONFIG_MODE,
                                 Values.create(SimulatorProtocol.Mode.WRITE_THROUGH_DELAYED.toString()))
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
                                 SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME)),
                             new MetaItem(
                                 STORE_DATA_POINTS, Values.create(true))
                         )
                )
        );
        thing = assetStorageService.merge(thing);
        thingId = thing.getId();

        // Some sample datapoints
        final Asset finalThing = assetStorageService.find(thingId, true);
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.systemDefault());

        AssetAttribute light1PowerConsumptionAttribute = thing.getAttribute("light1PowerConsumption")
            .orElseThrow(() -> new RuntimeException("Invalid test data"));

        persistenceService.doTransaction(em -> {
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(0.11), now.minusDays(80).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(1.22), now.minusDays(40).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(2.33), now.minusDays(20).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(3.44), now.minusDays(10).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(4.55), now.minusDays(8).toEpochSecond() * 1000);

            light1PowerConsumptionAttribute.setValue(Values.create(5.66), now.minusDays(6).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(6.77), now.minusDays(3).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(7.88), now.minusDays(1).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(8.99), now.minusHours(10).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(9.11), now.minusHours(5).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(10.22), now.minusHours(2).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(11.33), now.minusHours(1).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(11.44), now.minusMinutes(30).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(12.00), now.minusMinutes(5).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(12.11), now.minusSeconds(5).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);

            light1PowerConsumptionAttribute.setValue(Values.create(12.22), now.minusSeconds(1).toEpochSecond() * 1000);
            assetDatapointService.processAssetUpdate(em,
                                                     finalThing,
                                                     light1PowerConsumptionAttribute,
                                                     AttributeEvent.Source.SENSOR);
        });

        // ################################ Demo assets for 'customerA' realm ###################################

        ObjectValue locationValueA = Values.createObject().put("latitude", 51.438000).put("longitude", 5.470945);

        Asset smartHome = new Asset();
        smartHome.setRealmId(customerATenant.getId());
        smartHome.setName("Smart Home");
        smartHome.setType(BUILDING);
        smartHome.addAttributes(
            AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA),
            new AssetAttribute("geoStreet", STRING, Values.create("Wilhelminaplein 21C"))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Street")),
                    new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoStreet"))
                ),
            new AssetAttribute("geoPostalCode", AttributeValueType.NUMBER, Values.create(5611))
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
        Asset apartment1 = createDemoApartment(smartHome, "Apartment 1")
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA));
        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        Asset apartment1ServiceAgent = new Asset("Service Agent (Simulator)", AGENT, apartment1);
        apartment1ServiceAgent.addAttributes(
            initProtocolConfiguration(new AssetAttribute("apartmentSimulator"), SimulatorProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        SimulatorProtocol.CONFIG_MODE,
                        Values.create(SimulatorProtocol.Mode.WRITE_THROUGH_IMMEDIATE.toString())
                    ))
        );
        apartment1ServiceAgent = assetStorageService.merge(apartment1ServiceAgent);
        apartment1ServiceAgentId = apartment1ServiceAgent.getId();

        /* ############################ ROOMS ############################## */

        Asset apartment1Livingroom = createDemoApartmentRoom(apartment1, "Living Room")
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA));
        addDemoApartmentRoomMotionSensor(apartment1Livingroom, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentRoomCO2Sensor(apartment1Livingroom, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentRoomHumiditySensor(apartment1Livingroom, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentRoomThermometer(apartment1Livingroom, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentTemperatureControl(apartment1Livingroom, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        Asset apartment1Kitchen = createDemoApartmentRoom(apartment1, "Kitchen")
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA));
        addDemoApartmentRoomMotionSensor(apartment1Kitchen, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        for (String switchName : new String[]{"A", "B", "C"}) {
            addDemoApartmentSmartSwitch(apartment1Kitchen, switchName, true, attributeIndex -> {
                switch (attributeIndex) {
                    case 2:
                        return new MetaItem[]{
                            new MetaItem(AssetMeta.AGENT_LINK,
                                         new AttributeRef(apartment1ServiceAgentId,
                                                          "apartmentSimulator").toArrayValue()),
                            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT,
                                         Values.create(NumberSimulatorElement.ELEMENT_NAME))
                        };
                    case 3:
                        return new MetaItem[]{
                            new MetaItem(AssetMeta.AGENT_LINK,
                                         new AttributeRef(apartment1ServiceAgentId,
                                                          "apartmentSimulator").toArrayValue()),
                            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT,
                                         Values.create(NumberSimulatorElement.ELEMENT_NAME))
                        };
                    case 4:
                        return new MetaItem[]{
                            new MetaItem(AssetMeta.AGENT_LINK,
                                         new AttributeRef(apartment1ServiceAgentId,
                                                          "apartmentSimulator").toArrayValue()),
                            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT,
                                         Values.create(NumberSimulatorElement.ELEMENT_NAME))
                        };
                }
                return null;
            });
        }

        apartment1Kitchen = assetStorageService.merge(apartment1Kitchen);
        apartment1KitchenId = apartment1Kitchen.getId();

        Asset apartment1Hallway = createDemoApartmentRoom(apartment1, "Hallway")
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA));
        addDemoApartmentRoomMotionSensor(apartment1Hallway, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        apartment1Hallway = assetStorageService.merge(apartment1Hallway);
        apartment1HallwayId = apartment1Hallway.getId();

        addDemoApartmentVentilation(apartment1, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        apartment1 = assetStorageService.merge(apartment1);

        if (importDemoScenes) {
            Scene[] scenes = new Scene[]{
                new Scene("homeScene", "Home scene", "HOME", "0 0 7 ? *", false, 21d),
                new Scene("awayScene", "Away scene", "AWAY", "0 30 8 ? *", true, 15d),
                new Scene("eveningScene", "Evening scene", "EVENING", "0 30 17 ? *", false, 22d),
                new Scene("nightScene", "Night scene", "NIGHT", "0 0 22 ? *", true, 19d)
            };

            Asset demoApartmentSceneAgent = createDemoApartmentSceneAgent(
                apartment1, scenes, apartment1Livingroom, apartment1Kitchen, apartment1Hallway
            ).addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA));

            demoApartmentSceneAgent = assetStorageService.merge(demoApartmentSceneAgent);

            linkDemoApartmentWithSceneAgent(apartment1, demoApartmentSceneAgent, scenes);
            apartment1 = assetStorageService.merge(apartment1);
        }

        Asset apartment2 = new Asset("Apartment 2", RESIDENCE, smartHome);
        apartment2.addAttributes(
            AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA),
            new AssetAttribute("allLightsOffSwitch", AttributeValueType.BOOLEAN, Values.create(true))
                .setMeta(
                    new MetaItem(LABEL, Values.create("All Lights Off Switch")),
                    new MetaItem(DESCRIPTION, Values.create("When triggered, turns all lights in the apartment off")),
                    new MetaItem(RULE_EVENT, Values.create(true)),
                    new MetaItem(RULE_EVENT_EXPIRES, Values.create("3s"))
                )
        );
        apartment2 = assetStorageService.merge(apartment2);
        apartment2Id = apartment2.getId();

        Asset apartment2Livingroom = new Asset("Living Room", ROOM, apartment2);
        apartment2Livingroom.addAttributes(
            AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA),
            new AssetAttribute("motionSensor", AttributeValueType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Motion Sensor")),
                    new MetaItem(DESCRIPTION, Values.create("PIR sensor that sends 'true' when motion is sensed")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(RULE_EVENT, Values.create(true))
                ),
            new AssetAttribute("presenceDetected", AttributeValueType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Presence Detected")),
                    new MetaItem(DESCRIPTION, Values.create("Someone is currently present in the room")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("firstPresenceDetected", AttributeValueType.TIMESTAMP_MILLIS)
                .setMeta(
                    new MetaItem(LABEL, Values.create("First Presence Timestamp")),
                    new MetaItem(DESCRIPTION, Values.create("Timestamp of the first detected presence")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lastPresenceDetected", AttributeValueType.TIMESTAMP_MILLIS)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Last Presence Timestamp")),
                    new MetaItem(DESCRIPTION, Values.create("Timestamp of last detected presence")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("co2Level", AttributeValueType.CO2_PPM, Values.create(350))
                .setMeta(
                    new MetaItem(LABEL, Values.create("CO2 Level")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lightSwitch", AttributeValueType.BOOLEAN, Values.create(true))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Light Switch")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("windowOpen", AttributeValueType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true))
                )
        );
        apartment2Livingroom = assetStorageService.merge(apartment2Livingroom);
        apartment2LivingroomId = apartment2Livingroom.getId();

        Asset apartment2Bathroom = new Asset("Bathroom", ROOM, apartment2);
        apartment2Bathroom.addAttributes(
            AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA),
            new AssetAttribute("motionSensor", AttributeValueType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Motion Sensor")),
                    new MetaItem(DESCRIPTION, Values.create("PIR sensor that sends 'true' when motion is sensed")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(RULE_EVENT, Values.create(true))
                ),
            new AssetAttribute("presenceDetected", AttributeValueType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Presence Detected")),
                    new MetaItem(DESCRIPTION, Values.create("Someone is currently present in the room")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("firstPresenceDetected", AttributeValueType.TIMESTAMP_MILLIS)
                .setMeta(
                    new MetaItem(LABEL, Values.create("First Presence Timestamp")),
                    new MetaItem(DESCRIPTION, Values.create("Timestamp of the first detected presence")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lastPresenceDetected", AttributeValueType.TIMESTAMP_MILLIS)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Last Presence Timestamp")),
                    new MetaItem(DESCRIPTION, Values.create("Timestamp of last detected presence")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lightSwitch", AttributeValueType.BOOLEAN, Values.create(true))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Light Switch")),
                    new MetaItem(RULE_STATE, Values.create(true))
                )
        );
        apartment2Bathroom = assetStorageService.merge(apartment2Bathroom);
        apartment2BathroomId = apartment2Bathroom.getId();

        Asset apartment3 = new Asset("Apartment 3", RESIDENCE, smartHome)
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA));
        apartment3 = assetStorageService.merge(apartment3);
        apartment3Id = apartment3.getId();

        Asset apartment3Livingroom = new Asset("Living Room", ROOM, apartment3)
            .addAttributes(AssetAttribute.createWithDescriptor(AttributeType.LOCATION, locationValueA));
        apartment3Livingroom.addAttributes(
            new AssetAttribute("lightSwitch", AttributeValueType.BOOLEAN)
        );

        apartment3Livingroom = assetStorageService.merge(apartment3Livingroom);
        apartment3LivingroomId = apartment3Livingroom.getId();

        Asset console = ConsoleConfigration.initConsoleConfiguration(
            new Asset("Demo Android Console", CONSOLE, groundFloor),
            "Demo Android Console",
            "1.0",
            "Android 7.1.2",
            new HashMap<String, ConsoleProvider>() {
                {
                    put("geofence", new ConsoleProvider(
                        ORConsoleGeofenceAssetAdapter.NAME,
                        true,
                        false,
                        false,
                        null
                    ));
                }
            })
            .addAttributes(
                new AssetAttribute(AttributeType.LOCATION.getName(),
                                   AttributeType.LOCATION.getType())
                    .setMeta(new MetaItem(RULE_STATE))
            );
        console = assetStorageService.merge(console);
        consoleId = console.getId();

        // ################################ Link demo users and assets ###################################

        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.customerATenant.getId(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1Id));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.customerATenant.getId(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1LivingroomId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.customerATenant.getId(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1KitchenId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.customerATenant.getId(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1HallwayId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.customerATenant.getId(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment2Id));
    }
}