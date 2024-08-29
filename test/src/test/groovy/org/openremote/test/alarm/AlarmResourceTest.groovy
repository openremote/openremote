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
package org.openremote.test.alarm

import org.openremote.model.alarm.Alarm
import org.openremote.model.alarm.Alarm.Severity
import org.openremote.model.alarm.SentAlarm
import org.openremote.model.alarm.AlarmResource
import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import jakarta.ws.rs.WebApplicationException

class AlarmResourceTest extends Specification implements ManagerContainerTrait {
    @Shared
    static AlarmResource adminResource

    @Shared
    static AlarmResource regularUserResource

    @Shared
    static KeycloakTestSetup keycloakTestSetup

    @Shared
    static ManagerTestSetup managerTestSetup

    def setupSpec() {
        def container = startContainer(defaultConfig(), defaultServices())
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)

        def adminAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        regularUserResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM).proxy(AlarmResource.class)
        adminResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(AlarmResource.class)
    }

    // Create alarm as admin
    @Unroll
    def "should create an alarm with title '#title', content '#content', and severity '#severity'"() {
        when: "an alarm is created"
        def input = new Alarm(title, content, severity, null, MASTER_REALM)
        def alarm = adminResource.createAlarm(null, input)

        then:
        alarm != null
        alarm.title == title
        alarm.content == content
        alarm.severity == severity
        alarm.status == Alarm.Status.OPEN

        where:
        title           | content               | severity
        "Test Alarm"    | "Test Description"    | Severity.LOW
        "Another Alarm" | "Another Description" | Severity.MEDIUM
    }

    @Unroll
    def "should create an alarm with title '#title', content '#content', severity '#severity', and source '#source'"() {
        when: "an alarm is created"
        def input = new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(Alarm.Status.OPEN).setRealm(MASTER_REALM)
        def alarm = adminResource.createAlarm(null, input)

        then:
        alarm != null
        alarm.title == title
        alarm.content == content
        alarm.severity == severity
        alarm.status == Alarm.Status.OPEN
        alarm.source == source

        where:
        title           | content               | severity        | source
        "Test Alarm"    | "Test Description"    | Severity.LOW    | Alarm.Source.MANUAL
        "Another Alarm" | "Another Description" | Severity.MEDIUM | Alarm.Source.MANUAL
    }

    @Unroll
    def "should not create an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
        when:
        adminResource.createAlarm(null, new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status).setRealm(MASTER_REALM))

        then:
        WebApplicationException ex = thrown()
        ex.response.status == 400

        where:
        title                | content               | severity     | status
        null                 | "Test Description"    | Severity.LOW | Alarm.Status.OPEN
        "Another Test Alarm" | "Another Description" | null         | Alarm.Status.RESOLVED
    }

    // Create alarm without write:alarm role
    def "should not create an alarm as regular user"() {
        when:
        regularUserResource.createAlarm(null, new Alarm().setTitle("title").setContent("content").setSeverity(Severity.MEDIUM).setStatus(Alarm.Status.ACKNOWLEDGED).setRealm(MASTER_REALM))

        then:
        WebApplicationException ex = thrown()
        ex.response.status == 403
    }

    // Get alarms as admin
    def "should return list of alarms"() {
        when:
        def output = adminResource.getAlarms(null, MASTER_REALM, null, null, null)

        then:
        output != null
        output.size() == 4
    }

    // Get alarms without read:alarm role
    def "should not return list of alarms"() {
        when:
        regularUserResource.getAlarms(null, MASTER_REALM, null, null, null)

        then:
        WebApplicationException ex = thrown()
        ex.response.status == 403
    }

    // Update alarm as admin
    @Unroll
    def "should update an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
        when:
        def updatable = adminResource.getAlarms(null, MASTER_REALM, null, null, null)[0]
        adminResource.updateAlarm(null, updatable.id, new SentAlarm().setTitle(title).setContent(content).setRealm(MASTER_REALM).setSeverity(severity).setStatus(status))
        def updated = adminResource.getAlarms(null, MASTER_REALM, null, null, null)[0]

        then:
        updated != null
        updated.title == title
        updated.content == content
        updated.severity == severity
        updated.status == status

        where:
        title           | content               | severity        | status
        "Updated Alarm" | "Test Description"    | Severity.HIGH   | Alarm.Status.OPEN
        "Another Alarm" | "Updated Description" | Severity.MEDIUM | Alarm.Status.CLOSED
    }

    @Unroll
    def "should not update an alarm with id 'null'"() {
        when:
        adminResource.updateAlarm(null, null, new SentAlarm().setTitle('title').setContent('content').setSeverity(Severity.LOW).setStatus(Alarm.Status.OPEN))

        then:
        NullPointerException ex = thrown()
    }

    // Update alarm without write:alarm role
    @Unroll
    def "should not update an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
        when:
        def updatable = adminResource.getAlarms(null, MASTER_REALM, null, null, null)[0]
        regularUserResource.updateAlarm(null, updatable.id, new SentAlarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status))

        then:
        WebApplicationException ex = thrown()
        ex.response.status == 403

        where:
        title           | content               | severity        | status
        "Updated Alarm" | "Test Description"    | Severity.HIGH   | Alarm.Status.OPEN
        "Another Alarm" | "Updated Description" | Severity.MEDIUM | Alarm.Status.CLOSED
    }

    // Delete alarm without write:alarm role
    def "should not delete alarm without proper permissions"() {
        when:
        def delete = adminResource.getAlarms(null, MASTER_REALM, null, null, null)[0]
        regularUserResource.removeAlarm(null, delete.id)

        then:
        WebApplicationException ex = thrown()
        ex.response.status == 403
    }

    // Delete alarms without write:alarm role
    def "should not delete alarms without proper permissions"() {
        when:
        def delete = adminResource.getAlarms(null, MASTER_REALM, null, null, null)
        regularUserResource.removeAlarms(null, (List<Long>) delete.collect { it.id })

        then:
        WebApplicationException ex = thrown()
        ex.response.status == 403
    }

    // Delete one alarm as admin
    def "should delete one alarm as admin"() {
        when:
        for (int i = 0; i < 2; i++) {
            adminResource.createAlarm(null, new Alarm("Alarm " + i, "Content " + i, Severity.MEDIUM, null, MASTER_REALM))
        }
        def delete = adminResource.getAlarms(null, MASTER_REALM, null, null, null)[0]
        adminResource.removeAlarm(null, delete.id)

        then: "returns some alarms but not the deleted alarm"
        def alarms = adminResource.getAlarms(null, MASTER_REALM, null, null, null)
        alarms.find { it.id == delete.id } == null
        alarms.length > 0
    }

    // Delete multiple alarms as admin
    def "should delete multiple alarms as admin"() {
        when:
        for (int i = 0; i < 2; i++) {
            adminResource.createAlarm(null, new Alarm("Alarm " + i, "Content " + i, Severity.MEDIUM, null, MASTER_REALM))
        }
        def delete = adminResource.getAlarms(null, MASTER_REALM, null, null, null)
        adminResource.removeAlarms(null, (List<Long>) delete.collect { it.id })

        then: "returns no alarms"
        adminResource.getAlarms(null, MASTER_REALM, null, null, null).length == 0
    }

    // Get open alarms
    def "should get open alarms"() {
        when: "two open and one closed alarms are added"
        adminResource.createAlarm(null, new Alarm().setTitle('alarm 1').setContent('content').setStatus(Alarm.Status.OPEN).setSeverity(Severity.LOW).setRealm(MASTER_REALM))
        adminResource.createAlarm(null, new Alarm().setTitle('alarm 2').setContent('content').setStatus(Alarm.Status.OPEN).setSeverity(Severity.LOW).setRealm(MASTER_REALM))
        adminResource.createAlarm(null, new Alarm().setTitle('alarm 3').setContent('content').setStatus(Alarm.Status.CLOSED).setSeverity(Severity.LOW).setRealm(MASTER_REALM))
        def open = adminResource.getAlarms(null, MASTER_REALM, Alarm.Status.OPEN, null, null)

        then: "returns 2 open alarms"
        open.size() == 2
    }
}
