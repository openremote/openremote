package org.openremote.test.energy

import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.energy.EnergyOptimiser
import org.openremote.model.asset.impl.ElectricitySupplierAsset
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.util.Pair
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.function.Function
import java.util.stream.IntStream

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

class EnergyOptimisationTest extends Specification {

    def gridId = UniqueIdentifierGenerator.generateId("grid")
    def intervalCount = 8
    def intervalSize = 24 / intervalCount
    def currentInterval = 0
    def tariffExports = [-5, -2, -8, 2, 2, 5, -2, -2]
    def tariffImports = [3, -5, 10, 1, 3, -5, 7, 8]
    def powerNets = [-5, -25, 15, 0, 0, -5, -5, -2]

    def "Check basic import optimiser"() {

        given: "an energy optimisation instance"
        def optimisation = new EnergyOptimiser(intervalSize, 1d)

        and: "some import parameters"
        def powerImportMax = 7d

        when: "we request the optimised import power values for the input parameters"
        def powerNetLimits = new double[intervalCount]
        Arrays.fill(powerNetLimits, 30d)
        def costCalculator = optimisation.getImportOptimiser(powerNets as double[], powerNetLimits, tariffImports as double[], tariffExports as double[], 0d)
        List<Pair<Integer, double[]>> optimisedPower = IntStream.range(0, intervalCount).mapToObj{new Pair<>(it, costCalculator.apply(it, [0d, powerImportMax] as double[]))}.collect()
        Collections.sort(optimisedPower, Comparator.comparingDouble({Pair<Integer, double[]> optimisedInterval -> optimisedInterval.value[0]}))

        then: "the optimised import values should be correct"
        optimisedPower.get(0).key == 5
        optimisedPower.get(0).value[0] == -5d
        optimisedPower.get(0).value[1] == 0d
        optimisedPower.get(0).value[2] == 7d
        optimisedPower.get(1).key == 3
        optimisedPower.get(1).value[0] == 1d
        optimisedPower.get(1).value[1] == 0d
        optimisedPower.get(1).value[2] == 7d
        optimisedPower.get(2).key == 1
        optimisedPower.get(2).value[0] == 2d
        optimisedPower.get(2).value[1] == 0d
        optimisedPower.get(2).value[2] == 7d
        optimisedPower.get(3).key == 6
        optimisedPower.get(3).value[0] == 2d
        optimisedPower.get(3).value[1] == 0d
        optimisedPower.get(3).value[2] == 5d
        optimisedPower.get(4).key == 7
        optimisedPower.get(4).value[0] == 2d
        optimisedPower.get(4).value[1] == 0d
        optimisedPower.get(4).value[2] == 2d
        optimisedPower.get(5).key == 4
        optimisedPower.get(5).value[0] == 3d
        optimisedPower.get(5).value[1] == 0d
        optimisedPower.get(5).value[2] == 7d
        optimisedPower.get(6).key == 0
        optimisedPower.get(6).value[0] closeTo(4.428, 0.001)
        optimisedPower.get(6).value[1] == 7d
        optimisedPower.get(6).value[2] == 7d
        optimisedPower.get(7).key == 2
        optimisedPower.get(7).value[0] == 10d
        optimisedPower.get(7).value[1] == 0d
        optimisedPower.get(7).value[2] == 7d

        when: "we request the optimised consumption cost values for the EV asset with a minimum power requirement of 6"
        optimisedPower = IntStream.range(0, intervalCount).mapToObj{new Pair<>(it, costCalculator.apply(it, [6d, powerImportMax] as double[]))}.collect()
        Collections.sort(optimisedPower, Comparator.comparingDouble({Pair<Integer, double[]> optimisedInterval -> optimisedInterval.value[0]}))

        then: "the optimised import values should be correct"
        optimisedPower.get(0).key == 5
        optimisedPower.get(0).value[0] == -5d
        optimisedPower.get(0).value[1] == 6d
        optimisedPower.get(0).value[2] == 7d
        optimisedPower.get(1).key == 3
        optimisedPower.get(1).value[0] == 1d
        optimisedPower.get(1).value[1] == 6d
        optimisedPower.get(1).value[2] == 7d
        optimisedPower.get(2).key == 1
        optimisedPower.get(2).value[0] == 2d
        optimisedPower.get(2).value[1] == 6d
        optimisedPower.get(2).value[2] == 7d
        optimisedPower.get(3).key == 6
        optimisedPower.get(3).value[0] closeTo(2.8333, 0.001)
        optimisedPower.get(3).value[1] == 6d
        optimisedPower.get(3).value[2] == 6d
        optimisedPower.get(4).key == 4
        optimisedPower.get(4).value[0] == 3d
        optimisedPower.get(4).value[1] == 6d
        optimisedPower.get(4).value[2] == 7d
        optimisedPower.get(5).key == 0
        optimisedPower.get(5).value[0] closeTo(4.428, 0.001)
        optimisedPower.get(5).value[1] == 7d
        optimisedPower.get(5).value[2] == 7d
        optimisedPower.get(6).key == 7
        optimisedPower.get(6).value[0] == 6d
        optimisedPower.get(6).value[1] == 6d
        optimisedPower.get(6).value[2] == 6d
        optimisedPower.get(7).key == 2
        optimisedPower.get(7).value[0] == 10d
        optimisedPower.get(7).value[1] == 6d
        optimisedPower.get(7).value[2] == 7d
    }

