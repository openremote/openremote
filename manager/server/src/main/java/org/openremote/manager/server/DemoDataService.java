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
import org.apache.log4j.Logger;
import org.keycloak.admin.client.resource.*;
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
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.security.Tenant;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

import static org.openremote.manager.shared.Constants.*;
import static rx.Observable.fromCallable;

public class DemoDataService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(DemoDataService.class.getName());

    public static final String IMPORT_DEMO_DATA = "IMPORT_DEMO_DATA";
    public static final boolean IMPORT_DEMO_DATA_DEFAULT = false;

    public static final String DEMO_CONTROLLER_HOST = "DEMO_CONTROLLER_HOST";
    public static final String DEMO_CONTROLLER_HOST_DEFAULT = "192.168.99.100";
    public static final String DEMO_CONTROLLER_PORT = "DEMO_CONTROLLER_PORT";
    public static final int DEMO_CONTROLLER_PORT_DEFAULT = 8083;
    public static final String DEMO_CONTROLLER_USERNAME = "DEMO_CONTROLLER_USERNAME";
    public static final String DEMO_CONTROLLER_USERNAME_DEFAULT = "";
    public static final String DEMO_CONTROLLER_PASSWORD = "DEMO_CONTROLLER_PASSWORD";
    public static final String DEMO_CONTROLLER_PASSWORD_DEFAULT = "";
    public static final String DEMO_CONTROLLER_SECURE = "DEMO_CONTROLLER_SECURE";
    public static final boolean DEMO_CONTROLLER_SECURE_DEFAULT = false;

    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    public static final String ADMIN_PASSWORD = "admin";

    public String DEMO_AGENT_ID = null;

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
        if (!container.isDevMode() && !container.getConfigBoolean(IMPORT_DEMO_DATA, IMPORT_DEMO_DATA_DEFAULT)) {
            return;
        }

        LOG.info("--- IMPORTING DEMO DATA ---");

        // Use a non-proxy client to get the access token
        String accessToken = identityService.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, ADMIN_PASSWORD)
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

        identityService.configureRealm(masterRealm);

        realmResource.update(masterRealm);

        // Find out if there is a client already present for this application, if so, delete it
        fromCallable(clientsResource::findAll)
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(APP_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .subscribe(clientObjectId -> {
                clientsResource.get(clientObjectId).remove();
            });

        identityService.createClientApplication(accessToken, masterRealm.getRealm());

        String clientObjectId = fromCallable(() -> clientsResource.findByClientId(APP_CLIENT_ID))
            .flatMap(Observable::from)
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);

        ClientResource clientResource = clientsResource.get(clientObjectId);
        RolesResource rolesResource = clientResource.roles();

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
        smartOffice.setType(AssetType.BUILDING);
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

        ServerAsset agentAsset = new ServerAsset(lobby);
        agentAsset.setName("Demo Controller");
        agentAsset.setLocation(geometryFactory.createPoint(new Coordinate(5.460315214821094, 51.44541688237109)));
        agentAsset.setType(AssetType.AGENT);

        Agent agent = new Agent(new Attributes(), true);
        agent.setEnabled(true);
        agent.setConnectorType("urn:openremote:connector:controller2");
        for (Attribute connectorSetting : Controller2Component.SETTINGS.get()) {
            agent.getAttributes().put(connectorSetting.copy());
        }

        agent.getAttributes().get("host").setValue(
            container.getConfig(DEMO_CONTROLLER_HOST, DEMO_CONTROLLER_HOST_DEFAULT)
        );
        agent.getAttributes().get("port").setValue(
            container.getConfigInteger(DEMO_CONTROLLER_PORT, DEMO_CONTROLLER_PORT_DEFAULT)
        );
        agent.getAttributes().get("username").setValue(
            container.getConfig(DEMO_CONTROLLER_USERNAME, DEMO_CONTROLLER_USERNAME_DEFAULT)
        );
        agent.getAttributes().get("password").setValue(
            container.getConfig(DEMO_CONTROLLER_PASSWORD, DEMO_CONTROLLER_PASSWORD_DEFAULT)
        );
        agent.getAttributes().get("secure").setValue(
            container.getConfigBoolean(DEMO_CONTROLLER_SECURE, DEMO_CONTROLLER_SECURE_DEFAULT)
        );

        agentAsset.setAttributes(agent.getAttributes().getJsonObject());

        LOG.info("Adding demo agent: " + agentAsset);
        LOG.info("Configured demo agent attributes: " + agent.getAttributes());

        agentAsset = assetService.merge(agentAsset);

        DEMO_AGENT_ID = agentAsset.getId();
    }
}
