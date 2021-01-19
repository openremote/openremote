package org.openremote.test.protocol

import org.openremote.agent.protocol.macro.MacroAgent
import org.openremote.agent.protocol.timer.CronScheduler
import org.openremote.agent.protocol.timer.TimerAgent
import org.openremote.agent.protocol.timer.TimerProtocol
import org.openremote.agent.protocol.timer.TimerValue
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerTestSetup
import org.openremote.model.asset.Asset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeExecuteStatus
import org.openremote.model.attribute.MetaItem
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
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
        def agentService = container.getService(AgentService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        List<MacroAgent> sceneAgents
        List<TimerAgent> timerAgents
        Asset apartment1

        then: "the container should be running and attributes linked"
        conditions.eventually {
            assert isContainerRunning()
            assert noEventProcessedIn(assetProcessingService, 500)

            apartment1 = assetStorageService.find(managerTestSetup.apartment1Id, true)
            sceneAgents = assetStorageService.findAll(new AssetQuery().names(new StringPredicate(AssetQuery.Match.BEGIN, "Scene agent")).types(MacroAgent.class).parents(managerTestSetup.apartment1Id)).asList() as List<MacroAgent>
            timerAgents = agentService.agentMap.values().findAll {it.getName().startsWith("Timer agent") && it.parentId == managerTestSetup.apartment1Id} as List<TimerAgent>
            assert apartment1 != null
            assert apartment1.getAttribute("daySceneTimeFRIDAY").get().getValue().orElse("") == "08:30:00"
            assert sceneAgents != null
            assert sceneAgents.size() == 6
            assert timerAgents != null
            assert timerAgents.size() == 28
        }

        and: "the quartz scheduler is running and contains all the demo triggers with the correct cron expression"
        conditions.eventually {
            assert timerAgents.every {((TimerProtocol)agentService.getProtocolInstance(it.id)).cronScheduler != null}
            assert CronScheduler.scheduler != null
            assert CronScheduler.scheduler.isStarted()
            assert CronScheduler.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).size() == timerAgents.size()
        }

        and: "the quartz job has the correct time"
        conditions.eventually {
            def daySceneFridayTimer = timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}
            assert daySceneFridayTimer != null
            def timerId = TimerProtocol.getTimerId(daySceneFridayTimer)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def timerProtocol = ((TimerProtocol)agentService.getProtocolInstance(daySceneFridayTimer.id))
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
            assert timerAgents.every {
                ((TimerProtocol) agentService.getProtocolInstance(it.id)).linkedAttributes.size() == 2
            }
        }

        when: "a trigger is disabled"
        def disableScene = new AttributeEvent(managerTestSetup.apartment1Id, "daySceneEnabledFRIDAY", false)
        assetProcessingService.sendAttributeEvent(disableScene)

        then: "the corresponding cron job should be removed from the cron scheduler"
        conditions.eventually {
            def daySceneFridayTimer = timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}
            assert daySceneFridayTimer != null
            def timerId = TimerProtocol.getTimerId(daySceneFridayTimer)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def timerProtocol = ((TimerProtocol)agentService.getProtocolInstance(daySceneFridayTimer.id))
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail == null
            assert triggers.isEmpty()
        }

        and: "all protocol linked attributes should be re-linked"
        conditions.eventually {
            assert timerAgents.every {
                ((TimerProtocol) agentService.getProtocolInstance(it.id)).linkedAttributes.size() == 2
            }
        }

        when: "the trigger is re-enabled"
        def enableScene = new AttributeEvent(managerTestSetup.apartment1Id, "daySceneEnabledFRIDAY", true)
        assetProcessingService.sendAttributeEvent(enableScene)

        then: "the quartz job should be recreated and have the correct time"
        conditions.eventually {
            def daySceneFridayTimer = timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}
            assert daySceneFridayTimer != null
            def timerId = TimerProtocol.getTimerId(daySceneFridayTimer)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def timerProtocol = ((TimerProtocol)agentService.getProtocolInstance(daySceneFridayTimer.id))
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail != null
            assert triggers.size() == 1
            assert triggers[0] instanceof CronTrigger
            def cronTrigger = (CronTrigger)triggers[0]
            assert cronTrigger.cronExpression == "0 30 8 ? * FRI *"
        }

        when: "a trigger time is modified"
        def modifyTime = new AttributeEvent(managerTestSetup.apartment1Id, "daySceneTimeFRIDAY", "04:00:00")
        assetProcessingService.sendAttributeEvent(modifyTime)

        then: "the quartz job should have the new trigger time"
        conditions.eventually {
            def daySceneFridayTimer = timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}
            assert daySceneFridayTimer != null
            def timerId = TimerProtocol.getTimerId(daySceneFridayTimer)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def timerProtocol = ((TimerProtocol)agentService.getProtocolInstance(daySceneFridayTimer.id))
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
                new Attribute<>("daySceneCronFRIDAY", ValueType.TEXT)
                    .addOrReplaceMeta(
                        new MetaItem<>(MetaItemType.AGENT_LINK, new TimerAgent.TimerAgentLink(timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}.id)
                            .setTimerValue(TimerValue.CRON_EXPRESSION)
                        )
                    )
        )
        apartment1 = assetStorageService.merge(apartment1)

        then: "the attributes value should contain the timers cron expression"
        conditions.eventually {
            apartment1 = assetStorageService.find(apartment1.id, true)
            assert apartment1.getAttribute("daySceneCronFRIDAY").isPresent()
            assert apartment1.getAttribute("daySceneCronFRIDAY").get().getValue().isPresent()
            assert apartment1.getAttribute("daySceneCronFRIDAY").get().getValue().get() == "0 0 4 ? * FRI *"
        }

        when: "the trigger cron expression is modified"
        def modifyCron = new AttributeEvent(managerTestSetup.apartment1Id, "daySceneCronFRIDAY", "0 0 4 ? * MON,FRI *")
        assetProcessingService.sendAttributeEvent(modifyCron)

        then: "the quartz job should have the new cron expression"
        conditions.eventually {
            def daySceneFridayTimer = timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}
            assert daySceneFridayTimer != null
            def timerId = TimerProtocol.getTimerId(daySceneFridayTimer)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            def timerProtocol = ((TimerProtocol)agentService.getProtocolInstance(daySceneFridayTimer.id))
            def jobDetail = timerProtocol.cronScheduler.scheduler.getJobDetail(jobKey)
            def triggers = timerProtocol.cronScheduler.scheduler.getTriggersOfJob(jobKey)
            assert jobDetail != null
            assert triggers.size() == 1
            assert triggers[0] instanceof CronTrigger
            def cronTrigger = (CronTrigger)triggers[0]
            assert cronTrigger.cronExpression == "0 0 4 ? * MON,FRI *"
        }

        when: "a timer action is executed"
        ((TimerProtocol)agentService.getProtocolInstance(timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}.id)).doTriggerAction()

        then: "the linked macro should have been executed"
        conditions.eventually {
            apartment1 = assetStorageService.find(apartment1.id, true)
            assert apartment1.getAttribute("dayScene").get().getValue().get() == AttributeExecuteStatus.COMPLETED
            assert apartment1.getAttribute("lastExecutedScene").get().getValue().get() == "DAY"
        }

        when: "a timer is deleted"
        assetStorageService.delete([timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}.id])

        then: "the corresponding cron job should be removed from the cron scheduler"
        conditions.eventually {
            def daySceneFridayTimer = timerAgents.find {it.name == "Timer agent Day scene FRIDAY"}
            assert daySceneFridayTimer != null
            def timerId = TimerProtocol.getTimerId(daySceneFridayTimer)
            def jobKey = JobKey.jobKey("cronJob1", timerId)
            assert ((TimerProtocol)agentService.getProtocolInstance(daySceneFridayTimer.id)) == null
            assert CronScheduler.scheduler.getJobDetail(jobKey) == null
        }
    }
}