    def "Check basic export optimiser"() {

        given: "an energy optimisation instance"
        currentInterval = 0
        def optimisation = new EnergyOptimiser(intervalSize, 1d)

        and: "some storage export parameters"
        def powerExportMax = -20d

        when: "we request the optimised export power values for the input parameters"
        def powerNetLimits = new double[intervalCount]
        Arrays.fill(powerNetLimits, -30d)
        def costCalculator = optimisation.getExportOptimiser(powerNets as double[], powerNetLimits, tariffImports as double[], tariffExports as double[], 0d)
        List<Pair<Integer, double[]>> optimisedPower = IntStream.range(0, intervalCount).mapToObj{new Pair<>(it, costCalculator.apply(it, powerExportMax))}.collect()
        Collections.sort(optimisedPower, Comparator.comparingDouble({Pair<Integer, double[]> optimisedInterval -> optimisedInterval.value[0]}))

        then: "the optimised export values should be correct"
        optimisedPower.get(0).key == 2
        optimisedPower.get(0).value[0] == -9.5d
        optimisedPower.get(0).value[1] == -20d
        optimisedPower.get(0).value[2] == -20d
        optimisedPower.get(1).key == 0
        optimisedPower.get(1).value[0] == -5d
        optimisedPower.get(1).value[1] == -20d
        optimisedPower.get(1).value[2] == 0d
        optimisedPower.get(2).key == 1
        optimisedPower.get(2).value[0] == -2d
        optimisedPower.get(2).value[1] == -5d
        optimisedPower.get(2).value[2] == 0d
        optimisedPower.get(3).key == 6
        optimisedPower.get(3).value[0] == -2d
        optimisedPower.get(3).value[1] == -20d
        optimisedPower.get(3).value[2] == 0d
        optimisedPower.get(4).key == 7
        optimisedPower.get(4).value[0] == -2d
        optimisedPower.get(4).value[1] == -20d
        optimisedPower.get(4).value[2] == 0d
        optimisedPower.get(5).key == 3
        optimisedPower.get(5).value[0] == 2d
        optimisedPower.get(5).value[1] == -20d
        optimisedPower.get(5).value[2] == 0d
        optimisedPower.get(6).key == 4
        optimisedPower.get(6).value[0] == 2d
        optimisedPower.get(6).value[1] == -20d
        optimisedPower.get(6).value[2] == 0d
        optimisedPower.get(7).key == 5
        optimisedPower.get(7).value[0] == 5d
        optimisedPower.get(7).value[1] == -20d
        optimisedPower.get(7).value[2] == 0d
    }

