/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.setup.integration;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.manager.security.ManagerIdentityProvider;
import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Realm;
import org.openremote.model.security.User;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.RESTRICTED_USER_REALM_ROLE;

/**
 * We have the following demo users:
 * <ul>
 * <li><code>admin</code> - The superuser in the "master" realm with all access</li>
 * <li><code>testuser1</code> - (Password: testuser1) A user in the "master" realm with read/write access to assets and rules</li>
 * <li><code>testuser2</code> - (Password: testuser2) A user in the "building" realm with only read access to assets</li>
 * <li><code>testuser3</code> - (Password: testuser3) A user in the "building" realm with read/write access to a restricted set of assets and their rules</li>
 * <li><code>building</code> - (Password: building) A user in the "building" realm with read/write access to the building apartment 1 assets and their rules</li>
 * <li><code>testuser4</code> - (Password: testuser4) A user in the "smartcity" realm with read/write access to a restricted set of assets and their rules</li>
 *
 * </ul>
 */
public class KeycloakTestSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakTestSetup.class.getName());
    public static final String REALM_ROLE_TEST = "test-realm-role";
    public static final String REALM_ROLE_TEST2 = "test-realm-role-2";
    public String testuser1Id;
    public String testuser2Id;
    public String testuser3Id;
    public String smartCityUserId;
    public String buildingUserId;
    public Realm realmMaster;
    public Realm realmBuilding;
    public Realm realmEnergy;
    public Realm realmCity;
    public User serviceUser;
    public User serviceUser2;

    public KeycloakTestSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // Realms
        realmMaster = identityService.getIdentityProvider().getRealm(Constants.MASTER_REALM);
        realmBuilding = createRealm("building", "Building", true);
        realmCity = createRealm("smartcity", "Smart City", true);
        realmEnergy = createRealm("energy", "Energy Test", true);

        // Add a test realm roles
        keycloakProvider.getRealms(realmsResource -> {
                RealmResource realmResource = realmsResource.realm(realmBuilding.getName());
                realmResource.roles().create(new RoleRepresentation(REALM_ROLE_TEST, "Test role", false));
                realmResource.roles().create(new RoleRepresentation(REALM_ROLE_TEST2, "Test role 2", false));
                return null;
            });

        // Don't allow demo users to write assets
        ClientRole[] noWriteAccess = Arrays.stream(REGULAR_USER_ROLES)
            .filter(clientRole -> clientRole != ClientRole.WRITE_ASSETS)
            .toArray(ClientRole[]::new);


        // Users
        User testuser1 = createUser(MASTER_REALM, "testuser1", "testuser1", "DemoMaster", "DemoLast", null, true, REGULAR_USER_ROLES);
        this.testuser1Id = testuser1.getId();
        keycloakProvider.updateUserRoles(MASTER_REALM, testuser1Id, "account"); // Remove all roles for account client
        User testuser2 = createUser(realmBuilding.getName(), "testuser2", "testuser2", "DemoA2", "DemoLast", "testuser2@openremote.local", true, false, true, new ClientRole[] {
            ClientRole.WRITE_USER,
            ClientRole.READ_MAP,
            ClientRole.READ_ASSETS
        });
        this.testuser2Id = testuser2.getId();
        keycloakProvider.updateUserRoles(realmBuilding.getName(), testuser2Id, "account"); // Remove all roles for account client
        User testuser3 = createUser(realmBuilding.getName(), "testuser3", "testuser3", "DemoA3", "DemoLast", "testuser3@openremote.local", true, true, false, REGULAR_USER_ROLES);
        this.testuser3Id = testuser3.getId();
        keycloakProvider.updateUserRoles(realmBuilding.getName(), testuser3Id, "account"); // Remove all roles for account client
        // Add realm role
        keycloakProvider.updateUserRealmRoles(realmBuilding.getName(), testuser3Id, keycloakProvider.addRealmRoles(realmBuilding.getName(), testuser3Id, REALM_ROLE_TEST));
        User buildingUser = createUser(realmBuilding.getName(), "building", "building", "Building", "User", "building@openremote.local", true, noWriteAccess);
        this.buildingUserId = buildingUser.getId();
        keycloakProvider.updateUserRoles(realmBuilding.getName(), buildingUserId, "account"); // Remove all roles for account client
        // Add realm role
        keycloakProvider.updateUserRealmRoles(realmBuilding.getName(), buildingUserId, keycloakProvider.addRealmRoles(realmBuilding.getName(), buildingUserId, REALM_ROLE_TEST2));
        User smartCityUser = createUser(realmCity.getName(), "smartcity", "smartcity", "Smart", "City", null, true, noWriteAccess);
        this.smartCityUserId = smartCityUser.getId();
        keycloakProvider.updateUserRoles(realmCity.getName(), smartCityUserId, "account"); // Remove all roles for account client

        /*
         * Service user clients
         */
        serviceUser = new User()
            .setServiceAccount(true)
            .setEnabled(true)
            .setUsername("serviceuser");
        serviceUser = keycloakProvider.createUpdateUser(realmBuilding.getName(), serviceUser, UniqueIdentifierGenerator.generateId("serviceusertest"), true);
        keycloakProvider.updateUserRoles(
            realmBuilding.getName(),
            serviceUser.getId(),
            Constants.KEYCLOAK_CLIENT_ID,
            Stream.of(ClientRole.READ_ASSETS, ClientRole.WRITE_ASSETS, ClientRole.WRITE_ATTRIBUTES).map(ClientRole::getValue).toArray(String[]::new)
        );
        serviceUser2 = new User()
            .setServiceAccount(true)
            .setEnabled(true)
            .setUsername("serviceuser2");
        serviceUser2 = keycloakProvider.createUpdateUser(realmBuilding.getName(), serviceUser2, UniqueIdentifierGenerator.generateId("serviceuser2test"), true);
        keycloakProvider.updateUserRoles(
            realmBuilding.getName(),
            serviceUser2.getId(),
            Constants.KEYCLOAK_CLIENT_ID,
            Stream.of(ClientRole.READ_ASSETS, ClientRole.WRITE_ASSETS, ClientRole.WRITE_ATTRIBUTES).map(ClientRole::getValue).toArray(String[]::new)
        );

        // ################################ Make users restricted ###################################
        ManagerIdentityProvider identityProvider = identityService.getIdentityProvider();
        identityProvider.updateUserRealmRoles(realmBuilding.getName(), testuser3Id, identityProvider.addRealmRoles(realmBuilding.getName(), testuser3Id, RESTRICTED_USER_REALM_ROLE));
        identityProvider.updateUserRealmRoles(realmBuilding.getName(), buildingUserId, identityProvider.addRealmRoles(realmBuilding.getName(), buildingUserId, RESTRICTED_USER_REALM_ROLE));
        identityProvider.updateUserRealmRoles(realmBuilding.getName(), serviceUser2.getId(), identityProvider.addRealmRoles(realmBuilding.getName(), serviceUser2.getId(), RESTRICTED_USER_REALM_ROLE));
    }
}
