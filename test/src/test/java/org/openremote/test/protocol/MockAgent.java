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
package org.openremote.test.protocol;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class MockAgent extends Agent<MockAgent, MockProtocol, MockAgentLink> {

    public static final AttributeDescriptor<Boolean> REQUIRED = new AttributeDescriptor<>("requiredTest", ValueType.BOOLEAN);

    public static final AgentDescriptor<MockAgent, MockProtocol, MockAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        MockAgent.class, MockProtocol.class, MockAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected MockAgent() {
    }

    public MockAgent(String name) {
        super(name);
    }

    public Optional<Boolean> getRequired() {
        return getAttributes().getValue(REQUIRED);
    }

    public MockAgent setRequired(Boolean value) {
        getAttributes().getOrCreate(REQUIRED).setValue(value);
        return this;
    }

    @Override
    public MockProtocol getProtocolInstance() {
        return new MockProtocol(this);
    }
}
