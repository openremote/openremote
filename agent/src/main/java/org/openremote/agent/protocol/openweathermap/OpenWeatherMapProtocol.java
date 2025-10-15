/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.openweathermap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.syslog.SyslogCategory;

import jakarta.ws.rs.core.Response;
import org.openremote.model.util.UniqueIdentifierGenerator;

import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.value.MetaItemType.AGENT_LINK;

/**
 * Protocol for the OpenWeatherMap API (One Call 3.0 API)
 * 
 * This protocol periodically retrieves the weather data for all linked
 * attributes grouped by their asset, and using the asset location as the
 * coordinates for the API call.
 * 
 * The linked attributes have their current value updated based on the returned
 * current weather data. Additionally, the linked attributes have their
 * predicted values updated based on the returned forecasted weather data.
 * (Hourly & Daily)
 * 
 */
public class OpenWeatherMapProtocol extends AbstractProtocol<OpenWeatherMapAgent, OpenWeatherMapAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, OpenWeatherMapProtocol.class);
    public static final String PROTOCOL_DISPLAY_NAME = "OpenWeatherMap";
    private static final AtomicReference<ResteasyClient> client = new AtomicReference<>();

    // Initial delay to allow system to populate agent links
    private static final int INITIAL_POLLING_DELAY_MILLIS = 3000; // 3 seconds
    private static final int DEFAULT_POLLING_MILLIS = 3600000; // 1 hour

    protected ScheduledFuture<?> pollingFuture;

    public OpenWeatherMapProtocol(OpenWeatherMapAgent agent) {
        super(agent);
        initClient();
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "https://openweathermap.org/";
    }

    @Override
    protected void doStart(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.CONNECTING);

        if (agent.getAttributes().getValue(OpenWeatherMapAgent.ATTRIBUTION).isEmpty()) {
            setOpenWeatherMapAttribution();
        }

        if (agent.getAPIKey().isEmpty()) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.warning("API key is not set");
            return;
        }

        if (!healthCheck()) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.warning("Could not reach OpenWeatherMap API, either API is unavailable or API key is invalid");
            return;
        }

        // Schedule the polling task
        int pollingMillis = agent.getPollingMillis().orElse(DEFAULT_POLLING_MILLIS);
        pollingFuture = scheduledExecutorService.scheduleAtFixedRate(this::updateWeatherData, INITIAL_POLLING_DELAY_MILLIS, pollingMillis, TimeUnit.MILLISECONDS);

        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        // Cancel the polling task
        if (pollingFuture != null) {
            pollingFuture.cancel(true);
        }
    }

    @Override
    public boolean onAgentAttributeChanged(AttributeEvent event) {

        // handle the provision weather asset 'button'
        if (event.getName().equals(OpenWeatherMapAgent.PROVISION_WEATHER_ASSET.getName())) {
            boolean provisionWeatherAsset = (Boolean) event.getValue().orElse(false);
            if (provisionWeatherAsset) {
                provisionWeatherAsset();
                sendAttributeEvent(new AttributeEvent(agent.getId(), OpenWeatherMapAgent.PROVISION_WEATHER_ASSET.getName(), false));
            }

            return false;
        }

        return super.onAgentAttributeChanged(event);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, OpenWeatherMapAgentLink agentLink) throws RuntimeException {
        // Do nothing
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, OpenWeatherMapAgentLink agentLink) {
        // Do nothing
    }

    @Override
    protected void doLinkedAttributeWrite(OpenWeatherMapAgentLink agentLink, AttributeEvent event, Object processedValue) {
        // Do nothing
    }

    /**
     * Update the agent's linked attributes with weather data from the
     * OpenWeatherMap API
     * 
     * This method groups all linked attributes by their Asset ID and then retrieves
     * the weather data for each asset based on its location
     */
    protected void updateWeatherData() {
        LOG.fine("Retrieving weather data from OpenWeatherMap");

        if (getLinkedAttributes().isEmpty()) {
            LOG.fine("No linked attributes found, skipping weather data retrieval");
            return;
        }

        // <assetId, List<AttributeRef>>
        Map<String, List<AttributeRef>> assetGroups = new HashMap<>();

        // Group the attributes by asset ID
        getLinkedAttributes().forEach((attributeRef, attribute) -> assetGroups.computeIfAbsent(attributeRef.getId(), k -> new ArrayList<>()).add(attributeRef));

        assetGroups.forEach((assetId, attributeRefs) -> {
            Asset<?> asset = assetService.findAsset(assetId);
            if (asset == null) {
                LOG.log(Level.WARNING, () -> "Asset not found or location not set for asset: " + assetId);
                return;
            }

            GeoJSONPoint location = asset.getLocation().orElse(null);
            if (location == null) {
                LOG.log(Level.WARNING, () -> "Location not set for asset: " + assetId);
                return;
            }

            // Fetch the weather data for the asset location
            OpenWeatherMapResponse weatherData = fetchWeatherData(buildApiUrl(location.getY(), location.getX()));

            if (weatherData != null) {
                // Update attributes with current weather data
                try {
                    updateAttributes(attributeRefs, weatherData.getCurrent());
                } catch (Exception e) {
                    LOG.log(Level.WARNING, e, () -> "Failed to update attributes: " + attributeRefs);
                }

                // Update predicted data points with weather forecast
                try {
                    updatePredictedDataPoints(attributeRefs, weatherData.getDaily()); // write daily forecast, up to 8 days
                    updatePredictedDataPoints(attributeRefs, weatherData.getHourly()); // write hourly forecast, up to 48 hours
                } catch (Exception e) {
                    LOG.log(Level.WARNING, e, () -> "Failed to write predicted data points: " + attributeRefs);
                }
            }
        });
    }

    /**
     * Fetch the weather data from the OpenWeatherMap API for the given API URL
     * 
     * @param apiUrl the API URL
     * @return the OpenWeatherMapResponse from the API
     */
    protected OpenWeatherMapResponse fetchWeatherData(String apiUrl) {
        try (Response response = client.get().target(apiUrl).request().get()) {
            if (response.getStatus() == 200) {
                return response.readEntity(OpenWeatherMapResponse.class);
            } else {
                setConnectionStatus(ConnectionStatus.ERROR);
                LOG.warning("API request failed with status: " + response.getStatus());
                return null;
            }
        } catch (Exception e) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.log(Level.WARNING, e, () -> "Failed to fetch weather data");
            return null;
        }
    }

    /**
     * Update values of the linked attributes based on the given weather datapoint
     * 
     * @param attributeRefs list of attribute references
     * @param currentWeatherDatapoint the current weather datapoint
     */
    protected void updateAttributes(List<AttributeRef> attributeRefs, OpenWeatherMapResponse.WeatherDatapoint currentWeatherDatapoint) {
        for (AttributeRef attributeRef : attributeRefs) {
            OpenWeatherMapField field = getWeatherFieldType(attributeRef);

            // Get the corresponding `field` value from the weather data object

            Object value = getWeatherFieldValue(currentWeatherDatapoint, field);

            if (value == null) {
                LOG.log(Level.WARNING, () -> "Field or value is null for attribute: " + attributeRef);
                continue;
            }

            updateLinkedAttribute(attributeRef, value);
        }
    }

    /**
     * Write the predicted data points for the given attributes and weather data
     * 
     * @param attributeRefs list of attribute references
     * @param weatherDatapoints list of weather datapoints
     */
    protected void updatePredictedDataPoints(List<AttributeRef> attributeRefs, List<OpenWeatherMapResponse.WeatherDatapoint> weatherDatapoints) {
        for (AttributeRef attributeRef : attributeRefs) {

            if (weatherDatapoints == null || weatherDatapoints.isEmpty()) {
                LOG.log(Level.WARNING, () -> "Weather data points are null or empty for attribute: " + attributeRef);
                continue;
            }

            // Build the predicted datapoints
            List<ValueDatapoint<?>> valuesAndTimestamps = buildValueDatapoints(attributeRef, weatherDatapoints);

            // Update the predicted datapoints for the attribute reference
            predictedDatapointService.updateValues(attributeRef.getId(), attributeRef.getName(), valuesAndTimestamps);
        }
    }

    /**
     * Builds a list of ValueDatapoints based on the provided weather data and
     * attribute reference
     * 
     * @param attributeRef the attribute reference
     * @param weatherData the weather data
     * @return a list of ValueDatapoints
     */
    protected List<ValueDatapoint<?>> buildValueDatapoints(AttributeRef attributeRef, List<OpenWeatherMapResponse.WeatherDatapoint> weatherData) {
        List<ValueDatapoint<?>> values = new ArrayList<>();
        OpenWeatherMapField field = getWeatherFieldType(attributeRef);

        // Populate the values and timestamps for the attribute
        for (OpenWeatherMapResponse.WeatherDatapoint weatherDataPoint : weatherData) {
            Object value = getWeatherFieldValue(weatherDataPoint, field);
            values.add(new ValueDatapoint<>(toMillis(weatherDataPoint.getTimestamp()), value));
        }

        return values;
    }

    /**
     * Get the corresponding `field` value from the weather data object
     * 
     * @param weatherData the weather datapoint
     * @param field the field to get the value from
     * @return the corresponding `field` value
     */
    protected Object getWeatherFieldValue(OpenWeatherMapResponse.WeatherDatapoint weatherData, OpenWeatherMapField field) {
        switch (field) {
        case TEMPERATURE:
            return weatherData.getTemperature();
        case ATMOSPHERIC_PRESSURE:
            return weatherData.getPressure();
        case HUMIDITY_PERCENTAGE:
            return weatherData.getHumidity();
        case CLOUD_COVERAGE:
            return weatherData.getClouds();
        case WIND_SPEED:
            return weatherData.getWindSpeed();
        case WIND_DIRECTION_DEGREES:
            return weatherData.getWindDegrees();
        case WIND_GUST_SPEED:
            return weatherData.getWindGust();
        case PROBABILITY_OF_PRECIPITATION:
            return weatherData.getPop();
        case ULTRAVIOLET_INDEX:
            return weatherData.getUvi();
        case RAIN_AMOUNT:
            return weatherData.getRain();
        default:
            return null;
        }
    }

    /**
     * Get the agent link OpenWeatherMapField for the given attribute reference
     * 
     * @param attributeRef the attribute reference
     * @return the OpenWeatherMapField
     */
    protected OpenWeatherMapField getWeatherFieldType(AttributeRef attributeRef) {
        Attribute<?> attribute = linkedAttributes.get(attributeRef);
        OpenWeatherMapAgentLink agentLink = agent.getAgentLink(attribute);
        return agentLink.getField();
    }

    /**
     * Build the OpenWeatherMap API URL for the given latitude and longitude (One
     * Call 3.0 API)
     * 
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the OpenWeatherMap API URL
     */
    public String buildApiUrl(double latitude, double longitude) {
        String apiKey = agent.getAPIKey().orElseThrow(() -> new IllegalStateException("API key is not set"));
        return "https://api.openweathermap.org/data/3.0/onecall?lat=" + latitude + "&lon=" + longitude + "&units=metric&exclude=alerts,minutely&appid="
                + apiKey;
    }

    /**
     * Perform a health check by sending a request to the OpenWeatherMap API
     * 
     * @return true if the health check is successful, false otherwise
     */
    protected boolean healthCheck() {
        String apiUrl = buildApiUrl(0, 0);
        try (Response response = client.get().target(apiUrl).request().get()) {
            if (response.getStatus() != 200) {
                LOG.warning("Health check failed with status: " + response.getStatus());
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> "Failed to perform health check");
            return false;
        }
    }

    /**
     * Provision a weather asset with the appropriate agent links, location needs to
     * be manually configured after provisioning
     */
    protected WeatherAsset provisionWeatherAsset() {
        WeatherAsset weatherAsset = new WeatherAsset("Weather");
        weatherAsset.setParentId(agent.getId());
        weatherAsset.setRealm(agent.getRealm());
        weatherAsset.setId(UniqueIdentifierGenerator.generateId());

        // Temperature
        weatherAsset.getAttribute(WeatherAsset.TEMPERATURE).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.TEMPERATURE))));

        // Humidity
        weatherAsset.getAttribute(WeatherAsset.HUMIDITY).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.HUMIDITY_PERCENTAGE))));

        // Atmospheric Pressure
        weatherAsset.getAttribute(WeatherAsset.ATMOSPHERIC_PRESSURE).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.ATMOSPHERIC_PRESSURE))));

        // Wind Speed
        weatherAsset.getAttribute(WeatherAsset.WIND_SPEED).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.WIND_SPEED))));

        // Wind Direction
        weatherAsset.getAttribute(WeatherAsset.WIND_DIRECTION).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.WIND_DIRECTION_DEGREES))));

        // Wind Gust Speed
        weatherAsset.getAttribute(WeatherAsset.WIND_GUST_SPEED).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.WIND_GUST_SPEED))));

        // Cloud Coverage
        weatherAsset.getAttribute(WeatherAsset.CLOUD_COVERAGE).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.CLOUD_COVERAGE))));

        // Probability of Precipitation
        weatherAsset.getAttribute(WeatherAsset.PROBABILITY_OF_PRECIPITATION).ifPresent(attribute -> attribute.addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.PROBABILITY_OF_PRECIPITATION))));

        // Rain Amount
        weatherAsset.getAttribute(WeatherAsset.RAINFALL).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.RAIN_AMOUNT))));

        // Ultraviolet Index
        weatherAsset.getAttribute(WeatherAsset.UV_INDEX).ifPresent(attribute -> attribute
                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, new OpenWeatherMapAgentLink(agent.getId()).setField(OpenWeatherMapField.ULTRAVIOLET_INDEX))));

        return assetService.mergeAsset(weatherAsset);
    }

    /**
     * Set the required attribution for the OpenWeatherMap API
     */
    protected void setOpenWeatherMapAttribution() {
        sendAttributeEvent(new AttributeEvent(agent.getId(), OpenWeatherMapAgent.ATTRIBUTION.getName(),
                "Weather data provided by OpenWeather (https://openweathermap.org/)"));
    }

    protected static void initClient() {
        synchronized (client) {
            if (client.get() == null) {
                client.set(createClient(org.openremote.container.Container.SCHEDULED_EXECUTOR));
            }
        }
    }

    protected long toMillis(long timestamp) {
        return timestamp * 1000;
    }

}
