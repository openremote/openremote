package org.openremote.test.dashboard

import org.openremote.manager.setup.SetupService
import org.openremote.model.dashboard.Dashboard
import org.openremote.model.dashboard.DashboardResource
import org.openremote.test.ManagerContainerTrait
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import spock.lang.Ignore
import spock.lang.Specification

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.*

@Ignore
class DashboardTest extends Specification implements ManagerContainerTrait {

    def "Test invalid input Dashboard"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the dashboard resource"
        def serverUri = serverUri(serverPort)
        def dashboardResource = getClientApiTarget(serverUri, MASTER_REALM, accessToken).proxy(DashboardResource.class)

        when: "a dashboard is created in the authenticated realm"
        Dashboard testDashboard = new Dashboard();
        testDashboard = dashboardResource.create(null, testDashboard);

        then: "the dashboard should exist"
        testDashboard.displayName == "Test Room"
        testDashboard.template != null
    }
}
