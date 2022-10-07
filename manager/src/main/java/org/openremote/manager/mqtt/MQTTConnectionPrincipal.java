/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.mqtt;

import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;

import java.security.Principal;
import java.util.Objects;

public class MQTTConnectionPrincipal implements Principal {

    protected RemotingConnection connection;

    public MQTTConnectionPrincipal(RemotingConnection connection) {
        this.connection = connection;
    }

    @Override
    public String getName() {
        return connection.getProtocolName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MQTTConnectionPrincipal that = (MQTTConnectionPrincipal) o;
        return connection.equals(that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection);
    }

    public RemotingConnection getConnection() {
        return connection;
    }
}
