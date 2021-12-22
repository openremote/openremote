package org.openremote.test.energy

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.energy.ForecastWindService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.impl.ElectricityProducerWindAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

import static org.openremote.manager.energy.ForecastWindService.OPEN_WEATHER_API_APP_ID

class ForecastWindServiceTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri

            switch (requestUri.path) {
                case "/data/2.5/onecall":
                    def now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
                    def content = "{\n" +
                            "  \"lat\": 51.97,\n" +
                            "  \"lon\": 5.9,\n" +
                            "  \"timezone\": \"Europe/Amsterdam\",\n" +
                            "  \"timezone_offset\": 3600,\n" +
                            "  \"current\": {\n" +
                            "    \"dt\": ${now.toEpochSecond(ZoneOffset.UTC)},\n" +
                            "    \"sunrise\": 1637045887,\n" +
                            "    \"sunset\": 1637077471,\n" +
                            "    \"temp\": 5.37,\n" +
                            "    \"feels_like\": 5.37,\n" +
                            "    \"pressure\": 1022,\n" +
                            "    \"humidity\": 93,\n" +
                            "    \"dew_point\": 4.33,\n" +
                            "    \"uvi\": 0.51,\n" +
                            "    \"clouds\": 90,\n" +
                            "    \"visibility\": 6000,\n" +
                            "    \"wind_speed\": 3.45,\n" +
                            "    \"wind_deg\": 170,\n" +
                            "    \"wind_gust\": 0,\n" +
                            "    \"weather\": [\n" +
                            "      {\n" +
                            "        \"id\": 701,\n" +
                            "        \"main\": \"Mist\",\n" +
                            "        \"description\": \"mist\",\n" +
                            "        \"icon\": \"50d\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  \"hourly\": [\n" +
                            "    {\n" +
                            "      \"dt\": ${now.toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.37,\n" +
                            "      \"feels_like\": 5.37,\n" +
                            "      \"pressure\": 1022,\n" +
                            "      \"humidity\": 93,\n" +
                            "      \"dew_point\": 4.33,\n" +
                            "      \"uvi\": 0.51,\n" +
                            "      \"clouds\": 90,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 3.19,\n" +
                            "      \"wind_deg\": 230,\n" +
                            "      \"wind_gust\": 1.4,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(1).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.63,\n" +
                            "      \"feels_like\": 4.53,\n" +
                            "      \"pressure\": 1022,\n" +
                            "      \"humidity\": 89,\n" +
                            "      \"dew_point\": 3.96,\n" +
                            "      \"uvi\": 0.3,\n" +
                            "      \"clouds\": 92,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 2.61,\n" +
                            "      \"wind_deg\": 232,\n" +
                            "      \"wind_gust\": 2.04,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(2).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.79,\n" +
                            "      \"feels_like\": 4.46,\n" +
                            "      \"pressure\": 1022,\n" +
                            "      \"humidity\": 86,\n" +
                            "      \"dew_point\": 3.63,\n" +
                            "      \"uvi\": 0.11,\n" +
                            "      \"clouds\": 94,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 1.83,\n" +
                            "      \"wind_deg\": 239,\n" +
                            "      \"wind_gust\": 2.56,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(3).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.83,\n" +
                            "      \"feels_like\": 4.51,\n" +
                            "      \"pressure\": 1021,\n" +
                            "      \"humidity\": 83,\n" +
                            "      \"dew_point\": 3.17,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 96,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 1.83,\n" +
                            "      \"wind_deg\": 248,\n" +
                            "      \"wind_gust\": 2.92,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(4).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.89,\n" +
                            "      \"feels_like\": 4.64,\n" +
                            "      \"pressure\": 1021,\n" +
                            "      \"humidity\": 81,\n" +
                            "      \"dew_point\": 2.88,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 98,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 1.77,\n" +
                            "      \"wind_deg\": 254,\n" +
                            "      \"wind_gust\": 3.22,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(5).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.99,\n" +
                            "      \"feels_like\": 4.72,\n" +
                            "      \"pressure\": 1021,\n" +
                            "      \"humidity\": 78,\n" +
                            "      \"dew_point\": 2.31,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 1.81,\n" +
                            "      \"wind_deg\": 251,\n" +
                            "      \"wind_gust\": 3.57,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(6).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6,\n" +
                            "      \"feels_like\": 4.55,\n" +
                            "      \"pressure\": 1021,\n" +
                            "      \"humidity\": 77,\n" +
                            "      \"dew_point\": 2.15,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 1.98,\n" +
                            "      \"wind_deg\": 254,\n" +
                            "      \"wind_gust\": 3.76,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(7).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.95,\n" +
                            "      \"feels_like\": 4.68,\n" +
                            "      \"pressure\": 1021,\n" +
                            "      \"humidity\": 77,\n" +
                            "      \"dew_point\": 2.15,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 1.8,\n" +
                            "      \"wind_deg\": 242,\n" +
                            "      \"wind_gust\": 3.37,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(8).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.93,\n" +
                            "      \"feels_like\": 4.53,\n" +
                            "      \"pressure\": 1021,\n" +
                            "      \"humidity\": 77,\n" +
                            "      \"dew_point\": 2.18,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 1.92,\n" +
                            "      \"wind_deg\": 213,\n" +
                            "      \"wind_gust\": 3.31,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(9).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 5.94,\n" +
                            "      \"feels_like\": 4.27,\n" +
                            "      \"pressure\": 1020,\n" +
                            "      \"humidity\": 77,\n" +
                            "      \"dew_point\": 2.24,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 2.2,\n" +
                            "      \"wind_deg\": 195,\n" +
                            "      \"wind_gust\": 3.58,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(10).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.05,\n" +
                            "      \"feels_like\": 4.19,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 77,\n" +
                            "      \"dew_point\": 2.35,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 2.45,\n" +
                            "      \"wind_deg\": 192,\n" +
                            "      \"wind_gust\": 4.3,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(11).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.16,\n" +
                            "      \"feels_like\": 4.12,\n" +
                            "      \"pressure\": 1020,\n" +
                            "      \"humidity\": 78,\n" +
                            "      \"dew_point\": 2.52,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 2.7,\n" +
                            "      \"wind_deg\": 228,\n" +
                            "      \"wind_gust\": 6.04,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(12).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.24,\n" +
                            "      \"feels_like\": 4.04,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 78,\n" +
                            "      \"dew_point\": 2.66,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 2.93,\n" +
                            "      \"wind_deg\": 221,\n" +
                            "      \"wind_gust\": 7.28,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(13).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.33,\n" +
                            "      \"feels_like\": 3.88,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 79,\n" +
                            "      \"dew_point\": 2.84,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 3.34,\n" +
                            "      \"wind_deg\": 208,\n" +
                            "      \"wind_gust\": 7.93,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(14).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.49,\n" +
                            "      \"feels_like\": 3.7,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 79,\n" +
                            "      \"dew_point\": 2.97,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 3.98,\n" +
                            "      \"wind_deg\": 202,\n" +
                            "      \"wind_gust\": 8.55,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(15).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.52,\n" +
                            "      \"feels_like\": 3.53,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 79,\n" +
                            "      \"dew_point\": 3.15,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 98,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 4.37,\n" +
                            "      \"wind_deg\": 214,\n" +
                            "      \"wind_gust\": 9.76,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(16).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.69,\n" +
                            "      \"feels_like\": 3.65,\n" +
                            "      \"pressure\": 1018,\n" +
                            "      \"humidity\": 81,\n" +
                            "      \"dew_point\": 3.62,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 98,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 4.57,\n" +
                            "      \"wind_deg\": 211,\n" +
                            "      \"wind_gust\": 9.8,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(17).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.72,\n" +
                            "      \"feels_like\": 3.71,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 84,\n" +
                            "      \"dew_point\": 4.1,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 99,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 4.52,\n" +
                            "      \"wind_deg\": 218,\n" +
                            "      \"wind_gust\": 10.41,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04n\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(18).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 6.85,\n" +
                            "      \"feels_like\": 3.86,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 86,\n" +
                            "      \"dew_point\": 4.58,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 100,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 4.54,\n" +
                            "      \"wind_deg\": 225,\n" +
                            "      \"wind_gust\": 10.91,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0.01\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(19).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 7.15,\n" +
                            "      \"feels_like\": 4.2,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 89,\n" +
                            "      \"dew_point\": 5.39,\n" +
                            "      \"uvi\": 0,\n" +
                            "      \"clouds\": 98,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 4.6,\n" +
                            "      \"wind_deg\": 228,\n" +
                            "      \"wind_gust\": 10.3,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0.05\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(20).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 7.85,\n" +
                            "      \"feels_like\": 5.02,\n" +
                            "      \"pressure\": 1019,\n" +
                            "      \"humidity\": 91,\n" +
                            "      \"dew_point\": 6.35,\n" +
                            "      \"uvi\": 0.1,\n" +
                            "      \"clouds\": 91,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 4.72,\n" +
                            "      \"wind_deg\": 229,\n" +
                            "      \"wind_gust\": 10.31,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0.05\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(21).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 8.52,\n" +
                            "      \"feels_like\": 5.91,\n" +
                            "      \"pressure\": 1020,\n" +
                            "      \"humidity\": 93,\n" +
                            "      \"dew_point\": 7.31,\n" +
                            "      \"uvi\": 0.26,\n" +
                            "      \"clouds\": 94,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 4.6,\n" +
                            "      \"wind_deg\": 258,\n" +
                            "      \"wind_gust\": 9.79,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0.05\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(22).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 10.23,\n" +
                            "      \"feels_like\": 9.5,\n" +
                            "      \"pressure\": 1020,\n" +
                            "      \"humidity\": 84,\n" +
                            "      \"dew_point\": 7.47,\n" +
                            "      \"uvi\": 0.36,\n" +
                            "      \"clouds\": 94,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 3.92,\n" +
                            "      \"wind_deg\": 287,\n" +
                            "      \"wind_gust\": 8.04,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0.05\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"dt\": ${now.plusHours(23).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "      \"temp\": 10.03,\n" +
                            "      \"feels_like\": 9.05,\n" +
                            "      \"pressure\": 1020,\n" +
                            "      \"humidity\": 75,\n" +
                            "      \"dew_point\": 5.78,\n" +
                            "      \"uvi\": 0.37,\n" +
                            "      \"clouds\": 95,\n" +
                            "      \"visibility\": 10000,\n" +
                            "      \"wind_speed\": 3.91,\n" +
                            "      \"wind_deg\": 294,\n" +
                            "      \"wind_gust\": 8.34,\n" +
                            "      \"weather\": [\n" +
                            "        {\n" +
                            "          \"id\": 804,\n" +
                            "          \"main\": \"Clouds\",\n" +
                            "          \"description\": \"overcast clouds\",\n" +
                            "          \"icon\": \"04d\"\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"pop\": 0.05\n" +
                            "    },\n" +
                            "  ]\n" +
                            "}"
                    def responseBody = ValueUtil.JSON.readValue(content, ForecastWindService.WeatherForecastResponseModel.class)
                    requestContext.abortWith(
                            Response.ok(responseBody, MediaType.APPLICATION_JSON_TYPE).build()
                    )
                    return
            }

            requestContext.abortWith(Response.serverError().build())
        }
    }

    def "Test adding and removing asset with enabled attributes"() {
        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def config = defaultConfig()
        config << [(OPEN_WEATHER_API_APP_ID): "test-key"]

        if (!ForecastWindService.resteasyClient.configuration.isRegistered(mockServer)) {
            ForecastWindService.resteasyClient.register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(config, defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def forecastWindService = container.getService(ForecastWindService.class)

        expect: "a Future for calculation should exist and the asset should have filled in value for power and powerForecast"
        conditions.eventually {
            assert !forecastWindService.calculationFutures.isEmpty()
            assert forecastWindService.calculationFutures.get(managerTestSetup.electricityWindAssetId) != null
            def windAsset = assetStorageService.find(managerTestSetup.electricityWindAssetId)
            assert windAsset.getAttribute(ElectricityProducerWindAsset.POWER).flatMap { it.value }.orElse(0d) != 0d
            assert windAsset.getAttribute(ElectricityProducerWindAsset.POWER_FORECAST).flatMap { it.value }.orElse(0d) != 0d
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(windAsset.getId(), ElectricityProducerWindAsset.POWER.getName())).size() > 0
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(windAsset.getId(), ElectricityProducerWindAsset.POWER_FORECAST.getName())).size() > 0
        }

        when: "an asset is added with includeForecastWindService set to true"
        def newWindAsset = new ElectricityProducerWindAsset("WindAsset")
        newWindAsset.setParentId(managerTestSetup.electricityOptimisationAssetId)
        newWindAsset.setRealm(managerTestSetup.realmEnergyTenant)
        newWindAsset.setWindSpeedMax(18d);
        newWindAsset.setWindSpeedMin(2d);
        newWindAsset.setWindSpeedReference(12d);
        newWindAsset.setEfficiencyExport(100);
        newWindAsset.setPowerExportMax(2.5);
        newWindAsset.setLocation(new GeoJSONPoint(9.195295, 48.787418));
        newWindAsset.setSetActualValueWithForecast(true);
        newWindAsset.setIncludeForecastWindService(true);
        newWindAsset = assetStorageService.merge(newWindAsset)

        then: "the assetId should be present in the calculationFutures"
        conditions.eventually {
            assert forecastWindService.calculationFutures.get(newWindAsset.getId()) != null
            newWindAsset = assetStorageService.find(newWindAsset.getId())
            assert newWindAsset.getAttribute(ElectricityProducerWindAsset.POWER).flatMap { it.value }.orElse(0d) != 0d
            assert newWindAsset.getAttribute(ElectricityProducerWindAsset.POWER_FORECAST).flatMap { it.value }.orElse(0d) != 0d
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(newWindAsset.getId(), ElectricityProducerWindAsset.POWER.getName())).size() > 0
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(newWindAsset.getId(), ElectricityProducerWindAsset.POWER_FORECAST.getName())).size() > 0
        }

        when: "an asset is added with includeForecastWindService set to false"
        def newWindAsset2 = new ElectricityProducerWindAsset("WindAsset2")
        newWindAsset2.setParentId(managerTestSetup.electricityOptimisationAssetId)
        newWindAsset2.setRealm(managerTestSetup.realmEnergyTenant)
        newWindAsset2.setWindSpeedMax(18d);
        newWindAsset2.setWindSpeedMin(2d);
        newWindAsset2.setWindSpeedReference(12d);
        newWindAsset2.setEfficiencyExport(100);
        newWindAsset2.setPowerExportMax(2.5);
        newWindAsset2.setLocation(new GeoJSONPoint(9.195275, 48.787418));
        newWindAsset2.setSetActualValueWithForecast(false);
        newWindAsset2.setIncludeForecastWindService(false);
        newWindAsset2 = assetStorageService.merge(newWindAsset2)

        then: "the assetId shouldn't be present in the calculationFutures"
        conditions.eventually {
            assert forecastWindService.calculationFutures.get(newWindAsset2.getId()) == null
        }

        when: "an asset updated it's includeForecastWindService to true"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(newWindAsset2.getId(), ElectricityProducerWindAsset.INCLUDE_FORECAST_WIND_SERVICE.name, true))

        then: "it should be present present in the calculationFutures"
        conditions.eventually {
            assert forecastWindService.calculationFutures.get(newWindAsset2.getId()) != null
            newWindAsset2 = assetStorageService.find(newWindAsset2.getId())
            assert newWindAsset2.getAttribute(ElectricityProducerWindAsset.POWER).flatMap { it.value }.orElse(0d) == 0d
            assert newWindAsset2.getAttribute(ElectricityProducerWindAsset.POWER_FORECAST).flatMap { it.value }.orElse(0d) != 0d
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(newWindAsset2.getId(), ElectricityProducerWindAsset.POWER.getName())).size() > 0
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(newWindAsset2.getId(), ElectricityProducerWindAsset.POWER_FORECAST.getName())).size() > 0
        }

        when: "an asset updated it's setActualValueWithForecast to true"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(newWindAsset2.getId(), ElectricityProducerWindAsset.SET_ACTUAL_VALUE_WITH_FORECAST.name, true))

        then: "it power should be updated too"
        conditions.eventually {
            newWindAsset2 = assetStorageService.find(newWindAsset2.getId())
            assert newWindAsset2.getAttribute(ElectricityProducerWindAsset.POWER).flatMap { it.value }.orElse(0d) != 0d
        }

        when: "an asset updated it's includeForecastWindService to false"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(newWindAsset2.getId(), ElectricityProducerWindAsset.INCLUDE_FORECAST_WIND_SERVICE.name, false))

        then: "it shouldn't be present present in the calculationFutures"
        conditions.eventually {
            assert forecastWindService.calculationFutures.get(newWindAsset2.getId()) == null
        }

        when: "an asset is added with includeForecastWindService set to true but no location"
        def newWindAsset3 = new ElectricityProducerWindAsset("WindAsset3")
        newWindAsset3.setParentId(managerTestSetup.electricityOptimisationAssetId)
        newWindAsset3.setRealm(managerTestSetup.realmEnergyTenant)
        newWindAsset3.setWindSpeedMax(18d);
        newWindAsset3.setWindSpeedMin(2d);
        newWindAsset3.setWindSpeedReference(12d);
        newWindAsset3.setEfficiencyExport(100);
        newWindAsset3.setPowerExportMax(2.5);
        newWindAsset3.setSetActualValueWithForecast(true);
        newWindAsset3.setIncludeForecastWindService(true);
        newWindAsset3 = assetStorageService.merge(newWindAsset3)

        then: "the assetId shouldn't be present in the calculationFutures"
        conditions.eventually {
            assert forecastWindService.calculationFutures.get(newWindAsset3.getId()) == null
        }

        when: "an asset updated it's location to have a value"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(newWindAsset3.getId(), Asset.LOCATION.name, new GeoJSONPoint(9.195275, 48.787418)))

        and: "trigger the service by setting includeForecastWindService to true again "
        assetProcessingService.sendAttributeEvent(new AttributeEvent(newWindAsset3.getId(), ElectricityProducerWindAsset.INCLUDE_FORECAST_WIND_SERVICE.name, true))

        then: "it should be present present in the calculationFutures"
        conditions.eventually {
            assert forecastWindService.calculationFutures.get(newWindAsset3.getId()) != null
            newWindAsset3 = assetStorageService.find(newWindAsset3.getId())
            assert newWindAsset3.getAttribute(ElectricityProducerWindAsset.POWER).flatMap { it.value }.orElse(0d) != 0d
            assert newWindAsset3.getAttribute(ElectricityProducerWindAsset.POWER_FORECAST).flatMap { it.value }.orElse(0d) != 0d
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(newWindAsset3.getId(), ElectricityProducerWindAsset.POWER.getName())).size() > 0
            assert assetPredictedDatapointService.getDatapoints(new AttributeRef(newWindAsset3.getId(), ElectricityProducerWindAsset.POWER_FORECAST.getName())).size() > 0
        }
    }
}
