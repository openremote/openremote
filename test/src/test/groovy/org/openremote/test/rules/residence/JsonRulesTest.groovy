package org.openremote.test.rules.residence


import com.google.firebase.messaging.Message
import jakarta.mail.internet.InternetAddress
import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientRequestFilter
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.fortuna.ical4j.model.Recur
import org.openremote.container.timer.TimerService
import org.openremote.container.util.MailUtil
import org.openremote.manager.notification.LocalizedNotificationHandler
import org.openremote.model.notification.EmailNotificationMessage
import org.openremote.model.notification.LocalizedNotificationMessage
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.notification.EmailNotificationHandler
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.notification.PushNotificationHandler
import org.openremote.manager.rules.JsonRulesBuilder
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.webhook.WebhookService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.impl.ConsoleAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.attribute.Attribute
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
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.json.JsonRulesetDefinition
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.HOURS
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.rules.RulesetStatus.*
import static org.openremote.model.util.ValueUtil.parse
import static org.openremote.model.value.ValueType.TEXT
import static org.openremote.setup.integration.ManagerTestSetup.DEMO_RULE_STATES_SMART_BUILDING

class JsonRulesTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {

        private int successCount = 0
        private int failureCount = 0

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri
            def requestPath = requestUri.scheme + "://" + requestUri.host + requestUri.path

