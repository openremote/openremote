package org.openremote.test.rules.residence

import com.google.common.collect.Lists
import com.google.firebase.messaging.Message
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.notification.PushNotificationHandler
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.console.ConsoleRegistration
import org.openremote.model.console.ConsoleResource
import org.openremote.model.notification.AbstractNotificationMessage
import org.openremote.model.notification.Notification
import org.openremote.model.notification.NotificationSendResult
import org.openremote.model.notification.PushNotificationMessage
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.ObjectValue
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.manager.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.asset.AssetResource.Util.WRITE_ATTRIBUTE_HTTP_METHOD
import static org.openremote.model.asset.AssetResource.Util.getWriteAttributeUrl
import static org.openremote.model.value.Values.parse

class ResidenceNotifyAlarmTriggerTest extends Specification implements ManagerContainerTrait {

    def "Trigger notification when presence is detected and alarm enabled"() {

        def notificationIds = []
        def targetTypes = []
        def targetIds = []
        def messages = []

        given: "a mock push notification handler"
        PushNotificationHandler mockPushNotificationHandler = Spy(PushNotificationHandler) {
            isValid() >> true

            sendMessage(_ as Long, _ as Notification.Source, _ as String, _ as Notification.TargetType, _ as String, _ as AbstractNotificationMessage) >> {
                id, source, sourceId, targetType, targetId, message ->
                    notificationIds << id
                    targetTypes << targetType
                    targetIds << targetId
                    messages << message
                    callRealMethod()
            }

            // Assume sent to FCM
            sendMessage(_ as Message) >> {
                message -> return NotificationSendResult.success()
            }
        }

        and: "the container environment is started with the mock handler"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
        def serverPort = findEphemeralPort()
        def services = Lists.newArrayList(defaultServices())
        services.removeIf { it instanceof PushNotificationHandler }
        services.add(mockPushNotificationHandler)
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), services)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Notify Alarm Trigger", Ruleset.Lang.GROOVY, getClass().getResource("/demo/rules/DemoResidenceNotifyAlarmTrigger.groovy").text,
                managerDemoSetup.apartment1Id,
                false,
                false
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the alarm enabled, presence detected flag of room should not be set"
        def apartment1Asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
        assert !apartment1Asset.getAttribute("alarmEnabled").get().valueAsBoolean.orElse(null)
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").orElse(null).valueAsBoolean.orElse(null)

        and: "an authenticated test user"
        def realm = "tenantA"
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the notification and console resources"
        def authenticatedConsoleResource = getClientApiTarget(serverUri(serverPort), realm, accessToken).proxy(ConsoleResource.class)

        when: "a console is registered by the test user"
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
                                (ObjectValue) parse("{token: \"23123213ad2313b0897efd\"}").orElse(null)
                        ))
                    }
                },
                "",
                ["manager"] as String)
        def returnedConsoleRegistration = authenticatedConsoleResource.register(null, consoleRegistration)

        then: "the console should be registered"
        assert returnedConsoleRegistration.id != null

        when: "the alarm is enabled"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1Id, "alarmEnabled", Values.create(true), getClockTimeOf(container)
        ))

        then: "that value should be stored"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("alarmEnabled").get().valueAsBoolean
        }

        when: "the presence is detected in Living room of apartment 1"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "presenceDetected", Values.create(true), getClockTimeOf(container)
        ))

        then: "that value should be stored"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean.orElse(null)
        }

        and: "the user should be notified"
        conditions.eventually {
            assert notificationIds.size() == 1
            assert targetTypes[0] == Notification.TargetType.ASSET
            assert targetIds[0] == returnedConsoleRegistration.id
            def pushMessage = (PushNotificationMessage) messages[0]
            assert pushMessage.title == "Apartment Alarm"
            assert pushMessage.body.startsWith("Aanwezigheid in Living Room")
            assert pushMessage.buttons.size() == 2
            assert pushMessage.buttons[0].title == "Details"
            assert pushMessage.buttons[0].action.url == "#security"
            assert pushMessage.buttons[0].action.httpMethod == null
            assert pushMessage.buttons[0].action.data == null
            assert !pushMessage.buttons[0].action.openInBrowser
            assert !pushMessage.buttons[0].action.silent
            assert pushMessage.buttons[1].title == "Alarm uit"
            assert pushMessage.buttons[1].action.url == getWriteAttributeUrl(new AttributeRef(managerDemoSetup.apartment1Id, "alarmEnabled"))
            assert pushMessage.buttons[1].action.httpMethod == WRITE_ATTRIBUTE_HTTP_METHOD
            assert !Values.getBoolean(pushMessage.buttons[1].action.data).orElse(true)
            assert !pushMessage.buttons[1].action.openInBrowser
            assert pushMessage.buttons[1].action.silent
        }

        when: "time moves on and other events happen that trigger evaluation in the rule engine"
        advancePseudoClock(20, TimeUnit.MINUTES, container)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(444), getClockTimeOf(container)
        ))

        then: "still only one notification should have been sent"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("co2Level").get().valueAsInteger.orElse(null) == 444
            assert notificationIds.size() == 1
        }

        when: "time moves on more than the alarm silence duration"
        advancePseudoClock(30, TimeUnit.MINUTES, container)

        then: "another notification should be sent"
        conditions.eventually {
            assert notificationIds.size() == 2
        }

        when: "the presence is no longer triggered in Living room of apartment 1"
        advancePseudoClock(5, TimeUnit.SECONDS, container)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "presenceDetected", Values.create(false), getClockTimeOf(container)
        ))

        then: "that value should be stored"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().valueAsBoolean.orElse(null)
        }

        when: "time moves on"
        advancePseudoClock(40, TimeUnit.MINUTES, container)

        then: "still only two notifications should have been sent"
        conditions.eventually {
            assert notificationIds.size() == 2
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}