/*
 * Copyright 2020, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.setup.integration;

import static org.openremote.model.util.MapAccess.*;

import java.util.logging.Logger;

import org.openremote.agent.protocol.knx.KNXAgent;
import org.openremote.agent.protocol.velbus.VelbusTCPAgent;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.security.Realm;

public class ManagerTestAgentSetup extends ManagerSetup {

  private static final Logger LOG = Logger.getLogger(ManagerTestAgentSetup.class.getName());

  public static final String SETUP_CREATE_AGENT_KNX = "SETUP_CREATE_AGENT_KNX";
  public static final String SETUP_CREATE_AGENT_KNX_GATEWAY_IP =
      "SETUP_CREATE_AGENT_KNX_GATEWAY_IP";
  public static final String SETUP_CREATE_AGENT_KNX_LOCAL_IP = "SETUP_CREATE_AGENT_KNX_LOCAL_IP";

  public static final String SETUP_CREATE_AGENT_VELBUS = "SETUP_CREATE_AGENT_VELBUS";
  public static final String SETUP_CREATE_AGENT_VELBUS_HOST = "SETUP_CREATE_AGENT_VELBUS_HOST";
  public static final String SETUP_CREATE_AGENT_VELBUS_PORT = "SETUP_CREATE_AGENT_VELBUS_PORT";

  public String realmMasterName;

  protected final boolean knx;
  protected final String knxGatewayIp;
  protected final String knxLocalIp;

  protected final boolean velbus;
  protected final String velbusHost;
  protected final Integer velbusPort;

  public ManagerTestAgentSetup(Container container) {
    super(container);

    this.knx = getBoolean(container.getConfig(), SETUP_CREATE_AGENT_KNX, false);
    this.knxGatewayIp =
        getString(container.getConfig(), SETUP_CREATE_AGENT_KNX_GATEWAY_IP, "localhost");
    this.knxLocalIp =
        getString(container.getConfig(), SETUP_CREATE_AGENT_KNX_LOCAL_IP, "localhost");

    this.velbus = getBoolean(container.getConfig(), SETUP_CREATE_AGENT_VELBUS, false);
    this.velbusHost = getString(container.getConfig(), SETUP_CREATE_AGENT_VELBUS_HOST, "localhost");
    this.velbusPort = getInteger(container.getConfig(), SETUP_CREATE_AGENT_VELBUS_PORT, 6000);
  }

  @Override
  public void onStart() throws Exception {

    KeycloakTestSetup keycloakTestSetup = setupService.getTaskOfType(KeycloakTestSetup.class);
    Realm realmMaster = keycloakTestSetup.realmMaster;
    realmMasterName = realmMaster.getName();

    if (knx) {
      LOG.info("Enable KNX demo agent, gateway/local IP: " + knxGatewayIp + "/" + knxLocalIp);

      KNXAgent agent =
          new KNXAgent("Demo KNX agent")
              .setRealm(realmMasterName)
              .setHost(knxGatewayIp)
              .setBindHost(knxLocalIp);

      agent = assetStorageService.merge(agent);
    }

    if (velbus) {
      LOG.info("Enable Velbus demo agent, COM port: " + velbusPort);

      VelbusTCPAgent agent =
          new VelbusTCPAgent("Demo VELBUS agent")
              .setRealm(realmMasterName)
              .setHost(velbusHost)
              .setPort(velbusPort);

      agent = assetStorageService.merge(agent);
    }
  }
}
