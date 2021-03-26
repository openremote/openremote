package org.openremote.test.energy

import com.google.common.collect.Lists
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.energy.EnergyOptimisationService
import org.openremote.manager.energy.EnergyOptimiser
import org.openremote.manager.mqtt.MqttBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.model.attribute.AttributeEvent
import org.openremote.test.ContainerTrait
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.datapoint.AssetDatapointService.DATA_POINTS_MAX_AGE_DAYS_DEFAULT

/*
 * Copyright 2021, OpenRemote Inc.
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

@Ignore
class EnergyOptimisationAssetTest extends Specification implements ManagerContainerTrait {
    def "Test storage asset with consumer and producer"() {

        given: "the container environment is started"
        def spyOptimisationService = Spy(EnergyOptimisationService) {
            scheduleOptimisation(_ as String, _ as EnergyOptimiser) >> {
                optimisationAssetId, optimiser ->
            }

            def conditions = new PollingConditions(timeout: 10, delay: 0.2)
            def services = Lists.newArrayList(defaultServices())
            services.replaceAll { it instanceof EnergyOptimisationService ? spyOptimisationService : it }
            def container = startContainer(defaultConfig(), services)
            def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
            def agentService = container.getService(AgentService.class)
            def assetStorageService = container.getService(AssetStorageService.class)
            def assetProcessingService = container.getService(AssetProcessingService.class)
            def assetDatapointService = container.getService(AssetDatapointService.class)

            when: "a storage asset is added"
        }
    }
}
