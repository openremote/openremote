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
        return "https://api.openweathermap.org/data/3.0/onecall?lat=" + latitude + "&lon=" + longitude + "&exclude=alerts&appid=" + apiKey;
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
        int pollingMillis = agent.getPollingMillis().orElse(3600000);
        pollingFuture = scheduledExecutorService.scheduleAtFixedRate(this::retrieveWeatherData, 0, pollingMillis, TimeUnit.MILLISECONDS);

        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    /**
     * Set the required attribution for the OpenWeatherMap API
     */
    protected void setOpenWeatherMapAttribution() {
        sendAttributeEvent(new AttributeEvent(agent.getId(), OpenWeatherMapAgent.ATTRIBUTION.getName(),
                "Weather data provided by OpenWeather (https://openweathermap.org/)"));
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (pollingFuture != null) {
            pollingFuture.cancel(true);
        }
    }

    @Override
    public boolean onAgentAttributeChanged(AttributeEvent event) {
        // handle provision weather asset 'button'
        if (event.getName().equals(OpenWeatherMapAgent.PROVISION_WEATHER_ASSET.getName())) {
            boolean provisionWeatherAsset = (Boolean) event.getValue().orElse(false);
            if (provisionWeatherAsset) {
                LOG.info("Provisioning weather asset");
                // uncheck the button
                sendAttributeEvent(new AttributeEvent(agent.getId(), OpenWeatherMapAgent.PROVISION_WEATHER_ASSET.getName(), false));
            }
            return false; // Not a configuration attribute
        }

        // Let the super method determine if this is a configuration attribute
        return super.onAgentAttributeChanged(event);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, OpenWeatherMapAgentLink agentLink) throws RuntimeException {
        // Should we call the API when a new link is introduced? Al though this
        // might cause frequent calls when setting up the agent
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
     * Retrieve the weather data for all linked attributes at once, grouping them by
     * their coordinates (Attribute Asset location) and making one API call per
     * unique location.
     */
    protected void retrieveWeatherData() {
        Map<AttributeRef, Attribute<?>> linkedAttributes = getLinkedAttributes();

        // <locationKey, attributeRefs>
        Map<String, List<AttributeRef>> locationGroups = new HashMap<>();

        // Collect all linked attributes and group them by their coordinates
        linkedAttributes.forEach((attributeRef, attribute) -> {
            Asset<?> asset = assetService.findAsset(attributeRef.getId());
            if (asset == null || asset.getLocation().isEmpty()) {
                LOG.warning("Asset not found or location not set for attribute: " + attributeRef);
                return;
            }
            GeoJSONPoint location = asset.getLocation().get();
            String key = location.getY() + "," + location.getX(); // lat, lon
            locationGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(attributeRef);
        });

        // Process each location group, making one API call per unique location
        locationGroups.forEach((key, attributeRefs) -> processLocationGroup(key, attributeRefs, linkedAttributes));
    }

    /**
     * Process a location group - makes one API call per unique location and updates
     * the attributes based on the weather data.
     * 
     * @param locationKey the location key
     * @param attributeRefs the attribute references
     * @param linkedAttributes the linked attributes
     */
    protected void processLocationGroup(String locationKey, List<AttributeRef> attributeRefs, Map<AttributeRef, Attribute<?>> linkedAttributes) {
        Asset<?> asset = assetService.findAsset(attributeRefs.get(0).getId());
        GeoJSONPoint location = asset.getLocation().orElse(null);
        if (location == null) {
            String key = locationKey;
            LOG.log(Level.WARNING, () -> "Location is null for location group: " + key);
            return;
        }
        String apiUrl = getOneCallApiUrl(location.getY(), location.getX());

        try (Response response = client.get().target(apiUrl).request().get()) {
            if (response.getStatus() == 200) {
                OpenWeatherMapResponse weatherData = response.readEntity(OpenWeatherMapResponse.class);
                updateAttributesFromWeatherData(attributeRefs, linkedAttributes, weatherData);
            } else {
                setConnectionStatus(ConnectionStatus.ERROR);
                LOG.warning("API request failed with status: " + response.getStatus());
            }
        } catch (Exception e) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.log(Level.SEVERE, e, () -> "Failed to fetch weather data for location: " + locationKey);
        }
    }

    /**
     * Update values of the linked attributes based on the weather data
     * 
     * @param attributeRefs the attribute references
     * @param linkedAttributes the linked attributes
     * @param weatherData the weather data
     */
    protected void updateAttributesFromWeatherData(List<AttributeRef> attributeRefs, Map<AttributeRef, Attribute<?>> linkedAttributes,
            OpenWeatherMapResponse weatherData) {
        for (AttributeRef attrRef : attributeRefs) {
            // Get the attribute
            Attribute<?> attr = linkedAttributes.get(attrRef);

            // Get the agent link
            OpenWeatherMapAgentLink agentLink = agent.getAgentLink(attr);
            OpenWeatherMapField field = agentLink.getField().orElse(null);
            Object value = field != null ? extractCurrentFieldValue(weatherData, field) : null;

            // Check if the field or value is null
            if (field == null || value == null) {
                AttributeRef ref = attrRef;
                LOG.log(Level.WARNING, () -> "Field or value is null for attribute: " + ref);
                continue;
            }

            // Update the value via updateLinkedAttribute
            updateLinkedAttribute(attrRef, value);
        }
    }

    /**
     * Extract the current weather value from the weather entry based on the given
     * field
     * 
     * @param openWeatherMapResponse the weather response
     * @param field the field
     * @return the value
     */
    protected Object extractCurrentFieldValue(OpenWeatherMapResponse openWeatherMapResponse, OpenWeatherMapField field) {
        // Index 0 = current weather entry
        OpenWeatherMapResponse.WeatherEntry weatherEntry = openWeatherMapResponse.getList().get(0);
        // Extract the value based on the field
        return extractFieldValueFromWeatherEntry(weatherEntry, field);
    }

    /**
     * Extract the value from the weather entry based on the given field
     * 
     * @param weatherEntry the weather entry
     * @param field the field
     * @return the value
     */
    protected Object extractFieldValueFromWeatherEntry(OpenWeatherMapResponse.WeatherEntry weatherEntry, OpenWeatherMapField field) {
        switch (field) {
        case TEMP:
            return weatherEntry.getMain().getTemperature();
        case TEMP_FEELS_LIKE:
            return weatherEntry.getMain().getFeelsLike();
        case TEMP_MIN:
            return weatherEntry.getMain().getTemperatureMin();
        case TEMP_MAX:
            return weatherEntry.getMain().getTemperatureMax();
        case PRESSURE:
            return weatherEntry.getMain().getPressure();
        case HUMIDITY:
            return weatherEntry.getMain().getHumidity();
        case CLOUD_COVERAGE:
            return weatherEntry.getClouds().getAll();
        case WIND_SPEED:
            return weatherEntry.getWind().getSpeed();
        case WIND_DEG:
            return weatherEntry.getWind().getDegrees();
        case WIND_GUST:
            return weatherEntry.getWind().getGust();
        default:
            return null;
        }
    }

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

    protected static void initClient() {
        synchronized (client) {
            if (client.get() == null) {
                client.set(createClient(org.openremote.container.Container.SCHEDULED_EXECUTOR));
            }
        }
    }

}
