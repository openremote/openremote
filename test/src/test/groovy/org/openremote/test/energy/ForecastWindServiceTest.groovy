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
                            "\t\"lat\": 51.97,\n" +
                            "\t\"lon\": 5.9,\n" +
                            "\t\"timezone\": \"Europe/Amsterdam\",\n" +
                            "\t\"timezone_offset\": 3600,\n" +
                            "\t\"current\": {\n" +
                            "\t\t\"dt\": ${now.toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\"sunrise\": 1637045887,\n" +
                            "\t\t\"sunset\": 1637077471,\n" +
                            "\t\t\"temp\": 5.37,\n" +
                            "\t\t\"feels_like\": 5.37,\n" +
                            "\t\t\"pressure\": 1022,\n" +
                            "\t\t\"humidity\": 93,\n" +
                            "\t\t\"dew_point\": 4.33,\n" +
                            "\t\t\"uvi\": 0.51,\n" +
                            "\t\t\"clouds\": 90,\n" +
                            "\t\t\"visibility\": 6000,\n" +
                            "\t\t\"wind_speed\": 3.45,\n" +
                            "\t\t\"wind_deg\": 170,\n" +
                            "\t\t\"wind_gust\": 0,\n" +
                            "\t\t\"weather\": [\n" +
                            "\t\t\t{\n" +
                            "\t\t\t\t\"id\": 701,\n" +
                            "\t\t\t\t\"main\": \"Mist\",\n" +
                            "\t\t\t\t\"description\": \"mist\",\n" +
                            "\t\t\t\t\"icon\": \"50d\"\n" +
                            "\t\t\t}\n" +
                            "\t\t]\n" +
                            "\t},\n" +
                            "\t\"hourly\": [\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.37,\n" +
                            "\t\t\t\"feels_like\": 5.37,\n" +
                            "\t\t\t\"pressure\": 1022,\n" +
                            "\t\t\t\"humidity\": 93,\n" +
                            "\t\t\t\"dew_point\": 4.33,\n" +
                            "\t\t\t\"uvi\": 0.51,\n" +
                            "\t\t\t\"clouds\": 90,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.19,\n" +
                            "\t\t\t\"wind_deg\": 230,\n" +
                            "\t\t\t\"wind_gust\": 1.4,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(1).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.63,\n" +
                            "\t\t\t\"feels_like\": 4.53,\n" +
                            "\t\t\t\"pressure\": 1022,\n" +
                            "\t\t\t\"humidity\": 89,\n" +
                            "\t\t\t\"dew_point\": 3.96,\n" +
                            "\t\t\t\"uvi\": 0.3,\n" +
                            "\t\t\t\"clouds\": 92,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 2.61,\n" +
                            "\t\t\t\"wind_deg\": 232,\n" +
                            "\t\t\t\"wind_gust\": 2.04,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(2).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.79,\n" +
                            "\t\t\t\"feels_like\": 4.46,\n" +
                            "\t\t\t\"pressure\": 1022,\n" +
                            "\t\t\t\"humidity\": 86,\n" +
                            "\t\t\t\"dew_point\": 3.63,\n" +
                            "\t\t\t\"uvi\": 0.11,\n" +
                            "\t\t\t\"clouds\": 94,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 1.83,\n" +
                            "\t\t\t\"wind_deg\": 239,\n" +
                            "\t\t\t\"wind_gust\": 2.56,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(3).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.83,\n" +
                            "\t\t\t\"feels_like\": 4.51,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 83,\n" +
                            "\t\t\t\"dew_point\": 3.17,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 96,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 1.83,\n" +
                            "\t\t\t\"wind_deg\": 248,\n" +
                            "\t\t\t\"wind_gust\": 2.92,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(4).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.89,\n" +
                            "\t\t\t\"feels_like\": 4.64,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 81,\n" +
                            "\t\t\t\"dew_point\": 2.88,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 98,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 1.77,\n" +
                            "\t\t\t\"wind_deg\": 254,\n" +
                            "\t\t\t\"wind_gust\": 3.22,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(5).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.99,\n" +
                            "\t\t\t\"feels_like\": 4.72,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 78,\n" +
                            "\t\t\t\"dew_point\": 2.31,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 1.81,\n" +
                            "\t\t\t\"wind_deg\": 251,\n" +
                            "\t\t\t\"wind_gust\": 3.57,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(6).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6,\n" +
                            "\t\t\t\"feels_like\": 4.55,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 77,\n" +
                            "\t\t\t\"dew_point\": 2.15,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 1.98,\n" +
                            "\t\t\t\"wind_deg\": 254,\n" +
                            "\t\t\t\"wind_gust\": 3.76,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(7).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.95,\n" +
                            "\t\t\t\"feels_like\": 4.68,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 77,\n" +
                            "\t\t\t\"dew_point\": 2.15,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 1.8,\n" +
                            "\t\t\t\"wind_deg\": 242,\n" +
                            "\t\t\t\"wind_gust\": 3.37,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(8).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.93,\n" +
                            "\t\t\t\"feels_like\": 4.53,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 77,\n" +
                            "\t\t\t\"dew_point\": 2.18,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 1.92,\n" +
                            "\t\t\t\"wind_deg\": 213,\n" +
                            "\t\t\t\"wind_gust\": 3.31,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(9).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.94,\n" +
                            "\t\t\t\"feels_like\": 4.27,\n" +
                            "\t\t\t\"pressure\": 1020,\n" +
                            "\t\t\t\"humidity\": 77,\n" +
                            "\t\t\t\"dew_point\": 2.24,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 2.2,\n" +
                            "\t\t\t\"wind_deg\": 195,\n" +
                            "\t\t\t\"wind_gust\": 3.58,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(10).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.05,\n" +
                            "\t\t\t\"feels_like\": 4.19,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 77,\n" +
                            "\t\t\t\"dew_point\": 2.35,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 2.45,\n" +
                            "\t\t\t\"wind_deg\": 192,\n" +
                            "\t\t\t\"wind_gust\": 4.3,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(11).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.16,\n" +
                            "\t\t\t\"feels_like\": 4.12,\n" +
                            "\t\t\t\"pressure\": 1020,\n" +
                            "\t\t\t\"humidity\": 78,\n" +
                            "\t\t\t\"dew_point\": 2.52,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 2.7,\n" +
                            "\t\t\t\"wind_deg\": 228,\n" +
                            "\t\t\t\"wind_gust\": 6.04,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(12).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.24,\n" +
                            "\t\t\t\"feels_like\": 4.04,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 78,\n" +
                            "\t\t\t\"dew_point\": 2.66,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 2.93,\n" +
                            "\t\t\t\"wind_deg\": 221,\n" +
                            "\t\t\t\"wind_gust\": 7.28,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(13).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.33,\n" +
                            "\t\t\t\"feels_like\": 3.88,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 79,\n" +
                            "\t\t\t\"dew_point\": 2.84,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.34,\n" +
                            "\t\t\t\"wind_deg\": 208,\n" +
                            "\t\t\t\"wind_gust\": 7.93,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(14).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.49,\n" +
                            "\t\t\t\"feels_like\": 3.7,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 79,\n" +
                            "\t\t\t\"dew_point\": 2.97,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.98,\n" +
                            "\t\t\t\"wind_deg\": 202,\n" +
                            "\t\t\t\"wind_gust\": 8.55,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(15).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.52,\n" +
                            "\t\t\t\"feels_like\": 3.53,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 79,\n" +
                            "\t\t\t\"dew_point\": 3.15,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 98,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.37,\n" +
                            "\t\t\t\"wind_deg\": 214,\n" +
                            "\t\t\t\"wind_gust\": 9.76,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(16).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.69,\n" +
                            "\t\t\t\"feels_like\": 3.65,\n" +
                            "\t\t\t\"pressure\": 1018,\n" +
                            "\t\t\t\"humidity\": 81,\n" +
                            "\t\t\t\"dew_point\": 3.62,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 98,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.57,\n" +
                            "\t\t\t\"wind_deg\": 211,\n" +
                            "\t\t\t\"wind_gust\": 9.8,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(17).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.72,\n" +
                            "\t\t\t\"feels_like\": 3.71,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 84,\n" +
                            "\t\t\t\"dew_point\": 4.1,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 99,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.52,\n" +
                            "\t\t\t\"wind_deg\": 218,\n" +
                            "\t\t\t\"wind_gust\": 10.41,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(18).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.85,\n" +
                            "\t\t\t\"feels_like\": 3.86,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 86,\n" +
                            "\t\t\t\"dew_point\": 4.58,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.54,\n" +
                            "\t\t\t\"wind_deg\": 225,\n" +
                            "\t\t\t\"wind_gust\": 10.91,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.01\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(19).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 7.15,\n" +
                            "\t\t\t\"feels_like\": 4.2,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 89,\n" +
                            "\t\t\t\"dew_point\": 5.39,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 98,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.6,\n" +
                            "\t\t\t\"wind_deg\": 228,\n" +
                            "\t\t\t\"wind_gust\": 10.3,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.05\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(20).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 7.85,\n" +
                            "\t\t\t\"feels_like\": 5.02,\n" +
                            "\t\t\t\"pressure\": 1019,\n" +
                            "\t\t\t\"humidity\": 91,\n" +
                            "\t\t\t\"dew_point\": 6.35,\n" +
                            "\t\t\t\"uvi\": 0.1,\n" +
                            "\t\t\t\"clouds\": 91,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.72,\n" +
                            "\t\t\t\"wind_deg\": 229,\n" +
                            "\t\t\t\"wind_gust\": 10.31,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.05\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(21).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 8.52,\n" +
                            "\t\t\t\"feels_like\": 5.91,\n" +
                            "\t\t\t\"pressure\": 1020,\n" +
                            "\t\t\t\"humidity\": 93,\n" +
                            "\t\t\t\"dew_point\": 7.31,\n" +
                            "\t\t\t\"uvi\": 0.26,\n" +
                            "\t\t\t\"clouds\": 94,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.6,\n" +
                            "\t\t\t\"wind_deg\": 258,\n" +
                            "\t\t\t\"wind_gust\": 9.79,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.05\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(22).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 10.23,\n" +
                            "\t\t\t\"feels_like\": 9.5,\n" +
                            "\t\t\t\"pressure\": 1020,\n" +
                            "\t\t\t\"humidity\": 84,\n" +
                            "\t\t\t\"dew_point\": 7.47,\n" +
                            "\t\t\t\"uvi\": 0.36,\n" +
                            "\t\t\t\"clouds\": 94,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.92,\n" +
                            "\t\t\t\"wind_deg\": 287,\n" +
                            "\t\t\t\"wind_gust\": 8.04,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.05\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(23).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 10.03,\n" +
                            "\t\t\t\"feels_like\": 9.05,\n" +
                            "\t\t\t\"pressure\": 1020,\n" +
                            "\t\t\t\"humidity\": 75,\n" +
                            "\t\t\t\"dew_point\": 5.78,\n" +
                            "\t\t\t\"uvi\": 0.37,\n" +
                            "\t\t\t\"clouds\": 95,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.91,\n" +
                            "\t\t\t\"wind_deg\": 294,\n" +
                            "\t\t\t\"wind_gust\": 8.34,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.05\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(24).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 9.77,\n" +
                            "\t\t\t\"feels_like\": 8.22,\n" +
                            "\t\t\t\"pressure\": 1020,\n" +
                            "\t\t\t\"humidity\": 73,\n" +
                            "\t\t\t\"dew_point\": 5.19,\n" +
                            "\t\t\t\"uvi\": 0.53,\n" +
                            "\t\t\t\"clouds\": 99,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.02,\n" +
                            "\t\t\t\"wind_deg\": 273,\n" +
                            "\t\t\t\"wind_gust\": 6.7,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.18\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(25).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 10.16,\n" +
                            "\t\t\t\"feels_like\": 9.11,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 72,\n" +
                            "\t\t\t\"dew_point\": 5.19,\n" +
                            "\t\t\t\"uvi\": 0.31,\n" +
                            "\t\t\t\"clouds\": 99,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.88,\n" +
                            "\t\t\t\"wind_deg\": 276,\n" +
                            "\t\t\t\"wind_gust\": 7.62,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.12\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(26).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 9.19,\n" +
                            "\t\t\t\"feels_like\": 7.03,\n" +
                            "\t\t\t\"pressure\": 1021,\n" +
                            "\t\t\t\"humidity\": 75,\n" +
                            "\t\t\t\"dew_point\": 4.98,\n" +
                            "\t\t\t\"uvi\": 0.11,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.95,\n" +
                            "\t\t\t\"wind_deg\": 276,\n" +
                            "\t\t\t\"wind_gust\": 8.37,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0.04\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(27).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 7.33,\n" +
                            "\t\t\t\"feels_like\": 5.08,\n" +
                            "\t\t\t\"pressure\": 1022,\n" +
                            "\t\t\t\"humidity\": 83,\n" +
                            "\t\t\t\"dew_point\": 4.62,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 88,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.35,\n" +
                            "\t\t\t\"wind_deg\": 270,\n" +
                            "\t\t\t\"wind_gust\": 7.42,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(28).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.69,\n" +
                            "\t\t\t\"feels_like\": 4.25,\n" +
                            "\t\t\t\"pressure\": 1023,\n" +
                            "\t\t\t\"humidity\": 86,\n" +
                            "\t\t\t\"dew_point\": 4.51,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 91,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.44,\n" +
                            "\t\t\t\"wind_deg\": 260,\n" +
                            "\t\t\t\"wind_gust\": 8.64,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(29).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.54,\n" +
                            "\t\t\t\"feels_like\": 4,\n" +
                            "\t\t\t\"pressure\": 1023,\n" +
                            "\t\t\t\"humidity\": 87,\n" +
                            "\t\t\t\"dew_point\": 4.47,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 92,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.56,\n" +
                            "\t\t\t\"wind_deg\": 257,\n" +
                            "\t\t\t\"wind_gust\": 9.1,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(30).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.54,\n" +
                            "\t\t\t\"feels_like\": 3.94,\n" +
                            "\t\t\t\"pressure\": 1024,\n" +
                            "\t\t\t\"humidity\": 87,\n" +
                            "\t\t\t\"dew_point\": 4.48,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.66,\n" +
                            "\t\t\t\"wind_deg\": 253,\n" +
                            "\t\t\t\"wind_gust\": 9.4,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(31).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.56,\n" +
                            "\t\t\t\"feels_like\": 3.8,\n" +
                            "\t\t\t\"pressure\": 1024,\n" +
                            "\t\t\t\"humidity\": 87,\n" +
                            "\t\t\t\"dew_point\": 4.54,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.95,\n" +
                            "\t\t\t\"wind_deg\": 247,\n" +
                            "\t\t\t\"wind_gust\": 10.34,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(32).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 7.1,\n" +
                            "\t\t\t\"feels_like\": 4.42,\n" +
                            "\t\t\t\"pressure\": 1024,\n" +
                            "\t\t\t\"humidity\": 85,\n" +
                            "\t\t\t\"dew_point\": 4.71,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.03,\n" +
                            "\t\t\t\"wind_deg\": 243,\n" +
                            "\t\t\t\"wind_gust\": 9.87,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(33).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 7.61,\n" +
                            "\t\t\t\"feels_like\": 5,\n" +
                            "\t\t\t\"pressure\": 1024,\n" +
                            "\t\t\t\"humidity\": 83,\n" +
                            "\t\t\t\"dew_point\": 4.81,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.12,\n" +
                            "\t\t\t\"wind_deg\": 242,\n" +
                            "\t\t\t\"wind_gust\": 9.75,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(34).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 7.4,\n" +
                            "\t\t\t\"feels_like\": 4.69,\n" +
                            "\t\t\t\"pressure\": 1024,\n" +
                            "\t\t\t\"humidity\": 84,\n" +
                            "\t\t\t\"dew_point\": 4.89,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.24,\n" +
                            "\t\t\t\"wind_deg\": 239,\n" +
                            "\t\t\t\"wind_gust\": 10.22,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(35).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.66,\n" +
                            "\t\t\t\"feels_like\": 3.87,\n" +
                            "\t\t\t\"pressure\": 1025,\n" +
                            "\t\t\t\"humidity\": 90,\n" +
                            "\t\t\t\"dew_point\": 4.99,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.06,\n" +
                            "\t\t\t\"wind_deg\": 239,\n" +
                            "\t\t\t\"wind_gust\": 9.87,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(36).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.25,\n" +
                            "\t\t\t\"feels_like\": 3.37,\n" +
                            "\t\t\t\"pressure\": 1024,\n" +
                            "\t\t\t\"humidity\": 92,\n" +
                            "\t\t\t\"dew_point\": 5.05,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.04,\n" +
                            "\t\t\t\"wind_deg\": 236,\n" +
                            "\t\t\t\"wind_gust\": 9.76,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(37).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 6.18,\n" +
                            "\t\t\t\"feels_like\": 3.37,\n" +
                            "\t\t\t\"pressure\": 1025,\n" +
                            "\t\t\t\"humidity\": 93,\n" +
                            "\t\t\t\"dew_point\": 5.08,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.89,\n" +
                            "\t\t\t\"wind_deg\": 235,\n" +
                            "\t\t\t\"wind_gust\": 9.44,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(38).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.99,\n" +
                            "\t\t\t\"feels_like\": 3.04,\n" +
                            "\t\t\t\"pressure\": 1025,\n" +
                            "\t\t\t\"humidity\": 93,\n" +
                            "\t\t\t\"dew_point\": 4.93,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.06,\n" +
                            "\t\t\t\"wind_deg\": 239,\n" +
                            "\t\t\t\"wind_gust\": 10.04,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(39).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.74,\n" +
                            "\t\t\t\"feels_like\": 2.93,\n" +
                            "\t\t\t\"pressure\": 1025,\n" +
                            "\t\t\t\"humidity\": 94,\n" +
                            "\t\t\t\"dew_point\": 4.72,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.71,\n" +
                            "\t\t\t\"wind_deg\": 239,\n" +
                            "\t\t\t\"wind_gust\": 9.54,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(40).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.68,\n" +
                            "\t\t\t\"feels_like\": 2.89,\n" +
                            "\t\t\t\"pressure\": 1025,\n" +
                            "\t\t\t\"humidity\": 94,\n" +
                            "\t\t\t\"dew_point\": 4.64,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.66,\n" +
                            "\t\t\t\"wind_deg\": 236,\n" +
                            "\t\t\t\"wind_gust\": 9.14,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(41).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.56,\n" +
                            "\t\t\t\"feels_like\": 2.77,\n" +
                            "\t\t\t\"pressure\": 1026,\n" +
                            "\t\t\t\"humidity\": 94,\n" +
                            "\t\t\t\"dew_point\": 4.55,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 100,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.62,\n" +
                            "\t\t\t\"wind_deg\": 236,\n" +
                            "\t\t\t\"wind_gust\": 9.26,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(42).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.26,\n" +
                            "\t\t\t\"feels_like\": 2.52,\n" +
                            "\t\t\t\"pressure\": 1027,\n" +
                            "\t\t\t\"humidity\": 95,\n" +
                            "\t\t\t\"dew_point\": 4.43,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 59,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.43,\n" +
                            "\t\t\t\"wind_deg\": 233,\n" +
                            "\t\t\t\"wind_gust\": 8.77,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 803,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"broken clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04n\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(43).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 5.99,\n" +
                            "\t\t\t\"feels_like\": 3.21,\n" +
                            "\t\t\t\"pressure\": 1027,\n" +
                            "\t\t\t\"humidity\": 93,\n" +
                            "\t\t\t\"dew_point\": 4.9,\n" +
                            "\t\t\t\"uvi\": 0,\n" +
                            "\t\t\t\"clouds\": 77,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 3.77,\n" +
                            "\t\t\t\"wind_deg\": 230,\n" +
                            "\t\t\t\"wind_gust\": 9.2,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 803,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"broken clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(44).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 7.51,\n" +
                            "\t\t\t\"feels_like\": 4.8,\n" +
                            "\t\t\t\"pressure\": 1027,\n" +
                            "\t\t\t\"humidity\": 88,\n" +
                            "\t\t\t\"dew_point\": 5.6,\n" +
                            "\t\t\t\"uvi\": 0.24,\n" +
                            "\t\t\t\"clouds\": 85,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 4.29,\n" +
                            "\t\t\t\"wind_deg\": 232,\n" +
                            "\t\t\t\"wind_gust\": 9.78,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(45).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 8.75,\n" +
                            "\t\t\t\"feels_like\": 5.98,\n" +
                            "\t\t\t\"pressure\": 1027,\n" +
                            "\t\t\t\"humidity\": 85,\n" +
                            "\t\t\t\"dew_point\": 6.23,\n" +
                            "\t\t\t\"uvi\": 0.47,\n" +
                            "\t\t\t\"clouds\": 89,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 5.12,\n" +
                            "\t\t\t\"wind_deg\": 236,\n" +
                            "\t\t\t\"wind_gust\": 10.55,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(46).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 9.89,\n" +
                            "\t\t\t\"feels_like\": 7.14,\n" +
                            "\t\t\t\"pressure\": 1027,\n" +
                            "\t\t\t\"humidity\": 81,\n" +
                            "\t\t\t\"dew_point\": 6.69,\n" +
                            "\t\t\t\"uvi\": 0.66,\n" +
                            "\t\t\t\"clouds\": 91,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 5.87,\n" +
                            "\t\t\t\"wind_deg\": 240,\n" +
                            "\t\t\t\"wind_gust\": 10.95,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t},\n" +
                            "\t\t{\n" +
                            "\t\t\t\"dt\": ${now.plusHours(47).toEpochSecond(ZoneOffset.UTC)},\n" +
                            "\t\t\t\"temp\": 10.55,\n" +
                            "\t\t\t\"feels_like\": 9.72,\n" +
                            "\t\t\t\"pressure\": 1026,\n" +
                            "\t\t\t\"humidity\": 79,\n" +
                            "\t\t\t\"dew_point\": 6.9,\n" +
                            "\t\t\t\"uvi\": 0.69,\n" +
                            "\t\t\t\"clouds\": 92,\n" +
                            "\t\t\t\"visibility\": 10000,\n" +
                            "\t\t\t\"wind_speed\": 6.06,\n" +
                            "\t\t\t\"wind_deg\": 240,\n" +
                            "\t\t\t\"wind_gust\": 11.02,\n" +
                            "\t\t\t\"weather\": [\n" +
                            "\t\t\t\t{\n" +
                            "\t\t\t\t\t\"id\": 804,\n" +
                            "\t\t\t\t\t\"main\": \"Clouds\",\n" +
                            "\t\t\t\t\t\"description\": \"overcast clouds\",\n" +
                            "\t\t\t\t\t\"icon\": \"04d\"\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t],\n" +
                            "\t\t\t\"pop\": 0\n" +
                            "\t\t}\n" +
                            "\t]\n" +
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
