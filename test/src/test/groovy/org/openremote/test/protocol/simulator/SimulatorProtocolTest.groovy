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
import org.openremote.model.Constants
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.query.AssetDatapointAllQuery
import org.openremote.model.simulator.SimulatorReplayDatapoint
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

import static java.util.concurrent.TimeUnit.DAYS
import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.MINUTES
import static org.openremote.model.value.MetaItemType.AGENT_LINK


/**
 * Also see {@link org.openremote.test.assets.AssetDatapointTest} for usage of {@link SimulatorProtocol}.
 */
class SimulatorProtocolTest extends Specification implements ManagerContainerTrait {

    static final PollingConditions conditions = new PollingConditions(timeout: 60, delay: 0.2)

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
        given: "xxx"
        startContainer(defaultConfig(), defaultServices())
        stopPseudoClockAt(Instant.parse("1970-01-01T00:00:00.000Z").toEpochMilli())

        // Setup services
        assetStorageService = container.getService(AssetStorageService.class)
        assetDatapointService = container.getService(AssetDatapointService.class)
        agentService = container.getService(AgentService.class)

        // Must use boxed Long type in Spock closures, so avoiding parameter expansion so it doesn't silently fail.
        executor.schedule(_ as Runnable, _ as Long, _ as TimeUnit) >> { args ->
            delay = args[1] as Long
            // Mock get method on ScheduledFuture to resolve the future manually
            future.get() >> {
                args[0].run()
                return true
            }
            return future
        }
        agent = new SimulatorAgent("Test agent").setRealm(Constants.MASTER_REALM)
        agent = assetStorageService.merge(agent)
    }

    private getDataPoints = { def now = Instant.ofEpochMilli(getClockTimeOf(container)).atZone(ZoneId.of("UTC")).toLocalDateTime();
        new AssetDatapointAllQuery(
            now.minus(1, ChronoUnit.HOURS),
            now.plus(1, ChronoUnit.HOURS),
    ) }

    private waitForAgentConnection() {
        agent = (SimulatorAgent) assetStorageService.find(agent.getId(), Agent.class)
        assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        assert agentService.protocolInstanceMap.get(agent.getId()) != null
        protocol = (SimulatorProtocol) agentService.protocolInstanceMap.get(agent.getId())
        protocol.scheduledExecutorService = executor
        return protocol.linkedAttributes.size() == 1
    }

    def "Check Simulator Agent protocol without replay"() {
        given: ""
        asset = new ThingAsset("Test asset").setRealm(Constants.MASTER_REALM)
                .addAttributes(new Attribute<>(ThingAsset.NOTES, null)
                    .addMeta(new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId())))
                )
        asset = assetStorageService.merge(asset)

        when: "nothing is configured"
        def attribute = asset.getAttribute(ThingAsset.NOTES).get()
        def attributeRef = new AttributeRef(asset.getId(), attribute.getName())

        then: "the protocol should connect and the agent status should become CONNECTED"
        conditions.eventually {
            waitForAgentConnection()
        }

        and: "nothing happens"
        assert assetDatapointService.getDatapoints(attributeRef).size() == 0
    }

    def "Check Simulator Agent protocol with replay"() {
        given: ""
        asset = new ThingAsset("Test asset").setRealm(Constants.MASTER_REALM)
                .addAttributes(new Attribute<>(ThingAsset.NOTES, null)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                            new SimulatorReplayDatapoint(3600, "test")
                        ))
                    )
                )
        asset = assetStorageService.merge(asset)

        when: "replayData is configured to replay in 1hr"
        def attribute = asset.getAttribute(ThingAsset.NOTES).get()

        then: "the protocol should connect and the agent status should become CONNECTED"
        conditions.eventually {
            def agent = (SimulatorAgent) assetStorageService.find(agent.getId(), Agent.class)
            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert agentService.protocolInstanceMap.get(agent.getId()) != null
            def protocol = (SimulatorProtocol) agentService.protocolInstanceMap.get(agent.getId())
            assert protocol.linkedAttributes.size() == 1
        }

        when: ""
        agent = (SimulatorAgent) assetStorageService.find(agent.getId(), Agent.class)
        protocol = (SimulatorProtocol) agentService.protocolInstanceMap.get(agent.getId())
        protocol.scheduledExecutorService = executor

        then: "no datapoint is present up until 1hr"
        conditions.eventually {
            assert delay == 3600
            assert assetDatapointService.queryDatapoints(
                asset.getId(),
                attribute.getName(),
                getDataPoints.call()
            ).size() == 0
        }

        and: "1 datapoint is present after 1hr"
        advancePseudoClock(1, HOURS, container)
        future.get() // resolve future manually, because delay == 3600
        conditions.eventually {
            assert delay == 86400
            assert assetDatapointService.queryDatapoints(
                asset.getId(),
                attribute.getName(),
                getDataPoints.call()
            ).size() == 1
        }
    }

    def "Check Simulator Agent protocol with replay startDate"() {
    }

    def "Check Simulator Agent protocol with custom replay duration"() {
    }

    def "Check Simulator Agent protocol with custom recurrence schedule"() {
    }

    def "Check Simulator Agent protocol scheduling"() {


        // ------------------------------------
        // Test start date of the simulator
        // ------------------------------------

        when: "startDate is configured for the next day"
        then: "no datapoint is present up until 1day and 1hr"
        advancePseudoClock(1, DAYS, container)
        advancePseudoClock(59, MINUTES, container)
        // expect at   1970-01-01T23:59:00.000Z
        and : "1 datapoint is added after 1hr"
        advancePseudoClock(1, MINUTES, container)
        // expect at   1970-01-02T00:00:00.000Z

        // ------------------------------------
        // Test duration of the simulator
        // ------------------------------------

        when: "duration is not set"
        then: "1 datapoint is added after 1hr"
        // expect at   1970-01-03T00:00:00.000Z
        when: "duration is set to 2 days"
        then: "1 datapoint should be added"
        // expect at   1970-01-04T00:00:00.000Z
        and : "1 datapoint should be added"
        // expect at   1970-01-05T00:00:00.000Z
        and : "no datapoint should be added"
        // expect at   1970-01-06T00:00:00.000Z

//        defines the length of the replay loop (and of the filled-in predicted data points if applicable), if replayData contains data points after this duration, those values are ignored and never used

        // ------------------------------------
        // Test recurrence of the simulator
        // ------------------------------------

        when: ""
        then: ""

        when: ""
        then: ""

//        recurrence:
//        recurrence rule, following RFC 5545 RRULE format
//        if not provided, repeats indefinitely daily
    }
}
