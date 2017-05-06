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
import org.openremote.agent3.protocol.macro.MacroAction;
import org.openremote.agent3.protocol.macro.MacroProtocol;
import org.openremote.agent3.protocol.simulator.SimulatorProtocol;
import org.openremote.agent3.protocol.simulator.element.ColorSimulatorElement;
import org.openremote.agent3.protocol.simulator.element.DecimalSimulatorElement;
import org.openremote.agent3.protocol.simulator.element.IntegerSimulatorElement;
import org.openremote.agent3.protocol.simulator.element.SwitchSimulatorElement;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetState;
import org.openremote.model.attribute.*;
import org.openremote.model.value.Values;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.openremote.model.asset.AssetMeta.ABOUT;
import static org.openremote.model.asset.AssetMeta.LABEL;
import static org.openremote.model.asset.AssetType.*;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;
import static org.openremote.model.attribute.AttributeType.*;

public class ManagerDemoSetup extends AbstractManagerSetup {

    // Update these numbers whenever you change a RULE_STATE flag in test data
    public static final int DEMO_RULE_STATES_APARTMENT_1 = 38;
    public static final int DEMO_RULE_STATES_APARTMENT_2 = 3;
    public static final int DEMO_RULE_STATES_APARTMENT_3 = 0;
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
    public String apartment1SceneAgentId;
    public String apartment1LivingroomId;
    public String apartment1LivingroomThermostatId;
    public String apartment2Id;
    public String apartment3Id;
    public String apartment2LivingroomId;
    public String apartment3LivingroomId;
    public String masterRealmId;
    public String customerARealmId;

