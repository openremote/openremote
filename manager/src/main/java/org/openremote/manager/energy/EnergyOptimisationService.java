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
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.BooleanPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.util.AssetModelUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.value.MetaItemType;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;

/**
 * Handles optimisation instances for {@link EnergyOptimisationAsset}.
 */
public class EnergyOptimisationService extends RouteBuilder implements ContainerService {

    protected static final Logger LOG = Logger.getLogger(EnergyOptimisationService.class.getName());
    protected DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC));
    protected TimerService timerService;
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;
    protected ScheduledExecutorService executorService;
    protected Map<String, Pair<EnergyOptimiser, ScheduledFuture<?>>> assetEnergyOptimiserMap = new HashMap<>();

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

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

        clientEventService.addInternalSubscription(
            AttributeEvent.class,
            null,
            this::onAssetAttributeEvent
        );
    }

    @Override
    public void start(Container container) throws Exception {
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        // Load all enabled optimisation assets and instantiate an optimiser for each
        LOG.fine("Loading optimisation assets...");

        List<EnergyOptimisationAsset> energyOptimisationAssets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select().excludeParentInfo(true))
                .types(EnergyOptimisationAsset.class)
        )
            .stream()
            .map(asset -> (EnergyOptimisationAsset) asset)
            .filter(optimisationAsset -> !optimisationAsset.isOptimisationDisabled().orElse(false))
            .collect(Collectors.toList());

        LOG.fine("Found enabled optimisation asset count = " + energyOptimisationAssets.size());

        energyOptimisationAssets.forEach(this::startOptimisation);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("EnergyOptimisationAssetPersistenceChanges")
            .filter(isPersistenceEventForEntityType(EnergyOptimisationAsset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> processAssetChange((PersistenceEvent<EnergyOptimisationAsset>) exchange.getIn().getBody(PersistenceEvent.class)));
    }

    @Override
    public void stop(Container container) throws Exception {
        new ArrayList<>(assetEnergyOptimiserMap.keySet())
            .forEach(this::stopOptimisation);
    }

    protected void processAssetChange(PersistenceEvent<EnergyOptimisationAsset> persistenceEvent) {

    }

    protected void onAssetAttributeEvent(AttributeEvent attributeEvent) {

    }


    protected void startOptimisation(EnergyOptimisationAsset optimisationAsset) {
        LOG.fine("Initialising optimiser for optimisation asset: " + optimisationAsset);
        double intervalSize = optimisationAsset.getIntervalSize().orElse(0.25d);
        int financialWeighting = optimisationAsset.getFinancialWeighting().orElse(100);

        try {
            EnergyOptimiser optimiser = new EnergyOptimiser(intervalSize, ((double) financialWeighting) / 100);
            ScheduledFuture<?> runScheduler = scheduleOptimisation(optimisationAsset.getId(), optimiser);
            assetEnergyOptimiserMap.put(optimisationAsset.getId(), new Pair<>(optimiser, runScheduler));
        } catch (IllegalArgumentException e) {
            LOG.log(Level.SEVERE, "Failed to start energy optimiser for asset: " + optimisationAsset, e);
        }
    }

    protected void stopOptimisation(String optimisationAssetId) {
        Pair<EnergyOptimiser, ScheduledFuture<?>> optimiserAndScheduler = assetEnergyOptimiserMap.remove(optimisationAssetId);

        if (optimiserAndScheduler == null || optimiserAndScheduler.value == null) {
            return;
        }

        LOG.fine("Removing optimiser for optimisation asset: " + optimisationAssetId);

        optimiserAndScheduler.value.cancel(false);
    }


    /**
     * Schedules execution of the optimiser at the start of the interval window with up to 30s of offset randomness
     * added so that multiple optimisers don't all run at exactly the same instance; the interval execution times are
     * calculated relative to the hour. e.g. a 0.25h intervalSize (15min) would execute at NN:00+offset, NN:15+offset,
     * NN:30+offset, NN:45+offset...It is important that intervals coincide with any change in supplier tariff so that
     * the optimisation works effectively.
     */
    protected ScheduledFuture<?> scheduleOptimisation(String optimisationAssetId, EnergyOptimiser optimiser) throws IllegalStateException {

        if (optimiser == null) {
            throw new IllegalStateException("Optimiser instance not found for asset: " + optimisationAssetId);
        }

        long periodSeconds = (long) (optimiser.intervalSize * 60 * 60);

        if (periodSeconds < 300) {
            throw new IllegalStateException("Optimiser interval size is too small (minimum is 5 mins) for asset: " + optimisationAssetId);
        }

        long currentMillis = timerService.getCurrentTimeMillis();
        Instant optimisationStartTime = getOptimisationStartTime(currentMillis, periodSeconds);

        // Execute first optimisation at this period to initialise the system
        runOptimisation(optimisationAssetId, optimisationStartTime);

        // Schedule subsequent runs
        long offsetSeconds = (long) (Math.random() * 30);
        Duration startDuration = Duration.between(Instant.ofEpochMilli(currentMillis), optimisationStartTime.plus(offsetSeconds, ChronoUnit.SECONDS));

        return executorService.scheduleAtFixedRate(() ->
            runOptimisation(optimisationAssetId, Instant.ofEpochMilli(timerService.getCurrentTimeMillis()).truncatedTo(ChronoUnit.MINUTES)),
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

    /**
     * Runs the optimisation routine for the specified time; it is important that this method does not throw an
     * exception as it will cancel the scheduled task thus stopping future optimisations.
     */
    protected void runOptimisation(String optimisationAssetId, Instant optimisationTime) {
        Pair<EnergyOptimiser, ScheduledFuture<?>> optimiserAndScheduler = assetEnergyOptimiserMap.get(optimisationAssetId);

        if (optimiserAndScheduler == null) {
            return;
        }

        LOG.finer(getLogPrefix(optimisationAssetId) + "Running for time '" + formatter.format(optimisationTime));

        EnergyOptimiser optimiser = optimiserAndScheduler.key;
        int intervalCount = optimiser.get24HourIntervalCount();

        LOG.finest(getLogPrefix(optimisationAssetId) + "Fetching child assets of type '" + ElectricitySupplierAsset.class.getSimpleName() + "'");

        List<ElectricitySupplierAsset> supplierAssets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select().excludePath(true))
                .types(ElectricitySupplierAsset.class)
                .recursive(true)
                .parents(optimisationAssetId)
        ).stream()
            .filter(asset -> asset.hasAttribute(ElectricitySupplierAsset.TARIFF_IMPORT))
            .map(asset -> (ElectricitySupplierAsset)asset)
            .collect(Collectors.toList());

        if (supplierAssets.size() != 1) {
            LOG.warning(getLogPrefix(optimisationAssetId) + "Expected exactly one " + ElectricitySupplierAsset.class.getSimpleName() + " asset with a '" + ElectricitySupplierAsset.TARIFF_IMPORT.getName() + "' attribute but found: " + supplierAssets.size());
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Found child asset of type '" + ElectricitySupplierAsset.class.getSimpleName() + "': " + supplierAssets.get(0));
        }

        LOG.finest(getLogPrefix(optimisationAssetId) + "Fetching optimisable child assets of type '" + ElectricityStorageAsset.class.getSimpleName() + "'");

        AssetQuery query = new AssetQuery()
            .select(new AssetQuery.Select().excludePath(true))
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
                );

        List<ElectricityStorageAsset> optimisableStorageAssets = assetStorageService.findAll(query).stream()
            .map(asset -> (ElectricityStorageAsset)asset)
            .sorted(Comparator.comparingInt(asset -> asset.getEnergyLevelSchedule().map(schedule -> 0).orElse(1)))
            .collect(Collectors.toList());

        if (optimisableStorageAssets.isEmpty()) {
            LOG.warning(getLogPrefix(optimisationAssetId) + "Expected at least one optimisable '" + ElectricityStorageAsset.class.getSimpleName() + " asset with a '" + ElectricityAsset.POWER_SETPOINT.getName() + "' attribute but found none");
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Found optimisable child assets of type '" + ElectricityStorageAsset.class.getSimpleName() + "': " + optimisableStorageAssets.stream().map(Asset::getId).collect(Collectors.joining(", ")));
        }

        // Get consumers and producers and sum power demand for the next 24hrs
        double[] powerNets = new double[intervalCount];

        LOG.finest(getLogPrefix(optimisationAssetId) + "Fetching plain consumer and producer child assets of type '" + ElectricityProducerAsset.class.getSimpleName() + "', '" + ElectricityConsumerAsset.class.getSimpleName() + "', '" + ElectricityStorageAsset.class.getSimpleName() + "'");

        AtomicInteger count = new AtomicInteger(0);
        assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select().excludePath(true).excludeParentInfo(true))
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
        assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select().excludePath(true).excludeParentInfo(true))
                .recursive(true)
                .parents(optimisationAssetId)
                .types(ElectricityStorageAsset.class)
                .attributes(
                    new AttributePredicate().name(new StringPredicate(ElectricityAsset.POWER.getName())),
                    new AttributePredicate(ElectricityStorageAsset.SUPPORTS_IMPORT.getName(), new BooleanPredicate(false)),
                    new AttributePredicate(ElectricityStorageAsset.SUPPORTS_EXPORT.getName(), new BooleanPredicate(false))
                )
        ).forEach(asset -> {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            Attribute<Double> powerAttribute = asset.getAttribute(ElectricityAsset.POWER).get();
            double[] powerLevels = get24HAttributeValues(asset.getId(), powerAttribute, optimiser.getIntervalSize(), intervalCount, optimisationTime);
            IntStream.range(0, intervalCount).forEach(i -> powerNets[i] += powerLevels[i]);
            count.incrementAndGet();
        });

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getLogPrefix(optimisationAssetId) + "Found plain consumer and producer child assets count=" + count.get());
            LOG.finest("Calculated net power of consumers and producers: " + Arrays.toString(powerNets));
            LOG.finest("Getting supply costs for each interval");
        }

        // Get supplier costs for each interval
        ElectricitySupplierAsset supplierAsset = supplierAssets.get(0);
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

            LOG.finer(getLogPrefix(optimisationAssetId) + "Adjusting costs to include some carbon weighting, financialWeightingImport=" + financialWeightingImport + ", financialWeightingExport=" + financialWeightingExport);

            for (int i = 0; i < costsImport.length; i++) {
                costsImport[i] = (financialWeightingImport * costsImport[i]) + ((1-financialWeightingImport) * carbonImport[i]);
                costsExport[i] = (financialWeightingExport * costsExport[i]) + ((1-financialWeightingExport) * carbonExport[i]);
            }
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finer(getLogPrefix(optimisationAssetId) + "Import costs: " + Arrays.toString(costsImport));
            LOG.finer(getLogPrefix(optimisationAssetId) + "Export costs: " + Arrays.toString(costsExport));
        }

        // Optimise storage assets with priority on storage assets with an energy schedule (already sorted above)
        double importPowerMax = supplierAsset.getPowerImportMax().orElse(Double.MAX_VALUE);
        double exportPowerMax = -1 * supplierAsset.getPowerExportMax().orElse(Double.MAX_VALUE);
        double[] importPowerMaxes = new double[intervalCount];
        double[] exportPowerMaxes = new double[intervalCount];
        Arrays.fill(importPowerMaxes, importPowerMax);
        Arrays.fill(exportPowerMaxes, exportPowerMax);
        long periodSeconds = (long)(optimiser.getIntervalSize()*60*60);

        LOG.finer(getLogPrefix(optimisationAssetId) + "Optimising each storage asset");

        for (ElectricityStorageAsset storageAsset : optimisableStorageAssets) {
            boolean hasSetpoint = storageAsset.hasAttribute(ElectricityStorageAsset.POWER_SETPOINT);
            boolean supportsExport = storageAsset.isSupportsExport().orElse(false);
            boolean supportsImport = storageAsset.isSupportsImport().orElse(false);

            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Optimising power set points for storage asset: " + storageAsset);
            }

            if (!supportsExport && !supportsImport) {
                LOG.finest(getLogPrefix(optimisationAssetId) + "Storage asset doesn't support import or export: " + storageAsset.getId());
                continue;
            }

            if (!hasSetpoint) {
                LOG.info(getLogPrefix(optimisationAssetId) + "Storage asset has no '" + ElectricityStorageAsset.POWER_SETPOINT.getName() + "' attribute so cannot be controlled: " + storageAsset.getId());
                continue;
            }

            double[] setpoints = getStoragePowerSetpoints(optimiser, optimisationAssetId, storageAsset, powerNets, importPowerMaxes, exportPowerMaxes, costsImport, costsExport);

            if (setpoints != null) {

                // Assume these setpoints will be applied so update the power net values with these
                for (int i = 0; i < powerNets.length; i++) {
                    powerNets[i] += setpoints[i];
                }

                // Push the setpoints into the prediction service for the storage asset's setpoint attribute and set current setpoint
                List<Pair<?, LocalDateTime>> valuesAndTimestamps = IntStream.range(1, setpoints.length).mapToObj(i ->
                    new Pair<>(setpoints[i],
                        LocalDateTime.ofInstant(optimisationTime.plus(periodSeconds * i, ChronoUnit.SECONDS), ZoneId.systemDefault()))
                ).collect(Collectors.toList());

                assetPredictedDatapointService.updateValues(storageAsset.getId(), ElectricityAsset.POWER_SETPOINT.getName(), valuesAndTimestamps);
            }else {
                LOG.finest("Cannot optimise power set points for storage asset: " + storageAsset.getId());
            }

            assetProcessingService.sendAttributeEvent(new AttributeEvent(storageAsset.getId(), ElectricityAsset.POWER_SETPOINT, setpoints != null ? setpoints[0] : null));
        }
    }

    protected boolean isElectricityGroupAsset(Asset<?> asset) {
        if (!(asset instanceof GroupAsset)) {
            return false;
        }

        Class<?> assetClass = AssetModelUtil
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
            ValueDatapoint<?>[] predictedData = assetPredictedDatapointService.getValueDatapoints(
                ref,
                "minute",
                (long)(intervalSize * 60) + " minute",
                timestamp,
                timestamp.plus(24, HOURS).minus((long)(intervalSize * 60), ChronoUnit.MINUTES)
            );
            if (predictedData.length != values.length) {
                LOG.warning("Returned predicted data point count does not match interval count: Ref=" + ref + ", expected=" + values.length + ", actual=" + predictedData.length);
            } else {
                IntStream.range(0, predictedData.length).forEach(i ->
                    values[i] = predictedData[i].getValue() != null ? (double)(Object)predictedData[i].getValue() : 0d);
            }
        } else {
            LOG.finer("Electricity asset doesn't have any predicted data for attribute: Ref=" + ref);
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
    protected double[] getStoragePowerSetpoints(EnergyOptimiser optimiser, String optimisationAssetId, ElectricityStorageAsset storageAsset, double[] powerNets, double[] importPowerLimits, double[] exportPowerLimits, double[] costImports, double[] costExports) {

        int intervalCount = powerNets.length;
        boolean supportsExport = storageAsset.isSupportsExport().orElse(false);
        boolean supportsImport = storageAsset.isSupportsImport().orElse(false);

        LOG.finer(getLogPrefix(optimisationAssetId) + "Optimising storage asset: " + storageAsset);

        double energyCapacity = storageAsset.getEnergyCapacity().orElse(0d);
        double energyLevel = Math.min(energyCapacity, storageAsset.getEnergyLevel().orElse(-1d));

        if (energyCapacity <= 0d || energyLevel < 0) {
            LOG.info(getLogPrefix(optimisationAssetId) + "Storage asset has no capacity or energy level so cannot import or export energy: " + storageAsset.getId());
            return null;
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
        int[][] energySchedule = energyLevelScheduleOptional.map(dayArr -> Arrays.stream(dayArr).map(hourArr -> Arrays.stream(hourArr).mapToInt(Integer::intValue).toArray()).toArray(int[][]::new)).orElse(null);

        if (energySchedule != null) {
            LOG.finer(getLogPrefix(optimisationAssetId) + "Applying energy schedule for storage asset: " + storageAsset.getId());
            optimiser.applyEnergySchedule(energyLevelMins, energyLevelMaxs, energyCapacity, energySchedule, LocalDateTime.ofInstant(Instant.ofEpochMilli(timerService.getCurrentTimeMillis()), ZoneId.systemDefault()));
        }

        boolean isConnected = !(storageAsset instanceof ElectricVehicleAsset) || ((ElectricVehicleAsset)storageAsset).getChargerConnected().orElse(false);

        // TODO: Make these a function of energy level
        Function<Integer, Double> powerImportMaxCalculator = interval -> interval == 0 && !isConnected ? 0 : powerImportMax;
        Function<Integer, Double> powerExportMaxCalculator = interval -> interval == 0 && !isConnected ? 0 : powerExportMax;

        if (hasEnergyMinRequirement) {
            LOG.finer(getLogPrefix(optimisationAssetId) + "Normalising min energy requirements for storage asset: " + storageAsset.getId());
            optimiser.normaliseEnergyMinRequirements(energyLevelMins, powerImportMaxCalculator, powerExportMaxCalculator, energyLevel);
        }

        double[][] exportCostAndPower = null;
        double[][] importCostAndPower = null;
        double[] powerSetpoints = new double[intervalCount];

        Function<Integer, Double> energyLevelCalculator = interval ->
                energyLevel + IntStream.range(0, interval).mapToDouble(j -> powerSetpoints[j] * optimiser.getIntervalSize()).sum();

        // If asset supports exporting energy (V2G, battery storage, etc.) then need to determine if there are
        // opportunities to export energy to save/earn, taking into consideration the cost of exporting from this asset
        if (supportsExport) {
            LOG.finer(getLogPrefix(optimisationAssetId) + "Storage asset supports export so calculating export cost and power levels for each interval: " + storageAsset.getId());
            // Find intervals that save/earn by exporting energy from this storage asset by looking at power levels
            BiFunction<Integer, Double, double[]> exportOptimiser = optimiser.getExportOptimiser(powerNets, exportPowerLimits, costImports, costExports, storageAsset.getTariffExport().orElse(0d));
            exportCostAndPower = IntStream.range(0, intervalCount).mapToObj(it -> exportOptimiser.apply(it, powerExportMax))
                .toArray(double[][]::new);
        }

        // If asset supports importing energy then need to determine if there are opportunities to import energy to
        // save/earn, taking into consideration the cost of importing to this asset, also need to ensure that min
        // energy demands are met.
        if (supportsImport) {
            LOG.finer(getLogPrefix(optimisationAssetId) + "Storage asset supports import so calculating export cost and power levels for each interval: " + storageAsset.getId());
            BiFunction<Integer, double[], double[]> importOptimiser = optimiser.getImportOptimiser(powerNets, importPowerLimits, costImports, costExports, storageAsset.getTariffImport().orElse(0d));
            importCostAndPower = IntStream.range(0, intervalCount).mapToObj(it -> importOptimiser.apply(it, new double[]{0d, powerImportMax}))
                .toArray(double[][]::new);

            if (hasEnergyMinRequirement) {
                LOG.finer(getLogPrefix(optimisationAssetId) + "Applying imports to achieve min energy level requirements for storage asset: " + storageAsset.getId());
                optimiser.applyEnergyMinImports(importCostAndPower, energyLevelMins, powerSetpoints, energyLevelCalculator, importOptimiser, powerImportMaxCalculator);
            }
        }

        LOG.finer(getLogPrefix(optimisationAssetId) + "Applying earning opportunities for storage asset: " + storageAsset.getId());
        optimiser.applyEarningOpportunities(importCostAndPower, exportCostAndPower, energyLevelMins, energyLevelMaxs, powerSetpoints, energyLevelCalculator, powerImportMaxCalculator, powerExportMaxCalculator);
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(getLogPrefix(optimisationAssetId) + "Calculated earning opportunity power set points = " + Arrays.toString(powerSetpoints));

            double[][] finalImportCostAndPower = importCostAndPower;
            double[][] finalExportCostAndPower = exportCostAndPower;
            double totalCost = IntStream.range(0, powerSetpoints.length).mapToDouble(i -> {
                double setpoint = powerSetpoints[i];
                double cost = setpoint < 0d ? finalExportCostAndPower[i][0] : finalImportCostAndPower[i][0];
                return cost * setpoint;
            }).reduce(0, Double::sum);

            LOG.finer(getLogPrefix(optimisationAssetId) + "Calculated earning opportunity cost = " + totalCost);
        }

        return powerSetpoints;
    }
}
