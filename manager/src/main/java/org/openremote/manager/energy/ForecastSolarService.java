package org.openremote.manager.energy;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.core.Response;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.rules.RulesService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.ElectricityProducerSolarAsset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.syslog.SyslogCategory.DATA;

/**
 * Fills in power forecast from ForecastSolar (https://forecast.solar) for {@link ElectricityProducerSolarAsset}.
 */
public class ForecastSolarService extends RouteBuilder implements ContainerService {

    protected static class EstimateResponse {
        @JsonProperty
        protected Result result;
    }

    protected static class Result {
        @JsonProperty
        protected Map<String, Double> wattHours;
        @JsonProperty
        protected Map<String, Double> wattHoursDay;
        @JsonProperty
        protected Map<String, Double> watts;
    }

    public static final String OR_FORECAST_SOLAR_API_KEY = "OR_FORECAST_SOLAR_API_KEY";

    protected static final DateTimeFormatter ISO_LOCAL_DATE_TIME_WITHOUT_T = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter();

    protected ScheduledExecutorService executorService;
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected GatewayService gatewayService;
    protected ClientEventService clientEventService;
    protected RulesService rulesService;
    protected TimerService timerService;

    protected static final Logger LOG = SyslogCategory.getLogger(DATA, ForecastSolarService.class.getName());
    protected static final AtomicReference<ResteasyClient> resteasyClient = new AtomicReference<>();
    protected ResteasyWebTarget forecastSolarTarget;
    private String forecastSolarApiKey;
    private final Map<String, ScheduledFuture<?>> calculationFutures = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
                .routeId("Persistence-ForecastSolar")
                .filter(isPersistenceEventForEntityType(ElectricityProducerSolarAsset.class))
                .filter(isNotForGateway(gatewayService))
                .process(exchange -> processAssetChange((PersistenceEvent<ElectricityProducerSolarAsset>) exchange.getIn().getBody(PersistenceEvent.class)));
    }

    @Override
    public void init(Container container) throws Exception {
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        gatewayService = container.getService(GatewayService.class);
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        clientEventService = container.getService(ClientEventService.class);
        executorService = container.getExecutorService();
        rulesService = container.getService(RulesService.class);
        timerService = container.getService(TimerService.class);

        forecastSolarApiKey = getString(container.getConfig(), OR_FORECAST_SOLAR_API_KEY, null);
    }

    @Override
    public void start(Container container) throws Exception {
        if (forecastSolarApiKey == null) {
            LOG.fine("No value found for OR_FORECAST_SOLAR_API_KEY, ForecastSolarService won't start");
            return;
        }

        initClient();

        forecastSolarTarget = resteasyClient.get()
                .target("https://api.forecast.solar/" + forecastSolarApiKey + "/estimate");

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        // Load all enabled producer solar assets
        LOG.fine("Loading producer solar assets...");

        List<ElectricityProducerSolarAsset> electricityProducerSolarAssets = assetStorageService.findAll(
                        new AssetQuery()
                                .types(ElectricityProducerSolarAsset.class)
                )
                .stream()
                .map(asset -> (ElectricityProducerSolarAsset) asset)
                .filter(electricityProducerSolarAsset -> electricityProducerSolarAsset.isIncludeForecastSolarService().orElse(false))
                .toList();

        LOG.fine("Found includes producer solar asset count = " + electricityProducerSolarAssets.size());

        electricityProducerSolarAssets.forEach(this::startProcessing);

        clientEventService.addInternalSubscription(
                AttributeEvent.class,
                null,
                this::processAttributeEvent);
    }

    @Override
    public void stop(Container container) throws Exception {
        new ArrayList<>(calculationFutures.keySet()).forEach(this::stopProcessing);
    }

    protected static void initClient() {
        synchronized (resteasyClient) {
            if (resteasyClient.get() == null) {
                resteasyClient.set(createClient(org.openremote.container.Container.EXECUTOR_SERVICE));
            }
        }
    }

    protected void processAttributeEvent(AttributeEvent attributeEvent) {
        processElectricityProducerSolarAssetAttributeEvent(attributeEvent);
    }

    protected synchronized void processElectricityProducerSolarAssetAttributeEvent(AttributeEvent attributeEvent) {

        if (ElectricityProducerSolarAsset.POWER.getName().equals(attributeEvent.getName())
                || ElectricityProducerSolarAsset.POWER_FORECAST.getName().equals(attributeEvent.getName())) {
            // These are updated by this service
            return;
        }

        if (attributeEvent.getName().equals(ElectricityProducerSolarAsset.INCLUDE_FORECAST_SOLAR_SERVICE.getName())) {
            boolean enabled = (Boolean)attributeEvent.getValue().orElse(false);
            if (enabled && calculationFutures.containsKey(attributeEvent.getId())) {
                // Nothing to do here
                return;
            } else if (!enabled && !calculationFutures.containsKey(attributeEvent.getId())) {
                // Nothing to do here
                return;
            }

            LOG.fine("Processing producer solar asset attribute event: " + attributeEvent);
            stopProcessing(attributeEvent.getId());

            // Get latest asset from storage
            ElectricityProducerSolarAsset asset = (ElectricityProducerSolarAsset) assetStorageService.find(attributeEvent.getId());

            if (asset != null && asset.isIncludeForecastSolarService().orElse(false)) {
                startProcessing(asset);
            }
        }

        if (attributeEvent.getName().equals(ElectricityProducerSolarAsset.SET_ACTUAL_SOLAR_VALUE_WITH_FORECAST.getName())) {
            // Get latest asset from storage
            ElectricityProducerSolarAsset asset = (ElectricityProducerSolarAsset) assetStorageService.find(attributeEvent.getId());

            // Check if power is currently zero and set it if power forecast has an value
            if (asset.getPower().orElse(0d) == 0d && asset.getPowerForecast().orElse(0d) != 0d) {
                assetProcessingService.sendAttributeEvent(new AttributeEvent(asset.getId(), ElectricityProducerSolarAsset.POWER, asset.getPowerForecast().orElse(0d)), getClass().getSimpleName());
            }
        }
    }

    protected void processAssetChange(PersistenceEvent<ElectricityProducerSolarAsset> persistenceEvent) {
        LOG.fine("Processing producer solar asset change: " + persistenceEvent);
        stopProcessing(persistenceEvent.getEntity().getId());

        if (persistenceEvent.getCause() != PersistenceEvent.Cause.DELETE) {
            if (persistenceEvent.getEntity().isIncludeForecastSolarService().orElse(false)
                    && persistenceEvent.getEntity().getLocation().isPresent()) {
                startProcessing(persistenceEvent.getEntity());
            }
        }
    }

    protected void startProcessing(ElectricityProducerSolarAsset electricityProducerSolarAsset) {
        LOG.fine("Starting calculation for producer solar asset: " + electricityProducerSolarAsset);
        calculationFutures.put(electricityProducerSolarAsset.getId(), executorService.scheduleAtFixedRate(() -> {
            processSolarData(electricityProducerSolarAsset);
        }, 0, 1, TimeUnit.HOURS));
    }

    protected void stopProcessing(String electricityProducerSolarAssetId) {
        ScheduledFuture<?> scheduledFuture = calculationFutures.remove(electricityProducerSolarAssetId);
        if (scheduledFuture != null) {
            LOG.fine("Stopping calculation for producer solar asset: " + electricityProducerSolarAssetId);
            scheduledFuture.cancel(false);
        }
    }

    protected void processSolarData(ElectricityProducerSolarAsset electricityProducerSolarAsset) {
        Optional<Double> lat = electricityProducerSolarAsset.getAttribute(Asset.LOCATION).flatMap(attr -> attr.getValue().map(GeoJSONPoint::getY));
        Optional<Double> lon = electricityProducerSolarAsset.getAttribute(Asset.LOCATION).flatMap(attr -> attr.getValue().map(GeoJSONPoint::getX));
        Optional<Integer> pitch = electricityProducerSolarAsset.getPanelPitch();
        Optional<Integer> azimuth = electricityProducerSolarAsset.getPanelAzimuth();
        Optional<Double> kwp = electricityProducerSolarAsset.getPowerExportMax();
        if (lat.isPresent() && lon.isPresent() && pitch.isPresent() && azimuth.isPresent() && kwp.isPresent()) {
            try (Response response = forecastSolarTarget
                    .path(String.format("%f/%f/%d/%d/%f", lat.get(), lon.get(), pitch.get(), azimuth.get(), kwp.get()))
                    .request()
                    .build("GET")
                    .invoke()) {
                if (response != null && response.getStatus() == 200) {
                    EstimateResponse responseModel = response.readEntity(EstimateResponse.class);
                    if (responseModel != null) {
                        // Forecast date time is ISO8601 without 'T' so needs special formatter
                        LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli(timerService.getCurrentTimeMillis()), ZoneId.systemDefault());
                        LocalDateTime previousTimestamp = null;
                        boolean setActualValuePower = electricityProducerSolarAsset.isSetActualSolarValueWithForecast().orElse(false);
                        boolean setActualValueForecastPower = true;

                        for (Map.Entry<String, Double> wattItem : responseModel.result.watts.entrySet()) {
                            LocalDateTime timestamp = LocalDateTime.parse(wattItem.getKey(), ISO_LOCAL_DATE_TIME_WITHOUT_T);

                            assetPredictedDatapointService.updateValue(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER_FORECAST.getName(), -wattItem.getValue() / 1000, timestamp);
                            assetPredictedDatapointService.updateValue(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER.getName(), -wattItem.getValue() / 1000, timestamp);

                            if (setActualValueForecastPower && timestamp.isAfter(now)) {
                                assetProcessingService.sendAttributeEvent(new AttributeEvent(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER_FORECAST, -wattItem.getValue() / 1000), getClass().getSimpleName());
                                setActualValueForecastPower = false;

                                if (setActualValuePower) {
                                    assetProcessingService.sendAttributeEvent(new AttributeEvent(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER, -wattItem.getValue() / 1000), getClass().getSimpleName());
                                    setActualValuePower = false;
                                }
                            }
                            if (previousTimestamp != null && !previousTimestamp.toLocalDate().equals(timestamp.toLocalDate())) {
                                while (previousTimestamp.isBefore(timestamp)) {
                                    previousTimestamp = previousTimestamp.plusMinutes(15);
                                    assetPredictedDatapointService.updateValue(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER_FORECAST.getName(), -wattItem.getValue() / 1000, previousTimestamp);
                                    assetPredictedDatapointService.updateValue(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER.getName(), -wattItem.getValue() / 1000, previousTimestamp);
                                }
                            }
                            previousTimestamp = timestamp;
                        }
                        rulesService.fireDeploymentsWithPredictedDataForAsset(electricityProducerSolarAsset.getId());
                    }
                } else {
                    StringBuilder message = new StringBuilder("Unknown");
                    if (response != null) {
                        message.setLength(0);
                        message.append("Status ");
                        message.append(response.getStatus());
                        message.append(" - ");
                        message.append(response.readEntity(String.class));
                    }
                    LOG.warning("Request failed: " + message);
                }
            } catch (Throwable e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    LOG.log(Level.SEVERE, "Exception when requesting forecast solar data", e.getCause());
                } else {
                    LOG.log(Level.SEVERE, "Exception when requesting forecast solar data", e);
                }
            }
        } else {
            LOG.warning(String.format("Asset %s doesn't have all needed attributes filled in", electricityProducerSolarAsset.getId()));
        }
    }
}
