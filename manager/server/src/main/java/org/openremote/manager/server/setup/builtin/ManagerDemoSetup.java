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
import elemental.json.Json;
import org.openremote.agent3.protocol.simulator.SimulatorProtocol;
import org.openremote.agent3.protocol.simulator.element.ColorSimulatorElement;
import org.openremote.agent3.protocol.simulator.element.DecimalSimulatorElement;
import org.openremote.agent3.protocol.simulator.element.IntegerSimulatorElement;
import org.openremote.agent3.protocol.simulator.element.SwitchSimulatorElement;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.*;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetState;
import org.openremote.model.asset.AssetType;
import org.openremote.model.units.AttributeUnits;
import org.openremote.model.units.ColorRGB;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.openremote.model.AttributeType.*;
import static org.openremote.model.asset.AssetMeta.*;
import static org.openremote.model.asset.AssetType.*;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;

public class ManagerDemoSetup extends AbstractManagerSetup {

     // Update these numbers whenever you change a RULE_STATE flag in test data
    public static final int DEMO_RULE_STATES_APARTMENT_1 = 6;
    public static final int DEMO_RULE_STATES_APARTMENT_2 = 2;
    public static final int DEMO_RULE_STATES_APARTMENT_3 = 2;
    public static final int DEMO_RULE_STATES_SMART_HOME = DEMO_RULE_STATES_APARTMENT_1 + DEMO_RULE_STATES_APARTMENT_2 + DEMO_RULE_STATES_APARTMENT_3;
    public static final int DEMO_RULE_STATES_CUSTOMER_A = DEMO_RULE_STATES_SMART_HOME;
    public static final int DEMO_RULE_STATES_GLOBAL = DEMO_RULE_STATES_CUSTOMER_A;

    public String smartOfficeId;
    public String groundFloorId;
    public String lobbyId;
    public String agentId;
    public final String agentProtocolConfigName = "simulator123";
    public String thingId;
    public String smartHomeId;
    public String apartment1Id;
    public String apartment1LivingroomId;
    public String apartment1LivingroomThermostatId;
    public String apartment2Id;
    public String apartment3Id;
    public String apartment2LivingroomId;
    public String apartment3LivingroomId;

    public ManagerDemoSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        Tenant customerATenant = keycloakDemoSetup.customerATenant;

        // ################################ Demo assets for 'master' realm ###################################


