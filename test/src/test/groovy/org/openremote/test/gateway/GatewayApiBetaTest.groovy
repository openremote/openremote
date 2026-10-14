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
package org.openremote.test.gateway

import org.openremote.model.asset.ReadAssetsEvent
import org.openremote.model.gateway.GatewayCapabilitiesRequestEvent
import org.openremote.model.gateway.GatewayInitStartEvent
import org.openremote.model.util.ValueUtil
import org.openremote.model.event.shared.SharedEvent

import java.util.concurrent.CopyOnWriteArrayList

class GatewayApiBetaTest extends AbstractGatewayCompatibilityTest {

  // BETA: c706a7fb78542967c68eb97dcf6ef06673475ff7 (be467d6^); no gateway API version field.
  private static final String CAPABILITIES_JSON =
  '{"eventType":"gateway-capabilities-response","timestamp":0,"tunnelingSupported":true}'

  def "Gateway API BETA legacy initialization compatibility"() {
    given: "a provisioned gateway using the frozen pre-versioned wire format"
    def gateway = provisionCompatibilityGateway("BETA")
    List<SharedEvent> receivedEvents = new CopyOnWriteArrayList<>()
    def client = createCompatibilityClient(gateway, receivedEvents)

    when: "the BETA gateway connects"
    client.connect()

    then: "it ignores the unknown init start event and waits for the delayed legacy asset request"
    awaitGatewayEvent(receivedEvents, GatewayInitStartEvent)
    def initialRequest = awaitGatewayEvent(receivedEvents, ReadAssetsEvent, "INITIAL")
    initialRequest.assetQuery != null
    !receivedEvents.any { it instanceof GatewayCapabilitiesRequestEvent }

    when: "assets are synchronized"
    synchronizeAssets(client, receivedEvents)

    then: "the manager requests capabilities after synchronization"
    awaitGatewayEvent(receivedEvents, GatewayCapabilitiesRequestEvent)
    !ValueUtil.JSON.readTree(CAPABILITIES_JSON).has("version")

    when: "the gateway reports tunneling support without an API version"
    client.sendMessage(SharedEvent.MESSAGE_PREFIX + CAPABILITIES_JSON)

    then: "initialization completes with legacy tunneling support"
    assertCompatibilityConnected(gateway, receivedEvents, null, false)

    cleanup:
    cleanupCompatibilityGateway(client, gateway)
  }
}
