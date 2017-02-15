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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import elemental.json.Json;
import org.apache.log4j.Logger;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.openremote.manager.server.agent.AgentAttributes;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.model.asset.ThingAttribute;
import org.openremote.manager.server.agent.ThingAttributes;
import org.openremote.model.asset.ProtocolConfiguration;
import org.openremote.agent3.protocol.simulator.SimulatorProtocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthForm;
import org.openremote.manager.server.agent.AgentService;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.*;
import org.openremote.model.asset.AssetAttributeMeta;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.Color;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.manager.shared.Constants.*;
import static org.openremote.model.AttributeType.*;
import static org.openremote.model.asset.AssetAttributeMeta.*;
import static org.openremote.model.asset.AssetType.BUILDING;
import static rx.Observable.fromCallable;

public class DemoDataService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(DemoDataService.class.getName());

    // We use this client ID to access Keycloak because by default it allows obtaining
    // an access token from authentication directly, which gives us full access to import/delete
    // demo data as needed.
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";

    public static final String DEMO_IMPORT_DATA = "DEMO_IMPORT_DATA";
    public static final boolean DEMO_IMPORT_DATA_DEFAULT = false;
    public static final String DEMO_ADMIN_PASSWORD = "DEMO_ADMIN_PASSWORD";
    public static final String DEMO_ADMIN_PASSWORD_DEFAULT = "secret";

    public static String DEMO_AGENT_ID = null;
    public static String DEMO_THING_ID = null;

    protected Container container;
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;
    protected AgentService agentService;
    protected AssetService assetService;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);
        agentService = container.getService(AgentService.class);
        assetService = container.getService(AssetService.class);
    }

    @Override
    public void start(Container container) {
        if (!container.isDevMode() && !getBoolean(container.getConfig(), DEMO_IMPORT_DATA, DEMO_IMPORT_DATA_DEFAULT)) {
            return;
        }

        LOG.info("--- IMPORTING DEMO DATA ---");

        // Use a non-proxy client to get the access token
        String demoAdminPassword = container.getConfig().getOrDefault(DEMO_ADMIN_PASSWORD, DEMO_ADMIN_PASSWORD_DEFAULT);
        String accessToken = identityService.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, demoAdminPassword)
        ).getToken();

        try {
            deleteRealms(accessToken);
            configureMasterRealm(accessToken);
            createTenants(accessToken);
            storeDemoAssets();

            LOG.info("--- DEMO DATA IMPORT COMPLETE ---");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop(Container container) {
    }

    protected void deleteRealms(String accessToken) {
        RealmsResource realmsResource = identityService.getRealms(accessToken, false);
        List<RealmRepresentation> realms = realmsResource.findAll();
        for (RealmRepresentation realmRepresentation : realms) {
            if (!realmRepresentation.getRealm().equals(MASTER_REALM)) {
                identityService.getRealms(accessToken, false).realm(realmRepresentation.getRealm()).remove();
            }
        }
    }

    protected void configureMasterRealm(String accessToken) {
        RealmResource realmResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM);
        ClientsResource clientsResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).clients();
        UsersResource usersResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).users();

        RealmRepresentation masterRealm = realmResource.toRepresentation();

        masterRealm.setDisplayName("Master");

        // Set theme, timeouts, etc.
        identityService.configureRealm(masterRealm);

        realmResource.update(masterRealm);

        // Find out if there is a client already present for this application, if so, delete it
        fromCallable(clientsResource::findAll)
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(KEYCLOAK_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .subscribe(clientObjectId -> {
                clientsResource.get(clientObjectId).remove();
            });

        identityService.createClientApplication(accessToken, masterRealm.getRealm());

        // Get the application client ID (roles are assigned at the client app level)
        String clientObjectId = fromCallable(() -> clientsResource.findByClientId(KEYCLOAK_CLIENT_ID))
            .flatMap(Observable::from)
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);

        ClientResource clientResource = clientsResource.get(clientObjectId);
        RolesResource rolesResource = clientResource.roles();

        // Give admin all roles on application client level (we can only check
        // realm _or_ application client roles in @RolesAllowed)!
        RoleRepresentation readRole = rolesResource.get(ClientRole.READ.getValue()).toRepresentation();
        RoleRepresentation writeRole = rolesResource.get(ClientRole.WRITE.getValue()).toRepresentation();

        fromCallable(() -> usersResource.search(MASTER_REALM_ADMIN_USER, null, null, null, null, null))
            .flatMap(Observable::from)
            .map(userRepresentation -> usersResource.get(userRepresentation.getId()))
            .subscribe(adminUser -> {
                    adminUser.roles().clientLevel(clientObjectId).add(Arrays.asList(
                        readRole,
                        writeRole
                    ));
                    LOG.info("Assigned all application roles to 'admin' user");
                    UserRepresentation adminRep = adminUser.toRepresentation();
                    adminRep.setFirstName("System");
                    adminRep.setLastName("Administrator");
                    adminUser.update(adminRep);
                }
            );

        // Find out if there are any users except the admin, delete them
        fromCallable(() -> usersResource.search(null, null, null))
            .flatMap(Observable::from)
            .filter(userRepresentation -> !userRepresentation.getUsername().equals(MASTER_REALM_ADMIN_USER))
            .map(userRepresentation -> usersResource.get(userRepresentation.getId()))
            .subscribe(UserResource::remove);

        // Create a new 'test' user with 'read' role
        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername("test");
        testUser.setFirstName("Testuserfirst");
        testUser.setLastName("Testuserlast");
        testUser.setEnabled(true);
        usersResource.create(testUser);
        testUser = usersResource.search("test", null, null, null, null, null).get(0);

        CredentialRepresentation testUserCredential = new CredentialRepresentation();
        testUserCredential.setType("password");
        testUserCredential.setValue("test");
        testUserCredential.setTemporary(false);
        usersResource.get(testUser.getId()).resetPassword(testUserCredential);

        LOG.info("Added user '" + testUser.getUsername() + "' with password '" + testUserCredential.getValue() + "'");

        // Add mapping for client role 'read' to user 'test'
        usersResource.get(testUser.getId()).roles().clientLevel(clientObjectId).add(Arrays.asList(
            readRole,
            writeRole
        ));
    }

    protected void createTenants(String accessToken) throws Exception {
        Tenant customerA = new Tenant();
        customerA.setRealm("customerA");
        customerA.setDisplayName("Customer A");
        customerA.setEnabled(true);
        identityService.createTenant(accessToken, customerA);

        Tenant customerB = new Tenant();
        customerB.setRealm("customerB");
        customerB.setDisplayName("Customer B");
        customerB.setEnabled(true);
        identityService.createTenant(accessToken, customerB);
    }

    protected void storeDemoAssets() {

        GeometryFactory geometryFactory = new GeometryFactory();

        ServerAsset smartOffice = new ServerAsset();
        smartOffice.setRealm(MASTER_REALM);
        smartOffice.setName("Smart Office");
        smartOffice.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        smartOffice.setType(BUILDING);
        Attributes smartOfficeAttributes = new Attributes();
        smartOfficeAttributes.put(
            new Attribute("geoStreet", STRING, Json.create("Torenallee 20"))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("Street")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoStreet")))
                ),
            new Attribute("geoPostalCode", AttributeType.INTEGER, Json.create(5617))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("Postal Code")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoPostalCode")))
                ),
            new Attribute("geoCity", STRING, Json.create("Eindhoven"))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("City")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCity")))
                ),
            new Attribute("geoCountry", STRING, Json.create("Netherlands"))
                .setMetadata(new Metadata()
                    .add(createMetadataItem(LABEL, Json.create("Country")))
                    .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCountry")))
                )
        );
        smartOffice.setAttributes(smartOfficeAttributes.getJsonObject());
        smartOffice = assetService.merge(smartOffice);

        ServerAsset groundfloor = new ServerAsset(smartOffice);
        groundfloor.setName("Ground Floor");
        groundfloor.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        groundfloor.setType(AssetType.FLOOR);
        groundfloor = assetService.merge(groundfloor);

        ServerAsset lobby = new ServerAsset(groundfloor);
        lobby.setName("Lobby");
        lobby.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        lobby.setType(AssetType.ROOM);
        lobby = assetService.merge(lobby);

        ServerAsset agent = new ServerAsset(lobby);
        agent.setName("Demo Agent");
        agent.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        agent.setType(AssetType.AGENT);
        AgentAttributes agentAttributes = new AgentAttributes();
        agentAttributes.setEnabled(false);
        ProtocolConfiguration protocolConfigSimulator123 = new ProtocolConfiguration("simulator123", SimulatorProtocol.PROTOCOL_NAME);
        agentAttributes.put(protocolConfigSimulator123);
        agent.setAttributes(agentAttributes.getJsonObject());
        agent = assetService.merge(agent);
        DEMO_AGENT_ID = agent.getId();

        ServerAsset thing = new ServerAsset(agent);
        thing.setName("Demo Thing");
        thing.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        thing.setType(AssetType.THING);
        ThingAttributes thingAttributes = new ThingAttributes(thing);
        thingAttributes.put(
            new Attribute("light1Toggle", BOOLEAN, Json.create(true))
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The switch for the light in the living room"))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("switch")
                    ))
                ),
            new Attribute("light1Dimmer", INTEGER, Json.create(55))
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The dimmer for the light in the living room"))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.RANGE_MIN.getName(),
                        Json.create(0))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.RANGE_MAX.getName(),
                        Json.create(100))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("range")
                    ))
                ),
            new Attribute("light1Color", COLOR, new Color(88, 123, 88).asJsonValue())
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The color of the living room light"))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("color")
                    ))
                ),
            new Attribute("light1PowerConsumption", DECIMAL, Json.create(12.345))
                .setMetadata(new Metadata()
                    .add(new MetadataItem(
                        AssetAttributeMeta.DESCRIPTION.getName(),
                        Json.create("The total power consumption of the living room light"))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.READ_ONLY.getName(),
                        Json.create(true))
                    )
                    .add(new MetadataItem(
                        AssetAttributeMeta.FORMAT.getName(),
                        Json.create("%3d kWh"))
                    )
                    .add(new MetadataItem(
                        ThingAttribute.META_NAME_LINK, new AttributeRef(agent.getId(), "simulator123").asJsonValue()
                    ))
                    .add(new MetadataItem(
                        SimulatorProtocol.META_NAME_ELEMENT, Json.create("decimal")
                    ))
                )
        );
        thing.setAttributes(thingAttributes.getJsonObject());
        thing = assetService.merge(thing);
        DEMO_THING_ID = thing.getId();

    }
}
