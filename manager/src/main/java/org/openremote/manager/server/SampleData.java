package org.openremote.manager.server;

import elemental.json.Json;
import org.apache.log4j.Logger;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.*;
import org.openremote.manager.server.identity.KeycloakClient;
import org.openremote.manager.server.contextbroker.ContextBrokerService;
import org.openremote.manager.server.identity.IdentityService;
import org.openremote.manager.server.persistence.PersistenceService;
import org.openremote.manager.server.util.UrlUtil;
import org.openremote.manager.shared.model.ngsi.Attribute;
import org.openremote.manager.shared.model.ngsi.Entity;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID;
import static org.openremote.manager.server.Constants.MASTER_REALM;
import static org.openremote.manager.server.identity.KeycloakClient.ADMIN_CLI_CLIENT;
import static org.openremote.manager.server.util.IdentifierUtil.generateGlobalUniqueId;

public class SampleData {

    private static final Logger LOG = Logger.getLogger(SampleData.class.getName());

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    public void create(IdentityService identityService,
                       ContextBrokerService contextBrokerService,
                       PersistenceService persistenceService) {
        LOG.info("--- CREATING SAMPLE DATA ---");

        String accessToken = getAccessToken(identityService);
        configureMasterRealm(identityService, accessToken);
        registerClientApplications(identityService, accessToken);
        addRolesAndTestUsers(identityService, accessToken);

        createSampleRooms(contextBrokerService);

        /* TODO enable if we use JPA
        persistenceService.createSchema();
        EntityManager em = persistenceService.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(someSampleData);
        tx.commit();
        */

    }

    protected void createSampleRooms(ContextBrokerService contextBrokerService) {
        Entity room1 = new Entity(Json.createObject());
        room1.setId(generateGlobalUniqueId());
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
        room2.setId(generateGlobalUniqueId());
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


        contextBrokerService.getClient().listEntities()
            .flatMap(Observable::from)
            .flatMap(entity -> contextBrokerService.getClient().deleteEntity(entity))
            .toList().toBlocking().single();

        contextBrokerService.getClient().postEntity(room1).toBlocking().first();
        contextBrokerService.getClient().postEntity(room2).toBlocking().first();
    }

    protected String getAccessToken(IdentityService identityService) {
        KeycloakClient keycloakClient = identityService.getKeycloakClient();
        // Authorize as superuser
        AccessTokenResponse accessTokenResponse =
            keycloakClient.authenticateDirectly(MASTER_REALM, ADMIN_CLI_CLIENT, ADMIN_USERNAME, ADMIN_PASSWORD)
                .toBlocking().single();
        // Usually we would validate this token before using it, but I guess we are fine here...
        return accessTokenResponse.getToken();
    }

    protected void configureMasterRealm(IdentityService identityService, String accessToken) {
        KeycloakClient keycloakClient = identityService.getKeycloakClient();

        RealmRepresentation masterRealm =
            keycloakClient.getRealm(MASTER_REALM, accessToken).toBlocking().single();

        masterRealm.setDisplayNameHtml("<div class=\"kc-logo-text\"><span>OpenRemote</span></div>");

        masterRealm.setLoginTheme("openremote");

        keycloakClient.putRealm(MASTER_REALM, accessToken, masterRealm).toBlocking().single();
    }

    protected void registerClientApplications(IdentityService identityService, String accessToken) {
        KeycloakClient keycloakClient = identityService.getKeycloakClient();

        // Find out if there is a client already present for this application, if so, delete it
        keycloakClient.getClientApplications(MASTER_REALM, accessToken)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(MANAGER_CLIENT_ID))
            .flatMap(clientRepresentation -> keycloakClient.deleteClientApplication(MASTER_REALM, accessToken, clientRepresentation.getId()))
            .toBlocking().singleOrDefault(404);

        // Register a new client for this application
        ClientRepresentation managerClient = new ClientRepresentation();

        managerClient.setRegistrationAccessToken(accessToken);

        managerClient.setClientId(MANAGER_CLIENT_ID);

        managerClient.setName("OpenRemote Manager");
        managerClient.setPublicClient(true);

        String callbackUrl = UrlUtil.url(
            identityService.isConfigNetworkSecure() ? "https" : "http",
            identityService.getConfigNetworkHost(),
            identityService.getConfigNetworkWebserverPort(),
            MASTER_REALM
        ).toString();

        List<String> redirectUrls = new ArrayList<>();
        redirectUrls.add(callbackUrl);
        managerClient.setRedirectUris(redirectUrls);

        String clientResourceLocation =
            keycloakClient.registerClientApplication(MASTER_REALM, managerClient)
                .toBlocking().single();

        LOG.info("Registered client application '" + MANAGER_CLIENT_ID + "' with identity provider: " + clientResourceLocation);
    }

    protected void addRolesAndTestUsers(IdentityService identityService, String accessToken) {
        KeycloakClient keycloakClient = identityService.getKeycloakClient();

        String clientObjectId = keycloakClient.getClientApplications(MASTER_REALM, accessToken)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(MANAGER_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);

        // Register some roles
        RoleRepresentation readRole =
            keycloakClient.createRoleForClientApplication(
                MASTER_REALM, accessToken, clientObjectId, new RoleRepresentation("read", "Read all data", false)
            ).flatMap(location ->
                keycloakClient.getRoleOfClientApplicationByLocation(
                    MASTER_REALM, accessToken, location
                )
            ).toBlocking().single();
        LOG.info("Added role '" + readRole.getName() + "'");

        RoleRepresentation readMapRole =
            keycloakClient.createRoleForClientApplication(
                MASTER_REALM, accessToken, clientObjectId, new RoleRepresentation("read:map", "View map", false)
            ).flatMap(location ->
                keycloakClient.getRoleOfClientApplicationByLocation(
                    MASTER_REALM, accessToken, location
                )
            ).toBlocking().single();

        keycloakClient.addCompositesToRoleForClientApplication(
            MASTER_REALM, accessToken, clientObjectId, readRole.getName(), new RoleRepresentation[]{readMapRole}
        ).toBlocking().single();
        LOG.info("Added role '" + readMapRole.getName() + "'");

        // Find out if there is a 'test' user already present
        keycloakClient.getUsers(MASTER_REALM, accessToken)
            .filter(userRepresentation -> userRepresentation.getUsername().equals("test"))
            .flatMap(userRepresentation -> keycloakClient.deleteUser(MASTER_REALM, accessToken, userRepresentation.getId()))
            .toBlocking().singleOrDefault(404);

        // Create a new 'test' user with 'read' role
        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername("test");
        testUser.setFirstName("Testuserfirst");
        testUser.setLastName("Testuserlast");
        testUser.setEnabled(true);
        testUser = keycloakClient.createUser(MASTER_REALM, accessToken, testUser)
            .flatMap(location -> keycloakClient.getUserByLocation(MASTER_REALM, accessToken, location))
            .toBlocking().single();

        CredentialRepresentation testUserCredential = new CredentialRepresentation();
        testUserCredential.setType("password");
        testUserCredential.setValue("test");
        testUserCredential.setTemporary(false);
        keycloakClient.resetPassword(MASTER_REALM, accessToken, testUser.getId(), testUserCredential)
            .toBlocking().single();

        LOG.info("Added user '" + testUser.getUsername() + "' with password '" + testUserCredential.getValue() + "'");

        // Add mapping for client role 'read' to user 'test'
        keycloakClient.addUserClientRoleMapping(MASTER_REALM, accessToken, testUser.getId(), clientObjectId, new RoleRepresentation[]{readRole})
            .toBlocking().single();

    }
}
