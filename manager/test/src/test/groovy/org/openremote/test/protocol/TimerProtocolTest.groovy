package org.openremote.test.protocol

import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeExecuteStatus
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class TimerProtocolTest extends Specification implements ManagerContainerTrait {
    // TODO: Test the timer protocol
    @Ignore
    def "Check time trigger protocol"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0)

        when: "the container starts"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoRules(defaultConfig(serverPort), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        then: "the container should be running and attributes linked"
        conditions.eventually {
            assert isContainerRunning()
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment1.getAttribute("home").get().getValueAsString().orElse("") == AttributeExecuteStatus.READY.toString()
            assert apartment1.getAttribute("away").get().getValueAsString().orElse("") == AttributeExecuteStatus.READY.toString()
            assert apartment1.getAttribute("evening").get().getValueAsString().orElse("") == AttributeExecuteStatus.READY.toString()
            assert apartment1.getAttribute("night").get().getValueAsString().orElse("") == AttributeExecuteStatus.READY.toString()
            assert !apartment1.getAttribute("homeAlarmEnabled").get().getValueAsBoolean().orElse(true)
            assert apartment1.getAttribute("homeTargetTemperature").get().getValueAsNumber().orElse(0d) == 21d
        }

        when: "Apartment 1 home scene is executed"
        def macroExecute = new AttributeEvent(managerDemoSetup.apartment1Id, "home", AttributeExecuteStatus.REQUEST_START.asValue())
        assetProcessingService.sendAttributeEvent(macroExecute)

        then: "Apartment 1 alarm enabled, last scene and living room target temp attribute values should be updated to match the scene"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            def thermostat1 = assetStorageService.find(managerDemoSetup.apartment1LivingroomThermostatId, true)
            assert !apartment1.getAttribute("alarmEnabled").get().getValueAsBoolean().orElse(true)
            assert apartment1.getAttribute("lastExecutedScene").get().getValueAsString().orElse("") == "HOME"
            assert thermostat1.getAttribute("targetTemperature").get().getValueAsNumber().orElse(0d) == 21d
        }

        then: "Apartment 1 home scene attribute status should show as COMPLETED"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment1.getAttribute("home").get().getValueAsString().orElse("") == AttributeExecuteStatus.COMPLETED.toString()
        }

        when: "Apartment 1 away scene is executed"
        macroExecute = new AttributeEvent(managerDemoSetup.apartment1Id, "away", AttributeExecuteStatus.REQUEST_START.asValue())
        assetProcessingService.sendAttributeEvent(macroExecute)

        then: "Apartment 1 alarm enabled, last scene and living room target temp attribute values should be update to match the scene"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            def thermostat1 = assetStorageService.find(managerDemoSetup.apartment1LivingroomThermostatId, true)
            assert apartment1.getAttribute("alarmEnabled").get().getValueAsBoolean().orElse(false)
            assert apartment1.getAttribute("lastExecutedScene").get().getValueAsString().orElse("") == "AWAY"
            assert thermostat1.getAttribute("targetTemperature").get().getValueAsNumber().orElse(0d) == 15d
        }

        then: "Apartment 1 away scene attribute status should show as COMPLETED"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment1.getAttribute("away").get().getValueAsString().orElse("") == AttributeExecuteStatus.COMPLETED.toString()
        }

        when: "The target temperature of the home scene is modified via the apartment attribute"
        def updateTargetTemp = new AttributeEvent(managerDemoSetup.apartment1Id, "homeTargetTemperature", Values.create(10d))
        assetProcessingService.sendAttributeEvent(updateTargetTemp)

        then: "Apartment 1 home scene attribute status should reset to show as READY and home target temp should show new value"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment1.getAttribute("home").get().getValueAsString().orElse("") == AttributeExecuteStatus.READY.toString()
            assert apartment1.getAttribute("homeTargetTemperature").get().getValueAsNumber().orElse(0d) == 10d
            assert !apartment1.getAttribute("homeAlarmEnabled").get().getValueAsBoolean().orElse(true)
        }

        when: "Apartment 1 home scene is executed"
        macroExecute = new AttributeEvent(managerDemoSetup.apartment1Id, "home", AttributeExecuteStatus.REQUEST_START.asValue())
        assetProcessingService.sendAttributeEvent(macroExecute)

        then: "Apartment 1 alarm enabled, last scene and living room target temp attribute values should be updated to match the scene"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            def thermostat1 = assetStorageService.find(managerDemoSetup.apartment1LivingroomThermostatId, true)
            assert !apartment1.getAttribute("alarmEnabled").get().getValueAsBoolean().orElse(true)
            assert apartment1.getAttribute("lastExecutedScene").get().getValueAsString().orElse("") == "HOME"
            assert thermostat1.getAttribute("targetTemperature").get().getValueAsNumber().orElse(0d) == 10d
        }

        then: "Apartment 1 home scene attribute status should show as COMPLETED"
        conditions.eventually {
            def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment1.getAttribute("home").get().getValueAsString().orElse("") == AttributeExecuteStatus.COMPLETED.toString()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}