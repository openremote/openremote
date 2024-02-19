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
package org.openremote.agent.protocol.homeassistant;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.homeassistant.assets.HomeAssistantBaseAsset;
import org.openremote.agent.protocol.homeassistant.commands.EntityStateCommandFactory;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.protocol.ProtocolAssetDiscovery;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * A custom protocol that is used by the {@link HomeAssistantAgent}; there is a one-to-one mapping between an {@link
 * HomeAssistantAgent} {@link Asset} and its' {@link org.openremote.model.asset.agent.Protocol}.
 * This example does nothing useful but is intended to show where protocol classes should be created.
 */
public class HomeAssistantProtocol extends AbstractProtocol<HomeAssistantAgent, HomeAssistantAgentLink> implements ProtocolAssetDiscovery {

    public static final String PROTOCOL_DISPLAY_NAME = "HomeAssistant Client";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, HomeAssistantProtocol.class);
    public HomeAssistantEntityProcessor entityProcessor;
    protected HomeAssistantHttpClient client;
    protected HomeAssistantWebSocketClient webSocketClient;
    protected volatile boolean running;

    public HomeAssistantProtocol(HomeAssistantAgent agent) {
        super(agent);
    }

    @Override
    protected void doStart(Container container) {
        running = true;

        String url = agent.getHomeAssistantUrl().orElseThrow(() -> {
            String msg = "HomeAssistant URL is not defined so cannot start protocol: " + this;
            LOG.warning(msg);
            return new IllegalArgumentException(msg);
        });

        String accessToken = agent.getAccessToken().orElseThrow(() -> {
            String msg = "Access token is not defined so cannot start protocol: " + this;
            LOG.warning(msg);
            return new IllegalArgumentException(msg);
        });

        client = new HomeAssistantHttpClient(url, accessToken);
        if (client.isConnectionSuccessful()) {
            setConnectionStatus(ConnectionStatus.CONNECTED);
            LOG.info("Connection to HomeAssistant API successful");


            executorService = container.getExecutorService();
            webSocketClient = createWebSocketClient();
            entityProcessor = new HomeAssistantEntityProcessor(this, assetService);

            importAssets();
        } else {
            LOG.warning("Connection to HomeAssistant failed");
            setConnectionStatus(ConnectionStatus.DISCONNECTED);
        }

    }

    // Imports all entities from Home Assistant and merges them into the agents asset store
    // Uses the assetDiscoveryConsumer to pass the assets back to the agent
    private void importAssets() {
        var assetConsumer = new Consumer<AssetTreeNode[]>() {
            @Override
            public void accept(AssetTreeNode[] assetTreeNodes) {
                for (var assetTreeNode : assetTreeNodes) {
                    assetService.mergeAsset(assetTreeNode.getAsset());
                }
            }
        };

        startAssetDiscovery(assetConsumer);
    }


    @Override
    protected void doStop(Container container) {
        running = false;
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, HomeAssistantAgentLink agentLink) throws RuntimeException {
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, HomeAssistantAgentLink agentLink) {
    }

    @Override
    protected void doLinkedAttributeWrite(HomeAssistantAgentLink agentLink, AttributeEvent event, Object processedValue) {
        var asset = assetService.findAsset(event.getId());
        if (asset == null) {
            return; // asset not found
        }
        var command = EntityStateCommandFactory.createEntityStateCommand(asset, event.getRef(), processedValue.toString());
        if (command.isEmpty())
            return;

        client.setEntityState(agentLink.domainId, command.get());
        updateLinkedAttribute(event.getRef(), event.getValue());
    }


    // Called when an attribute is written to due to external changes made by Home Assistant
    public void handleExternalAttributeChange(AttributeEvent event) {
        updateLinkedAttribute(event.getRef(), event.getValue());
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return agent.getHomeAssistantUrl().orElse("");
    }


    @Override
    public Future<Void> startAssetDiscovery(Consumer<AssetTreeNode[]> assetConsumer) {
        var entities = client.getEntities();
        if (entities.isPresent()) {
            var assets = entityProcessor.convertEntitiesToAssets(entities.get());
            if (assets.isPresent()) {
                for (var asset : assets.get()) {
                    String entityType = HomeAssistantEntityProcessor.getEntityTypeFromEntityId(asset.getEntityId());
                    var parent = getOrCreateParentAsset(assetConsumer, entityType, asset);
                    if (parent.isEmpty())
                        continue;

                    asset.setParent(parent.get());
                    asset.setRealm(agent.getRealm());
                    assetService.mergeAsset(asset);
                }
            }
        }
        return null;
    }

    public Optional<Asset<?>> getOrCreateParentAsset(Consumer<AssetTreeNode[]> consumer, String entityType, HomeAssistantBaseAsset asset) {
        //find the parent asset based on the entity type (group for entity type attribute) and the parent of the parent has to be the agentId
        HomeAssistantBaseAsset parentAsset = (HomeAssistantBaseAsset) assetService.findAssets(new AssetQuery().attributeName("GroupForEntityType")).stream()
                .filter(a -> a.getAttributes().get("GroupForEntityType").orElseThrow().getValue().flatMap(v -> v.equals(entityType) ? Optional.of(v) : Optional.empty()).isPresent())
                .findFirst().orElse(null);


        if (parentAsset == null) {
            parentAsset = entityProcessor.initiateAssetClass(Map.of("friendly_name", entityType), entityType, UniqueIdentifierGenerator.generateId());
            parentAsset.setParentId(agent.getId());
            parentAsset.setRealm(agent.getRealm());
            parentAsset.setAgentId(agent.getId());
            parentAsset.setGroupForEntityType(entityType);
            parentAsset.setId(UniqueIdentifierGenerator.generateId());

            AssetTreeNode node = new AssetTreeNode(parentAsset);
            consumer.accept(new AssetTreeNode[]{node});
        }
        return Optional.of(parentAsset);
    }


    private HomeAssistantWebSocketClient createWebSocketClient() {
        if (getAgent().getHomeAssistantUrl().isEmpty()) {
            throw new RuntimeException("Home Assistant URL is not defined so cannot create websocket client for protocol: " + this);
        }
        var wsUrlString = (getAgent().getHomeAssistantUrl().get() + "/api/websocket").replace("http", "ws");
        var wsUrl = URI.create(wsUrlString);

        return new HomeAssistantWebSocketClient(this, wsUrl);
    }
}
