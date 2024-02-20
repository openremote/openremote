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
package org.openremote.setup.integration.protocol.http;

import org.openremote.agent.protocol.http.AbstractHTTPServerProtocol;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestHTTPServerProtocol extends AbstractHTTPServerProtocol<TestHTTPServerProtocol, HTTPServerTestAgent, DefaultAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "HTTP Server Test Protocol";
    public TestResourceImpl resource1 = new TestResourceImpl();

    public TestHTTPServerProtocol(HTTPServerTestAgent agent) {
        super(agent);
    }

    @Override
    protected Set<Object> getApiSingletons() {
        return new HashSet<>(Collections.singletonList(resource1));
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) throws RuntimeException {

    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {

    }

    @Override
    protected void doLinkedAttributeWrite(DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {

    }

    @Override
    public String getProtocolInstanceUri() {
        return "httpServerProtocol://test";
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }
}
