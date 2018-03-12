package org.openremote.test.rules.residence

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.notification.FCMDeliveryService
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.notification.NotificationResource
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.notification.ActionType
import org.openremote.model.notification.AlertAction
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.manager.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID

@Ignore
// TODO Broken
class ResidenceNotifyAlarmTriggerTest extends Specification implements ManagerContainerTrait {

    def "Trigger notification when presence is detected and alarm enabled"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine

        and: "a mock FCM delivery service"
        def mockFCMDeliveryService = Spy(FCMDeliveryService, constructorArgs: [container]) {
            // Always "deliver" to FCM
            sendPickupSignalThroughFCM(*_) >> {
                return true
            }
        }
        def notificationService = container.getService(NotificationService)
        notificationService.fcmDeliveryService = mockFCMDeliveryService

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Notify Alarm Trigger",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/demo/rules/DemoResidenceNotifyAlarmTrigger.groovy").text,
                Ruleset.Lang.GROOVY
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
        def realm = "customerA"
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the notification resource"
        def notificationResource = getClientTarget(serverUri(serverPort), realm, accessToken).proxy(NotificationResource.class)

        when: "the notification tokens of some devices are stored"
        notificationResource.storeDeviceToken(null, "device123", "token123", "ANDROID")

        then: "the tokens should be in the database"
        notificationService.findDeviceToken("device123", keycloakDemoSetup.testuser3Id).get().token == "token123"

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
            def alerts = notificationResource.getQueuedNotificationsOfCurrentUser(null)
            assert alerts.size() == 1
            assert alerts[0].title == "Apartment Alarm"
            assert alerts[0].message.startsWith("Aanwezigheid in Living Room")
            assert alerts[0].actions.length() == 2
            assert alerts[0].actions.getObject(0).orElse(null) == new AlertAction("Details", ActionType.LINK).objectValue
            assert alerts[0].appUrl == "#security"
            assert alerts[0].actions.getObject(1).orElse(null) == new AlertAction("Alarm uit", ActionType.ACTUATOR, managerDemoSetup.apartment1Id, "alarmEnabled", Values.create(false).toJson()).objectValue
        }

        when: "time moves on and other events happen that trigger evaluation in the rule engine"
        advancePseudoClock(20, TimeUnit.MINUTES, container)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(444), getClockTimeOf(container)
        ))

        then: "we have still only one notification pending"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("co2Level").get().valueAsInteger.orElse(null) == 444
            def alerts = notificationResource.getQueuedNotificationsOfCurrentUser(null)
            assert alerts.size() == 1
        }

        when: "time moves on"
        advancePseudoClock(30, TimeUnit.MINUTES, container)

        then: "we notify again and now have two notifications pending"
        conditions.eventually {
            def alerts = notificationResource.getQueuedNotificationsOfCurrentUser(null)
            assert alerts.size() == 2
        }

        when: "the presence os no longer in Living room of apartment 1"
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

        then: "we have still only two notifications pending"
        conditions.eventually {
            def alerts = notificationResource.getQueuedNotificationsOfCurrentUser(null)
            assert alerts.size() == 2
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}