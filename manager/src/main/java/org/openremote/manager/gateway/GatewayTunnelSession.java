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
import java.util.function.Supplier;

/**
 * Tracks a gateway tunnel session
 */
public class GatewayTunnelSession {
   protected CompletableFuture<Void> connectFuture;
   protected GatewayTunnelInfo tunnelInfo;
   protected Supplier<CompletableFuture<Void>> disconnectFutureSupplier;

   public GatewayTunnelSession(CompletableFuture<Void> connectFuture, GatewayTunnelInfo tunnelInfo, Supplier<CompletableFuture<Void>> disconnectFutureSupplier) {
      this.connectFuture = connectFuture;
      this.tunnelInfo = tunnelInfo;
      this.disconnectFutureSupplier = disconnectFutureSupplier;
   }

   CompletableFuture<Void> getConnectFuture() {
      return connectFuture;
   }

   CompletableFuture<Void> disconnect() {
      return disconnectFutureSupplier.get();
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
