package org.openremote.manager.server;

import elemental.json.Json;
import org.apache.log4j.Logger;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
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

        registerClientApplications(identityService);

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

    protected void registerClientApplications(IdentityService identityService) {
        KeycloakClient keycloakClient = identityService.getKeycloakClient();

        // Authorize as superuser
        AccessTokenResponse accessTokenResponse =
            keycloakClient.authenticateDirectly(MASTER_REALM, ADMIN_CLI_CLIENT, ADMIN_USERNAME, ADMIN_PASSWORD)
                .toBlocking().single();
        String accessToken = accessTokenResponse.getToken();
        // Usually we would validate this token before using it, but I guess we are fine here...

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

        LOG.info("Registered '" + MANAGER_CLIENT_ID + "' with identity provider: " + clientResourceLocation);
    }
}
