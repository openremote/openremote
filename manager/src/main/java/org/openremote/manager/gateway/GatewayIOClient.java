/*
 * Copyright 2024, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.gateway;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.openremote.agent.protocol.websocket.WebsocketIOClient;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.gateway.GatewayCapabilitiesRequestEvent;
import org.openremote.model.gateway.GatewayDisconnectEvent;
import org.openremote.model.syslog.SyslogCategory;

/**
 * This is a special version of {@link WebsocketIOClient} that waits for a {@link
 * org.openremote.model.gateway.GatewayCapabilitiesRequestEvent} from the central manager before
 * completing the connected future; this means that synchronisation failures will be handled with
 * exponential backoff. If no event is received within {@link #TIMEOUT_MILLIS} then it is assumed
 * the connection is ready as we could be talking to an old central manager that doesn't emit the
 * event.
 */
public class GatewayIOClient extends WebsocketIOClient<String> {

  private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, GatewayIOClient.class);
  protected static final int TIMEOUT_MILLIS = 30000;
  protected CompletableFuture<Void> syncFuture;

  public GatewayIOClient(URI uri, Map<String, List<String>> headers, OAuthGrant oAuthGrant) {
    super(uri, headers, oAuthGrant);
  }

  @Override
  protected Future<Void> startChannel() {
    CompletableFuture<Void> connectedFuture;
    try {
      connectedFuture = toCompletableFuture(super.startChannel());
    } catch (Exception e) {
      connectedFuture = CompletableFuture.failedFuture(e);
    }

    return connectedFuture
        .orTimeout(getConnectTimeoutMillis() + 1000L, TimeUnit.MILLISECONDS)
        .thenCompose(
            __ ->
                getFuture()
                    .orTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .handle(
                        (result, ex) -> {
                          syncFuture = null;
                          if (ex instanceof TimeoutException && !channel.isOpen()) {
                            // Channel could have been closed whilst waiting for sync
                            LOG.info("Channel has been closed unexpectedly during sync");
                            throw new RuntimeException(
                                "Channel has been closed unexpectedly during sync");
                          } else if (ex instanceof TimeoutException) {
                            LOG.finest("Timeout reached whilst waiting for sync complete event");
                          } else if (ex != null) {
                            throw new RuntimeException(ex.getMessage());
                          }
                          return null;
                        }));
  }

  @Override
  protected Void waitForConnectFuture(Future<Void> connectFuture) throws Exception {
    // Might need a better solution than this as we don't know how long the sync will take
    return connectFuture.get(getConnectTimeoutMillis() + 60000L, TimeUnit.MILLISECONDS);
  }

  protected CompletableFuture<Void> getFuture() {
    syncFuture = new CompletableFuture<>();
    return syncFuture;
  }

  @Override
  protected void onMessageReceived(String message) {
    if (syncFuture != null && message.contains(GatewayCapabilitiesRequestEvent.TYPE)) {
      LOG.finest("Gateway connection is now ready");
      syncFuture.complete(null);
    }

    if (syncFuture != null && message.contains(GatewayDisconnectEvent.TYPE)) {
      LOG.finest("Gateway disconnect event received during sync: " + message);
      syncFuture.completeExceptionally(
          new RuntimeException("Gateway disconnect event received during sync"));
    }
    super.onMessageReceived(message);
  }
}
