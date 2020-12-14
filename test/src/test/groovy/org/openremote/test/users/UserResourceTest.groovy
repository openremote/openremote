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

import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakTestSetup
import org.openremote.model.security.ClientRole
import org.openremote.model.security.Role
import org.openremote.model.security.UserResource
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotAllowedException
import java.lang.reflect.Array

import static org.openremote.container.security.IdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER

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

    def "Get and update roles"() {

        when: "a request is made for the roles in the smart city realm by the admin user"
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
                roles.find {it.name == ClientRole.READ_APPS.value}.id
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
        testRole.compositeRoleIds.contains(roles.find {it.name == ClientRole.READ_APPS.value}.id)

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
