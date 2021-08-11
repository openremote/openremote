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
package org.openremote.agent.protocol.controller;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.util.ModelIgnore;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@ModelIgnore
@Entity
public class ControllerAgent extends Agent<ControllerAgent, ControllerProtocol, ControllerAgentLink> {

    public static final AttributeDescriptor<String> CONTROLLER_URI = new AttributeDescriptor<>("controllerURI", ValueType.TEXT);

    public static final AgentDescriptor<ControllerAgent, ControllerProtocol, ControllerAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        ControllerAgent.class, ControllerProtocol.class, ControllerAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ControllerAgent() {
    }

    public ControllerAgent(String name) {
        super(name);
    }

    @Override
    public ControllerProtocol getProtocolInstance() {
        return new ControllerProtocol(this);
    }

    public Optional<String> getControllerURI() {
        return getAttributes().getValue(CONTROLLER_URI);
    }

    public ControllerAgent setControllerURI(String uri) {
        getAttributes().getOrCreate(CONTROLLER_URI).setValue(uri);
        return this;
    }
}
