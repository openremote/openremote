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

import org.apache.commons.io.IOUtils;
import org.openremote.agent.protocol.dmx.artnet.ArtnetClientProtocol;
import org.openremote.agent.protocol.simulator.SimulatorProtocol;
import org.openremote.container.Container;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.security.UserConfiguration;
import org.openremote.manager.setup.AbstractManagerSetup;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.UserAsset;
import org.openremote.model.attribute.*;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.security.Tenant;
import org.openremote.model.simulator.element.ColorSimulatorElement;
import org.openremote.model.simulator.element.NumberSimulatorElement;
import org.openremote.model.simulator.element.SwitchSimulatorElement;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.openremote.model.asset.AssetType.*;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;
import static org.openremote.model.attribute.AttributeValueType.*;
import static org.openremote.model.attribute.MetaItemType.*;
import static org.openremote.model.rules.Ruleset.Lang.GROOVY;

public class ManagerDemoSetup extends AbstractManagerSetup {

    // Update these numbers whenever you change a RULE_STATE flag in test data
    public static final int DEMO_RULE_STATES_APARTMENT_1 = 44;
    public static final int DEMO_RULE_STATES_APARTMENT_2 = 13;
    public static final int DEMO_RULE_STATES_APARTMENT_3 = 0;
    public static final int DEMO_RULE_STATES_SMART_OFFICE = 1;
    public static final int DEMO_RULE_STATES_SMART_BUILDING = DEMO_RULE_STATES_APARTMENT_1 + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_SMART_CITY = 65;
    public static final int DEMO_RULE_STATES_CUSTOMER_A = DEMO_RULE_STATES_SMART_BUILDING;
    public static final int DEMO_RULE_STATES_GLOBAL = DEMO_RULE_STATES_CUSTOMER_A + DEMO_RULE_STATES_SMART_OFFICE + DEMO_RULE_STATES_SMART_CITY;
    public static final int DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1 + 28;
    public static final int DEMO_RULE_STATES_SMART_HOME_WITH_SCENES = DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_CUSTOMER_A_WITH_SCENES = DEMO_RULE_STATES_SMART_HOME_WITH_SCENES;
    public static final int DEMO_RULE_STATES_GLOBAL_WITH_SCENES = DEMO_RULE_STATES_CUSTOMER_A_WITH_SCENES;
    public static GeoJSONPoint SMART_OFFICE_LOCATION = new GeoJSONPoint(5.460315214821094, 51.44541688237109);
    public static GeoJSONPoint SMART_BUILDING_LOCATION = new GeoJSONPoint(5.470945, 51.438000);
    public static GeoJSONPoint SMART_CITY_LOCATION = new GeoJSONPoint(5.670945, 51.435000);
    public static final String agentProtocolConfigName = "simulator123";
    public static final String thingLightToggleAttributeName = "light1Toggle";
    //public static final String ARTNET_AREA_CONFIGURATION = "{'lights': [{'id': 0, 'universe': 0, 'amountOfLeds': 3}" + "]}";
    public static final String ARTNET_DEFAULT_LIGHT_STATE = "{'r': 0, 'g': 0, 'b': 0, 'w': 0}";
    final protected boolean importDemoScenes;
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
    public String realmATenant;
    public String realmBTenant;
    public String smartCityServiceAgentId;



    public ManagerDemoSetup(Container container, boolean importDemoScenes) {
        super(container);
        this.importDemoScenes = importDemoScenes;
    }

    @Override
    public void onStart() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        Tenant realmATenant = keycloakDemoSetup.tenantA;
        Tenant realmBTenant = keycloakDemoSetup.tenantB;
        masterRealm = masterTenant.getRealm();
        this.realmATenant = realmATenant.getRealm();
        this.realmBTenant = realmBTenant.getRealm();

        // ################################ Demo assets for 'master' realm ###################################

        ObjectValue locationValue = SMART_OFFICE_LOCATION.toValue();

