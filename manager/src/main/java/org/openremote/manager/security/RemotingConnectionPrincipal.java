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
package org.openremote.manager.security;

import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * A {@link Principal} for associating an ActiveMQ {@link RemotingConnection} with the {@link javax.security.auth.Subject}
 */
// TODO: Remove this if/when ActiveMQ implements https://issues.apache.org/jira/browse/ARTEMIS-4059
public class RemotingConnectionPrincipal implements Principal {

    protected RemotingConnection remotingConnection;

    public RemotingConnectionPrincipal(RemotingConnection remotingConnection) {
        this.remotingConnection = remotingConnection;
    }

    @Override
    public String getName() {
        return null;
    }

    public RemotingConnection getRemotingConnection() {
        return remotingConnection;
    }

    public static RemotingConnection getRemotingConnectionFromSubject(Subject subject) {
        return subject != null ? subject.getPrincipals()
            .stream()
            .filter(p -> p instanceof RemotingConnectionPrincipal)
            .findFirst()
            .map(p -> ((RemotingConnectionPrincipal)p).getRemotingConnection())
            .orElse(null) : null;
    }
}
