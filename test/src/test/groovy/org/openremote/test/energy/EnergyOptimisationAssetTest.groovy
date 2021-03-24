package org.openremote.test.energy

import com.google.common.collect.Lists
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.energy.EnergyOptimisationService
import org.openremote.manager.energy.EnergyOptimiser
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.ElectricityAsset
import org.openremote.model.asset.impl.ElectricityStorageAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors

import static org.spockframework.util.Assert.that
import static spock.util.matcher.HamcrestMatchers.closeTo

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
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def scheduler = Executors.newScheduledThreadPool(1);
        def services = Lists.newArrayList(defaultServices())
        def spyOptimisationService = Spy(EnergyOptimisationService) {
            scheduleOptimisation(_ as String, _ as EnergyOptimiser) >> {
                // Don't use the scheduler as we will manually trigger the optimisation for testing
                optimisationAssetId, optimiser -> return null
            }
        }

        services.replaceAll { it instanceof EnergyOptimisationService ? spyOptimisationService : it }
        def container = startContainer(defaultConfig(), services)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def optimisationService = container.getService(EnergyOptimisationService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def timerService = container.getService(TimerService.class)

        expect: "an optimisation instance should exist"
        conditions.eventually {
            assert !optimisationService.assetEnergyOptimiserMap.isEmpty()
            assert optimisationService.assetEnergyOptimiserMap.get(managerTestSetup.electricityOptimisationAssetId) != null
        }

        and: "the optimisation start time should be correctly calculated"
        def optimiser = optimisationService.assetEnergyOptimiserMap.get(managerTestSetup.electricityOptimisationAssetId).key
        def now = Instant.ofEpochMilli(timerService.getCurrentTimeMillis())
        def optimisationTime = optimisationService.getOptimisationStartTime(now.toEpochMilli(), (long)optimiser.intervalSize * 60 * 60)
        assert optimisationTime.isBefore(now)
        assert optimisationTime.plus((long)optimiser.intervalSize*60, ChronoUnit.MINUTES).isAfter(now)

        when: "supplier tariff values are set for the next 24hrs"
        def tariffExports = [-5, -2, -8, 2, 2, 5, -2, -2]
        def tariffImports = [3, -5, 10, 1, 3, -5, 7, 8]
        now = Instant.ofEpochMilli(timerService.getCurrentTimeMillis()).truncatedTo(ChronoUnit.HOURS)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_IMPORT.name, tariffImports.get(0)))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_EXPORT.name, tariffExports.get(0)))

        for (int i = 1; i < tariffExports.size(); i++) {
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_IMPORT.name), tariffImports.get(i), now.plus((long)(optimiser.intervalSize * 60)*(i-1), ChronoUnit.MINUTES).toEpochMilli())
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_EXPORT.name), tariffExports.get(i), now.plus((long)(optimiser.intervalSize * 60)*(i-1), ChronoUnit.MINUTES).toEpochMilli())
        }

        and: "consumer and producer prediction values are set for the next 24hrs"
        [-5, -25, 15, 0, 0, -5, -5, -2]
        def consumerPower = [0, 5, 10, 0, 0, 10, 5, 0]
        def producerPower = [-5, -30, -5, 0, 0, -15, -10, -2]
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricityConsumerAssetId, ElectricityAsset.POWER.name, consumerPower.get(0)))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySolarAssetId, ElectricityAsset.POWER.name, producerPower.get(0)))

        for (int i = 1; i < consumerPower.size(); i++) {
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricityConsumerAssetId, ElectricityAsset.POWER.name), tariffImports.get(i), now.plus((long)(optimiser.intervalSize * 60)*(i-1), ChronoUnit.MINUTES).toEpochMilli())
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySolarAssetId, ElectricityAsset.POWER.name), tariffExports.get(i), now.plus((long)(optimiser.intervalSize * 60)*(i-1), ChronoUnit.MINUTES).toEpochMilli())
        }

        and: "the optimisation runs"
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime)

        then: "the setpoints of the storage asset for the next 24hrs should be correctly optimised"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 7d
            def setpoints = assetPredictedDatapointService.getValueDatapoints(
                    new AttributeRef(managerTestSetup.electricityBatteryAssetId, ElectricityAsset.POWER_SETPOINT.name),
                    "minute",
                    ((long)optimiser.intervalSize * 60) + " minute",
                    timerService.currentTimeMillis,
                    Instant.ofEpochMilli(timerService.currentTimeMillis).plus(1, ChronoUnit.DAYS).toEpochMilli()
            )

            assert setpoints.size() == 7
            assert setpoints[0].value == 7d
            assert setpoints[1].value == 7d
            assert that(setpoints[2].value, closeTo(2.33333, 0.0001))
            assert that(setpoints[3].value, closeTo(-4.66666, 0.0001))
            assert setpoints[4].value == 7d
            assert setpoints[5].value == -20d
            assert that(setpoints[6].value, closeTo(-5.66666, 0.0001))
        }
    }
}
