package org.openremote.test.rules.residence

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.firebase.messaging.Message
import net.fortuna.ical4j.model.Recur
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.notification.EmailNotificationHandler
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.notification.PushNotificationHandler
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetDeployment
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.UserAssetLink
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.asset.Asset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.calendar.CalendarEvent
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.console.ConsoleRegistration
import org.openremote.model.console.ConsoleResource
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.notification.AbstractNotificationMessage
import org.openremote.model.notification.Notification
import org.openremote.model.notification.NotificationSendResult
import org.openremote.model.notification.PushNotificationMessage
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.RulesetStatus
import org.openremote.model.rules.TemporaryFact
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.rules.json.JsonRulesetDefinition
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import org.simplejavamail.email.Email
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.temporal.ChronoUnit

import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.openremote.test.setup.ManagerTestSetup.DEMO_RULE_STATES_SMART_BUILDING
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.util.ValueUtil.parse

class JsonRulesTest extends Specification implements ManagerContainerTrait {

    def "Turn all lights off when console exits the residence geofence"() {

        List<PushNotificationMessage> pushMessages = []
        List<Email> emailMessages = []
        List<Notification.Target> pushTargets = []
        List<Notification.Target> emailTargets = []

        given: "the geofence notifier debounce is set to a small value for testing"
        def originalDebounceMillis = ORConsoleGeofenceAssetAdapter.NOTIFY_ASSETS_DEBOUNCE_MILLIS
        ORConsoleGeofenceAssetAdapter.NOTIFY_ASSETS_DEBOUNCE_MILLIS = 100

        and: "the rule firing delay time is set to a small value for testing"
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500

        and: "the container environment is started with the mock handler"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def pushNotificationHandler = container.getService(PushNotificationHandler.class)
        def emailNotificationHandler = container.getService(EmailNotificationHandler.class)
        def notificationService = container.getService(NotificationService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def timerService = container.getService(TimerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        RulesEngine realmBuildingEngine

        and: "a mock push notification handler"
        PushNotificationHandler mockPushNotificationHandler = Spy(pushNotificationHandler)
        mockPushNotificationHandler.isValid() >> true
        mockPushNotificationHandler.sendMessage(_ as Long, _ as Notification.Source, _ as String, _ as Notification.Target, _ as AbstractNotificationMessage) >> {
            id, source, sourceId, target, message ->
                pushMessages << message
                pushTargets << target
                callRealMethod()
        }
        // Assume sent to FCM
        mockPushNotificationHandler.sendMessage(_ as Message) >> {
            message -> return NotificationSendResult.success()
        }
        notificationService.notificationHandlerMap.put(pushNotificationHandler.getTypeName(), mockPushNotificationHandler)

        and: "a mock email notification handler"
        EmailNotificationHandler mockEmailNotificationHandler = Spy(emailNotificationHandler)
        mockEmailNotificationHandler.isValid() >> true
        mockEmailNotificationHandler.sendMessage(_ as Long, _ as Notification.Source, _ as String, _ as Notification.Target, _ as AbstractNotificationMessage) >> {
            id, source, sourceId, target, message ->
                emailTargets << target
                callRealMethod()
        }

        // Assume sent to FCM
        mockEmailNotificationHandler.sendMessage(_ as Email) >> {
            email ->
                emailMessages << email.get(0)
                return NotificationSendResult.success()
        }
        notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), mockEmailNotificationHandler)

        and: "some rules"
        Ruleset ruleset = new RealmRuleset(
            keycloakTestSetup.realmBuilding.name,
            "Demo Apartment - All Lights Off",
            Ruleset.Lang.JSON,
            getClass().getResource("/org/openremote/test/rules/BasicJsonRules.json").text)
        ruleset = rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running with asset states inserted and no longer tracking location rules"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert realmBuildingEngine.lastFireTimestamp > 0
            assert !realmBuildingEngine.trackLocationPredicates
        }

        and: "the room lights in an apartment to be on"
        conditions.eventually {
            def livingroomAsset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert livingroomAsset.getAttribute("lightSwitch", Boolean.class).get().value.get()
            assert livingroomAsset.getAttribute("lightSwitchTriggerTimes", String[].class).get().value.get().length == 2
            assert livingroomAsset.getAttribute("plantsWaterLevels", ObjectNode.class).get().value.get().get("cactus").asDouble() == 0.8d
            def bathRoomAsset = assetStorageService.find(managerTestSetup.apartment2BathroomId, true)
            assert bathRoomAsset.getAttribute("lightSwitch", Boolean.class).get().value.get()
        }

