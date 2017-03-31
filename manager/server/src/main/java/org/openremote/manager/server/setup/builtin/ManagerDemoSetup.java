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
import org.openremote.model.asset.AssetAttributes;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.AgentAttributes;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.units.AttributeUnits;
import org.openremote.model.units.ColorRGB;

import static org.openremote.model.AttributeType.*;
import static org.openremote.model.asset.AssetMeta.*;
import static org.openremote.model.asset.AssetType.*;

public class ManagerDemoSetup extends AbstractManagerSetup {

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
        Tenant customerBTenant = keycloakDemoSetup.customerBTenant;

        // ################################ Demo assets for 'master' realm ###################################


        ServerAsset smartOffice = new ServerAsset();
        smartOffice.setRealmId(masterTenant.getId());
        smartOffice.setName("Smart Office");
        smartOffice.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        smartOffice.setType(BUILDING);
        AssetAttributes smartOfficeAttributes = new AssetAttributes();
        smartOfficeAttributes.put(
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
        smartOffice.setAttributes(smartOfficeAttributes.getJsonObject());
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
        AgentAttributes agentAttributes = new AgentAttributes();
        ProtocolConfiguration protocolConfigSimulator123 = new ProtocolConfiguration(agentProtocolConfigName, SimulatorProtocol.PROTOCOL_NAME);
        protocolConfigSimulator123.setMeta(new Meta()
            .add(new MetaItem(
                    SimulatorProtocol.CONFIG_MODE,
                    Json.create(SimulatorProtocol.Mode.WRITE_THROUGH_DELAYED.toString())
                )
            )
            .add(new MetaItem(
                    SimulatorProtocol.CONFIG_WRITE_DELAY_MILLISECONDS,
                    Json.create(500)
                )
            ));
        agentAttributes.put(protocolConfigSimulator123);
        agent.setAttributes(agentAttributes.getJsonObject());
        agent = assetStorageService.merge(agent);
        agentId = agent.getId();

        ServerAsset thing = new ServerAsset(agent);
        thing.setName("Demo Thing");
        thing.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        thing.setType(AssetType.THING);
        AssetAttributes thingAttributes = new AssetAttributes(thing);
        thingAttributes.put(
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
                ),
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
                ),
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
                ),
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
                        AssetMeta.STORE_DATA_POINTS.getName(), Json.create(true)
                    ))
                )
        );
        thing.setAttributes(thingAttributes.getJsonObject());
        thing = assetStorageService.merge(thing);
        thingId = thing.getId();

        // ################################ Demo assets for 'customerA' realm ###################################

        ServerAsset smartHome = new ServerAsset();
        smartHome.setRealmId(customerATenant.getId());
        smartHome.setName("Smart Home");
        smartHome.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        smartHome.setType(BUILDING);
        AssetAttributes smartHomeAttributes = new AssetAttributes();
        smartHomeAttributes.put(
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
        smartHome.setAttributes(smartHomeAttributes.getJsonObject());
        smartHome = assetStorageService.merge(smartHome);
        smartHomeId = smartHome.getId();

        ServerAsset apartment1 = new ServerAsset(smartHome);
        apartment1.setName("Apartment 1");
        apartment1.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1.setType(RESIDENCE);
        AssetAttributes apartment1Attributes = new AssetAttributes();
        apartment1Attributes.put(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("All Lights Off Switch"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("When triggered, turns all lights in the apartment off"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(true))
                    )
                )
        );
        apartment1.setAttributes(apartment1Attributes.getJsonObject());
        apartment1 = assetStorageService.merge(apartment1);
        apartment1Id = apartment1.getId();

        ServerAsset apartment1Livingroom = new ServerAsset(apartment1);
        apartment1Livingroom.setName("Livingroom");
        apartment1Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment1Livingroom.setType(ROOM);
        AssetAttributes apartment1LivingroomAttributes = new AssetAttributes();
        apartment1LivingroomAttributes.put(
            new AssetAttribute("presenceSensor", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Sensor"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_EVENT, Json.create(true))
                    )
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Detected"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(true))
                    )
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Json.create(true))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Light Switch"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(false))
                    )
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Window Open"))
                    )
                )
        );
        apartment1Livingroom.setAttributes(apartment1LivingroomAttributes.getJsonObject());
        apartment1Livingroom = assetStorageService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        ServerAsset apartment1LivingroomThermostat = new ServerAsset(apartment1Livingroom);
        apartment1LivingroomThermostat.setName("Livingroom Thermostat");
        apartment1LivingroomThermostat.setType(AssetType.THING);
        AssetAttributes apartment1LivingroomThermostatAttributes = new AssetAttributes(apartment1LivingroomThermostat);
        apartment1LivingroomThermostatAttributes.put(
            new AssetAttribute("currentTemperature", DECIMAL, Json.createNull())
                .setMeta(new Meta()
                    .add(new MetaItem(
                        AssetMeta.LABEL,
                        Json.create("Current Temperature"))
                    )
                    .add(new MetaItem(
                        AssetMeta.RULES_FACT,
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
                        AssetMeta.RULES_FACT,
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

        apartment1LivingroomThermostat.setAttributes(apartment1LivingroomThermostatAttributes.getJsonObject());
        apartment1LivingroomThermostat = assetStorageService.merge(apartment1LivingroomThermostat);
        apartment1LivingroomThermostatId = apartment1LivingroomThermostat.getId();

        ServerAsset apartment2 = new ServerAsset(smartHome);
        apartment2.setName("Apartment 2");
        apartment2.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2.setType(RESIDENCE);
        AssetAttributes apartment2Attributes = new AssetAttributes();
        apartment2Attributes.put(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("All Lights Off Switch"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("When triggered, turns all lights in the apartment off"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(true))
                    )
                )
        );
        apartment2.setAttributes(apartment2Attributes.getJsonObject());
        apartment2 = assetStorageService.merge(apartment2);
        apartment2Id = apartment2.getId();

        ServerAsset apartment2Livingroom = new ServerAsset(apartment2);
        apartment2Livingroom.setName("Livingroom");
        apartment2Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment2Livingroom.setType(ROOM);
        AssetAttributes apartment2LivingroomAttributes = new AssetAttributes();
        apartment2LivingroomAttributes.put(
            new AssetAttribute("presenceSensor", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Sensor"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_EVENT, Json.create(true))
                    )
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Detected"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(true))
                    )
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Json.create(true))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Light Switch"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(false))
                    )
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Window Open"))
                    )
                )
        );
        apartment2Livingroom.setAttributes(apartment2LivingroomAttributes.getJsonObject());
        apartment2Livingroom = assetStorageService.merge(apartment2Livingroom);
        apartment2LivingroomId = apartment2Livingroom.getId();

        ServerAsset apartment3 = new ServerAsset(smartHome);
        apartment3.setName("Apartment 3");
        apartment3.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment3.setType(RESIDENCE);
        AssetAttributes apartment3Attributes = new AssetAttributes();
        apartment3Attributes.put(
            new AssetAttribute("allLightsOffSwitch", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("All Lights Off Switch"))
                    ).add(
                        new MetaItem(AssetMeta.DESCRIPTION, Json.create("When triggered, turns all lights in the apartment off"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(true))
                    )
                )
        );
        apartment3.setAttributes(apartment3Attributes.getJsonObject());
        apartment3 = assetStorageService.merge(apartment3);
        apartment3Id = apartment3.getId();

        ServerAsset apartment3Livingroom = new ServerAsset(apartment3);
        apartment3Livingroom.setName("Livingroom");
        apartment3Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.470945, 51.438000)));
        apartment3Livingroom.setType(ROOM);
        AssetAttributes apartment3LivingroomAttributes = new AssetAttributes();
        apartment3LivingroomAttributes.put(
            new AssetAttribute("presenceSensor", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Sensor"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_EVENT, Json.create(true))
                    )
                ),
            new AssetAttribute("presenceDetected", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Presence Detected"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(true))
                    )
                ),
            new AssetAttribute("lightSwitch", AttributeType.BOOLEAN, Json.create(true))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Light Switch"))
                    ).add(
                        new MetaItem(AssetMeta.RULES_FACT, Json.create(false))
                    )
                ),
            new AssetAttribute("windowOpen", AttributeType.BOOLEAN, Json.create(false))
                .setMeta(
                    new Meta().add(
                        new MetaItem(AssetMeta.LABEL, Json.create("Window Open"))
                    )
                )
        );
        apartment3Livingroom.setAttributes(apartment3LivingroomAttributes.getJsonObject());
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
