package org.openremote.test.rules.apartment

import elemental.json.Json
import elemental.json.JsonType
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1

/**
 * Created by ebariaux on 03/05/17.
 */
class ApartmentNotificationWhenAlarmTest  extends Specification implements ManagerContainerTrait {

    def "Trigger notification when presence is detected and alarm is set"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine apartment1Engine = null

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the alarm enabled, presence detected flag and timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("alarmEnabled").get().valueAsBoolean
        assert !livingRoomAsset.getAttribute("presenceDetected").get().valueAsBoolean
        assert livingRoomAsset.getAttribute("lastPresenceDetected").get().value.getType() == JsonType.NULL

        when: "the alarm is enabled"
        setPseudoClocksToRealTime(container, apartment1Engine)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1Id, "alarmEnabled", Json.create(true), getClockTimeOf(container)
        ))

        then: "that value should be stored"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("alarmEnabled").get().valueAsBoolean()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}