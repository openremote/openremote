package org.openremote.test.energy

import com.google.common.collect.Lists
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.energy.EnergyOptimisationService
import org.openremote.manager.energy.EnergyOptimiser
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.*
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.query.AssetDatapointIntervalQuery
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import org.openremote.setup.integration.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

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
class EnergyOptimisationAssetTest extends Specification implements ManagerContainerTrait {
    def "Test storage asset with consumer and producer"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def services = Lists.newArrayList(defaultServices())
        def spyOptimisationService = Spy(EnergyOptimisationService) {
            scheduleOptimisation(_ as String, _ as EnergyOptimiser, _ as Duration, _ as Long) >> {
                // Don't use the scheduler as we will manually trigger the optimisation for testing
                optimisationAssetId, optimiser, startDuration, periodSeconds ->
                    return null
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
            assert !optimisationService.assetOptimisationInstanceMap.isEmpty()
            assert optimisationService.assetOptimisationInstanceMap.get(managerTestSetup.electricityOptimisationAssetId) != null
        }

        when: "the pseudo clock is stopped and the system time is set to midnight of next day"
        stopPseudoClock()
        def now = Instant.ofEpochMilli(timerService.getCurrentTimeMillis())
        now = now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
        advancePseudoClock(now.toEpochMilli()-timerService.getCurrentTimeMillis(), TimeUnit.MILLISECONDS, container)

        then: "the optimisation start time should be correctly calculated"
        def optimiser = optimisationService.assetOptimisationInstanceMap.get(managerTestSetup.electricityOptimisationAssetId).energyOptimiser
        def optimisationTime = optimisationService.getOptimisationStartTime(now.toEpochMilli(), (long)optimiser.intervalSize * 60 * 60)
        def optimisationDateTime = LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault())
        assert optimisationTime.isBefore(now)
        assert optimisationTime.plus((long)optimiser.intervalSize*60, ChronoUnit.MINUTES).equals(now)

