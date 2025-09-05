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
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.simulator.SimulatorReplayDatapoint
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.DAYS
import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.MINUTES
import static org.openremote.model.value.MetaItemType.AGENT_LINK


/**
 * Also see {@link org.openremote.test.assets.AssetDatapointTest} for usage of {@link SimulatorProtocol}.
 */
class SimulatorProtocolTest extends Specification implements ManagerContainerTrait {

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
    }

    def "Check Simulator Agent protocol scheduling"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 60, delay: 0.2)

        and: "the container starts"
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "setup agent"
        def agent = new SimulatorAgent("Test agent")
                .setRealm(Constants.MASTER_REALM)

        and: "setup linked asset"
        def asset = new ThingAsset("Test asset")
                .setRealm(Constants.MASTER_REALM)
                .addAttributes(new Attribute<>(ThingAsset.NOTES, null)
                        .addMeta(new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId())))
                )

        and: "the agent and asset are added to the asset service"
        agent = assetStorageService.merge(agent)
        asset = assetStorageService.merge(asset)

        when: "nothing is configured"

        then: "the protocol should connect and the agent status should become CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId(), Agent.class)
            assert agent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        and : "nothing happens"
        def attribute = agent.getAttribute(ThingAsset.NOTES).get()
        assert assetDatapointService.getDatapoints(new AttributeRef(asset.getId(), attribute.getName())).size() == 0

        // ------------------------------------
        // Test replay data of the simulator
        // ------------------------------------

        when: "replayData is configured to replay in 1hr"
        attribute.addOrReplaceMeta(
            new MetaItem<>(AGENT_LINK, new SimulatorAgentLink(agent.getId()).setReplayData(
                new SimulatorReplayDatapoint(1000, "test")
            ))
        )
        // expect from 1970-01-01T00:00:00.000Z
        then: "no datapoint is present up until 1hr"
        // expect from 1970-01-01T00:59:99.999Z
        and : "1 datapoint is present after 1hr"
        // expect at   1970-01-01T23:59:99.999Z

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
