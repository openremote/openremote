/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.users

import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.manager.setup.SetupService
import org.openremote.model.query.UserQuery
import org.openremote.model.query.filter.TenantPredicate
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.model.security.ClientRole
import org.openremote.model.security.Role
import org.openremote.model.security.UserResource
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.ForbiddenException

import static org.openremote.container.security.IdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.Constants.RESTRICTED_USER_REALM_ROLE

class UserResourceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static UserResource adminUserResource
    @Shared
    static UserResource regularUserResource

    @Shared
    static KeycloakTestSetup keycloakTestSetup

    def setupSpec() {
        def container = startContainer(defaultConfig(), defaultServices())
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        def regularAccessToken = authenticate(
            container,
            keycloakTestSetup.tenantBuilding.realm,
            KEYCLOAK_CLIENT_ID,
            "testuser3",
            "testuser3"
        ).token

        adminUserResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(UserResource.class)
        regularUserResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.tenantBuilding.realm, regularAccessToken).proxy(UserResource.class)
    }

    def "User queries"() {

        when: "a request is made for all users by a super user"
        def users = adminUserResource.query(null, new UserQuery().tenant(new TenantPredicate(keycloakTestSetup.masterTenant.realm)))

        then: "all users should be returned including system users"
        users.size() == 3
        users.count {it.isSystemAccount() && it.username == KeycloakIdentityProvider.MANAGER_CLIENT_ID} == 1
        users.count {!it.isServiceAccount()} == 3

        when: "a request is made for all users by a regular user"
        users = regularUserResource.query(null, new UserQuery().tenant(new TenantPredicate(keycloakTestSetup.masterTenant.realm)))

        then: "only non system users of the users realm should be returned"
        users.size() == 4
        users.count {it.isSystemAccount() && it.username == KeycloakIdentityProvider.MANAGER_CLIENT_ID} == 0
        users.find {it.id == keycloakTestSetup.testuser2Id} != null
        users.find {it.id == keycloakTestSetup.testuser3Id} != null
        users.find {it.id == keycloakTestSetup.buildingUserId} != null
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser.id} != null

        when: "a request is made for all users ordered by username"
        users = regularUserResource.query(null, new UserQuery().tenant(new TenantPredicate(keycloakTestSetup.masterTenant.realm)).orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, false)))

        then: "only non system users of the users realm should be returned in username order"
        users.size() == 4
        users[0].username == "building"
        users[1].username == keycloakTestSetup.serviceUser.username
        users[2].username == "testuser2"
        users[3].username == "testuser3"

        when: "a request is made for subset of users ordered by username descending"
        users = regularUserResource.query(null, new UserQuery().tenant(new TenantPredicate(keycloakTestSetup.masterTenant.realm)).orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, true)).limit(2))

        then: "the correct users should be returned"
        users.size() == 2
        users[0].username == "testuser3"
        users[1].username == "testuser2"

        when: "a request is made for another subset of users ordered by username descending"
        users = regularUserResource.query(null, new UserQuery().tenant(new TenantPredicate(keycloakTestSetup.masterTenant.realm)).orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, true)).limit(2).offset(2))

        then: "the correct users should be returned"
        users.size() == 2
        users[0].username == keycloakTestSetup.serviceUser.username
        users[1].username == "building"

        when: "a request is made for only service users"
        users = adminUserResource.query(null, new UserQuery().select(new UserQuery.Select().excludeRegularUsers(true)).tenant(new TenantPredicate(keycloakTestSetup.tenantBuilding.realm)))

        then: "only service users of the requested realm should be returned"
        users.size() == 1
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser.id} != null
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser.id}.secret != null
    }

    def "Get and update roles"() {

        when: "a request is made for the roles in the building realm by the admin user"
        def roles = adminUserResource.getRoles(null, keycloakTestSetup.tenantBuilding.realm)

        then: "the standard client roles should have been returned"
        roles.size() == ClientRole.values().length
        def readComposite = roles.find {it.name == ClientRole.READ.value}
        readComposite != null
        readComposite.description == ClientRole.READ.description
        readComposite.compositeRoleIds.length == ClientRole.READ.composites.length
        assert readComposite.compositeRoleIds.every {roleId ->
            String roleName = roles.find {it.id == roleId}.name
            return ClientRole.READ.composites.any {it.value == roleName}
        }

        def readAssets = roles.find{ it.name == ClientRole.READ_ASSETS.value}
        readAssets != null
        readAssets.description == ClientRole.READ_ASSETS.description
        readAssets.compositeRoleIds == null

        when: "a request is made for the roles in the smart building realm by a regular user"
        regularUserResource.getRoles(null, keycloakTestSetup.tenantBuilding.realm)

        then: "a not allowed exception should be thrown"
        thrown(ForbiddenException.class)

        when: "a new composite role is created by the admin user"
        List<Role> updatedRoles = new ArrayList<>(Arrays.asList(roles))
        updatedRoles.add(new Role(
            null,
            "test",
            true, // Value is ignored on update
            false, // Value is ignored on update
            [
                roles.find {it.name == ClientRole.READ_LOGS.value}.id,
                roles.find {it.name == ClientRole.READ_MAP.value}.id
            ] as String[]
        ).setDescription("This is a test"))
        adminUserResource.updateRoles(null, keycloakTestSetup.tenantBuilding.realm, updatedRoles as Role[])
        roles = adminUserResource.getRoles(null, keycloakTestSetup.tenantBuilding.realm)
        def testRole = roles.find {it.name == "test"}

        then: "the new composite role should have been saved"
        testRole != null
        testRole.description == "This is a test"
        testRole.compositeRoleIds.length == 2
        testRole.compositeRoleIds.contains(roles.find {it.name == ClientRole.READ_LOGS.value}.id)
        testRole.compositeRoleIds.contains(roles.find {it.name == ClientRole.READ_MAP.value}.id)

        when: "an existing composite role is updated by the admin user"
        def writeRole = roles.find {it.name == ClientRole.WRITE.value}
        writeRole.compositeRoleIds = [
            roles.find {it.name == ClientRole.READ_ASSETS.value}.id
        ]
        adminUserResource.updateRoles(null, keycloakTestSetup.tenantBuilding.realm, roles)
        roles = adminUserResource.getRoles(null, keycloakTestSetup.tenantBuilding.realm)
        writeRole = roles.find {it.name == ClientRole.WRITE.value}

        then: "the write role should have been updated"
        writeRole != null
        writeRole.compositeRoleIds.length == 1
        writeRole.compositeRoleIds.contains(roles.find {it.name == ClientRole.READ_ASSETS.value}.id)
    }
}