    public ManagerDemoSetup(Container container) {
        super(container);
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
                        AssetMeta.DESCRIPTION,
                        Values.create("The switch for the light in the living room")),
                    new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).toArrayValue()),
                    new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(SwitchSimulatorElement.ELEMENT_NAME)
                    ))
                ),
            new AssetAttribute("light1Dimmer", INTEGER) // No initial value!
                .setMeta(new Meta(
                    new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Values.create("The dimmer for the light in the living room")),
                    new MetaItem(
                        AssetMeta.RANGE_MIN,
                        Values.create(0)),
                    new MetaItem(
                        AssetMeta.RANGE_MAX,
                        Values.create(100)),
                    new MetaItem(
                        AssetMeta.AGENT_LINK,
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
                        AssetMeta.DESCRIPTION,
                        Values.create("The color of the living room light")),
                    new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).toArrayValue()),
                    new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(ColorSimulatorElement.ELEMENT_NAME))
                    )
                ),
            new AssetAttribute("light1PowerConsumption", ENERGY_KWH, Values.create(12.345))
                .setMeta(new Meta(
                    new MetaItem(
                        AssetMeta.DESCRIPTION,
                        Values.create("The total power consumption of the living room light")),
                    new MetaItem(
                        AssetMeta.READ_ONLY,
                        Values.create(true)),
                    new MetaItem(
                        AssetMeta.FORMAT,
                        Values.create("%3d kWh")),
                    new MetaItem(
                        AssetMeta.AGENT_LINK,
                        new AttributeRef(agent.getId(), agentProtocolConfigName).toArrayValue()),
                    new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME)),
                    new MetaItem(
                        AssetMeta.STORE_DATA_POINTS.getUrn(), Values.create(true))
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

        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(0.11), now.minusDays(80).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(1.22), now.minusDays(40).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(2.33), now.minusDays(20).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(3.44), now.minusDays(10).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(4.55), now.minusDays(8).toEpochSecond() * 1000);

        light1PowerConsumptionAttribute.setValue(Values.create(5.66), now.minusDays(6).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(6.77), now.minusDays(3).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(7.88), now.minusDays(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(8.99), now.minusHours(10).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(9.11), now.minusHours(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(10.22), now.minusHours(2).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(11.33), now.minusHours(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(11.44), now.minusMinutes(30).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(12.00), now.minusMinutes(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(12.11), now.minusSeconds(5).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

        light1PowerConsumptionAttribute.setValue(Values.create(12.22), now.minusSeconds(1).toEpochSecond() * 1000);
        assetDatapointService.accept(new AssetState(thing, light1PowerConsumptionAttribute));

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

        ServerAsset apartment1 = new ServerAsset("Apartment 1", RESIDENCE, smartHome);
        apartment1.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1.setAttributes(
            new AssetAttribute("alarmEnabled", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(new Meta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Alarm Enabled")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("Send notifications when presence is detected")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                )),
            new AssetAttribute("vacationDays", AttributeType.INTEGER)
                .setMeta(new Meta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Vacation Days")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("Enable vacation mode for given days")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                )),
            new AssetAttribute("autoSceneSchedule", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Automatic scene schedule")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("Predict presence and automatically adjust scene schedule")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lastExecutedScene", AttributeType.STRING, Values.create("HOME"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Last executed scene")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("homeSceneMONDAY", AttributeType.STRING, Values.create("06:45:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Waking up on Monday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("awaySceneMONDAY", AttributeType.STRING, Values.create("08:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to work on Monday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("eveningSceneMONDAY", AttributeType.STRING, Values.create("17:30:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Back from work on Monday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("nightSceneMONDAY", AttributeType.STRING, Values.create("22:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to bed on Monday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("homeSceneTUESDAY", AttributeType.STRING, Values.create("06:45:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Waking up on Tuesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("awaySceneTUESDAY", AttributeType.STRING, Values.create("08:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to work on Tuesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("eveningSceneTUESDAY", AttributeType.STRING, Values.create("17:30:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Back from work on Tuesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("nightSceneTUESDAY", AttributeType.STRING, Values.create("22:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to bed on Tuesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("homeSceneWEDNESDAY", AttributeType.STRING, Values.create("06:45:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Waking up on Wednesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("awaySceneWEDNESDAY", AttributeType.STRING, Values.create("08:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to work on Wednesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("eveningSceneWEDNESDAY", AttributeType.STRING, Values.create("17:30:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Back from work on Wednesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("nightSceneWEDNESDAY", AttributeType.STRING, Values.create("22:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to bed on Wednesday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("homeSceneTHURSDAY", AttributeType.STRING, Values.create("06:45:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Waking up on Thursday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("awaySceneTHURSDAY", AttributeType.STRING, Values.create("08:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to work on Thursday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("eveningSceneTHURSDAY", AttributeType.STRING, Values.create("17:30:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Back from work on Thursday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))

                ),
            new AssetAttribute("nightSceneTHURSDAY", AttributeType.STRING, Values.create("22:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to bed on Thursday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("homeSceneFRIDAY", AttributeType.STRING, Values.create("06:45:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Waking up on Friday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("awaySceneFRIDAY", AttributeType.STRING, Values.create("08:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to work on Friday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("eveningSceneFRIDAY", AttributeType.STRING, Values.create("16:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Back from work on Friday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("nightSceneFRIDAY", AttributeType.STRING, Values.create("23:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to bed on Friday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("homeSceneSATURDAY", AttributeType.STRING, Values.create("07:30:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Waking up on Saturday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("awaySceneSATURDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to work on Saturday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("eveningSceneSATURDAY", AttributeType.STRING, Values.create("18:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Back from work on Saturday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("nightSceneSATURDAY", AttributeType.STRING, Values.create("23:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to bed on Saturday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("homeSceneSUNDAY", AttributeType.STRING, Values.create("08:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Waking up on Sunday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("awaySceneSUNDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to work on Sunday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("eveningSceneSUNDAY", AttributeType.STRING, Values.create("18:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Back from work on Sunday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("nightSceneSUNDAY", AttributeType.STRING, Values.create("22:00:00"))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Going to bed on Sunday")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                )
        );
        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        ServerAsset apartment1Livingroom = new ServerAsset("Living Room", ROOM, apartment1);
        apartment1Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1Livingroom.setAttributes(
            new AssetAttribute("motionCount", AttributeType.INTEGER, Values.create(0))
                .setMeta(new Meta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Motion Count")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("Sensor that increments a counter when motion is detected")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true)),
                    new MetaItem(AssetMeta.RULE_EVENT, Values.create(true))
                )),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN)
                .setMeta(new Meta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Presence Detected")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("Someone is currently present in the room")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                )),
            new AssetAttribute("lastPresenceDetected", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Last Presence Timestamp")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("Timestamp of last detected presence")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("co2Level", AttributeType.DECIMAL, Values.create(450))
                .setMeta(new Meta(
                    new MetaItem(AssetMeta.LABEL, Values.create("CO2 Level")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ))
        );
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        ServerAsset apartment1LivingroomThermostat = new ServerAsset("Living Room Thermostat", THING, apartment1Livingroom);
        apartment1LivingroomThermostat.setAttributes(
            new AssetAttribute("currentTemperature", DECIMAL)
                .setMeta(
                    new MetaItem(
                        AssetMeta.LABEL,
                        Values.create("Current Temperature")),
                    new MetaItem(
                        AssetMeta.RULE_STATE,
                        Values.create(true)),
                    new MetaItem(
                        AssetMeta.PROTECTED,
                        Values.create(true)),
                    new MetaItem(
                        AssetMeta.READ_ONLY,
                        Values.create(true)),
                    new MetaItem(
                        Constants.NAMESPACE + ":foo:bar", Values.create("FOO")),
                    new MetaItem(
                        "urn:thirdparty:bar", Values.create("BAR"))
                ),
            new AssetAttribute("targetTemperature", DECIMAL)
                .setMeta(
                    new MetaItem(
                        AssetMeta.LABEL,
                        Values.create("Target Temperature")),
                    new MetaItem(
                        AssetMeta.RULE_STATE,
                        Values.create(true)),
                    new MetaItem(
                        AssetMeta.PROTECTED,
                        Values.create(true)),
                    new MetaItem(
                        Constants.NAMESPACE + ":foo:bar", Values.create("FOO")),
                    new MetaItem(
                        "urn:thirdparty:bar", Values.create("BAR")),
                    new MetaItem(
                        SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME)
                    )
                )
        );
        apartment1LivingroomThermostat = assetStorageService.merge(apartment1LivingroomThermostat);
        apartment1LivingroomThermostatId = apartment1LivingroomThermostat.getId();

        // Create scene agent
        ServerAsset apartment1Scenes = new ServerAsset("Scenes", AGENT, apartment1);
        apartment1Scenes.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1Scenes.setAttributes(
            initProtocolConfiguration(new AssetAttribute("homeScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(false))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("HOME"))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(21d))).toMetaItem()
                ),
            initProtocolConfiguration(new AssetAttribute("awayScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(true))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("AWAY"))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(15d))).toMetaItem()
                ),
            initProtocolConfiguration(new AssetAttribute("eveningScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(false))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("EVENING"))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(22d))).toMetaItem()
                ),
            initProtocolConfiguration(new AssetAttribute("nightScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(true))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("NIGHT"))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(19d))).toMetaItem()
                )
        );
        apartment1Scenes = assetStorageService.merge(apartment1Scenes);
        apartment1SceneAgentId = apartment1Scenes.getId();

        // Update apartment 1 with scenes
        apartment1.addAttribute(
            new AssetAttribute("homeScene", AttributeType.STRING)
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Execute Home Scene")),
                    new MetaItem(AssetMeta.EXECUTABLE, Values.create(true)),
                    new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeScene").toArrayValue())
                )
        );

        apartment1.addAttribute(
            new AssetAttribute("awayScene", AttributeType.STRING)
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Execute Away Scene")),
                    new MetaItem(AssetMeta.EXECUTABLE, Values.create(true)),
                    new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayScene").toArrayValue())
                )
        );

        apartment1.addAttribute(
            new AssetAttribute("eveningScene", AttributeType.STRING)
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Execute Evening Scene")),
                    new MetaItem(AssetMeta.EXECUTABLE, Values.create(true)),
                    new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningScene").toArrayValue())
                )
        );

        apartment1.addAttribute(
            new AssetAttribute("nightScene", AttributeType.STRING)
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Execute Night Scene")),
                    new MetaItem(AssetMeta.EXECUTABLE, Values.create(true)),
                    new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightScene").toArrayValue())
                )
        );

        apartment1 = assetStorageService.merge(apartment1);

        ServerAsset apartment2 = new ServerAsset("Apartment 2", RESIDENCE, smartHome);
        apartment2.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2.setAttributes(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Values.create(true))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("All Lights Off Switch")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("When triggered, turns all lights in the apartment off")),
                    new MetaItem(AssetMeta.RULE_EVENT, Values.create(true)),
                    new MetaItem(AssetMeta.RULE_EVENT_EXPIRES, Values.create("10s"))
                )
        );
        apartment2 = assetStorageService.merge(apartment2);
        apartment2Id = apartment2.getId();

        ServerAsset apartment2Livingroom = new ServerAsset("Living Room", ROOM, apartment2);
        apartment2Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2Livingroom.setAttributes(
            new AssetAttribute("motionSensor", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                    new MetaItem(AssetMeta.LABEL, Values.create("Motion Sensor")),
                    new MetaItem(AssetMeta.DESCRIPTION, Values.create("PIR sensor that sends 'true' when motion is sensed")),
                    new MetaItem(AssetMeta.RULE_STATE, Values.create(true)),
                    new MetaItem(AssetMeta.RULE_EVENT, Values.create(true))
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                        new MetaItem(AssetMeta.LABEL, Values.create("Presence Detected")),
                        new MetaItem(AssetMeta.DESCRIPTION, Values.create(
                            "Someone is currently present in the room"
                        )),
                        new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lastPresenceDetected", AttributeType.DECIMAL)
                .setMeta(
                        new MetaItem(AssetMeta.LABEL, Values.create("Last Presence Timestamp")),
                        new MetaItem(AssetMeta.DESCRIPTION, Values.create(
                            "Timestamp of last detected presence"
                        )),
                        new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Values.create(true))
                .setMeta(
                        new MetaItem(AssetMeta.LABEL, Values.create("Light Switch")),
                        new MetaItem(AssetMeta.RULE_STATE, Values.create(false))
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Values.create(false))
                .setMeta(
                        new MetaItem(AssetMeta.LABEL, Values.create("Window Open"))
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
