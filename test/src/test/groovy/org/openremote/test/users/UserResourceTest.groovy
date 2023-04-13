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
import org.openremote.model.Constants
import org.openremote.model.query.UserQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.security.User
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.model.security.ClientRole
import org.openremote.model.security.Role
import org.openremote.model.security.UserResource
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import jakarta.ws.rs.ForbiddenException

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.security.User.EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE

class UserResourceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static UserResource adminUserResource
    @Shared
    static UserResource regularUserMasterResource
    @Shared
    static UserResource regularUserBuildingResource

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
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        def regularMasterAccessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            "testuser1",
            "testuser1"
        ).token

        def regularBuildingAccessToken = authenticate(
            container,
            keycloakTestSetup.realmBuilding.name,
            KEYCLOAK_CLIENT_ID,
            "testuser3",
            "testuser3"
        ).token

        adminUserResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(UserResource.class)
        regularUserMasterResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, regularMasterAccessToken).proxy(UserResource.class)
        regularUserBuildingResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name, regularBuildingAccessToken).proxy(UserResource.class)
    }

    def "User queries"() {

        when: "a request is made for all users by a super user"
        def users = adminUserResource.query(null, new UserQuery().realm(new RealmPredicate(keycloakTestSetup.realmMaster.name)))

        then: "all users should be returned including system users"
        users.size() == 3
        users.find {it.isSystemAccount() && it.username == Constants.MANAGER_CLIENT_ID} != null
        users.find {it.username == MASTER_REALM_ADMIN_USER} != null
        users.find {it.id == keycloakTestSetup.testuser1Id} != null

        when: "a request is made for all users in the same realm by a regular user"
        users = regularUserMasterResource.query(null, new UserQuery().realm(new RealmPredicate(keycloakTestSetup.realmMaster.name)))

        then: "only non system users of the users realm should be returned"
        users.size() == 2
        users.find {it.username == MASTER_REALM_ADMIN_USER} != null
        users.find {it.id == keycloakTestSetup.testuser1Id} != null

        when: "a request is made for all users in a different realm by a regular user"
        users = regularUserBuildingResource.query(null, new UserQuery().realm(new RealmPredicate(keycloakTestSetup.realmMaster.name)))

        then: "only non system users of the users realm should be returned"
        users.size() == 5
        users.count {it.isSystemAccount() && it.username == Constants.MANAGER_CLIENT_ID} == 0
        users.find {it.id == keycloakTestSetup.testuser2Id} != null
        users.find {it.id == keycloakTestSetup.testuser3Id} != null
        users.find {it.id == keycloakTestSetup.buildingUserId} != null
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser.id} != null
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser2.id} != null

        when: "a request is made for all users ordered by username"
        users = regularUserBuildingResource.query(null, new UserQuery().realm(new RealmPredicate(keycloakTestSetup.realmMaster.name)).orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, false)))

        then: "only non system users of the users realm should be returned in username order"
        users.size() == 5
        users[0].username == "building"
        users[1].username == keycloakTestSetup.serviceUser.username
        users[2].username == keycloakTestSetup.serviceUser2.username
        users[3].username == "testuser2"
        users[4].username == "testuser3"

        when: "a request is made for subset of users ordered by username descending"
        users = regularUserBuildingResource.query(null, new UserQuery().realm(new RealmPredicate(keycloakTestSetup.realmMaster.name)).orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, true)).limit(2))

        then: "the correct users should be returned"
        users.size() == 2
        users[0].username == "testuser3"
        users[1].username == "testuser2"

        when: "a request is made for another subset of users ordered by username descending"
        users = regularUserBuildingResource.query(null, new UserQuery().realm(new RealmPredicate(keycloakTestSetup.realmMaster.name)).orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, true)).limit(2).offset(3))

        then: "the correct users should be returned"
        users.size() == 2
        users[0].username == keycloakTestSetup.serviceUser.username
        users[1].username == "building"

        when: "a request is made for only service users (users with a secret)"
        users = adminUserResource.query(null, new UserQuery().serviceUsers(true).realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name)))

        then: "only service users of the requested realm should be returned"
        users.size() == 2
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser.id} != null
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser2.id} != null
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser.id}.secret != null
        users.find {it.isServiceAccount() && it.id == keycloakTestSetup.serviceUser2.id}.secret != null

        when: "a request is made for only restricted users"
        users = regularUserBuildingResource.query(null,
                new UserQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
                        .realmRoles(new StringPredicate(Constants.RESTRICTED_USER_REALM_ROLE))
                        .orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, false)))

        then: "only restricted users of this realm should be returned"
        users.size() == 3
        users[0].username == "building"
        users[1].username == keycloakTestSetup.serviceUser2.username
        users[2].username == "testuser3"

        when: "a request is made for non restricted users"
        users = regularUserBuildingResource.query(null,
                new UserQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
                        .realmRoles(new StringPredicate(Constants.RESTRICTED_USER_REALM_ROLE).negate(true))
                        .orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, false)))

        then: "only non restricted users of this realm should be returned"
        users.size() == 2
        users[0].username == keycloakTestSetup.serviceUser.username
        users[1].username == "testuser2"

        when: "a request is made based on user attributes"
        users = regularUserBuildingResource.query(null,
                new UserQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
                        .attributes(
                            new UserQuery.AttributeValuePredicate(true, new StringPredicate(User.SYSTEM_ACCOUNT_ATTRIBUTE)),
                            new UserQuery.AttributeValuePredicate(true, new StringPredicate(EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE), new StringPredicate("true"))
                ).orderBy(new UserQuery.OrderBy(UserQuery.OrderBy.Property.USERNAME, false)))

        then: "only matching users of this realm should be returned"
        users.size() == 4
        users[0].username == "building"
        users[1].username == keycloakTestSetup.serviceUser.username
        users[2].username == keycloakTestSetup.serviceUser2.username
        users[3].username == "testuser2"
    }

    def "Get and update roles"() {

        when: "a request is made for the roles in the building realm by the admin user"
        def roles = adminUserResource.getRoles(null, keycloakTestSetup.realmBuilding.name)

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
        regularUserBuildingResource.getRoles(null, keycloakTestSetup.realmBuilding.name)

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
        adminUserResource.updateRoles(null, keycloakTestSetup.realmBuilding.name, updatedRoles as Role[])
        roles = adminUserResource.getRoles(null, keycloakTestSetup.realmBuilding.name)
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
        adminUserResource.updateRoles(null, keycloakTestSetup.realmBuilding.name, roles)
        roles = adminUserResource.getRoles(null, keycloakTestSetup.realmBuilding.name)
        writeRole = roles.find {it.name == ClientRole.WRITE.value}

        then: "the write role should have been updated"
        writeRole != null
        writeRole.compositeRoleIds.length == 1
        writeRole.compositeRoleIds.contains(roles.find {it.name == ClientRole.READ_ASSETS.value}.id)
    }

    def "Get and update users"() {
        when: "a user is retrieved"
        def user = adminUserResource.get(null, keycloakTestSetup.realmBuilding.name, keycloakTestSetup.testuser3Id)

        then: "all data should be available"
        user != null
        user.attributes != null
        user.attributes.size() == 1
        user.attributes.get(0).getName() == EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE
        user.attributes.get(0).getValue() == "true"
        user.id == keycloakTestSetup.testuser3Id
        user.realm == keycloakTestSetup.realmBuilding.name
        user.username == "testuser3"

        when: "a new attribute is added to the user"
        user.setAttribute("test", "testvalue")
        user = adminUserResource.update(null, keycloakTestSetup.realmBuilding.name, user)

        then: "the update should have succeeded"
        user.attributes.size() == 2
        user.attributes.any {it.name == EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE && it.value == "true"}
        user.attributes.any {it.name == "test" && it.value == "testvalue"}
    }
}
