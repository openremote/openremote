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

import jakarta.ws.rs.WebApplicationException
import org.openremote.manager.setup.SetupService
import org.openremote.model.gateway.GatewayServiceResource
import org.openremote.model.gateway.GatewayTunnelInfo
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID

/**
 * Covers the role gate on the gateway tunnel endpoints. Opening a tunnel puts the manager on a
 * host inside the gateway's own network, so it has to require a role of its own rather than
 * accepting any account that happens to be signed in.
 */
class GatewayTunnelAuthorizationTest extends Specification implements ManagerContainerTrait {

  @Shared
  static GatewayServiceResource noRoleTunnelResource

  @Shared
  static String buildingRealm

  def setupSpec() {
    def container = startContainer(defaultConfig(), defaultServices())
    def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
    buildingRealm = keycloakTestSetup.realmBuilding.name

    // testuser2 holds write:user, read:map and read:assets and nothing else. It is the least
    // privileged account the API will accept, and it is not a restricted user.
    def accessToken = authenticate(container, buildingRealm, KEYCLOAK_CLIENT_ID, "testuser2", "testuser2")
    noRoleTunnelResource = getClientApiTarget(serverUri(serverPort), buildingRealm, accessToken)
      .proxy(GatewayServiceResource.class)
  }

  def "A user holding no tunnel role cannot list a gateway's tunnels"() {

    when: "a user with no administrative role lists the tunnels of an arbitrary gateway"
    noRoleTunnelResource.getGatewayActiveTunnelInfos(null, buildingRealm, "somegatewayid")

    then: "the request is refused"
    WebApplicationException ex = thrown()
    ex.response.status == 403
  }

  def "A user holding no tunnel role cannot read a single tunnel"() {

    when: "a user with no administrative role reads one tunnel by endpoint"
    noRoleTunnelResource.getActiveTunnelInfo(null, buildingRealm, "somegatewayid", "10.0.0.1", 22)

    then: "the request is refused"
    WebApplicationException ex = thrown()
    ex.response.status == 403
  }

  def "A user holding no tunnel role cannot start a tunnel"() {

    when: "a user with no administrative role opens a tunnel to a host on the gateway's network"
    noRoleTunnelResource.startTunnel(new GatewayTunnelInfo(
      buildingRealm,
      "somegatewayid",
      GatewayTunnelInfo.Type.TCP,
      "169.254.169.254",
      80))

    then: "the request is refused before the gateway is ever consulted"
    WebApplicationException ex = thrown()
    ex.response.status == 403
  }

  def "A user holding no tunnel role cannot stop a tunnel"() {

    when: "a user with no administrative role stops a tunnel"
    noRoleTunnelResource.stopTunnel(new GatewayTunnelInfo(buildingRealm, "somegatewayid"))

    then: "the request is refused"
    WebApplicationException ex = thrown()
    ex.response.status == 403
  }

  def "A user holding no tunnel role cannot list a realm's tunnels"() {

    when: "a user with no administrative role lists every tunnel in the realm"
    noRoleTunnelResource.getAllActiveTunnelInfos(null, buildingRealm)

    then: "the request is refused"
    WebApplicationException ex = thrown()
    ex.response.status == 403
  }
}
