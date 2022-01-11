package org.openremote.test.notification

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.google.firebase.messaging.Message
import org.openremote.container.web.WebService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.console.ConsoleResourceImpl
import org.openremote.manager.notification.EmailNotificationHandler
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.notification.PushNotificationHandler
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.model.query.UserQuery
import org.openremote.model.query.filter.TenantPredicate
import org.openremote.model.util.TextUtil
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.console.ConsoleRegistration
import org.openremote.model.console.ConsoleResource
import org.openremote.model.notification.*
import org.openremote.test.ManagerContainerTrait
import org.simplejavamail.email.Email
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.WebApplicationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.notification.PushNotificationAction.writeAttributeValueAction
import static org.openremote.model.util.ValueUtil.parse

class NotificationTest extends Specification implements ManagerContainerTrait {

    def "Check push notification functionality"() {

        List<String> notificationIds = []
        List<Notification.TargetType> notificationTargetTypes = []
        List<String> notificationTargetIds = []
        List<AbstractNotificationMessage> notificationMessages = []

        given: "the container environment is started with the mock handler"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def notificationService = container.getService(NotificationService.class)
        def pushNotificationHandler = container.getService(PushNotificationHandler.class)
        def consoleResource = (ConsoleResourceImpl)container.getService(WebService.class).getApiSingletons().find {it instanceof ConsoleResourceImpl}

        and: "the clock is stopped for testing purposes"
        stopPseudoClock()

        and: "a mock push notification handler"
        PushNotificationHandler mockPushNotificationHandler = Spy(pushNotificationHandler)
        mockPushNotificationHandler.isValid() >> true
        mockPushNotificationHandler.sendMessage(_ as Long, _ as Notification.Source, _ as String, _ as Notification.Target, _ as AbstractNotificationMessage) >> {
                id, source, sourceId, target, message ->
                    notificationIds << id
                    notificationTargetTypes << target.type
                    notificationTargetIds << target.id
                    notificationMessages << message
                    callRealMethod()
            }
        // Assume sent to FCM
        mockPushNotificationHandler.sendMessage(_ as Message) >> {
                message -> return NotificationSendResult.success()
            }

        notificationService.notificationHandlerMap.put(pushNotificationHandler.getTypeName(), mockPushNotificationHandler)

        and: "an authenticated test user"
        def realm = keycloakTestSetup.tenantBuilding.realm
        def testuser1AccessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            "testuser1",
            "testuser1"
        ).token
        def testuser2AccessToken = authenticate(
            container,
            realm,
            KEYCLOAK_CLIENT_ID,
            "testuser2",
            "testuser2"
        ).token
        def testuser3AccessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
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
                                        managerTestSetup.apartment1LivingroomId,
                                        "alarmEnabled"
                                ),
                                false), null, null), null, null, null)

        and: "the notification resource"
        def testuser1NotificationResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, testuser1AccessToken).proxy(NotificationResource.class)
        def testuser2NotificationResource = getClientApiTarget(serverUri(serverPort), realm, testuser2AccessToken).proxy(NotificationResource.class)
        def testuser3NotificationResource = getClientApiTarget(serverUri(serverPort), realm, testuser3AccessToken).proxy(NotificationResource.class)
        def adminNotificationResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(NotificationResource.class)
        def anonymousNotificationResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.tenantBuilding.realm).proxy(NotificationResource.class)
        def testuser1ConsoleResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, testuser1AccessToken).proxy(ConsoleResource.class)
        def testuser2ConsoleResource = getClientApiTarget(serverUri(serverPort), realm, testuser2AccessToken).proxy(ConsoleResource.class)
        def testuser3ConsoleResource = getClientApiTarget(serverUri(serverPort), realm, testuser3AccessToken).proxy(ConsoleResource.class)
        def adminConsoleResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(ConsoleResource.class)
        def anonymousConsoleResource = getClientApiTarget(serverUri(serverPort), realm).proxy(ConsoleResource.class)
        SentNotification[] notifications = []

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
                                false,
                                false,
                                null
                        ))
                        put("push", new ConsoleProvider(
                                "fcm",
                                true,
                                true,
                                true,
                                true,
                                false,
                                (ObjectNode) parse("{\"token\": \"23123213ad2313b0897efd\"}").orElse(null)
                        ))
                    }
                },
                "",
                ["manager"] as String[])
        def testuser2Console = testuser2ConsoleResource.register(null, consoleRegistration)
        def testuser3Console1 = testuser3ConsoleResource.register(null, consoleRegistration)
        def testuser3Console2 = testuser3ConsoleResource.register(null, consoleRegistration)
        def adminConsole = adminConsoleResource.register(null, consoleRegistration)
        def anonymousConsole = anonymousConsoleResource.register(null, consoleRegistration)

        then: "the consoles should have been created"
        testuser2Console.id != null
        testuser3Console1.id != null
        testuser3Console2.id != null
        adminConsole.id != null
        anonymousConsole.id != null

        when: "the admin user sends a push notification to an entire realm"
        notification.targets = [new Notification.Target(Notification.TargetType.TENANT, keycloakTestSetup.tenantBuilding.realm)]
        adminNotificationResource.sendNotification(null, notification)

        then: "all consoles in that realm should have been sent a notification (excluding testuser2 as they have disabled push notifications)"
        conditions.eventually {
            assert notificationIds.size() == 3
            assert notificationTargetTypes.count { t -> t == Notification.TargetType.ASSET } == 3
            assert !notificationTargetIds.contains(testuser2Console.id)
            assert notificationTargetIds.contains(testuser3Console1.id)
            assert notificationTargetIds.contains(testuser3Console2.id)
            assert notificationTargetIds.contains(anonymousConsole.id)
            assert notificationMessages.count { m -> m instanceof PushNotificationMessage && m.title == "Test Action" && m.body == "Click to cancel" && m.action != null } == 3
        }

        when: "a regular user sends a push notification to an entire realm"
        testuser2NotificationResource.sendNotification(null, notification)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "the admin user sends a notification to a user in a different realm with emailNotificationsDisabled set to true"
        notification.targets = [new Notification.Target(Notification.TargetType.USER, keycloakTestSetup.testuser2Id)]
        advancePseudoClock(1, TimeUnit.HOURS, container)
        adminNotificationResource.sendNotification(null, notification)

        then: "no notification should have been sent"
        ex = thrown()
        ex.response.status == 400
        notificationIds.size() == 3

        when: "the admin user sends a notification to a user in a different realm with emailNotificationsDisabled set to false"
        notification.targets = [new Notification.Target(Notification.TargetType.USER, keycloakTestSetup.testuser3Id)]
        advancePseudoClock(1, TimeUnit.HOURS, container)
        adminNotificationResource.sendNotification(null, notification)

        then: "the notification should have been sent"
        conditions.eventually {
            assert notificationIds.size() == 5
        }

        when: "a regular user sends a push notification to a user in a different realm"
        notification.targets = [new Notification.Target(Notification.TargetType.USER, keycloakTestSetup.testuser2Id)]
        testuser1NotificationResource.sendNotification(null, notification)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a restricted user sends a push notification to another user in the same realm"
        notification.targets = [new Notification.Target(Notification.TargetType.USER, keycloakTestSetup.testuser2Id)]
        testuser3NotificationResource.sendNotification(null, notification)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an anonymous user sends a push notification to a user"
        anonymousNotificationResource.sendNotification(null, notification)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "the admin user sends a push notification to the console assets in building realm"
        notification.targets = [new Notification.Target(Notification.TargetType.ASSET, testuser2Console.id),
                                new Notification.Target(Notification.TargetType.ASSET, testuser3Console1.id),
                                new Notification.Target(Notification.TargetType.ASSET, testuser3Console2.id),
                                new Notification.Target(Notification.TargetType.ASSET, anonymousConsole.id)]
        advancePseudoClock(1, TimeUnit.HOURS, container)
        adminNotificationResource.sendNotification(null, notification)

        then: "the notification should have been sent (not to testuser2)"
        conditions.eventually {
            assert notificationIds.size() == 8
            assert !notificationIds.contains(testuser2Console.id)
        }

        when: "a regular user sends a push notification to the console assets in a different realm"
        testuser1NotificationResource.sendNotification(null, notification)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a regular user sends a push notification to the console assets in the same realm"
        advancePseudoClock(1, TimeUnit.HOURS, container)
        testuser2NotificationResource.sendNotification(null, notification)

        then: "the notification should have been sent (not to testuser2)"
        conditions.eventually {
            assert notificationIds.size() == 11
            assert !notificationIds.contains(testuser2Console.id)
        }

        when: "a notification is sent using the same mechanism as an asset ruleset"
        notificationService.sendNotification(notification, Notification.Source.ASSET_RULESET, consoleResource.getConsoleParentAssetId(realm))

        then: "the notification should have been sent (not to testuser2)"
        conditions.eventually {
            assert notificationIds.size() == 14
            assert !notificationIds.contains(testuser2Console.id)
        }

        when: "a restricted user sends a push notification to the console assets in the same realm"
        testuser3NotificationResource.sendNotification(null, notification)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a restricted user sends a push notification to some consoles linked to them and some not linked to them"
        notification.targets = [new Notification.Target(Notification.TargetType.ASSET, testuser2Console.id),
                                new Notification.Target(Notification.TargetType.ASSET, testuser3Console1.id),
                                new Notification.Target(Notification.TargetType.ASSET, testuser3Console2.id),
                                new Notification.Target(Notification.TargetType.ASSET, anonymousConsole.id)]
        testuser3NotificationResource.sendNotification(null, notification)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        and: "no new notifications should have been sent"
        notificationIds.size() == 14

        when: "a restricted user sends a push notification to some consoles linked to them"
        advancePseudoClock(1, TimeUnit.HOURS, container)
        notification.targets = [new Notification.Target(Notification.TargetType.ASSET, testuser3Console1.id),
                                new Notification.Target(Notification.TargetType.ASSET, testuser3Console2.id)]
        testuser3NotificationResource.sendNotification(null, notification)

        then: "the notifications should have been sent"
        conditions.eventually {
            assert notificationIds.size() == 16
        }

        // -----------------------------------------------
        //    Check notification resource
        // -----------------------------------------------

        and: "all notifications sent to consoles in the building realm should be available via the REST API"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser2Console.id).length == 0
            notifications = adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id)
            assert notifications.length == 6
            assert notifications.every {n ->
                PushNotificationMessage pushMessage = n.message as PushNotificationMessage
                pushMessage.getTitle() == "Test Action" &&
                        pushMessage.getBody() == "Click to cancel" &&
                        pushMessage.getAction() != null &&
                        n.deliveredOn == null &&
                        n.acknowledgedOn == null
            }
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 6
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, anonymousConsole.id).length == 4
        }

        when: "the admin user marks a Building console notification as delivered and requests the notifications for Building consoles"
        adminNotificationResource.notificationDelivered(null, testuser3Console1.id, notifications.find {n -> n.targetId == testuser3Console1.id}.id)

        then: "the notification should have been updated"
        conditions.eventually {
            notifications = adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id)
            assert notifications.length == 6
            assert notifications.count {n -> n.deliveredOn != null} == 1
        }

        when: "the admin user marks a Building console notification as delivered and requests the notifications for Building consoles"
        adminNotificationResource.notificationAcknowledged(null, testuser3Console1.id, notifications.find {n -> n.targetId == testuser3Console1.id && n.deliveredOn != null}.id, new TextNode("dismissed"))

        then: "the notification should have been updated"
        conditions.eventually {
            notifications = adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id)
            assert notifications.length == 6
            assert notifications.count {n -> n.deliveredOn != null && n.acknowledgedOn != null && n.acknowledgement == "\"dismissed\""} == 1
        }

        when: "a regular user marks a console notification from another realm as delivered"
        testuser1NotificationResource.notificationDelivered(null, testuser3Console1.id, notifications.find {n -> n.targetId == testuser3Console1.id}.id)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a regular user marks a console notification for their own console as delivered"
        testuser3NotificationResource.notificationDelivered(null, testuser3Console1.id, notifications.find {n -> n.targetId == testuser3Console1.id}.id)

        then: "the notification should have been updated"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).count {it.deliveredOn != null} == 1
        }

