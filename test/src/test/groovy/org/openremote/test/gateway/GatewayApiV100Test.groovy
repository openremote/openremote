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

import org.openremote.model.event.shared.SharedEvent

import java.util.concurrent.CopyOnWriteArrayList

class GatewayApiV100Test extends AbstractGatewayCompatibilityTest {

  // 1.0.0: be467d6dfaa3875c5d48b65146cecb5bc5b8e30b introduced versioned capabilities.
  private static final String CAPABILITIES_JSON =
  '{"eventType":"gateway-capabilities-response","version":"1.0.0","tunnelingSupported":true,"timestamp":0}'

  def "Gateway API 1.0.0 initialization compatibility"() {
    given: "a provisioned gateway using the frozen 1.0.0 wire format"
    def gateway = provisionCompatibilityGateway("1.0.0")
    List<SharedEvent> receivedEvents = new CopyOnWriteArrayList<>()
    def client = createCompatibilityClient(gateway, receivedEvents)

    when: "the gateway completes versioned initialization"
    initializeVersionedGateway(client, receivedEvents, CAPABILITIES_JSON)

    then: "the manager detects 1.0.0 and connects without tunnel timeout management"
    assertCompatibilityConnected(gateway, receivedEvents, "1.0.0", false)

    cleanup:
    cleanupCompatibilityGateway(client, gateway)
  }
}
