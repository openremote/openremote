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
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.asset.impl.ElectricityProducerSolarAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
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

    protected ScheduledExecutorService scheduledExecutorService;
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
    private final Map<String, ElectricityProducerSolarAsset> electricityProducerSolarAssetMap = new HashMap<>();

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
        scheduledExecutorService = container.getScheduledExecutor();
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
        LOG.fine("Loading electricity producer solar assets...");

        List<ElectricityProducerSolarAsset> electricityProducerSolarAssets = assetStorageService.findAll(
                        new AssetQuery()
                                .types(ElectricityProducerSolarAsset.class)
                )
                .stream()
                .map(asset -> (ElectricityProducerSolarAsset) asset)
                .filter(electricityProducerSolarAsset -> electricityProducerSolarAsset.isIncludeForecastSolarService().orElse(false))
                .toList();

        LOG.fine("Number of electricity producer solar assets with forecast enabled = " + electricityProducerSolarAssets.size());

        for (ElectricityProducerSolarAsset electricityProducerSolarAsset : electricityProducerSolarAssets) {
            electricityProducerSolarAssetMap.put(electricityProducerSolarAsset.getId(), electricityProducerSolarAsset);
            getSolarForecast(electricityProducerSolarAsset);
            updateSolarForecastAttribute(electricityProducerSolarAsset);
        }

        // Start forecast solar thread
        scheduledExecutorService.scheduleAtFixedRate(this::processSolarData, 1, 1, TimeUnit.MINUTES);

        clientEventService.addSubscription(
                AttributeEvent.class,
                new AssetFilter<AttributeEvent>().setAssetClasses(Collections.singletonList(ElectricityProducerSolarAsset.class)),
                this::processElectricityProducerSolarAssetAttributeEvent);
    }

    @Override
    public void stop(Container container) throws Exception {
        scheduledExecutorService.shutdown();
    }

    protected static void initClient() {
        synchronized (resteasyClient) {
            if (resteasyClient.get() == null) {
                resteasyClient.set(createClient(org.openremote.container.Container.EXECUTOR));
            }
        }
    }

    protected synchronized void processElectricityProducerSolarAssetAttributeEvent(AttributeEvent attributeEvent) {
        String attributeName = attributeEvent.getName();

        // These are updated by this service
        if (ElectricityProducerSolarAsset.POWER.getName().equals(attributeName)
                || ElectricityProducerSolarAsset.POWER_FORECAST.getName().equals(attributeName)) {
            return;
        }

        // Set power attribute value with power forecast attribute value
        if (attributeName.equals(ElectricityProducerSolarAsset.SET_ACTUAL_SOLAR_VALUE_WITH_FORECAST.getName())) {
            boolean enabled = (Boolean) attributeEvent.getValue().orElse(false);

            // Get latest asset from storage
            ElectricityProducerSolarAsset asset = (ElectricityProducerSolarAsset) assetStorageService.find(attributeEvent.getId());

            if (asset != null && enabled) {
                assetProcessingService.sendAttributeEvent(new AttributeEvent(asset.getId(), ElectricityProducerSolarAsset.POWER, asset.getPowerForecast().orElse(null)), getClass().getSimpleName());
            } else if (asset != null) {
                assetProcessingService.sendAttributeEvent(new AttributeEvent(asset.getId(), ElectricityProducerSolarAsset.POWER, null), getClass().getSimpleName());
            }
            return;
        }

        // Enable solar forecast
        if (attributeName.equals(ElectricityProducerSolarAsset.INCLUDE_FORECAST_SOLAR_SERVICE.getName())) {
            boolean enabled = (Boolean) attributeEvent.getValue().orElse(false);

            if (enabled && !electricityProducerSolarAssetMap.containsKey(attributeEvent.getId())) {
                LOG.info(String.format("Enabled solar forecast for ElectricityProducerSolarAsset: name='%s', ID='%s';", attributeEvent.getAssetName(), attributeEvent.getId()));

                // Get latest asset from storage
                ElectricityProducerSolarAsset asset = (ElectricityProducerSolarAsset) assetStorageService.find(attributeEvent.getId());

                if (asset != null) {
                    electricityProducerSolarAssetMap.put(asset.getId(), asset);
                    getSolarForecast(asset);
                    updateSolarForecastAttribute(asset);
                }
            } else if (!enabled && electricityProducerSolarAssetMap.containsKey(attributeEvent.getId())) {
                LOG.info(String.format("Disabled solar forecast for ElectricityProducerSolarAsset: name='%s', ID='%s';", attributeEvent.getAssetName(), attributeEvent.getId()));
                electricityProducerSolarAssetMap.remove(attributeEvent.getId());
            }
        }

        // Update solar forecast
        if (attributeName.equals(ElectricityProducerSolarAsset.PANEL_AZIMUTH.getName()) ||
                attributeName.equals(ElectricityProducerSolarAsset.PANEL_PITCH.getName()) ||
                attributeName.equals(ElectricityProducerSolarAsset.POWER_EXPORT_MAX.getName()) ||
                attributeName.equals(ElectricityProducerSolarAsset.LOCATION.getName())) {
            // Get latest asset from storage
            ElectricityProducerSolarAsset asset = (ElectricityProducerSolarAsset) assetStorageService.find(attributeEvent.getId());

            if (asset != null && asset.isIncludeForecastSolarService().orElse(false)) {
                ElectricityProducerSolarAsset assetPrevious = electricityProducerSolarAssetMap.get(asset.getId());

                String valueStr = attributeEvent.getValue().toString();
                String valuePreviousStr = assetPrevious.getAttributes().get(attributeEvent.getName()).flatMap(Attribute::getValue).toString();

                // Only update solar forecast on attribute value change
                if (!valueStr.equals(valuePreviousStr)) {
                    Object value = attributeEvent.getValue().orElse(null);

                    if (attributeName.equals(ElectricityProducerSolarAsset.PANEL_AZIMUTH.getName())) {
                        asset.setPanelAzimuth((Integer) value);
                    } else if (attributeName.equals(ElectricityProducerSolarAsset.PANEL_PITCH.getName())) {
                        asset.setPanelPitch((Integer) value);
                    } else if (attributeName.equals(ElectricityProducerSolarAsset.POWER_EXPORT_MAX.getName())) {
                        asset.setPowerExportMax((Double) value);
                    } else if (attributeName.equals(ElectricityProducerSolarAsset.LOCATION.getName())) {
                        asset.setLocation((GeoJSONPoint) value);
                    }

                    getSolarForecast(asset);
                    updateSolarForecastAttribute(asset);
                }
                electricityProducerSolarAssetMap.put(asset.getId(), asset);
            }
        }
    }

    protected void processAssetChange(PersistenceEvent<ElectricityProducerSolarAsset> persistenceEvent) {
        LOG.fine("Processing producer solar asset change: " + persistenceEvent);

        if (persistenceEvent.getCause() == PersistenceEvent.Cause.CREATE && persistenceEvent.getEntity().isIncludeForecastSolarService().orElse(false)) {
            electricityProducerSolarAssetMap.put(persistenceEvent.getEntity().getId(), persistenceEvent.getEntity());
            getSolarForecast(persistenceEvent.getEntity());
            updateSolarForecastAttribute(persistenceEvent.getEntity());
        } else if (persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE) {
            electricityProducerSolarAssetMap.remove(persistenceEvent.getEntity().getId());
        }
    }


    protected void processSolarData() {
        // Check if there are any electricity producer solar assets to process
        if (electricityProducerSolarAssetMap.isEmpty()) {
            return;
        }

        int currentMinute = LocalDateTime.now().getMinute();

        // Update solar forecast every hour
        if (currentMinute == 0) {
            electricityProducerSolarAssetMap.forEach((assetId, electricityProducerSolarAsset) -> getSolarForecast(electricityProducerSolarAsset));
        }

        // Update solar power forecast attribute every 15 minutes
        if ((currentMinute % 15) == 0) {
            electricityProducerSolarAssetMap.forEach((assetId, electricityProducerSolarAsset) -> updateSolarForecastAttribute(electricityProducerSolarAsset));
        }
    }

    protected void getSolarForecast(ElectricityProducerSolarAsset electricityProducerSolarAsset) {
        Optional<Double> lat = electricityProducerSolarAsset.getAttribute(Asset.LOCATION).flatMap(attr -> attr.getValue().map(GeoJSONPoint::getY));
        Optional<Double> lon = electricityProducerSolarAsset.getAttribute(Asset.LOCATION).flatMap(attr -> attr.getValue().map(GeoJSONPoint::getX));
        Optional<Integer> pitch = electricityProducerSolarAsset.getPanelPitch();
        Optional<Integer> azimuth = electricityProducerSolarAsset.getPanelAzimuth();
        Optional<Double> kwp = electricityProducerSolarAsset.getPowerExportMax();

        if (lat.isEmpty() || lon.isEmpty() || pitch.isEmpty() || azimuth.isEmpty() || kwp.isEmpty()) {
            LOG.warning(String.format("ElectricityProducerSolarAsset: name='%s', ID='%s' doesn't have all needed attributes filled in;" +
                            " latitude='%s', longitude='%s', panelAzimuth='%s', panelPitch='%s', powerExportMax='%s'",
                    electricityProducerSolarAsset.getName(), electricityProducerSolarAsset.getId(), lat, lon, azimuth, pitch, kwp));
            return;
        }

        try (Response response = forecastSolarTarget
                .path(String.format("%f/%f/%d/%d/%f", lat.get(), lon.get(), pitch.get(), azimuth.get(), kwp.get()))
                .request()
                .build("GET")
                .invoke()) {
            if (response != null && response.getStatus() == 200) {
                EstimateResponse responseModel = response.readEntity(EstimateResponse.class);

                if (responseModel != null) {
                    HashMap<LocalDateTime, Double> solarForecast = new HashMap<>();
                    HashMap<LocalDateTime, Double> solarForecastPrevious = new HashMap<>();

                    // Get previous solar forecast from database
                    List<ValueDatapoint> solarForecastListPrevious = assetPredictedDatapointService.getDatapoints(new AttributeRef(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER_FORECAST.getName()));

                    for (ValueDatapoint datapoint : solarForecastListPrevious) {
                        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(datapoint.getTimestamp()), ZoneId.systemDefault());
                        Double powerKiloWatt = (Double) datapoint.getValue();
                        solarForecastPrevious.put(dateTime, powerKiloWatt);
                    }

                    // Get start and end dateTime of solar forecast
                    String minKey = responseModel.result.watts.keySet().stream().min(String::compareTo).orElse("");
                    String maxKey = responseModel.result.watts.keySet().stream().max(String::compareTo).orElse("");
                    LocalDateTime startDateTime = LocalDateTime.parse(minKey, ISO_LOCAL_DATE_TIME_WITHOUT_T).toLocalDate().atStartOfDay();
                    LocalDateTime endDateTime = LocalDateTime.parse(maxKey, ISO_LOCAL_DATE_TIME_WITHOUT_T).toLocalDate().plusDays(1).atStartOfDay();

                    // Prepopulate solar forecast map with 15-minute intervals
                    for (LocalDateTime dateTime = startDateTime; !dateTime.isAfter(endDateTime); dateTime = dateTime.plusMinutes(15)) {
                        solarForecast.put(dateTime, 0.0);
                    }

                    // Add solar forecast to solar forecast map
                    for (Map.Entry<String, Double> wattItem : responseModel.result.watts.entrySet()) {
                        LocalDateTime dateTime = LocalDateTime.parse(wattItem.getKey(), ISO_LOCAL_DATE_TIME_WITHOUT_T);
                        Double powerKiloWatt = -wattItem.getValue() / 1000;
                        solarForecast.put(dateTime, powerKiloWatt);
                    }

                    LocalDateTime currentDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timerService.getCurrentTimeMillis()), ZoneId.systemDefault());

                    // Update solar forecast in database
                    solarForecast.forEach((dateTime, powerKiloWatt) -> {
                        if (dateTime.isAfter(currentDateTime) || solarForecastPrevious.get(dateTime) == null) {
                            assetPredictedDatapointService.updateValue(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER_FORECAST.getName(), powerKiloWatt, dateTime);
                            assetPredictedDatapointService.updateValue(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER.getName(), powerKiloWatt, dateTime);
                        }
                    });
                }
                rulesService.fireDeploymentsWithPredictedDataForAsset(electricityProducerSolarAsset.getId());
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
    }

    protected void updateSolarForecastAttribute(ElectricityProducerSolarAsset electricityProducerSolarAsset) {
        Optional<Double> lat = electricityProducerSolarAsset.getAttribute(Asset.LOCATION).flatMap(attr -> attr.getValue().map(GeoJSONPoint::getY));
        Optional<Double> lon = electricityProducerSolarAsset.getAttribute(Asset.LOCATION).flatMap(attr -> attr.getValue().map(GeoJSONPoint::getX));
        Optional<Integer> pitch = electricityProducerSolarAsset.getPanelPitch();
        Optional<Integer> azimuth = electricityProducerSolarAsset.getPanelAzimuth();
        Optional<Double> kwp = electricityProducerSolarAsset.getPowerExportMax();

        if (lat.isEmpty() || lon.isEmpty() || pitch.isEmpty() || azimuth.isEmpty() || kwp.isEmpty()) {
            return;
        }

        // Get solar forecast data-points for current 15 minute interval
        long currentTimeMillis = timerService.getCurrentTimeMillis();
        long startTimeMillis = currentTimeMillis - currentTimeMillis % (15 * 60000);
        long endTimeMillis = startTimeMillis + 15 * 60000;
        AssetDatapointAllQuery assetDatapointQuery = new AssetDatapointAllQuery(startTimeMillis, endTimeMillis);
        List<ValueDatapoint<?>> solarForecastDatapoints = assetPredictedDatapointService.queryDatapoints(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER_FORECAST.getName(), assetDatapointQuery);

        if (solarForecastDatapoints == null || solarForecastDatapoints.size() < 2) {
            LOG.warning(String.format("ElectricityProducerSolarAsset: name='%s', ID='%s' doesn't have a solar forecast", electricityProducerSolarAsset.getName(), electricityProducerSolarAsset.getId()));
            return;
        }

        ValueDatapoint<?> solarForecastDatapointMax = solarForecastDatapoints.getFirst();
        ValueDatapoint<?> solarForecastDatapointMin = solarForecastDatapoints.getLast();

        // Get current timestamp of power forecast attribute
        ElectricityProducerSolarAsset asset = (ElectricityProducerSolarAsset) assetStorageService.find(electricityProducerSolarAsset.getId());
        long powerForecastAttributeTimeMillis = asset.getAttributes().get(ElectricityProducerSolarAsset.POWER_FORECAST).flatMap(Attribute::getTimestamp).orElse(0L);

        // Update power forecast attribute value
        Double powerKiloWatt;
        long timeMillis;

        if (solarForecastDatapointMin.getTimestamp() > powerForecastAttributeTimeMillis) {
            powerKiloWatt = (Double) solarForecastDatapointMin.getValue();
            timeMillis = solarForecastDatapointMin.getTimestamp();
        } else {
            long upperTimestamp = solarForecastDatapointMax.getTimestamp();
            long lowerTimestamp = solarForecastDatapointMin.getTimestamp();
            Double upperValue = (Double) solarForecastDatapointMax.getValue();
            Double lowerValue = (Double) solarForecastDatapointMin.getValue();

            if (upperValue == null || lowerValue == null) {
                return;
            }

            // Interpolate value
            double factor = (double) (currentTimeMillis - lowerTimestamp) / (upperTimestamp - lowerTimestamp);
            double interpolatedValue = lowerValue + factor * (upperValue - lowerValue);
            powerKiloWatt = Math.round(interpolatedValue * 1000.0) / 1000.0;
            timeMillis = currentTimeMillis;
        }

        // Update attributes
        assetProcessingService.sendAttributeEvent(new AttributeEvent(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER_FORECAST.getName(), powerKiloWatt, timeMillis));

        if (electricityProducerSolarAsset.isSetActualSolarValueWithForecast().orElse(false)) {
            assetProcessingService.sendAttributeEvent(new AttributeEvent(electricityProducerSolarAsset.getId(), ElectricityProducerSolarAsset.POWER.getName(), powerKiloWatt, timeMillis));
        }
    }
}
