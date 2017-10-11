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

import org.openremote.agent.protocol.knx.KNXProtocol;
import org.openremote.agent.protocol.velbus.VelbusSerialProtocol;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.AssetType.THING;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;

public class ManagerDemoAgentSetup extends AbstractManagerSetup {

    public String masterRealmId;

    public ManagerDemoAgentSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        masterRealmId = masterTenant.getId();

        // ########### Assets in 'master' realm  ####################
        ServerAsset agent = new ServerAsset("Agent", AGENT);
        agent.setRealmId(masterRealmId);
        agent.setAttributes(
            initProtocolConfiguration(new AssetAttribute("knxConfig"), KNXProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(KNXProtocol.META_KNX_GATEWAY_IP, Values.create("192.168.0.64")),
                    new MetaItem(KNXProtocol.META_KNX_LOCAL_IP, Values.create("192.168.0.65"))
                ),
            initProtocolConfiguration(new AssetAttribute("velbusConfig"), VelbusSerialProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(VelbusSerialProtocol.META_VELBUS_SERIAL_PORT, Values.create("COM3"))
                )
        );
        agent = assetStorageService.merge(agent);

        ServerAsset velbusDevices = new ServerAsset("VELBUS Devices", THING, agent);
        velbusDevices.setRealmId(masterRealmId);
        velbusDevices = assetStorageService.merge(velbusDevices);

        ServerAsset knxDevices = new ServerAsset("KNX Devices", THING, agent);
        knxDevices.setRealmId(masterRealmId);
        knxDevices = assetStorageService.merge(knxDevices);


    }
}