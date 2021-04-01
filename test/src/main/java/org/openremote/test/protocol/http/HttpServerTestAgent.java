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
package org.openremote.test.protocol.http;

import org.openremote.agent.protocol.http.AbstractHttpServerAgent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;

import javax.persistence.Entity;

@Entity
public class HttpServerTestAgent extends AbstractHttpServerAgent<HttpServerTestAgent, TestHttpServerProtocol, AgentLink.Default> {

    public static final AgentDescriptor<HttpServerTestAgent, TestHttpServerProtocol, AgentLink.Default> DESCRIPTOR = new AgentDescriptor<>(
        HttpServerTestAgent.class, TestHttpServerProtocol.class, AgentLink.Default.class
    );

    protected HttpServerTestAgent() {}

    public HttpServerTestAgent(String name) {
        super(name);
    }

    @Override
    public TestHttpServerProtocol getProtocolInstance() {
        return new TestHttpServerProtocol(this);
    }
}
