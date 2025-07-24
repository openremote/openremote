package org.openremote.test.microservices

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

class MicroserviceTest extends Specification implements ManagerContainerTrait {

    def "Test microservice registration and retrieval"() {

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
        def microserviceResource = getClientApiTarget(serverUri,  keycloakTestSetup.realmBuilding.getName(), accessToken).proxy(MicroserviceResource)
        def regularUserMicroserviceResource = getClientApiTarget(serverUri,  keycloakTestSetup.realmBuilding.getName(), regularUserAccessToken).proxy(MicroserviceResource)

        and: "test microservice objects are created"
        def energyService = new Microservice(
                "Energy Service",
                "energy-service",
                "https://demo.openremote.app/services/energy-service/ui",
                MicroserviceStatus.AVAILABLE,
                true
        )
        def weatherService = new Microservice(
                "Weather Service",
                "weather-service",
                "https://demo.openremote.app/services/weather-service/ui",
                MicroserviceStatus.AVAILABLE,
                false
        )

        then: "the microservice resources should be accessible"
        assert microserviceResource != null
        assert regularUserMicroserviceResource != null

        when: "the energy service is registered"
        def registrationResponse = microserviceResource.registerService(null, energyService)
        def energyInstanceId = registrationResponse.instanceId

        then: "the energy service should be registered successfully"
        assert registrationResponse != null
        assert registrationResponse.serviceId == "energy-service"
        assert energyInstanceId != null

        when: "all services are retrieved"
        def services = microserviceResource.getServices(null)

        then: "the returned services should contain the energy service"
        conditions.eventually {
            assert services != null
            assert services.length >= 1
            def service = services.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.AVAILABLE
            assert service.label == "Energy Service"
            assert service.multiTenancy == true
        }

        when: "time advances by 100 seconds (more than the 90 second TTL)"
        advancePseudoClock(100, TimeUnit.SECONDS, container)

        then: "the energy service should go unavailable due to TTL expiration"
        conditions.eventually {
            def servicesAfterTTL = microserviceResource.getServices(null)
            def service = servicesAfterTTL.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.UNAVAILABLE
        }

        when: "the weather service is registered"
        def weatherRegistrationResponse = microserviceResource.registerService(null, weatherService)
        def weatherInstanceId = weatherRegistrationResponse.instanceId

        then: "the weather service should be registered successfully"
        assert weatherRegistrationResponse != null
        assert weatherRegistrationResponse.serviceId == "weather-service"
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

        then: "both services should be unavailable due to TTL expiration"
        conditions.eventually {
            def servicesAfterSecondTTL = microserviceResource.getServices(null)
            def energyServiceInfo = servicesAfterSecondTTL.find { it.serviceId == "energy-service" }
            def weatherServiceInfo = servicesAfterSecondTTL.find { it.serviceId == "weather-service" }
            assert energyServiceInfo != null
            assert energyServiceInfo.status == MicroserviceStatus.UNAVAILABLE
            assert weatherServiceInfo != null
            assert weatherServiceInfo.status == MicroserviceStatus.UNAVAILABLE
        }

        when: "a heartbeat is sent for the weather service"
        def heartbeatResponse = microserviceResource.sendHeartbeat(null, weatherService.serviceId, weatherInstanceId)

        then: "the service should be marked as available"
        assert heartbeatResponse.status == Response.Status.NO_CONTENT.getStatusCode()
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

        when: "time advances by 60 seconds (less than the 90 second TTL)"
        advancePseudoClock(60, TimeUnit.SECONDS, container)

        then: "the weather service should still be available"
        conditions.eventually {
            def servicesAfter60Seconds = microserviceResource.getServices(null)
            def weatherServiceInfo = servicesAfter60Seconds.find { it.serviceId == "weather-service" }
            assert weatherServiceInfo != null
            assert weatherServiceInfo.status == MicroserviceStatus.AVAILABLE
        }

        when: "a heartbeat is sent for the energy service"
        def energyHeartbeatResponse = microserviceResource.sendHeartbeat(null, energyService.serviceId, energyInstanceId)

        then: "the service should be marked as available"
        assert energyHeartbeatResponse.status == Response.Status.NO_CONTENT.getStatusCode()
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
        def deregisterResponse = microserviceResource.deregisterService(null, energyService.serviceId, energyInstanceId)

        then: "the energy service should be deregistered successfully"
        assert deregisterResponse.status == Response.Status.NO_CONTENT.getStatusCode()

        and: "the energy service should no longer appear in the registry"
        conditions.eventually {
            def servicesAfterDeregister = microserviceResource.getServices(null)
            energyServiceInfo = servicesAfterDeregister.find { it.serviceId == "energy-service" }
            def weatherServiceInfo = servicesAfterDeregister.find { it.serviceId == "weather-service" }
            assert energyServiceInfo == null
            assert weatherServiceInfo != null
        }

        when: "a regular user tries to register the weather service"
        regularUserMicroserviceResource.registerService(null, weatherService)
        
        then: "the weather service should not be registered"
        thrown(NotAuthorizedException)

        when: "a regular user tries to deregister the weather service"
        def deregisterResp = regularUserMicroserviceResource.deregisterService(null, weatherService.serviceId, weatherInstanceId)

        then: "the weather service should not be deregistered"
        assert deregisterResp.status == Response.Status.UNAUTHORIZED.getStatusCode()

        def servicesAfterDeregisterAttempt = microserviceRegistryService.getServices()
        assert servicesAfterDeregisterAttempt.length == 1
        assert servicesAfterDeregisterAttempt.find { it.serviceId == "weather-service" } != null

        when: "a regular user tries to send a heartbeat for the weather service"
        def heartbeatResp = regularUserMicroserviceResource.sendHeartbeat(null, weatherService.serviceId, weatherInstanceId)

        then: "the weather service registration should not be updated"
        assert heartbeatResp.status == Response.Status.UNAUTHORIZED.getStatusCode()
    }
}
