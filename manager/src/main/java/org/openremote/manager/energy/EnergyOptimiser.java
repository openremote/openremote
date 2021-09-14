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
package org.openremote.manager.energy;

import org.openremote.model.util.Pair;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.manager.energy.EnergyOptimisationService.LOG;

public class EnergyOptimiser {

    protected double intervalSize;
    protected double financialWeighting;

    /**
     * 24 divided by intervalSize must be a whole number
     */
    public EnergyOptimiser(double intervalSize, double financialWeighting) throws IllegalArgumentException {
        if ((24d / intervalSize) != (int) (24d / intervalSize)) {
            throw new IllegalArgumentException("24 divided by intervalSizeHours must be whole number");
        }
        this.intervalSize = intervalSize;
        this.financialWeighting = Math.max(0, Math.min(1d, financialWeighting));
    }

    public double getIntervalSize() {
        return intervalSize;
    }

    public double getFinancialWeighting() {
        return financialWeighting;
    }

    public int get24HourIntervalCount() {
        return (int) (24d / intervalSize);
    }

//    /**
//     * Function to calculate the power requirements for the given storage asset at each interval in the window
//     * based on the provided power demand requirements and potential cost saving at each interval so for each interval
//     * a an array of [powerDemand, gridCost] is required. For a given interval a positive value means importing
//     * power and negative means exporting power:
//     * <ul>
//     * <li>powerDemand = The net demand of suppliers - consumers - higher priority supplier capabilities</li>
//     * <li>gridCost = Grid cost / kWh (positive means expense, negative means income)</li>
//     * </ul>
//     */
//    // TODO: powerImportMax should be a function of energy level
//    public Function<double[][], double[]> getStoragePowerCalculator(ElectricityStorageAsset storageAsset) {
//
//        return (powerExportsAndSavings) -> {
//
//            double energyCapacity = storageAsset.getEnergyCapacity().orElse(0d);
//            double storedEnergy = storageAsset.getEnergyLevel().orElse(0d);
//            double tariffExport = storageAsset.getTariffExport().orElse(0d);
//            double tariffImport = storageAsset.getTariffExport().orElse(0d);
//            double carbonExport = storageAsset.getCarbonExport().orElse(0d);
//            double carbonImport = storageAsset.getCarbonImport().orElse(0d);
//            double costExport = financialWeighting * tariffExport + (1d-financialWeighting) * carbonExport;
//            double costImport = financialWeighting * tariffImport + (1d-financialWeighting) * carbonImport;
//            double powerEfficiencyImport = storageAsset.getEfficiencyImport().orElse(100);
//            double powerEfficiencyExport = storageAsset.getEfficiencyExport().orElse(100);
//            double powerImportMax = storageAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
//            double powerExportMax = storageAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
//            int energyLevelMax = storageAsset.getEnergyLevelPercentageMax().orElse(100);
//
//
//            double[] powerImport = new double[intervalCount];
//            double[] powerExport = new double[intervalCount];
//            double[] energyLevel = new double[intervalCount];
//            Arrays.fill(energyLevel, storedEnergy);
//
////            // Calculate total energy requirement
////            double totalEnergyRequired = Arrays.stream(powerExportsAndSavings)
////                .map(interval -> interval[2] * intervalSize)
////                .reduce(0d, Double::sum);
//
////            // If we have enough energy stored then just consume that (no need to import more)
////            if (totalEnergyRequired < storedEnergy) {
////                Arrays.fill(powerImport, 0d);
////                return powerImport;
////            }
//
//            /* Look for storage import income opportunities (i.e. when grid pays us to import) */
//
//            // Order intervals based on potential income [gridCost+storageImportCost] < 0 (smallest first)
//            Integer[] indexesSortedIncome = IntStream.range(0, powerExportsAndSavings.length).boxed().toArray(Integer[]::new);
//            Arrays.sort(
//                indexesSortedIncome,
//                Comparator.comparingDouble((i) -> powerExportsAndSavings[i][1] + costExport)
//            );
//
//            for (int i=0; i<indexesSortedIncome.length; i++) {
//                double gridCost = powerExportsAndSavings[i][1];
//
//                if (gridCost + costImport >= 0) {
//                    // No income opportunity
//                    break;
//                }
//
//                // TODO: Ensure we don't exceed power demand limits
//                // Find an earlier interval
//            }
//
//
//            // Order intervals based on potential cost saving [gridCost-storageExportCost] (largest first)
//            Integer[] indexesSortedSaving = IntStream.range(0, powerExportsAndSavings.length).boxed().toArray(Integer[]::new);
//            Arrays.sort(
//                indexesSortedSaving,
//                Comparator.<Integer>comparingDouble((i) -> powerExportsAndSavings[i][1] - costExport).reversed()
//            );
//
//
//            // Find an interval earlier than each ordered export interval that costs less than the potential saving
//            Arrays.stream(indexesSortedSaving).forEach(powerExportIndex -> {
//                double[] powerExportDemand = powerExportsAndSavings[powerExportIndex];
//                double powerDemand = powerExportDemand[0];
//                double saving = powerExportDemand[1] - costExport;
//
//                // Order grid costs up to this interval (smallest first)
//                Integer[] indexesSortedGridCost = IntStream.range(0, powerExportIndex).boxed().toArray(Integer[]::new);
//                Arrays.sort(
//                    indexesSortedGridCost,
//                    Comparator.comparingDouble((i) -> powerExportsAndSavings[i][1])
//                );
//
//                // Go through intervals allocating required power demand until interval reaches powerImportMax or storage is full
//                for (int i=0; i<indexesSortedGridCost.length; i++) {
//                    // How to decide whether to just consume from storage or import
//                }
//
//                while (powerDemand > 0 && i<indexesSortedGridCost.length) {
//                    double used = powerImport[i];
//
//                    if (used < powerImportMax) {
//                        if ((powerImportMax - used) > powerDemand) {
//                            // This interval can fulfill entire demand
//                            used += powerDemand;
//                            powerDemand = 0;
//                        } else {
//                            // This interval can only partially fulfill the demand
//                            powerDemand -= (powerImportMax - used);
//                            used = powerImportMax;
//                        }
//                    }
//                    powerImport[i] = used;
//                    i++;
//                }
//            });
//        };
//    }

