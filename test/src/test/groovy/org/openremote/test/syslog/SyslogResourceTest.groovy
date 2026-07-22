package org.openremote.test.syslog

import jakarta.ws.rs.core.Response
import org.openremote.container.persistence.PersistenceService
import org.openremote.container.security.AuthContext
import org.openremote.container.security.IdentityProvider
import org.openremote.container.security.IdentityService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.security.ManagerIdentityProvider
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.syslog.SyslogService
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.event.shared.RealmFilter
import org.openremote.model.security.ClientRole
import org.openremote.model.syslog.SyslogCategory
import org.openremote.model.syslog.SyslogEvent
import org.openremote.model.syslog.SyslogLevel
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.util.MapAccess.getString
import static org.openremote.model.Constants.*

class SyslogResourceTest extends Specification implements ManagerContainerTrait {

    @Shared
    private static String userId

    def "Users with rules read role cannot retrieve syslog events"() {
        given: "the server container is started with the syslog resource"
        def container = startContainer(defaultConfig(), defaultServices(new SyslogService()))
        def identityProvider = container.getService(ManagerIdentityService.class).getIdentityProvider()
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

        cleanup: "the test user is removed"
        deleteUser(identityProvider)
    }

    def "Users with logs read role can retrieve syslog events"() {
        given: "the server container is started with the syslog resource"
        def container = startContainer(defaultConfig(), defaultServices(new SyslogService()))
        def identityProvider = container.getService(ManagerIdentityService.class).getIdentityProvider()
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

        cleanup: "the test user is removed"
        deleteUser(identityProvider)
    }

    def "Realm users only see events of their own realm"() {
        given: "the server container is started with the syslog resource"
        def container = startContainer(defaultConfig(), defaultServices(new SyslogService()))
        def identityProvider = container.getService(ManagerIdentityService.class).getIdentityProvider()
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def persistenceService = container.getService(PersistenceService.class)
        def buildingRealm = keycloakTestSetup.realmBuilding.name
        def cityRealm = keycloakTestSetup.realmCity.name

        and: "syslog events exist for multiple realms and as system logs"
        def subCategory = "SyslogResourceRealmTest-" + UUID.randomUUID()
        def now = getInstantTimeOf(container)
        persistenceService.doTransaction { em ->
            em.persist(new SyslogEvent(now.minusSeconds(30).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "building event", buildingRealm))
            em.persist(new SyslogEvent(now.minusSeconds(20).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "smartcity event", cityRealm))
            em.persist(new SyslogEvent(now.minusSeconds(10).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "system event"))
        }

        and: "a building realm user with the logs role"
        def username = "syslog-test-building"
        def user = keycloakTestSetup.createUser(
            buildingRealm,
            username,
            username,
            "Syslog",
            "Building",
            "${username}@openremote.local",
            true,
            [ClientRole.READ_LOGS] as ClientRole[]
        )
        userId = user.id
        def buildingAccessToken = authenticate(container, buildingRealm, KEYCLOAK_CLIENT_ID, username, username)

        when: "the building user requests syslog events"
        def response = getSyslogEvents(buildingAccessToken, buildingRealm, null, subCategory)

        then: "only events of the building realm are returned (no system logs, no other realms)"
        response.withCloseable { r ->
            assert r.status == Response.Status.OK.statusCode
            def events = r.readEntity(SyslogEvent[].class)
            assert events.length == 1
            assert events[0].message == "building event"
            assert events[0].realm == buildingRealm
            return true
        }

        when: "the building user requests another realm's syslog events"
        def forbiddenResponse = getSyslogEvents(buildingAccessToken, buildingRealm, cityRealm, subCategory)

        then: "the request is rejected"
        forbiddenResponse.withCloseable { r ->
            assert r.status == Response.Status.FORBIDDEN.statusCode
            return true
        }

        cleanup: "the test user is removed"
        identityProvider.deleteUser(buildingRealm, userId)
        userId = null
    }

