/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.manager.asset;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.AssetPredictedDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ForecastConfiguration;
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage;
import org.openremote.model.value.MetaItemType;

import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.openremote.container.concurrent.GlobalLock.withLockReturning;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;
import static org.openremote.model.value.MetaItemType.FORECAST;

/**
 * Calculates forecast values for asset attributes with an attached {@link MetaItemType#FORECAST}
 * configuration like {@link ForecastConfigurationWeightedExponentialAverage}.
 */
public class ForecastService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(ForecastService.class.getName());
    private static long STOP_TIMEOUT = Duration.ofSeconds(5).toMillis();

    protected TimerService timerService;
    protected GatewayService gatewayService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected PersistenceService persistenceService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected ScheduledExecutorService executorService;
    protected ForecastTaskManager forecastTaskManager = new ForecastTaskManager();

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        gatewayService = container.getService(GatewayService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        persistenceService = container.getService(PersistenceService.class);
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        executorService = container.getExecutorService();
    }

    @Override
    public void start(Container container) throws Exception {
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        LOG.fine("Loading forecast asset attributes...");

        List<Asset<?>> assets = getForecastAssets();

        Map<AttributeRef, ForecastConfiguration> configMap = assets
            .stream()
            .flatMap(asset -> asset.getAttributes()
                .stream()
                .filter(attr -> {
                    if (attr.hasMeta(FORECAST)) {
                        Optional<ForecastConfiguration> forecastConfig = attr.getMetaValue(FORECAST);
                        return forecastConfig.isPresent() &&
                               ForecastConfigurationWeightedExponentialAverage.TYPE.equals(forecastConfig.get().getType());
                    }
                    return false;
                })
               .map(attr -> new Pair<Asset<?>, Attribute<?>>(asset, attr))
               .toList()
               .stream())
            .collect(
                Collectors.toMap(
                    assetAndAttr -> new AttributeRef(
                        assetAndAttr.getKey().getId(),
                        assetAndAttr.getValue().getName()
                    ),
                    assetAndAttr -> assetAndAttr.getValue().getMetaValue(FORECAST).get()
                )
            );

        LOG.fine("Found forecast asset attributes count  = " + configMap.size());

        forecastTaskManager.init(configMap);
    }

    @Override
    public void stop(Container container) throws Exception {
        forecastTaskManager.stop(STOP_TIMEOUT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("ForecastConfigurationPersistenceChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                PersistenceEvent<Asset<?>> persistenceEvent = (PersistenceEvent<Asset<?>>)exchange.getIn().getBody(PersistenceEvent.class);
                processAssetChange(persistenceEvent);
            });
    }

    protected List<Asset<?>> getForecastAssets() {
        return withLockReturning(getClass().getSimpleName() + "::getForecastAssets", () -> {
            return assetStorageService.findAll(
                new AssetQuery().attributes(
                    new AttributePredicate().meta(
                        new NameValuePredicate(
                            FORECAST,
                            new StringPredicate(AssetQuery.Match.CONTAINS, true, "type")
                        )
                    )
                )
            );
        });
    }

    protected void processAssetChange(PersistenceEvent<Asset<?>> persistenceEvent) {

        Asset<?> asset = persistenceEvent.getEntity();

        switch (persistenceEvent.getCause()) {
            case CREATE:
                Map<AttributeRef, ForecastConfiguration> configMap = asset.getAttributes()
                    .stream()
                    .filter(attr -> {
                        if (attr.hasMeta(FORECAST)) {
                            Optional<ForecastConfiguration> forecastConfig = attr.getMetaValue(FORECAST);
                            return forecastConfig.isPresent() &&
                                   ForecastConfigurationWeightedExponentialAverage.TYPE.equals(forecastConfig.get().getType());
                        }
                        return false;
                    })
                    .map(attr -> new Pair<Asset<?>, Attribute<?>>(asset, attr))
                    .collect(
                        Collectors.toMap(
                            assetAndAttr -> new AttributeRef(
                                assetAndAttr.getKey().getId(),
                                assetAndAttr.getValue().getName()
                            ),
                            assetAndAttr -> assetAndAttr.getValue().getMetaValue(FORECAST).get()
                        )
                    );

                if (configMap.size() > 0) {
                    forecastTaskManager.add(configMap);
                }

                break;
            case UPDATE:
                if (persistenceEvent.getPropertyNames() == null || persistenceEvent.getPropertyNames().indexOf("attributes") < 0) {
                    return;
                }

                List<Attribute<?>> oldAttributes = ((AttributeMap)persistenceEvent.getPreviousState("attributes"))
                    .stream()
                    .filter(attr -> attr.hasMeta(FORECAST))
                    .collect(toList());
                List<Attribute<?>> newAttributes = ((AttributeMap) persistenceEvent.getCurrentState("attributes"))
                    .stream()
                    .filter(attr -> attr.hasMeta(FORECAST))
                    .collect(Collectors.toList());

                List<Attribute<?>> newOrModifiedAttributes = getAddedOrModifiedAttributes(oldAttributes, newAttributes).collect(toList());

                List<AttributeRef> attributesToDelete = newOrModifiedAttributes
                    .stream()
                    .filter(attr -> forecastTaskManager.containsAttribute(new AttributeRef(asset.getId(), attr.getName())))
                    .map(attr -> new AttributeRef(asset.getId(), attr.getName()))
                    .collect(Collectors.toList());

                attributesToDelete.addAll(oldAttributes
                    .stream()
                    .filter(oldAttribute -> {
                            return newAttributes
                                .stream()
                                .filter(newAttribute -> newAttribute.getName().equals(oldAttribute.getName()))
                                .count() == 0;
                        })
                    .map(attribute -> new AttributeRef(asset.getId(), attribute.getName()))
                    .filter(attributeRef -> !attributesToDelete.contains(attributeRef))
                    .collect(Collectors.toList())
                );

                forecastTaskManager.delete(attributesToDelete);

                configMap = newOrModifiedAttributes
                    .stream()
                    .filter(attr -> {
                        if (attr.hasMeta(FORECAST)) {
                            Optional<ForecastConfiguration> forecastConfig = attr.getMetaValue(FORECAST);
                            return forecastConfig.isPresent() &&
                                ForecastConfigurationWeightedExponentialAverage.TYPE.equals(forecastConfig.get().getType());
                        }
                        return false;
                    })
                    .map(attr -> new Pair<Asset<?>, Attribute<?>>(asset, attr))
                    .collect(
                        Collectors.toMap(
                            assetAndAttr -> new AttributeRef(
                                assetAndAttr.getKey().getId(),
                                assetAndAttr.getValue().getName()
                            ),
                            assetAndAttr -> assetAndAttr.getValue().getMetaValue(FORECAST).get()
                        )
                    );

                if (configMap.size() > 0) {
                    forecastTaskManager.add(configMap);
                }

                break;
            case DELETE:
                List<AttributeRef> attributeRefs = asset.getAttributes()
                    .stream()
                    .filter(attr -> attr.hasMeta(FORECAST))
                    .map(attr -> new AttributeRef(asset.getId(), attr.getName()))
                    .collect(Collectors.toList());

                forecastTaskManager.delete(attributeRefs);

                break;
        }
    }

    private class ForecastTaskManager {

        private static long DELAY_MIN_TO_CANCEL_SAFELY = Duration.ofSeconds(2).toMillis();
        private static long DEFAULT_SCHEDULE_DELAY = Duration.ofMinutes(15).toMillis();

        protected ScheduledFuture<?> scheduledFuture;
        protected Map<AttributeRef, Long> nextForecastCalculationMap = new HashMap<>();
        protected Map<AttributeRef, List<Long>> forecastTimestampMap = new HashMap<>();
        protected Map<AttributeRef, ForecastConfiguration> configMap = new HashMap<>();

        public synchronized void init(Map<AttributeRef, ForecastConfiguration> configMap) {
            long now = timerService.getCurrentTimeMillis();
            configMap.forEach((attributeRef, config) -> {
                if (config.isValid()) {
                    this.configMap.put(attributeRef, config);
                    loadForecastTimestampsFromDb(attributeRef, now);
                }
            });
            start(now, true);
        }

        public synchronized void add(Map<AttributeRef, ForecastConfiguration> configMap) {
            configMap.forEach((attributeRef, config) -> {
                if (config.isValid()) {
                    LOG.fine("Adding asset attribute to forecast calculation service: " + attributeRef);
                    this.configMap.put(attributeRef, config);
                }
            });
            long now = timerService.getCurrentTimeMillis();
            if (scheduledFuture != null) {
                if (scheduledFuture.getDelay(TimeUnit.MILLISECONDS) > DELAY_MIN_TO_CANCEL_SAFELY) {
                    scheduledFuture.cancel(false);
                    scheduledFuture = null;
                    start(now);
                }
            } else {
                start(now);
            }
        }

        public synchronized void delete(List<AttributeRef> attributeRefs) {
            attributeRefs.forEach(attributeRef -> delete(attributeRef));
        }

        public synchronized void delete(AttributeRef attributeRef) {
            LOG.fine("Removing asset attribute from forecast calculation service: " + attributeRef);

            nextForecastCalculationMap.remove(attributeRef);
            forecastTimestampMap.remove(attributeRef);
            configMap.remove(attributeRef);

            assetPredictedDatapointService.purgeValues(attributeRef.getId(), attributeRef.getName());
        }

        public synchronized boolean containsAttribute(AttributeRef attributeRef) {
            return configMap.containsKey(attributeRef);
        }

        private boolean stop(long timeout) {
            long start = timerService.getCurrentTimeMillis();
            while (true) {
                synchronized (ForecastTaskManager.this) {
                    if (scheduledFuture == null) {
                        return true;
                    } else if (scheduledFuture != null && scheduledFuture.getDelay(TimeUnit.MILLISECONDS) > DELAY_MIN_TO_CANCEL_SAFELY) {
                        scheduledFuture.cancel(false);
                        scheduledFuture = null;
                        return true;
                    } else {
                        if (timerService.getCurrentTimeMillis() - start > timeout) {
                            scheduledFuture.cancel(true);
                            scheduledFuture = null;
                            return false;
                        }
                    }
                }
                try {
                    Thread.currentThread().sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        private synchronized void start(long now) {
            start(now, false);
        }

        private synchronized void start(long now, boolean isServerRestart) {
            if (scheduledFuture == null) {
                addForecastTimestamps(now, isServerRestart);
                updateNextForecastCalculationMap();
                scheduleForecastCalculation(now, Optional.empty());
            }
        }

        private void calculateForecasts() {
            final long now = timerService.getCurrentTimeMillis();
            Map<AttributeRef, Pair<ForecastConfiguration, List<Long>>> configAndTimestampMap = new HashMap<>();

            try {
                synchronized (ForecastTaskManager.this) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    purgeForecastTimestamps(now);
                    addForecastTimestamps(now, false);
                    purgeForecastTimestamps(now);

                    nextForecastCalculationMap.forEach((attributeRef, nextForecastCalculationTimestamp) -> {
                        if (nextForecastCalculationTimestamp <= now && forecastTimestampMap.containsKey(attributeRef) && configMap.containsKey(attributeRef)) {
                            configAndTimestampMap.put(
                                attributeRef,
                                new Pair<>(configMap.get(attributeRef), forecastTimestampMap.get(attributeRef))
                            );
                        }
                    });
                }

                configAndTimestampMap.forEach((attributeRef, configAndTimestamps) -> {
                    Optional<Attribute<?>> attribute = Optional.empty();
                    Asset<?> asset = withLockReturning(getClass().getSimpleName() + "::calculateForecasts", () ->
                        assetStorageService.find(attributeRef.getId())
                    );
                    if (asset != null) {
                        attribute = asset.getAttribute(attributeRef.getName());
                    }
                    if (attribute.isEmpty()) {
                        return;
                    }

                    List<Long> forecastTimestamps = configAndTimestamps.getValue();
                    if (forecastTimestamps.size() == 0) {
                        return;
                    }
                    if (!(configAndTimestamps.getKey() instanceof ForecastConfigurationWeightedExponentialAverage)) {
                        return;
                    }
                    ForecastConfigurationWeightedExponentialAverage config = (ForecastConfigurationWeightedExponentialAverage) configAndTimestamps.getKey();

                    LOG.fine("Calculating forecast values for attribute: " + attributeRef);

                    Long offset = forecastTimestamps.get(0) - (now + config.getForecastPeriod().toMillis());
                    List<List<Long>> allSampleTimestamps = calculateSampleTimestamps(config, offset);
                    List<DatapointBucket> historyDatapointBuckets = getHistoryDataFromDb(attributeRef, config, offset);
                    Attribute<?> finalAttribute = attribute.get();

                    List<Optional<Number>> forecastValues = allSampleTimestamps.stream().map(sampleTimestamps -> {
                        List<AssetDatapoint> sampleDatapoints = findSampleDatapoints(historyDatapointBuckets, sampleTimestamps);

                        if (sampleDatapoints.size() == config.getPastCount()) {
                            return calculateWeightedExponentialAverage(finalAttribute, sampleDatapoints);
                        } else {
                            return Optional.<Number>empty();
                        }
                    }).collect(Collectors.toList());

                    if (forecastTimestamps.size() >= forecastValues.size()) {
                        List<Pair<?, LocalDateTime>> datapoints = IntStream
                            .range(0, forecastValues.size())
                            .filter(i -> forecastValues.get(i).isPresent())
                            .mapToObj(i -> new Pair<>(
                                forecastValues.get(i).get(),
                                LocalDateTime.ofInstant(Instant.ofEpochMilli(forecastTimestamps.get(i)), ZoneId.systemDefault()))
                            )
                            .collect(Collectors.toList());

                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }

                        assetPredictedDatapointService.purgeValues(attributeRef.getId(), attributeRef.getName());

                        if (datapoints.size() > 0) {
                            LOG.fine("Updating forecast values for attribute: " + attributeRef);
                            assetPredictedDatapointService.updateValues(attributeRef.getId(), attributeRef.getName(), datapoints);
                        }
                    }
                });

                synchronized (ForecastTaskManager.this) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    updateNextForecastCalculationMap();
                    scheduleForecastCalculation(timerService.getCurrentTimeMillis(), Optional.empty());
                }
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "Exception while calculating and updating forecast values", e);

                scheduleForecastCalculation(
                    timerService.getCurrentTimeMillis(),
                    Optional.of(DEFAULT_SCHEDULE_DELAY)
                );
            }
        }

        private synchronized void scheduleForecastCalculation(long now, Optional<Long> fixedDelay) {
            Optional<Long> delay = fixedDelay;

            if (delay.isEmpty()) {
                delay = calculateScheduleDelay(now);
            }

            if (delay.isPresent()) {
                LOG.fine("Scheduling next forecast calculation in '" + delay.get() + " [ms]'.");
                scheduledFuture = executorService.schedule(() -> calculateForecasts(), delay.get(), TimeUnit.MILLISECONDS);
            } else {
                scheduledFuture = null;
                if (configMap.size() > 0) {
                    LOG.fine("Scheduling next forecast calculation in '" + DEFAULT_SCHEDULE_DELAY + " [ms]'.");
                    scheduleForecastCalculation(now, Optional.of(DEFAULT_SCHEDULE_DELAY));
                }
            }
        }

        private List<List<Long>> calculateSampleTimestamps(ForecastConfigurationWeightedExponentialAverage config, Long offset) {
            List<List<Long>> sampleTimestamps = new ArrayList<>(config.getForecastCount());
            long now = timerService.getCurrentTimeMillis();
            long pastPeriod = config.getPastPeriod().toMillis();
            long forecastPeriod = config.getForecastPeriod().toMillis();

            for (int forecastIndex = 1; forecastIndex <= config.getForecastCount(); forecastIndex++) {
                List<Long> timestamps = new ArrayList<>(config.getPastCount());
                for (int pastPeriodIndex = config.getPastCount(); pastPeriodIndex > 0; pastPeriodIndex--) {
                    timestamps.add(now - (pastPeriod * pastPeriodIndex) + (forecastPeriod * forecastIndex) + offset);
                }
                sampleTimestamps.add(timestamps);
            }

            return sampleTimestamps;
        }

        private List<Long> calculateForecastTimestamps(long now, ForecastConfigurationWeightedExponentialAverage config) {
            List<Long> forecastTimestamps = new ArrayList<>(config.getForecastCount());
            long forecastPeriod = config.getForecastPeriod().toMillis();

            for (int forecastIndex = 1; forecastIndex <= config.getForecastCount(); forecastIndex++) {
                forecastTimestamps.add(now + forecastPeriod * forecastIndex);
            }

            return forecastTimestamps;
        }

        private List<AssetDatapoint> findSampleDatapoints(List<DatapointBucket> datapointBuckets, List<Long> sampleTimestamps) {
            List<AssetDatapoint> sampleDatapoints = new ArrayList<>(sampleTimestamps.size());

            for (Long timestamp : sampleTimestamps) {
                AssetDatapoint foundDatapoint = null;

                List<AssetDatapoint> datapoints = datapointBuckets
                    .stream()
                    .filter(bucket -> bucket.isInTimeRange(timestamp))
                    .findFirst()
                    .map(bucket -> bucket.getDatapoints())
                    .orElse(null);

                if (datapoints == null) {
                    continue;
                }

                for (AssetDatapoint assetDatapoint : datapoints) {
                    if (assetDatapoint.getTimestamp() <= timestamp) {
                        foundDatapoint = assetDatapoint;
                    } else if (assetDatapoint.getTimestamp() > timestamp) {
                        break;
                    }
                }

                if (foundDatapoint != null) {
                    sampleDatapoints.add(foundDatapoint);
                }
            }
            return sampleDatapoints;
        }

        private Optional<Number> calculateWeightedExponentialAverage(Attribute<?> attribute, List<AssetDatapoint> datapoints) {
            // a = 2 / (R + 1)
            // p: past period
            // Attr(t) = Attr(t-p) * a + Attr(t-2p) * (1 - a)
            List<Object> values = datapoints
                .stream()
                .map(dp -> dp.getValue())
                .collect(Collectors.toList());
            double R = datapoints.size();
            double a = 2 / (R + 1);

            Class<?> clazz = attribute.getType().getType();
            if (Long.class == clazz || Integer.class == clazz || Short.class == clazz || Byte.class == clazz ||
                Double.class == clazz || Float.class == clazz) {
                if (values.size() == 1) {
                    values.add(0, Double.valueOf(0));
                }
                Optional<Number> value = values
                    .stream()
                    .map(v -> (Number)v)
                    .reduce((olderValue, oldValue) ->
                        Double.valueOf(oldValue.doubleValue() * a + olderValue.doubleValue() * (1 - a))
                    );
                if (value.isPresent()) {
                    if (clazz == Long.class) {
                        value = Optional.of(Long.valueOf(value.get().longValue()));
                    } else if (clazz == Integer.class) {
                        value = Optional.of(Integer.valueOf(value.get().intValue()));
                    } else if (clazz == Short.class) {
                        value = Optional.of(Short.valueOf(value.get().shortValue()));
                    } else if (clazz == Byte.class) {
                        value = Optional.of(Byte.valueOf(value.get().byteValue()));
                    } else if (clazz == Double.class) {
                        value = Optional.of(Double.valueOf(value.get().doubleValue()));
                    } else if (clazz == Float.class) {
                        value = Optional.of(Float.valueOf(value.get().floatValue()));
                    } else {
                        value = Optional.empty();
                    }
                }
                return value;
            } else if (attribute.getType().getType() == BigDecimal.class) {
                if (values.size() == 1) {
                    values.add(0, BigDecimal.valueOf(0));
                }
                return values
                    .stream()
                    .map(v -> (Number)v)
                    .reduce((olderValue, oldValue) ->
                        ((BigDecimal)oldValue).multiply(BigDecimal.valueOf(a)).add(((BigDecimal)olderValue).multiply(BigDecimal.valueOf(1 - a)))
                    );
            } else if (attribute.getType().getType() == BigInteger.class) {
                if (values.size() == 1) {
                    values.add(0, BigInteger.valueOf(0));
                }
                // Attr(t) = Attr(t-p) * a + Attr(t-2p) * (1 - a)
                // Attr(t) = (Attr(t-p) * 2 / (R + 1) + Attr(t-2p) * (1 - 2 / (R + 1))) * (R + 1)/(R + 1)
                // Attr(t) = ((Attr(t-p) * 2 + Attr(t-2p) * (R - 1)) / (R + 1)
                return values
                    .stream()
                    .map(v -> (Number)v)
                    .reduce((olderValue, oldValue) ->
                        ((BigInteger)oldValue).multiply(BigInteger.valueOf(2))
                        .add(((BigInteger)olderValue).multiply(BigInteger.valueOf((long)R - 1)))
                        .divide(BigInteger.valueOf((long)R + 1))
                    );
            }
            return Optional.empty();
        }

        private void updateNextForecastCalculationMap() {
            nextForecastCalculationMap.clear();
            forecastTimestampMap.forEach((attributeRef, timestamps)-> {
                if (timestamps.size() > 0) {
                    nextForecastCalculationMap.put(attributeRef, timestamps.get(0));
                }
            });
        }

        private Optional<Long> calculateScheduleDelay(long now) {
            OptionalLong calculateForecastTimestamp = nextForecastCalculationMap.values().stream().mapToLong(v -> v).min();
            if (calculateForecastTimestamp.isPresent()) {
                long delay = calculateForecastTimestamp.getAsLong() - now;
                return Optional.of(delay < 0 ? 0 : delay);
            } else {
                return Optional.empty();
            }
        }

        private void addForecastTimestamps(long now, boolean isServerRestart) {
            configMap.forEach((attributeRef, config) -> {
                if (!(config instanceof ForecastConfigurationWeightedExponentialAverage)) {
                    return;
                }
                List<Long> newTimestamps = calculateForecastTimestamps(now, (ForecastConfigurationWeightedExponentialAverage)config);
                List<Long> oldTimestamps = forecastTimestampMap.get(attributeRef);
                if (oldTimestamps == null || oldTimestamps.size() == 0) {
                    if (newTimestamps.size() > 0) {
                        // force immediate forecast calculation
                        newTimestamps.add(0, now);
                    }
                    forecastTimestampMap.put(attributeRef, newTimestamps);
                } else if (newTimestamps.size() > 0 && oldTimestamps.size() > 0) {
                    long offset = oldTimestamps.get(0) - newTimestamps.get(0);
                    List<Long> newShiftedTimestamps = newTimestamps.stream().map(timestamp -> timestamp + offset).collect(Collectors.toList());
                    while(true) {
                        if (newShiftedTimestamps.get(0) < now) {
                            newShiftedTimestamps = newShiftedTimestamps
                                .stream()
                                .map(timestamp -> timestamp + ((ForecastConfigurationWeightedExponentialAverage)config).getForecastPeriod().toMillis())
                                .collect(Collectors.toList());
                        } else {
                            break;
                        }
                    }
                    if (isServerRestart && (oldTimestamps.get(0) < now || newTimestamps.size() > oldTimestamps.size())) {
                        // force immediate forecast calculation
                        newShiftedTimestamps.add(0, now);
                    }
                    forecastTimestampMap.put(attributeRef, newShiftedTimestamps);
                }
            });
        }

        public void purgeForecastTimestamps(long now) {
            forecastTimestampMap.values().stream().forEach(timestamps -> {
                int clearCount = 0;
                int index = Collections.binarySearch(timestamps, now);
                if (index >= 0) {
                    clearCount = index + 1;
                } else {
                    clearCount = index * (-1) -1;
                }
                if (clearCount > 0) {
                    if (clearCount == timestamps.size()) {
                        timestamps.clear();
                    } else {
                        try {
                            timestamps.subList(0, clearCount).clear();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }

        private List<DatapointBucket> getHistoryDataFromDb(AttributeRef attributeRef, ForecastConfigurationWeightedExponentialAverage config, long offset) {
            List<DatapointBucket> datapointBuckets = new ArrayList<>(config.getPastCount());

            StringBuilder sb = new StringBuilder();
            sb.append(
                "select dp from " + AssetDatapoint.class.getSimpleName() + " dp " +
                    "where dp.assetId = :assetId " +
                    "and dp.attributeName = :attributeName "
            );
            for (int i = 1; i <= config.getPastCount(); i++) {
                sb.append(i == 1 ? "and (" : " or " );
                sb.append("(dp.timestamp >= :timestampMin" + i + " and dp.timestamp <= :timestampMax" + i + ")");
                if (i == config.getPastCount()) {
                    sb.append(") ");
                }
            }
            sb.append("order by dp.timestamp asc");

            List<AssetDatapoint> datapoints = persistenceService.doReturningTransaction(entityManager -> {
                TypedQuery<AssetDatapoint> query = entityManager.createQuery(sb.toString(), AssetDatapoint.class)
                    .setParameter("assetId", attributeRef.getId())
                    .setParameter("attributeName", attributeRef.getName());
                long now = timerService.getCurrentTimeMillis();
                long pastPeriod = config.getPastPeriod().toMillis();
                long forecastPeriod = config.getForecastPeriod().toMillis();
                long totalForecastPeriod = Math.min(forecastPeriod * config.getForecastCount(), pastPeriod);

                for (int i = config.getPastCount(); i >= 1; i--) {
                    long timestampMin = now - (pastPeriod * i) + offset;
                    long timestampMax = now - (pastPeriod * i) + totalForecastPeriod + offset;

                    datapointBuckets.add(new DatapointBucket(timestampMin, timestampMax));

                    query.setParameter("timestampMin" + i, new Date(timestampMin));
                    query.setParameter("timestampMax" + i, new Date(timestampMax));
                }
                return query.getResultList();
            });

            datapoints.forEach(datapoint -> {
                datapointBuckets.stream()
                    .filter(bucket -> bucket.isInTimeRange(datapoint.getTimestamp()))
                    .findFirst()
                    .ifPresent(bucket -> bucket.add(datapoint));
            });

            return datapointBuckets;
        }

        private void loadForecastTimestampsFromDb(AttributeRef attributeRef, long now) {
            List<AssetPredictedDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef);
            List<Long> timestamps = datapoints
                .stream()
                .map(datapoint -> datapoint.getTimestamp())
                .filter(timestamp -> timestamp >= now)
                .sorted()
                .collect(Collectors.toList());;
            forecastTimestampMap.put(attributeRef, timestamps);
        }
    }

    private static class DatapointBucket {
        private long begin;
        private long end;
        private List<AssetDatapoint> datapoints = new ArrayList<>();

        public DatapointBucket(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }

        public long getBegin() {
            return begin;
        }

        public long getEnd() {
            return end;
        }

        public List<AssetDatapoint> getDatapoints() {
            return datapoints;
        }

        public boolean isInTimeRange(long timestamp) {
            return timestamp >= begin && timestamp <= end;
        }

        public void add(AssetDatapoint datapoint) {
            datapoints.add(datapoint);
        }
    }
}