    /**
     * Will take the supplied 24x7 energy schedule percentages and energy level min/max values and apply them to the
     * supplied energyLevelMins also adjusting for any intervalSize difference. The energy schedule should be in UTC
     * time.
     */
    public void applyEnergySchedule(double[] energyLevelMins, double[] energyLevelMaxs, double energyCapacity, int[][] energyLevelSchedule, LocalDateTime currentTime) {

        if (energyLevelSchedule == null) {
            return;
        }

        // Extract the schedule for the next 24 hour period starting at current hour plus 1 (need to attain energy level by the time the hour starts)
        OffsetDateTime date = currentTime.plus(1, ChronoUnit.HOURS).atOffset(ZoneOffset.UTC);
        int dayIndex = date.getDayOfWeek().getValue();
        int hourIndex = date.get(ChronoField.HOUR_OF_DAY);
        int i = 0;
        double[] schedule = new double[24];

        while (i < 24) {
            // Convert from % to absolute value
            schedule[i] = energyCapacity * energyLevelSchedule[dayIndex][hourIndex] * 0.01;
            hourIndex++;
            if (hourIndex > 23) {
                hourIndex = 0;
                dayIndex = (dayIndex + 1) % 7;
            }
            i++;
        }

        // Convert schedule intervals to match optimisation intervals - need to look at schedule for
        if (intervalSize <= 1d) {
            int hourIntervals = (int) (1d / intervalSize);

            for (i = 0; i < schedule.length; i++) {
                // Put energy level schedule value into first interval for the hour
                energyLevelMins[(hourIntervals * i)] = Math.min(energyLevelMaxs[hourIntervals * i], Math.max(energyLevelMins[hourIntervals * i], schedule[i]));
            }
        } else {
            int takeSize = (int) intervalSize;
            int hourIntervals = (int) (24d / intervalSize);

            for (i = 0; i < hourIntervals; i++) {
                // Take largest energy level for the intervals
                energyLevelMins[i] = Math.min(energyLevelMaxs[i], Math.max(energyLevelMins[i], java.util.Arrays.stream(schedule, (i * takeSize), (i * takeSize) + takeSize).max().orElse(0)));
            }
        }
    }

