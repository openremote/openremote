/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.asset.agent;

import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.TenantScopedEvent;

/**
 * Published by the server when a a protocol configuration of an
 * agent changes its {@link ConnectionStatus}.
 */
public class AgentStatusEvent extends TenantScopedEvent {


    protected AttributeRef protocolConfiguration;
    protected ConnectionStatus connectionStatus;

    protected AgentStatusEvent() {
    }

    public AgentStatusEvent(long timestamp, String realmId, AttributeRef protocolConfiguration, ConnectionStatus connectionStatus) {
        super(timestamp, realmId);
        this.protocolConfiguration = protocolConfiguration;
        this.connectionStatus = connectionStatus;
    }

    public AttributeRef getProtocolConfiguration() {
        return protocolConfiguration;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realmId=" + realmId +
            ", protocolConfiguration=" + protocolConfiguration +
            ", connectionStatus=" + connectionStatus +
            '}';
    }
}
