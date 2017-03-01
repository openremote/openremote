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
import com.vividsolutions.jts.geom.GeometryFactory;
import elemental.json.Json;
import org.openremote.agent3.protocol.simulator.SimulatorProtocol;
import org.openremote.container.Container;
import org.openremote.manager.server.agent.AgentAttributes;
import org.openremote.manager.server.agent.ThingAttributes;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.model.*;
import org.openremote.model.asset.*;

import static org.openremote.model.Constants.*;
import static org.openremote.model.AttributeType.*;
import static org.openremote.model.asset.AssetAttributeMeta.*;
import static org.openremote.model.asset.AssetType.RESIDENCE;
import static org.openremote.model.asset.AssetType.BUILDING;
import static org.openremote.model.asset.AssetType.ROOM;

public class ManagerDemoSetup extends AbstractManagerSetup {

    public String smartOfficeId;
    public String groundFloorId;
    public String lobbyId;
    public String agentId;
    public String thingId;
    public String smartHomeId;
    public String apartment1Id;
    public String apartment1LivingroomId;
    public String apartment2Id;
    public String apartment3Id;
    public String apartment2LivingroomId;
    public String apartment3LivingroomId;

    public ManagerDemoSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() throws Exception {

        // ################################ Demo assets for 'master' realm ###################################

        ServerAsset smartOffice = new ServerAsset();
        smartOffice.setRealm(MASTER_REALM);
        smartOffice.setName("Smart Office");
        smartOffice.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        smartOffice.setType(BUILDING);
        Attributes smartOfficeAttributes = new Attributes();
        smartOfficeAttributes.put(
            new Attribute("geoStreet", STRING, Json.create("Torenallee 20"))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("Street")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoStreet")))
                ),
            new Attribute("geoPostalCode", AttributeType.INTEGER, Json.create(5617))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("Postal Code")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoPostalCode")))
                ),
            new Attribute("geoCity", STRING, Json.create("Eindhoven"))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("City")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCity")))
                ),
            new Attribute("geoCountry", STRING, Json.create("Netherlands"))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("Country")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCountry")))
                )
        );
        smartOffice.setAttributes(smartOfficeAttributes.getJsonObject());
        smartOffice = assetService.merge(smartOffice);
        smartOfficeId = smartOffice.getId();

        ServerAsset groundFloor = new ServerAsset(smartOffice);
        groundFloor.setName("Ground Floor");
        groundFloor.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        groundFloor.setType(AssetType.FLOOR);
        groundFloor = assetService.merge(groundFloor);
        groundFloorId = groundFloor.getId();

        ServerAsset lobby = new ServerAsset(groundFloor);
        lobby.setName("Lobby");
        lobby.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        lobby.setType(AssetType.ROOM);
        lobby = assetService.merge(lobby);
        lobbyId = lobby.getId();

        ServerAsset agent = new ServerAsset(lobby);
        agent.setName("Demo Agent");
        agent.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        agent.setType(AssetType.AGENT);
        AgentAttributes agentAttributes = new AgentAttributes();
        agentAttributes.setEnabled(false);
        ProtocolConfiguration protocolConfigSimulator123 = new ProtocolConfiguration("simulator123", SimulatorProtocol.PROTOCOL_NAME);
        agentAttributes.put(protocolConfigSimulator123);
        agent.setAttributes(agentAttributes.getJsonObject());
        agent = assetService.merge(agent);
        agentId = agent.getId();

        ServerAsset thing = new ServerAsset(agent);
        thing.setName("Demo Thing");
        thing.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        thing.setType(AssetType.THING);
        ThingAttributes thingAttributes = new ThingAttributes(thing);
        thingAttributes.put(
            new Attribute("light1Toggle", BOOLEAN, Json.create(true))
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The switch for the light in the living room"))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("switch")
                    ))
                ),
            new Attribute("light1Dimmer", INTEGER) // No initial value!
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The dimmer for the light in the living room"))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.RANGE_MIN.getName(),
                        Json.create(0))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.RANGE_MAX.getName(),
                        Json.create(100))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("range")
                    ))
                ),
            new Attribute("light1Color", COLOR, new Color(88, 123, 88).asJsonValue())
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The color of the living room light"))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("color")
                    ))
                ),
            new Attribute("light1PowerConsumption", DECIMAL, Json.create(12.345))
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The total power consumption of the living room light"))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.READ_ONLY.getName(),
                        Json.create(true))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.FORMAT.getName(),
                        Json.create("%3d kWh"))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("decimal")
                    ))
                )
        );
        thing.setAttributes(thingAttributes.getJsonObject());
        thing = assetService.merge(thing);
        thingId = thing.getId();

        // ################################ Demo assets for 'customerA' realm ###################################

        ServerAsset smartHome = new ServerAsset();
        smartHome.setRealm("customerA");
        smartHome.setName("Smart Home");
        smartHome.setLocation(geometryFactory.createPoint(new Coordinate(5.469751699216005, 51.44760787406028)));
        smartHome.setType(BUILDING);
        smartHome = assetService.merge(smartHome);
        smartHomeId = smartHome.getId();

        ServerAsset apartment1 = new ServerAsset(smartHome);
        apartment1.setName("Apartment 1");
        apartment1.setLocation(geometryFactory.createPoint(new Coordinate(5.469751699216005, 51.44760787406028)));
        apartment1.setType(RESIDENCE);
        apartment1 = assetService.merge(apartment1);
        apartment1Id = apartment1.getId();

        ServerAsset apartment1Livingroom = new ServerAsset(apartment1);
        apartment1Livingroom.setName("Livingroom");
        apartment1Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.469751699216005, 51.44760787406028)));
        apartment1Livingroom.setType(ROOM);
        apartment1Livingroom = assetService.merge(apartment1Livingroom);
        apartment1LivingroomId = apartment1Livingroom.getId();

        ServerAsset apartment2 = new ServerAsset(smartHome);
        apartment2.setName("Apartment 2");
        apartment2.setLocation(geometryFactory.createPoint(new Coordinate(5.469751699216005, 51.44760787406028)));
        apartment2.setType(RESIDENCE);
        apartment2 = assetService.merge(apartment2);
        apartment2Id = apartment2.getId();

        ServerAsset apartment2Livingroom = new ServerAsset(apartment2);
        apartment2Livingroom.setName("Livingroom");
        apartment2Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.469751699216005, 51.44760787406028)));
        apartment2Livingroom.setType(ROOM);
        apartment2Livingroom = assetService.merge(apartment2Livingroom);
        apartment2LivingroomId = apartment2Livingroom.getId();

        ServerAsset apartment3 = new ServerAsset(smartHome);
        apartment3.setName("Apartment 3");
        apartment3.setLocation(geometryFactory.createPoint(new Coordinate(5.469751699216005, 51.44760787406028)));
        apartment3.setType(RESIDENCE);
        apartment3 = assetService.merge(apartment3);
        apartment3Id = apartment3.getId();

        ServerAsset apartment3Livingroom = new ServerAsset(apartment3);
        apartment3Livingroom.setName("Livingroom");
        apartment3Livingroom.setLocation(geometryFactory.createPoint(new Coordinate(5.469751699216005, 51.44760787406028)));
        apartment3Livingroom.setType(ROOM);
        apartment3Livingroom = assetService.merge(apartment3Livingroom);
        apartment3LivingroomId = apartment3Livingroom.getId();
    }
}