    /**
     * Adjusts the supplied energyLevelMin values to match the physical characteristics (i.e. the charge and discharge
     * rates).
     */
    public void normaliseEnergyMinRequirements(double[] energyLevelMins, Function<Integer, Double> powerImportMaxCalculator, Function<Integer, Double> powerExportMaxCalculator, double energyLevel) {

        int intervalCount = get24HourIntervalCount();
        Function<Integer, Double> previousEnergyLevelCalculator = i -> (i == 0 ? energyLevel : energyLevelMins[i - 1]);

        // Adjust energy min requirements to match physical characteristics (charge/discharge rate)
        IntStream.range(0, intervalCount).forEach(i -> {
            double energyDelta = energyLevelMins[i] - previousEnergyLevelCalculator.apply(i);

            if (energyDelta > 0) {

                // May need to increase earlier min values until there is no energy deficit with previous interval
                // If we reach interval 0 and there is still a deficit then need to reduce this energy level
                for (int j = i; j >= 0; j--) {
                    double previousMin = energyLevelMins[j] - (powerImportMaxCalculator.apply(j) * intervalSize);
                    double previous = previousEnergyLevelCalculator.apply(j);

                    if (previous < previousMin) {
                        if (j == 0) {
                            // Can't attain so shift all min values down
                            double shift = previous - previousMin;
                            for (int k = 0; k <= i; k++) {
                                energyLevelMins[k] += shift;
                            }
                        } else {
                            // Increase the previous min value
                            energyLevelMins[j - 1] = previousMin;
                        }
                    } else {
                        // Already at or above min requirement
                        break;
                    }
                }

            } else if (energyDelta < 0) {

                // May need to spread discharge over this and later intervals
                for (int j = i; j < intervalCount; j++) {

                    double min = previousEnergyLevelCalculator.apply(j) + (powerExportMaxCalculator.apply(j) * intervalSize);

                    if (min > energyLevelMins[j]) {
                        energyLevelMins[j] = min;
                    } else {
                        // Already at or above min requirement
                        break;
                    }
                }
            }
        });
    }

    /**
     * Will update the powerSetpoints in order to achieve the energyLevelMin values supplied.
     */
    public void applyEnergyMinImports(double[][] importCostAndPower, double[] energyLevelMins, double[] powerSetpoints, Function<Integer, Double> energyLevelCalculator, BiFunction<Integer, double[], double[]> importOptimiser, Function<Integer, Double> powerImportMaxCalculator) {
        // Ensure min energy levels are attained by the end of the interval as these have priority
        AtomicInteger fromInterval = new AtomicInteger(0);

        IntStream.range(0, get24HourIntervalCount()).forEach(i -> {
            double intervalEnergyLevel = energyLevelCalculator.apply(i);
            double energyDeficit = energyLevelMins[i] - intervalEnergyLevel;

            if (energyDeficit > 0) {
                double energyAttainable = powerImportMaxCalculator.apply(i) * intervalSize;
                energyAttainable = Math.min(energyDeficit, energyAttainable);
                powerSetpoints[i] = energyAttainable / intervalSize;
                energyDeficit -= energyAttainable;

                if (energyDeficit > 0) {
                    retrospectiveEnergyAllocator(importCostAndPower, energyLevelMins, powerSetpoints, importOptimiser, powerImportMaxCalculator, energyDeficit, fromInterval.getAndSet(i), i);
                }
            }
        });
    }

    /**
     * Creates earlier imports between fromInterval (inclusive) and toInterval (exclusive) in order to meet min energy
     * level requirement at the specified interval based on the provided energy level at the start of fromInterval.
     */
    public void retrospectiveEnergyAllocator(double[][] importCostAndPower, double[] energyLevelMins, double[] powerSetpoints, BiFunction<Integer, double[], double[]> importOptimiser, Function<Integer, Double> powerImportMaxCalculator, double energyLevel, int fromInterval, int toInterval) {

        double energyDeficit = energyLevelMins[toInterval] - energyLevel;

        if (energyDeficit <= 0) {
            return;
        }

        // Do import until energy deficit reaches 0 or there are no more intervals
        boolean canMeetDeficit = IntStream.range(fromInterval, toInterval).mapToDouble(i ->
            Math.min(powerImportMaxCalculator.apply(i), importCostAndPower[i][2])
        ).sum() >= energyDeficit;
        boolean morePowerAvailable = !canMeetDeficit && IntStream.range(fromInterval, toInterval).mapToObj(i -> importCostAndPower[i][2] < powerImportMaxCalculator.apply(i)).anyMatch(b -> b);

        if (!canMeetDeficit && morePowerAvailable) {
            // Need to push imports beyond optimum to fulfill energy deficit
            IntStream.range(fromInterval, toInterval).forEach(i -> {
                double powerImportMax = powerImportMaxCalculator.apply(i);
                if (importCostAndPower[i][2] < powerImportMax) {
                    importCostAndPower[i] = importOptimiser.apply(i, new double[]{0d, powerImportMax});
                }
            });
        }

        // Sort import intervals by cost (lowest to highest)
        List<Pair<Integer, double[]>> sortedImportCostAndPower = IntStream.range(fromInterval, toInterval)
            .mapToObj(i -> new Pair<>(i, importCostAndPower[i])).sorted(
                Comparator.comparingDouble(pair -> pair.value[0])
            ).collect(Collectors.toList());

        int i = 0;
        while (energyDeficit > 0 && i < sortedImportCostAndPower.size()) {
            double importPower = Math.min(powerImportMaxCalculator.apply(i), importCostAndPower[i][2]);
            double requiredPower = energyDeficit / intervalSize;
            // If we earn by importing then take the maximum power
            importPower = importCostAndPower[i][0] < 0 ? importPower : Math.min(importPower, requiredPower);
            powerSetpoints[i] = importPower;
            energyDeficit -= importPower;
            i++;
        }
    }

