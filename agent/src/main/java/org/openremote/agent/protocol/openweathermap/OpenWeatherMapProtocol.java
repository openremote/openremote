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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.syslog.SyslogCategory;

import jakarta.ws.rs.core.Response;

import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Protocol for the OpenWeatherMap API (One Call 3.0 API)
 * 
 * This protocol periodically retrieves the weather data for all linked
 * attributes grouped by their coordinates (Attribute Asset location) and makes
 * one API call per distinct location.
 * 
 * It then updates the attributes based on the returned weather data.
 * 
 */
public class OpenWeatherMapProtocol extends AbstractProtocol<OpenWeatherMapAgent, OpenWeatherMapAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, OpenWeatherMapProtocol.class);
    public static final String PROTOCOL_DISPLAY_NAME = "OpenWeatherMap";
    private static final AtomicReference<ResteasyClient> client = new AtomicReference<>();

    // Initial delay is used so the system has time to establish agent links
    private static final int INITIAL_MILLIS_DELAY = 5000; // 5 seconds
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

    /**
     * Get the OpenWeatherMap One Call API URL for the given latitude and longitude
     * 
     * @param latitude latitude of the location
     * @param longitude longitude of the location
     * @return the OpenWeatherMap One Call API URL
     */
    public String getOneCallApiUrl(double latitude, double longitude) {
        String apiKey = agent.getAPIKey().orElseThrow(() -> new IllegalStateException("API key is not set"));
        return "https://api.openweathermap.org/data/3.0/onecall?lat=" + latitude + "&lon=" + longitude + "&units=metric&exclude=alerts,minutely&appid="
                + apiKey;
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

        // Schedule a task to retrieve weather data periodically
        int pollingMillis = agent.getPollingMillis().orElse(DEFAULT_POLLING_MILLIS);
        pollingFuture = scheduledExecutorService.scheduleAtFixedRate(this::updateWeatherAttributes, INITIAL_MILLIS_DELAY, pollingMillis, TimeUnit.MILLISECONDS);

        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
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
                LOG.info("Provisioning weather asset");

                // Uncheck button
                sendAttributeEvent(new AttributeEvent(agent.getId(), OpenWeatherMapAgent.PROVISION_WEATHER_ASSET.getName(), false));
            }

            return false; // Return false since its not a configuration attribute
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
     * Update linked asset attributes with weather data
     */
    protected void updateWeatherAttributes() {
        LOG.fine("Retrieving weather data from OpenWeatherMap");

        if (getLinkedAttributes().isEmpty()) {
            LOG.fine("No linked attributes found, skipping weather data retrieval");
            return;
        }
        // <assetId, List<AttributeRef>>
        Map<String, List<AttributeRef>> assetGroups = new HashMap<>();

        // Group the attributes by asset ID
        getLinkedAttributes().forEach((attributeRef, attribute) -> assetGroups.computeIfAbsent(attributeRef.getId(), k -> new ArrayList<>()).add(attributeRef));


        // Retrieve the weather data for each asset, updating their linked attributes with the weather data
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

            // Fetch the weather data based on the asset location
            OpenWeatherMapResponse weatherData = fetchWeatherData(getOneCallApiUrl(location.getY(), location.getX()));

            if (weatherData != null) {
                updateAttributes(attributeRefs, weatherData);
            }
        });
    }

    /**
     * Fetch the weather data from the OpenWeatherMap API for the given API URL
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
            LOG.log(Level.SEVERE, e, () -> "Failed to fetch weather data");
            return null;
        }
    }

    /**
     * Update values of the linked attributes based on the given weather data
     */
    protected void updateAttributes(List<AttributeRef> attributeRefs, OpenWeatherMapResponse weatherData) {
        for (AttributeRef attributeRef : attributeRefs) {
            Attribute<?> attribute = linkedAttributes.get(attributeRef);

            // Get the agent link
            OpenWeatherMapAgentLink agentLink = agent.getAgentLink(attribute);
            OpenWeatherMapField field = agentLink.getField();

            // Extract the value from the weather entry based on the agent link field
            OpenWeatherMapResponse.WeatherEntry weatherEntry = weatherData.getCurrent();
            Object value = extractWeatherEntryFieldValue(weatherEntry, field);

            if (value == null) {
                LOG.log(Level.WARNING, () -> "Field or value is null for attribute: " + attributeRef);
                continue;
            }

            updateLinkedAttribute(attributeRef, value);
        }
    }

    /**
     * Extract the value from the weather entry based on the given field
     */
    protected Object extractWeatherEntryFieldValue(OpenWeatherMapResponse.WeatherEntry weatherEntry, OpenWeatherMapField field) {
        switch (field) {
        case TEMPERATURE:
            return weatherEntry.getTemperature();
        case ATMOSPHERIC_PRESSURE:
            return weatherEntry.getPressure();
        case HUMIDITY_PERCENTAGE:
            return weatherEntry.getHumidity();
        case CLOUD_COVERAGE:
            return weatherEntry.getClouds();
        case WIND_SPEED:
            return weatherEntry.getWindSpeed();
        case WIND_DIRECTION_DEGREES:
            return weatherEntry.getWindDegrees();
        case WIND_GUST_SPEED:
            return weatherEntry.getWindGust();
        case PROBABILITY_OF_PRECIPITATION:
            return weatherEntry.getPop();
        case ULTRAVIOLET_INDEX:
            return weatherEntry.getUvi();
        default:
            return null;
        }
    }

    /**
     * Perform a health check to the OpenWeatherMap API by retrieving null island
     * data
     */
    protected boolean healthCheck() {
        String apiUrl = getOneCallApiUrl(0, 0);
        try (Response response = client.get().target(apiUrl).request().get()) {
            if (response.getStatus() != 200) {
                LOG.warning("Health check failed with status: " + response.getStatus());
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e, () -> "Failed to perform health check");
            return false;
        }
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

}
