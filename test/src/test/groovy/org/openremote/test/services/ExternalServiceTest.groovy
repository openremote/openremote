package org.openremote.test.services

import jakarta.ws.rs.WebApplicationException
import org.openremote.manager.services.ExternalServiceRegistryService
import org.openremote.manager.setup.SetupService
import org.openremote.model.services.ExternalService
import org.openremote.model.services.ExternalServiceResource
import org.openremote.model.services.ExternalServiceStatus
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import jakarta.ws.rs.core.Response

import java.util.concurrent.TimeUnit

import static org.openremote.model.Constants.*

class ExternalServiceTest extends Specification implements ManagerContainerTrait {

    def "Test external service resource and local manager external service registry logic"() {

        given: "the server container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def externalServiceRegistryService = container.getService(ExternalServiceRegistryService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        WebApplicationException ex


        when: "the users are authenticated"
        // regular user for non-service-user testing but with read:services scope
        def regularUserAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        // building service user for single tenant logic
        def buildingServiceUserAccessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.getName(),
                keycloakTestSetup.serviceUser.username,
                keycloakTestSetup.serviceUser.secret
        ).token

        def buildingServiceUser2AccessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.getName(),
                keycloakTestSetup.serviceUser2.username,
                keycloakTestSetup.serviceUser2.secret
        ).token

        // master service user for multi-tenant logic
        def superServiceUserAccessToken = authenticate(
                container,
                MASTER_REALM,
                keycloakTestSetup.superServiceUser.username,
                keycloakTestSetup.superServiceUser.secret
        ).token

        then: "the users have been authenticated and the tokens are retrieved"
        conditions.eventually {
            assert buildingServiceUserAccessToken != null
            assert regularUserAccessToken != null
            assert superServiceUserAccessToken != null
        }

        when: "the external service resource is set up"
        def serverUri = serverUri(serverPort)
        def buildingExternalServiceResource = getClientApiTarget(serverUri, keycloakTestSetup.realmBuilding.getName(), buildingServiceUserAccessToken).proxy(ExternalServiceResource)
        def buildingExternalServiceResource2 = getClientApiTarget(serverUri, keycloakTestSetup.realmBuilding.getName(), buildingServiceUser2AccessToken).proxy(ExternalServiceResource)
        def regularUserExternalServiceResource = getClientApiTarget(serverUri, MASTER_REALM, regularUserAccessToken).proxy(ExternalServiceResource)
        def superServiceUserExternalServiceResource = getClientApiTarget(serverUri, MASTER_REALM, superServiceUserAccessToken).proxy(ExternalServiceResource)

        then: "the external service resources should be accessible"
        assert buildingExternalServiceResource != null
        assert regularUserExternalServiceResource != null
        assert superServiceUserExternalServiceResource != null


        // === Tests related to interacting with the external services resource as a service user for a specific realm ===

        when: "the building service user registers an external service"
        def buildingExternalService = new ExternalService(
                serviceId: "building-energy-management",
                label: "Building Energy Management Service",
                homepageUrl: "https://local.test/services/building-energy-management/ui",
                status: ExternalServiceStatus.AVAILABLE,
        )
        def registeredBuildingExternalService = buildingExternalServiceResource.registerService(null, buildingExternalService)

        then: "the building external service should be registered and the registered external service should have an instanceId assigned"
        assert registeredBuildingExternalService.instanceId != 0

        and: "the building external service should be included in the list of external services for the building realm"
        def buildingServices = buildingExternalServiceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())
        assert buildingServices != null
        assert buildingServices.size() == 1
        assert buildingServices[0].serviceId == buildingExternalService.serviceId
        assert buildingServices[0].label == buildingExternalService.label
        assert buildingServices[0].realm == buildingExternalService.realm
        assert buildingServices[0].homepageUrl == buildingExternalService.homepageUrl
        assert buildingServices[0].status == buildingExternalService.status

        when: "time advanced by 70 seconds, longer than the default registration lease duration"
        advancePseudoClock(70, TimeUnit.SECONDS)

        then: "the building external service should now be marked as unavailable"
        externalServiceRegistryService.markExpiredInstancesAsUnavailable()
        def buildingServiceAfterInstanceExpiry = buildingExternalServiceResource.getService(null, registeredBuildingExternalService.serviceId, registeredBuildingExternalService.instanceId)
        assert buildingServiceAfterInstanceExpiry.status == ExternalServiceStatus.UNAVAILABLE

        when: "the building service user sends a heartbeat"
        buildingExternalServiceResource.heartbeat(null, registeredBuildingExternalService.serviceId, registeredBuildingExternalService.instanceId)

        then: "the building external service should now be marked as available"
        def buildingServiceAfterHeartbeat = buildingExternalServiceResource.getService(null, registeredBuildingExternalService.serviceId, registeredBuildingExternalService.instanceId)
        assert buildingServiceAfterHeartbeat.status == ExternalServiceStatus.AVAILABLE
        when: "another service user tries to send a heartbeat for the same external service"
        buildingExternalServiceResource2.heartbeat(null, registeredBuildingExternalService.serviceId, registeredBuildingExternalService.instanceId)

        then: "the heartbeat should be rejected, and a forbidden response received"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        when: "another service user tries to de-register the same external service"
        buildingExternalServiceResource2.deregisterService(null, registeredBuildingExternalService.serviceId, registeredBuildingExternalService.instanceId)

        then: "the de-registration should be rejected, and a forbidden response received"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        when: "the building service user de-registers the external service"
        buildingExternalServiceResource.deregisterService(null, registeredBuildingExternalService.serviceId, registeredBuildingExternalService.instanceId)

        then: "the building external service should no longer be included in the list of external services for the building realm"
        def buildingServicesAfterDeregister = buildingExternalServiceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())
        assert buildingServicesAfterDeregister != null
        assert buildingServicesAfterDeregister.size() == 0

        when: "the building service user tries to retrieve the now deregistered external service"
        buildingExternalServiceResource.getService(null, registeredBuildingExternalService.serviceId, registeredBuildingExternalService.instanceId)

        then: "the building service user should receive a 404 not found response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.NOT_FOUND.getStatusCode()

        when: "the building service user tries to retrieve the external services from an inaccessible realm"
        buildingExternalServiceResource.getServices(null, MASTER_REALM)

        then: "the building service user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        // === Tests related to global service registration and management ===

        when: "the super user registers a global service (master realm + user is super user)"
        def globalExternalService = new ExternalService(
                serviceId: "forecasting-service",
                label: "Forecasting Service",
                homepageUrl: "https://local.test/services/forecasting-service/ui",
                status: ExternalServiceStatus.AVAILABLE,
        )
        def registeredGlobalExternalService = superServiceUserExternalServiceResource.registerGlobalService(null, globalExternalService)

        then: "the global external service should be registered and the registered external service should have an instanceId assigned"
        assert registeredGlobalExternalService.instanceId != null

        and: "the global external service should be included in the list of global services"
        def globalServices = superServiceUserExternalServiceResource.getGlobalServices()
        assert globalServices != null
        assert globalServices.size() == 1
        assert globalServices[0].serviceId == globalExternalService.serviceId

        when: "time advanced by 70 seconds, longer than the default registration lease duration"
        advancePseudoClock(70, TimeUnit.SECONDS)

        then: "the global external service should now be marked as unavailable"
        externalServiceRegistryService.markExpiredInstancesAsUnavailable()
        def globalServiceAfterInstanceExpiry = superServiceUserExternalServiceResource.getService(null, registeredGlobalExternalService.serviceId, registeredGlobalExternalService.instanceId)
        assert globalServiceAfterInstanceExpiry.status == ExternalServiceStatus.UNAVAILABLE

        when: "the super user sends a heartbeat for the global external service"
        superServiceUserExternalServiceResource.heartbeat(null, registeredGlobalExternalService.serviceId, registeredGlobalExternalService.instanceId)

        then: "the global external service should now be marked as available"
        def globalServiceAfterHeartbeat = superServiceUserExternalServiceResource.getService(null, registeredGlobalExternalService.serviceId, registeredGlobalExternalService.instanceId)
        assert globalServiceAfterHeartbeat.status == ExternalServiceStatus.AVAILABLE


        // === Tests related to interacting with global services as a service user from a realm different from the master realm ===
        
        when: "the building service user tries to retrieve the global services"
        def globalServicesForBuildingRealm = buildingExternalServiceResource.getGlobalServices()

        then: "the building service user should receive the global services which includes the newly registered global forecasting service"
        assert globalServicesForBuildingRealm != null
        assert globalServicesForBuildingRealm.size() == 1
        assert globalServicesForBuildingRealm[0].serviceId == registeredGlobalExternalService.serviceId

        when: "the building service user registers a new realm-bound external service"
        def buildingExternalService2 = new ExternalService(
                serviceId: "building-energy-management-2",
                label: "Building Energy Management Service 2",
                homepageUrl: "https://local.test/services/building-energy-management-2/ui",
                status: ExternalServiceStatus.AVAILABLE,
        )
        def registeredBuildingExternalService2 = buildingExternalServiceResource.registerService(null, buildingExternalService2)

        then: "the building service user should receive the new realm-bound external service"
        assert registeredBuildingExternalService2.instanceId != null

        when: "the building service user retrieves the global external services"
        def globalServicesForBuildingRealm2 = buildingExternalServiceResource.getGlobalServices()

        then: "the returned global external services should only include the global forecasting service"
        assert globalServicesForBuildingRealm2 != null
        assert globalServicesForBuildingRealm2.size() == 1
        assert globalServicesForBuildingRealm2[0].serviceId == registeredGlobalExternalService.serviceId

        when: "the building service user retrieves external services from its realm"
        def buildingServicesForBuildingRealm2 = buildingExternalServiceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())

        then: "the returned external services should only include the new realm-bound external service"
        assert buildingServicesForBuildingRealm2 != null
        assert buildingServicesForBuildingRealm2.size() == 1
        assert buildingServicesForBuildingRealm2[0].serviceId == buildingExternalService2.serviceId

        when: "the building service user tries to send a heartbeat for the global external service"
        buildingExternalServiceResource.heartbeat(null, registeredGlobalExternalService.serviceId, registeredGlobalExternalService.instanceId)

        then: "the building service user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        when: "the building service user tries to deregister the global external service"
        buildingExternalServiceResource.deregisterService(null, registeredGlobalExternalService.serviceId, registeredGlobalExternalService.instanceId)

        then: "the building service user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()


        // == Tests related to a regular user trying to interact with external services ===

        when: "the regular user tries to retrieve the global external services"
        
        def regularUserGlobalServices = regularUserExternalServiceResource.getGlobalServices()

        then: "the regular user should receive a list of all global external services, since they are available to all realms"
        assert regularUserGlobalServices != null
        assert regularUserGlobalServices.size() == 1
        assert regularUserGlobalServices[0].serviceId == registeredGlobalExternalService.serviceId

        when: "the regular user tries to retrieve the external services from its realm"
        def regularUserRealmServices = regularUserExternalServiceResource.getServices(null, MASTER_REALM)

        then: "the regular user should receive a list of all external services from its realm"
        assert regularUserRealmServices != null
        assert regularUserRealmServices.size() == 1

        when: "the regular user tries to retrieve the external services from a realm other than its own"
        regularUserExternalServiceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())

        then: "the regular user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        when: "the regular user tries to send a heartbeat for an external service"
        regularUserExternalServiceResource.heartbeat(null, registeredBuildingExternalService2.serviceId, registeredBuildingExternalService2.instanceId)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        when: "the regular user tries to deregister an external service"
        regularUserExternalServiceResource.deregisterService(null, registeredBuildingExternalService2.serviceId, registeredBuildingExternalService2.instanceId)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        when: "the regular user tries to register an external service"
        def regularUserNewService = new ExternalService(
                serviceId: "regular-user-new-service",
                label: "Regular User New Service",
                homepageUrl: "https://local.test/services/regular-user-new-service/ui",
                status: ExternalServiceStatus.AVAILABLE,
        )
        regularUserExternalServiceResource.registerService(null, regularUserNewService)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        when: "the regular user tries to register a global external service"
        regularUserExternalServiceResource.registerGlobalService(null, regularUserNewService)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()


        // == Additional miscellaneous tests ==

        when: "time advances by 70 seconds, longer than the default registration lease duration"
        advancePseudoClock(70, TimeUnit.SECONDS)

        then: "the global external service should now be marked as unavailable"
        externalServiceRegistryService.markExpiredInstancesAsUnavailable()
        def globalServiceAfterInstanceExpiryMisc = superServiceUserExternalServiceResource.getService(null, registeredGlobalExternalService.serviceId, registeredGlobalExternalService.instanceId)
        assert globalServiceAfterInstanceExpiryMisc.status == ExternalServiceStatus.UNAVAILABLE

        and: "the building external service should now be marked as unavailable"
        def buildingServiceAfterInstanceExpiryMisc = buildingExternalServiceResource.getService(null, registeredBuildingExternalService2.serviceId, registeredBuildingExternalService2.instanceId)
        assert buildingServiceAfterInstanceExpiryMisc.status == ExternalServiceStatus.UNAVAILABLE

        when: "time advances by 24 hours, longer than the default automatic deregistration threshold"
        advancePseudoClock(24, TimeUnit.HOURS)
        externalServiceRegistryService.deregisterLongExpiredInstances()

        then: "the building external service should now be deregistered, thus the list of external services for the building realm should be empty"
        def buildingRealmServices = buildingExternalServiceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())
        assert buildingRealmServices.size() == 0

        and: "the global external service should now be deregistered, thus the list of global external services should be empty"
        def globalRealmServices = buildingExternalServiceResource.getGlobalServices()
        assert globalRealmServices.size() == 0

    }
}