    /**
     * Will find the best earning opportunity for each interval (import or export) and will then try to apply them in
     * chronological order (reallocating earlier import/exports if it cost beneficial). The powerSetpoints will be
     * updated as a result.
     */
    public void applyEarningOpportunities(double[][] importCostAndPower, double[][] exportCostAndPower, double[] energyLevelMins, double[] energyLevelMaxs, double[] powerSetpoints, Function<Integer, Double> energyLevelCalculator, Function<Integer, Double> powerImportMaxCalculator, Function<Integer, Double> powerExportMaxCalculator) {
        LOG.finer("Applying earning opportunities");

        // Look for import and export earning opportunities
        double[][] primary = importCostAndPower != null ? importCostAndPower : exportCostAndPower; // Never null
        double[][] secondary = importCostAndPower != null ? exportCostAndPower : null; // Could be null

        List<Pair<Integer, double[]>> earningOpportunities = IntStream.range(0, primary.length).mapToObj(i -> {
            if (secondary == null) {
                return new Pair<>(i, primary[i]);
            }
            // Return whichever has the lowest cost
            if (primary[i][0] < secondary[i][0]) {
                return new Pair<>(i, primary[i]);
            }
            return new Pair<>(i, secondary[i]);
        })
            .filter(intervalCostAndPowerBand -> intervalCostAndPowerBand.value[0] < 0)
            .sorted(Comparator.comparingDouble(optimisedInterval -> optimisedInterval.value[0]))
            .collect(Collectors.toList());

        if (earningOpportunities.isEmpty()) {
            LOG.finest("No earning opportunities found");
        }

        if (LOG.isLoggable(Level.FINEST)) {
            earningOpportunities.forEach(op -> LOG.finest("Earning opportunity: interval=" + op.key + ", cost=" + op.value[0] + ", powerMin=" + op.value[1] + ", powerMax=" + op.value[2]));
        }

        // Go through each earning opportunity and determine if it can be utilised without breaching the energy min
        // levels
        for (Pair<Integer, double[]> earningOpportunity : earningOpportunities) {
            int interval = earningOpportunity.key;
            double[] costAndPower = earningOpportunity.value;
            assert importCostAndPower != null;
            assert exportCostAndPower != null;

            if (isImportOpportunity(costAndPower, powerSetpoints[interval], interval, powerImportMaxCalculator)) {
                // import opportunity and interval still available to import power
                applyImportOpportunity(importCostAndPower, exportCostAndPower, energyLevelMins, energyLevelMaxs, powerSetpoints, energyLevelCalculator, powerImportMaxCalculator, powerExportMaxCalculator, interval);
            } else if (isExportOpportunity(costAndPower, powerSetpoints[interval], interval, powerExportMaxCalculator)) {
                // export opportunity and interval still available to export power
                applyExportOpportunity(importCostAndPower, exportCostAndPower, energyLevelMins, energyLevelMaxs, powerSetpoints, energyLevelCalculator, powerImportMaxCalculator, powerExportMaxCalculator, interval);
            }
        }
    }

    protected boolean isImportOpportunity(double[] costAndPower, double powerSetpoint, int interval, Function<Integer, Double> powerImportMaxCalculator) {
        return costAndPower[2] > 0 && powerSetpoint >= 0 && powerSetpoint < Math.min(powerImportMaxCalculator.apply(interval), costAndPower[2]);
    }

    protected boolean isExportOpportunity(double[] costAndPower, double powerSetpoint, int interval, Function<Integer, Double> powerExportMaxCalculator) {
        return costAndPower[1] < 0 && powerSetpoint <= 0 && powerSetpoint > Math.max(powerExportMaxCalculator.apply(interval), costAndPower[1]);
    }

