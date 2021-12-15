package org.openremote.test.energy

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Ignore
import org.openremote.agent.protocol.websocket.WebsocketAgentProtocol
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.energy.ForecastSolarService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.ElectricityProducerSolarAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.geo.GeoJSONPoint
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
import java.time.temporal.ChronoUnit

import static org.openremote.manager.energy.ForecastSolarService.FORECAST_SOLAR_API_KEY

@Ignore
class ForecastSolarServiceTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri

            switch (requestUri.host) {
                case "api.forecast.solar":
                    def now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
                    def content = "\"{\\n\" +\n" +
                            "                \"  \\\"result\\\": {\\n\" +\n" +
                            "                \"    \\\"watts\\\": {\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.toLocalTime().toString()}\\\": 0,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(15).toLocalTime().toString()}\\\": 780000,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(30).toLocalTime().toString()}\\\": 3904036,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(45).toLocalTime().toString()}\\\": 3854598,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(60).toLocalTime().toString()}\\\": 3746874,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(75).toLocalTime().toString()}\\\": 3675780,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(90).toLocalTime().toString()}\\\": 3553500,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(105).toLocalTime().toString()}\\\": 3439679,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(120).toLocalTime().toString()}\\\": 3278604,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(135).toLocalTime().toString()}\\\": 3137450,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(150).toLocalTime().toString()}\\\": 2954064,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(165).toLocalTime().toString()}\\\": 2739056,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(180).toLocalTime().toString()}\\\": 2493609,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(195).toLocalTime().toString()}\\\": 2254420,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(210).toLocalTime().toString()}\\\": 1949976,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(225).toLocalTime().toString()}\\\": 1697104,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(240).toLocalTime().toString()}\\\": 1452142,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(255).toLocalTime().toString()}\\\": 1194060,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(270).toLocalTime().toString()}\\\": 925642,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(285).toLocalTime().toString()}\\\": 761949,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(300).toLocalTime().toString()}\\\": 594217,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(315).toLocalTime().toString()}\\\": 423624,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(330).toLocalTime().toString()}\\\": 80000,\\n\" +\n" +
                            "                \"      \\\"${now.plusDays(1).toLocalDate().toString()} ${now.toLocalTime().toString()}\\\": 0\\n\" +\n" +
                            "                \"    },\\n\" +\n" +
                            "                \"    \\\"watt_hours\\\": {\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.toLocalTime().toString()}\\\": 0,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(15).toLocalTime().toString()}\\\": 3471000,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(30).toLocalTime().toString()}\\\": 20778893,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(45).toLocalTime().toString()}\\\": 21742542,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(60).toLocalTime().toString()}\\\": 22679261,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(75).toLocalTime().toString()}\\\": 23598206,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(90).toLocalTime().toString()}\\\": 24486581,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(105).toLocalTime().toString()}\\\": 25346501,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(120).toLocalTime().toString()}\\\": 26166152,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(135).toLocalTime().toString()}\\\": 26950514,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(150).toLocalTime().toString()}\\\": 27689030,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(165).toLocalTime().toString()}\\\": 28373794,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(180).toLocalTime().toString()}\\\": 28997196,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(195).toLocalTime().toString()}\\\": 29560801,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(210).toLocalTime().toString()}\\\": 30048295,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(225).toLocalTime().toString()}\\\": 30472571,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(240).toLocalTime().toString()}\\\": 30835607,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(255).toLocalTime().toString()}\\\": 31134122,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(270).toLocalTime().toString()}\\\": 31365532,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(285).toLocalTime().toString()}\\\": 31556020,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(300).toLocalTime().toString()}\\\": 31704574,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(315).toLocalTime().toString()}\\\": 31810480,\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.plusMinutes(330).toLocalTime().toString()}\\\": 31817147,\\n\" +\n" +
                            "                \"      \\\"${now.plusDays(1).toLocalDate().toString()} ${now.toLocalTime().toString()}\\\": 0\\n\" +\n" +
                            "                \"    },\\n\" +\n" +
                            "                \"    \\\"watt_hours_day\\\": {\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()}\\\": 31817147\\n\" +\n" +
                            "                \"    }\\n\" +\n" +
                            "                \"  },\\n\" +\n" +
                            "                \"  \\\"message\\\": {\\n\" +\n" +
                            "                \"    \\\"code\\\": 0,\\n\" +\n" +
                            "                \"    \\\"type\\\": \\\"success\\\",\\n\" +\n" +
                            "                \"    \\\"text\\\": \\\"\\\",\\n\" +\n" +
                            "                \"    \\\"info\\\": {\\n\" +\n" +
                            "                \"      \\\"latitude\\\": 51.4969,\\n\" +\n" +
                            "                \"      \\\"longitude\\\": -0.2773,\\n\" +\n" +
                            "                \"      \\\"place\\\": \\\"W3 8BZ Turnham Green, Hounslow London Boro, England, GB\\\",\\n\" +\n" +
                            "                \"      \\\"timezone\\\": \\\"Europe/London\\\"\\n\" +\n" +
                            "                \"    },\\n\" +\n" +
                            "                \"    \\\"ratelimit\\\": {\\n\" +\n" +
                            "                \"      \\\"period\\\": 60,\\n\" +\n" +
                            "                \"      \\\"limit\\\": 5,\\n\" +\n" +
                            "                \"      \\\"remaining\\\": 4\\n\" +\n" +
                            "                \"    }\\n\" +\n" +
                            "                \"  }\\n\" +\n" +
                            "                \"}\""
                    def responseBody = new ObjectMapper().readValue(content, ForecastSolarService.EstimateResponse.class)
                    requestContext.abortWith(
                            Response.ok(responseBody, MediaType.APPLICATION_JSON_TYPE).build()
                    )
                    break
            }

            requestContext.abortWith(Response.serverError().build())
        }
    }

    def "Test adding and removing asset with enabled attributes"() {
        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def config = defaultConfig()
        config << [(FORECAST_SOLAR_API_KEY): System.getenv(FORECAST_SOLAR_API_KEY)]
        def container = startContainer(config, defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def forecastSolarService = container.getService(ForecastSolarService.class)

        and: "the web client is configured to use the mock HTTP server"
        if (!forecastSolarService.resteasyClient.configuration.isRegistered(mockServer)) {
            forecastSolarService.resteasyClient.register(mockServer, Integer.MAX_VALUE)
        }

        expect: "a Future for calculation should exist and the asset should have filled in value for power and powerForecast"
        conditions.eventually {
            assert !forecastSolarService.calculationFutures.isEmpty()
            assert forecastSolarService.calculationFutures.get(managerTestSetup.electricitySolarAssetId) != null
            def solarAsset = assetStorageService.find(managerTestSetup.electricitySolarAssetId)
            assert solarAsset.getAttribute(ElectricityProducerSolarAsset.POWER).flatMap { it.value }.orElse(0d) != 0d
            assert solarAsset.getAttribute(ElectricityProducerSolarAsset.POWER_FORECAST).flatMap { it.value }.orElse(0d) != 0d
        }

        when: "an asset is added with includeForecastSolarService set to true"
        def newSolarAsset = new ElectricityProducerSolarAsset("SolarAsset")
        newSolarAsset.setParentId(managerTestSetup.electricityOptimisationAssetId)
        newSolarAsset.setRealm(managerTestSetup.realmEnergyTenant)
        newSolarAsset.setPanelAzimuth(0);
        newSolarAsset.setPanelPitch(30);
        newSolarAsset.setEfficiencyExport(100);
        newSolarAsset.setPowerExportMax(2.5);
        newSolarAsset.setLocation(new GeoJSONPoint(9.195295, 48.787418));
        newSolarAsset.setSetActualValueWithForecast(true);
        newSolarAsset.setIncludeForecastSolarService(true);
        newSolarAsset = assetStorageService.merge(newSolarAsset)

        then: "the assetId should be present in the calculationFutures"
        conditions.eventually {
            assert forecastSolarService.calculationFutures.get(newSolarAsset.getId()) != null
            newSolarAsset = assetStorageService.find(newSolarAsset.getId())
            assert newSolarAsset.getAttribute(ElectricityProducerSolarAsset.POWER).flatMap { it.value }.orElse(0d) != 0d
            assert newSolarAsset.getAttribute(ElectricityProducerSolarAsset.POWER_FORECAST).flatMap { it.value }.orElse(0d) != 0d
        }

        when: "an asset is added with includeForecastSolarService set to false"
        def newSolarAsset2 = new ElectricityProducerSolarAsset("SolarAsset2")
        newSolarAsset2.setParentId(managerTestSetup.electricityOptimisationAssetId)
        newSolarAsset2.setRealm(managerTestSetup.realmEnergyTenant)
        newSolarAsset2.setPanelAzimuth(0);
        newSolarAsset2.setPanelPitch(30);
        newSolarAsset2.setEfficiencyExport(100);
        newSolarAsset2.setPowerExportMax(2.5);
        newSolarAsset2.setLocation(new GeoJSONPoint(9.195275, 48.787418));
        newSolarAsset2.setSetActualValueWithForecast(false);
        newSolarAsset2.setIncludeForecastSolarService(false);
        newSolarAsset2 = assetStorageService.merge(newSolarAsset2)

        then: "the assetId shouldn't be present in the calculationFutures"
        conditions.eventually {
            assert forecastSolarService.calculationFutures.get(newSolarAsset2.getId()) == null
        }

        when: "an asset updated it's includeForecastSolarService to true"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(newSolarAsset2.getId(), ElectricityProducerSolarAsset.INCLUDE_FORECAST_SOLAR_SERVICE.name, true))

        then: "it should be present present in the calculationFutures"
        conditions.eventually {
            assert forecastSolarService.calculationFutures.get(newSolarAsset2.getId()) != null
            assert newSolarAsset2.getAttribute(ElectricityProducerSolarAsset.POWER).flatMap { it.value }.orElse(0d) == 0d
            assert newSolarAsset2.getAttribute(ElectricityProducerSolarAsset.POWER_FORECAST).flatMap { it.value }.orElse(0d) != 0d
        }
    }
}