    def "Check energy schedule extraction"() {

        given: "an energy optimisation instance"
        currentInterval = 0
        def startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        def currentTime = startOfDay.plus(23, ChronoUnit.HOURS)
        def optimisation = new EnergyOptimiser(intervalSize, 1d)

        and: "some input parameters"
        double energyCapacity = 200d
        double energyLevelMin = 40d
        double energyLevelMax = 160d
        double[] energyMinLevels = new double[intervalCount]
        double[] energyMaxLevels = new double[intervalCount]
        Arrays.fill(energyMinLevels, energyLevelMin)
        Arrays.fill(energyMaxLevels, energyLevelMax)

        when: "an energy schedule is defined and the energy min levels generated from this"
        int[] energyScheduleDay = [
                0, // 00:00
                0,
                0,
                0,
                0,
                0,
                0,
                80,
                0,
                0,
                0,
                0,
                0, // 12:00
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0 // 23:00
        ]
        int[][] energyScheduleWeek = new int[24][7]
        Arrays.fill(energyScheduleWeek, energyScheduleDay)
        optimisation.applyEnergySchedule(energyMinLevels, energyMaxLevels, energyCapacity, energyScheduleWeek, currentTime)

        then: "the energy min levels should be correct"
        energyMinLevels == [40d, 40d, 160d, 40d, 40d, 40d, 40d, 40d] as double[]

        when: "the energy min levels are regenerated for a different time"
        currentTime = currentTime.plus(7, ChronoUnit.HOURS)
        Arrays.fill(energyMinLevels, energyLevelMin)
        optimisation.applyEnergySchedule(energyMinLevels, energyMaxLevels, energyCapacity, energyScheduleWeek, currentTime)

        then: "the energy min levels should be correct"
        energyMinLevels == [160d, 40d, 40d, 40d, 40d, 40d, 40d, 40d] as double[]

        when: "the optimisation is changed to have an interval size less than 1 hour"
        energyMinLevels = new double[24*4]
        energyMaxLevels = new double[24*4]
        Arrays.fill(energyMinLevels, energyLevelMin)
        Arrays.fill(energyMaxLevels, energyLevelMax)
        optimisation = new EnergyOptimiser(0.25d, 1d)
        optimisation.applyEnergySchedule(energyMinLevels, energyMaxLevels, energyCapacity, energyScheduleWeek, currentTime)

        then: "the energy min levels should be correct"
        energyMinLevels[0] == 160d
        IntStream.range(1, energyMinLevels.length).mapToDouble{energyMinLevels[it]}.allMatch{it == 40d}
    }