    /**
     * Tries to apply the maximum import power as defined in the importCostAndPower at the specified interval taking
     * into consideration the maximum power and energy levels; if there is insufficient power or energy capacity at the
     * interval then an earlier cost effective export opportunity will be attempted to offset the requirement. The
     * powerSetpoints will be updated as a result.
     */
    public void applyImportOpportunity(double[][] importCostAndPower, double[][] exportCostAndPower, double[] energyLevelMins, double[] energyLevelMaxs, double[] powerSetpoints, Function<Integer, Double> energyLevelCalculator, Function<Integer, Double> powerImportMaxCalculator, Function<Integer, Double> powerExportMaxCalculator, int interval) {
        LOG.finest("Applying import earning opportunity: interval=" + interval);
        double[] costAndPower = importCostAndPower[interval];
        double impPowerMin = costAndPower[1];
        double impPowerMax = Math.min(powerImportMaxCalculator.apply(interval), costAndPower[2]);
        double powerCapacity = impPowerMax - powerSetpoints[interval];

        if (impPowerMin > powerCapacity) {
            LOG.finest("Can't attain min power level to make use of this opportunity");
            return;
        }

        double energySpace = energyLevelMaxs[interval] - energyLevelCalculator.apply(interval);
        double energySpaceMax = powerCapacity * intervalSize;
        double energySpaceMin = impPowerMin * intervalSize;
        List<Pair<Integer, Double>> pastIntervalPowerDeltas = new ArrayList<>();

        int k = interval;
        while (k < powerSetpoints.length && energySpace > 0 && energySpace >= energySpaceMin) {
            double futureEnergySpace = energyLevelMaxs[k] - energyLevelCalculator.apply(k);
            energySpace = Math.min(energySpace, futureEnergySpace);
            k++;
        }

        if (energySpace < energySpaceMax && exportCostAndPower != null) {
            // Can't maximise on opportunity without exporting earlier on so can this be done
            // in a cost effective way
            LOG.finest("Looking for earlier export opportunities to maximise on this import opportunity: space=" + energySpace + ", max=" + energySpaceMax);
            int i = interval - 1;
            List<Pair<Integer, Double>> pastOpportunities = new ArrayList<>();

            while (i >= 0) {
                if (costAndPower[0] + exportCostAndPower[i][0] < 0 && powerSetpoints[i] <= 0) {
                    // We can afford to export earlier and still earn from this import
                    pastOpportunities.add(new Pair<>(i, exportCostAndPower[i][0]));
                }
                i--;
            }

            pastOpportunities.sort(Comparator.comparingDouble(op -> op.value));
            int j = 0;

            if (pastOpportunities.isEmpty()) {
                LOG.finest("No earlier export opportunities identified");
            }

            while (energySpace < energySpaceMax && j < pastOpportunities.size()) {
                // Energy level at this interval must be above energy min to consider exporting
                Pair<Integer, Double> opportunity = pastOpportunities.get(j);
                int pastInterval = opportunity.key;

                // Power capacity must be within the optimum power band
                double[] pastCostAndPower = exportCostAndPower[pastInterval];
                double expPowerMax = Math.max(powerExportMaxCalculator.apply(pastInterval), pastCostAndPower[1]);
                double expPowerCapacity = expPowerMax - powerSetpoints[pastInterval];

                if (expPowerCapacity >= 0 || expPowerCapacity > pastCostAndPower[2]) {
                    LOG.finest("Power capacity is outside optimum power band so cannot use this opportunity");
                    j++;
                    continue;
                }

                double energySurplusMin = pastCostAndPower[2] * intervalSize;
                double energySurplus = energyLevelMins[pastInterval] - energyLevelCalculator.apply(pastInterval);
                energySurplus = Math.max(energySurplus, energySpace - energySpaceMax);

                // We have spare energy capacity and power check if we don't violate energy min for any future exports
                k = pastInterval;
                while (k < powerSetpoints.length && energySurplus < 0 && energySurplus <= energySurplusMin) {
                    double futureEnergySurplus = energyLevelCalculator.apply(k) - energyLevelMins[k];
                    energySurplus = Math.max(energySurplus, -futureEnergySurplus);
                    if (energySurplus <= 0) {
                        LOG.finest("Earlier export opportunity would violate future energy min level: interval=" + j + ", futureInterval=" + k);
                    }
                    k++;
                }

                expPowerCapacity = Math.max(expPowerCapacity, energySurplus / intervalSize);

                if (expPowerCapacity < 0 && expPowerCapacity < pastCostAndPower[2]) {
                    // We can export in the optimum range
                    energySpace += (-1d * expPowerCapacity * intervalSize);
                    pastIntervalPowerDeltas.add(new Pair<>(pastInterval, expPowerCapacity));
                    LOG.finest("Earlier export opportunity identified: interval=" + pastInterval + ", power=" + expPowerCapacity);
                }

                j++;
            }
        }

        // Do original import if there is enough energy space
        if (energySpace > 0 && energySpace >= energySpaceMin) {

            // Adjust past interval set points as required
            pastIntervalPowerDeltas.forEach(intervalAndDelta -> powerSetpoints[intervalAndDelta.key] += intervalAndDelta.value);

            energySpaceMax = Math.min(energySpaceMax, energySpace);
            powerCapacity = Math.min(impPowerMax - powerSetpoints[interval], (energySpaceMax / intervalSize));
            powerSetpoints[interval] = powerSetpoints[interval] + powerCapacity;
            LOG.finer("Applied import earning opportunity: set point=" + powerSetpoints[interval] + " (delta: " + powerCapacity + ")");
        }
    }

