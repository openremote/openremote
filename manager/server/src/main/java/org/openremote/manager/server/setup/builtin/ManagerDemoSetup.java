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
import org.openremote.agent3.protocol.trigger.TriggerProtocol;
import org.openremote.agent3.protocol.trigger.TriggerType;
import org.openremote.agent3.protocol.trigger.time.TimeTriggerProperty;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetState;
import org.openremote.model.attribute.*;
import org.openremote.model.value.Values;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.openremote.agent3.protocol.macro.MacroProtocol.META_MACRO_ACTION_INDEX;
import static org.openremote.agent3.protocol.macro.MacroProtocol.PROPERTY_MACRO_ACTION;
import static org.openremote.agent3.protocol.trigger.TriggerConfiguration.*;
import static org.openremote.model.asset.AssetMeta.*;
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
        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        ServerAsset apartment1Livingroom = new ServerAsset("Living Room", ROOM, apartment1);
        apartment1Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1Livingroom.setAttributes(
            new AssetAttribute("motionCount", AttributeType.INTEGER, Values.create(0))
                .setMeta(new Meta(
                    new MetaItem(LABEL, Values.create("Motion Count")),
                    new MetaItem(DESCRIPTION, Values.create("Sensor that increments a counter when motion is detected")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(RULE_EVENT, Values.create(true))
                )),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN)
                .setMeta(new Meta(
                    new MetaItem(LABEL, Values.create("Presence Detected")),
                    new MetaItem(DESCRIPTION, Values.create("Someone is currently present in the room")),
                    new MetaItem(RULE_STATE, Values.create(true))
                )),
            new AssetAttribute("lastPresenceDetected", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Last Presence Timestamp")),
                    new MetaItem(DESCRIPTION, Values.create("Timestamp of last detected presence")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("co2Level", AttributeType.DECIMAL, Values.create(450))
                .setMeta(new Meta(
                    new MetaItem(LABEL, Values.create("CO2 Level")),
                    new MetaItem(RULE_STATE, Values.create(true))
                ))
        );
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        ServerAsset apartment1LivingroomThermostat = new ServerAsset("Living Room Thermostat", THING, apartment1Livingroom);
        apartment1LivingroomThermostat.setAttributes(
            new AssetAttribute("currentTemperature", DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Current temperature")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true)),
                    new MetaItem(READ_ONLY, Values.create(true)),
                    new MetaItem(
                        Constants.NAMESPACE + ":foo:bar", Values.create("FOO")),
                    new MetaItem(
                        "urn:thirdparty:bar", Values.create("BAR"))
                ),
            new AssetAttribute("targetTemperature", DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Target Temperature")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTECTED, Values.create(true)),
                    new MetaItem(Constants.NAMESPACE + ":foo:bar", Values.create("FOO")),
                    new MetaItem("urn:thirdparty:bar", Values.create("BAR")),
                    new MetaItem(SimulatorProtocol.SIMULATOR_ELEMENT, Values.create(DecimalSimulatorElement.ELEMENT_NAME)
                    )
                )
        );
        apartment1LivingroomThermostat = assetStorageService.merge(apartment1LivingroomThermostat);
        apartment1LivingroomThermostatId = apartment1LivingroomThermostat.getId();

        // Create scene and trigger agent
        ServerAsset apartment1SceneAgent = new ServerAsset("Scenes and triggers", AGENT, apartment1);
        apartment1SceneAgent.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));

        // Add scenes
        apartment1SceneAgent.setAttributes(
            initProtocolConfiguration(new AssetAttribute("homeScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(LABEL, Values.create("Home scene")),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(false))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(21d))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("HOME"))).toMetaItem()
                ),
            initProtocolConfiguration(new AssetAttribute("awayScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(LABEL, Values.create("Away scene")),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(true))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(15d))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("AWAY"))).toMetaItem()
                ),
            initProtocolConfiguration(new AssetAttribute("eveningScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(LABEL, Values.create("Evening scene")),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(false))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(22d))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("EVENING"))).toMetaItem()
                ),
            initProtocolConfiguration(new AssetAttribute("nightScene"), MacroProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(LABEL, Values.create("Night scene")),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "alarmEnabled"), Values.create(true))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1LivingroomThermostatId, "targetTemperature"), Values.create(19d))).toMetaItem(),
                    new MacroAction(new AttributeState(new AttributeRef(apartment1Id, "lastExecutedScene"), Values.create("NIGHT"))).toMetaItem()
                )
        );

        // Add triggers
        AssetAttribute homeMonday = initProtocolConfiguration(new AssetAttribute("homeMondayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Home Monday trigger"))
            );
        setTriggerType(homeMonday, TriggerType.TIME);
        setTriggerValue(homeMonday, Values.create("0 0 7 ? * MON *"));
        setTriggerAction(homeMonday, new AttributeState(apartment1Id, "home", Values.create("REQUEST_START")));
        AssetAttribute awayMonday = initProtocolConfiguration(new AssetAttribute("awayMondayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Away Monday trigger"))
            );
        setTriggerType(awayMonday, TriggerType.TIME);
        setTriggerValue(awayMonday, Values.create("0 30 8 ? * MON *"));
        setTriggerAction(awayMonday, new AttributeState(apartment1Id, "away", Values.create("REQUEST_START")));
        AssetAttribute eveningMonday = initProtocolConfiguration(new AssetAttribute("eveningMondayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Evening Monday trigger"))
            );
        setTriggerType(eveningMonday, TriggerType.TIME);
        setTriggerValue(eveningMonday, Values.create("0 30 17 ? * MON *"));
        setTriggerAction(eveningMonday, new AttributeState(apartment1Id, "evening", Values.create("REQUEST_START")));
        AssetAttribute nightMonday = initProtocolConfiguration(new AssetAttribute("nightMondayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Night Monday trigger"))
            );
        setTriggerType(nightMonday, TriggerType.TIME);
        setTriggerValue(nightMonday, Values.create("0 0 22 ? * MON *"));
        setTriggerAction(nightMonday, new AttributeState(apartment1Id, "night", Values.create("REQUEST_START")));


        AssetAttribute homeTuesday = initProtocolConfiguration(new AssetAttribute("homeTuesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Home Tuesday trigger"))
            );
        setTriggerType(homeTuesday, TriggerType.TIME);
        setTriggerValue(homeTuesday, Values.create("0 0 7 ? * TUE *"));
        setTriggerAction(homeTuesday, new AttributeState(apartment1Id, "home", Values.create("REQUEST_START")));
        AssetAttribute awayTuesday = initProtocolConfiguration(new AssetAttribute("awayTuesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Away Tuesday trigger"))
            );
        setTriggerType(awayTuesday, TriggerType.TIME);
        setTriggerValue(awayTuesday, Values.create("0 30 8 ? * TUE *"));
        setTriggerAction(awayTuesday, new AttributeState(apartment1Id, "away", Values.create("REQUEST_START")));
        AssetAttribute eveningTuesday = initProtocolConfiguration(new AssetAttribute("eveningTuesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Evening Tuesday trigger"))
            );
        setTriggerType(eveningTuesday, TriggerType.TIME);
        setTriggerValue(eveningTuesday, Values.create("0 30 17 ? * TUE *"));
        setTriggerAction(eveningTuesday, new AttributeState(apartment1Id, "evening", Values.create("REQUEST_START")));
        AssetAttribute nightTuesday = initProtocolConfiguration(new AssetAttribute("nightTuesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Night Tuesday trigger"))
            );
        setTriggerType(nightTuesday, TriggerType.TIME);
        setTriggerValue(nightTuesday, Values.create("0 0 22 ? * TUE *"));
        setTriggerAction(nightTuesday, new AttributeState(apartment1Id, "night", Values.create("REQUEST_START")));


        AssetAttribute homeWednesday = initProtocolConfiguration(new AssetAttribute("homeWednesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Home Wednesday trigger"))
            );
        setTriggerType(homeWednesday, TriggerType.TIME);
        setTriggerValue(homeWednesday, Values.create("0 0 7 ? * WED *"));
        setTriggerAction(homeWednesday, new AttributeState(apartment1Id, "home", Values.create("REQUEST_START")));
        AssetAttribute awayWednesday = initProtocolConfiguration(new AssetAttribute("awayWednesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Away Wednesday trigger"))
            );
        setTriggerType(awayWednesday, TriggerType.TIME);
        setTriggerValue(awayWednesday, Values.create("0 30 8 ? * WED *"));
        setTriggerAction(awayWednesday, new AttributeState(apartment1Id, "away", Values.create("REQUEST_START")));
        AssetAttribute eveningWednesday = initProtocolConfiguration(new AssetAttribute("eveningWednesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Evening Wednesday trigger"))
            );
        setTriggerType(eveningWednesday, TriggerType.TIME);
        setTriggerValue(eveningWednesday, Values.create("0 30 17 ? * WED *"));
        setTriggerAction(eveningWednesday, new AttributeState(apartment1Id, "evening", Values.create("REQUEST_START")));
        AssetAttribute nightWednesday = initProtocolConfiguration(new AssetAttribute("nightWednesdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Night Wednesday trigger"))
            );
        setTriggerType(nightWednesday, TriggerType.TIME);
        setTriggerValue(nightWednesday, Values.create("0 0 22 ? * WED *"));
        setTriggerAction(nightWednesday, new AttributeState(apartment1Id, "night", Values.create("REQUEST_START")));


        AssetAttribute homeThursday = initProtocolConfiguration(new AssetAttribute("homeThursdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Home Thursday trigger"))
            );
        setTriggerType(homeThursday, TriggerType.TIME);
        setTriggerValue(homeThursday, Values.create("0 0 7 ? * THU *"));
        setTriggerAction(homeThursday, new AttributeState(apartment1Id, "home", Values.create("REQUEST_START")));
        AssetAttribute awayThursday = initProtocolConfiguration(new AssetAttribute("awayThursdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Away Thursday trigger"))
            );
        setTriggerType(awayThursday, TriggerType.TIME);
        setTriggerValue(awayThursday, Values.create("0 30 8 ? * THU *"));
        setTriggerAction(awayThursday, new AttributeState(apartment1Id, "away", Values.create("REQUEST_START")));
        AssetAttribute eveningThursday = initProtocolConfiguration(new AssetAttribute("eveningThursdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Evening Thursday trigger"))
            );
        setTriggerType(eveningThursday, TriggerType.TIME);
        setTriggerValue(eveningThursday, Values.create("0 30 17 ? * THU *"));
        setTriggerAction(eveningThursday, new AttributeState(apartment1Id, "evening", Values.create("REQUEST_START")));
        AssetAttribute nightThursday = initProtocolConfiguration(new AssetAttribute("nightThursdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Night Thursday trigger"))
            );
        setTriggerType(nightThursday, TriggerType.TIME);
        setTriggerValue(nightThursday, Values.create("0 0 22 ? * THU *"));
        setTriggerAction(nightThursday, new AttributeState(apartment1Id, "night", Values.create("REQUEST_START")));


        AssetAttribute homeFriday = initProtocolConfiguration(new AssetAttribute("homeFridayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Home Friday trigger"))
            );
        setTriggerType(homeFriday, TriggerType.TIME);
        setTriggerValue(homeFriday, Values.create("0 0 7 ? * FRI *"));
        setTriggerAction(homeFriday, new AttributeState(apartment1Id, "home", Values.create("REQUEST_START")));
        AssetAttribute awayFriday = initProtocolConfiguration(new AssetAttribute("awayFridayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Away Friday trigger"))
            );
        setTriggerType(awayFriday, TriggerType.TIME);
        setTriggerValue(awayFriday, Values.create("0 30 8 ? * FRI *"));
        setTriggerAction(awayFriday, new AttributeState(apartment1Id, "away", Values.create("REQUEST_START")));
        AssetAttribute eveningFriday = initProtocolConfiguration(new AssetAttribute("eveningFridayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Evening Friday trigger"))
            );
        setTriggerType(eveningFriday, TriggerType.TIME);
        setTriggerValue(eveningFriday, Values.create("0 30 17 ? * FRI *"));
        setTriggerAction(eveningFriday, new AttributeState(apartment1Id, "evening", Values.create("REQUEST_START")));
        AssetAttribute nightFriday = initProtocolConfiguration(new AssetAttribute("nightFridayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Night Friday trigger"))
            );
        setTriggerType(nightFriday, TriggerType.TIME);
        setTriggerValue(nightFriday, Values.create("0 0 22 ? * FRI *"));
        setTriggerAction(nightFriday, new AttributeState(apartment1Id, "night", Values.create("REQUEST_START")));


        AssetAttribute homeSaturday = initProtocolConfiguration(new AssetAttribute("homeSaturdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Home Saturday trigger"))
            );
        setTriggerType(homeSaturday, TriggerType.TIME);
        setTriggerValue(homeSaturday, Values.create("0 0 8 ? * SAT *"));
        setTriggerAction(homeSaturday, new AttributeState(apartment1Id, "home", Values.create("REQUEST_START")));
        AssetAttribute awaySaturday = initProtocolConfiguration(new AssetAttribute("awaySaturdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Away Saturday trigger"))
            );
        setTriggerType(awaySaturday, TriggerType.TIME);
        setTriggerValue(awaySaturday, Values.create("0 30 9 ? * SAT *"));
        setTriggerAction(awaySaturday, new AttributeState(apartment1Id, "away", Values.create("REQUEST_START")));
        AssetAttribute eveningSaturday = initProtocolConfiguration(new AssetAttribute("eveningSaturdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Evening Saturday trigger"))
            );
        setTriggerType(eveningSaturday, TriggerType.TIME);
        setTriggerValue(eveningSaturday, Values.create("0 0 18 ? * SAT *"));
        setTriggerAction(eveningSaturday, new AttributeState(apartment1Id, "evening", Values.create("REQUEST_START")));
        AssetAttribute nightSaturday = initProtocolConfiguration(new AssetAttribute("nightSaturdayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Night Saturday trigger"))
            );
        setTriggerType(nightSaturday, TriggerType.TIME);
        setTriggerValue(nightSaturday, Values.create("0 0 23 ? * SAT *"));
        setTriggerAction(nightSaturday, new AttributeState(apartment1Id, "night", Values.create("REQUEST_START")));


        AssetAttribute homeSunday = initProtocolConfiguration(new AssetAttribute("homeSundayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Home Sunday trigger"))
            );
        setTriggerType(homeSunday, TriggerType.TIME);
        setTriggerValue(homeSunday, Values.create("0 0 8 ? * SUN *"));
        setTriggerAction(homeSunday, new AttributeState(apartment1Id, "home", Values.create("REQUEST_START")));
        AssetAttribute awaySunday = initProtocolConfiguration(new AssetAttribute("awaySundayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Away Sunday trigger"))
            );
        setTriggerType(awaySunday, TriggerType.TIME);
        setTriggerValue(awaySunday, Values.create("0 30 9 ? * SUN *"));
        setTriggerAction(awaySunday, new AttributeState(apartment1Id, "away", Values.create("REQUEST_START")));
        AssetAttribute eveningSunday = initProtocolConfiguration(new AssetAttribute("eveningSundayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Evening Sunday trigger"))
            );
        setTriggerType(eveningSunday, TriggerType.TIME);
        setTriggerValue(eveningSunday, Values.create("0 0 18 ? * SUN *"));
        setTriggerAction(eveningSunday, new AttributeState(apartment1Id, "evening", Values.create("REQUEST_START")));
        AssetAttribute nightSunday = initProtocolConfiguration(new AssetAttribute("nightSundayTrigger"), TriggerProtocol.PROTOCOL_NAME)
            .addMeta(
                new MetaItem(LABEL, Values.create("Night Sunday trigger"))
            );
        setTriggerType(nightSunday, TriggerType.TIME);
        setTriggerValue(nightSunday, Values.create("0 0 23 ? * SUN *"));
        setTriggerAction(nightSunday, new AttributeState(apartment1Id, "night", Values.create("REQUEST_START")));

        // Save the scenes and trigger agent
        apartment1SceneAgent.addAttributes(
            homeMonday,
            homeTuesday,
            homeWednesday,
            homeThursday,
            homeFriday,
            homeSaturday,
            homeSunday,
            awayMonday,
            awayTuesday,
            awayWednesday,
            awayThursday,
            awayFriday,
            awaySaturday,
            awaySunday,
            eveningMonday,
            eveningTuesday,
            eveningWednesday,
            eveningThursday,
            eveningFriday,
            eveningSaturday,
            eveningSunday,
            nightMonday,
            nightTuesday,
            nightWednesday,
            nightThursday,
            nightFriday,
            nightSaturday,
            nightSunday
        );
        apartment1SceneAgent = assetStorageService.merge(apartment1SceneAgent);
        apartment1SceneAgentId = apartment1SceneAgent.getId();

        // Update apartment 1 with scenes and triggers
        apartment1.addAttributes(
            new AssetAttribute("home", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home scene")),
                    new MetaItem(DESCRIPTION, Values.create("Execute the home scene")),
                    new MetaItem(EXECUTABLE, Values.create(true)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeScene").toArrayValue())
                ),
            new AssetAttribute("homeAlarmEnabled", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home alarm enabled")),
                    new MetaItem(DESCRIPTION, Values.create("Alarm enabled for home scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(0)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeScene").toArrayValue())
                ),
            new AssetAttribute("homeTargetTemperature", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home target temperature")),
                    new MetaItem(DESCRIPTION, Values.create("Target temperature for home scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(1)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeScene").toArrayValue())
                ),
            new AssetAttribute("away", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away scene")),
                    new MetaItem(DESCRIPTION, Values.create("Execute the away scene")),
                    new MetaItem(EXECUTABLE, Values.create(true)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayScene").toArrayValue())
                ),
            new AssetAttribute("awayAlarmEnabled", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away alarm enabled")),
                    new MetaItem(DESCRIPTION, Values.create("Alarm enabled for away scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(0)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayScene").toArrayValue())
                ),
            new AssetAttribute("awayTargetTemperature", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away target temperature")),
                    new MetaItem(DESCRIPTION, Values.create("Target temperature for away scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(1)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayScene").toArrayValue())
                ),
            new AssetAttribute("evening", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening scene")),
                    new MetaItem(DESCRIPTION, Values.create("Execute the evening scene")),
                    new MetaItem(EXECUTABLE, Values.create(true)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningScene").toArrayValue())
                ),
            new AssetAttribute("eveningAlarmEnabled", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening alarm enabled")),
                    new MetaItem(DESCRIPTION, Values.create("Alarm enabled for evening scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(0)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningScene").toArrayValue())
                ),
            new AssetAttribute("eveningTargetTemperature", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening target temperature")),
                    new MetaItem(DESCRIPTION, Values.create("Target temperature for evening scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(1)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningScene").toArrayValue())
                ),
            new AssetAttribute("night", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night scene")),
                    new MetaItem(DESCRIPTION, Values.create("Execute the night scene")),
                    new MetaItem(EXECUTABLE, Values.create(true)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightScene").toArrayValue())
                ),
            new AssetAttribute("nightAlarmEnabled", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night alarm enabled")),
                    new MetaItem(DESCRIPTION, Values.create("Alarm enabled for night scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(0)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightScene").toArrayValue())
                ),
            new AssetAttribute("nightTargetTemperature", AttributeType.DECIMAL)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night target temperature")),
                    new MetaItem(DESCRIPTION, Values.create("Target temperature for night scene")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(PROPERTY_MACRO_ACTION)),
                    new MetaItem(META_MACRO_ACTION_INDEX, Values.create(1)),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightScene").toArrayValue())
                ),
            new AssetAttribute("homeTimeMONDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home time Monday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeEnabledMONDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home enabled Monday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayTimeMONDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away time Monday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayEnabledMONDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away enabled Monday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningTimeMONDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening time Monday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningEnabledMONDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening enabled Monday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightTimeMONDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night time Monday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightEnabledMONDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night enabled Monday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightMondayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeTimeTUESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home time Tuesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeEnabledTUESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home enabled Tuesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayTimeTUESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away time Tuesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayEnabledTUESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away enabled Tuesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningTimeTUESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening time Tuesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningEnabledTUESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening enabled Tuesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightTimeTUESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night time Tuesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightEnabledTUESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night enabled Tuesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightTuesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeTimeWEDNESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home time Wednesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeEnabledWEDNESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home enabled Wednesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayTimeWEDNESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away time Wednesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayEnabledWEDNESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away enabled Wednesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningTimeWEDNESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening time Wednesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningEnabledWEDNESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening enabled Wednesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightTimeWEDNESDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night time Wednesday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightEnabledWEDNESDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night enabled Wednesday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightWednesdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeTimeTHURSDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home time Thursday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeEnabledTHURSDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home enabled Thursday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayTimeTHURSDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away time Thursday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayEnabledTHURSDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away enabled Thursday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningTimeTHURSDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening time Thursday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningEnabledTHURSDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening enabled Thursday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightTimeTHURSDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night time Thursday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightEnabledTHURSDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night enabled Thursday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightThursdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeTimeFRIDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home time Friday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeEnabledFRIDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home enabled Friday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayTimeFRIDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away time Friday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayEnabledFRIDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away enabled Friday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awayFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningTimeFRIDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening time Friday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningEnabledFRIDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening enabled Friday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightTimeFRIDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night time Friday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightEnabledFRIDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night enabled Friday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightFridayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeTimeSATURDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home time Saturday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeSaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeEnabledSATURDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home enabled Saturday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeSaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayTimeSATURDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away time Saturday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awaySaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayEnabledSATURDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away enabled Saturday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awaySaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningTimeSATURDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening time Saturday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningSaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningEnabledSATURDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening enabled Saturday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningSaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightTimeSATURDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night time Saturday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightSaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightEnabledSATURDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night enabled Saturday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightSaturdayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeTimeSUNDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home time Sunday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeSundayTrigger").toArrayValue())
                ),
            new AssetAttribute("homeEnabledSUNDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Home enabled Sunday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "homeSundayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayTimeSUNDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away time Sunday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awaySundayTrigger").toArrayValue())
                ),
            new AssetAttribute("awayEnabledSUNDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Away enabled Sunday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "awaySundayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningTimeSUNDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening time Sunday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningSundayTrigger").toArrayValue())
                ),
            new AssetAttribute("eveningEnabledSUNDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Evening enabled Sunday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "eveningSundayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightTimeSUNDAY", AttributeType.STRING)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night time Sunday")),
                    new MetaItem(RULE_STATE, Values.create(true)),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(TimeTriggerProperty.TIME.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightSundayTrigger").toArrayValue())
                ),
            new AssetAttribute("nightEnabledSUNDAY", AttributeType.BOOLEAN)
                .setMeta(
                    new MetaItem(LABEL, Values.create("Night enabled Sunday")),
                    new MetaItem(PROTOCOL_PROPERTY, Values.create(ENABLED.toString())),
                    new MetaItem(AGENT_LINK, new AttributeRef(apartment1SceneAgentId, "nightSundayTrigger").toArrayValue())
                )
        );

        apartment1 = assetStorageService.merge(apartment1);

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
                        new MetaItem(DESCRIPTION, Values.create(
                            "Someone is currently present in the room"
                        )),
                        new MetaItem(RULE_STATE, Values.create(true))
                ),
            new AssetAttribute("lastPresenceDetected", AttributeType.DECIMAL)
                .setMeta(
                        new MetaItem(LABEL, Values.create("Last Presence Timestamp")),
                        new MetaItem(DESCRIPTION, Values.create(
                            "Timestamp of last detected presence"
                        )),
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
