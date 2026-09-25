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
package org.openremote.manager.security

import com.nimbusds.jwt.JWTClaimsSet
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import org.openremote.container.security.TokenPrincipal
import org.openremote.container.timer.TimerService
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.model.query.UserQuery
import org.openremote.model.security.Credential
import org.openremote.model.security.Realm
import org.openremote.model.security.User
import spock.lang.Specification

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.SUPER_USER_REALM_ROLE

class UserResourceRealmTest extends Specification {

  static final String REALM = "building"
  static final String USER_ID = "user-1"
  static final long NOW = 1_800_000_000_000L

  def realm = new Realm().setName(REALM).setEnabled(true)
  def user = new User().setId(USER_ID).setRealm(REALM)
  def timer = Stub(TimerService) {
    getCurrentTimeMillis() >> NOW
  }
  def provider = Spy(ManagerKeycloakIdentityProvider)
  def mqtt = Mock(MQTTBrokerService)
  UserResourceImpl resource

  def setup() {
    provider.timerService = timer
    provider.getRealm(REALM) >> realm
    provider.isUserInRealm(USER_ID, REALM) >> true
    provider.getUser(USER_ID) >> user
    provider.createUpdateUser(REALM, _ as User, null, _) >> user
    provider.queryUsers(_ as UserQuery) >> ([user] as User[])
    provider.requestPasswordReset(REALM, USER_ID) >> {}
    provider.resetPassword(REALM, USER_ID, _ as Credential) >> {}
    mqtt.getConnectionUserId("connection-1") >> USER_ID
    mqtt.disconnectSession("connection-1") >> true

    def identityService = Stub(ManagerIdentityService) {
      getIdentityProvider() >> provider
    }
    resource = new UserResourceImpl(timer, identityService, mqtt)
    authenticate(false)
  }

  def "#operation rejects an existing caller after their realm is disabled"() {
    when: "the realm is active"
    invoke(resource, user)

    then:
    noExceptionThrown()

    when: "the realm is disabled without changing the caller's token"
    realm.setEnabled(false)
    invoke(resource, user)

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 403
    0 * provider.createUpdateUser(_, _, _, _)
    0 * provider.queryUsers(_)
    0 * provider.requestPasswordReset(_, _)
    0 * provider.resetPassword(_, _, _)
    0 * mqtt.disconnectSession(_)

    where:
    operation | invoke
    "query" | { r, u -> r.query(null, new UserQuery()) }
    "getCurrent" | { r, u -> r.getCurrent(null) }
    "create" | { r, u -> r.create(null, REALM, u) }
    "update" | { r, u -> r.update(null, REALM, u) }
    "updateCurrent" | { r, u -> r.updateCurrent(null, u) }
    "password reset" | { r, u -> r.requestPasswordReset(null, REALM, USER_ID) }
    "own password reset" | { r, u -> r.requestPasswordResetCurrent(null) }
    "password update" | { r, u -> r.updatePassword(null, REALM, USER_ID, new Credential()) }
    "own password update" | { r, u -> r.updatePasswordCurrent(null, new Credential()) }
    "MQTT disconnect" | { r, u -> r.disconnectUserSession(null, REALM, "connection-1") }
  }

  def "a realm with a future activation time rejects user queries"() {
    given:
    realm.setNotBefore((NOW / 1000 + 60) as Double)

    when:
    resource.query(null, new UserQuery())

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 403
    0 * provider.queryUsers(_)
  }

  def "superusers can still administer inactive realms"() {
    given:
    realm.setEnabled(enabled).setNotBefore(notBefore)
    authenticate(true)

    when:
    def result = resource.create(null, REALM, user)
    resource.update(null, REALM, user)
    resource.query(null, new UserQuery())

    then:
    result == user
    noExceptionThrown()

    where:
    enabled | notBefore
    false | 0d
    true | (NOW / 1000 + 60) as Double
  }

  def "#caller can disconnect a session in an authorized realm"() {
    given:
    authenticate(superUser, clientRoles, callerId)
    realm.setEnabled(enabled).setNotBefore(notBefore)

    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    noExceptionThrown()
    1 * mqtt.disconnectSession("connection-1") >> true

    where:
    caller | superUser | clientRoles | callerId | enabled | notBefore
    "owner without client roles" | false | [] | USER_ID | true | 0d
    "same-realm write administrator" | false | ["write:admin"] | "admin" | true | 0d
    "superuser without client roles" | true | [] | "superuser" | true | 0d
    "superuser in a disabled realm" | true | [] | "superuser" | false | 0d
    "superuser before realm activation" | true | [] | "superuser" | true | (NOW / 1000 + 60) as Double
  }

  def "ordinary users without write admin cannot disconnect another user's session"() {
    given:
    authenticate(false, clientRoles, "other-user")

    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 403
    0 * mqtt.disconnectSession(_)

    where:
    clientRoles << [[], ["read:admin"], ["write:user"]]
  }

  def "write administrators from another realm cannot disconnect the session"() {
    given:
    authenticate(false, ["write:admin"], "other-admin", callerRealm)

    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 405
    0 * mqtt.disconnectSession(_)

    where:
    callerRealm << [MASTER_REALM, "other-realm"]
  }

  def "an incorrect realm in the URL is rejected even for superusers"() {
    given:
    authenticate(superUser)

    when:
    resource.disconnectUserSession(null, "other-realm", "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 404
    0 * mqtt.disconnectSession(_)

    where:
    superUser << [false, true]
  }

  def "anonymous callers cannot disconnect an existing session"() {
    given:
    resource.securityContext = Stub(SecurityContext) {
      getUserPrincipal() >> null
    }

    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 401
    0 * mqtt.disconnectSession(_)
  }

  def "a missing session returns 404 without attempting a disconnect"() {
    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 404
    1 * mqtt.getConnectionUserId("connection-1") >> null
    0 * provider.getUser(_)
    0 * mqtt.disconnectSession(_)
  }

  def "a session whose owner was deleted returns 404 without attempting a disconnect"() {
    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 404
    1 * provider.getUser(USER_ID) >> null
    0 * mqtt.disconnectSession(_)
  }

  def "a session disappearing after authorization returns 404"() {
    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 404
    1 * mqtt.disconnectSession("connection-1") >> false
  }

  def "a caller cannot disconnect a session before realm activation"() {
    given:
    realm.setNotBefore((NOW / 1000 + 60) as Double)

    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 403
    0 * mqtt.disconnectSession(_)
  }

  def "a session owner no longer belonging to the resolved realm prevents disconnection"() {
    when:
    resource.disconnectUserSession(null, REALM, "connection-1")

    then:
    def exception = thrown(WebApplicationException)
    exception.response.status == 405
    1 * provider.isUserInRealm(USER_ID, REALM) >> false
    0 * mqtt.disconnectSession(_)
  }

  private void authenticate(boolean superUser, List<String> clientRoles = ["read:admin", "write:admin"], String userId = USER_ID, String authenticatedRealm = null) {
    def principal = new TokenPrincipal(new JWTClaimsSet.Builder()
            .subject(userId)
            .issuer("http://localhost/realms/" + (authenticatedRealm ?: (superUser ? MASTER_REALM : REALM)))
            .claim("realm_access", [roles: superUser ? [SUPER_USER_REALM_ROLE] : []])
            .claim("resource_access", [(KEYCLOAK_CLIENT_ID): [roles: clientRoles]])
            .build())
    resource.securityContext = Stub(SecurityContext) {
      getUserPrincipal() >> principal
    }
  }
}