    /**
     * Tries to apply the maximum export power as defined in the exportCostAndPower at the specified interval taking
     * into consideration the maximum power and energy levels; if there is insufficient power or energy capacity at the
     * interval then an earlier cost effective import opportunity will be attempted to offset the requirement. The
     * powerSetpoints will be updated as a result.
     */
    public void applyExportOpportunity(double[][] importCostAndPower, double[][] exportCostAndPower, double[] energyLevelMins, double[] energyLevelMaxs, double[] powerSetpoints, Function<Integer, Double> energyLevelCalculator, Function<Integer, Double> powerImportMaxCalculator, Function<Integer, Double> powerExportMaxCalculator, int interval) {
        LOG.finest("Applying export earning opportunity: interval=" + interval);
        double[] costAndPower = exportCostAndPower[interval];
        double expPowerMin = costAndPower[2];
        double expPowerMax = Math.max(powerExportMaxCalculator.apply(interval), costAndPower[1]);
        double powerCapacity = expPowerMax - powerSetpoints[interval];

        if (expPowerMin < powerCapacity) {
            LOG.finest("Can't attain min power level to make use of this opportunity");
            return;
        }

        double energySurplus = energyLevelCalculator.apply(interval) - energyLevelMins[interval];
        double energySurplusMin = -1d * expPowerMin * intervalSize;
        double energySurplusMax = -1d * powerCapacity * intervalSize;
        List<Pair<Integer, Double>> pastAndFutureIntervalPowerDeltas = new ArrayList<>();

        int k = interval;
        while (k < powerSetpoints.length && energySurplus > 0 && energySurplus >= energySurplusMin) {

            double futureEnergySurplus = energyLevelCalculator.apply(k) - energyLevelMins[k];

            // The following is an attempt to make use of an earning opportunity that would violate future energy limits
            // by allocating extra imports between 'now' and 'then' - this needs more work
//            if (futureEnergySurplus < energySurplusMin || futureEnergySurplus <= 0) {
//                // Try and allocate an import between this future point and the earning opportunity to prevent this deficit
//                int l = k - 1;
//                while (futureEnergySurplus < energySurplusMax && l > interval) {
//                    int finalL = l;
//                    if (powerSetpoints[l] >= 0 && pastAndFutureIntervalPowerDeltas.stream().noneMatch(delta -> delta.key.equals(finalL))) {
//
//                        double surplusDeficit = energySurplusMax - futureEnergySurplus;
//                        double intermediateEnergyCapacity = energyLevelMax - energyLevelCalculator.apply(l);
//                        double intermediatePowerCapacity = powerImportMaxCalculator.apply(l) - powerSetpoints[l];
//                        intermediatePowerCapacity = Math.min(intermediatePowerCapacity, surplusDeficit / intervalSize);
//                        intermediatePowerCapacity = Math.min(intermediatePowerCapacity, intermediateEnergyCapacity / intervalSize);
//
//                        if (intermediatePowerCapacity > 0 && powerSetpoints[l] + intermediatePowerCapacity > importCostAndPower[l][1] && costAndPower[0] + importCostAndPower[l][0] < 0) {
//                            // There is capacity and it is cost effective to use it
//                            futureEnergySurplus = intermediatePowerCapacity * intervalSize;
//                            pastAndFutureIntervalPowerDeltas.add(new Pair<>(l, intermediatePowerCapacity));
//                        }
//                    }
//                    l--;
//                }
//            }

            energySurplus = Math.min(energySurplus, futureEnergySurplus);
            k++;

        }

        if (energySurplus < energySurplusMax && importCostAndPower != null) {
            // Can't maximise on opportunity without importing earlier on so can this be done
            // in a cost effective way
            LOG.finest("Looking for earlier import opportunities to maximise on this export opportunity: surplus=" + energySurplus + ", max=" + energySurplusMax);
            int i = interval - 1;
            List<Pair<Integer, Double>> pastOpportunities = new ArrayList<>();

            while (i >= 0) {

                if (costAndPower[0] + importCostAndPower[i][0] < 0 && powerSetpoints[i] >= 0) {
                    // We can afford to import and still earn using original export
                    pastOpportunities.add(new Pair<>(i, importCostAndPower[i][0]));
                }

                i--;
            }

            pastOpportunities.sort(Comparator.comparingDouble(op -> op.value));
            int j = 0;

            if (pastOpportunities.isEmpty()) {
                LOG.finest("No earlier import opportunities identified");
            }

            while (energySurplus < energySurplusMax && j < pastOpportunities.size()) {
                Pair<Integer, Double> opportunity = pastOpportunities.get(j);
                int pastInterval = opportunity.key;

                // Power capacity must be within the optimum power band
                double[] pastCostAndPower = importCostAndPower[pastInterval];
                double impPowerMax = Math.min(powerImportMaxCalculator.apply(interval), pastCostAndPower[2]);
                double impPowerCapacity = impPowerMax - powerSetpoints[pastInterval];

                if (impPowerCapacity <= 0 || impPowerCapacity < pastCostAndPower[1]) {
                    LOG.finest("Power capacity is outside optimum power band so cannot use this opportunity");
                    j++;
                    continue;
                }

                double energySpaceMin = pastCostAndPower[1] * intervalSize;
                double energySpace = energyLevelMaxs[interval] - energyLevelCalculator.apply(pastInterval);
                energySpace = Math.max(energySpace, energySpace - energySurplusMax);

                // We have spare energy capacity and power check if we don't violate energy max for any future imports
                k = pastInterval;
                while (k < powerSetpoints.length && energySpace > 0 && energySpace >= energySpaceMin) {

                    double futureEnergySpace = energyLevelMaxs[k] - energyLevelCalculator.apply(k);
                    energySpace = Math.min(energySpace, futureEnergySpace);
                    if (energySpace <= 0) {
                        LOG.finest("Earlier import opportunity would violate future energy max level: interval=" + j + ", futureInterval=" + k);
                    }
                    k++;

                }

                impPowerCapacity = Math.min(impPowerCapacity, energySpace / intervalSize);

                if (impPowerCapacity > 0 && impPowerCapacity > pastCostAndPower[1]) {
                    // We can import in the optimum range
                    energySurplus += (impPowerCapacity * intervalSize);
                    pastAndFutureIntervalPowerDeltas.add(new Pair<>(pastInterval, impPowerCapacity));
                    LOG.finest("Earlier import opportunity identified: interval=" + pastInterval + ", power=" + impPowerCapacity);
                }

                j++;
            }
        }

        // Do original export if there is any energy surplus
        if (energySurplus > 0 && energySurplus >= energySurplusMin) {

            // Adjust past interval set points as required
            pastAndFutureIntervalPowerDeltas.forEach(intervalAndDelta -> powerSetpoints[intervalAndDelta.key] += intervalAndDelta.value);

            energySurplusMax = Math.min(energySurplusMax, energySurplus);
            powerCapacity = Math.max(expPowerMax - powerSetpoints[interval], -1d * (energySurplusMax / intervalSize));
            powerSetpoints[interval] = powerSetpoints[interval] + powerCapacity;
            LOG.finer("Applied export earning opportunity: interval=" + interval + ", set point=" + powerSetpoints[interval] + " (delta: " + powerCapacity + ")");
        }
    }

