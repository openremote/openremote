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
package org.openremote.test.users

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import jakarta.ws.rs.WebApplicationException
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.model.security.UserResource
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.nio.charset.StandardCharsets.UTF_8
import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.manager.mqtt.MQTTBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.mqtt.MQTTBrokerService.MQTT_SERVER_LISTEN_PORT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.util.MapAccess.getInteger
import static org.openremote.model.util.MapAccess.getString

class UserSessionResourceTest extends Specification implements ManagerContainerTrait {

  def "HTTP session disconnection by #caller enforces permissions and closes the MQTT connection"() {
    given:
    def container = startContainer(defaultConfig(), defaultServices())
    def setup = container.getService(SetupService).getTaskOfType(KeycloakTestSetup)
    def broker = container.getService(MQTTBrokerService)
    def realm = setup.realmBuilding.name
    def owner = setup.serviceUser
    def other = setup.serviceUser2
    def superuser = setup.superServiceUser
    def uri = serverUri(serverPort)
    def ownerToken = authenticate(container, realm, owner.username, owner.secret)
    def otherToken = authenticate(container, realm, other.username, other.secret)
    def crossRealmToken = authenticate(container, MASTER_REALM, KEYCLOAK_CLIENT_ID, setup.testuser1.username, "testuser1")
    def superuserToken = authenticate(container, MASTER_REALM, superuser.username, superuser.secret)
    def ownerTarget = getClientApiTarget(uri, realm, ownerToken)
    def superuserTarget = getClientApiTarget(uri, MASTER_REALM, superuserToken)
    def ownerResource = ownerTarget.proxy(UserResource)
    def otherResource = getClientApiTarget(uri, realm, otherToken).proxy(UserResource)
    def crossRealmResource = getClientApiTarget(uri, MASTER_REALM, crossRealmToken).proxy(UserResource)
    def anonymousResource = getClientApiTarget(uri, realm).proxy(UserResource)
    def superuserResource = superuserTarget.proxy(UserResource)
    def conditions = new PollingConditions(timeout: 15, initialDelay: 0.1, delay: 0.2)
    String sessionId = null
    def response = null

    // Use the raw client without automatic reconnect so a forced disconnect remains observable.
    def client = MqttClient.builder()
            .useMqttVersion3()
            .identifier(UniqueIdentifierGenerator.generateId())
            .serverHost(getString(container.config, MQTT_SERVER_LISTEN_HOST, "127.0.0.1"))
            .serverPort(getInteger(container.config, MQTT_SERVER_LISTEN_PORT, 1883))
            .buildAsync()

    when: "the service user opens an MQTT connection"
    client.connectWith().cleanSession(true)
            .simpleAuth().username(realm + ":" + owner.username).password(owner.secret.getBytes(UTF_8)).applySimpleAuth()
            .send().get(10, SECONDS)

    then: "the owner can discover the live session through HTTP"
    conditions.eventually {
      def sessions = ownerResource.getUserSessions(null, realm, owner.id)
      assert sessions.length == 1
      sessionId = sessions[0].getID()
      assert broker.getConnectionUserId(sessionId) == owner.id
    }

    when: "an anonymous caller attempts to disconnect it"
    anonymousResource.disconnectUserSession(null, realm, sessionId)

    then:
    def anonymousException = thrown(WebApplicationException)
    anonymousException.response.status == 401
    client.state == MqttClientState.CONNECTED
    broker.getConnectionUserId(sessionId) == owner.id

    when: "another user without write:admin attempts to disconnect it"
    otherResource.disconnectUserSession(null, realm, sessionId)

    then:
    def forbiddenException = thrown(WebApplicationException)
    forbiddenException.response.status == 403
    client.state == MqttClientState.CONNECTED
    broker.getConnectionUserId(sessionId) == owner.id

    when: "a non-superuser from another realm supplies the session owner's realm"
    crossRealmResource.disconnectUserSession(null, realm, sessionId)

    then:
    def crossRealmException = thrown(WebApplicationException)
    crossRealmException.response.status == 405
    client.state == MqttClientState.CONNECTED
    broker.getConnectionUserId(sessionId) == owner.id

    when: "a superuser supplies the wrong realm"
    superuserResource.disconnectUserSession(null, MASTER_REALM, sessionId)

    then:
    def wrongRealmException = thrown(WebApplicationException)
    wrongRealmException.response.status == 404
    client.state == MqttClientState.CONNECTED
    broker.getConnectionUserId(sessionId) == owner.id

    when: "an authorized caller disconnects the session"
    def authorizedTarget = caller == "owner" ? ownerTarget : superuserTarget
    response = authorizedTarget.path("user/${realm}/disconnect/${sessionId}".toString()).request().delete()

    then:
    response.status == 204
    conditions.eventually {
      assert client.state == MqttClientState.DISCONNECTED
      assert broker.getConnectionUserId(sessionId) == null
      assert ownerResource.getUserSessions(null, realm, owner.id).length == 0
    }

    when: "the owner repeats the request for the disconnected session"
    ownerResource.disconnectUserSession(null, realm, sessionId)

    then:
    def missingException = thrown(WebApplicationException)
    missingException.response.status == 404

    cleanup:
    response?.close()
    if (client?.state == MqttClientState.CONNECTED) {
      client.disconnect().get(10, SECONDS)
    }

    where:
    caller << ["owner", "superuser"]
  }
}
