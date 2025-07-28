package org.openremote.test.microservices

import jakarta.ws.rs.WebApplicationException
import org.openremote.manager.microservices.MicroserviceRegistryService
import org.openremote.manager.setup.SetupService
import org.openremote.model.microservices.Microservice
import org.openremote.model.microservices.MicroserviceResource
import org.openremote.model.microservices.MicroserviceStatus
import org.openremote.setup.integration.KeycloakTestSetup

import jakarta.ws.rs.NotAuthorizedException


import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import jakarta.ws.rs.core.Response

import java.util.concurrent.TimeUnit

import static org.openremote.model.Constants.*

class MicroserviceResourceTest extends Specification implements ManagerContainerTrait {

    def "Test microservice resource operations"() {

        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def microserviceRegistryService = container.getService(MicroserviceRegistryService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the users are authenticated"

        // regular user for non-service-user testing
        def regularUserAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        // service user for all operations
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.getName(),
                keycloakTestSetup.serviceUser.username,
                keycloakTestSetup.serviceUser.secret
        ).token

        then: "the users have been authenticated and the tokens are retrieved"
        conditions.eventually {
            assert accessToken != null
            assert regularUserAccessToken != null
        }

        when: "the microservice resource is set up"
        def serverUri = serverUri(serverPort)
        def microserviceResource = getClientApiTarget(serverUri, keycloakTestSetup.realmBuilding.getName(), accessToken).proxy(MicroserviceResource)
        def regularUserMicroserviceResource = getClientApiTarget(serverUri, keycloakTestSetup.realmBuilding.getName(), regularUserAccessToken).proxy(MicroserviceResource)

        and: "test microservice objects are created"
        def energyService = new Microservice(
                "Energy Service",
                "energy-service",
                "https://demo.openremote.app/services/energy-service/ui",
                MicroserviceStatus.AVAILABLE
        )
        def weatherService = new Microservice(
                "Weather Service",
                "weather-service",
                "https://demo.openremote.app/services/weather-service/ui",
                MicroserviceStatus.AVAILABLE
        )

        then: "the microservice resources should be accessible"
        assert microserviceResource != null
        assert regularUserMicroserviceResource != null


        when: "all services are retrieved"
        def services = microserviceResource.getServices(null)

        then: "the returned services should be empty"
        conditions.eventually {
            assert services != null
            assert services.length == 0
        }

        when: "the energy service is registered"
        def registeredEnergyService = microserviceResource.registerService(null, energyService)

        then: "the energy service should be registered successfully and should have an instanceId"
        assert registeredEnergyService != null
        def energyInstanceId = registeredEnergyService.getInstanceId()
        assert energyInstanceId != null

        when: "all services are retrieved"
        services = microserviceResource.getServices(null)

        then: "the returned services should contain the energy service"
        conditions.eventually {
            assert services != null
            assert services.length >= 1
            def service = services.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.AVAILABLE
            assert service.label == "Energy Service"
        }

        when: "the registered energy service is re-registered"
        microserviceResource.registerService(null, registeredEnergyService)

        then: "the energy service should not be registered again due to instanceId collision"
        WebApplicationException ex = thrown()
        ex.response.status == Response.Status.CONFLICT.getStatusCode()

        when: "time advances by 100 seconds (more than the 90 second lease duration)"
        advancePseudoClock(100, TimeUnit.SECONDS, container)

        then: "the energy service should go unavailable due to lease expiration"
        conditions.eventually {
            def servicesAfterLeaseDuration = microserviceResource.getServices(null)
            def service = servicesAfterLeaseDuration.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.UNAVAILABLE
        }

        when: "the weather service is registered"
        def registeredWeatherService = microserviceResource.registerService(null, weatherService)

        then: "the weather service should be registered successfully and should have an instanceId"
        assert registeredWeatherService != null
        def weatherInstanceId = registeredWeatherService.getInstanceId()
        assert weatherInstanceId != null

        when: "all services are retrieved"
        def allServices = microserviceResource.getServices(null)

        then: "the returned services should contain the energy service as unavailable and the weather service as available"
        conditions.eventually {
            assert allServices.length >= 2
            def energyServiceInfo = allServices.find { it.serviceId == "energy-service" }
            def weatherServiceInfo = allServices.find { it.serviceId == "weather-service" }
            assert energyServiceInfo != null
            assert energyServiceInfo.status == MicroserviceStatus.UNAVAILABLE
            assert weatherServiceInfo != null
            assert weatherServiceInfo.status == MicroserviceStatus.AVAILABLE
        }

        when: "time advances by 100 seconds again without sending a heartbeat for any service"
        advancePseudoClock(100, TimeUnit.SECONDS, container)

        then: "both services should be unavailable due to lease expiration"
        conditions.eventually {
            def servicesAfterSecondLeaseDuration = microserviceResource.getServices(null)
            def energyServiceInfo = servicesAfterSecondLeaseDuration.find { it.serviceId == "energy-service" }
            def weatherServiceInfo = servicesAfterSecondLeaseDuration.find { it.serviceId == "weather-service" }
            assert energyServiceInfo != null
            assert energyServiceInfo.status == MicroserviceStatus.UNAVAILABLE
            assert weatherServiceInfo != null
            assert weatherServiceInfo.status == MicroserviceStatus.UNAVAILABLE
        }

        when: "a heartbeat is sent for the weather service"
        microserviceResource.heartbeat(null, weatherService.serviceId, weatherInstanceId)

        then: "the service should be marked as available"
        conditions.eventually {
            def servicesAfterHeartbeat = microserviceResource.getServices(null)
            def weatherServiceInfo = servicesAfterHeartbeat.find { it.serviceId == "weather-service" }
            assert weatherServiceInfo != null
            assert weatherServiceInfo.status == MicroserviceStatus.AVAILABLE
        }

        and: "the energy service should still be unavailable"
        def energyServiceInfo = microserviceResource.getServices(null).find { it.serviceId == "energy-service" }
        assert energyServiceInfo != null
        assert energyServiceInfo.status == MicroserviceStatus.UNAVAILABLE

        when: "time advances by 60 seconds (less than the 90 second lease duration)"
        advancePseudoClock(60, TimeUnit.SECONDS, container)

        then: "the weather service should still be available"
        conditions.eventually {
            def servicesAfter60Seconds = microserviceResource.getServices(null)
            def weatherServiceInfo = servicesAfter60Seconds.find { it.serviceId == "weather-service" }
            assert weatherServiceInfo != null
            assert weatherServiceInfo.status == MicroserviceStatus.AVAILABLE
        }

        when: "a heartbeat is sent for the energy service"
        microserviceResource.heartbeat(null, energyService.serviceId, energyInstanceId)

        then: "the service should be marked as available"
        conditions.eventually {
            def servicesAfterHeartbeat = microserviceResource.getServices(null)
            energyServiceInfo = servicesAfterHeartbeat.find { it.serviceId == "energy-service" }
            assert energyServiceInfo != null
            assert energyServiceInfo.status == MicroserviceStatus.AVAILABLE
        }

        and: "the weather service should also still be available"
        conditions.eventually {
            def servicesAfterHeartbeat = microserviceResource.getServices(null)
            def weatherServiceInfo = servicesAfterHeartbeat.find { it.serviceId == "weather-service" }
            assert weatherServiceInfo != null
            assert weatherServiceInfo.status == MicroserviceStatus.AVAILABLE
        }

        when: "the energy service is deregistered"
        microserviceResource.deregisterService(null, energyService.serviceId, energyInstanceId)

        then: "the energy service should no longer appear in the registry"
        conditions.eventually {
            def servicesAfterDeregister = microserviceResource.getServices(null)
            energyServiceInfo = servicesAfterDeregister.find { it.serviceId == "energy-service" }
            def weatherServiceInfo = servicesAfterDeregister.find { it.serviceId == "weather-service" }
            assert energyServiceInfo == null
            assert weatherServiceInfo != null
        }

        when: "a heartbeat is sent for the now deregistered energy service"
        microserviceResource.heartbeat(null, energyService.serviceId, energyInstanceId)

        then: "the heartbeat should fail with a not found error"
        ex = thrown()
        ex.response.status == Response.Status.NOT_FOUND.getStatusCode()

        when: "another deregister attempt is made for the energy service"
        microserviceResource.deregisterService(null, energyService.serviceId, energyInstanceId)

        then: "the deregister should fail with a not found error"
        ex = thrown()
        ex.response.status == Response.Status.NOT_FOUND.getStatusCode()

        when: "a regular user tries to register the energy service"
        regularUserMicroserviceResource.registerService(null, energyService)

        then: "the register should fail with a unauthorized error"
        ex = thrown()
        ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        when: "a regular user tries to deregister the weather service"
        regularUserMicroserviceResource.deregisterService(null, weatherService.serviceId, weatherInstanceId)

        then: "the deregister should fail with a unauthorized error"
        ex = thrown()
        ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        and: "the weather service should still be available"
        conditions.eventually {
            def servicesAfterDeregisterAttempt = microserviceRegistryService.getServices()
            assert servicesAfterDeregisterAttempt.length == 1
            assert servicesAfterDeregisterAttempt.find { it.serviceId == "weather-service" } != null
        }

        when: "a regular user tries to send a heartbeat for the weather service"
        regularUserMicroserviceResource.heartbeat(null, weatherService.serviceId, weatherInstanceId)

        then: "the heartbeat should fail with a unauthorized error"
        ex = thrown()
        ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()
    }
}
