package org.openremote.test.alarm

import org.openremote.model.alarm.Alarm
import org.openremote.model.alarm.Alarm.Severity
import org.openremote.model.alarm.SentAlarm
import org.openremote.model.alarm.AlarmResource
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.alarm.AlarmService
import org.openremote.manager.asset.console.ConsoleResourceImpl
import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import spock.lang.Unroll
import spock.lang.Shared

import javax.ws.rs.WebApplicationException

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.util.ValueUtil.parse
import jakarta.ws.rs.WebApplicationException

class AlarmTest extends Specification implements ManagerContainerTrait{
    @Shared
    List<SentAlarm> alarms = []

    @Shared
    static AlarmService mockAlarmService

    @Shared 
    static AlarmResource adminResource

    @Shared
    static KeycloakTestSetup keycloakTestSetup

    @Shared
    static ManagerTestSetup managerTestSetup

    @Shared 
    static PollingConditions conditions


    def setupSpec() {
        conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        AlarmService alarmService = container.getService(AlarmService.class)


//        and: "a mock persistence service"
//        AlarmService mockAlarmService = Spy(alarmService)
//        mockAlarmService.sendAlarm(_ as Alarm) >> {
//            output ->
//                alarms << output
//                callRealMethod()
//        }
//        mockAlarmService.getAlarms(_ as String, _ as String) >> {
//
//        }
//
//        and: "an authenticated test user"
//        def testuser1AccessToken = authenticate(
//                container,
//                MASTER_REALM,
//                KEYCLOAK_CLIENT_ID,
//                "testuser1",
//                "testuser1"
//        ).token
//
//        and: "an authenticated superuser"
//        def adminAccessToken = authenticate(
//                container,
//                MASTER_REALM,
//                KEYCLOAK_CLIENT_ID,
//                MASTER_REALM_ADMIN_USER,
//                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
//        ).token
//
//        Alarm alarm = new Alarm("Test Alarm", "Test Content", Alarm.Severity.MEDIUM)
//        Alarm alarm1 = new Alarm("Test Alarm1", "Test Content1", Alarm.Severity.LOW)
//        Alarm update = new Alarm("Updated Alarm1", "Updated Content1", Alarm.Severity.HIGH)
//        //SentAlarm[] sentAlarms = [new SentAlarm("1", "Test SentAlarm", "Test Content", Alarm.Severity.HIGH, Alarm.Status.ACTIVE), new SentAlarm("2", "Test SentAlarm2", "Test Content", Alarm.Severity.MEDIUM, Alarm.Status.ACTIVE)]
//
//
//        and: "the mock alarm resource"
//        def testuser1Resource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, testuser1AccessToken).proxy(AlarmResource.class)
//        def adminResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(AlarmResource.class)
//        def anonymousResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name).proxy(AlarmResource.class)
//
//        when: "the anonymous user creates an alarm"
//        anonymousResource.createAlarm(null, alarm)
//
//        then: "no alarm should have been created"
//        WebApplicationException ex = thrown()
//        ex.response.status == 403
//
//        when: "the admin user creates an alarm"
//        adminResource.createAlarm(null, alarm1)
//
//        then: "an alarm should have been created"
//        conditions.eventually {
//            alarms = adminResource.getAlarms(null)
//            assert alarms.count {n -> n.content != null} == 1
//        }
//
//        when: "the admin user updates an existing alarm"
//        adminResource.updateAlarm(null, alarms.first().id, update)
//
//        then: "the alarm object has been updated"
//        conditions.eventually {
//            alarms = adminResource.getAlarms(null)
//            assert alarms.first().content == update.content
//            assert alarms.first().title == update.title
//        }
    }


