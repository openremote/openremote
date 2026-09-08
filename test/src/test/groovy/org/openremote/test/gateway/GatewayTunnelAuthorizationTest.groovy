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
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.gateway.GatewayService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.security.ManagerKeycloakIdentityProvider
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.gateway.GatewayServiceResource
import org.openremote.model.gateway.GatewayTunnelInfo
import org.openremote.model.security.User
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.READ_TUNNELS_ROLE
import static org.openremote.model.Constants.RESTRICTED_USER_REALM_ROLE

/**
 * Covers the role gate on the gateway tunnel endpoints. Opening a tunnel puts the manager on a
 * host inside the gateway's own network, so it has to require a role of its own rather than
 * accepting any account that happens to be signed in.
 *
 * The restricted user cases use plain assets in place of gateway assets. The filter keys on the
 * user asset link and not on the asset type, so this exercises the same query while leaving the
 * gateway connector map alone.
 */
class GatewayTunnelAuthorizationTest extends Specification implements ManagerContainerTrait {

  @Shared
  static GatewayServiceResource noRoleTunnelResource

  @Shared
  static GatewayServiceResource restrictedTunnelResource

  @Shared
  static GatewayServiceResource tunnelReaderResource

  @Shared
  static GatewayService gatewayService

  @Shared
  static ManagerKeycloakIdentityProvider identityProvider

  @Shared
  static String buildingRealm

  @Shared
  static String linkedGatewayId

  @Shared
  static String unlinkedGatewayId

  @Shared
  static List<String> createdUserIds = []

  def setupSpec() {
    def container = startContainer(defaultConfig(), defaultServices())
    def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
    def assetStorageService = container.getService(AssetStorageService.class)
    buildingRealm = keycloakTestSetup.realmBuilding.name
    gatewayService = container.getService(GatewayService.class)
    identityProvider = container.getService(ManagerIdentityService.class).identityProvider as ManagerKeycloakIdentityProvider

    // testuser2 holds write:user, read:map and read:assets and nothing else. It is the least
    // privileged account the API will accept, and it is not a restricted user.
    def accessToken = authenticate(container, buildingRealm, KEYCLOAK_CLIENT_ID, "testuser2", "testuser2")
    noRoleTunnelResource = getClientApiTarget(serverUri(serverPort), buildingRealm, accessToken)
            .proxy(GatewayServiceResource.class)

    linkedGatewayId = assetStorageService.merge(
            new ThingAsset("Linked gateway").setRealm(buildingRealm)).id
    unlinkedGatewayId = assetStorageService.merge(
            new ThingAsset("Unlinked gateway").setRealm(buildingRealm)).id

    restrictedTunnelResource = tunnelResourceFor(
            container, assetStorageService, "tunnelrestricted", true, linkedGatewayId)
    tunnelReaderResource = tunnelResourceFor(
            container, assetStorageService, "tunnelreader", false, null)

    [linkedGatewayId, unlinkedGatewayId].each { gatewayId ->
      def tunnel = new GatewayTunnelInfo(
      buildingRealm, gatewayId, GatewayTunnelInfo.Type.HTTPS, "localhost", 443)
      gatewayService.@tunnelInfos.put(tunnel.id, tunnel)
    }
  }

  def cleanupSpec() {
    if (gatewayService != null) {
      gatewayService.@tunnelInfos.values().removeIf { tunnel ->
        tunnel.gatewayId == linkedGatewayId || tunnel.gatewayId == unlinkedGatewayId
      }
    }
    if (identityProvider != null) {
      createdUserIds.each { identityProvider.deleteUser(buildingRealm, it) }
      createdUserIds.clear()
    }
  }

  /** Creates a user holding read:tunnels and returns the tunnel resource authenticated as them. */
  private GatewayServiceResource tunnelResourceFor(
          container, AssetStorageService assetStorageService, String username, boolean restricted,
          String linkedAssetId) {

    def user = identityProvider.createUpdateUser(
            buildingRealm, new User().setUsername(username).setEnabled(true), username, true)
    createdUserIds << user.id

    identityProvider.updateUserClientRoles(
            buildingRealm, user.id, KEYCLOAK_CLIENT_ID, READ_TUNNELS_ROLE)

    if (restricted) {
      identityProvider.updateUserRealmRoles(
              buildingRealm,
              user.id,
              identityProvider.addUserRealmRoles(buildingRealm, user.id, RESTRICTED_USER_REALM_ROLE))
    }

    if (linkedAssetId != null) {
      assetStorageService.storeUserAssetLinks(
              [new UserAssetLink(buildingRealm, user.id, linkedAssetId)])
    }

    def accessToken = authenticate(container, buildingRealm, KEYCLOAK_CLIENT_ID, username, username)
    return getClientApiTarget(serverUri(serverPort), buildingRealm, accessToken)
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

  def "A restricted user sees only the tunnels of the gateways linked to them"() {

    when: "a restricted user holding the tunnel read role lists every tunnel in the realm"
    def tunnels = restrictedTunnelResource.getAllActiveTunnelInfos(null, buildingRealm)

    then: "the realm's other tunnels are filtered out rather than the request being refused"
    tunnels.length == 1
    tunnels[0].gatewayId == linkedGatewayId
  }

  def "A user holding the tunnel read role sees the whole realm"() {

    when: "an unrestricted user holding the tunnel read role lists every tunnel in the realm"
    def tunnels = tunnelReaderResource.getAllActiveTunnelInfos(null, buildingRealm)

    then: "tunnels for gateways they are not linked to are returned too"
    tunnels.collect { it.gatewayId }.containsAll([linkedGatewayId, unlinkedGatewayId])
  }
}
