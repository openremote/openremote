package org.openremote.test.alarm

import org.openremote.model.alarm.Alarm
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

import javax.ws.rs.WebApplicationException

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.util.ValueUtil.parse

abstract class BaseSpec extends Specification {
    List<SentAlarm> alarms = []
    def conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
    def container
    def keycloakTestSetup
    def managerTestSetup
    def testuser1AccessToken
    def adminAccessToken
    def testuser1Resource
    def adminResource
    def anonymousResource

    def setup() {
        container = startContainer(defaultConfig(), defaultServices())
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        AlarmService alarmService = container.getService(AlarmService.class)

        AlarmService mockAlarmService = Spy(alarmService)
        mockAlarmService.sendAlarm(_ as Alarm) >> {
            output ->
                alarms << output
                callRealMethod()
        }
        mockAlarmService.getAlarms(_ as String, _ as String) >> {

        }

        testuser1AccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        adminAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token
        
        testuser1Resource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, testuser1AccessToken).proxy(AlarmResource.class)
        adminResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(AlarmResource.class)
        anonymousResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name).proxy(AlarmResource.class)
    }

    def cleanup() {
        container.stop()
    }
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
