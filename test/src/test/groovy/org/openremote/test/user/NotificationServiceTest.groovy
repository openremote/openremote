package org.openremote.test.user

import org.openremote.container.timer.TimerService
import org.openremote.manager.notification.FCMBaseMessage
import org.openremote.manager.notification.FCMDeliveryService
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.notification.NotificationResource
import org.openremote.model.notification.ActionType
import org.openremote.model.notification.AlertAction
import org.openremote.model.notification.AlertNotification
import org.openremote.model.notification.DeliveryStatus
import org.openremote.model.user.UserQuery
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.manager.notification.FCMDeliveryService.NOTIFICATION_FIREBASE_API_KEY
import static org.openremote.manager.notification.FCMDeliveryService.NOTIFICATION_FIREBASE_URL

class NotificationServiceTest extends Specification implements ManagerContainerTrait {

    def "Store notification tokens for user devices"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort) << [
            (NOTIFICATION_FIREBASE_API_KEY): "test",
            (NOTIFICATION_FIREBASE_URL): "test"
        ], defaultServices())
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "a mock FCM delivery service"
        def mockFCMDeliveryService = Spy(FCMDeliveryService, constructorArgs: [container]) {
            // Always "deliver" to FCM
            sendFCMMessage(_ as FCMBaseMessage) >> {
                return true
            }
        }
        def notificationService = container.getService(NotificationService)
        notificationService.fcmDeliveryService = mockFCMDeliveryService

        and: "an authenticated test user"
        def realm = "customerA"
        def testuser3AccessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token
        def testuser2AccessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser2",
                "testuser2"
        ).token

        and: "an authenticated superuser"
        def adminAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        def notification = new AlertNotification(
                title: "The Title",
                appUrl: '#test',
                message: "Message",
        )

        def alertAction = new AlertAction()

        alertAction.setTitle("TEST_ACTION")
        alertAction.setActionType(ActionType.ACTUATOR)
        notification.addAction(alertAction)

        and: "the notification resource"
        // For user operations
        def testuser3NotificationResource = getClientTarget(serverUri(serverPort), realm, testuser3AccessToken).proxy(NotificationResource.class)
        def testuser2NotificationResource = getClientTarget(serverUri(serverPort), realm, testuser2AccessToken).proxy(NotificationResource.class)

        // For superuser operations
        def adminNotificationResource = getClientTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(NotificationResource.class)

        expect: "there should be no user registered to receive notifications"
        notificationService.findAllUsersWithToken().isEmpty()

        when: "the notification tokens of some devices are stored"
        testuser3NotificationResource.storeDeviceToken(null, "device123", "token123", "ANDROID")
        testuser3NotificationResource.storeDeviceToken(null, "device456", "token456", "ANDROID")

        then: "the tokens should be in the database"
        def tokens = adminNotificationResource.getDeviceTokens(null, keycloakDemoSetup.testuser3Id)
        tokens.size() == 2
        tokens[0].id.deviceId == "device456"
        tokens[0].deviceType == "ANDROID"
        tokens[0].token == "token456"
        tokens[0].updatedOn.time <= container.getService(TimerService.class).currentTimeMillis
        tokens[1].id.deviceId == "device123"
        tokens[1].deviceType == "ANDROID"
        tokens[1].token == "token123"
        tokens[1].updatedOn.time <= container.getService(TimerService.class).currentTimeMillis

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
        testuser3NotificationResource.storeDeviceToken(null, "device456", "token789", "ANDROID")

        then: "the updated token should be in the database"
        def thirdToken = notificationService.findDeviceToken("device456", keycloakDemoSetup.testuser3Id)
        thirdToken.get().token == "token789"
        notificationService.findAllTokenForUser(keycloakDemoSetup.testuser3Id).size() == 2

        when: "the notification is stored"
        testuser3NotificationResource.storeNotificationForCurrentUser(null, notification)
        def alerts = testuser3NotificationResource.getQueuedNotificationsOfCurrentUser(null)

        then: "it should be one notification pending"
        alerts.size() == 1

        when: "a user tries to store a notification for another user"
        testuser2NotificationResource.storeNotificationForUser(null, keycloakDemoSetup.testuser3Id, notification)

        then: "an exception should be thrown"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "a user tries to acknowledge a notification she doesn't own"
        testuser2NotificationResource.ackNotificationOfCurrentUser(null, alerts.get(0).id)

        then: "an exception should be thrown"
        ex = thrown()
        ex.response.status == 403

        when: "the notification is acknowledged"
        testuser3NotificationResource.ackNotificationOfCurrentUser(null, alerts.get(0).id)

        then: "there should be no alert"
        testuser3NotificationResource.getQueuedNotificationsOfCurrentUser(null).size() == 0

        when: "the notification is stored"
        testuser3NotificationResource.storeNotificationForCurrentUser(null, notification)
        alerts = adminNotificationResource.getNotificationsOfUser(null, keycloakDemoSetup.testuser3Id)

        then: "it should be one notification queued"
        alerts.size() == 2
        alerts.any {
            it.deliveryStatus == DeliveryStatus.ACKNOWLEDGED
        }
        alerts.any {
            it.deliveryStatus == DeliveryStatus.QUEUED
        }

        when: "all notifications of the user are removed"
        adminNotificationResource.removeNotificationsOfUser(null, keycloakDemoSetup.testuser3Id)

        then: "there should be no alert"
        testuser3NotificationResource.getQueuedNotificationsOfCurrentUser(null).size() == 0

        when: "a device registration is removed"
        adminNotificationResource.deleteDeviceToken(null, keycloakDemoSetup.testuser3Id, "device456")
        tokens = adminNotificationResource.getDeviceTokens(null, keycloakDemoSetup.testuser3Id)

        then: "only the other registration should be present"
        tokens.size() == 1
        tokens[0].id.deviceId == "device123"
        tokens[0].deviceType == "ANDROID"
        tokens[0].token == "token123"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
