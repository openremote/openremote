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

import io.netty.channel.ChannelFuture;
import org.openremote.agent.protocol.websocket.WebsocketIOClient;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.event.shared.EventRequestResponseWrapper;
import org.openremote.model.gateway.GatewayCapabilitiesRequestEvent;
import org.openremote.model.syslog.SyslogCategory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a special version of {@link WebsocketIOClient} that waits for a
 * {@link org.openremote.model.gateway.GatewayCapabilitiesRequestEvent} from the central manager before completing the
 * connected future; this means that synchronisation failures will be handled with exponential backoff. If no event
 * is received within {@link #TIMEOUT_MILLIS} then it is assumed the connection is ready as we could be talking to
 * an old central manager that doesn't emit the event.
 */
public class GatewayIOClient extends WebsocketIOClient<String> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, GatewayIOClient.class);
    protected static final int TIMEOUT_MILLIS = 30000;
    protected CompletableFuture<Void> syncFuture;

    public GatewayIOClient(URI uri, Map<String, List<String>> headers, OAuthGrant oAuthGrant) {
        super(uri, headers, oAuthGrant);
    }

    @Override
    protected CompletableFuture<Void> createConnectedFuture(ChannelFuture channelStartFuture) {
        CompletableFuture<Void> connectedFuture = super.createConnectedFuture(channelStartFuture);
        return connectedFuture.thenCompose(__ ->
            getFuture()
                .orTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .handle((result, ex) -> {
                    syncFuture = null;
                    if (ex instanceof TimeoutException) {
                        LOG.finest("Timeout reached whilst waiting for sync complete event");
                    } else if (ex != null) {
                        throw new RuntimeException(ex.getMessage());
                    }
                    return null;
                })
        );
    }

    protected CompletableFuture<Void> getFuture() {
        syncFuture = new CompletableFuture<>();
        return syncFuture;
    }

    @Override
    protected void onMessageReceived(String message) {
        if (syncFuture != null && message.startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX) && message.contains(GatewayCapabilitiesRequestEvent.TYPE)) {
            LOG.finest("Gateway connection is now ready");
            syncFuture.complete(null);
        }

        super.onMessageReceived(message);
    }
}