// TODO: Update once console permissions model finalised
//        when: "an anonymous user marks a console notification from another console as delivered"
//        anonymousNotificationResource.notificationDelivered(null, testuser3Console1.id, notifications.find {n -> n.targetId == testuser3Console1.id}.id)
//
//        then: "access should be forbidden"
//        ex = thrown()
//        ex.response.status == 403

        when: "an anonymous user marks a console notification for their own console as delivered"
        notifications = adminNotificationResource.getNotifications(null, null, null, null, null, null, null, anonymousConsole.id)
        anonymousNotificationResource.notificationDelivered(null, anonymousConsole.id, notifications.find {n -> n.targetId == anonymousConsole.id}.id)

        then: "the notification should have been updated"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, anonymousConsole.id).count {it.deliveredOn != null} == 1
        }

        when: "a regular user tries to remove notifications"
        testuser1NotificationResource.removeNotifications(null, null, PushNotificationMessage.TYPE, null, null, MASTER_REALM, null, null)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a restricted user tries to remove notifications"
        testuser3NotificationResource.removeNotifications(null, null, PushNotificationMessage.TYPE, null, null, realm, null, null)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "the admin user removes notifications by timestamp and the notifications are retrieved again"
        notifications = adminNotificationResource.getNotifications(null, null, PushNotificationMessage.TYPE, null, null, null, null, null).reverse(true)
        def sentNotification = notifications[0]
        def removeCount = notifications.count {it.sentOn >= sentNotification.sentOn}
        adminNotificationResource.removeNotifications(null, null, PushNotificationMessage.TYPE, sentNotification.sentOn.getTime(), null, null, null, null)

        then: "notifications sent after or at that time should have been removed"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, PushNotificationMessage.TYPE, null, null, null, null, null).length == (notifications.length - removeCount)
        }

        when: "the admin user removes notifications sent to specific console assets without other constraints and the notifications are retrieved again"
        adminNotificationResource.removeNotifications(null, null, null, null, null, null, null, testuser3Console1.id)
        adminNotificationResource.removeNotifications(null, null, null, null, null, null, null, testuser3Console2.id)

        then: "all notifications sent to those consoles should have been removed"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 0
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 0
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, anonymousConsole.id).length == 4
        }

        when: "the admin user removes notifications by type and the notifications are retrieved again"
        adminNotificationResource.removeNotifications(null, null, PushNotificationMessage.TYPE, null, null, null, null, null)

        then: "the notifications should have been removed"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, PushNotificationMessage.TYPE, null, null, null, null, anonymousConsole.id).length == 0
        }

        when: "the admin user removes notifications without sufficient constraints"
        adminNotificationResource.removeNotifications(null, null, null, null, null, null, null, null)

        then: "request should not be allowed"
        ex = thrown()
        ex.response.status == 400

        // -----------------------------------------------
        //    Check notification repeat frequency
        // -----------------------------------------------

        when: "a repeat frequency is set and a notification is sent"
        // Move clock to beginning of next hour
        def advancement = Instant.ofEpochMilli(getClockTimeOf(container))
                                       .truncatedTo(ChronoUnit.HOURS)
                                       .plus(59, ChronoUnit.MINUTES)

        advancePseudoClock(ChronoUnit.MILLIS.between(Instant.ofEpochMilli(getClockTimeOf(container)), advancement), TimeUnit.MILLISECONDS, container)
        notification.setRepeatFrequency(RepeatFrequency.HOURLY)
        testuser3NotificationResource.sendNotification(null, notification)

        then: "the notifications should have been sent (to testuser3 consoles)"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 1
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 1
        }

        when: "a repeat frequency is set and a notification with the same name and scope as a previous notification is sent within the repeat window"
        testuser3NotificationResource.sendNotification(null, notification)

        then: "no new notifications should have been sent"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 1
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 1
        }

        when: "a repeat frequency is set and a notification with the same name but different scope is sent within the repeat window"
        adminNotificationResource.sendNotification(null, notification)

        then: "new notifications should have been sent"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 2
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 2
        }

        when: "time advances less than the repeat frequency and the notification is sent again"
        advancePseudoClock(30, TimeUnit.SECONDS, container)
        adminNotificationResource.sendNotification(null, notification)

        then: "no new notifications should have been sent"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 2
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 2
        }

        when: "time advances more than the repeat frequency and the notification is sent again"
        advancePseudoClock(2, TimeUnit.MINUTES, container)
        adminNotificationResource.sendNotification(null, notification)

        then: "new notifications should have been sent"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 3
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 3
        }

        when: "a repeat interval is used and a notification with the same name and scope is sent within the repeat window"
        notification.setRepeatInterval("P1M")
        advancePseudoClock(10, TimeUnit.DAYS, container)
        adminNotificationResource.sendNotification(null, notification)

        then: "no new notifications should have been sent"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 3
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 3
        }

        when: "time advances more than the repeat interval and the notification is sent again"
        advancePseudoClock(25, TimeUnit.DAYS, container)
        adminNotificationResource.sendNotification(null, notification)

        then: "new notifications should have been sent"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console1.id).length == 4
            assert adminNotificationResource.getNotifications(null, null, null, null, null, null, null, testuser3Console2.id).length == 4
        }

        and: "notifications are retrieved only for the past day only the relevant notifications should have been returned"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, getClockTimeOf(container)-(3600000*24), null, null, null, testuser3Console1.id).length == 1
            assert adminNotificationResource.getNotifications(null, null, null, getClockTimeOf(container)-(3600000*24), null, null, null, testuser3Console2.id).length == 1
        }

        and: "notifications are retrieved only for the past 40 days only the relevant notifications should have been returned"
        conditions.eventually {
            assert adminNotificationResource.getNotifications(null, null, null, getClockTimeOf(container)-(3600000L*24*40), null, null, null, testuser3Console1.id).length == 4
            assert adminNotificationResource.getNotifications(null, null, null, getClockTimeOf(container)-(3600000L*24*40), null, null, null, testuser3Console2.id).length == 4
        }

        cleanup: "the mock is removed"
        notificationService.notificationHandlerMap.put(pushNotificationHandler.getTypeName(), pushNotificationHandler)
    }

    def "Check email notification functionality"() {

        List<Email> sentEmails = []

        given: "the container environment is started with the mock handler"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def notificationService = container.getService(NotificationService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def emailNotificationHandler = container.getService(EmailNotificationHandler.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        and: "a mock email notification handler"
        EmailNotificationHandler mockEmailNotificationHandler = Spy(emailNotificationHandler)
        mockEmailNotificationHandler.isValid() >> true

        // Log email and assume sent to SMTP Server
        mockEmailNotificationHandler.sendMessage(_ as Email) >> {
            Email email ->
                sentEmails << email
                return NotificationSendResult.success()
        }
        notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), mockEmailNotificationHandler)

        expect: "the demo users to be created"
        conditions.eventually {
            def users = identityService.getIdentityProvider().queryUsers(new UserQuery().tenant(new TenantPredicate(keycloakTestSetup.tenantBuilding.realm)).select(new UserQuery.Select().excludeServiceUsers(true)))
            assert users.size() == 3
            assert users.count { !TextUtil.isNullOrEmpty(it.email)} == 3
        }

        when: "an email notification is sent to a tenant through same mechanism as rules"
        def notification = new Notification(
                "Test",
                new EmailNotificationMessage().setSubject("Test").setText("Hello world!"),
                Collections.singletonList(new Notification.Target(Notification.TargetType.TENANT, managerTestSetup.realmBuildingTenant)), null, null)
        notificationService.sendNotification(notification, Notification.Source.TENANT_RULESET, managerTestSetup.realmBuildingTenant)

        then: "the email should have been sent to the tenant users"
        conditions.eventually {
            assert sentEmails.size() >= 2
            assert sentEmails.every {it.getPlainText() == "Hello world!"}
            assert sentEmails.every {it.getSubject() == "Test"}
            assert sentEmails.any { it.getRecipients().size() == 1 && it.getRecipients().get(0).address == "testuser2@openremote.local"}
            assert !sentEmails.any { it.getRecipients().size() == 1 && it.getRecipients().get(0).address == "testuser3@openremote.local"}
            assert sentEmails.any { it.getRecipients().size() == 1 && it.getRecipients().get(0).address == "building@openremote.local"}
        }

        when: "an email attribute is added to an asset"
        def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId)
        kitchen.setEmail("kitchen@openremote.local")
        kitchen = assetStorageService.merge(kitchen)

        and: "an email notification is sent to a parent asset"
        ((EmailNotificationMessage)notification.message).subject = "Test 2"
        notification.setTargets([new Notification.Target(Notification.TargetType.ASSET, managerTestSetup.apartment1Id)])
        notificationService.sendNotification(notification)

        then: "the child asset with the email attribute should have been sent an email"
        conditions.eventually {
            assert sentEmails.size() >= 3
            assert sentEmails.any { it.getSubject() == "Test 2"}
            assert sentEmails.any { it.getRecipients().size() == 1 && it.getRecipients().get(0).address == "kitchen@openremote.local"}
        }

        when: "an email is sent to a custom target"
        ((EmailNotificationMessage)notification.message).subject = "Test Custom"
        notification.setTargets([new Notification.Target(Notification.TargetType.CUSTOM, "custom1@openremote.local;to:custom2@openremote.local;cc:custom3@openremote.local;bcc:custom4@openremote.local")])
        notificationService.sendNotification(notification)

        then: "the email should have been sent to all custom recipients"
        conditions.eventually {
            assert sentEmails.size() >= 4
            assert sentEmails.any { it.getSubject() == "Test Custom" && it.getRecipients().size() == 4}
            assert sentEmails.any { it.getSubject() == "Test Custom" && it.getRecipients().any{it.type == javax.mail.Message.RecipientType.TO && it.address == "custom1@openremote.local"}}
            assert sentEmails.any { it.getSubject() == "Test Custom" && it.getRecipients().any{it.type == javax.mail.Message.RecipientType.TO && it.address == "custom2@openremote.local"}}
            assert sentEmails.any { it.getSubject() == "Test Custom" && it.getRecipients().any{it.type == javax.mail.Message.RecipientType.CC && it.address == "custom3@openremote.local"}}
            assert sentEmails.any { it.getSubject() == "Test Custom" && it.getRecipients().any{it.type == javax.mail.Message.RecipientType.BCC && it.address == "custom4@openremote.local"}}
        }

        cleanup: "the mock is removed"
        notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), emailNotificationHandler)
    }
}