        when: "supplier tariff values are set for the next 24hrs"
        def tariffExports = [-5, -2, -8, 2, 2, 5, -2, -2]
        def tariffImports = [3, -5, 10, 1, 3, -5, 7, 8]
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
            def setpoints = assetPredictedDatapointService.queryDatapoints(
                    managerTestSetup.electricityBatteryAssetId,
                    ElectricityAsset.POWER_SETPOINT.name,
                    new AssetDatapointIntervalQuery(
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus(1, ChronoUnit.DAYS).minus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            (optimiser.intervalSize * 60) + " minutes",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
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
            def setpoints = assetPredictedDatapointService.queryDatapoints(
                    managerTestSetup.electricityBatteryAssetId,
                    ElectricityAsset.POWER_SETPOINT.name,
                    new AssetDatapointIntervalQuery(
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus(1, ChronoUnit.DAYS).minus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            (optimiser.intervalSize * 60) + " minutes",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
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
            def setpoints = assetPredictedDatapointService.queryDatapoints(
                    managerTestSetup.electricityBatteryAssetId,
                    ElectricityAsset.POWER_SETPOINT.name,
                    new AssetDatapointIntervalQuery(
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus(1, ChronoUnit.DAYS).minus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            (optimiser.intervalSize * 60) + " minutes",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
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

    def "Test savings calculations"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def services = Lists.newArrayList(defaultServices())
        def spyOptimisationService = Spy(EnergyOptimisationService) {
            scheduleOptimisation(_ as String, _ as EnergyOptimiser, _ as Duration, _ as Long) >> {
                    // Don't use the scheduler as we will manually trigger the optimisation for testing
                optimisationAssetId, optimiser, startDuration, periodSeconds ->
                    return null
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
            assert !optimisationService.assetOptimisationInstanceMap.isEmpty()
            assert optimisationService.assetOptimisationInstanceMap.get(managerTestSetup.electricityOptimisationAssetId) != null
        }

        when: "the battery energy level is set to lower limit for ease of testing"
        def batteryAsset = assetStorageService.find(managerTestSetup.electricityBatteryAssetId) as ElectricityBatteryAsset
        batteryAsset.setEnergyLevel(40)
        batteryAsset.setPowerImportMax(10)
        batteryAsset.setPowerExportMax(10)
        batteryAsset = assetStorageService.merge(batteryAsset)

        and: "the pseudo clock is stopped and the system time is set to midnight of two days time"
        stopPseudoClock()
        def now = Instant.ofEpochMilli(timerService.getCurrentTimeMillis())
        now = now.truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS)
        advancePseudoClock(now.toEpochMilli()-timerService.getCurrentTimeMillis(), TimeUnit.MILLISECONDS, container)

        then: "the optimisation start time should be correctly calculated"
        def optimiser = optimisationService.assetOptimisationInstanceMap.get(managerTestSetup.electricityOptimisationAssetId).energyOptimiser
        def optimisationTime = optimisationService.getOptimisationStartTime(now.toEpochMilli(), (long)optimiser.intervalSize * 60 * 60)
        def optimisationDateTime = LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault())
        assert optimisationTime.isBefore(now)
        assert optimisationTime.plus((long)optimiser.intervalSize*60, ChronoUnit.MINUTES).equals(now)

        and: "consumer and producer prediction values are set for the next 24hrs"
        def consumerPower = [0, 0, 0, 0, 10, 10, 10, 10]
        def producerPower = [-50, -50, -50, -50, 0, 0, 0, 0]
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricityConsumerAssetId, ElectricityAsset.POWER.name, consumerPower.get(0)))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySolarAssetId, ElectricityAsset.POWER.name, producerPower.get(0)))

        for (int i = 1; i < consumerPower.size(); i++) {
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricityConsumerAssetId, ElectricityAsset.POWER.name), consumerPower.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySolarAssetId, ElectricityAsset.POWER.name), producerPower.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
        }

        and: "supplier tariff values are set for the next 24hrs"
        def tariffExports = [-0.05, -0.05, -0.05, -0.05, -0.05, -0.05, -0.05, -0.05]
        def tariffImports = [0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08]
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_IMPORT.name, tariffImports.get(0)))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_EXPORT.name, tariffExports.get(0)))

        for (int i = 1; i < tariffExports.size(); i++) {
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_IMPORT.name), tariffImports.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
            assetPredictedDatapointService.updateValue(new AttributeRef(managerTestSetup.electricitySupplierAssetId, ElectricityAsset.TARIFF_EXPORT.name), tariffExports.get(i), optimisationDateTime.plus((long)(optimiser.intervalSize * 60)*i, ChronoUnit.MINUTES))
        }

        then: "the current values of each attribute should have reached the DB"
        conditions.eventually {
            assert (assetStorageService.find(managerTestSetup.electricityConsumerAssetId) as ElectricityConsumerAsset).getPower().orElse(-1d) == consumerPower.get(0)
            assert (assetStorageService.find(managerTestSetup.electricitySolarAssetId) as ElectricityProducerSolarAsset).getPower().orElse(-1d) == producerPower.get(0)
        }

        when: "the optimisation asset is reset which will cause optimisation to run"
        def optimisationAsset = assetStorageService.find(managerTestSetup.electricityOptimisationAssetId) as EnergyOptimisationAsset
        optimisationAsset.setOptimisationDisabled(true)
        optimisationAsset.setFinancialSaving(0d)
        optimisationAsset = ValueUtil.clone(assetStorageService.merge(optimisationAsset))
        optimisationAsset.setOptimisationDisabled(false)
        optimisationAsset = assetStorageService.merge(optimisationAsset)

        then: "the battery setpoint should be set to max to store produced energy for future deficit"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 10d

            def setpoints = assetPredictedDatapointService.queryDatapoints(
                    managerTestSetup.electricityBatteryAssetId,
                    ElectricityAsset.POWER_SETPOINT.name,
                    new AssetDatapointIntervalQuery(
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault()).plus(1, ChronoUnit.DAYS).minus((long)(optimiser.intervalSize * 60), ChronoUnit.MINUTES),
                            (optimiser.intervalSize * 60) + " minutes",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
            )

            assert setpoints.size() == 7
            assert setpoints[0].value == 10d
            assert setpoints[1].value == 10d
            assert setpoints[2].value == 10d
            assert setpoints[3].value == -10d
            assert setpoints[4].value == -10d
            assert setpoints[5].value == -10d
            assert setpoints[6].value == -10d
        }

        and: "the optimisation saving should be inverse of earning from lost production export -(10x3x0.05)"
        conditions.eventually {
            assert (assetStorageService.find(managerTestSetup.electricityOptimisationAssetId) as EnergyOptimisationAsset).getFinancialSaving().orElse(0d) == -1.5
        }

        when: "the battery energy level changes and time advances"
        batteryAsset.setEnergyLevel(70d)
        batteryAsset = assetStorageService.merge(batteryAsset)
        advancePseudoClock(1, TimeUnit.SECONDS, container) // This prevents attribute timestamp issues

        and: "another optimisation run occurs"
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime.plus((long)optimiser.intervalSize*60, ChronoUnit.MINUTES))

        then: "the optimisation saving should have decreased by the same amount (i.e. cost)"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 10d
            assert (assetStorageService.find(managerTestSetup.electricityOptimisationAssetId) as EnergyOptimisationAsset).getFinancialSaving().orElse(0d) == -3.0
        }

        when: "the battery energy level changes and time advances"
        batteryAsset.setEnergyLevel(100d)
        batteryAsset = assetStorageService.merge(batteryAsset)
        advancePseudoClock(1, TimeUnit.SECONDS, container) // This prevents attribute timestamp issues

        and: "another optimisation run occurs"
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime.plus((long)optimiser.intervalSize*60*2, ChronoUnit.MINUTES))

        then: "the optimisation saving should have decreased by the same amount (i.e. cost)"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 10d
            assert (assetStorageService.find(managerTestSetup.electricityOptimisationAssetId) as EnergyOptimisationAsset).getFinancialSaving().orElse(0d) == -4.5
        }

        when: "the battery energy level changes and time advances"
        batteryAsset.setEnergyLevel(130d)
        batteryAsset = assetStorageService.merge(batteryAsset)
        advancePseudoClock(1, TimeUnit.SECONDS, container) // This prevents attribute timestamp issues

        and: "another optimisation run occurs"
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime.plus((long)optimiser.intervalSize*60*3, ChronoUnit.MINUTES))

        then: "the optimisation saving should have decreased by the same amount (i.e. cost)"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == 10d
            assert (assetStorageService.find(managerTestSetup.electricityOptimisationAssetId) as EnergyOptimisationAsset).getFinancialSaving().orElse(0d) == -6.0
        }

        when: "the consumer and producer power attributes are updated (as they have changed according to the predicted data)"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricityConsumerAssetId, ElectricityAsset.POWER.name, consumerPower.get(4)))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.electricitySolarAssetId, ElectricityAsset.POWER.name, producerPower.get(4)))

        then: "the values should have reached the DB"
        conditions.eventually {
            assert (assetStorageService.find(managerTestSetup.electricityConsumerAssetId) as ElectricityConsumerAsset).getPower().orElse(-1d) == consumerPower.get(4)
            assert (assetStorageService.find(managerTestSetup.electricitySolarAssetId) as ElectricityProducerSolarAsset).getPower().orElse(-1d) == producerPower.get(4)
        }

        when: "another optimisation run occurs (this should now be exporting from storage)"
        batteryAsset.setEnergyLevel(160d)
        batteryAsset = assetStorageService.merge(batteryAsset)
        advancePseudoClock(1, TimeUnit.SECONDS, container) // This prevents attribute timestamp issues

        and: "another optimisation run occurs"
        optimisationService.runOptimisation(managerTestSetup.electricityOptimisationAssetId, optimisationTime.plus((long)optimiser.intervalSize*60*4, ChronoUnit.MINUTES))

        then: "the optimisation saving should have increased by the cost to import 30kWh (as battery will now be exporting)"
        conditions.eventually {
            assert ((ElectricityStorageAsset)assetStorageService.find(managerTestSetup.electricityBatteryAssetId)).getPowerSetpoint().orElse(0d) == -10d
            assert (assetStorageService.find(managerTestSetup.electricityOptimisationAssetId) as EnergyOptimisationAsset).getFinancialSaving().orElse(0d), closeTo(-6.0+2.4, 0.1)
        }
    }
}
