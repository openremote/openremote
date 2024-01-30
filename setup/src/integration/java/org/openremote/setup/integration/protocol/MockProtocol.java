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
package org.openremote.setup.integration.protocol;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.protocol.ProtocolAssetDiscovery;
import org.openremote.model.value.ValueType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * A mock protocol for testing purposes that records the various method calls and supports protocol discovery
 */
public class MockProtocol extends AbstractProtocol<MockAgent, MockAgentLink> implements ProtocolAssetDiscovery {

    public static final String PROTOCOl_NAME = "Mock protocol";
    public List<AttributeEvent> protocolWriteAttributeEvents = new ArrayList<>();
    public List<String> protocolMethodCalls = new ArrayList<>();
    public boolean updateSensor = true;
    protected Container container;

    public MockProtocol(MockAgent agent) {
        super(agent);
    }

    @Override
    protected void doStart(Container container) throws Exception {
        this.container = container;
        protocolMethodCalls.add("START");

        if (!agent.getRequired().isPresent()) {
            throw new IllegalStateException("Agent required attribute is not defined");
        }

        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        protocolMethodCalls.add("STOP");
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, MockAgentLink agentLink) throws RuntimeException {
        protocolMethodCalls.add("LINK_ATTRIBUTE:" + assetId + ":" + attribute.getName());

        if (!agentLink.getRequiredValue().isPresent()) {
            // This tests exception handling during linking of attributes
            throw new IllegalStateException("Attribute is not valid");
        }
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, MockAgentLink agentLink) {
        protocolMethodCalls.add("UNLINK_ATTRIBUTE:" + assetId + ":" + attribute.getName());
    }

    @Override
    protected void doLinkedAttributeWrite(MockAgentLink agentLink, AttributeEvent event, Object processedValue) {
        protocolMethodCalls.add("WRITE_ATTRIBUTE:" + event.getId() + ":" + event.getName());
        protocolWriteAttributeEvents.add(event);
        if (updateSensor) {
            updateReceived(event.getState());
        }
    }

    @Override
    public String getProtocolName() {
        return PROTOCOl_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "mock://" + getAgent().getId();
    }

    public void updateReceived(AttributeState state) {
        // Assume we've pushed the update to the actual device and it responded with OK
        // so now we want to cause a sensor update that will go through the processing
        // chain.
        updateLinkedAttribute(state);
    }

    protected void updateAttribute(AttributeState state) {
        sendAttributeEvent(state);
    }

    @Override
    public Future<Void> startAssetDiscovery(Consumer<AssetTreeNode[]> assetConsumer) {
        return container.getExecutorService().submit(() -> {

            // Simulate discovery init delay
            Thread.sleep(2000);

            // Discover a few assets
            assetConsumer.accept(new AssetTreeNode[] {
                new AssetTreeNode(
                    new ThingAsset("MockAsset1").addAttributes(
                        new Attribute<>("mock1", ValueType.TEXT, "dummy1"),
                        new Attribute<>("mock2", ValueType.POSITIVE_INTEGER, 1234)
                    )
                ),
                new AssetTreeNode(
                    new ThingAsset("MockAsset2").addAttributes(
                        new Attribute<>("mock1", ValueType.TEXT, "dummy2"),
                        new Attribute<>("mock2", ValueType.POSITIVE_INTEGER, 1234)
                    )
                )
            });

            // Simulate a delay
            Thread.sleep(1000);

            // Discover a few assets
            assetConsumer.accept(new AssetTreeNode[] {
                new AssetTreeNode(
                    new ThingAsset("MockAsset3").addAttributes(
                        new Attribute<>("mock1", ValueType.TEXT, "dummy3"),
                        new Attribute<>("mock2", ValueType.POSITIVE_INTEGER, 1234)
                    )
                ),
                new AssetTreeNode(
                    new ThingAsset("MockAsset3").addAttributes(
                        new Attribute<>("mock1", ValueType.TEXT, "dummy3"),
                        new Attribute<>("mock2", ValueType.POSITIVE_INTEGER, 1234)
                    )
                )
            });

           return null;
        });
    }
}
