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

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointIntervalQuery;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.BooleanPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;

/**
 * Handles optimisation instances for {@link EnergyOptimisationAsset}.
 */
public class EnergyOptimisationService extends RouteBuilder implements ContainerService {

    protected static class OptimisationInstance {
        EnergyOptimisationAsset optimisationAsset;
        EnergyOptimiser energyOptimiser;
        ScheduledFuture<?> optimiserFuture;

        /**
         * This keeps track of a theoretical energy level of storage assets. This is used to calculate
         * the theoretical un-optimised costs.
         */
        Map<String, Double> unoptimisedStorageAssetEnergyLevels = new HashMap<>();

        public OptimisationInstance(EnergyOptimisationAsset optimisationAsset, EnergyOptimiser energyOptimiser, ScheduledFuture<?> optimiserFuture) {
            this.optimisationAsset = optimisationAsset;
            this.energyOptimiser = energyOptimiser;
            this.optimiserFuture = optimiserFuture;
        }
    }

    protected static final Logger LOG = Logger.getLogger(EnergyOptimisationService.class.getName());
    protected static final int OPTIMISATION_TIMEOUT_MILLIS = 60000*10; // 10 mins
    protected DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC));
    protected TimerService timerService;
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;
    protected ScheduledExecutorService executorService;
    protected final Map<String, OptimisationInstance> assetOptimisationInstanceMap = new HashMap<>();
    protected List<String> forceChargeAssetIds = new ArrayList<>();

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        clientEventService = container.getService(ClientEventService.class);
        gatewayService = container.getService(GatewayService.class);
        executorService = container.getExecutorService();
    }

    @Override
    public void start(Container container) throws Exception {
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        // Load all enabled optimisation assets and instantiate an optimiser for each
        LOG.fine("Loading optimisation assets...");

        List<EnergyOptimisationAsset> energyOptimisationAssets = assetStorageService.findAll(
            new AssetQuery()
                .types(EnergyOptimisationAsset.class)
        )
            .stream()
            .map(asset -> (EnergyOptimisationAsset) asset)
            .filter(optimisationAsset -> !optimisationAsset.isOptimisationDisabled().orElse(false))
            .toList();

        LOG.fine("Found enabled optimisation asset count = " + energyOptimisationAssets.size());

        energyOptimisationAssets.forEach(this::startOptimisation);

        clientEventService.addInternalSubscription(
            AttributeEvent.class,
            null,
            this::processAttributeEvent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("Persistence-EnergyOptimisation")
            .filter(isPersistenceEventForEntityType(EnergyOptimisationAsset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> processAssetChange((PersistenceEvent<EnergyOptimisationAsset>) exchange.getIn().getBody(PersistenceEvent.class)));
    }

    @Override
    public void stop(Container container) throws Exception {
        new ArrayList<>(assetOptimisationInstanceMap.keySet())
            .forEach(this::stopOptimisation);
    }

    protected void processAssetChange(PersistenceEvent<EnergyOptimisationAsset> persistenceEvent) {
        LOG.fine("Processing optimisation asset change: " + persistenceEvent);
        stopOptimisation(persistenceEvent.getEntity().getId());

        if (persistenceEvent.getCause() != PersistenceEvent.Cause.DELETE) {
            if (!persistenceEvent.getEntity().isOptimisationDisabled().orElse(false)) {
                startOptimisation(persistenceEvent.getEntity());
            }
        }
    }

    protected void processAttributeEvent(AttributeEvent attributeEvent) {
        OptimisationInstance optimisationInstance = assetOptimisationInstanceMap.get(attributeEvent.getId());

        if (optimisationInstance != null) {
            processOptimisationAssetAttributeEvent(optimisationInstance, attributeEvent);
            return;
        }

        String attributeName = attributeEvent.getName();

        if ((attributeName.equals(ElectricityChargerAsset.VEHICLE_CONNECTED.getName()) || attributeName.equals(ElectricVehicleAsset.CHARGER_CONNECTED.getName()))
            && (Boolean)attributeEvent.getValue().orElse(false)) {
            // Look for forced charge asset
            if (forceChargeAssetIds.remove(attributeEvent.getId())) {
                LOG.fine("Previously force charged asset has now been disconnected so clearing force charge flag: " + attributeEvent.getId());
            }
            return;
        }

        // Check for request to force charge
        if (attributeName.equals(ElectricityStorageAsset.FORCE_CHARGE.getName())) {
            Asset<?> asset = assetStorageService.find(attributeEvent.getId());
            if (!(asset instanceof ElectricityStorageAsset)) {
                LOG.fine("Request to force charge asset will be ignored as asset not found or is not of type '" + ElectricityStorageAsset.class.getSimpleName() + "': " + attributeEvent.getId());
                return;
            }

            ElectricityStorageAsset storageAsset = (ElectricityStorageAsset) asset;

            if (attributeEvent.getValue().orElse(null) == AttributeExecuteStatus.REQUEST_START) {

                double powerImportMax = storageAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
                double maxEnergyLevel = getElectricityStorageAssetEnergyLevelMax(storageAsset);
                double currentEnergyLevel = storageAsset.getEnergyLevel().orElse(0d);
                LOG.fine("Request to force charge asset '" + attributeEvent.getId() + "': attempting to set powerSetpoint=" + powerImportMax);

                if (forceChargeAssetIds.contains(attributeEvent.getId())) {
                    LOG.fine("Request to force charge asset will be ignored as force charge already requested for asset: " + storageAsset);
                    return;
                }

                if (currentEnergyLevel >= maxEnergyLevel) {
                    LOG.fine("Request to force charge asset will be ignored as asset is already at or above maxEnergyLevel: " + storageAsset);
                    return;
                }

                forceChargeAssetIds.add(attributeEvent.getId());
                assetProcessingService.sendAttributeEvent(new AttributeEvent(storageAsset.getId(), ElectricityAsset.POWER_SETPOINT, powerImportMax), getClass().getSimpleName());
                assetProcessingService.sendAttributeEvent(new AttributeEvent(storageAsset.getId(), ElectricityStorageAsset.FORCE_CHARGE, AttributeExecuteStatus.RUNNING), getClass().getSimpleName());

            } else if (attributeEvent.<AttributeExecuteStatus>getValue().orElse(null) == AttributeExecuteStatus.REQUEST_CANCEL) {

                if (forceChargeAssetIds.remove(attributeEvent.getId())) {
                    LOG.info("Request to cancel force charge asset: " + storageAsset.getId());
                    assetProcessingService.sendAttributeEvent(new AttributeEvent(storageAsset.getId(), ElectricityAsset.POWER_SETPOINT, 0d), getClass().getSimpleName());
                    assetProcessingService.sendAttributeEvent(new AttributeEvent(storageAsset.getId(), ElectricityStorageAsset.FORCE_CHARGE, AttributeExecuteStatus.CANCELLED), getClass().getSimpleName());
                }
            }
        }
    }

    protected double getElectricityStorageAssetEnergyLevelMax(ElectricityStorageAsset asset) {
        double energyCapacity = asset.getEnergyCapacity().orElse(0d);
        int maxEnergyLevelPercentage = asset.getEnergyLevelPercentageMax().orElse(100);
        return energyCapacity * ((1d*maxEnergyLevelPercentage)/100d);
    }

    protected synchronized void processOptimisationAssetAttributeEvent(OptimisationInstance optimisationInstance, AttributeEvent attributeEvent) {

        if (EnergyOptimisationAsset.FINANCIAL_SAVING.getName().equals(attributeEvent.getName())
            || EnergyOptimisationAsset.CARBON_SAVING.getName().equals(attributeEvent.getName())) {
            // These are updated by this service
            return;
        }


        if (attributeEvent.getName().equals(EnergyOptimisationAsset.OPTIMISATION_DISABLED.getName())) {
            boolean disabled = (Boolean)attributeEvent.getValue().orElse(false);
            if (!disabled && assetOptimisationInstanceMap.containsKey(optimisationInstance.optimisationAsset.getId())) {
                // Nothing to do here
                return;
            } else if (disabled && !assetOptimisationInstanceMap.containsKey(optimisationInstance.optimisationAsset.getId())) {
                // Nothing to do here
                return;
            }
        }

        LOG.info("Processing optimisation asset attribute event: " + attributeEvent);
        stopOptimisation(attributeEvent.getId());

        // Get latest asset from storage
        EnergyOptimisationAsset asset = (EnergyOptimisationAsset) assetStorageService.find(attributeEvent.getId());

        if (asset != null && !asset.isOptimisationDisabled().orElse(false)) {
            startOptimisation(asset);
        }
    }

    protected synchronized void startOptimisation(EnergyOptimisationAsset optimisationAsset) {
        LOG.fine("Initialising optimiser for optimisation asset: " + optimisationAsset);
        double intervalSize = optimisationAsset.getIntervalSize().orElse(0.25d);
        int financialWeighting = optimisationAsset.getFinancialWeighting().orElse(100);

        try {
            EnergyOptimiser optimiser = new EnergyOptimiser(intervalSize, ((double) financialWeighting) / 100);

            long periodSeconds = (long) (optimiser.intervalSize * 60 * 60);

            if (periodSeconds < 300) {
                throw new IllegalStateException("Optimiser interval size is too small (minimum is 5 mins) for asset: " + optimisationAsset.getId());
            }

            long currentMillis = timerService.getCurrentTimeMillis();
            Instant optimisationStartTime = getOptimisationStartTime(currentMillis, periodSeconds);

            // Schedule subsequent runs
            long offsetSeconds = (long) (Math.random() * 30) + periodSeconds;
            Duration startDuration = Duration.between(Instant.ofEpochMilli(currentMillis), optimisationStartTime.plus(offsetSeconds, ChronoUnit.SECONDS));

            ScheduledFuture<?> optimisationFuture = scheduleOptimisation(optimisationAsset.getId(), optimiser, startDuration, periodSeconds);
            assetOptimisationInstanceMap.put(optimisationAsset.getId(), new OptimisationInstance(optimisationAsset, optimiser, optimisationFuture));

            // Execute first optimisation at the period that started previous to now
            LOG.finest(getLogPrefix(optimisationAsset.getId()) + "Running first optimisation for time '" + formatter.format(optimisationStartTime));

            executorService.execute(() -> {
                try {
                    runOptimisation(optimisationAsset.getId(), optimisationStartTime);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to run energy optimiser for asset: " + optimisationAsset.getId(), e);
                }
            });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to start energy optimiser for asset: " + optimisationAsset, e);
        }
    }

    protected synchronized void stopOptimisation(String optimisationAssetId) {
        OptimisationInstance optimisationInstance = assetOptimisationInstanceMap.remove(optimisationAssetId);

        if (optimisationInstance == null || optimisationInstance.optimiserFuture == null) {
            return;
        }

        LOG.fine("Removing optimiser for optimisation asset: " + optimisationAssetId);
        optimisationInstance.optimiserFuture.cancel(false);
    }


    /**
     * Schedules execution of the optimiser at the start of the interval window with up to 30s of offset randomness
     * added so that multiple optimisers don't all run at exactly the same instance; the interval execution times are
     * calculated relative to the hour. e.g. a 0.25h intervalSize (15min) would execute at NN:00+offset, NN:15+offset,
     * NN:30+offset, NN:45+offset...It is important that intervals coincide with any change in supplier tariff so that
     * the optimisation works effectively.
     */
    protected ScheduledFuture<?> scheduleOptimisation(String optimisationAssetId, EnergyOptimiser optimiser, Duration startDuration, long periodSeconds) throws IllegalStateException {

        if (optimiser == null) {
            throw new IllegalStateException("Optimiser instance not found for asset: " + optimisationAssetId);
        }

        return executorService.scheduleAtFixedRate(() -> {
                try {
                    runOptimisation(optimisationAssetId, Instant.ofEpochMilli(timerService.getCurrentTimeMillis()).truncatedTo(ChronoUnit.MINUTES));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to run energy optimiser for asset: " + optimisationAssetId, e);
                }
            },
            startDuration.getSeconds(),
            periodSeconds,
            TimeUnit.SECONDS);
    }

    /**
     * Gets the start time of the interval that the currentMillis value is within
     */
    protected static Instant getOptimisationStartTime(long currentMillis, long periodSeconds) {
        Instant now = Instant.ofEpochMilli(currentMillis);

        Instant optimisationStartTime = now
            .truncatedTo(ChronoUnit.DAYS);

        while (optimisationStartTime.isBefore(now)) {
            optimisationStartTime = optimisationStartTime.plus(periodSeconds, ChronoUnit.SECONDS);
        }

        // Move to one period before
        return optimisationStartTime.minus(periodSeconds, ChronoUnit.SECONDS);
    }

    protected String getLogPrefix(String optimisationAssetId) {
        return "Optimisation '" + optimisationAssetId + "': ";
    }

    protected void checkTimeoutAndThrow(String optimisationAssetId, long startTimeMillis) throws TimeoutException {
        long runtime = timerService.getCurrentTimeMillis() - startTimeMillis;
        if (runtime > OPTIMISATION_TIMEOUT_MILLIS) {
            String logMsg = getLogPrefix(optimisationAssetId) + "Optimisation has been running for " + runtime + "ms, timeout is at " + OPTIMISATION_TIMEOUT_MILLIS + "ms";
            LOG.warning(logMsg);
            throw new TimeoutException(logMsg);
        }
    }

    /**
     * Runs the optimisation routine for the specified time; it is important that this method does not throw an
     * exception as it will cancel the scheduled task thus stopping future optimisations.
     */
    protected void runOptimisation(String optimisationAssetId, Instant optimisationTime) throws Exception {
        OptimisationInstance optimisationInstance = assetOptimisationInstanceMap.get(optimisationAssetId);

        if (optimisationInstance == null) {
            return;
        }

        LOG.finest(getLogPrefix(optimisationAssetId) + "Running for time '" + formatter.format(optimisationTime));

        long startTimeMillis = timerService.getCurrentTimeMillis();
        EnergyOptimiser optimiser = optimisationInstance.energyOptimiser;
        int intervalCount = optimiser.get24HourIntervalCount();
        double intervalSize = optimiser.getIntervalSize();

        LOG.finest(getLogPrefix(optimisationAssetId) + "Fetching child assets of type '" + ElectricitySupplierAsset.class.getSimpleName() + "'");

        List<ElectricitySupplierAsset> supplierAssets = assetStorageService.findAll(
                new AssetQuery()
                    .types(ElectricitySupplierAsset.class)
                    .recursive(true)
                    .parents(optimisationAssetId)
            ).stream()
            .filter(asset -> asset.hasAttribute(ElectricitySupplierAsset.TARIFF_IMPORT))
            .map(asset -> (ElectricitySupplierAsset) asset).toList();

        if (supplierAssets.size() != 1) {
            LOG.warning(getLogPrefix(optimisationAssetId) + "Expected exactly one " + ElectricitySupplierAsset.class.getSimpleName() + " asset with a '" + ElectricitySupplierAsset.TARIFF_IMPORT.getName() + "' attribute but found: " + supplierAssets.size());
            return;
        }

        double[] powerNets = new double[intervalCount];
        ElectricitySupplierAsset supplierAsset = supplierAssets.get(0);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Found child asset of type '" + ElectricitySupplierAsset.class.getSimpleName() + "': " + supplierAsset);
        }

        // Do some basic validation
        if (supplierAsset.getTariffImport().isPresent()) {
            LOG.warning(getLogPrefix(optimisationAssetId) + ElectricitySupplierAsset.class.getSimpleName() + " asset '" + ElectricitySupplierAsset.TARIFF_IMPORT.getName() + "' attribute has no value");
        }

        LOG.finest(getLogPrefix(optimisationAssetId) + "Fetching optimisable child assets of type '" + ElectricityStorageAsset.class.getSimpleName() + "'");

        List<ElectricityStorageAsset> optimisableStorageAssets = assetStorageService.findAll(
            new AssetQuery()
                .recursive(true)
                .parents(optimisationAssetId)
                .types(ElectricityStorageAsset.class)
                .attributes(
                    new LogicGroup<>(
                        LogicGroup.Operator.AND,
                        Collections.singletonList(new LogicGroup<>(
                            LogicGroup.Operator.OR,
                            new AttributePredicate(ElectricityStorageAsset.SUPPORTS_IMPORT.getName(), new BooleanPredicate(true)),
                            new AttributePredicate(ElectricityStorageAsset.SUPPORTS_EXPORT.getName(), new BooleanPredicate(true))
                        )),
                        new AttributePredicate().name(new StringPredicate(ElectricityAsset.POWER_SETPOINT.getName())))
                )
        )
            .stream()
            .map(asset -> (ElectricityStorageAsset)asset)
            .collect(Collectors.toList());

        checkTimeoutAndThrow(optimisationAssetId, startTimeMillis);

        List<ElectricityStorageAsset> finalOptimisableStorageAssets = optimisableStorageAssets;
        optimisableStorageAssets = optimisableStorageAssets
            .stream()
            .filter(asset -> {

                // Exclude force charged assets (so we don't mess with the setpoint)
                if (forceChargeAssetIds.contains(asset.getId())) {
                    LOG.finest("Optimisable asset was requested to force charge so it won't be optimised: " + asset.getId());
                    @SuppressWarnings("OptionalGetWithoutIsPresent")
                    Attribute<Double> powerAttribute = asset.getAttribute(ElectricityAsset.POWER).get();
                    double[] powerLevels = get24HAttributeValues(asset.getId(), powerAttribute, optimiser.getIntervalSize(), intervalCount, optimisationTime);
                    IntStream.range(0, intervalCount).forEach(i -> powerNets[i] += powerLevels[i]);

                    double currentEnergyLevel = asset.getEnergyLevel().orElse(0d);
                    double maxEnergyLevel = getElectricityStorageAssetEnergyLevelMax(asset);
                    if (currentEnergyLevel >= maxEnergyLevel) {
                        LOG.info("Force charged asset has reached maxEnergyLevelPercentage so stopping charging: " + asset.getId());
                        forceChargeAssetIds.remove(asset.getId());
                        assetProcessingService.sendAttributeEvent(
                            new AttributeEvent(asset.getId(), ElectricityStorageAsset.POWER_SETPOINT, 0d),
                            getClass().getSimpleName());
                        assetProcessingService.sendAttributeEvent(new AttributeEvent(asset.getId(), ElectricityStorageAsset.FORCE_CHARGE, AttributeExecuteStatus.COMPLETED), getClass().getSimpleName());
                    }
                    return false;
                }

                if (asset instanceof ElectricityChargerAsset) {
                    // Check if it has a child vehicle asset
                    return finalOptimisableStorageAssets.stream()
                        .noneMatch(a -> {
                                if (a instanceof ElectricVehicleAsset && a.getParentId().equals(asset.getId())) {
                                // Take the lowest power max from vehicle or charger
                                double vehiclePowerImportMax = a.getPowerImportMax().orElse(Double.MAX_VALUE);
                                double vehiclePowerExportMax = a.getPowerExportMax().orElse(Double.MAX_VALUE);
                                double chargerPowerImportMax = asset.getPowerImportMax().orElse(Double.MAX_VALUE);
                                double chargerPowerExportMax = asset.getPowerExportMax().orElse(Double.MAX_VALUE);
                                double smallestPowerImportMax = Math.min(vehiclePowerImportMax, chargerPowerImportMax);
                                double smallestPowerExportMax = Math.min(vehiclePowerExportMax, chargerPowerExportMax);

                                if (smallestPowerImportMax < vehiclePowerImportMax) {
                                    LOG.fine("Reducing vehicle power import max due to connected charger limit: vehicle=" + a.getId() + ", oldPowerImportMax=" + vehiclePowerImportMax + ", newPowerImportMax=" + smallestPowerImportMax);
                                    a.setPowerImportMax(smallestPowerImportMax);
                                }
                                if (smallestPowerExportMax < vehiclePowerExportMax) {
                                    LOG.fine("Reducing vehicle power Export max due to connected charger limit: vehicle=" + a.getId() + ", oldPowerExportMax=" + vehiclePowerExportMax + ", newPowerExportMax=" + smallestPowerExportMax);
                                    a.setPowerExportMax(smallestPowerExportMax);
                                }
                                LOG.finest("Excluding charger from optimisable assets and child vehicle will be used instead: " + asset.getId());
                                return true;
                            }
                            return false;
                        });
                }
                return true;
            })
            .sorted(Comparator.comparingInt(asset -> asset.getEnergyLevelSchedule().map(schedule -> 0).orElse(1)))
            .collect(Collectors.toList());

        checkTimeoutAndThrow(optimisationAssetId, startTimeMillis);

        if (optimisableStorageAssets.isEmpty()) {
            LOG.warning(getLogPrefix(optimisationAssetId) + "Expected at least one optimisable '" + ElectricityStorageAsset.class.getSimpleName() + " asset with a '" + ElectricityAsset.POWER_SETPOINT.getName() + "' attribute but found none");
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Found optimisable child assets of type '" + ElectricityStorageAsset.class.getSimpleName() + "': " + optimisableStorageAssets.stream().map(Asset::getId).collect(Collectors.joining(", ")));
        }

        LOG.finest(getLogPrefix(optimisationAssetId) + "Fetching plain consumer and producer child assets of type '" + ElectricityProducerAsset.class.getSimpleName() + "', '" + ElectricityConsumerAsset.class.getSimpleName() + "', '" + ElectricityStorageAsset.class.getSimpleName() + "'");

        AtomicInteger count = new AtomicInteger(0);
        assetStorageService.findAll(
            new AssetQuery()
                .recursive(true)
                .parents(optimisationAssetId)
                .types(ElectricityConsumerAsset.class, ElectricityProducerAsset.class)
                .attributes(new AttributePredicate().name(new StringPredicate(ElectricityAsset.POWER.getName())))
        )
            //.stream()
            //.filter(asset -> !(asset instanceof GroupAsset) || isElectricityGroupAsset(asset))
            .forEach(asset -> {
                @SuppressWarnings("OptionalGetWithoutIsPresent")
                Attribute<Double> powerAttribute = asset.getAttribute(ElectricityAsset.POWER).get();
                double[] powerLevels = get24HAttributeValues(asset.getId(), powerAttribute, optimiser.getIntervalSize(), intervalCount, optimisationTime);
                IntStream.range(0, intervalCount).forEach(i -> powerNets[i] += powerLevels[i]);
                count.incrementAndGet();
            });

        checkTimeoutAndThrow(optimisationAssetId, startTimeMillis);

        // Get power of storage assets that don't support neither import or export (treat them as plain consumers/producers)
        List<ElectricityStorageAsset> plainStorageAssets = assetStorageService.findAll(
                new AssetQuery()
                    .recursive(true)
                    .parents(optimisationAssetId)
                    .types(ElectricityStorageAsset.class)
                    .attributes(
                        new AttributePredicate().name(new StringPredicate(ElectricityAsset.POWER.getName())),
                        new AttributePredicate(ElectricityStorageAsset.SUPPORTS_IMPORT.getName(), new BooleanPredicate(true), true, null),
                        new AttributePredicate(ElectricityStorageAsset.SUPPORTS_EXPORT.getName(), new BooleanPredicate(true), true, null)
                    )
            )
            .stream()
            .map(asset -> (ElectricityStorageAsset) asset).toList();

        checkTimeoutAndThrow(optimisationAssetId, startTimeMillis);

        // Exclude chargers with a power value != 0 and a child vehicle with a power value != 0 (avoid double counting - vehicle takes priority)
        plainStorageAssets
            .stream()
            .filter(asset -> {
                if (asset instanceof ElectricityChargerAsset) {
                    // Check if it has a child vehicle asset also check optimisable assets as child vehicle could be in there
                    return plainStorageAssets.stream()
                        .noneMatch(a -> {
                            if (a instanceof ElectricVehicleAsset && a.getParentId().equals(asset.getId())) {
                                LOG.finest("Excluding charger from plain consumer/producer calculations to avoid double counting power: " + asset.getId());
                                return true;
                            }
                            return false;
                        }) && finalOptimisableStorageAssets.stream()
                        .noneMatch(a -> {
                            if (a instanceof ElectricVehicleAsset && a.getParentId().equals(asset.getId())) {
                                LOG.finest("Excluding charger from plain consumer/producer calculations to avoid double counting power: " + asset.getId());
                                return true;
                            }
                            return false;
                        });
                }
                return true;
            })
            .forEach(asset -> {
                @SuppressWarnings("OptionalGetWithoutIsPresent")
                Attribute<Double> powerAttribute = asset.getAttribute(ElectricityAsset.POWER).get();
                double[] powerLevels = get24HAttributeValues(asset.getId(), powerAttribute, optimiser.getIntervalSize(), intervalCount, optimisationTime);
                IntStream.range(0, intervalCount).forEach(i -> powerNets[i] += powerLevels[i]);
                count.incrementAndGet();
            });

        checkTimeoutAndThrow(optimisationAssetId, startTimeMillis);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Found plain consumer and producer child assets count=" + count.get());
            LOG.finest("Calculated net power of consumers and producers: " + Arrays.toString(powerNets));
        }

        // Get supplier costs for each interval
        double financialWeightingImport = optimiser.getFinancialWeighting();
        double financialWeightingExport = optimiser.getFinancialWeighting();

        if (financialWeightingImport < 1d && !supplierAsset.getCarbonImport().isPresent()) {
            financialWeightingImport = 1d;
        }

        if (financialWeightingExport < 1d && !supplierAsset.getCarbonExport().isPresent()) {
            financialWeightingExport = 1d;
        }

        double[] costsImport = get24HAttributeValues(supplierAsset.getId(), supplierAsset.getAttribute(ElectricitySupplierAsset.TARIFF_IMPORT).orElse(null), optimiser.getIntervalSize(), intervalCount, optimisationTime);
        double[] costsExport = get24HAttributeValues(supplierAsset.getId(), supplierAsset.getAttribute(ElectricitySupplierAsset.TARIFF_EXPORT).orElse(null), optimiser.getIntervalSize(), intervalCount, optimisationTime);

        if (financialWeightingImport < 1d || financialWeightingExport < 1d) {
            double[] carbonImport = get24HAttributeValues(supplierAsset.getId(), supplierAsset.getAttribute(ElectricitySupplierAsset.CARBON_IMPORT).orElse(null), optimiser.getIntervalSize(), intervalCount, optimisationTime);
            double[] carbonExport = get24HAttributeValues(supplierAsset.getId(), supplierAsset.getAttribute(ElectricitySupplierAsset.CARBON_EXPORT).orElse(null), optimiser.getIntervalSize(), intervalCount, optimisationTime);

            LOG.finest(getLogPrefix(optimisationAssetId) + "Adjusting costs to include some carbon weighting, financialWeightingImport=" + financialWeightingImport + ", financialWeightingExport=" + financialWeightingExport);

            for (int i = 0; i < costsImport.length; i++) {
                costsImport[i] = (financialWeightingImport * costsImport[i]) + ((1-financialWeightingImport) * carbonImport[i]);
                costsExport[i] = (financialWeightingExport * costsExport[i]) + ((1-financialWeightingExport) * carbonExport[i]);
            }
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Import costs: " + Arrays.toString(costsImport));
            LOG.finest(getLogPrefix(optimisationAssetId) + "Export costs: " + Arrays.toString(costsExport));
        }

        // Savings variables
        List<String> obsoleteUnoptimisedAssetIds = new ArrayList<>(optimisationInstance.unoptimisedStorageAssetEnergyLevels.keySet());
        double unoptimisedPower = powerNets[0];
        double financialCost = 0d;
        double carbonCost = 0d;
        double unoptimisedFinancialCost = 0d;
        double unoptimisedCarbonCost = 0d;

        // Optimise storage assets with priority on storage assets with an energy schedule (already sorted above)
        double importPowerMax = supplierAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
        double exportPowerMax = -1 * supplierAsset.getPowerExportMax().orElse(Double.MAX_VALUE);
        double[] importPowerMaxes = new double[intervalCount];
        double[] exportPowerMaxes = new double[intervalCount];
        Arrays.fill(importPowerMaxes, importPowerMax);
        Arrays.fill(exportPowerMaxes, exportPowerMax);
        long periodSeconds = (long)(optimiser.getIntervalSize()*60*60);

        for (ElectricityStorageAsset storageAsset : optimisableStorageAssets) {
            boolean hasSetpoint = storageAsset.hasAttribute(ElectricityStorageAsset.POWER_SETPOINT);
            boolean supportsExport = storageAsset.isSupportsExport().orElse(false);
            boolean supportsImport = storageAsset.isSupportsImport().orElse(false);

            checkTimeoutAndThrow(optimisationAssetId, startTimeMillis);

            LOG.finest(getLogPrefix(optimisationAssetId) + "Optimising power set points for storage asset: " + storageAsset);

            if (!supportsExport && !supportsImport) {
                LOG.finest(getLogPrefix(optimisationAssetId) + "Storage asset doesn't support import or export: " + storageAsset.getId());
                continue;
            }

            if (!hasSetpoint) {
                LOG.info(getLogPrefix(optimisationAssetId) + "Storage asset has no '" + ElectricityStorageAsset.POWER_SETPOINT.getName() + "' attribute so cannot be controlled: " + storageAsset.getId());
                continue;
            }

            double energyCapacity = storageAsset.getEnergyCapacity().orElse(0d);
            double energyLevel = Math.min(energyCapacity, storageAsset.getEnergyLevel().orElse(-1d));

            if (energyCapacity <= 0d || energyLevel < 0) {
                LOG.info(getLogPrefix(optimisationAssetId) + "Storage asset has no capacity or energy level so cannot import or export energy: " + storageAsset.getId());
                continue;
            }

            double energyLevelMax = Math.min(energyCapacity, ((double) storageAsset.getEnergyLevelPercentageMax().orElse(100) / 100) * energyCapacity);
            double energyLevelMin = Math.min(energyCapacity, ((double) storageAsset.getEnergyLevelPercentageMin().orElse(0) / 100) * energyCapacity);
            double[] energyLevelMins = new double[intervalCount];
            double[] energyLevelMaxs = new double[intervalCount];
            Arrays.fill(energyLevelMins, energyLevelMin);
            Arrays.fill(energyLevelMaxs, energyLevelMax);

            // Does the storage support import and have an energy level schedule
            Optional<Integer[][]> energyLevelScheduleOptional = storageAsset.getEnergyLevelSchedule();
            boolean hasEnergyMinRequirement = energyLevelMin > 0 || energyLevelScheduleOptional.isPresent();
            double powerExportMax = storageAsset.getPowerExportMax().map(power -> -1*power).orElse(Double.MIN_VALUE);
            double powerImportMax = storageAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
            int[][] energySchedule = energyLevelScheduleOptional.map(dayArr -> Arrays.stream(dayArr).map(hourArr -> Arrays.stream(hourArr).mapToInt(i -> i != null ? i : 0).toArray()).toArray(int[][]::new)).orElse(null);

            if (energySchedule != null) {
                LOG.finest(getLogPrefix(optimisationAssetId) + "Applying energy schedule for storage asset: " + storageAsset.getId());
                optimiser.applyEnergySchedule(energyLevelMins, energyLevelMaxs, energyCapacity, energySchedule, LocalDateTime.ofInstant(Instant.ofEpochMilli(timerService.getCurrentTimeMillis()), ZoneId.systemDefault()));
            }

            double maxEnergyLevelMin = Arrays.stream(energyLevelMins).max().orElse(0);
            boolean isConnected = storageAssetConnected(storageAsset);

            // TODO: Make these a function of energy level
            Function<Integer, Double> powerImportMaxCalculator = interval -> interval == 0 && !isConnected ? 0 : powerImportMax;
            Function<Integer, Double> powerExportMaxCalculator = interval -> interval == 0 && !isConnected ? 0 : powerExportMax;

            if (hasEnergyMinRequirement) {
                LOG.finest(getLogPrefix(optimisationAssetId) + "Normalising min energy requirements for storage asset: " + storageAsset.getId());
                optimiser.normaliseEnergyMinRequirements(energyLevelMins, powerImportMaxCalculator, powerExportMaxCalculator, energyLevel);
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(getLogPrefix(optimisationAssetId) + "Min energy requirements for storage asset '" + storageAsset.getId() + "': " + Arrays.toString(energyLevelMins));
                }
            }

            // Calculate the power setpoints for this asset and update power net values for each interval
            double[] setpoints = getStoragePowerSetpoints(optimisationInstance, storageAsset, energyLevelMins, energyLevelMaxs, powerNets, importPowerMaxes, exportPowerMaxes, costsImport, costsExport);

            if (setpoints != null) {

                // Assume these setpoints will be applied so update the power net values with these
                for (int i = 0; i < powerNets.length; i++) {

                    if (i == 0) {
                        if (!storageAssetConnected(storageAsset)) {
                            LOG.finest("Optimised storage asset not connected so interval 0 will not be counted or actioned: " + storageAsset.getId());
                            setpoints[i] = 0;
                            continue;
                        }

                        // Update savings/cost data with costs specific to this asset
                        if (setpoints[i] > 0) {
                            financialCost += storageAsset.getTariffImport().orElse(0d) * setpoints[i] * intervalSize;
                        } else {
                            financialCost += storageAsset.getTariffExport().orElse(0d) * -1 * setpoints[i] * intervalSize;
                        }
                    }

                    powerNets[i] += setpoints[i];
                }

                // Push the setpoints into the prediction service for the storage asset's setpoint attribute and set current setpoint
                List<ValueDatapoint<?>> valuesAndTimestamps = IntStream.range(1, setpoints.length).mapToObj(i ->
                    new ValueDatapoint<>(optimisationTime.plus(periodSeconds * i, ChronoUnit.SECONDS).toEpochMilli(), setpoints[i])
                ).collect(Collectors.toList());

                assetPredictedDatapointService.updateValues(storageAsset.getId(), ElectricityAsset.POWER_SETPOINT.getName(), valuesAndTimestamps);
            }

            assetProcessingService.sendAttributeEvent(new AttributeEvent(storageAsset.getId(), ElectricityAsset.POWER_SETPOINT, setpoints != null ? setpoints[0] : null), getClass().getSimpleName());

            // Update unoptimised power for this asset
            obsoleteUnoptimisedAssetIds.remove(storageAsset.getId());
            double assetUnoptimisedPower = getStorageUnoptimisedImportPower(optimisationInstance, optimisationAssetId, storageAsset, maxEnergyLevelMin, Math.max(0, powerImportMax - unoptimisedPower));
            unoptimisedPower += assetUnoptimisedPower;
            unoptimisedFinancialCost += storageAsset.getTariffImport().orElse(0d) * assetUnoptimisedPower * intervalSize;
        }

        // Clear out un-optimised data for not found assets
        obsoleteUnoptimisedAssetIds.forEach(optimisationInstance.unoptimisedStorageAssetEnergyLevels.keySet()::remove);

        // Calculate and store savings data
        carbonCost = (powerNets[0] >= 0 ? supplierAsset.getCarbonImport().orElse(0d) : -1 * supplierAsset.getCarbonExport().orElse(0d)) * powerNets[0] * intervalSize;
        financialCost += (powerNets[0] >= 0 ? supplierAsset.getTariffImport().orElse(0d) : -1 * supplierAsset.getTariffExport().orElse(0d)) * powerNets[0] * intervalSize;
        unoptimisedCarbonCost = (unoptimisedPower >= 0 ? supplierAsset.getCarbonImport().orElse(0d) : -1 * supplierAsset.getCarbonExport().orElse(0d)) * unoptimisedPower * intervalSize;
        unoptimisedFinancialCost += (unoptimisedPower >= 0 ? supplierAsset.getTariffImport().orElse(0d) : -1 * supplierAsset.getTariffExport().orElse(0d)) * unoptimisedPower * intervalSize;

        double financialSaving = unoptimisedFinancialCost - financialCost;
        double carbonSaving = unoptimisedCarbonCost - carbonCost;

        LOG.info(getLogPrefix(optimisationAssetId) + "Current interval financial saving = " + financialSaving);
        LOG.info(getLogPrefix(optimisationAssetId) + "Current interval carbon saving = " + carbonSaving);

        financialSaving += optimisationInstance.optimisationAsset.getFinancialSaving().orElse(0d);
        carbonSaving += optimisationInstance.optimisationAsset.getCarbonSaving().orElse(0d);

        // Update in memory asset
        optimisationInstance.optimisationAsset.setFinancialSaving(financialSaving);
        optimisationInstance.optimisationAsset.setCarbonSaving(carbonSaving);

        // Push new values into the DB
        assetProcessingService.sendAttributeEvent(new AttributeEvent(optimisationAssetId, EnergyOptimisationAsset.FINANCIAL_SAVING, financialSaving), getClass().getSimpleName());
        assetProcessingService.sendAttributeEvent(new AttributeEvent(optimisationAssetId, EnergyOptimisationAsset.CARBON_SAVING, carbonSaving), getClass().getSimpleName());
    }

    protected boolean isElectricityGroupAsset(Asset<?> asset) {
        if (!(asset instanceof GroupAsset)) {
            return false;
        }

        Class<?> assetClass = ValueUtil
            .getAssetDescriptor(((GroupAsset)asset).getChildAssetType().orElse(null))
            .map(AssetDescriptor::getType)
            .orElse(null);

        return assetClass != null &&
            ElectricityAsset.class.isAssignableFrom(assetClass);
    }

    protected double[] get24HAttributeValues(String assetId, Attribute<Double> attribute, double intervalSize, int intervalCount, Instant optimisationTime) {

        double[] values = new double[intervalCount];

        if (attribute == null) {
            return values;
        }

        AttributeRef ref = new AttributeRef(assetId, attribute.getName());

        if (attribute.hasMeta(MetaItemType.HAS_PREDICTED_DATA_POINTS)) {
            LocalDateTime timestamp = LocalDateTime.ofInstant(optimisationTime, ZoneId.systemDefault());
            List<ValueDatapoint<?>> predictedData = assetPredictedDatapointService.queryDatapoints(
                    ref.getId(),
                    ref.getName(),
                    new AssetDatapointIntervalQuery(
                            timestamp,
                            timestamp.plus(24, HOURS).minus((long)(intervalSize * 60), ChronoUnit.MINUTES),
                            (intervalSize * 60) + " minutes",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
            );
            if (predictedData.size() != values.length) {
                LOG.warning("Returned predicted data point count does not match interval count: Ref=" + ref + ", expected=" + values.length + ", actual=" + predictedData.size());
            } else {

                IntStream.range(0, predictedData.size()).forEach(i -> {
                    if (predictedData.get(i).getValue() != null) {
                        values[i] = (double) (Object) predictedData.get(i).getValue();
                    } else {
                        // Average previous and next values to fill in gaps (goes up to 5 back and forward) - this fixes
                        // issues with resolution differences between stored predicted data and optimisation interval
                        Double previous = null;
                        Double next = null;
                        int j = i-1;
                        while (previous == null && j >= 0) {
                            previous = (Double) predictedData.get(j).getValue();
                            j--;
                        }
                        j = i+1;
                        while (next == null && j < predictedData.size()) {
                            next = (Double) predictedData.get(j).getValue();
                            j++;
                        }
                        if (next == null) {
                            next = previous;
                        }
                        if (previous == null) {
                            previous = next;
                        }
                        if (next != null) {
                            values[i] = (previous + next) / 2;
                        }
                    }
                });
            }
        }

        values[0] = attribute.getValue().orElse(0d);
        return values;
    }

    /**
     * Returns the power setpoint calculator for the specified asset (for producers power demand will only ever be
     * negative, for consumers it will only ever be positive and for storage assets that support export (i.e. supports
     * producer and consumer) it can be positive or negative at a given interval. For this to work the supplied
     * parameters should be updated when the system changes and not replaced so that references maintained by the
     * calculator are valid and up to date.
     */
    protected double[] getStoragePowerSetpoints(
        OptimisationInstance optimisationInstance,
        ElectricityStorageAsset storageAsset,
        double[] normalisedEnergyLevelMins,
        double[] energyLevelMaxs,
        double[] powerNets,
        double[] importPowerLimits,
        double[] exportPowerLimits,
        double[] costImports,
        double[] costExports) {

        EnergyOptimiser optimiser = optimisationInstance.energyOptimiser;
        String optimisationAssetId = optimisationInstance.optimisationAsset.getId();
        int intervalCount = optimiser.get24HourIntervalCount();
        boolean supportsExport = storageAsset.isSupportsExport().orElse(false);
        boolean supportsImport = storageAsset.isSupportsImport().orElse(false);

        LOG.finest(getLogPrefix(optimisationAssetId) + "Optimising storage asset: " + storageAsset);

        double energyCapacity = storageAsset.getEnergyCapacity().orElse(0d);
        double energyLevel = Math.min(energyCapacity, storageAsset.getEnergyLevel().orElse(-1d));
        double powerExportMax = storageAsset.getPowerExportMax().map(power -> -1*power).orElse(Double.MIN_VALUE);
        double powerImportMax = storageAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
        boolean isConnected = storageAssetConnected(storageAsset);

        // TODO: Make these a function of energy level
        Function<Integer, Double> powerImportMaxCalculator = interval -> interval == 0 && !isConnected ? 0 : powerImportMax;
        Function<Integer, Double> powerExportMaxCalculator = interval -> interval == 0 && !isConnected ? 0 : powerExportMax;

        double[][] exportCostAndPower = null;
        double[][] importCostAndPower = null;
        double[] powerSetpoints = new double[intervalCount];

        Function<Integer, Double> energyLevelCalculator = interval ->
                energyLevel + IntStream.range(0, interval).mapToDouble(j -> powerSetpoints[j] * optimiser.getIntervalSize()).sum();

        // If asset supports exporting energy (V2G, battery storage, etc.) then need to determine if there are
        // opportunities to export energy to save/earn, taking into consideration the cost of exporting from this asset
        if (supportsExport) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Storage asset supports export so calculating export cost and power levels for each interval: " + storageAsset.getId());
            // Find intervals that save/earn by exporting energy from this storage asset by looking at power levels
            BiFunction<Integer, Double, double[]> exportOptimiser = optimiser.getExportOptimiser(powerNets, exportPowerLimits, costImports, costExports, storageAsset.getTariffExport().orElse(0d));
            exportCostAndPower = IntStream.range(0, intervalCount).mapToObj(it -> exportOptimiser.apply(it, powerExportMax))
                .toArray(double[][]::new);
        }

        // If asset supports importing energy then need to determine if there are opportunities to import energy to
        // save/earn, taking into consideration the cost of importing to this asset, also need to ensure that min
        // energy demands are met.
        if (supportsImport) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Storage asset supports import so calculating export cost and power levels for each interval: " + storageAsset.getId());
            BiFunction<Integer, double[], double[]> importOptimiser = optimiser.getImportOptimiser(powerNets, importPowerLimits, costImports, costExports, storageAsset.getTariffImport().orElse(0d));
            importCostAndPower = IntStream.range(0, intervalCount).mapToObj(it -> importOptimiser.apply(it, new double[]{0d, powerImportMax}))
                .toArray(double[][]::new);

            boolean hasEnergyMinRequirement = Arrays.stream(normalisedEnergyLevelMins).anyMatch(el -> el > 0);

            if (hasEnergyMinRequirement) {
                LOG.finest(getLogPrefix(optimisationAssetId) + "Applying imports to achieve min energy level requirements for storage asset: " + storageAsset.getId());
                optimiser.applyEnergyMinImports(importCostAndPower, normalisedEnergyLevelMins, powerSetpoints, energyLevelCalculator, importOptimiser, powerImportMaxCalculator);
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(getLogPrefix(optimisationAssetId) + "Setpoints to achieve min energy level requirements for storage asset '" + storageAsset.getId() + "': " + Arrays.toString(powerSetpoints));
                }
            }
        }

        optimiser.applyEarningOpportunities(importCostAndPower, exportCostAndPower, normalisedEnergyLevelMins, energyLevelMaxs, powerSetpoints, energyLevelCalculator, powerImportMaxCalculator, powerExportMaxCalculator);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Calculated earning opportunity power set points for storage asset '" + storageAsset.getId() + "': " + Arrays.toString(powerSetpoints));
        }

        return powerSetpoints;
    }

    protected boolean storageAssetConnected(ElectricityStorageAsset storageAsset) {
        if (storageAsset instanceof ElectricVehicleAsset) {
            return ((ElectricVehicleAsset)storageAsset).getChargerConnected().orElse(false);
        }
        if (storageAsset instanceof ElectricityChargerAsset) {
            return ((ElectricityChargerAsset)storageAsset).getVehicleConnected().orElse(false);
        }
        return true;
    }

    /**
     * Gets the un-optimised import power for the first (current) interval for the supplied storage asset
     */
    protected double getStorageUnoptimisedImportPower(OptimisationInstance optimisationInstance, String optimisationAssetId, ElectricityStorageAsset storageAsset, double energyLevelTarget, double remainingPowerCapacity) {

        double intervalSize = optimisationInstance.energyOptimiser.getIntervalSize();
        boolean isConnected = storageAssetConnected(storageAsset);

        if (!isConnected) {
            optimisationInstance.unoptimisedStorageAssetEnergyLevels.remove(storageAsset.getId());
            return 0;
        }

        // Get current energy level from map or directly from the asset
        double energyLevel = optimisationInstance.unoptimisedStorageAssetEnergyLevels.get(storageAsset.getId()) != null ? optimisationInstance.unoptimisedStorageAssetEnergyLevels.get(storageAsset.getId()) : storageAsset.getEnergyLevel().orElse(-1d);

        if (energyLevel < 0) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Storage asset has no energy level so cannot calculate un-optimised power demand: " + storageAsset.getId());
            return 0;
        }

        // Calculate power
        double powerImportMax = storageAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
        double remainingEnergy = Math.max(0, energyLevelTarget - energyLevel);
        double toFillPower = remainingEnergy / intervalSize;
        double power = Math.min(Math.min(toFillPower, powerImportMax), remainingPowerCapacity);

        // Update energy level using previous interval power
        energyLevel += power * intervalSize;
        optimisationInstance.unoptimisedStorageAssetEnergyLevels.put(storageAsset.getId(), energyLevel);

        return power;
    }
}
