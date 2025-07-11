/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.websocket;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.openremote.model.asset.agent.AgentLink;

public class WebsocketAgentLink extends AgentLink<WebsocketAgentLink> {

  protected WebsocketSubscription[] websocketSubscriptions;

  // For Hydrators
  protected WebsocketAgentLink() {}

  public WebsocketAgentLink(String id) {
    super(id);
  }

  @JsonPropertyDescription(
      "Array of WebsocketSubscriptions that should be executed when the linked attribute is linked; the subscriptions are executed in the order specified in the array.")
  public Optional<WebsocketSubscription[]> getWebsocketSubscriptions() {
    return Optional.ofNullable(websocketSubscriptions);
  }

  public WebsocketAgentLink setWebsocketSubscriptions(
      WebsocketSubscription[] websocketSubscriptions) {
    this.websocketSubscriptions = websocketSubscriptions;
    return this;
  }
}
