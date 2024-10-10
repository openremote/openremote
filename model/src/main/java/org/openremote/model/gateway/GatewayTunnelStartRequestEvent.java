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
package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStartRequestEvent extends SharedEvent {

    protected String sshHostname;
    protected int sshPort;
    protected GatewayTunnelInfo info;

    @JsonCreator
    public GatewayTunnelStartRequestEvent(String sshHostname, int sshPort, GatewayTunnelInfo info) {
        this.sshHostname = sshHostname;
        this.sshPort = sshPort;
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
    }

    public String getSshHostname() {
        return sshHostname;
    }

    public int getSshPort() {
        return sshPort;
    }

    @Override
    public String toString() {
        return GatewayTunnelStartRequestEvent.class.getSimpleName() + "{" +
                "sshHostname='" + sshHostname + '\'' +
                ", sshPort=" + sshPort +
                ", info=" + info +
                '}';
    }
}
