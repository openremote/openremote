package org.openremote.test.rules.residence

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.setup.integration.ManagerTestSetup.DEMO_RULE_STATES_APARTMENT_2

@Ignore
class ResidenceAllLightsOffTest extends Specification implements ManagerContainerTrait {

    def "Turn all lights off"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine apartment2Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerTestSetup.apartment2Id,
            "Demo Apartment - All Lights Off",
            Ruleset.Lang.JAVASCRIPT,
            getClass().getResource("/org/openremote/test/rules/ResidenceAllLightsOff.js").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment2Engine = rulesService.assetEngines.get(managerTestSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.isRunning()
        }

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
        }

        and: "the room lights in an apartment to be on"
        conditions.eventually {
            def livingroomAsset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert livingroomAsset.getAttribute("lightSwitch").get().value.get()
            def bathRoomAsset = assetStorageService.find(managerTestSetup.apartment2BathroomId, true)
            assert bathRoomAsset.getAttribute("lightSwitch").get().value.get()
        }

        when: "the ALL LIGHTS OFF push-button is pressed for an apartment"
        def lightsOffEvent = new AttributeEvent(
                managerTestSetup.apartment2Id, "allLightsOffSwitch", true
        )
        assetProcessingService.sendAttributeEvent(lightsOffEvent)

        then: "the room lights in the apartment should be off"
        conditions.eventually {
            assert apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert apartment2Engine.assetEvents.size() == 1
            def livingroomAsset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert !livingroomAsset.getAttribute("lightSwitch").get().value.get()
            def bathRoomAsset = assetStorageService.find(managerTestSetup.apartment2BathroomId, true)
            assert !bathRoomAsset.getAttribute("lightSwitch").get().value.get()
        }

        when: "time advanced by 15 seconds"
        advancePseudoClock(15, SECONDS, container)

        and: "we turn the light on again in a room"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerTestSetup.apartment2LivingroomId, "lightSwitch", true)
        )

        then: "the light should still be on after a few seconds (the all lights off event expires after 3 seconds)"
        new PollingConditions(timeout: 5, initialDelay: 3).eventually {
            assert apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert apartment2Engine.assetEvents.size() == 0
            def livingroomAsset = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert livingroomAsset.getAttribute("lightSwitch").get().value.get()
        }

            }
}
