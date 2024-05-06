/*
 * Copyright 2024, OpenRemote Inc.
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

package org.openremote.manager.gateway;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.gateway.GatewayTunnelStartRequestEvent;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JSchGatewayTunnelFactory implements GatewayTunnelFactory {

    protected File sshKeyFile;
    protected JSch jSch;
    protected final Map<GatewayTunnelInfo, Session> sessionMap = new ConcurrentHashMap<>(2);

    public JSchGatewayTunnelFactory(File sshKeyFile) {
        this.sshKeyFile = sshKeyFile;
    }

    @Override
    public void startTunnel(GatewayTunnelStartRequestEvent startRequestEvent) throws Exception {
        if (sessionMap.containsKey(startRequestEvent.getInfo())) {
            throw new IllegalStateException("Tunnel already exists");
        }

        synchronized (this) {
            if (jSch == null) {
                jSch = new JSch();
                jSch.addIdentity(sshKeyFile.getAbsolutePath());
            }
        }

        GatewayTunnelInfo tunnelInfo = startRequestEvent.getInfo();
        Session session = jSch.getSession(null, startRequestEvent.getSshHostname(), startRequestEvent.getSshPort());
        session.setConfig("StrictHostKeyChecking", "no");
        String bindAddress = tunnelInfo.getType() ==  GatewayTunnelInfo.Type.TCP ? null : tunnelInfo.getId();
        int rPort = tunnelInfo.getType() == GatewayTunnelInfo.Type.HTTPS ? 443 : tunnelInfo.getType() == GatewayTunnelInfo.Type.HTTP ? 80 : startRequestEvent.getTcpPort();
        // Need to rewrite localhost to get outside of docker container
        String target = "localhost".equals(tunnelInfo.getTarget()) ? "172.17.0.1" : tunnelInfo.getTarget();
        session.setPortForwardingR(bindAddress, rPort, target, tunnelInfo.getTargetPort());
        session.connect();
        sessionMap.put(tunnelInfo, session);
    }

    @Override
    public void stopTunnel(GatewayTunnelInfo tunnelInfo) throws Exception {
        Session session = sessionMap.remove(tunnelInfo);

        if (session == null) {
            throw new IllegalStateException("No matching existing session found: " + tunnelInfo);
        }

        session.disconnect();
    }
}
