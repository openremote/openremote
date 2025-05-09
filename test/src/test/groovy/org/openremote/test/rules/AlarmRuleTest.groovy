package org.openremote.test.rules

import jakarta.mail.Message
import org.openremote.manager.alarm.AlarmService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.notification.EmailNotificationHandler
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.model.alarm.Alarm
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.notification.AbstractNotificationMessage
import org.openremote.model.notification.Notification
import org.openremote.model.notification.NotificationSendResult
import org.openremote.model.query.RulesetQuery
import org.openremote.model.rules.json.JsonRulesetDefinition
import org.openremote.model.rules.json.RuleActionAlarm
import org.openremote.model.security.User
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.rules.*
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant

class AlarmRuleTest extends Specification implements ManagerContainerTrait {

    @Shared
    static KeycloakTestSetup keycloakTestSetup

    @Shared
    static ManagerTestSetup managerTestSetup

    @Shared
    static String alarmsReadWriteUserId

    @Shared
    static AssetProcessingService assetProcessingService

    @Shared
    static AlarmService alarmService

    @Shared
    static AssetStorageService assetStorageService

    @Shared
    static NotificationService notificationService

    @Shared
    static RulesService rulesService

    @Shared
    static RulesetStorageService rulesetStorageService

    @Shared
    static List<AbstractNotificationMessage> notificationMessages = []

    def setupSpec() {
        def container = startContainer(defaultConfig(), defaultServices())

        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)

        User alarmsReadWriteUser = keycloakTestSetup.createUser(managerTestSetup.realmBuildingName, "alarmsrwuser1", "alarmsrwuser1", "Alarms R/W", "User", "alarmsrwuser@openremote.local", true, KeycloakTestSetup.REGULAR_USER_ROLES)
        alarmsReadWriteUserId = alarmsReadWriteUser.getId()

        alarmService = container.getService(AlarmService.class)
        assetProcessingService = container.getService(AssetProcessingService.class)
        assetStorageService = container.getService(AssetStorageService.class)
        notificationService = container.getService(NotificationService.class)
        rulesetStorageService = container.getService(RulesetStorageService.class)
        rulesService = container.getService(RulesService.class)


        def emailNotificationHandler = container.getService(EmailNotificationHandler.class)
        def throwPushHandlerException = false
        EmailNotificationHandler mockPushNotificationHandler = Spy(emailNotificationHandler)
        mockPushNotificationHandler.isValid() >> true
        mockPushNotificationHandler.sendMessage(_ as Long, _ as Notification.Source, _ as String, _ as Notification.Target, _ as AbstractNotificationMessage) >> {
            id, source, sourceId, target, message ->
                if (throwPushHandlerException) {
                    throw new Exception("Failed to send notification")
                }
                notificationMessages << message
                callRealMethod()
        }
        mockPushNotificationHandler.sendMessage(_ as Message) >> {
            message -> return NotificationSendResult.success()
        }

        notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), mockPushNotificationHandler)
    }

    def cleanup() {
        // Remove all alarms
        def alarms = alarmService.getAlarms(managerTestSetup.realmBuildingName, null, null, null)
        if (alarms.size() > 0) {
            alarmService.removeAlarms(alarms, (List<Long>) alarms.collect { it.id })
        }

        // Remove all rulesets
        List<RealmRuleset> rulesets = rulesetStorageService.findAll(RealmRuleset.class, new RulesetQuery().setRealm(managerTestSetup.realmBuildingName).setLanguages(Ruleset.Lang.JSON))
        rulesets.forEach { ruleset -> rulesetStorageService.delete(RealmRuleset.class, ruleset.id) }

        // Clear notifications
        notificationMessages.clear()
    }

    @Shared
    createAlarmRule = { severity, assigneeId ->
        when: "a ruleset with a notification action is created in the building realm"
        def rulesStr = getClass().getResource("/org/openremote/test/rules/CO2AlarmRule.json").text
        JsonRulesetDefinition rulesetDef = ValueUtil.JSON.readValue(rulesStr, JsonRulesetDefinition.class)
        rulesetDef.rules[0].when.groups.get(0).items.get(0).assets.ids(managerTestSetup.apartment2LivingroomId)
        ((RuleActionAlarm) rulesetDef.rules[0].then[0]).alarm.setSeverity(severity)
        ((RuleActionAlarm) rulesetDef.rules[0].then[0]).assigneeId = assigneeId
        rulesStr = ValueUtil.JSON.writeValueAsString(rulesetDef)
        def realmRuleset = new RealmRuleset(managerTestSetup.realmBuildingName, "CO2 Alarm Rule", Ruleset.Lang.JSON, rulesStr)
        realmRuleset = rulesetStorageService.merge(realmRuleset)

        then: "the ruleset should reach the engine"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        conditions.eventually {
            assert rulesService.realmEngines.containsKey(managerTestSetup.realmBuildingName)
            def deployment = rulesService.realmEngines.get(managerTestSetup.realmBuildingName).deployments.get(realmRuleset.id)
            assert deployment != null
            assert deployment.ruleset.version == realmRuleset.version
        }

        realmRuleset
    }

    def "rule in realm with alarm action creates an alarm with severity '#severity', assignee '#assigneeId' and '#emailNotifications' emailNotifications based on attribute event"() {
        def realmRuleset = createAlarmRule(severity, assigneeId)
getLOG()
        when: "the room linked attribute is updated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment2LivingroomId, "co2Level", 6000))

        then: "the linked co2Level attribute should equal the event value"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        conditions.eventually {
            def asset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("co2Level").flatMap { it.getValue() }.orElse(null) == 6000
        }

        and: "one alarm is created"
        def alarms = null
        conditions.eventually {
            alarms = alarmService.getAlarms(managerTestSetup.realmBuildingName, null, null, null)
            assert alarms.size() == 1
        }

        and: "the created alarm matches the rule action configuration"
        def alarm = alarms.get(0)
        alarm.id >= 0L
        alarm.realm == managerTestSetup.realmBuildingName
        alarm.title == "CO2 Alarm Rule"
        alarm.content == String.format("""\
ID: ${managerTestSetup.apartment2LivingroomId}
Asset name: Living Room 2
Attribute name: co2Level
Value: 6000
""")
        alarm.severity == severity
        alarm.status == Alarm.Status.OPEN
        alarm.source == Alarm.Source.REALM_RULESET
        alarm.sourceId == Long.toString(realmRuleset.id)
        alarm.createdOn.toInstant().isAfter(Instant.now().minusSeconds(30))
        alarm.createdOn.toInstant().isBefore(Instant.now())
        alarm.acknowledgedOn == null
        alarm.createdOn == alarm.lastModified
        alarm.assigneeId == assigneeId

        and: "the asset is linked to the alarm"
        def assetLinks = alarmService.getAssetLinks(alarm.id, managerTestSetup.realmBuildingName)
        assetLinks.size() == 1
        def assetLink = assetLinks.get(0)
        assetLink.id.alarmId == alarm.id
        assetLink.id.realm == managerTestSetup.realmBuildingName
        assetLink.id.assetId == managerTestSetup.apartment2LivingroomId
        assetLink.createdOn.toInstant().isAfter(Instant.now().minusSeconds(30))
        assetLink.createdOn.toInstant().isBefore(Instant.now())
        assetLink.assetName == "Living Room 2"
        assetLink.parentAssetName == "Apartment 2"

        and: "the expected number e-mail notifications matches"
        conditions.eventually {
            notificationMessages.size() == emailNotifications
        }

        where:
        severity              | assigneeId                    | emailNotifications
        Alarm.Severity.LOW    | null                          | 0
        Alarm.Severity.LOW    | keycloakTestSetup.testuser1Id | 0
        Alarm.Severity.MEDIUM | null                          | 0
        Alarm.Severity.MEDIUM | keycloakTestSetup.testuser2Id | 0
        Alarm.Severity.HIGH   | null                          | 1
        Alarm.Severity.HIGH   | keycloakTestSetup.testuser3Id | 1
        Alarm.Severity.HIGH   | alarmsReadWriteUserId         | 1
    }

    def "rule does not create alarm when rules service is restarted"() {
        def realmRuleset = createAlarmRule(Alarm.Severity.HIGH, null)

        when: "the room linked attribute is updated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment2LivingroomId, "co2Level", 6000))

        then: "the linked co2Level attribute should equal the event value"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        conditions.eventually {
            def asset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("co2Level").flatMap { it.getValue() }.orElse(null) == 6000
        }

        and: "one alarm is created"
        conditions.eventually {
            def alarms = alarmService.getAlarms(managerTestSetup.realmBuildingName, null, null, null)
            assert alarms.size() == 1
        }

        when: "the rules service is stopped"
        rulesService.stop(container)

        then: "the realm engine and rule is no longer deployed"
        conditions.eventually {
            assert !rulesService.realmEngines.containsKey(managerTestSetup.realmBuildingName)
        }

        and: "the linked co2Level attribute still equals the event value"
        def asset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
        asset.getAttribute("co2Level").flatMap { it.getValue() }.orElse(null) == 6000

        when: "the rules service is started"
        rulesService.start(container)

        then: "the rule is deployed again"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(managerTestSetup.realmBuildingName).deployments.get(realmRuleset.id)
            assert deployment != null
            assert deployment.ruleset.version == realmRuleset.version
        }

        and: "the number of created alarms remains the same"
        def conditions2 = new PollingConditions(initialDelay: 5, timeout: 6, delay: 0.2)
        conditions2.eventually {
            def alarms = alarmService.getAlarms(managerTestSetup.realmBuildingName, null, null, null)
            assert alarms.size() == 1
        }
    }

}
