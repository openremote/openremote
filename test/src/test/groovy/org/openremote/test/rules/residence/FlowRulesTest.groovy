package org.openremote.test.rules.residence


import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.flow.NodeCollection
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.rules.RulesetStatus.DEPLOYED

class FlowRulesTest extends Specification implements ManagerContainerTrait {

    def "Execute flow rules"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def startTemperature = 25

        and: "relevant attributes have been populated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerTestSetup.apartment1LivingroomId,
                "targetTemperature",
                startTemperature
        ))

        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerTestSetup.apartment1Bedroom1Id,
                "targetTemperature",
                0
        ))

        when: "a valid node collection is added"
        String json = getClass().getResource("/org/openremote/test/rules/BasicFlowRules.json").text
        json = json.replaceAll("%LIVING ROOM ID%", managerTestSetup.apartment1LivingroomId)
        json = json.replaceAll("%BEDROOM ID%", managerTestSetup.apartment1Bedroom1Id)
        NodeCollection realCollection = ValueUtil.JSON.readValue(json, NodeCollection.class)
        def ruleset = (new GlobalRuleset(
                realCollection.name,
                Ruleset.Lang.FLOW,
                json
        ))
        rulesetStorageService.merge(ruleset)

        then: "the flow should be deployed"
        conditions.eventually {
            assert rulesService.globalEngine != null
            assert rulesService.globalEngine.isRunning()
            assert rulesService.globalEngine.deployments.values().any({ it.name == realCollection.name && it.status == DEPLOYED})
        }

        and: "the flow should be executed correctly"
        conditions.eventually {
            def bedroomTargetTemp = assetStorageService.
                    find(managerTestSetup.apartment1Bedroom1Id).
                    getAttribute("targetTemperature").flatMap{it.value}.orElse(0d)
            assert bedroomTargetTemp.intValue() == (startTemperature.intValue() + 10) : ("it was actually " +  bedroomTargetTemp.intValue())//convert to int considering floating point inaccuracy
        }

    }
}
