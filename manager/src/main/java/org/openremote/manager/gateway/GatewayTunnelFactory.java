/*
 * Copyright 2024, OpenRemote Inc.
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

package org.openremote.manager.gateway;

import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.gateway.GatewayTunnelStartRequestEvent;

import java.util.function.Consumer;

/**
 * This interface is an abstraction for creating {@link GatewayTunnelSession}s and is used by edge gateway instances.
 */
public interface GatewayTunnelFactory {

   /**
    * Initialize the tunnel factory
    */
   void start();

   /**
    * Destroy the tunnel factory
    */
   void stop();

   /**
    * Create and start a tunnel session for the requested {@link GatewayTunnelInfo}; the closedCallback will be invoked
    * when the session is closed, if exceptionally the throwable will be passed to the callback.
    */
   GatewayTunnelSession createSession(GatewayTunnelStartRequestEvent startRequestEvent, Consumer<Throwable> closedCallback);
}