            switch (requestPath) {
                case "https://basicserver/webhookplain":
                    if (requestContext.method == "POST" && requestContext.mediaType == MediaType.TEXT_PLAIN_TYPE && requestContext.hasEntity() && requestContext.entity == "test-value") {
                        successCount++
                        requestContext.abortWith(Response.ok().build()); return
                    }
                    break
                case "https://basicserver/webhookjson":
                    if (requestContext.method == "POST" && requestContext.mediaType == MediaType.APPLICATION_JSON_TYPE && requestContext.getHeaderString('test-header') == "test-value" && requestContext.hasEntity()) {
                        def jsonBody = parse(requestContext.entity.toString()).get() as Map
                        if (jsonBody.size() == 1
                                && ((Map)((Object[])jsonBody.values()[0])[0]).get("assetName") == "TestThing"
                                && ((Map)((Object[])jsonBody.values()[0])[0]).get("value") == "test_message") {
                            successCount++
                            requestContext.abortWith(Response.ok().build()); return
                        }
                    }
                    break
            }
            failureCount++
            requestContext.abortWith(Response.serverError().build())
        }
    }

    def cleanup() {
        mockServer.successCount = 0
        mockServer.failureCount = 0
    }

    def "Turn all lights off when console exits the residence geofence"() {

        List<Tuple2<Notification.Target, PushNotificationMessage>> pushTargetsAndMessages = []
        List<jakarta.mail.Message> emailMessages = []
        List<Notification.Target> emailTargets = []
        List<LocalizedNotificationMessage> localizedMessages = []

        given: "the geofence notifier debounce is set to a small value for testing"
        Integer originalDebounceMillis = ORConsoleGeofenceAssetAdapter.NOTIFY_ASSETS_DEBOUNCE_MILLIS
        ORConsoleGeofenceAssetAdapter.NOTIFY_ASSETS_DEBOUNCE_MILLIS = 100

        and: "the container environment is started with the mock handler"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def pushNotificationHandler = container.getService(PushNotificationHandler.class)
        def emailNotificationHandler = container.getService(EmailNotificationHandler.class)
        def localizedNotificationHandler = container.getService(LocalizedNotificationHandler.class)
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
                pushTargetsAndMessages << new Tuple2<>(target, message)
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

        // Assume sent to server
        mockEmailNotificationHandler.sendMessage(_ as jakarta.mail.Message) >> {
            email ->
                emailMessages << email.get(0)
                return NotificationSendResult.success()
        }
        notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), mockEmailNotificationHandler)

        // Register localized handler
        LocalizedNotificationHandler mockLocalizedNotificationHandler = Spy(localizedNotificationHandler)
        mockLocalizedNotificationHandler.isValid() >> true
        mockLocalizedNotificationHandler.sendMessage(_ as Long, _ as Notification.Source, _ as String, _ as Notification.Target, _ as AbstractNotificationMessage) >> {
            id, source, sourceId, target, message ->
                localizedMessages << message
        }
        mockLocalizedNotificationHandler.notificationHandlerMap = notificationService.notificationHandlerMap
        notificationService.notificationHandlerMap.put(localizedNotificationHandler.getTypeName(), mockLocalizedNotificationHandler)

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
            assert realmBuildingEngine.facts.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert realmBuildingEngine.lastFireTimestamp > 0
            assert !realmBuildingEngine.trackLocationPredicates
        }

        and: "the room lights in an apartment to be on"
        conditions.eventually {
            def livingroomAsset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert livingroomAsset.getAttribute("lightSwitch").get().value.get()
            assert livingroomAsset.getAttribute("lightSwitchTriggerTimes").get().value.get().length == 2
            assert livingroomAsset.getAttribute("plantsWaterLevels").get().getValue(Map.class).get().get("cactus") == 0.8d
            def bathRoomAsset = assetStorageService.find(managerTestSetup.apartment2BathroomId, true)
            assert bathRoomAsset.getAttribute("lightSwitch").get().value.get()
        }

        when: "a user authenticates"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "another user authenticates"
        def accessToken2 = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "building",
                "building"
        ).token

        and: "a console is registered by the first user"
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
                                (Map<String, Object>)(parse("{\"token\": \"23123213ad2313b0897efd\"}").orElse(null)
                                )))
                    }
                },
                "",
                ["manager"] as String[])
        consoleRegistration = authenticatedConsoleResource.register(null, consoleRegistration)

        and: "a console is registered by the second user"
        def authenticatedConsoleResource2 = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name, accessToken2).proxy(ConsoleResource.class)
        def consoleRegistration2 = new ConsoleRegistration(null,
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
                                (Map<String, Object>)(parse("{\"token\": \"23123213ad2313b0897efd\"}").orElse(null)
                                )))
                    }
                },
                "",
                ["manager"] as String[])
        consoleRegistration2 = authenticatedConsoleResource2.register(null, consoleRegistration2)

        and: "the console attributes are marked as RULE_STATE (inc LOCATION which is needed to trigger the rule)"
        def asset = assetStorageService.find(consoleRegistration.id)
        asset.getAttribute(Asset.LOCATION).ifPresent { it.addMeta(new MetaItem<>(MetaItemType.RULE_STATE))}
        asset.getAttribute(ConsoleAsset.CONSOLE_NAME).ifPresent { it.addMeta(new MetaItem<>(MetaItemType.RULE_STATE))}
        asset.getAttribute(ConsoleAsset.CONSOLE_VERSION).ifPresent { it.addMeta(new MetaItem<>(MetaItemType.RULE_STATE))}
        asset = assetStorageService.merge(asset)

        then: "a geofence refresh notification should have been sent to the console"
        conditions.eventually {
            assert pushTargetsAndMessages.size() == 1
        }

        when:"additional users are linked to this console (to help with testing)"
        assetStorageService.storeUserAssetLinks(
            [
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(), keycloakTestSetup.testuser2Id, consoleRegistration.id),
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(), keycloakTestSetup.buildingUserId, consoleRegistration.id)
            ]
        )

        and: "the console location is set to the apartment"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION))

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
            def assetState = realmBuildingEngine.facts.assetStates.find {it.id == consoleRegistration.id && it.name == Asset.LOCATION.name}
            assert assetState != null
            assert assetState.getValue().isPresent()
            assert assetState.getValue(GeoJSONPoint.class).map{it.x == ManagerTestSetup.SMART_BUILDING_LOCATION.x}.orElse(false)
            assert assetState.getValue(GeoJSONPoint.class).map{it.y == ManagerTestSetup.SMART_BUILDING_LOCATION.y}.orElse(false)
        }

        when: "the console device moves outside the home geofence (as defined in the rule)"
        def outsideLocation = new GeoJSONPoint(0d, 0d)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, outsideLocation))

        then: "the apartment lights should be switched off"
        conditions.eventually {
            def livingroomAsset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert !livingroomAsset.getAttribute("lightSwitch").get().value.get()
            assert livingroomAsset.getAttribute("lightSwitchTriggerTimes").flatMap{it.value}.map{it.length}.orElse(0) == 2
            assert livingroomAsset.getAttribute("plantsWaterLevels").get().getValue(Map.class).get().get("cactus") == 0.8
            def bathRoomAsset = assetStorageService.find(managerTestSetup.apartment2BathroomId, true)
            assert !bathRoomAsset.getAttribute("lightSwitch").get().value.get()
        }

        and: "a push notification should have been sent to the console via the asset target with the title 'Test title'"
        conditions.eventually {
            assert pushTargetsAndMessages.count {it.v2.title == "Test title" && it.v1.type == Notification.TargetType.ASSET && it.v1.id == consoleRegistration.id} == 1
        }

        and: "two push notifications should have been sent to the consoles via the linked user targets with test-realm-role% realm attribute with the title 'Linked user test'"
        conditions.eventually {
            assert pushTargetsAndMessages.count {it.v2.title == "Linked user test" && it.v1.type == Notification.TargetType.ASSET && it.v1.id == consoleRegistration.id} == 1
            assert pushTargetsAndMessages.count {it.v2.title == "Linked user test" && it.v1.type == Notification.TargetType.ASSET && it.v1.id == consoleRegistration2.id} == 1
        }

        and: "a push notifications should have been sent to the consoles of the linked user target with test-realm-role-2 realm attribute with the title 'Linked user test 2'"
        conditions.eventually {
            assert pushTargetsAndMessages.count {it.v2.title == "Linked user test 2" && it.v1.type == Notification.TargetType.ASSET && it.v1.id == consoleRegistration.id} == 1
            assert pushTargetsAndMessages.count {it.v2.title == "Linked user test 2" && it.v1.type == Notification.TargetType.ASSET && it.v1.id == consoleRegistration2.id} == 1
        }

        and: "no push notification should have been sent for the test-realm-role-3 notification action"
        conditions.eventually {
            assert pushTargetsAndMessages.count {it.v2.title == "Linked user test 3" && it.v1.type == Notification.TargetType.ASSET && it.v1.id == consoleRegistration.id} == 0
            assert pushTargetsAndMessages.count {it.v2.title == "Linked user test 3" && it.v1.type == Notification.TargetType.ASSET && it.v1.id == consoleRegistration2.id} == 0
        }

        and: "an email notification should have been sent to test@openremote.io with the triggered asset in the body but only containing the triggered asset states"
        conditions.eventually {
            assert emailMessages.any {it.getRecipients(jakarta.mail.Message.RecipientType.TO).length == 1
                && (it.getRecipients(jakarta.mail.Message.RecipientType.TO)[0] as InternetAddress).address == "test@openremote.io"
                && MailUtil.toMailMessage(it, true).content == "<table cellpadding=\"30\"><tr><th>Asset ID</th><th>Asset Name</th><th>Attribute</th><th>Value</th></tr><tr><td>${consoleRegistration.id}</td><td>Test Console</td><td>location</td><td>" + ValueUtil.asJSON(outsideLocation).orElse("") + "</td></tr></table>"}
        }

        and : "an email notification should have been sent to the asset's linked user(s) (only testuser2 has email notifications enabled)"
        conditions.eventually {
            assert emailMessages.any {it.getRecipients(jakarta.mail.Message.RecipientType.TO).length == 1
                    && (it.getRecipients(jakarta.mail.Message.RecipientType.TO)[0] as InternetAddress).address == "testuser2@openremote.local"
                    && MailUtil.toMailMessage(it, true).content == "<table cellpadding=\"30\"><tr><th>Asset ID</th><th>Asset Name</th><th>Attribute</th><th>Value</th></tr><tr><td>${consoleRegistration.id}</td><td>Test Console</td><td>location</td><td>" + ValueUtil.asJSON(outsideLocation).orElse("") + "</td></tr></table>"}
        }

        and: "an localized email notification should have been sent with the triggered asset in the body but only containing the triggered asset states"
        String expectedHtml = "<table cellpadding=\"30\"><tr><th>Asset ID</th><th>Asset Name</th><th>Attribute</th><th>Value</th></tr><tr><td>${consoleRegistration.id}</td><td>Test Console</td><td>location</td><td>" + ValueUtil.asJSON(outsideLocation).orElse("") + "</td></tr></table>"
        conditions.eventually {
            assert localizedMessages.size() == 7
             // Get only email messages
            emailMessages = localizedMessages.findAll { localizedMsg ->
                localizedMsg.getMessages().values().stream().allMatch { message ->
                    message.type == EmailNotificationMessage.TYPE
                }
            }
            
            // Verify email content
            assert emailMessages.stream().allMatch {
                it.getMessages().values().stream().allMatch(m -> {
                    ((EmailNotificationMessage) m).getHtml() == expectedHtml
                })
            }
            
            assert emailMessages.stream().filter {
                it.getMessages().values().stream().allMatch {
                    ((EmailNotificationMessage) it).getSubject() == "Demo Apartment - All Lights Off"
                }
            }.count() == 3
            
            assert emailMessages.stream().filter {
                it.getMessages().values().stream().allMatch {
                    ((EmailNotificationMessage) it).getSubject() == "Linked user localized user test"
                }
            }.count() == 2
            
            // Get push messages
            def pushMessages = localizedMessages.findAll { localizedMsg ->
                localizedMsg.getMessages().values().stream().allMatch { message ->
                    message.type == PushNotificationMessage.TYPE
                }
            }
            
            // Verify push notifications
            def assetIdPushNotification = pushMessages.find { localizedMsg ->
                localizedMsg.getMessages().get("en") instanceof PushNotificationMessage &&
                ((PushNotificationMessage) localizedMsg.getMessages().get("en")).getBody()?.contains(consoleRegistration.id)
            }

            assert assetIdPushNotification != null
            assert ((PushNotificationMessage) assetIdPushNotification.getMessages().get("en")).getBody() == "English: Asset ${consoleRegistration.id} has moved"
            assert ((PushNotificationMessage) assetIdPushNotification.getMessages().get("nl")).getBody() == "Nederlands: Asset ${consoleRegistration.id} is verplaatst"

            // Verify no unprocessed placeholders remain
            assert pushMessages.every { localizedMsg ->
                localizedMsg.getMessages().values().every { msg ->
                    !((PushNotificationMessage) msg).getBody()?.contains("%ASSET_ID%")
                }
            }
        }

        and: "a push notification with asset ID should have been sent to both linked users"
        conditions.eventually {
            // Count notifications for users with test-realm-role
            def linkedNotifications = pushTargetsAndMessages.findAll {
                it.v2.title == "Linked Asset ID Test" && 
                it.v2.body == "This notification is about asset: ${consoleRegistration.id}"
            }
            assert linkedNotifications.size() == 2 // should be sent to both linked users
        }

        and: "the action URL should contain the corect asset ID"
        conditions.eventually {
            // First find the relevant message
            def targetMessage = pushTargetsAndMessages.find { it.v2.title == "URL Asset ID Test" }
            assert targetMessage != null
            
            // Then verify URL
            assert targetMessage.v2.action?.url == "/assets/${consoleRegistration.id}/details"
        }

        and: "notifications should still contain asset ID even for user without access"
        conditions.eventually {
        assetStorageService.storeUserAssetLinks([
            new UserAssetLink(keycloakTestSetup.realmBuilding.getName(), keycloakTestSetup.testuser3Id, consoleRegistration.id)
            ])
             // Print debug info
            println "Available notifications:"
            pushTargetsAndMessages.each { target, msg ->
                println "Target: ${target}, Message: title='${msg.title}', body='${msg.body}'"
            }
            
            assert pushTargetsAndMessages.any {
                it.v1.type == Notification.TargetType.ASSET &&
                it.v2.title == "Asset ID Test" &&
                it.v2.body == "Your asset ID is: ${consoleRegistration.id}"
            }
        }

        and: "after a few seconds the rule should not have fired again"
        new PollingConditions(timeout: 5, initialDelay: 1).eventually {
            assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 1
        }

        when: "the console device moves back inside the home geofence (as defined in the rule)"
        def lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION))

        then: "the engine fires at least one more time"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
        }

        when: "the console device moves outside the home geofence again (as defined in the rule)"
        emailMessages.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(-10d, -4d)))

        then: "another notification should have been sent to the console"
        conditions.eventually {
            assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 2
        }

        and: "an email notification should have been sent to test@openremote.io with the triggered asset in the body but only containing the triggered asset states"
        conditions.eventually {
            assert emailMessages.any {it.getRecipients(jakarta.mail.Message.RecipientType.TO).length == 1
                    && (it.getRecipients(jakarta.mail.Message.RecipientType.TO)[0] as InternetAddress).address == "test@openremote.io"
                    && MailUtil.toMailMessage(it, true).content == "<table cellpadding=\"30\"><tr><th>Asset ID</th><th>Asset Name</th><th>Attribute</th><th>Value</th></tr><tr><td>${consoleRegistration.id}</td><td>Test Console</td><td>location</td><td>" + ValueUtil.asJSON(new GeoJSONPoint(-10d, -4d)).orElse("") + "</td></tr></table>"}
        }

        when: "the console sends a location update with a new location but still outside the geofence"
        def timestamp = assetStorageService.find(consoleRegistration.id, true).getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(timerService.getCurrentTimeMillis())
        def attributeEvent = new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(10d, 10d), timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "after a few seconds the rule should not have fired again"
        new PollingConditions(timeout: 5, initialDelay: 1).eventually {
            assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 2
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
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == DEPLOYED
        }

        and: "another notification should have been sent to the console when rule is redeployed"
        conditions.eventually {
            assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 3
        }

        when: "the console device moves back inside the home geofence (as defined in the rule)"
        lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION))

        then: "the engine fires at least one more time"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
        }

        when: "the console device moves outside the home geofence again (as defined in the rule)"
        attributeEvent = new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(0d, 0d))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "after a few seconds the rule should not have fired again"
        new PollingConditions(timeout: 5, initialDelay: 1).eventually {
            assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 3
        }

        when: "the console device moves back inside the home geofence (as defined in the rule)"
        lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        assetProcessingService.sendAttributeEvent(new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, ManagerTestSetup.SMART_BUILDING_LOCATION))

        then: "the engine fires at least one more time"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
        }

        when: "when time advances 5 hours"
        advancePseudoClock(5, HOURS, container)

        and: "the console device moves outside the home geofence again (as defined in the rule)"
        attributeEvent = new AttributeEvent(consoleRegistration.id, Asset.LOCATION.name, new GeoJSONPoint(0d, 0d))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "another notification should have been sent to the console"
        conditions.eventually {
            assert pushTargetsAndMessages.findAll {it.v2.title == "Test title" && it.v1.id == consoleRegistration.id}.size() == 4
        }

        when: "a validity period is added to the ruleset"
        version = ruleset.version
        def validityStart = Instant.ofEpochMilli(getClockTimeOf(container)).plus(2, ChronoUnit.HOURS)
        def validityEnd = Instant.ofEpochMilli(getClockTimeOf(container)).plus(4, ChronoUnit.HOURS)
        def recur = new Recur(Recur.Frequency.DAILY, 3)
        recur.setInterval(2)
        def calendarEvent = new CalendarEvent(
                Date.from(validityStart),
                Date.from(validityEnd),
                recur)
        ruleset.setValidity(calendarEvent)
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the ruleset should be redeployed and paused until 1st occurrence"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.version == version+1
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == PAUSED
        }

        when: "the same AttributeEvent is sent"
        timestamp = attributeEvent.getTimestamp()+1
        attributeEvent.setTimestamp(timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the event should have been committed to the DB"
        conditions.eventually {
            def console = assetStorageService.find(consoleRegistration.id, true)
            assert console.getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(0) == timestamp
        }

        and: "no notification should have been sent as outside the validity period"
        assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 4

        when: "time advances to inside the validity period"
        advancePseudoClock(3, HOURS, container)

        then: "the ruleset should be unpaused (1st occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == DEPLOYED
        }

        when: "the same AttributeEvent is sent"
        timestamp = attributeEvent.getTimestamp()+1
        attributeEvent.setTimestamp(timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the event should have been committed to the DB"
        conditions.eventually {
            def console = assetStorageService.find(consoleRegistration.id, true)
            assert console.getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(0) == timestamp
        }

        and: "another notification should have been sent as inside the validity period"
        conditions.eventually {
            assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 5
        }

        when: "time advances past the validity period"
        advancePseudoClock(1, HOURS, container)

        then: "the ruleset should become paused again (until next occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == PAUSED
        }

        when: "the same AttributeEvent is sent"
        timestamp = attributeEvent.getTimestamp()+1
        attributeEvent.setTimestamp(timestamp)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the event should have been committed to the DB"
        conditions.eventually {
            def console = assetStorageService.find(consoleRegistration.id, true)
            assert console.getAttribute(Asset.LOCATION).flatMap{it.getTimestamp()}.orElse(0) == timestamp
        }

        and: "no notification should have been sent as outside the validity period"
        assert pushTargetsAndMessages.findAll {it.v2.title == "Test title"}.size() == 5

        when: "time advances to inside the next validity period"
        advancePseudoClock(47, HOURS, container)

        then: "eventually the ruleset should be unpaused (next occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == DEPLOYED
        }

        when: "time advances past the validity period"
        advancePseudoClock(1, HOURS, container)

        then: "the ruleset should become paused again (until last occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == PAUSED
        }

        when: "time advances to inside the next validity period"
        advancePseudoClock(47, HOURS, container)

        then: "eventually the ruleset should be unpaused (last occurrence)"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == DEPLOYED
        }

        when: "time advances past the validity period"
        advancePseudoClock(1, HOURS, container)

        then: "the ruleset should expire"
        conditions.eventually {
            assert realmBuildingEngine.deployments.find{it.key == ruleset.id}.value.status == EXPIRED
        }

        cleanup: "static variables are reset and the mock is removed"
        if (originalDebounceMillis != null) {
            ORConsoleGeofenceAssetAdapter.NOTIFY_ASSETS_DEBOUNCE_MILLIS = originalDebounceMillis
        }
        if (notificationService != null) {
            notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), emailNotificationHandler)
            notificationService.notificationHandlerMap.put(pushNotificationHandler.getTypeName(), pushNotificationHandler)
        }
    }

    def "Trigger actions when attribute conditions match for specified durations"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def timerService = container.getService(TimerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine realmBuildingEngine

        and: "the pseudo clock is stopped"
        stopPseudoClock()

        when: "a light asset is added to the building realm"
        def lightId = UniqueIdentifierGenerator.generateId("TestLight")
        def lightAsset = new LightAsset("TestLight")
                .setId(lightId)
                .setRealm(keycloakTestSetup.realmBuilding.name)
                .setLocation(new GeoJSONPoint(0, 0))

     
        def ruleState = new MetaItem<>(MetaItemType.RULE_STATE)
        lightAsset.getAttributes().get("brightness").get().addMeta(ruleState)
        lightAsset.getAttributes().get("onOff").get().addMeta(ruleState)
        lightAsset.getAttributes().get("notes").get().addMeta(ruleState)
        assetStorageService.merge(lightAsset)

        then: "the light asset should be present"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light != null
        }

        when: "a thing asset is added to the building realm"
        def thingId = UniqueIdentifierGenerator.generateId("TestThing")
        def thingAsset = new ThingAsset("TestThing")
                .setId(thingId)
                .setRealm(keycloakTestSetup.realmBuilding.name)
                .setLocation(new GeoJSONPoint(0, 0))
        thingAsset.getAttributes().get("notes").get().addMeta(ruleState)
        thingAsset = assetStorageService.merge(thingAsset)

        then: "the thing asset should be present"
        conditions.eventually {
            def thing = assetStorageService.find(thingId)
            assert thing != null
        }

        // RuleSet: WHEN:
        // Any light asset has brightness higher than 50 for 5 minutes and onOff is true
        // AND
        // Any thing asset has notes set to "Test"
        // OR
        // Any light asset has a note that equals "TriggerDuration" for 10 minutes
        // THEN:
        // Set the notes of the light asset to "Triggered"

        when: "a ruleset with a duration map has been added"
        def rulesStr = getClass().getResource("/org/openremote/test/rules/JsonAttributeDurationRule.json").text
        def rule = parse(rulesStr, JsonRulesetDefinition.class).orElseThrow()
        Ruleset ruleset = new RealmRuleset(
                keycloakTestSetup.realmBuilding.name,
                "Duration Rule",
                Ruleset.Lang.JSON,
                rulesStr)
        ruleset = rulesetStorageService.merge(ruleset)
        def lastFireTimestamp = 0

        then: "the rule engines to become available and be running with asset states inserted"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.facts.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING + 4 // 4 additional rule states
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        when: "the light asset is updated with brightness set to 100"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("brightness").get().setValue(100)
        assetStorageService.merge(lightAsset)

        then: "the brightness value should be 100"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("brightness").get().getValue().orElse(0) == 100
        }

        when: "time advances for 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should still have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "the thing asset is updated with notes set to Test"
        thingAsset = assetStorageService.find(thingId)
        thingAsset.getAttribute("notes").get().setValue("Test")
        assetStorageService.merge(thingAsset)

        then: "the thing asset should have its notes set to Test"
        conditions.eventually {
            def thing = assetStorageService.find(thingId)
            assert thing.getAttribute("notes").get().getValue().orElse("") == "Test"
        }

        when: "time advances by 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should still have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "time advances for 5 minutes"
        advancePseudoClock(5, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be empty" // because onOff is still false
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "the light asset is updated with onOff set to true"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("onOff").get().setValue(true)
        assetStorageService.merge(lightAsset)

        then: "the light asset should have its onOff attribute set to true"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("onOff").get().getValue().orElse(false) == true
        }

        when: "time advances for 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute set to Triggered" // because onOff was set to true, and brightness was above 50 for 5 minutes and the thing asset notes was set to Test
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == "Triggered"
        }

        when: "the light asset notes is updated to empty"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("notes").get().setValue("")
        assetStorageService.merge(lightAsset)

        then: "the light asset should have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "time advances for 10 minutes"
        advancePseudoClock(10, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }


        when: "the light asset notes is updated to TriggerDuration"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("notes").get().setValue("TriggerDuration")
        assetStorageService.merge(lightAsset)

        then: "the light asset should have its notes attribute be TriggerDuration"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == "TriggerDuration"
        }


        // Start OR condition part
        when: "time advances for 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be TriggerDuration"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == "TriggerDuration"
        }

        when: "time advances for 10 minutes"
        advancePseudoClock(10, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be Triggered" // because the OR condition was satisfied (thing asset notes was set to Test for 10 minutes)
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == "Triggered"
        }

        when: "the light asset notes and brightness are updated and thing asset notes is updated to empty"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("notes").get().setValue("")
        lightAsset.getAttribute("brightness").get().setValue(0)
        thingAsset = assetStorageService.find(thingId)
        thingAsset.getAttribute("notes").get().setValue("")
        assetStorageService.merge(lightAsset)
        assetStorageService.merge(thingAsset)

        then: "the light asset should have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
            assert light.getAttribute("brightness").get().getValue().orElse(0) == 0
            def thing = assetStorageService.find(thingId)
            assert thing.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "time advances for 1 minute"
        advancePseudoClock(1, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes empty abd brightness set to 0 and thing asset notes set to empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
            assert light.getAttribute("brightness").get().getValue().orElse(0) == 0
            def thing = assetStorageService.find(thingId)
            assert thing.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "time advances for 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired and"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        when: "light asset brightness is updated to 100 and thing asset notes is updated to Test"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("brightness").get().setValue(100)
        thingAsset = assetStorageService.find(thingId)
        thingAsset.getAttribute("notes").get().setValue("Test")
        assetStorageService.merge(lightAsset)
        assetStorageService.merge(thingAsset)

        then: "the light asset should have its brightness set to 100"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("brightness").get().getValue().orElse(0) == 100
            def thing = assetStorageService.find(thingId)
            assert thing.getAttribute("notes").get().getValue().orElse("") == "Test"
        }

        when: "time advances for 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "time advances for 3 minutes"
        advancePseudoClock(3, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "brightness is updated to 0"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("brightness").get().setValue(0)
        assetStorageService.merge(lightAsset)

        then: "the light asset should have its brightness set to 0" // this is done to reset the duration of the matched state
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("brightness").get().getValue().orElse(0) == 0
        }

        when: "time advances for 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "brightness is updated to 100"
        lightAsset = assetStorageService.find(lightId)
        lightAsset.getAttribute("brightness").get().setValue(100)
        assetStorageService.merge(lightAsset)

        then: "the light asset should have its brightness set to 100" // set to 100 to start the duration of the matched state
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("brightness").get().getValue().orElse(0) == 100
        }

        when: "time advances for 6 seconds"
        advancePseudoClock(6, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be empty"
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "time advances for 4 minutes"
        advancePseudoClock(3, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be empty" // because brightness was set to 0 in between so the timer was reset for that attribute match
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == ""
        }

        when: "time advances for 3 minutes"
        advancePseudoClock(3, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the light asset should have its notes attribute be Triggered" // because brightness has been above 50 for 5 minutes.
        conditions.eventually {
            def light = assetStorageService.find(lightId)
            assert light.getAttribute("notes").get().getValue().orElse("") == "Triggered"
        }
    }

    def "Trigger actions based on the position of the sun"() {

        given: "the container environment is started"
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

        and: "a thing asset is added to the building realm"
        def thingId = UniqueIdentifierGenerator.generateId("TestThing")
        def thingAsset = new ThingAsset("TestThing")
                .setId(thingId)
                .setRealm(keycloakTestSetup.realmBuilding.name)
                .setLocation(new GeoJSONPoint(0, 0))
                .addAttributes(
                        new Attribute<>("sunset", ValueType.INTEGER, null),
                        new Attribute<>("sunriseWithOffset", ValueType.INTEGER, null),
                        new Attribute<>("twilightHorizon", ValueType.INTEGER, null),
                )
        thingAsset = assetStorageService.merge(thingAsset)

        and: "the pseudo clock is stopped"
        stopPseudoClock()

        when: "a ruleset with sunset trigger condition is added whose action updates the thing asset"
        def rulesStr = getClass().getResource("/org/openremote/test/rules/JsonRuleSunset.json").text
        def rule = parse(rulesStr, JsonRulesetDefinition.class).orElseThrow()
        Ruleset ruleset = new RealmRuleset(
                keycloakTestSetup.realmBuilding.name,
                "Sunset Rule",
                Ruleset.Lang.JSON,
                rulesStr)
        ruleset = rulesetStorageService.merge(ruleset)
        def sunsetCalculator = JsonRulesBuilder.getSunCalculator(ruleset, rule.rules[0].when.groups[0].items[0].sun, timerService)
        def sunTimes = sunsetCalculator.execute()
        def lastFireTimestamp = 0

        then: "the rule engines to become available and be running with asset states inserted"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.facts.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
            assert sunsetCalculator != null
        }

        and: "the next sunset should be calculated and should be in the future"
        assert sunTimes.getSet() != null
        assert sunTimes.getSet().toInstant().isAfter(timerService.getNow())

        and: "the rule should not have triggered"
        conditions.eventually {
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunset").get().getValue().orElse(0) == 0
        }

        when: "time advances slightly"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired again and the rule should not have triggered"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunset").get().getValue().orElse(0) == 0
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        when: "time advances slightly past the next sunset time and the rule engine fires"
        advancePseudoClock(Duration.between(timerService.getNow(), sunTimes.getSet()).getSeconds()+1, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired again and the rule should have triggered"
        def lastUpdateTime = 0
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunset").get().getValue().orElse(0) == 100
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
            lastUpdateTime = thingAsset.getAttribute("sunset").get().getTimestamp().orElse(0)
        }

        when: "time advances slightly"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired again and the rule should not have triggered"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp >= lastFireTimestamp + 1000
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunset").get().getTimestamp().orElse(-1) == lastUpdateTime
        }

        when: "time advances past the sunset"
        sunTimes = sunsetCalculator.on(sunTimes.getSet().plusHours(1)).execute()
        advancePseudoClock(Duration.between(timerService.getNow(), sunTimes.getRise()).getSeconds(), TimeUnit.SECONDS, container)

        and: "the updated attribute is cleared"
        thingAsset.getAttribute("sunset").get().setValue(null)
        thingAsset = assetStorageService.merge(thingAsset)

        then: "the rule engine should have fired again and the rule should not have triggered"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunset").get().getValue().orElse(0) == 0
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        when: "time advances slightly past the next sunset time and the rule engine fires"
        sunTimes = sunsetCalculator.on(sunTimes.getSet().plusHours(1)).execute()
        advancePseudoClock(Duration.between(timerService.getNow(), sunTimes.getSet().plusSeconds(10)).getSeconds()+1, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired again and the rule should have triggered"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunset").get().getValue().orElse(0) == 100
        }

        when: "a schedule is added to the rule to enable it 2 hours after sunset each day"
        ruleset.setValidity(new CalendarEvent(sunTimes.getSet().plusHours(2).toDate(), sunTimes.getSet().plusHours(12).toDate(), new Recur(Recur.Frequency.DAILY, 5)))
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the rule should become paused in the engine"
        conditions.eventually {
            assert realmBuildingEngine.deployments.get(ruleset.id).status == PAUSED
        }

        when: "the updated attribute is cleared"
        thingAsset.getAttribute("sunset").get().setValue(null)
        thingAsset = assetStorageService.merge(thingAsset)

        and: "time advances into the next active time of the rule"
        sunTimes = sunsetCalculator.on(sunTimes.getSet().plusHours(1)).execute()
        advancePseudoClock(Duration.between(timerService.getNow(), sunTimes.getSet().plusHours(4)).getSeconds(), TimeUnit.SECONDS, container)

        then: "the rule should become un-paused in the engine"
        conditions.eventually {
            assert realmBuildingEngine.deployments.get(ruleset.id).status == DEPLOYED
        }

        then: "the rule engine should have fired again and the rule should not have triggered (as past sunset)"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunset").get().getValue().orElse(0) == 0
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        when: "a ruleset with sunrise offset trigger condition is added whose action updates the thing asset"
        rulesStr = getClass().getResource("/org/openremote/test/rules/JsonRuleSunriseOffset.json").text
        rule = parse(rulesStr, JsonRulesetDefinition.class).orElseThrow()
        ruleset = new RealmRuleset(
                keycloakTestSetup.realmBuilding.name,
                "Sunrise with offset Rule",
                Ruleset.Lang.JSON,
                rulesStr)
        ruleset = rulesetStorageService.merge(ruleset)
        sunsetCalculator = JsonRulesBuilder.getSunCalculator(ruleset, rule.rules[0].when.groups[0].items[0].sun, timerService)
        sunTimes = sunsetCalculator.execute()

        then: "the rule engines to become available and be running with asset states inserted"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.facts.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the next sunrise should be calculated and should be in the future"
        assert sunTimes.getRise() != null
        assert sunTimes.getRise().toInstant().isAfter(timerService.getNow())

        and: "the rule should not have triggered"
        conditions.eventually {
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunriseWithOffset").get().getValue().orElse(0) == 0
        }

        when: "time advances slightly past the next sunrise time and the rule engine fires"
        advancePseudoClock(Duration.between(timerService.getNow(), sunTimes.getRise()).getSeconds()+1, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired again and the rule should not have triggered"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunriseWithOffset").get().getValue().orElse(0) == 0
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        when: "time advances past the offset delay and the rule engine fires"
        advancePseudoClock(rule.rules[0].when.groups[0].items[0].sun.offsetMins, TimeUnit.MINUTES, container)

        then: "the rule engine should have fired again and the rule should have triggered"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("sunriseWithOffset").get().getValue().orElse(0) == 100
        }

        when: "a ruleset with twilight trigger condition is added whose action updates the thing asset"
        rulesStr = getClass().getResource("/org/openremote/test/rules/JsonRuleTwilight.json").text
        rule = parse(rulesStr, JsonRulesetDefinition.class).orElseThrow()
        ruleset = new RealmRuleset(
                keycloakTestSetup.realmBuilding.name,
                "Twilight Rule",
                Ruleset.Lang.JSON,
                rulesStr)
        ruleset = rulesetStorageService.merge(ruleset)
        sunsetCalculator = JsonRulesBuilder.getSunCalculator(ruleset, rule.rules[0].when.groups[0].items[0].sun, timerService)
        sunTimes = sunsetCalculator.execute()

        then: "the rule engines to become available and be running with asset states inserted"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.facts.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            lastFireTimestamp = realmBuildingEngine.lastFireTimestamp
        }

        and: "the next twilight should be calculated and should be in the future"
        assert sunTimes.getRise() != null
        assert sunTimes.getRise().toInstant().isAfter(timerService.getNow())

        and: "the rule should not have triggered"
        conditions.eventually {
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("twilightHorizon").get().getValue().orElse(0) == 0
        }

        when: "time advances slightly past the next occurrence time and the rule engine fires"
        advancePseudoClock(Duration.between(timerService.getNow(), sunTimes.getRise()).getSeconds()+1, TimeUnit.SECONDS, container)

        then: "the rule engine should have fired again and the rule should have triggered"
        conditions.eventually {
            assert realmBuildingEngine.lastFireTimestamp > lastFireTimestamp
            assert realmBuildingEngine.lastFireTimestamp == timerService.getNow().toEpochMilli()
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset.getAttribute("twilightHorizon").get().getValue().orElse(0) == 100
        }

        cleanup: "static variables are reset"
        if (notificationService != null) {
            notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), emailNotificationHandler)
            notificationService.notificationHandlerMap.put(pushNotificationHandler.getTypeName(), pushNotificationHandler)
        }
    }

    def "Trigger webhook when thing asset has changed"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def webhookService = container.getService(WebhookService.class)
        RulesEngine realmBuildingEngine

        and: "the web target builder is configured to use the mock server"
        if (!webhookService.clientBuilder.configuration.isRegistered(mockServer)) {
            webhookService.clientBuilder.register(mockServer, Integer.MAX_VALUE)
        }
        assert webhookService.clientBuilder.configuration.isRegistered(mockServer)

        and: "a thing asset is added to the building realm"
        def thingId = UniqueIdentifierGenerator.generateId("TestThing")
        def thingAsset = new ThingAsset("TestThing")
                .setId(thingId)
                .setRealm(keycloakTestSetup.realmBuilding.name)
                .setLocation(new GeoJSONPoint(0, 0))
                .addAttributes(
                        new Attribute<>("webhookAttribute", TEXT).addMeta(
                                new MetaItem<>(MetaItemType.RULE_STATE)
                        )
                )
        thingAsset = assetStorageService.merge(thingAsset)

        and: "a ruleset with 'has value' condition is added whose action triggers the webhook"
        Ruleset ruleset = new RealmRuleset(
                keycloakTestSetup.realmBuilding.name,
                "Webhook Rule",
                Ruleset.Lang.JSON,
                getClass().getResource("/org/openremote/test/rules/JsonRuleWebhook.json").text)
        ruleset = rulesetStorageService.merge(ruleset)
        assert ruleset != null

        expect: "the attribute we will change being unset"
        conditions.eventually {
            thingAsset = assetStorageService.find(thingId) as ThingAsset
            assert thingAsset != null
            assert thingAsset.getAttribute("webhookAttribute").get() != null
            assert !thingAsset.getAttribute("webhookAttribute").get().getValue().isPresent()
        }

        and: "the rule engines to become available and be running with asset states and deployments inserted"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.deployments.size() == 1
            assert realmBuildingEngine.deployments.values().iterator().next().name == "Webhook Rule"
            assert realmBuildingEngine.deployments.values().iterator().next().status == DEPLOYED
            assert realmBuildingEngine.facts.assetStates.size() == (DEMO_RULE_STATES_SMART_BUILDING + 1)
            assert realmBuildingEngine.lastFireTimestamp > 0
        }

        when: "the webhook attribute is changing"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(thingId, "webhookAttribute", "test_message"))

        then: "the webhook attribute should have been updated"
        conditions.eventually {
            def thingAsset2 = assetStorageService.find(thingId, true)
            assert thingAsset2 != null
            assert thingAsset2.getAttribute("webhookAttribute").get().value.get() == "test_message"
        }

        expect: "The mock server has received a successful response"
        conditions.eventually {
            assert mockServer.successCount == 2
            assert mockServer.failureCount == 0
        }
    }
}
