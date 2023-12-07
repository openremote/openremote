package org.openremote.manager.energy;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.core.Response;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.rules.RulesService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.impl.ElectricityProducerAsset;
import org.openremote.model.asset.impl.ElectricityProducerWindAsset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.syslog.SyslogCategory.DATA;

/**
 * Calculates power generation for {@link ElectricityProducerWindAsset}.
 */
public class ForecastWindService extends RouteBuilder implements ContainerService {

    protected static class WeatherForecastResponseModel {

        protected WeatherForecastModel current;

        @JsonProperty("hourly")
        protected WeatherForecastModel[] list;

        public WeatherForecastModel[] getList() {
            return list;
        }
    }

    protected static class WeatherForecastModel {

        /**
         * Seconds
         */
        @JsonProperty("dt")
        protected long timestamp;

        @JsonProperty("temp")
        protected double tempature;

        @JsonProperty("humidity")
        protected int humidity;

        @JsonProperty("wind_speed")
        protected double windSpeed;

        @JsonProperty("wind_deg")
        protected int windDirection;

        @JsonProperty("uvi")
        protected double uv;

        public long getTimestamp() {
            return timestamp * 1000;
        }

        public double getTempature() {
            return tempature;
        }

        public int getHumidity() {
            return humidity;
        }

        public double getWindSpeed() {
            return windSpeed;
        }

        public int getWindDirection() {
            return windDirection;
        }

        public double getUv() {
            return uv;
        }
    }

    public static final String OR_OPEN_WEATHER_API_APP_ID = "OR_OPEN_WEATHER_API_APP_ID";

    protected static final Logger LOG = SyslogCategory.getLogger(DATA, ForecastWindService.class.getName());
    protected static final AtomicReference<ResteasyClient> resteasyClient = new AtomicReference<>();
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected GatewayService gatewayService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected ClientEventService clientEventService;
    protected ScheduledExecutorService executorService;
    protected RulesService rulesService;
    private ResteasyWebTarget weatherForecastWebTarget;
    private String openWeatherAppId;

    private final Map<String, ScheduledFuture<?>> calculationFutures = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
                .routeId("Persistence-ForecastWind")
                .filter(isPersistenceEventForEntityType(ElectricityProducerWindAsset.class))
                .filter(isNotForGateway(gatewayService))
                .process(exchange -> processAssetChange((PersistenceEvent<ElectricityProducerWindAsset>) exchange.getIn().getBody(PersistenceEvent.class)));
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

