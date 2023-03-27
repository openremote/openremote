/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.agent.protocol.mail;

import org.openremote.model.asset.agent.AgentDescriptor;

import jakarta.persistence.Entity;

@Entity
public class MailAgent extends AbstractMailAgent<MailAgent, MailProtocol, MailAgentLink>  {

    public static final AgentDescriptor<MailAgent, MailProtocol, MailAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        MailAgent.class, MailProtocol.class, MailAgentLink.class
    );

    public MailAgent() {
    }

    public MailAgent(String name) {
        super(name);
    }

    @Override
    public MailProtocol getProtocolInstance() {
        return new MailProtocol(this);
    }
}
