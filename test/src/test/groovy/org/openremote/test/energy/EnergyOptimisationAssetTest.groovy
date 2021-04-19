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
import org.openremote.model.asset.impl.ElectricityConsumerAsset
import org.openremote.model.asset.impl.ElectricityProducerSolarAsset
import org.openremote.model.asset.impl.ElectricityStorageAsset
import org.openremote.model.asset.impl.ElectricitySupplierAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

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
class EnergyOptimisationAssetTest extends Specification implements ManagerContainerTrait {
    def "Test storage asset with consumer and producer"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
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

        when: "the pseudo clock is stopped and the system time is set to midnight of next day"
        stopPseudoClock()
        def now = Instant.ofEpochMilli(timerService.getCurrentTimeMillis())
        now = now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
        advancePseudoClock(now.toEpochMilli()-timerService.getCurrentTimeMillis(), TimeUnit.MILLISECONDS, container)

        then: "the optimisation start time should be correctly calculated"
        def optimiser = optimisationService.assetEnergyOptimiserMap.get(managerTestSetup.electricityOptimisationAssetId).key
        def optimisationTime = optimisationService.getOptimisationStartTime(now.toEpochMilli(), (long)optimiser.intervalSize * 60 * 60)
        assert optimisationTime.isBefore(now)
        assert optimisationTime.plus((long)optimiser.intervalSize*60, ChronoUnit.MINUTES).equals(now)

        when: "supplier tariff values are set for the next 24hrs"
        def tariffExports = [-5, -2, -8, 2, 2, 5, -2, -2]
        def tariffImports = [3, -5, 10, 1, 3, -5, 7, 8]
        def optimisationDateTime = LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault())
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_IMPORT.name, tariffImports.get(0)))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_EXPORT.name, tariffExports.get(0)))

        for (int i = 1; i < tariffExports.size(); i++) {
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_IMPORT.name), tariffImports.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_EXPORT.name), tariffExports.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
        }

        and: "consumer and producer prediction values are set for the next 24hrs"
        def consumerPower = [0, 5, 20, 0, 0, 10, 5, 0]
        def producerPower = [-5, -30, -5, 0, 0, -15, -10, -2]
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricityConsumerAssetId, ElectricityAsset.POWER.name, consumerPower.get(0)))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySolarAssetId, ElectricityAsset.POWER.name, producerPower.get(0)))

        for (int i = 1; i < consumerPower.size(); i++) {
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricityConsumerAssetId, ElectricityAsset.POWER.name), consumerPower.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySolarAssetId, ElectricityAsset.POWER.name), producerPower.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
        }

        then: "the current values of each attribute should have reached the DB"
        conditions.eventually {
            assert (assetStorageService.find(managerTestSetup.electricitySupplierAssetId) as ElectricitySupplierAsset).getTariffImport().orElse(0d) == tariffImports.get(0)
            assert (assetStorageService.find(managerTestSetup.electricitySupplierAssetId) as ElectricitySupplierAsset).getTariffExport().orElse(0d) == tariffExports.get(0)
            assert (assetStorageService.find(managerTestSetup.electricityConsumerAssetId) as ElectricityConsumerAsset).getPower().orElse(-1d) == consumerPower.get(0)
            assert (assetStorageService.find(managerTestSetup.electricitySolarAssetId) as ElectricityProducerSolarAsset).getPower().orElse(-1d) == producerPower.get(0)
        }

        when: "the optimisation runs"
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime)

        then: "the setpoints of the storage asset for the next 24hrs should be correctly optimised"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 0d
            def setpoints = assetPredictedDatapointService.getValueDatapoints(
                    new AttributeRef(managerTestSetup.electricityBatteryAssetId, ElectricityAsset.POWER_SETPOINT.name),
                    "minute",
                    (long)(optimiser.intervalSize * 60) + " minute",
                    LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                    LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus(1, ChronoUnit.DAYS).minus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES)
            )

            assert setpoints.size() == 7
            assert setpoints[0].value == 0d
            assert setpoints[1].value == -20d
            assert setpoints[2].value == 7d //that(setpoints[2].value, closeTo(2.33333, 0.0001))
            assert setpoints[3].value == 0d
            assert setpoints[4].value == 7d
            assert setpoints[5].value == -14d
            assert setpoints[6].value == 0d
        }

        when: "storage asset import and export tariffs are added to make storage un-viable and optimisation is run"
        def batteryAsset = ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId))
        batteryAsset.setTariffExport(10)
        batteryAsset.setTariffImport(10)
        batteryAsset = assetStorageService.merge(batteryAsset)
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime)

        then: "the battery should not be used in any interval"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 0d
            def setpoints = assetPredictedDatapointService.getValueDatapoints(
                    new AttributeRef(managerTestSetup.electricityBatteryAssetId, ElectricityAsset.POWER_SETPOINT.name),
                    "minute",
                    (long)(optimiser.intervalSize * 60) + " minute",
                    LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                    LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus(1, ChronoUnit.DAYS).minus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES)
            )

            assert setpoints.size() == 7
            assert setpoints[0].value == 0d
            assert setpoints[1].value == 0d
            assert setpoints[2].value == 0d
            assert setpoints[3].value == 0d
            assert setpoints[4].value == 0d
            assert setpoints[5].value == 0d
            assert setpoints[6].value == 0d
        }

        when: "storage asset import and export tariffs are modified to make storage viable in limited intervals and optimisation is run"
        batteryAsset = ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId))
        batteryAsset.setTariffExport(5)
        batteryAsset.setTariffImport(5)
        batteryAsset = assetStorageService.merge(batteryAsset)
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime)

        then: "the battery should only be used in the correct intervals"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 0d
            def setpoints = assetPredictedDatapointService.getValueDatapoints(
                    new AttributeRef(managerTestSetup.electricityBatteryAssetId, ElectricityAsset.POWER_SETPOINT.name),
                    "minute",
                    (long)(optimiser.intervalSize * 60) + " minute",
                    LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                    LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus(1, ChronoUnit.DAYS).minus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES)
            )

            assert setpoints.size() == 7
            assert setpoints[0].value == 0d
            assert setpoints[1].value == -20d
            assert setpoints[2].value == 0d
            assert setpoints[3].value == 0d
            assert setpoints[4].value == 0d
            assert setpoints[5].value == 0d
            assert setpoints[6].value == 0d
        }
    }
}
