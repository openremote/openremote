package org.openremote.test.protocol

import org.openremote.agent.protocol.timer.TimerProtocol
import org.openremote.agent.protocol.timer.TimerValue
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService

import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.query.AssetQuery
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.AgentLink
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.AttributeValueType
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import org.quartz.CronTrigger
import org.quartz.JobKey
import org.quartz.impl.matchers.GroupMatcher
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class TimerProtocolTest extends Specification implements ManagerContainerTrait {
    def "Check timer protocol agent and device asset deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0)

        when: "the container starts"
        def container = startContainerWithDemoScenesAndRules(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def timerProtocol = container.getService(TimerProtocol.class)
        Asset sceneAgent
        Asset apartment1

        then: "the container should be running and attributes linked"
        conditions.eventually {
            assert isContainerRunning()
            assert noEventProcessedIn(assetProcessingService, 500)

            apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            sceneAgent = assetStorageService.find(new AssetQuery().names("Scene Agent").types(AssetType.AGENT).parents(managerDemoSetup.apartment1Id))
            sceneAgent = assetStorageService.find(sceneAgent.id, true)
            assert apartment1.getAttribute("daySceneTimeFRIDAY").get().getValueAsString().orElse("") == "08:30:00"
            assert sceneAgent != null
        }

        and: "the quartz scheduler is running and contains all the demo triggers with the correct cron expression"
        conditions.eventually {
            assert timerProtocol.cronScheduler != null
            assert timerProtocol.cronScheduler.scheduler != null
            assert timerProtocol.cronScheduler.scheduler.isStarted()
            assert timerProtocol.cronScheduler.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).size() >= 28
        }

        and: "the quartz job has the correct time"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "daySceneFRIDAY")
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

        and: "all protocol linked attributes should be linked"
        conditions.eventually {
            assert timerProtocol.linkedAttributes.size() == 56
        }

        when: "a trigger is disabled"
        def disableScene = new AttributeEvent(managerDemoSetup.apartment1Id, "daySceneEnabledFRIDAY", Values.create(false))
        assetProcessingService.sendAttributeEvent(disableScene)

        then: "the corresponding cron job should be removed from the cron scheduler"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "daySceneFRIDAY")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail == null
            assert triggers.isEmpty()
        }

        and: "all protocol linked attributes should be re-linked"
        conditions.eventually {
            assert timerProtocol.linkedAttributes.size() == 56
        }
    
        when: "the trigger is re-enabled"
        def enableScene = new AttributeEvent(managerDemoSetup.apartment1Id, "daySceneEnabledFRIDAY", Values.create(true))
        assetProcessingService.sendAttributeEvent(enableScene)

        then: "the quartz job should be recreated and have the correct time"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "daySceneFRIDAY")
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
        def modifyTime = new AttributeEvent(managerDemoSetup.apartment1Id, "daySceneTimeFRIDAY", Values.create("04:00:00"))
        assetProcessingService.sendAttributeEvent(modifyTime)

        then: "the quartz job should have the new trigger time"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "daySceneFRIDAY")
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
                new AssetAttribute("daySceneCronFRIDAY", AttributeValueType.STRING)
                    .addMeta(
                        AgentLink.asAgentLinkMetaItem(new AttributeRef(sceneAgent.id, "daySceneFRIDAY")),
                        TimerValue.CRON_EXPRESSION.asMetaItem()
                )
        )
        apartment1 = assetStorageService.merge(apartment1)

        then: "the attributes value should contain the timers cron expression"
        conditions.eventually {
            apartment1 = assetStorageService.find(apartment1.id, true)
            apartment1.getAttribute("daySceneCronFRIDAY").get().getValueAsString().get() == "0 0 4 ? * FRI *"
        }

        when: "the trigger cron expression is modified"
        def modifyCron = new AttributeEvent(managerDemoSetup.apartment1Id, "daySceneCronFRIDAY", Values.create("0 0 4 ? * MON,FRI *"))
        assetProcessingService.sendAttributeEvent(modifyCron)

        then: "the quartz job should have the new cron expression"
        conditions.eventually {
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "daySceneFRIDAY")
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

        when: "a timer action is executed"
        timerProtocol.doTriggerAction(sceneAgent.getAttribute("daySceneFRIDAY").get())

        then: "the linked macro should have been executed"
        conditions.eventually {
            apartment1 = assetStorageService.find(apartment1.id, true)
            apartment1.getAttribute("dayScene").get().getValueAsString().get() == "COMPLETED"
            apartment1.getAttribute("lastExecutedScene").get().getValueAsString().get() == "DAY"
        }

        when: "a trigger is deleted"
        sceneAgent = assetStorageService.find(sceneAgent.id)
        sceneAgent.removeAttribute("daySceneFRIDAY")
        sceneAgent = assetStorageService.merge(sceneAgent)

        then: "the corresponding cron job should be removed from the cron scheduler"
        conditions.eventually {
            assert !sceneAgent.getAttribute("daySceneFRIDAY").isPresent()
            def awaySceneFridayRef = new AttributeRef(sceneAgent.id, "daySceneFRIDAY")
            def timerId = timerProtocol.getTimerId(awaySceneFridayRef)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail == null
            assert triggers.isEmpty()
        }
    }
}
