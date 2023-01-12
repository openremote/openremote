package org.openremote.test.alert

import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.alert.AlertService
import org.openremote.manager.asset.console.ConsoleResourceImpl
import org.openremote.manager.setup.SetupService
import org.openremote.manager.web.ManagerWebService
import org.openremote.model.alert.Alert
import org.openremote.model.alert.SentAlert
import org.openremote.model.alert.AlertResource
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.console.ConsoleRegistration
import org.openremote.model.console.ConsoleResource
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.util.ValueUtil.parse

class  AlertTest extends Specification implements ManagerContainerTrait {


    def "Check alert service functionality"() {

        List<Alert> createdAlerts = []

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def alertService = container.getService(AlertService.class)
//        def consoleResource = (ConsoleResourceImpl)container.getService(ManagerWebService.class).apiSingletons.find {it instanceof ConsoleResourceImpl}

        and: "a mock persistence service"
        AlertService mockAlertService = Spy(alertService)
//        mockAlertService.createAlert() >>

//        and: "an authenticated test user"
//        def realm = keycloakTestSetup.realmBuilding.name//        def testuser1AccessToken  = authenticate(
//                container,
//                MASTER_REALM,
//                KEYCLOAK_CLIENT_ID,
//                "testuser1",
//                "testuser1"
//        ).token
//        def testuser2AccessToken = authenticate(
//                container,
//                realm,
//                KEYCLOAK_CLIENT_ID,
//                "testuser2",
//                "testuser2"
//        ).token
//        def testuser3AccessToken = authenticate(
//                container,
//                realm,
//                KEYCLOAK_CLIENT_ID,
//                "testuser3",
//                "testuesr3"
//        ).token

        and: "an authenticated superuser"
        def adminAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        def alert = new Alert("TestAction", "Testing alert service", Alert.Severity.LOW)

        and: "the alert resource"
//        def testuser1AlertResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, testuser1AccessToken).proxy(AlertResource.class)
//        def testuser2AlertResource = getClientApiTarget(serverUri(serverPort), realm, testuser2AccessToken).proxy(AlertResource.class)
//        def testuser3AlertResource = getClientApiTarget(serverUri(serverPort), realm, testuser3AccessToken).proxy(AlertResource.class)
        def adminAlertResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(AlertResource.class)
        def anonymousAlertResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name).proxy(AlertResource.class)
//        def testuser1ConsoleResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, testuser1AccessToken).proxy(ConsoleResource.class)
//        def testuser2ConsoleResource = getClientApiTarget(serverUri(serverPort), realm, testuser2AccessToken).proxy(ConsoleResource.class)
//        def testuser3ConsoleResource = getClientApiTarget(serverUri(serverPort), realm, testuser3AccessToken).proxy(ConsoleResource.class)
//        def adminConsoleResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, adminAccessToken).proxy(ConsoleResource.class)
//        def anonymousConsoleResource = getClientApiTarget(serverUri(serverPort), realm).proxy(ConsoleResource.class)
//        SentAlert[] alerts = []

        when: "the admin user creates an alert for an entire realm"
        adminAlertResource.createAlert(null, alert)

        then: "an alert should have been added to the database"
        conditions.eventually {
            assert alertIds.size() == 1
        }

        when: "the anonymous user creates an alert for an entire realm"
        anonymousAlertResource.createAlert(null, alert)

        then: "an alert should have been added to the database"

    }

    // -----------------------------------------------
    //    Check alert resource api
    // -----------------------------------------------
    def "Check alert resource functionality"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def alertService = container.getService(AlertService.class)

    }

    // -----------------------------------------------
    //    Check rules engine integration
    // -----------------------------------------------
    def "Check alert integration with rules engine"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def alertService = container.getService(AlertService.class)

    }
}
