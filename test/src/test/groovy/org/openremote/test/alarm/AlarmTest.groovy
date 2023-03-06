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

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.util.ValueUtil.parse

class AlarmTest extends Specification implements ManagerContainerTrait{

    def "Check alarm service functionality"(){
        List<Alarm> alarms = []

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        AlarmService alarmService = container.getService(AlarmService.class)


        and: "a mock persistence service"
        AlarmService mockAlarmService = Spy(alarmService)
        mockAlarmService.sendAlarm(_ as Alarm) >> {
            output ->
                alarms << output
                callRealMethod()
        }
        mockAlarmService.getAlarms(_ as String, _ as String) >> {

        }

        and: "an authenticated test user"
        def testuser1AccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        and: "an authenticated superuser"
        def adminAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        Alarm alarm = new Alarm("Test Alarm", "Test Content", Alarm.Severity.MEDIUM)
        //SentAlarm[] sentAlarms = [new SentAlarm("1", "Test SentAlarm", "Test Content", Alarm.Severity.HIGH, Alarm.Status.ACTIVE), new SentAlarm("2", "Test SentAlarm2", "Test Content", Alarm.Severity.MEDIUM, Alarm.Status.ACTIVE)]


        and: "the mock alarm resource"
        def testuser1Resource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, testuser1AccessToken).proxy(AlarmResource.class)
        def adminResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(AlarmResource.class)
        def anonymousResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name).proxy(AlarmResource.class)

        when: "the admin user creates an alarm"
        mockAlarmService.sendAlarm(alarm);

        then: "an alarm should have been created"
        conditions.eventually {
            assert alarms.size() == 1
        }

        when: "the admin user marks a Building console notification as delivered and requests the notifications for Building consoles"
        adminResource.createAlarm(null, alarm)

        then: "the notification should have been updated"
        conditions.eventually {
            alarms = adminResource.getAlarms(null, null, null)
            assert alarms.length == 1
            //assert alarms.count {n -> n.deliveredOn != null} == 1
        }
    }
}
