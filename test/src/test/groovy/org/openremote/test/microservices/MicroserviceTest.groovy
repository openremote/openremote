package org.openremote.test.microservices

import jakarta.ws.rs.WebApplicationException
import org.openremote.manager.microservices.MicroserviceRegistryService
import org.openremote.manager.setup.SetupService
import org.openremote.model.microservices.Microservice
import org.openremote.model.microservices.MicroserviceResource
import org.openremote.model.microservices.MicroserviceStatus
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import jakarta.ws.rs.core.Response

import java.util.concurrent.TimeUnit

import static org.openremote.model.Constants.*

class MicroserviceTest extends Specification implements ManagerContainerTrait {

    def "Test microservice resource and local manager microservice registry logic"() {

        given: "the server container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def microserviceRegistryService = container.getService(MicroserviceRegistryService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)


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

        when: "the microservice resource is set up"
        def serverUri = serverUri(serverPort)
        def buildingMicroserviceResource = getClientApiTarget(serverUri, keycloakTestSetup.realmBuilding.getName(), buildingServiceUserAccessToken).proxy(MicroserviceResource)
        def regularUserMicroserviceResource = getClientApiTarget(serverUri, MASTER_REALM, regularUserAccessToken).proxy(MicroserviceResource)
        def superServiceUserMicroserviceResource = getClientApiTarget(serverUri, MASTER_REALM, superServiceUserAccessToken).proxy(MicroserviceResource)

        then: "the microservice resources should be accessible"
        assert buildingMicroserviceResource != null
        assert regularUserMicroserviceResource != null
        assert superServiceUserMicroserviceResource != null


        // === Tests related to interacting with the services resource as a service user for a specific realm ===

        when: "the building service user registers a microservice"
        def buildingMicroservice = new Microservice(
                serviceId: "building-energy-management",
                label: "Building Energy Management Service",
                realm: keycloakTestSetup.realmBuilding.getName(),
                homepageUrl: "https://local.test/services/building-energy-management/ui",
                status: MicroserviceStatus.AVAILABLE,
        )
        def registeredBuildingMicroservice = buildingMicroserviceResource.registerService(null, buildingMicroservice)

        then: "the building microservice should be registered and the registered microservice should have an instanceId assigned"
        assert registeredBuildingMicroservice.instanceId != null

        and: "the building microservice should be included in the list of services for the building realm"
        def buildingServices = buildingMicroserviceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())
        assert buildingServices != null
        assert buildingServices.size() == 1
        assert buildingServices[0].serviceId == buildingMicroservice.serviceId
        assert buildingServices[0].label == buildingMicroservice.label
        assert buildingServices[0].realm == buildingMicroservice.realm
        assert buildingServices[0].homepageUrl == buildingMicroservice.homepageUrl
        assert buildingServices[0].status == buildingMicroservice.status


        when: "the building service user tries to register the exact same microservice"
        buildingMicroserviceResource.registerService(null, registeredBuildingMicroservice)

        then: "the building service user should receive a 409 conflict response"
        def ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.CONFLICT.getStatusCode()

        when: "time advanced by 70 seconds, longer than the default registration lease duration"
        advancePseudoClock(70, TimeUnit.SECONDS)

        then: "the building microservice should now be marked as unavailable"
        microserviceRegistryService.markExpiredInstancesAsUnavailable()
        def buildingServiceAfterInstanceExpiry = buildingMicroserviceResource.getService(null, registeredBuildingMicroservice.serviceId, registeredBuildingMicroservice.instanceId)
        assert buildingServiceAfterInstanceExpiry.status == MicroserviceStatus.UNAVAILABLE

        when: "the building service user sends a heartbeat"
        buildingMicroserviceResource.heartbeat(null, registeredBuildingMicroservice.serviceId, registeredBuildingMicroservice.instanceId)

        then: "the building microservice should now be marked as available"
        def buildingServiceAfterHeartbeat = buildingMicroserviceResource.getService(null, registeredBuildingMicroservice.serviceId, registeredBuildingMicroservice.instanceId)
        assert buildingServiceAfterHeartbeat.status == MicroserviceStatus.AVAILABLE

        when: "the building service user de-registers the microservice"
        buildingMicroserviceResource.deregisterService(null, registeredBuildingMicroservice.serviceId, registeredBuildingMicroservice.instanceId)

