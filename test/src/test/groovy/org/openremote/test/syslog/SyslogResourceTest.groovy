package org.openremote.test.syslog

import jakarta.ws.rs.core.Response
import org.openremote.manager.setup.SetupService
import org.openremote.manager.syslog.SyslogService
import org.openremote.model.security.ClientRole
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.model.Constants.*

class SyslogResourceTest extends Specification implements ManagerContainerTrait {

    def "Users with rules read role cannot retrieve syslog events"() {
        given: "the server container is started with the syslog resource"
        def container = startContainer(defaultConfig(), defaultServices(new SyslogService()))
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "a user has the old rules role but not the logs role"
        def rulesAccessToken = createUserToken(container, keycloakTestSetup, "read-rules", ClientRole.READ_RULES)

        when: "the user requests syslog events"
        def response = getSyslogEvents(rulesAccessToken)

        then: "the resource rejects the wrong role"
        response.withCloseable { r ->
            assert r.status == Response.Status.FORBIDDEN.statusCode
            return true
        }
    }

    def "Users with logs read role can retrieve syslog events"() {
        given: "the server container is started with the syslog resource"
        def container = startContainer(defaultConfig(), defaultServices(new SyslogService()))
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "a user has the logs role"
        def logsAccessToken = createUserToken(container, keycloakTestSetup, "read-logs", ClientRole.READ_LOGS)

        when: "the user requests syslog events"
        def response = getSyslogEvents(logsAccessToken)

        then: "the resource allows the correct role"
        response.withCloseable { r ->
            assert r.status == Response.Status.OK.statusCode
            return true
        }
    }

    private String createUserToken(container, KeycloakTestSetup keycloakTestSetup, String roleName, ClientRole role) {
        def username = "syslog-${roleName}-${UUID.randomUUID()}"
        keycloakTestSetup.createUser(
            MASTER_REALM,
            username,
            username,
            "Syslog",
            roleName,
            "${username}@openremote.local",
            true,
            [role] as ClientRole[]
        )
        authenticate(container, MASTER_REALM, KEYCLOAK_CLIENT_ID, username, username)
    }

    private def getSyslogEvents(String accessToken) {
        getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken)
            .path("syslog")
            .path("event")
            .request()
            .get()
    }
}
