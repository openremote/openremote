/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.protocol.simulator

import org.openremote.agent.protocol.simulator.SimulatorAgent
import org.openremote.agent.protocol.simulator.SimulatorAgentLink
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.query.AssetDatapointAllQuery
import org.openremote.model.simulator.SimulatorReplayDatapoint
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.HOURS
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.HAS_PREDICTED_DATA_POINTS


/**
 * Also see {@link org.openremote.test.assets.AssetDatapointTest} for usage of {@link SimulatorProtocol}.
 */
class SimulatorProtocolTest extends Specification implements ManagerContainerTrait {

    static final PollingConditions conditions = new PollingConditions(timeout: 3, delay: 0.2)

    @Shared
    AssetStorageService assetStorageService

    @Shared
    AssetDatapointService assetDatapointService

    @Shared
    AssetPredictedDatapointService assetPredictedDatapointService

    @Shared
    AgentService agentService

    @Shared
    SimulatorAgent agent

    @Shared
    ThingAsset asset

    @Shared
    ScheduledFuture<?> future = Mock(ScheduledFuture)

    // Mock executor and rely on the delay argument to determine schedule
    @Shared
    ScheduledExecutorService executor = Mock(ScheduledExecutorService)

    @Shared
    SimulatorProtocol protocol

    @Shared
    Long delay

    def setupSpec() {
        given: "environment is setup"
        startContainer(defaultConfig(), defaultServices())

        // Setup services
        assetStorageService = container.getService(AssetStorageService.class)
        assetDatapointService = container.getService(AssetDatapointService.class)
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        agentService = container.getService(AgentService.class)

        // Create and link asset to agent
        agent = new SimulatorAgent("Test agent").setRealm(Constants.MASTER_REALM)
        agent = assetStorageService.merge(agent)
    }

    def setup() {
        resetPseudoClockAt()
        stopPseudoClockAt(Instant.parse("1970-01-01T00:00:00.000Z").toEpochMilli())

        future = Mock(ScheduledFuture)
        executor = Mock(ScheduledExecutorService)

        asset = new ThingAsset("Test asset").setRealm(Constants.MASTER_REALM)
        asset = assetStorageService.merge(asset)

        // Must use boxed Long type in Spock closures, so avoiding parameter expansion so it doesn't silently fail.
        executor.schedule(_ as Runnable, _ as Long, _ as TimeUnit) >> { args ->
            delay = args[1] as Long
            // Mock get method on ScheduledFuture to resolve the future manually
            future.get() >> {
                (args[0] as Runnable).run()
                return true
            }
            return future
        }

        agent = (SimulatorAgent) assetStorageService.find(agent.getId(), Agent.class)
        protocol = (SimulatorProtocol) agentService.protocolInstanceMap.get(agent.getId())
        protocol.scheduledExecutorService = executor
    }

    def cleanup() {
        assetStorageService.delete(List.of(asset.getId()))
        conditions.eventually {
            protocol.linkedAttributes.size() == 0
        }
    }

    private getDataPoints = {
        def now = Instant
                .ofEpochMilli(getClockTimeOf(container)).atZone(ZoneId.of("UTC")).toLocalDateTime();
        new AssetDatapointAllQuery(
            now.minus(1, ChronoUnit.HOURS),
            now.plus(1, ChronoUnit.HOURS),
    ) }

    private long getDatapointTimestamp(Attribute attribute) {
        return Math.round(assetDatapointService.queryDatapoints(
                asset.getId(),
                attribute.getName(),
                getDataPoints.call()
        ).get(0).getTimestamp() / 1000)
    }

    def "Check Simulator Agent protocol without replay"() {
        when: "nothing is configured"
        asset.addOrReplaceAttributes(new Attribute<>("test1", ValueType.TEXT)
                .addMeta(new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId())))
        )
        asset = assetStorageService.merge(asset)
        def attribute = asset.getAttribute("test1").get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.get(attributeRef) == attribute
        }

        and: "nothing happens"
        assetDatapointService.getDatapoints(attributeRef).size() == 0
    }

    def "Check Simulator Agent protocol with replay"() {
        when: "replayData is configured to add a datapoint in 1hr, 12hrs, and 24hrs every day"
        asset.addOrReplaceAttributes(new Attribute<>("test2", ValueType.TEXT)
                .addMeta(new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 12, "test"),
                        new SimulatorReplayDatapoint(3600 * 24, "test"))))
        )
        asset = assetStorageService.merge(asset)
        def attribute = asset.getAttribute("test2").get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.get(attributeRef) == attribute
        }

        (0..1).each { i ->
            def days = i * 3600 * 24 // Starts at 0 days, increments with 1 days

            then: "the delay is 1 hour"
            conditions.eventually {
                delay == 3600
            }

            when: "fast forward 1 hour"
            advancePseudoClock(1, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == days + 3600
            }

            and: "the delay is 11 hours"
            conditions.eventually {
                delay == 3600 * 11
            }

            when: "fast forward 11 hours"
            advancePseudoClock(11, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == days + 3600 * 12
            }

            and: "the delay is 12 hours"
            conditions.eventually {
                delay == 3600 * 12
            }

            when: "fast forward 12 hour"
            advancePseudoClock(12, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoints are present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == days + 3600 * 24
            }
        }
    }

    // Won't work because the mocks will overwrite each other
