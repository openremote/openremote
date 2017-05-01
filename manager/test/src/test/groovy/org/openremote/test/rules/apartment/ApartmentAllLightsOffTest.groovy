package org.openremote.test.rules.apartment

import elemental.json.Json
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_2

class ApartmentAllLightsOffTest extends Specification implements ManagerContainerTrait {

    def "Turn all lights off"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine apartment2Engine

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment2Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.isRunning()
        }

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
        }

        and: "the room lights in an apartment to be on"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
        assert livingRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean

        when: "the ALL LIGHTS OFF push-button is pressed for an apartment"
        setPseudoClocksToRealTime(container, apartment2Engine)
        def lightsOffEvent = new AttributeEvent(
                managerDemoSetup.apartment2Id, "allLightsOffSwitch", Json.create(true), getClockTimeOf(container)
        )
        assetProcessingService.sendAttributeEvent(lightsOffEvent)

        then: "the room lights in the apartment should be off"
        conditions.eventually {
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2 + 1
            livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert !livingRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean
        }

        when: "time advanced"
        advancePseudoClocks(15, SECONDS, container, apartment2Engine)

        then: "event expired"
        conditions.eventually {
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
        }

        cleanup: "stop the container"
        stopContainer(container)
    }
}
