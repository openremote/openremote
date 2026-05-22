package org.openremote.test.rules

import org.openremote.manager.notification.EmailNotificationHandler
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.notification.AbstractNotificationMessage
import org.openremote.model.notification.Notification
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.value.MetaItemType
import org.openremote.model.notification.NotificationSendResult
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

import static org.openremote.model.rules.RulesetStatus.DEPLOYED

class RuleResetImmediate extends Specification implements ManagerContainerTrait {

    def "Brightness repeatedly above threshold triggers rule again only with reset immediate"() {
        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def notificationService = container.getService(NotificationService.class)
        def emailNotificationHandler = container.getService(EmailNotificationHandler.class)
        RulesEngine engine = null
        def sentMessages = new CopyOnWriteArrayList<AbstractNotificationMessage>()

        and: "email notifications are captured in-memory"
        EmailNotificationHandler mockEmailNotificationHandler = Spy(emailNotificationHandler)
        mockEmailNotificationHandler.isValid() >> true
        mockEmailNotificationHandler.sendMessage(_ as Long, _ as Notification.Source, _ as String, _ as Notification.Target, _ as AbstractNotificationMessage) >> {
            id, source, sourceId, target, message ->
                sentMessages << message
        }
        mockEmailNotificationHandler.sendMessage(_ as jakarta.mail.Message) >> {
            message -> NotificationSendResult.success()
        }
        notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), mockEmailNotificationHandler)

        and: "two light assets are added, but only one has the reset immediate meta item"
        def resetLightAsset = new LightAsset("Reset immediate light")
                .setId(UniqueIdentifierGenerator.generateId("Reset immediate light"))
                .setRealm(keycloakTestSetup.realmBuilding.name)
        resetLightAsset.getAttributes().getOrCreate(LightAsset.BRIGHTNESS)
                .addMeta(new MetaItem<>(MetaItemType.RULE_STATE))
                .addMeta(new MetaItem<>(MetaItemType.RULE_RESET_IMMEDIATE))
        resetLightAsset = assetStorageService.merge(resetLightAsset)

        def normalLightAsset = new LightAsset("Normal light")
                .setId(UniqueIdentifierGenerator.generateId("Normal light"))
                .setRealm(keycloakTestSetup.realmBuilding.name)
        normalLightAsset.getAttributes().getOrCreate(LightAsset.BRIGHTNESS)
                .addMeta(new MetaItem<>(MetaItemType.RULE_STATE))
        normalLightAsset = assetStorageService.merge(normalLightAsset)

        and: "a JSON rule notifies when brightness is above the threshold"
        def ruleset = new RealmRuleset(
                keycloakTestSetup.realmBuilding.name,
                "Brightness reset immediate rule",
                Ruleset.Lang.JSON,
                getClass().getResource("/org/openremote/test/rules/JsonRuleResetImmediate.json").text)
        ruleset = rulesetStorageService.merge(ruleset)

        expect: "the custom asset states and ruleset should be loaded into the rule engine"
        conditions.eventually {
            engine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert engine != null
            assert engine.isRunning()
            assert engine.deployments.get(ruleset.id)?.status == DEPLOYED
            assert engine.hasPreviouslyFired()
            assert engine.facts.assetStates.any { it.id == resetLightAsset.id && it.name == LightAsset.BRIGHTNESS.name }
            assert engine.facts.assetStates.any { it.id == normalLightAsset.id && it.name == LightAsset.BRIGHTNESS.name }
        }

        when: "the reset-immediate light enters the matching range"
        advancePseudoClock(1, TimeUnit.SECONDS, container)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(resetLightAsset.id, LightAsset.BRIGHTNESS, 100))

        then: "the rule action fires once"
        conditions.eventually {
            assert sentMessages.size() == 1
        }

        when: "the same matching value is received again for the reset-immediate light"
        advancePseudoClock(1, TimeUnit.SECONDS, container)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(resetLightAsset.id, LightAsset.BRIGHTNESS, 100))

        then: "the rule action fires again"
        conditions.eventually {
            assert sentMessages.size() == 2
        }

        when: "the normal light enters the matching range"
        advancePseudoClock(1, TimeUnit.SECONDS, container)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(normalLightAsset.id, LightAsset.BRIGHTNESS, 100))

        then: "the rule action fires for the first matching event"
        conditions.eventually {
            assert sentMessages.size() == 3
        }

        when: "the same matching value is received again for the normal light"
        advancePseudoClock(1, TimeUnit.SECONDS, container)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(normalLightAsset.id, LightAsset.BRIGHTNESS, 100))

        then: "the rule action does not fire again without reset immediate"
        Thread.sleep(1000)
        sentMessages.size() == 3

        cleanup:
        if (notificationService != null && emailNotificationHandler != null) {
            notificationService.notificationHandlerMap.put(emailNotificationHandler.getTypeName(), emailNotificationHandler)
        }
    }
}
