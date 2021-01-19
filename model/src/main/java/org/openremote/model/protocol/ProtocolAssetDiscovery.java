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

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.util.TsIgnore;

import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * To be used by protocols that support linked device discovery; these devices can be represented as {@link Asset}s
 * with {@link Attribute}s that contain the necessary {@link org.openremote.model.attribute.MetaItem}s
 * to establish a link to the supplied {@link Agent}. These {@link Asset}s can then be structured in
 * a hierarchical representation using {@link AssetTreeNode}s.
 * <p>
 * {@link Protocol}s that support {@link Asset} discovery must implement this interface.
 * <p>
 * It is the caller's responsibility to ensure that only a single discovery request occurs at a given time.
 */
@FunctionalInterface
@TsIgnore
public interface ProtocolAssetDiscovery {

    /**
     * Start the process asynchronously; the implementation can make as many calls as it desires to the
     * assetConsumer with the found assets; when the implementation has finished then the returned future will become
     * {@link Future#isDone()}. The callee can also cancel the future at any time. If for some reason the process
     * cannot be started or encounters an error then this method should log more details before completing the future.
     */
    Future<Void> startAssetDiscovery(Consumer<AssetTreeNode[]> assetConsumer);
}