        when: "a user authenticates"
        def accessToken = authenticate(
            container,
            keycloakTestSetup.realmBuilding.name,
            KEYCLOAK_CLIENT_ID,
            "testuser3",
            "testuser3"
        ).token

        and: "a console is registered by that user"
        def authenticatedConsoleResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name, accessToken).proxy(ConsoleResource.class)
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
                        ((ObjectNode) parse("{\"token\": \"23123213ad2313b0897efd\"}").orElse(null)
                    )))
                }
            },
            "",
            ["manager"] as String[])
        consoleRegistration = authenticatedConsoleResource.register(null, consoleRegistration)

        then: "the console should have been registered and a geofence refresh notification should have been sent"
        conditions.eventually {
            assert consoleRegistration.id != null
            assert pushMessages.size() == 1
        }

        when:"an additional user is linked to this console (to help with testing)"
        assetStorageService.storeUserAssetLinks(
            Collections.singletonList(new UserAssetLink(keycloakTestSetup.realmBuilding.getName(), keycloakTestSetup.testuser2Id, consoleRegistration.id))
        )

        and: "the console location is set to the apartment"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION), AttributeEvent.Source.CLIENT)

        then: "the consoles location should have been updated"
        conditions.eventually {
            def testUser3Console = assetStorageService.find(consoleRegistration.id, true)
            assert testUser3Console != null
            def assetLocation = testUser3Console.getAttribute(Asset.LOCATION).flatMap { it.value }.orElse(null)
            assert assetLocation != null
            assert assetLocation.x == ManagerTestSetup.SMART_BUILDING_LOCATION.x
            assert assetLocation.y == ManagerTestSetup.SMART_BUILDING_LOCATION.y
            assert assetLocation.z == ManagerTestSetup.SMART_BUILDING_LOCATION.z
        }

        then: "the console location asset state should be in the rule engine"
        conditions.eventually {
            def assetState = realmBuildingEngine.assetStates.find {it.id == consoleRegistration.id && it.name == Asset.LOCATION.name}
            assert assetState != null
            assert assetState.getValue().isPresent()
            assert assetState.getValueAs(GeoJSONPoint.class).map{it.x == ManagerTestSetup.SMART_BUILDING_LOCATION.x}.orElse(false)
            assert assetState.getValueAs(GeoJSONPoint.class).map{it.y == ManagerTestSetup.SMART_BUILDING_LOCATION.y}.orElse(false)
        }

        when: "the console device moves outside the home geofence (as defined in the rule)"
        def outsideLocation = new GeoJSONPoint(0d, 0d)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, outsideLocation), AttributeEvent.Source.CLIENT)

        then: "the apartment lights should be switched off"
        conditions.eventually {
            def livingroomAsset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert !livingroomAsset.getAttribute("lightSwitch").get().value.get()
            assert livingroomAsset.getAttribute("lightSwitchTriggerTimes", String[].class).flatMap{it.value}.map{it.length}.orElse(0) == 2
            assert livingroomAsset.getAttribute("plantsWaterLevels", ObjectNode.class).get().getValue().map{it.get("cactus").asDouble()}.orElse(null) == 0.8
            def bathRoomAsset = assetStorageService.find(managerTestSetup.apartment2BathroomId, true)
            assert !bathRoomAsset.getAttribute("lightSwitch").get().value.get()
        }

        and: "a push notification should have been sent to the console via the asset target with the title 'Test title'"
        conditions.eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 1
            assert pushTargets.any {it.type == Notification.TargetType.ASSET && it.id == consoleRegistration.id}
        }

        and: "a push notification should have been sent to the console via the user target with the title 'Linked user test'"
        conditions.eventually {
            assert pushMessages.findAll {it.title == "Linked user test"}.size() == 1
            assert pushTargets.any {it.type == Notification.TargetType.ASSET && it.id == consoleRegistration.id}
        }

        and: "an email notification should have been sent to test@openremote.io with the triggered asset in the body"
        conditions.eventually {
            assert emailMessages.any {it.recipients.size() == 1
                && it.recipients[0].address == "test@openremote.io"
                && it.HTMLText == "<table cellpadding=\"30\"><tr><th>Asset ID</th><th>Asset Name</th><th>Attribute</th><th>Value</th></tr><tr><td>${consoleRegistration.id}</td><td>Test Console</td><td>location</td><td>" + ValueUtil.asJSON(outsideLocation).orElse("") + "</td></tr></table>"}
        }

        and : "an email notification should have been sent to the asset's linked user(s) (only testuser2 has email notifications enabled)"
        conditions.eventually {
            assert emailMessages.any {it.recipients.size() == 1
                    && it.recipients[0].address == "testuser2@openremote.local"
                    && it.HTMLText == "<table cellpadding=\"30\"><tr><th>Asset ID</th><th>Asset Name</th><th>Attribute</th><th>Value</th></tr><tr><td>${consoleRegistration.id}</td><td>Test Console</td><td>location</td><td>" + ValueUtil.asJSON(outsideLocation).orElse("") + "</td></tr></table>"}
        }

        and: "after a few seconds the rule should not have fired again"
        new PollingConditions(timeout: 5, initialDelay: 1).eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 1
        }

        when: "the console device moves back inside the home geofence (as defined in the rule)"
        def lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION), AttributeEvent.Source.CLIENT)

        and: "the engine fires at least one more time"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
        }

        and: "the console device moves outside the home geofence again (as defined in the rule)"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(0d, 0d)), AttributeEvent.Source.CLIENT)

        then: "another notification should have been sent to the console"
        conditions.eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 2
        }

        when: "the console sends a location update with a new location but still outside the geofence"
        def timestamp = assetStorageService.find(consoleRegistration.id, true).getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(timerService.getCurrentTimeMillis())
        def attributeEvent = new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(10d, 10d), timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent, AttributeEvent.Source.CLIENT)

        then: "after a few seconds the rule should not have fired again"
        new PollingConditions(timeout: 5, initialDelay: 1).eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 2
        }

        when: "the ruleset is modified to add a 4hr recurrence per asset"
        def version = ruleset.version
        JsonRulesetDefinition jsonRules = ValueUtil.JSON.readValue(ruleset.rules, JsonRulesetDefinition.class)
        jsonRules.rules[0].recurrence.mins = 240
        ruleset.rules = ValueUtil.asJSON(jsonRules).orElse(null)
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the ruleset to be redeployed"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.version == version+1
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.DEPLOYED
        }

        and: "another notification should have been sent to the console when rule is redeployed"
        conditions.eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 3
        }

        when: "the console device moves back inside the home geofence (as defined in the rule)"
        lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION), AttributeEvent.Source.CLIENT)

        then: "the engine fires at least one more time"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
        }

        when: "the console device moves outside the home geofence again (as defined in the rule)"
        attributeEvent = new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(0d, 0d))
        assetProcessingService.sendAttributeEvent(attributeEvent, AttributeEvent.Source.CLIENT)

        then: "after a few seconds the rule should not have fired again"
        new PollingConditions(timeout: 5, initialDelay: 1).eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 3
        }

        when: "the console device moves back inside the home geofence (as defined in the rule)"
        lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION), AttributeEvent.Source.CLIENT)

        then: "the engine fires at least one more time"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
        }

        when: "when time advances 5 hours"
        advancePseudoClock(5, HOURS, container)

        and: "the console device moves outside the home geofence again (as defined in the rule)"
        attributeEvent = new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(0d, 0d))
        assetProcessingService.sendAttributeEvent(attributeEvent, AttributeEvent.Source.CLIENT)

        then: "another notification should have been sent to the console"
        conditions.eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 4
            assert pushTargets[3].id == consoleRegistration.id
        }

        when: "the Rules PAUSE_SCHEDULER is overridden to facilitate testing"
        RulesEngine engine
        RulesetDeployment deployment
        def originalPause = RulesEngine.PAUSE_SCHEDULER
        RulesEngine.PAUSE_SCHEDULER = {e, d ->
            engine = e
            deployment = d
            long delay = d.getValidTo() - timerService.getCurrentTimeMillis()
            // Don't actually schedule just simulate time passing and let test decide when to move on
            advancePseudoClock(delay, MILLISECONDS, container)

        }
        def originalUnpause = RulesEngine.UNPAUSE_SCHEDULER
        RulesEngine.UNPAUSE_SCHEDULER = {e, d ->
            engine = e
            deployment = d
            long delay = d.getValidFrom() - timerService.getCurrentTimeMillis()
            // Don't actually schedule just simulate time passing and let test decide when to move on
            advancePseudoClock(delay, MILLISECONDS, container)
        }

        and: "a validity period is added to the ruleset (fictional times to ensure firing in sensible time within test)"
        version = ruleset.version
        def validityStart = Instant.ofEpochMilli(getClockTimeOf(container)).plus(2000, ChronoUnit.MILLIS)
        def validityEnd = Instant.ofEpochMilli(getClockTimeOf(container)).plus(4000, ChronoUnit.MILLIS)
        def recur = new Recur(Recur.DAILY, 3)
        recur.setInterval(2)
        def calendarEvent = new CalendarEvent(
            Date.from(validityStart),
            Date.from(validityEnd),
            recur)
        ruleset.getMeta().add(new MetaItem<Object>(Ruleset.VALIDITY ,calendarEvent))
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the ruleset should be redeployed and paused until 1st occurrence"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.version == version+1
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.PAUSED
        }

        when: "the same AttributeEvent is sent"
        timestamp = attributeEvent.getTimestamp()+1
        attributeEvent.setTimestamp(timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent, AttributeEvent.Source.CLIENT)

        then: "the event should have been committed to the DB"
        conditions.eventually {
            def console = assetStorageService.find(consoleRegistration.id, true)
            assert console.getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(0) == timestamp
        }

        and: "no notification should have been sent as outside the validity period"
        assert pushMessages.findAll {it.title == "Test title"}.size() == 4

        when: "the pause elapses"
        engine.unPauseRuleset(deployment)

        then: "eventually the ruleset should be unpaused (1st occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.DEPLOYED
        }

        when: "the same AttributeEvent is sent"
        timestamp = attributeEvent.getTimestamp()+1
        attributeEvent.setTimestamp(timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent, AttributeEvent.Source.CLIENT)

        then: "the event should have been committed to the DB"
        conditions.eventually {
            def console = assetStorageService.find(consoleRegistration.id, true)
            assert console.getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(0) == timestamp
        }

        and: "another notification should have been sent as inside the validity period"
        conditions.eventually {
            assert pushMessages.findAll {it.title == "Test title"}.size() == 5
        }

        when: "the un-pause elapses"
        engine.pauseRuleset(deployment)

        then: "the ruleset should become paused again (until next occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.PAUSED
        }

        when: "the same AttributeEvent is sent"
        timestamp = attributeEvent.getTimestamp()+1
        attributeEvent.setTimestamp(timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent, AttributeEvent.Source.CLIENT)

        then: "the event should have been committed to the DB"
        conditions.eventually {
            def console = assetStorageService.find(consoleRegistration.id, true)
            assert console.getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(0) == timestamp
        }

        and: "no notification should have been sent as outside the validity period"
        assert pushMessages.findAll {it.title == "Test title"}.size() == 5

        when: "the pause elapses"
        engine.unPauseRuleset(deployment)

        then: "eventually the ruleset should be unpaused (next occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.DEPLOYED
        }

        when: "the un-pause elapses"
        engine.pauseRuleset(deployment)

        then: "the ruleset should become paused again (until last occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.PAUSED
        }

        when: "the pause elapses"
        engine.unPauseRuleset(deployment)

        then: "eventually the ruleset should be unpaused (last occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.DEPLOYED
        }

        when: "the un-pause elapses"
        engine.pauseRuleset(deployment)

        then: "the ruleset should expire"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == RulesetStatus.EXPIRED
        }

        cleanup: "static variables are reset and the mock is removed"
        RulesEngine.PAUSE_SCHEDULER = originalPause
        RulesEngine.UNPAUSE_SCHEDULER = originalUnpause
        ORConsoleGeofenceAssetAdapter.NOTIFY_ASSETS_DEBOUNCE_MILLIS = originalDebounceMillis
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
        if (notificationService != null) {
            notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), emailNotificationHandler)
            notificationService.notificationHandlerMap.put(pushNotificationHandler.getTypeName(), pushNotificationHandler)
        }
    }
}