    def "Superusers see events of all realms and can filter by realm"() {
        given: "the server container is started with the syslog resource"
        def container = startContainer(defaultConfig(), defaultServices(new SyslogService()))
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def persistenceService = container.getService(PersistenceService.class)
        def buildingRealm = keycloakTestSetup.realmBuilding.name
        def cityRealm = keycloakTestSetup.realmCity.name

        and: "syslog events exist for multiple realms and as system logs"
        def subCategory = "SyslogResourceSuperTest-" + UUID.randomUUID()
        def now = getInstantTimeOf(container)
        persistenceService.doTransaction { em ->
            em.persist(new SyslogEvent(now.minusSeconds(30).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "building event", buildingRealm))
            em.persist(new SyslogEvent(now.minusSeconds(20).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "smartcity event", cityRealm))
            em.persist(new SyslogEvent(now.minusSeconds(10).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "system event"))
        }

        and: "the superuser is authenticated"
        def adminAccessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        when: "the superuser requests syslog events without a realm filter"
        def response = getSyslogEvents(adminAccessToken, MASTER_REALM, null, subCategory)

        then: "events of all realms including system logs are returned"
        response.withCloseable { r ->
            assert r.status == Response.Status.OK.statusCode
            def events = r.readEntity(SyslogEvent[].class)
            assert events.length == 3
            assert events.collect { it.message }.toSet() == ["building event", "smartcity event", "system event"].toSet()
            return true
        }

        when: "the superuser filters by realm"
        def filteredResponse = getSyslogEvents(adminAccessToken, MASTER_REALM, buildingRealm, subCategory)

        then: "only that realm's events are returned"
        filteredResponse.withCloseable { r ->
            assert r.status == Response.Status.OK.statusCode
            def events = r.readEntity(SyslogEvent[].class)
            assert events.length == 1
            assert events[0].realm == buildingRealm
            return true
        }
    }

    def "Syslog event subscriptions are realm scoped for non superusers"() {
        given: "the server container is started with the syslog resource"
        def container = startContainer(defaultConfig(), defaultServices(new SyslogService()))
        def clientEventService = container.getService(ClientEventService.class)

        and: "a non superuser auth context of the building realm with the logs role"
        def buildingAuth = [
            isSuperUser: { -> false },
            hasResourceRole: { String role, String client -> role == READ_LOGS_ROLE },
            getAuthenticatedRealmName: { -> "building" }
        ] as AuthContext

        and: "a superuser auth context"
        def superAuth = [
            isSuperUser: { -> true },
            hasResourceRole: { String role, String client -> true },
            getAuthenticatedRealmName: { -> MASTER_REALM }
        ] as AuthContext

        when: "a non superuser subscribes without a filter"
        def subscription = new EventSubscription(SyslogEvent.class)
        def authorized = clientEventService.authorizeEventSubscription("building", buildingAuth, subscription)

        then: "the subscription is authorized and a realm filter is forced that drops other realms and system logs"
        authorized
        subscription.filter instanceof RealmFilter
        subscription.filter.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", "building")) != null
        subscription.filter.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", "smartcity")) == null
        subscription.filter.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", null)) == null

        when: "a non superuser subscribes with a level/category filter"
        def filteredSubscription = new EventSubscription(SyslogEvent.class, new SyslogEvent.LevelCategoryFilter(SyslogLevel.INFO))
        def filteredAuthorized = clientEventService.authorizeEventSubscription("building", buildingAuth, filteredSubscription)

        then: "the client filter is preserved but forced to the user's realm"
        filteredAuthorized
        filteredSubscription.filter instanceof SyslogEvent.LevelCategoryFilter
        ((SyslogEvent.LevelCategoryFilter) filteredSubscription.filter).realm == "building"
        filteredSubscription.filter.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", "smartcity")) == null

        when: "a superuser subscribes without a filter"
        def superSubscription = new EventSubscription(SyslogEvent.class)
        def superAuthorized = clientEventService.authorizeEventSubscription(MASTER_REALM, superAuth, superSubscription)

        then: "the subscription is authorized and no filter is forced"
        superAuthorized
        superSubscription.filter == null

        when: "a user without the logs role subscribes"
        def noRoleAuth = [
            isSuperUser: { -> false },
            hasResourceRole: { String role, String client -> false },
            getAuthenticatedRealmName: { -> "building" },
            getUsername: { -> "syslog-test" }
        ] as AuthContext
        def deniedSubscription = new EventSubscription(SyslogEvent.class)

        then: "the subscription is not authorized"
        !clientEventService.authorizeEventSubscription("building", noRoleAuth, deniedSubscription)
    }

    private String createUserToken(container, KeycloakTestSetup keycloakTestSetup, String roleName, ClientRole role) {
        def username = "syslog-test-${roleName}"
        def user = keycloakTestSetup.createUser(
            MASTER_REALM,
            username,
            username,
            "Syslog",
            roleName,
            "${username}@openremote.local",
            true,
            [role] as ClientRole[]
        )

        userId = user.id
        authenticate(container, MASTER_REALM, KEYCLOAK_CLIENT_ID, username, username)
    }

   private static void deleteUser(ManagerIdentityProvider identityProvider) {
      if (userId != null) {
          identityProvider.deleteUser(MASTER_REALM, userId)
      }
   }

    private def getSyslogEvents(String accessToken, String requestRealm = MASTER_REALM, String realm = null, String subCategory = null) {
        def target = getClientApiTarget(serverUri(serverPort), requestRealm, accessToken)
            .path("syslog")
            .path("event")
        if (realm != null) {
            target = target.queryParam("realm", realm)
        }
        if (subCategory != null) {
            target = target.queryParam("subCategory", subCategory)
        }
        target.request().get()
    }
}
