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
import org.openremote.agent.protocol.upnp.UpnpProtocol;
import org.openremote.agent.protocol.velbus.VelbusSerialProtocol;
import org.openremote.container.Container;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.AssetType.THING;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;

public class ManagerDemoAgentSetup extends AbstractManagerSetup {

    private static final Logger LOG = Logger.getLogger(ManagerDemoAgentSetup.class.getName());

    public static final String SETUP_IMPORT_DEMO_AGENT_KNX = "SETUP_IMPORT_DEMO_AGENT_KNX";
    public static final String SETUP_IMPORT_DEMO_AGENT_KNX_GATEWAY_IP = "SETUP_IMPORT_DEMO_AGENT_KNX_GATEWAY_IP";
    public static final String SETUP_IMPORT_DEMO_AGENT_KNX_LOCAL_IP = "SETUP_IMPORT_DEMO_AGENT_KNX_LOCAL_IP";

    public static final String SETUP_IMPORT_DEMO_AGENT_VELBUS = "SETUP_IMPORT_DEMO_AGENT_VELBUS";
    public static final String SETUP_IMPORT_DEMO_AGENT_VELBUS_COM_PORT = "SETUP_IMPORT_DEMO_AGENT_VELBUS_COM_PORT";

    public static final String SETUP_IMPORT_DEMO_AGENT_UPNP = "SETUP_IMPORT_DEMO_AGENT_UPNP";

    public String masterRealmId;


    final protected boolean knx;
    final protected String knxGatewayIp;
    final protected String knxLocalIp;

    final protected boolean velbus;
    final protected String velbusComPort;

    final protected boolean upnp;

    public ManagerDemoAgentSetup(Container container) {
        super(container);

        this.knx = getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_AGENT_KNX, false);
        this.knxGatewayIp = getString(container.getConfig(), SETUP_IMPORT_DEMO_AGENT_KNX_GATEWAY_IP, "192.168.0.64");
        this.knxLocalIp = getString(container.getConfig(), SETUP_IMPORT_DEMO_AGENT_KNX_LOCAL_IP, "192.168.0.65");

        this.velbus = getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_AGENT_VELBUS, false);
        this.velbusComPort = getString(container.getConfig(), SETUP_IMPORT_DEMO_AGENT_VELBUS_COM_PORT, "COM3");

        this.upnp = getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_AGENT_UPNP, false);
    }

    @Override
    public void onStart() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        masterRealmId = masterTenant.getId();

        ServerAsset agent = new ServerAsset("Demo Agent", AGENT);
        agent.setRealmId(masterRealmId);
        agent = assetStorageService.merge(agent);

        if (knx) {
            LOG.info("Enable KNX demo protocol configuration, gateway/local IP: " + knxGatewayIp + "/" + knxLocalIp);
            agent.addAttributes(
                initProtocolConfiguration(new AssetAttribute("knxConfig"), KNXProtocol.PROTOCOL_NAME)
                    .addMeta(
                        new MetaItem(KNXProtocol.META_KNX_GATEWAY_IP, Values.create(knxGatewayIp)),
                        new MetaItem(KNXProtocol.META_KNX_LOCAL_IP, Values.create(knxLocalIp))
                    )
            );
            ServerAsset knxDevices = new ServerAsset("KNX Devices", THING, agent, masterRealmId);
            knxDevices = assetStorageService.merge(knxDevices);
        }

        if (velbus) {
            LOG.info("Enable Velbus demo protocol configuration, COM port: " + velbusComPort);
            agent.addAttributes(
                initProtocolConfiguration(new AssetAttribute("velbusConfig"), VelbusSerialProtocol.PROTOCOL_NAME)
                    .addMeta(
                        new MetaItem(VelbusSerialProtocol.META_VELBUS_SERIAL_PORT, Values.create(velbusComPort))
                    )
            );
            ServerAsset velbusDevices = new ServerAsset("VELBUS Devices", THING, agent, masterRealmId);
            velbusDevices = assetStorageService.merge(velbusDevices);
        }

        if (upnp) {
            LOG.info("Enable UPnP demo protocol configuration");
            ServerAsset upnpDevices = new ServerAsset("UPnP Devices", THING, agent, masterRealmId);
            upnpDevices = assetStorageService.merge(upnpDevices);
            agent.addAttributes(
                initProtocolConfiguration(new AssetAttribute("upnpConfig"), UpnpProtocol.PROTOCOL_NAME)
                .addMeta(
                    // TODO Protocols should create these grouping assets automatically and import assets underneath for each protocol configuration
                    new MetaItem(UpnpProtocol.GROUP_ASSET_ID, Values.create(upnpDevices.getId()))
                )
            );
        }

        agent = assetStorageService.merge(agent);
    }
}