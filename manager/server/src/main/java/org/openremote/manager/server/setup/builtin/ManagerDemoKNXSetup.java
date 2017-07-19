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

import static org.openremote.model.asset.AssetMeta.DESCRIPTION;
import static org.openremote.model.asset.AssetMeta.LABEL;
import static org.openremote.model.asset.AssetMeta.READ_ONLY;
import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.AssetType.BUILDING;
import static org.openremote.model.asset.AssetType.ROOM;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;

import org.openremote.agent.protocol.knx.KNXProtocol;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import com.vividsolutions.jts.geom.Coordinate;

public class ManagerDemoKNXSetup extends AbstractManagerSetup {

    public String masterRealmId;

    public ManagerDemoKNXSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        masterRealmId = masterTenant.getId();

        // ########### KNX assets in 'master' realm  ####################
        ServerAsset knxOffice = new ServerAsset("KNX Building", BUILDING);
        knxOffice.setRealmId(masterTenant.getId());
        //knxOffice.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        knxOffice = assetStorageService.merge(knxOffice);
        
        ServerAsset knxAgent = new ServerAsset("KNX Agent", AGENT, knxOffice);
        knxAgent.setAttributes(
            initProtocolConfiguration(new AssetAttribute("knxConfig"), KNXProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(KNXProtocol.KNX_GATEWAY_IP, Values.create("192.168.100.10"))
                )
        );
        knxAgent.setRealmId(masterTenant.getId());
        knxAgent = assetStorageService.merge(knxAgent);
        
        ServerAsset knxThing = new ServerAsset("Office", ROOM, knxOffice);
        knxThing.setAttributes(
                new AssetAttribute("Light1_Switch", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem(LABEL, Values.create("Light 1 (Switch)")),
                        new MetaItem(DESCRIPTION, Values.create("Light 1 connected to a KNX switch actuator")),
                        new MetaItem(KNXProtocol.ACTION_GA, Values.create("1/0/17")),
                        new MetaItem(KNXProtocol.STATUS_GA, Values.create("0/4/14")),
                        new MetaItem(KNXProtocol.DPT, Values.create("1.001")),
                        new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(knxAgent.getId(), "knxConfig").toArrayValue())
                    ),
                new AssetAttribute("Light2_Switch", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem(LABEL, Values.create("Light 2 (Switch)")),
                        new MetaItem(DESCRIPTION, Values.create("Light 2 connected to a KNX dimming actuator")),
                        new MetaItem(KNXProtocol.ACTION_GA, Values.create("1/0/11")),
                        new MetaItem(KNXProtocol.STATUS_GA, Values.create("0/4/10")),
                        new MetaItem(KNXProtocol.DPT, Values.create("1.001")),
                        new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(knxAgent.getId(), "knxConfig").toArrayValue())
                    ),
                new AssetAttribute("Light2_Dimming", AttributeType.PERCENTAGE)
                    .setMeta(
                        new MetaItem(LABEL, Values.create("Light 2 (Dimming)")),
                        new MetaItem(DESCRIPTION, Values.create("Light 2 connected to a KNX dimming actuator")),
                        new MetaItem(KNXProtocol.ACTION_GA, Values.create("1/0/13")),
                        new MetaItem(KNXProtocol.STATUS_GA, Values.create("0/4/11")),
                        new MetaItem(KNXProtocol.DPT, Values.create("5.001")),
                        new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(knxAgent.getId(), "knxConfig").toArrayValue())
                    ),
                new AssetAttribute("Temperature", AttributeType.TEMPERATURE_CELCIUS)
                    .setMeta(
                        new MetaItem(LABEL, Values.create("Temperature")),
                        new MetaItem(DESCRIPTION, Values.create("Temperature given by a KNX sensor")),
                        new MetaItem(READ_ONLY, Values.create(true)), 
                        new MetaItem(KNXProtocol.STATUS_GA, Values.create("4/1/10")),
                        new MetaItem(KNXProtocol.DPT, Values.create("9.001")),
                        new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(knxAgent.getId(), "knxConfig").toArrayValue())
                    ),
                new AssetAttribute("Presence", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem(LABEL, Values.create("Presence")),
                        new MetaItem(DESCRIPTION, Values.create("KNX presence detector")),
                        new MetaItem(READ_ONLY, Values.create(true)), 
                        new MetaItem(KNXProtocol.STATUS_GA, Values.create("4/0/2")),
                        new MetaItem(KNXProtocol.DPT, Values.create("1.001")),
                        new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(knxAgent.getId(), "knxConfig").toArrayValue())
                    ),
                new AssetAttribute("WindowStatus", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem(LABEL, Values.create("Window status")),
                        new MetaItem(DESCRIPTION, Values.create("KNX binary input")),
                        new MetaItem(READ_ONLY, Values.create(true)), 
                        new MetaItem(KNXProtocol.STATUS_GA, Values.create("4/0/16")),
                        new MetaItem(KNXProtocol.DPT, Values.create("1.001")),
                        new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(knxAgent.getId(), "knxConfig").toArrayValue())
                    )
        );


        knxThing = assetStorageService.merge(knxThing);
    }
}