        ServerAsset smartOffice = new ServerAsset();
        smartOffice.setRealmId(masterTenant.getId());
        smartOffice.setName("Smart Office");
        smartOffice.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        smartOffice.setType(BUILDING);
        List<AssetAttribute> smartOfficeAttributes = Arrays.asList(
            new AssetAttribute("geoStreet", STRING, Json.create("Torenallee 20"))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("Street")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoStreet")))
                ),
            new AssetAttribute("geoPostalCode", AttributeType.INTEGER, Json.create(5617))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("Postal Code")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoPostalCode")))
                ),
            new AssetAttribute("geoCity", STRING, Json.create("Eindhoven"))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("City")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCity")))
                ),
            new AssetAttribute("geoCountry", STRING, Json.create("Netherlands"))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("Country")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCountry")))
                )
        );
        smartOffice.setAttributeList(smartOfficeAttributes);
        smartOffice = assetStorageService.merge(smartOffice);
        smartOfficeId = smartOffice.getId();

        ServerAsset groundFloor = new ServerAsset(smartOffice);
        groundFloor.setName("Ground Floor");
        groundFloor.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        groundFloor.setType(AssetType.FLOOR);
        groundFloor = assetStorageService.merge(groundFloor);
        groundFloorId = groundFloor.getId();

        ServerAsset lobby = new ServerAsset(groundFloor);
        lobby.setName("Lobby");
        lobby.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        lobby.setType(AssetType.ROOM);
        lobby = assetStorageService.merge(lobby);
        lobbyId = lobby.getId();

        ServerAsset agent = new ServerAsset(lobby);
        agent.setName("Demo Agent");
        agent.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        agent.setType(AssetType.AGENT);
        List<AssetAttribute> agentAttributes = Collections.singletonList(
            initProtocolConfiguration(SimulatorProtocol.PROTOCOL_NAME)
                .apply(new AssetAttribute(agentProtocolConfigName))
                .setMeta(new Meta()
                    .add(new MetaItem(
                            SimulatorProtocol.CONFIG_MODE,
                            Json.create(SimulatorProtocol.Mode.WRITE_THROUGH_DELAYED.toString())
                        )
                    )
                    .add(new MetaItem(
                            SimulatorProtocol.CONFIG_WRITE_DELAY_MILLISECONDS,
                            Json.create(500)
                        )
                    )));
        agent.setAttributeList(agentAttributes);
        agent = assetStorageService.merge(agent);
        agentId = agent.getId();

        ServerAsset thing = new ServerAsset(agent);
        thing.setName("Demo Thing");
        thing.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        thing.setType(AssetType.THING);
        Stream<AssetAttribute> thingAttributes = Stream.<AssetAttribute>builder().add(
            new AssetAttribute("light1Toggle", BOOLEAN, Json.create(true))
                .setMeta(new Meta()
                    .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The switch for the light in the living room"))
                    )
                    .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).asJsonValue()
                    ))
                    .add(new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Json.create(SwitchSimulatorElement.ELEMENT_NAME)
                    ))
                )
        ).add(
            new AssetAttribute("light1Dimmer", INTEGER) // No initial value!
                .setMeta(new Meta()
                    .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The dimmer for the light in the living room"))
                    )
                    .add(new MetaItem(
                        AssetMeta.RANGE_MIN,
                        Json.create(0))
                    )
                    .add(new MetaItem(
                        AssetMeta.RANGE_MAX,
                        Json.create(100))
                    )
                    .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).asJsonValue()
                    ))
                    .add(new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Json.create(IntegerSimulatorElement.ELEMENT_NAME_RANGE)
                    ))
                    .add(new MetaItem(
                        SimulatorProtocol.CONFIG_MODE, Json.create(true)
                    ))
                )
        ).add(
            new AssetAttribute("light1Color", INTEGER_ARRAY, new ColorRGB(88, 123, 88).asJsonValue())
                .setMeta(new Meta()
                    .add(new MetaItem(
                        AssetMeta.UNITS,
                        Json.create(AttributeUnits.COLOR_RGB.toString())
                    ))
                    .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The color of the living room light"))
                    )
                    .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).asJsonValue()
                    ))
                    .add(new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Json.create(ColorSimulatorElement.ELEMENT_NAME)
                    ))
                )
        ).add(
            new AssetAttribute("light1PowerConsumption", DECIMAL, Json.create(12.345))
                .setMeta(new Meta()
                    .add(new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Json.create("The total power consumption of the living room light"))
                    )
                    .add(new MetaItem(
                        AssetMeta.READ_ONLY,
                        Json.create(true))
                    )
                    .add(new MetaItem(
                        AssetMeta.FORMAT,
                        Json.create("%3d kWh"))
                    )
                    .add(new MetaItem(
                        AssetMeta.UNITS,
                        Json.create(AttributeUnits.ENERGY_KWH.name()))
                    )
                    .add(new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).asJsonValue()
                    ))
                    .add(new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Json.create(DecimalSimulatorElement.ELEMENT_NAME)
                    ))
                    .add(new MetaItem(
                        AssetMeta.STORE_DATA_POINTS.getUrn(), Json.create(true)
                    ))
                )
        ).build();

        thing.setAttributeStream(thingAttributes);
        thing = assetStorageService.merge(thing);
        thingId = thing.getId();

        // Some sample datapoints
        thing = assetStorageService.find(thingId, true);
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.systemDefault());

        AssetAttribute light1PowerConsumptionAttribute = thing.getAttribute("light1PowerConsumption").get();

        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(0.11), now.minusDays(80).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(1.22), now.minusDays(40).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(2.33), now.minusDays(20).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(3.44), now.minusDays(10).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(4.55), now.minusDays(8).toEpochSecond() * 1000);

        light1PowerConsumptionAttribute.setValue(Json.create(5.66), now.minusDays(6).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(6.77), now.minusDays(3).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(7.88), now.minusDays(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(8.99), now.minusHours(10).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(9.11), now.minusHours(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(10.22), now.minusHours(2).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(11.33), now.minusHours(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(11.44), now.minusMinutes(30).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(12.00), now.minusMinutes(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(12.11), now.minusSeconds(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Json.create(12.22), now.minusSeconds(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        // ################################ Demo assets for 'customerA' realm ###################################

        ServerAsset smartHome = new ServerAsset();
        smartHome.setRealmId(customerATenant.getId());
        smartHome.setName("Smart Home");
        smartHome.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        smartHome.setType(BUILDING);
        List<AssetAttribute> smartHomeAttributes = Arrays.asList(
            new AssetAttribute("geoStreet", STRING, Json.create("Wilhelminaplein 21C"))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("Street")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoStreet")))
                ),
            new AssetAttribute("geoPostalCode", AttributeType.INTEGER, Json.create(5611))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("Postal Code")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoPostalCode")))
                ),
            new AssetAttribute("geoCity", STRING, Json.create("Eindhoven"))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("City")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCity")))
                ),
            new AssetAttribute("geoCountry", STRING, Json.create("Netherlands"))
                .setMeta(new Meta()
                    .add(createMetaItem(LABEL, Json.create("Country")))
                    .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCountry")))
                )
        );
        smartHome.setAttributeList(smartHomeAttributes);
        smartHome = assetStorageService.merge(smartHome);
        smartHomeId = smartHome.getId();

        ServerAsset apartment1 = new ServerAsset(smartHome);
        apartment1.setName("Apartment 1");
        apartment1.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1.setType(RESIDENCE);
        List<AssetAttribute> apartment1Attributes = Arrays.asList(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Json.create(true))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("All Lights Off Switch"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("When triggered, turns all lights in the apartment off"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_EVENT, Json.create(true))
                    ).add(
                        new MetaItem(AssetMeta.RULE_EVENT_EXPIRES, Json.create("10s"))
                    )
                ),
            new AssetAttribute("vacationDays", AttributeType.INTEGER, Json.createNull())
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Vacation Days"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("Enable vacation mode for given days"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                ),
            new AssetAttribute( "lastScene", AttributeType.STRING, Json.create("NIGHT"))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Current scene"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("The scene which is currently active"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                )
        );
        apartment1.setAttributeList(apartment1Attributes);
        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        ServerAsset apartment1Livingroom = new ServerAsset(apartment1);
        apartment1Livingroom.setName("Livingroom");
        apartment1Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1Livingroom.setType(ROOM);
        List<AssetAttribute> apartment1LivingroomAttributes = Arrays.asList(
            new AssetAttribute("presenceSensor", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Sensor"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_EVENT, Json.create(true))
                    )
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Detected"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Json.create(true))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Light Switch"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Window Open"))
                    )
                ),
            new AssetAttribute("co2Level", AttributeType.DECIMAL, Json.create(450))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("CO2 Level"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(false))
                    )
                )
        );
        apartment1Livingroom.setAttributeList(apartment1LivingroomAttributes);
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        ServerAsset apartment1LivingroomThermostat = new ServerAsset(apartment1Livingroom);
        apartment1LivingroomThermostat.setName("Livingroom Thermostat");
        apartment1LivingroomThermostat.setType(AssetType.THING);
        List<AssetAttribute> apartment1LivingroomThermostatAttributes = Arrays.asList(
            new AssetAttribute("currentTemperature", DECIMAL, Json.createNull())
                .setMeta(new Meta()
                    .add(new MetaItem(
                        AssetMeta.LABEL,
                        Json.create("Current Temperature"))
                    )
                    .add(new MetaItem(
                        AssetMeta.RULE_STATE,
                        Json.create(true)
                    ))
                    .add(new MetaItem(
                        AssetMeta.PROTECTED,
                        Json.create(true))
                    )
                    .add(new MetaItem(
                        AssetMeta.READ_ONLY,
                        Json.create(true))
                    )
                    .add(new MetaItem(
                        Constants.NAMESPACE + ":foo:bar", Json.create("FOO")
                    ))
                    .add(new MetaItem(
                        "urn:thirdparty:bar", Json.create("BAR")
                    ))
                ),
            new AssetAttribute("comfortTemperature", DECIMAL, Json.createNull())
                .setMeta(new Meta()
                    .add(new MetaItem(
                        AssetMeta.LABEL,
                        Json.create("Comfort Temperature"))
                    )
                    .add(new MetaItem(
                        AssetMeta.RULE_STATE,
                        Json.create(true)
                    ))
                    .add(new MetaItem(
                        AssetMeta.PROTECTED,
                        Json.create(true))
                    )
                    .add(new MetaItem(
                        Constants.NAMESPACE + ":foo:bar", Json.create("FOO")
                    ))
                    .add(new MetaItem(
                        "urn:thirdparty:bar", Json.create("BAR")
                    ))
                    .add(new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Json.create(DecimalSimulatorElement.ELEMENT_NAME)
                    ))
                )
        );

        apartment1LivingroomThermostat.setAttributeList(apartment1LivingroomThermostatAttributes);
        apartment1LivingroomThermostat = assetStorageService.merge(apartment1LivingroomThermostat);
        apartment1LivingroomThermostatId = apartment1LivingroomThermostat.getId();

        ServerAsset apartment2 = new ServerAsset(smartHome);
        apartment2.setName("Apartment 2");
        apartment2.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2.setType(RESIDENCE);
        List<AssetAttribute> apartment2Attributes = Collections.singletonList(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("All Lights Off Switch"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("When triggered, turns all lights in the apartment off"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                )
        );
        apartment2.setAttributeList(apartment2Attributes);
        apartment2 = assetStorageService.merge(apartment2);
        apartment2Id = apartment2.getId();

        ServerAsset apartment2Livingroom = new ServerAsset(apartment2);
        apartment2Livingroom.setName("Livingroom");
        apartment2Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2Livingroom.setType(ROOM);
        List<AssetAttribute> apartment2LivingroomAttributes = Arrays.asList(
            new AssetAttribute("presenceSensor", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Sensor"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_EVENT, Json.create(true))
                    )
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Detected"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Json.create(true))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Light Switch"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(false))
                    )
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Window Open"))
                    )
                )
        );
        apartment2Livingroom.setAttributeList(apartment2LivingroomAttributes);
        apartment2Livingroom = assetStorageService.merge(apartment2Livingroom);
        apartment2LivingroomId = apartment2Livingroom.getId();

        ServerAsset apartment3 = new ServerAsset(smartHome);
        apartment3.setName("Apartment 3");
        apartment3.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment3.setType(RESIDENCE);
        List<AssetAttribute> apartment3Attributes = Collections.singletonList(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("All Lights Off Switch"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("When triggered, turns all lights in the apartment off"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                )
        );
        apartment3.setAttributeList(apartment3Attributes);
        apartment3 = assetStorageService.merge(apartment3);
        apartment3Id = apartment3.getId();

        ServerAsset apartment3Livingroom = new ServerAsset(apartment3);
        apartment3Livingroom.setName("Livingroom");
        apartment3Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment3Livingroom.setType(ROOM);
        List<AssetAttribute> apartment3LivingroomAttributes = Arrays.asList(
            new AssetAttribute("presenceSensor", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Sensor"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_EVENT, Json.create(true))
                    )
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Detected"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(true))
                    )
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Json.create(true))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Light Switch"))
                    ).add(
                        new MetaItem(AssetMeta.RULE_STATE, Json.create(false))
                    )
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Window Open"))
                    )
                )
        );
        apartment3Livingroom.setAttributeList(apartment3LivingroomAttributes);
        apartment3Livingroom = assetStorageService.merge(apartment3Livingroom);
        apartment3LivingroomId = apartment3Livingroom.getId();

        // ################################ Link demo users and assets ###################################

        identityService.setRestrictedUser(keycloakDemoSetup.testuser3Id, true);
        assetStorageService.storeUserAsset(keycloakDemoSetup.testuser3Id, apartment1Id);
        assetStorageService.storeUserAsset(keycloakDemoSetup.testuser3Id, apartment1LivingroomId);
        assetStorageService.storeUserAsset(keycloakDemoSetup.testuser3Id, apartment1LivingroomThermostatId);
        assetStorageService.storeUserAsset(keycloakDemoSetup.testuser3Id, apartment2Id);


    }
}
