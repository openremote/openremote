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
package org.openremote.test.protocol.http;

import org.openremote.agent.protocol.http.AbstractHttpServerProtocol;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.value.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestHttpServerProtocol extends AbstractHttpServerProtocol {

    public static final String PROTOCOL_NAME = AbstractHttpServerProtocol.PROTOCOL_NAME + ":test";
    public TestResourceImpl resource1 = new TestResourceImpl();

    @Override
    protected Set<Object> getApiSingletons(AssetAttribute protocolConfiguration) {
        return new HashSet<>(Collections.singletonList(resource1));
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return Collections.emptyList();
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return Collections.emptyList();
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return "HTTP Server Test Protocol";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
