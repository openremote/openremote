package org.openremote.test.notification

import org.openremote.container.timer.TimerService
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.notification.PushNotificationHandler
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.console.ConsoleRegistration
import org.openremote.model.console.ConsoleResource
import org.openremote.model.notification.Notification
import org.openremote.model.notification.NotificationResource
import org.openremote.model.notification.NotificationSendResult
import org.openremote.model.notification.PushNotificationMessage
import org.openremote.model.query.UserQuery
import org.openremote.model.query.filter.UserAssetPredicate
import org.openremote.model.value.ObjectValue
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.notification.PushNotificationAction.writeAttributeValueAction
import static org.openremote.model.value.Values.parse

@Ignore
class NotificationResourceTest extends Specification implements ManagerContainerTrait {

    def "Check full notification resource behaviour"() {

        def notificationIds = []
        def targetTypes = []
        def targetIds = []
        def messages = []

        given: "a mock push notification handler"
        PushNotificationHandler mockPushNotificationHandler = Spy(PushNotificationHandler) {
            isValid() >> true

            // Assume sent to FCM
            sendMessage(*_) >> {
                id, targetType, targetId, message ->
                    notificationIds << id
                    targetTypes << targetType
                    targetIds << targetId
                    messages << message
                    return NotificationSendResult.success()
            }
        }

        and: "the container environment is started with the mock handler"
        def serverPort = findEphemeralPort()
        def services = defaultServices()
        ((NotificationService)services.find {it instanceof NotificationService}).notificationHandlerMap.put("push", mockPushNotificationHandler)
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), services)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def notificationService = container.getService(NotificationService.class)

        and: "an authenticated test user"
        def realm = keycloakDemoSetup.customerATenant.realm
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

        def notification = new Notification("TestAction",
                                           new PushNotificationMessage("Test Action",
                                                                       "Click to cancel",
                                                                       writeAttributeValueAction(
                                                                           new AttributeRef(
                                                                               managerDemoSetup.apartment1LivingroomId,
                                                                               "alarmEnabled"
                                                                           ),
                                                                           Values.create(false)),null, null), null)

        and: "the notification resource"
        def testuser2NotificationResource = getClientTarget(serverUri(serverPort), realm, testuser2AccessToken).proxy(NotificationResource.class)
        def testuser3NotificationResource = getClientTarget(serverUri(serverPort), realm, testuser3AccessToken).proxy(NotificationResource.class)
        def adminNotificationResource = getClientTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(NotificationResource.class)
        def anonymousNotificationResource = getClientTarget(serverUri(serverPort), keycloakDemoSetup.customerATenant.realm).proxy(NotificationResource.class)
        def testuser2ConsoleResource = getClientTarget(serverUri(serverPort), realm, testuser2AccessToken).proxy(ConsoleResource.class)
        def testuser3ConsoleResource = getClientTarget(serverUri(serverPort), realm, testuser3AccessToken).proxy(ConsoleResource.class)
        def adminConsoleResource = getClientTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(ConsoleResource.class)
        def anonymousConsoleResource = getClientTarget(serverUri(serverPort), realm).proxy(ConsoleResource.class)

        when: "various consoles are registered"
        def consoleRegistration = new ConsoleRegistration(null,
                                                          "Test Console",
                                                          "1.0",
                                                          "Android 7.0",
                                                          new HashMap<String, ConsoleProvider>() {
                                                              {
                                                                  put("geofence", new ConsoleProvider(
                                                                      ORConsoleGeofenceAssetAdapter.NAME,
                                                                      true,
                                                                      false,
                                                                      false,
                                                                      null
                                                                  ))
                                                                  put("push", new ConsoleProvider(
                                                                      "fcm",
                                                                      true,
                                                                      true,
                                                                      false,
                                                                      (ObjectValue)parse("{token: \"23123213ad2313b0897efd\"}").orElse(null)
                                                                  ))
                                                              }
                                                          })
        def testuser2Console = testuser2ConsoleResource.register(null, consoleRegistration)
        def testuser3Console = testuser3ConsoleResource.register(null, consoleRegistration)
        def adminConsole = adminConsoleResource.register(null, consoleRegistration)
        def anonymousConsole = anonymousConsoleResource.register(null, consoleRegistration)

        then: "the consoles should have been created"
        testuser2Console.id != null
        testuser3Console.id != null
        adminConsole.id != null
        anonymousConsole.id != null

        when: "the admin user sends a push notification to an entire realm"

        then: "all consoles in that realm should have been sent a notification"

        when: "a regular user sends a push notification to an entire realm"

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "the admin user sends a notification to testuser2"
        notification.setTargets(new Notification.Targets(User))
        adminNotificationResource.sendNotification(null, notification)

        then: "the notification should have been sent"
        notificationIds.size() == 1

        when: "a regular user sends a push notification to another realm user"

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a restricted user sends a push notification to another realm user"

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "the admin user sends a push notification to the Consoles asset in customerA realm"

        then: "the notification should have been sent"

        when: "a regular user sends a push notification to the Consoles asset in customerA realm"

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a restricted user sends a push notification to the Consoles asset in customerA realm"

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a regular user is linked to the Consoles asset in customerA realm and they send a notification to this asset"

        then: "the notification should have been sent"







        and: ""

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
//
//        and: "must find registered test user if limiting to same tenant"
//        notificationService.findAllUsersWithToken(new UserQuery().tenant(new UserQuery.TenantPredicate().realmId(keycloakDemoSetup.customerATenant.id))).size() == 1
//        def foundUser2 = notificationService.findAllUsersWithToken(new UserQuery().tenant(new UserQuery.TenantPredicate().realmId(keycloakDemoSetup.customerATenant.id))).first()
//        foundUser2 == keycloakDemoSetup.testuser3Id
//
//        and: "must not find registered test user if limiting to different tenant"
//        notificationService.findAllUsersWithToken(new UserQuery().tenant(new UserQuery.TenantPredicate().realmId(keycloakDemoSetup.customerBTenant.id))).isEmpty()

        and: "must find registered test user if limiting to asset user has access to"
        notificationService.findAllUsersWithToken(new UserQuery().asset(new UserAssetPredicate().id(managerDemoSetup.apartment1Id))).size() == 1
        def foundUser3 = notificationService.findAllUsersWithToken(new UserQuery().asset(new UserAssetPredicate().id(managerDemoSetup.apartment1Id))).first()
        foundUser3 == keycloakDemoSetup.testuser3Id

        and: "must not find registered test user if limiting to asset user has not access to"
        notificationService.findAllUsersWithToken(new UserQuery().asset(new UserAssetPredicate().id(managerDemoSetup.apartment3Id))).isEmpty()

        when: "the notification token of some device is updated"
        testuser3NotificationResource.storeDeviceToken(null, "device456", "token789", "ANDROID")

        then: "the updated token should be in the database"
        def thirdToken = notificationService.findDeviceToken("device456", keycloakDemoSetup.testuser3Id)
        thirdToken.get().token == "token789"
        notificationService.findAllTokenForUser(keycloakDemoSetup.testuser3Id).size() == 2

        when: "the notification is stored"
        testuser3NotificationResource.sendNotification(null, notification)
        def alerts = testuser3NotificationResource.getQueuedNotificationsOfCurrentUser(null)

        then: "it should be one notification pending"
        alerts.size() == 1

        when: "a user tries to store a notification for another user"
        testuser2NotificationResource.storeNotificationForUser(null, keycloakDemoSetup.testuser3Id, notification)

        then: "an exception should be thrown"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "a user tries to acknowledge a notification she doesn't own"
        testuser2NotificationResource.notificationAcknowledged(null, alerts.get(0).id)

        then: "an exception should be thrown"
        ex = thrown()
        ex.response.status == 403

        when: "the notification is acknowledged"
        testuser3NotificationResource.notificationAcknowledged(null, alerts.get(0).id)

        then: "there should be no alert"
        testuser3NotificationResource.getQueuedNotificationsOfCurrentUser(null).size() == 0

        when: "the notification is stored"
        testuser3NotificationResource.sendNotification(null, notification)
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
        adminNotificationResource.removeNotifications(null, keycloakDemoSetup.testuser3Id)

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