        Asset smartOffice = new Asset();
        smartOffice.setRealm(masterRealm);
        smartOffice.setName("Smart Office");
        smartOffice.setType(BUILDING);
        List<AssetAttribute> smartOfficeAttributes = Arrays.asList(
            new AssetAttribute(AttributeType.LOCATION, locationValue),
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
            .addAttributes(new AssetAttribute(AttributeType.LOCATION, locationValue));
        groundFloor = assetStorageService.merge(groundFloor);
        groundFloorId = groundFloor.getId();

        Asset lobby = new Asset("Lobby", ROOM, groundFloor)
            .addAttributes(new AssetAttribute(AttributeType.LOCATION, locationValue));
        lobby.addAttributes(
            new AssetAttribute("lobbyLocations", AttributeValueType.ARRAY)
        );

        lobby = assetStorageService.merge(lobby);
        lobbyId = lobby.getId();

        Asset agent = new Asset("Demo Agent", AGENT, lobby);
        agent.addAttributes(
            new AssetAttribute(AttributeType.LOCATION, locationValue),
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
            .addAttributes(new AssetAttribute(AttributeType.LOCATION, locationValue)
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
                        DATA_POINTS_MAX_AGE_DAYS,
                        Values.create(7)
                    ),
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

        //Art-Net Setup
        //SETUP MAIN ARTNET-ASSET UNDER MASTER ASSET
        Asset artNetArea = new Asset();
        artNetArea.setRealm(masterTenant.getRealm());
        artNetArea.setName("ArtNet Area 1");
        artNetArea.setType(AGENT);
        artNetArea.addAttributes(
                initProtocolConfiguration(new AssetAttribute(agentProtocolConfigName), ArtnetClientProtocol.PROTOCOL_NAME)
                        .addMeta(
                                new MetaItem(
                                        ArtnetClientProtocol.META_PROTOCOL_HOST,
                                        Values.create("192.168.70.200")
                                ),
                                new MetaItem(
                                        ArtnetClientProtocol.META_PROTOCOL_PORT,
                                        Values.create(6454)
                                ),
                                new MetaItem(
                                        ArtnetClientProtocol.META_ARTNET_CONFIGURATION,
                                        Values.createObject().putAll(new HashMap<String, Value>() {{
                                            put("protocolPrefix", Values.create("test"));
                                            put("lights", Values.createArray()
                                            .add(Values.createObject().putAll(new HashMap<String, Value>() {{
                                                put("lightId", Values.create(0));
                                                put("groupId", Values.create(0));
                                                put("universe", Values.create(0));
                                                put("amountOfLeds", Values.create(3));
                                                put("requiredValues", Values.create("r,g,b,w"));
                                            }}))
                                            .add(
                                            Values.createObject().putAll(new HashMap<String, Value>() {{
                                                put("lightId", Values.create(2));
                                                put("groupId", Values.create(0));
                                                put("universe", Values.create(0));
                                                put("amountOfLeds", Values.create(3));
                                                put("requiredValues", Values.create("r,g,b,w"));
                                            }})).add(
                                            Values.createObject().putAll(new HashMap<String, Value>() {{
                                                put("lightId", Values.create(1));
                                                put("groupId", Values.create(0));
                                                put("universe", Values.create(1));
                                                put("amountOfLeds", Values.create(3));
                                                put("requiredValues", Values.create("r,g,b,w"));
                                            }})));
                                        }}))));
        artNetArea = assetStorageService.merge(artNetArea);

        //SETUP LIGHT-ASSETS UNDER AREA
        for(int i = 0; i <= 2; i++) {
            Asset artNetLight = new Asset();
            artNetLight.setParent(artNetArea);
            artNetLight.setName("ArtNet Light " + i);
            artNetLight.setType(THING);
            List<AssetAttribute> artNetLightAttributes = Arrays.asList(
                    new AssetAttribute("Id", NUMBER, Values.create(i)).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                    new AssetAttribute("Values", OBJECT, Values.parseOrNull(ARTNET_DEFAULT_LIGHT_STATE)).addMeta(
                            new MetaItem(AGENT_LINK, new AttributeRef(artNetArea.getId(), agentProtocolConfigName).toArrayValue()),
                            new MetaItem(ArtnetClientProtocol.META_ARTNET_LIGHT_ID, Values.create(i))
                    ),
                    new AssetAttribute("Switch", BOOLEAN, Values.create(true)).addMeta(
                            new MetaItem(AGENT_LINK, new AttributeRef(artNetArea.getId(), agentProtocolConfigName).toArrayValue()),
                            new MetaItem(ArtnetClientProtocol.META_ARTNET_LIGHT_ID, Values.create(i))
                    ),
                    new AssetAttribute("Dim", NUMBER, Values.create(100)).addMeta(
                            new MetaItem(AGENT_LINK, new AttributeRef(artNetArea.getId(), agentProtocolConfigName).toArrayValue()),
                            new MetaItem(ArtnetClientProtocol.META_ARTNET_LIGHT_ID, Values.create(i))
                    )
            );
            artNetLight.setAttributes(artNetLightAttributes);
            artNetLight = assetStorageService.merge(artNetLight);

        }
        //END Art-Net Setup

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

        // ################################ Demo assets for 'tenantA' realm ###################################

        ObjectValue locationValueA = SMART_BUILDING_LOCATION.toValue();

        Asset smartBuilding = new Asset();
        smartBuilding.setRealm(this.realmATenant);
        smartBuilding.setName("Smart Building");
        smartBuilding.setType(BUILDING);
        smartBuilding.addAttributes(
            new AssetAttribute(AttributeType.LOCATION, locationValueA),
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
        smartBuilding = assetStorageService.merge(smartBuilding);
        smartBuildingId = smartBuilding.getId();

        // The "Apartment 1" is the demo apartment with complex scenes
        Asset apartment1 = createDemoApartment(smartBuilding, "Apartment 1", SMART_BUILDING_LOCATION);
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
            .addAttributes(
                    new AssetAttribute(AttributeType.LOCATION, locationValueA),
                    new AssetAttribute("lightsCeiling", NUMBER, Values.create(0))
                            .setMeta(
                                    new MetaItem(RANGE_MIN, Values.create(0)),
                                    new MetaItem(RANGE_MAX, Values.create(100)),
                                    new MetaItem(LABEL, Values.create("Ceiling lights (range)")),
                                    new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                                    new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true))
                            ),
                    new AssetAttribute("lightsStand", AttributeValueType.BOOLEAN, Values.create(true))
                            .setMeta(
                                    new MetaItem(LABEL, Values.create("Floor stand lights (on/off)")),
                                    new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                                    new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true))
                            )
            );
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

        apartment1Livingroom.setId(apartment1LivingroomId);
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        Asset apartment1Kitchen = createDemoApartmentRoom(apartment1, "Kitchen")
            .addAttributes(
                    new AssetAttribute(AttributeType.LOCATION, locationValueA),
                    new AssetAttribute("lights", AttributeValueType.BOOLEAN, Values.create(true))
                        .addMeta(new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)))
                        .addMeta(new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)))
            );
        addDemoApartmentRoomMotionSensor(apartment1Kitchen, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        for (String switchName : new String[]{"A", "B", "C"}) {
            addDemoApartmentSmartSwitch(apartment1Kitchen, switchName, true, attributeIndex -> {
                switch (attributeIndex) {
                    case 2:
                        return new MetaItem[]{
                            new MetaItem(MetaItemType.AGENT_LINK,
                                         new AttributeRef(apartment1ServiceAgentId,
                                                          "apartmentSimulator").toArrayValue()),
                            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT,
                                         Values.create(NumberSimulatorElement.ELEMENT_NAME))
                        };
                    case 3:
                        return new MetaItem[]{
                            new MetaItem(MetaItemType.AGENT_LINK,
                                         new AttributeRef(apartment1ServiceAgentId,
                                                          "apartmentSimulator").toArrayValue()),
                            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT,
                                         Values.create(NumberSimulatorElement.ELEMENT_NAME))
                        };
                    case 4:
                        return new MetaItem[]{
                            new MetaItem(MetaItemType.AGENT_LINK,
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
            .addAttributes(
                    new AssetAttribute(AttributeType.LOCATION, locationValueA),
                    new AssetAttribute("lights", AttributeValueType.BOOLEAN, Values.create(true))
                            .addMeta(new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)))
                            .addMeta(new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)))
            );
        addDemoApartmentRoomMotionSensor(apartment1Hallway, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        apartment1Hallway = assetStorageService.merge(apartment1Hallway);
        apartment1HallwayId = apartment1Hallway.getId();

        Asset apartment1Bedroom1 = createDemoApartmentRoom(apartment1, "Bedroom")
                .addAttributes(
                        new AssetAttribute(AttributeType.LOCATION, locationValueA),
                        new AssetAttribute("lights", AttributeValueType.BOOLEAN, Values.create(true))
                                .addMeta(new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)))
                                .addMeta(new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)))
                );
        addDemoApartmentRoomCO2Sensor(apartment1Bedroom1, true, () -> new MetaItem[]{
                new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
                new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentRoomHumiditySensor(apartment1Bedroom1, true, () -> new MetaItem[]{
                new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
                new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentRoomThermometer(apartment1Bedroom1, true, () -> new MetaItem[]{
                new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
                new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentTemperatureControl(apartment1Bedroom1, true, () -> new MetaItem[]{
                new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
                new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        apartment1Bedroom1 = assetStorageService.merge(apartment1Bedroom1);
        apartment1Bedroom1Id = apartment1Bedroom1.getId();

        Asset apartment1Bathroom = new Asset("Bathroom", ROOM, apartment1);
        apartment1Bathroom.addAttributes(
            new AssetAttribute(AttributeType.LOCATION, locationValueA),
            new AssetAttribute("lights", AttributeValueType.BOOLEAN, Values.create(true))
                    .setMeta(
                            new MetaItem(RULE_STATE, Values.create(true)),
                            new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                            new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true))
                    )
        );
        addDemoApartmentRoomThermometer(apartment1Bathroom, true, () -> new MetaItem[]{
                new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
                new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        addDemoApartmentTemperatureControl(apartment1Bathroom, true, () -> new MetaItem[]{
                new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
                new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        apartment1Bathroom = assetStorageService.merge(apartment1Bathroom);
        apartment1BathroomId = apartment1Bathroom.getId();


        addDemoApartmentVentilation(apartment1, true, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(apartment1ServiceAgentId, "apartmentSimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });

        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        if (importDemoScenes) {
            Scene[] scenes = new Scene[]{
                new Scene("morningScene", "Morning scene", "MORNING", "0 0 7 ? *", false, 21d),
                new Scene("dayScene", "Day scene", "DAY", "0 30 8 ? *", true, 15d),
                new Scene("eveningScene", "Evening scene", "EVENING", "0 30 17 ? *", false, 22d),
                new Scene("nightScene", "Night scene", "NIGHT", "0 0 22 ? *", true, 19d)
            };

            Asset demoApartmentSceneAgent = createDemoApartmentSceneAgent(
                apartment1, scenes, apartment1Livingroom, apartment1Kitchen, apartment1Hallway
            ).addAttributes(new AssetAttribute(AttributeType.LOCATION, locationValueA));

            demoApartmentSceneAgent = assetStorageService.merge(demoApartmentSceneAgent);

            linkDemoApartmentWithSceneAgent(apartment1, demoApartmentSceneAgent, scenes);
            apartment1 = assetStorageService.merge(apartment1);
        }

        Asset apartment2 = new Asset("Apartment 2", RESIDENCE, smartBuilding);
        apartment2.addAttributes(
            new AssetAttribute(AttributeType.LOCATION, locationValueA),
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
            new AssetAttribute(AttributeType.LOCATION, locationValueA),
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
                ),
            new AssetAttribute("lightSwitchTriggerTimes", ARRAY, Values.createArray().add(Values.create("1800")).add(Values.create("0830")))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Lightswitch Trigger Times")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("plantsWaterLevels", OBJECT, Values.createObject().put("cactus", 0.8))
                .setMeta(
                    new MetaItem(LABEL, Values.create("Water levels of the plants")),
                    new MetaItem(RULE_STATE, Values.create(true))
                )
        );
        apartment2Livingroom = assetStorageService.merge(apartment2Livingroom);
        apartment2LivingroomId = apartment2Livingroom.getId();

        Asset apartment2Bathroom = new Asset("Bathroom", ROOM, apartment2);
        apartment2Bathroom.addAttributes(
            new AssetAttribute(AttributeType.LOCATION, locationValueA),
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

        Asset apartment3 = new Asset("Apartment 3", RESIDENCE, smartBuilding)
            .addAttributes(new AssetAttribute(AttributeType.LOCATION, locationValueA));
        apartment3 = assetStorageService.merge(apartment3);
        apartment3Id = apartment3.getId();

        Asset apartment3Livingroom = new Asset("Living Room", ROOM, apartment3)
            .addAttributes(new AssetAttribute(AttributeType.LOCATION, locationValueA));
        apartment3Livingroom.addAttributes(
            new AssetAttribute("lightSwitch", AttributeValueType.BOOLEAN)
        );

        apartment3Livingroom = assetStorageService.merge(apartment3Livingroom);
        apartment3LivingroomId = apartment3Livingroom.getId();

        // ################################ Link demo users and assets ###################################

        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1Id));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1LivingroomId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1KitchenId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1Bedroom1Id));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1BathroomId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                                                         keycloakDemoSetup.testuser3Id,
                                                         apartment1HallwayId));

        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                keycloakDemoSetup.buildingUserId,
                apartment1Id));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                keycloakDemoSetup.buildingUserId,
                apartment1LivingroomId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                keycloakDemoSetup.buildingUserId,
                apartment1KitchenId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                keycloakDemoSetup.buildingUserId,
                apartment1Bedroom1Id));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                keycloakDemoSetup.buildingUserId,
                apartment1BathroomId));
        assetStorageService.storeUserAsset(new UserAsset(keycloakDemoSetup.tenantA.getRealm(),
                keycloakDemoSetup.buildingUserId,
                apartment1HallwayId));

        // ################################ Make users restricted ###################################

        UserConfiguration testuser3Config = identityService.getUserConfiguration(keycloakDemoSetup.testuser3Id);
        testuser3Config.setRestricted(true);
        testuser3Config = identityService.mergeUserConfiguration(testuser3Config);

        UserConfiguration buildingUserConfig = identityService.getUserConfiguration(keycloakDemoSetup.buildingUserId);
        testuser3Config.setRestricted(true);
        buildingUserConfig = identityService.mergeUserConfiguration(buildingUserConfig);

        // ################################ Realm B ###################################

        ObjectValue locationValueB = SMART_CITY_LOCATION.toValue();

        Asset smartCity = new Asset();
        smartCity.setRealm(this.realmBTenant);
        smartCity.setName("Smart City");
        smartCity.setType(BUILDING);
        smartCity.addAttributes(
            new AssetAttribute(AttributeType.LOCATION, locationValueB),

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
        smartCity = assetStorageService.merge(smartCity);

        Asset smartCityServiceAgent = new Asset("Service Agent (Simulator)", AGENT, smartCity);
        smartCityServiceAgent.addAttributes(
            initProtocolConfiguration(new AssetAttribute("citySimulator"), SimulatorProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        SimulatorProtocol.CONFIG_MODE,
                        Values.create(SimulatorProtocol.Mode.WRITE_THROUGH_IMMEDIATE.toString())
                    ))
        );
        smartCityServiceAgent = assetStorageService.merge(smartCityServiceAgent);
        smartCityServiceAgentId = smartCityServiceAgent.getId();

        // ################################ Realm B Area 1 ###################################

        Asset assetArea1 = new Asset("Area 1", THING, smartCity);
        assetArea1 = assetStorageService.merge(assetArea1);

        Asset camera1Asset = createDemoCameraAsset("Camera1", assetArea1, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(smartCityServiceAgentId, "citySimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        camera1Asset = assetStorageService.merge(camera1Asset);

        Asset microPhone1Asset = createDemoMicrophoneAsset("Microphone1", assetArea1, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(smartCityServiceAgentId, "citySimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        microPhone1Asset = assetStorageService.merge(microPhone1Asset);

        Asset enviroment1Asset = createDemoEnviromentAsset("Enviroment1", assetArea1, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(smartCityServiceAgentId, "citySimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        enviroment1Asset = assetStorageService.merge(enviroment1Asset);

        Asset light1Asset = createDemoLightAsset("Light1", assetArea1);
        light1Asset = assetStorageService.merge(light1Asset);

        // ################################ Realm B Area 2 ###################################

        Asset assetArea2 = new Asset("Area 2", THING, smartCity);
        assetArea2 = assetStorageService.merge(assetArea2);

        Asset camera2Asset = createDemoCameraAsset("Camera2", assetArea2, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(smartCityServiceAgentId, "citySimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        camera2Asset = assetStorageService.merge(camera2Asset);

        Asset enviroment2Asset = createDemoEnviromentAsset("Enviroment2", assetArea2, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(smartCityServiceAgentId, "citySimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        enviroment2Asset = assetStorageService.merge(enviroment2Asset);


        // ################################ Realm B Area 3 ###################################

        Asset assetArea3 = new Asset("Area 3", THING, smartCity);
        assetArea3 = assetStorageService.merge(assetArea3);

        Asset camera3Asset = createDemoCameraAsset("Camera3", assetArea3, () -> new MetaItem[]{
            new MetaItem(AGENT_LINK, new AttributeRef(smartCityServiceAgentId, "citySimulator").toArrayValue()),
            new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(NumberSimulatorElement.ELEMENT_NAME))
        });
        camera3Asset = assetStorageService.merge(camera3Asset);

        AssetRuleset camera3Rules = new AssetRuleset(
            "Camera3_Rules",
                GROOVY, IOUtils.toString(getClass().getResource("/demo/rules/DemoSmartCityCamera.groovy"), "UTF-8"), camera3Asset.getId(),
                false,
                false
        );
        camera3Rules = rulesetStorageService.merge(camera3Rules);

        Asset light3Asset = createDemoLightAsset("Light3", assetArea3);
        light3Asset = assetStorageService.merge(light3Asset);
    }
}
