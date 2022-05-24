/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.setup.demo;

import org.openremote.agent.protocol.knx.KNXAgent;
import org.openremote.agent.protocol.velbus.VelbusTCPAgent;
import org.openremote.container.util.MapAccess;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.security.Realm;

import java.util.logging.Logger;

public class ManagerDemoAgentSetup extends ManagerSetup {

    private static final Logger LOG = Logger.getLogger(ManagerDemoAgentSetup.class.getName());

    public static final String OR_SETUP_IMPORT_DEMO_AGENT_KNX = "OR_SETUP_IMPORT_DEMO_AGENT_KNX";
    public static final String OR_SETUP_IMPORT_DEMO_AGENT_KNX_GATEWAY_IP = "OR_SETUP_IMPORT_DEMO_AGENT_KNX_GATEWAY_IP";
    public static final String OR_SETUP_IMPORT_DEMO_AGENT_KNX_LOCAL_IP = "OR_SETUP_IMPORT_DEMO_AGENT_KNX_LOCAL_IP";

    public static final String OR_SETUP_IMPORT_DEMO_AGENT_VELBUS = "OR_SETUP_IMPORT_DEMO_AGENT_VELBUS";
    public static final String OR_SETUP_IMPORT_DEMO_AGENT_VELBUS_HOST = "OR_SETUP_IMPORT_DEMO_AGENT_VELBUS_HOST";
    public static final String OR_SETUP_IMPORT_DEMO_AGENT_VELBUS_PORT = "OR_SETUP_IMPORT_DEMO_AGENT_VELBUS_PORT";

    public String realmMasterName;

    final protected boolean knx;
    final protected String knxGatewayIp;
    final protected String knxLocalIp;

    final protected boolean velbus;
    final protected String velbusHost;
    final protected Integer velbusPort;

    public ManagerDemoAgentSetup(Container container) {
        super(container);

        this.knx = MapAccess.getBoolean(container.getConfig(), OR_SETUP_IMPORT_DEMO_AGENT_KNX, false);
        this.knxGatewayIp = MapAccess.getString(container.getConfig(), OR_SETUP_IMPORT_DEMO_AGENT_KNX_GATEWAY_IP, "localhost");
        this.knxLocalIp = MapAccess.getString(container.getConfig(), OR_SETUP_IMPORT_DEMO_AGENT_KNX_LOCAL_IP, "localhost");

        this.velbus = MapAccess.getBoolean(container.getConfig(), OR_SETUP_IMPORT_DEMO_AGENT_VELBUS, false);
        this.velbusHost = MapAccess.getString(container.getConfig(), OR_SETUP_IMPORT_DEMO_AGENT_VELBUS_HOST, "localhost");
        this.velbusPort = MapAccess.getInteger(container.getConfig(), OR_SETUP_IMPORT_DEMO_AGENT_VELBUS_PORT, 6000);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Realm realmMaster = keycloakDemoSetup.realmMaster;
        realmMasterName = realmMaster.getName();

        if (knx) {
            LOG.info("Enable KNX demo agent, gateway/local IP: " + knxGatewayIp + "/" + knxLocalIp);

            KNXAgent agent = new KNXAgent("Demo KNX agent")
                .setRealm(realmMasterName)
                .setHost(knxGatewayIp)
                .setBindHost(knxLocalIp);

            agent = assetStorageService.merge(agent);
        }

        if (velbus) {
            LOG.info("Enable Velbus demo agent, host/port: " + velbusHost + "/" + velbusPort);

            VelbusTCPAgent agent = new VelbusTCPAgent("Demo VELBUS agent")
                .setRealm(realmMasterName)
                .setHost(velbusHost)
                .setPort(velbusPort);

            agent = assetStorageService.merge(agent);
        }
    }
}