        openWeatherAppId = getString(container.getConfig(), OR_OPEN_WEATHER_API_APP_ID, null);
    }

    @Override
    public void start(Container container) throws Exception {
        if (openWeatherAppId == null) {
            LOG.fine("No value found for OR_OPEN_WEATHER_API_APP_ID, ForecastWindService won't start");
            return;
        }

        initClient();

        weatherForecastWebTarget = resteasyClient.get()
                .target("https://api.openweathermap.org/data/2.5")
                .queryParam("units", "metric")
                .queryParam("exclude", "minutely,daily,alerts")
                .queryParam("appid", openWeatherAppId);

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        // Load all enabled producer wind assets
        LOG.fine("Loading producer wind assets...");

        List<ElectricityProducerWindAsset> electricityProducerWindAssets = assetStorageService.findAll(
                        new AssetQuery()
                                .types(ElectricityProducerWindAsset.class)
                )
                .stream()
                .map(asset -> (ElectricityProducerWindAsset) asset)
                .filter(electricityProducerWindAsset -> electricityProducerWindAsset.isIncludeForecastWindService().orElse(false)
                        && electricityProducerWindAsset.getLocation().isPresent())
                .toList();

        LOG.fine("Found includes producer wind asset count = " + electricityProducerWindAssets.size());

        electricityProducerWindAssets.forEach(this::startCalculation);

        clientEventService.addInternalSubscription(
                AttributeEvent.class,
                null,
                this::processAttributeEvent);
    }

    @Override
    public void stop(Container container) throws Exception {
        new ArrayList<>(calculationFutures.keySet()).forEach(this::stopCalculation);
    }

    protected static void initClient() {
        synchronized (resteasyClient) {
            if (resteasyClient.get() == null) {
                resteasyClient.set(createClient(org.openremote.container.Container.EXECUTOR_SERVICE));
            }
        }
    }

    protected void processAttributeEvent(AttributeEvent attributeEvent) {
        processElectricityProducerWindAssetAttributeEvent(attributeEvent);
    }

    protected synchronized void processElectricityProducerWindAssetAttributeEvent(AttributeEvent attributeEvent) {

        if (ElectricityProducerWindAsset.POWER.getName().equals(attributeEvent.getName())
                || ElectricityProducerWindAsset.POWER_FORECAST.getName().equals(attributeEvent.getName())) {
            // These are updated by this service
            return;
        }

        if (attributeEvent.getName().equals(ElectricityProducerWindAsset.INCLUDE_FORECAST_WIND_SERVICE.getName())) {
            boolean enabled = (Boolean)attributeEvent.getValue().orElse(false);
            if (enabled && calculationFutures.containsKey(attributeEvent.getId())) {
                // Nothing to do here
                return;
            } else if (!enabled && !calculationFutures.containsKey(attributeEvent.getId())) {
                // Nothing to do here
                return;
            }

            LOG.fine("Processing producer wind asset attribute event: " + attributeEvent);
            stopCalculation(attributeEvent.getId());

            // Get latest asset from storage
            ElectricityProducerWindAsset asset = (ElectricityProducerWindAsset) assetStorageService.find(attributeEvent.getId());

            if (asset != null && asset.isIncludeForecastWindService().orElse(false) && asset.getLocation().isPresent()) {
                startCalculation(asset);
            }
        }

        if (attributeEvent.getName().equals(ElectricityProducerWindAsset.SET_ACTUAL_WIND_VALUE_WITH_FORECAST.getName())) {
            // Get latest asset from storage
            ElectricityProducerWindAsset asset = (ElectricityProducerWindAsset) assetStorageService.find(attributeEvent.getId());

            // Check if power is currently zero and set it if power forecast has an value
            if (asset.getPower().orElse(0d) == 0d && asset.getPowerForecast().orElse(0d) != 0d) {
                assetProcessingService.sendAttributeEvent(new AttributeEvent(asset.getId(), ElectricityProducerWindAsset.POWER, asset.getPowerForecast().orElse(0d)), getClass().getSimpleName());
            }
        }
    }

    protected void processAssetChange(PersistenceEvent<ElectricityProducerWindAsset> persistenceEvent) {
        LOG.fine("Processing producer wind asset change: " + persistenceEvent);
        stopCalculation(persistenceEvent.getEntity().getId());

        if (persistenceEvent.getCause() != PersistenceEvent.Cause.DELETE) {
            if (persistenceEvent.getEntity().isIncludeForecastWindService().orElse(false)
                    && persistenceEvent.getEntity().getLocation().isPresent()) {
                startCalculation(persistenceEvent.getEntity());
            }
        }
    }

    protected void startCalculation(ElectricityProducerWindAsset electricityProducerWindAsset) {
        LOG.fine("Starting calculation for producer wind asset: " + electricityProducerWindAsset);
        calculationFutures.put(electricityProducerWindAsset.getId(), executorService.scheduleAtFixedRate(() -> {
            processWeatherData(electricityProducerWindAsset);
        }, 0, 1, TimeUnit.HOURS));
    }

    protected void stopCalculation(String electricityProducerWindAssetId) {
        ScheduledFuture<?> scheduledFuture = calculationFutures.remove(electricityProducerWindAssetId);
        if (scheduledFuture != null) {
            LOG.fine("Stopping calculation for producer wind asset: " + electricityProducerWindAssetId);
            scheduledFuture.cancel(false);
        }
    }

    protected void processWeatherData(ElectricityProducerWindAsset electricityProducerWindAsset) {
        try (Response response = weatherForecastWebTarget
                .path("onecall")
                .queryParam("lat", electricityProducerWindAsset.getLocation().get().getY())
                .queryParam("lon", electricityProducerWindAsset.getLocation().get().getX())
                .request()
                .build("GET")
                .invoke()) {
            if (response != null && response.getStatus() == 200) {

                WeatherForecastResponseModel weatherForecastResponseModel = response.readEntity(WeatherForecastResponseModel.class);

                double currentPower = calculatePower(electricityProducerWindAsset, weatherForecastResponseModel.current);

                assetProcessingService.sendAttributeEvent(new AttributeEvent(electricityProducerWindAsset.getId(), ElectricityProducerAsset.POWER_FORECAST.getName(), -currentPower), getClass().getSimpleName());

                if (electricityProducerWindAsset.isSetActualWindValueWithForecast().orElse(false)) {
                    assetProcessingService.sendAttributeEvent(new AttributeEvent(electricityProducerWindAsset.getId(), ElectricityProducerAsset.POWER.getName(), -currentPower), getClass().getSimpleName());
                }

                for (WeatherForecastModel weatherForecastModel : weatherForecastResponseModel.getList()) {
                    double powerForecast = calculatePower(electricityProducerWindAsset, weatherForecastModel);

                    LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(weatherForecastModel.getTimestamp()), ZoneId.systemDefault());
                    assetPredictedDatapointService.updateValue(electricityProducerWindAsset.getId(), ElectricityProducerAsset.POWER_FORECAST.getName(), -powerForecast, timestamp);
                    assetPredictedDatapointService.updateValue(electricityProducerWindAsset.getId(), ElectricityProducerAsset.POWER.getName(), -powerForecast, timestamp);

                    for (int i = 0; i < 3; i++) {
                        timestamp = timestamp.plusMinutes(15);
                        assetPredictedDatapointService.updateValue(electricityProducerWindAsset.getId(), ElectricityProducerAsset.POWER_FORECAST.getName(), -powerForecast, timestamp);
                        assetPredictedDatapointService.updateValue(electricityProducerWindAsset.getId(), ElectricityProducerAsset.POWER.getName(), -powerForecast, timestamp);
                    }
                }

                rulesService.fireDeploymentsWithPredictedDataForAsset(electricityProducerWindAsset.getId());
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
                LOG.log(Level.SEVERE, "Exception when requesting openweathermap data", e.getCause());
            } else {
                LOG.log(Level.SEVERE, "Exception when requesting openweathermap data", e);
            }
        }
    }

    protected double calculatePower(ElectricityProducerWindAsset electricityProducerWindAsset, WeatherForecastModel weatherForecastModel) {
        double windSpeed = weatherForecastModel.getWindSpeed();
        double powerForecast = 0;
        double windSpeedMin = electricityProducerWindAsset.getWindSpeedMin().orElse(0d);
        double windSpeedMax = electricityProducerWindAsset.getWindSpeedMax().orElse(0d);
        double windSpeedReference = electricityProducerWindAsset.getWindSpeedReference().orElse(0d);
        double energyExportMax = electricityProducerWindAsset.getPowerExportMax().orElse(0d);

        if (windSpeed <= 0 || windSpeed < windSpeedMin) {
            powerForecast = 0;
        }
        if (windSpeedMin <= windSpeed && windSpeed <= windSpeedReference) {
            powerForecast = Math.pow((windSpeed / windSpeedReference), 2) * energyExportMax;
        }
        if (windSpeedReference < windSpeed && windSpeed <= windSpeedMax) {
            powerForecast = energyExportMax;
        }
        if (windSpeed > windSpeedMax) {
            powerForecast = 0;
        }
        if (powerForecast < 0) {
            powerForecast = 0;
        }
        return powerForecast;
    }
}
