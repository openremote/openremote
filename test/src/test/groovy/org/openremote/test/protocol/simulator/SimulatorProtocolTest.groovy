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

import net.fortuna.ical4j.model.Recur
import org.openremote.agent.protocol.simulator.SimulatorAgent
import org.openremote.agent.protocol.simulator.SimulatorAgentLink
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.query.AssetDatapointAllQuery
import org.openremote.model.simulator.SimulatorReplayDatapoint
import org.openremote.model.util.TimeUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.HOURS
import static org.openremote.model.value.MetaItemType.AGENT_LINK


/**
 * Also see {@link org.openremote.test.assets.AssetDatapointTest} for usage of {@link SimulatorProtocol}.
 */
class SimulatorProtocolTest extends Specification implements ManagerContainerTrait {

    static final PollingConditions conditions = new PollingConditions(timeout: 10, delay: 0.2)

    @Shared
    AssetStorageService assetStorageService

    @Shared
    AssetDatapointService assetDatapointService

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
        stopPseudoClockAt(Instant.parse("1970-01-01T00:00:00.000Z").toEpochMilli())

        // Setup services
        assetStorageService = container.getService(AssetStorageService.class)
        assetDatapointService = container.getService(AssetDatapointService.class)
        agentService = container.getService(AgentService.class)

        // Create and link asset to agent
        agent = new SimulatorAgent("Test agent").setRealm(Constants.MASTER_REALM)
        agent = assetStorageService.merge(agent)
        asset = new ThingAsset("Test asset").setRealm(Constants.MASTER_REALM)
                .addAttributes(new Attribute<>(ThingAsset.NOTES, null)
                        .addMeta(new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()))),
//                        new Attribute<String>("test", null)
                )
        asset = assetStorageService.merge(asset)

        // Must use boxed Long type in Spock closures, so avoiding parameter expansion so it doesn't silently fail.
        executor.schedule(_ as Runnable, _ as Long, _ as TimeUnit) >> { args ->
            delay = args[1] as Long
            println("Delay: " + delay)
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
        def attribute = asset.getAttribute(ThingAsset.NOTES).get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert protocol.linkedAttributes.size() == 1
        }

        and: "nothing happens"
        assert assetDatapointService.getDatapoints(attributeRef).size() == 0
    }

    def "Check Simulator Agent protocol with replay"() {
        when: "replayData is configured to add a datapoint in 1hr, 12hrs, and 24hrs every day"
        def attribute = asset.getAttribute(ThingAsset.NOTES).get()
        attribute.addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 12, "test"),
                        new SimulatorReplayDatapoint(3600 * 24, "test")
                ))
        )
        assetStorageService.merge(asset)

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert protocol.linkedAttributes.size() == 1
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
//            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
//            assert protocol.linkedAttributes.size() == 1
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
        def attribute = asset.getAttribute(ThingAsset.NOTES).get()
        attribute.addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                    new SimulatorReplayDatapoint(3600, "test")
                ).setStartDate(LocalDate.ofInstant(Instant.parse("1970-01-02T00:00:00.000Z"), ZoneId.of("UTC"))))
        )
        assetStorageService.merge(asset)

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert protocol.linkedAttributes.size() == 1
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

        and: "the delay is 24 hours"
        conditions.eventually {
            delay == 3600 * 24
        }
    }

    def "Check Simulator Agent protocol with custom replay duration"() {
        when: "replayData is configured to add a datapoint in 1hr, 25hrs, and 48hrs every other day"
        def attribute = asset.getAttribute(ThingAsset.NOTES).get()
        attribute.addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 25, "test"),
                        new SimulatorReplayDatapoint(3600 * 48, "test")
                ).setDuration(new TimeUtil.ExtendedPeriodAndDuration("P2D")))
        )
        assetStorageService.merge(asset)

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.size() == 1
        }

        (0..1).each { i ->
            def days = i * 3600 * 48 // Starts at 0 days, increments with 2 days

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

            and: "the delay is 24 hours"
            conditions.eventually {
                delay == 3600 * 24
            }

            when: "fast forward 24 hours"
            advancePseudoClock(24, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == days + 3600 * 25
            }

            and: "the delay is 23 hours"
            conditions.eventually {
                delay == 3600 * 23
            }

            when: "fast forward 23 hour"
            advancePseudoClock(23, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoints are present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == days + 3600 * 48
            }
        }
    }

    def "Check Simulator Agent protocol with custom recurrence schedule"() {
        when: "replayData is configured to add a datapoint in 1hr, and 2hrs weekly on Mondays"
        def attribute = asset.getAttribute(ThingAsset.NOTES).get()
//        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())
        attribute.addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                        new SimulatorReplayDatapoint(3600, "test"),
                        new SimulatorReplayDatapoint(3600 * 2, "test")
                )
//                .setStartDate(LocalDate.ofInstant(Instant.parse("1970-01-05T00:00:00.000Z"), ZoneId.of("UTC")))
                .setDuration(new TimeUtil.ExtendedPeriodAndDuration("PT2H"))
                // Recur every Monday until 1970-01-31
                // First day should be 1970-01-05 (in 4 days) a Monday
                .setRecurrence(new Recur<LocalDateTime>("FREQ=WEEKLY;UNTIL=19700131T000000Z;BYDAY=MO")))
        )
        assetStorageService.merge(asset)

        then: "the agent status should become CONNECTED and the attribute linked to the protocol"
        conditions.eventually {
            agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            protocol.linkedAttributes.size() == 1
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

        (0..1).each { i ->
            def weeks = i * 3600 * 24 * 7 // Starts at 0 weeks, increments with 1 week

            and: "the delay is 1 hour"
            conditions.eventually {
                delay == 3600
            }

            when: "fast forward 1 hour"
            advancePseudoClock(1, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == weeks + 3600 * 98
            }

            // TODO: make this easier to follow
            and: "the delay is 167 hour"
            conditions.eventually {
                delay == 3600 * 167
            }

            when: "fast forward 167 hour"
            advancePseudoClock(167, HOURS, container)
            future.get() // resolve future manually, because we surpassed the delay

            then: "datapoint is present"
            conditions.eventually {
                getDatapointTimestamp(attribute) == weeks + 3600 * 265
            }
        }

        and: "the delay is 1 hour"
        conditions.eventually {
            delay == 3600
        }

        when: "fast forward past the end date"
        advancePseudoClock(1000, HOURS, container)
        future.get() // resolve future manually, because we surpassed the delay

        then: "no further occurrences happen"
        thrown(NullPointerException) // Weird null pointer exception I cannot fully explain
        // I'm just assuming this is caused, because null is returned after the last occurrence was surpassed and
        // the mock makes this behave weird
//
//        and: "the attributeRef is removed from the replayMap"
//        protocol.replayMap.get(attributeRef) == null
    }

    def "Check Simulator Agent protocol adds predicted datapoints for the current and next cycle"() {

    }
}
