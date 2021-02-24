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
package org.openremote.agent.protocol.event;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.security.ClientRole;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class ClientEventAgent extends Agent<ClientEventAgent, ClientEventProtocol, AgentLink.Default> {

    public static final ValueDescriptor<ClientRole> VALUE_CLIENT_ROLE = new ValueDescriptor<>("Client role", ClientRole.class);

    public static final AttributeDescriptor<String> CLIENT_SECRET = new AttributeDescriptor<>("clientSecret", ValueType.TEXT);
    public static final AttributeDescriptor<ClientRole[]> CLIENT_ROLES = new AttributeDescriptor<>("clientRoles", VALUE_CLIENT_ROLE.asArray());

    public static final AgentDescriptor<ClientEventAgent, ClientEventProtocol, AgentLink.Default> DESCRIPTOR =new AgentDescriptor<>(
        ClientEventAgent.class, ClientEventProtocol.class, AgentLink.Default.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ClientEventAgent() {
    }


    public ClientEventAgent(String name) {
        super(name);
    }

    @Override
    public ClientEventProtocol getProtocolInstance() {
        return new ClientEventProtocol(this);
    }

    public Optional<String> getClientSecret() {
        return getAttributes().getValue(CLIENT_SECRET);
    }

    public Optional<ClientRole[]> getClientRoles() {
        return getAttributes().getValue(CLIENT_ROLES);
    }
}
