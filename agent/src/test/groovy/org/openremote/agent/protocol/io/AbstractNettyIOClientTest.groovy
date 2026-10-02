/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.agent.protocol.io

import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.channel.embedded.EmbeddedChannel
import org.openremote.model.asset.agent.ConnectionStatus
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class AbstractNettyIOClientTest extends Specification {

  /** Enough of a client to own a channel; nothing here connects to anything. */
  private static class StubIOClient extends AbstractNettyIOClient<String> {

    @Override
    protected Class<? extends Channel> getChannelClass() {
      return EmbeddedChannel
    }

    @Override
    protected EventLoopGroup getWorkerGroup() {
      return null
    }

    @Override
    protected CompletableFuture<Void> startChannel() {
      return new CompletableFuture<Void>()
    }

    @Override
    String getClientUri() {
      return "stub://test"
    }
  }

  def "a peer that closes the channel before the attempt reports success fails that attempt"() {

    given: "a client part way through a connection attempt"
    def client = new StubIOClient()
    client.connectionStatus = ConnectionStatus.CONNECTING
    def connectAttempt = new CompletableFuture<Void>()
    client.connectFuture = connectAttempt

    and: "a channel carrying the client's close handling"
    def channel = new EmbeddedChannel()
    client.initChannel(channel)

    when: "the peer closes it before the attempt has reported success"
    channel.close().sync()

    then: "the attempt fails, so the retry policy reconnects instead of the close being dropped"
    // A close seen while the status is still CONNECTING used to fall through silently, and the
    // attempt then reported itself connected on a channel that had already gone. Nothing closes a
    // second time, so the client stayed CONNECTED forever and never reconnected.
    connectAttempt.isCompletedExceptionally()

    and: "the client is not left claiming to be connected"
    client.connectionStatus != ConnectionStatus.CONNECTED
  }

  def "a close after the client has disconnected does not fail anything"() {

    given: "a client that is disconnecting"
    def client = new StubIOClient()
    client.connectionStatus = ConnectionStatus.DISCONNECTING
    def connectAttempt = new CompletableFuture<Void>()
    client.connectFuture = connectAttempt

    and: "a channel carrying the client's close handling"
    def channel = new EmbeddedChannel()
    client.initChannel(channel)

    when: "the channel closes as part of that disconnect"
    channel.close().sync()

    then: "the close is left alone, since a deliberate disconnect must not trigger a reconnect"
    !connectAttempt.isDone()
  }
}
