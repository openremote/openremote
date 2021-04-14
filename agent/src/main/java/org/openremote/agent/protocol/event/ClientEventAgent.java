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

    public static final ValueDescriptor<ClientRole> VALUE_CLIENT_ROLE = new ValueDescriptor<>("clientRole", ClientRole.class);

    public static final AttributeDescriptor<String> CLIENT_SECRET = new AttributeDescriptor<>("clientSecret", ValueType.TEXT);
    public static final AttributeDescriptor<Boolean> WRITE = new AttributeDescriptor<>("write", ValueType.BOOLEAN);
    public static final AttributeDescriptor<Boolean> READ = new AttributeDescriptor<>("read", ValueType.BOOLEAN);

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

    public ClientEventAgent setClientSecret(String value) {
        getAttributes().getOrCreate(CLIENT_SECRET).setValue(value);
        return this;
    }

    public Optional<Boolean> isRead() {
        return getAttributes().getValue(READ);
    }

    public ClientEventAgent setRead(Boolean value) {
        getAttributes().getOrCreate(READ).setValue(value);
        return this;
    }

    public Optional<Boolean> isWrite() {
        return getAttributes().getValue(WRITE);
    }

    public ClientEventAgent setWrite(Boolean value) {
        getAttributes().getOrCreate(WRITE).setValue(value);
        return this;
    }
}