//    def "Check Simulator Agent protocol adds predicted datapoints for multiple attributes"() {
//        when: "replayData is configured for both attributes to add a datapoint in 1hr every day"
//        def attribute1 = asset.getAttribute(ThingAsset.NOTES).get()
//        attribute1.addOrReplaceMeta(
//                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
//                        new SimulatorReplayDatapoint(3600, "test"),
//                ))
//        )
//        def attribute2 = asset.getAttribute("test").get()
//        attribute2.addOrReplaceMeta(
//                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
//                        new SimulatorReplayDatapoint(3600 * 2, "test"),
//                ))
//        )
//        assetStorageService.merge(asset)
//
//        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
//        conditions.eventually {
//            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
//            protocol.linkedAttributes.size() == 1
//        }
//
//        (0..1).each { i ->
//            def days = i * 3600 * 24 // Starts at 0 days, increments with 1 days
//
//            then: "the delay is 1 hour for attribute 1"
//            conditions.eventually {
//                delay == 3600
//            }
//
//            when: "fast forward 1 hour"
//            advancePseudoClock(1, HOURS, container)
//            future.get() // resolve future manually, because we surpassed the delay
//
//            then: "datapoint is present"
//            conditions.eventually {
//                getDatapointTimestamp(attribute1) == days + 3600
//            }
//
//            and: "the delay is 1 more hour for attribute 2"
//            conditions.eventually {
//                delay == 3600
//            }
//
//            when: "fast forward 1 hour"
//            advancePseudoClock(1, HOURS, container)
//            future.get() // resolve future manually, because we surpassed the delay
//
//            then: "datapoint is present"
//            conditions.eventually {
//                getDatapointTimestamp(attribute2) == days + 3600 * 2
//            }
//
//            when: "fast forward 22 hours"
//            advancePseudoClock(22, HOURS, container)
//        }
//        then: ""
//    }

    def "Check Simulator Agent protocol with replay startDate"() {
        when: "replayData is configured to replay in 1day and 1hr"
        asset.addOrReplaceAttributes(new Attribute<>("test3", ValueType.TEXT)
                .addMeta(new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 25, "test"),
                        new SimulatorReplayDatapoint(3600 * 49, "test")
                ).setSchedule(new SimulatorAgentLink.Schedule(
                        Date.from(Instant.parse("1970-01-02T00:00:00.000Z")),
                        null,
                        null
                ))
        )))
        asset = assetStorageService.merge(asset)
        def attribute = asset.getAttribute("test3").get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.get(attributeRef) == attribute
        }

        and: "the delay is 25 hours"
        conditions.eventually {
            delay == 3600 * 25
        }

        when: "fast forward 25 hours"
        advancePseudoClock(25, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "datapoint is present"
        conditions.eventually {
            getDatapointTimestamp(attribute) == 3600 * 25
        }

        // Starting at 2 days, because start date + first datapoint = 2 days since epoch
        (2..3).each { i ->
            def days = i * 3600 * 24 // Starts at 0 days, increments with 1 day

            and: "the delay is 24 hours"
            conditions.eventually {
                delay == 3600 * 24
            }

            when: "fast forward 25 hours"
            advancePseudoClock(24, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == days + 3600
            }
        }

        and: "attribute is removed from replay map"
        conditions.eventually {
            protocol.replayMap.get(attributeRef) == null
        }
    }

    def "Check Simulator Agent protocol with custom recurrence schedule"() {
        when: "replayData is configured to add a datapoint in 1hr, and 2hrs weekly on Mondays"
        asset.addOrReplaceAttributes(new Attribute<>("test5", ValueType.TEXT).addMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 2, "test")
                ).setSchedule(new SimulatorAgentLink.Schedule(
                        // First day should be 1970-01-05 (in 4 days) a Monday
                        Date.from(Instant.parse("1970-01-05T00:00:00.000Z")),
                        null,
                        // Recur every Monday until 1970-01-31
                        "FREQ=WEEKLY;UNTIL=19700131T000000Z;BYDAY=MO"
                ))
        )))
        asset = assetStorageService.merge(asset)
        def attribute = asset.getAttribute("test5").get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.get(attributeRef) == attribute
        }

        then: "the delay is 97 hours"
        conditions.eventually {
            delay == 3600 * 97
        }

        when: "fast forward 97 hours"
        advancePseudoClock(97, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "datapoint is present"
        conditions.eventually {
            getDatapointTimestamp(attribute) == 3600 * 97
        }

        and: "the delay is 1 hour"
        conditions.eventually {
            delay == 3600
        }

        when: "fast forward 1 hour"
        advancePseudoClock(1, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "datapoint is present"
        conditions.eventually {
            getDatapointTimestamp(attribute) == 3600 * 98
        }

        (1..3).each { i ->
            def weeks = i * 3600 * 24 * 7 // Starts at 0 weeks, increments with 1 week
            def firstWeekOffset = 96 * 3600;

            and: "the delay is 1 week and -1 hour"
            conditions.eventually {
                delay == 3600 * 167
            }

            when: "fast forward 1 week and -1 hour"
            advancePseudoClock(167, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == firstWeekOffset + weeks + 3600
            }

            and: "the delay is 1 hour"
            conditions.eventually {
                delay == 3600
            }

            when: "fast forward 1 hour"
            advancePseudoClock(1, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == firstWeekOffset + weeks + 3600 * 2
            }
        }

        and: "the delay is 1 hour"
        conditions.eventually {
            delay == 3600
        }

        when: "fast forward past the end date"
        advancePseudoClock(1000, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "the attributeRef is removed from the replayMap"
        conditions.eventually {
            protocol.replayMap.get(attributeRef) == null
        }
    }

    def "Check Simulator Agent protocol adds predicted datapoints for the current and next cycle"() {
        when: "replayData is configured for both attributes to add a datapoint in 1 and 2 hours every day"
        asset.addOrReplaceAttributes(new Attribute<>("test6", ValueType.TEXT).addMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 2, "test"),
                )),
                new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true))
        )
        asset = assetStorageService.merge(asset)
        def attribute = asset.getAttribute("test6").get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.get(attributeRef) == attribute
        }

        and: "the delay is 1 hour"
        conditions.eventually {
            delay == 3600
        }

        and: "the predicted datapoints are present"
        conditions.eventually {
            def datapoints = assetPredictedDatapointService.getDatapoints(attributeRef)
            datapoints.get(3).getTimestamp()/1000 == 3600
            datapoints.get(2).getTimestamp()/1000 == 3600 * 2
            datapoints.get(1).getTimestamp()/1000 == 3600 * 25
            datapoints.get(0).getTimestamp()/1000 == 3600 * 26
        }

        when: "fast forward 1 hour"
        advancePseudoClock(1, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "datapoint is present"
        conditions.eventually {
            getDatapointTimestamp(attribute) == 3600
        }

        and: "the delay is 1 hour"
        conditions.eventually {
            delay == 3600
        }

        when: "fast forward 1 hour"
        advancePseudoClock(1, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "datapoint is present"
        conditions.eventually {
            getDatapointTimestamp(attribute) == 3600 * 2
        }

        and: "the predicted datapoints are present"
        def datapoints1 = assetPredictedDatapointService.getDatapoints(attributeRef)
        datapoints1.get(5).getTimestamp()/1000 == 3600
        datapoints1.get(4).getTimestamp()/1000 == 3600 * 2
        datapoints1.get(3).getTimestamp()/1000 == 3600 * 25
        datapoints1.get(2).getTimestamp()/1000 == 3600 * 26
        datapoints1.get(1).getTimestamp()/1000 == 3600 * 47 // TODO: is this correct?
        datapoints1.get(0).getTimestamp()/1000 == 3600 * 48

        when: "reset agentLink"
        attribute.addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId())),
        )
        assetStorageService.merge(asset)

        then: "purge datapoints"
        conditions.eventually {
            assetPredictedDatapointService.getDatapoints(attributeRef).size() == 0
        }
    }

    def "Check Simulator Agent protocol adds predicted datapoints for one-time occurrence"() {
        when: "replayData is configured for both attributes to add a datapoint in 1 and 2 hours"
        asset.addOrReplaceAttributes(new Attribute<>("test7", ValueType.TEXT).addMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 2, "test"),
                ).setSchedule(new SimulatorAgentLink.Schedule(
                        Date.from(Instant.parse("1970-01-01T00:00:00.000Z")),
                        null, null
                ))),
                new MetaItem<>(HAS_PREDICTED_DATA_POINTS, true))
        )
        asset = assetStorageService.merge(asset)
        def attribute = asset.getAttribute("test7").get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.get(attributeRef) == attribute
        }

        and: "the delay is 1 hour"
        conditions.eventually {
            delay == 3600
        }

        and: "the predicted datapoints are present"
        conditions.eventually {
            def datapoints = assetPredictedDatapointService.getDatapoints(attributeRef)
            datapoints.get(1).getTimestamp()/1000 == 3600
            datapoints.get(0).getTimestamp()/1000 == 3600 * 2
        }

        when: "fast forward 1 hour"
        advancePseudoClock(1, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "the predicted datapoints are present"
        conditions.eventually {
            def datapoints = assetPredictedDatapointService.getDatapoints(attributeRef)
            datapoints.get(1).getTimestamp()/1000 == 3600
            datapoints.get(0).getTimestamp()/1000 == 3600 * 2
        }
    }
}