        then: "the building microservice should no longer be included in the list of services for the building realm"
        def buildingServicesAfterDeregister = buildingMicroserviceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())
        assert buildingServicesAfterDeregister != null
        assert buildingServicesAfterDeregister.size() == 0

        when: "the building service user tries to retrieve the now deregistered microservice"
        buildingMicroserviceResource.getService(null, registeredBuildingMicroservice.serviceId, registeredBuildingMicroservice.instanceId)

        then: "the building service user should receive a 404 not found response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.NOT_FOUND.getStatusCode()

        when: "the building service user tries to register the microservice with an inaccessible realm"
        def buildingMicroserviceWithInaccessibleRealm = new Microservice(
                serviceId: "building-energy-management",
                label: "Building Energy Management Service",
                realm: MASTER_REALM,
                homepageUrl: "https://local.test/services/building-energy-management/ui",
                status: MicroserviceStatus.AVAILABLE,
        )
        buildingMicroserviceResource.registerService(null, buildingMicroserviceWithInaccessibleRealm)

        then: "the building service user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        when: "the building service user tries to retrieve the microservices from an inaccessible realm"
        buildingMicroserviceResource.getServices(null, MASTER_REALM)

        then: "the building service user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        // === Tests related to global service registration and management ===

        when: "the super user registers a global microservice (master realm + user is super user)"
        def globalMicroservice = new Microservice(
                serviceId: "forecasting-service",
                label: "Forecasting Service",
                realm: MASTER_REALM,
                homepageUrl: "https://local.test/services/forecasting-service/ui",
                status: MicroserviceStatus.AVAILABLE,
        )
        def registeredGlobalMicroservice = superServiceUserMicroserviceResource.registerGlobalService(null, globalMicroservice)

        then: "the global microservice should be registered and the registered microservice should have an instanceId assigned"
        assert registeredGlobalMicroservice.instanceId != null

        and: "the global microservice should be included in the list of global services"
        def globalServices = superServiceUserMicroserviceResource.getGlobalServices()
        assert globalServices != null
        assert globalServices.size() == 1
        assert globalServices[0].serviceId == globalMicroservice.serviceId

        when: "time advanced by 70 seconds, longer than the default registration lease duration"
        advancePseudoClock(70, TimeUnit.SECONDS)

        then: "the global microservice should now be marked as unavailable"
        microserviceRegistryService.markExpiredInstancesAsUnavailable()
        def globalServiceAfterInstanceExpiry = superServiceUserMicroserviceResource.getService(null, registeredGlobalMicroservice.serviceId, registeredGlobalMicroservice.instanceId)
        assert globalServiceAfterInstanceExpiry.status == MicroserviceStatus.UNAVAILABLE

        when: "the super user sends a heartbeat for the global microservice"
        superServiceUserMicroserviceResource.heartbeat(null, registeredGlobalMicroservice.serviceId, registeredGlobalMicroservice.instanceId)

        then: "the global microservice should now be marked as available"
        def globalServiceAfterHeartbeat = superServiceUserMicroserviceResource.getService(null, registeredGlobalMicroservice.serviceId, registeredGlobalMicroservice.instanceId)
        assert globalServiceAfterHeartbeat.status == MicroserviceStatus.AVAILABLE


        when: "the super user tries to register a global microservice with a non-master realm"
        def globalMicroservice2 = new Microservice(
                serviceId: "forecasting-service-2",
                label: "Forecasting Service 2",
                realm: keycloakTestSetup.realmBuilding.getName(),
                homepageUrl: "https://local.test/services/forecasting-service-2/ui",
                status: MicroserviceStatus.AVAILABLE,
        )
        superServiceUserMicroserviceResource.registerGlobalService(null, globalMicroservice2)

        then: "the super user should receive a 400 bad request response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.BAD_REQUEST.getStatusCode()


        // === Tests related to interacting with global services as a service user from a realm different from the master realm ===
        
        when: "the building service user tries to retrieve the global microservices"
        def globalServicesForBuildingRealm = buildingMicroserviceResource.getGlobalServices()

        then: "the building service user should receive the global services which includes the newly registered global forecasting service"
        assert globalServicesForBuildingRealm != null
        assert globalServicesForBuildingRealm.size() == 1
        assert globalServicesForBuildingRealm[0].serviceId == registeredGlobalMicroservice.serviceId

        when: "the building service user registers a new realm-bound microservice"
        def buildingMicroservice2 = new Microservice(
                serviceId: "building-energy-management-2",
                label: "Building Energy Management Service 2",
                realm: keycloakTestSetup.realmBuilding.getName(),
                homepageUrl: "https://local.test/services/building-energy-management-2/ui",
                status: MicroserviceStatus.AVAILABLE,
        )
        def registeredBuildingMicroservice2 = buildingMicroserviceResource.registerService(null, buildingMicroservice2)

        then: "the building service user should receive the new realm-bound microservice"
        assert registeredBuildingMicroservice2.instanceId != null

        when: "the building service user retrieves the global microservices"
        def globalServicesForBuildingRealm2 = buildingMicroserviceResource.getGlobalServices()

        then: "the returned global services should only include the global forecasting service"
        assert globalServicesForBuildingRealm2 != null
        assert globalServicesForBuildingRealm2.size() == 1
        assert globalServicesForBuildingRealm2[0].serviceId == registeredGlobalMicroservice.serviceId

        when: "the building service user retrieves services from its realm"
        def buildingServicesForBuildingRealm2 = buildingMicroserviceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())

        then: "the returned services should only include the new realm-bound microservice"
        assert buildingServicesForBuildingRealm2 != null
        assert buildingServicesForBuildingRealm2.size() == 1
        assert buildingServicesForBuildingRealm2[0].serviceId == buildingMicroservice2.serviceId

        when: "the building service user tries to send a heartbeat for the global microservice"
        buildingMicroserviceResource.heartbeat(null, registeredGlobalMicroservice.serviceId, registeredGlobalMicroservice.instanceId)

        then: "the building service user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        when: "the building service user tries to deregister the global microservice"
        buildingMicroserviceResource.deregisterService(null, registeredGlobalMicroservice.serviceId, registeredGlobalMicroservice.instanceId)

        then: "the building service user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()


        // == Tests related to a regular user trying to interact with services ===

        when: "the regular user tries to retrieve the global services"
        def regularUserGlobalServices = regularUserMicroserviceResource.getGlobalServices()

        then: "the regular user should receive a list of all global services, since they are available to all realms"
        assert regularUserGlobalServices != null
        assert regularUserGlobalServices.size() == 1
        assert regularUserGlobalServices[0].serviceId == registeredGlobalMicroservice.serviceId

        when: "the regular user tries to retrieve the services from its realm"
        def regularUserRealmServices = regularUserMicroserviceResource.getServices(null, MASTER_REALM)

        then: "the regular user should receive a list of all services from its realm"
        assert regularUserRealmServices != null
        assert regularUserRealmServices.size() == 1

        when: "the regular user tries to retrieve the services from a realm other than its own"
        regularUserMicroserviceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())

        then: "the regular user should receive a 403 forbidden response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.FORBIDDEN.getStatusCode()

        when: "the regular user tries to send a heartbeat for a service"
        regularUserMicroserviceResource.heartbeat(null, registeredBuildingMicroservice2.serviceId, registeredBuildingMicroservice2.instanceId)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        when: "the regular user tries to deregister a service"
        regularUserMicroserviceResource.deregisterService(null, registeredBuildingMicroservice2.serviceId, registeredBuildingMicroservice2.instanceId)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        when: "the regular user tries to register a service"
        def regularUserNewService = new Microservice(
                serviceId: "regular-user-new-service",
                label: "Regular User New Service",
                realm: keycloakTestSetup.realmBuilding.getName(),
                homepageUrl: "https://local.test/services/regular-user-new-service/ui",
                status: MicroserviceStatus.AVAILABLE,
        )
        regularUserMicroserviceResource.registerService(null, regularUserNewService)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()

        when: "the regular user tries to register a global service"
        regularUserMicroserviceResource.registerGlobalService(null, regularUserNewService)

        then: "the regular user should receive a 401 unauthorized response"
        ex = thrown(WebApplicationException)
        assert ex.response.status == Response.Status.UNAUTHORIZED.getStatusCode()


        // == Additional miscellaneous tests ==

        when: "time advances by 70 seconds, longer than the default registration lease duration"
        advancePseudoClock(70, TimeUnit.SECONDS)

        then: "the global microservice should now be marked as unavailable"
        microserviceRegistryService.markExpiredInstancesAsUnavailable()
        def globalServiceAfterInstanceExpiryMisc = superServiceUserMicroserviceResource.getService(null, registeredGlobalMicroservice.serviceId, registeredGlobalMicroservice.instanceId)
        assert globalServiceAfterInstanceExpiryMisc.status == MicroserviceStatus.UNAVAILABLE

        and: "the building microservice should now be marked as unavailable"
        def buildingServiceAfterInstanceExpiryMisc = buildingMicroserviceResource.getService(null, registeredBuildingMicroservice2.serviceId, registeredBuildingMicroservice2.instanceId)
        assert buildingServiceAfterInstanceExpiryMisc.status == MicroserviceStatus.UNAVAILABLE

        when: "time advances by 24 hours, longer than the default automatic deregistration threshold"
        advancePseudoClock(24, TimeUnit.HOURS)
        microserviceRegistryService.deregisterLongExpiredInstances()

        then: "the building microservice should now be deregistered, thus the list of services for the building realm should be empty"
        def buildingRealmServices = buildingMicroserviceResource.getServices(null, keycloakTestSetup.realmBuilding.getName())
        assert buildingRealmServices.size() == 0

        and: "the global microservice should now be deregistered, thus the list of global services should be empty"
        def globalRealmServices = buildingMicroserviceResource.getGlobalServices()
        assert globalRealmServices.size() == 0

    }
}
