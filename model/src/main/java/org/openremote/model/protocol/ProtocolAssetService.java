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
package org.openremote.model.protocol;

import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.query.AssetQuery;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for protocols to perform limited asset related operations within their own realm
 */
public interface ProtocolAssetService {

    /**
     * Protocols may store assets in the context or update existing assets. A unique identifier must be set by the
     * protocol implementor, and the parent must be the {@link Agent} associated with the requesting protocol instance.
     * This operation stores transient or detached state and returns the current state. It will override any existing
     * stored asset data, ignoring versions.
     */
    <T extends Asset<?>> T mergeAsset(T asset);

    /**
     * Protocols may remove assets from the context store.
     *
     * @return <code>false</code> if the delete could not be performed (asset may have children?)
     */
    boolean deleteAssets(String...assetIds);

    /**
     * Get asset from the store by ID.
     */
    <T extends Asset<?>> T findAsset(String assetId);

    /**
     * Get assets by an {@link org.openremote.model.query.AssetQuery}; can only access {@link Asset}s that are
     * within the same realm as the {@link Agent}.
     */
    List<Asset<?>> findAssets(AssetQuery assetQuery);

    /**
     * Send an {@link AttributeEvent} through the system
     */
    void sendAttributeEvent(AttributeEvent attributeEvent);

    /**
     * Subscribe to changes of {@link Asset}s that are descendants of the agent.
     * <p>
     * When an agent is unlinked from a protocol then all subscriptions will be automatically removed also; it is safe
     * to call this method multiple times for the same agentId and assetChangeConsumer and only a single subscription
     * would actually be created.
     */
    void subscribeChildAssetChange(Consumer<PersistenceEvent<Asset<?>>> assetChangeConsumer);

    /**
     * Unsubscribe to changes of {@link Asset}s that are descendants of the agent.
     * <p>
     * When an agent is unlinked from a protocol then all subscriptions will be automatically removed also.
     */
    void unsubscribeChildAssetChange(Consumer<PersistenceEvent<Asset<?>>> assetChangeConsumer);
}
