package org.openremote.manager.server2;

import elemental.json.Json;
import org.apache.log4j.Logger;
import org.keycloak.representations.idm.*;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.server.contextbroker.ContextBrokerService;
import org.openremote.manager.server.util.UrlUtil;
import org.openremote.manager.server2.identity.AuthForm;
import org.openremote.manager.server2.identity.IdentityService;
import org.openremote.manager.server2.identity.Keycloak;
import org.openremote.manager.shared.model.ngsi.Attribute;
import org.openremote.manager.shared.model.ngsi.Entity;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID;
import static org.openremote.manager.server.Constants.MASTER_REALM;
import static org.openremote.manager.server.identity.KeycloakClient.ADMIN_CLI_CLIENT;
import static org.openremote.manager.server.util.IdentifierUtil.generateGlobalUniqueId;
import static rx.Observable.fromCallable;

public class SampleDataService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SampleDataService.class.getName());

    public static final String IMPORT_SAMPLE_DATA = "IMPORT_SAMPLE_DATA";
    public static final boolean IMPORT_SAMPLE_DATA_DEFAULT = false;

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    protected IdentityService identityService;
    /* TODO
    protected ContextBrokerService contextBrokerService,
    protected PersistenceService persistenceService
    */

    @Override
    public void prepare(Container container) {
        if (!container.isDevMode())
            return;

        identityService = container.getService(IdentityService.class);
    }

    @Override
    public void start(Container container) {
        if (!container.isDevMode())
            return;

        LOG.info("--- CREATING SAMPLE DATA ---");

        String accessToken = identityService.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT, ADMIN_USERNAME, ADMIN_PASSWORD)
        ).getToken();

        configureMasterRealm(identityService, accessToken);
        registerClientApplications(identityService, accessToken);
        addRolesAndTestUsers(identityService, accessToken);

        /* TODO
        createSampleRooms(contextBrokerService);

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

    protected void configureMasterRealm(IdentityService identityService, String accessToken) {
        Keycloak keycloak = identityService.getKeycloak(accessToken);

        RealmRepresentation masterRealm = keycloak.getRealm(MASTER_REALM);

        masterRealm.setDisplayNameHtml("<div class=\"kc-logo-text\"><span>OpenRemote</span></div>");

        masterRealm.setLoginTheme("openremote");

        keycloak.putRealm(MASTER_REALM, masterRealm);
    }

    protected void registerClientApplications(IdentityService identityService, String accessToken) {
        Keycloak keycloak = identityService.getKeycloak(accessToken);

        // Find out if there is a client already present for this application, if so, delete it
        fromCallable(() -> keycloak.getClientApplications(MASTER_REALM))
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(MANAGER_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .subscribe(clientObjectId -> {
                keycloak.deleteClientApplication(MASTER_REALM, clientObjectId);
            });

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
            keycloak.registerClientApplication(MASTER_REALM, managerClient).getLocation().toString();

        LOG.info("Registered client application '" + MANAGER_CLIENT_ID + "' with identity provider: " + clientResourceLocation);
    }

    protected void addRolesAndTestUsers(IdentityService identityService, String accessToken) {
        Keycloak keycloak = identityService.getKeycloak(accessToken);

        String clientObjectId = fromCallable(() -> keycloak.getClientApplications(MASTER_REALM))
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(MANAGER_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);

        // Register some roles
        RoleRepresentation readRole =
            fromCallable(() -> keycloak.createRoleForClientApplication(
                MASTER_REALM, clientObjectId, new RoleRepresentation("read", "Read all data", false)
            )).map(response ->
                identityService.getTarget(response.getLocation(), accessToken).request(APPLICATION_JSON).get(RoleRepresentation.class)
            ).toBlocking().single();
        LOG.info("Added role '" + readRole.getName() + "'");

        RoleRepresentation readMapRole =
            fromCallable(() -> keycloak.createRoleForClientApplication(
                MASTER_REALM, clientObjectId, new RoleRepresentation("read:map", "View map", false)
            )).map(response ->
                identityService.getTarget(response.getLocation(), accessToken).request(APPLICATION_JSON).get(RoleRepresentation.class)
            ).toBlocking().single();

        keycloak.addCompositesToRoleForClientApplication(
            MASTER_REALM, clientObjectId, readRole.getName(), new RoleRepresentation[]{readMapRole}
        );
        LOG.info("Added role '" + readMapRole.getName() + "'");

        // Find out if there is a 'test' user already present
        fromCallable(() -> keycloak.getUsers(MASTER_REALM))
            .flatMap(Observable::from)
            .filter(userRepresentation -> userRepresentation.getUsername().equals("test"))
            .subscribe(userRepresentation -> keycloak.deleteUser(MASTER_REALM, userRepresentation.getId()));

        // Create a new 'test' user with 'read' role
        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername("test");
        testUser.setFirstName("Testuserfirst");
        testUser.setLastName("Testuserlast");
        testUser.setEnabled(true);
        final UserRepresentation finalTestUser = testUser;
        testUser = fromCallable(() -> keycloak.createUser(MASTER_REALM, finalTestUser))
            .map(response ->
                identityService.getTarget(response.getLocation(), accessToken).request(APPLICATION_JSON).get(UserRepresentation.class)
            ).toBlocking().single();

        CredentialRepresentation testUserCredential = new CredentialRepresentation();
        testUserCredential.setType("password");
        testUserCredential.setValue("test");
        testUserCredential.setTemporary(false);
        keycloak.resetPassword(MASTER_REALM, testUser.getId(), testUserCredential);

        LOG.info("Added user '" + testUser.getUsername() + "' with password '" + testUserCredential.getValue() + "'");

        // Add mapping for client role 'read' to user 'test'
        keycloak.addUserClientRoleMapping(MASTER_REALM, testUser.getId(), clientObjectId, new RoleRepresentation[]{readRole});

    }
}