    /**
     * Returns a function that can be used to calculate any export saving (per kWh) and power band needed to achieve it
     * based on requested interval index and power export max value (negative as this is for export). This is used to
     * determine whether there are export opportunities for earning/saving rather than using the grid.
     */
    public BiFunction<Integer, Double, double[]> getExportOptimiser(double[] powerNets, double[] powerNetLimits, double[] tariffImports, double[] tariffExports, double assetExportCost) {

        // Power max should be negative as this is export
        return (interval, powerMax) -> {
            double powerNet = powerNets[interval];
            double powerNetLimit = powerNetLimits[interval];
            double tariffImport = tariffImports[interval];
            double tariffExport = tariffExports[interval];
            powerMax = Math.max(powerMax, powerNetLimit - powerNet);

            if (powerMax >= 0) {
                // No capacity to export
                return new double[]{Double.MAX_VALUE, 0d, 0d};
            }

            if (powerNet <= 0) {
                // Already net exporting so tariff will not change if we export more
                return new double[]{tariffExport + assetExportCost, powerMax, 0d};
            }

            if (powerNet + powerMax > 0d) {
                // Can't make tariff flip (we're reducing import hence the -1d)
                return new double[]{(-1d * tariffImport) + assetExportCost, powerMax, 0d};
            }

            // We can flip tariffs if we export enough power
            double powerStart = 0d;
            double powerEnd = 0d - powerNet; // Inflection point where switch to export instead of import

            // If import was paying then reducing import is a loss in earnings
            double cost = powerEnd * (tariffImport - assetExportCost);

            // Is it beneficial to include remaining (-ve export power)
            if (tariffExport + assetExportCost < 0d || tariffExport <= (-1d * tariffImport)) {
                if (Math.abs((-1d * tariffImport) - tariffExport) > Double.MIN_VALUE) {
                    // We need to be at power max to achieve the optimum cost
                    powerStart = powerMax;
                }
                cost += -1d * (powerMax - powerEnd) * (tariffExport + assetExportCost);
                powerEnd = powerMax;
            }

            // Normalise the cost
            cost = cost / (-1d * powerEnd);

            return new double[]{cost, powerEnd, powerStart};
        };
    }

