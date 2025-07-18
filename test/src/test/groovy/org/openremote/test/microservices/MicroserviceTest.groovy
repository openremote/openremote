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

        when: "a service is registered"
        def registrationResult = microserviceResource.registerService(null, energyService)

        then: "the service should be registered successfully"
        assert registrationResult

        when: "the service is retrieved"
        def services = microserviceResource.getServices(null)

        then: "the registered service should be available"
        conditions.eventually {
            assert services != null
            assert services.length >= 1
            def service = services.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.AVAILABLE
            assert service.label == "Energy Service"
            assert service.multiTenancy == true
        }

        when: "the service registration is updated"
        energyService.setLabel("Updated Energy Service")
        def updateResult = microserviceResource.registerService(null, energyService)

        then: "the service should be updated successfully"
        assert updateResult

        and: "the updated service should be reflected in the registry"
        conditions.eventually {
            def updatedServices = microserviceResource.getServices(null)
            def service = updatedServices.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.label == "Updated Energy Service"
            assert service.url == "https://demo.openremote.app/services/energy-service/ui"
        }

        when: "time advances by 70 seconds (more than the 60 second TTL)"
        advancePseudoClock(70, TimeUnit.SECONDS, container)

        and: "the expiration check runs"
        microserviceRegistryService.expirationCheck()

        then: "the service should go unavailable due to TTL expiration"
        conditions.eventually {
            def servicesAfterTTL = microserviceResource.getServices(null)
            def service = servicesAfterTTL.find { it.serviceId == "energy-service" }
            assert service != null
            assert service.status == MicroserviceStatus.UNAVAILABLE
        }

        when: "another service is registered"
        def weatherRegistrationResult = microserviceResource.registerService(null, weatherService)

        then: "the second service should be registered successfully"
        assert weatherRegistrationResult

        when: "all services are retrieved"
        def allServices = microserviceResource.getServices(null)

        then: "there should be 2 services, with one unavailable and one available"
        conditions.eventually {
            assert allServices.length >= 2
            energyService = allServices.find { it.serviceId == "energy-service" }
            weatherService = allServices.find { it.serviceId == "weather-service" }
            assert energyService != null
            assert energyService.status == MicroserviceStatus.UNAVAILABLE
            assert weatherService != null
            assert weatherService.status == MicroserviceStatus.AVAILABLE
        }

        when: "both services are updated"
        energyService.setStatus(MicroserviceStatus.AVAILABLE)
        def energyUpdateResult = microserviceResource.registerService(null, energyService)
        def weatherUpdateResult = microserviceResource.registerService(null, weatherService)

        then: "both services should be updated successfully"
        assert energyUpdateResult
        assert weatherUpdateResult

        and: "both services should be available"
        conditions.eventually {
            def bothServicesUpdated = microserviceResource.getServices(null)
            energyService = bothServicesUpdated.find { it.serviceId == "energy-service" }
            weatherService = bothServicesUpdated.find { it.serviceId == "weather-service" }
            assert energyService != null
            assert energyService.status == MicroserviceStatus.AVAILABLE
            assert weatherService != null
            assert weatherService.status == MicroserviceStatus.AVAILABLE
        }

        when: "time advances by 70 seconds again without updating services"
        advancePseudoClock(70, TimeUnit.SECONDS, container)

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

        when: "a service is unregistered"
        def unregisterResult = microserviceResource.unregisterService(null, energyService)

        then: "the service should be unregistered successfully"
        assert unregisterResult

        and: "the service should no longer appear in the registry"
        conditions.eventually {
            def servicesAfterUnregister = microserviceResource.getServices(null)
            energyService = servicesAfterUnregister.find { it.serviceId == "energy-service" }
            weatherService = servicesAfterUnregister.find { it.serviceId == "weather-service" }
            assert energyService == null
            assert weatherService != null
        }


        // Permission integration tests, a regular user should only be able to list services and not update/register/unregister

        when: "the regular user tries to unregister a service"
        regularUserMicroserviceResource.unregisterService(null, weatherService)

        then: "the service should not be unregistered as the user is not a super user"
        thrown(ForbiddenException)
        def servicesAfterUnregisterAttempt = microserviceRegistryService.getServices()
        assert servicesAfterUnregisterAttempt.length == 1
        assert servicesAfterUnregisterAttempt.find { it.serviceId == "weather-service" } != null

        when: "the regular user tries to update a service"
        regularUserMicroserviceResource.registerService(null, weatherService)

        then: "the service should not be updated as the user is not a super user"
        thrown(ForbiddenException)

        when: "the regular user tries to get services for listing purposes"
        def regularUserServices = regularUserMicroserviceResource.getServices(null)

        then: "the registered services should be returned"
        assert regularUserServices != null
        assert regularUserServices.length == 1
    }
}
