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

class AlarmTest extends Specification implements ManagerContainerTrait{
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
        assert alarm != null
        assert alarm.title == title
        assert alarm.content == content
        assert alarm.severity == severity
        assert alarm.status == Alarm.Status.OPEN


        where:
        title | content | severity
        "Test Alarm" | "Test Description" | Severity.LOW
        "Another Alarm" | "Another Description" | Severity.MEDIUM
    }

    @Unroll
    def "should create an alarm with title '#title', content '#content', severity '#severity', and source '#source'"() {
        when: "an alarm is created"
        def input = new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(Alarm.Status.OPEN).setRealm(MASTER_REALM)
        def alarm = adminResource.createAlarmWithSource(null, input, source, 'test')


        then:
        assert alarm != null
        assert alarm.title == title
        assert alarm.content == content
        assert alarm.severity == severity
        assert alarm.status == Alarm.Status.OPEN
        assert alarm.source == source


        where:
        title | content | severity | source
        "Test Alarm" | "Test Description" | Severity.LOW | Alarm.Source.AGENT
        "Another Alarm" | "Another Description" | Severity.MEDIUM | Alarm.Source.REALM_RULESET
    }

    @Unroll
    def "should not create an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
        when:
        adminResource.createAlarm(null, new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status).setRealm(MASTER_REALM))

        and:
        adminResource.createAlarmWithSource(null, new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status).setRealm(MASTER_REALM), Alarm.Source.MANUAL, "id")

        then:
        WebApplicationException ex = thrown()
        assert ex.response.status == 400

        where:
        title | content | severity | status
        null | "Test Description" | Severity.LOW | Alarm.Status.OPEN
        "Another Test Alarm" | "Another Description" | null | Alarm.Status.RESOLVED
    }

    // Create alarm without write:alarm role
    def "should not create an alarm as regular user"() {
        when:
        regularUserResource.createAlarm(null, new Alarm().setTitle("title").setContent("content").setSeverity(Severity.MEDIUM).setStatus(Alarm.Status.ACKNOWLEDGED).setRealm(MASTER_REALM))

        and:
        regularUserResource.createAlarmWithSource(null, new Alarm().setTitle("title").setContent("content").setSeverity(Severity.MEDIUM).setStatus(Alarm.Status.ACKNOWLEDGED).setRealm(MASTER_REALM), Alarm.Source.MANUAL, "id")

        then:
        WebApplicationException ex = thrown()
        assert ex.response.status == 403
    }


    // Get alarms as admin
    def "should return list of alarms"() {
        when:
        def output = adminResource.getAlarms(null)

        then:
        assert output != null
        assert output.size() == 4
    }

    // Get alarms without read:alarm role
    def "should not return list of alarms"() {
        when:
        regularUserResource.getAlarms(null)

        then:
        WebApplicationException ex = thrown()
        assert ex.response.status == 403
    }

    // Update alarm as admin
    @Unroll
    def "should update an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
        when:
        def updatable = adminResource.getAlarms(null)[0]
        adminResource.updateAlarm(null, updatable.id, new SentAlarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status))
        def updated = adminResource.getAlarms(null)[0]

        then:
        assert updated != null
        assert updated.title == title
        assert updated.content == content
        assert updated.severity == severity
        assert updated.status == status


        where:
        title | content | severity | status
        "Updated Alarm" | "Test Description" | Severity.HIGH | Alarm.Status.OPEN
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
        def updatable = adminResource.getAlarms(null)[0]
        regularUserResource.updateAlarm(null, updatable.id, new SentAlarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status))

        then:
        WebApplicationException ex = thrown()
        assert ex.response.status == 403

        where:
        title | content | severity | status
        "Updated Alarm" | "Test Description" | Severity.HIGH | Alarm.Status.OPEN
        "Another Alarm" | "Updated Description" | Severity.MEDIUM | Alarm.Status.CLOSED
    }

    // Delete alarm without write:alarm role
    def "should not delete alarm without proper permissions"() {
        when:
        def delete = adminResource.getAlarms(null)[0]
        regularUserResource.removeAlarm(null, delete.id)

        then:
        WebApplicationException ex = thrown()
        assert ex.response.status == 403
    }

    // Delete alarms without write:alarm role
    def "should not delete alarms without proper permissions"() {
        when:
        def delete = adminResource.getAlarms(null)
        regularUserResource.removeAlarms(null, (List<Long>) delete.collect{it.id})

        then:
        WebApplicationException ex = thrown()
        assert ex.response.status == 403
    }

    // Delete alarm as admin
    def "should delete alarm as admin"() {
        when:
        def delete = adminResource.getAlarms(null)[0]
        adminResource.removeAlarm(null, delete.id)

        then:
        adminResource.getAlarms(null).find {it.id == delete.id} == null
    }

    // Delete alarms as admin
    def "should delete alarms as admin"() {
        when:
        def delete = adminResource.getAlarms(null)
        adminResource.removeAlarms(null, (List<Long>) delete.collect{it.id})

        then:
        adminResource.getAlarms(null).length == 0
    }

    // Get open alarms
    def "should get open alarms"() {
        when: "two open and one closed alarms are added"
        adminResource.createAlarm(null, new Alarm().setTitle('alarm 1').setContent('content').setStatus(Alarm.Status.OPEN).setSeverity(Severity.LOW).setRealm(MASTER_REALM))
        adminResource.createAlarm(null, new Alarm().setTitle('alarm 2').setContent('content').setStatus(Alarm.Status.OPEN).setSeverity(Severity.LOW).setRealm(MASTER_REALM))
        adminResource.createAlarm(null, new Alarm().setTitle('alarm 3').setContent('content').setStatus(Alarm.Status.CLOSED).setSeverity(Severity.LOW).setRealm(MASTER_REALM))
        def open = adminResource.getOpenAlarms(null)

        then: "returns 2 open alarms"
        open.size() == 2
    }
}