    /**
     * Returns a function that can be used to calculate the optimum cost (per kWh) and power band needed to achieve it
     * based on requested interval index, power min and power max values. The returned power band will satisfy the
     * requested power min value but this could mean that cost is not optimum for that interval if a lower power could
     * be used. If possible a 0 power min should be tried first for all applicable intervals and if more energy is
     * required then another pass can be made with a high enough min power to allow desired energy levels to be reached.
     * This is used to determine the best times and power values for importing energy to meet the requirements.
     */
    public BiFunction<Integer, double[], double[]> getImportOptimiser(double[] powerNets, double[] powerNetLimits, double[] tariffImports, double[] tariffExports, double assetImportCost) {

        return (interval, powerRequiredMinMax) -> {

            double powerNet = powerNets[interval];
            double powerNetLimit = powerNetLimits[interval];
            double tariffImport = tariffImports[interval];
            double tariffExport = tariffExports[interval];
            double powerMin = powerRequiredMinMax[0];
            double powerMax = Math.min(powerRequiredMinMax[1], powerNetLimit - powerNet);

            if (powerMax <= 0d) {
                // No capacity to import
                return new double[]{Double.MAX_VALUE, 0d, 0d};
            }

            if (powerNet >= 0d) {
                // Already net importing so tariff will not change if we import more
                return new double[]{tariffImport + assetImportCost, powerMin, powerMax};
            }

            if (powerNet + powerMax < 0d) {
                // Can't make tariff flip (we're reducing import hence the -1d)
                return new double[]{(-1d * tariffExport) + assetImportCost, powerMin, powerMax};
            }

            // We can flip tariffs if we take enough power
            double powerStart = powerMin;
            double powerEnd = 0d - powerNet; // Inflection point where switch to import instead of export

            // If export was paying then reducing export is a loss in earnings i.e. a cost and vice versa hence the -1d
            double cost = -1d * powerEnd * (tariffExport - assetImportCost);

            if (powerMin > powerEnd) {
                // We have to flip to meet power req
                cost += (powerMin - powerEnd) * (tariffImport + assetImportCost);
                powerEnd = powerMin;
            }

            if (powerEnd < powerMax) {
                // Is it beneficial to include remaining (+ve import power)
                if (tariffImport + assetImportCost < 0d || tariffImport <= (-1d * tariffExport)) {
                    if (Math.abs((-1d * tariffExport) - tariffImport) > Double.MIN_VALUE) {
                        // We need to be at power max to achieve the optimum cost
                        powerStart = powerMax;
                    }
                    cost += (powerMax - powerEnd) * (tariffImport + assetImportCost);
                    powerEnd = powerMax;
                }
            }

            // Normalise the cost
            cost = cost / powerEnd;

            return new double[]{cost, powerStart, powerEnd};
        };
    }
}
