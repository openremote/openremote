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
package org.openremote.agent.protocol.artnet;

import org.openremote.agent.protocol.io.IOAgent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.util.ModelIgnore;

import javax.persistence.Entity;


@ModelIgnore
@Entity
public class ArtnetAgent extends IOAgent<ArtnetAgent, ArtnetProtocol, DefaultAgentLink> {

    public static final AgentDescriptor<ArtnetAgent, ArtnetProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        ArtnetAgent.class, ArtnetProtocol.class, DefaultAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ArtnetAgent() {
    }

    public ArtnetAgent(String name) {
        super(name);
    }

    @Override
    public ArtnetProtocol getProtocolInstance() {
        return new ArtnetProtocol(this);
    }
}
