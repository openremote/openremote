package org.openremote.test.user

import org.openremote.manager.server.notification.NotificationService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.notification.NotificationResource
import org.openremote.model.notification.ActionType
import org.openremote.model.notification.AlertAction
import org.openremote.model.notification.AlertNotification
import org.openremote.model.user.UserQuery
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID

class NotificationServiceTest extends Specification implements ManagerContainerTrait {

    def "Store notification tokens for user devices"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoScenesOrRules(defaultConfig(serverPort), defaultServices())
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def notificationService = container.getService(NotificationService.class)

        and: "an authenticated test user"
        def realm = "customerA"
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        def notificationAlert = new AlertNotification(
                title: "The Title",
                appUrl: '#test',
                message: "Message",
        )

        def alertAction = new AlertAction()

        alertAction.setTitle("TEST_ACTION")
        alertAction.setActionType(ActionType.ACTUATOR)
        notificationAlert.addAction(alertAction)

        and: "the notification resource"
        def notificationResource = getClientTarget(serverUri(serverPort), realm, accessToken).proxy(NotificationResource.class)

        expect: "there should be no user registered to receive notifications"
        notificationService.findAllUsersWithToken().isEmpty()

        when: "the notification tokens of some devices are stored"
        notificationResource.storeDeviceToken(null, "device123", "token123")
        notificationResource.storeDeviceToken(null, "device456", "token456")

        then: "the tokens should be in the database"
        notificationService.findDeviceToken("device123", keycloakDemoSetup.testuser3Id) == "token123"
        notificationService.findDeviceToken("device456", keycloakDemoSetup.testuser3Id) == "token456"

        and: "test user must be registered to receive notifications"
        notificationService.findAllUsersWithToken().size() == 1
        def foundUser = notificationService.findAllUsersWithToken().first()
        foundUser == keycloakDemoSetup.testuser3Id

        and: "must find registered test user if limiting to same tenant"
        notificationService.findAllUsersWithToken(new UserQuery().tenant(new UserQuery.TenantPredicate().realmId(keycloakDemoSetup.customerATenant.id))).size() == 1
        def foundUser2 = notificationService.findAllUsersWithToken(new UserQuery().tenant(new UserQuery.TenantPredicate().realmId(keycloakDemoSetup.customerATenant.id))).first()
        foundUser2 == keycloakDemoSetup.testuser3Id

        and: "must not find registered test user if limiting to different tenant"
        notificationService.findAllUsersWithToken(new UserQuery().tenant(new UserQuery.TenantPredicate().realmId(keycloakDemoSetup.customerBTenant.id))).isEmpty()

        and: "must find registered test user if limiting to asset user has access to"
        notificationService.findAllUsersWithToken(new UserQuery().asset(new UserQuery.AssetPredicate().id(managerDemoSetup.apartment1Id))).size() == 1
        def foundUser3 = notificationService.findAllUsersWithToken(new UserQuery().asset(new UserQuery.AssetPredicate().id(managerDemoSetup.apartment1Id))).first()
        foundUser3 == keycloakDemoSetup.testuser3Id

        and: "must not find registered test user if limiting to asset user has not access to"
        notificationService.findAllUsersWithToken(new UserQuery().asset(new UserQuery.AssetPredicate().id(managerDemoSetup.apartment3Id))).isEmpty()

        when: "the notification token of some device is updated"
        notificationResource.storeDeviceToken(null, "device456", "token789")

        then: "the updated token should be in the database"
        notificationService.findDeviceToken("device456", keycloakDemoSetup.testuser3Id) == "token789"
        notificationService.findAllTokenForUser(keycloakDemoSetup.testuser3Id).size() == 2

        when: "the Alert Notification is stored"
        notificationResource.storeAlertNotification(notificationAlert)
        def alerts = notificationResource.getAlertNotification()

        then: "it should be one alert notification pending"
        alerts.size() == 1

        when: "the alert Notification is acknowledge"
        notificationResource.removeAlertNotification(alerts.get(0).id)

        then: "it should be no alert"
        notificationResource.getAlertNotification().size() == 0

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
