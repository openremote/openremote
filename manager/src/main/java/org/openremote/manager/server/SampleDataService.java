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
package org.openremote.manager.server;

import elemental.json.Json;
import org.apache.log4j.Logger;
import org.keycloak.representations.idm.*;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.security.AuthForm;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.KeycloakResource;
import org.openremote.manager.server.assets.AssetsService;
import org.openremote.manager.server.assets.ContextBrokerResource;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.ngsi.Attribute;
import org.openremote.manager.shared.ngsi.Entity;
import rx.Observable;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.container.web.WebClient.getTarget;
import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID;
import static org.openremote.manager.server.Constants.MASTER_REALM;
import static rx.Observable.fromCallable;

public class SampleDataService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SampleDataService.class.getName());

    public static final String IMPORT_SAMPLE_DATA = "IMPORT_SAMPLE_DATA";
    public static final boolean IMPORT_SAMPLE_DATA_DEFAULT = false;

    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    protected IdentityService identityService;
    protected AssetsService assetsService;
    /* TODO
    protected PersistenceService persistenceService
    */

    @Override
    public void init(Container container) throws Exception {
        identityService = container.getService(ManagerIdentityService.class);
        assetsService = container.getService(AssetsService.class);
    }

    @Override
    public void configure(Container container) throws Exception {

    }

    @Override
    public void start(Container container) {
        if (!container.isDevMode() && !container.getConfigBoolean(IMPORT_SAMPLE_DATA, IMPORT_SAMPLE_DATA_DEFAULT)) {
            return;
        }

        LOG.info("--- CREATING SAMPLE DATA ---");

        String accessToken = identityService.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, ADMIN_USERNAME, ADMIN_PASSWORD)
        ).getToken();

        configureMasterRealm(identityService, accessToken);
        registerClientApplications(identityService, accessToken);
        addRolesAndTestUsers(identityService, accessToken);

        createSampleRooms(assetsService);

        /* TODO
        persistenceService.createSchema();
        EntityManager em = persistenceService.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(someSampleData);
        tx.commit();
        */
    }

    @Override
    public void stop(Container container) {
    }

    protected void createSampleRooms(AssetsService assetsService) {
        Entity room1 = new Entity(Json.createObject());
        room1.setId("Room1");
        room1.setType("Room");
        room1.addAttribute(
            new Attribute("temperature", Json.createObject())
                .setType("float")
                .setValue(Json.create(21.3))
        ).addAttribute(
            new Attribute("label", Json.createObject())
                .setType("string")
                .setValue(Json.create("Office 123"))
        );

        Entity room2 = new Entity(Json.createObject());
        room2.setId("Room2");
        room2.setType("Room");
        room2.addAttribute(
            new Attribute("temperature", Json.createObject())
                .setType("float")
                .setValue(Json.create(22.1))
        ).addAttribute(
            new Attribute("label", Json.createObject())
                .setType("string")
                .setValue(Json.create("Office 456"))
        );

        ContextBrokerResource ngsiService = assetsService.getContextBroker();

        fromCallable(() -> ngsiService.getEntities(null))
            .map(Entity::from)
            .flatMap(Observable::from)
            .flatMap(entity -> fromCallable(() -> ngsiService.deleteEntity(entity.getId())))
            .toList().toBlocking().single();

        ngsiService.postEntity(room1);
        ngsiService.postEntity(room2);
    }

    protected void configureMasterRealm(IdentityService identityService, String accessToken) {
        KeycloakResource keycloakResource = identityService.getKeycloak(accessToken);

        RealmRepresentation masterRealm = keycloakResource.getRealm(MASTER_REALM);

        masterRealm.setDisplayNameHtml("<div class=\"kc-logo-text\"><span>OpenRemote</span></div>");

        masterRealm.setLoginTheme("openremote");
        masterRealm.setAccountTheme("openremote");

        keycloakResource.putRealm(MASTER_REALM, masterRealm);
    }

    protected void registerClientApplications(IdentityService identityService, String accessToken) {
        KeycloakResource keycloakResource = identityService.getKeycloak(accessToken);

        // Find out if there is a client already present for this application, if so, delete it
        fromCallable(() -> keycloakResource.getClientApplications(MASTER_REALM))
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(MANAGER_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .subscribe(clientObjectId -> {
                keycloakResource.deleteClientApplication(MASTER_REALM, clientObjectId);
            });

        // Register a new client for this application
        ClientRepresentation managerClient = new ClientRepresentation();

        managerClient.setRegistrationAccessToken(accessToken);

        managerClient.setClientId(MANAGER_CLIENT_ID);

        managerClient.setName("OpenRemote Manager");
        managerClient.setPublicClient(true);

        // TODO this should only be enabled in dev mode, we need it for integration tests
        managerClient.setDirectAccessGrantsEnabled(true);

        String callbackUrl = UriBuilder.fromUri("/").path(MASTER_REALM).path("*").build().toString();

        List<String> redirectUrls = new ArrayList<>();
        redirectUrls.add(callbackUrl);
        managerClient.setRedirectUris(redirectUrls);

        String baseUrl = UriBuilder.fromUri("/").path(MASTER_REALM).build().toString();
        managerClient.setBaseUrl(baseUrl);

        String clientResourceLocation =
            keycloakResource.registerClientApplication(MASTER_REALM, managerClient).getLocation().toString();

        LOG.info("Registered client application '" + MANAGER_CLIENT_ID + "' with identity provider: " + clientResourceLocation);
    }

    protected void addRolesAndTestUsers(IdentityService identityService, String accessToken) {
        KeycloakResource keycloakResource = identityService.getKeycloak(accessToken);

        String clientObjectId = fromCallable(() -> keycloakResource.getClientApplications(MASTER_REALM))
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(MANAGER_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);

        // Register some roles
        RoleRepresentation readRole =
            fromCallable(() -> keycloakResource.createRoleForClientApplication(
                MASTER_REALM, clientObjectId, new RoleRepresentation("read", "Read all data", false)
            )).map(response ->
                getTarget(identityService.getHttpClient(), response.getLocation(), accessToken).request(APPLICATION_JSON).get(RoleRepresentation.class)
            ).toBlocking().single();
        LOG.info("Added role '" + readRole.getName() + "'");

        RoleRepresentation readMapRole =
            fromCallable(() -> keycloakResource.createRoleForClientApplication(
                MASTER_REALM, clientObjectId, new RoleRepresentation("read:map", "View map", false)
            )).map(response ->
                getTarget(identityService.getHttpClient(), response.getLocation(), accessToken).request(APPLICATION_JSON).get(RoleRepresentation.class)
            ).toBlocking().single();

        RoleRepresentation readAssetsRole =
            fromCallable(() -> keycloakResource.createRoleForClientApplication(
                MASTER_REALM, clientObjectId, new RoleRepresentation("read:assets", "Read context broker assets", false)
            )).map(response ->
                getTarget(identityService.getHttpClient(), response.getLocation(), accessToken).request(APPLICATION_JSON).get(RoleRepresentation.class)
            ).toBlocking().single();
        LOG.info("Added role '" + readAssetsRole.getName() + "'");

        keycloakResource.addCompositesToRoleForClientApplication(
            MASTER_REALM, clientObjectId, readRole.getName(), new RoleRepresentation[]{readMapRole, readAssetsRole}
        );

        RoleRepresentation writeRole =
            fromCallable(() -> keycloakResource.createRoleForClientApplication(
                MASTER_REALM, clientObjectId, new RoleRepresentation("write", "Write all data", false)
            )).map(response ->
                getTarget(identityService.getHttpClient(), response.getLocation(), accessToken).request(APPLICATION_JSON).get(RoleRepresentation.class)
            ).toBlocking().single();
        LOG.info("Added role '" + writeRole.getName() + "'");

        RoleRepresentation writeAssetsRole =
            fromCallable(() -> keycloakResource.createRoleForClientApplication(
                MASTER_REALM, clientObjectId, new RoleRepresentation("write:assets", "Write context broker assets", false)
            )).map(response ->
                getTarget(identityService.getHttpClient(), response.getLocation(), accessToken).request(APPLICATION_JSON).get(RoleRepresentation.class)
            ).toBlocking().single();
        LOG.info("Added role '" + writeAssetsRole.getName() + "'");

        keycloakResource.addCompositesToRoleForClientApplication(
            MASTER_REALM, clientObjectId, writeRole.getName(), new RoleRepresentation[]{writeAssetsRole}
        );

        // Give admin all roles (we can only check realm _or_ application roles in @RolesAllowed)!
        fromCallable(() -> keycloakResource.getUsers(MASTER_REALM, "admin"))
            .flatMap(Observable::from)
            .subscribe(adminUser -> {
                    keycloakResource.addUserClientRoleMapping(MASTER_REALM, adminUser.getId(), clientObjectId, new RoleRepresentation[]{
                        readRole,
                        writeRole
                    });
                    LOG.info("Assigned all application roles to 'admin' user");
                }
            );

        // Find out if there is a 'test' user already present, delete it
        fromCallable(() -> keycloakResource.getUsers(MASTER_REALM, "test"))
            .flatMap(Observable::from)
            .subscribe(testUser -> keycloakResource.deleteUser(MASTER_REALM, testUser.getId()));

        // Create a new 'test' user with 'read' role
        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername("test");
        testUser.setFirstName("Testuserfirst");
        testUser.setLastName("Testuserlast");
        testUser.setEnabled(true);
        final UserRepresentation finalTestUser = testUser;
        testUser = fromCallable(() -> keycloakResource.createUser(MASTER_REALM, finalTestUser))
            .map(response ->
                getTarget(identityService.getHttpClient(), response.getLocation(), accessToken).request(APPLICATION_JSON).get(UserRepresentation.class)
            ).toBlocking().single();

        CredentialRepresentation testUserCredential = new CredentialRepresentation();
        testUserCredential.setType("password");
        testUserCredential.setValue("test");
        testUserCredential.setTemporary(false);
        keycloakResource.resetPassword(MASTER_REALM, testUser.getId(), testUserCredential);

        LOG.info("Added user '" + testUser.getUsername() + "' with password '" + testUserCredential.getValue() + "'");

        // Add mapping for client role 'read' to user 'test'
        keycloakResource.addUserClientRoleMapping(MASTER_REALM, testUser.getId(), clientObjectId, new RoleRepresentation[]{
            readRole,
            writeRole
        });

    }
}
