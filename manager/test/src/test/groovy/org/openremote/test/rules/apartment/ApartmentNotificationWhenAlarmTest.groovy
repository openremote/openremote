package org.openremote.test.rules.apartment

import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.notification.NotificationService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.notification.NotificationResource
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID

class ApartmentNotificationWhenAlarmTest  extends Specification implements ManagerContainerTrait {

    def "Trigger notification when presence is detected and alarm is set"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def notificationService = container.getService(NotificationService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Notification when Alarm",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/demo/rules/DemoApartmentNotificationWhenAlarm.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the alarm enabled, presence detected flag and timestamp of the room should not be set"
        def apartment1Asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
        assert !apartment1Asset.getAttribute("alarmEnabled").get().valueAsBoolean.orElse(null)
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").orElse(null).valueAsBoolean.orElse(null)
        assert !livingRoomAsset.getAttribute("lastPresenceDetected").orElse(null).getValue().isPresent()

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
        notificationResource.storeDeviceToken(null, "device123", "token123")

        then: "the tokens should be in the database"
        notificationService.findDeviceToken("device123", keycloakDemoSetup.testuser3Id) == "token123"

        when: "the alarm is enabled"
        setPseudoClocksToRealTime(container, apartment1Engine)
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
            assert asset.getAttribute("presenceDetected").get().valueAsBoolean
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}