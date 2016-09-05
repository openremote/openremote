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
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.idm.*;
import org.openremote.agent.controller2.Controller2Component;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthForm;
import org.openremote.manager.server.agent.AgentService;
import org.openremote.manager.server.agent.ConnectorService;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.asset.AssetAttributeType;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.attribute.*;
import org.openremote.manager.shared.connector.Connector;
import rx.Observable;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openremote.manager.shared.Constants.*;
import static org.openremote.manager.shared.attribute.AttributeType.STRING;
import static rx.Observable.fromCallable;

public class SampleDataService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SampleDataService.class.getName());

    public static final String IMPORT_SAMPLE_DATA = "IMPORT_SAMPLE_DATA";
    public static final boolean IMPORT_SAMPLE_DATA_DEFAULT = false;

    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    public static final String ADMIN_PASSWORD = "admin";

    protected Container container;
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;
    protected ConnectorService connectorService;
    protected AgentService agentService;
    protected AssetService assetService;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);
        connectorService = container.getService(ConnectorService.class);
        agentService = container.getService(AgentService.class);
        assetService = container.getService(AssetService.class);
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

        // Use a non-proxy client to get the access token
        String accessToken = identityService.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, ADMIN_PASSWORD)
        ).getToken();

        deleteRealms(accessToken);
        configureMasterRealm(accessToken);
        registerClientApplications(accessToken);
        addRolesAndTestUsers(accessToken);
        storeSampleAssets();
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
        RealmRepresentation masterRealm = realmResource.toRepresentation();

        masterRealm.setDisplayName("OpenRemote");
        masterRealm.setDisplayNameHtml("<div class=\"kc-logo-text\"><span>OpenRemote</span></div>");

        masterRealm.setLoginTheme("openremote");
        masterRealm.setAccountTheme("openremote");

        masterRealm.setSsoSessionIdleTimeout(10800); // 3 hours

        // TODO: Make SSL setup configurable
        masterRealm.setSslRequired(SslRequired.NONE.toString());

        // TODO: This should only be set in dev mode, 60 seconds is enough in production?
        masterRealm.setAccessTokenLifespan(900); // 15 minutes

        realmResource.update(masterRealm);
    }

    protected void registerClientApplications(String accessToken) {
        ClientsResource clientsResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).clients();

        // Find out if there is a client already present for this application, if so, delete it
        fromCallable(clientsResource::findAll)
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(MANAGER_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .subscribe(clientObjectId -> {
                clientsResource.get(clientObjectId).remove();
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
            clientsResource.create(managerClient).getLocation().toString();

        LOG.info("Registered client application '" + MANAGER_CLIENT_ID + "' with identity provider: " + clientResourceLocation);
    }

    protected void addRolesAndTestUsers(String accessToken) {
        ClientsResource clientsResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).clients();
        UsersResource usersResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).users();

        String clientObjectId = fromCallable(() -> clientsResource.findByClientId(MANAGER_CLIENT_ID))
            .flatMap(Observable::from)
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);

        // Register some roles
        ClientResource clientResource = clientsResource.get(clientObjectId);
        RolesResource rolesResource = clientResource.roles();

        identityService.addDefaultRoles(rolesResource);

        // Give admin all roles (we can only check realm _or_ application roles in @RolesAllowed)!
        RoleRepresentation readRole = rolesResource.get("read").toRepresentation();
        RoleRepresentation writeRole = rolesResource.get("write").toRepresentation();

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

    protected void storeSampleAssets() {

        GeometryFactory geometryFactory = new GeometryFactory();

        ServerAsset smartOffice = new ServerAsset();
        smartOffice.setName("Smart Office");
        smartOffice.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        smartOffice.setType(AssetType.BUILDING);
        assetService.create(smartOffice);

        ServerAsset floor;
        ServerAsset floor6 = null;
        for (int i = 0; i < 7; i++) {
            floor = new ServerAsset(smartOffice);
            floor.setName("Floor " + (i + 1));
            floor.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
            floor.setType(AssetType.FLOOR);
            assetService.create(floor);

            if (i == 5)
                floor6 = floor;
        }

        ServerAsset room;
        ServerAsset room3 = null;
        for (int i = 0; i < 13; i++) {
            room = new ServerAsset(floor6);
            room.setName("6.00" + (i + 1));
            room.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));

            if (i == 2) {
                // This is our sample agent
                room.setType(AssetType.AGENT);
                Attributes room3Attributes = new Attributes();
                room3Attributes.put(
                    new Attribute(Connector.ASSET_ATTRIBUTE_CONNECTOR, STRING, Json.create("urn:openremote:connector:controller2"))
                );
                for (Attribute connectorSetting : Controller2Component.SETTINGS.get()) {
                    room3Attributes.put(connectorSetting.copy());
                }
                room3Attributes.get("host").setValue("192.168.123.123");
                room3Attributes.get("port").setValue(8080);
                room.setAttributes(room3Attributes.getJsonObject());
            } else {
                room.setType(AssetType.ROOM);
            }

            assetService.create(room);

            if (i == 2)
                room3 = room;
        }

        ServerAsset wallpanel = new ServerAsset(room3);
        wallpanel.setName("Wallpanel");
        wallpanel.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        wallpanel.setType(AssetType.DEVICE);
        Attributes wallpanelAttributes = new Attributes();
        wallpanelAttributes.put(
            new Attribute("temperature", AttributeType.FLOAT, Json.create(21.3))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.SENSOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("Actual Temperature")))
                    .addElement(new MetadataElement("scale", "urn:openremote:scale", Json.create("celcius")))
                ),
            new Attribute("setpointSensor", AttributeType.FLOAT, Json.create(22.5))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.SENSOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("Setpoint Temperature")))
                    .addElement(new MetadataElement("scale", "urn:openremote:scale", Json.create("celcius")))
                ),
            new Attribute("setpointActuator", AttributeType.FLOAT, Json.create(22.5))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.ACTUATOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("Setpoint Control")))
                    .addElement(new MetadataElement("scale", "urn:openremote:scale", Json.create("celcius")))
                ),
            new Attribute("statusSensor", AttributeType.BOOLEAN, Json.create(true))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.SENSOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("On/Off Status")))
                ),
            new Attribute("statusActuator", AttributeType.BOOLEAN, Json.create(true))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.ACTUATOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("On/Off Control")))
                )
        );
        wallpanel.setAttributes(wallpanelAttributes.getJsonObject());
        assetService.create(wallpanel);

        ServerAsset presence = new ServerAsset(room3);
        presence.setName("Presence");
        presence.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        presence.setType(AssetType.DEVICE);
        Attributes presenceAttributes = new Attributes();
        presenceAttributes.put(
            new Attribute("presence", AttributeType.BOOLEAN, Json.create(false))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.SENSOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("Presence Detected")))
                )
        );
        presence.setAttributes(presenceAttributes.getJsonObject());
        assetService.create(presence);

        ServerAsset windows = new ServerAsset(room3);
        windows.setName("Windows");
        windows.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        windows.setType(AssetType.DEVICE);
        Attributes windowsAttributes = new Attributes();
        windowsAttributes.put(
            new Attribute("status", AttributeType.BOOLEAN, Json.create(false))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.SENSOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("Windows Open")))
                )
        );
        windows.setAttributes(windowsAttributes.getJsonObject());
        assetService.create(windows);

        ServerAsset valve = new ServerAsset(room3);
        valve.setName("Valve");
        valve.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        valve.setType(AssetType.DEVICE);
        Attributes valveAttributes = new Attributes();
        valveAttributes.put(
            new Attribute("status", AttributeType.BOOLEAN, Json.create(true))
                .setMetadata(new Metadata()
                    .addElement(new MetadataElement("type", AttributeType.STRING.getValue(), AssetAttributeType.SENSOR.getJsonValue()))
                    .addElement(new MetadataElement("label", AttributeType.STRING.getValue(), Json.create("Valve Open")))
                )
        );
        valve.setAttributes(valveAttributes.getJsonObject());
        assetService.create(valve);
    }
}
