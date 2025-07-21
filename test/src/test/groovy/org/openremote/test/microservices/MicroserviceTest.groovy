package org.openremote.test.microservices

import org.openremote.manager.microservices.MicroserviceRegistryService
import org.openremote.manager.setup.SetupService
import org.openremote.model.microservices.Microservice
import org.openremote.model.microservices.MicroserviceResource
import org.openremote.model.microservices.MicroserviceStatus
import org.openremote.setup.integration.KeycloakTestSetup

import jakarta.ws.rs.ForbiddenException
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.model.Constants.*

class MicroserviceTest extends Specification implements ManagerContainerTrait {

    def "Test microservice registration and retrieval"() {

        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def microserviceRegistryService = container.getService(MicroserviceRegistryService.class)
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the users are authenticated"

        // regular user for non-super-user testing
        def regularUserAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        // super user, for all operations
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token


        then: "the users have been authenticated and the tokens are retrieved"
        conditions.eventually {
            assert accessToken != null
            assert regularUserAccessToken != null
        }

        when: "the microservice resource is set up"
        def serverUri = serverUri(serverPort)
        def microserviceResource = getClientApiTarget(serverUri, MASTER_REALM, accessToken).proxy(MicroserviceResource)
        def regularUserMicroserviceResource = getClientApiTarget(serverUri, MASTER_REALM, regularUserAccessToken).proxy(MicroserviceResource)

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

        when: "the registered services are retrieved"
        def services = microserviceResource.getServices(null)

        then: "there should be no registered services available"
        conditions.eventually {
            assert services.length < 1
        }

        when: "a service is registered"
        def registrationResult = microserviceResource.register(null, energyService)

        then: "the service should be registered successfully"
        assert registrationResult

        when: "the registered services are retrieved"
        services = microserviceResource.getServices(null)

        then: "the retrieved services should contain the newly registered service"
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

        and: "the expiration check runs"
        microserviceRegistryService.expirationCheck()

        then: "the service should go unavailable due to TTL expiration"
        conditions.eventually {
            def servicesAfterTTL = microserviceResource.getServices(null)
            def service = servicesAfterTTL.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.UNAVAILABLE
        }

        when: "the service sends a heartbeat"
        def heartbeatResult = microserviceResource.heartbeat(null, "energy-service")

        then: "the service should be updated successfully"
        assert heartbeatResult

        and: "the service should be available again"
        conditions.eventually {
            def servicesAfterHeartbeat = microserviceResource.getServices(null)
            def service = servicesAfterHeartbeat.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.AVAILABLE
        }

        when: "another service is registered"
        def weatherRegistrationResult = microserviceResource.register(null, weatherService)

        then: "the second service should be registered successfully"
        assert weatherRegistrationResult

        when: "all services are retrieved"
        def allServices = microserviceResource.getServices(null)

        then: "there should be 2 services, with both being available"
        conditions.eventually {
            assert allServices.length >= 2
            energyService = allServices.find { it.serviceId == "energy-service" }
            weatherService = allServices.find { it.serviceId == "weather-service" }
            assert energyService != null
            assert energyService.status == MicroserviceStatus.AVAILABLE
            assert weatherService != null
            assert weatherService.status == MicroserviceStatus.AVAILABLE
        }


        when: "time advances by 100 seconds again without updating services"
        advancePseudoClock(100, TimeUnit.SECONDS, container)

        and: "the expiration check runs"
        microserviceRegistryService.expirationCheck()

        then: "both services should eventually go unavailable due to TTL expiration"
        conditions.eventually {
            def servicesAfterSecondTTL = microserviceResource.getServices(null)
            energyService = servicesAfterSecondTTL.find { it.serviceId == "energy-service" }
            weatherService = servicesAfterSecondTTL.find { it.serviceId == "weather-service" }
            assert energyService != null
            assert energyService.status == MicroserviceStatus.UNAVAILABLE
            assert weatherService != null
            assert weatherService.status == MicroserviceStatus.UNAVAILABLE
        }

        when: "both services send a heartbeat"
        def energyHeartbeatResult = microserviceResource.heartbeat(null, "energy-service")
        def weatherHeartbeatResult = microserviceResource.heartbeat(null, "weather-service")

        then: "both services should be updated successfully"
        assert energyHeartbeatResult
        assert weatherHeartbeatResult

        and: "both services should be available again"
        conditions.eventually {
            def servicesAfterSecondHeartbeat = microserviceResource.getServices(null)
            energyService = servicesAfterSecondHeartbeat.find { it.serviceId == "energy-service" }
            weatherService = servicesAfterSecondHeartbeat.find { it.serviceId == "weather-service" }
            assert energyService != null
            assert energyService.status == MicroserviceStatus.AVAILABLE
            assert weatherService != null
            assert weatherService.status == MicroserviceStatus.AVAILABLE
        }

        when: "a service is deregistered"
        def deregisterResult = microserviceResource.deregister(null, energyService.serviceId)

        then: "the service should be deregistered successfully"
        assert deregisterResult

        and: "the service should no longer appear in the registry"
        conditions.eventually {
            def servicesAfterDeregister = microserviceResource.getServices(null)
            energyService = servicesAfterDeregister.find { it.serviceId == "energy-service" }
            weatherService = servicesAfterDeregister.find { it.serviceId == "weather-service" }
            assert energyService == null
            assert weatherService != null
        }


        // Permission integration tests, a regular user should only be able to list services and not register/deregister/heartbeat
        when: "the regular user tries to deregister a service"
        regularUserMicroserviceResource.deregister(null, weatherService.serviceId)

        then: "the service should not be deregistered as the user is not a super user"
        thrown(ForbiddenException)
        def servicesAfterUnregisterAttempt = microserviceRegistryService.getServices()
        assert servicesAfterUnregisterAttempt.length == 1
        assert servicesAfterUnregisterAttempt.find { it.serviceId == "weather-service" } != null

        when: "the regular user tries to send a heartbeat"
        regularUserMicroserviceResource.heartbeat(null, weatherService.serviceId)

        then: "the service should not be updated as the user is not a super user"
        thrown(ForbiddenException)

        when: "the regular user tries to register a service"
        regularUserMicroserviceResource.register(null, weatherService)

        then: "the service should not be registered as the user is not a super user"
        thrown(ForbiddenException)

        when: "the regular user tries to get services for listing purposes"
        def regularUserServices = regularUserMicroserviceResource.getServices(null)

        then: "the registered services should be returned"
        assert regularUserServices != null
        assert regularUserServices.length == 1
    }
}
