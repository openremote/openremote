package org.openremote.test.protocol

import org.openremote.agent.protocol.timer.TimerProtocol
import org.openremote.agent.protocol.timer.TimerValue
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.AgentLink
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.AttributeType
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import org.quartz.CronTrigger
import org.quartz.JobKey
import org.quartz.impl.matchers.GroupMatcher
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.server.setup.builtin.BuiltinSetupTasks.SETUP_IMPORT_DEMO_SCENES

class TimerProtocolTest extends Specification implements ManagerContainerTrait {
    def "Check timer protocol agent and device asset deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0)

        when: "the container starts"
        def serverPort = findEphemeralPort()
        def container = startContainerWithDemoScenesAndRules(defaultConfig(serverPort) << [(SETUP_IMPORT_DEMO_SCENES): "true"], defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def timerProtocol = container.getService(TimerProtocol.class)
        ServerAsset sceneAgent
        ServerAsset apartment1

        then: "the container should be running and attributes linked"
        conditions.eventually {
            assertNothingProcessedFor(assetProcessingService, 500)

            apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            sceneAgent = assetStorageService.find(new AssetQuery().name("Scene Agent").type(AssetType.AGENT).parent(managerDemoSetup.apartment1Id))
            assert apartment1.getAttribute("awaySceneTimeFRIDAY").get().getValueAsString().orElse("") == "08:30:00"
            assert sceneAgent != null
        }

        and: "the quartz scheduler is running and contains all the demo triggers with the correct cron expression"
        conditions.eventually {
            assert timerProtocol.cronScheduler != null
            assert timerProtocol.cronScheduler.scheduler != null
            assert timerProtocol.cronScheduler.scheduler.isStarted()
            assert timerProtocol.cronScheduler.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).size() == 28
        }

        and: "the quartz job has the correct time"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "awaySceneFriday")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail != null
            assert triggers.size() == 1
            assert triggers[0] instanceof CronTrigger
            def cronTrigger = (CronTrigger)triggers[0]
            assert cronTrigger.cronExpression == "0 30 8 ? * FRI *"
        }

        when: "a trigger is disabled"
        def disableScene = new AttributeEvent(managerDemoSetup.apartment1Id, "awaySceneEnabledFRIDAY", Values.create(false))
        assetProcessingService.sendAttributeEvent(disableScene)

        then: "the corresponding cron job should be removed from the cron scheduler"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "awaySceneFriday")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail == null
            assert triggers.isEmpty()
        }

        when: "the trigger is re-enabled"
        def enableScene = new AttributeEvent(managerDemoSetup.apartment1Id, "awaySceneEnabledFRIDAY", Values.create(true))
        assetProcessingService.sendAttributeEvent(enableScene)

        then: "the quartz job should be recreated and have the correct time"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "awaySceneFriday")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail != null
            assert triggers.size() == 1
            assert triggers[0] instanceof CronTrigger
            def cronTrigger = (CronTrigger)triggers[0]
            assert cronTrigger.cronExpression == "0 30 8 ? * FRI *"
        }

        when: "a trigger time is modified"
        def modifyTime = new AttributeEvent(managerDemoSetup.apartment1Id, "awaySceneTimeFRIDAY", Values.create("04:00:00"))
        assetProcessingService.sendAttributeEvent(modifyTime)

        then: "the quartz job should have the new trigger time"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "awaySceneFriday")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail != null
            assert triggers.size() == 1
            assert triggers[0] instanceof CronTrigger
            def cronTrigger = (CronTrigger)triggers[0]
            assert cronTrigger.cronExpression == "0 0 4 ? * FRI *"
        }

        when: "an attribute is added that links to a timers cron expression"
        apartment1.addAttributes(
                new AssetAttribute("awaySceneCronFRIDAY", AttributeType.STRING)
                    .addMeta(
                        AgentLink.asAgentLinkMetaItem(new AttributeRef(sceneAgent.id, "awaySceneFriday")),
                        TimerValue.CRON_EXPRESSION.asMetaItem()
                )
        )
        apartment1 = assetStorageService.merge(apartment1)

        then: "the attributes value should contain the timers cron expression"
        conditions.eventually {
            apartment1 = assetStorageService.find(apartment1.id, true)
            apartment1.getAttribute("awaySceneCronFRIDAY").get().getValueAsString() == "0 0 4 ? * FRI *"
        }

        when: "the trigger cron expression is modified"
        def modifyCron = new AttributeEvent(managerDemoSetup.apartment1Id, "awaySceneCronFRIDAY", Values.create("0 0 4 ? * MON,FRI *"))
        assetProcessingService.sendAttributeEvent(modifyCron)

        then: "the quartz job should have the new cron expression"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "awaySceneFriday")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail != null
            assert triggers.size() == 1
            assert triggers[0] instanceof CronTrigger
            def cronTrigger = (CronTrigger)triggers[0]
            assert cronTrigger.cronExpression == "0 0 4 ? * MON,FRI *"
        }

        when: "a trigger is deleted"
        sceneAgent = assetStorageService.find(sceneAgent.id)
        sceneAgent.removeAttribute("awaySceneFriday")
        sceneAgent = assetStorageService.merge(sceneAgent)

        then: "the corresponding cron job should be removed from the cron scheduler"
        conditions.eventually {
            assert !sceneAgent.getAttribute("awaySceneFriday").isPresent()
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "awaySceneFriday")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail == null
            assert triggers.isEmpty()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}