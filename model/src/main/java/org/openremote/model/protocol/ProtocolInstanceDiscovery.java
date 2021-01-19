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
import org.openremote.model.util.TsIgnore;

import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * To be used by protocols that support {@link Protocol} instance discovery; these instances can be represented as
 * {@link Agent} {@link Asset}s with {@link Attribute}s that contain the necessary configuration to establish a
 * connection to the protocol instance.
 * <p>
 * Implementations must have a no args constructor (i.e. a factory/provider) so that instances can be created when
 * discovery is requested for the associated {@link org.openremote.model.asset.agent.Protocol}. Implementations are
 * not re-used.
 */
@TsIgnore
@FunctionalInterface
public interface ProtocolInstanceDiscovery {

    /**
     * Start the process asynchronously; the implementation can make as many calls as it desires to the
     * agentConsumer with the found agents; when the implementation has finished then the returned future will become
     * {@link Future#isDone()}. The callee can also cancel the future at any time. If for some reason the process
     * cannot be started or encounters an error then this method should log more details before completing the future.
     */
    <T extends Agent<?, ?, ?>> Future<Void> startInstanceDiscovery(Consumer<T[]> agentConsumer);
}
