package org.openremote.test.rules

import elemental.json.Json
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesDeployment
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.server.setup.builtin.RulesDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.model.asset.AssetAttributes
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class ApartmentActionsTest extends Specification implements ManagerContainerTrait {

    def "Check rules RHS action execution"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 5)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesDemoSetup = container.getService(SetupService.class).getTaskOfType(RulesDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesDeployment customerAEngine

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
        }

        and: "the demo attributes marked with RULES_FACT = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.facts.size() == 8
            assert customerAEngine.facts.size() == 8
        }

        and: "the room lights in an apartment to be on"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert new AssetAttributes(livingRoomAsset.getAttributes()).get("lightSwitch").valueAsBoolean

        when: "the ALL LIGHTS OFF switch is set to off for an apartment"
        def apartment1AllLightsOffChange = new AttributeEvent(
                managerDemoSetup.apartment1Id, "allLightsOffSwitch", Json.create(false)
        )
        assetProcessingService.updateAttributeValue(apartment1AllLightsOffChange)

        then: "the room lights in the apartment should be off"
        conditions.eventually {
            livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !new AssetAttributes(livingRoomAsset.getAttributes()).get("lightSwitch").valueAsBoolean
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
