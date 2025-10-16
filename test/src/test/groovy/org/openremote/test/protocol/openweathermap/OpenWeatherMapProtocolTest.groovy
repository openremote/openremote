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
package org.openremote.test.protocol.openweathermap

import org.openremote.agent.protocol.openweathermap.OpenWeatherMapAgent
import org.openremote.agent.protocol.openweathermap.OpenWeatherMapAgentLink
import org.openremote.agent.protocol.openweathermap.OpenWeatherMapProtocol
import org.openremote.agent.protocol.openweathermap.OpenWeatherMapProperty
import org.openremote.agent.protocol.openweathermap.OpenWeatherMapResponse
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.WeatherAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientRequestFilter
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK

class OpenWeatherMapProtocolTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri

            switch (requestUri.host) {
                case "api.openweathermap.org":
                    if (requestUri.path.startsWith("/data/3.0/onecall")) {
                    def now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)

                    // Mock response from the OpenWeatherMap API (One Call 3.0 API)
                    def content = """{
                        "current": {
                            "dt": ${now.toEpochSecond(ZoneOffset.UTC)},
                            "temp": 15.5,
                            "pressure": 1013,
                            "humidity": 65,
                            "wind_speed": 3.2,
                            "wind_deg": 180,
                            "wind_gust": 4.1,
                            "clouds": 20,
                            "uvi": 2.5,
                            "pop": 0.1,
                            "rain": {"1h": 0.5}
                        },
                        "hourly": [
                            {
                                "dt": ${now.toEpochSecond(ZoneOffset.UTC)},
                                "temp": 15.5,
                                "pressure": 1013,
                                "humidity": 65,
                                "wind_speed": 3.2,
                                "wind_deg": 180,
                                "wind_gust": 4.1,
                                "clouds": 20,
                                "uvi": 2.5,
                                "pop": 0.1,
                                "rain": {"1h": 0.5}
                            },
                            {
                                "dt": ${now.plusHours(1).toEpochSecond(ZoneOffset.UTC)},
                                "temp": 16.2,
                                "pressure": 1012,
                                "humidity": 68,
                                "wind_speed": 3.8,
                                "wind_deg": 185,
                                "wind_gust": 4.5,
                                "clouds": 25,
                                "uvi": 2.8,
                                "pop": 0.15,
                                "rain": {"1h": 0.2}
                            }
                        ],
                        "daily": [
                            {
                                "dt": ${now.toEpochSecond(ZoneOffset.UTC)},
                                "temp": {"day": 18.5, "min": 12.0, "max": 22.0, "night": 14.0, "eve": 16.0, "morn": 13.0},
                                "pressure": 1013,
                                "humidity": 65,
                                "wind_speed": 3.2,
                                "wind_deg": 180,
                                "wind_gust": 4.1,
                                "clouds": 20,
                                "uvi": 2.5,
                                "pop": 0.1,
                                "rain": 0.5
                            },
                            {
                                "dt": ${now.plusDays(1).toEpochSecond(ZoneOffset.UTC)},
                                "temp": {"day": 19.2, "min": 13.0, "max": 23.0, "night": 15.0, "eve": 17.0, "morn": 14.0},
                                "pressure": 1011,
                                "humidity": 70,
                                "wind_speed": 3.5,
                                "wind_deg": 185,
                                "wind_gust": 4.8,
                                "clouds": 30,
                                "uvi": 2.7,
                                "pop": 0.2,
                                "rain": 1.2
                            }
                        ]
                    }"""
                    
                    def responseBody = ValueUtil.JSON.readValue(content, OpenWeatherMapResponse.class)
                    requestContext.abortWith(
                        Response.ok(responseBody, MediaType.APPLICATION_JSON_TYPE).build()
                    )
                    return
                    }
                    break
            }

            requestContext.abortWith(Response.serverError().build())
        }
    }

    def "OpenWeatherMap Integration Tests"() {
        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        OpenWeatherMapProtocol.initClient()

        if (!OpenWeatherMapProtocol.client.get().configuration.isRegistered(mockServer)) {
            OpenWeatherMapProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)

        when: "a OpenWeatherMap agent is created"
        def agent = new OpenWeatherMapAgent("Weather Agent")
        agent.setRealm(MASTER_REALM)
        agent.setAPIKey("test-key")
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((OpenWeatherMapProtocol)agentService.getProtocolInstance(agent.id)) != null
        }

        and: "the connection status for the agent should be CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId())
            agentService.getAgent(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "a weather asset is provisioned"
        def protocol = (OpenWeatherMapProtocol) agentService.getProtocolInstance(agent.id)
        def weatherAsset = protocol.provisionWeatherAsset()

        then: "the weather asset should be provisioned"
        conditions.eventually {
            weatherAsset = assetStorageService.find(weatherAsset.id)
            assert weatherAsset != null
            assert weatherAsset.id != null
            assert weatherAsset.name == "Weather"
            assert weatherAsset.realm == agent.realm
            assert weatherAsset.parentId == agent.id
        }

        and: "the attributes for the weather asset should be linked to the agent"
        conditions.eventually {
            assert weatherAsset.getAttribute(WeatherAsset.TEMPERATURE).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.HUMIDITY).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.ATMOSPHERIC_PRESSURE).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.WIND_SPEED).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.WIND_DIRECTION).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.WIND_GUST_SPEED).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.CLOUD_COVERAGE).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.PROBABILITY_OF_PRECIPITATION).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.RAINFALL).get().hasMeta(AGENT_LINK)
            assert weatherAsset.getAttribute(WeatherAsset.UV_INDEX).get().hasMeta(AGENT_LINK)
        }


        when: "a weather update is triggered but the weather asset has no location set"
        protocol.updateAllLinkedAttributes()

        then: "no weather data should be updated since location is unknown"
        conditions.eventually {
            weatherAsset = assetStorageService.find(weatherAsset.id)
            assert weatherAsset.getAttribute(WeatherAsset.TEMPERATURE).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.HUMIDITY).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.ATMOSPHERIC_PRESSURE).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.WIND_SPEED).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.WIND_DIRECTION).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.WIND_GUST_SPEED).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.CLOUD_COVERAGE).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.PROBABILITY_OF_PRECIPITATION).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.RAINFALL).get().getValue().orElse(null) == null
            assert weatherAsset.getAttribute(WeatherAsset.UV_INDEX).get().getValue().orElse(null) == null
        }

        when: "the weather asset has its location set"
        weatherAsset.setLocation(new GeoJSONPoint(5.46108d, 51.44593d))
        weatherAsset = assetStorageService.merge(weatherAsset)

        then: "the weather asset should have its location set"
        conditions.eventually {
            weatherAsset = assetStorageService.find(weatherAsset.id)
            assert weatherAsset != null
            assert weatherAsset.getLocation().map{it.x}.orElse(null) == 5.46108d
            assert weatherAsset.getLocation().map{it.y}.orElse(null) == 51.44593d
        }

        when: "a weather update is triggered and the weather asset now has a location set"
        protocol.updateAllLinkedAttributes()

        then: "the weather data should be updated with current values"
        conditions.eventually {
            weatherAsset = assetStorageService.find(weatherAsset.id)
            assert weatherAsset.getAttribute(WeatherAsset.TEMPERATURE).get().getValue().orElse(null) == 15.5d
            assert weatherAsset.getAttribute(WeatherAsset.HUMIDITY).get().getValue().orElse(null) == 65
            assert weatherAsset.getAttribute(WeatherAsset.ATMOSPHERIC_PRESSURE).get().getValue().orElse(null) == 1013
            assert weatherAsset.getAttribute(WeatherAsset.WIND_SPEED).get().getValue().orElse(null) == 3.2d
            assert weatherAsset.getAttribute(WeatherAsset.WIND_DIRECTION).get().getValue().orElse(null) == 180
            assert weatherAsset.getAttribute(WeatherAsset.WIND_GUST_SPEED).get().getValue().orElse(null) == 4.1d
            assert weatherAsset.getAttribute(WeatherAsset.CLOUD_COVERAGE).get().getValue().orElse(null) == 20
            assert weatherAsset.getAttribute(WeatherAsset.PROBABILITY_OF_PRECIPITATION).get().getValue().orElse(null) == 0.1d
            assert weatherAsset.getAttribute(WeatherAsset.RAINFALL).get().getValue().orElse(null) == 0.5d
            assert weatherAsset.getAttribute(WeatherAsset.UV_INDEX).get().getValue().orElse(null) == 2.5d
        }

        and: "the predicted data points should be written"
        conditions.eventually {
            // We expect 3 predicted datapoints (response has 4, but 3 unique timestamps)
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.TEMPERATURE.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.HUMIDITY.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.ATMOSPHERIC_PRESSURE.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.WIND_SPEED.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.WIND_DIRECTION.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.WIND_GUST_SPEED.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.CLOUD_COVERAGE.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.PROBABILITY_OF_PRECIPITATION.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.RAINFALL.name)).size() == 3
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(weatherAsset.id, WeatherAsset.UV_INDEX.name)).size() == 3
        }

        cleanup: "remove mock client"
        if (OpenWeatherMapProtocol.client.get() != null) {
            OpenWeatherMapProtocol.client.set(null)
        }
    }
}
