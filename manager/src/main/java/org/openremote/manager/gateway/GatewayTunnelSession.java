/*
 *
 *  * Copyright 2026, OpenRemote Inc.
 *  *
 *  * See the CONTRIBUTORS.txt file in the distribution for a
 *  * full listing of individual contributors.
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as
 *  * published by the Free Software Foundation, either version 3 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openremote.manager.gateway;

import org.openremote.model.gateway.GatewayTunnelInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Tracks a gateway tunnel session; the {@link #getConnectFuture} should return a future that tracks the initial
 * connection of the tunnel; the {@link GatewayTunnelFactory} is responsible for then reconnecting the tunnel session
 * on failure. If the initial connection fails then the {@link GatewayTunnelFactory} should do nothing other than report
 * the failure via the {@link #getConnectFuture()} exception.
 * <p>
 * The {@link #disconnect} should disconnect the tunnel session and/or cancel any retry logic; if the initial connection
 * failed then this method should do nothing.
 */
public class GatewayTunnelSession {
   protected CompletableFuture<Void> connectFuture;
   protected GatewayTunnelInfo tunnelInfo;
   protected Runnable disconnectRunnable;

   public GatewayTunnelSession(CompletableFuture<Void> connectFuture,
                               GatewayTunnelInfo tunnelInfo,
                               Runnable disconnectRunnable) {
      this.connectFuture = connectFuture;
      this.tunnelInfo = tunnelInfo;
      this.disconnectRunnable = disconnectRunnable;
   }

   public CompletableFuture<Void> getConnectFuture() {
      return connectFuture;
   }

   public void disconnect() {
      disconnectRunnable.run();
   }

   public GatewayTunnelInfo getTunnelInfo() {
      return tunnelInfo;
   }

   @Override
   public String toString() {
      return "GatewayTunnelSession{" +
         "connectFuture=" + connectFuture +
         ", tunnelInfo=" + tunnelInfo +
         '}';
   }
}
