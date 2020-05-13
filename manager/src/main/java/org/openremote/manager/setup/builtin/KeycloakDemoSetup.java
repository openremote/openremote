/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.setup.builtin;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.openremote.container.Container;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.manager.mqtt.MqttBrokerService;
import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Tenant;
import org.openremote.model.Constants;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;

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
public class KeycloakDemoSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakDemoSetup.class.getName());

    public String testuser1Id;
    public String testuser2Id;
    public String testuser3Id;
    public String smartCityUserId;
    public String buildingUserId;
    public Tenant masterTenant;
    public Tenant tenantBuilding;
    public Tenant tenantCity;

    public KeycloakDemoSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // Tenants
        masterTenant = identityService.getIdentityProvider().getTenant(Constants.MASTER_REALM);

        tenantBuilding = new Tenant();
        tenantBuilding.setRealm("building");
        tenantBuilding.setDisplayName("Building");
        tenantBuilding.setEnabled(true);
        keycloakProvider.createTenant(new ClientRequestInfo(null, accessToken), tenantBuilding, emailConfig);
        tenantBuilding = keycloakProvider.getTenant(tenantBuilding.getRealm());

        tenantCity = new Tenant();
        tenantCity.setRealm("smartcity");
        tenantCity.setDisplayName("Smart City");
        tenantCity.setEnabled(true);
        keycloakProvider.createTenant(new ClientRequestInfo(null, accessToken), tenantCity, emailConfig);
        tenantCity = keycloakProvider.getTenant(tenantCity.getRealm());

        // Users
        String masterClientObjectId = getClientObjectId(masterClientsResource, KEYCLOAK_CLIENT_ID);
        RolesResource masterRolesResource = masterClientsResource.get(masterClientObjectId).roles();

        UserRepresentation testuser1 = new UserRepresentation();
        testuser1.setUsername("testuser1");
        testuser1.setFirstName("DemoMaster");
        testuser1.setLastName("DemoLast");
        testuser1.setEnabled(true);
        masterUsersResource.create(testuser1);
        testuser1 = masterUsersResource.search("testuser1", null, null, null, null, null).get(0);
        this.testuser1Id = testuser1.getId();
        CredentialRepresentation testuser1Credentials = new CredentialRepresentation();
        testuser1Credentials.setType("password");
        testuser1Credentials.setValue("testuser1");
        testuser1Credentials.setTemporary(false);
        masterUsersResource.get(testuser1.getId()).resetPassword(testuser1Credentials);
        masterUsersResource.get(testuser1.getId()).roles().clientLevel(masterClientObjectId).add(Arrays.asList(
            masterRolesResource.get(ClientRole.WRITE_USER.getValue()).toRepresentation(),
            masterRolesResource.get(ClientRole.READ_MAP.getValue()).toRepresentation(),
            masterRolesResource.get(ClientRole.READ_ASSETS.getValue()).toRepresentation(),
            masterRolesResource.get(ClientRole.READ_RULES.getValue()).toRepresentation(),
            masterRolesResource.get(ClientRole.WRITE_ASSETS.getValue()).toRepresentation(),
            masterRolesResource.get(ClientRole.WRITE_RULES.getValue()).toRepresentation()
        ));
        LOG.info("Added demo user '" + testuser1.getUsername() + "' with password '" + testuser1Credentials.getValue() + "'");

        UsersResource tenantBuildingUsersResource = keycloakProvider.getRealms(accessToken).realm("building").users();
        ClientsResource tenantBuildingClientsResource = keycloakProvider.getRealms(accessToken).realm("building").clients();
        String tenantBuildingClientObjectId = getClientObjectId(tenantBuildingClientsResource, KEYCLOAK_CLIENT_ID);
        RolesResource tenantBuildingRolesResource = tenantBuildingClientsResource.get(tenantBuildingClientObjectId).roles();

        /**
         * MQTT Client
         */
        ClientRepresentation mqttClientRepresentation = new ClientRepresentation();
        String buildingMqttClientId = MqttBrokerService.MQTT_CLIENT_ID_PREFIX + UniqueIdentifierGenerator.generateId(tenantBuilding.getRealm());
        mqttClientRepresentation.setClientId(buildingMqttClientId);
        mqttClientRepresentation.setName("MQTT");
        mqttClientRepresentation.setStandardFlowEnabled(false);
        mqttClientRepresentation.setImplicitFlowEnabled(false);
        mqttClientRepresentation.setDirectAccessGrantsEnabled(false);
        mqttClientRepresentation.setServiceAccountsEnabled(true);
        mqttClientRepresentation.setSecret(UniqueIdentifierGenerator.generateId(tenantBuilding.getRealm()));
        keycloakProvider.createClientApplication(new ClientRequestInfo(null, keycloakProvider.getAdminAccessToken(null)), tenantBuilding.getRealm(), mqttClientRepresentation);

        ClientResource mqttResource = tenantBuildingClientsResource.get(getClientObjectId(tenantBuildingClientsResource, buildingMqttClientId));
        UserRepresentation user = mqttResource.getServiceAccountUser();
        UsersResource realmUsersResource = keycloakProvider.getRealms(accessToken).realm(tenantBuilding.getRealm()).users();
        realmUsersResource.get(user.getId()).roles().clientLevel(tenantBuildingClientObjectId).add(Arrays.asList(
                tenantBuildingRolesResource.get(ClientRole.READ_ASSETS.getValue()).toRepresentation(),
                tenantBuildingRolesResource.get(ClientRole.WRITE_ASSETS.getValue()).toRepresentation()
        ));

        UserRepresentation testuser2 = new UserRepresentation();
        testuser2.setUsername("testuser2");
        testuser2.setFirstName("DemoA2");
        testuser2.setLastName("DemoLast");
        testuser2.setEmail("testuser2@openremote.local");
        testuser2.setEnabled(true);
        tenantBuildingUsersResource.create(testuser2);
        testuser2 = tenantBuildingUsersResource.search("testuser2", null, null, null, null, null).get(0);
        this.testuser2Id = testuser2.getId();
        CredentialRepresentation testuser2Credentials = new CredentialRepresentation();
        testuser2Credentials.setType("password");
        testuser2Credentials.setValue("testuser2");
        testuser2Credentials.setTemporary(false);
        tenantBuildingUsersResource.get(testuser2.getId()).resetPassword(testuser2Credentials);
        tenantBuildingUsersResource.get(testuser2.getId()).roles().clientLevel(tenantBuildingClientObjectId).add(Arrays.asList(
            tenantBuildingRolesResource.get(ClientRole.WRITE_USER.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_MAP.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_ASSETS.getValue()).toRepresentation()
        ));
        LOG.info("Added demo user '" + testuser2.getUsername() + "' with password '" + testuser2Credentials.getValue() + "'");

        UserRepresentation testuser3 = new UserRepresentation();
        testuser3.setUsername("testuser3");
        testuser3.setFirstName("DemoA3");
        testuser3.setLastName("DemoLast");
        testuser3.setEmail("testuser3@openremote.local");
        testuser3.setEnabled(true);
        tenantBuildingUsersResource.create(testuser3);
        testuser3 = tenantBuildingUsersResource.search("testuser3", null, null, null, null, null).get(0);
        this.testuser3Id = testuser3.getId();
        CredentialRepresentation testuser3Credentials = new CredentialRepresentation();
        testuser3Credentials.setType("password");
        testuser3Credentials.setValue("testuser3");
        testuser3Credentials.setTemporary(false);
        tenantBuildingUsersResource.get(testuser3.getId()).resetPassword(testuser3Credentials);
        tenantBuildingUsersResource.get(testuser3.getId()).roles().clientLevel(tenantBuildingClientObjectId).add(Arrays.asList(
            tenantBuildingRolesResource.get(ClientRole.WRITE_USER.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_MAP.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_ASSETS.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.WRITE_RULES.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.WRITE_ASSETS.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_RULES.getValue()).toRepresentation()
        ));
        LOG.info("Added demo user '" + testuser3.getUsername() + "' with password '" + testuser3Credentials.getValue() + "'");

        UserRepresentation buildingUser = new UserRepresentation();
        buildingUser.setUsername("building");
        buildingUser.setFirstName("Building");
        buildingUser.setLastName("User");
        buildingUser.setEmail("building@openremote.local");
        buildingUser.setEnabled(true);
        tenantBuildingUsersResource.create(buildingUser);
        buildingUser = tenantBuildingUsersResource.search("building", null, null, null, null, null).get(0);
        this.buildingUserId = buildingUser.getId();
        CredentialRepresentation buildingUserCredentials = new CredentialRepresentation();
        buildingUserCredentials.setType("password");
        buildingUserCredentials.setValue("building");
        buildingUserCredentials.setTemporary(false);
        tenantBuildingUsersResource.get(buildingUser.getId()).resetPassword(buildingUserCredentials);
        tenantBuildingUsersResource.get(buildingUser.getId()).roles().clientLevel(tenantBuildingClientObjectId).add(Arrays.asList(
            tenantBuildingRolesResource.get(ClientRole.WRITE_USER.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_MAP.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_ASSETS.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.WRITE_RULES.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.WRITE_ASSETS.getValue()).toRepresentation(),
            tenantBuildingRolesResource.get(ClientRole.READ_RULES.getValue()).toRepresentation()
        ));
        LOG.info("Added demo user '" + buildingUser.getUsername() + "' with password '" + buildingUserCredentials.getValue() + "'");

        UsersResource tenantCityUsersResource = keycloakProvider.getRealms(accessToken).realm("smartcity").users();
        ClientsResource tenantCityClientsResource = keycloakProvider.getRealms(accessToken).realm("smartcity").clients();
        String tenantCityClientObjectId = getClientObjectId(tenantCityClientsResource, KEYCLOAK_CLIENT_ID);
        RolesResource tenantCityRolesResource = tenantCityClientsResource.get(tenantCityClientObjectId).roles();

        UserRepresentation smartCityUser = new UserRepresentation();
        smartCityUser.setUsername("smartCity");
        smartCityUser.setFirstName("Smart");
        smartCityUser.setLastName("City");
        smartCityUser.setEmail("smartCityUser@openremote.local");
        smartCityUser.setEnabled(true);
        tenantCityUsersResource.create(smartCityUser);
        smartCityUser = tenantCityUsersResource.search("smartCity", null, null, null, null, null).get(0);
        this.smartCityUserId = smartCityUser.getId();
        CredentialRepresentation smartCityCredentials = new CredentialRepresentation();
        smartCityCredentials.setType("password");
        smartCityCredentials.setValue("smartCity");
        smartCityCredentials.setTemporary(false);
        tenantCityUsersResource.get(smartCityUser.getId()).resetPassword(smartCityCredentials);
        tenantCityUsersResource.get(smartCityUser.getId()).roles().clientLevel(tenantCityClientObjectId).add(Arrays.asList(
            tenantCityRolesResource.get(ClientRole.WRITE_USER.getValue()).toRepresentation(),
            tenantCityRolesResource.get(ClientRole.READ_MAP.getValue()).toRepresentation(),
            tenantCityRolesResource.get(ClientRole.READ_ASSETS.getValue()).toRepresentation(),
            tenantCityRolesResource.get(ClientRole.WRITE_ASSETS.getValue()).toRepresentation(),
            tenantCityRolesResource.get(ClientRole.READ_RULES.getValue()).toRepresentation(),
            tenantCityRolesResource.get(ClientRole.WRITE_RULES.getValue()).toRepresentation()
        ));
        LOG.info("Added demo user '" + smartCityUser.getUsername() + "' with password '" + smartCityCredentials.getValue() + "'");
    }
}
