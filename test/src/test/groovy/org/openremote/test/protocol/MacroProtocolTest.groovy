package org.openremote.test.protocol

import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerTestSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeExecuteStatus
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

class MacroProtocolTest extends Specification implements ManagerContainerTrait {
    def "Check macro agent and device asset deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0)

        when: "the container starts"
        def container = startContainerWithDemoScenesAndRules(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)

        and: "the clock is stopped for testing purposes"
        stopPseudoClock()

        then: "the container should be running and attributes linked"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 500)

            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment1.getAttribute("morningScene", AttributeExecuteStatus.class).get().getValue().orElse(null) == AttributeExecuteStatus.READY
            assert apartment1.getAttribute("dayScene", AttributeExecuteStatus.class).get().getValue().orElse(null) == AttributeExecuteStatus.READY
            assert apartment1.getAttribute("eveningScene", AttributeExecuteStatus.class).get().getValue().orElse(null) == AttributeExecuteStatus.READY
            assert apartment1.getAttribute("nightScene", AttributeExecuteStatus.class).get().getValue().orElse(null) == AttributeExecuteStatus.READY
            assert !apartment1.getAttribute("morningSceneAlarmEnabled").flatMap{it.value}.orElse(true)
            assert apartment1.getAttribute("morningSceneTargetTemperature", Double.class).get().getValue().orElse(0d) == 21d
        }

        when: "Apartment 1 home scene is executed"
        def macroExecute = new AttributeEvent(managerTestSetup.apartment1Id, "morningScene", AttributeExecuteStatus.REQUEST_START)
        assetProcessingService.sendAttributeEvent(macroExecute)

        then: "Apartment 1 alarm enabled, last scene and living room target temp attribute values should be updated to match the scene"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            def livingRoom = assetStorageService.find(managerTestSetup.apartment1LivingroomId, true)
            assert !apartment1.getAttribute("alarmEnabled").flatMap{it.value}.orElse(true)
            assert apartment1.getAttribute("lastExecutedScene").get().getValue().orElse("") == "MORNING"
            assert livingRoom.getAttribute("targetTemperature", Double.class).get().getValue().orElse(0d) == 21d
        }

        then: "Apartment 1 home scene attribute status should show as COMPLETED"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment1.getAttribute("morningScene", AttributeExecuteStatus.class).get().getValue().orElse(null) == AttributeExecuteStatus.COMPLETED
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "Apartment 1 away scene is executed"
        macroExecute = new AttributeEvent(managerTestSetup.apartment1Id, "dayScene", AttributeExecuteStatus.REQUEST_START)
        assetProcessingService.sendAttributeEvent(macroExecute)

        then: "Apartment 1 alarm enabled, last scene and living room target temp attribute values should be update to match the scene"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            def livingRoom = assetStorageService.find(managerTestSetup.apartment1LivingroomId, true)
            assert apartment1.getAttribute("alarmEnabled").flatMap{it.value}.orElse(false)
            assert apartment1.getAttribute("lastExecutedScene").get().getValue().orElse("") == "DAY"
            assert livingRoom.getAttribute("targetTemperature", Double.class).get().getValue().orElse(0d) == 15d
        }

        then: "Apartment 1 away scene attribute status should show as COMPLETED"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment1.getAttribute("dayScene", AttributeExecuteStatus.class).get().getValue().orElse(null) == AttributeExecuteStatus.COMPLETED
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "The target temperature of the home scene is modified via the apartment attribute"
        def updateTargetTemp = new AttributeEvent(managerTestSetup.apartment1Id, "morningSceneTargetTemperature", 10d)
        assetProcessingService.sendAttributeEvent(updateTargetTemp)

        then: "Apartment 1 home target temp should show new value"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment1.getAttribute("morningSceneTargetTemperature", Double.class).get().getValue().orElse(0d) == 10d
            assert !apartment1.getAttribute("morningSceneAlarmEnabled").flatMap{it.value}.orElse(true)
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "Apartment 1 home scene is executed"
        macroExecute = new AttributeEvent(managerTestSetup.apartment1Id, "morningScene", AttributeExecuteStatus.REQUEST_START)
        assetProcessingService.sendAttributeEvent(macroExecute)

        then: "Apartment 1 alarm enabled, last scene and living room target temp attribute values should be updated to match the scene"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            def livingRoom = assetStorageService.find(managerTestSetup.apartment1LivingroomId, true)
            assert !apartment1.getAttribute("alarmEnabled").flatMap{it.value}.orElse(true)
            assert apartment1.getAttribute("lastExecutedScene").get().getValue().orElse("") == "MORNING"
            assert livingRoom.getAttribute("targetTemperature",Double.class).get().getValue().orElse(0d) == 10d
        }

        then: "Apartment 1 home scene attribute status should show as COMPLETED"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment1.getAttribute("morningScene", AttributeExecuteStatus.class).get().getValue().orElse(null) == AttributeExecuteStatus.COMPLETED
        }
    }
}
