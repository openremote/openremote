package org.openremote.test.energy

import com.fasterxml.jackson.databind.ObjectMapper
import org.jboss.resteasy.spi.ResteasyUriInfo
import org.jboss.resteasy.util.BasicAuthHelper
import org.openremote.container.web.OAuthServerResponse
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.energy.ForecastSolarService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.ElectricityProducerSolarAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.OAuthGrant
import org.openremote.model.auth.OAuthPasswordGrant
import org.openremote.model.auth.OAuthRefreshTokenGrant
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.Form
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import static org.openremote.manager.energy.ForecastSolarService.FORECAST_SOLAR_API_KEY

class ForecastSolarServiceTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {


        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri

            switch (requestUri.host)
            {
                case "api.forecast.solar":
                    def now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
                    def content = "\"{\\n\" +\n" +
                            "                \"  \\\"result\\\": {\\n\" +\n" +
                            "                \"    \\\"watts\\\": {\\n\" +\n" +
                            "                \"      \\\"${now.toLocalDate().toString()} ${now.toLocalTime().toString()}\\\": 0,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 10:34:00\\\": 780000,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:00:00\\\": 3904036,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:15:00\\\": 3854598,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:30:00\\\": 3746874,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:45:00\\\": 3675780,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:00:00\\\": 3553500,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:15:00\\\": 3439679,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:30:00\\\": 3278604,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:45:00\\\": 3137450,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:00:00\\\": 2954064,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:15:00\\\": 2739056,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:30:00\\\": 2493609,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:45:00\\\": 2254420,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:00:00\\\": 1949976,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:15:00\\\": 1697104,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:30:00\\\": 1452142,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:45:00\\\": 1194060,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:00:00\\\": 925642,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:15:00\\\": 761949,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:30:00\\\": 594217,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:45:00\\\": 423624,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:50:00\\\": 80000,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:54:00\\\": 0,\\n\" +\n" +
                            "                \"      \\\"2021-09-02 06:09:00\\\": 0\\n\" +\n" +
                            "                \"    },\\n\" +\n" +
                            "                \"    \\\"watt_hours\\\": {\\n\" +\n" +
                            "                \"      \\\"2021-09-01 06:07:00\\\": 0,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 10:34:00\\\": 3471000,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:00:00\\\": 20778893,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:15:00\\\": 21742542,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:30:00\\\": 22679261,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 15:45:00\\\": 23598206,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:00:00\\\": 24486581,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:15:00\\\": 25346501,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:30:00\\\": 26166152,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 16:45:00\\\": 26950514,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:00:00\\\": 27689030,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:15:00\\\": 28373794,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:30:00\\\": 28997196,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 17:45:00\\\": 29560801,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:00:00\\\": 30048295,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:15:00\\\": 30472571,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:30:00\\\": 30835607,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 18:45:00\\\": 31134122,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:00:00\\\": 31365532,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:15:00\\\": 31556020,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:30:00\\\": 31704574,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:45:00\\\": 31810480,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:50:00\\\": 31817147,\\n\" +\n" +
                            "                \"      \\\"2021-09-01 19:54:00\\\": 31817147,\\n\" +\n" +
                            "                \"      \\\"2021-09-02 06:09:00\\\": 0\\n\" +\n" +
                            "                \"    },\\n\" +\n" +
                            "                \"    \\\"watt_hours_day\\\": {\\n\" +\n" +
                            "                \"      \\\"2021-09-01\\\": 31817147\\n\" +\n" +
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
//                    def response = new ObjectMapper().readValue(, ForecastSolarService.EstimateResponse.class)

                    // OAuth token request extract the grant info
                    def grant = ((Form)requestContext.getEntity()).asMap()
                    if (grant.getFirst(OAuthGrant.VALUE_KEY_GRANT_TYPE) == "password"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_ID ) == "client1"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_SECRET ) == "secret1"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_SCOPE ) == "scope1 scope2"
                            && grant.getFirst(OAuthPasswordGrant.VALUE_KEY_USERNAME) == "testuser"
                            && grant.getFirst(OAuthPasswordGrant.VALUE_KEY_PASSWORD) == "password1") {
                        accessToken = "accesstoken" + accessTokenCount++
                        def response = new OAuthServerResponse()
                        response.accessToken = accessToken
                        response.expiresIn = requestPath == "https://oauthserver/token" ? 5 : 100
                        response.tokenType = "Bearer"

                        // Include refresh token if configured to support it
                        if (supportsRefresh) {
                            refreshToken = "refreshtoken" + accessTokenCount
                            response.refreshToken = refreshToken
                        }

                        requestContext.abortWith(
                                Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build()
                        )
                        return
                    } else if (grant.getFirst(OAuthGrant.VALUE_KEY_GRANT_TYPE) == "client_credentials"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_ID ) == "client1"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_SECRET ) == "secret1"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_SCOPE ) == "scope1 scope2") {
                        accessToken = "accesstoken" + accessTokenCount++
                        def response = new OAuthServerResponse()
                        response.accessToken = accessToken
                        response.expiresIn = requestPath == "https://oauthserver/token" ? 1 : 100
                        response.tokenType = "Bearer"

                        requestContext.abortWith(
                                Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build()
                        )
                        return
                    } else if (supportsRefresh && grant.getFirst(OAuthGrant.VALUE_KEY_GRANT_TYPE) == "refresh_token"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_ID ) == "client1"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_SECRET ) == "secret1"
                            && grant.getFirst(OAuthGrant.VALUE_KEY_SCOPE ) == "scope1 scope2"
                            && grant.getFirst(OAuthRefreshTokenGrant.REFRESH_TOKEN_GRANT_TYPE) == refreshToken) {
                        refreshTokenCount++
                        accessToken = "accesstoken" + accessTokenCount++
                        refreshToken = "refreshtoken" + accessTokenCount
                        def response = new OAuthServerResponse()
                        response.accessToken = accessToken
                        response.refreshToken = refreshToken
                        response.expiresIn = requestPath == "https://oauthserver/token" ? 1 : 100
                        response.tokenType = "Bearer"

                        requestContext.abortWith(
                                Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build()
                        )
                        return
                    }
                    break
            }

            requestContext.abortWith(Response.serverError().build())
        }
    }

    def "Test adding and removing asset with enabled attributes"() {
        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def config = defaultConfig()
        config << [(FORECAST_SOLAR_API_KEY) : System.getenv(FORECAST_SOLAR_API_KEY)]
        def container = startContainer(config, defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def forecastSolarService = container.getService(ForecastSolarService.class)

        expect: "a Future for calculation should exist and the asset should have filled in value for power and powerForecast"
        conditions.eventually {
            assert !forecastSolarService.calculationFutures.isEmpty()
            assert forecastSolarService.calculationFutures.get(managerTestSetup.electricitySolarAssetId) != null
            def solarAsset = assetStorageService.find(managerTestSetup.electricitySolarAssetId)
            assert solarAsset.getAttribute(ElectricityProducerSolarAsset.POWER).flatMap{it.value}.orElse(0d) != 0d
            assert solarAsset.getAttribute(ElectricityProducerSolarAsset.POWER_FORECAST).flatMap{it.value}.orElse(0d) != 0d
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
            assert newSolarAsset.getAttribute(ElectricityProducerSolarAsset.POWER).flatMap{it.value}.orElse(0d) != 0d
            assert newSolarAsset.getAttribute(ElectricityProducerSolarAsset.POWER_FORECAST).flatMap{it.value}.orElse(0d) != 0d
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
            assert newSolarAsset2.getAttribute(ElectricityProducerSolarAsset.POWER).flatMap{it.value}.orElse(0d) == 0d
            assert newSolarAsset2.getAttribute(ElectricityProducerSolarAsset.POWER_FORECAST).flatMap{it.value}.orElse(0d) != 0d
        }
    }
}
