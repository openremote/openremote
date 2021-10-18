/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.mqtt;

import org.openremote.agent.protocol.io.AbstractIOClientProtocol;
import org.openremote.agent.protocol.io.IOAgent;
import org.openremote.model.asset.agent.AgentLink;

public abstract class AbstractMQTTClientProtocol<T extends AbstractMQTTClientProtocol<T, U, V, W, X>, U extends IOAgent<U, T, X>, V, W extends AbstractMQTT_IOClient<V>, X extends AgentLink<?>> extends AbstractIOClientProtocol<T,U,MQTTMessage<V>,W,X> {

    protected AbstractMQTTClientProtocol(U agent) {
        super(agent);
    }
}
