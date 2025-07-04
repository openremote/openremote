package org.openremote.test.microservices

import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions


import static org.openremote.model.Constants.*

class MicroserviceTest extends Specification implements ManagerContainerTrait {

    def "Test microservice registration and retrieval"() {

        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the service user is authenticated"
        def username2 = keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser2.username
        def password2 = keycloakTestSetup.serviceUser2.secret

        def serviceUserAccessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                username2,
                password2
        ).token

        then: "the service user has been authenticated and the token is retrieved"
        conditions.eventually {
            assert serviceUserAccessToken != null
        }

        // Setup the microservice resource
        def serverUri = serverUri(serverPort)
        def microserviceResource = getClientApiTarget(serverUri, MASTER_REALM, serviceUserAccessToken).proxy(MicroserviceResource)



        // Register a service

        // Retrieve a service

        // Update the service registration

        // Check TTL mechanism, after 60 seconds service should go unavailable when not updated in that time window

        // Register another service

        // Retrieve all services should be 2, with one unavailable and one available

        // Update both services and check that they are available

        // Don't update both services and check that they eventually go unavailable

    }
}
