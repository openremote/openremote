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

import jakarta.persistence.TypedQuery;
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
import org.openremote.model.datapoint.Datapoint;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.value.ForecastConfiguration;
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage;
import org.openremote.model.value.MetaItemType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;
import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;
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

        Set<ForecastAttribute> forecastAttributes = assets
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
               .map(attr -> new ForecastAttribute(asset, attr)))
            .collect(Collectors.toSet());

        LOG.fine("Found forecast asset attributes count  = " + forecastAttributes.size());

        forecastTaskManager.init(forecastAttributes);
    }

    @Override
    public void stop(Container container) throws Exception {
        forecastTaskManager.stop(STOP_TIMEOUT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("Persistence-ForecastConfiguration")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                PersistenceEvent<Asset<?>> persistenceEvent = (PersistenceEvent<Asset<?>>)exchange.getIn().getBody(PersistenceEvent.class);
                processAssetChange(persistenceEvent);
            });
    }

    protected List<Asset<?>> getForecastAssets() {
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
    }

    protected void processAssetChange(PersistenceEvent<Asset<?>> persistenceEvent) {

        Asset<?> asset = persistenceEvent.getEntity();
        Set<ForecastAttribute> forecastAttributes = null;

        switch (persistenceEvent.getCause()) {
            case CREATE:
                forecastAttributes = asset.getAttributes()
                    .stream()
                    .filter(attr -> {
                        if (attr.hasMeta(FORECAST)) {
                            Optional<ForecastConfiguration> forecastConfig = attr.getMetaValue(FORECAST);
                            return forecastConfig.isPresent() &&
                                   ForecastConfigurationWeightedExponentialAverage.TYPE.equals(forecastConfig.get().getType());
                        }
                        return false;
                    })
                    .map(attr -> new ForecastAttribute(asset, attr))
                    .collect(Collectors.toSet());

                forecastTaskManager.add(forecastAttributes);

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

                Set<ForecastAttribute> attributesToDelete = newOrModifiedAttributes
                    .stream()
                    .map(attr -> new ForecastAttribute(asset, attr))
                    .filter(attr -> forecastTaskManager.containsAttribute(attr))
                    .collect(Collectors.toSet());

                attributesToDelete.addAll(oldAttributes
                    .stream()
                    .filter(oldAttr -> {
                        return newAttributes
                            .stream()
                            .filter(newAttr -> newAttr.getName().equals(oldAttr.getName()))
                            .count() == 0;
                    })
                    .map(attr -> new ForecastAttribute(asset, attr))
                    .toList()
                );

                forecastTaskManager.delete(attributesToDelete);

                forecastAttributes = newOrModifiedAttributes
                    .stream()
                    .filter(attr -> {
                        if (attr.hasMeta(FORECAST)) {
                            Optional<ForecastConfiguration> forecastConfig = attr.getMetaValue(FORECAST);
                            return forecastConfig.isPresent() &&
                                ForecastConfigurationWeightedExponentialAverage.TYPE.equals(forecastConfig.get().getType());
                        }
                        return false;
                    })
                    .map(attr -> new ForecastAttribute(asset, attr))
                    .collect(Collectors.toSet());

                forecastTaskManager.add(forecastAttributes);

                break;
            case DELETE:
                forecastAttributes = asset.getAttributes()
                    .stream()
                    .filter(attr -> attr.hasMeta(FORECAST))
                    .map(attr -> new ForecastAttribute(asset, attr))
                    .collect(Collectors.toSet());

                forecastTaskManager.delete(forecastAttributes);

                break;
        }
    }

    private class ForecastTaskManager {

        private static long DELAY_MIN_TO_CANCEL_SAFELY = Duration.ofSeconds(2).toMillis();
        private static long DEFAULT_SCHEDULE_DELAY = Duration.ofMinutes(15).toMillis();

        protected ScheduledFuture<?> scheduledFuture;
        protected Map<ForecastAttribute, Long> nextForecastCalculationMap = new HashMap<>();
        protected Set<ForecastAttribute> forecastAttributes = new HashSet<>();

        public synchronized void init(Set<ForecastAttribute> attributes) {
            if (attributes == null || attributes.size() == 0) {
                return;
            }
            long now = timerService.getCurrentTimeMillis();
            attributes.forEach(attr -> {
                if (attr.isValidConfig()) {
                    attr.setForecastTimestamps(loadForecastTimestampsFromDb(attr.getAttributeRef(), now));
                    forecastAttributes.add(attr);
                }
            });
            start(now, true);
        }

        public synchronized void add(Set<ForecastAttribute> attributes) {
            if (attributes == null || attributes.size() == 0) {
                return;
            }
            attributes.forEach(attr -> {
                if (attr.isValidConfig()) {
                    LOG.fine("Adding asset attribute to forecast calculation service: " + attr.getAttributeRef());
                    forecastAttributes.add(attr);
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

        public synchronized void delete(Set<ForecastAttribute> attributes) {
            attributes.forEach(attr -> delete(attr));
        }

        public synchronized void delete(ForecastAttribute attribute) {
            LOG.fine("Removing asset attribute from forecast calculation service: " + attribute.getAttributeRef());

            nextForecastCalculationMap.remove(attribute);
            forecastAttributes.remove(attribute);

            assetPredictedDatapointService.purgeValues(attribute.getAttributeRef().getId(), attribute.getAttributeRef().getName());
        }

        public synchronized boolean containsAttribute(ForecastAttribute attribute) {
            return forecastAttributes.contains(attribute);
        }

        public synchronized boolean containsAttribute(AttributeRef attributeRef) {
            return forecastAttributes
                .stream()
                .filter(attr -> attr.getAttributeRef().equals(attributeRef))
                .findFirst()
                .isPresent();
        }

        public synchronized ForecastAttribute getAttribute(AttributeRef attributeRef) {
            return forecastAttributes
                .stream()
                .filter(attr -> attr.getAttributeRef().equals(attributeRef))
                .findFirst()
                .orElse(null);
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

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        private void calculateForecasts() {
            final long now = timerService.getCurrentTimeMillis();
            List<ForecastAttribute> attributesToCalculate = new ArrayList<>();

            try {
                synchronized (ForecastTaskManager.this) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    purgeForecastTimestamps(now);
                    addForecastTimestamps(now, false);
                    purgeForecastTimestamps(now);

                    nextForecastCalculationMap.forEach((attribute, nextForecastCalculationTimestamp) -> {
                        if (nextForecastCalculationTimestamp <= now) {
                            attributesToCalculate.add(attribute);
                        }
                    });
                }

                attributesToCalculate.forEach(attr -> {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    List<Long> forecastTimestamps = attr.getForecastTimestamps();
                    if (forecastTimestamps == null || forecastTimestamps.size() == 0) {
                        return;
                    }
                    if (!(attr.getConfig() instanceof ForecastConfigurationWeightedExponentialAverage)) {
                        return;
                    }
                    ForecastConfigurationWeightedExponentialAverage weaConfig = (ForecastConfigurationWeightedExponentialAverage)attr.getConfig();

                    LOG.fine("Calculating forecast values for attribute: " + attr.getAttributeRef());

                    Long offset = forecastTimestamps.get(0) - (now + weaConfig.getForecastPeriod().toMillis());
                    List<List<Long>> allSampleTimestamps = calculateSampleTimestamps(weaConfig, offset);
                    List<DatapointBucket> historyDatapointBuckets = getHistoryDataFromDb(attr.getAttributeRef(), weaConfig, offset);

                    List<Optional<Number>> forecastValues = allSampleTimestamps.stream().map(sampleTimestamps -> {
                        List<AssetDatapoint> sampleDatapoints = findSampleDatapoints(historyDatapointBuckets, sampleTimestamps);

                        if (sampleDatapoints.size() == weaConfig.getPastCount()) {
                            return calculateWeightedExponentialAverage(attr.getAttribute(), sampleDatapoints);
                        } else {
                            return Optional.<Number>empty();
                        }
                    }).toList();

                    if (forecastTimestamps.size() >= forecastValues.size()) {
                        List<ValueDatapoint<?>> datapoints = IntStream
                            .range(0, forecastValues.size())
                            .filter(i -> forecastValues.get(i).isPresent())
                            .mapToObj(i -> new ValueDatapoint<>(
                                forecastTimestamps.get(i),
                                forecastValues.get(i).get())
                            )
                            .collect(Collectors.toList());

                        assetPredictedDatapointService.purgeValues(attr.getId(), attr.getName());

                        if (datapoints.size() > 0) {
                            LOG.fine("Updating forecast values for attribute: " + attr.getAttributeRef());
                            assetPredictedDatapointService.updateValues(attr.getId(), attr.getName(), datapoints);
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
            } catch (Exception e) {
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
                if (forecastAttributes.size() > 0) {
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
                .map(Datapoint::getValue)
                .collect(Collectors.toList());
            double R = datapoints.size();
            double a = 2 / (R + 1);

            Class<?> clazz = attribute.getTypeClass();
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
            } else if (attribute.getTypeClass() == BigDecimal.class) {
                if (values.size() == 1) {
                    values.add(0, BigDecimal.valueOf(0));
                }
                return values
                    .stream()
                    .map(v -> (Number)v)
                    .reduce((olderValue, oldValue) ->
                        ((BigDecimal)oldValue).multiply(BigDecimal.valueOf(a)).add(((BigDecimal)olderValue).multiply(BigDecimal.valueOf(1 - a)))
                    );
            } else if (attribute.getTypeClass() == BigInteger.class) {
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
            forecastAttributes.forEach(attr -> {
                List<Long> timestamps = attr.getForecastTimestamps();
                if (timestamps != null && timestamps.size() > 0) {
                    nextForecastCalculationMap.put(attr, timestamps.get(0));
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
            forecastAttributes.forEach(attr -> {
                ForecastConfiguration config = attr.getConfig();
                ForecastConfigurationWeightedExponentialAverage weaConfig = null;
                if (config instanceof ForecastConfigurationWeightedExponentialAverage) {
                    weaConfig = (ForecastConfigurationWeightedExponentialAverage)config;
                } else {
                    return;
                }
                List<Long> newTimestamps = calculateForecastTimestamps(now, weaConfig);
                List<Long> oldTimestamps = attr.getForecastTimestamps();
                if (oldTimestamps == null || oldTimestamps.size() == 0) {
                    if (newTimestamps.size() > 0) {
                        // force immediate forecast calculation
                        newTimestamps.add(0, now);
                    }
                    attr.setForecastTimestamps(newTimestamps);
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
                    attr.setForecastTimestamps(newShiftedTimestamps);
                }
            });
        }

        public void purgeForecastTimestamps(long now) {
            forecastAttributes.forEach(attr -> {
                List<Long> timestamps = attr.getForecastTimestamps();
                if (timestamps == null) {
                    return;
                }
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

        private List<Long> loadForecastTimestampsFromDb(AttributeRef attributeRef, long now) {
            List<ValueDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef);
            List<Long> timestamps = datapoints
                .stream()
                .map(ValueDatapoint::getTimestamp)
                .filter(timestamp -> timestamp >= now)
                .sorted()
                .collect(Collectors.toList());;
            return timestamps;
        }
    }

    public static class ForecastAttribute {

        private String assetId;
        private AttributeRef attributeRef;
        private Attribute<?> attribute;
        private ForecastConfiguration config;
        private List<Long> forecastTimestamps = new ArrayList<>();

        public ForecastAttribute(Asset<?> asset, Attribute<?> attribute) {
            this(asset.getId(), attribute);
        }

        public ForecastAttribute(String assetId, Attribute<?> attribute) {
            requireNonNullAndNonEmpty(assetId);
            if (attribute == null) {
                throw new IllegalArgumentException("Attribute cannot be null");
            }
            this.assetId = assetId;
            this.attribute = attribute;
            this.attributeRef = new AttributeRef(assetId, attribute.getName());
            this.config = attribute.getMetaValue(FORECAST).orElse(null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForecastAttribute that = (ForecastAttribute) o;
            return assetId.equals(that.assetId) && attribute.getName().equals(that.attribute.getName());
        }

        @Override
        public int hashCode() {
            int result = assetId.hashCode();
            result = 31 * result + attribute.getName().hashCode();
            return result;
        }

        public String getId() {
            return assetId;
        }

        public String getName() {
            return attribute.getName();
        }

        public AttributeRef getAttributeRef() {
            return attributeRef;
        }

        public Attribute<?> getAttribute() {
            return attribute;
        }

        public ForecastConfiguration getConfig() {
            return config;
        }

        public boolean isValidConfig() {
            return (config != null && config.isValid());
        }

        public void setForecastTimestamps(List<Long> timestamps) {
            this.forecastTimestamps = timestamps;
        }

        public List<Long> getForecastTimestamps() {
            return forecastTimestamps;
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