    def "Check full storage optimisation functionality"() {

        given: "an energy optimisation instance"
        currentInterval = 0
        def startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        def currentTime = startOfDay.plus(23, ChronoUnit.HOURS)
        def optimisation = new EnergyOptimiser(intervalSize, 1d)

        and: "some input parameters"
        double energyCapacity = 200d
        double energyLevelMin = 40d
        double energyLevelMax = 160d
        double currentEnergyLevel = 100d
        double[] powerSetpoints = new double[intervalCount]
        double[] energyMinLevels = new double[intervalCount]
        double[] energyMaxLevels = new double[intervalCount]
        Arrays.fill(energyMinLevels, energyLevelMin)
        Arrays.fill(energyMaxLevels, energyLevelMax)
        Function<Integer, Double> powerImportMaxCalculator = {interval -> 7d}
        Function<Integer, Double> powerExportMaxCalculator = {interval -> -20d}

        when: "an energy schedule is defined and the energy min levels generated from this"
        int[] energyScheduleDay = [
                0, // 00:00
                0,
                0,
                0,
                0,
                0,
                0,
                80,
                0,
                0,
                0,
                0,
                0, // 12:00
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0 // 23:00
        ]
        int[][] energyScheduleWeek = new int[24][7]
        Arrays.fill(energyScheduleWeek, energyScheduleDay)
        optimisation.applyEnergySchedule(energyMinLevels, energyMaxLevels, energyCapacity, energyScheduleWeek, currentTime)

        then: "the energy min levels should be correct"
        energyMinLevels == [40d, 40d, 160d, 40d, 40d, 40d, 40d, 40d] as double[]

        when: "the energy min levels are normalised"
        optimisation.normaliseEnergyMinRequirements(energyMinLevels, powerImportMaxCalculator, powerExportMaxCalculator, currentEnergyLevel)

        then: "the energy min levels should be correctly normalised"
        energyMinLevels == [118d, 139d, 160d, 100d, 40d, 40d, 40d, 40d] as double[]

        when: "the energy min levels are modified to improve code coverage in this test"
        Arrays.fill(energyMaxLevels, 170d)
        energyMinLevels = [133d, 150d, 166d, 130d, 130d, 100d, 10d, 100d] as double[]

        and: "the energy min levels are normalised"
        optimisation.normaliseEnergyMinRequirements(energyMinLevels, powerImportMaxCalculator, powerExportMaxCalculator, currentEnergyLevel)

        then: "the energy min levels should be correctly normalised"
        energyMinLevels == [121d, 142d, 163d, 130d, 130d, 100d, 79d, 100d] as double[]

        when: "the import requirements are optimised"
        def powerNetLimits = new double[intervalCount]
        Arrays.fill(powerNetLimits, 30d)
        def importCostCalculator = optimisation.getImportOptimiser(powerNets as double[], powerNetLimits, tariffImports as double[], tariffExports as double[], 0d)
        double[][] optimisedImport = IntStream.range(0, intervalCount).mapToObj{importCostCalculator.apply(it, [0d, powerImportMaxCalculator.apply(it)] as double[])}.toArray({new double[it][1]})

        and: "the export requirements are optimised"
        Arrays.fill(powerNetLimits, -30d)
        def exportCostCalculator = optimisation.getExportOptimiser(powerNets as double[], powerNetLimits, tariffImports as double[], tariffExports as double[], 0d)
        double[][] optimisedExport = IntStream.range(0, intervalCount).mapToObj{exportCostCalculator.apply(it, powerExportMaxCalculator.apply(it))}.toArray({new double[it][1]})

        and: "the applyEnergyMinImports routine is run on the input parameters"
        Function<Integer, Double> energyLevelCalculator = {int interval ->
            currentEnergyLevel + IntStream.range(0, interval).mapToDouble({j -> powerSetpoints[j] * intervalSize}).sum()
        }
        optimisation.applyEnergyMinImports(optimisedImport, energyMinLevels, powerSetpoints, energyLevelCalculator, importCostCalculator, powerImportMaxCalculator)

        then: "the power setpoints should have been updated to meet energy min requirements in the most cost effective way"
        powerSetpoints[0] == 7d
        powerSetpoints[1] == 7d
        powerSetpoints[2] == 7d
        powerSetpoints[3] == 0d
        powerSetpoints[4] == 0d
        powerSetpoints[5] == 0d
        powerSetpoints[6] == 0d
        powerSetpoints[7] == 0d

        when: "the earning opportunities are applied to the power setpoints"
        optimisation.applyEarningOpportunities(optimisedImport, optimisedExport, energyMinLevels, energyMaxLevels, powerSetpoints, energyLevelCalculator, powerImportMaxCalculator, powerExportMaxCalculator)

        then: "the power setpoints should have been updated to reflect utilisation of earning opportunities"
        powerSetpoints[0] == 7d
        powerSetpoints[1] == 7d
        powerSetpoints[2] == 7d
        powerSetpoints[3] == 0d
        powerSetpoints[4] closeTo(-4.66666, 0.0001)
        powerSetpoints[5] == 7d
        powerSetpoints[6] == -20d
        powerSetpoints[7] closeTo(-3.33333, 0.0001)
    }
}