    @Unroll
    def "should create an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
        when:
        def alarm = mockAlarmService.sendAlarm(new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status))

        then:
        conditions.eventually {
            alarm != null
            alarm.title == title
            alarm.content == content
            alarm.severity == severity
            alarm.status == status
        }

        where:
        title | content | severity | status
        "Test Alarm" | "Test Description" | Severity.LOW | Alarm.Status.ACTIVE
        "Another Alarm" | "Another Description" | Severity.MEDIUM | Alarm.Status.RESOLVED
    }

    @Unroll
    def "should not create an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
        when:
        def alarm = adminResource.createAlarm(null, new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status))

        then:
        WebApplicationException ex = thrown()
        ex.response.status == 400

        where:
        title | content | severity | status
        null | "Test Description" | Severity.LOW | Alarm.Status.ACTIVE
        "Another Alarm" | null | Severity.MEDIUM | Alarm.Status.RESOLVED
        "Another Test Alarm" | "Another Description" | null | Alarm.Status.RESOLVED
    }

    def "should return list of alarms"() {
        when:
        def output = adminResource.getAlarms()

        then:
        output != null
        output.size() == 2
    }

    // @Unroll
    // def "should update an alarm with title '#title', content '#content', severity '#severity', and status '#status'"() {
    //     when:
    //     adminResource.updateAlarm(alarms[0].id, new Alarm().setTitle(title).setContent(content).setSeverity(severity).setStatus(status))
    //     def updated = adminResource.getAlarms()[0]

    //     then:
    //     conditions.eventually {
    //         updated != null
    //         updated.title == title
    //         updated.content == content
    //         updated.severity == severity
    //         updated.status == status
    //     }

    //     where:
    //     title | content | severity | status
    //     "Updated Alarm" | "Test Description" | Severity.HIGH | Alarm.Status.ACTIVE
    //     "Another Alarm" | "Updated Description" | Severity.MEDIUM | Alarm.Status.INACTIVE
    // }

    @Unroll
    def "should not update an alarm with id 'null'"() {
        when:
        adminResource.updateAlarm(null, null, new SentAlarm().setTitle('title').setContent('content').setSeverity(Severity.LOW).setStatus(Alarm.Status.ACTIVE))

        then:
        NullPointerException ex = thrown()
    }

    // @Unroll
    // def "should get an alarm with id '#id'"() {
    //     given:
    //     def alarm = alarmResourceImp.createAlarm("Test Alarm", "Test Description")

    //     when:
    //     def retrievedAlarm = alarmResourceImp.getAlarm(id)

    //     then:
    //     retrievedAlarm != null
    //     retrievedAlarm.id == id
    //     retrievedAlarm.name == alarm.name
    //     retrievedAlarm.description == alarm.description

    //     where:
    //     id << [alarm.id, "invalid-id"]
    // }

    // @Unroll
    // def "should update an alarm with id '#id' to have name '#name' and description '#description'"() {
    //     given:
    //     def alarm = alarmResourceImp.createAlarm("Test Alarm", "Test Description")

    //     when:
    //     def updatedAlarm = alarmResourceImp.updateAlarm(id, name, description)

    //     then:
    //     updatedAlarm != null
    //     updatedAlarm.id == id
    //     updatedAlarm.name == name
    //     updatedAlarm.description == description

    //     where:
    //     id | name | description
    //     alarm.id | "Updated Alarm" | "Updated Description"
    //     "invalid-id" | "Invalid Alarm" | "Invalid Description"
    // }

    // @Unroll
    // def "should delete an alarm with id '#id'"() {
    //     given:
    //     def alarm = alarmResourceImp.createAlarm("Test Alarm", "Test Description")

    //     when:
    //     def isDeleted = alarmResourceImp.deleteAlarm(id)

    //     then:
    //     isDeleted == true

    //     where:
    //     id << [alarm.id, "invalid-id"]
    // }
}

class AlarmTest extends BaseSpec implements ManagerContainerTrait{
    @Unroll
    def "should create an alarm with name '#name' and description '#description'"() {
        when:
        def alarm = adminResource.sendAlarm(name, description)

        then:
        alarm != null
        alarm.name == name
        alarm.content == description

        where:
        name | description
        "Test Alarm" | "Test Description"
        "Another Alarm" | "Another Description"
    }

    // @Unroll
    // def "should get an alarm with id '#id'"() {
    //     given:
    //     def alarm = alarmResourceImp.createAlarm("Test Alarm", "Test Description")

    //     when:
    //     def retrievedAlarm = alarmResourceImp.getAlarm(id)

    //     then:
    //     retrievedAlarm != null
    //     retrievedAlarm.id == id
    //     retrievedAlarm.name == alarm.name
    //     retrievedAlarm.description == alarm.description

    //     where:
    //     id << [alarm.id, "invalid-id"]
    // }

    // @Unroll
    // def "should update an alarm with id '#id' to have name '#name' and description '#description'"() {
    //     given:
    //     def alarm = alarmResourceImp.createAlarm("Test Alarm", "Test Description")

    //     when:
    //     def updatedAlarm = alarmResourceImp.updateAlarm(id, name, description)

    //     then:
    //     updatedAlarm != null
    //     updatedAlarm.id == id
    //     updatedAlarm.name == name
    //     updatedAlarm.description == description

    //     where:
    //     id | name | description
    //     alarm.id | "Updated Alarm" | "Updated Description"
    //     "invalid-id" | "Invalid Alarm" | "Invalid Description"
    // }

    // @Unroll
    // def "should delete an alarm with id '#id'"() {
    //     given:
    //     def alarm = alarmResourceImp.createAlarm("Test Alarm", "Test Description")

    //     when:
    //     def isDeleted = alarmResourceImp.deleteAlarm(id)

    //     then:
    //     isDeleted == true

    //     where:
    //     id << [alarm.id, "invalid-id"]
    // }
}
